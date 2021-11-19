package main.java.model.ashraeprm.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.java.config.ServerConfig;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFParser;

public class WindowWallRatioParserTest {

	public static void main1(String[] args) {
        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");

		IDFFileObject idfModel = new IDFFileObject();
		IDFParser idfP = new IDFParser();
		idfP.parseIDFFromLocalMachine(new File("/Users/weilixu/Desktop/5ZoneAirCooled.idf"), idfModel);

		IDDParser parser = new IDDParser(idfModel.getVersion());
		parser.validateIDF(idfModel);

		WindowWallRatioParser wwrParser = new WindowWallRatioParser(idfModel);
		wwrParser.adjustToTargetRatio(0.85, "S");
		
		String fileName = "/Users/weilixu/Desktop/generated.idf";
		try {
			// Assume default encoding.
			FileWriter fileWriter = new FileWriter(fileName);

			// Always wrap FileWriter in BufferedWriter.
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			// Note that write() does not automatically
			// append a newline character.
			bufferedWriter.write(idfModel.getModelFileContent());

			// Always close files.
			bufferedWriter.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file '" + fileName + "'");
			// Or we could just do this:
			// ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");
		IDFFileObject idfModel = new IDFFileObject();
		IDFParser idfP = new IDFParser();
		
		idfP.parseIDFFromLocalMachine(new File("/Users/weilixu/Desktop/idf.idf"), idfModel);
		IDDParser parser = new IDDParser(idfModel.getVersion());
		parser.validateIDF(idfModel);
		
		Set<String> surfaceList = new HashSet<String>();
		
		List<IDFObject> feneObjList = idfModel.getCategoryList("FenestrationSurface:Detailed");
		for(int i=0; i<feneObjList.size(); i++) {
			IDFObject obj = feneObjList.get(i);
			if(obj.getDataByStandardComment("Construction Name").equals("Tower C")) {
				surfaceList.add(obj.getDataByStandardComment("Building Surface Name").toLowerCase());
			}
		}
		WindowWallRatioParser wwrParser = new WindowWallRatioParser(idfModel, surfaceList);
		wwrParser.adjustToTargetRatio(0.674, "E");
		wwrParser.adjustToTargetRatio(0.674, "W");
		wwrParser.adjustToTargetRatio(0.719, "N");
		wwrParser.adjustToTargetRatio(0.719, "S");


		String fileName = "/Users/weilixu/Desktop/generated.idf";
		try {
			// Assume default encoding.
			FileWriter fileWriter = new FileWriter(fileName);

			// Always wrap FileWriter in BufferedWriter.
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

			// Note that write() does not automatically
			// append a newline character.
			bufferedWriter.write(idfModel.getModelFileContent());

			// Always close files.
			bufferedWriter.close();
		} catch (IOException ex) {
			System.out.println("Error writing to file '" + fileName + "'");
			// Or we could just do this:
			// ex.printStackTrace();
		}
	}
}
