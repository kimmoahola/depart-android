package com.sekakuoro.depart.stops;

import java.util.Iterator;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;

public class TurkuStops extends StopUpdater {

  private static final String idPattern = ".+\\([0-9]+\\)$";

  public TurkuStops() {
    super("TurkuStops", CLOUD_EU_URL + "StopsTurku_jhx45zsb", new Rect((int) (22.0944 * 1E6), (int) (60.3459 * 1E6),
        (int) (22.4420 * 1E6), (int) (60.6899 * 1E6)), LocationItemCollection.TypeIdEnum.Stop,
        LocationItemCollection.AreaTypeIdEnum.Turku);
  }

  @Override
  protected String getDiskCacheFilename() {
    return this.getClass().getSimpleName();
  }

  @Override
  protected boolean parsePayload(final String payload) {
    final boolean ok = super.parsePayload(payload);

    for (final Iterator<LocationItem> it = itemcoll.values().iterator(); it.hasNext();) {
      final LocationItem item = it.next();
      if (!item.getTitle().matches(idPattern))
        item.setTitle(item.getTitle() + " (" + item.getId() + ")");
    }

    return ok;
  }

}
