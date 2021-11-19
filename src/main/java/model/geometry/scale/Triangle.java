package main.java.model.geometry.scale;

public class Triangle {
	private Coordinate3D p1;
	private Coordinate3D p2;
	private Coordinate3D p3;
	
	private Line3D edge12;
	private Line3D edge23;
	private Line3D edge31;
	
	private double len12;
	private double len23;
	private double len31;
	
	private boolean isValid;
	private double area;
	
	/**
	 * not used
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public Triangle(Coordinate3D p1, Coordinate3D p2, Coordinate3D p3){
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		
		this.edge12 = new Line3D(p1, p2);
		this.edge23 = new Line3D(p2, p3);
		this.edge31 = new Line3D(p3, p1);
		
		this.len12 = edge12.getLen();
		this.len23 = edge23.getLen();
		this.len31 = edge31.getLen();
		
		this.isValid = this.isValidTriangle();
		this.area = this.isValid ? this.computeArea() : 0;
	}
	
	public Coordinate3D getP1() {
		return p1;
	}

	public Coordinate3D getP2() {
		return p2;
	}

	public Coordinate3D getP3() {
		return p3;
	}

	public double getLen12() {
		return len12;
	}

	public double getLen23() {
		return len23;
	}

	public double getLen31() {
		return len31;
	}

	public boolean isValid() {
		return isValid;
	}

	public double getArea() {
		return area;
	}
	
	public double maxEdgeLen(){
		if(len12 > len23){
			return len12>len31 ? len12 : len31;
		}else {
			return len23>len31 ? len23 : len31;
		}
	}
	
	private boolean isValidTriangle(){
		return len12 + len23 > len31
				&& len23 + len31 > len12
				&& len31 + len12 > len23;
	}
	
	private double computeArea(){
		double p = (len12 + len23 + len31)/2;
		return Math.sqrt(p*(p-len12)*(p-len23)*(p-len31));
	}
}
