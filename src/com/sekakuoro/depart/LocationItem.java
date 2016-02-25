package com.sekakuoro.depart;

import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.net.Uri;

import com.google.android.maps.GeoPoint;
import com.sekakuoro.depart.LocationItemCollection.AreaTypeIdEnum;
import com.sekakuoro.depart.LocationItemCollection.TypeIdEnum;
import com.sekakuoro.depart.mapui.BitmapFactory;

public class LocationItem {

  public LocationItemCollection itemcoll = null;

  private String title = "";
  public String shortId = "";
  private String id = ""; // Optional more detailed ID
  private String uniqueId = ""; // Unique ID of format "<area>_<id>" like
                                // "Tre_1234"

  // Micro degrees (degrees * 1E6).
  public int lat = 0;

  // Micro degrees (degrees * 1E6).
  public int lng = 0;

  // Degrees
  public short bearing = -1;

  public AreaTypeIdEnum areaTypeId = AreaTypeIdEnum.NONE;
  public TypeIdEnum typeId = TypeIdEnum.NONE;

  public LocationItem() {
  }

  public LocationItem(final LocationItem item) {
    updateFromLocationItem(item);
  }

  public void updateFromLocationItem(final LocationItem item) {
    if (item == null)
      return;
    this.itemcoll = item.itemcoll;
    this.title = item.title;
    this.shortId = item.shortId;
    this.id = item.id;
    this.uniqueId = item.uniqueId;
    this.lat = item.lat;
    this.lng = item.lng;
    this.bearing = item.bearing;
    this.areaTypeId = item.areaTypeId;
    this.typeId = item.typeId;
    this.isUpdated = item.isUpdated;
    this.geopoint = item.geopoint;
    this.bitmap = item.bitmap;
  }

  public LocationItem(final LocationItemCollection i) {
    itemcoll = i;

    if (i != null) {
      typeId = i.typeId;
      areaTypeId = i.areaTypeId;
    }
  }

  public void setId(final String id) {
    this.id = id;
    uniqueId = getAreaTypeString() + "_" + this.id;
  }

  public String getId() {
    return id;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setTitle(final String newTitle) {
    title = newTitle;
  }

  public String getTitle() {
    return title;
  }

  public boolean isStop() {
    return typeId == TypeIdEnum.Stop;
  }

  public String getAreaAndTypeString() {
    return areaTypeId.toString() + " " + typeId.toString();
  }

  public String getPrefixedTitle() {
    String areaTypeName = "";

    if (areaTypeId == AreaTypeIdEnum.Vr && isStop())
      areaTypeName = areaTypeId.toString().toUpperCase();
    else if (areaTypeId == AreaTypeIdEnum.Vr && !isStop())
      areaTypeName = MyApp.getResourcesWrapper().getString(R.string.train);
    else if (areaTypeId == AreaTypeIdEnum.Tre && !isStop())
      areaTypeName = MyApp.getResourcesWrapper().getString(R.string.bus);
    else if (areaTypeId == AreaTypeIdEnum.Hsl && !isStop())
      areaTypeName = areaTypeId.toString().toUpperCase();
    else if (areaTypeId == AreaTypeIdEnum.Turku && !isStop())
      areaTypeName = MyApp.getResourcesWrapper().getString(R.string.bus);

    if (areaTypeName.length() > 0)
      areaTypeName += ": ";

    return areaTypeName + getTitle();
  }

  private String getAreaTypeString() {
    return areaTypeId.name().toLowerCase();
  }

  public Uri getTimetableUri() {
    Uri uri = Uri.EMPTY;

    if (!isStop())
      return uri;

    switch (areaTypeId) {
      case Hsl:
        uri = Uri.parse("http://api.reittiopas.fi/hsl/prod/?request=stop&user=departhsl&pass=811f4d37&code=" + id);
      break;
      case Tre:
        uri = Uri.parse("http://lissu.tampere.fi/ajax_servers/getStopBox.php?stop=" + id + "&mobile=1");
      break;
      case Vr:
        uri = Uri.parse("http://188.117.35.14/TrainRSS/TrainService.svc/StationInfo?station=" + id);
      break;
      case Turku:
        uri = Uri.parse("http://aikataulut.foli.fi/webapi/departures/bystop/" + id
            + "?maxInfos=10&key=dbf4284a9ca3a9858f3ec3ef37e26cd4&_=" + System.currentTimeMillis());
      break;
      case Oulu:
        long start_time = System.currentTimeMillis() / 1000;
        long end_time = start_time + 24 * 3600; // add 24 hours
        uri = Uri
            .parse("http://it2.infotripla.fi/otp-rest-servlet/ws/routers/default/transit/stopTimesForStop?agency=&endTime="
                + end_time + "&extended=true&id=" + id + "&references=true&startTime=" + start_time);
      break;
      case Jyvaskyla:
        final String encodedTitle = Uri.encode(title);
        uri = Uri.parse("http://info.jyvaskylanliikenne.fi/?ua=select&v=pda&key=" + encodedTitle + "&lcn=" + id + "%7C"
            + encodedTitle);
      break;
      default:
      break;
    }

    return uri;
  }

  public Uri getBrowserTimetableUri() {
    Uri uri = Uri.EMPTY;

    if (isStop()) {
      if (areaTypeId == AreaTypeIdEnum.Tre)
        uri = Uri.parse("http://aikataulut.tampere.fi/?mobile=1&stop=" + id);
      else if (areaTypeId == AreaTypeIdEnum.Vr) {
        final String encodedTitle = Uri.encode(title);
        uri = Uri
            .parse("https://shop.vr.fi/vrmobiili/ParseLocationDataForStation.do?query.stationName=" + encodedTitle);
      } else if (areaTypeId == AreaTypeIdEnum.Hsl)
        uri = Uri.parse("http://www.omatlahdot.fi/omatlahdot/web?stopid=" + id
            + "&Submit=Hae&command=quicksearch&view=mobile");
      else if (areaTypeId == AreaTypeIdEnum.Oulu)
        uri = Uri.parse("http://www.oulunliikenne.fi/#/joukkoliikenne/aikataulut/pysakki/" + id);
      else if (areaTypeId == AreaTypeIdEnum.Turku)
        uri = Uri.parse("http://aikataulut.foli.fi/onlineinfomobile/index.fi.html#departures?stopId=" + id);
      else if (areaTypeId == AreaTypeIdEnum.Jyvaskyla) {
        final String encodedTitle = Uri.encode(title);
        uri = Uri.parse("http://info.jyvaskylanliikenne.fi/?ua=select&v=pda&key=" + encodedTitle + "&lcn=" + id + "%7C"
            + encodedTitle);
      }
    } else {
      if (areaTypeId == AreaTypeIdEnum.Tre) {
        final String lineNumber = title.replaceAll("[^0-9AB]+", "");
        uri = Uri.parse("http://aikataulut.tampere.fi/?line=" + lineNumber + "&mobile=1&page=lineTimetable");
      } else if (areaTypeId == AreaTypeIdEnum.Vr) {
        uri = Uri.parse("https://shop.vr.fi/vrmobiili/ParseLocationDataForTrain.do?trainQuery=" + id);
      } else if (areaTypeId == AreaTypeIdEnum.Hsl) {
        if (typeId == TypeIdEnum.Metro) {
          if (title.equals("M"))
            uri = Uri.parse("http://aikataulut.hsl.fi/linjat/fi/hMetro_Mellunmaki.html");
          else if (title.equals("V"))
            uri = Uri.parse("http://aikataulut.hsl.fi/linjat/fi/hMetro_Vuosaari.html");
        } else
          uri = Uri.parse("http://aikataulut.hsl.fi/linjat/fi/haku/?key=" + title);
      } else if (areaTypeId == AreaTypeIdEnum.Turku) {
        uri = Uri.parse("http://www.foli.fi/fi/node/1581/");
      }
    }

    return uri;
  }

  public static Uri getBrowserVehicleTimetableUriStatic(TypeIdEnum typeId, AreaTypeIdEnum areaTypeId, String title,
      String id) {
    Uri uri = Uri.EMPTY;

    if (title == null || id == null)
      return uri;

    if (areaTypeId == AreaTypeIdEnum.Tre) {
      final String lineNumber = title.replaceAll("[^0-9AB]+", "");
      uri = Uri.parse("http://aikataulut.tampere.fi/?line=" + lineNumber + "&mobile=1&page=lineTimetable");
    } else if (areaTypeId == AreaTypeIdEnum.Vr) {
      uri = Uri.parse("https://shop.vr.fi/vrmobiili/ParseLocationDataForTrain.do?trainQuery=" + id);
      // }
    } else if (areaTypeId == AreaTypeIdEnum.Hsl) {
      if (title.equals("M"))
        uri = Uri.parse("http://aikataulut.hsl.fi/linjat/fi/hMetro_Mellunmaki.html");
      else if (title.equals("V"))
        uri = Uri.parse("http://aikataulut.hsl.fi/linjat/fi/hMetro_Vuosaari.html");
      // }
      else
        uri = Uri.parse("http://aikataulut.hsl.fi/linjat/fi/haku/?key=" + title);
    } else if (areaTypeId == AreaTypeIdEnum.Turku) {
      uri = Uri.parse("http://turku.seasam.com/nettinaytto/web?linjatunnus=" + title + "&command=search&view=mobile");
    }

    return uri;
  }

  public String getAnalyticsPagePath() {
    String path = "getAnalyticsPagePath_Error";

    if (areaTypeId == LocationItemCollection.AreaTypeIdEnum.Tre && !isStop()) {
      String lineNumber = title.replaceAll("[^0-9]+", "");
      path = "Vehicles/tre/" + lineNumber;
    } else if (areaTypeId == LocationItemCollection.AreaTypeIdEnum.Tre && isStop())
      path = "Stops/tre/" + id;
    else if (areaTypeId == LocationItemCollection.AreaTypeIdEnum.Vr && !isStop()) {
      String[] trainNumberSplit;
      if (title.matches("IC2.*"))
        trainNumberSplit = title.split("[^0-9]+");
      else
        trainNumberSplit = id.split("[^0-9]+");
      if (trainNumberSplit.length > 0) {
        String trainNumber = trainNumberSplit[trainNumberSplit.length - 1];
        path = "Vehicles/vr/" + trainNumber;
      }
    } else if (areaTypeId == LocationItemCollection.AreaTypeIdEnum.Hsl && !isStop())
      path = "Vehicles/hsl/" + title;
    else {
      if (isStop())
        path = "Stops/";
      else
        path = "Vehicle/";
      path += getAreaTypeString() + "/" + id;
    }

    return path;
  }

  public String getAnalyticsAreaPath() {
    String path;

    if (isStop())
      path = "Stops/";
    else
      path = "Vehicle/";

    path += getAreaTypeString();

    return path;
  }

  public JSONObject toJSONObject() {
    try {
      final JSONObject json = new JSONObject();
      json.put("title", title);
      json.put("id", id);
      json.put("lat", lat);
      json.put("lng", lng);
      json.put("bearing", bearing);
      json.put("areaTypeId", areaTypeId.ordinal());
      json.put("typeId", typeId.ordinal());
      return json;
    } catch (JSONException e) {
      return new JSONObject();
    }
  }

  public void fromJSONObjectString(final String json) throws ParseException {
    try {
      final JSONObject obj = new JSONObject(json);
      fromJSON(obj);
    } catch (JSONException e) {
      throw new ParseException("Invalid json", 0);
    }
  }

  public void fromJSON(final JSONObject json) throws ParseException {
    if (json == null) {
      MyApp.logErrorToAnalytics("LocationItem: json == null");
      throw new ParseException("json == null", 0);
    }

    try {
      if (json.getString("title").length() == 0) {
        MyApp.logErrorToAnalytics("LocationItem: Title too short: " + json.getString("title").length());
        throw new ParseException("Title too short.", 0);
      }

      if (json.getString("id").length() == 0) {
        MyApp.logErrorToAnalytics("LocationItem: Id too short: " + json.getString("id").length());
        throw new ParseException("Id too short.", 0);
      }

      setTitle(json.getString("title"));
      id = json.getString("id");
      shortId = id;

      lat = json.getInt("lat");
      lng = json.getInt("lng");

      if (lat == 0 || lng == 0) {
        MyApp.logErrorToAnalytics("LocationItem: Latitude or longitude is zero.");
        throw new ParseException("Invalid latitude or longitude.", 0);
      }

      try {
        bearing = (short) json.getInt("bearing");
      } catch (JSONException e) {
      }
      try {
        areaTypeId = AreaTypeIdEnum.values()[json.getInt("areaTypeId")];
      } catch (JSONException e) {
      }
      try {
        typeId = TypeIdEnum.values()[json.getInt("typeId")];
      } catch (JSONException e) {
      }
      setId(id); // Set the id again so that uniqueId updates after setting
                 // areaTypeId
    } catch (JSONException e) {
      throw new ParseException("Invalid json", 0);
    }

  }

  public void fromJSON(final JSONArray json) throws ParseException {
    if (json == null) {
      MyApp.logErrorToAnalytics("LocationItem: json == null");
      throw new ParseException("json == null", 0);
    }

    if (json.length() < 4 || json.length() > 8) {
      MyApp.logErrorToAnalytics("LocationItem: json.length(): " + json.length());
      throw new ParseException("Did not contain 4-8 fields.", 0);
    }

    try {
      final JSONObject obj = new JSONObject();
      obj.put("title", json.getString(0));
      obj.put("id", json.getString(1));
      obj.put("lat", json.getInt(2));
      obj.put("lng", json.getInt(3));

      if (json.length() == 8) {
        obj.put("bearing", json.getInt(5));
        obj.put("areaTypeId", json.getInt(6));
        obj.put("typeId", json.getInt(7));
      }
      fromJSON(obj);
    } catch (JSONException e) {
      throw new ParseException("Invalid json", 0);
    }
  }

  public void markAsUpdated() {
    isUpdated = true;
  }

  public void markAsNotUpdated() {
    isUpdated = false;
  }

  public boolean isUpdated() {
    return isUpdated;
  }

  public GeoPoint getGeoPoint() {
    if (geopoint == null)
      updateGeoPoint();
    return geopoint;
  }

  private void updateGeoPoint() {
    geopoint = new GeoPoint(lat, lng);
  }

  private boolean isUpdated = true;

  private GeoPoint geopoint = null;

  private Bitmap bitmap = null;

  public Bitmap getBitmap() {
    return bitmap;
  }

  private void updateBitmap() {
    bitmap = BitmapFactory.createBitmap(this);
  }

  public void update() {
    updateGeoPoint();
    updateBitmap();
    markAsUpdated();
  }

  public String getUserTitle() {
    final FavoriteItem fav = FavoritesCollection.get(this);
    if (fav != null)
      return fav.getUserTitle();
    else
      return this.title;
  }

}
