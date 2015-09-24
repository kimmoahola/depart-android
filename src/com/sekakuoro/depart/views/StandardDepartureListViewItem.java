package com.sekakuoro.depart.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.R;
import com.sekakuoro.depart.TimetableItem;

public class StandardDepartureListViewItem extends LinearLayout {

  private TextView line = null;
  private TextView title = null;
  private TextView time1 = null;
  private TextView time2 = null;
  private TextView time3 = null;

  private LocationItem item = null;

  public StandardDepartureListViewItem(Context context) {
    super(context);
    inflate();
  }

  public StandardDepartureListViewItem(Context context, TimetableItem timetableItem, LocationItem item) {
    super(context);
    inflate();
    update(timetableItem);
    this.item = item;
  }

  private void inflate() {
    LayoutInflater.from(getContext()).inflate(R.layout.location_item_listview_row, this, true);
    line = (TextView) this.findViewById(R.id.line);
    title = (TextView) this.findViewById(R.id.destination);
    time1 = (TextView) this.findViewById(R.id.time1);
    time2 = (TextView) this.findViewById(R.id.time2);
    time3 = (TextView) this.findViewById(R.id.time3);
  }

  public void update(TimetableItem timetableItem) {
    if (line != null)
      line.setText(timetableItem.line);

    if (title != null)
      title.setText(timetableItem.destination);

    if (time1 != null && time2 != null && time3 != null) {
      if (timetableItem.times.size() == 0) {
        time1.setText("");
        time2.setText("");
        time3.setText("-");
      } else if (timetableItem.times.size() == 1) {
        time1.setText("");
        time2.setText("");
        time3.setText(timetableItem.times.get(0));
      } else if (timetableItem.times.size() == 2) {
        time1.setText("");
        time2.setText(timetableItem.times.get(0));
        time3.setText(timetableItem.times.get(1));
      } else if (timetableItem.times.size() > 2) {
        time1.setText(timetableItem.times.get(0));
        time2.setText(timetableItem.times.get(1));
        time3.setText(timetableItem.times.get(2));
      }
    }

    this.setTag(timetableItem);
  }

  public void onClick() {
    TimetableItem titem = (TimetableItem) this.getTag();
    if (item != null && titem != null) {
      final Uri uri = LocationItem.getBrowserVehicleTimetableUriStatic(item.typeId, item.areaTypeId, titem.title,
          titem.id);
      if (!uri.equals(Uri.EMPTY)) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MyApp.trackEvent("Departure", "External", item.getAnalyticsPagePath(), 1);
        super.getContext().startActivity(intent);
      }
    }
  }

}
