package main.java.model.geometry.scale;

import main.java.model.idf.IDFObject;

import java.util.List;

public class Window extends Polygon {
    private String name;
    private int multiplier = 1;
    private IDFObject idfObject;

    public Window(List<Coordinate3D> coords, String name) {
        super(coords);
        this.name = name;
    }

    public IDFObject getIDFObject() {
        return this.idfObject;
    }

    public void setIDFObject(IDFObject idfObject) {
        this.idfObject = idfObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double getArea() {
        return super.getArea() * multiplier;
    }
}
