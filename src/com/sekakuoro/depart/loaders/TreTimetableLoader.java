package com.sekakuoro.depart.loaders;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;

public class TreTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private ArrayList<TimetableItem> timetableItems = new ArrayList<TimetableItem>();

  private static final Pattern pTime = Pattern.compile("(.{2})(.{2})");

  public TreTimetableLoader(Context context, LocationItem item) {
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

  @Override
  public ArrayList<TimetableItem> loadInBackground() {
    timetableItems.clear();

    if (item == null)
      return timetableItems;

    final String payload = MyApp.GetHttpFile(item.getTimetableUri().toString(), item);
    final Map<Entry<String, String>, ArrayList<String>> departures = new HashMap<Entry<String, String>, ArrayList<String>>();
    // key: (code, line)
    // value: times

    try {
      final JSONArray jsonArray = new JSONArray(payload);

      {
        final JSONArray departuresJsonArray = jsonArray.getJSONObject(0).getJSONArray("departures");
        final int departuresLen = departuresJsonArray.length();
        for (int j = 0; j < departuresLen; ++j) {
          final JSONObject jsonObjectDeparture = departuresJsonArray.getJSONObject(j);
          final String code = jsonObjectDeparture.getString("code");
          final String line = jsonObjectDeparture.getString("name1");
          String time = jsonObjectDeparture.getString("time");

          // Data may have times more than 2400 which means the vehicle has
          // started the journey yesterday but passes the stop today.
          int timeInt = Integer.parseInt(time);
          if (timeInt >= 2400) {
            timeInt -= 2400;
            time = pTime.matcher(String.format("%04d", timeInt)).replaceFirst("$1:$2");
          } else if (time.length() < 4) // times like "945"
            time = pTime.matcher(String.format("%04d", timeInt)).replaceFirst("$1:$2");
          else
            time = pTime.matcher(time).replaceFirst("$1:$2");

          final ArrayList<String> timesOld = departures.get(new SimpleEntry<String, String>(code, line));
          // Log.e("MYAPP", "timesOld" + timesOld.toString());
          if (timesOld != null) {
            timesOld.add(time);
          } else {
            final ArrayList<String> timesNew = new ArrayList<String>();
            timesNew.add(time);
            departures.put(new SimpleEntry<String, String>(code, line), timesNew);
          }

        }
      }
    } catch (JSONException e) {
      Log.e("MYAPP", "exception", e);
    }

    for (Iterator<Entry<Entry<String, String>, ArrayList<String>>> iterator = departures.entrySet().iterator(); iterator
        .hasNext();) {
      try {
        final Entry<Entry<String, String>, ArrayList<String>> dep = (Entry<Entry<String, String>, ArrayList<String>>) iterator
            .next();

        Entry<String, String> code_name = dep.getKey();

        final TimetableItem titem = new TimetableItem();
        titem.line = code_name.getKey().trim();
        titem.destination = code_name.getValue();
        titem.title = titem.line;
        titem.id = "";
        titem.times.addAll(dep.getValue());
        timetableItems.add(titem);
      } catch (IndexOutOfBoundsException e) {
      }
    }

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