package main.java.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import main.java.config.ServerConfig;
import main.java.model.idf.IDFParser;
import main.java.model.vc.Branch;
import main.java.model.weather.EPWReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFObjectKeyMaker;
import main.java.model.idf.meta.ObjectSeq;
import main.java.model.idf.meta.ObjectSeqFactory;
import main.java.model.meta.ModelFileObject;
import main.java.model.vc.BranchType;
import main.java.model.vc.Project;
import main.java.tools.idd.ObjectFields;

public class ModelUtil {
	private static final Logger LOG = LoggerFactory.getLogger(ModelUtil.class);
	private static final List<String> REQUIRED_METERS = Arrays.asList("electricity:facility", "gas:facility",
			"districtheating:facility", "districtcooling:facility");

	public static int getValueCommentsPad(ModelFileObject obj1, ModelFileObject obj2) {
		int padObj1 = obj1.getValueCommentPad();
		int padObj2 = obj2.getValueCommentPad();
		return Math.max(padObj1, padObj2);
	}

	public static <T> LinkedHashMap<String, Integer> buildSortedLabelMap(String version, Map<String, T> map,
			BranchType type) {
		if (version == null) {
			return null;
		}

		ObjectSeq labelSeq = ObjectSeqFactory.getIDFObjectSeq(version, type);
		if (labelSeq == null) {
			LOG.error("IDF version not found in IDFObjectSeqFactory");
			return null;
		}

		HashMap<String, Integer> labelSeqMap = new HashMap<>();
		Set<String> labels = map.keySet();
		for (String label : labels) {
			int seq = labelSeq.getObjectSeq(label.trim().toLowerCase());
			if (seq < 0) {
				LOG.error(label + " not found in labelSeq");
			} else {
				labelSeqMap.put(label, seq);
			}
		}

		LinkedHashMap<String, Integer> sortedLabelMap = new LinkedHashMap<>();
		Stream<Map.Entry<String, Integer>> st = labelSeqMap.entrySet().stream();
		st.sorted((arg0, arg1) -> {
			Integer i1 = arg0.getValue();
			Integer i2 = arg1.getValue();
			return i1.compareTo(i2);
		}).forEachOrdered(e -> sortedLabelMap.put(e.getKey(), e.getValue()));

		return sortedLabelMap;
	}

	public static <T> void sortIDFLabelMap(String version, LinkedHashMap<String, T> map, BranchType type) {
		LinkedHashMap<String, Integer> sortedLabel = buildSortedLabelMap(version, map, type);

		LinkedHashMap<String, T> tmp = new LinkedHashMap<>(map);
		map.clear();

		Stream<Map.Entry<String, Integer>> st = sortedLabel.entrySet().stream();
		st.forEach(e -> map.put(e.getKey(), tmp.get(e.getKey())));

		tmp.clear();
	}

	public static JsonObject populateToJson(IDFObject idfObj, String idfVersion) {
		String label = idfObj.getObjLabel();
		JsonObject fieldsJo = new JsonObject();

		List<String[]> fields = ObjectFields.getFieldsNameAndUnit(idfVersion, label);
		if (fields == null) {
			return fieldsJo;
		}

		int size = fields.size();

		String[] vals = idfObj.getData();
		int len = vals.length;

		fieldsJo.addProperty("EPlusType", label);

		for (int i = 0, j = 0; i < len && j < size; i++, j++) {
			String[] field = fields.get(j);
			String unit = field[1];
			if (unit == null) {
				unit = "";
			} else {
				unit = " (" + unit + ")";
			}

			fieldsJo.addProperty(field[0], vals[i] + unit);
		}

		return fieldsJo;
	}

	public static JsonObject populateToJsonSeparateProperties(IDFObject idfObj, String idfVersion) {
		String label = idfObj.getObjLabel();
		JsonObject fieldsJo = new JsonObject();

		List<String[]> fields = ObjectFields.getFieldsNameAndUnit(idfVersion, label);
		if (fields == null) {
			return fieldsJo;
		}

		int size = fields.size();

		String[] vals = idfObj.getData();
		int len = vals.length;

		fieldsJo.addProperty("name", label);

		JsonArray properties = new JsonArray();
		for (int i = 0, j = 0; i < len && j < size; i++, j++) {
			String[] field = fields.get(j);
			String unit = field[1];
			if (unit == null) {
				unit = "";
			}

			JsonObject prop = new JsonObject();
			prop.addProperty("name", field[0]);
			prop.addProperty("value", vals[i]);
			prop.addProperty("unit", unit);

			properties.add(prop);
		}

		fieldsJo.add("properties", properties);

		return fieldsJo;
	}

	public static JsonObject updateIDFFileObjectFromEditor(String content, String label, IDFFileObject idfFileObj,
			IDFObjectKeyMaker keyMaker, IDDParser iddParser) {
		JsonObject status = new JsonObject();
		List<IDFObject> objs = new ArrayList<>();

		String[] rows = content.replaceAll("\\r", "").split("\\n");
		int ExclamationIndex = 0, pad = 0;
		ArrayList<String> lines = new ArrayList<>(), units = new ArrayList<>(), comments = new ArrayList<>(),
				topComments = new ArrayList<>();
		String comment = null, unit = "";
		for (String line : rows) {
			if (line.isEmpty()) {
				continue;
			}

			line = line.trim();

			ExclamationIndex = line.indexOf("!");
			if (ExclamationIndex == 0) {
				topComments.add(line);
				continue;
			}

			// if there is no exclamation mark, it is the head of IDF line
			if (ExclamationIndex > 0) {
				comment = line.substring(ExclamationIndex);
				comment = comment.substring(comment.indexOf(" ") + 1).trim(); // comment starts with '!- ' or '! '

				// line only has values
				line = line.substring(0, ExclamationIndex).trim();
			}

			if (!line.isEmpty()) {
				if (line.equalsIgnoreCase("lead input;") || line.equalsIgnoreCase("end lead input;")
						|| line.equalsIgnoreCase("simulation data;") || line.equalsIgnoreCase("end simulation data;")) {
					continue;
				}

				// process every statement line (delimited by ;)
				String[] values = line.substring(0, line.length() - 1).split(",");
				int len = values.length;

				for (int i = 0; i < len - 1; i++) {
					lines.add(values[i]);
					units.add(unit);
					comments.add("");
				}

				lines.add(values[len - 1]);
				units.add(unit);
				comments.add(comment);

				if (line.endsWith(";")) {
					IDFObject idfObj = new IDFObject(lines, units, comments, topComments);
					idfObj.updateKey(keyMaker.makeKey(idfObj));

					objs.add(idfObj);

					if (pad < idfObj.getMaxLineLen()) {
						pad = idfObj.getMaxLineLen();
					}

					lines.clear();
					units.clear();
					comments.clear();
					topComments.clear();
				}

				comment = "";
			}
		}

		idfFileObj.setValueCommentPad(pad);

		/*
		 * if(iddParser!=null){ JsonArray errorMessage = new JsonArray();
		 * if(!iddParser.validateObject(objs, label, errorMessage)){ StringBuilder sb =
		 * new StringBuilder();
		 * 
		 * for(int i=0;i<errorMessage.size();i++){ JsonObject msg =
		 * errorMessage.get(i).getAsJsonObject(); String type =
		 * msg.get("type").getAsString(); if(type.equals("Severe") ||
		 * type.equals("Error")){
		 * sb.append(msg.get("name").getAsString()+": "+msg.get("message").getAsString()
		 * +"<br/>"); sb.append("Level: "+type+"<br/><br/>"); } }
		 * 
		 * if(sb.length()>0){ status.addProperty("status", "error");
		 * status.addProperty("error_msg", sb.toString()); return status; } } }
		 */

		/**
		 * update IDFFileObject
		 */
		// update label-list map
		Map<String, List<IDFObject>> objsMap = idfFileObj.getObjectsMap();
		List<IDFObject> oldObjs = objsMap.get(label.toLowerCase());

		if (!objs.isEmpty()) {
			objsMap.put(label.toLowerCase(), objs);
		} else {
			objsMap.remove(label.toLowerCase());
		}

		// remove old obj from key map
		Map<String, IDFObject> keyMap = idfFileObj.getKeyMap();
		if (oldObjs != null) {
			for (IDFObject oldObj : oldObjs) {
				keyMap.remove(oldObj.getKey());
			}
		}

		// add new obj to key map and label-key-tree-map
		Map<String, TreeMap<String, IDFObject>> labelKeyMap = idfFileObj.getLabelKeyMap();
		TreeMap<String, IDFObject> treeObjs = new TreeMap<>();
		for (IDFObject obj : objs) {
			keyMap.put(obj.getKey(), obj);
			treeObjs.put(obj.getKey(), obj);
		}
		labelKeyMap.put(label.toLowerCase(), treeObjs);

		idfFileObj.invalidSortedLabelMap();

		/**
		 * update finished
		 */
		status.addProperty("status", "success");
		return status;
	}

//	public static JsonObject prepareForSimulation(IDFFileObject idfFileObj, Project proj, SimulationMode mode,
//			String unit, boolean isCustomize) {
//		/**
//		 * 0.5. Set up the parameters
//		 */
//		Set<String> environmentData = new HashSet<>();
//		environmentData.add("Site Outdoor Air Drybulb Temperature");
//		environmentData.add("Site Outdoor Air Relative Humidity");
//		environmentData.add("Site Wind Speed");
//		environmentData.add("Site Wind Direction");
//
//		/**
//		 * 1. set SimulationControl, enable all flags
//		 */
//		List<IDFObject> simControlList = idfFileObj.getCategoryList("SimulationControl");
//        removeExtra(simControlList, idfFileObj, 0);
//
//		IDFObject simControl;
//        
//        if(NumUtil.readVersion(idfFileObj.getVersion(), 82) >= 83) {
//            simControl = new IDFObject("SimulationControl", 8);
//            simControl.setOriginalCommentNoUnit(5, "Do HVAC Sizing Simulation for Sizing Periods");
//            
//            simControl.setOriginalCommentNoUnit(6, "Maximum Number of HVAC Sizing Simulation Passes");
//            simControl.setIndexedData(6, "1");
//        }else {
//            simControl = new IDFObject("SimulationControl", 6);
//        }
//        simControl.setOriginalCommentNoUnit(0, "Do Zone Sizing Calculation");
//        simControl.setOriginalCommentNoUnit(1, "Do System Sizing Calculation");
//        simControl.setOriginalCommentNoUnit(2, "Do Plant Sizing Calculation");
//        simControl.setOriginalCommentNoUnit(3, "Run Simulation for Sizing Periods");
//        simControl.setOriginalCommentNoUnit(4, "Run Simulation for Weather File Run Periods");
//        
//        idfFileObj.addObject(simControl);
//
//		simControl.setIndexedData(0, "Yes");
//		simControl.setIndexedData(1, "Yes");
//		simControl.setIndexedData(2, "Yes");
//		switch (mode) {
//			case LOAD:
//				simControl.setIndexedData(3, "Yes");
//				simControl.setIndexedData(4, "NO");
//                simControl.setIndexedData(5, "Yes");
//				break;
//			case FULL:
//			default:
//				simControl.setIndexedData(3, "Yes");
//				simControl.setIndexedData(4, "Yes");
//                simControl.setIndexedData(5, "Yes");
//		}
//
//		/**
//		 * 2. set Output:Table:SummaryReports, AllSummary and ZoneComponentLoadSummary
//		 */
//		List<IDFObject> summaryReportsList = idfFileObj.getCategoryList("Output:Table:SummaryReports");
//		removeExtra(summaryReportsList, idfFileObj, 0);
//		int reportsNum = 3;
//		IDFObject summaryReports = new IDFObject("Output:Table:SummaryReports", reportsNum);
//		summaryReports.setOriginalCommentNoUnit(0, "Report 1 Name");
//		if (mode == SimulationMode.FULL) {
//			summaryReports.setOriginalCommentNoUnit(1, "Report 2 Name");
//		}
//
//		switch (mode) {
//			case LOAD:
//				summaryReports.setIndexedData(0, "HVACSizingSummary");
//				summaryReports.setIndexedData(1, "ZoneComponentLoadSummary");
//				break;
//			case FULL:
//			default:
//				summaryReports.setIndexedData(0, "AllSummary");
//				summaryReports.setIndexedData(1, "ZoneComponentLoadSummary");
//		}
//		idfFileObj.addObject(summaryReports);
//
//		/**
//		 * Set Maximum Number of Warmup Days no more than 25
//		 */
//		List<IDFObject> buildingList = idfFileObj.getCategoryList("Building");
//		if (buildingList != null && !buildingList.isEmpty()) {
//			IDFObject idfObject = buildingList.get(0);
//
//			int maxWarmUpsNum = 50;
//			String maxWarmUps = idfObject.getDataByStandardComment("Maximum Number of Warmup Days");
//			if (maxWarmUps != null && !maxWarmUps.isEmpty()) {
//				try {
//					maxWarmUpsNum = Integer.parseInt(maxWarmUps);
//				} catch (NumberFormatException e) {
//					maxWarmUpsNum = 50;
//				}
//			}
//			if (maxWarmUpsNum > 25) {
//				idfObject.setDataByStandardComment("Maximum Number of Warmup Days", "25");
//			}
//		}
//
//		if (mode == SimulationMode.FULL) {
//			/**
//			 * 2.5 add environment data
//			 */
//			// iterate Output:Variable, remove the matched objects
//			List<IDFObject> variableList = idfFileObj.getCategoryList("Output:Variable");
//			if (variableList != null && !variableList.isEmpty()) {
//				int len = variableList.size();
//				int pointer = 0;
//				for (int i = 0; i < len; i++) {
//					IDFObject obj = idfFileObj.getCategoryList("Output:Variable").get(pointer);
//					if (environmentData.contains(obj.getDataByStandardComment("Variable Name"))) {
//						idfFileObj.removeIDFObject(obj);
//					} else {
//						pointer++;
//					}
//				}
//			}
//			// now adds back the environment data.
//			Iterator<String> enviItr = environmentData.iterator();
//			while (enviItr.hasNext()) {
//				String name = enviItr.next();
//				idfFileObj.addObject(createEnviDataOutputObj(name));
//			}
//			
//			/**
//			 * 2.5 add monthly report table
//			 */
//			List<IDFObject> monthlyOutputObjects = createMonthlyDataOutputObj();
//			List<IDFObject> monthlyOutputs = idfFileObj.getCategoryList("Output:Table:Monthly");
//			if(monthlyOutputs!=null) {
//				Set<String> outputList = new HashSet<>();
//				for(IDFObject idfObj : monthlyOutputObjects) {
//					outputList.add(idfObj.getIndexedData(0));
//				}
//				
//				List<IDFObject> removeList = new ArrayList<>();
//				for(IDFObject existObj : monthlyOutputs) {
//					if(outputList.contains(existObj.getName())) {
//						removeList.add(existObj);
//					}
//				}
//				removeExtra(removeList, idfFileObj, 0);
//			}
//			
//			for(IDFObject idfObj : monthlyOutputObjects) {
//				idfFileObj.addObject(idfObj);
//			}
//
//			/**
//			 * 2.5 Convert all Output:Meter:MeterFileOnly to Output:Meter, and enable
//			 * required meters
//			 */
//			Set<String> seenMeter = new HashSet<>();
//			List<IDFObject> toRemove = new ArrayList<>();
//
//			// iterate Output:Meter:MeterFileOnly, set required meter to hourly meter, and set label to Output:Meter
//			List<IDFObject> meterFileOnlyList = idfFileObj.getCategoryList("Output:Meter:MeterFileOnly");
//			if (meterFileOnlyList != null && !meterFileOnlyList.isEmpty()) {
//				for (IDFObject meter : meterFileOnlyList) {
//					String meterName = meter.getName();
//					if (StringUtil.isNullOrEmpty(meterName)) {
//						toRemove.add(meter);
//						continue;
//					}
//
//					meterName = meterName.toLowerCase();
//					if (REQUIRED_METERS.contains(meterName)) {
//						meter.setIndexedData(1, "Hourly");
//					}
//
//					meter.setObjLabel("Output:Meter");
//
//					seenMeter.add(meterName);
//				}
//			}
//
//			// iterate Ouput:Meter, if meter is already seen, remove it
//			List<IDFObject> outputMeterList = idfFileObj.getCategoryList("Output:Meter");
//			if (outputMeterList != null && !outputMeterList.isEmpty()) {
//				for (IDFObject idfObject : outputMeterList) {
//					String meterName = idfObject.getName();
//					if (StringUtil.isNullOrEmpty(meterName)) {
//						toRemove.add(idfObject);
//						continue;
//					}
//
//					meterName = meterName.toLowerCase();
//					if (seenMeter.contains(meterName)) {
//						continue;
//					}
//
//					if (REQUIRED_METERS.contains(meterName)) {
//						idfObject.setIndexedData(1, "Hourly");
//					}
//
//					seenMeter.add(meterName);
//				}
//			}
//
//			// if any required meter is not seen, add it
//			for (String requiredMeter : REQUIRED_METERS) {
//				if (seenMeter.add(requiredMeter)) {
//					IDFObject meter = new IDFObject("Output:Meter", 3);
//					meter.setIndexedData(0, requiredMeter);
//					meter.setIndexedData(1, "Hourly");
//
//					idfFileObj.addObject(meter);
//				}
//			}
//
//			// remove objects
//			for (IDFObject toDel : toRemove) {
//				idfFileObj.removeIDFObject(toDel);
//			}
//		}
//
//		/**
//		 * 3. set OutputControl:Table:Style, output is HTML
//		 */
//		List<IDFObject> tableStyleList = idfFileObj.getCategoryList("OutputControl:Table:Style");
//		if (tableStyleList != null && !tableStyleList.isEmpty()) {
//			// remove existing
//			removeExtra(tableStyleList, idfFileObj, 0);
//		}
//		IDFObject tableStyle;
//		/*if (unit.equalsIgnoreCase("ip")
//				|| (mode!=SimulationMode.LOAD
//						&& proj.getWeatherFileCountry() != null && proj.getWeatherFileCountry().equals("USA")
//						&& proj.getWeatherFileState() != null && proj.getWeatherFileState().equals("CA"))) {*/
//		if (unit.equalsIgnoreCase("ip")){
//			tableStyle = new IDFObject("OutputControl:Table:Style", 3);
//			tableStyle.setIndexedData(0, "CommaAndHTML");
//			tableStyle.setIndexedData(1, "inchpound");
//		}else {
//			//LOG.info("NOT USA CA state project, use default, jtokwh");
//			tableStyle = new IDFObject("OutputControl:Table:Style", 3);
//			tableStyle.setIndexedData(0, "CommaAndHTML");
//			tableStyle.setIndexedData(1, "jtokwh");
//		}
//		idfFileObj.addObject(tableStyle);
//
//		/**
//		 * 4. trim Chiller:Electric:EIR object to 30 fields
//		 */
//		List<IDFObject> chillers = idfFileObj.getCategoryList("Chiller:Electric:EIR");
//		if (chillers != null) {
//			for (IDFObject chiller : chillers) {
//				chiller.trimTo(31); // include label
//			}
//		}
//
//		/**
//		 * Check if external csv schedule files exist
//		 */
//		JsonObject jo = new JsonObject();
//		JsonArray ja = new JsonArray();
//
//		List<IDFObject> extSch = idfFileObj.getCategoryList("schedule:file");
//		if (isCustomize || extSch == null || extSch.size() == 0) {
//			jo.addProperty("status", "success");
//			jo.add("csv", ja);
//		} else {
//			Set<String> files = new HashSet<>();
//			for (IDFObject sch : extSch) {
//				String fileName = sch.getDataByStandardComment("File Name");
//
//				int lastSlash = fileName.lastIndexOf("\\");
//				if (lastSlash > -1) {
//					fileName = fileName.substring(lastSlash + 1);
//					sch.setDataByStandardComment("File Name", fileName);
//				}
//
//				files.add(fileName);
//			}
//			List<String> fileNames = new ArrayList<>(files);
//
//			CloudFileExistanceTest tester = CloudFileExistanceTestFactory.getCloudFileExistanceTest();
//			List<String> nonExist = tester == null ? fileNames
//					: tester.filterNonExist(proj, PathUtil.PROJECT_SCHEDULE + "/", fileNames);
//
//			if (nonExist.isEmpty()) {
//				jo.addProperty("status", "success");
//
//				for (String csv : fileNames) {
//					ja.add(csv);
//				}
//				jo.add("csv", ja);
//			} else {
//				jo.addProperty("status", "error");
//				jo.addProperty("error_msg", "Some external schedules are not in cloud storage");
//
//				for (String missed : nonExist) {
//					ja.add(missed);
//				}
//				jo.add("missed", ja);
//			}
//		}
//
//		return jo;
//	}

	private static void removeExtra(List<IDFObject> list, IDFFileObject idfFileObject, int start) {
		if (list != null && list.size() > start) {
			List<IDFObject> copyToDel = new ArrayList<>(list);
			for (int i = start; i < copyToDel.size(); i++) {
				IDFObject extraSummaryReports = copyToDel.get(i);
				idfFileObject.removeIDFObject(extraSummaryReports);
			}
		}
	}

//	public static JsonObject updateOSMFileObject(String content, String label, OSMFileObject osmFileObj) {
//		JsonObject status = new JsonObject();
//		List<IDFObject> objs = new ArrayList<>();
//
//		String[] rows = content.replaceAll("\\r", "").split("\\n");
//		int ExclamationIndex = 0, pad = 0;
//		ArrayList<String> lines = new ArrayList<>(), units = new ArrayList<>(), comments = new ArrayList<>(),
//				topComments = new ArrayList<>();
//		String comment = null, unit = "";
//
//		for (String line : rows) {
//			if (line.isEmpty()) {
//				continue;
//			}
//
//			line = line.trim();
//
//			ExclamationIndex = line.indexOf("!");
//			if (ExclamationIndex == 0) {
//				topComments.add(line);
//				continue;
//			}
//
//			// if there is no exclamation mark, it is the head of IDF line
//			if (ExclamationIndex > 0) {
//				comment = line.substring(ExclamationIndex);
//				comment = comment.substring(comment.indexOf(" ") + 1).trim(); // comment starts with '!- ' or '! '
//
//				// line only has values
//				line = line.substring(0, ExclamationIndex).trim();
//			}
//
//			if (!line.isEmpty()) {
//				if (line.equalsIgnoreCase("lead input;") || line.equalsIgnoreCase("end lead input;")
//						|| line.equalsIgnoreCase("simulation data;") || line.equalsIgnoreCase("end simulation data;")) {
//					continue;
//				}
//
//				// process every statement line (delimited by ;)
//				String[] values = line.substring(0, line.length() - 1).split(",");
//				int len = values.length;
//
//				for (int i = 0; i < len - 1; i++) {
//					lines.add(values[i]);
//					units.add(unit);
//					comments.add("");
//				}
//
//				lines.add(values[len - 1]);
//				units.add(unit);
//				comments.add(comment);
//
//				if (line.endsWith(";")) {
//					IDFObject idfObj = new IDFObject(lines, units, comments, topComments);
//
//					objs.add(idfObj);
//
//					if (pad < idfObj.getMaxLineLen()) {
//						pad = idfObj.getMaxLineLen();
//					}
//
//					lines.clear();
//					units.clear();
//					comments.clear();
//					topComments.clear();
//				}
//
//				comment = "";
//			}
//		}
//
//		// osmFileObj.setValueCommentPad(pad);
//
//		// JsonArray errorMessage = new JsonArray();
//
//		/**
//		 * update OSMFileObject
//		 */
//		// update label-list map
//		Map<String, List<IDFObject>> objsMap = osmFileObj.getObjectsMap();
//		List<IDFObject> oldObjs = objsMap.get(label.toLowerCase());
//
//		if (!objs.isEmpty()) {
//			objsMap.put(label.toLowerCase(), objs);
//		} else {
//			objsMap.remove(label.toLowerCase());
//		}
//
//		// remove old obj from key map
//		Map<String, IDFObject> keyMap = osmFileObj.getKeyMap();
//		for (IDFObject oldObj : oldObjs) {
//			keyMap.remove(oldObj.getKey());
//		}
//
//		// add new obj to key map and label-key-tree-map
//		Map<String, TreeMap<String, IDFObject>> labelKeyMap = osmFileObj.getLabelKeyMap();
//		TreeMap<String, IDFObject> treeObjs = new TreeMap<>();
//		for (IDFObject obj : objs) {
//			keyMap.put(obj.getKey(), obj);
//			treeObjs.put(obj.getKey(), obj);
//		}
//		labelKeyMap.put(label.toLowerCase(), treeObjs);
//
//		osmFileObj.invalidSortedLabelMap();
//
//		/**
//		 * update finished
//		 */
//		status.addProperty("status", "success");
//		return status;
//	}

	public static String getLaterVersion(String v1, String v2) {
		String[] v1Split = v1.split("\\.");
		String[] v2Split = v2.split("\\.");

		int v1_1 = Integer.parseInt(v1Split[0]);
		int v2_1 = Integer.parseInt(v2Split[0]);
		if (v1_1 != v2_1) {
			return v1_1 > v2_1 ? v1 : v2;
		}

		int v1_2 = Integer.parseInt(v1Split[1]);
		int v2_2 = Integer.parseInt(v2Split[1]);
		return v1_2 > v2_2 ? v1 : v2;
	}

	public static void copyIDFObject(IDFObject src, IDFObject dest, int start, int end) {
		dest.setObjLabel(src.getObjLabel());
		dest.setName(src.getName());
		dest.updateKey(src.getKey());
		dest.setObjLabel(src.getObjLabel());
		dest.setLabelMsg(src.getLabelMsg());
		dest.setObjIdentifier(src.getObjIdentifier());

		for (int i = start; i < end; i++) {
			dest.setIndexedData(i, src.getIndexedData(i), src.getIndexedUnit(i));
			dest.setOriginalCommentNoUnit(i, src.getOriginalCommentNoUnit(i));
			dest.setIndexedStandardComment(i, src.getIndexedStandardComment(i));
		}
	}

	public static IDFFileObject buildIDFFileObjectFromString(String content) {
		IDFFileObject idfFileObj = new IDFFileObject();

		return buildIDFFileObjectFromString(content, idfFileObj);
	}

	public static IDFFileObject buildIDFFileObjectFromString(String content, IDFFileObject idfFileObject) {
		IDFParser parser = new IDFParser();

		File tmp = FileUtil.convertStringToFile(content);
		parser.parseIDFFromIDFFile(tmp, idfFileObject);
		tmp.delete();

		return idfFileObject;
	}

	public static boolean isVersionEqOrLater(String version, String target) {
		String[] splitVersion = version.split("\\.");
		String[] splitTarget = target.split("\\.");

		try {
			int v1 = Integer.parseInt(splitVersion[0]);
			int t1 = Integer.parseInt(splitTarget[0]);

			if (v1 < t1) {
				return false;
			} else if (v1 > t1) {
				return true;
			} else {
				int v2 = Integer.parseInt(splitVersion[1]);
				int t2 = Integer.parseInt(splitTarget[1]);
				return v2 >= t2;
			}
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
		}
		return false;
	}
	
	private static List<IDFObject> createMonthlyDataOutputObj(){
		List<IDFObject> monthlyObjList = new ArrayList<>();
		IDFObject comfort = new IDFObject("Output:Table:Monthly", 15);
		comfort.setIndexedData(0, "Occupant Comfort Data Summary");
		comfort.setIndexedData(1, "3");
		comfort.setIndexedData(2, "Zone People Occupant Count");
		comfort.setIndexedData(3, "HoursNonZero");
		comfort.setIndexedData(4, "Zone Air Temperature");
		comfort.setIndexedData(5, "SumOrAverageDuringHoursShown");
		comfort.setIndexedData(6, "Zone Air Relative Humidity");
		comfort.setIndexedData(7, "SumOrAverageDuringHoursShown");
		comfort.setIndexedData(8, "Zone Thermal Comfort Mean Radiant Temperature");
		comfort.setIndexedData(9, "SumOrAverageDuringHoursShown");
		comfort.setIndexedData(10, "Zone Thermal Comfort ASHRAE 55 Simple Model Summer or Winter Clothes Not Comfortable Time");
		comfort.setIndexedData(11, "SumOrAverageDuringHoursShown");
		comfort.setIndexedData(12, "Zone Thermal Comfort Fanger Model PMV");
		comfort.setIndexedData(13, "SumOrAverageDuringHoursShown");
		monthlyObjList.add(comfort);
		
		IDFObject oa = new IDFObject("Output:Table:Monthly", 11);
		oa.setIndexedData(0, "Outdoor Air Summary");
		oa.setIndexedData(1, "3");
		oa.setIndexedData(2, "Zone People Occupant Count");
		oa.setIndexedData(3, "HoursNonZero");
		oa.setIndexedData(4, "Zone People Occupant Count");
		oa.setIndexedData(5, "SumOrAverageDuringHoursShown");
		oa.setIndexedData(6, "Zone Mechanical Ventilation Air Changes per Hour");
		oa.setIndexedData(7, "SumOrAverageDuringHoursShown");
		oa.setIndexedData(8, "Zone Infiltration Air Change Rate");
		oa.setIndexedData(9, "SumOrAverageDuringHoursShown");
		monthlyObjList.add(oa);
		
		IDFObject hvacAirSysLoads = new IDFObject("Output:Table:Monthly", 7);
		hvacAirSysLoads.setIndexedData(0, "Overall HVAC Air System Loads");
		hvacAirSysLoads.setIndexedData(1, "3");
		hvacAirSysLoads.setIndexedData(2, "Air System Total Heating Energy");
		hvacAirSysLoads.setIndexedData(3, "SumOrAverage");
		hvacAirSysLoads.setIndexedData(4, "Air System Total Cooling Energy");
		hvacAirSysLoads.setIndexedData(5, "SumOrAverage");
		monthlyObjList.add(hvacAirSysLoads);
		
		IDFObject hvacAirSysEnergy = new IDFObject("Output:Table:Monthly", 7);
		hvacAirSysEnergy.setIndexedData(0, "Overall HVAC System Energy");
		hvacAirSysEnergy.setIndexedData(1, "3");
		hvacAirSysEnergy.setIndexedData(2, "Electricity:HVAC");
		hvacAirSysEnergy.setIndexedData(3, "SumOrAverage");
		hvacAirSysEnergy.setIndexedData(4, "Heating:Gas");
		hvacAirSysEnergy.setIndexedData(5, "SumOrAverage");
		monthlyObjList.add(hvacAirSysEnergy);
		
		IDFObject setpointNotMet = new IDFObject("Output:Table:Monthly", 11);
		setpointNotMet.setIndexedData(0, "Setpoints Not Met With Temperatures");
		setpointNotMet.setIndexedData(1, "2");
		setpointNotMet.setIndexedData(2, "Zone Heating Setpoint Not Met Time");
		setpointNotMet.setIndexedData(3, "HoursNonZero");
		setpointNotMet.setIndexedData(4, "Zone Heating Setpoint Not Met While Occupied Time");
		setpointNotMet.setIndexedData(5, "HoursNonZero");
		setpointNotMet.setIndexedData(6, "Zone Cooling Setpoint Not Met Time");
		setpointNotMet.setIndexedData(7, "HoursNonZero");
		setpointNotMet.setIndexedData(8, "Zone Cooling Setpoint Not Met While Occupied Time");
		setpointNotMet.setIndexedData(9, "HoursNonZero");
		monthlyObjList.add(setpointNotMet);
		
		IDFObject electricityPerformance = new IDFObject("Output:Table:Monthly", 29);
		electricityPerformance.setIndexedData(0, "Building Electricity");
		electricityPerformance.setIndexedData(1, "2");
		electricityPerformance.setIndexedData(2, "InteriorLights:Electricity");
		electricityPerformance.setIndexedData(3, "SumOrAverage");
		electricityPerformance.setIndexedData(4, "ExteriorLights:Electricity");
		electricityPerformance.setIndexedData(5, "SumOrAverage");
		electricityPerformance.setIndexedData(6, "InteriorEquipment:Electricity");
		electricityPerformance.setIndexedData(7, "SumOrAverage");
		electricityPerformance.setIndexedData(8, "ExteriorEquipment:Electricity");
		electricityPerformance.setIndexedData(9, "SumOrAverage");
		electricityPerformance.setIndexedData(10, "Fans:Electricity");
		electricityPerformance.setIndexedData(11, "SumOrAverage");
		electricityPerformance.setIndexedData(12, "Pumps:Electricity");
		electricityPerformance.setIndexedData(13, "SumOrAverage");
		electricityPerformance.setIndexedData(14, "Heating:Electricity");
		electricityPerformance.setIndexedData(15, "SumOrAverage");
		electricityPerformance.setIndexedData(16, "Cooling:Electricity");
		electricityPerformance.setIndexedData(17, "SumOrAverage");
		electricityPerformance.setIndexedData(18, "HeatRejection:Electricity");
		electricityPerformance.setIndexedData(19, "SumOrAverage");
		electricityPerformance.setIndexedData(20, "Humidifier:Electricity");
		electricityPerformance.setIndexedData(21, "SumOrAverage");
		electricityPerformance.setIndexedData(22, "HeatRecovery:Electricity");
		electricityPerformance.setIndexedData(23, "SumOrAverage");
		electricityPerformance.setIndexedData(24, "WaterSystems:Electricity");
		electricityPerformance.setIndexedData(25, "SumOrAverage");
		electricityPerformance.setIndexedData(26, "Cogeneration:Electricity");
		electricityPerformance.setIndexedData(27, "SumOrAverage");
		monthlyObjList.add(electricityPerformance);
		
		IDFObject gasPerformance = new IDFObject("Output:Table:Monthly", 15);
		gasPerformance.setIndexedData(0, "Building Natural Gas");
		gasPerformance.setIndexedData(1, "2");
		gasPerformance.setIndexedData(2, "InteriorEquipment:Gas");
		gasPerformance.setIndexedData(3, "SumOrAverage");
		gasPerformance.setIndexedData(4, "ExteriorEquipment:Gas");
		gasPerformance.setIndexedData(5, "SumOrAverage");
		gasPerformance.setIndexedData(6, "Heating:Gas");
		gasPerformance.setIndexedData(7, "SumOrAverage");
		gasPerformance.setIndexedData(8, "Cooling:Gas");
		gasPerformance.setIndexedData(9, "SumOrAverage");
		gasPerformance.setIndexedData(10, "WaterSystems:Gas");
		gasPerformance.setIndexedData(11, "SumOrAverage");
		gasPerformance.setIndexedData(12, "Cogeneration:Gas");
		gasPerformance.setIndexedData(13, "SumOrAverage");
		monthlyObjList.add(gasPerformance);
		
		return monthlyObjList;
	}

	private static IDFObject createEnviDataOutputObj(String dataName) {
		ArrayList<String> lines = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		ArrayList<String> comments = new ArrayList<String>();
		ArrayList<String> topComments = new ArrayList<String>();

		lines.add("Output:Variable");
		units.add("");
		comments.add("");
		topComments.add("!- Generated by BuildSimHub");

		lines.add("*");
		units.add("");
		comments.add("Key Value");

		lines.add(dataName);
		units.add("");
		comments.add("Variable Name");

		lines.add("hourly");
		units.add("");
		comments.add("Reporting Frequency");

		return new IDFObject(lines, units, comments, topComments);
	}

    public static JsonObject getLocationFromWeatherFile(File weatherFile){
        String weatherFileName = weatherFile.getName();
        JsonObject locationJO = new JsonObject();

        String[] split = weatherFileName.split("_");
        String country = split[0];
        String state = split.length>1 ? split[1] : "";

        EPWReader reader = new EPWReader(weatherFile);
        locationJO.addProperty("full", reader.getCity()+", "+state+", "+country);

        locationJO.addProperty("lat", reader.getLatitude());
        locationJO.addProperty("lng", reader.getLongitude());
        locationJO.addProperty("postcode", reader.getZipCode());

        locationJO.addProperty("country", country);
        locationJO.addProperty("state", state);

        return locationJO;
    }
}
