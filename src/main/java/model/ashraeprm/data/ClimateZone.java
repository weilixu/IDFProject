package main.java.model.ashraeprm.data;

import main.java.util.StringUtil;

public enum ClimateZone {
	CLIMATEZONE1A("Climate Zone 1 A"), 
	CLIMATEZONE1B("Climate Zone 1 B"), 
	CLIMATEZONE2A("Climate Zone 2 A"), 
	CLIMATEZONE2B("Climate Zone 2 B"), 
	CLIMATEZONE3A("Climate Zone 3 A"), 
	CLIMATEZONE3B("Climate Zone 3 B"), 
	CLIMATEZONE3C("Climate Zone 3 C"), 
	CLIMATEZONE4A("Climate Zone 4 A"), 
	CLIMATEZONE4B("Climate Zone 4 B"), 
	CLIMATEZONE4C("Climate Zone 4 C"), 
	CLIMATEZONE5A("Climate Zone 5 A"), 
	CLIMATEZONE5B("Climate Zone 5 B"), 
	CLIMATEZONE5C("Climate Zone 5 C"), 
	CLIMATEZONE6A("Climate Zone 6 A"), 
	CLIMATEZONE6B("Climate Zone 6 B"), 
	CLIMATEZONE7A("Climate Zone 7 A"), 
	CLIMATEZONE7B("Climate Zone 7 B"), 
	CLIMATEZONE8A("Climate Zone 8 A"), 
	CLIMATEZONE8B("Climate Zone 8 B");

	private String cZone;

	private ClimateZone(String cZone) {
		this.cZone = cZone;
	}

	/**
	 * -1 means this climate zone does not require economizer
	 * 
	 * Outdoor air economizers shall be included in baseline HVAC Systems 3 through
	 * 8 based on climate as specified in Table G3.1.2.6A.
	 * 
	 * @return
	 */
	public double getEconomizerShutoffLimit() {
		if (cZone.equals("Climate Zone 1 A") || cZone.equals("Climate Zone 1 B") || cZone.equals("Climate Zone 2 A")
				|| cZone.equals("Climate Zone 3 A") || cZone.equals("Climate Zone 4 A")) {
			return -1.0;
		} else {
			if (cZone.equals("Climate Zone 2 B") || cZone.equals("Climate Zone 3 B") || cZone.equals("Climate Zone 3 C")
					|| cZone.equals("Climate Zone 4 B") || cZone.equals("Climate Zone 4 C")
					|| cZone.equals("Climate Zone 5 B") || cZone.equals("Climate Zone 5 C")
					|| cZone.equals("Climate Zone 6 B") || cZone.equals("Climate Zone 7 B")
					|| cZone.equals("Climate Zone 8 A") || cZone.equals("Climate Zone 8 B")) {
				return 23.89;
			} else if (cZone.equals("Climate Zone 5 A") || cZone.equals("Climate Zone 6 A")
					|| cZone.equals("Climate Zone 7 A")) {
				return 21.11;
			} else {
				return 18.33;
			}
		}
	}

	@Override
	public String toString() {
		return cZone;
	}

	public static ClimateZone lookup(String climateZone){
	    if(StringUtil.isNullOrEmpty(climateZone)){
	        return null;
        }

		switch (climateZone.toUpperCase()){
			case "1A":
				return CLIMATEZONE1A;
			case "1B":
				return CLIMATEZONE1B;
			case "2A":
				return CLIMATEZONE2A;
			case "2B":
				return CLIMATEZONE2B;
			case "3A":
				return CLIMATEZONE3A;
			case "3B":
				return CLIMATEZONE3B;
			case "3C":
				return CLIMATEZONE3C;
			case "4A":
				return CLIMATEZONE4A;
			case "4B":
				return CLIMATEZONE4B;
			case "4C":
				return CLIMATEZONE4C;
			case "5A":
				return CLIMATEZONE5A;
			case "5B":
				return CLIMATEZONE5B;
			case "5C":
				return CLIMATEZONE5C;
			case "6A":
				return CLIMATEZONE6A;
			case "6B":
				return CLIMATEZONE6B;
			case "7A":
				return CLIMATEZONE7A;
			case "7B":
				return CLIMATEZONE7B;
			case "8A":
				return CLIMATEZONE8A;
			case "8B":
				return CLIMATEZONE8B;
			default:
				return null;
		}
	}
}
