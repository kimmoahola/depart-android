package com.sekakuoro.depart.loaders;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.TimetableItem;

public class NullTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  public NullTimetableLoader(Context context) {
    super(context);
  }

  @Override
  protected void onStartLoading() {
    forceLoad();
  }

  @Override
  public ArrayList<TimetableItem> loadInBackground() {
    return new ArrayList<TimetableItem>();
  }

}