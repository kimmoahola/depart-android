package com.sekakuoro.depart.loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.sekakuoro.depart.LocationItem;
import com.sekakuoro.depart.LocationItemCollection;
import com.sekakuoro.depart.MyApp;
import com.sekakuoro.depart.TimetableItem;
import com.sekakuoro.depart.TimetableItem.TypeId;
import com.sekakuoro.depart.helpers.Utils;
import com.ximpleware.AutoPilot;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

public class VrTimetableLoader extends AsyncTaskLoader<ArrayList<TimetableItem>> {

  private LocationItem item = null;
  private ArrayList<TimetableItem> timetableItems = new ArrayList<TimetableItem>();

  public VrTimetableLoader(Context context, LocationItem item) {
    super(context);
    this.item = item;
  }

  @Override
  protected void onStartLoading() {
    if (timetableItems != null && timetableItems.size() > 0)
      deliverResult(timetableItems);
    else
      forceLoad();
  }

  @Override
  public ArrayList<TimetableItem> loadInBackground() {

    final String payload = MyApp.GetHttpFile(item.getTimetableUri().toString(), item);
    timetableItems.clear();

    try {
      final VTDGen vg = new VTDGen();
      vg.setDoc(payload.getBytes());
      vg.parse(false);
      final VTDNav vn = vg.getNav();
      final AutoPilot ap = new AutoPilot(vn);
      ap.selectXPath("/rss/channel/item");

      while (ap.evalXPath() != -1) {
        try {
          final String completed = Utils.GetVTDElementText(vn, "completed");

          // bugs in VR data
          final String toStation = Utils.GetVTDElementText(vn, "toStation").replace("IMT", "IMR");
          final String fromStation = Utils.GetVTDElementText(vn, "fromStation").replace("IMT", "IMR");

          final String title = Utils.GetVTDElementText(vn, "title");
          final String guid = Utils.GetVTDElementText(vn, "guid");

          boolean departing = false;
          boolean arriving = false;

          if (!toStation.equals(item.getId()))
            departing = true;
          if (!fromStation.equals(item.getId()))
            arriving = true;

          if (departing) {
            final String destination = getStationName(toStation);
            TimetableItem titem = findSimilarTrain(timetableItems, TypeId.Departing, title, destination);
            if (titem == null) {
              titem = new TimetableItem();
              titem.typeId = TypeId.Departing;
              titem.line = title;
              titem.destination = destination;
              titem.title = title;
              titem.id = guid;
              timetableItems.add(titem);
            }

            final String scheduledDepartTime = Utils.GetVTDElementText(vn, "scheduledDepartTime");
            titem.addTime(scheduledDepartTime);
          }

          if (arriving) {
            final String destination = getStationName(fromStation);
            TimetableItem titem = findSimilarTrain(timetableItems, TypeId.Arriving, title, destination);
            if (titem == null) {
              titem = new TimetableItem();
              titem.typeId = TypeId.Arriving;
              titem.line = title;
              titem.destination = destination;
              titem.title = title;
              titem.id = guid;
              timetableItems.add(titem);
            }

            final String scheduledTime = Utils.GetVTDElementText(vn, "scheduledTime");
            titem.addTime(scheduledTime);
          }

          // We leave one departed train and one arriving
          if (completed.equals("1") && timetableItems.size() > 0) {

            // Find item which is different type than just added item
            TimetableItem titemAiempiErilainen = null;
            if (timetableItems.size() > 1) {
              for (int j = timetableItems.size() - 2; j >= 0; j--) {
                if (timetableItems.get(j).typeId != timetableItems.get(timetableItems.size() - 1).typeId) {
                  titemAiempiErilainen = timetableItems.get(j);
                  break;
                }
              }
            }

            final TimetableItem titem = timetableItems.get(timetableItems.size() - 1);
            timetableItems.clear();
            if (titemAiempiErilainen != null)
              timetableItems.add(titemAiempiErilainen);
            timetableItems.add(titem);
          }

        } catch (Exception e) {
        }
      }
    } catch (Exception e) {
    }

    return timetableItems;
  }

  private TimetableItem findSimilarTrain(List<TimetableItem> timetableItems, TypeId typeId, String line,
      String destination) {
    for (TimetableItem titem : timetableItems) {
      if (titem.line.equals(line) && titem.destination.equals(destination) && titem.typeId == typeId)
        return titem;
    }

    return null;
  }

  private String getStationName(String stationId) {
    final LocationItem itemWithTitle = MyApp.uc.findLocationItemById(stationId,
        LocationItemCollection.AreaTypeIdEnum.Vr);
    if (itemWithTitle != null)
      return itemWithTitle.getTitle();

    final String stationName = stationMap.get(stationId);
    if (stationName != null)
      return stationName;

    return stationId;
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

  // Every prog must have a dirty hax
  // station id, station name
  private static final Map<String, String> stationMap = new HashMap<String, String>(210);

  static {
    stationMap.put("ALV", "Alavus");
    stationMap.put("DRA", "Dragsvik");
    stationMap.put("EPZ", "Eläinpuisto-Zoo");
    stationMap.put("ENO", "Eno");
    stationMap.put("EPO", "Espoo");
    stationMap.put("HPJ", "Haapajärvi");
    stationMap.put("HPK", "Haapamäki");
    stationMap.put("HAA", "Haarajoki");
    stationMap.put("HKS", "Hankasalmi");
    stationMap.put("HNK", "Hanko");
    stationMap.put("HKP", "Hanko-Pohjoinen");
    stationMap.put("HVA", "Harjavalta");
    stationMap.put("HAU", "Haukivuori");
    stationMap.put("HNV", "Heinävesi");
    stationMap.put("HKI", "Helsinki");
    stationMap.put("HR", "Herrala");
    stationMap.put("HKH", "Hiekkaharju");
    stationMap.put("HK", "Hikiä");
    stationMap.put("HP", "Humppila");
    stationMap.put("HPL", "Huopalahti");
    stationMap.put("HY", "Hyvinkää");
    stationMap.put("HL", "Hämeenlinna");
    stationMap.put("ILM", "Iisalmi");
    stationMap.put("ITA", "Iittala");
    stationMap.put("ILA", "Ilmala");
    stationMap.put("IMR", "Imatra");
    stationMap.put("IKR", "Inkeroinen");
    stationMap.put("IKO", "Inkoo");
    stationMap.put("IKY", "Isokyrö");
    stationMap.put("JNS", "Joensuu");
    stationMap.put("JK", "Jokela");
    stationMap.put("JRS", "Jorvas");
    stationMap.put("JTS", "Joutseno");
    stationMap.put("JJ", "Juupajoki");
    stationMap.put("JY", "Jyväskylä");
    stationMap.put("JR", "Järvelä");
    stationMap.put("JP", "Järvenpää");
    stationMap.put("KAJ", "Kajaani");
    stationMap.put("KAN", "Kannelmäki");
    stationMap.put("KNS", "Kannus");
    stationMap.put("KR", "Karjaa");
    stationMap.put("KRU", "Karkku");
    stationMap.put("KHA", "Kauhava");
    stationMap.put("KLH", "Kauklahti");
    stationMap.put("KNI", "Kauniainen");
    stationMap.put("KA", "Kausala");
    stationMap.put("KEM", "Kemi");
    stationMap.put("KJÄ", "Kemijärvi");
    stationMap.put("KEA", "Kera");
    stationMap.put("KE", "Kerava");
    stationMap.put("KTI", "Kesälahti");
    stationMap.put("KEU", "Keuruu");
    stationMap.put("KIL", "Kilo");
    stationMap.put("KKN", "Kirkkonummi");
    stationMap.put("KIT", "Kitee");
    stationMap.put("KRV", "Kiuruvesi");
    stationMap.put("KOH", "Kohtavaara");
    stationMap.put("KVH", "Koivuhovi");
    stationMap.put("KVY", "Koivukylä");
    stationMap.put("KKI", "Kokemäki");
    stationMap.put("KOK", "Kokkola");
    stationMap.put("KLI", "Kolari");
    stationMap.put("KLO", "Kolho");
    stationMap.put("KON", "Kontiomäki");
    stationMap.put("KRA", "Koria");
    stationMap.put("KRS", "Korso");
    stationMap.put("KTA", "Kotka");
    stationMap.put("KTS", "Kotka satama");
    stationMap.put("KV", "Kouvola");
    stationMap.put("KUO", "Kuopio");
    stationMap.put("KUT", "Kupittaa");
    stationMap.put("KYN", "Kylänlahti");
    stationMap.put("KY", "Kymi");
    stationMap.put("KLN", "Kyminlinna");
    stationMap.put("LH", "Lahti");
    stationMap.put("LAI", "Laihia");
    stationMap.put("LNA", "Lapinlahti");
    stationMap.put("LR", "Lappeenranta");
    stationMap.put("LAA", "Lappila");
    stationMap.put("LPO", "Lappohja");
    stationMap.put("LPA", "Lapua");
    stationMap.put("LPV", "Leppävaara");
    stationMap.put("LIS", "Lieksa");
    stationMap.put("LVT", "Lievestuore");
    stationMap.put("LM", "Loimaa");
    stationMap.put("LOH", "Louhela");
    stationMap.put("LMA", "Luoma");
    stationMap.put("LUS", "Lusto");
    stationMap.put("ML", "Malmi");
    stationMap.put("MLO", "Malminkartano");
    stationMap.put("MNK", "Mankki");
    stationMap.put("MRL", "Martinlaakso");
    stationMap.put("MAS", "Masala");
    stationMap.put("MI", "Mikkeli");
    stationMap.put("MIS", "Misi");
    stationMap.put("MLA", "Mommila");
    stationMap.put("MH", "Muhos");
    stationMap.put("MUL", "Muurola");
    stationMap.put("MKI", "Myllykoski");
    stationMap.put("MY", "Myllymäki");
    stationMap.put("MYR", "Myyrmäki");
    stationMap.put("MR", "Mäntyharju");
    stationMap.put("NSL", "Nastola");
    stationMap.put("NVL", "Nivala");
    stationMap.put("NOA", "Nokia");
    stationMap.put("NUP", "Nuppulinna");
    stationMap.put("NRM", "Nurmes");
    stationMap.put("OI", "Oitti");
    stationMap.put("OV", "Orivesi");
    stationMap.put("OVK", "Orivesi keskusta");
    stationMap.put("OU", "Oulainen");
    stationMap.put("OL", "Oulu");
    stationMap.put("OLK", "Oulunkylä");
    stationMap.put("PTI", "Paimenportti");
    stationMap.put("PTO", "Paltamo");
    stationMap.put("PAR", "Parikkala");
    stationMap.put("PKO", "Parkano");
    stationMap.put("PRL", "Parola");
    stationMap.put("PSL", "Pasila");
    stationMap.put("PEL", "Pello");
    stationMap.put("PVI", "Petäjävesi");
    stationMap.put("PM", "Pieksämäki");
    stationMap.put("PH", "Pihlajavesi");
    stationMap.put("PJM", "Pitäjänmäki");
    stationMap.put("POH", "Pohjois-Haaga");
    stationMap.put("PRI", "Pori");
    stationMap.put("PLA", "Puistola");
    stationMap.put("PMK", "Pukinmäki");
    stationMap.put("PUN", "Punkaharju");
    stationMap.put("PUR", "Purola");
    stationMap.put("PKY", "Pääskylahti");
    stationMap.put("RKL", "Rekola");
    stationMap.put("REE", "Retretti");
    stationMap.put("RI", "Riihimäki");
    stationMap.put("ROI", "Rovaniemi");
    stationMap.put("RNN", "Runni");
    stationMap.put("RKI", "Ruukki");
    stationMap.put("RY", "Ryttylä");
    stationMap.put("SLO", "Salo");
    stationMap.put("STA", "Santala");
    stationMap.put("SAU", "Saunakallio");
    stationMap.put("SAV", "Savio");
    stationMap.put("SK", "Seinäjoki");
    stationMap.put("SIJ", "Siilinjärvi");
    stationMap.put("SPL", "Simpele");
    stationMap.put("STI", "Siuntio");
    stationMap.put("SGY", "Skogby");
    stationMap.put("SKV", "Sukeva");
    stationMap.put("SNJ", "Suonenjoki");
    stationMap.put("TMS", "Tammisaari");
    stationMap.put("TPE", "Tampere");
    stationMap.put("TNA", "Tapanila");
    stationMap.put("TSL", "Tavastila");
    stationMap.put("TK", "Tervajoki");
    stationMap.put("TRV", "Tervola");
    stationMap.put("TKL", "Tikkurila");
    stationMap.put("TL", "Toijala");
    stationMap.put("TOL", "Tolsa");
    stationMap.put("TRI", "Tornio itäinen");
    stationMap.put("TRL", "Tuomarila");
    stationMap.put("TU", "Turenki");
    stationMap.put("TKU", "Turku");
    stationMap.put("TUS", "Turku satama");
    stationMap.put("TUU", "Tuuri");
    stationMap.put("UIM", "Uimaharju");
    stationMap.put("UTJ", "Utajärvi");
    stationMap.put("VAA", "Vaala");
    stationMap.put("VS", "Vaasa");
    stationMap.put("VNA", "Vainikkala");
    stationMap.put("VMO", "Valimo");
    stationMap.put("VMA", "Vammala");
    stationMap.put("VKS", "Vantaankoski");
    stationMap.put("VAR", "Varkaus");
    stationMap.put("VTI", "Vihanti");
    stationMap.put("VIH", "Vihtari");
    stationMap.put("VIA", "Viiala");
    stationMap.put("VNJ", "Viinijärvi");
    stationMap.put("VLH", "Villähde");
    stationMap.put("VLP", "Vilppula");
    stationMap.put("VSL", "Vuonislahti");
    stationMap.put("YST", "Ylistaro");
    stationMap.put("YTR", "Ylitornio");
    stationMap.put("YV", "Ylivieska");
    stationMap.put("HOL", "Höljäkkä");
    stationMap.put("JAS", "Jämsä");
    stationMap.put("KJA", "Kemijärvi");
    stationMap.put("KIA", "Kerimäki");
    stationMap.put("KRO", "Kyrölä");
    stationMap.put("KAP", "Käpylä");
    stationMap.put("LPAE", "Lempäälä");
    stationMap.put("MAK", "Mäkkylä");
    stationMap.put("MLAE", "Mäntsälä");
    stationMap.put("PTS", "Pietarsaari");
    stationMap.put("PHA", "Pyhäsalmi");
    stationMap.put("PNA", "Pännäinen");
    stationMap.put("SLK", "Savonlinna");
    stationMap.put("UKA", "Uusikylä");
    stationMap.put("AHT", "Ähtäri");
    stationMap.put("MVA", "Moskova (Leningr.)");
    stationMap.put("PTL", "Pietari (Laatokka)");
    stationMap.put("PTR", "Pietari (Suomi)");
    stationMap.put("TVE", "Tver");
    stationMap.put("VYB", "Viipuri");
  }

}