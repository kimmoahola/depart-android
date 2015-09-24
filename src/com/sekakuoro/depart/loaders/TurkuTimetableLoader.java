package com.sekakuoro.depart.loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;

public class TurkuTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private Map<String, TimetableItem> timetableItems = new HashMap<String, TimetableItem>(); // <line,
                                                                                            // item>

  private static final Pattern p = Pattern.compile("<tr [^>]*>(.*?)</tr>", Pattern.DOTALL);
  private static final Pattern pLine = Pattern.compile("<td .*?linecol.*?>(.*?)</td>");
  private static final Pattern pDest = Pattern.compile("<td .*?destcol.*?>(.*?)</td>");
  private static final Pattern pTimes = Pattern.compile("<td .*?timecol.*?>(.*?)</td>");
  private static final Pattern pToBeSure = Pattern.compile("(<.*?>)");

  public TurkuTimetableLoader(Context context, LocationItem item) {
    super(context);
    this.item = item;
  }

  @Override
  protected void onStartLoading() {
    if (timetableItems != null && timetableItems.size() > 0)
      deliverResult(new ArrayList<TimetableItem>(timetableItems.values()));
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
      final String matched = m.group(1);

      final Matcher mLine = pLine.matcher(matched);
      if (mLine.find()) {
        final String line = mLine.group(1);

        TimetableItem titem = timetableItems.get(line);
        if (titem == null) {
          titem = new TimetableItem();
          timetableItems.put(line, titem);
          titem.line = line;

          final Matcher mDest = pDest.matcher(matched);
          if (mDest.find())
            titem.destination = mDest.group(1);

          // Just in case
          titem.destination = StringEscapeUtils.unescapeHtml4(pToBeSure.matcher(titem.destination).replaceAll(""));
          titem.line = pToBeSure.matcher(titem.line).replaceAll("");
          titem.title = titem.line;
          titem.id = "";
        }

        final Matcher mTimes = pTimes.matcher(matched);
        if (mTimes.find())
          titem.addTime(mTimes.group(1));
      }
    }

    return new ArrayList<TimetableItem>(timetableItems.values());
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