package main.java.model.geometry;

import java.util.ArrayList;
import java.util.List;

import main.java.model.idf.IDFObject;
import main.java.util.NumUtil;

public class XYSurface {
    private List<double[]> surfaceCoordinates = new ArrayList<>();
    
    public boolean buildXYSurface(IDFObject surfaceObj){
        int len = surfaceObj.getObjLen()-1;
        
        int numOfVertex = surfaceObj.getStandardCommentIndex("Number of Vertices");
        Double surfaceZ = null;
        for(int j=numOfVertex+1;j<len;j+=3){
            double surX = NumUtil.readDouble(surfaceObj.getIndexedData(j), 0.0);
            double surY = NumUtil.readDouble(surfaceObj.getIndexedData(j+1), 0.0);
            Double surZ = NumUtil.readDouble(surfaceObj.getIndexedData(j+2), 0.0);
            
            if(surfaceZ!=null && surfaceZ.compareTo(surZ)!=0){
                return false;
            }
            
            if(surfaceZ==null){
                surfaceZ = surZ;
            }
            
            surfaceCoordinates.add(new double[]{surX, surY});
        }
        return true;
    }
    
    public double calArea(){
        int i,j;
        double area = 0; 
        
        int points = surfaceCoordinates.size();
        for (i=0; i < points; i++) {
            j = (i + 1) % points;

            area += surfaceCoordinates.get(i)[0] * surfaceCoordinates.get(j)[1];
            area -= surfaceCoordinates.get(i)[1] * surfaceCoordinates.get(j)[0];
        }

        area /= 2;
        return (area < 0 ? -area : area);
    }
}
