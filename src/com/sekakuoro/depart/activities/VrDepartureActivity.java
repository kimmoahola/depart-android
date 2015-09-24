package com.sekakuoro.depart.activities;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.View;

import com.sekakuoro.depart.R;
import com.sekakuoro.depart.TimetableItem;
import com.sekakuoro.depart.TimetableItem.TypeId;
import com.sekakuoro.depart.helpers.DepartureSelector;
import com.sekakuoro.depart.views.LocationItemView;

public class VrDepartureActivity extends AbstractDepartureActivity {

  private LocationItemView liv1 = null;
  private LocationItemView liv2 = null;
  private int selectedTab = 0;
  private LoaderCallbacks<ArrayList<TimetableItem>> loaderCallbacks = new MyLoaderCallbacks();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.location_item_activity_vr);

    liv1 = (LocationItemView) findViewById(R.id.locationItemView1);
    if (liv1 != null) {
      liv1.setItem(item, this);
      liv1.onStartLoading();
    }

    liv2 = (LocationItemView) findViewById(R.id.locationItemView2);
    if (liv2 != null) {
      liv2.setItem(item, this);
      liv2.onStartLoading();
    }

    ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    actionBar.setDisplayShowTitleEnabled(false);

    if (savedInstanceState != null)
      selectedTab = savedInstanceState.getInt("selectedTab", 0);

    TabListener tl = new TabListener();
    Tab tab = actionBar.newTab().setText(getResources().getString(R.string.departing)).setTabListener(tl);
    actionBar.addTab(tab, (selectedTab == 0));

    tab = actionBar.newTab().setText(getResources().getString(R.string.arriving)).setTabListener(tl);
    actionBar.addTab(tab, (selectedTab == 1));

    getLoaderManager().initLoader(0, null, loaderCallbacks);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt("selectedTab", selectedTab);
  }

  protected void refresh() {
    liv1.onStartLoading();
    liv2.onStartLoading();
    getLoaderManager().restartLoader(0, null, loaderCallbacks);
  }

  public class TabListener implements ActionBar.TabListener {

    public void onTabSelected(Tab tab, FragmentTransaction ft) {

      selectedTab = tab.getPosition();

      View tabView = null;
      if (tab.getPosition() == 0)
        tabView = findViewById(R.id.tab1);
      else if (tab.getPosition() == 1)
        tabView = findViewById(R.id.tab2);
      if (tabView != null)
        tabView.setVisibility(View.VISIBLE);
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
      View tabView = null;
      if (tab.getPosition() == 0)
        tabView = findViewById(R.id.tab1);
      else if (tab.getPosition() == 1)
        tabView = findViewById(R.id.tab2);
      if (tabView != null)
        tabView.setVisibility(View.GONE);
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }
  }

  public class MyLoaderCallbacks implements LoaderManager.LoaderCallbacks<ArrayList<TimetableItem>> {

    public Loader<ArrayList<TimetableItem>> onCreateLoader(final int arg0, final Bundle b) {
      return DepartureSelector.getLoader(getBaseContext(), item);
    }

    public void onLoadFinished(final Loader<ArrayList<TimetableItem>> loader, final ArrayList<TimetableItem> payload) {

      if (liv1 != null && liv2 != null) {
        liv1.onStopLoading();
        liv2.onStopLoading();

        if (payload != null) {
          liv1.getAdapter().clear();
          liv2.getAdapter().clear();

          for (int i = 0; i < payload.size(); i++) {
            if (payload.get(i).typeId == TypeId.Departing)
              liv1.getAdapter().add(payload.get(i));
            else if (payload.get(i).typeId == TypeId.Arriving)
              liv2.getAdapter().add(payload.get(i));
          }
        }
      }

      setRefreshTimer(true);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<TimetableItem>> arg0) {
      liv1.onStopLoading();
      liv2.onStopLoading();
      setRefreshTimer(false);
    }

  }

}
