package com.sekakuoro.depart.activities;

import java.util.ArrayList;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;

import com.sekakuoro.depart.TimetableItem;
import com.sekakuoro.depart.helpers.DepartureSelector;
import com.sekakuoro.depart.views.LocationItemView;

public class StandardDepartureActivity extends AbstractDepartureActivity {

  private LocationItemView liv = null;
  private LoaderCallbacks<ArrayList<TimetableItem>> loaderCallbacks = new MyLoaderCallbacks();

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    liv = new LocationItemView(this);
    setContentView(liv, new LocationItemView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    liv.setItem(item, this);
    liv.onStartLoading();
    getLoaderManager().initLoader(0, null, loaderCallbacks);
  }

  @Override
  protected void refresh() {
    if (liv != null) {
      liv.onStartLoading();
      getLoaderManager().restartLoader(0, null, loaderCallbacks);
    }
  }

  public class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<TimetableItem>> {

    public MyLoaderCallbacks() {
    }

    public Loader<ArrayList<TimetableItem>> onCreateLoader(int arg0, Bundle b) {
      liv.onStartLoading();

      return DepartureSelector.getLoader(getBaseContext(), item);
    }

    public void onLoadFinished(Loader<ArrayList<TimetableItem>> loader, ArrayList<TimetableItem> payload) {
      liv.onStopLoading();

      if (payload == null || payload.isEmpty()) {
        // If loading stop info fails, don't delete already loaded stuff if they
        // are fresh.
        if (lastUpdateTimestamp > 0 && System.currentTimeMillis() - lastUpdateTimestamp > 1000 * 60)
          liv.getAdapter().clear();
        return;
      }

      liv.getAdapter().clear();
      liv.getAdapter().addAll(payload);

      setRefreshTimer(true);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<TimetableItem>> arg0) {
      liv.onStopLoading();
      setRefreshTimer(false);
    }

  }

}
