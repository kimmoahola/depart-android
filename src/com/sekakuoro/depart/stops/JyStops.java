package com.sekakuoro.depart.stops;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItemCollection;

public class JyStops extends StopUpdater {

  public JyStops() {
    super(
        "JyStops",
        CLOUD_EU_URL + "StopsJy_jhx45zsb",
        new Rect((int) (24.986 * 1E6), (int) (61.963 * 1E6), (int) (26.50 * 1E6), (int) (62.520 * 1E6)),
        LocationItemCollection.TypeIdEnum.Stop,
        LocationItemCollection.AreaTypeIdEnum.Jyvaskyla);
  }

  @Override
  protected String getDiskCacheFilename() {
    return this.getClass().getSimpleName();
  }

}
