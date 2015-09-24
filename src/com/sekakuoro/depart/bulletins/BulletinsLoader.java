package com.sekakuoro.depart.bulletins;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.MyApp;

public class BulletinsLoader extends AsyncTaskLoader<ArrayList<BulletinsItem>> {

  private BulletinsFeed bulletinsFeed = null;
  public int index = -1;

  public BulletinsLoader(Context context, int i) {
    super(context);

    index = i;
    bulletinsFeed = MyApp.bulletinFeeds.get(index);
  }

  @Override
  protected void onStartLoading() {

    if (bulletinsFeed != null && !bulletinsFeed.isOld() && bulletinsFeed.itemList != null
        && bulletinsFeed.itemList.size() > 0) {
      deliverResult(bulletinsFeed.itemList);
    } else {
      forceLoad();
    }

  }

  @Override
  public ArrayList<BulletinsItem> loadInBackground() {

    if (bulletinsFeed == null) {
      return null;
    }

    bulletinsFeed.load();
    return bulletinsFeed.itemList;
  }

  @Override
  public void deliverResult(ArrayList<BulletinsItem> newList) { // UI thread
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

  protected void onReleaseResources(ArrayList<BulletinsItem> apps) {
  }
}