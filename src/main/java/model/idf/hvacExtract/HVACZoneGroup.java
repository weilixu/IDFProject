package main.java.model.idf.hvacExtract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import main.java.model.idd.EnergyPlusFieldTemplate;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.util.ModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class HVACZoneGroup {
	//use it on webposting mod
    //private static AtomicLong idCounter = new AtomicLong();//id for jsonmap
	private AtomicLong idCounter = new AtomicLong();
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private ArrayList<ZoneEquipmentList> equipmentListArray;
    // 1: parent system object, 2: parent system name, 3: zone
    private Map<String, Map<String, List<ZoneEquipment>>> hvacMap;
    private IDFFileObject eplusData;
    private String version = null;
    //Idd parser is required for querying system.
    private IDDParser iddParser;
    private HashMap<String, String> equipmentIdMap;

    public HVACZoneGroup(IDFFileObject eplusData) {
        this.eplusData = eplusData;
        this.version = eplusData.getVersion();

        equipmentListArray = new ArrayList<ZoneEquipmentList>();
        hvacMap = new HashMap<>();
        equipmentIdMap = new HashMap<String, String>();//set up an id map
        hvacThermalZoneGroup();
        rearrangeHVAC();
    }

    /**
     * sample code of how to use this module
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
    }

    /**
     * HVAC query first step, always set up the idd parser for the search
     *
     * @param p: IddParser
     */
    public void setIddParser(IDDParser p) {
        this.iddParser = p;
    }

    public JsonObject getObjectData(String systemLabel, String systemName, String zoneName) {
        if (iddParser == null) {
            LOG.error("Insufficient data for HVAC system: Missing IDD file");
            return null;
        }
        JsonObject equipmentListObject = new JsonObject();
        JsonArray equipmentPropertyArray = new JsonArray();
        if (zoneName == null || zoneName.isEmpty()) {
            //get the system data
            IDFObject systemObj = eplusData.getIDFObjectByName(systemLabel, systemName);
            if (systemObj == null) {
                LOG.error("System cannot be found: " + systemLabel + ", " + systemName);
                equipmentListObject.addProperty("error_msg", "System cannot be found: " + systemLabel + ", " + systemName);
                return equipmentListObject;
            }

            String systemType = systemObj.getObjLabel();

            if (systemType.equals("AirLoopHVAC")) {
                equipmentListObject.addProperty("id", equipmentIdMap.get("AirLoopHVAC:" + systemName));
                equipmentListObject.addProperty("name", systemName);

                String branchListName = systemObj.getDataByStandardComment("Branch list Name");
                IDFObject obj = eplusData.getIDFObjectByName("BranchList", branchListName);
                if (obj == null) {
                    LOG.error("BranchList not found: " + branchListName);
                    equipmentListObject.addProperty("error_msg", "BranchList not found: " + branchListName);
                    return equipmentListObject;
                }
                String branchName = obj.getDataByStandardComment("Branch 1 Name");
                if (branchName == null) {
                    branchName = obj.getIndexedData(1);
                }

                IDFObject branch = eplusData.getIDFObjectByName("Branch", branchName);
                if (branch == null) {
                    LOG.error("Branch not found: " + branchName);
                    equipmentListObject.addProperty("error_msg", "Branch not found: " + branchName);
                    return equipmentListObject;
                }

                //need to analyze the branch - get object type and name
                int beginOfExtension = iddParser.getObject("Branch").getBeginningOfExtensible();
                int numExtension = iddParser.getObject("Branch").numOfExtensibles();
                //System.out.println(branch.getName() + " " + beginOfExtension + " " + numExtension +" " + branch.getObjLen());
                int counter = beginOfExtension;
                while (branch.getObjLen() - 1 > counter) {
                    JsonObject equipmentProp = new JsonObject();

                    String objectType = branch.getIndexedData(counter);
                    String objectName = branch.getIndexedData(counter + 1);
                    counter += numExtension;

                    IDFObject equip = eplusData.getIDFObjectByName(objectType, objectName);
                    if (equip == null) {
                        LOG.error(objectType + " not found: " + objectName);
                        continue;
                    }
                    //System.out.println(objectType + " " + objectName);
                    equipmentProp.add("properties", populateToJsonSeparateProperties(equip, eplusData.getVersion(), 0, 1));//maximum trace to 3 levels
                    equipmentPropertyArray.add(equipmentProp);
                }
                //equipmentListObject.add("properties", equipmentPropertyArray);//maximum trace to 3 levels

            } else if (systemType.equals("AirConditioner:VariableRefrigerantFlow") ||
                    systemType.equals("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl") ||
                    systemType.equals("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:HR")) {
                //VRF case
                JsonObject equip = new JsonObject();
                equip.addProperty("id", equipmentIdMap.get(systemLabel + ":" + systemName));
                equip.addProperty("name", systemName);

                equip.add("properties", populateToJsonSeparateProperties(systemObj, eplusData.getVersion(), 0, 3));//maximum trace to 3 levels
                equipmentPropertyArray.add(equip);
            } else {
                //Zone level equipment case - just extract the system
                JsonObject equip = new JsonObject();
                equip.addProperty("id", equipmentIdMap.get(systemLabel + ":" + systemName));
                equip.addProperty("name", systemName);

                equip.add("properties", populateToJsonSeparateProperties(systemObj, eplusData.getVersion(), 0, 3));//maximum trace to 3 levels
                equipmentPropertyArray.add(equip);
            }

            equipmentListObject.add("system", equipmentPropertyArray);

        } else {
            //zone level data extraction
            for (ZoneEquipmentList zel : equipmentListArray) {
                //System.out.println("Processing: " + zel.getEquipmentListName());
                if (zel.getZoneName().equalsIgnoreCase(zoneName)) {
                    for (int i = 0; i < zel.getSizeOfEquipment(); i++) {
                        ZoneEquipment ze = zel.getEquipment(i);
                        if (ze.getParentSystemName().equalsIgnoreCase(systemName)) {
                            JsonObject equipmentProp = new JsonObject();

                            equipmentProp.addProperty("id", equipmentIdMap.get(ze.getEquipmentObjectType() + ":" + ze.getEquipmentName()));
                            equipmentProp.addProperty("name", ze.getEquipmentName());
                            equipmentProp.addProperty("zone", ze.getZoneName());
                            equipmentProp.addProperty("parentsystemID", equipmentIdMap.get(ze.getParentSystemObjectType() + ":" + ze.getParentSystemName()));
                            equipmentProp.addProperty("coolingsequence", ze.getCoolingSequence());
                            equipmentProp.addProperty("heatingsequence", ze.getHeatingSequence());

                            //fill in the properties for this equipmentProp
                            //System.out.println(equipType + " " + equipName);
                            IDFObject object = eplusData.getIDFObjectByName(ze.getEquipmentObjectType(), ze.getEquipmentName());
                            if (object == null) {
                                LOG.error(ze.getEquipmentObjectType() + " not found: " + ze.getEquipmentObjectType());
                                continue;
                            }
                            //System.out.println(object);
                            equipmentProp.add("properties", populateToJsonSeparateProperties(object, eplusData.getVersion(), 0, 3));//maximum trace to 3 levels

                            equipmentPropertyArray.add(equipmentProp);

                        }
                    }
                }
            }
            equipmentListObject.add("zone", equipmentPropertyArray);
        }
        return equipmentListObject;
    }

    public JsonObject getObjectsData() {
        if (iddParser == null) {
            //TODO raise error: "insufficient data for HVAC data tracing"
            return null;
        }
        //long startTime = System.currentTimeMillis();

        JsonObject equipmentListObject = new JsonObject();
        JsonArray equipmentPropertyArray = new JsonArray();
        for (ZoneEquipmentList zel : equipmentListArray) {
            //System.out.println("Processing: " + zel.getEquipmentListName());

            for (int i = 0; i < zel.getSizeOfEquipment(); i++) {
                ZoneEquipment ze = zel.getEquipment(i);
                //System.out.println("Start with: " + ze.getEquipmentName() + " " + (System.currentTimeMillis() - startTime)/1000 + "s");

                JsonObject equipmentProp = new JsonObject();
                String equipName = ze.getEquipmentName();
                String equipType = ze.getEquipmentObjectType();
                //setup properties

                String zoneId = equipmentIdMap.get(equipType + ":" + equipName);  //if zoneId is null, parentsystemID points to a system, otherwise points to itselfs

                equipmentProp.addProperty("id", zoneId);
                equipmentProp.addProperty("name", equipName);
                equipmentProp.addProperty("zone", ze.getZoneName());

                String systemType = ze.getParentSystemObjectType();
                String systemName = ze.getParentSystemName();
                String equipId = equipmentIdMap.get(systemType + ":" + systemName);
                equipmentProp.addProperty("parentsystemID", equipId);
                equipmentProp.addProperty("parentsystemName", systemName);
                equipmentProp.addProperty("coolingsequence", ze.getCoolingSequence());
                equipmentProp.addProperty("heatingsequence", ze.getHeatingSequence());
                //System.out.println("Record the basic info " + (System.currentTimeMillis() - startTime)/1000 + "s");

                //fill in the properties for this equipmentProp
                //System.out.println(equipType + " " + equipName);
                IDFObject object = eplusData.getIDFObjectByName(equipType, equipName);
                if (object == null) {
                    LOG.error(equipType + " not found: " + equipName);
                    continue;
                }
                //System.out.println("Get the object for processing " + (System.currentTimeMillis() - startTime)/1000 + "s");

                //System.out.println(object);
                equipmentProp.add("properties", populateToJsonSeparateProperties(object, eplusData.getVersion(), 0, 2));//maximum trace to 3 levels
                //System.out.println("Populate the objects " + (System.currentTimeMillis() - startTime)/1000 + "s");

                equipmentPropertyArray.add(equipmentProp);
                //System.out.println("Add equipment " + (System.currentTimeMillis() - startTime)/1000 + "s");

                //System.out.println("Finish with " + ze.getEquipmentName());
            }
        }
        equipmentListObject.add("zone", equipmentPropertyArray);

        //add systems - this only records central systems
        JsonArray systemPropertyArray = new JsonArray();
        Iterator<String> systemType = hvacMap.keySet().iterator();
        //first level
        while (systemType.hasNext()) {
            String system = systemType.next();
            if (system.equalsIgnoreCase("AirLoopHVAC")) {
                //second level
                Iterator<String> systemNameList = hvacMap.get(system).keySet().iterator();
                while (systemNameList.hasNext()) {
                    String systemName = systemNameList.next();

                    JsonObject systemProp = new JsonObject();
                    systemProp.addProperty("id", equipmentIdMap.get("AirLoopHVAC:" + systemName));
                    System.out.println(equipmentIdMap.get("AirLoopHVAC:DOAS"));
                    systemProp.addProperty("system_type", "AirLoopHVAC");
                    systemProp.addProperty("name", systemName);
                    JsonArray systemEquip = new JsonArray();

                    IDFObject obj = eplusData.getIDFObjectByName(system, systemName);
                    if (obj == null) {
                        LOG.error(system + " not found: " + systemName);
                        continue;
                    }
                    String branchListName = obj.getDataByStandardComment("Branch List Name");

                    obj = eplusData.getIDFObjectByName("BranchList", branchListName);
                    if (obj == null) {
                        LOG.error("BranchList not found: " + branchListName);
                        continue;
                    }
                    String branchName = obj.getDataByStandardComment("Branch 1 Name");
                    //TODO hack for non standard comment
                    if (branchName == null) {
                        branchName = obj.getIndexedData(1);
                    }

                    IDFObject branch = eplusData.getIDFObjectByName("Branch", branchName);
                    if (branch == null) {
                        LOG.error("Branch not found: " + branchName);
                        continue;
                    }

                    //need to analyze the branch - get object type and name
                    int beginOfExtension = iddParser.getObject("Branch").getBeginningOfExtensible();
                    int numExtension = iddParser.getObject("Branch").numOfExtensibles();
                    ///System.out.println(beginOfExtension + " " + numExtension + " " + branch.getMaxLineLen());
                    int counter = beginOfExtension;

                    while (branch.getObjLen() - 1 > counter) {
                        String objectType = branch.getIndexedData(counter);
                        String objectName = branch.getIndexedData(counter + 1);
                        //System.out.println(objectType + " " + objectName);
                        counter += numExtension;
                        IDFObject equip = eplusData.getIDFObjectByName(objectType, objectName);
                        if (equip == null) {
                            LOG.error(objectType + " not found: " + objectName);
                            continue;
                        }
                        //systemProp.add("properties", populateToJsonSeparateProperties(equip, eplusData.getVersion(), 0, 3));//maximum trace to 3 levels
                        systemEquip.add(populateToJsonSeparateProperties(equip, eplusData.getVersion(), 0, 4));
                    }
                    systemProp.add("system", systemEquip);
                    systemPropertyArray.add(systemProp);
                }
            } else if (system.equalsIgnoreCase("AirConditioner:VariableRefrigerantFlow") || system.equalsIgnoreCase("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl") ||
                    system.equalsIgnoreCase("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:HR")) {
                Iterator<String> systemNameList = hvacMap.get(system).keySet().iterator();
                while (systemNameList.hasNext()) {
                    String systemName = systemNameList.next();
                    //System.out.println(systemName);

                    JsonObject systemProp = new JsonObject();
                    JsonArray systemEquip = new JsonArray();
                    systemProp.addProperty("id", equipmentIdMap.get(system + ":" + systemName));
                    systemProp.addProperty("system_type", system);
                    systemProp.addProperty("name", systemName);
                    IDFObject vrfAirConditioner = eplusData.getIDFObjectByName(system, systemName);
                    if (vrfAirConditioner == null) {
                        LOG.error(system + " not found: " + systemName);
                        continue;
                    }
                    systemEquip.add(populateToJsonSeparateProperties(vrfAirConditioner, eplusData.getVersion(), 0, 3));
                    systemProp.add("system", systemEquip);//maximum trace to 3 levels

                    systemPropertyArray.add(systemProp);
                }
            }
        }

        equipmentListObject.add("system", systemPropertyArray);

        return equipmentListObject;
    }

    public Map<String, Map<String, List<ZoneEquipment>>> getHVACZoning() {
        return this.hvacMap;
    }

    /*
     * debug purpose
     */
    protected HashMap<String, String> getIdMap() {
        return equipmentIdMap;
    }

    private void rearrangeHVAC() {
        for (ZoneEquipmentList zel : equipmentListArray) {
            for (int i = 0; i < zel.getSizeOfEquipment(); i++) {
                ZoneEquipment ze = zel.getEquipment(i);

                String parentSystemObjectType = ze.getParentSystemObjectType();
                if (parentSystemObjectType == null) {
                    LOG.warn("Zone equipment " + ze.getEquipmentName() + "'s parent system object type is null");
                    continue;
                }

                if (!hvacMap.containsKey(parentSystemObjectType)) {

                    //LOG.info(ze.getParentSystemObjectType());

                    hvacMap.put(parentSystemObjectType, new HashMap<>());
                }

                if (!hvacMap.get(parentSystemObjectType).containsKey(ze.getParentSystemName())) {

                    //LOG.info(ze.getParentSystemName());

                    hvacMap.get(parentSystemObjectType).put(ze.getParentSystemName(),
                            new ArrayList<ZoneEquipment>());
                }

                hvacMap.get(parentSystemObjectType).get(ze.getParentSystemName()).add(ze);
            }
        }
    }

    private void hvacThermalZoneGroup() {
        //--// HashMap<String, ArrayList<ValueNode>> equipList = eplusData.getObjectListCopy("ZoneHVAC:EquipmentList");
        Map<String, IDFObject> equipList = eplusData.getCategoryMap("ZoneHVAC:EquipmentList");

        if (equipList == null) {
            return;
        }

        Iterator<String> equipmentListIterator = equipList.keySet().iterator();
        while (equipmentListIterator.hasNext()) {
            //--// ArrayList<ValueNode> zoneEquipmentList = equipList.get(equipmentListIterator.next());
            IDFObject zoneEquipmentList = equipList.get(equipmentListIterator.next());

            //--// ZoneEquipmentList equipmentList = new ZoneEquipmentList(zoneEquipmentList.get(0).getAttribute());
            ZoneEquipmentList equipmentList = new ZoneEquipmentList(zoneEquipmentList.getName());

            String[] fields = zoneEquipmentList.getData();  //includes everything except object label

            //loop over all the available equipment
            int offset = ModelUtil.isVersionEqOrLater(this.version, "8.9") ? 2 : 1;
            int group = ModelUtil.isVersionEqOrLater(this.version, "9.0") ? 6 : 4;
            int size = (fields.length - 1) / group; //fields.length-1 => exclude name field
            for (int i = 1; i <= size; i++) {
                int index = offset + (i - 1) * group; // object type index

                // Name of the zone equipment List
                String equipmentType = fields[index];
                String name = equipmentList.getEquipmentListName();

                equipmentIdMap.put(equipmentType + ":" + name, createID());

                ZoneEquipment equip = new ZoneEquipment(fields[index + 1]); // equipment name
                equip.setCoolingSequence(fields[index + 2]);
                equip.setHeatingSequence(fields[index + 3]);

                // Cases
                if (equipmentType.equalsIgnoreCase("ZoneHVAC:TerminalUnit:VariableRefrigerantFlow")) {
                    // look for parent system in ZoneHVAC:EquipmentList;
                    equip.setObjectType(equipmentType);

                    processVRFEquipment(equip, name);

                    equipmentList.putEquipment(equip);
                } else if (equipmentType.equalsIgnoreCase("ZoneHVAC:AirDistributionUnit")
                        || equipmentType.equalsIgnoreCase("AirTerminal:SingleDuct:Uncontrolled")) {
                    // air terminal series, search for nodes
                    equip.setObjectType(equipmentType);

                    processAirTerminalEquipment(equip, name);

                    equipmentList.putEquipment(equip);

                } else {
                    equip.setObjectType(equipmentType);
                    equip.setParentSystemName(equip.getEquipmentName());// zone

                    equip.setParentSystemObjectType(equip.getEquipmentObjectType());

                    //check ID
                    if (!equipmentIdMap.containsKey(equip.getEquipmentObjectType() + ":" + equip.getEquipmentName())) {
                        equipmentIdMap.put(equip.getEquipmentObjectType() + ":" + equip.getEquipmentName(), createID());
                    }

                    //--// HashMap<String, ArrayList<ValueNode>> equipConnectList = eplusData.getObjectListCopy("ZoneHVAC:EquipmentConnections");
                    Map<String, IDFObject> equipConnectList = eplusData.getCategoryMap("ZoneHVAC:EquipmentConnections");

                    Iterator<String> zoneEquipIterator = equipConnectList.keySet().iterator();
                    while (zoneEquipIterator.hasNext()) {
                        //--// ArrayList<ValueNode> equipConnect = equipConnectList.get(zoneEquipIterator.next());
                        IDFObject equipConnect = equipConnectList.get(zoneEquipIterator.next());

                        if (equipConnect.getIndexedData(1).equalsIgnoreCase(name)) {
                            equip.setZoneName(equipConnect.getName());
                            break; // find the target, stop
                        }
                    }

                    equipmentList.putEquipment(equip);
                }
            }
            equipmentListArray.add(equipmentList);
        }
    }

    private void processAirTerminalEquipment(ZoneEquipment equip, String equipListName) {

        String returnAirNode = "";
        boolean isPlenum = false;

        //--// HashMap<String, ArrayList<ValueNode>> equipConnectList = eplusData.getObjectListCopy("ZoneHVAC:EquipmentConnections");
        Map<String, IDFObject> equipConnectList = eplusData.getCategoryMap("ZoneHVAC:EquipmentConnections");

        Iterator<String> zoneEquipIterator = equipConnectList.keySet().iterator();
        while (zoneEquipIterator.hasNext()) {
            //--// ArrayList<ValueNode> equipConnect = equipConnectList.get(zoneEquipIterator.next());
            IDFObject equipConnect = equipConnectList.get(zoneEquipIterator.next());

            if (equipConnect.getIndexedData(1).equalsIgnoreCase(equipListName)) {
                equip.setZoneName(equipConnect.getName());

                //--// returnAirNode = equipConnect.get(equipConnect.size() - 1).getAttribute();
                returnAirNode = equipConnect.getIndexedData(equipConnect.getData().length - 1);

                break; // find the target, stop
            }
        }

        //check nodelist
        List<IDFObject> nodeList = eplusData.getCategoryList("NodeList");
        for (int i = 0; i < nodeList.size(); i++) {
            if (nodeList.get(i).getName().equals(returnAirNode)) {
                //only one return air node, idiot put it in the nodelist so we
                //extract it out
                returnAirNode = nodeList.get(i).getIndexedData(1);
            }
        }

        // Now look for this return air node in : AirLoopHVAC:ZoneMixer or
        // AirLoopHVAC:ReturnPlenum
        // First, exam AirLoopHVAC:ReturnPlenum
        String returnComponent = "";

        //--// HashMap<String, ArrayList<ValueNode>> returnPlenum = eplusData.getObjectListCopy("AirLoopHVAC:ReturnPlenum");
        Map<String, IDFObject> returnPlenum = eplusData.getCategoryMap("AirLoopHVAC:ReturnPlenum");

        if (returnPlenum != null) {
            Iterator<String> plenumIterator = returnPlenum.keySet().iterator();
            while (plenumIterator.hasNext()) {
                //--// ArrayList<ValueNode> plenumList = returnPlenum.get(plenumIterator.next());
                IDFObject plenumList = returnPlenum.get(plenumIterator.next());

                String[] fields = plenumList.getData();
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].equalsIgnoreCase(returnAirNode)) {
                        // 3 is the outlet node name of DOAS
                        returnComponent = fields[3];
                        isPlenum = true;
                    }
                }
            }
        }

        //--// HashMap<String, ArrayList<ValueNode>> returnMixer = eplusData.getObjectListCopy("AirLoopHVAC:ZoneMixer");
        Map<String, IDFObject> returnMixer = eplusData.getCategoryMap("AirLoopHVAC:ZoneMixer");
        //System.out.println("This is the return air node:" + returnAirNode);
        if (returnMixer != null) {
            Iterator<String> mixerIterator = returnMixer.keySet().iterator();
            while (mixerIterator.hasNext()) {
                //--// ArrayList<ValueNode> mixerList = returnMixer.get(mixerIterator.next());
                IDFObject mixerList = returnMixer.get(mixerIterator.next());
                String[] data = mixerList.getData();

                for (int i = 2; i < data.length; i++) {  //iterate through inlet node name (?)
                    if (data[i].equalsIgnoreCase(returnAirNode) || (isPlenum && data[i].equalsIgnoreCase(returnComponent))) {
                        returnComponent = data[1];
                    }
                }
            }
        }

        // look for airloop now
        //--// HashMap<String, ArrayList<ValueNode>> airLoopList = eplusData.getObjectListCopy("AirLoopHVAC");
        Map<String, IDFObject> airLoopList = eplusData.getCategoryMap("AirLoopHVAC");

        Iterator<String> airLoopIterator = airLoopList.keySet().iterator();
        while (airLoopIterator.hasNext()) {
            // ArrayList<ValueNode> airLoop = airLoopList.get(airLoopIterator.next());
            IDFObject airLoop = airLoopList.get(airLoopIterator.next());
            String[] data = airLoop.getData();
            String[] comments = airLoop.getStandardComments();

            for (int i = 0; i < comments.length; i++) {
                //System.out.println("This is the return component" + returnComponent);
                //System.out.println(equip.getEquipmentName() + " " + airLoop.getName() + " " + returnComponent);
                if (comments[i].equalsIgnoreCase("Demand Side Outlet Node Name") && data[i].equalsIgnoreCase(returnComponent)) {
                    equip.setParentSystemName(airLoop.getName());
                    equip.setParentSystemObjectType("AirLoopHVAC");

                    //check ID
                    if (!equipmentIdMap.containsKey("AirLoopHVAC:" + airLoop.getName())) {
                        equipmentIdMap.put("AirLoopHVAC:" + airLoop.getName(), createID());
                    }
                }
            }
        }
    }

    private void processVRFEquipment(ZoneEquipment equip, String equipListName) {
        //this part extracts the thermal zone name of this particular equipment
        //--// HashMap<String, ArrayList<ValueNode>> equipConnectList = eplusData.getObjectListCopy("ZoneHVAC:EquipmentConnections");
        Map<String, IDFObject> equipConnectList = eplusData.getCategoryMap("ZoneHVAC:EquipmentConnections");

        Iterator<String> zoneEquipIterator = equipConnectList.keySet().iterator();
        while (zoneEquipIterator.hasNext()) {
            //--// ArrayList<ValueNode> equipConnect = equipConnectList.get(zoneEquipIterator.next());
            IDFObject equipConnect = equipConnectList.get(zoneEquipIterator.next());

            if (equipConnect.getIndexedData(1).equalsIgnoreCase(equipListName)) {
                equip.setZoneName(equipConnect.getName());
                break; // find the target, stop
            }
        }

        // now look for the equipment's parent system
        //first of all, find this terminal unit in the zoneterminalunitlist object
        String terminalListName = ""; // use to look for the airconditioners
        //--// HashMap<String, ArrayList<ValueNode>> zoneTerminalList = eplusData.getObjectListCopy("ZoneTerminalUnitList");
        Map<String, IDFObject> zoneTerminalList = eplusData.getCategoryMap("ZoneTerminalUnitList");

        Iterator<String> zoneTerminalListIterator = zoneTerminalList.keySet().iterator();
        while (zoneTerminalListIterator.hasNext()) {
            //--// ArrayList<ValueNode> zoneTerminal = zoneTerminalList.get(zoneTerminalListIterator.next());
            IDFObject zoneTerminal = zoneTerminalList.get(zoneTerminalListIterator.next());
            String[] data = zoneTerminal.getData();

            for (int i = 1; i < data.length; i++) {
                if (equip.getEquipmentName().equalsIgnoreCase(data[i])) {
                    terminalListName = zoneTerminal.getName();
                    break;
                }
            }
        }

        // find parent system in airconditioner:variableRefrigerantFlow
        //--// HashMap<String, ArrayList<ValueNode>> airCondVRFList = eplusData.getObjectListCopy("AirConditioner:VariableRefrigerantFlow");
        Map<String, IDFObject> airCondVRFList = eplusData.getCategoryMap("AirConditioner:VariableRefrigerantFlow");
        if (airCondVRFList != null && !airCondVRFList.isEmpty()) {
            Iterator<String> vrfIterator = airCondVRFList.keySet().iterator();
            while (vrfIterator.hasNext()) {
                //--// ArrayList<ValueNode> vrfProperty = airCondVRFList.get(vrfIterator.next());
                IDFObject vrfProperty = airCondVRFList.get(vrfIterator.next());
                String[] data = vrfProperty.getData();
                String[] comments = vrfProperty.getStandardComments();

                for (int i = 0; i < comments.length; i++) {
                    //System.out.println("This is the terminal list: " + terminalListName + " this is the comment " + comments[i] + " this is the data " + data[i]);
                    if (comments[i].equalsIgnoreCase("Zone Terminal Unit List Name") && data[i].equalsIgnoreCase(terminalListName)) {
                        //System.out.println("This is the selected data: " + data[i]);
                        equip.setParentSystemName(vrfProperty.getName());
                        equip.setParentSystemObjectType("AirConditioner:VariableRefrigerantFlow");

                        //check ID
                        if (!equipmentIdMap.containsKey("AirConditioner:VariableRefrigerantFlow:" + vrfProperty.getName())) {
                            equipmentIdMap.put("AirConditioner:VariableRefrigerantFlow:" + vrfProperty.getName(), createID());
                        }

                        break;
                    }
                }
            }
        }

        Map<String, IDFObject> airCondVRFFluidList = eplusData.getCategoryMap("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl");
        if (airCondVRFFluidList != null && !airCondVRFFluidList.isEmpty()) {
            Iterator<String> vrfFluidIterator = airCondVRFFluidList.keySet().iterator();
            while (vrfFluidIterator.hasNext()) {
                //--// ArrayList<ValueNode> vrfProperty = airCondVRFList.get(vrfIterator.next());
                IDFObject vrfProperty = airCondVRFFluidList.get(vrfFluidIterator.next());
                String[] data = vrfProperty.getData();
                String[] comments = vrfProperty.getStandardComments();

                for (int i = 0; i < comments.length; i++) {
                    if (comments[i].equalsIgnoreCase("Zone Terminal Unit list Name") && data[i].equalsIgnoreCase(terminalListName)) {

                        equip.setParentSystemName(vrfProperty.getName());
                        equip.setParentSystemObjectType("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl");

                        //check ID
                        if (!equipmentIdMap.containsKey("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:" + vrfProperty.getName())) {
                            equipmentIdMap.put("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:" + vrfProperty.getName(), createID());
                        }

                        break;
                    }
                }
            }
        }

        Map<String, IDFObject> airCondVRFFluidHRList = eplusData.getCategoryMap("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:HR");
        if (airCondVRFFluidHRList != null && !airCondVRFFluidHRList.isEmpty()) {
            Iterator<String> airCondVRFFluidHRItr = airCondVRFFluidHRList.keySet().iterator();
            while (airCondVRFFluidHRItr.hasNext()) {
                //--// ArrayList<ValueNode> vrfProperty = airCondVRFList.get(vrfIterator.next());
                IDFObject vrfProperty = airCondVRFFluidList.get(airCondVRFFluidHRItr.next());
                String[] data = vrfProperty.getData();
                String[] comments = vrfProperty.getStandardComments();

                for (int i = 0; i < comments.length; i++) {
                    if (comments[i].equalsIgnoreCase("Zone Terminal Unit list Name") && data[i].equalsIgnoreCase(terminalListName)) {

                        equip.setParentSystemName(vrfProperty.getName());
                        equip.setParentSystemObjectType("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:HR");

                        //check ID
                        if (!equipmentIdMap.containsKey("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:HR:" + vrfProperty.getName())) {
                            equipmentIdMap.put("AirConditioner:VariableRefrigerantFlow:FluidTemperatureControl:HR:" + vrfProperty.getName(), createID());
                        }
                        break;
                    }
                }
            }
        }

    }

    private JsonObject populateToJsonSeparateProperties(IDFObject idfObj, String idfVersion, int level, int maximum) {


        //3 levels maximum
        if (level >= maximum) {
            return null;
        }

        String label = idfObj.getObjLabel();
        JsonObject fieldsJo = new JsonObject();

        //List<String[]> fields = ObjectFields.getFieldsNameAndUnit(idfVersion, label);


        //int size = fields.size();

        String[] vals = idfObj.getData();
        int len = vals.length;

        fieldsJo.addProperty("name", label);

        JsonArray properties = new JsonArray();
        int size = idfObj.getObjLen() - 1;
        for (int i = 0, j = 0; i < len && j < size; i++, j++) {
            //String[] field = fields.get(j);

            JsonObject prop = new JsonObject();
            EnergyPlusFieldTemplate field = iddParser.getObject(idfObj.getObjLabel()).getFieldTemplateByIndex(i);
            if (field == null) {
                LOG.error("Cannot find field: " + idfObj.getObjLabel() + ", " + i);
                throw new IllegalStateException();
            }

            prop.addProperty("name", field.getFieldName());
            prop.addProperty("value", vals[i]);
            if (field.getUnit() != null) {
                prop.addProperty("unit", field.getUnit());
            } else {
                prop.addProperty("unit", "");
            }


            //System.out.println(label + " " + field[0]);
            if (field.getObjectListRef().size() > 0 &&
                    !vals[i].isEmpty() && (!field.getFieldName().contains("Schedule") && !field.getFieldName().contains("Curve"))) {
                //this field has linked object
                //System.out.println(label + " " + field[0] + " " + vals[i]);

                IDFObject childObj = iddParser.getLinkedObject(label, field.getFieldName(), vals[i]);
                //hack for non standard comment
                if (childObj == null) {
                    childObj = iddParser.getLinkedObject(label, field.getFieldIdx(), vals[i]);
                }

                if (childObj == null) {
                    continue; //hack, when current field if object type and next field if object name
                }

                //System.out.println(childObj.getObjLabel());
                int childLevel = level + 1;
                prop.add("child", populateToJsonSeparateProperties(childObj, idfVersion, childLevel, maximum));

            }

            properties.add(prop);
        }

        fieldsJo.add("properties", properties);

        return fieldsJo;
    }

    private String createID() {
        return String.valueOf(idCounter.getAndIncrement());
    }

}