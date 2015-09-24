package com.sekakuoro.depart.loaders;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;

public class OuluTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private ArrayList<TimetableItem> timetableItems = new ArrayList<TimetableItem>();

  public OuluTimetableLoader(Context context, LocationItem item) {
    super(context);
    this.item = item;
  }


  @Override
  protected void onStartLoading() {
    if (timetableItems != null && timetableItems.size() > 0)
      deliverResult(timetableItems);
    else
      forceLoad();
  }


  public class LineComparator implements Comparator<TimetableItem> {
    @Override
    public int compare(TimetableItem o1, TimetableItem o2) {
      return o1.line.compareTo(o2.line);
    }
  }


  @Override
  public ArrayList<TimetableItem> loadInBackground() {

    timetableItems.clear();

    if (item == null)
      return timetableItems;

    final String payload = MyApp.GetHttpFile(item.getTimetableUri().toString(), item);
    final Map<String, String> linesName = new HashMap<String, String>(); // internal id, line name
    final Map<String, String> linesDest = new HashMap<String, String>(); // internal id, destination
    final Map<String, ArrayList<String>> departures = new HashMap<String, ArrayList<String>>(); // internal id, times

    try {
      final JSONObject jsonObject = new JSONObject(payload);

      final JSONArray routesJsonArray = jsonObject.getJSONArray("routes");
      final int routesLen = routesJsonArray.length();
      for (int j = 0; j < routesLen; ++j) {
        final JSONObject jsonRouteObject = routesJsonArray.getJSONObject(j);
        final String routeId = jsonRouteObject.getJSONObject("id").getString("id");
        linesName.put(routeId, jsonRouteObject.getString("shortName"));
        linesDest.put(routeId, jsonRouteObject.getString("longName"));
      }

      SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
      format.setTimeZone(TimeZone.getDefault());

      final JSONArray stopTimesJsonArray = jsonObject.getJSONArray("stopTimes");
      final int stopTimesLen = stopTimesJsonArray.length();
      for (int j = 0; j < stopTimesLen; ++j) {
        final JSONObject jsonStopTimeObject = stopTimesJsonArray.getJSONObject(j);

        if (!jsonStopTimeObject.getString("phase").equals("departure"))
          continue;

        Date dateTime = new Date(jsonStopTimeObject.getLong("time") * 1000);
        final String time = format.format(dateTime);

        final String routeId = jsonStopTimeObject.getJSONObject("trip").getJSONObject("route").getJSONObject("id").getString("id");

        final ArrayList<String> timesOld = departures.get(routeId);
        if (timesOld != null)
          timesOld.add(time);
        else {
          final ArrayList<String> timesNew = new ArrayList<String>();
          timesNew.add(time);
          departures.put(routeId, timesNew);
        }
      }

    } catch (JSONException e) {}

    for (Iterator<Entry<String, ArrayList<String>>> iterator = departures.entrySet().iterator(); iterator.hasNext();) {
      try {
        final Entry<String, ArrayList<String>> dep = (Entry<String, ArrayList<String>>) iterator.next();

        final TimetableItem titem = new TimetableItem();

        titem.line = linesName.get(dep.getKey());
        titem.destination = linesDest.get(dep.getKey());
        titem.title = titem.line;
        titem.id = dep.getKey();
        titem.times.addAll(dep.getValue());
        timetableItems.add(titem);
      }
      catch (IndexOutOfBoundsException e) {}
    }

    Collections.sort(timetableItems, new LineComparator());

    return timetableItems;
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
