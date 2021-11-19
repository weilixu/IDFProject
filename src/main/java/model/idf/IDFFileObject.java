package main.java.model.idf;

import main.java.model.meta.ModelFileObject;
import main.java.model.vc.AddIDFObjectResult;
import main.java.model.vc.BranchType;
import main.java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class IDFFileObject implements Serializable, ModelFileObject {
    private static final long serialVersionUID = -713045092413712529L;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    /**
     * NOTE: This class has deepClone() function, any member change should change deepClone() accordingly
     */

    //Key is made by IDFObjectKeyMaker
    private Map<String, List<IDFObject>> objMap;
    private Map<String, TreeMap<String, IDFObject>> objLabelKeyMap;
    private Map<String, IDFObject> objKeyMap;

    //save added IDFObject before we see version
    private List<IDFObject> tempList;

    private String version = null;
    private String fileHash = null;

    private int valueCommentPad = Global.IDF_DISPLAY_PADDING_VALUE_COMMENT;

    // for merge and grouped display
    private LinkedHashMap<String, Integer> sortedLabelMap = null;

    private Map<String, String> parametricSetting = null;

    private transient IDFObjectKeyMaker maker = null;

    public IDFFileObject() {
        this.objMap = new HashMap<>();
        this.objLabelKeyMap = new HashMap<>();
        this.objKeyMap = new HashMap<>();
        this.tempList = new ArrayList<>();
    }

    public void resetParametricSetting() {
        this.parametricSetting = new HashMap<>();
    }

    public void addParametricSetting(String key, String value) {
        if (parametricSetting == null) {
            parametricSetting = new HashMap<>();
        }
        parametricSetting.put(key, value);
    }

    public Map<String, String> getParametricSetting() {
        return parametricSetting;
    }

    public void setVersion() {
        this.version = this.getVersion();
    }

    /**
     * Return null if idfObj is null<br/>
     * Return "" if add successful<br/>
     * Return objLabel if duplicated
     */
    public String addObject(IDFObject idfObj) {
        if (idfObj == null) {
            return null;
        }

        if (this.version == null) {
            this.version = getVersion();
        }

        /* set version if first seen */
        String label = idfObj.getObjLabel().toLowerCase();
        if (label.equals("version")) {
            if (!StringUtil.isNullOrEmpty(this.version)) {
                LOG.error("Version object is duplicated");
                return "Version";
            }
            String version = idfObj.getIndexedData(0);
            //pick first two numbers
            String[] split = version.split("\\.");
            if (split.length > 1) {
                this.version = split[0] + "." + split[1];
            } else {
                return null;
            }

            maker = new IDFObjectKeyMaker(this.version);
        }

        /* add IDFObject to objMap: label->List<IDFObject> */
        if (!objMap.containsKey(label)) {
            objMap.put(label, new ArrayList<>());
        }
        objMap.get(label).add(idfObj);

        /* if version is not seen, put IDFObject to tempList */
        if (StringUtil.isNullOrEmpty(this.version)) {
            tempList.add(idfObj);
            return "";
        }

        /* if version is seen, clean tempList */
        StringBuilder duplicated = new StringBuilder();
        if (!tempList.isEmpty()) {
            for (IDFObject obj : tempList) {
                String key = maker.makeKey(obj);
                AddIDFObjectResult res = saveIDFObject(obj.getObjLabel().toLowerCase(), key, obj);
                if (res == AddIDFObjectResult.DUPLICATE) {

                    LOG.error(obj.getObjLabel() + " object has duplicates");
                    duplicated.append(" ").append(obj.getObjLabel()).append(" ");
                }
            }
            tempList.clear();
        }

        /* save current IDFObject */
        String key = maker.makeKey(idfObj);
        AddIDFObjectResult res = saveIDFObject(label, key, idfObj);
        if (res == AddIDFObjectResult.DUPLICATE) {
            LOG.error(idfObj.getObjLabel() + " object has duplicates");
            duplicated.append(" ").append(idfObj.getObjLabel()).append(" ");
        }

        this.fileHash = null;
        return duplicated.toString();
    }

    private AddIDFObjectResult saveIDFObject(String label, String key, IDFObject obj) {
        obj.updateKey(key);

        /* add IDFObject to objKeyMap: key->IDFObject */
        IDFObject pre = objKeyMap.put(key, obj);
        if (pre != null) {
            if (pre.getValues().equalsIgnoreCase(obj.getValues())) {
                //exact same
                List<IDFObject> list = objMap.get(label);
                for (int idx = 0; idx < list.size(); idx++) {
                    IDFObject idfObj = list.get(idx);
                    if (idfObj.getKey().equals(pre.getKey())) {
                        list.remove(idx);
                        return AddIDFObjectResult.SUCCESS;
                    }
                }

                //couldn't find the duplicate obj again...
                return AddIDFObjectResult.DUPLICATE;
            } else {
                return AddIDFObjectResult.DUPLICATE;
            }
        }

        /* add IDFObject to objNamedMap: label->{key->IDFObject} */
        if (!objLabelKeyMap.containsKey(label)) {
            objLabelKeyMap.put(label, new TreeMap<>());
        }
        objLabelKeyMap.get(label).put(key, obj);
        return AddIDFObjectResult.SUCCESS;
    }

    public void buildSortedLabelMap() {
        String version = getVersion();

        this.sortedLabelMap = ModelUtil.buildSortedLabelMap(version, this.objMap, BranchType.idf);
    }

    public List<IDFObject> getCategoryList(String idfLabel) {
        return objMap.get(idfLabel.toLowerCase());
    }

    public TreeMap<String, IDFObject> getCategoryMap(String idfLabel) {
        return this.objLabelKeyMap.get(idfLabel.toLowerCase());
    }

    public String getFileHash() {
        if (fileHash == null) {
            if (sortedLabelMap == null) {
                this.buildSortedLabelMap();
            }

            if (sortedLabelMap == null) {
                return null;
            }

            /* Use lambda to concatenate strings */
            /*String concatenateHash = sortedLabelMap.keySet().stream().map(key -> {
                TreeMap<String, IDFObject> objs = objLabelKeyMap.get(key);
                return objs.values().stream().map(obj -> obj.getValuesHash()).collect(Collectors.joining()).toString();
            }).collect(Collectors.joining()).toString();*/

            StringBuilder sb = new StringBuilder();
            Set<String> keys = sortedLabelMap.keySet();
            for (String key : keys) {
                TreeMap<String, IDFObject> objs = objLabelKeyMap.get(key);
                if (objs == null) {
                    LOG.debug("Cannot get IDF object for key: "+key);
                    continue;
                }
                for (IDFObject idfObj : objs.values()) {
                    sb.append(idfObj.getValuesHash());
                }
            }
            String concatenateHash = sb.toString();

            fileHash = Hasher.hash(concatenateHash, HashMethod.SHA256);
        }
        return fileHash;
    }

    public String getModelFileContent() {
        buildSortedLabelMap();

        StringBuilder sb = new StringBuilder();
        Set<String> keys = sortedLabelMap.keySet();
        for (String key : keys) {
            TreeMap<String, IDFObject> objs = objLabelKeyMap.get(key);
            if (objs != null) {
                for (IDFObject obj : objs.values()) {
                    sb.append(obj.printStatement(valueCommentPad)).append(Global.UNIVERSAL_LINE_DELIMITER);
                }
            }
        }

        return sb.toString();
    }

    public File getTempFile() {
        return FileUtil.convertStringToFile(getModelFileContent());
    }

    public List<String> getIDFFileContentList() {
        if (sortedLabelMap == null) {
            buildSortedLabelMap();
        }

        List<String> res = new ArrayList<>();
        Set<String> keys = sortedLabelMap.keySet();
        for (String key : keys) {
            TreeMap<String, IDFObject> objs = objLabelKeyMap.get(key);
            for (IDFObject obj : objs.values()) {
                res.add(obj.printStatement(valueCommentPad) + Global.UNIVERSAL_LINE_DELIMITER);
            }
        }

        return res;
    }

    /**
     * Version should be in format like 8.3
     *
     * @return
     */
    public String getVersion() {
        List<IDFObject> versionObjects = objMap.get("version");
        if (versionObjects == null) {
            versionObjects = objMap.get("Version");
        }

        if (versionObjects == null) {
            //LOG.error("No Version Object found!", new IllegalStateException());
            return null;
        }

        IDFObject versionObject = versionObjects.get(0);
        String version = versionObject.getIndexedData(0);

        //pick first two numbers
        String[] split = version.split("\\.");
        return split[0] + "." + split[1];
    }

    public IDFObject getObject(String objKey) {
        if (objKey == null) {
            return null;
        }
        return objKeyMap.get(objKey.toLowerCase());
    }


    public IDFObject getIDFObjectByName(String idfLabel, String name) {
        if (idfLabel == null || name == null) {
            return null;
        }

        List<IDFObject> objs = objMap.get(idfLabel.toLowerCase());
        if (objs == null) {
            LOG.error("Cannot find idf object with label " + idfLabel + ", " + name, new IllegalStateException());
            return null;
        }
        for (IDFObject obj : objs) {
            if (obj.getName().equalsIgnoreCase(name)) {
                return obj;
            }
        }
        LOG.error("Cannot find idf object: " + idfLabel + ", " + name, new IllegalStateException());
        return null;
    }

    public Map<String, List<IDFObject>> getObjectsMap() {
        return this.objMap;
    }

    public int getSize() {
        return this.objMap.size();
    }

    public LinkedHashMap<String, Integer> getSortedLabelMap() {
        if (sortedLabelMap == null) {
            buildSortedLabelMap();
        }

        return sortedLabelMap;
    }

    public int getValueCommentPad() {
        return valueCommentPad;
    }

    public void setValueCommentPad(int valueCommentPad) {
        if (valueCommentPad + 4 > this.valueCommentPad) {
            this.valueCommentPad = valueCommentPad + 4;
        }
    }

    public void invalidSortedLabelMap() {
        this.sortedLabelMap = null;
    }

    public boolean removeIDFObject(IDFObject idfObj) {
        boolean isRemoved = false;

        String label = idfObj.getObjLabel().toLowerCase();

        String key = idfObj.getKey();
        objKeyMap.remove(key);

        if (objMap.containsKey(label)) {
            List<IDFObject> list = objMap.get(label);

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getKey().equals(key)) {
                    list.remove(i);
                    objLabelKeyMap.get(label).remove(key);
                    isRemoved = true;

                    if (list.isEmpty()) {
                        objMap.remove(label);
                        objLabelKeyMap.remove(label);

                        invalidSortedLabelMap();
                    }

                    this.fileHash = null;

                    break;
                }
            }
        }

        return isRemoved;
    }

    public boolean setIDFObject(IDFObject idfObj) {
        String label = idfObj.getObjLabel().toLowerCase();
        String key = idfObj.getKey();

        if (objKeyMap.containsKey(key)) {
            objKeyMap.put(key, idfObj);

            List<IDFObject> list = objMap.get(label);

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getKey().equals(key)) {
                    list.set(i, idfObj);
                    objLabelKeyMap.get(label).put(key, idfObj);
                    break;
                }
            }

            this.fileHash = null;

            return true;
        }

        return false;
    }

    public IDFFileObject deepClone() {
        IDFFileObject idfFileObj = new IDFFileObject();

        Set<String> keys = this.objKeyMap.keySet();
        for (String key : keys) {
            IDFObject obj = this.objKeyMap.get(key);

            key = key.toLowerCase();
            idfFileObj.objKeyMap.put(key, obj.deepClone());
        }

        keys = this.objMap.keySet();
        for (String key : keys) {
            List<IDFObject> list = this.objMap.get(key);

            ArrayList<IDFObject> clone = new ArrayList<>();
            for (IDFObject idfObj : list) {
                clone.add(idfFileObj.objKeyMap.get(idfObj.getKey().toLowerCase()));
            }

            idfFileObj.objMap.put(key, clone);
        }

        keys = this.objLabelKeyMap.keySet();
        for (String key : keys) {
            TreeMap<String, IDFObject> map = this.objLabelKeyMap.get(key);

            TreeMap<String, IDFObject> clone = new TreeMap<>();
            Set<String> mapKeys = map.keySet();
            for (String mapKey : mapKeys) {
                clone.put(mapKey.toLowerCase(), idfFileObj.objKeyMap.get(map.get(mapKey).getKey().toLowerCase()));
            }

            idfFileObj.objLabelKeyMap.put(key, clone);
        }

        idfFileObj.fileHash = this.fileHash;

        if (sortedLabelMap != null) {
            idfFileObj.sortedLabelMap = new LinkedHashMap<>();

            keys = this.sortedLabelMap.keySet();
            for (String key : keys) {
                idfFileObj.sortedLabelMap.put(key, this.sortedLabelMap.get(key));
            }
        }

        idfFileObj.maker = new IDFObjectKeyMaker(getVersion());

        return idfFileObj;
    }

    public Map<String, TreeMap<String, IDFObject>> getLabelKeyMap() {
        return this.objLabelKeyMap;
    }

    public Map<String, IDFObject> getKeyMap() {
        return this.objKeyMap;
    }

    public BranchType getType() {
        return BranchType.idf;
    }

    /**
     * Don't use except for maintenance purpose
     */
    public void maintenance() {
    }
}
