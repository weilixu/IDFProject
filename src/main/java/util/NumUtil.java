package main.java.util;

import com.google.gson.JsonElement;

import java.text.DecimalFormat;

public class NumUtil {
	private static long GB = 1 << 30;
	private static long MB = 1 << 20;
	private static long KB = 1 << 10;
	private static DecimalFormat df0 = new DecimalFormat("#");
	private static DecimalFormat df2 = new DecimalFormat("#.##");
	private static DecimalFormat df4 = new DecimalFormat("#.####");

	public static final double MAX_VALUE = Double.MAX_VALUE;
	public static final double MIN_VALUE = -(Double.MAX_VALUE-1);

	public static double readDouble(String num, double defaultVal) {
		if (num == null || num.isEmpty()) {
			return defaultVal;
		}

		try {
			return Double.valueOf(num);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	public static int readInt(String num, int defaultVal) {
		if (num == null || num.isEmpty()) {
			return defaultVal;
		}

		try{
			return Integer.parseInt(num);
		}catch (NumberFormatException e){
			try {
				Double d = Double.parseDouble(num);
				return d.intValue();
			}catch (NumberFormatException ex){
				return defaultVal;
			}
		}
	}
	
	public static int readVersion(String version, int defaultVal) {
		if(version == null || version.isEmpty()) {
			return defaultVal;
		}
		
		String versionNoDot = version.replace(".","");
		try {
			return Integer.parseInt(versionNoDot);
		}catch (NumberFormatException e) {
			try {
				Double d = Double.parseDouble(versionNoDot);
				return d.intValue();
			}catch(NumberFormatException ex) {
				return defaultVal;
			}
		}
	}

    public static int readInt(JsonElement je, int defaultVal){
        if(je==null || je.isJsonNull()){
            return defaultVal;
        }
        return readInt(je.getAsString(), defaultVal);
    }

    public static String formatNumber2(Number n){
        return df2.format(n.doubleValue());
    }

	public static String formatNumber4(Number n) {
		return df4.format(n.doubleValue());
	}

	public static String readableSize(long size) {
		if (size >= GB) {
			return df2.format((double) size / GB) + " GB";
		}
		if (size >= MB) {
			return df2.format((double) size / MB) + " MB";
		}
		if (size >= KB) {
			return df2.format((double) size / KB) + " KB";
		}
		return size + " Bytes";
	}

	public static String calRate(Number n1, Number n2) {
		double d1 = n1.doubleValue();
		double d2 = n2.doubleValue();

		if(d2==0d){
			return "0%";
		}

		double rate = d1 / d2 * 100;
		return df0.format(rate) + "%";
	}

	/**
	 * E.g. [0.3, 0.4, 0.5]
	 * 
	 * @return
	 */
	public static double[] readDoubleArray(String str) {
		if(str.equals("default")) {
			//skip the default key word
			return new double[0];
		}
		
		str = str.substring(1, str.length() - 1); // trim [ and ]
		if (str.length() > 0) {
			String[] nums = str.split(",");
			double[] res = new double[nums.length];
			for (int i = 0; i < res.length; i++) {
				res[i] = readDouble(nums[i].trim(), 0);
			}
			return res;
		}
		
		return new double[0];
	}

	public static double parseDouble(String num){
	    double res = Double.parseDouble(num);
	    if(res==Double.NEGATIVE_INFINITY){
            res = NumUtil.MIN_VALUE;
        }
        return res;
    }
}