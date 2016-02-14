package com.sekakuoro.depart.helpers;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.Loader;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection.AreaTypeIdEnum;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;
import com.sekakuoro.depart.activities.StandardDepartureActivity;
import com.sekakuoro.depart.activities.VrDepartureActivity;
import com.sekakuoro.depart.loaders.HslTimetableLoader;
import com.sekakuoro.depart.loaders.JyvaskylaTimetableLoader;
import com.sekakuoro.depart.loaders.NullTimetableLoader;
import com.sekakuoro.depart.loaders.OuluTimetableLoader;
import com.sekakuoro.depart.loaders.TreTimetableLoader;
import com.sekakuoro.depart.loaders.TurkuTimetableLoader;
import com.sekakuoro.depart.loaders.VrTimetableLoader;

public class DepartureSelector {

  public static Loader<ArrayList<TimetableItem>> getLoader(final Context context, final LocationItem item) {
    if (item == null) {
      MyApp.logErrorToAnalytics("DepartureSelector: item null");
      return new NullTimetableLoader(context);
    }

    if (item.areaTypeId == AreaTypeIdEnum.Hsl)
      return new HslTimetableLoader(context, item);
    else if (item.areaTypeId == AreaTypeIdEnum.Tre)
      return new TreTimetableLoader(context, item);
    else if (item.areaTypeId == AreaTypeIdEnum.Vr)
      return new VrTimetableLoader(context, item);
    else if (item.areaTypeId == AreaTypeIdEnum.Turku)
      return new TurkuTimetableLoader(context, item);
    else if (item.areaTypeId == AreaTypeIdEnum.Oulu)
      return new OuluTimetableLoader(context, item);
    else if (item.areaTypeId == AreaTypeIdEnum.Jyvaskyla)
      return new JyvaskylaTimetableLoader(context, item);
    else {
      MyApp.logErrorToAnalytics("DepartureSelector: " + item.toJSONObject().toString());
      return new NullTimetableLoader(context);
    }
  }

  public static Intent getLocationItemIntent(final Context context, final LocationItem item) {
    if (context == null || item == null)
      return null;

    if (!item.isStop()) {
      MyApp.trackEvent("Map", "External", item.getAnalyticsPagePath(), 1);
      return new Intent(Intent.ACTION_VIEW, item.getBrowserTimetableUri()).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    Class<?> cls;
    if (item.areaTypeId == AreaTypeIdEnum.Vr && item.isStop())
      cls = VrDepartureActivity.class;
    else
      cls = StandardDepartureActivity.class;
    return new Intent(context, cls).putExtra("item", item.toJSONObject().toString());
  }
}
