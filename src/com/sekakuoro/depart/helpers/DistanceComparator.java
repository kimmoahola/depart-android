package com.sekakuoro.depart.helpers;

import java.util.Comparator;

import com.google.android.maps.GeoPoint;
import com.sekakuoro.depart.LocationItem;

public class DistanceComparator implements Comparator<LocationItem> {
  private final GeoPoint geoPoint;

  public DistanceComparator(GeoPoint t) {
    geoPoint = t;
  }

  public int compare(LocationItem i1, LocationItem i2) {
    
    if (i1.equals(i2)) {
      return 0;
    }

    final long i1XDiff = geoPoint.getLongitudeE6() - i1.getGeoPoint().getLongitudeE6();
    final long i1YDiff = geoPoint.getLatitudeE6() - i1.getGeoPoint().getLatitudeE6();
    final long i1DistanceSquared = i1XDiff*i1XDiff + i1YDiff*i1YDiff;

    final long i2XDiff = geoPoint.getLongitudeE6() - i2.getGeoPoint().getLongitudeE6();
    final long i2YDiff = geoPoint.getLatitudeE6() - i2.getGeoPoint().getLatitudeE6();
    final long i2DistanceSquared = i2XDiff*i2XDiff + i2YDiff*i2YDiff;

    if (i1DistanceSquared > i2DistanceSquared) {
      return 1;
    }
    else {
      return -1;
    }
  }

}
