package com.sekakuoro.depart;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;

public class FavoritesCollection {

  private static final String STATE_KEY = "Depart_favs";
  private static Context context = null;
  private static volatile boolean loadFinished = false;
  private static final Object loadLock = new Object();

  // Keys: LocationItem.getUniqueId(), Values: FavoriteItem
  private static Map<String, FavoriteItem> favorites = new LinkedHashMap<String, FavoriteItem>();

  private FavoritesCollection() {
  }

  public static void setContextAndLoad(final Context c) {
    if (c == null)
      return;
    context = c;
    load();
  }

  public static Collection<FavoriteItem> getFavoritesValues() {
    waitLoad();
    return favorites.values();
  }

  public static Iterator<FavoriteItem> getFavoritesIterator() {
    waitLoad();
    return favorites.values().iterator();
  }

  public static void add(final LocationItem item) {
    if (item == null)
      return;
    final FavoriteItem fav = new FavoriteItem(item);
    waitLoad();
    if (favorites.put(item.getUniqueId(), fav) != null)
      save();
  }

  public static void remove(final LocationItem item) {
    if (item == null)
      return;
    waitLoad();
    if (favorites.remove(item.getUniqueId()) != null)
      save();
  }

  public static FavoriteItem get(final LocationItem item) {
    return favorites.get(item.getUniqueId());
  }

  /**
   * Marks the LocationItem as a favorite. If the item was not favorite
   * previously, mark it as favorite and return true. Otherwise unmark the item
   * and return false.
   */
  public static boolean toggle(final LocationItem item) {
    if (item == null)
      return false;
    if (!isFavorited(item)) {
      final FavoriteItem fav = new FavoriteItem(item);
      favorites.put(item.getUniqueId(), fav);
      save();
      return true;
    } else {
      favorites.remove(item.getUniqueId());
      save();
      return false;
    }
  }

  public static boolean isFavorited(final LocationItem item) {
    if (item == null)
      return false;
    waitLoad();
    return favorites.containsKey(item.getUniqueId());
  }

  // Updates favorite's location, name etc. from the newly loaded stops.
  public static void updateItemsFrom(final LocationItemCollection itemcoll) {
    if (itemcoll == null)
      return;
    waitLoad();
    for (LocationItem item : itemcoll.values()) {
      final FavoriteItem fav = favorites.get(item.getUniqueId());
      if (fav != null) {
        fav.updateFromLocationItem(item);
      }
    }
  }

  private static JSONArray toJSONArray() {
    final Iterator<FavoriteItem> it = favorites.values().iterator();
    final JSONArray jsonArray = new JSONArray();
    while (it.hasNext()) {
      final FavoriteItem item = (FavoriteItem) it.next();
      jsonArray.put(item.toFavJSONObject());
    }
    return jsonArray;
  }

  private static void fromJSONArray(final String json) {
    try {
      final JSONArray jsonArray = new JSONArray(json);
      for (int i = 0; i < jsonArray.length(); i++) {
        try {
          final FavoriteItem fav = new FavoriteItem(jsonArray.getJSONObject(i));
          favorites.put(fav.getUniqueId(), fav);
        } catch (ParseException e) {
        }
      }
    } catch (JSONException e) {
    }

  }

  private static void waitLoad() {
    if (loadFinished)
      return;

    synchronized (loadLock) {
      while (!loadFinished) {
        try {
          loadLock.wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public static void save() {
    if (context == null)
      return;

    MyApp.executeRunnable(new Runnable() {
      public void run() {
        synchronized (loadLock) {
          try {
            final FileOutputStream fos = context.openFileOutput(STATE_KEY, Context.MODE_PRIVATE);
            final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);
            final BufferedWriter bw = new BufferedWriter(outputStreamWriter);
            bw.write("3"); // version
            bw.newLine();
            bw.write(toJSONArray().toString());
            bw.close();
          } catch (FileNotFoundException e) {
          } catch (IOException e) {
          }

          loadLock.notifyAll();
        }
      }
    });
  }

  private static void load() {
    if (context == null)
      return;

    MyApp.executeRunnable(new Runnable() {
      public void run() {
        synchronized (loadLock) {
          boolean loadedOldVersion = false;
          try {
            final FileInputStream fis = context.openFileInput(STATE_KEY);
            final InputStreamReader inputStreamReader = new InputStreamReader(fis);
            final BufferedReader br = new BufferedReader(inputStreamReader);
            final String ver = br.readLine(); // version
            String line;
            if (ver.equals("3")) {
              line = br.readLine();
              fromJSONArray(line);
            }
            fis.close();
          } catch (FileNotFoundException e) {
          } catch (IOException e) {
          }

          loadFinished = true;
          loadLock.notifyAll();

          if (loadedOldVersion)
            save();
        }
      }
    });
  }

}
