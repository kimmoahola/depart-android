package com.sekakuoro.depart.loaders;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;

public class TreTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private ArrayList<TimetableItem> timetableItems = new ArrayList<TimetableItem>();

  private static final Pattern p = Pattern.compile("<td style=\"back(.*?)<td>&nbsp;</td></tr>", Pattern.DOTALL);
  private static final Pattern pLine = Pattern.compile("ground.*?>(.*?)</td>", Pattern.DOTALL);
  private static final Pattern pDest = Pattern.compile("right:2px;\">(.*?)</a>", Pattern.DOTALL);
  private static final Pattern pTimes = Pattern.compile("<span style=\"white-space:nowrap;?\">(.*?)</span></td>",
      Pattern.DOTALL);
  private static final Pattern pToBeSure = Pattern.compile("(<.*?>)");

  public TreTimetableLoader(Context context, LocationItem item) {
    super(context);
    this.item = item;
  }

  @Override
  protected void onStartLoading() {
    if (timetableItems != null && timetableItems.size() > 0)
      deliverResult(timetableItems);
    else
      forceLoad();
  }

  @Override
  public ArrayList<TimetableItem> loadInBackground() {

    if (item == null)
      return new ArrayList<TimetableItem>();

    final String payload = MyApp.GetHttpFile(item.getTimetableUri().toString(), item);

    timetableItems.clear();

    final Matcher m = p.matcher(payload);
    while (m.find()) {
      final TimetableItem titem = new TimetableItem();
      final String matched = m.group(1);

      final Matcher mLine = pLine.matcher(matched);
      if (mLine.find())
        titem.line = mLine.group(1);

      final Matcher mDest = pDest.matcher(matched);
      if (mDest.find())
        titem.destination = mDest.group(1);

      final Matcher mTimes = pTimes.matcher(matched);
      if (mTimes.find()) {
        String time = pToBeSure.matcher(mTimes.group(1)).replaceAll("");
        if (!time.equals("-"))
          titem.addTime(time);
        if (mTimes.find()) {
          time = pToBeSure.matcher(mTimes.group(1)).replaceAll("");
          if (!time.equals("-"))
            titem.addTime(time);
        }
      }

      // Just in case
      titem.destination = StringEscapeUtils.unescapeHtml4(pToBeSure.matcher(titem.destination).replaceAll(""));
      titem.line = pToBeSure.matcher(titem.line).replaceAll("");
      titem.title = titem.line;
      titem.id = "";

      timetableItems.add(titem);
    }

    return timetableItems;
  }

  @Override
  public void deliverResult(ArrayList<TimetableItem> newList) { // UI thread
    super.deliverResult(newList);
  }

  @Override
  protected void onStopLoading() {
    cancelLoad();
  }

  @Override
  protected void onReset() {
    super.onReset();
    onStopLoading();
  }

}