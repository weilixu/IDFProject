package main.java.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import main.java.config.ServerConfig;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFParser;

public class IDDTest {
	
//	public static void main1(String[] args) throws IOException{
//        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");
//        
//        //osm test
//        File osm = new File("/Applications/OpenStudio-2.4.0/Examples/residential/workflows/run/in.osm");
//        
//        OSMFileObject osmFileObj = new OSMFileObject();
//        OSMParser parser = new OSMParser();
//        parser.parseOSMFromLocalMachine(osm, osmFileObj);
//        
//        OSIDDParser idd = new OSIDDParser(null);
//        idd.validateOSM(osmFileObj);
//        
//        JsonObject handleMap = idd.getHandleNameMapinJsonObject();
//        
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
//		
//		FileWriter file = null;
//		try {
//			file = new FileWriter("/Users/weilixu/Documents/osidd.txt");
//			
//			file.write(gson.toJson(handleMap));
//			
//			file.flush();
//			file.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public static void main(String[] args) throws IOException{
        ServerConfig.setConfigPath("/Users/weilixu/eclipse-workspace/IDFProject/resources/server.config");
        
        File idfFile = new File("/Users/weilixu/Desktop/data/mediumoffice87.idf");
        
        IDFFileObject model = new IDFFileObject();
        IDFParser parser = new IDFParser();
        parser.parseIDFFromLocalMachine(idfFile, model);
        
        IDDParser iddParser = new IDDParser(model.getVersion());
        List<IDFObject> runperiod = model.getCategoryList("RunPeriodControl:SpecialDays");
        System.out.println(runperiod.get(0).printStatement(100));
        
        JsonObject msg = iddParser.validateIDF(model);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(msg));
	}
}
