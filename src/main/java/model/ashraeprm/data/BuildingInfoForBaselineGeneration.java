package main.java.model.ashraeprm.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.java.util.NumUtil;
import main.java.util.StringUtil;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.config.ServerConfig;
import main.java.model.ashraebaseline.data.ConstructionParser;
import main.java.model.ashraebaseline.data.LightingParser;
import main.java.model.ashraeprm.BuildingConstruction;
import main.java.model.ashraeprm.BuildingLighting;
import main.java.model.ashraeprm.hvac.HVACTypes;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFParser;
import main.java.model.result.smartUtil.EnergyModelHVACZone;
import main.java.model.result.sysmerge.SystemMerger;
import main.java.report.templateReport.ModelResultsUtility;

@SuppressWarnings("unused")
public class BuildingInfoForBaselineGeneration implements BuildingConstruction, BuildingLighting {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private String unit;

	/**
	 * basic information about the building
	 */
	private String buildingType;
	private String bldgFunc;
	private int numOfFloor;
	private int numOfFloorAboveGround;
	private Double totalFloorArea;
	private Double conditionedFloorArea;
	private boolean electricHeating;
	private HVACTypes hvacSystemType;
	private Double[] fanPowerSummaryList;

	/**
	 * Exception detector Plugin
	 */
	// return fan information
	private double numberOfSystem = 0.0;
	private double supplyReturnRatio = 0.0;
	private boolean hasReturnFan = false;

	// District System
	private boolean districtHeat = false;
	private boolean districtCool = false;

	/**
	 * set point not met
	 */
	private Double heatingSetPointNotMet;
	private Double coolingSetPointNotMet;

	/**
	 * set point not met
	 */
	private Double totalCoolingLoad;
	private Double totalHeatingLoad;

	/**
	 * climate zone
	 */
	private ClimateZone cZone;

	// for creating HVAC system
	private HashMap<String, ArrayList<EnergyModelHVACZone>> floorMap;
	private HashMap<String, ArrayList<EnergyModelHVACZone>> heatOnlyZones;
	private Map<String, Map<String, ArrayList<IDFObject>>> serviceHotWater;
	private boolean addedServiceWater = false;

	/**
	 * Energyplus data
	 */
	private IDFFileObject baselineModel;
	private SystemMerger merger;
	private IDFObject standardZoneSizingObj;

	/**
	 * Lighting Module data
	 */
	public final static String LIGHTS = "Lights";
	// EnergyPlus objects that relates to lights object
	public static final String ZONE = "Zone";
	public static final String ZONELIST = "ZoneList";
	/**
	 * Construction Module Data
	 */
	// EnergyPlus Objects that require to be removed
	public static final String MATERIAL = "Material";
	public static final String MATERIALMASS = "Material:NoMass";
	public static final String CONSTRUCTION = "Construction";
	public static final String CONSTRUCTIONINTERNALSOURCE = "Construction:InternalSource";
	public static final String SIMPLE_WINDOW = "WindowMaterial:SimpleGlazingSystem";

	// Baseline Construction Name (to replace the original constructions)
	public static final String EXTERNAL_WALL = "Project Wall";
	public static final String ROOF = "Project Roof";
	public static final String PARTITION = "Project Partition";
	public static final String INTERNAL_FLOOR = "Project Internal Floor";
	public static final String BG_WALL = "Project Below Grade Wall";
	public static final String EXTERNAL_FLOOR = "Project External Floor";
	public static final String SOG_FLOOR = "Project Slab On Grade Floor";
	public static final String WINDOW = "Project Window";
	public static final String DOOR = "Project Door";
	// private static final String CURTAIN_WALL = "Project Curtain Wall";
	public static final String SKYLIGHT = "Project Skylight";

	// EnergyPlus objects that relates to the construction changes
	public static final String BLDG_SURFACE = "BuildingSurface:Detailed";
	public static final String BLDG_FENE = "FenestrationSurface:Detailed";
	public static final String BLDG_INTERNAL_MASS = "InternalMass";

	private JsonArray generationStatus;
	private JsonObject hvacReportObject;

	public BuildingInfoForBaselineGeneration(String bldgType, String bldgFunc, ClimateZone climateZone,
			IDFFileObject baselineModel, HashMap<String, ArrayList<EnergyModelHVACZone>> floorMap, JsonArray status) {

		// residential or non-residential
		buildingType = bldgType;
		totalCoolingLoad = 0.0;
		totalHeatingLoad = 0.0;
		cZone = climateZone;
		this.baselineModel = baselineModel;
		generationStatus = status;
		hvacReportObject = new JsonObject();
		serviceHotWater = new HashMap<String, Map<String, ArrayList<IDFObject>>>();

		IDFObject unitObj = baselineModel.getCategoryList("OutputControl:Table:Style").get(0);
		if (unitObj == null) {
			unit = "jtomj";
		} else {
			String data = unitObj.getDataByStandardComment("Unit Conversion");
			if (data == null) {
				unit = "jtomj";
			} else {
				unit = data.toLowerCase();
			}
		}
		electricHeating = false;

		this.floorMap = floorMap;

		heatOnlyZones = null;

		this.bldgFunc = bldgFunc;

		// register exception detectors plugins

	}

	public void initializeBuildingData() {
		floorMap.clear();
		totalCoolingLoad = 0.0;
		totalHeatingLoad = 0.0;

	}

	/*
	 * All Setter Methods
	 */
	public void setTotalFloorArea(Double area) {
		totalFloorArea = area;
	}

	public void setNumOfFloor(int num) {
		numOfFloor = num;
	}

	public void setNumOfFloorAboveGround(int num) {
		numOfFloorAboveGround = num;
	}

	public void setConditionedFloorArea(Double area) {
		conditionedFloorArea = area;
	}

	public void setHeatTimeSetPointNotMet(Double hr) {
		heatingSetPointNotMet = hr;
	}

	public void setCoolTimeSetPointNotMet(Double hr) {
		coolingSetPointNotMet = hr;
	}

	public void setElectricHeating() {
		electricHeating = true;
	}

	public void setSupplyReturnRatio(double ratio) {
		supplyReturnRatio = ratio;
	}

	public void setNumberOfSystem(double number) {
		numberOfSystem = number;
	}

	public void setReturnFanIndicator(boolean has) {
		hasReturnFan = has;
	}

	public void setDistrictHeat(boolean dist) {
		districtHeat = dist;
	}

	public void setDistrictCool(boolean dist) {
		districtCool = dist;
	}

	public void setFanPowerSummaryList(Double[] fanPowers) {
		fanPowerSummaryList = fanPowers;
	}

	public boolean getHeatingMethod() {
		return electricHeating;
	}

	public boolean hasReturnFan() {
		return hasReturnFan;
	}

	public boolean isDistrictHeat() {
		return districtHeat;
	}

	public boolean isDistrictCool() {
		return districtCool;
	}

	public boolean isHeatOnlyZone(String zone) {
		if (heatOnlyZones == null || heatOnlyZones.isEmpty()) {
			return false;
		}

		return heatOnlyZones.containsKey(zone);
	}

	public String getUnitType() {
		return unit;
	}

	/**
	 * get the total cooling load
	 * 
	 * @return
	 */
	public Double getTotalCoolingLoad() {
		return Math.round(totalCoolingLoad * 100.0) / 100.0;
	}

	/**
	 * get the total heating load
	 * 
	 * @return
	 */
	public Double getTotalHeatingLoad() {
		return Math.round(totalHeatingLoad * 100.0) / 100.;
	}

	/*
	 * All getter methods
	 */
	public Double getTotalFloorArea() {
		return totalFloorArea;
	}

	public Double getConditionedFloorArea() {
		return conditionedFloorArea;
	}

	public Integer getNumberOfFloor() {
		return numOfFloor;
	}

	public Double getHeatingSetPointNotMet() {
		return heatingSetPointNotMet;
	}

	public Double getCoolingSetPointNotMet() {
		return coolingSetPointNotMet;
	}

	public Double getSupplyReturnFanRatio() {
		return supplyReturnRatio / numberOfSystem;
	}

	public Double getTotalSupplyAirFlowRateOfOneFloor(String floor) {

		if (floorMap.containsKey(floor)) {
			// system 5,6,7,8
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			Double totalAirFlow = 0.0;
			for (EnergyModelHVACZone zone : zoneList) {
				totalAirFlow += Math.max(zone.getCoolingAirFlow(), zone.getHeatingAirFlow());
			}
			return totalAirFlow;
		} else {
			// for the single zone systems
			Iterator<String> floorItr = floorMap.keySet().iterator();
			while (floorItr.hasNext()) {
				ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floorItr.next());
				for (EnergyModelHVACZone zone : zoneList) {
					if (zone.getZoneName().equals(floor)) {
						return Math.max(zone.getCoolingAirFlow(), zone.getHeatingAirFlow());
					}
				}
			}
		}
		return -1.0;
	}

	public Double getSupplyAirFlowRateOfOneZone(String zoneName) {
		Iterator<String> floorItr = floorMap.keySet().iterator();
		while (floorItr.hasNext()) {
			String floor = floorItr.next();
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			for (EnergyModelHVACZone zone : zoneList) {
				if (zone.getZoneName().equals(zoneName)) {
					return Math.max(zone.getCoolingAirFlow(), zone.getHeatingAirFlow());
				}
			}
		}

		return null;
	}

	public Double getMinimumOAFlowRateOfOneZone(String zoneName) {
		Iterator<String> floorItr = floorMap.keySet().iterator();
		while (floorItr.hasNext()) {
			String floor = floorItr.next();
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			for (EnergyModelHVACZone zone : zoneList) {
				if (zone.getZoneName().equals(zoneName)) {
					return zone.getMinimumVentilation();
				}
			}
		}
		return null;
	}

	public Double getHeatingLoadOfOneZone(String zoneName) {
		Iterator<String> floorItr = floorMap.keySet().iterator();
		while (floorItr.hasNext()) {
			String floor = floorItr.next();
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			for (EnergyModelHVACZone zone : zoneList) {
				if (zone.getZoneName().equals(zoneName)) {
					return zone.getHeatingLoad();
				}
			}
		}
		return null;
	}

	public Double getCoolingLoadOfOneZone(String zoneName) {
		Iterator<String> floorItr = floorMap.keySet().iterator();
		while (floorItr.hasNext()) {
			String floor = floorItr.next();
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			for (EnergyModelHVACZone zone : zoneList) {
				if (zone.getZoneName().equals(zoneName)) {
					return zone.getCoolingLoad();
				}
			}
		}
		return null;
	}

	public Double getTotalMinmumOAFlowRateOfOneFloor(String floor) {
		if (floorMap.containsKey(floor)) {
			// system 5,6,7,8
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			Double totalOA = 0.0;
			for (EnergyModelHVACZone zone : zoneList) {
				totalOA += zone.getMinimumVentilation();
			}
			return totalOA;
		} else {
			Iterator<String> floorItr = floorMap.keySet().iterator();
			while (floorItr.hasNext()) {
				ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floorItr.next());
				for (EnergyModelHVACZone zone : zoneList) {
					if (zone.getZoneName().equals(floor)) {
						return zone.getMinimumVentilation();
					}
				}
			}
		}
		return -1.0;
	}

	public IDFFileObject getBaselineModel() {
		return baselineModel;
	}

	public Double[] getFanSummaryInDesignModel() {
		return fanPowerSummaryList;
	}

	public HVACTypes getHVACSystemType() {
		return hvacSystemType;
	}

	public JsonObject getHvacReportObject() {
		return hvacReportObject;
	}

	public void addZoneSizingObjectForEveryZone() {
		Iterator<String> floorItr = floorMap.keySet().iterator();
		while (floorItr.hasNext()) {
			String floor = floorItr.next();
			ArrayList<EnergyModelHVACZone> floorZoneList = floorMap.get(floor);
			for (EnergyModelHVACZone zone : floorZoneList) {
				IDFObject standardSizing = standardZoneSizingObj.deepClone();

				String zoneName = zone.getZoneName();
				if (this.isHeatOnlyZone(zoneName)) {
					standardSizing.setDataByStandardComment("Zone Heating Design Supply Air Temperature Input Method",
							"SupplyAirTemperature");
					standardSizing.setDataByStandardComment("Zone Heating Design Supply Air Temperature", "40.6");
				}

				standardSizing.setDataByStandardComment("Zone or ZoneList Name", zone.getZoneName());
				baselineModel.addObject(standardSizing);
			}
		}
	}

	public void addEnergyPlusObject(IDFObject obj) {
		baselineModel.addObject(obj);
	}

	public void removeEnergyPlusObject(IDFObject obj) {
		baselineModel.removeIDFObject(obj);
	}

	public void processZoneSizingResults(Document html) {

		double[] totalLoadList = ModelResultsUtility.processZoneSizingResultsForPRM(html, floorMap);
		
		totalCoolingLoad = totalLoadList[0];
		JsonObject tempObj = new JsonObject();
		tempObj.addProperty("message", "The total cooling load is: " + totalCoolingLoad);
		generationStatus.add(tempObj);
		totalHeatingLoad = totalLoadList[1];
		tempObj = new JsonObject();
		tempObj.addProperty("message", "The total heating load is: " + totalHeatingLoad);
		generationStatus.add(tempObj);
		
		// WX: 1/10/2018 G3.1.1 exception e: Zones with heating only systems
		Iterator<String> floorItr = floorMap.keySet().iterator();
		while (floorItr.hasNext()) {
			String floor = floorItr.next();
			ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
			Iterator<EnergyModelHVACZone> zoneItr = zoneList.iterator();

			while (zoneItr.hasNext()) {
				EnergyModelHVACZone zone = zoneItr.next();
				if (zone.getIsHeatOnly()) {
					if (heatOnlyZones == null) {
						heatOnlyZones = new HashMap<String, ArrayList<EnergyModelHVACZone>>();
					}
					heatOnlyZones.put(zone.getZoneName(), new ArrayList<EnergyModelHVACZone>());
					heatOnlyZones.get(zone.getZoneName()).add(zone);
					zoneItr.remove();
				}
			}
		}
		
	}

	/**
	 * This method only works for sys 7 and sys 8 and it should be called after the
	 * replaceHVACSystem method has been succcessfully executed.
	 * 
	 * @param numberOfChiller
	 */
	public void addMultipleChillerSchema(int numberOfChiller) {
		merger.multipleChillerConfigurationGeneration(numberOfChiller);
	}

	public void replaceHVACSystem(IDFFileObject baselineHVACTemplate, HVACTypes systemType) {
		// initialize the hvac report module
		hvacReportObject.add("airsys", new JsonArray());
		hvacReportObject.add("watersys", new JsonObject());

		JsonArray airSysArray = hvacReportObject.get("airsys").getAsJsonArray();

		hvacSystemType = systemType;

		// G3.1.2.2 Equipment Capacities. 15% for cooling and 25% for heating
		List<IDFObject> sizingZoneList = baselineHVACTemplate.getCategoryList("Sizing:Zone");
		if(sizingZoneList!=null) {
			standardZoneSizingObj = sizingZoneList.get(0);
		}
		IDFFileObject heatOnlyZoneTemplate = null;
		String heatOnlyZoneSystemType = "sys9";
		// G3.1.1 e spaces with heat only system should modeled with heat only system in
		// the baseline
		if (heatOnlyZones != null && !heatOnlyZones.isEmpty()) {
			IDFParser idfParser = new IDFParser();
			heatOnlyZoneTemplate = new IDFFileObject();
			if (getHeatingMethod()) {
				heatOnlyZoneSystemType = "sys0";
			}

			// ____REPORT____
			Iterator<String> heatOnlyZoneItr = heatOnlyZones.keySet().iterator();
			while(heatOnlyZoneItr.hasNext()) {
				String zoneName = heatOnlyZoneItr.next();
				JsonObject heatOnlySys = new JsonObject();
				airSysArray.add(heatOnlySys);
				heatOnlySys.addProperty("System Name", zoneName);
				if (heatOnlyZoneSystemType.equals("sys0")) {
					heatOnlySys.addProperty("System Type", "sys10");
				} else {
					heatOnlySys.addProperty("System Type", heatOnlyZoneSystemType);
				}
				heatOnlySys.addProperty("Supply air temperature rest", "Not required");
				heatOnlySys.addProperty("Fan Control", "Constant Volume");
				heatOnlySys.addProperty("Similar Number of Systems", "Not used");
			}
			// ____END OF REPORT____

			File model = new File(ServerConfig.readProperty("ResourcePath") + "/prmhvactemplate/"
					+ baselineModel.getVersion() + "/" + heatOnlyZoneSystemType + ".idf");
			idfParser.parseIDFFromIDFFile(model, heatOnlyZoneTemplate);// TODO WX whichever works on server
		}

		// set up merger
		merger = new SystemMerger(baselineHVACTemplate, baselineModel);
		if (systemType.getCategory().equals("sys3") || systemType.getCategory().equals("sys4")
				|| systemType.equals(HVACTypes.SYS9) || systemType.equals(HVACTypes.SYS10)) {

			// need to rearrange the floorMap;
			JsonObject tempObj = new JsonObject();
			tempObj.addProperty("message", "Rearranging the floor map for " + systemType.toString());
			generationStatus.add(tempObj);

			HashMap<String, ArrayList<EnergyModelHVACZone>> newFloorMap = new HashMap<String, ArrayList<EnergyModelHVACZone>>();
			Iterator<String> floorsItr = floorMap.keySet().iterator();
			while (floorsItr.hasNext()) {
				String floor = floorsItr.next();
				ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
				for (int i = 0; i < zoneList.size(); i++) {
					String zoneName = zoneList.get(i).getZoneName();
					newFloorMap.put(zoneName, new ArrayList<>());
					newFloorMap.get(zoneName).add(zoneList.get(i));
					// ____REPORT___
					JsonObject mainAirSys = new JsonObject();
					airSysArray.add(mainAirSys);
					mainAirSys.addProperty("System Name", zoneName);
					mainAirSys.addProperty("System Type", hvacSystemType.toString());
					//mainAirSys.addProperty("Supply air temperature rest", hvacSystemType.airSupplyReset());
					//mainAirSys.addProperty("Fan Control", hvacSystemType.getFanControl());
					mainAirSys.addProperty("Similar Number of Systems", "Not used");
					// ____END OF REPORT____
				}
			}

			merger.replaceSystemsForPRM(newFloorMap, null, null, heatOnlyZones, heatOnlyZoneTemplate);
		} else {
			HashMap<String, ArrayList<EnergyModelHVACZone>> newFloorMap = new HashMap<String, ArrayList<EnergyModelHVACZone>>();

			// another one
			HashMap<String, ArrayList<EnergyModelHVACZone>> exceptionFloorMap = new HashMap<String, ArrayList<EnergyModelHVACZone>>();
			Integer systemCounter = 0;

			for (String floor : floorMap.keySet()) {
				JsonObject tempObj = new JsonObject();
				tempObj.addProperty("message", "Working on floor: " + floor);
				generationStatus.add(tempObj);

				ArrayList<EnergyModelHVACZone> zoneList = floorMap.get(floor);
				Double floorArea = 0.0;
				Double heatingLoad = 0.0;
				Double coolingLoad = 0.0;
				Double operationHours = 0.0;
				for (EnergyModelHVACZone zone : zoneList) {
					floorArea += zone.getArea();
					heatingLoad += zone.getHeatingLoad();
					coolingLoad += zone.getCoolingLoad();
					operationHours += zone.getOperationHourInAWeek();
				}

				tempObj = new JsonObject();
				tempObj.addProperty("message", "Floor: " + floor + " has floor area of " + floorArea
						+ ", total heating load: " + heatingLoad + ", total cooling load:" + coolingLoad);
				generationStatus.add(tempObj);

				Double threshold = 31.5;// threshold set for different spaces W/m2
				if (unit.equals("inchpound")) {
					threshold = 10.0;// Btu/h-ft2
				}
				for (int i = 0; i < zoneList.size(); i++) {
					EnergyModelHVACZone zone = zoneList.get(i);
					Double heatLoad = zone.getHeatingLoad() / zone.getArea();
					Double coolLoad = zone.getCoolingLoad() / zone.getArea();
					Double hours = zone.getOperationHourInAWeek();
					tempObj = new JsonObject();
					tempObj.addProperty("message",
							zone.getZoneName() + " Peak heating load is: " + heatLoad + " peak cooling load is: "
									+ coolLoad + " and operation hours in a week is: " + hours + " hr");
					if (heatLoad - heatingLoad / zoneList.size() > threshold) {
						// Peak Heating load for zone: zone Name is differ from the average load : load
						tempObj = new JsonObject();
						tempObj.addProperty("message",
								"Peak Heating load for zone: " + zone.getZoneName()
										+ " is difference from the average: " + heatingLoad / floorArea
										+ "by more than " + threshold);
						generationStatus.add(tempObj);

						exceptionFloorMap.put(zone.getZoneName(), new ArrayList<EnergyModelHVACZone>());
						exceptionFloorMap.get(zone.getZoneName()).add(zone);
						// ____START OF REPORT____
						JsonObject exceptionSys = new JsonObject();
						airSysArray.add(exceptionSys);
						exceptionSys.addProperty("System Name", zone.getZoneName());
						exceptionSys.addProperty("System Type", heatOnlyZoneSystemType);
						exceptionSys.addProperty("Supply air temperature rest", "Not required");
						exceptionSys.addProperty("Fan Control", "Constant Volume");
						exceptionSys.addProperty("Similar Number of Systems", exceptionFloorMap.size());
						// ____END OF REPORT____
					} else if (coolLoad - coolingLoad / zoneList.size() > threshold) {
						tempObj = new JsonObject();
						tempObj.addProperty("message",
								"Peak Cooling load for zone: " + zone.getZoneName()
										+ " is difference from the average: " + coolingLoad / floorArea
										+ "by more than " + threshold);
						generationStatus.add(tempObj);
						exceptionFloorMap.put(zone.getZoneName(), new ArrayList<EnergyModelHVACZone>());
						exceptionFloorMap.get(zone.getZoneName()).add(zone);
						// ____START OF REPORT____
						JsonObject exceptionSys = new JsonObject();
						airSysArray.add(exceptionSys);
						exceptionSys.addProperty("System Name", zone.getZoneName());
						exceptionSys.addProperty("System Type", heatOnlyZoneSystemType);
						exceptionSys.addProperty("Supply air temperature rest", "Not required");
						exceptionSys.addProperty("Fan Control", "Constant Volume");
						exceptionSys.addProperty("Similar Number of Systems", exceptionFloorMap.size());
						// ____END OF REPORT____
					} else if  (Math.abs(operationHours / zoneList.size()) - hours > 40) {
						// Opereation hours of zone : zone name is : hours, which is more than 40 hours
						// difference from
						// average operation hours: hours)
						tempObj = new JsonObject();
						tempObj.addProperty("message",
								"Operation hours of zone: " + zone.getZoneName() + " excced the average hours: "
										+ operationHours / zoneList.size() + " by more than 40 hours");
						generationStatus.add(tempObj);

						exceptionFloorMap.put(zone.getZoneName(), new ArrayList<EnergyModelHVACZone>());
						exceptionFloorMap.get(zone.getZoneName()).add(zone);
						// ____START OF REPORT____
						JsonObject exceptionSys = new JsonObject();
						airSysArray.add(exceptionSys);
						exceptionSys.addProperty("System Name", zone.getZoneName());
						exceptionSys.addProperty("System Type", heatOnlyZoneSystemType);
						exceptionSys.addProperty("Supply air temperature rest", "Not required");
						exceptionSys.addProperty("Fan Control", "Constant Volume");
						exceptionSys.addProperty("Similar Number of Systems", exceptionFloorMap.size());
						// ____END OF REPORT____
					} else {
						if (!newFloorMap.containsKey(floor)) {
							newFloorMap.put(floor, new ArrayList<EnergyModelHVACZone>());
						}
						newFloorMap.get(floor).add(zone);
					}
				}
				// ____REPORT___
				JsonObject mainAirSys = new JsonObject();
				airSysArray.add(mainAirSys);
				mainAirSys.addProperty("System Name", floor);
				mainAirSys.addProperty("System Type", hvacSystemType.toString());
				//mainAirSys.addProperty("Supply air temperature rest", hvacSystemType.airSupplyReset());
				//mainAirSys.addProperty("Fan Control", hvacSystemType.getFanControl());
				mainAirSys.addProperty("Similar Number of Systems", "Not used");
				// ____END OF REPORT____
			}
			
			if (!exceptionFloorMap.isEmpty()) {
				IDFParser idfParser = new IDFParser();
				IDFFileObject exceptionSystemTemplate = new IDFFileObject();
				String exceptionSystemType = "sys3";
				if (getHeatingMethod()) {
					JsonObject tempObj = new JsonObject();
					tempObj.addProperty("message",
							"The primary heating method is electricity, the exception zones will be installed with system type 4");
					generationStatus.add(tempObj);

					exceptionSystemType = "sys4";
				}

				File model = new File(ServerConfig.readProperty("ResourcePath") + "/prmhvactemplate/"
						+ baselineModel.getVersion() + "/" + exceptionSystemType + ".idf");

				idfParser.parseIDFFromIDFFile(model, exceptionSystemTemplate);
				merger.replaceSystemsForPRM(newFloorMap, exceptionFloorMap, exceptionSystemTemplate, heatOnlyZones,
						heatOnlyZoneTemplate);
			} else {
				merger.replaceSystemsForPRM(newFloorMap, null, null, heatOnlyZones, heatOnlyZoneTemplate);
			}
		}
	}

	/**
	 * Building construction methods
	 */
	@Override
	public ClimateZone getClimateZone() {
		return cZone;
	}

	@Override
	public void replaceConstruction(ConstructionParser envelope) {
		// remove all the building envelope related objects - this is not exhaustive
		List<IDFObject> deleteList = baselineModel.getCategoryList(MATERIAL);
		if (baselineModel.getCategoryList(MATERIALMASS) != null) {
			deleteList.addAll(baselineModel.getCategoryList(MATERIALMASS));
		}
		if (baselineModel.getCategoryList(CONSTRUCTION) != null) {
			deleteList.addAll(baselineModel.getCategoryList(CONSTRUCTION));
		}
		if (baselineModel.getCategoryList(SIMPLE_WINDOW) != null) {
			deleteList.addAll(baselineModel.getCategoryList(SIMPLE_WINDOW));
		}
		
		if(baselineModel.getCategoryList(CONSTRUCTIONINTERNALSOURCE)!=null) {
			deleteList.addAll(baselineModel.getCategoryList(CONSTRUCTIONINTERNALSOURCE));
		}

		// deleting
		for (int i = 0; i < deleteList.size(); i++) {
			baselineModel.removeIDFObject(deleteList.get(i));
		}
		// Iterator<IDFObject> deleteObjItr = deleteList.iterator();
		// while (deleteObjItr.hasNext()) {
		// IDFObject deleteObj = deleteObjItr.next();
		// }

		List<IDFObject> objects = envelope.getObjects();
		for (IDFObject newConst : objects) {
			
			JsonObject tempObj = new JsonObject();
			tempObj.addProperty("message", "adding new object: " + newConst.getName());
			generationStatus.add(tempObj);
			baselineModel.addObject(newConst);
		}

		JsonObject tempObj = new JsonObject();
		tempObj.addProperty("message", "replacing building surfaces with the new constructions...");
		generationStatus.add(tempObj);
		replaceBuildingSurface();
		replaceFenestrationSurface();
		replaceInternalMass();
	}

	public Double getBaselineWindowUValue() {
		IDFObject constObj = baselineModel.getIDFObjectByName("WindowMaterial:Simple", "Simple Baseline");
		if (constObj != null) {
			String uValueStr = constObj.getIndexedData(1);
			Double uValue = NumUtil.readDouble(uValueStr, -1.0);
			return uValue;
		}
		return -1.0;
	}

	public Double getBaselineWindowSHGC() {
		IDFObject constObj = baselineModel.getIDFObjectByName("WindowMaterial:Simple", "Simple Baseline");
		if (constObj != null) {
			String shgcStr = constObj.getIndexedData(2);
			Double shgc = NumUtil.readDouble(shgcStr, -1.0);
			return shgc;
		}
		return -1.0;
	}

	public Double getBaselineConstructionUValue(String construction) {
		IDFObject constObj = baselineModel.getIDFObjectByName("Construction", construction);
		Double rValue = 0.0;
		if (constObj != null) {
			for (int i = 1; i < constObj.getObjLen() - 1; i++) {
				String materialName = constObj.getIndexedData(i);
				IDFObject material = baselineModel.getIDFObjectByName("Material", materialName);
				if (material != null) {
					Double materialThickness = 0.0;
					Double materialConductivity = 0.0;
					for (int j = 1; j < material.getObjLen() - 1; j++) {
						if (material.getOriginalCommentNoUnit(j).equals("Thickness")) {
							materialThickness = NumUtil.readDouble(material.getIndexedData(j), 0.0);
						}
						if (material.getOriginalCommentNoUnit(j).equals("Conductivity")) {
							materialConductivity = NumUtil.readDouble(material.getIndexedData(j), 0.0);
						}
					}
					if (materialConductivity != 0) {
						rValue += materialThickness / materialConductivity;
					}
				} else {
					// should be in nomass
					material = baselineModel.getIDFObjectByName("Material:NoMass", materialName);
					for (int j = 1; j < material.getObjLen() - 1; j++) {
						if (material.getOriginalCommentNoUnit(j).equals("Thermal Resistance")) {
							rValue += NumUtil.readDouble(material.getIndexedData(j), 0.0);
						}
					}
				}
			}

			return 1 / rValue;
		} else {

			return null;
		}
	}
	/**
	 * building lighting power density method - space by space / bldg method
	 */
	@Override
	public void replaceLighting(LightingParser parser, JsonArray lpdData) {
		Map<String, String[]> zoneLitSchedMap = new HashMap<>();
		// Step 1, set LPD assumption map
		List<IDFObject> lights = baselineModel.getCategoryList(LIGHTS);
		if (lights != null) {
			// assume one zone one lpd
			// else, there is no point to replace LPD
			for (int i = 0; i < lights.size(); i++) {
				IDFObject light = lights.get(i);
				String zone = light.getDataByStandardComment("Zone or ZoneList Name");
				String sched = light.getDataByStandardComment("Schedule Name");
				String returnAir = light.getDataByStandardComment("Return Air Fraction");
				String radiant = light.getDataByStandardComment("Fraction Radiant");
				String visible = light.getDataByStandardComment("Fraction Visible");
				String replaceable = light.getDataByStandardComment("Fraction Replaceable");

				String[] lightStrArray = new String[] { sched, returnAir, radiant, visible, replaceable };

				IDFObject zoneObj = baselineModel.getIDFObjectByName(ZONE, zone);
				if (zoneObj == null) {
					// we then search the zone list
					IDFObject zoneListObj = baselineModel.getIDFObjectByName(ZONELIST, zone);
					if (zoneListObj != null) {
						for (int j = 1; j < zoneListObj.getObjLen() - 1; j++) {
							String subZoneName = zoneListObj.getIndexedData(j);

							zoneLitSchedMap.put(subZoneName, lightStrArray);
						}
					} else {
						// this must be an error light object
					}
				} else {
					zoneLitSchedMap.put(zone, lightStrArray);
				}
			}
			// Step 2, remove all the LPD objects
			int len = lights.size();
			for (int k = 0; k < len; k++) {
				baselineModel.removeIDFObject(lights.get(0));
			}
			// Step 3, insert the LPD objects back
			Iterator<String> zoneLitSchedMapItr = zoneLitSchedMap.keySet().iterator();
			while (zoneLitSchedMapItr.hasNext()) {
				String zone = zoneLitSchedMapItr.next();
				IDFObject zoneObj = baselineModel.getIDFObjectByName("Zone", zone);

				String zoneType = StringUtil.checkNullAndEmpty(zoneObj.getKeyedExtraInfo("zone_type"),
						"Office, Enclosed");
				String category = StringUtil.checkNullAndEmpty(zoneObj.getKeyedExtraInfo("zone_category"),
						"Common Space Types");

				/*
				 * String[] defaultTypeList = parser.getDefaultLPDType(bldgFunc); String
				 * defaultType = defaultTypeList[1]; String defaultCategory =
				 * defaultTypeList[0];
				 */

				// extract the LPD from the database
				String lpd = parser.getLPDValue(category, zoneType);

				JsonObject zoneJSObj = new JsonObject();
				zoneJSObj.addProperty("zone_name", zone);
				zoneJSObj.addProperty("zone_type", zoneType);
				zoneJSObj.addProperty("baseline_lpd", lpd);
				lpdData.add(zoneJSObj);

				JsonObject tempObj = new JsonObject();
				tempObj.addProperty("message", "Working on Zone: " + zone + ", Zone type: " + category + "/" + zoneType
						+ ", Lighting power density is set to " + lpd + " W/m2");
				generationStatus.add(tempObj);

				// generate LPD
				if (lpd != null) {
					IDFObject newLPDObj = generateLPD(zone, lpd, zoneLitSchedMap.get(zone));
					// insert LPD back to the baselineModel
					baselineModel.addObject(newLPDObj);
				}
			}
		}
	}

	private IDFObject generateLPD(String zone, String lpd, String[] data) {
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		ArrayList<String> comments = new ArrayList<String>();
		ArrayList<String> topComments = new ArrayList<String>();

		lines.add("Lights");
		units.add("");
		comments.add("");
		topComments.add("!- Generated by BuildSimHub");

		lines.add(zone + "_BASELINE_LPD");
		units.add("");
		comments.add("Name");

		lines.add(zone);
		units.add("");
		comments.add("Zone or ZoneList Name");

		lines.add(data[0]);
		units.add("");
		comments.add("Schedule Name");

		lines.add("Watts/Area");
		units.add("");
		comments.add("Design Level Calculation Method");

		lines.add("");
		units.add("W");
		comments.add("Lighting Level");

		lines.add(lpd);
		units.add("W/m2");
		comments.add("Watts per Zone Floor Area");

		lines.add("");
		units.add("W/person");
		comments.add("Watts per Person");

		lines.add(data[1]);
		units.add("");
		comments.add("Return Air Fraction");

		lines.add(data[2]);
		units.add("");
		comments.add("Fraction Radiant");

		lines.add(data[3]);
		units.add("");
		comments.add("Fraction Visible");

		lines.add(data[4]);
		units.add("");
		comments.add("Fraction Replaceable");

		lines.add("General");
		units.add("");
		comments.add("End-Use Subcategory");

		return new IDFObject(lines, units, comments, topComments);

	}

	/**
	 * replace the fenestration surface. The checking algorithm depends on the name
	 * generated from asset score tool. fenestration --> windows skylight
	 * -->Skylight
	 */
	private void replaceFenestrationSurface() {
		List<IDFObject> feneSurfaceObjList = baselineModel.getCategoryList(BLDG_FENE);
		if (feneSurfaceObjList != null) {
			for (IDFObject feneSurface : feneSurfaceObjList) {
				String surfaceType = "window";
				String tempSurfaceType = feneSurface.getDataByStandardComment("Surface Type");
				if (tempSurfaceType.contains("Daylight")) {
					surfaceType = "skylight";
				} else if (tempSurfaceType.equalsIgnoreCase("Window")) {
					surfaceType = "window";
				} else if(tempSurfaceType.equalsIgnoreCase("Door")) {
					surfaceType = "door";
				}

				// set the construction name for the fenestration
				String constructionName = null;
				if (surfaceType != null) {
					if (surfaceType.equals("window")) {
						constructionName = WINDOW;
					} else if (surfaceType.equals("skylight")) {
						constructionName = SKYLIGHT;
					} else if (surfaceType.equals("door")) {
						constructionName = DOOR;
					}
				}
				feneSurface.setDataByStandardComment("Construction Name", constructionName);
			}
		}
	}

	/**
	 * replace the internal mass objects with updated constructions. So far this
	 * method only replace the constructions with internal walls (partitions) regard
	 * to the generated idf file from Asset Score Tool
	 */
	private void replaceInternalMass() {
		List<IDFObject> internalMassObjList = baselineModel.getCategoryList(BLDG_INTERNAL_MASS);
		if (internalMassObjList != null) {
			for (IDFObject internalMass : internalMassObjList) {
				internalMass.setDataByStandardComment("Construction Name", PARTITION);
			}
		}
	}

	/**
	 * replace the building surface constructions
	 */
	private void replaceBuildingSurface() {
		List<IDFObject> surfaceObjList = baselineModel.getCategoryList(BLDG_SURFACE);
		if (surfaceObjList != null) {
			for (IDFObject surface : surfaceObjList) {
				String surfaceType = surface.getDataByStandardComment("Surface Type");
				String outsideBoundary = surface.getDataByStandardComment("Outside Boundary Condition");
				String revisedOB = null;
				String constructionName = null;

				if (surfaceType != null && outsideBoundary != null) {
					if (surfaceType.equalsIgnoreCase("WALL") && outsideBoundary.equalsIgnoreCase("OUTDOORS")) {
						constructionName = EXTERNAL_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL") && outsideBoundary.equalsIgnoreCase("SURFACE")) {
						constructionName = PARTITION;
					} else if(surfaceType.equalsIgnoreCase("WALL") && outsideBoundary.equalsIgnoreCase("ZONE")){
						constructionName = PARTITION;
					} else if (surfaceType.equalsIgnoreCase("WALL") && outsideBoundary.equalsIgnoreCase("GROUND")) {
						constructionName = BG_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("GroundFCfactorMethod")) {
						constructionName = BG_WALL;
						revisedOB = "GROUND";
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("GroundBasementPreprocessorAverageWall")) {
						constructionName = BG_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("GroundBasementPreprocessorUpperWall")) {
						constructionName = BG_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("GroundBasementPreprocessorLowerWall")) {
						constructionName = BG_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("OtherSideCoefficients")) {
						// other side coefficient can be anything - default to external wall
						constructionName = EXTERNAL_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("OtherSideConditionsModel")) {
						// other side model can be anything - default to external wall
						constructionName = EXTERNAL_WALL;
					} else if (surfaceType.equalsIgnoreCase("WALL")
							&& outsideBoundary.equalsIgnoreCase("Adiabatic")) {
						constructionName = EXTERNAL_WALL;
					} else if (surfaceType.equalsIgnoreCase("FLOOR") && outsideBoundary.equalsIgnoreCase("SURFACE")) {
						constructionName = INTERNAL_FLOOR;
					} else if(surfaceType.equalsIgnoreCase("FLOOR") && outsideBoundary.equalsIgnoreCase("ZONE")) {
						constructionName = INTERNAL_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("FLOOR") && outsideBoundary.equalsIgnoreCase("OUTDOORS")) {
						constructionName = EXTERNAL_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("FLOOR") && outsideBoundary.equalsIgnoreCase("Adiabatic")) {
						constructionName = EXTERNAL_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("FLOOR")
							&& outsideBoundary.equalsIgnoreCase("GroundBasementPreprocessorAverageFloor")) {
						constructionName = EXTERNAL_FLOOR;
						revisedOB = "Ground";
					} else if(surfaceType.equalsIgnoreCase("FLOOR") &&
							outsideBoundary.equalsIgnoreCase("GroundFCfactorMethod")){
						constructionName = SOG_FLOOR;
						revisedOB = "Ground";
					} else if (surfaceType.equalsIgnoreCase("FLOOR") && outsideBoundary.equalsIgnoreCase("GROUND")) {
						constructionName = SOG_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("FLOOR")
							&& outsideBoundary.equalsIgnoreCase("GroundSlabPreprocessorAverage")) {
						constructionName = SOG_FLOOR;
						revisedOB = "Ground";
					} else if (surfaceType.equalsIgnoreCase("FLOOR")
							&& outsideBoundary.equalsIgnoreCase("GroundSlabPreprocessorCore")) {
						constructionName = SOG_FLOOR;
						revisedOB = "Ground";
					} else if (surfaceType.equalsIgnoreCase("FLOOR")
							&& outsideBoundary.equalsIgnoreCase("GroundSlabPreprocessorPerimeter")) {
						constructionName = SOG_FLOOR;
						revisedOB = "Ground";
					} else if (surfaceType.equalsIgnoreCase("FLOOR")
							&& outsideBoundary.equalsIgnoreCase("OtherSideCoefficients")) {
						// other side coefficient can be anything - default to external floor
						constructionName = EXTERNAL_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("FLOOR")
							&& outsideBoundary.equalsIgnoreCase("OtherSideConditionsModel")) {
						// other side model can be anything - default to external floor
						constructionName = EXTERNAL_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("CEILING")) {
						constructionName = INTERNAL_FLOOR;
					} else if (surfaceType.equalsIgnoreCase("ROOF")) {
						constructionName = ROOF;
					} else {
						constructionName = ""; // hopefully never gets to this
						// point
					}
				}
				surface.setDataByStandardComment("Construction Name", constructionName);
				
				if(revisedOB!=null) {
					surface.setDataByStandardComment("Outside Boundary Condition", revisedOB);
				}
			}
		}
	}

	/**
	 * This function is use for debug purpose
	 * 
	 * @param fileDir
	 */
	public void exportMergeResults(String fileDir) {
		FileWriter file = null;
		try {
			file = new FileWriter(fileDir);
			file.write(baselineModel.getModelFileContent());

			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exportMergeResults(File file) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
			fw.write(baselineModel.getModelFileContent());

			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
