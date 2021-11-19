package main.java.model.idf;

import main.java.util.Global;
import main.java.util.HashMethod;
import main.java.util.Hasher;
import main.java.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class IDFObject implements Serializable {
    private static final long serialVersionUID = 5009729187189221805L;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    /**
     * NOTE: This class has deepClone() function, any member change should change deepClone() accordingly
     */

    private String objLabel; //category
    private String[] values = null;
    private String[] units = null;
    private String[] commentsNoUnit = null;
    private String[] standardComments = null;
    private String[] topComments = null;

    private String objName = null; //usual the second field
    private String objKey = null;  //distinguish objects with the same label
    private String valuesHash = "";  //for comparison purpose

    private int maxLineLen = 0;
    private int objLen = 0;

    // for IDD validation
    private String label;
    private String labelMsg;

    //W.X. 9/9/2017 for data processing purpose - label this object with some identification
    //e.g. zone name that this object connect to, or other object this object connects to
    private String objectIdentifier;

    private Map<String, String> extraInfoMap = null;

    public IDFObject(ArrayList<String> lines,
                     ArrayList<String> units,
                     ArrayList<String> comments,
                     ArrayList<String> topComments) {
        if (topComments != null && !topComments.isEmpty()) {
            this.topComments = topComments.toArray(new String[0]);
        }

        if (lines == null) {
            //special object contains end of file comments
            objLabel = "EOFComments";
            objName = objLabel;
            objKey = objLabel;
            return;
        }

        this.objLabel = lines.get(0);
        if (objLabel.equals("Version") && this.topComments == null) {
            //padding empty comment, to correctly show Version object
            this.topComments = new String[]{"!"};
        }

        objLen = lines.size();
        if (objLen > 1) {
            this.values = new String[objLen - 1];
            this.units = new String[objLen - 1];
            this.commentsNoUnit = new String[objLen - 1];
            this.standardComments = new String[objLen - 1];

            for (int i = 1; i < objLen; i++) {
                this.values[i - 1] = lines.get(i).trim();

                if (this.values[i - 1].length() > maxLineLen) {
                    maxLineLen = this.values[i - 1].length();
                }

                this.units[i - 1] = units.get(i);
                this.commentsNoUnit[i - 1] = comments.get(i);

                if (this.units[i - 1] != null) {
                    this.units[i - 1] = this.units[i - 1].trim();
                }
                if (this.commentsNoUnit[i - 1] != null) {
                    this.commentsNoUnit[i - 1] = this.commentsNoUnit[i - 1].trim();
                }
            }

            objName = values[0];
            objKey = objLabel;

            updateValuesHash();
        }
    }

    /**
     * Content length includes label
     */
    public IDFObject(String label, int contentLen) {
        this.objLabel = label;

        this.values = new String[contentLen - 1];
        Arrays.fill(values, "");

        this.units = new String[contentLen - 1];
        this.commentsNoUnit = new String[contentLen - 1];
        this.standardComments = new String[contentLen - 1];

        this.objLen = contentLen;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabelMsg() {
        return labelMsg;
    }

    public void setLabelMsg(String labelMsg) {
        this.labelMsg = labelMsg;
    }


    public String getLine(int i, int pad, boolean isHTML) {
        if (i < 0 || i >= objLen) {
            return null;
        }

        if (i == 0) {
            return objLabel + ",";
        }

        String data = this.values[i - 1];
        String unit = this.units[i - 1];
        String comment = this.commentsNoUnit[i - 1];

        if (comment != null && !comment.isEmpty()) {
            int padNeed = pad - data.length();

            if (isHTML) {
                comment = StringUtil.spacesHTML(padNeed) + "!- " + comment;
            } else {
                comment = StringUtil.spaces(padNeed) + "!- " + comment;
            }

            if (unit != null && !unit.isEmpty()) {
                comment += " {" + unit + "}";
            }

        } else {
            comment = "";
        }

        String valueEnd = ",";
        if (i == objLen - 1) {
            valueEnd = ";";
        }

        return StringUtil.spaces(Global.IDF_DISPLAY_PADDING_OBJ_FIELD) + data + valueEnd + comment;
    }

    public String printStatement(int pad) {
        String lineDelimiter = Global.UNIVERSAL_LINE_DELIMITER;

        StringBuilder sb = new StringBuilder();

        if (topComments != null) {
            Arrays.stream(topComments).forEach(e -> sb.append(e).append(lineDelimiter));
        }

        if (values == null) {
            return sb.toString();
        }

        for (int i = 0; i < objLen; i++) {
            sb.append(getLine(i, pad, false)).append(lineDelimiter);
        }

        return sb.toString();
    }


    public String getObjLabel() {
        return this.objLabel;
    }

    public void setObjLabel(String label) {
        this.objLabel = label;
    }

    public String[] getData() {
        return this.values;
    }

    public String[] getUnit() {
        return this.units;
    }

    public String getIndexedUnit(int idx) {
        if (units == null || idx < 0 || idx >= units.length) {
            return null;
        }
        return units[idx].trim();
    }

    public String[] getStandardComments() {
        return this.standardComments;
    }

    public String getIndexedStandardComment(int idx) {
        if (standardComments == null || idx < 0 || idx >= standardComments.length) {
            return null;
        }
        return standardComments[idx].trim();
    }

    public void setIndexedStandardComment(int idx, String standardComment) {
        if (standardComments == null || idx < 0 || idx >= standardComments.length) {
            return;
        }
        standardComments[idx] = standardComment;
    }

    public void clearStandardComments() {
        this.standardComments = new String[this.commentsNoUnit.length];
    }

    public String getName() {
        return this.objName;
    }

    public void setName(String name) {
        this.objName = name;
    }

    public String getObjIdentifier() {
        return objectIdentifier;
    }

    public void setObjIdentifier(String identifier) {
        this.objectIdentifier = identifier;
    }

    public String getKey() {
        return this.objKey;
    }

    public void updateKey(String key) {
        this.objKey = key;
    }

    public int getTopCommentsLen() {
        return topComments == null ? 0 : topComments.length;
    }

    public String[] getTopComments() {
        return this.topComments;
    }

    public void setTopComments(String[] topComments) {
        this.topComments = topComments;
    }

    public int getObjLen() {
        return this.objLen;
    }

    public int getMaxLineLen() {
        return this.maxLineLen;
    }

    /**
     * From Name, index starts from 0
     */
    public String getIndexedData(int i) {
        if (values == null || i < 0 || i >= values.length) {
            return null;
        }
        return values[i].trim();
    }

    public int getIndexOfStandardComment(String standardComment) {
        int idx = -1;
        for (int i = 0; i < standardComments.length; i++) {
            if (standardComments[i] != null && standardComments[i].equalsIgnoreCase(standardComment)) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    public String getDataByStandardComment(String standardComment) {
        int idx = getIndexOfStandardComment(standardComment);
        if (idx > -1) {
            return values[idx];
        } else {
            /*LOG.error("Get data by standard comment cannot find value, label:"+objLabel+" name:"+objName+", comment:"+standardComment,
            		new IllegalStateException());
            LOG.warn(this.printStatement(1));*/
            LOG.error("Get data by standard comment cannot find value, label:" + objLabel + " name:" + objName + ", comment:" + standardComment);
            return null;
        }
    }

    public boolean setDataByStandardComment(String standardComment, String data) {
        int idx = getIndexOfStandardComment(standardComment);
        if (idx > -1) {
            values[idx] = data;
            return true;
        } else {
            LOG.warn("Set data by standard comment cannot find value, label:" + objLabel + " name:" + objName + ", comment:" + standardComment);
            return false;
        }
    }

    public String getUnitByStandardComment(String standardComment) {
        for (int i = 0; i < standardComments.length; i++) {
            if (standardComments[i] != null && standardComments[i].equalsIgnoreCase(standardComment)) {
                return units[i] == null ? "" : units[i];
            }
        }
        LOG.warn("Get unit by standard comment cannot find value, label:" + objLabel + ", comment:" + standardComment);
        return null;
    }

    public void setIndexedData(int index, String value) {
        if (values != null && index >= 0 && index < values.length) {
            values[index] = value;

            if (index == 0) {
                this.setName(value);
            }

            updateValuesHash();
        }
    }

    public void setIndexedData(int index, String value, String unit) {
        if (values != null && index >= 0 && index < values.length) {
            values[index] = value;
            units[index] = unit;
            if (index == 0) {
                this.setName(value);
            }

            updateValuesHash();
        }
    }

    public void setOriginalCommentNoUnit(int index, String comment) {
        if (values != null && index > 0 && index < values.length) {
            commentsNoUnit[index] = comment;

            updateValuesHash();
        }
    }

    public String getOriginalCommentNoUnit(int index) {
        if (commentsNoUnit != null && index > 0 && index < commentsNoUnit.length) {
            return commentsNoUnit[index];
        }
        return null;
    }

    public void setOriginalCommentWithUnit(int index, String comment, String unit) {
        if (values != null && index >= 0 && index < values.length) {
            commentsNoUnit[index] = comment;
            units[index] = unit;

            updateValuesHash();
        }
    }

    private void updateValuesHash() {
        if (values != null) {
            this.valuesHash = Hasher.hash(this.getValues(), HashMethod.SHA256);
        } else {
            this.valuesHash = "";
        }
    }

    public String getValues() {
        return Arrays.stream(values).map(e -> e != null ? e.trim() : "null").collect(Collectors.joining("\r\n"));
    }

    public String getValuesHash() {
        return this.valuesHash;
    }

    public int getStandardCommentIndex(String standardComment) {
        for (int i = 0; i < objLen - 1; i++) {
            if (standardComment.equals(standardComments[i])) {
                return i;
            }
        }
        return -1;
    }

    public IDFObject deepClone() {
        IDFObject idfObj = new IDFObject(this.objLabel, this.objLen);
        idfObj.values = Arrays.copyOf(this.values, this.values.length);
        idfObj.units = Arrays.copyOf(this.units, this.units.length);
        idfObj.commentsNoUnit = Arrays.copyOf(this.commentsNoUnit, this.commentsNoUnit.length);
        idfObj.standardComments = Arrays.copyOf(this.standardComments, this.standardComments.length);

        if (topComments != null) {
            idfObj.topComments = Arrays.copyOf(this.topComments, this.topComments.length);
        }

        idfObj.objName = this.objName;
        idfObj.objKey = this.objKey;
        idfObj.valuesHash = this.valuesHash;

        idfObj.maxLineLen = this.maxLineLen;

        idfObj.label = this.label;
        idfObj.labelMsg = this.labelMsg;

        if (extraInfoMap != null) {
            idfObj.extraInfoMap = new HashMap<>();
            idfObj.extraInfoMap.putAll(extraInfoMap);
        }

        return idfObj;
    }

    public boolean endsWith(String string) {
        // TODO Auto-generated method stub
        return false;
    }

    public void trimTo(int len) {
        if (objLen > len) {
            this.objLen = len;

            // TODO remove data
        }
    }

    /**
     * WARNING: not multi thread safe
     */
    public void writeKeyedExtraInfo(String key, String value) {
        if (this.extraInfoMap == null) {
            this.extraInfoMap = new HashMap<>();
        }

        this.extraInfoMap.put(key, value);
    }

    /**
     * WARNING: not multi thread safe
     */
    public String getKeyedExtraInfo(String key) {
        if (this.extraInfoMap == null) {
            return null;
        }

        return this.extraInfoMap.get(key);
    }
}
