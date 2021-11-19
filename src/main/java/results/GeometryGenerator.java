package main.java.results;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

import main.java.config.ServerConfig;
import main.java.model.geometry.EnvelopeFromIDFFileObject;
import main.java.model.geometry.GeometryNewFromIDFFileObject;
import main.java.model.geometry.solar.SolarPosition;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFParser;
import main.java.model.idf.hvacExtract.HVACZoneGroup;
import main.java.model.idf.hvacExtract.IDFHVACExtractor;

public class GeometryGenerator {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	public IDFFileObject model = null;
	public IDDParser iddParser = null;
	public final String GEO_FILE = "geometry.js";
	public final String HVAC_FILE = "hvac.js";
	public final String ENVELOPE_FILE = "envelope.js";

	
	public GeometryGenerator(IDFFileObject model) throws JsonIOException, IOException {

		this.model = model;
        iddParser = new IDDParser(this.model.getVersion());
        iddParser.validateIDF(this.model);
		String path = ServerConfig.readProperty("ViewerDataSavedPath");
		Gson gson = new Gson();
		
		JsonObject geometry = generateGeometry();
		String geometryStr = gson.toJson(geometry);
		String geometryOutputStr = "sourceGeometry=" + geometryStr;
		BufferedWriter writer = new BufferedWriter(new FileWriter(path + "/" + GEO_FILE));
		writer.write(geometryOutputStr);
		writer.close();
		
		JsonObject hvac = generateHVAC();
		String hvacStr = gson.toJson(hvac);
		String hvacOutputStr = "sourceHvac=" + hvacStr;
		writer = new BufferedWriter(new FileWriter(path + "/" + HVAC_FILE));
		writer.write(hvacOutputStr);
		writer.close();
		
		JsonObject envelope = getEnvelope();
		String envelopeStr = gson.toJson(envelope);
		String envelopeOutputStr = "sourceEnvelope=" + envelopeStr;
		writer = new BufferedWriter(new FileWriter(path + "/" + ENVELOPE_FILE));
		writer.write(envelopeOutputStr);
		writer.close();	
	}
	
	private JsonObject generateGeometry() {
		JsonObject jo = new JsonObject();
		GeometryNewFromIDFFileObject geometry = new GeometryNewFromIDFFileObject();
        JsonObject data = geometry.extractGeometry(model);
        
        jo.add("data", data);
        
        // HVAC Zonings
        JsonObject zoningsJO = new JsonObject();
       
        
        HVACZoneGroup hvacZone = new HVACZoneGroup(model);
        hvacZone.setIddParser(iddParser);
        
        JsonObject hvacGroup = hvacZone.getObjectsData();
        
        JsonArray systems = hvacGroup.getAsJsonArray("system");
        Map<String, JsonObject> idToControl = new HashMap<>();
        
        Iterator<JsonElement> sysIter = systems.iterator();
        while(sysIter.hasNext()){
            JsonObject sys = (JsonObject)sysIter.next();
            
            String sysType = sys.get("system_type").getAsString();
            String sysName = sys.get("name").getAsString();
            String sysId = sys.get("id").getAsString();
            
            if(!zoningsJO.has(sysType)){
                zoningsJO.add(sysType, new JsonArray());
            }
            JsonArray ja = zoningsJO.get(sysType).getAsJsonArray();
            
            JsonObject controlJO = new JsonObject();
            controlJO.addProperty("name", sysName);
            controlJO.addProperty("type", "system");
            controlJO.addProperty("id", sysId);
            
            ja.add(controlJO);
            
            idToControl.put(sysId, controlJO);
        }
        
        JsonObject zoneBasedInfo = new JsonObject();        
        
        JsonArray zones = hvacGroup.getAsJsonArray("zone");
        Iterator<JsonElement> zoneIter = zones.iterator();
        while(zoneIter.hasNext()){
            JsonObject zone = (JsonObject)zoneIter.next();
            
            String zoneId = zone.get("id").isJsonNull() ? null : zone.get("id").getAsString();
            String parentSystemId = zone.get("parentsystemID").getAsString();
            String zoneEqName = zone.get("name").getAsString();
            String zoneName = zone.get("zone").getAsString();
            
            if(zoneId!=null && zoneId.equals(parentSystemId)){
                //zone based
                String sysType = zone.get("properties").getAsJsonObject().get("name").getAsString();
                
                if(!zoningsJO.has(sysType)){
                    zoningsJO.add(sysType, new JsonArray());
                }
                JsonArray ja = zoningsJO.get(sysType).getAsJsonArray();
                
                JsonObject controlJO = new JsonObject();
                controlJO.addProperty("name", zoneEqName);
                controlJO.addProperty("type", "zone");
                controlJO.addProperty("zones", zoneName);
                
                ja.add(controlJO);
                
                zoneBasedInfo.add(zoneEqName, zone.get("properties"));
            }else {
                //system controlled
                
                JsonObject sysControlJo = idToControl.get(parentSystemId);
                if(sysControlJo.has("zones")){
                    sysControlJo.addProperty("zones", sysControlJo.get("zones").getAsString()+"&"+zoneName);
                }else {
                    sysControlJo.addProperty("zones", zoneName);
                }
            }
        }
        
        jo.add("hvac_zone_based_info", zoneBasedInfo);
        
        jo.add("hvacZonings", zoningsJO);
        
        return jo;
	}
	
	private JsonObject generateHVAC() {
		JsonObject hvacJo = new JsonObject();

		IDFHVACExtractor extractor = new IDFHVACExtractor();
		HVACZoneGroup hvacZoneGroup = extractor.extract(model, hvacJo);
		JsonObject data = hvacZoneGroup.getObjectsData();
		hvacJo.add("hvac_info", data);
		
		return hvacJo;
		
	}
	
	private JsonObject getEnvelope() {
		JsonObject jo = new JsonObject();
		JsonObject data = new JsonObject();
		JsonObject materials = new JsonObject();
		
		EnvelopeFromIDFFileObject envelope = new EnvelopeFromIDFFileObject();
        envelope.extractEnvelope(data, model, "hierarchy");
        envelope.extractEnvelopeFloorMaterial(materials, model);
        
        
        jo.add("data", data);
        jo.add("materials", materials);
        return jo;
	}
	
	public static void main(String[] args) throws IOException{
        ServerConfig.setConfigPath("/Users/weilixu/eclipse-workspace/IDFProject/resources/server.config");
        
        File idfFile = new File("/Users/weilixu/Desktop/data/doasvrf.idf");
        //File idfFile = new File("/Users/weilixu/Desktop/data/mediumoffice87.idf");

        IDFFileObject model = new IDFFileObject();
        IDFParser parser = new IDFParser();
        parser.parseIDFFromLocalMachine(idfFile, model);
        
        IDDParser iddParser = new IDDParser(model.getVersion());
        JsonObject msg = iddParser.validateIDF(model);

        GeometryGenerator geoGen = new GeometryGenerator(model);
	}

}
