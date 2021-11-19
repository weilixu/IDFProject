package main.java.util;

public class BenchMarkUnitUtil {
	
	//units
	private static final String JTOKWH = "JtoKWH";
	//private static final String JTOMJ = "JtoMJ";
	//private static final String JTOGJ = "JtoGJ";
	private static final String INCHPOUND = "InchPound";
	
	public static double getEUIConversionFactor(String unit){
		if(unit==null) {
			return 3.6;     // if null, it's default back to None, which equals to MJ
		}
		
		//default is kWh/m2
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 1.0;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 0.316998;
		}else {
			return 3.6;
		}
	}
	
	public static double getEUIConversionFactorsFromMJUnitSystem(String unit){
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 0.27778;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 0.08805505392;
		}else {
			return 1.0;
		}
	}
	
	public static double getEUIConversionFactorsFromGJUnitSystem(String unit){
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 277.778;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 88.05505392;
		}else {
			return 1.0;
		}
	}
	
	public static String getEUIUnitString(String unit){
		if(unit == null) {
			return "";
		}
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "kWh/m2";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "kBtu/ft2";
		}else {
			return "MJ/m2";
		}
	}
	
	public static String getEUIUnitStringFromMJUnitSystem(String unit){
		if(unit == null) {
			return "";
		}
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "kWh/m2";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "kBtu/ft2";
		}else {
			return "MJ/m2";
		}
	}
	
	public static String getEUIUnitStringFromGJUnitSystem(String unit){
		if(unit == null) {
			return "";
		}
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "kWh/m2";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "kBtu/ft2";
		}else {
			return "GJ/m2";
		}
	}

	public static double getAreaConversionFactor(String unit){
		if(unit==null) {
			return 1d;
		}
		//default is m2
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 1.0;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 10.7639;
		}else {
			return 1.0;
		}
	}
	
	public static String getAreaUnitString(String unit){
		if(unit==null) {
			return "";
		}
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "m2";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "ft2";
		}else {
			return "m2";
		}
	}
	
	public static double getEnergyConversionFactor(String unit){
		if(unit==null) {
			return 0.0036;
		}
		//default is kWh
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 1.0;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 3.4121;
		}else {
			return 0.0036;
		}
	}
	
	public static String getEnergyUnitString(String unit){
		if(unit==null) {
			return "";
		}
		//default is kWh
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "kWh";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "kBtu";
		}else {
			return "GJ";
		}
	}
	
	public static double getUValueConversionFactor(String unit){
		if(unit==null) {
			return 1d;
		}
		//default is W/m2-K
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 1.0;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 0.176;
		}else {
			return 1.0;
		}
	}
	
	public static String getUValueUnitString(String unit){
		if(unit==null) {
			return "";
		}
		//default is kWh
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "W/m2-K";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "Btu/h-ft2-F";
		}else {
			return "W/m2-K";
		}
	}
	
	public static double getPowerMeterConversionFactor(String unit){
		if(unit==null) {
			return 1d;
		}
		//default is W/m2
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 1.0;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 0.316998;
		}else {
			return 1.0;
		}
	}
	
	public static String getPowerMeterUnitString(String unit){
		if(unit==null) {
			return "";
		}
		//default is kW
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "kW/m2";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "Btu/h-ft2";
		}else {
			return "kW/m2";
		}
	}
	
	public static double getVolumeConversionFactor(String unit){
		if(unit==null) {
			return 1d;
		}
		//default is m3
		if(unit.equalsIgnoreCase(JTOKWH)){
			return 1.0;
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return 35.3147;
		}else {
			return 1.0;
		}
	}
	
	public static String getVolumeUnitString(String unit){
		if(unit==null) {
			return "";
		}
		//default is kW
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "m3";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "ft3";
		}else {
			return "m3";
		}
	}
	
	//all the other units name conversions
	public static String getPeopleUnitString(String unit){
		if(unit==null) {
			return "";
		}
		//default is m2 per person
		if(unit.equalsIgnoreCase(JTOKWH)){
			return "m2 per person";
		}else if(unit.equalsIgnoreCase(INCHPOUND)){
			return "ft2 per person";
		}else {
			return "m2 per person";
		}
	}

}
