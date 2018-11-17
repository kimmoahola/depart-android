package com.sekakuoro.depart;

import java.security.cert.CertificateException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.sekakuoro.depart.helpers.Utils;
import com.sekakuoro.depart.mapui.MyLocationOverlay;
import com.sekakuoro.depart.stops.HslStops;
import com.sekakuoro.depart.stops.JyStops;
import com.sekakuoro.depart.stops.TreStops;
import com.sekakuoro.depart.stops.TurkuStops;
import com.sekakuoro.depart.stops.VrStops;
import com.sekakuoro.depart.tracker.Hsl;
import com.sekakuoro.depart.tracker.Tre;
import com.sekakuoro.depart.tracker.Turku;
import com.sekakuoro.depart.tracker.Vr;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MyApp extends Application {
  public static final String TAG = "MyApp";

  public static final boolean DEBUG = false;

  public static final int UPDATER_TIMINGS_FREQ = 600;
  public static final int ABSTRACT_DEPARTURE_ACTIVITY_TIMINGS_FREQ = 15;

  public static MyApp instance;

  private static GoogleAnalytics sAnalytics = null;
  private static Tracker sTracker = null;
  public static MyLocationOverlay myLocationOverlay;

  public static UpdaterCollection uc = new UpdaterCollection();

  public static int updateSpeedSelection = 1;
  public static float[] updateSpeedMultipliers = { 0.6666f, 1.0f, 1.6666f };

  public static boolean useGPS = true;
  public static long filterId = -1;

  public static final String STATE_KEY = "Depart";

  private static boolean useAnalytics = true;

  // These store the location of a stop when user wishes to zoom to the selected
  // stop
  public static int zoomToLatE6 = 0;
  public static int zoomToLngE6 = 0;

  private static ThreadFactory threadFactory = new ThreadFactory() {
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r);
      thread.setPriority(Thread.MIN_PRIORITY);
      return thread;
    }
  };

  private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(getDefaultCorePoolSize(),
      threadFactory);

  public static MyApp getApp() {
    return instance;
  }

  @Override
  public void onCreate() {
    instance = this;
    super.onCreate();
    sAnalytics = GoogleAnalytics.getInstance(this);
    Utils.init();

    FavoritesCollection.setContextAndLoad(this.getBaseContext());

    getPrefs();

    final Drawable myLocDrawable = this.getResources().getDrawable(R.drawable.ic_maps_indicator_current_position);
    myLocationOverlay = new MyLocationOverlay(myLocDrawable, this.getBaseContext());

    uc.add(new TreStops());
    uc.add(new HslStops());
    uc.add(new TurkuStops());
    uc.add(new JyStops());
    uc.add(new VrStops());

    uc.add(new Tre());
    uc.add(new Hsl());
    uc.add(new Vr());
    uc.add(new Turku());
  }

  public static Resources getResourcesWrapper() {
    return MyApp.getApp().getResources();
  }

  private static void getPrefs() {

    try {

      SharedPreferences settings = getApp().getSharedPreferences(MyApp.STATE_KEY, MODE_PRIVATE);
      final int appVersionFromPrefs = settings.getInt("appVersion", 0);
      final int versionCodeInt = getApp().getPackageManager().getPackageInfo(getApp().getPackageName(), 0).versionCode;
      if (appVersionFromPrefs == 0) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("appVersion", versionCodeInt);
        editor.apply();
      }

      if (appVersionFromPrefs > 0 && appVersionFromPrefs < versionCodeInt) {
        // Here can be shown an "app updated" dialog etc.
      }

      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApp().getBaseContext());

      final String def = getApp().getResources().getString(R.string.updateSpeedDefaultValue);
      updateSpeedSelection = Integer.parseInt(prefs.getString("updateSpeedPref", def));

      useGPS = prefs.getBoolean("useGPSPref", true);

    } catch (Exception e) {
    }

  }

  public static float getUpdateSpeedMultiplier() {
    return updateSpeedMultipliers[updateSpeedSelection];
  }

  public static void setFilter(long id) {
    filterId = id;
  }

  synchronized private static Tracker getTracker() {
    if (sTracker == null) {
      sTracker = sAnalytics.newTracker("UA-00000000-0");
    }

    return sTracker;
  }

  public static void trackView(final String analyticsPagePath) {
    if (useAnalytics) {
      Tracker t = getTracker();
      t.setScreenName(analyticsPagePath);
      t.send(new HitBuilders.ScreenViewBuilder().build());
    }
  }

  public static void trackEvent(final String category, final String action, final String label, final long value) {
    if (useAnalytics) {
      getTracker().send(new HitBuilders.EventBuilder()
              .setCategory(category)
              .setAction(action)
              .setLabel(label)
              .setValue(value)
              .build());
    }
  }

  public static void trackTiming(final String category, final long interval, final String name, final String label) {
    if (interval <= 1000 * 60 && useAnalytics) {
      getTracker().send(new HitBuilders.TimingBuilder()
              .setCategory(category)
              .setValue(interval)
              .setVariable(name)
              .setLabel(label)
              .build());
    }
  }

  public static String GetHttpFile(final String url, final LocationItem item) {
    long start = 0;
    if (item != null && ((int) (Math.random() * MyApp.UPDATER_TIMINGS_FREQ)) == 1)
      start = System.currentTimeMillis();

    final String payload = GetHttpFile(url);

    if (start > 0) {
      final long end = System.currentTimeMillis();
      MyApp.trackTiming("MyApp", end - start, "GetHttpFile item", item.getAnalyticsPagePath());
    }

    return payload;
  }

  public static String GetHttpFile(final String url, final LocationItemCollection itemcoll) {
    long start = 0;
    if (itemcoll != null && ((int) (Math.random() * MyApp.UPDATER_TIMINGS_FREQ)) == 1)
      start = System.currentTimeMillis();

    final String payload = GetHttpFile(url);

    if (start > 0) {
      final long end = System.currentTimeMillis();
      MyApp.trackTiming("MyApp", end - start, "GetHttpFile itemcoll", itemcoll.getAnalyticsAreaPath());
    }

    return payload;
  }

  public static byte[] GetHttpFileAsBytes(final String url, final LocationItemCollection itemcoll) {
    long start = 0;
    if (itemcoll != null && ((int) (Math.random() * MyApp.UPDATER_TIMINGS_FREQ)) == 1)
      start = System.currentTimeMillis();

    final byte[] payload = GetHttpFileAsBytes(url);

    if (start > 0) {
      final long end = System.currentTimeMillis();
      MyApp.trackTiming("MyApp", end - start, "GetHttpFile itemcoll", itemcoll.getAnalyticsAreaPath());
    }

    return payload;
  }

  private static OkHttpClient getUnsafeOkHttpClient() {
    // Since this is not a very serious app, trusting all certificates etc. is fine.

    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return new java.security.cert.X509Certificate[]{};
            }
          }
      };

      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      OkHttpClient.Builder builder = new OkHttpClient.Builder();
      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
      builder.hostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });

      return builder
          .connectTimeout(30, TimeUnit.SECONDS)
          .writeTimeout(30, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build();
    } catch (Exception e) {
      Log.e("getUnsafeOkHttpClient", "exception", e);
    }
    return null;
  }

  private static ResponseBody GetHttpFileResponse(final String url) {
    try {
      OkHttpClient client = getUnsafeOkHttpClient();

      if (client == null) {
        return null;
      }

      Request request = new Request.Builder().url(url).build();

      Response response = client.newCall(request).execute();
      return response.body();
    } catch (Exception e) {
      Log.e("GetHttpFileResponse", url, e);
    }

    return null;
  }

  private static String GetHttpFile(final String url) {
    Log.e("GetHttpFile", url);
    try {
      ResponseBody body = GetHttpFileResponse(url);
      if (body != null) {
          return body.string();
      }
    } catch (Exception e) {
      Log.e("GetHttpFile exception", url, e);
    }

    Log.e("GetHttpFile no response", url);

    return "";
  }

  public static byte[] GetHttpFileAsBytes(final String url) {
    try {
      ResponseBody body = GetHttpFileResponse(url);
      if (body != null) {
        return body.bytes();
      }
    } catch (Exception e) {
    }

    return new byte[] { 0 };
  }

  public static void executeRunnable(Runnable runnable) {
    executor.execute(runnable);
  }

  public static ScheduledFuture<?> executeRunnableAfterDelay(Runnable runnable, long delay) {
    return executor.schedule(runnable, delay, TimeUnit.MILLISECONDS);
  }

  private static int getDefaultCorePoolSize() {
    return Math.min(Math.max(Utils.N_OF_CORES, 2), 5);
  }

  public static void onResume() {
    executor.setCorePoolSize(getDefaultCorePoolSize());
  }

  public static void onPause() {
    executor.setCorePoolSize(1);
  }

  public static void logErrorToAnalytics(final String error) {
    MyApp.trackEvent("Error", "Error", error, 1);
  }

}
