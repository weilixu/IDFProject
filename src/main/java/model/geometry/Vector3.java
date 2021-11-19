package main.java.model.geometry;

public class Vector3 {
    public static final double EPSILON = 0.001D;
    private double x, y, z;
    
    public Vector3(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3 cross(Vector3 v){
        
        double vx = v.getX(), 
               vy = v.getY(), 
               vz = v.getZ();

        return new Vector3(y*vz - z*vy,
                           z*vx - x*vz,
                           x*vy - y*vx);
    }
    
    public void normalize(){
        double len = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        
        x /= len;
        y /= len;
        z /= len;
    }
    
    public boolean isZeroVector(){
        return !(Math.abs(x)>EPSILON 
                || Math.abs(y)>EPSILON 
                || Math.abs(z)>EPSILON);
    }
}
