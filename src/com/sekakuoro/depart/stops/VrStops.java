package com.sekakuoro.depart.stops;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItemCollection;

public class VrStops extends StopUpdater {

  public VrStops() {
    super("VrStops", CLOUD_EU_URL + "StopsVr_jhx45zsb", new Rect((int) (21.6217 * 1E6), (int) (55.7760 * 1E6),
        (int) (37.6555 * 1E6), (int) (67.3488 * 1E6)), LocationItemCollection.TypeIdEnum.Stop,
        LocationItemCollection.AreaTypeIdEnum.Vr);
  }

  @Override
  protected String getDiskCacheFilename() {
    return this.getClass().getSimpleName();
  }

  @Override
  protected void afterUpdate() {
    super.afterUpdate();
  }

}
