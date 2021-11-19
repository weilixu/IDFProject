package main.java.model.gbXML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.Namespace;

import main.java.model.idd.EnergyPlusObjectTemplate;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class CampusTranslator {

	private HashMap<String, String> bs_idToObjectMap;
	private HashMap<String, GbXMLThermalZone> bs_idToThermalZoneMap;
	private HashMap<String, GbXMLSpace> bs_idToSpaceMap;
	private Namespace ns;
	private Double lengthMultiplier;
	private Double totalFloorArea;
	private int numberOfFloors;// for baseline purpose

	private EnvelopeTranslator envelopeTranslator;
	private ScheduleTranslator scheduleTranslator;

	private String areaUnit;
	private String volumnUnit;
	private String bldgType;
	private IDDParser iddParser;

	private ArrayList<String> lines;
	private ArrayList<String> units;
	private ArrayList<String> comments;
	private ArrayList<String> topComments;

	private GbXMLResources resources;// resources to fill out the missing data

	public CampusTranslator(Namespace ns, Double multiplier, IDDParser parser) {
		bs_idToObjectMap = new HashMap<String, String>();
		bs_idToThermalZoneMap = new HashMap<String, GbXMLThermalZone>();
		bs_idToSpaceMap = new HashMap<String, GbXMLSpace>();
		this.ns = ns;

		lengthMultiplier = multiplier;
		this.iddParser = parser;

		lines = new ArrayList<String>();
		units = new ArrayList<String>();
		comments = new ArrayList<String>();
		topComments = new ArrayList<String>();

		resources = new GbXMLResources();
	}

	public void setEnvelopeTranslator(EnvelopeTranslator et) {
		envelopeTranslator = et;
	}

	public void setScheduleTranslator(ScheduleTranslator st) {
		scheduleTranslator = st;
	}

	public void setAreaUnit(String unit) {
		areaUnit = unit;
	}

	public void setVolumnUnit(String unit) {
		volumnUnit = unit;
	}

	public Double getTotalFloorArea() {
		return totalFloorArea;
	}

	public Integer getNumberOfFloor() {
		return numberOfFloors;
	}
	
	public String getBldgType() {
		return bldgType;
	}
	
	public HashMap<String, List<String>> getFloorToZoneMap(){
		HashMap<String, List<String>> floorToZoneMap = new HashMap<String, List<String>>();
		Iterator<String> spaceItr = bs_idToSpaceMap.keySet().iterator();
		while(spaceItr.hasNext()) {
			GbXMLSpace space = bs_idToSpaceMap.get(spaceItr.next());
			String floor = space.getBuildingStoreyId();
			String zoneName = space.getSpaceName();
			if(!floorToZoneMap.containsKey(floor)) {
				floorToZoneMap.put(floor, new ArrayList<String>());
			}
			floorToZoneMap.get(floor).add(zoneName);
		}
		
		return floorToZoneMap;
	}

	public HashMap<String, GbXMLSpace> getSpaceMap() {
		return bs_idToSpaceMap;
	}

	/**
	 * For HVAC process, get all the spaces using the same thermal zone object
	 * 
	 * @param zone
	 * @return
	 */
	protected ArrayList<String> getSpacesInAThermalZone(String zone) {
		ArrayList<String> spaces = new ArrayList<String>();

		Iterator<String> spaceKeyItr = bs_idToSpaceMap.keySet().iterator();
		while (spaceKeyItr.hasNext()) {
			String key = spaceKeyItr.next();
			GbXMLSpace space = bs_idToSpaceMap.get(key);
			if (space.getThermalZoneId().equalsIgnoreCase(zone)) {
				spaces.add(space.getSpaceId());
			}
		}
		return spaces;
	}

	public void translateThermalZone(Element element) {
		// this is just translation of thermal zone for the space mapping later
		String id = element.getAttributeValue("id");
		GbXMLThermalZone tz = new GbXMLThermalZone(ns);
		tz.translateThermalZone(element);
		bs_idToThermalZoneMap.put(id, tz);
	}

	public void translateCampus(Element element, IDFFileObject file) {
		// site information
		Element siteInfo = element.getChild("Location", ns);
		String name = "";
		if (siteInfo != null) {
			// TODO Error, the gbXML file is not a valid file, missing
			// <Location>
			name = siteInfo.getChildText("Name",ns);
			name = name.replaceAll(",", "_");
			
			String latitude = siteInfo.getChildText("Latitude",ns);
			if (latitude == null) {
				latitude = "";
			}
			String longitude = siteInfo.getChildText("Longitude",ns);
			if (longitude == null) {
				longitude = "";
			}
			String elevation = siteInfo.getChildText("Elevation",ns);
			if (elevation == null) {
				elevation = "";
			}
			
			recordInputs("Site:Location", "", "", "");
			recordInputs(name, "", "Name", "");
			recordInputs(latitude, "deg", "Latitude", "");
			recordInputs(longitude, "deg", "Longitude", "");
			recordInputs("", "hr", "Time Zone", "");
			recordInputs(elevation, "m", "Elevation", "");
			addObject(file);
		}
		
		if (name == null) {
			name = "Facility";
		}

		Element buildingElement = element.getChild("Building", ns);
		// a valide gbXML must have a building
		if (buildingElement == null) {
			// TODO Error, the gbXML file is not a valid file, missing
			// <Building>
		}
		// analyze building
		translateBuilding(buildingElement, file);

		// translate surfaces
		List<Element> surfaceElements = element.getChildren("Surface", ns);
		for (int i = 0; i < surfaceElements.size(); i++) {
			Element surface = surfaceElements.get(i);
			translateSurface(surface, file);
		}
	}

	public void convertBuilding(IDFFileObject file) {
		// convert bs_idToSpaceMap
		Iterator<String> spacesIt = bs_idToSpaceMap.keySet().iterator();
		while (spacesIt.hasNext()) {
			GbXMLSpace space = bs_idToSpaceMap.get(spacesIt.next());
			// first of all complete the zone object
			String conditionType = space.getConditionType();
			// System.out.println(conditionType);
			String zoneAlgorithm = "";
			if (conditionType.equals("Heated") || conditionType.equals("Cooled")
					|| conditionType.equals("HeatedAndCooled") || conditionType.equals("Unconditioned")) {
				zoneAlgorithm = "TARP";
			} else {
				zoneAlgorithm = "CeilingDiffuser";
			}
			// add zones
			EnergyPlusObjectTemplate zoneTemp = iddParser.getObject("Zone");
			IDFObject zone = new IDFObject("Zone", zoneTemp.getNumberOfFields() + 1);
			zone.setTopComments(new String[] { "!- Generated by BuildSimHub" });
			zone.setIndexedStandardComment(0, "Name");
			zone.setIndexedData(0, space.getSpaceName());
			
			for(int i=1; i<zoneTemp.getNumberOfFields(); i++) {
				String fieldName = zoneTemp.getFieldTemplateByIndex(i).getFieldName();
				zone.setIndexedStandardComment(i, fieldName);
				
				if(fieldName.equals("Volume")) {
					zone.setIndexedData(i, space.getVolume().toString(), "m3");
				}else if(fieldName.equals("Area")) {
					zone.setIndexedData(i, space.getArea().toString(), "m2");
				}else if(fieldName.equals("Zone Inside Convection Algorithm")) {
					zone.setIndexedData(i, zoneAlgorithm);
				}else {
					String value = "";
					if(zoneTemp.getFieldTemplateByIndex(i).getDefault()!=null) {
						value = zoneTemp.getFieldTemplateByIndex(i).getDefault();
					}
					String unit = null;
					if(zoneTemp.getFieldTemplateByIndex(i).getUnit()!=null) {
						unit = zoneTemp.getFieldTemplateByIndex(i).getUnit();
					}
					if(unit == null) {
						zone.setIndexedData(i, value);
					}else {
						zone.setIndexedData(i, value, unit);
					}
				}
			}
			file.addObject(zone);
		
			Map<String, String[]> oaMap = resources.getOAforSpace(space.getSpaceName());
			Map<String, HashMap<String, String[]>> internalLoadMap = resources
					.getInternalLoadforSpace(space.getSpaceName());
			// add People
			if (conditionType.equals("Heated") || conditionType.equals("Cooled")
					|| conditionType.equals("HeatedAndCooled") || conditionType.equals("Unconditioned")) {

				Double numPeople = space.getPeopleNumber();
				if (conditionType.equals("Unconditioned")) {
					numPeople = 0.0; // force to 0.0 for unconditioned spaces.
				}

				if (numPeople == null) {
					numPeople = Double.valueOf(oaMap.get("PeopleNumber")[0]);
					// TODO Warning, the people value is empty, fill it in based
					// on its spaceType
				}
				
				Double heatGain = 120.0;
				if(space.getPeopleHeatGain()!=null) {
					heatGain = space.getPeopleHeatGain()[0] + space.getPeopleHeatGain()[1];
				}
				
				String pplSchedule = "";

				if (space.getPeopleScheduleId() == null) {
					// TODO the working schedule should be able to define (12/7,
					// 8/5 etc.)
					pplSchedule = scheduleTranslator.getScheduleNameFromID(ScheduleTranslator.BUILDING_OCC_SCHEDULE);
					if (pplSchedule == null) {
						pplSchedule = ScheduleTranslator.BUILDING_OCC_SCHEDULE;
						if (scheduleTranslator.getScheduleNameFromID(pplSchedule) == null) {
							scheduleTranslator.addPeopleSchedule(ScheduleTranslator.BUILDING_OCC_SCHEDULE,
									ScheduleTranslator.BUILDING_OCC_SCHEDULE, file);
						}
					}

				} else {
					pplSchedule = space.getPeopleScheduleId();
				}
				
				// generate activity schedule
				String id = "" + heatGain.hashCode();
				String scheduleName = scheduleTranslator.getScheduleNameFromID(id);
				if (scheduleName == null) {
					scheduleTranslator.addSimpleCompactSchedule("Activity Schedule", id, heatGain, file);
					scheduleName = scheduleTranslator.getScheduleNameFromID(id);
				}
				
				EnergyPlusObjectTemplate peopleTemp = iddParser.getObject("People");
				IDFObject people = new IDFObject("People", peopleTemp.getNumberOfMinFields() + 1);
				people.setTopComments(new String[] { "!- Generated by BuildSimHub" });
				for(int i=0; i<peopleTemp.getNumberOfMinFields(); i++) {
					String fieldName = peopleTemp.getFieldTemplateByIndex(i).getFieldName();
					people.setIndexedStandardComment(i, fieldName);
					if(fieldName.equals("Name")) {
						people.setIndexedData(i, space.getSpaceName() + " People");
					}else if(fieldName.equals("Zone or ZoneList Name")) {
						people.setIndexedData(i, space.getSpaceName());
					}else if(fieldName.equals("Number of People Schedule Name")) {
						people.setIndexedData(i, scheduleTranslator.getScheduleNameFromID(pplSchedule));
					}else if(fieldName.equals("Number of People Calculation Method")) {
						if(numPeople <= 0) {
							people.setIndexedData(i, "People");
						}else {
							people.setIndexedData(i, "Area/Person");
						}
					}else if(fieldName.equals("Number of People")){
						people.setIndexedData(i, "0");
					}else if(fieldName.equals("People per Zone Floor Area")) {
						people.setIndexedData(i, "","person/m2");
					}else if(fieldName.equals("Zone Floor Area per Person")) {
						if(numPeople > 0) {
							people.setIndexedData(i, numPeople.toString(), "m2/person");
						}else {
							people.setIndexedData(i, "","m2/person");
						}
					}else if(fieldName.equals("Fraction Radiant")) {
						people.setIndexedData(i, "","m2/person");
					}else if(fieldName.equals("Sensible Heat Fraction")) {
						people.setIndexedData(i, "autocalculate");
					}else if(fieldName.equals("Activity Level Schedule Name")) {
						people.setIndexedData(i, scheduleName);
					}else {
						String value = "";
						if(peopleTemp.getFieldTemplateByIndex(i).getDefault()!=null) {
							value = peopleTemp.getFieldTemplateByIndex(i).getDefault();
						}
						String unit = null;
						if(peopleTemp.getFieldTemplateByIndex(i).getUnit()!=null) {
							unit = peopleTemp.getFieldTemplateByIndex(i).getUnit();
						}
						
						if(unit!=null) {
							people.setIndexedData(i, value, unit);
						}else {
							people.setIndexedData(i, value);
						}
					}
				}
				file.addObject(people);
			}
			// add lights
			Double lightPowerPerArea = space.getLightPowerPerArea();
			if (lightPowerPerArea == null) {
				lightPowerPerArea = Double.parseDouble(internalLoadMap.get("LightPowerPerArea").get("Electricity")[0]);
				lightPowerPerArea = lightPowerPerArea * GbXMLUnitConversion.powerPerAreaRate(
						internalLoadMap.get("LightPowerPerArea").get("Electricity")[1], "WattPerSquareMeter");
			}
			String lightSchedule = space.getLightScheduleId();
			if (lightSchedule == null) {
				lightSchedule = ScheduleTranslator.BUILDING_LIGHT_SCHEDULE;
				if (scheduleTranslator.getScheduleNameFromID(lightSchedule) == null) {
					scheduleTranslator.addLightSchedule(ScheduleTranslator.BUILDING_LIGHT_SCHEDULE,
							ScheduleTranslator.BUILDING_LIGHT_SCHEDULE, file);
				}
			}
			EnergyPlusObjectTemplate lightTemp = iddParser.getObject("Lights");
			IDFObject light = new IDFObject("Lights", lightTemp.getNumberOfMinFields() + 1);
			light.setTopComments(new String[] { "!- Generated by BuildSimHub" });
			for(int i=0; i<lightTemp.getNumberOfMinFields(); i++) {
				
			}
			
			recordInputs("Lights", "", "", "");
			recordInputs(space.getSpaceName() + " Lights", "", "Name", "");
			recordInputs(space.getSpaceName(), "", "Zone or ZoneList Name", "");
			recordInputs(scheduleTranslator.getScheduleNameFromID(lightSchedule), "", "Schedule Name", "");
			recordInputs("Watts/Area", "", "Design Level Calculation Method", "");
			recordInputs("", "W", "Lighting Level", "");
			recordInputs(lightPowerPerArea.toString(), "W/m2", "Watts per Zone Floor Area", "");
			recordInputs("", "W/person", "Watts per Person", "");
			recordInputs("0", "", "Return Air Fraction", "");
			// TODO Warning - The values for the fractions are assumed to be
			// Pendant Direct/Indirect, T8.
			// more options refer to Table 1.22 in INputOutput Reference
			recordInputs("0.32", "", "Fraction Radiant", "");
			recordInputs("0.23", "", "Fraction Visible", "");
			recordInputs("0.45", "", "Fraction Replaceable", "");
			recordInputs("General", "", "End-Use Subcategory", "");
			recordInputs("No", "", "Return Air Fraction Calculated from Plenum Temperature", "");
			addObject(file);

			// int eletric equipment
			Double equipmentPowerPerArea = space.getEquipPowerPerArea();
			if (equipmentPowerPerArea == null) {
				equipmentPowerPerArea = Double
						.parseDouble(internalLoadMap.get("EquipPowerPerArea").get("Electricity")[0]);
				equipmentPowerPerArea = equipmentPowerPerArea * GbXMLUnitConversion.powerPerAreaRate(
						internalLoadMap.get("EquipPowerPerArea").get("Electricity")[1], "WattPerSquareMeter");
			}
			String equipmentSchedule = space.getEquipmentScheduleId();
			if (equipmentSchedule == null) {
				equipmentSchedule = ScheduleTranslator.BUILDING_EQUIP_SCHEDUEL;
				if (scheduleTranslator.getScheduleNameFromID(equipmentSchedule) == null) {
					scheduleTranslator.addLightSchedule(ScheduleTranslator.BUILDING_EQUIP_SCHEDUEL,
							ScheduleTranslator.BUILDING_EQUIP_SCHEDUEL, file);
				}
			}

			recordInputs("ElectricEquipment", "", "", "");
			recordInputs(space.getSpaceName() + " ElectricEquipment", "", "Name", "");
			recordInputs(space.getSpaceName(), "", "Zone or ZoneList Name", "");
			recordInputs(scheduleTranslator.getScheduleNameFromID(equipmentSchedule), "", "Schedule Name", "");
			recordInputs("Watts/Area", "", "Design Level Calculation Method", "");
			recordInputs("", "W", "Design Level", "");
			recordInputs(equipmentPowerPerArea.toString(), "W/m2", "Watts per Zone Floor Area", "");
			recordInputs("", "W/person", "Watts per Person", "");
			recordInputs("0.0", "", "Fraction Latent", "");
			recordInputs("0.3", "", "Fraction Radiant", "");
			recordInputs("0.2", "", "Fraction Lost", "");
			recordInputs("General", "", "End-Use Subcategory", "");
			addObject(file);

			// zone infiltration: design flow rate
			Double infiltrationFlow = space.getInfiltrationFlow();
			String infiltrationUnit = space.getInfiltrationUnit();
			recordInputs("ZoneInfiltration:DesignFlowRate", "", "", "");
			recordInputs(space.getSpaceName() + " Infiltration", "", "Name", "");
			recordInputs(space.getSpaceName(), "", "Zone or ZoneList Name", "");

			// generate infiltration schedule
			// TODO warning - infiltration schedule is set to 1 for all year
			// around
			Double infiltrationSchedValue = 1.0;
			String id = "" + infiltrationSchedValue.hashCode();
			String infiltrationSchedule = scheduleTranslator.getScheduleNameFromID(id);
			if (infiltrationSchedule == null) {
				scheduleTranslator.addSimpleCompactSchedule("Infiltration Schedule", id, infiltrationSchedValue, file);
				infiltrationSchedule = scheduleTranslator.getScheduleNameFromID(id);
			}
			recordInputs(infiltrationSchedule, "", "Schedule Name", "");
			if (infiltrationUnit.equals("m2")) {
				recordInputs("Flow/zone", "", "Design Flow Rate Calculation Method", "");
				recordInputs(infiltrationFlow.toString(), "m3/s", "Design Flow Rate", "");
				recordInputs("", "m3/s-m2", "Flow per Zone Floor Area", "");
				recordInputs("", "m3/s-m2", "Flow per Exterior Surface Area", "");
				recordInputs("", "1/hr", "Air Change per Hour", "");
			} else {
				recordInputs("AirChanges/Hour", "", "Design Flow Rate Calculation Method", "");
				recordInputs("", "m3/s", "Design Flow Rate", "");
				recordInputs("", "m3/s-m2", "Flow per Zone Floor Area", "");
				recordInputs("", "m3/s-m2", "Flow per Exterior Surface Area", "");
				recordInputs(infiltrationFlow.toString(), "1/hr", "Air Change per Hour", "");
			}
			recordInputs("1", "", "Constant Term Coefficient", "");
			recordInputs("0", "", "Temperature Term Coefficient", "");
			recordInputs("0", "", "Velocity Term Coefficient", "");
			recordInputs("0", "", "Velocity Squared Term Coefficient", "");
			addObject(file);
			
			//System.out.println(conditionType);
			//System.out.println(bs_idToThermalZoneMap);
			// Process Thermal Zone
			// Add last condition to make sure if there is no thermal zone defined, then
			// we assume all the thermal zones are conditioned.
			if (conditionType.equals("Heated") || conditionType.equals("Cooled")
					|| conditionType.equals("HeatedAndCooled") || bs_idToThermalZoneMap.isEmpty()) {
				convertThermalZone(space.getSpaceName(), space.getSpaceType(), space.getThermalZone(), file, oaMap);
			}
		}
	}

	private void translateSurface(Element element, IDFFileObject file) {
		Element planarGeometryElement = element.getChild("PlanarGeometry", ns);
		Element polyLoopElement = planarGeometryElement.getChild("PolyLoop", ns);
		List<Element> cartesianPointElements = polyLoopElement.getChildren("CartesianPoint", ns);

		LinkedList<Double[]> coordinateList = new LinkedList<Double[]>();

		Element rectangular = element.getChild("RectangularGeometry", ns);
		Double tiltAngle = Double.parseDouble(rectangular.getChildText("Tilt", ns));

		for (int i = 0; i < cartesianPointElements.size(); i++) {
			List<Element> coordianteElements = cartesianPointElements.get(i).getChildren("Coordinate", ns);
			if (coordianteElements.size() != 3) {
				// TODO Error - one cartesianpoint should have 3 coordinates to
				// represents x, y, z.
				// current number of points is: coordianteElements.size(). Check
				// surface element.getAttributeValue("id")
				// STOP Processing
			}

			Double x = lengthMultiplier * Double.parseDouble(coordianteElements.get(0).getText());
			Double y = lengthMultiplier * Double.parseDouble(coordianteElements.get(1).getText());
			Double z = lengthMultiplier * Double.parseDouble(coordianteElements.get(2).getText());
			Double[] point = { x, y, z };
			coordinateList.add(point);
		}

		String surfaceType = element.getAttributeValue("surfaceType");
		if (surfaceType.contains("Shade")) {
			// process it as a shading surface
		} else if (surfaceType.contains("FreestandingColumn") || surfaceType.contains("EmbeddedColumn")) {
			// do nothing
			return;
		} else {
			// regular building surfaces
			List<Element> adjacentSpaceElements = element.getChildren("AdjacentSpaceId", ns);
			if (adjacentSpaceElements.isEmpty()) {
				// TODO warning: (surface has no adjacent spaces, will not be
				// translated)
				return;
			} else if (adjacentSpaceElements.size() == 2) {
				String spaceId1 = adjacentSpaceElements.get(0).getAttributeValue("spaceIdRef");
				String spaceId2 = adjacentSpaceElements.get(1).getAttributeValue("spaceIdRef");
				if (spaceId1.equals(spaceId2)) {
					// TODO warning: surface has two adjacent spaces which are
					// the same space: + spaceId1 + will not be translated
					return;
				}
			} else if (adjacentSpaceElements.size() > 2) {
				// TODO Error, Surface has more than 2 adjacent surfaces, will
				// not be translated
				return;
			}

			String surfaceId = element.getAttributeValue("id");
			String surfaceName = element.getChildText("Name", ns);
			if (surfaceName == null) {
				surfaceName = "";
			}
			surfaceName = escapeName(surfaceId, surfaceName);

			bs_idToObjectMap.put(surfaceId, surfaceName);

			String exposedToSun = element.getAttributeValue("exposedToSun");
			if (exposedToSun == null) {
				exposedToSun = "false";
			}

			// set surface type
			String eplusSurfaceType = "Wall";
			// wall types
			if (surfaceType.contains("ExteriorWall") || surfaceType.contains("InteriorWall")
					|| surfaceType.contains("UndergroundWall")) {
				eplusSurfaceType = "Wall";
				// roof types
			} else if (surfaceType.contains("Roof")) {
				eplusSurfaceType = "Roof";
				// ceiling type
			} else if (surfaceType.contains("Ceiling") || surfaceType.contains("UndergroundCeiling")) {
				eplusSurfaceType = "Ceiling";
				// floor type
			} else if (surfaceType.contains("UndergroundSlab") || surfaceType.contains("SlabOnGrade")
					|| surfaceType.contains("InteriorFloor") || surfaceType.contains("RaisedFloor")
					|| surfaceType.contains("ExposedFloor")) {
				eplusSurfaceType = "Floor";
			} else if (surfaceType.contains("Air")) {
				eplusSurfaceType = assignDefaultSurfaceType(tiltAngle);
				if (eplusSurfaceType.equals("RoofCeiling")) {
					eplusSurfaceType = "Ceiling"; // roof should never been air
				}
			}

			// set boundary conditions
			String outsideBoundaryCondition = "Surface";
			String sunExposure = "NoSun";
			String windExposure = "NoWind";
			if (exposedToSun.contains("true")) {
				outsideBoundaryCondition = "Outdoors";
				sunExposure = "SunExposed";
				windExposure = "WindExposed";
			} else if (surfaceType.contains("InteriorWall")) {
				outsideBoundaryCondition = "Adiabatic";
				sunExposure = "NoSun";
				windExposure = "NoWind";
			} else if (surfaceType.contains("SlabOnGrade")) {
				outsideBoundaryCondition = "Ground";
				sunExposure = "NoSun";
				windExposure = "NoWind";
			}

			String constructionName = "";
			String constructionIdRef = "";
			// TODO if air wall? how to solve this?
			if (surfaceType.contains("Air")) {

				String id = eplusSurfaceType + "Air";
				constructionName = envelopeTranslator.getObjectName(id);
				if (constructionName == null) {
					envelopeTranslator.addAirConstruction(eplusSurfaceType, id, file);
					constructionName = envelopeTranslator.getObjectName(id);
				}
			} else {
				// not air wall
				constructionIdRef = element.getAttributeValue("constructionIdRef");
				constructionName = envelopeTranslator.getObjectName(constructionIdRef);
			}

			if (constructionName == null) {
				// TODO this means there is no construction assigned to this
				// surface in the original gbXML
				// set to ""
				// WX possible link to our database in the future?
				if (surfaceType.contains("ExteriorWall")) {
					constructionName = "Project Wall";
				} else if (surfaceType.contains("InteriorWall")) {
					constructionName = "Project Partition";
				} else if (surfaceType.contains("UndergroundWall")) {
					constructionName = "Project Below Grade Wall";
					// roof types
				} else if (surfaceType.contains("Roof")) {
					constructionName = "Project Roof";
					// ceiling type
				} else if (surfaceType.contains("Ceiling") || surfaceType.contains("UndergroundCeiling")
						|| surfaceType.contains("InteriorFloor")) {
					constructionName = "Project Internal Floor";
					// floor type
				} else if (surfaceType.contains("UndergroundSlab") || surfaceType.contains("RaisedFloor")
						|| surfaceType.contains("ExposedFloor")) {
					constructionName = "Project External Floor";
				} else if (surfaceType.contains("SlabOnGrade")) {
					constructionName = "Project Slab On Grade Floor";
				} else if (surfaceType.contains("Air")) {
					constructionName = "Project Internal Floor";
				}
			}

			// translate subsurfaces
			List<Element> subSurfaces = element.getChildren("Opening", ns);
			for (Element ss : subSurfaces) {
				translateSubSurface(ss, file, surfaceName);
			}
			// adjacent surface
			String spaceId = adjacentSpaceElements.get(0).getAttributeValue("spaceIdRef");
			if (spaceId == null) {
				// TODO Error: there is no space Id in the space.
				
			}
			GbXMLSpace s = bs_idToSpaceMap.get(spaceId);
			if (s == null) {
				// TODO Error: there is no space have matching ID: +spaceId;
			}
			// assign spaceName for the surface
			String space1Name = s.getSpaceName();

			// add object label
			recordInputs("BuildingSurface:Detailed", "", "", "");

			// must be an interior surface: ceiling/floor or interior wall
			if (adjacentSpaceElements.size() == 2) {
				String adjacentSpaceId = adjacentSpaceElements.get(1).getAttributeValue("spaceIdRef");
				GbXMLSpace as = bs_idToSpaceMap.get(adjacentSpaceId);

				// deal with the ceiling/floors - unify the surface types
				String tempSurfType = assignDefaultSurfaceType(tiltAngle);
				if (!tempSurfType.contains(eplusSurfaceType)) {
					// TODO Warning, changing surface type from:
					// eplusSurfaceType to tempSurfType for surface: surfaceName
					if (tempSurfType.equals("RoofCeiling")) {
						eplusSurfaceType = "Ceiling";
					} else {
						eplusSurfaceType = tempSurfType;
					}
				}

				// clone the surface and sub surfaces and reverse vertices
				String space2Name = as.getSpaceName();

				// create two
				// first the original one
				recordInputs(surfaceName, "", "Name", "");
				recordInputs(eplusSurfaceType, "", "Surface Type", "");
				recordInputs(constructionName, "", "Construction Name", "");
				recordInputs(space1Name, "", "Zone Name", "");
				recordInputs(outsideBoundaryCondition, "", "Outside Boundary Condition", "");
				recordInputs(surfaceName + "_reversed", "", "Outside Boundary Condition Object", "");
				recordInputs(sunExposure, "", "Sun Exposure", "");
				recordInputs(windExposure, "", "Wind Exposure", "");
				recordInputs("", "", "View Factor to Ground", "");
				recordInputs("", "", "Number of Vertices", "");

				for (int j = 0; j < coordinateList.size(); j++) {
					Double[] point = coordinateList.get(j);
					for (int k = 0; k < 3; k++) {
						recordInputs(point[k].toString(), "m", "Vertex " + (j + 1) + " coordinate", "");
					} // for
				} // for
				addObject(file); // add

				// second
				// reverse construction
				String reversedConsName = envelopeTranslator.getObjectName(constructionIdRef + "_reversed");
				IDFObject reversedConstruction = null;
				if (reversedConsName == null) {
					Map<String, IDFObject> constructionMap = file.getCategoryMap("Construction");
					if (constructionMap != null) {
						// WX, in case there is no construction in the original
						// gbXML
						Iterator<String> constructionObjects = constructionMap.keySet().iterator();
						while (constructionObjects.hasNext()) {
							IDFObject idfOb = constructionMap.get(constructionObjects.next());
							if (idfOb.getName().equalsIgnoreCase(constructionName)) {
								// System.out.println(constructionName);

								reversedConstruction = reverseConstruction(idfOb);
								break;
							}
						}
						file.addObject(reversedConstruction);

						if (reversedConstruction == null) {
							// did not find the construction set to "";
							// this may be triggled because: no construction
							// element in gbXML,
							// but air construction is built for surface type
							// air
							reversedConsName = "";
						} else {
							reversedConsName = reversedConstruction.getName();
						}
						envelopeTranslator.setReversedConstruction(constructionIdRef + "_reversed", reversedConsName);
					} else {
						reversedConsName = "";
					}
				}

				recordInputs("BuildingSurface:Detailed", "", "", "");
				Collections.reverse(coordinateList);

				recordInputs(surfaceName + "_reversed", "", "Name", "");
				recordInputs(eplusSurfaceType, "", "Surface Type", "");
				recordInputs(reversedConsName, "", "Construction Name", "");
				recordInputs(space2Name, "", "Zone Name", "");
				recordInputs(outsideBoundaryCondition, "", "Outside Boundary Condition", "");
				recordInputs(surfaceName, "", "Outside Boundary Condition Object", "");
				recordInputs(sunExposure, "", "Sun Exposure", "");
				recordInputs(windExposure, "", "Wind Exposure", "");
				recordInputs("", "", "View Factor to Ground", "");
				recordInputs("", "", "Number of Vertices", "");

				for (int j = 0; j < coordinateList.size(); j++) {
					Double[] point = coordinateList.get(j);
					for (int k = 0; k < 3; k++) {
						recordInputs(point[k].toString(), "m", "Vertex " + (j + 1) + " coordinate", "");
					} // for
				} // for
				addObject(file); // add
			} else {
				// exterior surface case
				recordInputs(surfaceName, "", "Name", "");
				recordInputs(eplusSurfaceType, "", "Surface Type", "");
				recordInputs(constructionName, "", "Construction Name", "");
				recordInputs(space1Name, "", "Zone Name", "");
				recordInputs(outsideBoundaryCondition, "", "Outside Boundary Condition", "");
				recordInputs("", "", "Outside Boundary Condition Object", "");
				recordInputs(sunExposure, "", "Sun Exposure", "");
				recordInputs(windExposure, "", "Wind Exposure", "");
				recordInputs("", "", "View Factor to Ground", "");
				recordInputs("", "", "Number of Vertices", "");

				for (int j = 0; j < coordinateList.size(); j++) {
					Double[] point = coordinateList.get(j);
					for (int k = 0; k < 3; k++) {
						recordInputs(point[k].toString(), "m", "Vertex " + (j + 1) + " coordinate", "");
					} // for
				} // for
				addObject(file);
			} // if - adjacent surface
		} // if - surfaceType
	}

	private void translateSubSurface(Element element, IDFFileObject file, String hostSurfaceName) {

		Element planarGeometryElement = element.getChild("PlanarGeometry", ns);
		Element polyLoopElement = planarGeometryElement.getChild("PolyLoop", ns);
		List<Element> cartesianPointElements = polyLoopElement.getChildren("CartesianPoint", ns);

		LinkedList<Double[]> coordinateList = new LinkedList<Double[]>();

		for (int i = 0; i < cartesianPointElements.size(); i++) {
			List<Element> coordianteElements = cartesianPointElements.get(i).getChildren("Coordinate", ns);
			if (coordianteElements.size() != 3) {
				// TODO Error - one cartesianpoint should have 3 coordinates to
				// represents x, y, z.
				// current number of points is: coordianteElements.size(). Check
				// surface element.getAttributeValue("id")
				// STOP Processing
			}

			Double x = lengthMultiplier * Double.parseDouble(coordianteElements.get(0).getText());
			Double y = lengthMultiplier * Double.parseDouble(coordianteElements.get(1).getText());
			Double z = lengthMultiplier * Double.parseDouble(coordianteElements.get(2).getText());
			Double[] point = { x, y, z };
			coordinateList.add(point);
		}

		String subSurfaceId = element.getAttributeValue("id");
		String subSurfaceName = element.getChildText("Name", ns);
		if (subSurfaceName == null) {
			subSurfaceName = "";
		}

		bs_idToObjectMap.put(subSurfaceId, subSurfaceName);
		subSurfaceName = escapeName(subSurfaceId, subSurfaceName);

		// translate openingType;
		String openingType = element.getAttributeValue("openingType");
		if (openingType.contains("FixedWindow") || openingType.contains("OperableWindow")
				|| openingType.contains("FixedSkylight") || openingType.contains("OperableSkylight")) {
			openingType = "Window";
		} else if (openingType.contains("SlidingDoor")) {
			openingType = "GlassDoor";
		} else if (openingType.contains("NonSlidingDoor")) {
			openingType = "Door";
			// do we even want to handling air?
		} else if (openingType.contains("Air")) {
			//openingType = "Air";
			openingType = "Window";
		}

		String constructionIdRef = element.getAttributeValue("constructionIdRef");
		if (constructionIdRef == null) {
			constructionIdRef = element.getAttributeValue("windowTypeIdRef");
		}
		String constructionName = "";
		// if air opening?
		if (openingType.contains("Air")) {
			String id = openingType + "Air";
			constructionName = envelopeTranslator.getObjectName(id);
			if (constructionName == null) {
				envelopeTranslator.addAirConstruction(openingType, id, file);
				constructionName = envelopeTranslator.getObjectName(id);
			}
		} else {
			constructionName = envelopeTranslator.getObjectName(constructionIdRef);
		}

		if (constructionName == null) {

			if (openingType.contains("Window")) {
				constructionName = "Project Window";
			} else if (openingType.contains("SlidingDoor") || openingType.contains("NonSlidingDoor") || openingType.contains("GlassDoor")) {
				constructionName = "Project Curtain Wall";
			} else if (openingType.contains("FixedSkylight") || openingType.contains("OperableSkylight")) {
				constructionName = "Project Skylight";
				// do we even want to handling air?
			} else if (openingType.contains("Air")) {
				constructionName = "Project Window";
			}
		}
		// start filling in the data
		if (coordinateList.size() == 4 || coordinateList.size() == 3) {
			recordInputs("FenestrationSurface:Detailed", "", "", "");
			recordInputs(subSurfaceName, "", "Name", "");
			recordInputs(openingType, "", "Surface Type", "");
			recordInputs(constructionName, "", "Construction Name", "");
			recordInputs(hostSurfaceName, "", "Building Surface Name", "");
			recordInputs("", "", "Outside Boundary Condition Object", "");
			recordInputs("autocalculate", "", "View Factor to Ground", "");
			recordInputs("", "", "Shading Control Name", "");
			recordInputs("", "", "Frame and Divider Name", "");
			recordInputs("", "", "Multiplier", "");
			recordInputs("" + coordinateList.size(), "", "Number of Vertices", "");

			for (int i = 0; i < coordinateList.size(); i++) {
				Double[] point = coordinateList.get(i);
				for (int k = 0; k < 3; k++) {
					//System.out.println(point[k]);
					recordInputs(point[k].toString(), "m", "Vertex" + (i + 1) + "-coordinate", "");
				} // for
			} // for
			//System.out.println(subSurfaceName + openingType + constructionName + hostSurfaceName);
			addObject(file);
		} else if (coordinateList.size() > 4) {
			// triangularize the surface
			for (int i = 0; i < coordinateList.size() - 2; i++) {
				// no need to consider the first and last point, so minus 2
				recordInputs("FenestrationSurface:Detailed", "", "", "");
				recordInputs(subSurfaceName + "_" + i, "", "Name", "");
				recordInputs(openingType, "", "Surface Type", "");
				recordInputs(constructionName, "", "Construction Name", "");
				recordInputs(hostSurfaceName, "", "Building Surface Name", "");
				recordInputs("", "", "Outside Boundary Condition Object", "");
				recordInputs("autocalculate", "", "View Factor to Ground", "");
				recordInputs("", "", "Shading Control Name", "");
				recordInputs("", "", "Frame and Divider Name", "");
				recordInputs("", "", "Multiplier", "");
				recordInputs("3", "", "Number of Vertices", "");// triangle
				ArrayList<Double[]> tempCoordinateList = new ArrayList<Double[]>();
				tempCoordinateList.add(coordinateList.get(i));
				tempCoordinateList.add(coordinateList.get(i + 1));
				tempCoordinateList.add(coordinateList.getLast());
				for (int j = 0; j < tempCoordinateList.size(); j++) {
					for (int k = 0; k < 3; k++) {
						recordInputs(tempCoordinateList.get(j)[k].toString(), "m", "Vertex" + (i + 1) + "-coordinate",
								"");
					} // for
				}
				addObject(file);

			}
		}
	}

	private void convertThermalZone(String spaceName, String spaceType, GbXMLThermalZone thermalZone,
			IDFFileObject file, Map<String, String[]> oaMap) {
		// check if the thermal zone is exist{
		if (thermalZone != null) {
			// zone air distribution
			String airDistObjectName = spaceName + " Air Distribution";
			recordInputs("DesignSpecification:ZoneAirDistribution", "", "", "");
			recordInputs(spaceName + " Air Distribution", "", "Name", "");
			recordInputs("1", "", "Zone Air Distribution Effectiveness in Cooling Mode", "");
			recordInputs("0.8", "", "Zone Air Distribution Effectiveness in Heating Mode", "");
			recordInputs("", "", "Zone Air Distribution Effectiveness Schedule Name", "");
			recordInputs("", "", "Zone Secondary Recirculation Fraction", "");
			addObject(file);

			// zone outdoor air
			String oaObjectName = spaceName + " OutdoorAir";
			recordInputs("DesignSpecification:OutdoorAir", "", "", "");
			recordInputs(oaObjectName, "", "Name", "");
			recordInputs("Sum", "", "Outdoor Air Method", "");// TODO warning:
																// outdoor air
																// method is set
																// to "SUM";
			Double oaPerArea = thermalZone.getOAFlowPerArea();
			Double oaPerPerson = thermalZone.getOAFlowPerPerson();
			Double oaPerZone = thermalZone.getOAFlowPerZone();
			Double oaACH = thermalZone.getACH();
			if (oaPerPerson == null) {

				Double tempOA = Double.valueOf(oaMap.get("OAFlowPerPerson")[0]);
				oaPerPerson = tempOA
						* GbXMLUnitConversion.flowConversionRate(oaMap.get("OAFlowPerPerson")[1], "CubicMPerSec");
			}
			recordInputs(oaPerPerson.toString(), "", "Outdoor Air Flow per Person", "");

			if (oaPerArea == null) {
				Double tempOA = Double.valueOf(oaMap.get("OAFlowPerArea")[0]);
				oaPerArea = tempOA * GbXMLUnitConversion.flowPerAreaConversionRate(oaMap.get("OAFlowPerArea")[1],
						"CubicMPerSecPerSquareM");
			}
			recordInputs(oaPerArea.toString(), "", "Outdoor Air Flow per Zone Floor Area", "");

			if (oaPerZone == null) {
				oaPerZone = 0.0;
			}
			recordInputs(oaPerZone.toString(), "", "Outdoor Air Flow per Zone", "");

			if (oaACH == null) {
				oaACH = 0.0;
			}
			recordInputs(oaACH.toString(), "", "Outdoor Air FLow Air Changes per Hour", "");

			String oaScheduleId = thermalZone.getOaScheduleId();
			String osScheduleName = "";
			if (oaScheduleId != null) {
				osScheduleName = scheduleTranslator.getScheduleNameFromID(oaScheduleId);
			}
			// WX check if there is schedule or not
			if (osScheduleName == null) {
				osScheduleName = "";
			}

			recordInputs(osScheduleName, "", "Outdoor Air FLow Rate Fraction Schedule Name", "");
			addObject(file);

			// sizing zone
			recordInputs("Sizing:Zone", "", "", "");
			recordInputs(spaceName, "", "Zone or ZoneList Name", "");
			recordInputs("TemperatureDifference", "", "Zone Cooling Design Supply Air Temperature Input Method", "");
			recordInputs("12.8", "C", "Zone Cooling Design Supply Air Temperature", "");
			recordInputs("11", "deltaC", "Zone Cooling Design Supply Air Temperature Difference", "");
			recordInputs("TemperatureDifference", "", "Zone Heating Design Supply Air Temperature Input Method", "");
			recordInputs("50", "C", "Zone Heating Design Supply Air Temperature", "");
			recordInputs("11", "deltaC", "Zone Heating Design Supply Air Temperature Difference", "");
			recordInputs("0.0103", "", "Zone Cooling Design Supply Air Humidity Ratio", "");
			recordInputs("0.0066", "", "Zone Heating Design Supply Air Humidity Ratio", "");
			recordInputs(oaObjectName, "", "Design Specification Outdoor Air Object Name", "");
			// sizing factors
			Double coolSizeFactor = thermalZone.getCoolingSizingFactor();
			if (coolSizeFactor == null) {
				coolSizeFactor = 1.15;
				// TODO Warning: no cooling sizing factor is found, default to
				// 1.25
			}

			Double heatSizeFactor = thermalZone.getHeatingSizingFactor();
			if (heatSizeFactor == null) {
				heatSizeFactor = 1.25;
				// TODO Warning: no heating sizing factor is found, default to
				// 1.15
			}
			recordInputs(heatSizeFactor.toString(), "", "Zone Heating Sizing Factor", "");
			recordInputs(coolSizeFactor.toString(), "", "Zone Cooling Sizing Factor", "");
			// Design air methods
			// TODO WX: These are somehow not included in the current gbXML
			// thermal zone element, we will
			// go with the default setting in EnergyPlus
			recordInputs("DesignDay", "", "Cooling Design Air Flow Method", "");
			recordInputs("0", "m3/s", "Cooling Design Air Flow Rate", "");
			recordInputs("0.000762", "m3/s-m2", "Cooling Minimum Air Flow per Zone Floor Area", "");
			recordInputs("0", "m3/s", "Cooling Minimum Air Flow", "");
			recordInputs("0", "", "Cooling Minimum Air Fraction", "");

			recordInputs("DesignDay", "", "Heating Design Air Flow Method", "");
			recordInputs("0", "m3/s", "Heating Design Air Flow Rate", "");
			recordInputs("0.002032", "m3/s-m2", "Heating Maximum Air Flow per Zone Floor Area", "");
			recordInputs("01415762", "m3/s", "Heating Maximum Air Flow", "");
			recordInputs("0.3", "", "Heating Maximum Air Fraction", "");
			// air distribution
			recordInputs(airDistObjectName, "", "Design Specification Zone Air Distribution Object Name", "");
			addObject(file);

			// now thermostat
			String heatScheduleId = thermalZone.getHeatScheduleId();
			String coolScheduleId = thermalZone.getCoolScheduleId();

			String thermostatType = "";
			int controlType = thermalZone.getThermostatControlType();
			String thermostatName = null;
			if (controlType == 1) {
				thermostatName = bs_idToObjectMap.get(heatScheduleId);
				thermostatType = "ThermostatSetpoint:SingleHeating";
			} else if (controlType == 2) {
				thermostatName = bs_idToObjectMap.get(coolScheduleId);
				thermostatType = "ThermostatSetpoint:SingleCooling";
			} else if (controlType == 3) {
				thermostatName = bs_idToObjectMap.get(heatScheduleId);
				thermostatType = "ThermostatSetpoint:SingleHeatingOrCooling";
				if (thermostatName == null) {
					thermostatName = bs_idToObjectMap.get(coolScheduleId);
					thermostatType = "ThermostatSetpoint:SingleHeatingOrCooling";
				}
			} else {
				thermostatType = "ThermostatSetpoint:DualSetpoint";
				thermostatName = bs_idToObjectMap.get(heatScheduleId + coolScheduleId);
			}
			String controlTypeSchedule = scheduleTranslator.getScheduleNameFromID("controlType " + controlType);
			if (controlTypeSchedule == null) {
				// build control type schedule
				scheduleTranslator.addSimpleCompactSchedule("Control Type - Always " + controlType,
						"controlType " + controlType, (double) controlType, file);
				controlTypeSchedule = scheduleTranslator.getScheduleNameFromID("controlType " + controlType);
			}
			if (thermostatName == null) {
				// build the thermostat
				if (controlType == 1) {
					thermostatType = "ThermostatSetpoint:SingleHeating";
					recordInputs(thermostatType, "", "Name", "");
					recordInputs(heatScheduleId + " Single Heat", "", "", "");
					String name = scheduleTranslator.getScheduleNameFromID(heatScheduleId);
					if (name == null) {
						recordInputs("", "", "Setpoint Temperature Schedule Name", "");
					} else {
						recordInputs(name, "", "Setpoint Temperature Schedule Name", "");
					}
					addObject(file);
					bs_idToObjectMap.put(heatScheduleId, heatScheduleId + " Single Heat");
					thermostatName = heatScheduleId + " Single Heat";
				} else if (controlType == 2) {
					thermostatType = "ThermostatSetpoint:SingleCooling";
					recordInputs(thermostatType, "", "Name", "");
					recordInputs(coolScheduleId + " Single Cool", "", "", "");
					String name = scheduleTranslator.getScheduleNameFromID(coolScheduleId);
					if (name == null) {
						// TODO Warning no heating schedule found for space:
						// spaceName
						recordInputs("", "", "Setpoint Temperature Schedule Name", "");
					} else {
						recordInputs(name, "", "Setpoint Temperature Schedule Name", "");
					}
					recordInputs(scheduleTranslator.getScheduleNameFromID(coolScheduleId), "",
							"Setpoint Temperature Schedule Name", "");
					addObject(file);
					bs_idToObjectMap.put(coolScheduleId, coolScheduleId + " Single Cool");
					thermostatName = coolScheduleId + " Single Cool";
				} else if (controlType == 3) {
					// TODO WX never seen the case before, need to revisit
					thermostatType = "ThermostatSetpoint:SingleHeatingOrCooling";
					recordInputs(thermostatType, "", "Name", "");
					recordInputs(coolScheduleId + " Single Heat or Cool", "", "", "");
					String name = scheduleTranslator.getScheduleNameFromID(heatScheduleId);
					if (name == null) {
						// TODO Warning no cooling schedule found for space:
						// spaceName
						recordInputs("", "", "Setpoint Temperature Schedule Name", "");
					} else {
						recordInputs(name, "", "Setpoint Temperature Schedule Name", "");
					}
					recordInputs(scheduleTranslator.getScheduleNameFromID(coolScheduleId), "",
							"Setpoint Temperature Schedule Name", "");
					addObject(file);
					bs_idToObjectMap.put(coolScheduleId, coolScheduleId + " Single Heat or Cool");
					thermostatName = coolScheduleId + " Single Heat or Cool";
				} else if (controlType == 4) {
					thermostatType = "ThermostatSetpoint:DualSetpoint";
					recordInputs(thermostatType, "", "", "");
					recordInputs(heatScheduleId + coolScheduleId + " Dual SP", "", "Name", "");

					String heatName = scheduleTranslator.getScheduleNameFromID(heatScheduleId);
					if (heatName == null) {
						scheduleTranslator.addHeatingSchedule(ScheduleTranslator.BUILDING_HTGSP_SCHEDULE, ScheduleTranslator.BUILDING_HTGSP_SCHEDULE, file);
						recordInputs(ScheduleTranslator.BUILDING_HTGSP_SCHEDULE, "", "Heating Setpoint Temperature Schedule Name", "");
					} else {
						recordInputs(heatName, "", "Heating Setpoint Temperature Schedule Name", "");
					}

					String coolName = scheduleTranslator.getScheduleNameFromID(coolScheduleId);
					if (coolName == null) {
						scheduleTranslator.addHeatingSchedule(ScheduleTranslator.BUILDING_CLGSP_SCHEDULE, ScheduleTranslator.BUILDING_CLGSP_SCHEDULE, file);
						recordInputs(ScheduleTranslator.BUILDING_CLGSP_SCHEDULE, "", "Cooling Setpoint Temperature Schedule Name", "");
					} else {
						recordInputs(coolName, "", "Cooling Setpoint Temperature Schedule Name", "");
					}

					addObject(file);
					bs_idToObjectMap.put(heatScheduleId + coolScheduleId, heatScheduleId + coolScheduleId + " Dual SP");
					thermostatName = heatScheduleId + coolScheduleId + " Dual SP";
				}
			}
			// jump into zone control
			recordInputs("ZoneControl:Thermostat", "", "", "");
			recordInputs(spaceName + " Thermostat", "", "Name", "");
			recordInputs(spaceName, "", "Zone or ZoneList Name", "");
			recordInputs(controlTypeSchedule, "", "Control Type Schedule Name", "");
			recordInputs(thermostatType, "", "Control 1 Object Type", "");
			recordInputs(thermostatName, "", "Control 1 Name", "");
			addObject(file);

		} else {
			// TODO WARNING: there is no thermal zone assigned to the space +
			// spaceName. Default office setting is applied - this should
			// following the
			// baseline template for now. need to revisit later
			// zone air distribution
			String airDistObjectName = spaceName + " Air Distribution";
			recordInputs("DesignSpecification:ZoneAirDistribution", "", "", "");
			recordInputs(airDistObjectName, "", "Name", "");
			recordInputs("1", "", "Zone Air Distribution Effectiveness in Cooling Mode", "");
			recordInputs("0.8", "", "Zone Air Distribution Effectiveness in Heating Mode", "");
			recordInputs("", "", "Zone Air Distribution Effectiveness Schedule Name", "");
			recordInputs("", "", "Zone Secondary Recirculation Fraction", "");
			addObject(file);

			// zone outdoor air
			String oaObjectName = spaceName + " OutdoorAir";
			recordInputs("DesignSpecification:OutdoorAir", "", "", "");
			recordInputs(oaObjectName, "", "Name", "");
			recordInputs("Sum", "", "Outdoor Air Method", "");// TODO warning:
																// outdoor air
																// method is set
																// to "SUM";
			recordInputs("0.0003", "", "Outdoor Air Flow per Person", "");
			recordInputs("0.0025", "", "Outdoor Air Flow per Zone Floor Area", "");
			recordInputs("0.0", "", "Outdoor Air Flow per Zone", "");
			recordInputs("0.0", "", "Outdoor Air FLow Air Changes per Hour", "");
			recordInputs("Default_Office_OA_Schedule", "", "Outdoor Air FLow Rate Fraction Schedule Name", "");
			addObject(file);

			// add this default OA schedule here!
			scheduleTranslator.addSimpleCompactSchedule("Default_Office_OA_Schedule", "OA_id", 1.0, file);

			// sizing zone
			recordInputs("Sizing:Zone", "", "", "");
			recordInputs(spaceName, "", "Zone or ZoneList Name", "");
			recordInputs("TemperatureDifference", "", "Zone Cooling Design Supply Air Temperature Input Method", "");
			recordInputs("12.8", "C", "Zone Cooling Design Supply Air Temperature", "");
			recordInputs("11", "deltaC", "Zone Cooling Design Supply Air Temperature Difference", "");
			recordInputs("TemperatureDifference", "", "Zone Heating Design Supply Air Temperature Input Method", "");
			recordInputs("50", "C", "Zone Heating Design Supply Air Temperature", "");
			recordInputs("11", "deltaC", "Zone Heating Design Supply Air Temperature Difference", "");
			recordInputs("0.0103", "", "Zone Cooling Design Supply Air Humidity Ratio", "");
			recordInputs("0.0066", "", "Zone Heating Design Supply Air Humidity Ratio", "");
			recordInputs(oaObjectName, "", "Design Specification Outdoor Air Object Name", "");
			// sizing factors
			recordInputs("1.25", "", "Zone Heating Sizing Factor", "");
			recordInputs("1.15", "", "Zone Cooling Sizing Factor", "");
			// Design air methods
			recordInputs("DesignDay", "", "Cooling Design Air Flow Method", "");
			recordInputs("0", "m3/s", "Cooling Design Air Flow Rate", "");
			recordInputs("0.000762", "m3/s-m2", "Cooling Minimum Air Flow per Zone Floor Area", "");
			recordInputs("0", "m3/s", "Cooling Minimum Air Flow", "");
			recordInputs("0", "", "Cooling Minimum Air Fraction", "");

			recordInputs("DesignDay", "", "Heating Design Air Flow Method", "");
			recordInputs("0", "m3/s", "Heating Design Air Flow Rate", "");
			recordInputs("0.002032", "m3/s-m2", "Heating Maximum Air Flow per Zone Floor Area", "");
			recordInputs("01415762", "m3/s", "Heating Maximum Air Flow", "");
			recordInputs("0.3", "", "Heating Maximum Air Fraction", "");
			// air distribution
			recordInputs(airDistObjectName, "", "Design Specification Zone Air Distribution Object Name", "");
			addObject(file);

			// now thermostat
			// String heatScheduleId = thermalZone.getHeatScheduleId();
			// String coolScheduleId = thermalZone.getCoolScheduleId();

			String thermostatType = "ThermostatSetpoint:DualSetpoint";
			Integer controlType = 4;
			String thermostatName = "Dual Setpoint Dual SP";

			String controlTypeSchedule = scheduleTranslator.getScheduleNameFromID("controlType " + controlType);
			if (controlTypeSchedule == null) {
				// build control type schedule
				scheduleTranslator.addSimpleCompactSchedule("Control Type - Always " + controlType,
						"controlType " + controlType, (double) controlType, file);
				controlTypeSchedule = scheduleTranslator.getScheduleNameFromID("controlType " + controlType);
			}

			scheduleTranslator.addHeatingSchedule(ScheduleTranslator.BUILDING_HTGSP_SCHEDULE, ScheduleTranslator.BUILDING_HTGSP_SCHEDULE, file);
			String heatName = ScheduleTranslator.BUILDING_HTGSP_SCHEDULE;
			scheduleTranslator.addCoolingSchedule(ScheduleTranslator.BUILDING_CLGSP_SCHEDULE, ScheduleTranslator.BUILDING_CLGSP_SCHEDULE, file);
			String coolName = ScheduleTranslator.BUILDING_CLGSP_SCHEDULE;

			recordInputs(thermostatType, "", "", "");
			recordInputs(thermostatName, "", "Name", "");
			recordInputs(heatName, "", "Heating Setpoint Temperature Schedule Name", "");
			recordInputs(coolName, "", "Cooling Setpoint Temperature Schedule Name", "");
			addObject(file);

			// jump into zone control
			recordInputs("ZoneControl:Thermostat", "", "", "");
			recordInputs(spaceName + " Thermostat", "", "Name", "");
			recordInputs(spaceName, "", "Zone or ZoneList Name", "");
			recordInputs(controlTypeSchedule, "", "Control Type Schedule Name", "");
			recordInputs(thermostatType, "", "Control 1 Object Type", "");
			recordInputs(thermostatName, "", "Control 1 Name", "");
			addObject(file);
		}

	}

	/**
	 * 
	 * @param element
	 *            building element
	 */
	private void translateBuilding(Element element, IDFFileObject file) {
		String id = element.getAttributeValue("id");
		String type = element.getAttributeValue("buildingType");
		
		if(type == null) {
			bldgType = "nonresidential";
		}else {
			bldgType = getBuildingType(type);
		}
		
		String name = element.getChildText("Name", ns);
		totalFloorArea = Double.parseDouble(element.getChildText("Area", ns));
		name = escapeName(id, name);
		bs_idToObjectMap.put(id, name);

		// Create building objects
		recordInputs("Building", "", "", "");
		recordInputs(name, "", "Name", "");
		recordInputs("0", "", "North Axis", ""); // TODO default to 0, need
													// further investigate
		recordInputs("City", "", "Terrain", ""); // TODO Warning - the location
													// function is not
													// completed, currently all
													// the terrain is default to
													// city
		recordInputs("0.04", "", "Temperature Convergence Tolerance Value", "");
		recordInputs("0.4", "deltaC", "", "");
		recordInputs("FullExterior", "", "Solar Distribution", "");
		recordInputs("25", "", "Maximum Number of Warmup Days", "");
		recordInputs("6", "", "Minimum Number of Warmup Days", "");
		addObject(file);

		// set the global geometry rule
		recordInputs("GlobalGeometryRules", "", "", "");
		recordInputs("LowerLeftCorner", "", "Starting Vertex Position", "");
		recordInputs("CounterClockWise", "", "Vertex Entry Direction", "");
		recordInputs("Relative", "", "Coordinate System", "");
		recordInputs("", "", "Daylighting Reference Point Coordinate System", "");// TODO
																					// -
																					// lighting
																					// control
		recordInputs("", "", "Rectangular Surface Coordinate System", "");
		addObject(file);

		// do story - for baseline purpose
		numberOfFloors = element.getChildren("BuildingStorey", ns).size();

		// translate storey

		// space
		List<Element> spaceElements = element.getChildren("Space", ns);
		// TODO progress bar info
		for (int i = 0; i < spaceElements.size(); i++) {
			// translateSpace
			Element space = spaceElements.get(i);
			GbXMLSpace aSpace = new GbXMLSpace(lengthMultiplier, ns);
			aSpace.setAreaUnit(areaUnit);
			aSpace.setVolumeUnit(volumnUnit);
			aSpace.translateSpace(space);

			if (aSpace.getThermalZoneId() != null) {
				aSpace.setGbXMLThermalZone(bs_idToThermalZoneMap.get(aSpace.getThermalZoneId()));
			}
			bs_idToSpaceMap.put(aSpace.getSpaceId(), aSpace);
		}
	}

	private String assignDefaultSurfaceType(Double tilt) {
		if (tilt < 60) {
			return "RoofCeiling";
		} else if (tilt < 179) {
			return "Wall";
		} else {
			return "Floor";
		}
	}

	private IDFObject reverseConstruction(IDFObject construction) {

		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		ArrayList<String> comments = new ArrayList<String>();
		ArrayList<String> topComments = new ArrayList<String>();

		String name = construction.getName();
		String[] value = construction.getData();
		String[] unit = construction.getUnit();
		String[] comment = construction.getStandardComments();

		lines.add("Construction");
		units.add("");
		comments.add("");

		lines.add(name + "_reversed");
		units.add("");
		comments.add("Name");

		for (int i = 0; i < value.length - 1; i++) {
			// skips 1, the name of the construction
			lines.add(value[value.length - 1 - i]);
			units.add(unit[i]);
			comments.add(comment[i]);
		}

		return new IDFObject(lines, units, comments, topComments);
	}

	private String escapeName(String id, String name) {
		String value = id;
		if (name != null && !name.isEmpty()) {
			value = name;
		}
		return value.replace(",", "-").replace(";", "-");
	}

	private void recordInputs(String line, String unit, String comment, String topComments) {
		lines.add(line);
		units.add(unit);
		comments.add(comment);
	}

	private void addObject(IDFFileObject file) {
		file.addObject(new IDFObject(lines, units, comments, topComments));
		lines.clear();
		units.clear();
		comments.clear();
		topComments.clear();
	}
	
    private String getBuildingType(String bldgType) {
        if (bldgType.equals("singlefamily") || bldgType.equals("multifamily") || bldgType.equals("hotel")
                || bldgType.equals("dormitory") || bldgType.equals("motel")) {
            return "residential";
        } else {
            return "nonresidential";
        }
    }
}
