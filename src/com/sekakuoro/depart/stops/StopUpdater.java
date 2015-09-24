package com.sekakuoro.depart.stops;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Rect;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection.AreaTypeIdEnum;
import com.sekakuoro.depart.LocationItemCollection.TypeIdEnum;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.Updater;
import com.sekakuoro.depart.helpers.Utils;

public abstract class StopUpdater extends Updater {

  private boolean needsToSaveToDisk = false;
  private byte[] encryptedCompressedPayload = new byte[] {};
  private boolean parseFailed = false;

  public StopUpdater(final String tag, final String u, final Rect r, final TypeIdEnum t, final AreaTypeIdEnum a) {
    super(tag, u, r, t, a);
  }

  protected abstract String getDiskCacheFilename();

  @Override
  protected long getUpdateIntervalMillis() {
    return 3 * 24 * 60 * 60 * 1000;
  }

  @Override
  public void clear(final boolean deepClean) {
    super.clear(deepClean);

    if (deepClean)
      deleteCacheFile();
  }

  @Override
  protected void afterUpdate() {
    super.afterUpdate();

    if (needsToSaveToDisk && !parseFailed)
      saveDiskCacheFile();
    else if (parseFailed)
      deleteCacheFile();
  }

  @Override
  protected String getPayload() {
    if (hasValidDiskCache()) {

      final String diskPayload = new String(Utils.decompress(getDiskCacheFile()));
      if (diskPayload != null && diskPayload.length() > 0) {
        needsToSaveToDisk = false;
        return diskPayload;
      }
    } else
      deleteCacheFile();

    encryptedCompressedPayload = MyApp.GetHttpFileAsBytes(url, itemcoll);
    final String netPayload = new String(Utils.decompress(encryptedCompressedPayload));
    needsToSaveToDisk = true;
    return netPayload;
  }

  @Override
  protected boolean parsePayload(final String payload) {
    if (payload == null)
      return false;

    final int lineBreakIndex = payload.indexOf('\n');
    if (lineBreakIndex < 0) {
      parseFailed = true;
      return false;
    }

    final int ver = Integer.parseInt(payload.substring(0, lineBreakIndex));
    if (ver != 2) {
      parseFailed = true;
      return false;
    }

    itemcoll.clear();

    final String jsonString = payload.substring(lineBreakIndex + 1);

    try {
      final JSONArray ar = new JSONArray(jsonString);
      for (int i = 0; i < ar.length(); i++) {
        final JSONArray locationItemJson = ar.getJSONArray(i);

        final LocationItem item = new LocationItem(itemcoll);
        try {
          item.fromJSON(locationItemJson);
          item.update();

          itemcoll.updatingLock.lock();
          try {
            itemcoll.add(item);
          } finally {
            itemcoll.updatingLock.unlock();
          }

        } catch (ParseException e) {
        }
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return true;
  }

  private byte[] getDiskCacheFile() {
    try {
      final File file = new File(MyApp.getApp().getCacheDir(), getDiskCacheFilename());
      final RandomAccessFile raf = new RandomAccessFile(file, "r");
      final byte[] bytes = new byte[(int) raf.length()];
      raf.readFully(bytes);
      raf.close();
      return bytes;
    } catch (IOException e) {
      return new byte[] {};
    }
  }

  private boolean hasValidDiskCache() {
    try {
      final File file = new File(MyApp.getApp().getCacheDir(), getDiskCacheFilename() + ".meta");
      final RandomAccessFile raf = new RandomAccessFile(file, "r");
      final byte[] bytes = new byte[(int) raf.length()];
      raf.readFully(bytes);
      raf.close();

      final JSONObject obj = new JSONObject(new String(bytes));
      lastUpdate = obj.getLong("lastUpdate");
    } catch (Exception e) {
      return false;
    }

    return !isOld();
  }

  private void saveDiskCacheFile() {
    if (encryptedCompressedPayload.length == 0)
      return;

    final File file = new File(MyApp.getApp().getCacheDir(), getDiskCacheFilename());

    try {
      file.createNewFile();
      final FileOutputStream fos = new FileOutputStream(file);
      fos.write(encryptedCompressedPayload);
      fos.close();

      final File fileMeta = new File(MyApp.getApp().getCacheDir(), getDiskCacheFilename() + ".meta");
      try {
        fileMeta.createNewFile();
        final FileWriter fw = new FileWriter(fileMeta);

        final JSONObject obj = new JSONObject();
        obj.put("version", (int) 1);
        obj.put("lastUpdate", System.currentTimeMillis());
        fw.write(obj.toString());
        fw.close();

      } catch (Exception e) {
        fileMeta.delete();
        throw e;
      }

    } catch (Exception e) {
      file.delete();
    }
  }

  private void deleteCacheFile() {
    File file = new File(MyApp.getApp().getCacheDir(), getDiskCacheFilename());
    file.delete();
    file = new File(MyApp.getApp().getCacheDir(), getDiskCacheFilename() + ".meta");
    file.delete();
  }

}
