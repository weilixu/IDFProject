package main.java.model.idf;

import main.java.model.idd.EnergyPlusFieldTemplate;
import main.java.model.idd.EnergyPlusObjectTemplate;
import main.java.model.idd.IDDFactory;
import main.java.model.idd.IDDParser;
import main.java.util.HashMethod;
import main.java.util.Hasher;
import main.java.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class IDFObjectKeyMaker implements Serializable {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private IDDParser parser;
    private String version;

    private long autoIncrId = 1;
    
    public IDFObjectKeyMaker(String idfVersion){
        this.parser = IDDFactory.getParser(idfVersion);
        this.version = idfVersion;
    }

    private long getAutoIncrId(){
        return autoIncrId++;
    }

    private String handleEMS(String label){
        String lower = label.toLowerCase();
        if(lower.startsWith("energymanagementsystem")){
            return lower+getAutoIncrId();
        }
        return null;
    }

    /**
     * Not thread safe
     */
    public String makeKey(IDFObject obj){
        String label = obj.getObjLabel();

        String key = handleEMS(label);
        if(!StringUtil.isNullOrEmpty(key)){
            return key;
        }
        
        key = label;
        EnergyPlusObjectTemplate objIDD = parser.getObject(label);
        if(objIDD==null){
            LOG.error("Cannot find "+label+" in "+version+" idd");
            return key.toLowerCase()+getAutoIncrId();
        }
        if(!objIDD.isUniqueObject()){
            key = handleException(label, obj);
            if(key==null){
                EnergyPlusFieldTemplate fieldIDD = objIDD.getFieldTemplateByIndex(0);
                if(fieldIDD.getFieldName().endsWith("Name") && fieldIDD.isRequired()){
                    key = label+"_"+obj.getIndexedData(0);
                }else {
                    key = label+"_"+obj.getValuesHash();
                }
            }
        }
        
        return key.toLowerCase();
    }
    
    private String handleException(String label, IDFObject obj){
        label = label.toLowerCase().trim();
        if(label.startsWith("fluidproperties:")){
            switch(label){
                case "fluidproperties:temperatures":
                    return label+"_"+obj.getIndexedData(0);
                case "fluidproperties:saturated":
                case "fluidproperties:superheated":
                case "fluidproperties:concentration:":
                    return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1)+"_"+obj.getIndexedData(2)+"_"+obj.getIndexedData(3);
            }
        }else if(label.startsWith("lifecyclecost:")) {
        	switch(label) {
	        	case "lifecyclecost:usepriceescalation":
	        		return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1);
	    	}
	    }else if(label.startsWith("output:meter")){
            switch(label){
                case "output:meter":
                case "output:meter:meterfileonly":
                    return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1);
            }
        }else if(label.startsWith("output:")) {
        	switch(label) {
	        	case "output:illuminancemap":
	        		return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1)+"_"+obj.getIndexedData(2);
        	}
        }else if(label.startsWith("roomair:temperaturepattern:")){
        	switch(label) {
	        	case "roomair:temperaturepattern:twogradient":
	        		return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1);
        	}
        }else if(label.startsWith("utilitycost:")){
            switch(label){
	            case "utilitycost:variable":
	            case "utilitycost:computation":
	                return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1);
	            case "utilitycost:charge:simple":
	            case "utilitycost:charge:block":
	            case "utilitycost:qualify":
	                return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1)+"_"+obj.getIndexedData(2);
	            case "utilitycost:ratchet":
	                return label+"_"+obj.getIndexedData(0)+"_"+obj.getIndexedData(1)+"_"+obj.getIndexedData(2)+"_"+obj.getIndexedData(3);
	        }
	    }else if(label.startsWith("zonehvac:")) {
        	switch(label) {
	        	case "equipmentconnections":
	        		return label+"_"+obj.getIndexedData(0);
        	}
        }
        return null;
    }
    
    public static void main(String[] args) {
    	System.out.println(Hasher.hash("123\r\n456", HashMethod.SHA256));
    }
}
