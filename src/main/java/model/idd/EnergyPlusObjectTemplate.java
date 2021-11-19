package main.java.model.idd;

import main.java.model.idf.IDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class EnergyPlusObjectTemplate implements Serializable{
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    /**
     * 
     */
    private static final long serialVersionUID = -2045243342890828089L;
    //object type element
    private static final String MEMOTOKEN = "\\memo";
    private static final String UNIQUETOKEN = "\\unique-object";
    private static final String REQUIREDTOKEN = "\\required-object";
    private static final String MINFIELDSTOKEN = "\\min-fields";
    private static final String OBSOLETETOKEN = "\\obsolete";
    private static final String EXTENSIBLETOKEN = "\\extensible";
    private static final String FORMATTOKEN = "\\format";
    private static final String REFCLASSNAME = "\\reference-class-name";
    
    ArrayList<EnergyPlusFieldTemplate> fieldList;
    HashMap<String, Integer> fieldNameMap; //ease for search
    HashMap<String, Integer> fieldIDMap;
    
    private String name;
    
    private int numOfFields = 0;
    
    private int numOfMinField=-1;
    private StringBuffer memo;
    
    private boolean uniqueObject = false;
    private boolean requiredObject = false;
    private boolean obsolete = false;
    private boolean extensible = false;
    private int beginExtensible = -1;
    private int numOfExtensible = -1;
    private String format;
    
    public EnergyPlusObjectTemplate(String name){
        this.name = name;
        //two exceptions
        if(this.name.equals("MaterialProperty:GlazingSpectralData")) {
        		beginExtensible = 1;
        		numOfExtensible = 4;
        }else if(this.name.equals("Schedule:Day:List")) {
        		beginExtensible = 4;
        		numOfExtensible = 1;
        } else if(this.name.equals("Table:MultiVariableLookup")) {
        		//numOfExtensible = 1;
        }
        fieldList = new ArrayList<>();
        fieldNameMap = new HashMap<>();
        fieldIDMap = new HashMap<>();
        memo = new StringBuffer();
    }
    
    /**
     * example of ID: "A1" or "N1"
     * @param ID
     * @return
     */
    public EnergyPlusFieldTemplate getFieldTemplateByID(String ID){
        return fieldList.get(fieldIDMap.get(ID));
    }
    
    public EnergyPlusFieldTemplate getFieldTemplateByName(String name){
        if(name==null){
            return null;
        }

        Integer idx = fieldNameMap.get(name.toLowerCase());
        if(idx==null || idx<0 || idx>=fieldList.size()){
            return null;
        }
        return fieldList.get(idx);
    }
    
    public EnergyPlusFieldTemplate getFieldTemplateByIndex(int index){
    		int newIndex = index;
        if(beginExtensible > -1 && index > beginExtensible){
        		if(name.equals("Table:MultiVariableLookup")) {
        			newIndex = 34;//exception for table
        		}else if(numOfExtensible==1 && index >= fieldList.size()) {
                //assume it is repeating the last field
                	newIndex = fieldList.size()-1;
            }else {
                newIndex = beginExtensible + (index - beginExtensible)%numOfExtensible;
            }
        }
        if(newIndex >= fieldList.size()){
        	//LOG.warn(beginExtensible + " " + numOfExtensible + " " + index + " " + newIndex);
        	//System.out.println(name);
        	for(int i=0; i<fieldList.size(); i++){
                //LOG.warn(fieldList.get(i).getFieldName());
        	}
        	return null;
        }
        
        return fieldList.get(newIndex);
    }
    
    //getter methods
    public String getObjectName(){
        return name;
    }
    
    public boolean isUniqueObject(){
        return uniqueObject;
    }
    
    public boolean isRequiredObject(){
        return requiredObject;
    }
    
    public boolean isObsolete(){
        return obsolete;
    }
    
    public boolean isExtensible(){
        return extensible;
    }
    
    public int numOfExtensibles(){
        return numOfExtensible;
    }
    
    public String getFormat(){
        return format;
    }
    
    public String getMemo(){
        return memo.toString();
    }
    
    /**
     * Indicates the number of minimum fields for this object
     * @return
     */
    public int getNumberOfMinFields(){
        return numOfMinField;
    }
    
    public int getNumberOfFields(){
        return numOfFields;
    }
    
    public void processElement(String line){
        if(line.contains(MEMOTOKEN)){
            memo.append(getContentFromLine(line,MEMOTOKEN));
        }else if(line.contains(UNIQUETOKEN)){
            uniqueObject = true;
        }else if(line.contains(REQUIREDTOKEN)){
            requiredObject = true;
        }else if(line.contains(MINFIELDSTOKEN)){
            numOfMinField = Integer.parseInt(getContentFromLine(line, MINFIELDSTOKEN));
        }else if(line.contains(OBSOLETETOKEN)){
            obsolete = true;
        }else if(line.contains(EXTENSIBLETOKEN)){
            extensible = true;
            
            //get the number of extensible variables
            StringBuffer sb = new StringBuffer();
            int index = line.indexOf(":")+1;
            while(index < line.length() && line.charAt(index)!=' '){
                sb.append(line.charAt(index));
                index++;
            }

            numOfExtensible = Integer.parseInt(sb.toString());
        }else if(line.contains(FORMATTOKEN)){
            format = getContentFromLine(line, FORMATTOKEN);
        }else if(line.contains(REFCLASSNAME)){
            //so far no reference class name element
        }
    }
    
    //record the beginning of extensible field sets
    public void setTheBeginningOfExtensible(){
        beginExtensible = numOfFields - 1;
    }
    
    /**
     * 
     * @return -1 no extensible, >0 yes
     */
    public Integer getBeginningOfExtensible(){
        return beginExtensible;
    }
    
    public void addObjectField(EnergyPlusFieldTemplate temp){
        //System.out.println("Field Name: " + temp.getFieldName());
        
        temp.setFieldIdx(numOfFields);
        temp.setObjName(this.getObjectName());
        fieldList.add(temp);
        fieldNameMap.put(temp.getFieldName().toLowerCase(), numOfFields);//recording the location
        fieldIDMap.put(temp.getFieldType()+temp.getFieldNumber(), numOfFields);//pointer to the other field within this object
        numOfFields++;
    }
    
    private String getContentFromLine(String line, String token){
        if(line.length() == token.length()){
            //in case empty comments in a token
            return "";
        }
        return line.substring((line.indexOf(token) + token.length())+1, line.length());
    }

    public int getPositionOfFieldName(String name){
        for(EnergyPlusFieldTemplate fieldTemplate : fieldList){
            String fieldName = fieldTemplate.getFieldName();
            if(fieldName.equalsIgnoreCase(name)){
                return fieldTemplate.getFieldIdx()+1;
            }
        }

        return -1;
    }

    public IDFObject buildEmptyObject(int len){
        IDFObject object = new IDFObject(name, len);

        int idx=0;
        for(EnergyPlusFieldTemplate fieldTemplate : fieldList){
            if(idx==len){
                break;
            }

            object.setIndexedStandardComment(fieldTemplate.getFieldIdx(), fieldTemplate.getFieldName());
            object.setOriginalCommentNoUnit(fieldTemplate.getFieldIdx(), fieldTemplate.getFieldName());
        }

        return object;
    }
}
