package com.sekakuoro.depart.stops;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItemCollection;

public class TreStops extends StopUpdater {

  public TreStops() {
    super("TreStops", CLOUD_EU_URL + "StopsTre_jhx45zsb", new Rect((int) (23.5598 * 1E6), (int) (61.4300 * 1E6),
        (int) (24.0590 * 1E6), (int) (61.7897 * 1E6)), LocationItemCollection.TypeIdEnum.Stop,
        LocationItemCollection.AreaTypeIdEnum.Tre);
  }

  @Override
  protected String getDiskCacheFilename() {
    return this.getClass().getSimpleName();
  }

}
