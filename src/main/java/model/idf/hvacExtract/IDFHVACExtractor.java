package main.java.model.idf.hvacExtract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class IDFHVACExtractor {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public HVACZoneGroup extract(IDFFileObject idfFileObject, JsonObject res) {
        IDDParser iddParser = new IDDParser(idfFileObject.getVersion());
        iddParser.validateIDF(idfFileObject);

        // HVAC Zonings
        JsonObject zoningsJO = new JsonObject();

        HVACZoneGroup hvacZone = new HVACZoneGroup(idfFileObject);
        hvacZone.setIddParser(iddParser);

        JsonObject hvacGroup = hvacZone.getObjectsData();

        JsonArray systems = hvacGroup.getAsJsonArray("system");
        Map<String, JsonObject> idToControl = new HashMap<>();

        for (JsonElement system : systems) {
            JsonObject sys = (JsonObject) system;

            String sysType = sys.get("system_type").getAsString();
            String sysName = sys.get("name").getAsString();
            String sysId = sys.get("id").getAsString();

            if (!zoningsJO.has(sysType)) {
                zoningsJO.add(sysType, new JsonArray());
            }
            JsonArray ja = zoningsJO.get(sysType).getAsJsonArray();

            JsonObject controlJO = new JsonObject();
            controlJO.addProperty("name", sysName);
            controlJO.addProperty("type", "system");

            ja.add(controlJO);

            idToControl.put(sysId, controlJO);
        }

        JsonObject zoneBasedInfo = new JsonObject();

        JsonArray zones = hvacGroup.getAsJsonArray("zone");
        for (JsonElement jsonElement : zones) {
            JsonObject zone = (JsonObject) jsonElement;

            String zoneId = zone.get("id").isJsonNull() ? null : zone.get("id").getAsString();
            String parentSystemId = zone.get("parentsystemID").isJsonNull() ? null : zone.get("parentsystemID").getAsString();
            String zoneEqName = zone.get("name").getAsString();
            String zoneName = zone.get("zone").getAsString();

            if (zoneId != null && zoneId.equals(parentSystemId)) {
                //zone based
                String sysType = zone.get("properties").getAsJsonObject().get("name").getAsString();

                if (!zoningsJO.has(sysType)) {
                    zoningsJO.add(sysType, new JsonArray());
                }
                JsonArray ja = zoningsJO.get(sysType).getAsJsonArray();

                JsonObject controlJO = new JsonObject();
                controlJO.addProperty("name", zoneEqName);
                controlJO.addProperty("type", "zone");
                controlJO.addProperty("zones", zoneName);

                ja.add(controlJO);

                zoneBasedInfo.add(zoneEqName, zone.get("properties"));
            } else if (parentSystemId != null) {
                //system controlled

                JsonObject sysControlJo = idToControl.get(parentSystemId);
                if (sysControlJo == null) {
                    LOG.warn("No control JsonObject found for parent system id: " + parentSystemId);
                    continue;
                }
                if (sysControlJo.has("zones")) {
                    sysControlJo.addProperty("zones", sysControlJo.get("zones").getAsString() + "&" + zoneName);
                } else {
                    sysControlJo.addProperty("zones", zoneName);
                }
            }
        }

        res.add("hvac_zone_based_info", zoneBasedInfo);
        res.add("hvacZonings", zoningsJO);

        return hvacZone;
    }
}
