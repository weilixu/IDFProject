package main.java.model.geometry.scale;

import main.java.model.idf.IDFObject;
import main.java.util.NumUtil;

import java.util.ArrayList;
import java.util.List;

public class Polygon {
	public static final int SCALE_SUCCESS = 0;
	public static final int SCALE_POLYGON_INVALID = -1;
	public static final int SCALE_RATIO_INVALID = -2;
	
	private static final double NORMAL_SCALE = 100;
	
	public static final double CONTAIN_TEST_EPSILON = 0.0001;
	
	private List<Coordinate3D> coords;
	
	private List<Triangle> triangles;
	
	private double area;
	private boolean isValid;
	private int numPoints;

	public Polygon(IDFObject surfaceObj){
        int len = surfaceObj.getObjLen()-1;  //exclude object label line

        List<Coordinate3D> coords = new ArrayList<>();
        //vertex coordinates start after number of vertices, extensible till the end
        int numOfVertex = surfaceObj.getStandardCommentIndex("Number of Vertices");
        for(int j=numOfVertex+1;j<len;j+=3) {
            double surX = NumUtil.readDouble(surfaceObj.getIndexedData(j), 0.0);
            double surY = NumUtil.readDouble(surfaceObj.getIndexedData(j + 1), 0.0);
            double surZ = NumUtil.readDouble(surfaceObj.getIndexedData(j + 2), 0.0);

            Coordinate3D coord = new Coordinate3D(surX, surY, surZ);
            coords.add(coord);
        }

        init(coords);
	}
	
	/**
	 * Follow the sequence of the coordinates, 
	 * the polygon should not be self-intersected
	 * @param coords
	 */
	public Polygon(List<Coordinate3D> coords){
		init(coords);
	}

	private void init(List<Coordinate3D> coords){
        this.numPoints = coords.size();
        if(this.numPoints<3){
            this.isValid = false;
            this.coords = null;
            //this.triangles = null;
            this.area = 0;
        }else {
            this.isValid = true;
            this.coords = coords;

            //this.buildTriangles();
            this.area = this.computeArea();
        }
    }

	public Coordinate3D massCenter(){
		Coordinate3D center = new Coordinate3D();
		for(Coordinate3D corner : coords){
		    center.add(corner);
        }
        ScaleUtility.scale(center, 1D/coords.size());
        return center;
	}
	
	public int getNumPoints(){
		return this.numPoints;
	}
	
	public List<Coordinate3D> getCoords() {
		return coords;
	}

	public double getArea() {
		return area;
	}

	public boolean isValid() {
		return isValid;
	}

	/**
	 * Assume the polygon is convex
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	private void buildTriangles(){
		this.triangles = new ArrayList<Triangle>(coords.size()-2);
		
		Coordinate3D top = coords.get(0);
		for(int i=2;i<coords.size();i++){
			Coordinate3D p2 = coords.get(i-1);
			Coordinate3D p3 = coords.get(i);
			Triangle tri = new Triangle(top, p2, p3);
			triangles.add(tri);
			if(!tri.isValid()){
				coords = null;
				triangles = null;
				isValid = false;
				break;
			}
		}
	}
	
	/**
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	private double computeArea_sum_triangle(){
		if(!isValid){
			return 0;
		}
		
		double area = 0;
		for(Triangle tri : triangles){
			if(tri.isValid()){
				area += tri.getArea();
			}else {
				area = 0;
				break;
			}
		}
		return area;
	}
	
	/**
	 * Polygon is not self-intersected
	 * @return
	 */
	private double computeArea(){
		if(!isValid){
			return 0;
		}
		
		Coordinate3D p1, p2;
		Coordinate3D sum = new Coordinate3D();
		for(int i=0;i<numPoints;i++){
			p1 = coords.get(i);
			if(i<numPoints-1){
				p2 = coords.get(i+1);
			}else {
				p2 = coords.get(0);
			}
			
			Coordinate3D cross = ScaleUtility.cross(p1, p2);
			sum.setX(sum.getX() + cross.getX());
			sum.setY(sum.getY() + cross.getY());
			sum.setZ(sum.getZ() + cross.getZ());
		}
		
		Coordinate3D normal = this.getNorm();
		ScaleUtility.normalize(normal);
		
		double area = ScaleUtility.dot(sum, normal);
		return Math.abs(area/2);
	}
	
	/**
	 * Origin at first point in coords
	 * @return
	 */
	private Coordinate3D getNorm(){
		if(isValid){
			Coordinate3D p1 = this.coords.get(0);
			Coordinate3D p2 = this.coords.get(1);
			Coordinate3D p3 = this.coords.get(2);
			
			Coordinate3D vector21 = ScaleUtility.makeVector(p1, p2);
			Coordinate3D vector31 = ScaleUtility.makeVector(p1, p3);
			
			return ScaleUtility.cross(vector21, vector31);
		}
		
		return new Coordinate3D(); //origin
	}
	
	/**
	 * areaScaleRatio must be great than or equal to 0<br/>
	 * areaScaleRatio<1 and >0 => shrink<br/>
	 * areaScaleRatio>1 => enlarge<br/>
	 * 
	 * @param areaScaleRatio
	 * @return
	 */
	public int scale(double areaScaleRatio){
		if(!isValid){
			return Polygon.SCALE_POLYGON_INVALID;
		}
		if(areaScaleRatio < 0){
			return Polygon.SCALE_RATIO_INVALID;
		}
		
		double edgeScaleRatio = Math.sqrt(areaScaleRatio);
		double vectorScaleRatio = edgeScaleRatio-1;
		
		//Coordinate3D top = this.coords.get(0);
        Coordinate3D center = massCenter();
		
		Coordinate3D vectorNormal = this.getNorm();
		ScaleUtility.normalize(vectorNormal);
		ScaleUtility.scale(vectorNormal, Polygon.NORMAL_SCALE);
		
		Coordinate3D shiftedCenter = ScaleUtility.pointVectorAdd(center, vectorNormal);
		Coordinate3D vectorNomralShiftback = vectorNormal.duplicate();
		ScaleUtility.scale(vectorNomralShiftback, vectorScaleRatio);

		for(int i=0;i<coords.size();i++){
			Coordinate3D p = coords.get(i);
			
			Coordinate3D vectorShift = ScaleUtility.makeVector(shiftedCenter, p);
			ScaleUtility.scale(vectorShift, vectorScaleRatio);
			
			Coordinate3D shiftedP = ScaleUtility.pointVectorAdd(p, vectorShift);
			Coordinate3D scaledP = ScaleUtility.pointVectorAdd(shiftedP, vectorNomralShiftback);
			
			coords.set(i, scaledP);
		}
		
		// update triangles
		//this.buildTriangles();
		
		// update area
		this.area = this.computeArea();
		
		return Polygon.SCALE_SUCCESS;
	}

	public Polygon extratPolygon(){
		List<Coordinate3D> coors = new ArrayList<>();
		for(Coordinate3D coor : this.coords){
			coors.add(coor.duplicate());
		}

		Polygon copy = new Polygon(coors);
		return copy;
	}

    /**
     * Polygon should be convex, not work for concave
     * @param point
     * @return
     */
	public boolean containsPointConvexPolygon(Coordinate3D point){
		double angleSum = 0;
		for(int i=0,j=coords.size()-1;i<coords.size();j=i,i++){
			Coordinate3D v1 = coords.get(i);
			Coordinate3D v2 = coords.get(j);
			
			Coordinate3D pv1Vector = ScaleUtility.makeVector(point, v1);
			Coordinate3D pv2Vector = ScaleUtility.makeVector(point, v2);
			
			double pv1Len = pv1Vector.vectorLen();
			double pv2Len = pv2Vector.vectorLen();
			
			double lenTimes = pv1Len * pv2Len;
			if(lenTimes <= Polygon.CONTAIN_TEST_EPSILON){
				return true; //point is on polygon's vertex, consider inside
			}
			
			double cosAngle = ScaleUtility.dot(pv1Vector, pv2Vector) / lenTimes;
			angleSum += Math.acos(cosAngle);
		}
		
		return Math.abs(2*Math.PI-angleSum) < Polygon.CONTAIN_TEST_EPSILON;
	}

    /**
     * works for any polygon that all the z is the same
     * @return
     */
    public boolean containsPointFlatPolygon(Coordinate3D test1, Coordinate3D test2){
        List<Line2D> twoDs = new ArrayList<>();
        for(int i=0;i<coords.size();i++){
            Coordinate3D c1 = coords.get(i);
            Coordinate3D c2 = coords.get((i+1)%coords.size());
            twoDs.add(new Line2D(new Coordinate2D(c1.getX(), c1.getY()),
                    new Coordinate2D(c2.getX(), c2.getY())));
        }

        Line2D test = new Line2D(new Coordinate2D(test1.getX(), test1.getY()),
                new Coordinate2D(test2.getX(), test2.getY()));

        int intersections = 0;
        int endOnLines = 0;
        for(Line2D line2D : twoDs){
            int flag = line2D.isIntersect(test);
            if(flag==1){
                intersections++;
            }else if(flag==0){
                endOnLines++;
            }
        }
        intersections += endOnLines/2;

        return intersections%2==1;
    }

    /**
     * Polygon must be canvass polygon
     */
    public List<Polygon> triangulize(){
        List<Polygon> res = new ArrayList<>();

        if(this.numPoints==3){
            res.add(this);
        }else {
            for(int startIdx = 1;startIdx+1<this.numPoints;startIdx++){
                List<Coordinate3D> triCoords = new ArrayList<>();
                triCoords.add(this.coords.get(0));
                triCoords.set(1, this.coords.get(startIdx));
                triCoords.set(2, this.coords.get(startIdx+1));
                Polygon tri = new Polygon(triCoords);
                res.add(tri);
            }
        }

        return res;
    }
}
