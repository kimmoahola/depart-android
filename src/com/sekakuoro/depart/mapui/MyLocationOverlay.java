package com.sekakuoro.depart.mapui;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.FloatMath;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.sekakuoro.depart.activities.MyMapActivity;

public class MyLocationOverlay extends ItemizedOverlay<OverlayItem> implements LocationListener {

  private static final long UPDATE_TIME = 10000;
  private static final int UPDATE_DISTANCE = 0;
  private static final int OLD_THRESHOLD = 30 * 1000;
  private static final float DEGREES_PER_METER = 0.000009013469388f;

  private final Handler handler = new Handler();
  private final Drawable centerDrawable;
  private final int centerDrawableWidthHalf;
  private final int centerDrawableHeightHalf;
  private final LocationManager mLocationManager;

  private GeoPoint lastKnownPoint = null;
  private Location lastKnownLocation = null;
  private OverlayItem overlay = null;
  private GeoPoint accuracyLeftGeo = null;
  private boolean enabled = false;

  private static final Paint accuracyPaintFill;
  private static final Paint accuracyPaint;
  private Point center = new Point();
  private Point left = new Point();

  private static final float[] distanceBetweenResult = new float[1];

  static {
    accuracyPaint = new Paint();
    accuracyPaint.setAntiAlias(true);
    accuracyPaint.setStrokeWidth(2.0f);
    accuracyPaintFill = new Paint(accuracyPaint);
    accuracyPaintFill.setColor(0x106666ff);
    accuracyPaintFill.setStyle(Style.FILL);
    accuracyPaint.setColor(0xaa6666ff);
    accuracyPaint.setStyle(Style.STROKE);
  }

  public MyLocationOverlay(final Drawable defaultMarker, final Context c) {
    super(boundCenter(defaultMarker));
    centerDrawable = defaultMarker;
    centerDrawableWidthHalf = centerDrawable.getIntrinsicWidth() / 2;
    centerDrawableHeightHalf = centerDrawable.getIntrinsicHeight() / 2;
    mLocationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);

    populate();
  }

  @Override
  public void draw(final Canvas canvas, final MapView m, final boolean shadow) {
    if (shadow == false && lastKnownLocation != null && lastKnownPoint != null && MyMapActivity.mapView != null) {
      final Projection projection = MyMapActivity.mapView.getProjection();
      projection.toPixels(lastKnownPoint, center);

      // Accuracy circle
      if (accuracyLeftGeo != null) {
        projection.toPixels(accuracyLeftGeo, left);
        final int radius = center.x - left.x;
        canvas.drawCircle(center.x, center.y, radius, accuracyPaint);
        canvas.drawCircle(center.x, center.y, radius, accuracyPaintFill);
      }

      centerDrawable.setBounds(center.x - centerDrawableWidthHalf, center.y - centerDrawableHeightHalf, center.x
          + centerDrawableWidthHalf, center.y + centerDrawableHeightHalf);
      centerDrawable.draw(canvas);
    }
  }

  public void enableLocationUpdates() {
    handler.removeCallbacksAndMessages(null);

    if (enabled)
      return;

    removeLocationIfOld();

    final List<String> providers = mLocationManager.getProviders(false);
    for (final String p : providers) {
      try {
        if ((!com.sekakuoro.depart.MyApp.useGPS && !p.toLowerCase().equals("gps")) || com.sekakuoro.depart.MyApp.useGPS) {
          mLocationManager.requestLocationUpdates(p, UPDATE_TIME, UPDATE_DISTANCE, this);
          enabled = true;
        }
      } catch (Exception e) {
      }

      try {
        final Location candidate = mLocationManager.getLastKnownLocation(p);
        onLocationChanged(candidate);
      } catch (Exception e) {
      }
    }
  }

  public void disableLocationUpdates() {
    handler.removeCallbacksAndMessages(null);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        reallyDisableLocationUpdates();
      }
    }, 1000);
  }

  public void reallyDisableLocationUpdates() {
    enabled = false;
    mLocationManager.removeUpdates(this);
  }

  public Location getLastKnownLocation() {
    return lastKnownLocation;
  }

  public GeoPoint getLastKnownGeoPoint() {
    return lastKnownPoint;
  }

  private void removeLocationIfOld() {
    if (lastKnownLocation != null) {
      if (lastKnownLocation.getTime() + OLD_THRESHOLD <= System.currentTimeMillis())
        setNewLocation(null);
    }
  }

  public int getLocationAccuracyLatitudeSpan() {
    if (lastKnownLocation == null)
      return 0;

    float accuracy = lastKnownLocation.getAccuracy() * 0.8f;
    if (accuracy < 200.0f)
      accuracy = 200.0f;

    return (int) (accuracy * DEGREES_PER_METER * 1e6f * 2.0f);
  }

  public int getLocationAccuracyLongitudeSpan() {
    if (lastKnownLocation == null)
      return 0;

    float accuracy = lastKnownLocation.getAccuracy() * 0.8f;
    if (accuracy < 200.0f)
      accuracy = 200.0f;

    final float magic = 1.0f / FloatMath.cos((float) Math.toRadians(lastKnownLocation.getLatitude()));
    return (int) (magic * accuracy * DEGREES_PER_METER * 1e6f * 2.0f);
  }

  private void setNewLocation(final Location location) {
    lastKnownLocation = location;
    lastKnownPoint = createGeoPoint(location);

    accuracyLeftGeo = null;

    if (lastKnownLocation != null) {
      final float accuracy = lastKnownLocation.getAccuracy();
      if (accuracy > 12.0) {
        final float latitude = (float) lastKnownLocation.getLatitude();
        final float longitude = (float) lastKnownLocation.getLongitude();
        Location.distanceBetween(latitude, longitude, latitude, longitude + 1.0f, distanceBetweenResult);
        accuracyLeftGeo = new GeoPoint((int) (latitude * 1e6),
            (int) ((longitude - accuracy / distanceBetweenResult[0]) * 1e6));
      }
    }

    replaceOverlay(createCenterOverlay(lastKnownPoint));
  }

  private static boolean isBetterLocation(final Location location, final Location currentBestLocation) {

    if (location == null)
      return false;

    if (location.getTime() + OLD_THRESHOLD > System.currentTimeMillis()) {
      if (currentBestLocation == null
          || (currentBestLocation != null && location.getTime() > currentBestLocation.getTime())) {
        return true;
      }
    }

    return false;
  }

  private static GeoPoint createGeoPoint(final Location loc) {
    if (loc == null)
      return null;

    final int lat = (int) (loc.getLatitude() * 1E6);
    final int lng = (int) (loc.getLongitude() * 1E6);
    return new GeoPoint(lat, lng);
  }

  private static OverlayItem createCenterOverlay(final GeoPoint point) {
    if (point == null) {
      return null;
    }

    return new OverlayItem(point, null, null);
  }

  private void replaceOverlay(final OverlayItem newOverlay) {
    overlay = newOverlay;
    populate();
    if (MyMapActivity.mapView != null) {
      MyMapActivity.mapView.invalidate();
    }
  }

  @Override
  protected OverlayItem createItem(final int i) {
    return overlay;
  }

  @Override
  public int size() {
    if (overlay != null)
      return 1;
    else
      return 0;
  }

  public void onLocationChanged(final Location location) {
    if (isBetterLocation(location, lastKnownLocation)) {
      setNewLocation(location);
    }
  }

  public void onProviderDisabled(String provider) {
  }

  public void onProviderEnabled(String provider) {
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {
  }

}
