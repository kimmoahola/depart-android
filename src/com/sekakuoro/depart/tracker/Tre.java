package com.sekakuoro.depart.tracker;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.Updater;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Tre extends Updater {

  public Tre() {
    super("Tre", "http://lissu.tampere.fi/ajax_servers/busLocations.php", new Rect((int) (23.5598 * 1E6),
        (int) (61.4300 * 1E6), (int) (24.0590 * 1E6), (int) (61.7897 * 1E6)), LocationItemCollection.TypeIdEnum.Bus,
        LocationItemCollection.AreaTypeIdEnum.Tre);
  }

  @Override
  protected boolean parsePayload(String payload) {
    boolean ok = true;

    try {
      final JSONArray jsonArray = new JSONArray(payload);
      final int len = jsonArray.length();

      for (int i = 0; i < len; ++i) {
        try {
          final JSONObject jsonObject = jsonArray.getJSONObject(i);

          final String id = jsonObject.getString("journeyId");
          final String title = jsonObject.getString("lCode");
          final short bearing = Short.parseShort(jsonObject.getString("bearing"));
          final int lat = (int) (jsonObject.getDouble("y") * 1E6);
          final int lng = (int) (jsonObject.getDouble("x") * 1E6);

          if (id.length() == 0 || title.length() == 0 || lat == 0 || lng == 0)
            continue;

          itemcoll.updatingLock.lock();
          try {
            LocationItem item = itemcoll.findLocationItemById(id);
            if (item == null) {
              item = new LocationItem(itemcoll);
              item.setId(id);
              itemcoll.add(item);
            }

            item.setTitle(title);
            item.bearing = bearing;
            item.lat = lat;
            item.lng = lng;

            item.update();
          } finally {
            itemcoll.updatingLock.unlock();
          }
        } catch (JSONException e) {
        } catch (NumberFormatException e) {
        }
      }
    } catch (JSONException e) {
      ok = false;
    }

    return ok;
  }

}
