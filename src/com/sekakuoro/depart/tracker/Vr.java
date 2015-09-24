package com.sekakuoro.depart.tracker;

import java.io.IOException;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.Updater;

public class Vr extends Updater {

  public Vr() {
    super("Vr", "http://188.117.35.14/TrainRSS/TrainService.svc/AllTrains", new Rect((int) (21.6217 * 1E6),
        (int) (55.7760 * 1E6), (int) (37.6555 * 1E6), (int) (67.3488 * 1E6)), LocationItemCollection.TypeIdEnum.Train,
        LocationItemCollection.AreaTypeIdEnum.Vr);

    maxItemSpeed = 80.0f * 1000.0f / 3600.0f; // meters per second
    minUpdateSpeed = 5000;
  }

  @Override
  protected boolean parsePayload(String payload) {
    boolean ok = true;

    try {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      XmlPullParser xpp = factory.newPullParser();
      xpp.setInput(new StringReader(payload));
      factory.setNamespaceAware(true);
      int eventType = xpp.getEventType();

      int newLat = 0;
      int newLng = 0;
      short newBearing = 0;
      String newTitle = "";
      String newGuid = "";

      while (eventType != XmlPullParser.END_DOCUMENT) {

        if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("item")) {

          int state = 0;

          try {
            while (true) {
              if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("georss:point")) {
                final String[] latlng = xpp.nextText().split(" ");
                try {
                  newLat = (int) (Float.parseFloat(latlng[0]) * 1e6);
                  newLng = (int) (Float.parseFloat(latlng[1]) * 1e6);
                } catch (NumberFormatException e) {
                  break;
                }

                if (newLat == 0 || newLng == 0)
                  break;
                state++;
              } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("dir")) {
                try {
                  newBearing = Short.parseShort(xpp.nextText());
                } catch (NumberFormatException e) {
                  break;
                }
                state++;
              } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("title")) {
                newTitle = xpp.nextText();
                if (newTitle.length() == 0)
                  break;
                state++;
              } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("guid")) {
                newGuid = xpp.nextText();
                if (newGuid.length() == 0)
                  break;
                state++;
              } else if (state == 4) {

                itemcoll.updatingLock.lock();
                try {
                  LocationItem item = itemcoll.findLocationItemById(newGuid);
                  if (item == null) {
                    item = new LocationItem(itemcoll);
                    item.setId(newGuid);
                    itemcoll.add(item);
                  }

                  item.setTitle(newTitle);
                  item.bearing = newBearing;
                  item.lat = newLat;
                  item.lng = newLng;
                  item.update();
                } finally {
                  itemcoll.updatingLock.unlock();
                }

                break;
              } else if (eventType == XmlPullParser.END_TAG && xpp.getName().equals("item")) {
                break;
              }

              eventType = xpp.next();

            } // while
          } catch (XmlPullParserException e) {
          } catch (NumberFormatException e) {
          } catch (IOException e) {
          }
        } // if item

        eventType = xpp.next();
      }

    } catch (XmlPullParserException e) {
      ok = false;
    } catch (IOException e) {
      ok = false;
    }

    return ok;

  }

}
