package main.java.model.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import main.java.util.NumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class GeometryNewFromIDFFileObject {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    public static int total_surfaces = 0;
    private static boolean hole_normal = true;
    
    public JsonObject extractGeometry(IDFFileObject idfFileObj){
        JsonObject jo = new JsonObject();
        
        GeometryParser gp = new GeometryParser(idfFileObj);
        if(!gp.isParseSuccess()){
            jo.addProperty("error", gp.getParseErrorMsg());
            return jo;
        }
        
        ////////////////////////////////////////////////////////
        
        //consturct zone -> surfaces json object
        JsonObject geometry = new JsonObject();
        List<String> zones = gp.getZoneNames();
        List<IDFObject> surfaces = null;
        for(String zone : zones){
            surfaces = gp.getZoneSurfaces(zone);
            if(surfaces==null){
                LOG.warn("zone "+zone+" have no surfaces");
                continue;
            }
            
            JsonObject zoneJo = new JsonObject();
            for(IDFObject surface : surfaces){
                zoneJo.addProperty(surface.getName().trim(), "");
            }
            geometry.add("ZONE_"+zone.trim().toLowerCase(), zoneJo);
        }
        
        //calculating offset of building's coordinates for translate building to center of the view port
        /*
         * maxPoints:
         * maxX, maxY, maxZ, minX, minY, minZ
         */
        double[] maxPoints = adjustSurfaceAndFenestration(geometry, gp.getSurfaces());
        double translateX = -(maxPoints[0] + maxPoints[3])/2.0;
        double translateY = -(maxPoints[1] + maxPoints[4])/2.0;
        double translateZ = -(maxPoints[2] + maxPoints[5])/2.0;
        
        JsonObject offsets = new JsonObject();
        
        offsets.addProperty("X", translateX);
        offsets.addProperty("Y", translateY);
        offsets.addProperty("Z", translateZ);
        geometry.add("OFFSETS", offsets);
        
        ////////////////////////////////////////////////////////
        
        //for writing javascript
        StringBuilder jsBuilder = new StringBuilder();
        
        /*jsBuilder.append("var material = new THREE.LineBasicMaterial({color:0x000000});\n"  //0x52B5D4 -> blue wire
                        +"var vArray, cArray;\n"
                        +"var geometry, line;\n");*/
        jsBuilder.append("var material = new THREE.ShaderMaterial({\n"
                +"vertexShader: document.getElementById( 'vertexShader' ).textContent,\n"
                +"fragmentShader: document.getElementById( 'line_fragmentShader' ).textContent\n"
            +"});");
        
        //construct layer -> zones structure
        JsonObject zoneLayer = new JsonObject();
        
        Map<Integer, List<String>> heightZones = gp.getHeightZones();
        Map<String, List<Surface>> zoneSurfaces = gp.getZoneSurfaces();
        
        int layerCount = 0;
        int repeat;
        
        //construct datum - zones structure
        JsonArray multiplierLevels = new JsonArray();
        int lastLayerCeilingHeight = 0;
        List<Integer> heights = new ArrayList<>(heightZones.keySet());
        if(heights.get(0)<0){
            lastLayerCeilingHeight = heights.get(0);
        }
        for(int levelHeight: heights){
            zones = heightZones.get(levelHeight);
            
            //use a sample zone to get zone height which should equals to datum height
            List<Surface> surfaceList = zoneSurfaces.get(zones.get(0));
            double zoneHighest = Double.MIN_VALUE;
            double zoneLowest = NumUtil.MAX_VALUE;
            for(Surface sur: surfaceList){
                if(zoneHighest<sur.getHighestPoint()){
                    zoneHighest = sur.getHighestPoint();
                }
                if(zoneLowest>sur.getLowestPoint()){
                    zoneLowest = sur.getLowestPoint();
                }
            }
            
            //acquire height
            double zoneHeight = zoneHighest - zoneLowest;
            
            //All multiplier zones with multiplier numbers
            Map<String, Integer> multiplierZones = gp.getMultiplierZones();
            //Get multiplier number: either 1 - default value or the actual value
            if(multiplierZones.containsKey(zones.get(0))){
                repeat = multiplierZones.get(zones.get(0));
            }else {
                repeat = 1;
            }

            repeat = 1;
            if(repeat==1){
                //draw the level(s) listed in the idf file
                draw(zones, jsBuilder, zoneSurfaces, layerCount, zoneLayer, levelHeight, translateX, translateY, 0, 0, 0);
                layerCount++;
            }else {
                //round downNum to nearest int
                int downNum = (int)((levelHeight-lastLayerCeilingHeight)/zoneHeight + 0.5);
                int multiNum = 0-downNum;
                for(int i=downNum;i>0;i--,multiNum++){
                    multiplierLevels.add(layerCount);
                    draw(zones, jsBuilder, zoneSurfaces, layerCount, zoneLayer, levelHeight, translateX, translateY, zoneHeight, (int)zoneHeight, multiNum);
                    layerCount++;
                }
                
                //draw the level listed in the idf file
                draw(zones, jsBuilder, zoneSurfaces, layerCount, zoneLayer, levelHeight, translateX, translateY, 0, 0, 0);
                layerCount++;
                
                multiNum = 1;
                for(int i=downNum;i<repeat-1;i++,multiNum++){
                    multiplierLevels.add(layerCount);
                    draw(zones, jsBuilder, zoneSurfaces, layerCount, zoneLayer, levelHeight, translateX, translateY, zoneHeight, (int)zoneHeight, multiNum);
                    layerCount++;
                }
            }
            
            //record layer height for multiplier downward draw
            lastLayerCeilingHeight = (int)(levelHeight+zoneHeight*repeat);
        }
        geometry.add("DATUM_STRUCTS", zoneLayer);
        
        //for shading
        List<Surface> shadings = gp.getSurroundShading();
        if(!shadings.isEmpty()){
            zones.clear();
            zones.add("shading");
            
            zoneSurfaces.clear();
            zoneSurfaces.put("shading", shadings);
            
            JsonObject shadingLayer = new JsonObject();
            
            draw(zones, jsBuilder, zoneSurfaces, -1, shadingLayer, 0, translateX, translateY, 0, 0, 0);
            
            jsBuilder.append("hasShading = true;\n");
        }
    
        jo.addProperty("javascript", jsBuilder.toString());
        jo.add("geometry", geometry);

        if(multiplierLevels.size()>0) {
            jo.add("multipliers", multiplierLevels);
            jsBuilder.append("hasMultiplier = true;\n");
        }
        
        //LOG.debug("Total surfaces: "+total_surfaces);
        total_surfaces = 0;
        
        return jo;
    }
    
    private void updateMaxMinPoints(double[] res, Surface surface){
        if(res[0] < surface.getMaxX()){
            res[0] = surface.getMaxX();
        }
        if(res[1] < surface.getMaxY()){
            res[1] = surface.getMaxY();
        }
        if(res[2] < surface.getMaxZ()){
            res[2] = surface.getMaxZ();
        }
        if(res[3] > surface.getMinX()){
            res[3] = surface.getMinX();
        }
        if(res[4] > surface.getMinY()){
            res[4] = surface.getMinY();
        }
        if(res[5] > surface.getMinZ()){
            res[5] = surface.getMinZ();
        }
    }
    
    /**
     * surfaces list includes building surface and fenestration
     * @param geo
     * @param surfaces
     * @return
     */
    private double[] adjustSurfaceAndFenestration(JsonObject geo, List<Surface> surfaces){
        double[] res = new double[]{Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, 
                                    Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        
        
        for(Surface surface : surfaces){
            List<double[]> surfaceCoor = surface.getSurfaceCoordinates();
            
            JsonObject surfaceJO = new JsonObject();
            surfaceJO.addProperty("num", surfaceCoor.size());
            for(int i=0;i<surfaceCoor.size();i++){
                double[] coor = surfaceCoor.get(i);
                surfaceJO.addProperty("POINT_"+i, coor[0]+","+coor[1]+","+coor[2]);
            }
            
            updateMaxMinPoints(res, surface);
            
            List<Surface> holes = surface.getHoles();
            for(Surface hole : holes){
                List<double[]> holeCoor = hole.getSurfaceCoordinates();
                
                JsonObject holeJO = new JsonObject();
                holeJO.addProperty("num", holeCoor.size());
                
                if(hole_normal){
                    for(int i=0;i<holeCoor.size();i++){
                        double[] coor = holeCoor.get(i);
                        holeJO.addProperty("POINT_"+i, coor[0]+","+coor[1]+","+coor[2]);
                    }
                }else {
                    for(int i=holeCoor.size()-1;i>-1;i--){
                        double[] coor = holeCoor.get(i);
                        holeJO.addProperty("POINT_"+i, coor[0]+","+coor[1]+","+coor[2]);
                    }
                }
                
                
                geo.add("SURFACE_"+hole.getName().trim(), holeJO);
                geo.add("SURFACE_"+hole.getName().trim().toUpperCase(), holeJO);
            }
            geo.add("SURFACE_"+surface.getName().trim(), surfaceJO);
            geo.add("SURFACE_"+surface.getName().trim().toUpperCase(), surfaceJO);
        }
        
        return res;
    }
    
    private void draw(List<String> zones, 
                      StringBuilder jsBuilder, 
                      Map<String, List<Surface>> zoneSurfaces, 
                      int layerCount, 
                      JsonObject zoneLayer, 
                      double levelHeight, 
                      double translateX, 
                      double translateY, 
                      double transZ, 
                      int zoneHeight, 
                      int multiplierNum){
        
        StringBuilder resultString = new StringBuilder();
        StringBuilder vertexStringBuilder = new StringBuilder();
        StringBuilder colorStringBuilder = new StringBuilder();
        
        jsBuilder.append("var layer = new THREE.Object3D();\n"
                        +"layer.name = 'LEVEL_" + layerCount +"_surfaces';\n"
                        +"var lines = new THREE.Object3D();\n"
                        +"lines.name = 'LEVEL_" + layerCount +"_lines';\n"
                        //+"layer_surface_map[" + layerCount + "] = [];\n"
                        +"scene.add(layer);\n"
                        +"scene.add(lines);\n"
                        //+"var mergeGeometry_roof = new THREE.Geometry();\n"
                        //+"var mergeGeometry_surface = new THREE.Geometry();\n"
        );
        
        vertexStringBuilder.append("vArray = [");
        colorStringBuilder.append("cArray = [");
        
        //draw each level
        for(String zone: zones){
            generateLevelJS(vertexStringBuilder, 
                            colorStringBuilder, 
                            jsBuilder, 
                            zoneSurfaces.get(zone), 
                            translateX, 
                            translateY, 
                            transZ, 
                            multiplierNum,
                            layerCount,
                            zone);
            resultString.append(zone + "&");
        }
        resultString.append(layerCount);
        
        if(multiplierNum!=0){
            //zoneLayer.addProperty("DATUM_"+((int)datumLowestPoint + (multiplierNum * datumHeight)), "MULTI&" + layerCount);
            zoneLayer.addProperty("LEVEL_"+layerCount, "MULTI&" + layerCount);
        }else{
            //zoneLayer.addProperty("DATUM_"+(int)datumLowestPoint, resultString.toString());
            zoneLayer.addProperty("LEVEL_"+layerCount, resultString.toString());
        }
        
        vertexStringBuilder.setLength(vertexStringBuilder.length()-1);
        colorStringBuilder.setLength(colorStringBuilder.length()-1);
        vertexStringBuilder.append("];\n");
        colorStringBuilder.append("];\n");
        
        colorStringBuilder.append("line_geometry = new THREE.BufferGeometry();\n"
                                 +"line_geometry.addAttribute('position', new THREE.BufferAttribute(new Float32Array(vArray), 3));\n"
                                 +"line_geometry.addAttribute('color', new THREE.BufferAttribute(new Float32Array(cArray), 3));\n"
                                 +"line = THREE.Line(line_geometry, material, THREE.LinePieces);\n"  //LineSegments
                                 +"line.applyMatrix( new THREE.Matrix4().makeTranslation("+translateX+", "+translateY+", 0 ));\n"
                                 +"lines.add(line);"
                                 
                                 /*+"var mesh = new THREE.Mesh( mergeGeometry_roof, meshFaceMaterial );\n"
                                 +"mesh.applyMatrix( new THREE.Matrix4().makeTranslation("+translateX+", "+translateY+", 0 ));\n"
                                 +"mesh.castShadow = true;\n"
                                 +"mesh.receiveShadow = true;\n"
                                 +"mesh.name='merged_mesh_roof';\n"
                                 +"layer.add( mesh );\n"*/

                                 /*+"var mesh = new THREE.Mesh( mergeGeometry_surface, meshFaceMaterial );\n"
                                 +"mesh.applyMatrix( new THREE.Matrix4().makeTranslation("+translateX+", "+translateY+", 0 ));\n"
                                 +"mesh.castShadow = true;\n"
                                 +"mesh.receiveShadow = true;\n"
                                 +"mesh.name='merged_mesh_surface';\n"
                                 +"layer.add( mesh );\n"*/
                                 );

        jsBuilder.append(vertexStringBuilder.toString() + colorStringBuilder.toString());
    }
    
    private void generateLevelJS(StringBuilder vertexStringBuilder, 
                                 StringBuilder colorStringBuilder, 
                                 StringBuilder jsBuilder, 
                                 List<Surface> surfaces, 
                                 double transX,
                                 double transY,
                                 double transZ, 
                                 int multiplierNum,
                                 int layerCount,
                                 String zoneName){
        //LOG.info("JS: "+jsBuilder.length()+", Vertex: "+vertexStringBuilder.length()+", Color: "+colorStringBuilder.length()+", multiplier: "+multiplierNum+", zone: "+zoneName+", surface: "+surfaces.size());

        boolean isBuildingSurface = true;
        
        //to correctly draw multiplier layer
        double zOffset = multiplierNum*transZ;
        
        for(Surface surface : surfaces){
            if(surface == null){
                isBuildingSurface = false;
                continue;
            }else {
                total_surfaces += 1;
                
                // draw lines
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
                
                /*
                if(total_surfaces>-1){
                    continue;
                }
                //*/
                
                //draw surface
                switch(surface.getSurfaceType()){
                    case Floor:
                    case Wall:
                    case InnerWall:
                    case Ceiling:
                    case Roof:
                    case Shading:
                    case Overhang:
                    case Fin:
                        break;
                    default:
                        continue;  // don't draw window and door
                }
                
                jsBuilder.append("var geometry = new THREE.Geometry();\n");
                
                List<double[]> surCoor = surface.getSurfaceCoordinates();
                List<Surface> holes = surface.getHoles();
                
                double[] vv;
                for(int i=0;i<surCoor.size();i++){
                    vv = surCoor.get(i);
                    jsBuilder.append("geometry.vertices.push(new THREE.Vector3("+vv[0]+","+vv[1]+","+(vv[2]+zOffset)+"));\n");
                }
                for(Surface hole : holes){
                    List<double[]> vertexs = hole.getSurfaceCoordinates();
                    if(hole_normal){
                        for(int i=0;i<vertexs.size();i++){
                            double[] h = vertexs.get(i);
                            jsBuilder.append("geometry.vertices.push(new THREE.Vector3("+h[0]+","+h[1]+","+(h[2]+zOffset)+"));\n");
                        }
                    }else {
                        for(int i=vertexs.size()-1;i>-1;i--){
                            double[] h = vertexs.get(i);
                            jsBuilder.append("geometry.vertices.push(new THREE.Vector3("+h[0]+","+h[1]+","+(h[2]+zOffset)+"));\n");
                        }
                    }
                }
                
                Vector3 norm = null;
                
                double[] coor0 = surCoor.get(0),
                         coor1 = surCoor.get(1);
                Vector3 v1 = new Vector3(coor0[0]-coor1[0], coor0[1]-coor1[1], coor0[2]-coor1[2]);
                for(int i=2;i<surCoor.size();i++){
                    coor1 = surCoor.get(i);
                    Vector3 v2 = new Vector3(coor0[0]-coor1[0], coor0[1]-coor1[1], coor0[2]-coor1[2]);
                    
                    norm = v1.cross(v2);
                    norm.normalize();
                    
                    if(norm.isZeroVector()){
                        norm = null;
                        continue;
                    }
                    break;
                }
                
                if(norm == null){
                    LOG.error("Surface get norm vector failed");
                    continue;
                }
                
                boolean projectYZ = false, projectXZ = false;
                if(Math.abs(norm.getZ()) < Vector3.EPSILON){
                    //vertical surface
                    
                    if(Math.abs(Math.abs(norm.getX())-1) < Vector3.EPSILON){
                        //on YZ plan
                        projectYZ = true;
                    }else {
                        //not on YZ plan
                        projectXZ = true;
                    }
                }
                
                jsBuilder.append("var pts = [");
                StringBuilder holeIndex = new StringBuilder("var hIdx = [");
                
                int idx = surCoor.size();
                if(projectYZ){
                    for(double[] v : surCoor){
                        jsBuilder.append(v[1]+","+v[2]+", ");
                    }
                    
                    if(holes.isEmpty()){
                        holeIndex.append("];\n");
                    }else {
                        for(Surface hole : holes){
                            jsBuilder.append(" ");
                            holeIndex.append(idx+", ");
                            
                            List<double[]> vertexs = hole.getSurfaceCoordinates();
                            
                            if(hole_normal){
                                for(int i=0;i<vertexs.size();i++){
                                    double[] h = vertexs.get(i);
                                    jsBuilder.append(h[1]+","+h[2]+", ");
                                    idx++;
                                }
                            }else {
                                for(int i=vertexs.size()-1;i>-1;i--){
                                    double[] h = vertexs.get(i);
                                    jsBuilder.append(h[1]+","+h[2]+", ");
                                    idx++;
                                }
                            }
                        }
                        
                        holeIndex.setLength(holeIndex.length()-2);
                        holeIndex.append("];\n");
                    }
                }else if(projectXZ){
                    for(double[] v : surCoor){
                        jsBuilder.append(v[0]+","+v[2]+", ");
                    }
                    
                    if(holes.isEmpty()){
                        holeIndex.append("];\n");
                    }else {
                        for(Surface hole : holes){
                            jsBuilder.append(" ");
                            holeIndex.append(idx+", ");
                            
                            List<double[]> vertexs = hole.getSurfaceCoordinates();
                            
                            if(hole_normal){
                                for(int i=0;i<vertexs.size();i++){
                                    double[] h = vertexs.get(i);
                                    jsBuilder.append(h[0]+","+h[2]+", ");
                                    idx++;
                                }
                            }else {
                                for(int i=vertexs.size()-1;i>-1;i--){
                                    double[] h = vertexs.get(i);
                                    jsBuilder.append(h[0]+","+h[2]+", ");
                                    idx++;
                                }
                            }
                        }
                        
                        holeIndex.setLength(holeIndex.length()-2);
                        holeIndex.append("];\n");
                    }
                }else {
                    for(double[] v : surCoor){
                        jsBuilder.append(v[0]+","+v[1]+","+v[2]+", ");
                    }
                    
                    if(holes.isEmpty()){
                        holeIndex.append("];\n");
                    }else {
                        for(Surface hole : holes){
                            jsBuilder.append(" ");
                            holeIndex.append(idx+", ");
                            
                            List<double[]> vertexs = hole.getSurfaceCoordinates();
                            
                            if(hole_normal){
                                for(int i=0;i<vertexs.size();i++){
                                    double[] h = vertexs.get(i);
                                    jsBuilder.append(h[0]+","+h[1]+","+h[2]+", ");
                                    idx++;
                                }
                            }else {
                                for(int i=vertexs.size()-1;i>-1;i--){
                                    double[] h = vertexs.get(i);
                                    jsBuilder.append(h[0]+","+h[1]+","+h[2]+", ");
                                    idx++;
                                }
                            }
                        }
                        
                        holeIndex.setLength(holeIndex.length()-2);
                        holeIndex.append("];\n");
                    }
                }
                jsBuilder.setLength(jsBuilder.length()-2);
                jsBuilder.append("];\n");
                jsBuilder.append(holeIndex.toString());
                
                int dim = (projectYZ || projectXZ) ? 2 : 3;
                jsBuilder.append(""
                        +"var triangles = earcut(pts, hIdx, "+dim+");\n"
                        +"for(var i=0;i<triangles.length;i+=3){\n"
                            +"var face = new THREE.Face3(triangles[i], triangles[i+1], triangles[i+2]);\n"
                            +"geometry.faces.push(face);\n"
                        +"}\n"
                        +"geometry.computeFaceNormals();\n"
                        +"geometry.computeVertexNormals();\n");
                if(surface.getSurfaceType()==SurfaceType.Roof
                        || surface.getSurfaceType()==SurfaceType.Ceiling){
                    jsBuilder.append("geometry.name='Roof';\n"
                            +"geometry.surface_name = '"+surface.getName()+"';\n");
                    //jsBuilder.append("mergeGeometry_roof.merge(geometry, geometry.matrix, "+materialIdx+");\n");
                    //jsBuilder.append("mergeGeometry_roof.merge(geometry, geometry.matrix, 0);\n");

                    jsBuilder.append("var mesh = new THREE.Mesh( geometry, meshFaceMaterial );\n"
                            +"mesh.applyMatrix( new THREE.Matrix4().makeTranslation("+transX+", "+transY+", 0 ));\n"
                            +"mesh.castShadow = true;\n"
                            +"mesh.receiveShadow = true;\n"
                            //+"mesh.name='merged_mesh_surface';\n"
                            +"mesh.zone_name = '"+zoneName+"';\n"
                            +"mesh.surface_name = '"+surface.getName()+"';\n"
                            +"mesh.name = 'ceil_roof';\n"
                            +"layer.add( mesh );\n");
                }else {
                    jsBuilder.append("geometry.name='Surface';\n");
                    jsBuilder.append("var mesh = new THREE.Mesh( geometry, meshFaceMaterial );\n"
                            +"mesh.applyMatrix( new THREE.Matrix4().makeTranslation("+transX+", "+transY+", 0 ));\n"
                            +"mesh.castShadow = true;\n"
                            +"mesh.receiveShadow = true;\n"
                            //+"mesh.name='merged_mesh_surface';\n"
                            +"mesh.zone_name = '"+zoneName+"';\n"
                            +"mesh.surface_name = '"+surface.getName()+"';\n"
                            +"mesh.name = 'surface';\n"
                            +"layer.add( mesh );\n");
                    //jsBuilder.append("mergeGeometry_surface.merge(geometry, geometry.matrix, "+materialIdx+");\n");
                    //jsBuilder.append("mergeGeometry_surface.merge(geometry, geometry.matrix, 0);\n");
                    //jsBuilder.append("layer_surface_map[" + layerCount + "].push(geometry);\n");
                }
            }
        }
    }
}
