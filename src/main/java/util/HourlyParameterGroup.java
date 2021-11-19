package main.java.util;

public final class HourlyParameterGroup {
	//this class contains regularly seen parameters
	//private static HashMap<String, List<String>> groupVariableMap;
	
	public static String[] getPumpVariables(){
		String[] pumpVar = new String[]{
			"Pump Electric Energy",
			"Pump Electric Power",
			"Pump Shaft Power",
			"Pump Fluid Heat Gain Rate",
			"Pump Outlet Temperature",
			"Pump Mass Flow Rate"
		};
		
		return pumpVar;
	}
	
	public static String[] getChillerVariables() {
		String[] chillerVar = new String[]{
				"Chiller Part Load Ratio",
				"Chiller Cycling Ratio",
				"Chiller Electric Power",
				"Chiller Electric Energy",
				"Chiller Evaporator Cooling Rate",
				"Chiller Evaporator Cooling Energy",
				"Chiller False Load Heat Transfer Rate",
				"Chiller False Load Heat Transfer Energy",
				"Chiller Evaporator Inlet Temperature",
				"Chiller Evaporator Outlet Temperature",
				"Chiller Evaporator Mass Flow Rate",
				"Chiller Condenser Heat Transfer Rate",
				"Chiller Condenser Heat Transfer Energy",
				"Chiller COP",
				"Chiller Capacity Temperature Modifier Multiplier",
				"Chiller EIR Temperature Modifier Multiplier",
				"Chiller EIR Part Load Modifier Multiplier",
				"Chiller Condenser Inlet Temperature",
				"Chiller Condenser Outlet Temperature",
				"Chiller Condenser Mass Flow Rate"
			};
		
		return chillerVar;
	}
	
	public static String[] getBoilerVariables() {
		String[] boilerVar = new String[]{
				"Boiler Heating Rate",
				"Boiler Heating Energy",
				"Boiler Gas Rate",
				"Boiler Gas Energy",
				"Boiler Inlet Temperature",
				"Boiler Outlet Temperature",
				"Boiler Mass Flow Rate",
				"Boiler Ancillary Electric Power",
				"Boiler Ancillary Electric Energy",
				"Boiler Part Load Ratio"
			};
		
		return boilerVar;
	}
	
	public static String[] getCoolingTowerVariables() {
		String[] coolTowerVar = new String[]{
				"Cooling Tower Inlet Temperature",
				"Cooling Tower Outlet Temperature",
				"Cooling Tower Mass Flow Rate",
				"Cooling Tower Heat Transfer Rate",
				"Cooling Tower Fan Electric Power",
				"Cooling Tower Fan Electric Energy",
				"Cooling Tower Bypass Fraction",
				"Cooling Tower Operating Cells Count",
				"Cooling Tower Fan Cycling Ratio",
				"Cooling Tower Make Up Water Volume Flow Rate",
				"Cooling Tower Make Up Water Volume",
				"Cooling Tower Make Up Mains Water Volume",
				"Cooling Tower Water Evaporation Volume Flow Rate",
				"Cooling Tower Water Evaporation Volume",
				"Cooling Tower Water Drift Volume Flow Rate",
				"Cooling Tower Water Drift Volume",
				"Cooling Tower Water Blowdown Volume Flow Rate",
				"Cooling Tower Water Blowdown Volume"
			};
		
		return coolTowerVar;
	}
	
	public static String[] performanceCurveVariables() {
		String[] performanceCurv = new String[]{
				"Performance Curve Input Variable 1 Value",
				"Performance Curve Input Variable 2 Value",
			};
		
		return performanceCurv;
	}
	
	public static String[] systemNodeVariables() {
		String[] sysNodeVar = new String[]{
				"System Node Temperature",
				"System Node Mass Flow Rate",
				"System Node Humidity Ratio",
				"System Node Setpoint Temperature",
				"System Node Setpoint High Temperature",
				"System Node Setpoint Low Temperature",
				"System Node Setpoint Humidity Ratio",
				"System Node Setpoint Minimum Humidity Ratio",
				"System Node Setpoint Maximum Humidity Ratio",
				"System Node Relative Humidity",
				"System Node Pressure",
				"System Node Standard Density Volume Flow Rate",
				"System Node Current Density Volume Flow Rate",
				"System Node Current Density",
				"System Node Enthalpy",
				"System Node Wetbulb Temperature",
				"System Node Dewpoint Temperature",
				"System Node Quality",
				"System Node Height",
				"System Node Minimum Temperature",
				"System Node Maximum Temperature",
				"System Node Minimum Limit Mass Flow Rate",
				"System Node Maximum Limit Mass Flow Rate",
				"System Node Minimum Available Mass Flow Rate",
				"System Node Maximum Available Mass Flow Rate",
				"System Node Setpoint Mass Flow Rate",
				"System Node Requested Mass Flow Rate",
				"System Node Last Timestep Temperature",
				"System Node Last Timestep Enthalpy"
			};
		return sysNodeVar;
	}
	
	public static String[] airSystemVariables() {
		String[] airSystemVar = new String[]{
				"Air System Total Heating Energy",
				"Air System Total Cooling Energy",
				"Air System Hot Water Energy",
				"Air System Steam Energy",
				"Air System Chilled Water Energy",
				"Air System Electric Energy",
				"Air System Gas Energy",
				"Air System Water Volume",
				"Air System Fan Air Heating Energy",
				"Air System Cooling Coil Total Cooling Energy",
				"Air System Heating Coil Total Heating Energy",
				"Air System Heat Exchanger Total Heating Energy",
				"Air System Heat Exchanger Total Cooling Energy",
				"Air System Solar Collector Total Heating Energy",
				"Air System Solar Collector Total Cooling Energy",
				"Air System User Defined Air Terminal Total Heating Energy",
				"Air System User Defined Air Terminal Total Cooling Energy",
				"Air System Humidifier Total Heating Energy",
				"Air System Evaporative Cooler Total Cooling Energy",
				"Air System Desiccant Dehumidifier Total Cooling Energy",
				"Air System Fan Electric Energy",
				"Air System Heating Coil Hot Water Energy",
				"Air System Cooling Coil Chilled Water Energy",
				"Air System DX Heating Coil Electric Energy",
				"Air System DX Cooling Coil Electric Energy",
				"Air System Heating Coil Electric Energy",
				"Air System Heating Coil Gas Energy",
				"Air System Heating Coil Steam Energy",
				"Air System Humidifier Electric Energy",
				"Air System Evaporative Cooler Electric Energy",
				"Air System Desiccant Dehumidifier Electric Energy"
			};
		return airSystemVar;
	}

}
