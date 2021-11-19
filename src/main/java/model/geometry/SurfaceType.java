package main.java.model.geometry;

import main.java.model.idf.IDFObject;

public enum SurfaceType {
    Floor,
    Ceiling,
    Roof,
    Wall,
    InnerWall,
    Fenestration,
    Shading,
    WinDoor,
    Fin,
    Overhang;
    
    public static SurfaceType getSurfaceType(IDFObject surface){
        if(surface.getObjLabel().equals("FenestrationSurface:Detailed")){
            return Fenestration;
        }
        
        String idfType = surface.getDataByStandardComment("Surface Type");
        String sunExposure = surface.getDataByStandardComment("Sun Exposure");
        String windExposure = surface.getDataByStandardComment("Wind Exposure");
        if(idfType==null){
            return Wall;
        }
        switch(idfType.trim().toLowerCase()){
            case "floor":
                return Floor;
            case "ceiling":
                return Ceiling;
            case "roof":
                return Roof;
            case "wall":
                if((sunExposure!=null && sunExposure.equals("NoSun"))
                        || (windExposure!=null && windExposure.equals("NoWind"))){
                    return InnerWall;
                }
                return Wall;
            default:
                return Fenestration;
        }
    }
}
