package main.java.model.geometry.scale;

import main.java.model.idf.IDFObject;

import java.util.List;

public class Door extends Polygon{
	private int multiplier = 1;
	private IDFObject idfObject;

	public Door(List<Coordinate3D> coords) {
		super(coords);
	}

    public IDFObject getIDFObject() {
        return this.idfObject;
    }

    public void setIDFObject(IDFObject idfObject) {
        this.idfObject = idfObject;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    public int getMultiplier() {
        return this.multiplier;
    }
}
