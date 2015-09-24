package com.sekakuoro.depart.mapui;

import com.sekakuoro.depart.LocationItem;

import android.graphics.Bitmap;

public class BitmapFactory {

  private BitmapFactory() {  
  }

  public static Bitmap createBitmap(LocationItem item) {
    Bitmap newBitmap = null;
    
    if ((newBitmap = VehicleBitmapFactory.createBitmap(item)) == null) {
      newBitmap = StopBitmapFactory.createBitmap(item);
    }
    
    return newBitmap;
  }
  
}
