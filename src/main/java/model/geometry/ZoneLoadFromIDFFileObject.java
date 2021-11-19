package main.java.model.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import main.java.config.ServerConfig;
import main.java.db.vc.IdfVersionControlDAOTestImpl;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFParser;
import main.java.model.idf.html.HTMLProcessor;
import main.java.model.idf.simRes.SimulationResultType;
import main.java.report.templateReport.ModelResultsUtility;
import main.java.util.NumUtil;
import main.java.util.StringUtil;
import main.java.util.VisualUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZoneLoadFromIDFFileObject {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        File idf = new File("/Users/weilixu/Desktop/data/test.idf");
        File html = new File("/Users/weilixu/Desktop/data/testTable.html");

        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");

        Document doc = null;
        try {
            doc = Jsoup.parse(html, null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HTMLProcessor.processHTML(doc);

        IDFFileObject idfFileObj = new IDFFileObject();
        IDFParser parser = new IDFParser();
        parser.parseIDFFromIDFFile(idf, idfFileObj);
        IDDParser idd = new IDDParser(idfFileObj.getVersion());
        idd.validateIDF(idfFileObj);

        ZoneLoadFromIDFFileObject zone = new ZoneLoadFromIDFFileObject();
        JsonObject jo = new JsonObject();
        JsonObject result = zone.extractBuildingComponentLoadsAPI(jo, idfFileObj, doc);
        System.out.println(result);
    }

    public void extractZoneLoads(JsonObject jo, IDFFileObject idfFileObj, String commitId, String type) {
        if (type.equals("hierarchy")) {
            extractForHierarchy(jo, idfFileObj, commitId);
        }
    }

    @SuppressWarnings("unused")
    public JsonArray extractZoneLoadsAPI(JsonObject jo, IDFFileObject idfFileObject, String commitId, String zoneName, String loadItem, String loadType) {
        Document doc = getHTMLDoc(commitId, jo);
        if (doc == null) {
            return null;
        }

        String report = "ZoneComponentLoadSummary";
        String forCat = zoneName.trim().replaceAll("\\W", "");
        return null;
    }

    public JsonArray extractZoneLoadsAPI(JsonObject jo, IDFFileObject idfFileObj, String commitId, String zoneName) {
        Document doc = getHTMLDoc(commitId, jo);
        if (doc == null) {
            return null;
        }

        JsonArray ja = new JsonArray();

        //Map<String, String> units = new HashMap<>();

        JsonObject zoneJO = new JsonObject();
        zoneJO.addProperty("zone_name", zoneName);
        ja.add(zoneJO);

        JsonObject zoneLoadJS = ModelResultsUtility.processAZoneLoadInJson(doc, zoneName);
        JsonObject result = ModelResultsUtility.getZoneFloorArea(doc, zoneName);

        if (zoneLoadJS == null) {
            return ja;
        }

        if (result == null) {
            return ja;
        }

        Double zoneFloorArea = result.get("value").getAsDouble();
        String faUnit = result.get("unit").getAsString();

        zoneJO.addProperty("floor_area", zoneFloorArea);
        zoneJO.addProperty("floor_area_unit", faUnit);

        //zoneJO.addProperty("heating_unit", units.get("heatunit"));
        //zoneJO.addProperty("cooling_unit", units.get("coolunit"));
        zoneJO.add("data", zoneLoadJS);

        JsonObject peakCondition = ModelResultsUtility.processAZoneLoadPeakCondition(doc, zoneName);
        if (peakCondition != null) {
            if (peakCondition.has("cooling_peak_load_time")
                    && !peakCondition.get("cooling_peak_load_time").isJsonNull()) {
                zoneJO.addProperty("cooling_peak_load_time",
                        peakCondition.get("cooling_peak_load_time").getAsString());
            }
            if (peakCondition.has("heating_peak_load_time")
                    && !peakCondition.get("heating_peak_load_time").isJsonNull()) {
                zoneJO.addProperty("heating_peak_load_time",
                        peakCondition.get("heating_peak_load_time").getAsString());
            }
        }
        return ja;
    }

    /**
     * This is for testing purpose only
     *
     * @param jo
     * @param idfFileObj
     * @param doc
     * @return
     */
    public JsonObject extractBuildingComponentLoadsAPI(JsonObject jo, IDFFileObject idfFileObj, Document doc) {
        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones == null || zones.isEmpty()) {
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "No zones defined in this model");
        }
        List<String> zoneNames = new ArrayList<String>();
        for (IDFObject zone : zones) {
            String zoneName = zone.getName();
            if (!zoneName.toLowerCase().contains("plenum")) {
                zoneNames.add(zoneName);
            }
        }

        Double floorArea = 0.0;
        String floorAreaUnit = "m2";

        String firstZoneName = zoneNames.get(0);
        JsonObject totalZoneLoad = ModelResultsUtility.processAZoneLoadInJson(doc, firstZoneName);

        for (int i = 1; i < zoneNames.size(); i++) {
            String zoneName = zoneNames.get(i);
            JsonObject zoneLoadJS = ModelResultsUtility.processAZoneLoadInJson(doc, zoneName);
            JsonObject result = ModelResultsUtility.getZoneFloorArea(doc, zoneName);

            if (zoneLoadJS == null) {
                continue;
            }

            if (result == null) {
                continue;
            }

            floorArea += result.get("value").getAsDouble();
            floorAreaUnit = result.get("unit").getAsString();

            JsonArray coolingComponents = zoneLoadJS.get("cooling").getAsJsonArray();
            JsonArray heatingComponents = zoneLoadJS.get("heating").getAsJsonArray();

            JsonArray totalCoolingComponents = totalZoneLoad.get("cooling").getAsJsonArray();
            JsonArray totalHeatingComponents = totalZoneLoad.get("heating").getAsJsonArray();

            int len = coolingComponents.size();
            for (int j = 0; j < len; j++) {
                /*
                 * {'load_component': 'People', 'Sensible - Instant': 1830.75,
                 * 'Sensible - Delayed': 881.34, 'Sensible - Return Air': 0.0,
                 * 'Latent': 1784.89, 'Total': 4496.98, '%Grand Total': 35.33,
                 * 'Related Area': 1067.45, 'Total per Area': 14.37}
                 */
                JsonObject coolComp = coolingComponents.get(j).getAsJsonObject();
                double coolSensibleInstant = coolComp.get("Sensible - Instant").getAsDouble();
                double coolSensibleDelayed = coolComp.get("Sensible - Delayed").getAsDouble();
                double coolSensibleReturnAir = coolComp.get("Sensible - Return Air").getAsDouble();
                double coolLatent = coolComp.get("Latent").getAsDouble();
                double coolTotal = coolComp.get("Total").getAsDouble();

                JsonObject totalCoolComp = totalCoolingComponents.get(j).getAsJsonObject();
                totalCoolComp.addProperty("Sensible - Instant", totalCoolComp.get("Sensible - Instant").getAsDouble() + coolSensibleInstant);
                totalCoolComp.addProperty("Sensible - Delayed", totalCoolComp.get("Sensible - Delayed").getAsDouble() + coolSensibleDelayed);
                totalCoolComp.addProperty("Sensible - Return Air", totalCoolComp.get("Sensible - Return Air").getAsDouble() + coolSensibleReturnAir);
                totalCoolComp.addProperty("Latent", totalCoolComp.get("Latent").getAsDouble() + coolLatent);
                totalCoolComp.addProperty("Total", totalCoolComp.get("Total").getAsDouble() + coolTotal);

                //heating
                JsonObject heatComp = heatingComponents.get(j).getAsJsonObject();
                double heatSensibleInstant = heatComp.get("Sensible - Instant").getAsDouble();
                double heatSensibleDelayed = heatComp.get("Sensible - Delayed").getAsDouble();
                double heatSensibleReturnAir = heatComp.get("Sensible - Return Air").getAsDouble();
                double heatLatent = heatComp.get("Latent").getAsDouble();
                double heatTotal = heatComp.get("Total").getAsDouble();

                JsonObject totalHeatComp = totalHeatingComponents.get(j).getAsJsonObject();
                totalHeatComp.addProperty("Sensible - Instant", totalHeatComp.get("Sensible - Instant").getAsDouble() + heatSensibleInstant);
                totalHeatComp.addProperty("Sensible - Delayed", totalHeatComp.get("Sensible - Delayed").getAsDouble() + heatSensibleDelayed);
                totalHeatComp.addProperty("Sensible - Return Air", totalHeatComp.get("Sensible - Return Air").getAsDouble() + heatSensibleReturnAir);
                totalHeatComp.addProperty("Latent", totalHeatComp.get("Latent").getAsDouble() + heatLatent);
                totalHeatComp.addProperty("Total", totalHeatComp.get("Total").getAsDouble() + heatTotal);
            }
        }

        JsonObject data = new JsonObject();

        data.addProperty("floor_area", floorArea);
        data.addProperty("floor_area_unit", floorAreaUnit);
        data.add("data", totalZoneLoad);

        return totalZoneLoad;
    }

    public JsonObject extractBuildingComponentLoadsAPI(JsonObject jo, IDFFileObject idfFileObj, String commitId) {
        Document doc = getHTMLDoc(commitId, jo);
        if (doc == null) {
            return null;
        }

        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones == null || zones.isEmpty()) {
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "No zones defined in this model");
        }
        List<String> zoneNames = new ArrayList<String>();
        for (IDFObject zone : zones) {
            String zoneName = zone.getName();
            if (!zoneName.toLowerCase().contains("plenum")
                    && !zoneName.toLowerCase().contains("attic")) {
                zoneNames.add(zoneName);
            }
        }

        Double floorArea = 0.0;
        String floorAreaUnit = "m2";

        String firstZoneName = zoneNames.get(0);
        LOG.debug("First zone name: " + firstZoneName);
        JsonObject totalZoneLoad = ModelResultsUtility.processAZoneLoadInJson(doc, firstZoneName);

        for (int i = 1; i < zoneNames.size(); i++) {
            String zoneName = zoneNames.get(i);
            JsonObject zoneLoadJS = ModelResultsUtility.processAZoneLoadInJson(doc, zoneName);
            JsonObject result = ModelResultsUtility.getZoneFloorArea(doc, zoneName);

            if (zoneLoadJS == null) {
                continue;
            }

			floorArea += result.get("value").getAsDouble();
            floorAreaUnit = result.get("unit").getAsString();

            JsonArray coolingComponents = zoneLoadJS.get("cooling").getAsJsonArray();
            JsonArray heatingComponents = zoneLoadJS.get("heating").getAsJsonArray();

            JsonArray totalCoolingComponents = totalZoneLoad.get("cooling").getAsJsonArray();
            JsonArray totalHeatingComponents = totalZoneLoad.get("heating").getAsJsonArray();

            int len = coolingComponents.size();
            for (int j = 0; j < len; j++) {
                /*
                 * {'load_component': 'People', 'Sensible - Instant': 1830.75,
                 * 'Sensible - Delayed': 881.34, 'Sensible - Return Air': 0.0,
                 * 'Latent': 1784.89, 'Total': 4496.98, '%Grand Total': 35.33,
                 * 'Related Area': 1067.45, 'Total per Area': 14.37}
                 */
                JsonObject coolComp = coolingComponents.get(j).getAsJsonObject();
                double coolSensibleInstant = coolComp.get("Sensible - Instant").getAsDouble();
                double coolSensibleDelayed = coolComp.get("Sensible - Delayed").getAsDouble();
                double coolSensibleReturnAir = coolComp.get("Sensible - Return Air").getAsDouble();
                double coolLatent = coolComp.get("Latent").getAsDouble();
                double coolTotal = coolComp.get("Total").getAsDouble();

                JsonObject totalCoolComp = totalCoolingComponents.get(j).getAsJsonObject();
                totalCoolComp.addProperty("Sensible - Instant", totalCoolComp.get("Sensible - Instant").getAsDouble() + coolSensibleInstant);
                totalCoolComp.addProperty("Sensible - Delayed", totalCoolComp.get("Sensible - Delayed").getAsDouble() + coolSensibleDelayed);
                totalCoolComp.addProperty("Sensible - Return Air", totalCoolComp.get("Sensible - Return Air").getAsDouble() + coolSensibleReturnAir);
                totalCoolComp.addProperty("Latent", totalCoolComp.get("Latent").getAsDouble() + coolLatent);
                totalCoolComp.addProperty("Total", totalCoolComp.get("Total").getAsDouble() + coolTotal);

                //heating
                JsonObject heatComp = heatingComponents.get(j).getAsJsonObject();
                double heatSensibleInstant = heatComp.get("Sensible - Instant").getAsDouble();
                double heatSensibleDelayed = heatComp.get("Sensible - Delayed").getAsDouble();
                double heatSensibleReturnAir = heatComp.get("Sensible - Return Air").getAsDouble();
                double heatLatent = heatComp.get("Latent").getAsDouble();
                double heatTotal = heatComp.get("Total").getAsDouble();

                JsonObject totalHeatComp = totalHeatingComponents.get(j).getAsJsonObject();
                totalHeatComp.addProperty("Sensible - Instant", heatComp.get("Sensible - Instant").getAsDouble() + heatSensibleInstant);
                totalHeatComp.addProperty("Sensible - Delayed", heatComp.get("Sensible - Delayed").getAsDouble() + heatSensibleDelayed);
                totalHeatComp.addProperty("Sensible - Return Air", heatComp.get("Sensible - Return Air").getAsDouble() + heatSensibleReturnAir);
                totalHeatComp.addProperty("Latent", heatComp.get("Latent").getAsDouble() + heatLatent);
                totalHeatComp.addProperty("Total", heatComp.get("Total").getAsDouble() + heatTotal);
            }
        }

        JsonObject data = new JsonObject();

        data.addProperty("floor_area", floorArea);
        data.addProperty("floor_area_unit", floorAreaUnit);
        data.add("data", totalZoneLoad);

        return totalZoneLoad;
    }

    public JsonObject extractBuildingLoadsAPI(JsonObject jo, IDFFileObject idfFileObj, String commitId) {
        Document doc = getHTMLDoc(commitId, jo);
        if (doc == null) {
            return null;
        }

        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones == null || zones.isEmpty()) {
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "No zones defined in this model");
        }
        List<String> zoneNames = new ArrayList<String>();
        for (IDFObject zone : zones) {
            String zoneName = zone.getName();
            if (!zoneName.toLowerCase().contains("plenum")) {
                zoneNames.add(zoneName);
            }
        }

        JsonObject bldgLoad = new JsonObject();
        Map<String, String> units = new HashMap<>();
        double totalHeatingLoad = 0.0;
        double totalCoolingLoad = 0.0;
        double totalConditionedFloorArea = 0.0;
        String faUnit = "";

        for (String zoneName : zoneNames) {
            Map<String, Double> res = ModelResultsUtility.processAZoneLoad(doc, zoneName, units);
            JsonObject result = ModelResultsUtility.getZoneFloorArea(doc, zoneName);

            Double zoneFloorArea = result.get("value").getAsDouble();
            totalConditionedFloorArea += zoneFloorArea;

            faUnit = result.get("unit").getAsString();

            if (res == null) {
                continue;
            }

            double heat = 0D;
            double cool = 0D;
            for (String name : res.keySet()) {
                if (name.startsWith("heat")) {
                    heat += res.get(name).doubleValue();
                } else if (name.startsWith("cool")) {
                    cool += res.get(name).doubleValue();
                }
            }

            totalHeatingLoad += heat;
            totalCoolingLoad += cool;

        }

        bldgLoad.addProperty("heating_load", totalHeatingLoad);
        bldgLoad.addProperty("cooling_load", totalCoolingLoad);
        bldgLoad.addProperty("heating_load_density", totalHeatingLoad / totalConditionedFloorArea);
        bldgLoad.addProperty("cooling_load_density", totalHeatingLoad / totalConditionedFloorArea);
        bldgLoad.addProperty("heating_unit", units.get("heatunit"));
        bldgLoad.addProperty("cooling_unit", units.get("coolunit"));
        bldgLoad.addProperty("heating_load_density_unit", units.get("heatunit") + "/" + faUnit);
        bldgLoad.addProperty("heating_load_density_unit", units.get("heatunit") + "/" + faUnit);

        return bldgLoad;

    }

    public JsonArray extractZoneLoadsAPI(JsonObject jo, IDFFileObject idfFileObj, Document doc) {
        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones == null || zones.isEmpty()) {
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "No zones defined in this model");
            return null;
        }
        List<String> zoneNames = new ArrayList<String>();
        for (IDFObject zone : zones) {
            String zoneName = zone.getName();
            if (!zoneName.toLowerCase().contains("plenum")
                    && !zoneName.toLowerCase().contains("attic")) {
                zoneNames.add(zoneName);
            }
        }

        JsonArray ja = new JsonArray();

        Map<String, String> units = new HashMap<>();
        for (String zoneName : zoneNames) {
            JsonObject zoneJO = new JsonObject();
            zoneJO.addProperty("zone_name", zoneName);

            Map<String, Double> res = ModelResultsUtility.processAZoneLoad(doc, zoneName, units);
            JsonObject result = ModelResultsUtility.getZoneFloorArea(doc, zoneName);

            Double zoneFloorArea = result.get("value").getAsDouble();
            String faUnit = result.get("unit").getAsString();

            if (res == null) {
                continue;
            }

            ja.add(zoneJO);
            zoneJO.addProperty("heating_unit", units.get("heatunit"));
            zoneJO.addProperty("cooling_unit", units.get("coolunit"));

            double heat = 0D;
            double cool = 0D;
            for (String name : res.keySet()) {
                if (name.startsWith("heat")) {
                    heat += res.get(name).doubleValue();
                } else if (name.startsWith("cool")) {
                    cool += res.get(name).doubleValue();
                }
            }
            zoneJO.addProperty("heating_load", heat);
            zoneJO.addProperty("cooling_load", cool);
            zoneJO.addProperty("heating_load_density", heat / zoneFloorArea);
            zoneJO.addProperty("cooling_load_density", cool / zoneFloorArea);
            zoneJO.addProperty("heating_load_density_unit", units.get("heatunit") + "/" + faUnit);
            zoneJO.addProperty("cooling_load_density_unit", units.get("coolunit") + "/" + faUnit);

            JsonObject peakCondition = ModelResultsUtility.processAZoneLoadPeakCondition(doc, zoneName);
            if (peakCondition != null) {
                if (peakCondition.has("cooling_peak_load_time")
                        && !peakCondition.get("cooling_peak_load_time").isJsonNull()) {
                    zoneJO.addProperty("cooling_peak_load_time",
                            peakCondition.get("cooling_peak_load_time").getAsString());
                }
                if (peakCondition.has("heating_peak_load_time")
                        && !peakCondition.get("heating_peak_load_time").isJsonNull()) {
                    zoneJO.addProperty("heating_peak_load_time",
                            peakCondition.get("heating_peak_load_time").getAsString());
                }
            }
        }

        return ja;
    }

    public JsonArray extractZoneLoadsAPI(JsonObject jo, IDFFileObject idfFileObj, String commitId) {
        Document doc = getHTMLDoc(commitId, jo);
        if (doc == null) {
            return null;
        }

        return extractZoneLoadsAPI(jo, idfFileObj, doc);
    }

    private Document getHTMLDoc(String commitId, JsonObject jo) {
        // try to get load simulation result
        String contents = IdfVersionControlDAOTestImpl.getInstance().retrieveCommitSimulationResult(commitId,
                SimulationResultType.HTML, null);
        if (StringUtil.isNullOrEmpty(contents)) {
            String[] returnContents = IdfVersionControlDAOTestImpl.getInstance().retrieveLoadSimulationResult(commitId);

            if (returnContents == null) {
                jo.addProperty("status", "error");
                jo.addProperty("error_msg", "Zone Load Simulation does not run.");
                return null;
            }

            if (returnContents[0] == null || returnContents[0].isEmpty()) {
                if (returnContents[1] == null || returnContents[1].isEmpty()) {
                    jo.addProperty("status", "error");
                    jo.addProperty("error_msg", "Zone Load Simulation is still undergoing.");
                    return null;
                }

                jo.addProperty("status", "error");
                jo.addProperty("error_msg",
                        "Zone Load Simulation encounters error, please run simulation to see the error");
                return null;
            }
            contents = returnContents[0];
        }

        Document doc = Jsoup.parse(contents);
        HTMLProcessor.processHTML(doc);

        return doc;
    }

    private void extractForHierarchy(JsonObject jo, IDFFileObject idfFileObj, String commitId) {
        Document doc = getHTMLDoc(commitId, jo);
        if (doc == null) {
            return;
        }

        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if (zones == null || zones.isEmpty()) {
            jo.addProperty("status", "error");
            jo.addProperty("error_msg", "No zones defined in this model");
            return;
        }

        List<String> zoneNames = new ArrayList<>();
        for (IDFObject zone : zones) {
            String zoneName = zone.getName();
            if (!zoneName.toLowerCase().contains("plenum")) {
                zoneNames.add(zoneName);
            }
        }
        JsonObject heating = new JsonObject();
        JsonObject cooling = new JsonObject();

        double heatMax = NumUtil.MIN_VALUE, heatMin = NumUtil.MAX_VALUE;
        double coolMax = NumUtil.MIN_VALUE, coolMin = NumUtil.MAX_VALUE;
        Map<String, String> units = new HashMap<>();

        for (String zoneName : zoneNames) {
            Map<String, Double> res = ModelResultsUtility.processAZoneLoad(doc, zoneName, units);
            if (res == null) {
                continue;
            }

            double heat = 0D;
            double cool = 0D;
            for (String name : res.keySet()) {
                if (name.startsWith("heat")) {
                    heat += res.get(name);
                } else if (name.startsWith("cool")) {
                    cool += res.get(name);
                }
            }
            heating.addProperty(zoneName, heat);
            if (heatMax < heat) {
                heatMax = heat;
            }
            if (heatMin > heat) {
                heatMin = heat;
            }

            cooling.addProperty(zoneName, cool);
            if (coolMax < cool) {
                coolMax = cool;
            }
            if (coolMin > cool) {
                coolMin = cool;
            }
        }

        JsonObject zoneJO = new JsonObject();
        JsonArray zoneArray = new JsonArray();

        for (String zoneName : zoneNames) {
            JsonObject zoneObj = new JsonObject();
            zoneObj.addProperty("name", zoneName);

            JsonElement loadEle = heating.get(zoneName);
            if (loadEle == null) {
                continue;
            }
            double load = loadEle.getAsDouble();
            JsonObject heatObj = new JsonObject();
            heatObj.addProperty("value", load);
            heatObj.addProperty("unit", units.get("heatunit"));
            zoneObj.add("heating_load", heatObj);
            jo.addProperty("heating_unit", units.get("heatunit"));

            // reverse color logic for heading load
            heating.addProperty(zoneName + "_color", VisualUtil.makeColorReverse(load, heatMin, heatMax));

            load = cooling.get(zoneName).getAsDouble();
            JsonObject coolObj = new JsonObject();
            coolObj.addProperty("value", load);
            coolObj.addProperty("unit", units.get("coolunit"));
            zoneObj.add("cooling_load", coolObj);
            jo.addProperty("cooling_unit", units.get("coolunit"));

            // color logic for cooling load
            cooling.addProperty(zoneName + "_color", VisualUtil.makeColor(load, coolMin, coolMax));

            ModelResultsUtility.addZoneInfoForGeometryDisplay(zoneObj, doc);
            zoneArray.add(zoneObj);
        }

        /**
         * Zone data map: (key - data) zone_info: data - zoneObjs zoneObj:
         *
         * name -> zoneName heating_load -> heaing_loadObj: value, unit cooling_load ->
         * cooling_loadObj: value, unit area -> area obj: value, unit lighting->
         * lighting obj: value, unit people-> people obj: value, unit equipment->
         * equipment obj: value, unit windowarea -> window area obj: value, unit
         * abovewallarea -> above ground gross wall area obj: value, unit
         * undergroundwallarea -> underground gross wall area obj: value, unit
         * conditioned -> conditioned: value, unit
         *
         * Below are values that may or may not exist in the load calculations if their
         * value is not exist in the load calculation, replace with: Not available
         * heatFromZoneHVAC -> sensible heating energy from zone-based HVAC obj: value,
         * unit coolFromZoneHVAC -> sensible cooling energy from zone-based HVAC obj:
         * value, unit heatFromCentralHVAC -> sensible heating energy from central HVAC
         * obj: value, unit coolFromCentralHVAC -> sensible cooling energy from central
         * HVAC obj: value, unit operationhour -> zone operation hours obj: value, unit
         * coolsupplyair -> cooling supply air flow rate obj: value, unit heatsupplyair
         * -> heating supply air flow rate obj: value, unit
         */

        zoneJO.add("data", zoneArray);

        jo.add("heating_load", heating);
        jo.add("cooling_load", cooling);
        jo.add("zone_info", zoneJO);
        jo.addProperty("heating_min", ((int) (heatMin * 100)) / 100D);
        jo.addProperty("heating_max", ((int) (heatMax * 100)) / 100D);
        jo.addProperty("cooling_min", ((int) (coolMin * 100)) / 100D);
        jo.addProperty("cooling_max", ((int) (coolMax * 100)) / 100D);
        jo.addProperty("status", "success");
    }
}
