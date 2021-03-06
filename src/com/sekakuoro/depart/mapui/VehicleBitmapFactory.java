package com.sekakuoro.depart.mapui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.helpers.Utils;

public class VehicleBitmapFactory {

  private VehicleBitmapFactory() {
  }

  public static Bitmap createBitmap(LocationItem item) {

    if (item.isStop())
      return null;

    Bitmap newBitmap = item.getBitmap();
    if (newBitmap == null)
      newBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
    else
      newBitmap.eraseColor(Color.TRANSPARENT);

    // Direction arrow
    int bearing;
    float bearingLeft;
    float bearingLeftX = 0;
    float bearingLeftY = 0;
    float bearingRight = 0;
    float bearingRightX = 0;
    float bearingRightY = 0;
    float bearingCenter = 0;
    float bearingCenterX = 0;
    float bearingCenterY = 0;
    float bearingCenterShortX = 0;
    float bearingCenterShortY = 0;

    if (item.bearing > 0) {
      bearing = item.bearing - 1;
      bearingLeft = (float) (Math.toRadians(bearing) - bearingArrowSideRad - HALF_PI);
      bearingLeftX = (float) (Math.cos(bearingLeft) * (distanceInner + 1));
      bearingLeftY = (float) (Math.sin(bearingLeft) * (distanceInner + 1));
      bearingRight = (float) (Math.toRadians(bearing) + bearingArrowSideRad - HALF_PI);
      bearingRightX = (float) (Math.cos(bearingRight) * (distanceInner + 1));
      bearingRightY = (float) (Math.sin(bearingRight) * (distanceInner + 1));
      bearingCenter = (float) (Math.toRadians(bearing) - HALF_PI);
      bearingCenterX = (float) (Math.cos(bearingCenter) * distanceBearing);
      bearingCenterY = (float) (Math.sin(bearingCenter) * distanceBearing);
      bearingCenterShortX = (float) (Math.cos(bearingCenter) * distanceInner);
      bearingCenterShortY = (float) (Math.sin(bearingCenter) * distanceInner);
    }

    synchronized (c) {

      c.setBitmap(newBitmap);

      if (item.bearing > 0) {
        path.reset();
        path.moveTo(bearingLeftX, bearingLeftY);
        path.lineTo(bearingCenterX, bearingCenterY);
        path.lineTo(bearingRightX, bearingRightY);
        path.lineTo(bearingCenterShortX, bearingCenterShortY);
        path.lineTo(bearingLeftX, bearingLeftY);
        path.close();
        c.drawPath(path, bearingPaint);
      }

      if (item.typeId == LocationItemCollection.TypeIdEnum.Train)
        circleInnerPaint.setARGB(GENERAL_ALPHA, 100, 195, 10);
      else if (item.typeId == LocationItemCollection.TypeIdEnum.Tram)
        circleInnerPaint.setARGB(GENERAL_ALPHA, 4, 170, 100);
      else if (item.typeId == LocationItemCollection.TypeIdEnum.Metro)
        circleInnerPaint.setARGB(GENERAL_ALPHA, 224, 84, 19);
      else
        // Bus
        circleInnerPaint.setARGB(GENERAL_ALPHA, 63, 143, 255);

      // Background circle
      c.drawCircle(0, 0, distanceInner, circleInnerPaint);

      // Background circle border
      c.drawCircle(0, 0, distanceInner, circleOuterPaint);

      // Text

      String title = item.getTitle();

      if (title.length() > 4) {
        title = title.replaceAll("(?<=[A-Za-z])(?=[0-9])|(?<=[0-9])(?=[A-Za-z])", " ");
      }

      final int spacePos = title.indexOf(' ');
      if (title.length() > 3 && spacePos > 0) {
        // Two row text

        // First row
        c.drawText(title.substring(0, spacePos), 0, textYPos - textVerySmallPaint.getTextSize() / 2.0f, textVerySmallPaint);

        // Second row
        c.drawText(title.substring(spacePos + 1), 0, textYPos + textVerySmallPaint.getTextSize() / 3.0f, textVerySmallPaint);

      } else if (title.length() > 3) {
        c.drawText(title, 0, textYPos - 4.0f, textVerySmallPaint);
      } else if (title.length() > 2) {
        c.drawText(title, 0, textYPos - 2.0f, textSmallPaint);
      } else {
        c.drawText(title, 0, textYPos, textPaint);
      }

    } // synchronized (c)

    return newBitmap;
  }

  private static final float HALF_PI = (float) (Math.PI / 2.0);

  private static final Paint bearingPaint = new Paint();
  private static final Paint textPaint = new Paint();
  private static final Paint textSmallPaint = new Paint();
  private static final Paint textVerySmallPaint = new Paint();
  private static final Paint circleInnerPaint = new Paint();
  private static final Paint circleOuterPaint = new Paint();
  private static final Path path = new Path();

  private static final int GENERAL_ALPHA = 192;

  private static final int distanceInner = Utils.dipToPx(14);
  private static final int distanceBearing = (int) Math.ceil(Math.sqrt(2 * distanceInner * distanceInner));
  private static final float bearingArrowSideRad = (float) Math.cos(distanceInner / distanceBearing);

  private static final int bitmapWidth = (distanceBearing + 2) * 2;
  private static final int bitmapHeight = bitmapWidth;

  private static final float textYPos;

  private static final Canvas c = new Canvas();

  static {
    bearingPaint.setColor(Color.BLACK);
    bearingPaint.setAlpha(GENERAL_ALPHA);
    bearingPaint.setStyle(Paint.Style.FILL);
    bearingPaint.setAntiAlias(true);

    textPaint.setTextAlign(Paint.Align.CENTER);
    textPaint.setTextSize(Utils.dipToPx(16));
    textPaint.setColor(Color.BLACK);
    textPaint.setAntiAlias(true);
    textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    textYPos = textPaint.getTextSize() / 3.0f - 1.0f;

    textSmallPaint.set(textPaint);
    textSmallPaint.setTextSize(Utils.dipToPx(13));

    textVerySmallPaint.set(textPaint);
    textVerySmallPaint.setTextSize(Utils.dipToPx(11));

    circleInnerPaint.setAntiAlias(false);
    circleInnerPaint.setStyle(Paint.Style.FILL);

    circleOuterPaint.setAntiAlias(true);
    circleOuterPaint.setARGB(GENERAL_ALPHA, 0, 0, 0);
    circleOuterPaint.setStyle(Paint.Style.STROKE);
    circleOuterPaint.setStrokeWidth(Utils.dipToPx(2));

    c.translate(bitmapWidth / 2, bitmapHeight / 2);
  }

}
