package com.sekakuoro.depart.mapui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.FloatMath;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.Updater;
import com.sekakuoro.depart.Updater.UpdaterListener;
import com.sekakuoro.depart.activities.MyMapActivity;

public class MyOverlay extends Overlay implements UpdaterListener {

  protected Point itemScreenCoords = new Point();
  private Rect screenRectPix = new Rect();

  public Rect areaRect = new Rect(); // micro degrees

  private Activity activity;
  protected Updater updater;

  public MyOverlay(Activity a) {
    super();

    activity = a;
  }

  public MyOverlay(Activity a, Updater u) {
    super();

    activity = a;
    setUpdater(u);
  }

  public void setUpdater(Updater u) {
    updater = u;
  }

  // Updater calls this. Loops through updater.items and creates contents of
  // mOverlays.
  public void onRefresh() {

    if (updater.itemcoll.hasItems()) {
      calculateAreaRect();
      calculateSubAreaRect();
      if (MyMapActivity.mapView != null && Rect.intersects(MyMapActivity.getVisibleMapAreaRect(), areaRect)) {
        MyMapActivity.mapView.postInvalidate();
      }
    }

  }

  public void onClear() {

    updater.itemcoll.updatingLock.lock();
    try {
      areaRect.setEmpty();
      clearSubArea();
    } finally {
      updater.itemcoll.updatingLock.unlock();
    }

  }

  public void onStartUpdating() {
    if (!subAreasInitialized()) {
      onRefresh();
    }
  }

  @Override
  public void draw(Canvas canvas, MapView mapView, boolean shadow) {
    // This is called multiple times per second.

    // Don't draw shadow
    if (shadow == true) {
      return;
    }

    updater.itemcoll.updatingLock.lock();

    try {

      if (!updater.itemcoll.shouldDraw())
        return;

      final Rect screenRect = MyMapActivity.getVisibleMapAreaRect();

      if (Rect.intersects(screenRect, areaRect) && subAreaRects != null) {

        // Let's guess
        final int bW2 = 10;
        final int bH2 = 10;

        // As pixels
        screenRectPix.set(-bW2, -bH2, mapView.getContext().getResources().getDisplayMetrics().widthPixels + bW2,
            mapView.getContext().getResources().getDisplayMetrics().heightPixels + bH2);

        final Projection proj = mapView.getProjection();

        for (int i = 0; i < subAreaRectItems.size(); ++i) {
          final List<LocationItem> overlayItemsInSubRect = subAreaRectItems.get(i);

          if (overlayItemsInSubRect != null && overlayItemsInSubRect.size() > 0 && i < subAreaRects.length) {
            if (Rect.intersects(screenRect, subAreaRects[i])) {

              for (final LocationItem item : overlayItemsInSubRect) {

                // Slow operation
                proj.toPixels(item.getGeoPoint(), itemScreenCoords);

                final Bitmap b = item.getBitmap();
                if (b != null) {
                  final int bitmapWidth = b.getWidth() / 2;
                  final int bitmapHeight = b.getHeight() / 2;
                  canvas.drawBitmap(b, itemScreenCoords.x - bitmapWidth, itemScreenCoords.y - bitmapHeight, null);
                }
              }

            }
          }
        }
      }

    } finally {
      updater.itemcoll.updatingLock.unlock();
    }

  }

  private void calculateAreaRect() {
    updater.itemcoll.updatingLock.lock();
    try {
      if (updater.itemcoll.hasItems()) {
        areaRect.set(updater.itemcoll.values().iterator().next().getGeoPoint().getLongitudeE6(), updater.itemcoll
            .values().iterator().next().getGeoPoint().getLatitudeE6(), updater.itemcoll.values().iterator().next()
            .getGeoPoint().getLongitudeE6() + 1, updater.itemcoll.values().iterator().next().getGeoPoint()
            .getLatitudeE6() + 1);

        for (LocationItem item : updater.itemcoll.values()) {
          areaRect.union(item.getGeoPoint().getLongitudeE6(), item.getGeoPoint().getLatitudeE6(), item.getGeoPoint()
              .getLongitudeE6() + 1, item.getGeoPoint().getLatitudeE6() + 1);
        }

      } else {
        areaRect.setEmpty();
      }

    } finally {
      updater.itemcoll.updatingLock.unlock();
    }

  }

  private int subAreaSize = 0;
  private static final int itemsPerSubArea = 20;
  private Rect subAreaRects[] = null; // micro degrees
  private List<List<LocationItem>> subAreaRectItems = new ArrayList<List<LocationItem>>();

  private boolean subAreasInitialized() {
    return subAreaSize > 1;
  }

  private void initSubArea() {
    updater.itemcoll.updatingLock.lock();
    try {
      if (!updater.itemcoll.hasItems()) {
        clearSubArea();
        return;
      }

      final int oldSubAreaSize = subAreaSize;

      subAreaSize = (int) FloatMath.floor(FloatMath.sqrt(updater.itemcoll.size() / itemsPerSubArea));
      if (subAreaSize < 2) {
        subAreaSize = 2;
      }

      if (oldSubAreaSize != subAreaSize) {
        subAreaRects = new Rect[subAreaSize * subAreaSize];
        subAreaRectItems.clear();
        for (int i = 0; i < subAreaSize * subAreaSize; ++i) {
          List<LocationItem> innerList = new ArrayList<LocationItem>();
          subAreaRectItems.add(innerList);
        }
      } else {
        for (int i = 0; i < subAreaSize * subAreaSize; ++i) {
          subAreaRectItems.get(i).clear();
        }
      }
    } finally {
      updater.itemcoll.updatingLock.unlock();
    }
  }

  private void clearSubArea() {
    subAreaSize = 0;
    subAreaRects = null;
    subAreaRectItems.clear();
  }

  private void calculateSubAreaRect() {

    initSubArea();

    // Pick item in order but skip those which has an item about 1-2 millimeters
    // from it.

    if (updater.itemcoll.hasItems()) {

      final List<LocationItem> mOverlaysStillToBeAdded;

      updater.itemcoll.updatingLock.lock();
      try {
        mOverlaysStillToBeAdded = new LinkedList<LocationItem>(updater.itemcoll.values());
      } finally {
        updater.itemcoll.updatingLock.unlock();
      }

      final int subAreaWidth = (int) FloatMath.ceil((float) areaRect.width() / subAreaSize) + 1;
      final int subAreaHeight = (int) FloatMath.ceil((float) areaRect.height() / subAreaSize) + 1;

      for (int x = 0; x < subAreaSize && mOverlaysStillToBeAdded.size() > 0; ++x) {
        for (int y = 0; y < subAreaSize && mOverlaysStillToBeAdded.size() > 0; ++y) {
          final int pos = x * subAreaSize + y;

          final Rect r = new Rect();
          r.left = subAreaWidth * x + areaRect.left;
          r.right = r.left + subAreaWidth;
          r.top = subAreaHeight * y + areaRect.top;
          r.bottom = r.top + subAreaHeight;

          updater.itemcoll.updatingLock.lock();
          try {
            subAreaRects[pos] = r;

            // Remove item when it's added to an area.
            for (Iterator<LocationItem> it = mOverlaysStillToBeAdded.iterator(); it.hasNext();) {

              final LocationItem item = it.next();
              if (r.contains(item.getGeoPoint().getLongitudeE6(), item.getGeoPoint().getLatitudeE6())) {
                subAreaRectItems.get(pos).add(item);
                it.remove();
              }
            }
          } finally {
            updater.itemcoll.updatingLock.unlock();
          }

        }
      }

    }

  }

  @Override
  public boolean onTap(GeoPoint p, MapView mapView) {
    // Stops the touch as it's handled in MyMapView.
    return true;
  }

}
