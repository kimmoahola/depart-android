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

public class Hsl extends Updater {

  public Hsl() {
    super("Hsl", "https://api.digitransit.fi/realtime/vehicle-positions/v1/hfp/", new Rect(
        (int) (24.1 * 1E6), (int) (60.0 * 1E6), (int) (25.6 * 1E6), (int) (60.6 * 1E6)),
        LocationItemCollection.TypeIdEnum.Bus, LocationItemCollection.AreaTypeIdEnum.Hsl);
  }

  @Override
  protected boolean parsePayload(final String payload) {
    boolean ok = true;

    try {
      final JSONObject rootJsonObject = (JSONObject) new JSONTokener(payload).nextValue();

      for (Iterator<String> iter = rootJsonObject.keys(); iter.hasNext(); ) {
        try {
          final String keyName = iter.next();
          final JSONObject jsonObject = rootJsonObject.getJSONObject(keyName).getJSONObject("VP");

          final String id = jsonObject.getString("veh");

          if (id.startsWith("TKL")) {
            continue;
          }

          final String title = jsonObject.getString("desi");
          final int lat = (int) (jsonObject.getDouble("lat") * 1E6);
          final int lng = (int) (jsonObject.getDouble("long") * 1E6);

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

            item.setTitle(title);

            if (jsonObject.has("hdg")) {
              item.bearing = (short) jsonObject.getInt("hdg");
            }

            item.lat = lat;
            item.lng = lng;

            if (keyName.contains("/bus/") || keyName.contains("/ferry/")) {
              item.typeId = LocationItemCollection.TypeIdEnum.Bus;
            } else if (keyName.contains("/rail/")) {
              item.typeId = LocationItemCollection.TypeIdEnum.Train;
            } else if (keyName.contains("/tram/")) {
              item.typeId = LocationItemCollection.TypeIdEnum.Tram;
            } else {
              item.typeId = LocationItemCollection.TypeIdEnum.Bus;
              Log.d("Hsl", "keyName " + keyName);
            }

            item.update();
          } finally {
            itemcoll.updatingLock.unlock();
          }

        } catch (JSONException e) {
          Log.e("Hsl", "JSONException", e);
        } catch (NumberFormatException e) {
          Log.e("Hsl", "NumberFormatException", e);
        }
      }

    } catch (JSONException e) {
      Log.e("Hsl", "JSONException", e);
      ok = false;
    }

    return ok;
  }

}
