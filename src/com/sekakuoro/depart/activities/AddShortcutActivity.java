package com.sekakuoro.depart.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;

import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;

public class AddShortcutActivity extends Activity {

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    final ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher);
    final Intent intent = new Intent();

    final Intent launchIntent = new Intent(this, FavoritesActivity.class);
    final ComponentName comp = new ComponentName("com.sekakuoro.depart", ".TestActivity");
    launchIntent.setComponent(comp);

    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "testi2");
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

    setResult(RESULT_OK, intent);

    MyApp.trackView("AddShortcut");

    finish();

  }

}
