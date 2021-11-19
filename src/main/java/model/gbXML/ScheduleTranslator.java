package main.java.model.gbXML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import main.java.model.idf.IDFFileObject;
import main.java.model.idf.IDFObject;

public class ScheduleTranslator {

    private HashMap<String, String> bs_idToObjectMap;
    private String[] timeInterval = {"01:00","02:00","03:00","04:00","05:00","06:00","07:00","08:00",
            "09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00",
            "20:00","21:00","22:00","23:00","24:00"};
    private Namespace ns;
    
    private ArrayList<String> lines;
    private ArrayList<String> units;
    private ArrayList<String> comments;
    private ArrayList<String> topComments;

    public static final String BUILDING_OCC_SCHEDULE = "Building_OCC_Sch";
    public static final String BUILDING_LIGHT_SCHEDULE = "Building_Light_Sch";
    public static final String BUILDING_EQUIP_SCHEDUEL = "Building_Equipment_Sch";
    public static final String BUILDING_HTGSP_SCHEDULE = "Building_Heating_Sp_Schedule";
    public static final String BUILDING_CLGSP_SCHEDULE = "Building_Cooling_Sp_Schedule";
    
    
    public ScheduleTranslator(Namespace ns) {
        bs_idToObjectMap = new HashMap<String, String>();
        this.ns = ns;
        
        lines = new ArrayList<String>();
        units = new ArrayList<String>();
        comments = new ArrayList<String>();
        topComments = new ArrayList<String>();
    }

    /**
     * Function translates the schedule (year) to Schedule:Year object
     * consequently translates the weekly and daily schedules 
     * to Schedule:Week and Schedule:day...
     * 
     * TODO Current function does not check the validity of the time span
     * as the standard require that the year schedules should not
     * span more than one calendar year.
     * 
     * @param element:
     *            schedule element
     * @param element: body
     * @param file
     */
    public void translateSchedule(Element element, Element body, IDFFileObject file) {
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> units = new ArrayList<String>();
        ArrayList<String> comments = new ArrayList<String>();
        ArrayList<String> topComments = new ArrayList<String>();
        String beginYear = "2011";//arbitary beginning year, use to check calendar year
        
        lines.add("Schedule:Year");
        units.add("");
        comments.add("");
        
        String id = element.getAttributeValue("id");
        String type = element.getAttributeValue("type");

        String name = element.getChildText("Name",ns);
        name = escapeName(id, name);
        
        lines.add(name);
        units.add("");
        comments.add("Name");

        bs_idToObjectMap.put(id, name);
        String scheduleTypeLimit = createScheduleTypeLimit(type, file);
        
        lines.add(scheduleTypeLimit);
        units.add("");
        comments.add("Schedule Type Limits Name");
        
        //start processing year schedules
        List<Element> scheduleYearElements = element.getChildren("YearSchedule",ns);
        for(int i=0; i<scheduleYearElements.size(); i++){
            Element scheduleYearElement = scheduleYearElements.get(i);
            
            String beginDateString = scheduleYearElement.getChildText("BeginDate",ns);
            String[] beginDateParts = beginDateString.split("-"); //2015-01-01
            if(i==0){
                //fix the calendar year
                beginYear = beginDateParts[0];
            }
            
            if(beginDateParts.length!=3){
                //TODO ERROR - 'Schedule Year name: ' + name + "begin date does not contain adequate information / have" +
                //"the correct format. A correct example of begin date should be: 2015-01-01";
                //STOP the processing
            }else if(!beginDateParts[0].equals(beginYear)){
                //TODO ERROR - "Begin Year does not match: " + beginYear. The year schedule span no more than a calendar year.
                //STOP the processing
            }
            String endDateString = scheduleYearElement.getChildText("EndDate",ns);
            String[] endDateParts = endDateString.split("-");
            if(endDateParts.length!=3){
                //TODO ERROR - 'Schedule Year name: ' + name + "end date does not contain adequate information / have" +
                //"the correct format. A correct example of end date should be: 2015-12-31";
                //STOP the processing
            }else if(!endDateParts[0].equals(beginYear)){
                //TODO ERROR - "End Year does not match: " + beginYear. The year schedule span no more than a calendar year.
                //STOP the processing
            }
            
            String weekScheduleId = scheduleYearElement.getChild("WeekScheduleId",ns).getAttributeValue("weekScheduleIdRef");
            String weekScheduleName = null;
            
            List<Element> scheduleWeekElementList = body.getChildren("WeekSchedule",ns);
            for(int j=0; j<scheduleWeekElementList.size(); j++){
                Element scheduleWeekElement = scheduleWeekElementList.get(j);
                if(scheduleWeekElement.getAttributeValue("id").equals(weekScheduleId)){
                    weekScheduleName = translateScheduleWeek(scheduleWeekElement, body, file);
                    break;
                }
            }
            
            if(weekScheduleName!=null){
                lines.add(weekScheduleName);
                units.add("");
                comments.add("Schedule:Week Name");
                
                lines.add(beginDateParts[1]);
                units.add("");
                comments.add("Start Month");
                
                lines.add(beginDateParts[2]);
                units.add("");
                comments.add("Start Day");
                
                lines.add(endDateParts[1]);
                units.add("");
                comments.add("End Month");
                
                lines.add(endDateParts[2]);
                units.add("");
                comments.add("End Day");
            }else{
                //TODO Error: "cannot find the weekly schedule under year schedule:" + name + " weekly schedule id: " + weekScheduleId"
                //Stop processing
            }//if
        }//for
        file.addObject(new IDFObject(lines, units, comments, topComments));
    }
    
    /**
     * This method is called after the schedule processing is completed
     * It is mainly used for the type of schedules that are CURRENTLY not
     * covered by gbXML but critical for EnergyPlus simulation.
     * It must be called after schedules are processed
     * More info visit EnergyPlus input and output document
     * @param file
     */
    public void addMiscScheduleTypeLimits(IDFFileObject file){
        //add HVAC control type schedule limit
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> units = new ArrayList<String>();
        ArrayList<String> comments = new ArrayList<String>();
        ArrayList<String> topComments = new ArrayList<String>();
        
        lines.add("ScheduleTypeLimits");
        units.add("");
        comments.add("");
        
        lines.add("Control Type");
        units.add("");
        comments.add("Name");
        
        lines.add("0");
        units.add("");
        comments.add("Lower Limit Value");
        
        lines.add("4");
        units.add("");
        comments.add("Upper Limit Value");
        
        lines.add("DISCRETE");
        units.add("");
        comments.add("Numeric Type");
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        bs_idToObjectMap.put("controlType","Control Type");
        lines.clear();
        units.clear();
        comments.clear();
        
        //add humidity level
        lines.add("ScheduleTypeLimits");
        units.add("");
        comments.add("");
        
        lines.add("Humidity");
        units.add("");
        comments.add("Name");
        
        lines.add("10");
        units.add("");
        comments.add("Lower Limit Value");
        
        lines.add("90");
        units.add("");
        comments.add("Upper Limit Value");
        
        lines.add("CONTINUOUS");
        units.add("");
        comments.add("Numeric Type");
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        bs_idToObjectMap.put("humidity","Humidity");
        lines.clear();
        units.clear();
        comments.clear();
        
        //add fraction level
        lines.add("ScheduleTypeLimits");
        units.add("");
        comments.add("");
        
        lines.add("Fraction");
        units.add("");
        comments.add("Name");
        
        lines.add("0");
        units.add("");
        comments.add("Lower Limit Value");
        
        lines.add("1");
        units.add("");
        comments.add("Upper Limit Value");
        
        lines.add("CONTINUOUS");
        units.add("");
        comments.add("Numeric Type");
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        bs_idToObjectMap.put("fraction","Fraction");
        lines.clear();
        units.clear();
        comments.clear();
        
        //add temperature level
        lines.add("ScheduleTypeLimits");
        units.add("");
        comments.add("");
        
        lines.add("Temperature");
        units.add("");
        comments.add("Name");
        
        lines.add("-60");
        units.add("");
        comments.add("Lower Limit Value");
        
        lines.add("200");
        units.add("");
        comments.add("Upper Limit Value");
        
        lines.add("CONTINUOUS");
        units.add("");
        comments.add("Numeric Type");
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        bs_idToObjectMap.put("temperature","Temperature");
        lines.clear();
        units.clear();
        comments.clear();
        
//        if(bs_idToObjectMap.containsValue("Any Number")){
//            lines.add("ScheduleTypeLimits");
//            units.add("");
//            comments.add("");
//            
//            lines.add("Any Number");
//            units.add("");
//            comments.add("Name");
//            file.addObject(new IDFObject(lines, units, comments, topComments));
//            bs_idToObjectMap.put("anynumber","Any Number");
//        }
        
        //add fraction level
        lines.add("ScheduleTypeLimits");
        units.add("");
        comments.add("");
        
        lines.add("Any Number");
        units.add("");
        comments.add("Name");
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        bs_idToObjectMap.put("anynumber","Any Number");
        lines.clear();
        units.clear();
        comments.clear();

    }
    
    /**
     * get the correspondent schedule from its id
     * @param id
     * @return
     */
    public String getScheduleNameFromID(String id){
        return bs_idToObjectMap.get(id);
    }
    
    /**
     * this function adds a simple compact schedule. The schedule only carries one value
     * oftenly use for specify the activity level, control types etc. use Any Number schedule type
     * @param scheduleName
     * @param value
     */
    public void addSimpleCompactSchedule(String scheduleName, String scheduleId,Double value, IDFFileObject file){
        bs_idToObjectMap.put(scheduleId, scheduleName);
        recordInputs("Schedule:Compact","","","");
        recordInputs(scheduleName,"","Name","");
        if(scheduleId.contains("controlType")){
            recordInputs("Control Type","","Schedule Type Limits Name","");
        }else{
            recordInputs("Any Number","","Schedule Type Limits Name","");
            
        }
        recordInputs("Through: 12/31","","Field 1","");
        recordInputs("For: AllDays","","Field 2","");
        recordInputs("Until: 24:00","","Field 3","");
        recordInputs(value.toString(),"","Field4","");
        addObject(file);
        
    }
    
    /**
     * Add people schedule from default
     * Add a default office schedule to the energy model
     * Currently, there is only one option: work 6 days and everyday 18 hours.
     * More option should add in according to user inputs.
     * TODO add one more variable to identify the preferred schedule
     * @param scheduleName
     * @param scheduleId
     * @param file
     */
    public void addPeopleSchedule(String scheduleName, String scheduleId, IDFFileObject file){
        bs_idToObjectMap.put(scheduleId, scheduleName);
        recordInputs("Schedule:Compact","","","");
        recordInputs(scheduleName,"","Name","");
        recordInputs("Fraction","","Schedule Type Limits name","");
        recordInputs("Through: 12/31","","Field 1","");
        recordInputs("For: SummerDesignDay","","Field 2","");
        recordInputs("Until: 06:00","","Field 3","");
        recordInputs("0.0","","Field 4","");
        recordInputs("Until: 22:00","","Field 5","");
        recordInputs("1.0","","Field 6","");
        recordInputs("Until: 24:00","","Field 7","");
        recordInputs("0.05","","Field 8","");
        recordInputs("For: Weekdays","","Field 9","");
        recordInputs("Until: 06:00","","Field 10","");
        recordInputs("0.0","","Field 11","");
        recordInputs("Until: 07:00","","Field 12","");
        recordInputs("0.1","","Field 13","");
        recordInputs("Until: 08:00","","Field 14","");
        recordInputs("0.2","","Field 15","");
        recordInputs("Until: 12:00","","Field 16","");
        recordInputs("0.95","","Field 17","");
        recordInputs("Until: 13:00","","Field 18","");
        recordInputs("0.5","","Field 19","");
        recordInputs("Until: 17:00","","Field 20","");
        recordInputs("0.95","","Field 21","");
        recordInputs("Until: 18:00","","Field 22","");
        recordInputs("0.7","","Field 23","");
        recordInputs("Until: 20:00","","Field 24","");
        recordInputs("0.4","","Field 25","");
        recordInputs("Until: 22:00","","Field 26","");
        recordInputs("0.1","","Field 27","");
        recordInputs("Until: 24:00","","Field 28","");
        recordInputs("0.05","","Field 29","");
        recordInputs("For: Saturday","","Field 30","");
        recordInputs("Until: 06:00","","Field 31","");
        recordInputs("0.0","","Field 32","");
        recordInputs("Until: 08:00","","Field 33","");
        recordInputs("0.1","","Field 34","");
        recordInputs("Until: 14:00","","Field 35","");
        recordInputs("0.5","","Field 36","");
        recordInputs("Until: 17:00","","Field 37","");
        recordInputs("0.1","","Field 38","");
        recordInputs("Until: 24:00","","Field 39","");
        recordInputs("0.0","","Field 40","");
        recordInputs("For: AllOtherDays","","Field 41","");
        recordInputs("Until: 24:00","","Field 42","");
        recordInputs("0.0","","Field 43","");   
        addObject(file);
    }
    
    /**
     * Add light schedule from default
     * Add a default office schedule to the energy model
     * Currently, there is only one option: work 6 days and everyday 18 hours.
     * More option should add in according to user inputs.
     * TODO add one more variable to identify the preferred schedule
     * @param scheduleName
     * @param scheduleId
     * @param file
     */
    public void addLightSchedule(String scheduleName, String scheduleId, IDFFileObject file){
        bs_idToObjectMap.put(scheduleId, scheduleName);
        recordInputs("Schedule:Compact","","","");
        recordInputs(scheduleName,"","Name","");
        recordInputs("Fraction","","Schedule Type Limits name","");
        recordInputs("Through: 12/31","","Field 1","");
        recordInputs("For: Weekdays","","Field 2","");
        recordInputs("Until: 05:00","","Field 3","");
        recordInputs("0.05","","Field 4","");
        recordInputs("Until: 07:00","","Field 5","");
        recordInputs("0.1","","Field 6","");
        recordInputs("Until: 08:00","","Field 7","");
        recordInputs("0.3","","Field 8","");
        recordInputs("Until: 17:00","","Field 9","");
        recordInputs("0.9","","Field 10","");
        recordInputs("Until: 18:00","","Field 11","");
        recordInputs("0.7","","Field 12","");
        recordInputs("Until: 20:00","","Field 13","");
        recordInputs("0.5","","Field 14","");
        recordInputs("Until: 22:00","","Field 15","");
        recordInputs("0.3","","Field 16","");
        recordInputs("Until: 23:00","","Field 17","");
        recordInputs("0.1","","Field 18","");
        recordInputs("Until: 24:00","","Field 19","");
        recordInputs("0.05","","Field 20","");
        recordInputs("For: Saturday","","Field 21","");
        recordInputs("Until: 06:00","","Field 22","");
        recordInputs("0.05","","Field 23","");
        recordInputs("Until: 08:00","","Field 24","");
        recordInputs("0.1","","Field 25","");
        recordInputs("Until: 14:00","","Field 26","");
        recordInputs("0.5","","Field 27","");
        recordInputs("Until: 17:00","","Field 28","");
        recordInputs("0.15","","Field 29","");
        recordInputs("Until: 24:00","","Field 30","");
        recordInputs("0.05","","Field 31","");
        recordInputs("For SummerDesignDay","","Field 32","");
        recordInputs("Until: 24:00","","Field 33","");
        recordInputs("1.0","","Field 34","");
        recordInputs("For: WinterDesignDay","","Field 35","");
        recordInputs("Until: 24:00","","Field 36","");
        recordInputs("0.0","","Field 37","");
        recordInputs("For: AllOtherDays","","Field 38","");
        recordInputs("Until: 24:00","","Field 39","");
        recordInputs("0.05","","Field 40","");   
        addObject(file);
    }
    
    /**
     * Add light schedule from default
     * Add a default office schedule to the energy model
     * Currently, there is only one option: work 6 days and everyday 18 hours.
     * More option should add in according to user inputs.
     * TODO add one more variable to identify the preferred schedule
     * @param scheduleName
     * @param scheduleId
     * @param file
     */
    public void addEquipmentSchedule(String scheduleName, String scheduleId, IDFFileObject file){
        bs_idToObjectMap.put(scheduleId, scheduleName);
        recordInputs("Schedule:Compact","","","");
        recordInputs(scheduleName,"","Name","");
        recordInputs("Fraction","","Schedule Type Limits name","");
        recordInputs("Through: 12/31","","Field 1","");
        recordInputs("For: Weekdays","","Field 2","");
        recordInputs("Until: 08:00","","Field 3","");
        recordInputs("0.4","","Field 4","");
        recordInputs("Until: 12:00","","Field 5","");
        recordInputs("0.9","","Field 6","");
        recordInputs("Until: 13:00","","Field 7","");
        recordInputs("0.8","","Field 8","");
        recordInputs("Until: 17:00","","Field 9","");
        recordInputs("0.9","","Field 10","");
        recordInputs("Until: 18:00","","Field 11","");
        recordInputs("0.8","","Field 12","");
        recordInputs("Until: 20:00","","Field 13","");
        recordInputs("0.6","","Field 14","");
        recordInputs("Until: 22:00","","Field 15","");
        recordInputs("0.5","","Field 16","");
        recordInputs("Until: 24:00","","Field 17","");
        recordInputs("0.4","","Field 18","");
        recordInputs("For: Saturday","","Field 19","");
        recordInputs("Until: 06:00","","Field 20","");
        recordInputs("0.3","","Field 21","");
        recordInputs("Until: 08:00","","Field 22","");
        recordInputs("0.4","","Field 23","");
        recordInputs("Until: 14:00","","Field 24","");
        recordInputs("0.5","","Field 25","");
        recordInputs("Until: 17:00","","Field 26","");
        recordInputs("0.35","","Field 27","");
        recordInputs("Until: 24:00","","Field 28","");
        recordInputs("0.3","","Field 29","");
        recordInputs("For SummerDesignDay","","Field 30","");
        recordInputs("Until: 24:00","","Field 31","");
        recordInputs("1.0","","Field 32","");
        recordInputs("For: WinterDesignDay","","Field 33","");
        recordInputs("Until: 24:00","","Field 34","");
        recordInputs("0.0","","Field 35","");
        recordInputs("For: AllOtherDays","","Field 36","");
        recordInputs("Until: 24:00","","Field 37","");
        recordInputs("0.3","","Field 38","");   
        addObject(file);
    }
    
    
    /**
     * Add heating setpoint schedule from default
     * Add a default office schedule to the energy model
     * Currently, there is only one option: work 6 days and everyday 18 hours.
     * More option should add in according to user inputs.
     * TODO add one more variable to identify the preferred schedule
     * @param scheduleName
     * @param scheduleId
     * @param file
     */
    public void addHeatingSchedule(String scheduleName, String scheduleId, IDFFileObject file){
        bs_idToObjectMap.put(scheduleId, scheduleName);
        recordInputs("Schedule:Compact","","","");
        recordInputs(scheduleName,"","Name","");
        recordInputs("Temperature","","Schedule Type Limits name","");
        recordInputs("Through: 12/31","","Field 1","");
        recordInputs("For: Weekdays","","Field 2","");
        recordInputs("Until: 06:00","","Field 3","");
        recordInputs("15.6","","Field 4","");
        recordInputs("Until: 22:00","","Field 5","");
        recordInputs("21","","Field 6","");
        recordInputs("Until: 24:00","","Field 7","");
        recordInputs("15.6","","Field 8","");
        recordInputs("For: Saturday","","Field 9","");
        recordInputs("Until: 06:00","","Field 10","");
        recordInputs("15.6","","Field 11","");
        recordInputs("Until: 18:00","","Field 12","");
        recordInputs("21.0","","Field 13","");
        recordInputs("Until: 24:00","","Field 14","");
        recordInputs("15.6","","Field 15","");
        recordInputs("For SummerDesignDay","","Field 16","");
        recordInputs("Until: 24:00","","Field 17","");
        recordInputs("15.6","","Field 18","");
        recordInputs("For: WinterDesignDay","","Field 19","");
        recordInputs("Until: 24:00","","Field 20","");
        recordInputs("21.0","","Field 21","");
        recordInputs("For: AllOtherDays","","Field 22","");
        recordInputs("Until: 24:00","","Field 23","");
        recordInputs("15.6","","Field 24","");   
        addObject(file);
    }
    
    /**
     * Add cooling setpoint schedule from default
     * Add a default office schedule to the energy model
     * Currently, there is only one option: work 6 days and everyday 18 hours.
     * More option should add in according to user inputs.
     * TODO add one more variable to identify the preferred schedule
     * @param scheduleName
     * @param scheduleId
     * @param file
     */
    public void addCoolingSchedule(String scheduleName, String scheduleId, IDFFileObject file){
        bs_idToObjectMap.put(scheduleId, scheduleName);
        recordInputs("Schedule:Compact","","","");
        recordInputs(scheduleName,"","Name","");
        recordInputs("Temperature","","Schedule Type Limits name","");
        recordInputs("Through: 12/31","","Field 1","");
        recordInputs("For: Weekdays SummerDesignDay","","Field 2","");
        recordInputs("Until: 06:00","","Field 3","");
        recordInputs("26.7","","Field 4","");
        recordInputs("Until: 22:00","","Field 5","");
        recordInputs("24","","Field 6","");
        recordInputs("Until: 24:00","","Field 7","");
        recordInputs("26.7","","Field 8","");
        recordInputs("For: Saturday","","Field 9","");
        recordInputs("Until: 06:00","","Field 10","");
        recordInputs("26.7","","Field 11","");
        recordInputs("Until: 18:00","","Field 12","");
        recordInputs("24.0","","Field 13","");
        recordInputs("Until: 24:00","","Field 14","");
        recordInputs("26.7","","Field 15","");
        recordInputs("For: WinterDesignDay","","Field 19","");
        recordInputs("Until: 24:00","","Field 20","");
        recordInputs("26.7","","Field 21","");
        recordInputs("For: AllOtherDays","","Field 22","");
        recordInputs("Until: 24:00","","Field 23","");
        recordInputs("26.7","","Field 24","");   
        addObject(file);
    }
    
    private String translateScheduleWeek(Element element, Element body, IDFFileObject file){
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> units = new ArrayList<String>();
        ArrayList<String> comments = new ArrayList<String>();
        ArrayList<String> topComments = new ArrayList<String>();
        
        lines.add("Schedule:Week:Daily");
        units.add("");
        comments.add("");
        
        String id = element.getAttributeValue("id");
        //String type = element.attr("type"); weekschedule do not need to translate the type
        
        String name = element.getChildText("Name",ns);
        name = escapeName(id, name);
        bs_idToObjectMap.put(id, name);
        lines.add(name);
        units.add("");
        comments.add("Name");
        
        //no need to translate type
        
        List<Element> dayElements = element.getChildren("Day",ns);
        HashMap<String, String> dayTypeMap = new HashMap<String, String>();
        for(int i=0; i<dayElements.size(); i++){
            String dayType = dayElements.get(i).getAttributeValue("dayType");
            String dayScheduleIdRef = dayElements.get(i).getAttributeValue("dayScheduleIdRef");
            
            List<Element> dayScheduleElementList = body.getChildren("DaySchedule",ns);
            for(int j=0; j<dayScheduleElementList.size(); j++){
                Element dayScheduleElement = dayScheduleElementList.get(j);
                if(dayScheduleElement.getAttributeValue("id").equals(dayScheduleIdRef)){
                    String dayScheduleName = translateScheduleDay(dayScheduleElement, body, file);
                    dayTypeMap.put(dayType, dayScheduleName);
                    break;
                }
            }
        }
        //fill the dates
        String sunday = "";
        String monday = "";
        String tuesday = "";
        String wednesday = "";
        String thursday = "";
        String friday = "";
        String saturday = "";
        String holiday = "";
        String summerDesignDay = "";
        String winterDesignDay = "";
        String customDay1 = "";
        String customDay2 = "";
        
        if(dayTypeMap.containsKey("All")){
            sunday = dayTypeMap.get("All");
            monday = dayTypeMap.get("All");
            tuesday = dayTypeMap.get("All");
            wednesday = dayTypeMap.get("All");
            thursday = dayTypeMap.get("All");
            friday = dayTypeMap.get("All");
            saturday = dayTypeMap.get("All");
            holiday = dayTypeMap.get("All");
            summerDesignDay = dayTypeMap.get("All");
            winterDesignDay = dayTypeMap.get("All");
            customDay1 = dayTypeMap.get("All");
            customDay2 = dayTypeMap.get("All");
        }
        
        if(dayTypeMap.containsKey("Weekday")){
            monday = dayTypeMap.get("Weekday");
            tuesday = dayTypeMap.get("Weekday");
            wednesday = dayTypeMap.get("Weekday");
            thursday = dayTypeMap.get("Weekday");
            friday = dayTypeMap.get("Weekday");
            customDay1 = dayTypeMap.get("Weekday");
            customDay2 = dayTypeMap.get("Weekday");
        }
        
        if(dayTypeMap.containsKey("Weekend")){
            sunday = dayTypeMap.get("Weekend");
            saturday = dayTypeMap.get("Weekend");
            customDay1 = dayTypeMap.get("Weekend");
            customDay2 = dayTypeMap.get("Weekend");
        }
        
        if(dayTypeMap.containsKey("Holiday")){
            holiday = dayTypeMap.get("Holiday");
        }
        
        if(dayTypeMap.containsKey("WeekendOrHoliday")){
            sunday = dayTypeMap.get("WeekendOrHoliday");
            saturday = dayTypeMap.get("WeekendOrHoliday");
            holiday = dayTypeMap.get("WeekendOrHoliday");
            customDay1 = dayTypeMap.get("WeekendOrHoliday");
            customDay2 = dayTypeMap.get("WeekendOrHoliday");
        }
        
        if(dayTypeMap.containsKey("HeatingDesignDay")){
            winterDesignDay = dayTypeMap.get("HeatingDesignDay");
        }
        
        if(dayTypeMap.containsKey("CoolingDesignDay")){
            summerDesignDay = dayTypeMap.get("CoolingDesignDay");
        }
        
        if(dayTypeMap.containsKey("Sun")){
            sunday = dayTypeMap.get("Sun");
        }
        
        if(dayTypeMap.containsKey("Mon")){
            monday = dayTypeMap.get("Mons");
        }
        
        if(dayTypeMap.containsKey("Tue")){
            tuesday = dayTypeMap.get("Tue");
        }
        
        if(dayTypeMap.containsKey("Wed")){
            wednesday = dayTypeMap.get("Wed");
        }
        
        if(dayTypeMap.containsKey("Thu")){
            thursday = dayTypeMap.get("Thu");
        }
        
        if(dayTypeMap.containsKey("Fri")){
            friday = dayTypeMap.get("Fri");
        }
        
        if(dayTypeMap.containsKey("Sat")){
            saturday = dayTypeMap.get("Sat");
        }
        
        //now fill in the object in a order
        lines.add(sunday);
        units.add("");
        comments.add("Sunday Schedule:Day");
        
        lines.add(monday);
        units.add("");
        comments.add("Monday Schedule:Day");
        
        lines.add(tuesday);
        units.add("");
        comments.add("Tuesday Schedule:Day");
        
        lines.add(wednesday);
        units.add("");
        comments.add("Wednesday Schedule:Day");
        
        lines.add(thursday);
        units.add("");
        comments.add("Thursday Schedule:Day");
        
        lines.add(friday);
        units.add("");
        comments.add("Friday Schedule:Day");
        
        lines.add(saturday);
        units.add("");
        comments.add("Saturday Schedule:Day");
        
        lines.add(holiday);
        units.add("");
        comments.add("Holiday Schedule:Day");
        
        lines.add(summerDesignDay);
        units.add("");
        comments.add("SummerDesignDay Schedule:Day");
        
        lines.add(winterDesignDay);
        units.add("");
        comments.add("WinterDesignDay Schedule:Day");
        
        lines.add(customDay1);
        units.add("");
        comments.add("CustomDay1 Schedule:Day");
        
        lines.add(customDay2);
        units.add("");
        comments.add("CustomDay2 Schedule:Day");
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        
        return name;
    }
    
    private String translateScheduleDay(Element element, Element body, IDFFileObject file){
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> units = new ArrayList<String>();
        ArrayList<String> comments = new ArrayList<String>();
        ArrayList<String> topComments = new ArrayList<String>();
        
        lines.add("Schedule:Day:Interval");
        units.add("");
        comments.add("");
        
        String id = element.getAttributeValue("id");
        String type = element.getAttributeValue("type");
        
        String name = "";//no name element
        name = escapeName(id, name);
        bs_idToObjectMap.put(id, name);
        lines.add(name);
        units.add("");
        comments.add("Name");
        //pre-condition: this must be processed in the year schedule
        //if not, then an error handling should be raised.
        String scheduleLimitType = bs_idToObjectMap.get(type);
        
        if(scheduleLimitType==null){
            //ERROR: day schedule has a type that does not match the year schedule type
            //Stop processing
        		scheduleLimitType = "Any Number";
        }
        
        lines.add(scheduleLimitType);
        units.add("");
        comments.add("Schedule Type Limits Name");
        
        lines.add("No");
        units.add("");
        comments.add("Interpolate to Timestep"); //no interpolate allowed in the conversion
        
        List<Element> valueElements = element.getChildren("ScheduleValue",ns);
        if(24%valueElements.size()!=0){
            //ERROR: the number of schedule value does not represents 24 hours. + "valueElements.size()"
            //this should be "2,3,4,6,8,12".
            //STOP Processing
        }
        int repeatTime = 24 / valueElements.size();//in a day there is 24 hours
        int timeCounter = 0;
        for(int i=0; i<valueElements.size(); i++){
            for(int j=0; j<repeatTime; j++){
                lines.add(timeInterval[timeCounter]);
                units.add("hh:mm");
                comments.add("Time " + timeCounter);
                
                lines.add(valueElements.get(i).getText());
                units.add("");
                comments.add("Value Until Time " + timeCounter);
                
                timeCounter++;
            }
        }
        
        file.addObject(new IDFObject(lines, units, comments, topComments));
        return name;
    }

    private String createScheduleTypeLimit(String type, IDFFileObject file){
    		if(type==null) {
    			type = "ActivityLevel";//default to any number
    		}
        if(bs_idToObjectMap.containsKey(type.toUpperCase())){
            return null;
        }else{
            String name = null;
            ArrayList<String> lines = new ArrayList<String>();
            ArrayList<String> units = new ArrayList<String>();
            ArrayList<String> comments = new ArrayList<String>();
            ArrayList<String> topComments = new ArrayList<String>();
            if(type.equalsIgnoreCase("Temp")){
                lines.add("ScheduleTypeLimits");
                units.add("");
                comments.add("");
                
                name = "Temperature";
                lines.add(name);
                units.add("");
                comments.add("Name");
                
                lines.add("-60");
                units.add("");
                comments.add("Lower Limit Value");
                
                lines.add("200");
                units.add("");
                comments.add("Upper Limit Value");
                
                lines.add("CONTINUOUS");
                units.add("");
                comments.add("Numeric Type");
                
                bs_idToObjectMap.put(type, "Temperature");
            }else if(type.equalsIgnoreCase("Fraction")){
                lines.add("ScheduleTypeLimits");
                units.add("");
                comments.add("");
                
                name = "Fraction";
                lines.add(name);
                units.add("");
                comments.add("Name");
                
                lines.add("0.0");
                units.add("");
                comments.add("Lower Limit Value");
                
                lines.add("1.0");
                units.add("");
                comments.add("Upper Limit Value");
                
                lines.add("CONTINUOUS");
                units.add("");
                comments.add("Numeric Type");
                
                bs_idToObjectMap.put(type, "Fraction");
            }else if(type.equalsIgnoreCase("OnOff")){
                lines.add("ScheduleTypeLimits");
                units.add("");
                comments.add("");
                
                name = "On/Off";
                lines.add(name);
                units.add("");
                comments.add("Name");
                
                lines.add("0");
                units.add("");
                comments.add("Lower Limit Value");
                
                lines.add("1");
                units.add("");
                comments.add("Upper Limit Value");
                
                lines.add("DISCRETE");
                units.add("");
                comments.add("Numeric Type");
                
                bs_idToObjectMap.put(type, "On/Off");
            }else if(type.equalsIgnoreCase("ResetTemp")){
                lines.add("ScheduleTypeLimits");
                units.add("");
                comments.add("");
                
                name = "Reset Temperature";
                lines.add(name);
                units.add("");
                comments.add("Name");
                
                lines.add("-60");
                units.add("");
                comments.add("Lower Limit Value");
                
                lines.add("200");
                units.add("");
                comments.add("Upper Limit Value");
                
                lines.add("CONTINUOUS");
                units.add("");
                comments.add("Numeric Type");
                
                bs_idToObjectMap.put(type, "Reset Temperature");
            }else if(type.equalsIgnoreCase("ActivityLevel")){
                lines.add("ScheduleTypeLimits");
                units.add("");
                comments.add("");
                
                name = "Any Number";
                lines.add(name);
                units.add("");
                comments.add("Name");
                
                bs_idToObjectMap.put(type, "Activity Level");
            }
            file.addObject(new IDFObject(lines, units, comments, topComments));
            
            return name;
        }
    }

    private String escapeName(String id, String name) {
        String value = id;
        if (name!=null && !name.isEmpty()) {
            value = name;
        }

        return value.replace(",", "-").replace(";", "-");
    }
    
    private void recordInputs(String line, String unit, String comment, String topComments){
        lines.add(line);
        units.add(unit);
        comments.add(comment);
    }
    
    private void addObject(IDFFileObject file){
        file.addObject(new IDFObject(lines,units, comments, topComments));
        lines.clear();
        units.clear();
        comments.clear();
        topComments.clear();
    }
}
