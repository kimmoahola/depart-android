package com.sekakuoro.depart.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;
import com.sekakuoro.depart.TimetableItem.TypeId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;

public class VrTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private ArrayList<TimetableItem> timetableItems = new ArrayList<TimetableItem>();

  public VrTimetableLoader(Context context, LocationItem item) {
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

  private void processFoo(Uri uri, TypeId typeId) {
    final String payload = MyApp.GetHttpFile(uri.toString(), item);

    try {
      final JSONObject rootJsonObject = (JSONObject) new JSONTokener(payload).nextValue();
      final JSONArray jsonArray = rootJsonObject.getJSONArray("traindata");
      final int len = jsonArray.length();

      for (int i = 0; i < len; ++i) {
        try {
          final JSONObject jsonObject = jsonArray.getJSONObject(i);

          TimetableItem titem = new TimetableItem();
          titem.typeId = typeId;
          titem.line = jsonObject.getString("title");
          titem.title = jsonObject.getString("title");
          titem.id = jsonObject.getString("id");
          if (typeId == TypeId.Departing) {
            titem.destination = getStationName(jsonObject.getString("to"));
            titem.addTime(jsonObject.getString("etd"));
          } else {
            titem.destination = getStationName(jsonObject.getString("from"));
            titem.addTime(jsonObject.getString("eta"));
          }
          timetableItems.add(titem);

        } catch (JSONException e) {
          Log.e("VrTimetableLoader", "JSONException", e);
        } catch (NumberFormatException e) {
          Log.e("VrTimetableLoader", "NumberFormatException", e);
        }
      }
    } catch (Exception e) {
      Log.e("VrTimetableLoader", "Exception", e);
    }
  }

  @Override
  public ArrayList<TimetableItem> loadInBackground() {
    timetableItems.clear();

    processFoo(
        Uri.parse("https://junatkartalla-cal-prod.herokuapp.com/stations/single/" + item.getId() + "?type=DEPARTURES"),
        TypeId.Departing);

    processFoo(
        Uri.parse("https://junatkartalla-cal-prod.herokuapp.com/stations/single/" + item.getId() + "?type=ARRIVALS"),
        TypeId.Arriving);

    return timetableItems;
  }

  private String getStationName(String stationId) {
    final LocationItem itemWithTitle = MyApp.uc.findLocationItemById(stationId,
        LocationItemCollection.AreaTypeIdEnum.Vr);
    if (itemWithTitle != null) {
      return itemWithTitle.getTitle();
    }

    return stationId;
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