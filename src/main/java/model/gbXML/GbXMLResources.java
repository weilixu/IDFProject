package main.java.model.gbXML;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.config.ServerConfig;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class GbXMLResources {
    
    private Element ASHRAEOARoot;
    private Element spaceMapperRoot;
    private Element internalLoadRoot;
    
    public GbXMLResources(){
        SAXBuilder builder = new SAXBuilder();

        try {
            Document oaDoc = (Document) builder.build(new File(ServerConfig.readProperty("ResourcePath")+"/ashrae62.1oa.xml"));
            Document spaceDoc = (Document)builder.build(new File(ServerConfig.readProperty("ResourcePath") + "/spacemap.xml"));
            Document ilDoc = (Document)builder.build(new File(ServerConfig.readProperty("ResourcePath") + "/internalloads.xml"));
            ASHRAEOARoot = oaDoc.getRootElement();
            spaceMapperRoot = spaceDoc.getRootElement();
            internalLoadRoot = ilDoc.getRootElement();
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, String[]> getOAforSpace(String spaceType){
        
        Map<String, String[]> oaMap = new HashMap<String, String[]>();//oaItem, 1:value, 2 unit
        Element spaceMap = spaceMapperRoot.getChild(spaceType);
        if(spaceMap==null){
            //TODO Warning - spaceType is not valid reset to OfficeEnclosed
            spaceType = "OfficeEnclosed";
            spaceMap = spaceMapperRoot.getChild(spaceType);
        }
        
        //get oa building and oa space
        String building = spaceMap.getChildText("oaBuilding");
        String space = spaceMap.getChildText("oaSpace");
        
        //search the item in the oa document
        List<Element> data = ASHRAEOARoot.getChildren();
        
        for(int i=0; i<data.size(); i++){
            Element d = data.get(i);
            if(d.getAttributeValue("buildingType").equals(building) && d.getAttributeValue("spaceType").equals(space)){
                //OAFlowPerArea
                Element oaArea = d.getChild("OAFlowPerArea");
                oaMap.put("OAFlowPerArea", new String[2]);
                oaMap.get("OAFlowPerArea")[0] = oaArea.getText();
                oaMap.get("OAFlowPerArea")[1] = oaArea.getAttributeValue("unit");
                
                //OAFlowPerPerson                
                Element oaPerson = d.getChild("OAFlowPerPerson");
                oaMap.put("OAFlowPerPerson", new String[2]);
                oaMap.get("OAFlowPerPerson")[0] = oaPerson.getText();
                oaMap.get("OAFlowPerPerson")[1] = oaPerson.getAttributeValue("unit");
                
                //PeopleNumber
                Element people = d.getChild("PeopleNumber");
                oaMap.put("PeopleNumber", new String[2]);
                oaMap.get("PeopleNumber")[0] = people.getText();
                oaMap.get("PeopleNumber")[1] = people.getAttributeValue("unit");
                                
                break;
            }
        }
        return oaMap;
    }
    
    public Map<String, HashMap<String, String[]>> getInternalLoadforSpace(String spaceType){
        HashMap<String, HashMap<String,String[]>> loadMap = new HashMap<String, HashMap<String,String[]>>();//loadItem, 1:value, 2 unit
        Element spaceMap = spaceMapperRoot.getChild(spaceType);
        if(spaceMap==null){
            //TODO Warning - spaceType is not valid reset to OfficeEnclosed
            spaceType = "OfficeEnclosed";
            spaceMap = spaceMapperRoot.getChild(spaceType);
        }
        
        //get oa building and oa space
        String building = spaceMap.getChildText("ilBuilding");
        String space = spaceMap.getChildText("ilSpace");
        String light = spaceMap.getChildText("lightSpace");
        
        List<Element> lightObject = internalLoadRoot.getChildren("light");
        for(int i=0; i<lightObject.size(); i++){
            Element lightEle = lightObject.get(i);
            String spaceTypeAttr = lightEle.getAttributeValue("spaceType");
            if(spaceTypeAttr.equals(light)){
                loadMap.put("LightPowerPerArea", new HashMap<String, String[]>());
                loadMap.get("LightPowerPerArea").put("Electricity", new String[2]);
                loadMap.get("LightPowerPerArea").get("Electricity")[0] = lightEle.getText();
                loadMap.get("LightPowerPerArea").get("Electricity")[1] = lightEle.getAttributeValue("unit");
            }
        }
        
        List<Element> equipObject = internalLoadRoot.getChildren("data");
        for(int j=0; j<equipObject.size(); j++){
            Element equipEle = equipObject.get(j);
            String buildingTypeAttr = equipEle.getAttributeValue("buildingType");
            String spaceTypeAttr = equipEle.getAttributeValue("spaceType");
            if(buildingTypeAttr.equals(building) && spaceTypeAttr.equals(space)){
                loadMap.put("EquipPowerPerArea", new HashMap<String,String[]>());
                
                List<Element> equipmentList = equipEle.getChildren("EquipPowerPerArea");
                for(int k=0; k<equipmentList.size(); k++){
                    Element equip = equipmentList.get(k);
                    String fuelType = equip.getAttributeValue("powerType");
                    
                    loadMap.get("EquipPowerPerArea").put(fuelType, new String[2]);
                    loadMap.get("EquipPowerPerArea").get(fuelType)[0] = equip.getText();
                    loadMap.get("EquipPowerPerArea").get(fuelType)[1] = equip.getAttributeValue("unit");

                }
            }
        }
        return loadMap;
    }
    
}
