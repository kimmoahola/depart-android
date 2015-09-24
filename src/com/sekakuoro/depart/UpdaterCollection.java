package com.sekakuoro.depart;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpdaterCollection {

  public List<Updater> updaters = new ArrayList<Updater>();

  public void add(Updater newUpdater) {
    updaters.add(newUpdater);
  }

  public ArrayList<LocationItem> getLocationItems() {
    ArrayList<LocationItem> all = new ArrayList<LocationItem>();

    for (final Updater updater : updaters) {
      try {
        if (!updater.itemcoll.updatingLock.tryLock(100, TimeUnit.MILLISECONDS)) {
          continue;
        }
        try {
          if (updater.itemcoll.hasItems())
            all.addAll(updater.itemcoll.values());
        } finally {
          updater.itemcoll.updatingLock.unlock();
        }
      } catch (InterruptedException e) {
        break;
      }
    }

    return all;
  }

  // Return the LocationItem having the id.
  public LocationItem findLocationItemById(final String id) {
    for (final Updater updater : updaters) {
      final LocationItem item = updater.itemcoll.findLocationItemById(id);
      if (item != null)
        return item;
    }
    return null;
  }

  // Return the LocationItem having the id and a certain area type.
  public LocationItem findLocationItemById(final String id, final LocationItemCollection.AreaTypeIdEnum areaTypeId) {
    for (final Updater updater : updaters) {
      if (updater.itemcoll.areaTypeId != areaTypeId)
        continue;
      LocationItem item = updater.itemcoll.findLocationItemById(id);
      if (item != null)
        return item;
    }
    return null;
  }

  public void enableAll() {
    for (final Updater updater : updaters) {
      if (!updater.itemcoll.isStop() && updater.isPossibleAreaRectVisible())
        updater.enable();
    }
    for (final Updater updater : updaters) {
      if (updater.itemcoll.isStop() && updater.isPossibleAreaRectVisible())
        updater.enable();
    }
  }

  public void disableAll() {
    for (final Updater updater : updaters)
      updater.disable();
  }

  public void refreshAll() {
    for (final Updater updater : updaters)
      updater.refresh(true);
  }

  public void onMapMove() {
    for (final Updater updater : updaters) {
      if (updater.isPossibleAreaRectVisible() && updater.shouldLoad())
        updater.enable();
      else
        updater.disable();
    }
  }

}
