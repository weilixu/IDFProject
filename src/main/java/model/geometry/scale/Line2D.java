package main.java.model.geometry.scale;

public class Line2D {
    private Coordinate2D start;
    private Coordinate2D end;

    public Line2D(Coordinate2D start, Coordinate2D end){
        this.start = start;
        this.end = end;
    }

    public Coordinate2D getStart() {
        return start;
    }

    public Coordinate2D getEnd() {
        return end;
    }

    /**
     * p, q, r should be co-linear
     * check if q is on the segment of pr
     */
    private boolean onSegment(Coordinate2D p, Coordinate2D q, Coordinate2D r) {
        if (q.getX() <= Math.max(p.getX(), r.getX())
                && q.getX() >= Math.min(p.getX(), r.getX())
                && q.getY() <= Math.max(p.getY(), r.getY())
                && q.getY() >= Math.min(p.getY(), r.getY())) {
            return true;
        }
        return false;
    }

    /** To find orientation of ordered triplet (p, q, r).
     * The function returns following values
     * 0 --> p, q and r are co-linear
     * 1 --> Clockwise
     * 2 --> Counterclockwise
     */
    private int orientation(Coordinate2D p, Coordinate2D q, Coordinate2D r) {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.
        double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) - (q.getX() - p.getX()) * (r.getY() - q.getY());

        if (Math.abs(val) <= Polygon.CONTAIN_TEST_EPSILON){
            return 0;  // co-linear
        }

        return (val > 0) ? 1: 2; // clock or counter-clock wise
    }

    public int isIntersect(Line2D line){
        Coordinate2D p1 = this.start;
        Coordinate2D q1 = this.end;
        Coordinate2D p2 = line.getStart();
        Coordinate2D q2 = line.getEnd();

        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        if (o1 != o2 && o3 != o4) {
            return 1;
        }

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) {
            return 0;
        }

        // p1, q1 and q2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) {
            return 0;
        }

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) {
            return 0;
        }

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2)) {
            return 0;
        }

        return -1; // Doesn't fall in any of the above cases
    }
}
