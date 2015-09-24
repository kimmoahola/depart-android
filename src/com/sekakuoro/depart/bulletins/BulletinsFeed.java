package com.sekakuoro.depart.bulletins;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class BulletinsFeed {

  public long lastLoadTimestamp = 0;
  private long maxAge = 3600 * 3 * 1000;

  public String title = null;
  public int titleResourceId = 0;
  public String baseUrl = null;
  public String url = null;
  public String analyticsId = null;
  public String lang = null;

  public ArrayList<BulletinsItem> itemList = new ArrayList<BulletinsItem>();
  public long latestItemTimestamp = 0;

  public BulletinsFeed(int t, String a, String l, String u) {
    titleResourceId = t;
    analyticsId = a;
    lang = l;
    baseUrl = u;
    url = baseUrl;
    if (lang.length() > 0) {
      url += lang;
    }
  }

  public boolean isOld() {
    return lastLoadTimestamp + maxAge < System.currentTimeMillis();
  }

  public void load() {
    try {
      clear();

      URL rssUrl = new URL(url);
      SAXParserFactory mySAXParserFactory = SAXParserFactory.newInstance();
      SAXParser mySAXParser = mySAXParserFactory.newSAXParser();
      XMLReader myXMLReader = mySAXParser.getXMLReader();
      RSSHandler myRSSHandler = new RSSHandler();
      myXMLReader.setContentHandler(myRSSHandler);
      InputSource is = new InputSource(rssUrl.openStream());
      if (url.contains("liikennevirasto")) {
        is.setEncoding("ISO-8859-1");
      }
      myXMLReader.parse(is);

      lastLoadTimestamp = System.currentTimeMillis();

      Collections.sort(itemList, new TimestampComparator());

      if (itemList.size() > 0)
        latestItemTimestamp = itemList.get(0).timestamp;

      while (itemList.size() > 20)
        itemList.remove(itemList.size() - 1);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void addItem(BulletinsItem item) {
    itemList.add(item);
  }

  public void clear() {
    lastLoadTimestamp = 0;
    latestItemTimestamp = 0;
    itemList.clear();

    url = baseUrl;
    if (lang.length() > 0) {
      url += lang;
    }
  }

  public class RSSHandler extends DefaultHandler {
    BulletinsItem currentItem = null;
    boolean inItem = false;
    StringBuffer chars = null;

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

      if (localName.equalsIgnoreCase("item")) {
        inItem = true;
        currentItem = new BulletinsItem();
      }
      if (inItem) {
        chars = new StringBuffer();
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {

      if (localName.equalsIgnoreCase("title") && inItem) {
        currentItem.title = chars.toString().trim();
      } else if (localName.equalsIgnoreCase("link") && inItem) {
        if (url.contains("vr.fi")) {
          currentItem.link = "http://ext-service.vr.fi/juha/internet/mobile/mobiletiedote.action?lang=" + lang;
        } else {
          currentItem.link = chars.toString().trim().replace(" ", "%20");
        }
      } else if (localName.equalsIgnoreCase("description") && inItem) {
        currentItem.description = chars.toString().trim();
      } else if (localName.equalsIgnoreCase("pubDate") && inItem) {

        final SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        try {
          final Date d = formatter.parse(chars.toString());
          currentItem.timestamp = d.getTime();
        } catch (ParseException e) {
        }

      }

      if (localName.equalsIgnoreCase("item") && inItem) {
        addItem(currentItem);
        inItem = false;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      if (inItem) {
        chars.append(new String(ch, start, length));
      }
    }

  }

  class TimestampComparator implements Comparator<BulletinsItem> {

    public int compare(BulletinsItem i1, BulletinsItem i2) {
      if (i1.equals(i2))
        return 0;
      else if (i1.timestamp < i2.timestamp)
        return 1;
      else
        return -1;
    }
  }

}
