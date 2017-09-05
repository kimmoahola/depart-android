package com.sekakuoro.depart.tracker;

import android.graphics.Rect;
import android.util.Log;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.Updater;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;

public class Vr extends Updater {

  public Vr() {
    super("Vr", "", new Rect((int) (21.6217 * 1E6),
        (int) (55.7760 * 1E6), (int) (37.6555 * 1E6), (int) (67.3488 * 1E6)), LocationItemCollection.TypeIdEnum.Train,
        LocationItemCollection.AreaTypeIdEnum.Vr);

    maxItemSpeed = 80.0f * 1000.0f / 3600.0f; // meters per second
    minUpdateSpeed = 5000;
  }

  @Override
  protected String getUrl() {
    return "https://junatkartalla-cal-prod.herokuapp.com/trains/" + (System.currentTimeMillis() - 1000 * 30);
  }

  @Override
  protected boolean parsePayload(String payload) {
    boolean ok = true;

    try {
      final JSONObject rootJsonObject = ((JSONObject) new JSONTokener(payload).nextValue()).getJSONObject("trains");

      for (Iterator<String> iter = rootJsonObject.keys(); iter.hasNext(); ) {
        try {
          final String keyName = iter.next();
          final JSONObject jsonObject = rootJsonObject.getJSONObject(keyName);

          // title to two lines
          final String id = jsonObject.getString("id").replaceAll("(?<=[A-Za-z])(?=[0-9])|(?<=[0-9])(?=[A-Za-z])", " ");
          final String title = id;
          final int lat = (int) (jsonObject.getDouble("latitude") * 1E6);
          final int lng = (int) (jsonObject.getDouble("longitude") * 1E6);
          final short bearing = (short) jsonObject.getInt("direction");
          final short speed = (short) jsonObject.getInt("speed");

          if (id.length() == 0 || title.length() == 0 || lat == 0 || lng == 0) {
            continue;
          }

          itemcoll.updatingLock.lock();
          try {
            LocationItem item = itemcoll.findLocationItemById(id);
            if (item == null) {
              item = new LocationItem(itemcoll);
              item.setId(id);
              itemcoll.add(item);
            }

            item.typeId = LocationItemCollection.TypeIdEnum.Train;
            item.setTitle(title);

            if (speed != 0 && bearing != 0) {
              item.bearing = bearing;
            }

            item.lat = lat;
            item.lng = lng;

            item.update();
          } finally {
            itemcoll.updatingLock.unlock();
          }

        } catch (JSONException e) {
          Log.e("Vr", "JSONException", e);
        } catch (NumberFormatException e) {
          Log.e("Vr", "NumberFormatException", e);
        }
      }

    } catch (JSONException e) {
      Log.e("Vr", "JSONException", e);
      ok = false;
    }

    return ok;
  }

}
