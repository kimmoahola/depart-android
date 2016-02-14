package com.sekakuoro.depart;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sekakuoro.depart.activities.MyMapActivity;

public class LocationItemCollection {

  // Keys: LocationItem.getId(), Values: LocationItem
  private HashMap<String, LocationItem> items = new HashMap<String, LocationItem>();

  public final Lock updatingLock = new ReentrantLock();

  public enum AreaTypeIdEnum {
    NONE, Tre, Vr, Hsl, Oulu, Turku, Jyvaskyla
  }

  public enum TypeIdEnum {
    NONE, Bus, Train, Tram, Stop, Metro
  }

  public volatile AreaTypeIdEnum areaTypeId = AreaTypeIdEnum.NONE;
  public volatile TypeIdEnum typeId = TypeIdEnum.NONE;

  // The updatingLock has to be locked before calling this
  public void add(LocationItem i) {
    items.put(i.getId(), i);
  }

  // The updatingLock has to be locked before calling this
  public LocationItem findLocationItemById(final String id) {
    return items.get(id);
  }

  // The updatingLock has to be locked before calling this
  public boolean hasItems() {
    return items != null && items.size() > 0;
  }

  public int size() {
    if (items != null)
      return items.size();
    else
      return 0;
  }

  public Collection<LocationItem> values() {
    if (items != null)
      return items.values();
    else
      return null;
  }

  public boolean isStop() {
    return typeId == TypeIdEnum.Stop;
  }

  public void clear() {
    updatingLock.lock();
    try {
      if (items != null) {
        items.clear();
      }
    } finally {
      updatingLock.unlock();
    }
  }

  public void markAllAsNotUpdated() {
    updatingLock.lock();
    try {
      for (final LocationItem item : items.values()) {
        item.markAsNotUpdated();
      }
    } finally {
      updatingLock.unlock();
    }
  }

  public void removeNotUpdated() {
    updatingLock.lock();
    try {
      for (final Iterator<LocationItem> it = items.values().iterator(); it.hasNext();) {
        final LocationItem item = it.next();
        if (!item.isUpdated()) {
          it.remove();
        }
      }
    } finally {
      updatingLock.unlock();
    }
  }

  // The updatingLock has to be locked before calling this
  public boolean shouldDraw() {
    return MyMapActivity.getZoomLevel() >= getShouldDrawZoomLevel();
  }

  // Return a map zoom level in which this collection should be drawn.
  public int getShouldDrawZoomLevel() {
    if (areaTypeId != AreaTypeIdEnum.Vr) {
      if (isStop())
        return 13;
      else
        return 12;
    } else
      return 3;
  }

  private String getAreaTypeString() {
    return areaTypeId.name().toLowerCase();
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

}
