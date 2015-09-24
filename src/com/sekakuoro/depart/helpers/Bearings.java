package com.sekakuoro.depart.helpers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.google.android.maps.GeoPoint;
import com.sekakuoro.depart.R;

public class Bearings {

  private SensorManager sensorManager = null;
  private BearingsListener bearingsListener = null;
  private static final int N_OF_CACHE_BITMAPS = 16; // (int) (360/22.5);
  private static final float DEGREES_PER_BITMAP = (float) (360.0 / N_OF_CACHE_BITMAPS);
  private static final List<Bitmap> bitmapsRotated = new ArrayList<Bitmap>(N_OF_CACHE_BITMAPS);
  private final Handler handler = new Handler();

  public interface BearingsListener {
    void onPhoneBearingChanged(int bearing);
  }

  public Bearings(final Activity a) {
    sensorManager = (SensorManager) a.getSystemService(Context.SENSOR_SERVICE);

    // If cache not created, create it!
    if (bitmapsRotated.isEmpty()) {
      final Bitmap bearingBitmap = BitmapFactory.decodeResource(a.getResources(), R.drawable.bearing);

      if (bearingBitmap != null) {
        final Canvas bearingCanvas = new Canvas();
        final Paint bearingPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        for (int i = 0; i < N_OF_CACHE_BITMAPS; i++) {
          final Bitmap b = Bitmap.createBitmap(bearingBitmap.getWidth(), bearingBitmap.getHeight(),
              Bitmap.Config.ARGB_8888);

          final float direction = DEGREES_PER_BITMAP * i - 180;
          bearingCanvas.setBitmap(b);
          bearingCanvas.rotate(direction, bearingBitmap.getWidth() / 2, bearingBitmap.getHeight() / 2);
          bearingCanvas.drawBitmap(bearingBitmap, 0, 0, bearingPaint);
          bearingCanvas.rotate(-direction, bearingBitmap.getWidth() / 2, bearingBitmap.getHeight() / 2);

          bitmapsRotated.add(b);
        }
      }
    }
  }

  public void start(final BearingsListener bl) {
    handler.removeCallbacksAndMessages(null);

    if (sensorManager == null)
      return;

    bearingsListener = bl;
    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        (int) (1 * 1e6));
    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
        (int) (1 * 1e6));
  }

  public void stop() {
    handler.removeCallbacksAndMessages(null);

    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (sensorManager == null)
          return;
        sensorManager.unregisterListener(sensorEventListener);
      }
    }, 1000);
  }

  /**
   * Return an angle which must be turnd so that the target is on the direction
   * pointed by the device.
   * 
   * @param phoneBearing
   *          Phone bearing: 0 <= phoneBearing < 360
   * @param userGeoPoint
   *          Phone location
   * @param targetGeoPoint
   *          Target location
   * @return -180 <= x < 180
   */
  public static int getDirectionToTarget(final int phoneBearing, final GeoPoint userGeoPoint,
      final GeoPoint targetGeoPoint) {

    final int bearingToTarget = (int) Utils.GeoUtils.bearing(userGeoPoint, targetGeoPoint);

    int direction = bearingToTarget - phoneBearing;
    if (direction >= 180)
      direction -= 360;
    else if (direction < -180)
      direction += 360;

    return direction;
  }

  /**
   * Return the bitmap for a bearing.
   * 
   * @param bearing
   *          -180 <= bearing < 180
   * @return the bitmap for a bearing.
   */
  public static Bitmap getBearingBitmap(final int bearing) {
    final int index = (int) Math.round(((bearing + 180) / DEGREES_PER_BITMAP) % 15.5);

    if (index >= 0 && index < N_OF_CACHE_BITMAPS)
      return bitmapsRotated.get(index);
    else
      return null;
  }

  private SensorEventListener sensorEventListener = new SensorEventListener() {

    private final float[] orientVals = new float[3];
    private final float[] gravity = new float[3];
    private final float[] geomag = new float[3];
    private final float[] inR = new float[16];
    private final float[] I = new float[16];

    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }

    public void onSensorChanged(final SensorEvent event) {
      if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        return;

      switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
          System.arraycopy(event.values, 0, gravity, 0, Math.min(event.values.length, gravity.length));
        break;
        case Sensor.TYPE_MAGNETIC_FIELD:
          System.arraycopy(event.values, 0, geomag, 0, Math.min(event.values.length, geomag.length));
        break;
      }

      if (gravity != null && geomag != null && bearingsListener != null) {
        if (SensorManager.getRotationMatrix(inR, I, gravity, geomag)) {
          SensorManager.getOrientation(inR, orientVals);
          final int bearing = (int) Utils.GeoUtils.radToBearing(orientVals[0]);

          bearingsListener.onPhoneBearingChanged(bearing);
        }
      }
    }

  };

}
