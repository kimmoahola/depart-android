package com.sekakuoro.depart.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;

public class MyPreferenceActivity extends PreferenceActivity {

  private Preference updateSpeedPref;

  @SuppressWarnings("deprecation")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);
    setTitle(getResources().getString(R.string.settings));
    getActionBar().setHomeButtonEnabled(true);

    updateSpeedPref = (Preference) findPreference("updateSpeedPref");
    updateSpeedPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
          MyApp.updateSpeedSelection = Integer.parseInt(newValue.toString());
        } catch (Exception e) {
        }
        updateUpdateSpeedSummary();
        MyApp.trackEvent("Preferences", "Change", preference.getKey(), MyApp.updateSpeedSelection);
        return true;
      }
    });

    final Preference useGPSPref = (Preference) findPreference("useGPSPref");
    useGPSPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
          MyApp.useGPS = Boolean.parseBoolean(newValue.toString());
          MyApp.myLocationOverlay.reallyDisableLocationUpdates();
          MyApp.trackEvent("Preferences", "Change", preference.getKey(), MyApp.useGPS ? 0 : 1);
        } catch (Exception e) {
        }
        return true;
      }
    });

    updateUpdateSpeedSummary();
  }

  @Override
  protected void onStart() {
    super.onStart();

    MyApp.trackView("Preferences");
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

  public void updateUpdateSpeedSummary() {
    final String updateSpeedName = this.getResources().getStringArray(R.array.updateSpeedNames)[MyApp.updateSpeedSelection];
    updateSpeedPref.setSummary(updateSpeedName);
  }

}
