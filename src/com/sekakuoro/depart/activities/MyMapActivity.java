package com.sekakuoro.depart.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.Updater;
import com.sekakuoro.depart.mapui.MyOverlay;
import com.sekakuoro.depart.views.MyMapView;
import com.sekakuoro.depart.views.MyMapView.OnMoveListener;

import java.util.ArrayList;
import java.util.List;

public class MyMapActivity extends MapActivity {

  private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 123;
  public static boolean isPaused = true;
  public int startCount = 1;

  private SharedPreferences settings = null;

  public static MyMapView mapView;
  private static MapController mapController;
  private boolean satellite = false;
  private List<MyOverlay> overlays = new ArrayList<MyOverlay>(MyApp.uc.updaters.size());

  private static final int PROGRESS_INTERVAL = 1000;
  private Handler handlerProgress = new Handler();
  private ProgressBar progressBar = null;

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    settings = getSharedPreferences(MyApp.STATE_KEY, 0);

    setContentView(R.layout.main);
    setTitle(getResources().getString(R.string.map));

    initMap();
  }

  @Override
  protected void onStart() {
    super.onStart();

    MyApp.trackView("Map");
  }

  @Override
  protected void onResume() {
    super.onResume();

    isPaused = false;

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
          Manifest.permission.ACCESS_FINE_LOCATION)) {

      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
      }
    } else {
      MyApp.myLocationOverlay.enableLocationUpdates();
    }

    startCount = settings.getInt("startCount", 1);

    MyApp.uc.enableAll();

    handlerProgress.postDelayed(progressRunnable, PROGRESS_INTERVAL);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // permission was granted
          MyApp.myLocationOverlay.enableLocationUpdates();
        }
        break;
      }
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    isPaused = true;

    MyApp.uc.disableAll();
    MyApp.myLocationOverlay.disableLocationUpdates();

    final SharedPreferences.Editor editor = settings.edit();
    editor.putInt("startCount", startCount + 1);
    if (mapView != null) {
      editor.putInt("map.center.lat", mapView.getMapCenter().getLatitudeE6());
      editor.putInt("map.center.lng", mapView.getMapCenter().getLongitudeE6());
      editor.putInt("map.zoom", mapView.getZoomLevel());
    }
    editor.apply();
  }

  @Override
  protected void onStop() {
    super.onStop();

    // We send an empty event so we get more accurate time-on-page info.
    MyApp.trackEvent("", "", "", 0);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    for (int i = 0; i < MyApp.uc.updaters.size(); ++i) {
      MyApp.uc.updaters.get(i).setUpdaterListener(null);
    }

    mapView = null;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    updateSatelliteButton(menu.findItem(R.id.satellite));
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {

      case R.id.favorites:
        startActivity(new Intent(getBaseContext(), FavoritesActivity.class));
        return true;

      case R.id.myLocationId:
        focusMapToUser();
        MyApp.trackEvent("Map", "My location", "Click", 1);
        return true;

      case R.id.satellite:
        satellite = !satellite;
        if (mapView != null)
          mapView.setSatellite(satellite);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("map.satellite", satellite);
        editor.apply();
        MyApp.trackEvent("Map", "Satellite", "Click", satellite ? 1 : 0);
        return true;

      case R.id.bulletins:
        startActivity(new Intent(getBaseContext(), BulletinsActivity.class));
        return true;

      case R.id.refresh:
        handlerProgress.postDelayed(progressRunnable, 100);
        MyApp.trackEvent("Map", "Refresh", "Click", 1);
        MyApp.uc.refreshAll();
        return true;

      case R.id.settings:
        startActivity(new Intent(getBaseContext(), MyPreferenceActivity.class));
        return true;

      case R.id.about:
        startActivity(new Intent(getBaseContext(), AboutActivity.class));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void initMap() {
    final int latDefault = (int) (61.5 * 1E6);
    final int lngDefault = (int) (24.5 * 1E6);
    final int zoomDefault = 7;

    mapView = (MyMapView) findViewById(R.id.mapview);
    mapView.setBuiltInZoomControls(true);
    mapController = mapView.getController();

    int lat, lng, zoom;
    if (MyApp.zoomToLatE6 != 0 && MyApp.zoomToLngE6 != 0) {
      // We arrive here after the user wants to zoom to the stop on the stop
      // view.
      lat = MyApp.zoomToLatE6;
      lng = MyApp.zoomToLngE6;
      zoom = 17;
      MyApp.zoomToLatE6 = MyApp.zoomToLngE6 = 0;
    } else {
      lat = settings.getInt("map.center.lat", latDefault);
      lng = settings.getInt("map.center.lng", lngDefault);
      zoom = settings.getInt("map.zoom", zoomDefault);
    }
    satellite = settings.getBoolean("map.satellite", false);

    mapController.setCenter(new GeoPoint(lat, lng));
    mapController.setZoom(zoom);
    mapView.setSatellite(satellite);
    mapView.setOnMoveListener(new OnMoveListener() {
      private long lastCall = 0;

      public void onMove(MapView mapView, GeoPoint center, boolean stopped) {
        if (System.currentTimeMillis() - lastCall > 400) {
          lastCall = System.currentTimeMillis();
          onMapMove();
        }
      }
    });

    for (int i = 0; i < MyApp.uc.updaters.size(); ++i) {
      MyOverlay mo = new MyOverlay(this, MyApp.uc.updaters.get(i));
      MyApp.uc.updaters.get(i).setUpdaterListener(mo);
      overlays.add(mo);
    }

    mapView.getOverlays().addAll(overlays);

    mapView.getOverlays().add(MyApp.myLocationOverlay);
  }

  private void onMapMove() {
    MyApp.uc.onMapMove();
    handlerProgress.postDelayed(progressRunnable, PROGRESS_INTERVAL / 4);
  }

  public void focusMapToUser() {
    GeoPoint myLocation = MyApp.myLocationOverlay.getLastKnownGeoPoint();
    if (myLocation != null) {
      mapController.animateTo(myLocation);
      int latSpan = MyApp.myLocationOverlay.getLocationAccuracyLatitudeSpan();
      int lngSpan = MyApp.myLocationOverlay.getLocationAccuracyLongitudeSpan();
      mapController.zoomToSpan(latSpan, lngSpan);
    } else {
      Toast.makeText(this, getResources().getString(R.string.myLocationNA), Toast.LENGTH_SHORT).show();
    }
  }

  public void updateSatelliteButton(final MenuItem mi) {
    if (mi == null)
      return;

    if (satellite)
      mi.setTitle(getResources().getString(R.string.satelliteOff));
    else
      mi.setTitle(getResources().getString(R.string.satellite));
  }

  // Return the visible are on the map in Rect format which has coordinates in
  // micro degrees.
  // If no map or paused, then empty Rect.
  public static Rect getVisibleMapAreaRect() {
    if (mapView == null || isPaused) {
      return new Rect();
    }

    final Projection proj = mapView.getProjection();

    final GeoPoint topLeft = proj.fromPixels(0, 0); // Always slow operation

    // Always slow operation
    final GeoPoint bottomRight = proj.fromPixels(mapView.getContext().getResources().getDisplayMetrics().widthPixels,
        mapView.getContext().getResources().getDisplayMetrics().heightPixels);

    return new Rect(topLeft.getLongitudeE6(), bottomRight.getLatitudeE6(), bottomRight.getLongitudeE6(),
        topLeft.getLatitudeE6());
  }

  public static int getZoomLevel() {
    if (mapView != null) {
      return mapView.getZoomLevel();
    } else {
      return 10;
    }
  }

  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  private final Runnable progressRunnable = new Runnable() {
    public synchronized void run() {
      handlerProgress.removeCallbacksAndMessages(null);

      if (progressBar == null) {
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        if (progressBar == null)
          return;
      }

      if (isPaused) {
        progressBar.setVisibility(View.GONE);
        return;
      }

      final boolean visible = (progressBar.getVisibility() == View.VISIBLE);

      for (Updater updater : MyApp.uc.updaters) {
        if ((!updater.itemcoll.hasItems() || updater.millisToNextUpdate() < -5000)
            && updater.isPossibleAreaRectVisible() && updater.itemcoll.shouldDraw()) {
          if (!visible) {
            progressBar.setVisibility(View.VISIBLE);
          }
          handlerProgress.postDelayed(progressRunnable, PROGRESS_INTERVAL);
          return;
        }
      }

      progressBar.setVisibility(View.GONE);
      handlerProgress.postDelayed(progressRunnable, PROGRESS_INTERVAL * 2);
    }
  };

}