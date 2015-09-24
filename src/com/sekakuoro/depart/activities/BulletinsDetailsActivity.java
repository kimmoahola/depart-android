package com.sekakuoro.depart.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.bulletins.BulletinsItem;

public class BulletinsDetailsActivity extends Activity {

  BulletinsItem bi = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bulletins_details);

    Bundle extras = this.getIntent().getExtras();
    if (extras == null)
      finish();

    setTitle(extras.getString("feedTitle"));
    getActionBar().setHomeButtonEnabled(true);

    TextView title = (TextView) findViewById(R.id.title);
    TextView description = (TextView) findViewById(R.id.description);
    TextView pubdate = (TextView) findViewById(R.id.pubdate);

    if (title == null || description == null || pubdate == null)
      return;

    bi = (BulletinsItem) extras.getSerializable("item");
    if (bi == null)
      return;

    title.setText(bi.title);

    description.setText(bi.description);
    
    String dateFormat = "dd MMM yyyy HH:mm";
    if (Locale.getDefault().getLanguage().equals("fi"))
      dateFormat = "dd.MM.yyyy HH.mm";

    SimpleDateFormat df = new SimpleDateFormat(dateFormat, Locale.US);
    String time = df.format(new Date(bi.timestamp));
    pubdate.setText(time);
  }
  
  @Override
  protected void onStart() {
    super.onStart();

    MyApp.trackView("Bulletins/" + this.getIntent().getExtras().getString("feedAnalyticsId") + "/item");
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.bulletins_details_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    switch (id) {
      case android.R.id.home:
        startActivity(new Intent(this, MyMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return true;
      case R.id.external:
        if (bi != null && bi.link != null && bi.link.length() > 0) {
          MyApp.trackEvent("BulletinsDetailsActivity", "External", "Click", 1);
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bi.link)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          return true;
        }
        else
          return super.onOptionsItemSelected(item);
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
