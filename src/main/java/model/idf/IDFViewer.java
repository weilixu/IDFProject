package main.java.model.idf;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonObject;

import main.java.model.result.IDFViewerResult;

public class IDFViewer {
    public IDFViewerResult build(File idfFile){
        IDFParser idfParser = new IDFParser();
        IDFFileObject idfObj = new IDFFileObject();
        
        JsonObject jo = idfParser.parseIDFFromIDFFile(idfFile, idfObj);
        if(jo.get("status").getAsString().equals("error")){
            return null;
        }
        
        return build(idfObj);
    }
    
    public IDFViewerResult build(IDFFileObject idfObj){
        int pad = idfObj.getValueCommentPad();
        
        IDFViewerResult res = new IDFViewerResult();
        
        
        
        LinkedHashMap<String, Integer> sortedLabel = idfObj.getSortedLabelMap();
        for(String label : sortedLabel.keySet()){
            TreeMap<String, IDFObject> objs = idfObj.getCategoryMap(label);
            
            for(Entry<String, IDFObject> entry : objs.entrySet()){
                IDFObject obj = entry.getValue();
                
                String[] comments = obj.getTopComments();
                if(comments != null){
                    for(String comment : comments){
                        res.addLine(comment);
                    }
                }
                
                res.addIDFObj(entry.getValue(), pad);
            }
        }
        
        return res;
    }
}
