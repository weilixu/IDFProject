package main.java.model.geometry.scale;

public class Line3D {
	private Coordinate3D end1;
	private Coordinate3D end2;
	private double len;
	
	public Line3D(Coordinate3D start, Coordinate3D end){
		this.end1 = start;
		this.end2 = end;
		this.len = this.computeLength();
	}

	public Coordinate3D getStart() {
		return end1;
	}

	public void setStart(Coordinate3D end1) {
		this.end1 = end1;
	}

	public Coordinate3D getEnd() {
		return end2;
	}

	public void setEnd(Coordinate3D end2) {
		this.end2 = end2;
	}

	public double getLen() {
		return len;
	}
	
	private double computeLength(){
		double len = 0;
		len += Math.pow(end1.getX() - this.end2.getX(), 2);
		len += Math.pow(end1.getY() - this.end2.getY(), 2);
		len += Math.pow(end1.getZ() - this.end2.getZ(), 2);
		return Math.sqrt(len);
	}

	public Coordinate3D getMiddlePoint(){
		Coordinate3D mid = end1.duplicate();
		mid.add(end2);
		mid.scale(0.5);
		return mid;
	}
}
