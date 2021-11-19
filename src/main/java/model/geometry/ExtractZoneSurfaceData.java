package main.java.model.geometry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import main.java.model.idf.IDFFileObject;
import main.java.util.JsonUtil;
import main.java.util.NumUtil;
import main.java.util.StringUtil;
import main.java.util.VisualUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExtractZoneSurfaceData {
    private static JsonObject processZoneRowTableData(String zoneCol, String dataCol, JsonArray table, boolean hasUnit){
        JsonObject data = new JsonObject();
        String unit = "";
        double max = NumUtil.MIN_VALUE;
        double min = NumUtil.MAX_VALUE;

        Map<String, String> zoneNameMap = new HashMap<>();
        Set<String> zones = new HashSet<>();
        for(int i=0;i<table.size();i++) {
            JsonObject row = table.get(i).getAsJsonObject();
            String rowCol = JsonUtil.readValue(row, "col", "");

            if (rowCol.equals(zoneCol)) {
                String rowIdx = JsonUtil.readValue(row, "row", "1");
                String zone = JsonUtil.readValue(row, "value", "Zone Name NA");
                zoneNameMap.put(rowIdx, zone);
                zones.add(zone);
            }
        }

        for(int i=0;i<table.size();i++) {
            JsonObject row = table.get(i).getAsJsonObject();
            String rowCol = JsonUtil.readValue(row, "col", "");

            if (rowCol.startsWith(dataCol)) {
                if(hasUnit) {
                    unit = rowCol.substring(rowCol.indexOf("{") + 1, rowCol.indexOf("}"));
                }

                String rowIdx = JsonUtil.readValue(row, "row", "1");
                double value = NumUtil.readDouble(JsonUtil.readValue(row, "value", "0"), 0d);

                if (max < value) {
                    max = value;
                }
                if (min > value) {
                    min = value;
                }

                data.addProperty(zoneNameMap.get(rowIdx), value);
            }
        }

        for(String zone : zones){
            double value = data.get(zone).getAsDouble();
            data.addProperty(zone+"_color",VisualUtil.makeColor(value, min, max));
        }

        JsonObject res = new JsonObject();
        res.addProperty("unit", unit);
        res.addProperty("min", min);
        res.addProperty("max", max);
        res.add("data", data);
        return res;
    }

    private static JsonObject processZoneTableData(String col, JsonArray table, boolean isReverse){
        JsonObject data = new JsonObject();
        String unit = "";
        double max = NumUtil.MIN_VALUE;
        double min = NumUtil.MAX_VALUE;

        Set<String> zones = new HashSet<>();
        for(int i=0;i<table.size();i++){
            JsonObject row = table.get(i).getAsJsonObject();
            String rowCol = JsonUtil.readValue(row, "col", "");

            if(rowCol.equals(col)){
                String zone = JsonUtil.readValue(row, "row", "");
                if(StringUtil.isNullOrEmpty(zone)){
                    continue;
                }

                if(zone.toLowerCase().contains("plenum")
                        || zone.toLowerCase().contains("total")
                        || zone.equalsIgnoreCase("facility")){
                    continue;
                }

                zones.add(zone);

                double value = NumUtil.readDouble(JsonUtil.readValue(row, "value", "0"), 0d);
                if (max < value) {
                    max = value;
                }
                if (min > value) {
                    min = value;
                }

                data.addProperty(zone, value);

                unit = JsonUtil.readValue(row, "unit", "");
            }
        }

        for(String zone : zones){
            double value = data.get(zone).getAsDouble();
            data.addProperty(zone+"_color",
                    isReverse ? VisualUtil.makeColorReverse(value, min, max) : VisualUtil.makeColor(value, min, max));
        }

        JsonObject res = new JsonObject();
        res.addProperty("unit", unit);
        res.addProperty("min", min);
        res.addProperty("max", max);
        res.add("data", data);
        return res;
    }

//    public static JsonObject getZoneColoredData(String target, IDFFileObject idfObj, String commitId){
//        JsonObject res = new JsonObject();
//
//        Document simRes = null;
//        String htmlContent = IdfVersionControlDAOTestImpl.getInstance().retrieveCommitSimulationResult(commitId,
//                SimulationResultType.HTML, null);
//        if(!StringUtil.isNullOrEmpty(htmlContent)){
//            simRes = Jsoup.parse(htmlContent);
//        }
//        DataSourceCache cache = new DataSourceCache();
//
//        switch (target){
//            case "zone_heat_load":
//            case "zone_cool_load":
//                ZoneLoadFromIDFFileObject zl = new ZoneLoadFromIDFFileObject();
//                JsonArray zoneLoads = zl.extractZoneLoadsAPI(res, idfObj, commitId);
//
//                JsonObject loads = new JsonObject();
//                String unit = "";
//                double min = NumUtil.MAX_VALUE;
//                double max = NumUtil.MIN_VALUE;
//
//                if(target.equals("zone_heat_load")){
//                    for(int i=0;i<zoneLoads.size();i++){
//                        JsonObject load = zoneLoads.get(i).getAsJsonObject();
//                        double value = load.get("heating_load").getAsDouble();
//
//                        loads.addProperty(load.get("zone_name").getAsString().toUpperCase(), value);
//                        unit = load.get("heating_unit").getAsString();
//
//                        if(min>value){
//                            min=value;
//                        }
//                        if(max<value){
//                            max=value;
//                        }
//                    }
//                }else if(target.equals("zone_cool_load")){
//                    for(int i=0;i<zoneLoads.size();i++){
//                        JsonObject load = zoneLoads.get(i).getAsJsonObject();
//                        double value = load.get("cooling_load").getAsDouble();
//
//                        loads.addProperty(load.get("zone_name").getAsString().toUpperCase(), value);
//                        unit = load.get("cooling_unit").getAsString();
//
//                        if(min>value){
//                            min=value;
//                        }
//                        if(max<value){
//                            max=value;
//                        }
//                    }
//                }
//
//                Map<String, String> colors = new HashMap<>();
//                for(Map.Entry<String, JsonElement> entry : loads.entrySet()){
//                    double value = entry.getValue().getAsDouble();
//                    if(target.equals("zone_heat_load")){
//                        colors.put(entry.getKey()+"_color", VisualUtil.makeColorReverse(value, min, max));
//                    }else if(target.equals("zone_cool_load")){
//                        colors.put(entry.getKey()+"_color", VisualUtil.makeColor(value, min, max));
//                    }
//                }
//
//                for(String zone : colors.keySet()){
//                    loads.addProperty(zone, colors.get(zone));
//                }
//
//                res.add("data", loads);
//                res.addProperty("unit", unit);
//
//                if(target.equals("zone_heat_load")){
//                    res.addProperty("min", max);
//                    res.addProperty("max", min);
//                }else if(target.equals("zone_cool_load")){
//                    res.addProperty("min", min);
//                    res.addProperty("max", max);
//                }
//
//                break;
//            case "zone_heat_not_met":
//            case "zone_cool_not_met":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SystemSummary:EntireFacility:TimeSetpointNotMet");
//                    if(ov==null){
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    }else {
//                        if(target.equals("zone_heat_not_met")){
//                            res = processZoneTableData("During Heating", ov.getJsonObjects(), false);
//                        }else if(target.equals("zone_cool_not_met")){
//                            res = processZoneTableData("During Cooling", ov.getJsonObjects(), false);
//                        }
//                    }
//                }
//                break;
//
//            case "zone_not_comfort":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SystemSummary:EntireFacility:TimeNotComfortableBasedonSimpleASHRAE552004");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Summer or Winter Clothes", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_people":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "InputVerificationandResultsSummary:EntireFacility:ZoneSummary");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("People", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_lighting":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "InputVerificationandResultsSummary:EntireFacility:ZoneSummary");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Lighting", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_epd":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "InputVerificationandResultsSummary:EntireFacility:ZoneSummary");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Plug and Process", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_design_volume_flow":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "InitializationSummary:EntireFacility:ZoneInfiltrationAirflowStatsNominal");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneRowTableData("Zone Name","Design Volume Flow Rate", ov.getJsonObjects(), true);
//                    }
//                }
//                break;
//
//            case "zone_ach":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "InitializationSummary:EntireFacility:ZoneInfiltrationAirflowStatsNominal");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneRowTableData("Zone Name","ACH - Air Changes per Hour", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_sensible_air_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Sensible Air Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_sensible_air_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Sensible Air Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_sensible_air_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Zone Eq & Other Sensible Air Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_sensible_air_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Zone Eq & Other Sensible Air Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_terminal_air_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Terminal Unit Sensible Air Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_terminal_air_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Terminal Unit Sensible Air Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_heated_surf_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Heated Surface Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_cooled_surf_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Cooled Surface Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_people_sensible_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("People Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_light_sensible_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Lights Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_eq_sensible_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Equipment Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_win_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Window Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_inter_air_transfer_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Interzone Air Transfer Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_infiltration_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Infiltration Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_opaque_surf_conduct_other_heat_add":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Opaque Surface Conduction and Other Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_eq_sensible_heat_remove":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Equipment Sensible Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_win_heat_remove":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Window Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_inter_air_transfer_heat_remove":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Interzone Air Transfer Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_infiltration_heat_remove":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Infiltration Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_opaque_surf_conduct_other_heat_remove":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:AnnualBuildingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Opaque Surface Conduction and Other Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_sensible_air_heat_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Sensible Air Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_sensible_air_cool_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Sensible Air Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_sensible_air_heat_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Zone Eq & Other Sensible Air Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_sensible_air_cool_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Zone Eq & Other Sensible Air Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_terminal_air_heat_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Terminal Unit Sensible Air Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_eq_terminal_air_cool_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Terminal Unit Sensible Air Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_heated_surf_heat_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Heated Surface Heating", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_hvac_input_cooled_surf_cool_peak":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("HVAC Input Cooled Surface Cooling", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_people_sensible_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("People Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_light_sensible_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Lights Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_eq_sensible_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Equipment Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_win_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Window Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_inter_air_transfer_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Interzone Air Transfer Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_infiltration_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Infiltration Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_opaque_surf_conduct_other_heat_add_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Opaque Surface Conduction and Other Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_eq_sensible_heat_remove_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Equipment Sensible Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_win_heat_remove_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Window Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_inter_air_transfer_heat_remove_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Interzone Air Transfer Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_infiltration_heat_remove_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Infiltration Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_opaque_surf_conduct_other_heat_remove_peak_heat":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakHeatingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Opaque Surface Conduction and Other Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_people_sensible_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("People Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_light_sensible_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Lights Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_eq_sensible_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Equipment Sensible Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_win_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Window Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_inter_air_transfer_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Interzone Air Transfer Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_infiltration_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Infiltration Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_opaque_surf_conduct_other_heat_add_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Opaque Surface Conduction and Other Heat Addition", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_eq_sensible_heat_remove_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Equipment Sensible Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_win_heat_remove_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Window Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_inter_air_transfer_heat_remove_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Interzone Air Transfer Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_infiltration_heat_remove_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Infiltration Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//
//            case "zone_opaque_surf_conduct_other_heat_remove_peak_cool":
//                if (simRes==null) {
//                    res.addProperty("status", "error");
//                    res.addProperty("error_msg", "No HTML is produced for this simulation job");
//                }else {
//                    DataSource ds = new HTMLTable(cache);
//                    OperationValue ov = ds.process(null, simRes, "SensibleHeatGainSummary:EntireFacility:PeakCoolingSensibleHeatGainComponents");
//                    if (ov == null) {
//                        res.addProperty("status", "error");
//                        res.addProperty("error_msg", "Requested table data not available in the model");
//                    } else {
//                        res = processZoneTableData("Opaque Surface Conduction and Other Heat Removal", ov.getJsonObjects(), false);
//                    }
//                }
//                break;
//        }
//
//        if(res.has("data")) {
//            if (res.get("data").getAsJsonObject().entrySet().size() == 0) {
//                res.addProperty("status", "error");
//                res.addProperty("error_msg", "Request data is not available in this model");
//                res.remove("min");
//                res.remove("max");
//            } else {
//                res.addProperty("status", "success");
//                JsonObject data = res.get("data").getAsJsonObject();
//
//                //get zone area
//                DataSource ds = new HTMLTable(cache);
//                OperationValue ov = ds.process(null, simRes, "InputVerificationandResultsSummary:EntireFacility:ZoneSummary");
//                if(ov!=null){
//                    String areaUnit = "";
//                    JsonArray table = ov.getJsonObjects();
//                    for(int i=0;i<table.size();i++) {
//                        JsonObject row = table.get(i).getAsJsonObject();
//                        String rowCol = JsonUtil.readValue(row, "col", "");
//
//                        if (rowCol.equals("Area")) {
//                            String zone = JsonUtil.readValue(row, "row", "");
//                            if (StringUtil.isNullOrEmpty(zone)) {
//                                continue;
//                            }
//
//                            double value = NumUtil.readDouble(JsonUtil.readValue(row, "value", "0"), 0d);
//                            data.addProperty(zone+"_area", value);
//
//                            areaUnit = JsonUtil.readValue(row, "unit", "");
//                        }
//                    }
//                    res.addProperty("area_unit", areaUnit);
//                }
//            }
//        }
//
//        return res;
//    }
//
//    public static JsonObject getESOSurfaceColoredData(String name, String resolution, String commitId){
//        JsonObject res = new JsonObject();
//        res.addProperty("status", "success");
//        res = GetMonthlyData.getMonthlyDataFromEso(commitId, name, resolution);
//
//        if(res.get("status").getAsString().equals("success")){
//            Set<Map.Entry<String, JsonElement>> allData = res.get("data").getAsJsonObject().entrySet();
//
//            double min = NumUtil.MAX_VALUE;
//            double max = NumUtil.MIN_VALUE;
//            String unit = "";
//            JsonObject surfaceNameJO = new JsonObject();
//            for (Map.Entry<String, JsonElement> data : allData) {
//                String surfaceName = data.getKey();
//                surfaceName = surfaceName.substring(surfaceName.indexOf(":")+1);
//
//                JsonObject surfData = data.getValue().getAsJsonObject();
//                surfaceNameJO.add(surfaceName, surfData);
//                unit = surfData.get("unit").getAsString();
//
//                JsonArray surfDataVals = surfData.getAsJsonArray("data");
//                for (int i = 0; i < surfDataVals.size(); i++) {
//                    JsonObject jo = surfDataVals.get(i).getAsJsonObject();
//                    double val = NumUtil.readDouble(JsonUtil.readValue(jo, "value", "0"), 0d);
//                    if (min > val) {
//                        min = val;
//                    }
//                    if (max < val) {
//                        max = val;
//                    }
//                }
//            }
//
//            res.addProperty("unit", unit);
//            res.addProperty("max", max);
//            res.addProperty("min", min);
//            res.add("data", surfaceNameJO);
//
//            allData = res.get("data").getAsJsonObject().entrySet();
//            for (Map.Entry<String, JsonElement> data : allData) {
//                JsonObject surfData = data.getValue().getAsJsonObject();
//                JsonArray surfDataVals = surfData.getAsJsonArray("data");
//                for (int i = 0; i < surfDataVals.size(); i++) {
//                    JsonObject jo = surfDataVals.get(i).getAsJsonObject();
//                    double val = NumUtil.readDouble(JsonUtil.readValue(jo, "value", "0"), 0d);
//
//                    String color = VisualUtil.makeColor(val, min, max);
//                    jo.addProperty("color", color);
//                }
//            }
//
//            res.addProperty("min", min);
//            res.addProperty("max", max);
//
//            //get surface area
//            Document simRes = null;
//            String htmlContent = IdfVersionControlDAOTestImpl.getInstance().retrieveCommitSimulationResult(commitId,
//                    SimulationResultType.HTML, null);
//            if(!StringUtil.isNullOrEmpty(htmlContent)){
//                simRes = Jsoup.parse(htmlContent);
//            }
//            DataSourceCache cache = new DataSourceCache();
//            DataSource ds = new HTMLTable(cache);
//            OperationValue ov = ds.process(null, simRes, "EnvelopeSummary:EntireFacility:OpaqueExterior");
//            if (ov != null) {
//                JsonObject data = res.get("data").getAsJsonObject();
//
//                JsonArray table = ov.getJsonObjects();
//                String areaUnit = "";
//                for(int i=0;i<table.size();i++) {
//                    JsonObject row = table.get(i).getAsJsonObject();
//                    String rowCol = JsonUtil.readValue(row, "col", "");
//
//                    if (rowCol.equals("Gross Area")) {
//                        String surfaceName = JsonUtil.readValue(row, "row", "");
//                        String value = JsonUtil.readValue(row, "value", "");
//                        if(!StringUtil.isNullOrEmpty(surfaceName)){
//                            data.addProperty(surfaceName+"_area", value);
//                        }
//                        areaUnit = JsonUtil.readValue(row, "unit", "");
//                    }
//                }
//
//                res.addProperty("area_unit", areaUnit);
//            }
//        }
//
//        return res;
//    }
}
