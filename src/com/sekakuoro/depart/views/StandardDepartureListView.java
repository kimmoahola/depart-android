package com.sekakuoro.depart.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.TimetableItem;

public class StandardDepartureListView extends ListView {

  public LocationItemArrayAdapter adapter = null;

  public StandardDepartureListView(Context context, AttributeSet attrs) {
    super(context, attrs);

    final List<TimetableItem> items = new ArrayList<TimetableItem>();
    adapter = new LocationItemArrayAdapter(context, android.R.layout.simple_list_item_1, items);

    setAdapter(adapter);

    setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
        if (view != null && view instanceof StandardDepartureListViewItem)
          ((StandardDepartureListViewItem) view).onClick();
      }
    });
  }

  public void setItem(LocationItem item) {
    adapter.setItem(item);
  }

  public static class LocationItemArrayAdapter extends ArrayAdapter<TimetableItem> {

    private LocationItem item = null;

    public LocationItemArrayAdapter(Context context, int textViewResourceId, List<TimetableItem> items) {
      super(context, textViewResourceId, items);
    }

    public void setItem(LocationItem item) {
      this.item = item;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
      if (convertView != null && convertView instanceof StandardDepartureListViewItem)
        ((StandardDepartureListViewItem) convertView).update(getItem(position));
      else
        convertView = new StandardDepartureListViewItem(getContext(), getItem(position), item);

      return convertView;
    }

  }

}
