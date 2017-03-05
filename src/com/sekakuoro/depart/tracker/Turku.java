package com.sekakuoro.depart.tracker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.Updater;

public class Turku extends Updater {

  public Turku() {
    super("Turku", "", new Rect((int) (21.3 * 1E6), (int) (60.1 * 1E6), (int) (23.1 * 1E6), (int) (60.9 * 1E6)),
        LocationItemCollection.TypeIdEnum.Bus, LocationItemCollection.AreaTypeIdEnum.Turku);
  }

  @Override
  protected String getUrl() {
    String currentTime = (new SimpleDateFormat("HH:mm:ss")).format(new Date());
    String currentDate = (new SimpleDateFormat("yyyyMMdd")).format(new Date());

    try {
      return "http://reittiopas.foli.fi/bin/query.exe/3finny?"
          + "look_minx=20000000&"
          + "look_maxx=24000000&"
          + "look_miny=60000000&"
          + "look_maxy=61000000&"
          + "tpl=trains2json3&"
          + "look_productclass=1&"
          + "look_json=yes&"
          + "performLocating=1&"
          + "look_requesttime="
          + currentTime
          + "&"
          + URLEncoder
              .encode(
                  "look_nv=get_rtmsgstatus|yes|get_zntrainname|no|zugposmode|2|interval|35000|intervalstep|2000|get_nstop|yes|get_pstop|yes|get_fstop|yes|get_stopevaids|yes|get_stoptimes|yes|get_rtstoptimes|yes|tplmode|trains2json3|correctunrealpos|no|livemapTrainfilter|yes|get_zusatztyp|yes|get_infotext|yes|&",
                  "UTF-8") + "interval=35000&" + "intervalstep=2000&" + "livemapRequest=yes&" + "ts=" + currentDate
          + "&";
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }

  @Override
  protected boolean parsePayload(String payload) {
    boolean ok = true;

    itemcoll.updatingLock.lock();
    try {
      itemcoll.clear(); // Clear all items because Turku bus json don't have ID
                        // on the busses.

      final JSONArray jsonArray = (new JSONArray(payload)).getJSONArray(0);
      final int len = jsonArray.length();

      for (int i = 0; i < len; ++i) {
        try {
          final JSONArray arr = jsonArray.getJSONArray(i);

          final String title = arr.getString(0).replace("Linja", "").replace("Lin", "").trim();
          final int lat = (int) arr.getInt(2);
          final int lng = (int) arr.getInt(1);

          if (title.length() == 0 || lat == 0 || lng == 0)
            continue;

          LocationItem item = new LocationItem(itemcoll);
          item.setId("turku_bus_" + Integer.toString(i));
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
