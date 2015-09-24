package com.sekakuoro.depart;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import android.graphics.Rect;

import com.sekakuoro.depart.activities.MyMapActivity;

/*
 - Stops and vehicles are loaded periodically.
 - Only loads stuff that can be visible on the screen, or close to be seen.
 - Shows a spinner when loading something takes a long time.
 - Removes the spinner when the stuff being loaded is not visible anymore.
 */

public abstract class Updater {

  public static interface UpdaterListener {
    public abstract void onClear();

    public abstract void onRefresh();

    public abstract void onStartUpdating();
  }

  private String tag = "";

  public String getTag() {
    return tag;
  }

  public Updater(String tag, String u, Rect r, LocationItemCollection.TypeIdEnum t,
      LocationItemCollection.AreaTypeIdEnum a) {
    this.tag = tag;
    url = u;
    possibleAreaRect = r;
    itemcoll.typeId = t;
    itemcoll.areaTypeId = a;
  }

  public void setUpdaterListener(UpdaterListener listener) {
    onUpdaterListener = listener;
  }

  private void callRefresh() {
    if (onUpdaterListener != null) {
      long start = 0;
      if (((int) (Math.random() * MyApp.UPDATER_TIMINGS_FREQ)) == 1)
        start = System.currentTimeMillis();
      onUpdaterListener.onRefresh();
      if (start > 0) {
        final long end = System.currentTimeMillis();
        MyApp.trackTiming("Updater", end - start, "callRefresh", getTag());
      }
    }
  }

  private void callClear() {
    if (onUpdaterListener != null) {
      onUpdaterListener.onClear();
    }
  }

  private void callOnStartUpdating() {
    if (onUpdaterListener != null) {
      onUpdaterListener.onStartUpdating();
    }
  }

  public LocationItemCollection itemcoll = new LocationItemCollection();

  protected static final String CLOUD_EU_URL = "http://eu.cdn.sekakuoro.com/";

  protected String url;
  protected float maxItemSpeed = 40.0f * 1000.0f / 3600.0f; // meters per
                                                            // seconds
  protected int minUpdateSpeed = 1500;
  protected int maxUpdateSpeed = 30000;
  protected volatile long lastUpdate = 0;
  protected long updateStartTime = 0;
  private UpdaterListener onUpdaterListener = null;

  // Micro degrees
  private Rect possibleAreaRect = new Rect();

  protected void afterUpdate() {
  }

  private void setLastUpdate() {
    if (!itemcoll.hasItems()) {
      lastUpdate = (long) (System.currentTimeMillis() - getUpdateIntervalMillis() + 5000);
    } else {
      lastUpdate = System.currentTimeMillis();
    }

  }

  public boolean isPossibleAreaRectVisible() {
    Rect mapAreaRect = MyMapActivity.getVisibleMapAreaRect();
    if (mapAreaRect.isEmpty()) {
      return false;
    }
    return Rect.intersects(mapAreaRect, possibleAreaRect);
  }

  public boolean isOld() {
    return millisToNextUpdate() <= 0;
  }

  public long millisSinceLastUpdate() {
    return System.currentTimeMillis() - lastUpdate;
  }

  public long millisToNextUpdate() {
    return getUpdateIntervalMillis() - millisSinceLastUpdate();
  }

  // Return the update interval of items in milliseconds.
  protected long getUpdateIntervalMillis() {

    final float metersPerPixel;

    if (MyMapActivity.mapView != null) {
      final float pixelsPerMeter = MyMapActivity.mapView.getProjection().metersToEquatorPixels(1000.0f) / 1000.0f;
      metersPerPixel = 1.0f / pixelsPerMeter;
    } else {
      metersPerPixel = 5.0f;
    }

    float halfVehicleSizeInMeters = metersPerPixel * 7.0f;
    float updateSpeed = MyApp.getUpdateSpeedMultiplier();
    long delay = (long) ((halfVehicleSizeInMeters / maxItemSpeed * 1000.0f) * updateSpeed);
    if (delay < minUpdateSpeed) {
      delay = minUpdateSpeed;
    } else if (delay > maxUpdateSpeed) {
      delay = maxUpdateSpeed;
    }

    return delay;
  }

  // Parses the stuff loaded from teh internets.
  protected boolean parsePayload(String payload) {
    return false;
  }

  // Loads stuff from the net. Overwritten in StopUpdater.
  protected String getPayload() {
    return MyApp.GetHttpFile(url, itemcoll);
  }

  enum States {
    Enabled, Disabled, Refreshing
  }

  private volatile States currentState = States.Disabled;
  private volatile States nextState = States.Disabled;

  private final WorkerRunnable worker = new WorkerRunnable();

  private volatile ScheduledFuture<?> clearerScheduledFuture = null;

  public synchronized void enable() {
    nextState = States.Enabled;
    processState(false);
  }

  public synchronized void disable() {
    nextState = States.Disabled;
    processState(false);
  }

  public synchronized void refresh(final boolean deepClean) {
    if (currentState == States.Refreshing)
      return;
    nextState = States.Refreshing;
    processState(deepClean);
  }

  private synchronized void processState(final boolean deepClean) {
    if (nextState == States.Enabled) {
      if (currentState == States.Refreshing)
        return;
      currentState = nextState;
      processStateEnabled();
    } else if (nextState == States.Disabled) {
      if (currentState == States.Refreshing)
        return;
      currentState = nextState;
      processStateDisabled();
    } else if (nextState == States.Refreshing) {
      nextState = currentState;
      currentState = States.Refreshing;
      processStateRefresh(deepClean);
    }
  }

  private void processStateEnabled() {
    stopClearer();
    worker.requestStart();
  }

  private void processStateDisabled() {
    worker.requestStop();
    if (currentState != States.Refreshing)
      startClearer();
  }

  private void processStateRefresh(final boolean deepClean) {
    MyApp.executeRunnable(new Runnable() {
      public void run() {
        processStateDisabled();
        worker.waitForThreadToFinish();
        clear(deepClean);
        currentState = nextState;
        processState(false);
      }
    });
  }

  private void startClearer() {

    if (!itemcoll.hasItems() || clearerScheduledFuture != null) {
      return;
    }

    // The stops are cleaned after 15 minutes.
    long clearAtTime = Math.min(millisToNextUpdate() + 2000, 15 * 60 * 1000);

    if (clearAtTime < 2000) {
      clearAtTime = 2000;
    }

    clearerScheduledFuture = MyApp.executeRunnableAfterDelay(new Runnable() {
      public void run() {
        if (!isPossibleAreaRectVisible() && !worker.isScheduledOrRunning() && clearerScheduledFuture != null) {
          clearerScheduledFuture = null;
          refresh(false);
        }
      }
    }, clearAtTime);
  }

  private void stopClearer() {
    if (clearerScheduledFuture != null) {
      clearerScheduledFuture.cancel(false);
      clearerScheduledFuture = null;
    }
  }

  // Removes stuff from the memory and also calls layer's clear()
  protected void clear(final boolean deepClean) {
    itemcoll.clear();
    lastUpdate = 0;
    callClear();
  }

  class WorkerRunnable implements Runnable {
    private volatile boolean shouldStop = true;
    private volatile boolean threadRunning = false;
    private volatile ScheduledFuture<?> scheduledFuture = null;

    public synchronized void requestStart() {

      // If the thread is not running, stop the possible timer for a new timing.
      if (!threadRunning) {
        requestStop();
      } else
        return;

      shouldStop = false;

      final long delay = Math.max(millisToNextUpdate(), 0);
      scheduledFuture = MyApp.executeRunnableAfterDelay(this, delay);

      callOnStartUpdating();
    }

    public synchronized void requestStop() {
      shouldStop = true;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
        scheduledFuture = null;
      }
    }

    public synchronized void waitForThreadToFinish() {
      while (threadRunning) {
        try {
          if (scheduledFuture != null) {
            scheduledFuture.get();
          }
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        } catch (CancellationException e) {
        }
      }
    }

    private boolean isScheduledOrRunning() {
      return threadRunning || scheduledFuture != null;
    }

    public void run() {
      if (currentState != States.Enabled || threadRunning || shouldStop)
        return;

      threadRunning = true;
      try {

        final String payload = getPayload();

        if (shouldStop)
          throw new InterruptedException();

        itemcoll.markAllAsNotUpdated();

        if (shouldStop)
          throw new InterruptedException();

        long start = 0;
        if (((int) (Math.random() * MyApp.UPDATER_TIMINGS_FREQ)) == 1)
          start = System.currentTimeMillis();
        parsePayload(payload);
        if (start > 0) {
          final long end = System.currentTimeMillis();
          MyApp.trackTiming("Updater", end - start, "parsePayload", getTag());
        }

        if (shouldStop) {
          clear(false);
          throw new InterruptedException();
        }

        itemcoll.removeNotUpdated();
        FavoritesCollection.updateItemsFrom(itemcoll);

        callRefresh();
        afterUpdate();

      } catch (InterruptedException e) {
      } finally {
        setLastUpdate();
        threadRunning = false;
        scheduledFuture = null;

        if (currentState == States.Enabled)
          requestStart();
        else
          requestStop();
      }

    }

  }

  public boolean shouldLoad() {
    if (itemcoll.isStop()) {
      // Starts loading stuff before we are at the correct zoom level.
      return MyMapActivity.getZoomLevel() >= itemcoll.getShouldDrawZoomLevel() - 2;
    } else
      return MyMapActivity.getZoomLevel() >= itemcoll.getShouldDrawZoomLevel();
  }

}
