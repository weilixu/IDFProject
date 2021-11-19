package main.java.model.geometry;

import com.google.gson.JsonObject;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZoneTypeManager {
    public static Map<String, JsonObject> readZoneTypes(IDFFileObject idfFileObj) {
        Map<String, JsonObject> res = new HashMap<>();

        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones != null) {
            for (IDFObject zone : zones) {
                String zoneName = zone.getName();
                String zoneType = zone.getKeyedExtraInfo("zone_type");
                String zoneCategory = zone.getKeyedExtraInfo("zone_category");

                JsonObject jo = new JsonObject();
                jo.addProperty("type", zoneType);
                jo.addProperty("category", zoneCategory);
                res.put(zoneName, jo);
            }
        }

        return res;
    }

    public static void setZoneTypes(IDFFileObject idfFileObj, Map<String, JsonObject> zoneTypes) {
        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones != null) {
            for (IDFObject zone : zones) {
                String zoneName = zone.getName();
                JsonObject zoneInfo = zoneTypes.get(zoneName);

                if (zoneInfo != null && !zoneInfo.isJsonNull()) {
                    zone.writeKeyedExtraInfo("zone_type", zoneInfo.get("type").getAsString());
                    zone.writeKeyedExtraInfo("zone_category", zoneInfo.get("category").getAsString());
                }
            }
        }
    }
}
