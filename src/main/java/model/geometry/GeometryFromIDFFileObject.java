package main.java.model.geometry;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.util.NumUtil;

public class GeometryFromIDFFileObject{
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    public JsonObject extractGeometry(IDFFileObject idfFileObj){
        JsonObject jo = new JsonObject();
        
        //for writing javascript
        StringBuilder jsBuilder = new StringBuilder();
        
        JsonObject geometry = new JsonObject();
        
        //Construct datum Object, for old three.js
        /*jsBuilder.append("var vArray;\n"
                       + "var cArray;\n"
                       + "THREE.Datum = function(vertexArray, colorArray){\n\n"
                                   + "var parent = null;\n"
                                + "var vertices = new Float32Array(vertexArray);\n"
                                + "var colors = new Float32Array(colorArray);\n"
                                + "var geometry = new THREE.BufferGeometry();\n"
                                    + "geometry.addAttribute('position', new THREE.BufferAttribute(vertices, 3));\n"
                                    + "geometry.addAttribute('color', new THREE.BufferAttribute(colors, 3));\n"
                                + "var material = new THREE.LineBasicMaterial({color:0x52B5D4});\n"
                                + "THREE.Line.call(this, geometry, material, THREE.LinePieces);\n"
                                + "this.hide = function(){\n"
                                    + "this.visible = false;\n"
                                + "};\n"
                                + "this.show = function(){\n"
                                    + "this.visible = true;\n"
                                + "}\n"
                        + "};\n"
                        + "THREE.Datum.prototype = Object.create( THREE.Line.prototype );\n"
                        + "THREE.Datum.prototype.constructor = THREE.Datum;\n");*/
        jsBuilder.append("var lines = new THREE.Object3D();\n"
                        +"scene.add(lines);\n"
                        +"var material = new THREE.LineBasicMaterial({color:0x52B5D4});\n"
                        +"var vArray, cArray;\n"
                        +"var geometry, line;\n");
        
        GeometryParser gp = new GeometryParser(idfFileObj);
        
        //construct layer -> zones structure
        JsonObject zoneLayer = new JsonObject();
        
        /*
         * This collection contains:
         * A sorted zone - surface + null pointer + fenestration - coordinates
         * A hashmap contains lowest point - zone list pairs
         * 
         */
        SortedCollection sortedCollection = gp.getSortedCollection();
        
        //reconsturct zone -> surfaces structure - legacy issue
        List<String> zones = gp.getZoneNames();
        List<IDFObject> surfaces;
        for(String zone : zones){
            JsonObject zoneJo = new JsonObject();
            surfaces = gp.getZoneSurfaces(zone);
            if(surfaces==null){
                LOG.warn("zone "+zone+" have no surfaces");
                continue;
            }
            for(IDFObject surface : surfaces){
                zoneJo.addProperty(surface.getName().trim(), "");
            }
            geometry.add("ZONE_"+zone.trim().toLowerCase(), zoneJo);
        }
        
        //calculating offset of building's coordinates for translate building to center of the view port
        double[] maxPoint = generateJson(geometry, sortedCollection.getSurfaces(), sortedCollection.getSurfaceToZone());
        double translateX = -(maxPoint[0] + maxPoint[3])/(double)2;
        double translateY = -(maxPoint[1] + maxPoint[4])/(double)2;
        double translateZ = -(maxPoint[2] + maxPoint[5])/(double)2;
        
        JsonObject offsets = new JsonObject();
        offsets.addProperty("X", translateX);
        offsets.addProperty("Y", translateY);
        offsets.addProperty("Z", translateZ);
        geometry.add("OFFSETS", offsets);
        
        //construct and draw datum
        Map<String, List<Surface>> building = sortedCollection.getZoneToSurface();
        Map<Double, List<String>> sortedDatum = sortedCollection.getZoneLowestPoints();
        Set<Double> datumLowestPoints = sortedDatum.keySet();
        int layerCount = 0;
        int repeat = 0;
        double buildingHeight = 0;
        
        //construct datum - zones structure
        for(double datumLowestPoint: datumLowestPoints){
            //recycled arraylist zone from previous code segment
            zones = sortedDatum.get(datumLowestPoint);
            
            //use a sample zone to get zone height which should equals to datum height
            List<IDFObject> sampleZoneSurfaces = gp.getZoneSurfaces(zones.get(0));
            boolean firstCheck = true;
            double zoneHighest = 0;
            double zoneLowest = 0;
            for(IDFObject statement: sampleZoneSurfaces){
                int numberOfVertex =  (statement.getObjLen()-1-10)/3;  //exclude object label line, vertex coordinates start from 10, extensible till end
                for(int i = 0; i < numberOfVertex; i++){
                    double tempZoneZ = NumUtil.readDouble(statement.getIndexedData(12 + i*3), 0.0);
                    if(firstCheck){
                        zoneHighest = tempZoneZ;
                        zoneLowest = tempZoneZ;
                        firstCheck = false;
                    }
                    else{
                        if(zoneHighest < tempZoneZ){
                            zoneHighest = tempZoneZ;
                        }
                        if(zoneLowest > tempZoneZ){
                            zoneLowest = tempZoneZ;
                        }
                    }
                }
            }
            
            //acquire height
            double datumHeight = zoneHighest - zoneLowest;
            
            //All multipliered zones with multiplier numbers
            Map<String, Integer> multiplierZones = gp.getMultiplierZones();
            //Get mulitplier number: either 1 - default value or the actual value
            if(multiplierZones.containsKey(zones.get(0))){
                repeat = multiplierZones.get(zones.get(0));
            }
            else{
                repeat = 1;
            }
            
            //There is a multiplier level below
            if((int)Math.round(datumLowestPoint) > (int)Math.round(buildingHeight)){
                //draw the datum listed in the idf file
                draw(zones, jsBuilder, building,layerCount, zoneLayer, datumLowestPoint, translateX, translateY, false, 0, 0, 0);
                buildingHeight = buildingHeight + datumHeight;
                layerCount++;
                repeat--;
                //building upwards NOTE: transZ is the datum height
                for(int i = 0; i < repeat; i++){
                    draw(zones, jsBuilder, building,layerCount, zoneLayer, datumLowestPoint, translateX, translateY, true, datumHeight, (int)datumHeight, (i+1));
                    buildingHeight = buildingHeight + datumHeight;
                    layerCount++;
                }
            }
            //Building upwards
            else{
                //first pass always draw the datum listed in the idf file
                draw(zones, jsBuilder, building,layerCount, zoneLayer, datumLowestPoint, translateX, translateY, false, 0, 0, 0);
                buildingHeight = buildingHeight + datumHeight;
                layerCount++;
                repeat--;
                //building upwards
                for(int i = 0; i < repeat; i++){
                    draw(zones, jsBuilder, building,layerCount, zoneLayer, datumLowestPoint, translateX, translateY, true, datumHeight, (int)datumHeight, (i+1));
                    buildingHeight = buildingHeight + datumHeight;
                    layerCount++;
                }
            }
        }
        geometry.add("DATUM_STRUCTS", zoneLayer);
        
        jsBuilder.append("");
    
        jo.addProperty("javascript", jsBuilder.toString());
        jo.add("geometry", geometry);
        
        return jo;
    }

    private double[] generateJson(JsonObject jo, Map<String, IDFObject> surfaces, Map<String, IDFObject> surfaceToZone){
        int len;
        IDFObject surface;
        Set<String> keys = surfaces.keySet();
        
        double valueX, valueY, valueZ;
        double maxX=0, maxY=0, maxZ=0, minX = 0, minY = 0, minZ = 0;
        
        for(String key : keys){
            surface = surfaces.get(key);
            String surfaceName = surface.getName();
            
            IDFObject zone = surfaceToZone.get(surfaceName);
            if(zone==null){
                LOG.error("Surface "+surfaceName+"'s zone cannot be found.");
                continue;
            }
            
            double rotate = NumUtil.readDouble(zone.getIndexedData(1), 0.0);
            double radian = 0 - rotate*Math.PI/180.0;
            
            double cosR = Math.cos(radian);
            double sinR = Math.sin(radian);
            
            double zoneX = NumUtil.readDouble(zone.getIndexedData(2), 0.0);
            double zoneY = NumUtil.readDouble(zone.getIndexedData(3), 0.0);
            double zoneZ = NumUtil.readDouble(zone.getIndexedData(4), 0.0);
            
            len = surface.getObjLen()-1;  //exclude object label line
            
            JsonObject surfaceJO = new JsonObject();
            surfaceJO.addProperty("num", (len-10)/3); //vertex coordinates start from row 10, extensible till the end
            
            for(int j=10,i=0;j<len;j+=3,i++){
                double surX = NumUtil.readDouble(surface.getIndexedData(j), 0.0);
                double surY = NumUtil.readDouble(surface.getIndexedData(j+1), 0.0);
                double surZ = NumUtil.readDouble(surface.getIndexedData(j+2), 0.0);
                
                valueX = surX*cosR - surY*sinR + zoneX;
                valueY = surX*sinR + surY*cosR + zoneY;
                valueZ = surZ + zoneZ;
                
                surfaceJO.addProperty("POINT_"+i, valueX+","+valueY+","+valueZ);
    
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
            }
            
            jo.add("SURFACE_"+key.trim(), surfaceJO);
        }
        
        return new double[]{maxX, maxY, maxZ, minX, minY, minZ};
    }
    
    private void draw(List<String> zones, 
            StringBuilder jsBuilder, 
            Map<String, List<Surface>> building, 
            int layerCount, 
            JsonObject zoneLayer, 
            double datumLowestPoint, 
            double translateX, 
            double translateY, 
            boolean multiplier, 
            double transZ, 
            int datumHeight, 
            int multiplierNum){
        
        StringBuilder resultString = new StringBuilder();
        StringBuilder vertexStringBuilder = new StringBuilder();
        StringBuilder colorStringBuilder = new StringBuilder();
        
        vertexStringBuilder.append("vArray = [");
        colorStringBuilder.append("cArray = [");
        
        //draw each datum
        if(multiplier){
            for(String zone: zones){
                generateMultiplierJS(vertexStringBuilder, colorStringBuilder, jsBuilder, building.get(zone), transZ, multiplierNum);
                resultString.append(zone + "&");
            }
        }
        else{
            for(String zone: zones){
                generateJS(vertexStringBuilder, colorStringBuilder, jsBuilder, building.get(zone));
                resultString.append(zone + "&");
            }
        }
        resultString.append(layerCount);
        if(multiplier){
            //zoneLayer.addProperty("DATUM_"+((int)datumLowestPoint + (multiplierNum * datumHeight)), "MULTI&" + layerCount);
            zoneLayer.addProperty("DATUM_"+layerCount, "MULTI&" + layerCount);
        }else{
            //zoneLayer.addProperty("DATUM_"+(int)datumLowestPoint, resultString.toString());
            zoneLayer.addProperty("DATUM_"+layerCount, resultString.toString());
        }
        
        vertexStringBuilder.setLength(vertexStringBuilder.length()-1);
        colorStringBuilder.setLength(colorStringBuilder.length()-1);
        vertexStringBuilder.append("];\n");
        colorStringBuilder.append("];\n");
        
        //for old three.js
        /* int datumIdx = (int)datumLowestPoint;
        if(multiplier){
            datumIdx += ((int)datumLowestPoint + (multiplierNum * datumHeight));
        }
        String datumIdxStr = null;
        if(datumIdx>-1){
            datumIdxStr = String.valueOf(datumIdx);
        }else {
            datumIdxStr = "_"+String.valueOf(datumIdx * -1);
        }
        
        colorStringBuilder.append("var datum" + datumIdxStr + " = new THREE.Datum(vArray, cArray);\n"
                + "datum" + datumIdxStr + ".applyMatrix( new THREE.Matrix4().makeTranslation("+translateX+", "+translateY+", 0 ));\n"
                + "datum" + datumIdxStr + ".name = 'DATUM_" + layerCount +"';\n"
                + "scene.add(datum" + datumIdxStr + ");\n");*/
        
        colorStringBuilder.append("geometry = new THREE.BufferGeometry();\n"
                                 +"geometry.addAttribute('position', new THREE.BufferAttribute(new Float32Array(vArray), 3));\n"
                                 +"geometry.addAttribute('color', new THREE.BufferAttribute(new Float32Array(cArray), 3));\n"
                                 +"line = THREE.Line(geometry, material, THREE.LinePieces);\n"
                                 +"line.applyMatrix( new THREE.Matrix4().makeTranslation("+translateX+", "+translateY+", 0 ));\n"
                                 +"line.name = 'DATUM_" + layerCount +"';\n"
                                 +"lines.add(line);");

        jsBuilder.append(vertexStringBuilder.toString() + colorStringBuilder.toString());
    }
    
    private void generateJS(StringBuilder vertexStringBuilder, 
            StringBuilder colorStringBuilder, 
            StringBuilder jsBuilder, 
            List<Surface> surfaces){
        
        boolean isBuildingSurface = true;
        for(Surface surface : surfaces){
            if(surface == null){
                isBuildingSurface = false;
                continue;
            }
            else{
                List<Double> coordinates = surface.getLineCoordinates();
                int vertexCount = 0;
                for(double coor : coordinates){
                    vertexStringBuilder.append(coor + ",");
                    if(vertexCount == 2){
                        if(isBuildingSurface){
                            colorStringBuilder.append("0,0,1,");
                        }
                        else{
                            colorStringBuilder.append("0,1,0,");
                        }
                    }
                    vertexCount++;
                    if(vertexCount == 3){
                        vertexCount = 0;
                    }
                }
            }
        }
    }
    
    private void generateMultiplierJS(StringBuilder vertexStringBuilder, 
                                      StringBuilder colorStringBuilder, 
                                      StringBuilder jsBuilder, 
                                      List<Surface> surfaces, 
                                      double transZ, 
                                      int multiplierNum){
        boolean isBuildingSurface = true;
        for(Surface surface : surfaces){
            if(surface == null){
                isBuildingSurface = false;
                continue;
            }
            else{
                List<Double> coordinates = surface.getLineCoordinates();
                int zCoorCounter = 1;
                int vertexCount = 0;
                double newCoor = 0;
                for(double coor : coordinates){
                    //every third coordinate is need to be changed
                    if((zCoorCounter % 3 == 0) && (zCoorCounter != 0)){                        
                        newCoor = coor+(transZ*multiplierNum);
                    }
                    else{
                        newCoor = coor;
                    }
                    zCoorCounter++;
                    
                    vertexStringBuilder.append(newCoor + ",");
                    if(vertexCount == 2){
                        if(isBuildingSurface){
                            colorStringBuilder.append("0.82,0.82,0.82,");
                        }
                        else{
                            colorStringBuilder.append("0.7,0.7,0.7,");
                        }
                    }
                    vertexCount++;
                    if(vertexCount == 3){
                        vertexCount = 0;
                    }
                }
            }
        }
    }
}
