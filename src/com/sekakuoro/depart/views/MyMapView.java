package com.sekakuoro.depart.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.helpers.DepartureSelector;
import com.sekakuoro.depart.helpers.DistanceComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class MyMapView extends MapView {

  public interface OnMoveListener {
    public void onMove(MapView mapView, GeoPoint center, boolean stopped);
  }

  private OnMoveListener mOnMoveListener;

  private long lastTouchTime = 0;
  private float lastTouchX = 0.0f;
  private float lastTouchY = 0.0f;
  private boolean doubleTapping = false;
  private boolean singleTapping = false;
  private final Handler onTapHandler = new Handler();

  private float scaledDoubleTapSlop = ViewConfiguration.get(this.getContext()).getScaledDoubleTapSlop() * 0.3f;

  class OnTapRunnable implements Runnable {
    final int x, y;

    OnTapRunnable(int tapScreenCoordsX, int tapScreenCoordsY) {
      x = tapScreenCoordsX;
      y = tapScreenCoordsY;
    }

    public void run() {
      doubleTapping = false;
      handleItemTap(x, y);
      onTapRunnable = null;
    }
  }

  Runnable onTapRunnable = null;

  public MyMapView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setOnMoveListener(OnMoveListener m) {
    mOnMoveListener = m;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {

    if (ev.getAction() == MotionEvent.ACTION_DOWN) {

      if (doubleTapping && System.currentTimeMillis() - lastTouchTime <= ViewConfiguration.getDoubleTapTimeout()) {
        lastTouchTime = 0;
        final float diffX = lastTouchX - ev.getX();
        final float diffY = lastTouchY - ev.getY();
        final float distance = (float) Math.sqrt(diffX * diffX + diffY * diffY);
        if (distance <= scaledDoubleTapSlop) {

          // Double tap

          singleTapping = false;

          if (onTapRunnable != null) {
            onTapHandler.removeCallbacks(onTapRunnable);
            onTapRunnable = null;
          }

          this.getController().zoomInFixing((int) ev.getX(), (int) ev.getY());
        } else
          singleTapping = true;
      } else
        singleTapping = true;

      lastTouchX = ev.getX();
      lastTouchY = ev.getY();
      doubleTapping = true;
    } else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
      final float diffX = lastTouchX - ev.getX();
      final float diffY = lastTouchY - ev.getY();
      final float distance = (float) Math.sqrt(diffX * diffX + diffY * diffY);
      if (distance > scaledDoubleTapSlop) {
        doubleTapping = false;
        if (onTapRunnable != null) {
          onTapHandler.removeCallbacks(onTapRunnable);
          onTapRunnable = null;
        }
      }
    } else if (ev.getAction() == MotionEvent.ACTION_UP) {
      if (doubleTapping) {
        lastTouchTime = System.currentTimeMillis();
        lastTouchX = ev.getX();
        lastTouchY = ev.getY();
        if (onTapRunnable == null && singleTapping) {
          onTapRunnable = new OnTapRunnable((int) ev.getX(), (int) ev.getY());
          onTapHandler.postDelayed(onTapRunnable, ViewConfiguration.getDoubleTapTimeout());
        }
      }
    }

    return super.onTouchEvent(ev);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (mOnMoveListener != null)
      mOnMoveListener.onMove(this, getMapCenter(), false);
  }

  private void handleItemTap(int tapScreenCoordsX, int tapScreenCoordsY) {
    final Projection proj = getProjection();
    final float GESTURE_THRESHOLD_DP = 40.0f; // ~5.5 mm, when density is 1.0
    final float screenWidth = getResources().getDisplayMetrics().widthPixels;
    final float screenHeight = getResources().getDisplayMetrics().heightPixels;

    final float scale = getResources().getDisplayMetrics().density;

    // Convert the dps to pixels, based on density scale
    final int mGestureThreshold = (int) (GESTURE_THRESHOLD_DP * scale + 0.5f);

    final ArrayList<LocationItem> items = MyApp.uc.getLocationItems();
    final int maxItemsToCheck = 30;
    final ArrayList<LocationItem> clickedItems = new ArrayList<LocationItem>(maxItemsToCheck);

    final int len = items.size();
    final Point itemScreenCoords = new Point();
    for (int i = 0; i < len && clickedItems.size() < maxItemsToCheck; ++i) {
      final LocationItem item = items.get(i);

      if (!item.itemcoll.shouldDraw())
        continue;

      proj.toPixels(item.getGeoPoint(), itemScreenCoords);

      // Speed up
      if (itemScreenCoords.x < -40 || itemScreenCoords.x > screenWidth + 40 || itemScreenCoords.y < -40
          || itemScreenCoords.y > screenHeight + 40)
        continue;

      final int xDiff = itemScreenCoords.x - tapScreenCoordsX;
      final int yDiff = itemScreenCoords.y - tapScreenCoordsY;
      final float distance = (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff);

      if (distance <= mGestureThreshold)
        clickedItems.add(item);
    }

    if (clickedItems.size() > 0) {

      // Sort closest item to top of the list
      Collections.sort(clickedItems, new DistanceComparator(proj.fromPixels(tapScreenCoordsX, tapScreenCoordsY)));

      // VR station to the top of the list so that they are easier to click
      ArrayList<LocationItem> clickedItemsVr = new ArrayList<LocationItem>();
      for (Iterator<LocationItem> iterator = clickedItems.iterator(); iterator.hasNext();) {
        LocationItem locationItem = (LocationItem) iterator.next();

        if (locationItem.areaTypeId == LocationItemCollection.AreaTypeIdEnum.Vr) {
          iterator.remove();
          clickedItemsVr.add(locationItem);
        }
      }
      clickedItems.addAll(0, clickedItemsVr);

      if (clickedItems.size() == 1) {
        getContext().startActivity(DepartureSelector.getLocationItemIntent(getContext(), clickedItems.get(0)));
        return;
      }

      final int maxItems = 5;
      while (clickedItems.size() > maxItems) {
        clickedItems.remove(clickedItems.size() - 1);
      }

      final CharSequence[] clickedItemsChar = new CharSequence[clickedItems.size()];
      for (int i = 0; i < clickedItems.size(); ++i) {
        clickedItemsChar[i] = clickedItems.get(i).getPrefixedTitle();
      }

      AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
      dialog.setTitle(getResources().getText(R.string.openTimetables));
      dialog.setItems(clickedItemsChar, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int i) {
          getContext().startActivity(DepartureSelector.getLocationItemIntent(getContext(), clickedItems.get(i)));
        }
      });
      dialog.show();
    }

  }

}
