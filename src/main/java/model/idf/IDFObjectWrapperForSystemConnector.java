package main.java.model.idf;

import java.io.Serializable;
import java.util.ArrayList;

import main.java.model.idd.EnergyPlusObjectTemplate;

public class IDFObjectWrapperForSystemConnector implements Serializable{
	private static final long serialVersionUID = 3468989980104469088L;
	private IDFObject originalObj;
	private EnergyPlusObjectTemplate objTemplate;
	
	//1. true - keep, 2. false - discard
	private Boolean[] fieldLabel;
	
	//new field names
	private ArrayList<String> newFieldList;
	
	public IDFObjectWrapperForSystemConnector(IDFObject object, EnergyPlusObjectTemplate iddObject){
		originalObj = object;
		objTemplate = iddObject;
		fieldLabel = new Boolean[originalObj.getObjLen()-1];
		newFieldList = new ArrayList<String>();
	}
	
	public IDFObject getOriginalObject(){
		return originalObj;
	}
	
	public String getObjectName(){
		return originalObj.getName();
	}
	
	public String getObjectLabel(){
		return originalObj.getObjLabel();
	}
	
	public void setFieldLabel(int index, Boolean flag){
		fieldLabel[index] = flag;
	}
	
	public void insertNewField(String newField){
		newFieldList.add(newField);
	}
	
	public int getObjLen(){
		return originalObj.getObjLen();
	}
	
	public String getIndexedData(int index){
		return originalObj.getIndexedData(index);
	}
	
	/**
	 * Pre-condition: the remain one exists at begining and end, there
	 * are no remaining in the middle of an object
	 * @return
	 */
	public IDFObject getModifiedIDFObject(){
		ArrayList<String> lines = new ArrayList<String>(); 
        ArrayList<String> units = new ArrayList<String>(); 
        ArrayList<String> comments = new ArrayList<String>(); 
        ArrayList<String> topComments = new ArrayList<String>();
        //add object label
        boolean flag = false;
        lines.add(originalObj.getObjLabel());
        units.add("");
        comments.add("");
                
        int counter = 0;
        for(int i=0; i<originalObj.getObjLen()-1; i++){
        	if(fieldLabel[i]){
        		lines.add(originalObj.getIndexedData(i));
        		units.add(objTemplate.getFieldTemplateByIndex(counter).getUnit());
        		comments.add(objTemplate.getFieldTemplateByIndex(counter).getFieldName());
        		counter ++;//increase the counter
        	}else if(!flag){
        		//insert all the rest
        		for(int j=0; j<newFieldList.size(); j++){
        			lines.add(newFieldList.get(j));
        			units.add(objTemplate.getFieldTemplateByIndex(counter).getUnit());
        			comments.add(objTemplate.getFieldTemplateByIndex(counter).getFieldName());
            		counter++;
        		}
        		flag = true;//indicates the new branches have been inserted.
        	}
        }
		return new IDFObject(lines, units, comments, topComments);
	}
}
