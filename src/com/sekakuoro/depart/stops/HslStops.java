package com.sekakuoro.depart.stops;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItemCollection;

public class HslStops extends StopUpdater {

  public HslStops() {
    super("HslStops", CLOUD_EU_URL + "StopsHsl_jhx45zsb", new Rect((int) (24.2975 * 1E6), (int) (60.0049 * 1E6),
        (int) (25.4994 * 1E6), (int) (60.6313 * 1E6)), LocationItemCollection.TypeIdEnum.Stop,
        LocationItemCollection.AreaTypeIdEnum.Hsl);
  }

  @Override
  protected String getDiskCacheFilename() {
    return this.getClass().getSimpleName();
  }

}
