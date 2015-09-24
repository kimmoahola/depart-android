package com.sekakuoro.depart.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.bulletins.BulletinsFeed;
import com.sekakuoro.depart.bulletins.BulletinsItem;
import com.sekakuoro.depart.helpers.Utils;

public class BulletinsAreaActivity extends Activity {

  private boolean isPaused = true;

  private final Handler handler = new Handler();
  private static final long timestampUpdateDelay = 3000;

  private ProgressDialog dialog = null;

  private BulletinsFeed bulletinFeed = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bulletins);
    getActionBar().setHomeButtonEnabled(true);

    MyApp.initBulletinFeeds();

    final Bundle b = this.getIntent().getExtras();
    if (b != null) {
      try {
        final Integer integer = (Integer) b.get("index");
        if (integer != null) {
          bulletinFeed = MyApp.bulletinFeeds.get(integer.intValue());
          setTitle(bulletinFeed.title);
        }
      } catch (ClassCastException e) {
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (bulletinFeed != null)
      MyApp.trackView("Bulletins/" + bulletinFeed.analyticsId);
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

    if (dialog != null) {
      dialog.dismiss();
      dialog = null;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        startActivity(new Intent(this, MyMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public void onRowClick(View v) {
    if (bulletinFeed != null) {
      final Intent intent = new Intent(getBaseContext(), BulletinsDetailsActivity.class);
      intent.putExtra("item", (BulletinsItem) v.getTag());
      intent.putExtra("feedTitle", bulletinFeed.title);
      intent.putExtra("feedAnalyticsId", bulletinFeed.analyticsId);
      startActivity(intent);
    }
  }

  private void startTimestampUpdater() {
    if (isPaused)
      return;
    updateTimestamps();
    addBulletinItems();
    handler.postDelayed(new Runnable() {
      public void run() {
        if (isPaused)
          return;
        startTimestampUpdater();
      }
    }, timestampUpdateDelay);
  }

  public void updateTimestamp(View row) {
    if (row == null)
      return;

    final BulletinsItem item = (BulletinsItem) row.getTag();
    if (item == null)
      return;

    final TextView timestamp = (TextView) row.findViewById(R.id.timestamp);
    if (timestamp == null)
      return;

    if (item.timestamp > 0) {
      final String ago = Utils.TimeDate.getRelativeTimeJustNow(item.timestamp);
      timestamp.setText(ago);
    } else
      timestamp.setText("");
  }

  public void updateTimestamps() {
    final ViewGroup vg = (ViewGroup) findViewById(R.id.bulletinsList);
    if (vg == null)
      return;

    for (int i = 0; i < vg.getChildCount(); i++) {
      final View row = vg.getChildAt(i);
      if (row == null)
        continue;

      updateTimestamp(row);
    }
  }

  void addBulletinItems() {
    if (bulletinFeed == null)
      return;

    final ViewGroup vg = (ViewGroup) findViewById(R.id.bulletinsList);
    if (vg == null)
      return;

    if (bulletinFeed.itemList.size() == 0) {
      if (dialog == null)
        dialog = ProgressDialog.show(this, "", getResources().getString(R.string.bulletinsLoading), true, true);
      return;
    }

    if (bulletinFeed.itemList.size() <= vg.getChildCount())
      return;

    final LayoutInflater inflater = getLayoutInflater();

    for (int i = vg.getChildCount(); i < bulletinFeed.itemList.size(); i++) {
      final BulletinsItem item = bulletinFeed.itemList.get(i);
      final View row = inflater.inflate(R.layout.bulletins_table_row, vg, false);
      if (row == null)
        continue;

      final TextView title = (TextView) row.findViewById(R.id.title);
      if (title == null)
        continue;
      title.setText(item.title);

      row.setTag(item);
      vg.addView(row);

      updateTimestamp(row);
    }

    if (dialog != null && vg.getChildCount() > 0) {
      dialog.dismiss();
      dialog = null;
    }
  }

}
