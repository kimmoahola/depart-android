package com.sekakuoro.depart.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.sekakuoro.depart.FavoriteItem;
import com.sekakuoro.depart.FavoritesCollection;
import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.helpers.Bearings;
import com.sekakuoro.depart.helpers.Bearings.BearingsListener;
import com.sekakuoro.depart.helpers.DepartureSelector;
import com.sekakuoro.depart.helpers.DistanceComparator;
import com.sekakuoro.depart.helpers.Utils;

public class FavoritesActivity extends Activity implements OnLongClickListener {

  private FavoritesArrayAdapter adapter = null;
  private Bearings bearings = null;
  private ListView listView = null;
  private TextView noFavoritesTextView = null;
  private boolean isTouching = false;

  @Override
  protected void onCreate(final Bundle b) {
    super.onCreate(b);

    setContentView(R.layout.favorites_activity);
    setTitle(getResources().getString(R.string.favorites));
    getActionBar().setHomeButtonEnabled(true);

    listView = (ListView) findViewById(R.id.listView);
    noFavoritesTextView = (TextView) findViewById(R.id.textView);
    bearings = new Bearings(this);
  }

  @Override
  protected void onStart() {
    super.onStart();

    updateFavoritesList();

    MyApp.trackView("Favorites");
  }

  @Override
  protected void onResume() {
    super.onResume();

    bearings.start(new BearingsListener() {
      public void onPhoneBearingChanged(final int bearing) {
        if (!isTouching)
          updateDistanceAndBearing(bearing);
      }
    });
    MyApp.myLocationOverlay.enableLocationUpdates();
  }

  @Override
  protected void onPause() {
    super.onPause();

    MyApp.myLocationOverlay.disableLocationUpdates();
    bearings.stop();
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

  private void updateDistanceAndBearing(int bearing) {
    if (listView != null) {
      for (int i = 0; i < listView.getChildCount() && !isTouching; i++) {
        final View view = listView.getChildAt(i);
        if (view != null) {
          final FavoriteItem item = (FavoriteItem) view.getTag();
          final GeoPoint userGeoPoint = MyApp.myLocationOverlay.getLastKnownGeoPoint();
          if (item != null && userGeoPoint != null) {
            final ImageView bearingImage = (ImageView) view.findViewById(R.id.bearing);
            if (bearingImage != null) {
              final int direction = Bearings.getDirectionToTarget(bearing, userGeoPoint, item.getGeoPoint());
              bearingImage.setImageBitmap(Bearings.getBearingBitmap(direction));
            }

            final TextView distanceText = (TextView) view.findViewById(R.id.distance);
            if (distanceText != null) {
              distanceText.setText(Utils.GeoUtils.distance(userGeoPoint, item.getGeoPoint()) + " m");
            }
          }
        }
      }
    }
  }

  private void updateFavoritesList() {
    if (listView == null)
      return;

    final List<FavoriteItem> items = new ArrayList<FavoriteItem>(FavoritesCollection.getFavoritesValues());

    if (MyApp.myLocationOverlay.getLastKnownGeoPoint() != null)
      Collections.sort(items, new DistanceComparator(MyApp.myLocationOverlay.getLastKnownGeoPoint()));

    adapter = new FavoritesArrayAdapter(this, android.R.layout.simple_list_item_1, items, this);
    listView.setAdapter(adapter);

    checkIfEmpty();
  }

  private void updateFavoriteView(final View v, final FavoriteItem fav) {
    if (v == null || fav == null)
      return;

    final TextView title = (TextView) v.findViewById(R.id.title);
    title.setText(fav.getUserTitle());

    final GeoPoint userGeoPoint = MyApp.myLocationOverlay.getLastKnownGeoPoint();
    if (userGeoPoint != null) {
      final TextView distance = (TextView) v.findViewById(R.id.distance);
      distance.setText(Utils.GeoUtils.distance(userGeoPoint, fav.getGeoPoint()) + " m");
    }
  }

  private void checkIfEmpty() {
    if (noFavoritesTextView != null && adapter != null) {
      if (adapter.isEmpty())
        noFavoritesTextView.setVisibility(View.VISIBLE);
      else
        noFavoritesTextView.setVisibility(View.GONE);
    }
  }

  public void onRowClick(final View v) {
    if (v == null)
      return;
    final LocationItem item = (LocationItem) v.getTag();
    if (item != null)
      startActivity(DepartureSelector.getLocationItemIntent(getBaseContext(), item));
  }

  public boolean onLongClick(final View v) {
    if (v == null)
      return false;
    final FavoriteItem longPressedItem = (FavoriteItem) v.getTag();

    if (longPressedItem != null) {
      final AlertDialog.Builder dialog = new AlertDialog.Builder(v.getContext());
      dialog.setTitle(longPressedItem.getUserTitle());
      final CharSequence[] clickedItemsChar = { getResources().getString(R.string.delete),
          getResources().getString(R.string.rename) };
      dialog.setItems(clickedItemsChar, new DialogInterface.OnClickListener() {
        public void onClick(final DialogInterface dialog, final int which) {
          if (which == 0) // delete
            removeFavorite(longPressedItem);
          else if (which == 1) // rename
            showRenameDialog(longPressedItem);
        }
      });
      dialog.show();
    }

    return true;
  }

  private void removeFavorite(final FavoriteItem fav) {
    if (fav == null)
      return;
    FavoritesCollection.remove(fav);
    if (adapter != null)
      adapter.remove(fav);
    checkIfEmpty();
    Toast.makeText(this, R.string.unfavorited, Toast.LENGTH_SHORT).show();
    MyApp.trackEvent("FavoritesActivity", "Delete", fav.getAnalyticsPagePath(), 1);
  }

  private void showRenameDialog(final FavoriteItem fav) {
    if (fav == null)
      return;

    final AlertDialog.Builder renameDialogBuilder = new AlertDialog.Builder(this);
    final EditText newName = new EditText(renameDialogBuilder.getContext());
    newName.setText(fav.getUserTitle());
    newName.setSingleLine();

    renameDialogBuilder.setView(newName).setTitle("Rename").setIcon(R.drawable.ic_launcher)
        .setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            MyApp.trackEvent("FavoritesActivity", "Rename", fav.getAnalyticsPagePath(), 1);
            fav.setUserTitle(newName.getText().toString());
            FavoritesCollection.save();
          }
        }).setNeutralButton(R.string.reset, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            fav.resetUserTitle();
            FavoritesCollection.save();
          }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
          }
        });

    newName.requestFocus();
    renameDialogBuilder.show().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
  }

  class FavoritesArrayAdapter extends ArrayAdapter<FavoriteItem> {

    OnLongClickListener onLongClickListener = null;

    public FavoritesArrayAdapter(Context context, int textViewResourceId, List<FavoriteItem> items,
        OnLongClickListener onLongClickListener) {
      super(context, textViewResourceId, items);
      this.onLongClickListener = onLongClickListener;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
      if (convertView == null) {
        final LayoutInflater inflater = getLayoutInflater();
        convertView = inflater.inflate(R.layout.favorites_table_row, parent, false);
        if (convertView == null)
          return null;
      }

      final FavoriteItem fav = getItem(position);
      convertView.setTag(fav);

      updateFavoriteView(convertView, fav);

      if (onLongClickListener != null)
        convertView.setOnLongClickListener(onLongClickListener);

      // Must prevent updating stuff on the map when the user long-presses, or
      // else... the long-press fails somehow.
      convertView.setOnTouchListener(new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
          isTouching = !(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL);
          return false;
        }
      });

      return convertView;
    }

  }

}
