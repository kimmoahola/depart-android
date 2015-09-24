package com.sekakuoro.depart.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.bulletins.BulletinsFeed;
import com.sekakuoro.depart.bulletins.BulletinsItem;
import com.sekakuoro.depart.bulletins.BulletinsLoader;
import com.sekakuoro.depart.helpers.Utils;

public class BulletinsActivity extends Activity {

  private boolean isPaused = true;

  private final Handler handler = new Handler();
  private static final long timestampUpdateDelay = 10000;
  private long starts[];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.bulletins);
    setTitle(getResources().getString(R.string.bulletins));
    getActionBar().setHomeButtonEnabled(true);

    MyApp.initBulletinFeeds();

    final String lang = MyApp.getRssLang();
    for (BulletinsFeed feed : MyApp.bulletinFeeds) {
      if (feed.lang.length() > 0 && !feed.lang.equals(lang)) {
        feed.lang = lang;
        feed.clear();
      }

      // If the system language has changed...
      feed.title = getResources().getString(feed.titleResourceId);
    }

    final LayoutInflater inflater = getLayoutInflater();
    final ViewGroup vg = (ViewGroup) findViewById(R.id.bulletinsList);
    if (vg == null)
      return;

    for (int i = 0; i < MyApp.bulletinFeeds.size(); i++) {
      final View row = inflater.inflate(R.layout.bulletins_table_row, vg, false);
      if (row == null)
        return;
      row.setTag(Integer.valueOf(i));
      vg.addView(row);

      updateFeedRow(row);
    }

    startLoaders();
  }

  @Override
  protected void onStart() {
    super.onStart();

    MyApp.trackView("Bulletins");
  }

  @Override
  protected void onResume() {
    super.onResume();
    isPaused = false;

    startTimestampUpdater();
  }

  @Override
  protected void onPause() {
    super.onPause();
    isPaused = true;
    for (int i = 0; i < MyApp.bulletinFeeds.size(); i++)
      starts[i] = 0;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.bulletins_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int id = item.getItemId();
    switch (id) {
      case R.id.refresh:
        for (int i = 0; i < MyApp.bulletinFeeds.size(); i++)
          MyApp.bulletinFeeds.get(i).clear();
        updateTimestamps();
        startLoaders();
        MyApp.trackEvent("BulletinsActivity", "Refresh", "Click", 1);
        return true;
      case android.R.id.home:
        startActivity(new Intent(this, MyMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void onRowClick(final View v) {
    final Intent intent = new Intent(getBaseContext(), BulletinsAreaActivity.class);
    intent.putExtra("index", (Integer) v.getTag());
    startActivity(intent);
  }

  private void startTimestampUpdater() {
    if (isPaused)
      return;
    updateTimestamps();
    handler.postDelayed(new Runnable() {
      public void run() {
        if (isPaused)
          return;
        startTimestampUpdater();
      }
    }, timestampUpdateDelay);
  }

  private void startLoaders() {
    starts = new long[MyApp.bulletinFeeds.size()];

    for (int i = 0; i < MyApp.bulletinFeeds.size(); i++) {

      if (((int) (Math.random() * MyApp.BULLETINS_TIMINGS_FREQ)) == 1)
        starts[i] = System.currentTimeMillis();
      else
        starts[i] = 0;

      getLoaderManager().restartLoader(i, null, new LoaderCallbacks(this));
    }

  }

  public void updateFeedRow(final View row) {
    if (row == null)
      return;

    final BulletinsFeed item = MyApp.bulletinFeeds.get((Integer) row.getTag());
    if (item == null)
      return;

    final TextView title = (TextView) row.findViewById(R.id.title);
    if (title == null)
      return;

    title.setText(item.title);

    updateTimestamp(row);
  }

  public void updateTimestamp(final View row) {
    if (row == null)
      return;

    final BulletinsFeed item = MyApp.bulletinFeeds.get((Integer) row.getTag());
    if (item == null)
      return;

    final TextView timestamp = (TextView) row.findViewById(R.id.timestamp);
    if (timestamp == null)
      return;

    if (item.latestItemTimestamp > 0) {
      final String ago = Utils.TimeDate.getRelativeTimeJustNow(item.latestItemTimestamp);
      timestamp.setText(ago);
    } else {
      timestamp.setText(getResources().getString(R.string.bulletinUpdating));
    }
  }

  public void updateTimestamps() {
    final ViewGroup vg = (ViewGroup) findViewById(R.id.bulletinsList);
    if (vg == null)
      return;

    for (int i = 0; i < vg.getChildCount(); i++) {
      final View row = vg.getChildAt(i);
      if (row == null)
        return;

      updateTimestamp(row);
    }
  }

  private class LoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<BulletinsItem>> {

    private final Context context;

    LoaderCallbacks(Context c) {
      context = c;
    }

    public Loader<ArrayList<BulletinsItem>> onCreateLoader(int id, Bundle b) {
      return new BulletinsLoader(context, id);
    }

    public void onLoadFinished(final Loader<ArrayList<BulletinsItem>> loader, final ArrayList<BulletinsItem> payload) {

      if (payload == null) {
        return;
      }

      final BulletinsLoader bl = (BulletinsLoader) loader;
      if (bl == null)
        return;

      final ViewGroup vg = (ViewGroup) findViewById(R.id.bulletinsList);
      if (vg == null)
        return;

      final View row = vg.getChildAt(bl.index);

      updateFeedRow(row);

      if (starts[bl.index] > 0) {
        final long end = System.currentTimeMillis();

        MyApp.trackTiming("BulletinsActivity", end - starts[bl.index], "onLoadFinished", "Bulletins/"
            + MyApp.bulletinFeeds.get(bl.index).analyticsId);

        starts[bl.index] = 0;
      }
    }

    public void onLoaderReset(final Loader<ArrayList<BulletinsItem>> arg0) {
    }
  }
}
