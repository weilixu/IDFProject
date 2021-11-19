package main.java.model.idd;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import main.java.config.ServerConfig;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.util.NumUtil;
import main.java.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

@SuppressWarnings("unused")
public class IDDParser implements Serializable {
    private static final long serialVersionUID = -2687097932614172299L;
    // group type element
    private static final String GROUPTOKEN = "\\group";
    private static final String ESCAPETOKEN = "!";
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private EnergyPlusTemplate temp;

    private String version;

    // processing necessary indicators
    private String currentGroup = "";
    private EnergyPlusObjectTemplate currentObject = null;
    private EnergyPlusFieldTemplate currentField = null;

    private boolean processGroup = false; // this skips the beginning misc lines
    private boolean processField = false;
    private boolean processObject = false;

    // private HashMap<String, ArrayList<String>> referenceListMap;
    private HashSet<String> referencedObjSet;
    private HashMap<String, ArrayList<String>> objectListMap;
    // private HashMap<String, IDFObject> nameToObjectMap;
    private HashMap<String, HashMap<String, IDFObject>> nameToObjectMap;// key -
    // hashMap
    // -
    // name,
    // object
    private HashMap<String, ArrayList<EnergyPlusFieldTemplate>> keyToFieldMap;
    private HashMap<String, ArrayList<String>> nodeListMap;// 1. Node Name, 2.
    // {ObjLabel:Name}
    // private ArrayList<String> nodeList;

    public IDDParser(String version) {
        if (version != null) {
            this.version = version;
        } else {
            // default to 8.7
            this.version = "8.7";
        }

        // referenceListMap = new HashMap<String, ArrayList<String>>();
        referencedObjSet = new HashSet<String>();
        objectListMap = new HashMap<String, ArrayList<String>>();
        // nameToObjectMap = new HashMap<String, IDFObject>();
        nameToObjectMap = new HashMap<String, HashMap<String, IDFObject>>();
        keyToFieldMap = new HashMap<String, ArrayList<EnergyPlusFieldTemplate>>();
        // nodeList = new ArrayList<String>();
        nodeListMap = new HashMap<String, ArrayList<String>>();

        processIdd();
        // processReferenceMap();
        processReferenceMap2();
    }

    public static void main(String[] args) {
    }

    public String getVersion() {
        return version;
    }

    private void processIdd() {
        temp = new EnergyPlusTemplate();
        try (BufferedReader br = new BufferedReader(
                new FileReader(new File(ServerConfig.readProperty("ResourcePath") + "idd_v" + version)))) {
            for (String line; (line = br.readLine()) != null; ) {
                // process the line
                int commentIndex = line.indexOf(ESCAPETOKEN);
                if (commentIndex > 0 && line.substring(0, line.indexOf(ESCAPETOKEN)).equals("")) {
                    continue;
                } else {
                    processLines(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processReferenceMap() {
        Iterator<String> eplusGroups = temp.getListofEnergyPlusGroupTemplate().iterator();
        while (eplusGroups.hasNext()) {// search group
            EnergyPlusGroupTemplate eplusGroupsTemp = temp.getEnergyPlusGroup(eplusGroups.next());
            Iterator<String> eplusObjects = eplusGroupsTemp.getObjectListFromGroup().iterator();
            while (eplusObjects.hasNext()) {// search object
                EnergyPlusObjectTemplate eplusObjTemp = eplusGroupsTemp.getObjectTemplate(eplusObjects.next());
                for (int i = 0; i < eplusObjTemp.getNumberOfFields(); i++) {// search
                    // fields
                    List<String> objRefList = eplusObjTemp.getFieldTemplateByIndex(i).getObjectListRef();
                    if (!objRefList.isEmpty()) {
                        for (int j = 0; j < objRefList.size(); j++) {// search
                            // each
                            // ref
                            if (!keyToFieldMap.containsKey(objRefList.get(j))) {
                                keyToFieldMap.put(objRefList.get(j), new ArrayList<EnergyPlusFieldTemplate>());
                            }
                            keyToFieldMap.get(objRefList.get(j)).add(eplusObjTemp.getFieldTemplateByIndex(i));
                        } // for
                    } // if
                } // for
            } // while
        } // while
    }

    private void processReferenceMap2() {
        Iterator<String> eplusGroups = temp.getListofEnergyPlusGroupTemplate().iterator();
        while (eplusGroups.hasNext()) {// search group
            EnergyPlusGroupTemplate eplusGroupsTemp = temp.getEnergyPlusGroup(eplusGroups.next());
            Iterator<String> eplusObjects = eplusGroupsTemp.getObjectListFromGroup().iterator();
            while (eplusObjects.hasNext()) {// search object
                EnergyPlusObjectTemplate eplusObjTemp = eplusGroupsTemp.getObjectTemplate(eplusObjects.next());
                for (int i = 0; i < eplusObjTemp.getNumberOfFields(); i++) {// search
                    // fields
                    List<String> objRefList = eplusObjTemp.getFieldTemplateByIndex(i).getReference(); // DIFF
                    // with
                    // processReferenceMap()
                    if (!objRefList.isEmpty()) {
                        for (int j = 0; j < objRefList.size(); j++) {// search
                            // each
                            // ref
                            if (!keyToFieldMap.containsKey(objRefList.get(j))) {
                                keyToFieldMap.put(objRefList.get(j), new ArrayList<EnergyPlusFieldTemplate>());
                            }
                            keyToFieldMap.get(objRefList.get(j)).add(eplusObjTemp.getFieldTemplateByIndex(i));
                        } // for
                    } // if
                } // for
            } // while
        } // while
    }

    public JsonObject getKeyFieldMap() {
        JsonObject mapData = new JsonObject();

        JsonArray arrayData = new JsonArray();

        Iterator<String> keys = keyToFieldMap.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            ArrayList<EnergyPlusFieldTemplate> templateList = keyToFieldMap.get(key);

            JsonObject objKey = new JsonObject();
            objKey.addProperty("referenceKey", key);

            JsonArray objArrays = new JsonArray();
            for (int i = 0; i < templateList.size(); i++) {
                EnergyPlusFieldTemplate eplusTemplate = templateList.get(i);
                JsonObject objProperty = new JsonObject();
                objProperty.addProperty("label", eplusTemplate.getObjName());
                objProperty.addProperty("field", eplusTemplate.getFieldName());
                objProperty.addProperty("index", eplusTemplate.getFieldIdx());
                objArrays.add(objProperty);
            }

            objKey.add("referenceArray", objArrays);
            arrayData.add(objKey);
        }
        mapData.add("map", arrayData);
        return mapData;
    }

    /**
     * Get the node link map - allow to show how the node connects to the other
     * objects in EnergyPlus
     *
     * @return
     */
    public JsonObject getNodeMapInJson() {
        JsonObject nodeJson = new JsonObject();

        Iterator<String> nodeItr = nodeListMap.keySet().iterator();
        while (nodeItr.hasNext()) {
            String node = nodeItr.next();
            JsonArray nodeObjectList = new JsonArray();

            ArrayList<String> objListStr = nodeListMap.get(node);
            for (int i = 0; i < objListStr.size(); i++) {
                JsonObject linkedObj = new JsonObject();
                String[] objInfo = objListStr.get(i).split("@");
                linkedObj.addProperty("label", objInfo[0]);
                linkedObj.addProperty("name", objInfo[1]);
                nodeObjectList.add(linkedObj);
            }

            nodeJson.add(node, nodeObjectList);
        }
        return nodeJson;
    }

    // /**
    // * return the available object names under the referenced value
    // *
    // * @param objName
    // * @param fieldName
    // * @return
    // */
    // public ArrayList<String> getListOfAvailableReference(String objName, String
    // fieldName) {
    // ArrayList<String> availReferenceList = new ArrayList<String>();
    // String key = (objName + ":" + fieldName).toLowerCase();
    // // System.out.println("The key: " + key);
    // if (objectListMap.containsKey(key)) {
    // ArrayList<String> objectList = objectListMap.get(key);
    // // System.out.println("Size of object list: " + objectList.size());
    // for (String s : objectList) {
    // availReferenceList.addAll(referenceListMap.get(s));
    // }
    // }
    // return availReferenceList;
    // }

    public HashMap<String, ArrayList<String>> getAvailableNode() {
        return nodeListMap;
    }

    public Set<String> getInvalidObjectList(IDFFileObject file) {
        Set<String> res = new HashSet<>();
        try {
            Map<String, List<IDFObject>> idf = file.getObjectsMap();
            Iterator<String> objectNameItr = idf.keySet().iterator();
            while (objectNameItr.hasNext()) {
                String objectName = objectNameItr.next();
                List<IDFObject> objectList = idf.get(objectName);

                JsonArray errorArray = new JsonArray();
                if (!validateObject(objectList, objectName, errorArray)) {
                    for (int i = 0; i < errorArray.size(); i++) {
                        String level = errorArray.get(i).getAsJsonObject().get("type").getAsString();
                        if (level.equals("Severe") || level.equals("Error")) {
                            res.add(objectName);
                            break;
                        }
                    }
                } else {
                    if (errorArray.size() != 0) {
                        // validationObj.add(objectName, errorArray);
                    }
                }
            }

            // second level - check the object to reference
            Iterator<String> objectListItr = objectListMap.keySet().iterator();
            int counter = 0;
            while (objectListItr.hasNext()) {
                counter++;
                String objFieldName = objectListItr.next();
                // System.out.println(objFieldName);
                ArrayList<String> listOfKeys = objectListMap.get(objFieldName);
                String[] objFieldNameList = objFieldName.split("@");
                String value = objFieldNameList[3];

                if (!referencedObjSet.contains(value) && !objFieldNameList[0].equals("branch")) {
                    res.add(objFieldNameList[0]);
                }
            }
        } catch (Exception ex) {
            LOG.error("Validation failed: ", ex);
        }
        return res;
    }
    
    public void fixIDF(IDFFileObject file) {
    		JsonObject validateObj = new JsonObject();
    		try {
             Map<String, List<IDFObject>> idf = file.getObjectsMap();
             Iterator<String> objectNameItr = idf.keySet().iterator();
             while (objectNameItr.hasNext()) {
            	 	String objectName = objectNameItr.next();
            	 	List<IDFObject> objectList = idf.get(objectName);
            	 	////dfdafdafa
            	 	fixObject(objectList, objectName);
             }
    		}catch(Exception ex) {
                LOG.error("Validation failed: ", ex);
    		}
    }

    public JsonObject validateIDF(IDFFileObject file) {
        JsonObject validationObj = new JsonObject();
        JsonArray validationErrorMsg = new JsonArray();
        try {
            Map<String, List<IDFObject>> idf = file.getObjectsMap();
            Iterator<String> objectNameItr = idf.keySet().iterator();
            while (objectNameItr.hasNext()) {
                String objectName = objectNameItr.next();
                List<IDFObject> objectList = idf.get(objectName);

                JsonArray errorArray = new JsonArray();
                if (!validateObject(objectList, objectName, errorArray)) {
                    validationErrorMsg.addAll(errorArray);
                } else {
                    if (errorArray.size() != 0) {
                        // validationObj.add(objectName, errorArray);
                    }
                }
            }

            // second level - check the object to reference
            Iterator<String> objectListItr = objectListMap.keySet().iterator();
            int counter = 0;
            while (objectListItr.hasNext()) {
                counter++;
                String objFieldName = objectListItr.next();
                // System.out.println(objFieldName);
                ArrayList<String> listOfKeys = objectListMap.get(objFieldName);
                String[] objFieldNameList = objFieldName.split("@");
                String value = objFieldNameList[3];

                if (!referencedObjSet.contains(value) && !objFieldNameList[0].equals("branch")) {
                    JsonObject messageObj = new JsonObject();
                    messageObj.addProperty("type", "Severe");
                    messageObj.addProperty("label", objFieldNameList[0]);
                    messageObj.addProperty("name", objFieldNameList[1]);
                    value = StringUtil.escapeBracket(value);
                    messageObj.addProperty("error_msg",
                            "The value: " + value + " connects to no other classes, use editor to debug");
                    validationErrorMsg.add(messageObj);
                }
                // System.out.println(counter);
            }

            if (validationErrorMsg.size() != 0) {
                StringBuilder sbServer = new StringBuilder();
                StringBuilder sbError = new StringBuilder();
                for (int i = 0; i < validationErrorMsg.size(); i++) {
                    JsonObject err = validationErrorMsg.get(i).getAsJsonObject();
                    String type = err.get("type").getAsString();
                    if (type.equalsIgnoreCase("Severe") || type.equalsIgnoreCase("Error")) {

                        StringBuilder block = new StringBuilder();
                        block.append("<tr><td style='font-weight:bold;padding-right:10px;'>").append(type)
                                .append(": </td><td style=''>")
                                .append(err.get("error_msg").getAsString() + "</td></tr>")
                                .append("<tr><td></td><td style='border-top:1px solid black;'>Class: ")
                                .append(err.get("label").getAsString() + ", Name: " + err.get("name").getAsString()
                                        + "</td></tr>")
                                .append("<tr style='height:15px;width:1px;'><td></td><td></td></tr>");

                        switch (type) {
                            case "Severe":
                                sbServer.append(block.toString());
                                break;
                            case "Error":
                                sbError.append(block.toString());
                                break;
                            default:
                        }
                    }
                }

                if (sbServer.length() > 0 || sbError.length() > 0) {
                    validationObj.addProperty("validation", "false");
                    validationObj.addProperty("error_msg",
                            "<div style='max-height:400px;overflow:auto;'><table style='text-align:left;'>"
                                    + sbServer.toString() + sbError.toString() + "</table></div>");
                } else {
                    validationObj.addProperty("validation", "true");
                }
            } else {
                validationObj.addProperty("validation", "true");
            }
        } catch (Exception ex) {
            LOG.error("Validation failed: ", ex);
            validationObj.addProperty("validation", "false");
        }
        return validationObj;
    }

    public EnergyPlusObjectTemplate getObject(String obj) {

        return temp.getEplusObjectTemplate(obj.toLowerCase());
    }

    public JsonObject convertIDDtoJSon() {
        JsonObject iddObj = new JsonObject();
        JsonArray iddArray = new JsonArray();
        Iterator<String> keyGroup = temp.getListofEnergyPlusGroupTemplate().iterator();
        while (keyGroup.hasNext()) {
            EnergyPlusGroupTemplate group = temp.getEnergyPlusGroup(keyGroup.next());
            Iterator<String> groupKeyObj = group.getObjectListFromGroup().iterator();

            while (groupKeyObj.hasNext()) {
                String objName = groupKeyObj.next();
                EnergyPlusObjectTemplate eplusObject = group.getObjectTemplate(objName);

                // initialize json
                JsonObject eplusJsonObject = new JsonObject();
                JsonArray objectFieldArray = new JsonArray();

                // fill in data
                eplusJsonObject.addProperty("name", eplusObject.getObjectName());// name
                eplusJsonObject.addProperty("memo", eplusObject.getMemo());// memo
                eplusJsonObject.addProperty("numfield", eplusObject.getNumberOfFields());// number
                // of
                // fields
                eplusJsonObject.addProperty("format", eplusObject.getFormat()); // formats
                // of
                // the
                // object
                eplusJsonObject.addProperty("minfield", eplusObject.getNumberOfMinFields()); // number
                // of
                // minimum
                // fields
                // required
                eplusJsonObject.addProperty("beginextensible", eplusObject.getBeginningOfExtensible()); // beginning
                // of
                // extensible
                eplusJsonObject.addProperty("numextensible", eplusObject.numOfExtensibles());// number
                // of
                // extensibles
                eplusJsonObject.addProperty("isextensible", eplusObject.isExtensible()); // boolean,
                // indicates
                // whether
                // the
                // object
                // is
                // extensible
                eplusJsonObject.addProperty("required", eplusObject.isRequiredObject()); // boolean,
                // indicates
                // whether
                // the
                // object
                // is
                // required
                eplusJsonObject.addProperty("unique", eplusObject.isUniqueObject()); // boolean,
                // indicates
                // whether
                // the
                // object
                // is
                // unique
                // (only
                // one
                // allowed
                // in
                // a
                // file)
                for (int i = 0; i < eplusObject.getNumberOfFields(); i++) {
                    EnergyPlusFieldTemplate eplusField = eplusObject.getFieldTemplateByIndex(i);

                    // initialize json
                    JsonObject eplusJsonField = new JsonObject();

                    // fill in data
                    eplusJsonField.addProperty("name", eplusField.getFieldName());// field
                    // comments
                    // /
                    // name
                    eplusJsonField.addProperty("note", eplusField.getNote());// field
                    // notes
                    eplusJsonField.addProperty("fieldtype", eplusField.getFieldType());// field
                    // type
                    // (A
                    // -
                    // string,
                    // N
                    // -
                    // numeric)
                    eplusJsonField.addProperty("type", eplusField.getType());// type
                    // of
                    // data
                    // (integer,
                    // real,
                    // alpha,
                    // choice,
                    // object-list,
                    // external-list,
                    // node)
                    eplusJsonField.addProperty("unitbased", eplusField.getUnitBasedField());// For
                    // field
                    // that
                    // may
                    // have
                    // multiple
                    // possible
                    // units
                    eplusJsonField.addProperty("unit", eplusField.getUnit());// field
                    // unit
                    eplusJsonField.addProperty("ipunit", eplusField.getIPUnit());// field
                    // unit
                    // in
                    // IP
                    eplusJsonField.addProperty("min", eplusField.getInclusiveMin().toString());// minimum
                    eplusJsonField.addProperty("max", eplusField.getInclusiveMax().toString());// maximum
                    eplusJsonField.addProperty("mingreater", eplusField.getMin().toString());// min>:
                    eplusJsonField.addProperty("maxsmaller", eplusField.getMax().toString());// max<:
                    eplusJsonField.addProperty("default", eplusField.getDefault());// field
                    // comments
                    // /
                    // name
                    eplusJsonField.addProperty("autocalculatable", eplusField.isAutoCalculatable());// field
                    // whether
                    // is
                    // autocalculatable
                    eplusJsonField.addProperty("autosizable", eplusField.isAutoSizable());// field
                    // whether
                    // is
                    // autosizable
                    eplusJsonField.addProperty("beginofextensible", eplusField.isBeginOfExtensible());// field
                    // is
                    // the
                    // beginning
                    // of
                    // extensible
                    eplusJsonField.addProperty("required", eplusField.isRequired());// field
                    // whether
                    // is
                    // required

                    eplusJsonField.addProperty("key", new Gson().toJson(eplusField.getKeys()));// field
                    // whether
                    // is
                    // autocalculatable
                    eplusJsonField.addProperty("objectlist", new Gson().toJson(eplusField.getObjectListRef()));// object
                    // list
                    // shows
                    // this
                    // field
                    // can
                    // reference
                    // to
                    // the
                    // other
                    // objects
                    eplusJsonField.addProperty("referencelist", new Gson().toJson(eplusField.getReference()));// reference
                    // list
                    // shows
                    // this
                    // field
                    // can
                    // be
                    // referenced
                    // by
                    // other
                    // objects

                    // add to object array
                    objectFieldArray.add(eplusJsonField);
                }
                eplusJsonObject.add("fieldlist", objectFieldArray);
                iddArray.add(eplusJsonObject);
            }
        }
        iddObj.add("objects", iddArray);
        return iddObj;
    }

    /*
     * Temporarily added by Haoyu Feng for label validation purpose
     *
     */
    public JsonObject getAllLabels() {
        JsonObject iddObj = new JsonObject();
        JsonArray iddArray = new JsonArray();
        Iterator<String> keyGroup = temp.getListofEnergyPlusGroupTemplate().iterator();
        while (keyGroup.hasNext()) {
            EnergyPlusGroupTemplate group = temp.getEnergyPlusGroup(keyGroup.next());
            Iterator<String> groupKeyObj = group.getObjectListFromGroup().iterator();

            while (groupKeyObj.hasNext()) {
                String objName = groupKeyObj.next();
                EnergyPlusObjectTemplate eplusObject = group.getObjectTemplate(objName);

                /*
                 * // initialize json JsonObject eplusJsonObject = new JsonObject(); JsonArray
                 * objectFieldArray = new JsonArray();
                 *
                 * // fill in data eplusJsonObject.addProperty("name",
                 * eplusObject.getObjectName());// name
                 */
                iddArray.add(eplusObject.getObjectName());
            }
        }
        iddObj.add("objects", iddArray);
        return iddObj;
    }

    public JsonObject convertIDDtoJSonByLabel(String objLabel) {
        JsonObject iddObj = new JsonObject();
        JsonArray iddArray = new JsonArray();
        Iterator<String> keyGroup = temp.getListofEnergyPlusGroupTemplate().iterator();
        while (keyGroup.hasNext()) {
            EnergyPlusGroupTemplate group = temp.getEnergyPlusGroup(keyGroup.next());
            Iterator<String> groupKeyObj = group.getObjectListFromGroup().iterator();

            while (groupKeyObj.hasNext()) {
                String objName = groupKeyObj.next();
                EnergyPlusObjectTemplate eplusObject = group.getObjectTemplate(objName);
                if (eplusObject.getObjectName().equals(objLabel)) {

                    // initialize json
                    JsonObject eplusJsonObject = new JsonObject();
                    JsonArray objectFieldArray = new JsonArray();

                    // fill in data
                    eplusJsonObject.addProperty("name", eplusObject.getObjectName());// name
                    eplusJsonObject.addProperty("memo", eplusObject.getMemo());// memo
                    eplusJsonObject.addProperty("numfield", eplusObject.getNumberOfFields());// number
                    // of
                    // fields
                    eplusJsonObject.addProperty("format", eplusObject.getFormat()); // formats
                    // of
                    // the
                    // object
                    eplusJsonObject.addProperty("minfield", eplusObject.getNumberOfMinFields()); // number
                    // of
                    // minimum
                    // fields
                    // required
                    eplusJsonObject.addProperty("beginextensible", eplusObject.getBeginningOfExtensible()); // beginning
                    // of
                    // extensible
                    eplusJsonObject.addProperty("isextensible", eplusObject.isExtensible()); // boolean,
                    // indicates
                    // whether
                    // the
                    // object
                    // is
                    // extensible
                    eplusJsonObject.addProperty("required", eplusObject.isRequiredObject()); // boolean,
                    // indicates
                    // whether
                    // the
                    // object
                    // is
                    // required
                    eplusJsonObject.addProperty("unique", eplusObject.isUniqueObject()); // boolean,
                    // indicates
                    // whether
                    // the
                    // object
                    // is
                    // unique
                    // (only
                    // one
                    // allowed
                    // in
                    // a
                    // file)

                    for (int i = 0; i < eplusObject.getNumberOfFields(); i++) {
                        EnergyPlusFieldTemplate eplusField = eplusObject.getFieldTemplateByIndex(i);

                        // initialize json
                        JsonObject eplusJsonField = new JsonObject();

                        // fill in data
                        eplusJsonField.addProperty("name", eplusField.getFieldName());// field
                        // comments
                        // /
                        // name
                        eplusJsonField.addProperty("note", eplusField.getNote());// field
                        // notes
                        eplusJsonField.addProperty("fieldtype", eplusField.getFieldType());// field
                        // type
                        // (A
                        // -
                        // string,
                        // N
                        // -
                        // numeric)
                        eplusJsonField.addProperty("type", eplusField.getType());// type
                        // of
                        // data
                        // (integer,
                        // real,
                        // alpha,
                        // choice,
                        // object-list,
                        // external-list,
                        // node)
                        eplusJsonField.addProperty("unitbased", eplusField.getUnitBasedField());// For
                        // field
                        // that
                        // may
                        // have
                        // multiple
                        // possible
                        // units
                        eplusJsonField.addProperty("unit", eplusField.getUnit());// field
                        // unit
                        eplusJsonField.addProperty("ipunit", eplusField.getIPUnit());// field
                        // unit
                        // in
                        // IP
                        eplusJsonField.addProperty("min", eplusField.getInclusiveMin().toString());// minimum
                        eplusJsonField.addProperty("max", eplusField.getInclusiveMax().toString());// maximum
                        eplusJsonField.addProperty("mingreater", eplusField.getMin().toString());// min>:
                        eplusJsonField.addProperty("maxsmaller", eplusField.getMax().toString());// max<:
                        eplusJsonField.addProperty("default", eplusField.getDefault());// field
                        // comments
                        // /
                        // name
                        eplusJsonField.addProperty("autocalculatable", eplusField.isAutoCalculatable());// field
                        // whether
                        // is
                        // autocalculatable
                        eplusJsonField.addProperty("autosizable", eplusField.isAutoSizable());// field
                        // whether
                        // is
                        // autosizable
                        eplusJsonField.addProperty("beginofextensible", eplusField.isBeginOfExtensible());// field
                        // is
                        // the
                        // beginning
                        // of
                        // extensible
                        eplusJsonField.addProperty("required", eplusField.isRequired());// field
                        // whether
                        // is
                        // required

                        eplusJsonField.addProperty("key", new Gson().toJson(eplusField.getKeys()));// field
                        // whether
                        // is
                        // autocalculatable
                        eplusJsonField.addProperty("objectlist", new Gson().toJson(eplusField.getObjectListRef()));// object
                        // list
                        // shows
                        // this
                        // field
                        // can
                        // reference
                        // to
                        // the
                        // other
                        // objects
                        eplusJsonField.addProperty("referencelist", new Gson().toJson(eplusField.getReference()));// reference
                        // list
                        // shows
                        // this
                        // field
                        // can
                        // be
                        // referenced
                        // by
                        // other
                        // objects

                        // add to object array
                        objectFieldArray.add(eplusJsonField);
                    }
                    eplusJsonObject.add("fieldlist", objectFieldArray);
                    iddArray.add(eplusJsonObject);
                    iddObj.add("objects", iddArray);
                    return iddObj;
                }
            }
        }
        iddObj.add("objects", iddArray);
        return iddObj;
    }

    /**
     * get a linked object with object label, the field and the object name.
     *
     * @param objLabel:  label of the host object
     * @param fieldName: name of the host object field
     * @param objName:   the field input
     * @return IDFObject or null
     */
    public IDFObject getLinkedObject(String objLabel, String fieldName, String objName) {
        EnergyPlusFieldTemplate field = getObject(objLabel.toLowerCase())
                .getFieldTemplateByName(fieldName.toLowerCase());

        List<String> objectLinkList = field.getObjectListRef();

        for (String s : objectLinkList) {
            if (nameToObjectMap.containsKey(s)) {
                if (nameToObjectMap.get(s).containsKey(objName.toLowerCase())) {
                    return nameToObjectMap.get(s).get(objName.toLowerCase());
                }
            }
        }
        return null;
    }

    public Set<String> getLinkedObjectNameSet(String objLabel, String fieldName) {
        EnergyPlusFieldTemplate field = getObject(objLabel.toLowerCase())
                .getFieldTemplateByName(fieldName.toLowerCase());

        List<String> objectLinkList = field.getObjectListRef();

        for (String s : objectLinkList) {
            if (nameToObjectMap.containsKey(s)) {
                return nameToObjectMap.get(s).keySet();
            }
        }

        return null;
    }

    /**
     * get a linked object with object label, the field index and the object name.
     */
    public IDFObject getLinkedObject(String objLabel, int fieldIdx, String objName) {
        EnergyPlusFieldTemplate field = getObject(objLabel.toLowerCase()).getFieldTemplateByIndex(fieldIdx);
        List<String> objectLinkList = field.getObjectListRef();

        // Iterator<String> keyItr = nameToObjectMap.keySet().iterator();
        // while(keyItr.hasNext()){
        // System.out.println(keyItr.next());
        // }
        for (String s : objectLinkList) {
            if (nameToObjectMap.containsKey(s)) {

                if (nameToObjectMap.get(s).containsKey(objName.toLowerCase())) {
                    return nameToObjectMap.get(s).get(objName.toLowerCase());
                }
            }
        }
        return null;
    }
    
    public void fixObject(List<IDFObject> objectList, String objectName) {
        EnergyPlusObjectTemplate objectTemplate = temp.getEplusObjectTemplate(objectName.toLowerCase());
        List<IDFObject> deleteList = new ArrayList<IDFObject>();
        List<IDFObject> addList = new ArrayList<IDFObject>();
        
        for(int i=0; i<objectList.size(); i++) {
        		IDFObject object = objectList.get(i);
        		
        		// get the characteristics of the object
            int minField = objectTemplate.getNumberOfMinFields();
            int numberOfField = objectTemplate.getNumberOfFields();
            int beginExtensible = objectTemplate.getBeginningOfExtensible();
            int extensibleNumber = -1;
            
         // WX 09/20/2017 change from 0 to -1, objects such as
            // energymanagementsystem:globalvariable begins extensible at index
            // of 0
            if (beginExtensible > -1) {
                extensibleNumber = objectTemplate.numOfExtensibles();
            }

            if (object.getData().length < minField) {
                //create a new object
            		deleteList.add(object);
            		IDFObject tempObj = new IDFObject(object.getObjLabel(), minField);
            		tempObj.setTopComments(new String[] { "!- Modified by BuildSimHub" });
            		for(int j=0; j<minField; j++) {
            			tempObj.setIndexedStandardComment(j, objectTemplate.getFieldTemplateByIndex(j).getFieldName());
            			if(j<object.getData().length) {
                			tempObj.setIndexedData(j, object.getIndexedData(j));
            			}else {
            				if(objectTemplate.getFieldTemplateByIndex(j).getDefault()!=null) {
                    			tempObj.setIndexedData(j, objectTemplate.getFieldTemplateByIndex(j).getDefault());
            				}else if(objectTemplate.getFieldTemplateByIndex(j).getKeys()!=null) {
            					tempObj.setIndexedData(j, objectTemplate.getFieldTemplateByIndex(j).getKeys().get(0));
            				}else if(objectTemplate.getFieldTemplateByIndex(j).getMin()>NumUtil.MIN_VALUE) {
            					tempObj.setIndexedData(j, ""+objectTemplate.getFieldTemplateByIndex(j).getMin());
            				}else if(objectTemplate.getFieldTemplateByIndex(j).getMin()<NumUtil.MAX_VALUE) {
            					tempObj.setIndexedData(j, ""+objectTemplate.getFieldTemplateByIndex(j).getMax());
            				}
            			}
            		}
            		addList.add(tempObj);
            		
            } else {
                // start read fields
                String[] data = object.getData();
                String[] comment = object.getStandardComments();
                if (comment == null) {
                    object.clearStandardComments();
                    comment = object.getStandardComments();
                }
                String[] unit = object.getUnit();

                for (int j = 0; j < data.length; j++) {
                    // if extensible, j might be a lot larger than the actual
                    // number of the field in the template
                    // check j
                    int index = j;
                    if (index >= numberOfField) {
                        // WX 09/20/2017 change from 0 to -1, objects such as
                        // energymanagementsystem:globalvariable begins
                        // extensible at index of 0
                        if (beginExtensible > -1 && extensibleNumber > 0) {
                            // reset index
                            index = beginExtensible + (index - numberOfField) % extensibleNumber;
                        } else {
                        		deleteList.add(object);
                        		IDFObject tempObj = new IDFObject(object.getObjLabel(), index);
                        		tempObj.setTopComments(new String[] { "!- Modified by BuildSimHub" });
                        		for(int k=0; k<index; k++) {
                        			tempObj.setIndexedStandardComment(k, objectTemplate.getFieldTemplateByIndex(k).getFieldName());
                        			tempObj.setIndexedData(k, data[k]);
                        		}
                        		
                        		addList.add(tempObj);
                        		break;
                        }
                    }
                    EnergyPlusFieldTemplate ft = objectTemplate.getFieldTemplateByIndex(index);

                    if (ft == null) {
                    		deleteList.add(object);
                    		IDFObject tempObj = new IDFObject(object.getObjLabel(), index);
                    		tempObj.setTopComments(new String[] { "!- Modified by BuildSimHub" });
                    		for(int k=0; k<index; k++) {
                    			tempObj.setIndexedStandardComment(k, objectTemplate.getFieldTemplateByIndex(k).getFieldName());
                    			tempObj.setIndexedData(k, data[k]);
                    		}
                    		addList.add(tempObj);
                        // stop processing this object
                        break;
                    }

                    if (comment[j] == null || !comment[j].equals(ft.getFieldName())) {
                        comment[j] = ft.getFieldName();
                    }

                    if (unit[j] == null) {
                        unit[j] = ft.getUnit();
                    }

                    // check the inputs
                    if (ft.getFieldType().equals("N")) {
                        // numeric value
                        Double value = null;
                        try {
                            value = Double.parseDouble(data[j]);
                        } catch (NumberFormatException e) {
                            if (ft.isRequired()) {
                                if (!(ft.isAutoCalculatable() || ft.isAutoSizable())) {
                                    data[j] = "0.0";
                                } else if (ft.isAutoCalculatable() && !data[j].equalsIgnoreCase("AutoCalculate")) {
                                		data[j] = "AutoCalculate";
                                } else if (ft.isAutoSizable() && !data[j].equalsIgnoreCase("autosize")) {
                                    data[j] = "Autosize";
                                }
                            } else {
                                if (!data[j].equals("")) {
                                    if (!(ft.isAutoCalculatable() || ft.isAutoSizable())) {
                                        data[j] = "0.0";
                                    } else if (ft.isAutoCalculatable() && !data[j].equalsIgnoreCase("AutoCalculate")) {
                                		   data[j] = "AutoCalculate";
                                    } else if (ft.isAutoSizable() && !data[j].equalsIgnoreCase("autosize")) {
                                        data[j] = "Autosize";
                                    }
                                }
                            }
                        } // catch

                        if (value != null) {
                            if (ft.getInclusiveMin() != null && value < ft.getInclusiveMin()) {
                                data[j] = "" + (ft.getInclusiveMin()+0.00001);
                            } // if

                            if (ft.getInclusiveMax() != null && value > ft.getInclusiveMax()) {
                                data[j] = "" + (ft.getInclusiveMax()+0.00001);
                            } // if
                        } else if (ft.getDefault() != null && !(ft.isAutoCalculatable() || ft.isAutoSizable())) {
                            data[j] = ft.getDefault();
                        }
                    } else {
                        // it is alpha data type
                        if (!ft.getKeys().isEmpty()) {
                            if (!ft.getKeys().contains(data[j].toLowerCase()) && ft.isRequired()) {
                                data[j] = ft.getKeys().get(0);
                            }
                        }
                    } // if-else
                }
            }
        }
    }

    public boolean validateObject(List<IDFObject> objectList, String objectName, JsonArray errorMessage) {
        EnergyPlusObjectTemplate objectTemplate = temp.getEplusObjectTemplate(objectName.toLowerCase());

        referencedObjSet.add(objectTemplate.getObjectName().toLowerCase());// WX Need to add this for reference class
        // name
        for (int i = 0; i < objectList.size(); i++) {
            IDFObject object = objectList.get(i);

            // get the characteristics of the object
            int minField = objectTemplate.getNumberOfMinFields();
            int numberOfField = objectTemplate.getNumberOfFields();
            int beginExtensible = objectTemplate.getBeginningOfExtensible();
            int extensibleNumber = -1;
            // WX 09/20/2017 change from 0 to -1, objects such as
            // energymanagementsystem:globalvariable begins extensible at index
            // of 0
            if (beginExtensible > -1) {
                extensibleNumber = objectTemplate.numOfExtensibles();
            }

            if (object.getData().length < minField) {
                JsonObject messageObj = new JsonObject();
                messageObj.addProperty("type", "Severe");
                messageObj.addProperty("label", objectName);
                messageObj.addProperty("name", object.getData()[0]);
                messageObj.addProperty("error_msg", "Object has less number of field (" + object.getData().length
                        + ") then the minimum requirement:" + minField);
                errorMessage.add(messageObj);
            } else {
                // start read fields
                String[] data = object.getData();
                String[] comment = object.getStandardComments();
                if (comment == null) {
                    object.clearStandardComments();
                    comment = object.getStandardComments();
                }
                String[] unit = object.getUnit();

                for (int j = 0; j < data.length; j++) {
                    // if extensible, j might be a lot larger than the actual
                    // number of the field in the template
                    // check j
                    int index = j;
                    if (index >= numberOfField) {
                        // WX 09/20/2017 change from 0 to -1, objects such as
                        // energymanagementsystem:globalvariable begins
                        // extensible at index of 0
                        if (beginExtensible > -1 && extensibleNumber > 0) {
                            // reset index
                            index = beginExtensible + (index - numberOfField) % extensibleNumber;
                        } else {
                            JsonObject messageObj = new JsonObject();
                            messageObj.addProperty("type", "Severe");
                            messageObj.addProperty("label", objectName);
                            messageObj.addProperty("name", object.getData()[0]);
                            messageObj.addProperty("error_msg", "Object has more fields (" + object.getData().length
                                    + ") than its maximum requirement:" + numberOfField);
                            errorMessage.add(messageObj);
                        }
                    }
                    EnergyPlusFieldTemplate ft = objectTemplate.getFieldTemplateByIndex(index);

                    if (ft == null) {
                        JsonObject messageObj = new JsonObject();
                        messageObj.addProperty("type", "Severe");
                        messageObj.addProperty("label", objectName);
                        messageObj.addProperty("name", object.getData()[0]);
                        messageObj.addProperty("error_msg", "Object has more fields (" + object.getData().length
                                + ") than its maximum requirement");
                        // stop processing this object
                        break;
                    }

                    if (comment[j] == null || !comment[j].equals(ft.getFieldName())) {
                        comment[j] = ft.getFieldName();
                    }

                    if (unit[j] == null) {
                        unit[j] = ft.getUnit();
                    }

                    // check the inputs
                    if (ft.getFieldType().equals("N")) {
                        // numeric value
                        Double value = null;
                        try {
                            value = Double.parseDouble(data[j]);
                        } catch (NumberFormatException e) {
                            if (ft.isRequired()) {
                                if (!(ft.isAutoCalculatable() || ft.isAutoSizable())) {
                                    JsonObject messageObj = new JsonObject();
                                    messageObj.addProperty("type", "Error");
                                    messageObj.addProperty("label", objectName);
                                    messageObj.addProperty("name", object.getData()[0]);
                                    messageObj.addProperty("error_msg",
                                            data[j] + "is not a valid input for Field: '" + ft.getFieldName() + "'");
                                    errorMessage.add(messageObj);

                                } else if (ft.isAutoCalculatable() && !data[j].equalsIgnoreCase("AutoCalculate")) {
                                    JsonObject messageObj = new JsonObject();
                                    messageObj.addProperty("type", "Error");
                                    messageObj.addProperty("label", objectName);
                                    messageObj.addProperty("name", object.getData()[0]);
                                    messageObj.addProperty("error_msg",
                                            data[j] + "is autocalculable but the key word is not AutoCalculate");
                                    errorMessage.add(messageObj);

                                } else if (ft.isAutoSizable() && !data[j].equalsIgnoreCase("autosize")) {
                                    JsonObject messageObj = new JsonObject();
                                    messageObj.addProperty("type", "Error");
                                    messageObj.addProperty("label", objectName);
                                    messageObj.addProperty("name", object.getData()[0]);
                                    messageObj.addProperty("error_msg",
                                            data[j] + "is autosizable but the key word is not autosize");
                                    errorMessage.add(messageObj);
                                }
                            } else {
                                if (!data[j].equals("")) {
                                    if (!(ft.isAutoCalculatable() || ft.isAutoSizable())) {
                                        JsonObject messageObj = new JsonObject();
                                        messageObj.addProperty("type", "Warning");
                                        messageObj.addProperty("label", objectName);
                                        messageObj.addProperty("name", object.getData()[0]);
                                        messageObj.addProperty("error_msg", data[j]
                                                + " is not a valid input for Field: '" + ft.getFieldName() + "'.");
                                        errorMessage.add(messageObj);
                                    } else if (ft.isAutoCalculatable() && !data[j].equalsIgnoreCase("AutoCalculate")) {
                                        JsonObject messageObj = new JsonObject();
                                        messageObj.addProperty("type", "Warning");
                                        messageObj.addProperty("label", objectName);
                                        messageObj.addProperty("name", object.getData()[0]);
                                        messageObj.addProperty("error_msg",
                                                data[j] + " is autocalculable but the key word is not AutoCalculate");
                                        errorMessage.add(messageObj);
                                    } else if (ft.isAutoSizable() && !data[j].equalsIgnoreCase("autosize")) {
                                        JsonObject messageObj = new JsonObject();
                                        messageObj.addProperty("type", "Warning");
                                        messageObj.addProperty("label", objectName);
                                        messageObj.addProperty("name", object.getData()[0]);
                                        messageObj.addProperty("error_msg",
                                                data[j] + " is autosizable but the key word is not autosize");
                                        errorMessage.add(messageObj);
                                    }
                                }
                            }
                        } // catch

                        if (value != null) {
                            if (ft.getInclusiveMin() != null && value < ft.getInclusiveMin()) {
                                JsonObject messageObj = new JsonObject();
                                messageObj.addProperty("type", "Error");
                                messageObj.addProperty("label", objectName);
                                messageObj.addProperty("name", object.getData()[0]);
                                messageObj.addProperty("error_msg", value + " is smaller than the minimum: "
                                        + ft.getInclusiveMin() + " in Field: '" + ft.getFieldName() + "'.");
                                errorMessage.add(messageObj);
                            } // if

                            if (ft.getInclusiveMax() != null && value > ft.getInclusiveMax()) {
                                JsonObject messageObj = new JsonObject();
                                messageObj.addProperty("type", "Error");
                                messageObj.addProperty("label", objectName);
                                messageObj.addProperty("name", object.getData()[0]);
                                messageObj.addProperty("error_msg", value + "is large than the maximum: "
                                        + ft.getInclusiveMax() + " in Field: '" + ft.getFieldName() + "'");
                                errorMessage.add(messageObj);
                            } // if
                        } else if (ft.getDefault() != null && !(ft.isAutoCalculatable() || ft.isAutoSizable())) {
                            data[j] = ft.getDefault();
                            JsonObject messageObj = new JsonObject();
                            messageObj.addProperty("type", "Warning");
                            messageObj.addProperty("label", objectName);
                            messageObj.addProperty("name", object.getData()[0]);
                            messageObj.addProperty("error_msg", "Field: '" + ft.getFieldName()
                                    + "' is empty, it is assigned with a default value: " + ft.getDefault());
                            errorMessage.add(messageObj);
                        }
                    } else {
                        // it is alpha data type
                        if (!ft.getKeys().isEmpty()) {
                            if (!ft.getKeys().contains(data[j].toLowerCase()) && ft.isRequired()) {
                                Object[] range = ft.getKeys().toArray();
                                JsonObject messageObj = new JsonObject();
                                messageObj.addProperty("type", "Warning");
                                messageObj.addProperty("label", objectName);
                                messageObj.addProperty("name", object.getData()[0]);
                                messageObj.addProperty("error_msg",
                                        "Field: '" + ft.getFieldName() + "' is a key field." + "Instead of " + data[j]
                                                + ", should select from the range of" + Arrays.toString(range));
                                errorMessage.add(messageObj);
                            }
                        }
                    } // if-else

                    if (ft.getType() != null && ft.getType().equals("node") && !data[j].isEmpty()) {
                        if (!nodeListMap.containsKey(data[j])) {
                            nodeListMap.put(data[j], new ArrayList<String>());
                        }
                        nodeListMap.get(data[j]).add(object.getObjLabel() + "@" + object.getName());
                    }

                    // start forming the reference - object-List map
                    List<String> referenceList = ft.getReference();
                    if (referenceList.size() > 0) {
                        referencedObjSet.add(data[j].toLowerCase());
                    }

                    for (String s : referenceList) {
                        // if (!referenceListMap.containsKey(s)) {
                        // referenceListMap.put(s, new ArrayList<String>());
                        // }
                        // String objKey = objectName + "@" + data[j];
                        // referenceListMap.get(s).add(objKey);// reference

                        if (!nameToObjectMap.containsKey(s)) {
                            nameToObjectMap.put(s, new HashMap<String, IDFObject>());
                        }
                        nameToObjectMap.get(s).put(data[j].toLowerCase(), object);
                    }

                    if (ft.getType() != null && ft.getType().equals("object-list") && !data[j].isEmpty()) {
                        List<String> objectListRef = ft.getObjectListRef();
                        String objFieldName = objectTemplate.getObjectName() + "@" + object.getName() + "@"
                                + ft.getFieldName() + "@" + data[j];
                        if (!objectListMap.containsKey(objFieldName.toLowerCase())) {// TODO WX possible error??
                            objectListMap.put(objFieldName.toLowerCase(), new ArrayList<String>());
                        }
                        for (String s : objectListRef) {
                            objectListMap.get(objFieldName.toLowerCase()).add(s);
                        }
                    }
                }
            }
        }
        return errorMessage.size() == 0;
    }

    private void processLines(String line) {
        // System.out.println(line + " " + processField);
        if (line.startsWith(GROUPTOKEN)) {
            processGroup = true;
            currentGroup = subStringWithNoToken(line, GROUPTOKEN);

            temp.addEnergyPlusGroup(new EnergyPlusGroupTemplate(currentGroup));
        } else if (processGroup && !line.equals("")) {
            if (isFirstCharactersNonWhiteSpace(line)) {
                // this line indicates an EnergyPlus object and its name
                currentObject = new EnergyPlusObjectTemplate(line.substring(0, line.lastIndexOf(",")).trim());
                processObject = true;
                temp.addEnergyPlusObject(currentGroup, currentObject);

                // turn the field processing off
                processField = false;
                currentField = null;
            } else if (isField(line)) {

                if (!line.endsWith("fields as indicated")) {// in this case,
                    // they are
                    // extensible,
                    // continue repeat
                    currentField = new EnergyPlusFieldTemplate(line.trim());
                    processField = true;

                    currentObject.addObjectField(currentField);
                    // turn off the flag of object processing
                    processObject = false;
                } else {
                    line = line.substring(0, line.indexOf("\\"));
                    String[] inputs = line.split(",");
                    // skip the last ,
                    for (int i = 0; i < inputs.length - 1; i++) {
                        String str = inputs[i];
                        // System.out.println(str);
                        EnergyPlusFieldTemplate field = new EnergyPlusFieldTemplate(
                                str.trim() + " \field extensible data " + i);
                        currentObject.addObjectField(field);
                    }
                }
            } else if (processObject) {
                // process Object level elements
                currentObject.processElement(line.trim());
                if (line.contains("\\required-object")) {
                    // EplusMap.get
                }
            } else if (processField) {
                // process Field level elements;
                if (line.contains("\\begin-extensible")) {
                    currentObject.setTheBeginningOfExtensible();
                }
                currentField.processElement(line.trim());
            }
        }
    }

    private String subStringWithNoToken(String line, String token) {
        return line.substring(token.length()).trim();
    }

    private boolean isFirstCharactersNonWhiteSpace(String line) {
        // System.out.println(line);
        return !Character.isWhitespace(line.charAt(0)) && line.charAt(0) != '\\' && !line.contains(GROUPTOKEN)
                && !line.contains(ESCAPETOKEN);
    }

    private boolean isField(String line) {
        String temp = line.trim();
        // System.out.println(temp);
        if (temp.isEmpty()) {
            return false;
        }

        if ((temp.charAt(0) == 'A' || temp.charAt(0) == 'N') && (int) temp.charAt(1) < 65) {
            return true;
        }

        return false;
        // return Character.isWhitespace(line.charAt(0)) &&
        // Character.isWhitespace(line.charAt(1)) && line.charAt(2)!='\\' &&
        // line.charAt(2)!=' ' && line.charAt(2)!='!';
    }
}
