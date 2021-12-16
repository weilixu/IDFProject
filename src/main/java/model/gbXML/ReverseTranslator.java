package main.java.model.gbXML;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Jsoup;

import com.google.gson.Gson;

import main.java.config.ServerConfig;
import main.java.model.ashraeprm.data.WindowWallRatioParser;
import main.java.model.idd.EnergyPlusFieldTemplate;
import main.java.model.idd.EnergyPlusObjectTemplate;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

@SuppressWarnings("unused")
public class ReverseTranslator {
    private Gson errorLog;
    
    private Document doc;
    private Namespace ns;
    
    private String m_temperatureUnit;
    private String m_lengthUnit;
    private String areaUnit;
    private String volumeUnit;
    private boolean m_useSIUnitsForResults;
    private Double m_lengthMultiplier;
    
    private IDFFileObject file;
    private ClimateTranslation climateTranslator;
    private EnvelopeTranslator envelopeTranslator;
    private ScheduleTranslator scheduleTranslator;
    private CampusTranslator campusTranslator;
    private OutputModule outputTranslator;
    
    private IDDParser iddParser;
    
    public ReverseTranslator(Document d, IDFFileObject idfFileObj){
        doc = d;
        ns = doc.getRootElement().getNamespace();
        file = idfFileObj;
        iddParser = new IDDParser("8.7");
        
        envelopeTranslator = new EnvelopeTranslator(ns, iddParser);
        scheduleTranslator = new ScheduleTranslator(ns);
        climateTranslator = new ClimateTranslation(ns, iddParser);
        outputTranslator = new OutputModule(ns);
        
        //error log
        errorLog = new Gson();
    }
    
    public void convert(){
        
        addEssentialFileElement();
        
        translateGBXML(doc.getRootElement(), doc);
        
        iddParser.validateIDF(file);
        //window validation - cases when window is same size as the wall
        WindowWallRatioParser winValidation = new WindowWallRatioParser(file);
        winValidation.windowWallValidation();
        
    }
    
    private void translateGBXML(Element gbXML, Document doc){
        //get the document element gbXML
        //they are not mapped directly to IDF, but needed to map
        String temperatureUnit = gbXML.getAttributeValue("temperatureUnit");
        if(temperatureUnit.equalsIgnoreCase("F")){
            m_temperatureUnit = "F";
        }else if(temperatureUnit.equalsIgnoreCase("C")){
            m_temperatureUnit = "C";
        }else if(temperatureUnit.equalsIgnoreCase("K")){
            m_temperatureUnit = "K";
        }else if(temperatureUnit.equalsIgnoreCase("R")){
            m_temperatureUnit = "R";
        }else{
            //TODO should give a warning: "No temperature unit specified, using C"
            m_temperatureUnit = "C";
        }
        
        String lengthUnit = gbXML.getAttributeValue("lengthUnit");
        
        m_lengthMultiplier = GbXMLUnitConversion.lengthUnitConversionRate(lengthUnit, "Meters");
        // {SquareKilometers, SquareMeters, SquareCentimeters, SquareMillimeters, SquareMiles, SquareYards, SquareFeet, SquareInches}
        areaUnit = gbXML.getAttributeValue("areaUnit");
        
        // {CubicKilometers, CubicMeters, CubicCentimeters, CubicMillimeters, CubicMiles, CubicYards, CubicFeet, CubicInches}
        volumeUnit = gbXML.getAttributeValue("volumeUnit");
        
        // {true, false}
        String useSIUnitsForResults = gbXML.getAttributeValue("useSIUnitsForResults");
        if(useSIUnitsForResults.equalsIgnoreCase("False")){
            m_useSIUnitsForResults = false;
        }else{
            m_useSIUnitsForResults = true;
        }
        
        //do climate translation
        climateTranslator.setUpEnvironmentForBaseline(file);
        climateTranslator.setUpDesignDayConditionForBaseline(file);
        //do materials before constructions
        //TODO progress bar info
        
        List<Element> materialElements = gbXML.getChildren("Material",ns);
//        if(materialElements.isEmpty()){
//        	//fill in baseline material elements
//        	ASHRAEConstructions baselineCons = new ASHRAEConstructions(file.getVersion());
//        	baselineCons.getBaselineConstructions(file);//fill in baseline
//        }
        
        //Debug: System.out.println(materialElements.size());
        for(int i=0; i<materialElements.size(); i++){
            Element materialElement = materialElements.get(i);
            //Debug: IDFObject material = envelopeTranslator.translateMaterial(materialElement);
            file.addObject(envelopeTranslator.translateMaterial(materialElement));
        }//for
        
        //do construction before surfaces
        //TODO progress bar info
        List<Element> layerElements = gbXML.getChildren("Layer",ns);
        List<Element> contructionElements = gbXML.getChildren("Construction",ns);
        for(int i=0; i<contructionElements.size(); i++){
            Element constructionElement = contructionElements.get(i);
            //Debug: IDFObject construction = envelopeTranslator.translateConstruction(constructionElement, layerElements);
            //Debug: System.out.println(construction.printStatement(50));
            IDFObject construction = envelopeTranslator.translateConstruction(constructionElement, layerElements);
            if(construction != null) {
            		file.addObject(construction);
            }
        }//for
        
        //do windows before surfaces
        //TODO progress bar info
        List<Element> windowTypeElements = gbXML.getChildren("WindowType",ns);
        for(int i=0; i<windowTypeElements.size(); i++){
            Element windowTypeElement = windowTypeElements.get(i);
            
            envelopeTranslator.translateWindowType(windowTypeElement, file);
        }
        
        //do schedules before loads
        //TODO progress bar info
        List<Element> scheduleElements = gbXML.getChildren("Schedule",ns);
        for(int i=0; i<scheduleElements.size(); i++){
            Element scheduleElement = scheduleElements.get(i);
            scheduleTranslator.translateSchedule(scheduleElement, gbXML, file);
        }
        //add more misc schedule types
        scheduleTranslator.addMiscScheduleTypeLimits(file);
        //progress bar process schedule finished
        
        //start processing campus
        campusTranslator = new CampusTranslator(ns, m_lengthMultiplier, iddParser);
        campusTranslator.setAreaUnit(areaUnit);
        campusTranslator.setVolumnUnit(volumeUnit);
        campusTranslator.setEnvelopeTranslator(envelopeTranslator);
        campusTranslator.setScheduleTranslator(scheduleTranslator);
        
        //do thermal zones before spaces
        List<Element> zoneElements = gbXML.getChildren("Zone",ns);
        //TODO progress bar info
        for(int i=0; i<zoneElements.size(); i++){
            Element zoneElement = zoneElements.get(i);
            campusTranslator.translateThermalZone(zoneElement);
        }
        
        //do geometry
        Element campusElements = gbXML.getChild("Campus",ns);
        campusTranslator.translateCampus(campusElements, file);
        campusTranslator.convertBuilding(file);
        //file.setValueCommentPad(100);
        //System.out.println(file.getIDFFileContent());
        
        //do hvac
        //TODO read the HVAC system
        //TODO if no HVAC system, then baseline system selection should be implemented.
        //TODO baseline should be able to extend to residential buildings
//        ASHRAEHVAC baselineHVAC = new ASHRAEHVAC(file, scheduleTranslator, campusTranslator, iddParser.getVersion());
//        baselineHVAC.selectSystem();
//
        //outputs
        outputTranslator.addTableSummary(file);
    }
    
    private void addEssentialFileElement(){    		
    		IDFObject version = new IDFObject("Version", 2);
    		version.setTopComments(new String[] { "!- Generated by BuildSimHub" });
    		
    		version.setIndexedStandardComment(0, "Version Identifier");
    		version.setIndexedData(0, iddParser.getVersion());
        file.addObject(version);

        //TODO this should depends on user options - from interface
        EnergyPlusObjectTemplate simControlTemp = iddParser.getObject("SimulationControl");
        int minField = simControlTemp.getNumberOfMinFields();
		IDFObject simControl = new IDFObject("SimulationControl", minField + 1);
		simControl.setTopComments(new String[] { "!- Generated by BuildSimHub" });
		for(int i=0; i<minField; i++) {
			EnergyPlusFieldTemplate fieldTemp = simControlTemp.getFieldTemplateByIndex(i);
			simControl.setIndexedStandardComment(i, fieldTemp.getFieldName());
    			
    			if(fieldTemp.getDefault()!=null) {
    				simControl.setIndexedData(i, simControlTemp.getFieldTemplateByIndex(i).getFieldName());
    			}else {
    				simControl.setIndexedData(i, "No");
    			}
		}
		file.addObject(simControl);

        //Shadow calculation
		IDFObject shadownCalculation = new IDFObject("ShadowCalculation", 6);
		shadownCalculation.setTopComments(new String[] { "!- Generated by BuildSimHub" });
		shadownCalculation.setIndexedStandardComment(0, "Calculation Method");
		shadownCalculation.setIndexedData(0, "AverageOverDaysInFrequency");
		shadownCalculation.setIndexedStandardComment(1, "Calculation Frequency");
		shadownCalculation.setIndexedData(1, "20");
		shadownCalculation.setIndexedStandardComment(2, "Maximum Figures in Shadow Overlap Calculations");
		shadownCalculation.setIndexedData(2, "15000");
		shadownCalculation.setIndexedStandardComment(3, "Polygon Clipping Algorithm");
		shadownCalculation.setIndexedData(3, "SutherlandHodgman");
		shadownCalculation.setIndexedStandardComment(4, "Sky Diffuse Modeling Algorithm");
		shadownCalculation.setIndexedData(4, "SimpleSkyDiffuseModeling");
        file.addObject(shadownCalculation);
        
        //inside algorithm
        IDFObject surfaceConv = new IDFObject("SurfaceConvectionAlgorithm:Inside", 2);
        surfaceConv.setTopComments(new String[] { "!- Generated by BuildSimHub" });
        surfaceConv.setIndexedStandardComment(0, "Algorithm");
        surfaceConv.setIndexedData(0, "TARP");
        file.addObject(surfaceConv);
        
        //outside algorithm
        IDFObject surfaceConvOutside = new IDFObject("SurfaceConvectionAlgorithm:Outside", 2);
        surfaceConvOutside.setTopComments(new String[] { "!- Generated by BuildSimHub" });
        surfaceConvOutside.setIndexedStandardComment(0, "Algorithm");
        surfaceConvOutside.setIndexedData(0, "DOE-2");
        file.addObject(surfaceConvOutside);
        
        //heat balance algorithm
        IDFObject heatBalance = new IDFObject("HeatBalanceAlgorithm", 5);
        heatBalance.setTopComments(new String[] { "!- Generated by BuildSimHub" });
        heatBalance.setIndexedStandardComment(0, "Algorithm");
        heatBalance.setIndexedData(0, "ConductionTransferFunction");
        heatBalance.setIndexedStandardComment(1, "Surface Temperature Upper Limit");
        heatBalance.setIndexedData(1, "2000","C");
        heatBalance.setIndexedStandardComment(2, "Minimum Surface Convection Heat Transfer Coefficient Value");
        heatBalance.setIndexedData(2, "","W/m2-K");
        heatBalance.setIndexedStandardComment(3, "Maximum Surface Convection Heat Transfer Coefficient Value");
        heatBalance.setIndexedData(3, "","W/m2-K");
        file.addObject(heatBalance);
        
//        //capacitance multiplier
//        IDFObject zonecapacitance = new IDFObject("ZoneCapacitanceMultiplier:ResearchSpecial", 7);
//        zonecapacitance.setTopComments(new String[] { "!- Generated by BuildSimHub" });
//        zonecapacitance.setIndexedStandardComment(0, "Temperature Capacity Multiplier");
//        zonecapacitance.setIndexedData(0, "1");
//        zonecapacitance.setIndexedStandardComment(1, "Humidity Capacity Multiplier");
//        zonecapacitance.setIndexedData(1, "1");
//        zonecapacitance.setIndexedStandardComment(2, "Carbon Dioxide Capacity Multiplier");
//        zonecapacitance.setIndexedData(2, "1");
//        zonecapacitance.setIndexedStandardComment(3, "Generic Contaminant Capacity Multiplier");
//        zonecapacitance.setIndexedData(3, "1");
//        zonecapacitance.setIndexedStandardComment(4, "Generic Contaminant Capacity Multiplier");
//        zonecapacitance.setIndexedData(4, "1");
//        zonecapacitance.setIndexedStandardComment(5, "Generic Contaminant Capacity Multiplier");
//        zonecapacitance.setIndexedData(5, "1");
//        file.addObject(zonecapacitance);
        
        //Timestep
        IDFObject timestep = new IDFObject("Timestep", 2);
        timestep.setTopComments(new String[] { "!- Generated by BuildSimHub" });
        timestep.setIndexedStandardComment(0, "Number of Timesteps per Hour");
        timestep.setIndexedData(0, "6");
        file.addObject(timestep);
        
        //Convegence limits
        IDFObject convergenceLimits = new IDFObject("ConvergenceLimits", 5);
        convergenceLimits.setTopComments(new String[] { "!- Generated by BuildSimHub" });
        convergenceLimits.setIndexedStandardComment(0, "Minimum System Timestep");
        convergenceLimits.setIndexedData(0, "1", "minutes");
        convergenceLimits.setIndexedStandardComment(1, "Maximum HVAC iterations");
        convergenceLimits.setIndexedData(1, "25");
        convergenceLimits.setIndexedStandardComment(2, "Minimum Plant Iterations");
        convergenceLimits.setIndexedData(2, "");
        convergenceLimits.setIndexedStandardComment(3, "Maximum Plant Iterations");
        convergenceLimits.setIndexedData(3, "");
        file.addObject(convergenceLimits);
    }

    public void exportFile(String idfFilePath){
        try {
            file.setValueCommentPad(5);
            PrintWriter out = new PrintWriter(idfFilePath + "/test.idf");
            out.println(file.getModelFileContent());
            out.close();
            
            //GeometryFromIDFFileObject idfConverter = new GeometryFromIDFFileObject();
            //idfConverter.extractGeometry(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public IDFFileObject getIDFFileObject(){
        return file;
    }
    
    public static void main(String[] args){
      ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");

  	  SAXBuilder builder = new SAXBuilder();
  	  File xmlFile = new File("/Users/weilixu/Desktop/data/test.xml");
//  	  
    	  try {
			Document d = (Document) builder.build(xmlFile);
			
			IDFFileObject file = new IDFFileObject();
			
			ReverseTranslator trans = new ReverseTranslator(d, file);
			trans.convert();
			
			trans.exportFile("/Users/weilixu/Desktop/data/");
			
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
