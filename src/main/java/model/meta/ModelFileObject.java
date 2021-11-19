package main.java.model.meta;

import main.java.model.idf.IDFObject;
import main.java.model.vc.BranchType;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface ModelFileObject {
    String getFileHash();

    String getVersion();

    BranchType getType();

    LinkedHashMap<String, Integer> getSortedLabelMap();

    TreeMap<String, IDFObject> getCategoryMap(String label);

    int getValueCommentPad();

    List<IDFObject> getCategoryList(String label);

    String getModelFileContent();

    void buildSortedLabelMap();

    ModelFileObject deepClone();

    boolean removeIDFObject(IDFObject idfObj);

    boolean setIDFObject(IDFObject idfObj);

    void setVersion();

    String addObject(IDFObject idfObj);

    void invalidSortedLabelMap();

    IDFObject getObject(String key);

    File getTempFile();

    Map<String, List<IDFObject>> getObjectsMap();
}
