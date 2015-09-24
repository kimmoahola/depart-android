package com.sekakuoro.depart.tracker;

import java.io.BufferedReader;
import java.io.StringReader;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.LocationItemCollection.TypeIdEnum;
import com.sekakuoro.depart.Updater;

public class Hsl extends Updater {

  public Hsl() {
    super("Hsl", "http://83.145.232.209:10001/?type=vehicles&lng1=23&lat1=60&lng2=26&lat2=61", new Rect(
        (int) (24.2975 * 1E6), (int) (60.0049 * 1E6), (int) (25.4994 * 1E6), (int) (60.6313 * 1E6)),
        LocationItemCollection.TypeIdEnum.Bus, LocationItemCollection.AreaTypeIdEnum.Hsl);
  }

  @Override
  protected boolean parsePayload(final String payload) {
    boolean ok = true;

    final BufferedReader br = new BufferedReader(new StringReader(payload));

    try {
      String line = null;
      while ((line = br.readLine()) != null) {
        try {
          final String s[] = line.split(";");
          if (s.length < 5 || s[0].length() == 0)
            continue;

          TypeIdEnum typeId = itemcoll.typeId;
          String title = "";

          if (s[0].toLowerCase().startsWith("metro")) {
            if (s[1].equals("1"))
              title = "M";
            else if (s[1].equals("2"))
              title = "V";
            typeId = TypeIdEnum.Metro;
          } else { // Bus & Tram
            title = s[1].substring(1).replaceFirst("^0+", "").replaceAll(" .+", "")
                .replaceFirst("([0-9]+[A-Za-z]+)[0-9]+$", "$1").trim();
            if (title.length() == 0)
              continue;

            try {
              if (Integer.parseInt(title.replaceAll("[^0-9]+", "")) <= 10)
                typeId = TypeIdEnum.Tram;
            } catch (NumberFormatException e) {
            }
          }

          final short bearing = Short.parseShort(s[4]);
          final int lat = (int) (Float.parseFloat(s[3]) * 1E6);
          final int lng = (int) (Float.parseFloat(s[2]) * 1E6);

          if (lat == 0 || lng == 0)
            continue;

          itemcoll.updatingLock.lock();
          try {
            LocationItem item = itemcoll.findLocationItemById(s[0]);

            if (item == null) {
              item = new LocationItem(itemcoll);
              item.setId(s[0]);
              item.typeId = typeId;
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
        } catch (Exception e) {
        }
      } // while
    } catch (Exception e) {
      ok = false;
    }

    return ok;

  }

}
