package main.java.model.ashraeprm.data;

import main.java.model.geometry.Surface;
import main.java.model.geometry.scale.*;
import main.java.model.idd.EnergyPlusObjectTemplate;
import main.java.model.idd.IDDParser;
import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;
import main.java.parametric.modelModifier.ParametricModifier;
import main.java.util.ModelUtil;
import main.java.util.NumUtil;
import main.java.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

public class WindowWallRatioParser {
	public static double THRESHOLD = 0.4;
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	private LinkedList<Wall> walls;
	private List<IDFObject> feneSurfaces;
	private Double totalWallArea = 0.0;
	private Double totalWindowWallArea = 0.0;
	private Double conditiondWallArea = 0.0;
	private IDFFileObject idfFileObject;
	private String defaultWindowConstructionName = "";
	// private Double totalFenestrationArea = 0.0;

	public WindowWallRatioParser(IDFFileObject idfModel, Set<String> orientationSurfaces) {
		this.idfFileObject = idfModel;
		this.walls = new LinkedList<>();
		HashMap<String, Wall> idfWalls = new HashMap<>();
		List<IDFObject> buildSurfaces = idfModel.getCategoryList("BuildingSurface:Detailed");

		// get all conditioned zones
        Set<String> condZones = new HashSet<>();
        List<IDFObject> connections = idfFileObject.getCategoryList("ZoneHVAC:EquipmentConnections");
        for(IDFObject connection : connections){
            condZones.add(connection.getDataByStandardComment("Zone Name").toLowerCase());
        }

		for (IDFObject obj : buildSurfaces) {
			String zone = obj.getDataByStandardComment("Zone Name").toLowerCase();
			String surfaceName = obj.getDataByStandardComment("Name").toLowerCase();
			if (orientationSurfaces == null || orientationSurfaces.contains(surfaceName)) {
				String type = obj.getDataByStandardComment("Surface Type");
				if ("Wall".equalsIgnoreCase(type)) {
					String sunExpose = obj.getDataByStandardComment("Sun Exposure");
					if ("SunExposed".equalsIgnoreCase(sunExpose)) {
						List<Coordinate3D> coords = readSurfaceCoords(obj);
						Wall newWall = new Wall(coords);

						IDFObject zoneObj = idfModel.getIDFObjectByName("Zone", zone);

						// set orientation
						Surface tempSurf = new Surface(obj, zoneObj);
						String orientation = tempSurf.getFaceDirection(tempSurf.getFaceAngle(idfModel));

						String multiplierStr = zoneObj.getDataByStandardComment("Multiplier");
						if (multiplierStr != null) {
							int multiplier = Integer.parseInt(multiplierStr);
							newWall.setMultliplier(multiplier);
						}

						newWall.setOrientation(orientation);
						newWall.setInCondZone(condZones.contains(zone));

						idfWalls.put(obj.getName(), newWall);
					}
				}
			}
		}

		this.feneSurfaces = idfModel.getCategoryList("FenestrationSurface:Detailed");
		int winConstructionCounter = 0;
		for (IDFObject fene : feneSurfaces) {
			String type = fene.getDataByStandardComment("Surface Type");

			if ("Window".equalsIgnoreCase(type) || "Door".equalsIgnoreCase(type)
					|| "GlassDoor".equalsIgnoreCase(type)) {
				List<Coordinate3D> coords = this.readSurfaceCoords(fene);
				String buildSurfaceName = fene.getDataByStandardComment("Building Surface Name");

				if (idfWalls.containsKey(buildSurfaceName)) {
					int multiplier = NumUtil.readInt(fene.getDataByStandardComment("Multiplier"), 1);
					if ("Window".equalsIgnoreCase(type)) {
						idfWalls.get(buildSurfaceName).addWindow(coords, fene.getName(), multiplier, fene);
					} else {
						idfWalls.get(buildSurfaceName).addDoor(coords, fene.getName(), multiplier, fene);
					}
				}

                if ("Window".equalsIgnoreCase(type)){
                    String constructionName = fene.getDataByStandardComment("Construction Name");
                    if(winConstructionCounter==0){
                        winConstructionCounter++;
                        defaultWindowConstructionName = constructionName;
                    }else {
                        if(constructionName.equalsIgnoreCase(defaultWindowConstructionName)){
                            winConstructionCounter++;
                        }else {
                            winConstructionCounter--;
                        }
                    }
                }
			}
		}

		if(StringUtil.isNullOrEmpty(defaultWindowConstructionName)){
		    // generate default window construction
            defaultWindowConstructionName = "Default Window Construction by BSH";
            IDFObject newGlazing = ParametricModifier.generateDefaultSimpleGlazing(defaultWindowConstructionName, 0.4, 2.4);
            List<String> materialList = new ArrayList<String>();
            materialList.add(newGlazing.getName());

            IDFObject newConstruction = ParametricModifier.generateNewConstructionWithListOfMaterials(defaultWindowConstructionName, materialList);
            idfFileObject.addObject(newGlazing);
            idfFileObject.addObject(newConstruction);
        }

		Set<String> names = idfWalls.keySet();
		for (String name : names) {
			Wall wall = idfWalls.get(name);
			double area = wall.getArea() * wall.getMultiplier();
			if (wall.hasWindow()) {
				totalWindowWallArea += area;
				this.walls.add(wall);
			}
			totalWallArea += area;
		}
	}

	public WindowWallRatioParser(IDFFileObject idfModel) {
		this.idfFileObject = idfModel;
		this.walls = new LinkedList<>();

		HashMap<String, Wall> idfWalls = new HashMap<>();
		List<IDFObject> buildSurfaces = idfModel.getCategoryList("BuildingSurface:Detailed");
		List<IDFObject> conditionedZones = idfModel.getCategoryList("ZoneHVAC:EquipmentConnections");
		Set<String> zoneNameSet = new HashSet<>();
		if (conditionedZones != null) {
			for (IDFObject conditionedZone : conditionedZones) {
				zoneNameSet.add(conditionedZone.getDataByStandardComment("Zone Name"));
			}
		}

		for (IDFObject obj : buildSurfaces) {
			String type = obj.getDataByStandardComment("Surface Type");
			String zone = obj.getDataByStandardComment("Zone Name");

			if ("Wall".equalsIgnoreCase(type)) {
				String sunExpose = obj.getDataByStandardComment("Sun Exposure");
				if ("SunExposed".equalsIgnoreCase(sunExpose)) {
					List<Coordinate3D> coords = readSurfaceCoords(obj);
					Wall newWall = new Wall(coords);

					IDFObject zoneObj = idfModel.getIDFObjectByName("Zone", zone);
					int multiplier = NumUtil.readInt(zoneObj.getDataByStandardComment("Multiplier"), 1);

					// set orientation
					Surface tempSurf = new Surface(obj, zoneObj);
					String orientation = tempSurf.getFaceDirection(tempSurf.getFaceAngle(idfModel));

					newWall.setMultliplier(multiplier);
					newWall.setOrientation(orientation);
					newWall.setZoneName(zone);
					newWall.setOutside(true);
                    newWall.setSurfaceName(obj.getName());
                    newWall.setInCondZone(zoneNameSet.contains(zone));

					idfWalls.put(obj.getName(), newWall);
				}
			}
		}

		// List<IDFObject> removeFenes = new ArrayList<>();
		this.feneSurfaces = idfModel.getCategoryList("FenestrationSurface:Detailed");
		int winConstructionCounter = 0;
		if (feneSurfaces != null) {
			for (IDFObject fene : feneSurfaces) {
				String type = fene.getDataByStandardComment("Surface Type");
				if ("Window".equalsIgnoreCase(type) || "Door".equalsIgnoreCase(type)
						|| "GlassDoor".equalsIgnoreCase(type)) {
					List<Coordinate3D> coords = this.readSurfaceCoords(fene);
					String buildSurfaceName = fene.getDataByStandardComment("Building Surface Name");

					if (idfWalls.containsKey(buildSurfaceName)) {
						int multiplier = NumUtil.readInt(fene.getDataByStandardComment("Multiplier"), 1);
						if ("Window".equalsIgnoreCase(type)) {
							idfWalls.get(buildSurfaceName).addWindow(coords, fene.getName(), multiplier, fene);
						} else {
							idfWalls.get(buildSurfaceName).addDoor(coords, fene.getName(), multiplier, fene);
						}
					}

                    if ("Window".equalsIgnoreCase(type)){
                        String constructionName = fene.getDataByStandardComment("Construction Name");
                        if(winConstructionCounter==0){
                            winConstructionCounter++;
                            defaultWindowConstructionName = constructionName;
                        }else {
                            if(constructionName.equalsIgnoreCase(defaultWindowConstructionName)){
                                winConstructionCounter++;
                            }else {
                                winConstructionCounter--;
                            }
                        }
                    }
				}
			}
		}
		/*
		 * if (removeFenes.size() > 0) { for (int i = 0; i < removeFenes.size(); i++) {
		 * idfModel.removeIDFObject(removeFenes.get(i)); } }
		 */

		Set<String> names = idfWalls.keySet();
		for (String name : names) {
			Wall wall = idfWalls.get(name);
			double area = wall.getArea() * wall.getMultiplier();
			if (wall.hasWindow()) {
				totalWindowWallArea += area;
			}
			if (zoneNameSet.contains(wall.getZoneName())) {
				conditiondWallArea += area;
			}
			this.walls.add(wall);
			totalWallArea += area;
		}
	}

	public Double getTotalWindowToWallRatio() {
		return getTotalWindowArea() / totalWallArea * 100;
	}

	public Double getTotalWindowToWallRatio(String orientation) {
		return getTotalWindowArea(orientation) / totalWallArea * 100;
	}

	public Double getTotalWallArea() {
		return totalWallArea;
	}

	public Double getTotalWallArea(String orientation) {
		Double area = 0.0;
		for (Wall w : walls) {
			if (w.getOrientation().equals(orientation)) {
				area += w.getArea() * w.getMultiplier();
			}
		}
		return area;
	}

	public double getConditionedWallArea() {
		return conditiondWallArea;
	}

	public Double getTotalWindowArea() {
		Double windowArea = 0.0;

		for (Wall wall : walls) {
			if (wall.hasWindow()) {
				windowArea += wall.getWindowArea();
			}
		}
		return windowArea;
	}

	public Double getTotalWindowArea(String orientation) {
		Double windowArea = 0.0;

		for (Wall wall : walls) {
			if (wall.getOrientation().equals(orientation) && wall.hasWindow()) {
				windowArea += wall.getWindowArea();
			}
		}
		return windowArea;
	}

	/**
	 * Same for BuildingSurface:Detailed and FenestrationSurface:Detailed
	 *
	 * @param obj
	 * @return
	 */
	private List<Coordinate3D> readSurfaceCoords(IDFObject obj) {
		List<Coordinate3D> coords = new LinkedList<Coordinate3D>();
		// add 1 to point to the first coordinate
		int index = obj.getIndexOfStandardComment("Number of Vertices") + 1;
		while (index < obj.getStandardComments().length) {
			double x = Double.parseDouble(obj.getIndexedData(index));
			double y = Double.parseDouble(obj.getIndexedData(index + 1));
			double z = Double.parseDouble(obj.getIndexedData(index + 2));
			Coordinate3D coord = new Coordinate3D(x, y, z);
			coords.add(coord);
			index += 3;
		}
		return coords;
	}

	public void windowWallValidation() {
		for (Wall wall : walls) {
			// System.out.println("beFORE: " + wall.getWindowArea() + " " +
			// wall.getWallArea());
			if (wall.getWindowArea() == wall.getWallArea()) {
				wall.scaleWindows(0.8);
				this.saveWindowCoords(wall);
				// System.out.println("AFTER: " + wall.getWindowArea() + " " +
				// wall.getWallArea());

			}
		}
	}

	public void updateThreshold(double threshold) {
		if (threshold >= 0 && threshold <= 1) {
			THRESHOLD = threshold;
		}
	}

	public double getMaxWWR() {
		return 0.89;
	}

	public boolean adjustToTargetRatio(Double targetRatio, String orientation) {
		double ratio;
		if (orientation == null) {
			ratio = getTotalWindowToWallRatio()/100d;
		} else {
			ratio = getTotalWindowToWallRatio(orientation);
			//System.out.println(ratio);
		}

		if (targetRatio > getMaxWWR()) {
			targetRatio = getMaxWWR();
		}

		double winWallRatio = targetRatio * totalWallArea;

		// shrink
		if (ratio > winWallRatio) {
			double scaleRatio = targetRatio / ratio;
			for (Wall wall : walls) {
				if (wall.hasWindow()) {
					if (orientation == null ^ wall.getOrientation().equals(orientation)) {
						wall.scaleWindows(scaleRatio);
						this.saveWindowCoords(wall);
					}
				}
			}

			return true;
		}
		// enlarge
		else if (ratio < winWallRatio) {
			for (Wall wall : walls) {
			    if(!wall.hasWindow() && !wall.isInCondZone()){
			        continue;
                }

                if (orientation == null ^ wall.getOrientation().equals(orientation)) {
                    List<Window> wins = wall.getWindows();
                    Iterator<Window> iter = wins.iterator();
                    Window firstWin = wall.hasWindow() ? iter.next() : null; // keep one window
                    while (iter.hasNext()) {
                        Window win = iter.next();
                        idfFileObject.removeIDFObject(win.getIDFObject());
                        iter.remove();
                    }

                    List<Door> doors = wall.getDoors();
                    for (Door door : doors) {
                        idfFileObject.removeIDFObject(door.getIDFObject());
                    }
                    doors.clear();

                    // shrink wall to THRESHOLD
                    Polygon wallPolygon = wall.extratPolygon();
                    double scaleRatio = targetRatio;
                    wallPolygon.scale(scaleRatio);

                    IDFObject base;
                    if(wall.hasWindow()){
                        base = firstWin.getIDFObject();
                    }else {
                        int points = wallPolygon.getNumPoints();
                        if(points>4){
                            points = 3;
                        }

                        IDDParser parser = new IDDParser(idfFileObject.getVersion());
                        EnergyPlusObjectTemplate objTemplate = parser.getObject("FenestrationSurface:Detailed");
                        int numVertices = objTemplate.getPositionOfFieldName("Number of Vertices");
                        base = objTemplate.buildEmptyObject(numVertices+1+points*3);

                        base.setDataByStandardComment("Name", wall.getSurfaceName() + " new window");
                        base.setName(base.getDataByStandardComment("Name"));
                        base.setDataByStandardComment("Surface Type", "WINDOW");
                        base.setDataByStandardComment("Construction Name", defaultWindowConstructionName);
                        base.setDataByStandardComment("Building Surface Name", wall.getSurfaceName());
                        base.setDataByStandardComment("View Factor to Ground", "autocalculate");
                        base.setDataByStandardComment("Multiplier", "1");
                        base.setDataByStandardComment("Number of Vertices", points+"");
                    }
                    int metaLen = NumUtil.readInt(base.getDataByStandardComment("Number of Vertices"), 4) + 1;
                    if (wallPolygon.getNumPoints() > 4) { // wall is more than square, needs cut into triangles
                        List<Polygon> cuts = wallPolygon.triangulize();
                        int preWinPoints = NumUtil.readInt(base.getDataByStandardComment("Number of Vertices"), 4);
                        if (preWinPoints != 3) {
                            if(wall.hasWindow()){
                                idfFileObject.removeIDFObject(base);
                            }

                            base = new IDFObject("", metaLen + 3 * 3);
                            ModelUtil.copyIDFObject(firstWin.getIDFObject(), base, 0, metaLen - 1);
                            base.setDataByStandardComment("Number of Vertices", "3");
                        }
                        base.setDataByStandardComment("Multiplier", "1");

                        String winName = base.getName();
                        for (int i = 0; i < cuts.size(); i++) {
                            Polygon polygon = cuts.get(i);
                            IDFObject copy = base.deepClone();

                            List<Coordinate3D> coords = polygon.getCoords();
                            int coorIdx = base.getIndexOfStandardComment("Number of Vertices");
                            for (int j = 0; j < coords.size(); j++) {
                                Coordinate3D coor3D = coords.get(j);
                                copy.setIndexedData(++coorIdx, coor3D.getX() + "");
                                copy.setIndexedData(++coorIdx, coor3D.getY() + "");
                                copy.setIndexedData(++coorIdx, coor3D.getZ() + "");
                            }
                            copy.setName(winName + "%wwr_expand_" + i);

                            idfFileObject.addObject(copy);
                        }

                    } else { // wall is square or triangle
                        if(!wall.hasWindow()){
                            idfFileObject.addObject(base);
                        }

                        int preWinPoints = NumUtil.readInt(base.getDataByStandardComment("Number of Vertices"), 4);
                        if (preWinPoints != wallPolygon.getNumPoints()) {
                            base = new IDFObject("", metaLen + wallPolygon.getNumPoints() * 3);
                            ModelUtil.copyIDFObject(firstWin.getIDFObject(), base, 0, metaLen - 1);
                            base.setDataByStandardComment("Number of Vertices", wallPolygon.getNumPoints() + "");

                            if(wall.hasWindow()) {
                                idfFileObject.removeIDFObject(firstWin.getIDFObject());
                            }
                            idfFileObject.addObject(base);
                        }
                        base.setDataByStandardComment("Multiplier", "1");

                        int coorIdx = base.getIndexOfStandardComment("Number of Vertices");
                        List<Coordinate3D> coords = wallPolygon.getCoords();
                        for (int i = 0; i < coords.size(); i++) {
                            Coordinate3D coor3D = coords.get(i);
                            base.setIndexedData(++coorIdx, coor3D.getX() + "");
                            base.setIndexedData(++coorIdx, coor3D.getY() + "");
                            base.setIndexedData(++coorIdx, coor3D.getZ() + "");
                        }
                    }
                }
            }
			return true;
		}
		return false;
	}

	/**
	 * Return whether the window is adjusted, only do shrink if current ratio >
	 * THRESHOLD
	 * 
	 * @return
	 */
	public boolean adjustToThreshold(JsonArray status) {
		double ratio = getTotalWindowToWallRatio() / 100;
		// shrink
		if (ratio > WindowWallRatioParser.THRESHOLD) {
			JsonObject tempObj = new JsonObject();
			tempObj.addProperty("message", "Total window to wall ratio is: " + ratio
					+ " , this is larger than the threshold: " + WindowWallRatioParser.THRESHOLD);
			status.add(tempObj);
			double scaleRatio = WindowWallRatioParser.THRESHOLD / ratio;
			for (Wall wall : walls) {
				wall.scaleWindows(scaleRatio);
				this.saveWindowCoords(wall);
			}

			return true;
		} else {
			JsonObject tempObj = new JsonObject();
			tempObj.addProperty("message", "Total window to wall ratio is: " + ratio
					+ " , this is smaller than the threshold: " + WindowWallRatioParser.THRESHOLD);
			status.add(tempObj);
		}

		return false;
	}

	private void saveWindowCoords(Wall wall) {
		List<Window> wins = wall.getWindows();
		for (Window win : wins) {
			String name = win.getName();
			for (int i = 0; i < feneSurfaces.size(); i++) {
				IDFObject fene = feneSurfaces.get(i);
				if (fene.getName().equals(name)) {
					int index = fene.getIndexOfStandardComment("Number of Vertices") + 1;

					List<Coordinate3D> points = win.getCoords();
					for (Coordinate3D point : points) {
						fene.setIndexedData(index, String.valueOf(point.getX()));
						fene.setIndexedData(index + 1, String.valueOf(point.getY()));
						fene.setIndexedData(index + 2, String.valueOf(point.getZ()));
						index += 3;
					}
				}
			}
		}
	}

}
