package main.java.model.geometry;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import main.java.config.ServerConfig;
import main.java.model.geometry.scale.Coordinate2D;
import main.java.model.geometry.scale.Coordinate3D;
import main.java.model.geometry.scale.ScaleUtility;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.model.idf.IDFParser;
import main.java.util.NumUtil;

public class Surface {     
    private List<Double> lineCoordinates = new ArrayList<>();
    private List<double[]> surfaceCoordinates = new ArrayList<>();
    
    
    private String name;
    private String zoneName;
    private SurfaceType type;
    private boolean isSlabOnGrade = false;
    
    private List<Surface> holes = new ArrayList<>();
    
    private double maxX = NumUtil.MIN_VALUE,
                   maxY = NumUtil.MIN_VALUE,
                   maxZ = NumUtil.MIN_VALUE,
                   minX = NumUtil.MAX_VALUE,
                   minY = NumUtil.MAX_VALUE,
                   minZ = NumUtil.MAX_VALUE;

    public Surface(List<Coordinate3D> points){
        double[] coord = points.get(0).getCoords();
        surfaceCoordinates.add(coord);
        lineCoordinates.add(coord[0]);
        lineCoordinates.add(coord[1]);
        lineCoordinates.add(coord[2]);
        maxX = minX = coord[0];
        maxY = minY = coord[1];
        maxZ = minZ = coord[2];

        for(int i=1;i<points.size();i++){
            double[] coors = points.get(i).getCoords();
            surfaceCoordinates.add(coors);

            if(maxX < coors[0]){
                maxX = coors[0];
            }
            if(maxY < coors[1]){
                maxY = coors[1];
            }
            if(maxZ < coors[2]){
                maxZ = coors[2];
            }
            if(minX > coors[0]){
                minX = coors[0];
            }
            if(minY > coors[1]){
                minY = coors[1];
            }
            if(minZ > coors[2]){
                minZ = coors[2];
            }

            lineCoordinates.add(coors[0]);
            lineCoordinates.add(coors[1]);
            lineCoordinates.add(coors[2]);

            lineCoordinates.add(coors[0]);
            lineCoordinates.add(coors[1]);
            lineCoordinates.add(coors[2]);
        }

        surfaceCoordinates.add(points.get(0).getCoords());

        lineCoordinates.add(coord[0]);
        lineCoordinates.add(coord[1]);
        lineCoordinates.add(coord[2]);
    }

    public Surface(IDFObject winDoorObj, Surface parentSurface){
        this.isSlabOnGrade = false;

        List<double[]> parentCoords = parentSurface.getSurfaceCoordinates();
        double winDoorStartX = NumUtil.readDouble(winDoorObj.getDataByStandardComment("Starting X Coordinate"), 0);
        double winDoorStartZ = NumUtil.readDouble(winDoorObj.getDataByStandardComment("Starting Z Coordinate"), 0);
        double winDoorLen = NumUtil.readDouble(winDoorObj.getDataByStandardComment("Length"), 1);
        double winDoorHeight = NumUtil.readDouble(winDoorObj.getDataByStandardComment("Height"), 0);

        double[] p1Coord = parentCoords.get(0);
        Coordinate3D p1 = new Coordinate3D(p1Coord[0], p1Coord[1], p1Coord[2]);
        double[] p2Coord = parentCoords.get(1);
        Coordinate3D p2 = new Coordinate3D(p2Coord[0], p2Coord[1], p2Coord[2]);
        double[] p3Coord = parentCoords.get(2);
        Coordinate3D p3 = new Coordinate3D(p3Coord[0], p3Coord[1], p3Coord[2]);

        Coordinate3D lenVector = ScaleUtility.makeVector(p1, p2);
        lenVector.setZ(0); // horizontal shift
        ScaleUtility.normalize(lenVector);

        Coordinate3D lenVectorReverse = lenVector.duplicate();
        lenVectorReverse.scale(-1);

        Coordinate3D heightVector = ScaleUtility.makeVector(p2, p3);
        double p2xLen = ScaleUtility.dot(heightVector, lenVectorReverse);
        Coordinate3D p2xVector = lenVectorReverse.duplicate();
        p2xVector.scale(p2xLen);
        Coordinate3D x = p2.duplicate();
        x.add(p2xVector);

        Coordinate3D xp3 = ScaleUtility.makeVector(x, p3); // xp3 is perpendicular to p1p2, and xp3 is in the plan of p1p2p3
        ScaleUtility.normalize(xp3);

        Coordinate3D wd1Shift = lenVector.duplicate();
        wd1Shift.scale(winDoorStartX);
        Coordinate3D upShift = xp3.duplicate();
        upShift.scale(winDoorStartZ);
        wd1Shift.add(upShift);
        Coordinate3D wd1 = p1.duplicate();
        wd1.add(wd1Shift);

        Coordinate3D wd2 = wd1.duplicate();
        Coordinate3D wd2Shift = lenVector.duplicate();
        wd2Shift.scale(winDoorLen);
        wd2.add(wd2Shift);

        Coordinate3D wd3 = wd2.duplicate();
        Coordinate3D w3Shift = xp3.duplicate();
        w3Shift.scale(winDoorHeight);
        wd3.add(w3Shift);

        Coordinate3D wd4 = wd3.duplicate();
        Coordinate3D wd4Shift = lenVectorReverse.duplicate();
        wd4Shift.scale(winDoorLen);
        wd4.add(wd4Shift);

        surfaceCoordinates.add(wd1.getCoords());
        surfaceCoordinates.add(wd2.getCoords());
        surfaceCoordinates.add(wd3.getCoords());
        surfaceCoordinates.add(wd4.getCoords());
        surfaceCoordinates.add(wd1.getCoords());

        double[] coord = surfaceCoordinates.get(0);
        lineCoordinates.add(coord[0]);
        lineCoordinates.add(coord[1]);
        lineCoordinates.add(coord[2]);
        maxX = minX = coord[0];
        maxY = minY = coord[1];
        maxZ = minZ = coord[2];

        for(int i=1;i<surfaceCoordinates.size();i++){
            double[] array = surfaceCoordinates.get(i);
            double valueX = array[0];
            double valueY = array[1];
            double valueZ = array[2];
            if(maxX < valueX){
                maxX = valueX;
            }
            if(maxY < valueY){
                maxY = valueY;
            }
            if(maxZ < valueZ){
                maxZ = valueZ;
            }
            if(minX > valueX){
                minX = valueX;
            }
            if(minY > valueY){
                minY = valueY;
            }
            if(minZ > valueZ){
                minZ = valueZ;
            }

            lineCoordinates.add(valueX);
            lineCoordinates.add(valueY);
            lineCoordinates.add(valueZ);

            lineCoordinates.add(valueX);
            lineCoordinates.add(valueY);
            lineCoordinates.add(valueZ);
        }

        lineCoordinates.add(coord[0]);
        lineCoordinates.add(coord[1]);
        lineCoordinates.add(coord[2]);

        name = winDoorObj.getName();
        zoneName = parentSurface.getZoneName();

        type = SurfaceType.WinDoor;
    }
     
    public Surface(IDFObject surfaceObj, IDFObject zoneObj){
        double rotate = NumUtil.readDouble(zoneObj.getDataByStandardComment("Direction of Relative North"), 0.0);
        double radian = 0 - rotate*Math.PI/180.0;
         
        double cosR = Math.cos(radian);
        double sinR = Math.sin(radian);
         
        double zoneX = NumUtil.readDouble(zoneObj.getDataByStandardComment("X Origin"), 0.0);
        double zoneY = NumUtil.readDouble(zoneObj.getDataByStandardComment("Y Origin"), 0.0);
        double zoneZ = NumUtil.readDouble(zoneObj.getDataByStandardComment("Z Origin"), 0.0);
         
        int len = surfaceObj.getObjLen()-1;  //exclude object label line
        double lastX = 0D, lastY = 0D, lastZ = 0D, 
               firstX = 0D, firstY = 0D, firstZ = 0D, 
               valueX, valueY, valueZ;
        
        boolean isOnGround = true;
         
        //vertex coordinates start after number of vertices, extensible till the end
        int numOfVertex = surfaceObj.getStandardCommentIndex("Number of Vertices");
        for(int j=numOfVertex+1;j<len;j+=3){
            double surX = NumUtil.readDouble(surfaceObj.getIndexedData(j), 0.0);
            double surY = NumUtil.readDouble(surfaceObj.getIndexedData(j+1), 0.0);
            double surZ = NumUtil.readDouble(surfaceObj.getIndexedData(j+2), 0.0);
             
            valueX = surX*cosR - surY*sinR + zoneX;
            valueY = surX*sinR + surY*cosR + zoneY;
            valueZ = surZ + zoneZ;
            
            if(maxX < valueX){
                maxX = valueX;
            }
            if(maxY < valueY){
                maxY = valueY;
            }
            if(maxZ < valueZ){
                maxZ = valueZ;
            }
            if(minX > valueX){
                minX = valueX;
            }
            if(minY > valueY){
                minY = valueY;
            }
            if(minZ > valueZ){
                minZ = valueZ;
            }
             
            if(j == numOfVertex+1){
                firstX = valueX;
                firstY = valueY;
                firstZ = valueZ;
            }else {
                lineCoordinates.add(lastX);
                lineCoordinates.add(lastY);
                lineCoordinates.add(lastZ);
                 
                lineCoordinates.add(valueX);
                lineCoordinates.add(valueY);
                lineCoordinates.add(valueZ);
            }
             
            lastX = valueX;
            lastY = valueY;
            lastZ = valueZ;
            
            surfaceCoordinates.add(new double[]{valueX, valueY, valueZ});
            
            if(valueZ>0.1){
                isOnGround = false;
            }
            
            if(valueZ>-0.1 && valueZ < 0.1) {
                isSlabOnGrade = true;
            }
        }
        
        surfaceCoordinates.add(new double[]{firstX, firstY, firstZ});
        
        lineCoordinates.add(lastX);
        lineCoordinates.add(lastY);
        lineCoordinates.add(lastZ);
        lineCoordinates.add(firstX);
        lineCoordinates.add(firstY);
        lineCoordinates.add(firstZ);
         
        name = surfaceObj.getName();
        zoneName = zoneObj.getName();
        
        type = SurfaceType.getSurfaceType(surfaceObj);
        
        if(isOnGround){
            //do small lift
            for(double[] coor : surfaceCoordinates){	
                coor[2] += 0.05;  
            }
        }
    }
    
    public Surface(IDFObject surfaceObj){         
        int len = surfaceObj.getObjLen()-1;  //exclude object label line
        double lastX = 0D, lastY = 0D, lastZ = 0D, 
               firstX = 0D, firstY = 0D, firstZ = 0D;
        
        boolean isOnGround = true;
         
        //vertex coordinates start after number of vertices, extensible till the end
        int numOfVertex = surfaceObj.getStandardCommentIndex("Number of Vertices");
        for(int j=numOfVertex+1;j<len;j+=3){
            double surX = NumUtil.readDouble(surfaceObj.getIndexedData(j), 0.0);
            double surY = NumUtil.readDouble(surfaceObj.getIndexedData(j+1), 0.0);
            double surZ = NumUtil.readDouble(surfaceObj.getIndexedData(j+2), 0.0);
            
            if(maxX < surX){
                maxX = surX;
            }
            if(maxY < surY){
                maxY = surY;
            }
            if(maxZ < surZ){
                maxZ = surZ;
            }
            if(minX > surX){
                minX = surX;
            }
            if(minY > surY){
                minY = surY;
            }
            if(minZ > surZ){
                minZ = surZ;
            }
             
            if(j == numOfVertex+1){
                firstX = surX;
                firstY = surY;
                firstZ = surZ;
            }else {
                lineCoordinates.add(lastX);
                lineCoordinates.add(lastY);
                lineCoordinates.add(lastZ);
                 
                lineCoordinates.add(surX);
                lineCoordinates.add(surY);
                lineCoordinates.add(surZ);
            }
             
            lastX = surX;
            lastY = surY;
            lastZ = surZ;
            
            surfaceCoordinates.add(new double[]{surX, surY, surZ});
            
            if(surZ>0.1){
                isOnGround = false;
            }
            
            if(surZ>-0.1 && surZ < 0.1) {
        			isSlabOnGrade = true;
            }
        }
        
        surfaceCoordinates.add(new double[]{firstX, firstY, firstZ});
        
        lineCoordinates.add(lastX);
        lineCoordinates.add(lastY);
        lineCoordinates.add(lastZ);
        lineCoordinates.add(firstX);
        lineCoordinates.add(firstY);
        lineCoordinates.add(firstZ);
         
        name = surfaceObj.getName();
        zoneName = null;
        
        type = SurfaceType.Shading;
        
        if(isOnGround){
            //do small lift
            for(double[] coor : surfaceCoordinates){
                coor[2] += 0.05;
            }
        }
    }

    public double getHeight(){
        Coordinate3D h = null;
        Coordinate3D l1 = null;
        Coordinate3D l2 = null;

        for(int i=0;i<surfaceCoordinates.size();i++){
            double[] coord = surfaceCoordinates.get(i);
            if(coord[2]==maxZ && h==null){
                h = new Coordinate3D(coord);
            }
            if(coord[2]==minZ){
                if(l1==null){
                    l1 = new Coordinate3D(coord);
                }else if(l2==null){
                    l2 = new Coordinate3D(coord);
                }
            }
        }

        if(h==null || l1==null){
            return maxZ - minZ;
        }

        if(l2==null){
            return ScaleUtility.makeVector(l1, h).vectorLen();
        }else {
            return ScaleUtility.pointToLine(h, l1, l2);
        }
    }

    /**
     * 0: upper left point<br/>1: upper right point<br/>
     * 1: lower left point<br/>1: lower right point<br/>
     */
    public List<Coordinate3D> getPositionedCoordinatesByHeight(IDFFileObject idfFileObject){
        String vertexDirection = "ccw";
        List<IDFObject> geoRules = idfFileObject.getCategoryList("GlobalGeometryRules");
        if(geoRules!=null && !geoRules.isEmpty()) {
            IDFObject geoRule = geoRules.get(0);
            if (geoRule.getDataByStandardComment("Vertex Entry Direction").equalsIgnoreCase("Clockwise")) {
                vertexDirection = "cw";
            }
        }

        Coordinate3D p1 = null;
        Coordinate3D p2 = null;
        int p1Idx = -1;
        int p2Idx = -1;
        for(int i=0;i<surfaceCoordinates.size();i++){
            double[] coord = surfaceCoordinates.get(i);
            if(coord[2]==maxZ){
                if(p1==null){
                    p1 = new Coordinate3D(coord);
                    p1Idx = i;
                }else if(p2==null){
                    p2 = new Coordinate3D(coord);
                    p2Idx = i;
                    break;
                }
            }
        }

        if(p1==null){
            return null;
        }

        List<Coordinate3D> res = new ArrayList<>();
        int totalVertices = surfaceCoordinates.size();
        if(p2==null){
            int preIdx = (p1Idx-1)%totalVertices;
            int nextIdx = (p1Idx+1)%totalVertices;

            Coordinate3D pre = new Coordinate3D(surfaceCoordinates.get(preIdx));
            Coordinate3D next = new Coordinate3D(surfaceCoordinates.get(nextIdx));
            Coordinate3D lr;
            if(vertexDirection.equals("ccw")){
                lr = ScaleUtility.makeVector(next, pre);
            }else {
                lr = ScaleUtility.makeVector(pre, next);
            }
            ScaleUtility.normalize(lr);
            lr.scale(0.5);

            Coordinate3D right = p1.duplicate();
            right.add(lr);

            lr.scale(-1);
            p1.add(lr);

            res.add(p1);
            res.add(right);
            if(vertexDirection.equals("ccw")){
                res.add(next);
                res.add(pre);
            }else {
                res.add(pre);
                res.add(next);
            }

            return res;
        }

        int lowerLeftIdx;
        int lowerRightIdx;
        if(vertexDirection.equals("ccw")){
            if(Math.abs(p1Idx-p2Idx)==1){
                res.add(p2);
                res.add(p1);

                lowerLeftIdx = (p2Idx+1)%totalVertices;
                lowerRightIdx = (p1Idx-1)%totalVertices;
            }else {
                res.add(p1);
                res.add(p2);
                lowerLeftIdx = (p1Idx+1)%totalVertices;
                lowerRightIdx = (p2Idx-1)%totalVertices;
            }
        }else {
            if(Math.abs(p1Idx-p2Idx)==1){
                res.add(p1);
                res.add(p2);

                lowerLeftIdx = (p1Idx-1)%totalVertices;
                lowerRightIdx = (p2Idx+1)%totalVertices;
            }else {
                res.add(p2);
                res.add(p1);

                lowerLeftIdx = (p2Idx-1)%totalVertices;
                lowerRightIdx = (p1Idx+1)%totalVertices;
            }
        }

        res.add(new Coordinate3D(surfaceCoordinates.get(lowerLeftIdx)));
        res.add(new Coordinate3D(surfaceCoordinates.get(lowerRightIdx)));
        return res;
    }



    /**
     * 0: upper left point<br/>1: upper right point
     */
    public List<Coordinate3D> getTopLineCoordinatesByStartVertex(IDFFileObject idfFileObject){
        String vertexDirection = "ccw";
        int leftIdx = 0, rightIdx = 3;   // default start vertex is upper left
        List<IDFObject> geoRules = idfFileObject.getCategoryList("GlobalGeometryRules");
        if(geoRules!=null && !geoRules.isEmpty()){
            IDFObject geoRule = geoRules.get(0);
            if(geoRule.getDataByStandardComment("Vertex Entry Direction").equalsIgnoreCase("Clockwise")){
                vertexDirection = "cw";
            }

            String startPos = geoRule.getDataByStandardComment("Starting Vertex Position");
            if(startPos.equalsIgnoreCase("UpperLeftCorner")){
                if(vertexDirection.equals("ccw")){
                    rightIdx = 3;
                }else {
                    rightIdx = 1;
                }
                leftIdx = 0;
            }else if(startPos.equalsIgnoreCase("LowerLeftCorner")){
                if(vertexDirection.equals("ccw")){
                    leftIdx = 3;
                }else {
                    leftIdx = 1;
                }
                rightIdx = 2;
            }else if(startPos.equalsIgnoreCase("UpperRightCorner")){
                if(vertexDirection.equals("ccw")){
                    leftIdx = 1;
                }else {
                    leftIdx = 3;
                }
                rightIdx = 0;
            }else if(startPos.equalsIgnoreCase("LowerRightCorner")){
                if(vertexDirection.equals("ccw")){
                    rightIdx = 3;
                }else {
                    rightIdx = 1;
                }
                leftIdx = 2;
            }
        }

        List<Coordinate3D> res = new ArrayList<>();
        Coordinate3D left = new Coordinate3D(surfaceCoordinates.get(leftIdx));
        Coordinate3D right = new Coordinate3D(surfaceCoordinates.get(rightIdx));
        res.add(left);
        res.add(right);
        return res;
    }

    public Coordinate3D getNormalVector(IDFFileObject idfFileObject){
        String vertexDirection = "ccw";
        List<IDFObject> geoRules = idfFileObject.getCategoryList("GlobalGeometryRules");
        if(geoRules!=null && !geoRules.isEmpty()){
            IDFObject geoRule = geoRules.get(0);
            if(geoRule.getDataByStandardComment("Vertex Entry Direction").equalsIgnoreCase("Clockwise")){
                vertexDirection = "cw";
            }
        }

        Coordinate3D p1 = new Coordinate3D(surfaceCoordinates.get(0));
        Coordinate3D p2 = new Coordinate3D(surfaceCoordinates.get(1));
        Coordinate3D p3 = new Coordinate3D(surfaceCoordinates.get(2));
        Coordinate3D p12 = ScaleUtility.makeVector(p1, p2);
        Coordinate3D p23 = ScaleUtility.makeVector(p2, p3);

        Coordinate3D normVector = vertexDirection.equalsIgnoreCase("ccw")
                ? ScaleUtility.cross(p12, p23) : ScaleUtility.cross(p23, p12);
        ScaleUtility.normalize(normVector);

        return normVector;
    }

    /**
     * x+ is 0 degree, x- is 180/-180 degree, y+ is 90 degree, y- is -90 degree
     */
    public double getFaceAngle(IDFFileObject idfFileObject){
        Coordinate3D normVector = getNormalVector(idfFileObject);

        Coordinate2D xy = new Coordinate2D(normVector.getX(), normVector.getY());
        if(xy.getY()==0 && xy.getX()==0){
            return 999;
        }

        ScaleUtility.normalize(xy);
        return Math.toDegrees(Math.atan2(xy.getY(), xy.getX()));
    }

    public String getFaceDirection(double faceAngle){
        if(faceAngle>=22.5&&faceAngle<67.5){
            return "NE";
        }
        if(faceAngle>=67.5&&faceAngle<112.5){
            return "N";
        }
        if(faceAngle>=112.5&&faceAngle<157.5){
            return "NW";
        }
        if((faceAngle>=157.5&&faceAngle<=180)
                ||(faceAngle>=-180&&faceAngle<-157.5)){
            return "W";
        }
        if(faceAngle>=-157.5&&faceAngle<-112.5){
            return "SW";
        }
        if(faceAngle>=-112.5&&faceAngle<-67.5){
            return "S";
        }
        if(faceAngle>=-67.5&&faceAngle<-22.5){
            return "SE";
        }
        if((faceAngle>=-22.5&&faceAngle<=0)
            ||(faceAngle>=0&&faceAngle<22.5)){
            return "E";
        }
        return "up";
    }

    public double getSlabOnGradePriemeter() {
    		int previousPointIndex = 0;
    		int afterPointIndex = 0;
    		double[] lowerPoint = null;
    		double[] anotherPoint = null;

    		double curMinZ = NumUtil.MAX_VALUE;
    		for(int i=0; i<surfaceCoordinates.size(); i++) {
    			double[] coor = surfaceCoordinates.get(i);
    			if(coor[2] <= curMinZ) {
    				lowerPoint = coor;
    				curMinZ = coor[2];

    				if(i==0) {
    					previousPointIndex = surfaceCoordinates.size()-1;
    					afterPointIndex = i+1;
    				}else if(i==surfaceCoordinates.size()-1) {
    					previousPointIndex = i-1;
    					afterPointIndex = 0;
    				}else {
    					previousPointIndex = i-1;
    					afterPointIndex = i+1;
    				}
    			}
    		}
    		
    		if(surfaceCoordinates.get(previousPointIndex)[2] == minZ) {
    			anotherPoint = surfaceCoordinates.get(previousPointIndex);
    		}else if(surfaceCoordinates.get(afterPointIndex)[2] == minZ) {
    			anotherPoint = (surfaceCoordinates.get(afterPointIndex));
    		}else if(surfaceCoordinates.get(previousPointIndex)[2] > surfaceCoordinates.get(afterPointIndex)[2]) {
    			anotherPoint = surfaceCoordinates.get(previousPointIndex);
    		}else {
    			anotherPoint = (surfaceCoordinates.get(afterPointIndex));
    		}

    		return Math.sqrt((lowerPoint[0] - anotherPoint[0])* (lowerPoint[0] - anotherPoint[0]) +
    				(lowerPoint[1] - anotherPoint[1])*(lowerPoint[1] - anotherPoint[1]) +
    				(lowerPoint[2] - anotherPoint[2])*(lowerPoint[2] - anotherPoint[2]));
    }
    
    public void addHole(Surface hole){
        holes.add(hole);
        
        /*List<double[]> holeCoor = hole.getSurfaceCoordinates();
        for(int i=holeCoor.size()-1;i>-1;i--){
            surfaceCoordinates.add(holeCoor.get(i));
        }*/
    }
    
    public boolean isWallOnGround() {
    		return type.equals(SurfaceType.Wall) && isSlabOnGrade;
    }
    
    public List<Surface> getHoles(){
        return holes;
    }
 
    public List<Double> getLineCoordinates(){
        return this.lineCoordinates;
    }
    
    public List<double[]> getSurfaceCoordinates(){
        return this.surfaceCoordinates;
    }

    public void setZoneName(String zoneName){
        this.zoneName = zoneName;
    }
     
    public String getZoneName(){
        return this.zoneName;
    }
    
    public double getHighestPoint(){
        return this.maxZ;
    }
     
    public double getLowestPoint(){
        return this.minZ;
    }

    public void setName(String name){
        this.name = name;
    }
     
    public String getName(){
        return this.name;
    }
     
    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }
    
    public SurfaceType getSurfaceType(){
        return type;
    }

    public void setSurfaceType(SurfaceType type){
        this.type = type;
    }

    public static void main(String[] args){
        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");
        
        File idfFile = new File("/Users/weilixu/Desktop/absolute180.idf");
        IDFFileObject model = new IDFFileObject();
        IDFParser parser = new IDFParser();
        parser.parseIDFFromLocalMachine(idfFile, model);
        
        IDDParser iddParser = new IDDParser(model.getVersion());
        iddParser.validateIDF(model);
        
        List<IDFObject> bldgSurfaceDetailed = model.getCategoryList("BuildingSurface:Detailed");
        for(IDFObject obj : bldgSurfaceDetailed) {
        		String zoneName = obj.getDataByStandardComment("Zone Name");
        		IDFObject zone = model.getIDFObjectByName("Zone", zoneName);
        		Surface newSurface = new Surface(obj, zone);
        		System.out.println(newSurface.getName() + " : " + newSurface.getFaceDirection(newSurface.getFaceAngle(model)));
        }

    }
}