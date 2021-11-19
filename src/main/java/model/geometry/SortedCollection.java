package main.java.model.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import main.java.model.idf.IDFObject;

public class SortedCollection {
    private Map<String, List<Surface>> zoneToSurface;
    private Map<Double, List<String>> zoneLowestPoints;
    private Map<String, IDFObject> surfaces;
    private Map<String, IDFObject> surfaceToZone;
    
    public SortedCollection(Map<String, List<Surface>> zoneSurfaces, 
                            Map<String, Double> zoneLowestPoint, 
                            Map<String, IDFObject> surface,
                            Map<String, IDFObject> surfaceToZone){
        this.zoneToSurface = zoneSurfaces;
        
        Map<Double, List<String>> sortedLayers = new TreeMap<>();
        Set<String> zones = zoneLowestPoint.keySet();
        for(String zone: zones){
            if(sortedLayers.containsKey(zoneLowestPoint.get(zone))){
                sortedLayers.get(zoneLowestPoint.get(zone)).add(zone);
            }
            else{
                List<String> newList = new ArrayList<String>();
                newList.add(zone);
                sortedLayers.put(zoneLowestPoint.get(zone), newList);
            }
        }
        
        this.zoneLowestPoints = sortedLayers;
        this.surfaces = surface;
        this.surfaceToZone = surfaceToZone;
    }
    
    public Map<String, List<Surface>> getZoneToSurface(){
        return this.zoneToSurface;
    }
    
    public Map<Double, List<String>> getZoneLowestPoints(){
        return this.zoneLowestPoints;
    }
    
    public Map<String, IDFObject> getSurfaces(){
        return this.surfaces;
    }
    
    public Map<String, IDFObject> getSurfaceToZone(){
        return this.surfaceToZone;
    }
}
