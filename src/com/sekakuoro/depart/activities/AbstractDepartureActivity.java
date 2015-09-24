package com.sekakuoro.depart.activities;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sekakuoro.depart.FavoritesCollection;
import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.helpers.Bearings;
import com.sekakuoro.depart.interfaces.ActivityLifecycleInterface;

public abstract class AbstractDepartureActivity extends Activity {

  private long start = 0;

  private static final long UPDATE_INTERVAL = 30 * 1000;
  public long lastUpdateTimestamp = 0;
  private final Handler refreshTimerHandler = new Handler();

  // Here gets added those Views and Layouts which wants to know when the
  // activity comes visible and not visible.
  public Set<ActivityLifecycleInterface> activityLifecycleInterfaces = new HashSet<ActivityLifecycleInterface>();

  protected LocationItem item = new LocationItem();
  protected Bearings bearings = null;

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    if (((int) (Math.random() * MyApp.ABSTRACT_DEPARTURE_ACTIVITY_TIMINGS_FREQ)) == 1)
      start = System.currentTimeMillis();

    try {
      final Bundle bundleExtra = this.getIntent().getExtras();
      String stringItem = null;
      if (bundleExtra != null && (stringItem = bundleExtra.getString("item")) != null) {
        item.fromJSONObjectString(stringItem);
        setTitle(item.getUserTitle());
      } else
        throw new ParseException("", 0);
    } catch (Exception e) {
      MyApp.logErrorToAnalytics("AbstractDepartureActivity: item == null");
      item = null;
    }

    bearings = new Bearings(this);

    getActionBar().setHomeButtonEnabled(true);
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (item != null)
      MyApp.trackView(item.getAnalyticsPagePath());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (lastUpdateTimestamp > 0 && System.currentTimeMillis() - lastUpdateTimestamp > UPDATE_INTERVAL)
      refresh();
    for (ActivityLifecycleInterface interf : activityLifecycleInterfaces)
      interf.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    start = 0;
    refreshTimerHandler.removeCallbacksAndMessages(null);
    for (ActivityLifecycleInterface interf : activityLifecycleInterfaces)
      interf.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.location_item_menu, menu);
    updateStarIcon(menu.findItem(R.id.star));
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem mItem) {
    int id = mItem.getItemId();
    switch (id) {
      case android.R.id.home:
        startActivity(new Intent(this, MyMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return true;
      case R.id.star:
        if (item != null) {
          if (FavoritesCollection.toggle(item)) {
            Toast.makeText(this, R.string.favorited, Toast.LENGTH_SHORT).show();
            MyApp.trackEvent("AbstractDepartureActivity", "Favorited", item.getAnalyticsPagePath(), 1);
          } else {
            Toast.makeText(this, R.string.unfavorited, Toast.LENGTH_SHORT).show();
            MyApp.trackEvent("AbstractDepartureActivity", "Unfavorited", item.getAnalyticsPagePath(), 1);
          }
        }
        updateStarIcon(mItem);
        return true;
      case R.id.external:
        if (item != null) {
          MyApp.trackEvent("AbstractDepartureActivity", "External", item.getAnalyticsPagePath(), 1);
          final Uri uri = item.getBrowserTimetableUri();
          if (!uri.equals(Uri.EMPTY))
            startActivity(new Intent(Intent.ACTION_VIEW, uri).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          return true;
        } else
          return super.onOptionsItemSelected(mItem);
      case R.id.refresh:
        if (item != null)
          MyApp.trackEvent("AbstractDepartureActivity", "Refresh", item.getAnalyticsPagePath(), 1);
        refresh();
        return true;
      case R.id.zoomTo:
        if (item != null) {
          MyApp.trackEvent("AbstractDepartureActivity", "ZoomTo", item.getAnalyticsPagePath(), 1);
          MyApp.zoomToLatE6 = item.getGeoPoint().getLatitudeE6();
          MyApp.zoomToLngE6 = item.getGeoPoint().getLongitudeE6();
          final Intent intent = new Intent(this, MyMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
        }
        return true;
      default:
        return super.onOptionsItemSelected(mItem);
    }
  }

  protected abstract void refresh();

  protected void setRefreshTimer(final boolean measureTime) {
    lastUpdateTimestamp = System.currentTimeMillis();
    refreshTimerHandler.removeCallbacksAndMessages(null);
    refreshTimerHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        refresh();
      }
    }, UPDATE_INTERVAL);

    if (measureTime && start > 0 && item != null) {
      final long end = System.currentTimeMillis();
      MyApp.trackTiming("AbstractDepartureActivity", end - start, "onLoadFinished", item.getAnalyticsAreaPath());
    }
    start = 0;
  }

  private void updateStarIcon(final MenuItem mItem) {
    if (mItem == null)
      return;

    if (FavoritesCollection.isFavorited(item))
      mItem.setIcon(R.drawable.star_fill);
    else
      mItem.setIcon(R.drawable.star);
  }

}
