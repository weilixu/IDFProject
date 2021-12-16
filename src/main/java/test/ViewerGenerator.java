package main.java.test;

import com.google.gson.JsonObject;
import main.java.config.ServerConfig;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFParser;
import main.java.results.GeometryGenerator;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ViewerGenerator {
    public static void main(String[] args) throws IOException {
        ServerConfig.setConfigPath("C:\\Users\\xuwe123\\Documents\\javaproject\\resources\\server.config");

        File idfFile = new File("C:\\Users\\xuwe123\\OneDrive - PNNL\\Documents\\Projects\\IECC2021\\C403-DCV\\MinimumOAControllerComparison\\after-template-change\\IECC_OfficeSmall_STD2021_Albuquerque.idf");

        IDFFileObject model = new IDFFileObject();
        IDFParser parser = new IDFParser();
        parser.parseIDFFromLocalMachine(idfFile, model);

        IDDParser iddParser = new IDDParser(model.getVersion());
        JsonObject msg = iddParser.validateIDF(model);

        GeometryGenerator geoGen = new GeometryGenerator(model);
        File htmlFile = new File("WebContent/IDF3DViewerSocket.html");
        Desktop.getDesktop().browse(htmlFile.toURI());
    }
}
