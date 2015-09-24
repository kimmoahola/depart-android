package com.sekakuoro.depart;

import java.text.ParseException;

import org.json.JSONException;
import org.json.JSONObject;

public class FavoriteItem extends LocationItem {

  private String userTitle = ""; // User given name

  public FavoriteItem(final LocationItem item) {
    super(item);
  }

  public FavoriteItem() {
    super((LocationItem) null);
  }

  public FavoriteItem(final JSONObject json) throws ParseException {
    fromJSONObject(json);
  }

  public void setUserTitle(final String newUserTitle) {
    userTitle = newUserTitle;
  }

  public String getUserTitle() {
    if (hasUserTitle())
      return userTitle;
    else
      return getPrefixedTitle();
  }

  public boolean hasUserTitle() {
    return (userTitle != null && userTitle.length() > 0);
  }

  public void resetUserTitle() {
    userTitle = "";
  }

  public JSONObject toFavJSONObject() {
    final JSONObject json = super.toJSONObject();
    try {
      json.put("userTitle", userTitle);
    } catch (JSONException e) {
    }
    return json;
  }

  public void fromJSONObject(final JSONObject json) throws ParseException {
    if (json == null)
      throw new ParseException("json == null", 0);

    userTitle = json.optString("userTitle");
    super.fromJSON(json);
  }

}
