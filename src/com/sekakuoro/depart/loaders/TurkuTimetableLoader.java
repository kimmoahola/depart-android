package com.sekakuoro.depart.loaders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;

public class TurkuTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private Map<String, TimetableItem> timetableItems = new LinkedHashMap<String, TimetableItem>(); // line,
                                                                                                  // item

  public TurkuTimetableLoader(Context context, LocationItem item) {
    super(context);
    this.item = item;
  }

  @Override
  protected void onStartLoading() {
    if (timetableItems != null && timetableItems.size() > 0)
      deliverResult(new ArrayList<TimetableItem>(timetableItems.values()));
    else
      forceLoad();
  }

  @Override
  public ArrayList<TimetableItem> loadInBackground() {
    timetableItems.clear();

    if (item == null)
      return new ArrayList<TimetableItem>();

    final String payload = MyApp.GetHttpFile(item.getTimetableUri().toString(), item);

    try {
      final JSONObject jsonObject = new JSONObject(payload);

      {
        final JSONArray departuresJsonArray = jsonObject.getJSONArray("departures");
        final int departuresLen = departuresJsonArray.length();
        for (int j = 0; j < departuresLen; ++j) {
          final JSONObject jsonObjectDeparture = departuresJsonArray.getJSONObject(j);
          final String route = jsonObjectDeparture.getString("route");
          String time = jsonObjectDeparture.getString("strTime");

          if (time.equals("0")) {
            time = new String("0 min");
          }

          final TimetableItem existingRoute = timetableItems.get(route);
          if (existingRoute != null)
            existingRoute.times.add(time);
          else {
            TimetableItem titem = new TimetableItem();
            titem.line = route;
            titem.destination = jsonObjectDeparture.getString("destination");
            titem.times.add(time);

            timetableItems.put(route, titem);
          }

        }
      }
    } catch (JSONException e) {
    }

    return new ArrayList<TimetableItem>(timetableItems.values());
  }

  @Override
  public void deliverResult(ArrayList<TimetableItem> newList) { // UI thread
    super.deliverResult(newList);
  }

  @Override
  protected void onStopLoading() {
    cancelLoad();
  }

  @Override
  protected void onReset() {
    super.onReset();
    onStopLoading();
  }

}