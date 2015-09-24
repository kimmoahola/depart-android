package com.sekakuoro.depart.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.sekakuoro.depart.FavoritesCollection;
import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.TimetableItem;
import com.sekakuoro.depart.activities.AbstractDepartureActivity;
import com.sekakuoro.depart.helpers.Bearings;
import com.sekakuoro.depart.helpers.Utils;
import com.sekakuoro.depart.interfaces.ActivityLifecycleInterface;

public class LocationItemView extends RelativeLayout implements ActivityLifecycleInterface {

  private Bearings bearings = null;
  private LocationItem item = null;

  private StandardDepartureListView listView = null;
  private ImageView bearingImage = null;
  private TextView distanceText = null;
  private ProgressBar progressBar = null;

  public LocationItemView(Context context) {
    super(context);
    initView(context);
  }

  public LocationItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initView(context);
  }

  public LocationItemView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initView(context);
  }

  private void initView(Context context) {
    View.inflate(context, R.layout.location_item_view, this);
    listView = (StandardDepartureListView) findViewById(R.id.listView);
    bearingImage = (ImageView) findViewById(R.id.bearing);
    distanceText = (TextView) findViewById(R.id.distance);
    progressBar = (ProgressBar) findViewById(R.id.progressBar);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    for (int i = 0; i < getChildCount(); i++)
      getChildAt(i).layout(l, t, r, b);
  }

  public void setItem(LocationItem item, Activity activity) {
    this.item = item;
    if (item == null || listView == null)
      return;

    listView.setItem(item);

    final TextView nameView = (TextView) findViewById(R.id.name);
    nameView.setText(item.getPrefixedTitle());

    bearings = new Bearings(activity);

    if (AbstractDepartureActivity.class.isInstance(activity))
      ((AbstractDepartureActivity) activity).activityLifecycleInterfaces.add(this);
  }

  protected void updateDistanceAndBearing(int bearing) {
    final GeoPoint userGeoPoint = MyApp.myLocationOverlay.getLastKnownGeoPoint();
    if (item != null && userGeoPoint != null) {
      if (bearingImage != null) {
        final int direction = Bearings.getDirectionToTarget(bearing, userGeoPoint, item.getGeoPoint());
        bearingImage.setImageBitmap(Bearings.getBearingBitmap(direction));
      }

      if (distanceText != null)
        distanceText.setText(Utils.GeoUtils.distance(userGeoPoint, item.getGeoPoint()) + " m");
    }
  }

  public void updateStarIcon(final MenuItem mItem) {
    if (mItem == null)
      return;

    if (FavoritesCollection.isFavorited(item))
      mItem.setIcon(R.drawable.star_fill);
    else
      mItem.setIcon(R.drawable.star);
  }

  public ArrayAdapter<TimetableItem> getAdapter() {
    if (listView == null)
      return null;
    else
      return listView.adapter;
  }

  @Override
  public void onResume() {
    bearings.start(new Bearings.BearingsListener() {
      public void onPhoneBearingChanged(int bearing) {
        updateDistanceAndBearing(bearing);
      }
    });
    MyApp.myLocationOverlay.enableLocationUpdates();
  }

  @Override
  public void onPause() {
    MyApp.myLocationOverlay.disableLocationUpdates();
    bearings.stop();
  }

  public void onStartLoading() {
    if (progressBar != null)
      progressBar.setVisibility(View.VISIBLE);
  }

  public void onStopLoading() {
    if (progressBar != null)
      progressBar.setVisibility(View.GONE);
  }

}
