package com.sekakuoro.depart.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;

public class AboutActivity extends Activity {

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    setContentView(R.layout.about);
    setTitle(getResources().getString(R.string.aboutHelp));
    getActionBar().setHomeButtonEnabled(true);
    
    final WebView webView = (WebView) findViewById(R.id.webView);
    webView.loadUrl(getString(R.string.aboutUrl));
  }

  @Override
  protected void onStart() {
    super.onStart();
    MyApp.trackView("About");
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

}
