package main.java.model.geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.util.ModelUtil;

public class EnvelopeFromIDFFileObject {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private String idfVersion = null;
    
    public void extractEnvelope(JsonObject jo, IDFFileObject idfFileObj, String type){
        idfVersion = idfFileObj.getVersion();
        
        if(type.equals("hierarchy")){
            generatorHierarchy(jo, idfFileObj);
            if(jo.has("error")){
                return;
            }
            
            generatorMaterialsForHierarchy(jo, idfFileObj);
        }
    }
    
    public void extractEnvelopeFloor(JsonObject jo, IDFFileObject idfFileObj){
        idfVersion = idfFileObj.getVersion();
        
        generatorHierarchy(jo, idfFileObj);
    }
    
    public void extractEnvelopeFloorMaterial(JsonObject jo, IDFFileObject idfFileObj){
        idfVersion = idfFileObj.getVersion();
        
        generatorMaterialsForHierarchy(jo, idfFileObj);
    }
    
    /**
     * <table>
     *     <tr>
     *         <td>jo:</td><td>{buildingName</td><td>zones(zone JO list)</td><td>materials(material JO)}</td>
     *     </tr>
     *     <tr>
     *         <td>zone:</td><td>{zoneName</td><td>surfaces(surface JO list)</td><td>}</td>
     *     </tr>
     *     <tr>
     *         <td>surface:</td><td>{surfaceName</td><td>surfaceConstruction</td><td>fenestrations(fenestration JO list, could be null}</td>
     *     </tr>
     *     <tr>
     *         <td>fenestration:</td><td>{fenestrationName</td><td>fenestrationConstruction</td><td>}</td>
     *     </tr>
     * </table>
     */
    private void generatorHierarchy(JsonObject jo, IDFFileObject idfFileObj){
        GeometryParser gp = new GeometryParser(idfFileObj);
        if(!gp.isParseSuccess()){
            jo.addProperty("error", gp.getParseErrorMsg());
            return;
        }
        
        jo.addProperty("buildingName", gp.getBuilding().getName());
        
        List<String> zoneNames = gp.getZoneNames();
        List<IDFObject> surfaceObjs = null;
        List<IDFObject> fenestrationObjs = null;
        
        JsonObject zones = new JsonObject();
        int zoneIndex = 0;
        for(String zoneName : zoneNames){
            JsonObject zoneJO = new JsonObject();
            zoneJO.addProperty("zoneName", zoneName);
            
            surfaceObjs = gp.getZoneSurfaces(zoneName);
            if(surfaceObjs==null){
                LOG.warn("zone "+zoneName+" have no surfaces");
                continue;
            }
            
            JsonObject surfaces = new JsonObject();
            int surfaceIndex = 0;
            for(IDFObject surfaceObj : surfaceObjs){
                JsonObject surfaceJO = new JsonObject();
                
                String surfaceName = surfaceObj.getName();
                surfaceJO.addProperty("surfaceName", surfaceName);
                surfaceJO.addProperty("surfaceConstruction", surfaceObj.getDataByStandardComment("Construction Name"));
                
                fenestrationObjs = gp.getSurfaceFenestrationNames(surfaceName);
                if(fenestrationObjs!=null && !fenestrationObjs.isEmpty()){
                    JsonObject fenestrations = new JsonObject();
                    
                    int fenestrationIndex=0;
                    for(IDFObject fenestrationObj : fenestrationObjs){
                        JsonObject fenestrationJO = new JsonObject();
                        
                        String fenestrationName = fenestrationObj.getName();
                        fenestrationJO.addProperty("fenestrationName", fenestrationName);
                        fenestrationJO.addProperty("fenestrationConstruction", fenestrationObj.getDataByStandardComment("Construction Name"));
                        
                        fenestrations.add(String.valueOf(fenestrationIndex++), fenestrationJO);
                    }
                    
                    surfaceJO.add("fenestrations", fenestrations);
                }else {
                    surfaceJO.add("fenestrations", null);
                }
                
                surfaces.add(String.valueOf(surfaceIndex++), surfaceJO);
            }
            
            zoneJO.add("surfaces", surfaces);
            zones.add(String.valueOf(zoneIndex++), zoneJO);
        }
        
        jo.add("zones", zones);
    }
    
    /**
     * return jo:
     * <table>
     *     <tr>
     *         <td>jo:</td><td>{materials(material JO list)</td><td>constructs(construct JO list)}</td>
     *     </tr>
     *     <tr>
     *         <td>material:</td><td>{name</td><td>fields(field JO list)}</td>
     *     </tr>
     *     <tr>
     *         <td>construct:</td><td>{name</td><td>fields(field JO list)}</td>
     *     </tr>
     * </table>
     * @return
     */
    private void generatorMaterialsForHierarchy(JsonObject jo, IDFFileObject idfFileObj){
        MaterialParser mp = new MaterialParser(idfFileObj);

        JsonObject materialJo = new JsonObject();
        
        JsonObject jmaterials = new JsonObject();
        
        HashMap<String, IDFObject> materials = mp.getMaterials();
        if(materials!=null && !materials.isEmpty()){
            populateCategoryForHierarchy(jmaterials, materials);
        }
        
        HashMap<String, IDFObject> glazings = mp.getWindows();
        if(glazings!=null && !glazings.isEmpty()){
            populateCategoryForHierarchy(jmaterials, glazings);
        }
        materialJo.add("materials", jmaterials);
        
        //Process construct
        JsonObject jconstruct = new JsonObject();
        HashMap<String, IDFObject> construct = mp.getConstructions();
        if(construct!=null && !construct.isEmpty()){
            populateCategoryForHierarchy(jconstruct, construct);
        }
        materialJo.add("constructs", jconstruct);
        
        jo.add("materials", materialJo);
    }
    
    private void populateCategoryForHierarchy(JsonObject jo, HashMap<String, IDFObject> data){
        Set<String> keys = data.keySet();
        IDFObject stat = null;
        for(String key : keys){
            stat = data.get(key);
            
            JsonObject fieldsJo = ModelUtil.populateToJson(stat, idfVersion);
            
            jo.add(stat.getName(), fieldsJo);
        }
    }
}
