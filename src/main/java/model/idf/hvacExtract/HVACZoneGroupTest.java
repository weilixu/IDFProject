package main.java.model.idf.hvacExtract;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import main.java.config.ServerConfig;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFParser;

public class HVACZoneGroupTest {
	
	public static void main(String[] args){
		ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");
		File from = new File("/Users/weilixu/Desktop/data/test/in.idf");
		
		IDFFileObject idfModel = new IDFFileObject();
		IDFParser modelParser = new IDFParser();
		
		IDDParser parser = new IDDParser("9.3");
		modelParser.parseIDFFromLocalMachine(from, idfModel);
		
		System.out.println(idfModel.getModelFileContent());
		
		//JsonObject validationResults = parser.validateIDF(idfModel);
		//System.out.println(validationResults);
		//HVACZoneGroup zoneGroup = new HVACZoneGroup(idfModel);
		//zoneGroup.getObjectsData();
		//zoneGroup.setIddParser(parser);
		
//		FileWriter file = null;
//		try {
//			file = new FileWriter("/Users/weilixu/Documents/debug.txt");
//
//			file.write(zoneGroup.getObjectsData().toString());
//
//			file.flush();
//			file.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		//System.out.println(zoneGroup.getObjectsData());
	}
}
