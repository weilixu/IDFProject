package main.java.model.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class MaterialParser {
    private HashMap<String, IDFObject> materials = null;
    private HashMap<String, IDFObject> windows = null;
    private HashMap<String, IDFObject> constructions = null;
    
    private HashMap<String, ArrayList<String>> materialToConstruction = null;
    
    public MaterialParser(IDFFileObject idfFileObj){
        materials = new HashMap<>();
        windows = new HashMap<>();
        constructions = new HashMap<>();
        materialToConstruction = new HashMap<>();
        
        Map<String, List<IDFObject>> bigMap = idfFileObj.getObjectsMap();
        
        Set<String> labels = bigMap.keySet();
        for(String label : labels){
            if(label.startsWith("material") || label.startsWith("Material")){
                List<IDFObject> list = idfFileObj.getCategoryList(label);
                for(IDFObject o : list){
                    materials.put(o.getName(), o);
                }
            }else if(label.startsWith("windowmaterial") || label.startsWith("WindowMaterial")){
                List<IDFObject> list = idfFileObj.getCategoryList(label);
                for(IDFObject o : list){
                    windows.put(o.getName(), o);
                }
            }else if(label.startsWith("construction") || label.startsWith("Construction")){
                List<IDFObject> list = idfFileObj.getCategoryList(label);
                for(IDFObject o : list){
                    constructions.put(o.getName(), o);
                    
                    String[] data = o.getData();
                    String materialName = null;
                    
                    //data[0] is construction name
                    for(int i=1;i<data.length;i++){
                        materialName = data[i];
                        if(!materialToConstruction.containsKey(materialName)){
                            materialToConstruction.put(materialName, new ArrayList<String>());
                        }
                        materialToConstruction.get(materialName).add(data[0]);
                    }
                }
            }
        }
    }
    
    public HashMap<String, IDFObject> getMaterials(){
        return this.materials;
    }
    
    public HashMap<String, IDFObject> getWindows(){
        return this.windows;
    }
    
    public HashMap<String, IDFObject> getConstructions(){
        return this.constructions;
    }
}
