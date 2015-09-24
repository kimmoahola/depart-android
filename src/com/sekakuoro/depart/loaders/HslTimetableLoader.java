package com.sekakuoro.depart.loaders;

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

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;

public class HslTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private ArrayList<TimetableItem> timetableItems = new ArrayList<TimetableItem>();

  private static final Pattern pTime = Pattern.compile("(.{2})(.{2})");
  private static final Pattern pLine = Pattern.compile("^0+");

  public HslTimetableLoader(Context context, LocationItem item) {
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
    final Map<String, String> lines = new HashMap<String, String>(); // line,
                                                                     // destination
    final Map<String, ArrayList<String>> departures = new HashMap<String, ArrayList<String>>(); // line,
                                                                                                // times

    try {
      final JSONArray jsonArray = new JSONArray(payload);

      final JSONArray linesJsonArray = jsonArray.getJSONObject(0).getJSONArray("lines");
      final int linesLen = linesJsonArray.length();
      for (int j = 0; j < linesLen; ++j) {
        String line = (String) linesJsonArray.get(j);
        final String[] lineSplit = line.split(":");
        if (lineSplit.length == 2)
          lines.put(lineSplit[0], lineSplit[1]);
      }

      {
        final JSONArray departuresJsonArray = jsonArray.getJSONObject(0).getJSONArray("departures");
        final int departuresLen = departuresJsonArray.length();
        for (int j = 0; j < departuresLen; ++j) {
          final JSONObject jsonObjectDeparture = departuresJsonArray.getJSONObject(j);
          final String code = jsonObjectDeparture.getString("code");
          String time = jsonObjectDeparture.getString("time"); // HSL: string,
                                                               // TRE: int

          // HSL data may have times more than 2400 which means the vehicle has
          // started the journey yesterday but passes the stop today.
          int timeInt = Integer.parseInt(time);
          if (timeInt >= 2400) {
            timeInt -= 2400;
            time = pTime.matcher(String.format("%04d", timeInt)).replaceFirst("$1:$2");
          } else if (time.length() < 4) // times like "945"
            time = pTime.matcher(String.format("%04d", timeInt)).replaceFirst("$1:$2");
          else
            time = pTime.matcher(time).replaceFirst("$1:$2");

          final ArrayList<String> timesOld = departures.get(code);
          if (timesOld != null)
            timesOld.add(time);
          else {
            final ArrayList<String> timesNew = new ArrayList<String>();
            timesNew.add(time);
            departures.put(code, timesNew);
          }

        }
      }
    } catch (JSONException e) {
    }

    for (Iterator<Entry<String, ArrayList<String>>> iterator = departures.entrySet().iterator(); iterator.hasNext();) {
      try {
        final Entry<String, ArrayList<String>> dep = (Entry<String, ArrayList<String>>) iterator.next();

        final TimetableItem titem = new TimetableItem();
        titem.line = pLine.matcher(((String) dep.getKey()).substring(1, 5)).replaceFirst("").trim();
        if (titem.line.equals("300V")) // Metros
          titem.line = "V";
        else if (titem.line.equals("300M"))
          titem.line = "M";
        titem.destination = lines.get(dep.getKey());
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