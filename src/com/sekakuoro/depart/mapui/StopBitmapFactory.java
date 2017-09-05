package com.sekakuoro.depart.mapui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.helpers.Utils;

public class StopBitmapFactory {

  private StopBitmapFactory() {
  }

  public static Bitmap createBitmap(LocationItem item) {
    if (!item.isStop())
      return null;

    else if (item.areaTypeId == LocationItemCollection.AreaTypeIdEnum.Vr)
      return bitmapVr;
    else
      return bitmap;
  }

  private static Bitmap bitmap = drawStop(Utils.dipToPx(7), Color.argb(96, 255, 255, 25));
  private static Bitmap bitmapVr = drawStop(Utils.dipToPx(9), Color.argb(192, 100, 195, 10));

  private static Bitmap drawStop(int radius, int color) {

    Paint circleInnerPaint = new Paint();
    circleInnerPaint.setAntiAlias(false);
    circleInnerPaint.setColor(color);
    circleInnerPaint.setStyle(Paint.Style.FILL);

    Paint circleOuterPaint = new Paint();
    circleOuterPaint.setAntiAlias(true);
    circleOuterPaint.setARGB(128, 0, 0, 0);
    circleOuterPaint.setStyle(Paint.Style.STROKE);
    circleOuterPaint.setStrokeWidth(Utils.dipToPx(2));

    final int bitmapWidth = (radius + 4) * 2;
    final int bitmapHeight = bitmapWidth;

    Canvas c = new Canvas();
    c.translate(bitmapWidth / 2, bitmapHeight / 2);

    Bitmap b = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
    c.setBitmap(b);

    // Background circle
    c.drawCircle(0, 0, radius, circleInnerPaint);

    // Background circle border
    c.drawCircle(0, 0, radius, circleOuterPaint);

    return b;
  }

}
