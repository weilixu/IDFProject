package main.java.model.geometry.solar;

import java.time.Year;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class SolarPosition {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
        
    private Integer rawOff;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    
    private Integer year;
    
    private static Map<Integer, String> timeToIDMap;
    
    static {
        timeToIDMap = new HashMap<>();
        setMap();
    }
    
    private static void setMap(){
        timeToIDMap.put(-10, "HST");
        timeToIDMap.put(-9, "AKST");
        timeToIDMap.put(-8, "PST");
        timeToIDMap.put(-7, "MST");
        timeToIDMap.put(-6, "CST");
        timeToIDMap.put(-5, "EST");
        timeToIDMap.put(-4, "AST");
        timeToIDMap.put(0, "GMT");
        timeToIDMap.put(1, "CET");
        timeToIDMap.put(8, "PRC");
    }
    
    public SolarPosition(IDFFileObject file){
        List<IDFObject> sites = file.getCategoryList("Site:Location");
        if(sites==null || sites.isEmpty()){
            //use default values, Chicago location
            latitude = 41.98;
            longitude = -87.92;
            rawOff = -6;
            elevation = 201.0;
        }else {
            IDFObject siteObject = sites.get(0);//there is only one site location
            
            String latStr = siteObject.getDataByStandardComment("Latitude");
            latitude = latStr!=null&&!latStr.isEmpty() ? Double.parseDouble(latStr) : 41.98;
            
            String lonStr = siteObject.getDataByStandardComment("Longitude");
            longitude = lonStr!=null&&!lonStr.isEmpty() ? Double.parseDouble(lonStr) : -87.92;
            
            String rawOffStr = siteObject.getDataByStandardComment("Time Zone");
            rawOff = rawOffStr!=null&&!rawOffStr.isEmpty() ? Double.valueOf(rawOffStr).intValue() : -6;
            
            String eleStr = siteObject.getDataByStandardComment("Elevation");
            elevation = eleStr!=null&&!eleStr.isEmpty() ? Double.parseDouble(eleStr) : 201.0;
        }
        
        List<IDFObject> runPeriods = file.getCategoryList("RunPeriod");
        String yearStr = null;
        if(runPeriods!=null && !runPeriods.isEmpty()){
            IDFObject runPeriod = runPeriods.get(0);//only need the first one
            yearStr = runPeriod.getDataByStandardComment("Start Year");
        }
        if(yearStr==null){
            year = Year.now().getValue();
        }else{
            year = Integer.parseInt(yearStr);
        }
    }
    
    /**
     * year
     * month (1-12)
     * day (1-31)
     * hour (0-23)
     * minute (0-59)
     * second (0-59)
     */
    public JsonArray getSolarPosition(int month, int day){
        JsonArray ja = new JsonArray();
        
        LOG.debug("raw off: "+rawOff+", id: "+timeToIDMap.get(rawOff));
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(rawOff * 60 * 60 * 1000, timeToIDMap.get(rawOff)));
        for(int i=0;i<24;i++){
            time.set(year, month-1, day, i, 0, 0);
            
            AzimuthZenithAngle result = SPA.calculateSolarPosition(time, latitude, longitude, elevation, DeltaT.estimate(time));

            JsonObject jo = new JsonObject();
            jo.addProperty("azimuth", result.getAzimuth());
            jo.addProperty("angle", result.getZenithAngle());
            
            ja.add(jo);
        }
        return ja;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getElevation() {
        return elevation;
    }
}
