package main.java.model.geometry.scale;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Floor extends Polygon{
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private int multiplier;
    private String zoneName;
    
    public Floor(List<Coordinate3D> points) {
        super(points);
        multiplier = 1;
    }
    
    public void setMultliplier(Integer multi) {
        multiplier = multi;
    }
    
    public Integer getMultiplier(){ return this.multiplier; }
    public String getZoneName(){ return this.zoneName; }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }
    
    public double getFloorArea() {
        return super.getArea() * multiplier;
    }
}
