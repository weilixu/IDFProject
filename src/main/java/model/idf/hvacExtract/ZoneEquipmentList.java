package main.java.model.idf.hvacExtract;

import java.util.ArrayList;

public class ZoneEquipmentList {
    private String name;
    private ArrayList<ZoneEquipment> equipmentMap;
    
    public ZoneEquipmentList(String name){
        this.name = name;
        equipmentMap = new ArrayList<ZoneEquipment>();
    }
    
    public void putEquipment(ZoneEquipment equip){

        equipmentMap.add(equip);
    }
    
    public Integer getSizeOfEquipment(){
        return equipmentMap.size();
    }
    
    public ZoneEquipment getEquipment(int Index){
        return equipmentMap.get(Index);
    }
    
    public String getEquipmentListName(){
        return name;
    }
    
    /**
     * A equipment list represents all the equipment under a zone
     * @return
     */
    public String getZoneName(){
    	return equipmentMap.get(0).getZoneName();
    }
}
