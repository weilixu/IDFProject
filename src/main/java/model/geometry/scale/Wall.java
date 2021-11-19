package main.java.model.geometry.scale;

import main.java.model.idf.IDFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class Wall extends Polygon {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private List<Window> windows;
    private List<Door> doors;
    private int multiplier;
    private String zoneName;
    private String orientation;
    private boolean isOutside;
    private boolean isInCondZone;
    private String surfaceName;

    public Wall(List<Coordinate3D> points) {
        super(points);
        windows = new LinkedList<>();
        doors = new LinkedList<>();
        multiplier = 1;
    }

    /**
     * Return false if exists fenestration's vertex not on or within wall
     * polygon<br/>
     * fenestration type refer to public static final fields
     *
     * @return
     */
    private boolean isInWall(List<Coordinate3D> points) {
        boolean flag = true;

        for (Coordinate3D point : points) {
            if (!super.containsPointConvexPolygon(point)) {
                flag = false;
            }
        }

        if (!flag) {
            LOG.error("Fenestration is not on or within wall polygon");
        }
        return flag;
    }
    
    public void setOrientation(String orientation) {
    		this.orientation = orientation;
    }

    public void setOutside(boolean isOutside){ this.isOutside = isOutside; }

    public void setSurfaceName(String surfaceName){ this.surfaceName = surfaceName; }

    public void setMultliplier(Integer multi) {
        multiplier = multi;
    }

    public void setInCondZone(boolean inCondZone){ this.isInCondZone = inCondZone; }

    public Integer getMultiplier(){ return this.multiplier; }
    
    public String getOrientation(){ return this.orientation; }

    public boolean isOutside(){ return this.isOutside; }

    public boolean isInCondZone(){ return this.isInCondZone; }

    public String getSurfaceName(){ return this.surfaceName; }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getZoneName(){ return this.zoneName; }

    /**
     * Return false if exists fenestration's vertex not on or within wall
     * polygon
     *
     * @return
     */
    public boolean addWindow(List<Coordinate3D> points, String name, int multiplier, IDFObject feneIDFObj) {
        Window win = new Window(points, name);
        win.setMultiplier(multiplier);
        win.setIDFObject(feneIDFObj);

        this.windows.add(win);

        return true;
        //return this.isInWall(points);
    }

    /**
     * Return false if exists fenestration's vertex not on or within wall
     * polygon
     *
     * @return
     */
    public boolean addDoor(List<Coordinate3D> points, String name, int multiplier, IDFObject feneIDFObj) {
        Door door = new Door(points);
        door.setMultiplier(multiplier);
        door.setIDFObject(feneIDFObj);
        this.doors.add(door);

        return this.isInWall(points);
    }

    public double getWallArea() {
        return super.getArea() * multiplier;
    }

    public double getWindowArea() {
        double area = 0;
        for (Window win : windows) {
            area += win.getArea() * multiplier;
        }
        return area;
    }

    /**
     * ratio>0 => enlarge windows
     * ratio<0 => shrink windows
     * returns Polygon's scale related flag
     */
    public int scaleWindows(double ratio) {
        if(ratio<0){
            return Polygon.SCALE_RATIO_INVALID;
        }

        int flag = 0;
        for (Window win : windows) {
            int tmp = win.scale(ratio);
            if (flag == Polygon.SCALE_SUCCESS) {
                flag = tmp; //capture the first error flag if any
            }
        }
        return flag;
    }

    public boolean hasWindow() {
        return !windows.isEmpty();
    }

    public List<Window> getWindows() {
        return this.windows;
    }

    public List<Door> getDoors() {
        return this.doors;
    }
}
