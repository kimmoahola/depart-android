package com.sekakuoro.depart.tracker;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.Updater;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;

public class Turku extends Updater {

  public Turku() {
    super("Turku", "", new Rect((int) (21.3 * 1E6), (int) (60.1 * 1E6), (int) (23.1 * 1E6), (int) (60.9 * 1E6)),
        LocationItemCollection.TypeIdEnum.Bus, LocationItemCollection.AreaTypeIdEnum.Turku);
  }

  @Override
  protected String getUrl() {
    return "http://data.foli.fi/siri/vm";
  }

  @Override
  protected boolean parsePayload(String payload) {
    boolean ok = true;

    itemcoll.updatingLock.lock();
    try {
      itemcoll.clear(); // Clear all items because Turku bus json don't have ID
                        // on the busses.

      final JSONObject rootJsonObject = ((JSONObject) new JSONTokener(payload).nextValue())
          .getJSONObject("result").getJSONObject("vehicles");

      for (Iterator<String> iter = rootJsonObject.keys(); iter.hasNext(); ) {
        try {
          final String keyName = iter.next();
          final JSONObject jsonObject = rootJsonObject.getJSONObject(keyName);

          final String title = jsonObject.getString("lineref").trim();
          final int lat = (int) (jsonObject.getDouble("latitude") * 1E6);
          final int lng = (int) (jsonObject.getDouble("longitude") * 1E6);

          if (title.length() == 0 || lat == 0 || lng == 0)
            continue;

          LocationItem item = new LocationItem(itemcoll);
          item.setId(keyName);
          itemcoll.add(item);

          item.setTitle(title);
          item.lat = lat;
          item.lng = lng;

          item.update();
        } catch (JSONException e) {
        }
      }
    } catch (JSONException e) {
      ok = false;
    } finally {
      itemcoll.updatingLock.unlock();
    }
    return ok;
  }

}
