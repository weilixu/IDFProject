package main.java.model.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

/**
 * This is a lit version of geometry parser for various purpose 1. Baseline
 * floor identifier
 * 
 * @author weilixu
 *
 */
public class GeometryParserLit {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	private Map<String, IDFObject> zoneMap = null;
	private Map<String, IDFObject> plenumMap = null;
	private Map<String, Integer> multiplierZones = null;

	private Map<Integer, List<String>> heightZones = null;
	//WX, for plenum
	private int numOfFloor = 0;

	private List<Surface> surroundShading = null;

	private boolean parseSuccess = true;
	private String parseErrMsg = "";
	private double sogLength = 0.0;
	private int numOfZones = 0;

	public GeometryParserLit(IDFFileObject idfFileObj) {
		plenumMap = new HashMap<>();
		multiplierZones = new HashMap<>();

		// extract zones
		zoneMap = new HashMap<>();
		List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
		if (zones == null || zones.isEmpty()) {
			parseSuccess = false;
			parseErrMsg = "Missing Zone Objects";
			return;
		}
		for (IDFObject zone : zones) {
			// String zoneName = zone.getName();
			String convectionAlgorithm = "";

			// TODO WX need to redefine the plenums
			for (int i = 1; i < zone.getObjLen() - 1; i++) {
				if (zone.getStandardComments()[i].equalsIgnoreCase("Zone Inside Convection Algorithm")) {
					convectionAlgorithm = zone.getIndexedData(i);
				} else if (zone.getStandardComments()[i].equalsIgnoreCase("Multiplier")) {
					String multiplierStr = zone.getIndexedData(i);
					int multiplier = multiplierStr == null || multiplierStr.isEmpty() ? 1
							: Double.valueOf(multiplierStr.trim()).intValue();
					if (multiplier != 1) {
						multiplierZones.put(zone.getName(), multiplier);
					}
				}
			}

			if (!convectionAlgorithm.toLowerCase().contains("ceilingdiffuser")) {
				numOfZones++;
				zoneMap.put(zone.getName(), zone);
			} else {
				plenumMap.put(zone.getName(), zone);
			}
		}

		// extract building surfaces
		Map<String, Double> zoneFloorHeight = new HashMap<>();
		List<IDFObject> surfaces = idfFileObj.getCategoryList("BuildingSurface:Detailed");
		if (surfaces == null || surfaces.isEmpty()) {
			parseSuccess = false;
			parseErrMsg = "Missing BuildingSurface:Detailed Objects";
			return;
		}
		for (IDFObject surfaceObj : surfaces) {
			String surfaceName = surfaceObj.getName();
			if (!surfaceName.toLowerCase().contains("plenum")) {
				IDFObject zoneObj = getZoneObj(surfaceObj);
				if (zoneObj == null) {
					// this surface is not belong to existing zone, ignore
					continue;
				}

				String zoneName = zoneObj.getName();

				Surface sur = new Surface(surfaceObj, zoneObj);
				if(sur.isWallOnGround()) {
					sogLength += sur.getSlabOnGradePriemeter();
				}

				if (!zoneFloorHeight.containsKey(zoneName) || zoneFloorHeight.get(zoneName) > sur.getLowestPoint()) {
					zoneFloorHeight.put(zoneName, sur.getLowestPoint());
				}
			}
		}
		
		// height to zone list
		heightZones = new TreeMap<>();
		for (String zoneName : zoneFloorHeight.keySet()) {
			int height = zoneFloorHeight.get(zoneName).intValue();
			if (!heightZones.containsKey(height)) {
				heightZones.put(height, new ArrayList<>());
			}
			heightZones.get(height).add(zoneName);
		}
		
		// now calculate number of floors
		Iterator<Integer> zoneHeightMap = heightZones.keySet().iterator();
		while(zoneHeightMap.hasNext()) {
			List<String> zoneList = heightZones.get(zoneHeightMap.next());
			int min = Integer.MAX_VALUE;
			for(String s : zoneList) {
				if(multiplierZones.containsKey(s)) {
					if(min > multiplierZones.get(s)) {
						min = multiplierZones.get(s);
					}
				}else {
					min = 1;
				}
			}
			
			if(min <= 1) {
				numOfFloor += 1;
			}else {
				numOfFloor += min;
			}
			
		}
		
		// extract Shading:Building:Detailed
		surroundShading = new ArrayList<>();
		List<IDFObject> shadings = idfFileObj.getCategoryList("Shading:Building:Detailed");
		if (shadings != null) {
			for (IDFObject shadingObj : shadings) {
				Surface sur = new Surface(shadingObj);
				surroundShading.add(sur);
			}
		}

		// shadings = idfFileObj.getCategoryList("Shading:Site:Detailed");
		// if (shadings != null) {
		// for (IDFObject shadingObj : shadings) {
		// Surface sur = new Surface(shadingObj);
		// surroundShading.add(sur);
		// }
		// }
	}

	public Map<Integer, List<String>> getFloorMap() {
		return heightZones;
	}
	
	public Integer getNumberOfStories() {
		return numOfFloor;
	}
	
	public Integer getNumberOfStoriesAboveGround() {
		int counter = 0;
		Iterator<Integer> storyCounter = heightZones.keySet().iterator();
		while(storyCounter.hasNext()) {
			Integer level = storyCounter.next();
			if(level >= 0) {
				List<String> zoneList = heightZones.get(level);
				int min = Integer.MAX_VALUE;
				for(String s : zoneList) {
					if(multiplierZones.containsKey(s)) {
						if(min > multiplierZones.get(s)) {
							min = multiplierZones.get(s);
						}
					}else {
						min = 1;
					}
				}
				
				if(min <= 1) {
					counter ++;
				}else {
					counter += min;
				}
			}
		}
		
		return counter;
	}
	
	public int getNumberOfZones() {
		return numOfZones;
	}
	
	public Double getSlabOnGradeLinearLength() {
		return sogLength;
	}

	public boolean isParseSuccess() {
		return this.parseSuccess;
	}

	public String getParseErrorMsg() {
		return this.parseErrMsg;
	}

	private IDFObject getZoneObj(IDFObject surfaceObj) {
		String zoneName = surfaceObj.getDataByStandardComment("Zone Name");

		IDFObject zoneObj = zoneMap.get(zoneName);
		if (zoneObj == null) {
			zoneObj = plenumMap.get(zoneName);
		}
		if (zoneObj == null) {
			LOG.warn("Surface " + surfaceObj.getName() + " zone cannot be found, (zone name: " + zoneName + ")");
		}
		return zoneObj;
	}

}
