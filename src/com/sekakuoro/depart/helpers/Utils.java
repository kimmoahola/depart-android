package com.sekakuoro.depart.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.android.maps.GeoPoint;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

public class Utils {

  public static final int N_OF_CORES = getNumCores();
  private static float dipScale = 1.0f;

  public static void init() {
    dipScale = MyApp.getApp().getResources().getDisplayMetrics().density;
  }

  /**
   * Gets the number of cores available in this device, across all processors.
   * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
   * 
   * @return The number of cores, or 1 if failed to get result
   */
  private static int getNumCores() {
    class CpuFilter implements FileFilter {
      public boolean accept(File pathname) {
        if (Pattern.matches("cpu[0-9]", pathname.getName()))
          return true;
        return false;
      }
    }

    try {
      final File dir = new File("/sys/devices/system/cpu/");
      return dir.listFiles(new CpuFilter()).length;
    } catch (Exception e) {
      return 1;
    }
  }

  public static int dipToPx(final float dip) {
    return (int) (dip * dipScale + 0.5f);
  }

  public static String GetVTDElementText(final VTDNav vn, final String elementName) {
    String text = "";

    try {
      if (vn.toElement(VTDNav.FIRST_CHILD, elementName)) {
        int result = vn.getText();
        if (result != -1)
          text = vn.toString(result);
        vn.toElement(VTDNav.PARENT);
      }
    } catch (NavException e) {
    }

    return text;
  }

  public static class TimeDate {
    final static int SECOND = 1;
    final static int MINUTE = 60 * SECOND;
    final static int HOUR = 60 * MINUTE;
    final static int DAY = 24 * HOUR;
    final static int MONTH = 30 * DAY;

    public static String getRelativeTimeJustNow(long startTime) {
      final long delta = (System.currentTimeMillis() - startTime) / 1000;

      if (delta < 2 * MINUTE) {
        return MyApp.getApp().getResources().getString(R.string.justnow);
      }

      return getRelativeTimeWithDelta(delta);
    }

    public static String getRelativeTime(long startTime) {

      final long delta = (System.currentTimeMillis() - startTime) / 1000;
      return getRelativeTimeWithDelta(delta);
    }

    public static String getRelativeTimeWithDelta(long delta) {
      final String ago = MyApp.getApp().getResources().getString(R.string.ago);

      if (delta < 0)
        return "";

      if (delta < 2 * MINUTE)
        return MyApp.getApp().getResources().getString(R.string.minute) + " " + ago;

      if (delta < 45 * MINUTE)
        return Math.round(delta / 60.0) + " " + MyApp.getApp().getResources().getString(R.string.minutes) + " " + ago;

      if (delta < 90 * MINUTE)
        return MyApp.getApp().getResources().getString(R.string.hour) + " " + ago;

      if (delta < 20 * HOUR)
        return Math.round(delta / 3600.0) + " " + MyApp.getApp().getResources().getString(R.string.hours) + " " + ago;

      if (delta < 48 * HOUR)
        return MyApp.getApp().getResources().getString(R.string.day) + " " + ago;

      if (delta < 30 * DAY)
        return Math.round(delta / (3600.0 * 24.0)) + " " + MyApp.getApp().getResources().getString(R.string.days) + " "
            + ago;

      int months = (int) Math.round(delta / (3600.0 * 24.0 * 30.0));
      return months <= 1 ? MyApp.getApp().getResources().getString(R.string.month) + " " + ago : months + " "
          + MyApp.getApp().getResources().getString(R.string.months) + " " + ago;
    }

  }

  /**
   * Library for some use useful latitude/longitude math
   */
  public static class GeoUtils {
    private static final int EARTH_RADIUS_METERS = 6371000;

    /**
     * Computes the distance in meters between two points on Earth.
     * 
     * @param lat1
     *          Latitude of the first point
     * @param lon1
     *          Longitude of the first point
     * @param lat2
     *          Latitude of the second point
     * @param lon2
     *          Longitude of the second point
     * @return Distance between the two points in meters.
     */
    public static int distance(double lat1, double lon1, double lat2, double lon2) {
      final double lat1Rad = Math.toRadians(lat1);
      final double lat2Rad = Math.toRadians(lat2);
      final double deltaLonRad = Math.toRadians(lon2 - lon1);

      return (int) (Math.acos(Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad)
          * Math.cos(deltaLonRad)) * EARTH_RADIUS_METERS);
    }

    /**
     * Computes the distance in meters between two points on Earth.
     * 
     * @param p1
     *          First point
     * @param p2
     *          Second point
     * @return Distance between the two points in meters.
     */
    public static int distance(GeoPoint p1, GeoPoint p2) {
      final double lat1 = p1.getLatitudeE6() / 1e6;
      final double lon1 = p1.getLongitudeE6() / 1e6;
      final double lat2 = p2.getLatitudeE6() / 1e6;
      final double lon2 = p2.getLongitudeE6() / 1e6;

      return distance(lat1, lon1, lat2, lon2);
    }

    /**
     * Computes the bearing in degrees between two points on Earth.
     * 
     * @param p1
     *          First point
     * @param p2
     *          Second point
     * @return Bearing between the two points in degrees. A value of 0 means due
     *         north and 90 is east. 0 <= value < 360
     */
    public static double bearing(GeoPoint p1, GeoPoint p2) {
      double lat1 = p1.getLatitudeE6() / 1e6;
      double lon1 = p1.getLongitudeE6() / 1e6;
      double lat2 = p2.getLatitudeE6() / 1e6;
      double lon2 = p2.getLongitudeE6() / 1e6;

      return bearing(lat1, lon1, lat2, lon2);
    }

    /**
     * Computes the bearing in degrees between two points on Earth.
     * 
     * @param lat1
     *          Latitude of the first point
     * @param lon1
     *          Longitude of the first point
     * @param lat2
     *          Latitude of the second point
     * @param lon2
     *          Longitude of the second point
     * @return Bearing between the two points in degrees. A value of 0 means due
     *         north and 90 is east. 0 <= value < 360
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
      double lat1Rad = Math.toRadians(lat1);
      double lat2Rad = Math.toRadians(lat2);
      double deltaLonRad = Math.toRadians(lon2 - lon1);

      double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
      double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad);
      return radToBearing(Math.atan2(y, x));
    }

    /**
     * Converts an angle in radians to degrees
     */
    public static double radToBearing(double rad) {
      return (Math.toDegrees(rad) + 360) % 360;
    }
  }

  public static byte[] compress(final byte[] payload) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final GZIPOutputStream gos = new GZIPOutputStream(os);
      gos.write(payload);
      gos.close();
      final byte[] compressed = os.toByteArray();
      os.close();
      return compressed;
    } catch (Exception e) {
      return new byte[] {};
    }
  }

  public static byte[] decompress(final byte[] compressed) {
    try {
      final int BUFFER_SIZE = 8192;
      final ByteArrayInputStream is = new ByteArrayInputStream(compressed);
      final GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      final byte[] data = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = gis.read(data)) != -1)
        buffer.write(data, 0, bytesRead);
      gis.close();
      is.close();
      return buffer.toByteArray();
    } catch (Exception e) {
      return new byte[] {};
    }
  }

}
