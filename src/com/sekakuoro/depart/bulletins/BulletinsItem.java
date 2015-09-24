package com.sekakuoro.depart.bulletins;

import java.io.Serializable;

public class BulletinsItem implements Serializable {

  private static final long serialVersionUID = -4660211779756733242L;

  public String title;
  public String link;
  public String description;
  public long timestamp;

  public BulletinsItem() {
  }

  @Override
  public String toString() {
    return title;
  }

}
