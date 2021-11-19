package main.java.model.idf.hvacExtract;

public class ZoneEquipment {
    private String zoneName;
    private String equipmentName;
    private String equipmentObjectType;
    private String parentSystemObjectType;
    private String parentSystemName;
    
    private Double coolingSequence;
    private Double heatingSequence;
    
    public ZoneEquipment(String equipmentName){
        this.equipmentName = equipmentName;
    }
    
    public void setParentSystemObjectType(String type){
        parentSystemObjectType = type;
    }
    
    public void setHeatingSequence(String index){
        heatingSequence = Double.parseDouble(index);
    }
    
    public void setCoolingSequence(String index){
        coolingSequence = Double.parseDouble(index);
    }
    
    public void setZoneName(String name){
        zoneName = name;
    }
    
    public void setObjectType(String objType){
        equipmentObjectType = objType;
    }
    
    public void setParentSystemName(String name){
        parentSystemName = name;
    }
    
    public Double getHeatingSequence(){
        return heatingSequence;
    }
    
    public Double getCoolingSequence(){
        return coolingSequence;
    }
    
    public  String getParentSystemObjectType(){
        return parentSystemObjectType;
    }
    
    public String getZoneName(){
        return zoneName;
    }
    
    public String getEquipmentObjectType(){
        return equipmentObjectType;
    }
    
    public String getEquipmentName(){
        return equipmentName;
    }
    
    public String getParentSystemName(){
        return parentSystemName;
    }
    
    @Override
    public String toString(){
        return zoneName;
    }
}
