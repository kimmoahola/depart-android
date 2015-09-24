package com.sekakuoro.depart.stops;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItemCollection;

public class OuluStops extends StopUpdater {

  public OuluStops() {
    super(
        "OuluStops",
        CLOUD_EU_URL + "StopsOulu_jhx45zsb",
        new Rect((int) (25.179 * 1E6), (int) (64.762 * 1E6), (int) (26.215 * 1E6), (int) (65.523 * 1E6)),
        LocationItemCollection.TypeIdEnum.Stop,
        LocationItemCollection.AreaTypeIdEnum.Oulu);
  }

  @Override
  protected String getDiskCacheFilename() {
    return this.getClass().getSimpleName();
  }

}
