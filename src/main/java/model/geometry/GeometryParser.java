package main.java.model.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import main.java.model.geometry.scale.Coordinate3D;
import main.java.model.geometry.scale.ScaleUtility;
import main.java.util.NumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class GeometryParser {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private IDFObject building = null;
    
    private Map<String, IDFObject> zoneMap = null;
    private Map<String, IDFObject> surfaceMap = null;
    private Map<String, IDFObject> fenestrationMap = null;
    private Map<String, Surface> surfaces = null;
    
    private Map<String, IDFObject> plenumMap = null;
    
    private Map<String, List<IDFObject>> zoneToSurface = null;
    private Map<String, List<Surface>> zoneSurfaces = null;
    private Map<String, IDFObject> surfaceToZone = null;
    
    private Map<String, List<IDFObject>> surfaceToFenestration = null;
    private Map<String, IDFObject> fenestrationToSurface = null;
    
    private Map<String, Integer> multiplierZones = null;
    
    private Map<String, Double> zoneFloorHeight = null;
    private Map<Integer, List<String>> heightZones = null;
    
    private List<Surface> surroundShading = null;

    private Map<String, List<IDFObject>> feneOverhangMap;
    private Map<String, List<IDFObject>> feneFinMap;
    
    private boolean parseSuccess = true;
    private String parseErrMsg = "";
    
    //private boolean hasHVACSortList = false;
    //private HashMap<String, ArrayList<String>> ohuMap = null;
    //private HashMap<String, ArrayList<String>> vrfMap = null;
    
    public GeometryParser(IDFFileObject idfFileObj){
        plenumMap = new HashMap<>();
        multiplierZones = new HashMap<>();
        surfaces = new HashMap<>();
        
        //extract building
        List<IDFObject> buildings = idfFileObj.getCategoryList("Building");
        if(buildings!=null && !buildings.isEmpty()){
            building = buildings.get(0);
        }

        // extract fins and overhangs
        feneOverhangMap = new HashMap<>();
        List<IDFObject> overhangs = idfFileObj.getCategoryList("Shading:Overhang");
        if(overhangs!=null){
            for(IDFObject overhang : overhangs){
                String feneName = overhang.getDataByStandardComment("Window or Door Name");
                if(!feneOverhangMap.containsKey(feneName)){
                    feneOverhangMap.put(feneName, new ArrayList<>());
                }
                feneOverhangMap.get(feneName).add(overhang);
            }
        }
        overhangs = idfFileObj.getCategoryList("Shading:Overhang:Projection");
        if(overhangs!=null){
            for(IDFObject overhang : overhangs){
                String feneName = overhang.getDataByStandardComment("Window or Door Name");
                if(!feneOverhangMap.containsKey(feneName)){
                    feneOverhangMap.put(feneName, new ArrayList<>());
                }
                feneOverhangMap.get(feneName).add(overhang);
            }
        }

        feneFinMap = new HashMap<>();
        List<IDFObject> fins = idfFileObj.getCategoryList("Shading:Fin");
        if(fins!=null){
            for(IDFObject fin : fins){
                String feneName = fin.getDataByStandardComment("Window or Door Name");
                if(!feneFinMap.containsKey(feneName)){
                    feneFinMap.put(feneName, new ArrayList<>());
                }
                feneFinMap.get(feneName).add(fin);
            }
        }
        fins = idfFileObj.getCategoryList("Shading:Fin:Projection");
        if(fins!=null){
            for(IDFObject fin : fins){
                String feneName = fin.getDataByStandardComment("Window or Door Name");
                if(!feneFinMap.containsKey(feneName)){
                    feneFinMap.put(feneName, new ArrayList<>());
                }
                feneFinMap.get(feneName).add(fin);
            }
        }
        
        //extract zones
        zoneMap = new HashMap<>();
        List<IDFObject> zones = idfFileObj.getCategoryList("Zone");
        if(zones==null || zones.isEmpty()){
            parseSuccess = false;
            parseErrMsg = "Missing Zone Objects";
            return;
        }
        for(IDFObject zone : zones){
            String zoneName = zone.getName();
            if(!zoneName.toLowerCase().contains("plenum")){
                zoneMap.put(zone.getName(), zone);
            }else {
                plenumMap.put(zoneName, zone);
            }
            
            String multiplierStr = zone.getDataByStandardComment("Multiplier");
            int multiplier = multiplierStr==null||multiplierStr.isEmpty() ? 1 : Double.valueOf(multiplierStr.trim()).intValue();
            
            if(multiplier!=1){
                multiplierZones.put(zoneName, multiplier);
            }
        }
        
        //extract building surfaces
        surfaceMap = new HashMap<>();
        zoneToSurface = new HashMap<>();
        zoneSurfaces = new HashMap<>();
        surfaceToZone = new HashMap<>();
        zoneFloorHeight = new HashMap<>();
        List<IDFObject> surfaces = idfFileObj.getCategoryList("BuildingSurface:Detailed");
        if(surfaces==null || surfaces.isEmpty()){
            parseSuccess = false;
            parseErrMsg = "Missing BuildingSurface:Detailed Objects";
            return;
        }
        for(IDFObject surfaceObj : surfaces){
            String surfaceName = surfaceObj.getName();
            if(surfaceName.toLowerCase().contains("plenum")){
                plenumMap.put(surfaceName, surfaceObj);
            }else {
                IDFObject zoneObj = getZoneObj(surfaceObj);
                if(zoneObj==null){
                    // this surface is not belong to existing zone, ignore
                    continue;
                }

                String zoneName = surfaceObj.getDataByStandardComment("Zone Name");
                surfaceMap.put(surfaceName, surfaceObj);
                
                if(!zoneToSurface.containsKey(zoneName)){
                    zoneToSurface.put(zoneName, new ArrayList<>());
                }                
                zoneToSurface.get(zoneName).add(surfaceObj);
                surfaceToZone.put(surfaceName, zoneObj);
                
                Surface sur = new Surface(surfaceObj, zoneObj);
                this.surfaces.put(surfaceName, sur);
                
                if(!zoneFloorHeight.containsKey(zoneName) 
                        || zoneFloorHeight.get(zoneName)>sur.getLowestPoint()){
                    zoneFloorHeight.put(zoneName, sur.getLowestPoint());
                }
                
                if(!zoneSurfaces.containsKey(zoneName)){
                    zoneSurfaces.put(zoneName, new ArrayList<>());
                }                
                zoneSurfaces.get(zoneName).add(sur);
            }
        }
        
        // height to zone list
        heightZones = new TreeMap<>();
        for(String zoneName : zoneFloorHeight.keySet()){
            int height = zoneFloorHeight.get(zoneName).intValue();
            if(!heightZones.containsKey(height)){
                heightZones.put(height, new ArrayList<>());
            }
            heightZones.get(height).add(zoneName);
        }
        
        //extract fenestration
        fenestrationMap = new HashMap<>();
        surfaceToFenestration = new HashMap<>();
        fenestrationToSurface = new HashMap<>();
        List<IDFObject> fenestrations = idfFileObj.getCategoryList("FenestrationSurface:Detailed");
        if(fenestrations!=null){
            for(IDFObject fenestrationObj : fenestrations){
                String fenestrationName = fenestrationObj.getName();
                String surfaceName = fenestrationObj.getDataByStandardComment("Building Surface Name");
                
                if(surfaceName==null){
                    continue;
                }
                
                fenestrationMap.put(fenestrationName, fenestrationObj);
                
                if(!surfaceToFenestration.containsKey(surfaceName)){
                    surfaceToFenestration.put(surfaceName, new ArrayList<>());
                }
                
                surfaceToFenestration.get(surfaceName).add(fenestrationObj);
                
                IDFObject surfaceObj = surfaceMap.get(surfaceName);
                if(surfaceObj==null){
                    LOG.warn("Fenestration's parent surface not found, fenestration: "+fenestrationName+", parent surface: "+surfaceName);
                    continue;
                }
                
                fenestrationToSurface.put(fenestrationName, surfaceObj);
                
                IDFObject zoneObj = getZoneObj(surfaceObj);
                
                Surface fene = new Surface(fenestrationObj, zoneObj);
                Surface parentSurface = this.surfaces.get(surfaceName);
                parentSurface.addHole(fene);
                
                zoneSurfaces.get(surfaceToZone.get(surfaceName).getName()).add(fene);

                List<IDFObject> feneOverhangs = feneOverhangMap.get(fenestrationName);
                if(feneOverhangs!=null){
                    for(IDFObject overhang : feneOverhangs){
                        Surface overhangSurface = buildOverhang(overhang, fene, idfFileObj);
                        if(overhangSurface!=null) {
                            zoneSurfaces.get(surfaceToZone.get(surfaceName).getName()).add(overhangSurface);
                        }
                    }
                }

                List<IDFObject> feneFins = feneFinMap.get(fenestrationName);
                if(feneFins!=null){
                    for(IDFObject fin : feneFins){
                        Surface[] finSurfaces = buildFins(fin, fene, idfFileObj);
                        if(finSurfaces!=null){
                            zoneSurfaces.get(surfaceToZone.get(surfaceName).getName()).add(finSurfaces[0]);
                            zoneSurfaces.get(surfaceToZone.get(surfaceName).getName()).add(finSurfaces[1]);
                        }
                    }
                }
            }
        }

        // extra Window and Door for older E+ versions
        List<IDFObject> window = idfFileObj.getCategoryList("Window");
        List<IDFObject> door = idfFileObj.getCategoryList("Door");
        if(window!=null || door!=null){
            List<IDFObject> windowDoor = new ArrayList<>();
            if(window!=null){
                windowDoor.addAll(window);
            }
            if(door!=null){
                windowDoor.addAll(door);
            }

            for(IDFObject winDoorObj : windowDoor) {
                String winDoorName = winDoorObj.getName();
                String surfaceName = winDoorObj.getDataByStandardComment("Building Surface Name");

                if (surfaceName == null) {
                    continue;
                }

                fenestrationMap.put(winDoorName, winDoorObj);

                if (!surfaceToFenestration.containsKey(surfaceName)) {
                    surfaceToFenestration.put(surfaceName, new ArrayList<>());
                }

                surfaceToFenestration.get(surfaceName).add(winDoorObj);

                IDFObject surfaceObj = surfaceMap.get(surfaceName);
                if (surfaceObj == null) {
                    LOG.warn("Winodw/Door's parent surface not found, winDoor: " + winDoorName + ", parent surface: " + surfaceName);
                    continue;
                }

                fenestrationToSurface.put(winDoorName, surfaceObj);

                Surface parentSurface = this.surfaces.get(surfaceName);
                Surface winDoor = new Surface(winDoorObj, parentSurface);
                parentSurface.addHole(winDoor);

                zoneSurfaces.get(surfaceToZone.get(surfaceName).getName()).add(winDoor);
            }
        }
        
        //ohuMap = new HashMap<>();
        //vrfMap = new HashMap<>();
        
        //extract Shadings
        surroundShading = new ArrayList<>();
        List<IDFObject> shadings = idfFileObj.getCategoryList("Shading:Building:Detailed");
        if(shadings!=null){
            for(IDFObject shadingObj : shadings){
                Surface sur = new Surface(shadingObj);
                surroundShading.add(sur);
            }
        }
        
        shadings = idfFileObj.getCategoryList("Shading:Site:Detailed");
        if(shadings!=null){
            for(IDFObject shadingObj : shadings){
                Surface sur = new Surface(shadingObj);
                surroundShading.add(sur);
            }
        }
    }

    private Surface[] buildFins(IDFObject finIDF, Surface fene, IDFFileObject idfFileObject){
        List<Coordinate3D> posPoints = fene.getPositionedCoordinatesByHeight(idfFileObject);
        if(posPoints==null){
            return null;
        }

        Coordinate3D upperLeft = posPoints.get(0);
        Coordinate3D upperRight = posPoints.get(1);
        Coordinate3D lowerLeft = posPoints.get(2);
        Coordinate3D lowerRight = posPoints.get(3);

        Coordinate3D feneNormal = fene.getNormalVector(idfFileObject);

        double leftExt = NumUtil.readDouble(finIDF.getDataByStandardComment("Left Extension from Window/Door"), 0);
        if(leftExt>0){
            Coordinate3D leftUL = ScaleUtility.makeVector(upperLeft, lowerLeft);
            Coordinate3D leftExtVector = ScaleUtility.cross(leftUL, feneNormal);
            ScaleUtility.normalize(leftExtVector);
            leftExtVector.scale(leftExt);

            upperLeft.add(leftExtVector);
            lowerLeft.add(leftExtVector);
        }

        double rightExt = NumUtil.readDouble(finIDF.getDataByStandardComment("Right Extension from Window/Door"), 0);
        if(rightExt>0){
            Coordinate3D rightUL = ScaleUtility.makeVector(upperRight, lowerRight);
            Coordinate3D rightExtVector = ScaleUtility.cross(feneNormal, rightUL);
            ScaleUtility.normalize(rightExtVector);
            rightExtVector.scale(rightExt);

            upperRight.add(rightExtVector);
            lowerRight.add(rightExtVector);
        }

        double leftUp = NumUtil.readDouble(finIDF.getDataByStandardComment("Left Distance Above Top of Window"), 0);
        if(leftUp>0){
            Coordinate3D leftLU = ScaleUtility.makeVector(lowerLeft, upperLeft);
            ScaleUtility.normalize(leftLU);
            leftLU.scale(leftUp);

            upperLeft.add(leftLU);
        }

        double rightUp = NumUtil.readDouble(finIDF.getDataByStandardComment("Right Distance Above Top of Window"), 0);
        if(rightUp>0){
            Coordinate3D rightLU = ScaleUtility.makeVector(lowerRight, upperRight);
            ScaleUtility.normalize(rightLU);
            rightLU.scale(rightUp);

            upperRight.add(rightLU);
        }

        double leftDown = NumUtil.readDouble(finIDF.getDataByStandardComment("Left Distance Below Bottom of Window"), 0);
        if(leftDown>0){
            Coordinate3D leftUL = ScaleUtility.makeVector(upperLeft, lowerLeft);
            ScaleUtility.normalize(leftUL);
            leftUL.scale(leftDown);

            lowerLeft.add(leftUL);
        }

        double rightDown = NumUtil.readDouble(finIDF.getDataByStandardComment("Right Distance Below Bottom of Window"), 0);
        if(rightDown>0){
            Coordinate3D rightUL = ScaleUtility.makeVector(upperRight, lowerRight);
            ScaleUtility.normalize(rightUL);
            rightUL.scale(rightDown);

            lowerRight.add(rightUL);
        }

        double leftTilt = NumUtil.readDouble(finIDF.getDataByStandardComment("Left Tilt Angle from Window/Door"), 90);
        Coordinate3D leftTiltVector = feneNormal;
        if(leftTilt!=180){
            Coordinate3D leftUL = ScaleUtility.makeVector(upperLeft, lowerLeft); // rotation axis given tilt (right hand rule)
            Coordinate3D innerVector = ScaleUtility.cross(feneNormal, leftUL);   // vector to be rotated

            ScaleUtility.normalize(innerVector);
            ScaleUtility.normalize(leftUL);

            // Rodrigues' rotation formula
            leftTiltVector = ScaleUtility.cross(leftUL, innerVector);
            leftTiltVector.scale(Math.sin(Math.toRadians(leftTilt)));
            innerVector.scale(Math.cos(Math.toRadians(leftTilt)));
            leftTiltVector.add(innerVector);

            ScaleUtility.normalize(leftTiltVector);
        }

        double rightTilt = NumUtil.readDouble(finIDF.getDataByStandardComment("Right Tilt Angle from Window/Door"), 90);
        Coordinate3D rightTiltVector = feneNormal;
        if(rightTilt!=180){
            Coordinate3D rightLU = ScaleUtility.makeVector(lowerRight, upperRight); // rotation axis given tilt (right hand rule)
            Coordinate3D innerVector = ScaleUtility.cross(feneNormal, rightLU);   // vector to be rotated

            ScaleUtility.normalize(innerVector);
            ScaleUtility.normalize(rightLU);

            // Rodrigues' rotation formula
            rightTiltVector = ScaleUtility.cross(rightLU, innerVector);
            rightTiltVector.scale(Math.sin(Math.toRadians(rightTilt)));
            innerVector.scale(Math.cos(Math.toRadians(rightTilt)));
            rightTiltVector.add(innerVector);

            ScaleUtility.normalize(rightTiltVector);
        }

        if(finIDF.getObjLabel().equalsIgnoreCase("Shading:Fin")){
            leftTiltVector.scale(NumUtil.readDouble(finIDF.getDataByStandardComment("Left Depth"), 1));
            rightTiltVector.scale(NumUtil.readDouble(finIDF.getDataByStandardComment("Right Depth"), 1));
        }else if(finIDF.getObjLabel().equalsIgnoreCase("Shading:Fin:Projection")){
            Coordinate3D upperLR = ScaleUtility.makeVector(upperLeft, upperRight);
            Coordinate3D lowerLR = ScaleUtility.makeVector(lowerLeft, lowerRight);

            double width = Math.max(upperLR.vectorLen(), lowerLR.vectorLen());
            leftTiltVector.scale(width*NumUtil.readDouble(finIDF.getDataByStandardComment("Left Depth as Fraction of Window/Door Height"), 1));
            rightTiltVector.scale(width*NumUtil.readDouble(finIDF.getDataByStandardComment("Right Depth as Fraction of Window/Door Height"), 1));
        }

        Coordinate3D upperLeftTilt = upperLeft.duplicate();
        upperLeftTilt.add(leftTiltVector);

        Coordinate3D lowerLeftTilt = lowerLeft.duplicate();
        lowerLeftTilt.add(leftTiltVector);

        List<Coordinate3D> leftFinPoints = new ArrayList<>();
        leftFinPoints.add(upperLeft);
        leftFinPoints.add(lowerLeft);
        leftFinPoints.add(lowerLeftTilt);
        leftFinPoints.add(upperLeftTilt);

        Surface leftFin = new Surface(leftFinPoints);
        leftFin.setSurfaceType(SurfaceType.Fin);
        leftFin.setZoneName(fene.getZoneName());
        leftFin.setName(finIDF.getName()+"_LEFT");

        Coordinate3D upperRightTilt = upperRight.duplicate();
        upperRightTilt.add(rightTiltVector);

        Coordinate3D lowerRightTilt = lowerRight.duplicate();
        lowerRightTilt.add(rightTiltVector);

        List<Coordinate3D> rightFinPoints = new ArrayList<>();
        rightFinPoints.add(upperRight);
        rightFinPoints.add(upperRightTilt);
        rightFinPoints.add(lowerRightTilt);
        rightFinPoints.add(lowerRight);

        Surface rightFin = new Surface(rightFinPoints);
        rightFin.setSurfaceType(SurfaceType.Fin);
        rightFin.setZoneName(fene.getZoneName());
        rightFin.setName(finIDF.getName()+"_RIGHT");

        return new Surface[]{leftFin, rightFin};
    }

    private Surface buildOverhang(IDFObject overhangIDF, Surface fene, IDFFileObject idfFileObject){
        List<Coordinate3D> topPoints = fene.getPositionedCoordinatesByHeight(idfFileObject);
        if(topPoints==null){
            return null;
        }

        Coordinate3D left = topPoints.get(0);
        Coordinate3D right = topPoints.get(1);

        double leftExt = NumUtil.readDouble(overhangIDF.getDataByStandardComment("Left extension from Window/Door Width"), 0);
        if(leftExt!=0){
            Coordinate3D rl = ScaleUtility.makeVector(right, left);
            ScaleUtility.normalize(rl);
            rl.scale(leftExt);
            left.add(rl);
        }

        double rightExt = NumUtil.readDouble(overhangIDF.getDataByStandardComment("Right extension from Window/Door Width"), 0);
        if(rightExt!=0){
            Coordinate3D lr = ScaleUtility.makeVector(left, right);
            ScaleUtility.normalize(lr);
            lr.scale(rightExt);
            right.add(lr);
        }

        Coordinate3D feneNormal = fene.getNormalVector(idfFileObject);

        double topMove = NumUtil.readDouble(overhangIDF.getDataByStandardComment("Height above Window or Door"), 0);
        if(topMove!=0){
            Coordinate3D lr = ScaleUtility.makeVector(left, right);
            Coordinate3D uppVector = ScaleUtility.cross(feneNormal, lr);

            ScaleUtility.normalize(uppVector);
            uppVector.scale(topMove);

            left.add(uppVector);
            right.add(uppVector);
        }

        double tilt = NumUtil.readDouble(overhangIDF.getDataByStandardComment("Tilt Angle from Window/Door"), 90);
        Coordinate3D tiltVector = feneNormal;
        if(tilt!=90){
            Coordinate3D rl = ScaleUtility.makeVector(right, left);
            Coordinate3D downVector = ScaleUtility.cross(feneNormal, rl);

            ScaleUtility.normalize(downVector);  // vector to be rotated

            ScaleUtility.normalize(rl);          // rotation axis given tilt (right hand rule)

            tiltVector = ScaleUtility.cross(rl, downVector);  // Rodrigues' rotation formula
            tiltVector.scale(Math.sin(Math.toRadians(tilt)));
            downVector.scale(Math.cos(Math.toRadians(tilt)));
            tiltVector.add(downVector);

            ScaleUtility.normalize(tiltVector);
        }

        if(overhangIDF.getObjLabel().equalsIgnoreCase("Shading:Overhang")){
            tiltVector.scale(NumUtil.readDouble(overhangIDF.getDataByStandardComment("Depth"), 1));
        }else if(overhangIDF.getObjLabel().equalsIgnoreCase("Shading:Overhang:Projection")){
            double height = fene.getHeight();
            tiltVector.scale(height*NumUtil.readDouble(overhangIDF.getDataByStandardComment("Depth as Fraction of Window/Door Height"), 1));
        }

        Coordinate3D leftTilt = left.duplicate();
        leftTilt.add(tiltVector);

        Coordinate3D rightTilt = right.duplicate();
        rightTilt.add(tiltVector);

        List<Coordinate3D> overhangPoints = new ArrayList<>();
        overhangPoints.add(left);
        overhangPoints.add(leftTilt);
        overhangPoints.add(rightTilt);
        overhangPoints.add(right);

        Surface overhang = new Surface(overhangPoints);
        overhang.setSurfaceType(SurfaceType.Overhang);
        overhang.setZoneName(fene.getZoneName());
        overhang.setName(overhangIDF.getName());
        return overhang;
    }
    
    public boolean isParseSuccess(){
        return this.parseSuccess;
    }
    
    public String getParseErrorMsg(){
        return this.parseErrMsg;
    }
    
    private IDFObject getZoneObj(IDFObject surfaceObj){
        String zoneName = surfaceObj.getDataByStandardComment("Zone Name");
        
        IDFObject zoneObj = zoneMap.get(zoneName);
        if(zoneObj==null){
            zoneObj = plenumMap.get(zoneName);
        }
        if(zoneObj==null){
            LOG.warn("Surface "+surfaceObj.getName()+" zone cannot be found, (zone name: "+zoneName+")");
        }
        
        return zoneObj;
    }
    
    public IDFObject getBuilding() {
        return building;
    }

    public void setBuilding(IDFObject building) {
        this.building = building;
    }
    
    /*public boolean hasHVACSortList(){
        return this.hasHVACSortList;
    }*/
    
    public void addZone(IDFObject zone){
        zoneMap.put(zone.getName(), zone);
        
        if(!zoneToSurface.containsKey(zone.getName())){
            zoneToSurface.put(zone.getName(), new ArrayList<IDFObject>());
        }
    }
    
    public void addSurface(IDFObject surfaceObj){
        String zoneName = surfaceObj.getDataByStandardComment("Zone Name");
        
        surfaceMap.put(surfaceObj.getName(), surfaceObj);
        
        List<IDFObject> surfaceList = zoneToSurface.get(zoneName);
        if(surfaceList == null){
            zoneToSurface.put(zoneName, new ArrayList<>());
            surfaceList = zoneToSurface.get(zoneName);
        }
        surfaceList.add(surfaceObj);
        
        if(!surfaceToFenestration.containsKey(surfaceObj.getName())){
            surfaceToFenestration.put(surfaceObj.getName(), new ArrayList<IDFObject>());
        }
    }
    
    public void addFenestration(IDFObject fenestration){      //FenestrationSurface:Detailed
        String surfaceName = fenestration.getDataByStandardComment("Building Surface Name");
        
        fenestrationMap.put(fenestration.getName(), fenestration);
        
        List<IDFObject> fenestrationList = surfaceToFenestration.get(surfaceName);
        if(fenestrationList == null){
            surfaceToFenestration.put(surfaceName, new ArrayList<IDFObject>());
            fenestrationList = surfaceToFenestration.get(surfaceName);
        }
        fenestrationList.add(fenestration);
    }
    
    public Map<String, IDFObject> getZones() {
        return zoneMap;
    }
    
    public IDFObject getZoneObj(String zoneName){
        return zoneMap.get(zoneName);
    }

    public Map<String, IDFObject> getSurfaceMap() {
        return surfaceMap;
    }
    
    public IDFObject getSurfaceObj(String surfaceName){
        return this.surfaceMap.get(surfaceName);
    }
    
    public IDFObject getSurfaceZoneObj(String surfaceName){
        return this.surfaceToZone.get(surfaceName);
    }

    public Map<String, IDFObject> getFenestrationMap() {
        return fenestrationMap;
    }
    
    public IDFObject getFenestrationObj(String fenestrationName){
        return this.fenestrationMap.get(fenestrationName);
    }
    
    public IDFObject getFenestrationSurface(String fenestrationName){
        return this.fenestrationMap.get(fenestrationName);
    }

    public Map<String, List<IDFObject>> getZoneToSurfaces() {
        return zoneToSurface;
    }
    
    public Map<String, Integer> getMultiplierZones() {
        return this.multiplierZones;
    }
    
    public List<IDFObject> getZoneSurfaces(String zoneName){
        return this.zoneToSurface.get(zoneName);
    }
    
    public List<IDFObject> getSurfaceFenestrationNames(String surfaceName){
        return this.surfaceToFenestration.get(surfaceName);
    }
    
    public List<String> getZoneNames() {
        return new ArrayList<>(zoneMap.keySet());
    }
    
    public List<String> getSurfaceNames(){
        return new ArrayList<>(surfaceMap.keySet());
    }
    
    public List<String> getFenestrationNames(){
        return new ArrayList<>(fenestrationMap.keySet());
    }
    
    public List<Surface> getSurroundShading(){
        return this.surroundShading;
    }

    public SortedCollection getSortedCollection() {
        //Step One: assemble all surfaces into zones
        Map<String, IDFObject> allSurfaces = this.surfaceMap;
        
        ArrayList<Surface> surfaces = new ArrayList<>();
        Set<String> keys = allSurfaces.keySet();
        
        //Populate surfaces with the first part (all building surfaces)
        Map<String, IDFObject> surfaceToZone = new HashMap<>();
        for(String key : keys){
            IDFObject surface = allSurfaces.get(key);
            String surfaceName = surface.getName();
            
            String zoneName = surface.getDataByStandardComment("Zone Name");
            IDFObject zone = zoneMap.get(zoneName);
            
            if(zone==null){
                zone = plenumMap.get(zoneName);
            }
            
            if(zone==null){
                LOG.error("Surface "+surfaceName+"'s zone not found: "+zoneName);
                continue;
            }
            
            surfaceToZone.put(surfaceName, zone);
            surfaces.add(new Surface(surface, zone));
        }
        
        //Sort the first part into zone lists and updating the lowest point of that zone
        Map<String, List<Surface>> zoneSurfaces = new HashMap<>();
        Map<String, Double> zoneLowestPoint = new HashMap<>();
        
        for(Surface surface: surfaces){
            String zone = surface.getZoneName();
            if(zoneSurfaces.containsKey(zone)){
                //Overwrite the last null pointer with new data
                zoneSurfaces.get(zone).set(zoneSurfaces.get(zone).size()-1, surface);
                //New null pointer to mark the current end of building surfaces
                zoneSurfaces.get(zone).add(null);
                //Updating the lowest point of that zone
                if(zoneLowestPoint.get(zone) > surface.getLowestPoint()){
                    zoneLowestPoint.put(zone, surface.getLowestPoint());
                }
            }
            else{
                ArrayList<Surface> newSurfaceArrayList = new ArrayList<>();
                newSurfaceArrayList.add(surface);
                //Null pointer to mark the current end of building surfaces
                newSurfaceArrayList.add(null);
                zoneSurfaces.put(zone, newSurfaceArrayList);
                zoneLowestPoint.put(zone, surface.getLowestPoint());
            }
        }
        
        //Second part is to sort all fenestrational surfaces in the same fashion
        allSurfaces = this.fenestrationMap;
        surfaces = new ArrayList<>();
        keys = allSurfaces.keySet();
        
        //Populating surface arraylist
        for(String key : keys){
            IDFObject fenestration = allSurfaces.get(key);
            String fenestrationName = fenestration.getName();
            String surfaceName = fenestration.getIndexedData(3);
            
            IDFObject zone = surfaceToZone.get(surfaceName);
            
            if(zone==null){
                LOG.error("Fenestration "+fenestrationName+"'s zone not found, surface name: "+surfaceName);
                continue;
            }
            
            surfaceToZone.put(fenestrationName, zone);
            surfaces.add(new Surface(fenestration, zone));
        }
        
        //Updating key set to all zone names
        //NOTE: right now all zones should be included in the set
        keys = zoneToSurface.keySet();

        //In second part, compare the surface name with all surface names in zone - surface collection to put fenestration into correct zone list
        for(Surface surface: surfaces){
            //NOTE: For fenestration, the zone name is actually the building surface name
            String fenestrationSurface = surface.getZoneName();
            for(String zoneName : keys){
            	for(IDFObject obj : zoneToSurface.get(zoneName)) {
            		if(obj.getName().equalsIgnoreCase(fenestrationSurface)) {
            			zoneSurfaces.get(zoneName).add(surface);
            			break;
            		}
            	}
                /*if(zoneToSurface.get(zoneName).contains(fenestrationSurface)){
                    zoneSurfaces.get(zoneName).add(surface);
                    break;
                }*/
            }
            //Assuming all fenestration points are contained within the corresponding building surface so that
            //no updating lowest point is required
        }
        
        allSurfaces = new HashMap<String, IDFObject>();
        allSurfaces.putAll(surfaceMap);
        allSurfaces.putAll(fenestrationMap);
        
        SortedCollection sortedCollection = new SortedCollection(zoneSurfaces, zoneLowestPoint, allSurfaces, surfaceToZone);
        
        return sortedCollection;
    }

    public void addMultiplierZone(IDFObject stat) {
        String zoneName = stat.getIndexedData(0);
        int multiplier = Integer.parseInt(stat.getIndexedData(6));
        
        multiplierZones.put(zoneName, multiplier);
    }
    
    public List<Surface> getSurfaces(){
        return new ArrayList<>(surfaces.values());
    }
    
    public Map<Integer, List<String>> getHeightZones(){
        return heightZones;
    }
    
    public Map<String, List<Surface>> getZoneSurfaces(){
        return zoneSurfaces;
    }
    
    /*public void genHVACSortList(){
        ohuMap = new HashMap<String, ArrayList<String>>();
        vrfMap = new HashMap<String, ArrayList<String>>();
        
        for(String zoneName : zoneNames){
            String[] split = zoneName.split("%");
            if(split.length>5){
                String ohuName = split[3].trim();
                if(!ohuMap.containsKey(ohuName)){
                    ohuMap.put(ohuName, new ArrayList<String>());
                }
                ohuMap.get(ohuName).add(zoneName);
                
                String vrfName = split[4].trim();
                if(!vrfMap.containsKey(vrfName)){
                    vrfMap.put(vrfName, new ArrayList<String>());
                }
                vrfMap.get(vrfName).add(zoneName);
            }
        }
        
        hasHVACSortList = true;
    }
    
    public HashMap<String, ArrayList<String>> getOHUMap(){
        return this.ohuMap;
    }
    public HashMap<String, ArrayList<String>> getVRFMap(){
        return this.vrfMap;
    }*/
}

