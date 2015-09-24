package com.sekakuoro.depart;

import java.util.ArrayList;
import java.util.List;

public class TimetableItem {
  public String line = "";
  public String destination = "";
  public List<String> times = new ArrayList<String>();

  // These are here so that on the timetable view a row can be clicked.
  public String title = null; // LocationItem.title
  public String id = null; // LocationItem.id

  public enum TypeId {
    Departing, Arriving
  }

  public TypeId typeId = TypeId.Departing;

  public void addTime(String time) {
    if (!this.times.contains(time))
      this.times.add(time);
  }

}
