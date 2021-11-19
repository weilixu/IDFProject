package main.java.model.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class EPWReader {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	public static final String LOC_LABEL = "LOCATION";
	
	private String city;
	private String state;
	private String country;
	private String source;
	private String wmoid;
	private Double latitude;
	private Double longitude;
	private Double elevation;
	private Double timeZone;
	private String zipCode = "";

    public EPWReader(File weatherFile) {
        String line;
        try (FileReader fileReader = new FileReader(weatherFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader)){
            while((line = bufferedReader.readLine()) != null) {
                //TODO only process the location line is sufficient for the current application
				line = line.trim();
				if(line.startsWith(LOC_LABEL)) {
					String[] locInfo = line.split(",");
					if(locInfo.length > 7) {
						city = locInfo[1].trim();
						state = locInfo[2].trim();
						country = locInfo[3].trim();
						source = locInfo[4].trim();
						wmoid = locInfo[5].trim();
						latitude = Double.parseDouble(locInfo[6].trim());
						longitude = Double.parseDouble(locInfo[7].trim());
					}else {
						city = "Pittsburgh";
						state = "PA";
						country = "USA";
						source = "unknwon";
						wmoid = "725200";
						latitude = 40.4406;
						longitude = -79.9959;

					}

					if(locInfo.length >7) {
						timeZone = Double.parseDouble(locInfo[6].trim());
						elevation = Double.parseDouble(locInfo[7].trim());
					}else {
						timeZone = -5.0;
						elevation = 461.0;
					}

					break;
				}
            }
        } catch(FileNotFoundException ex) {
            LOG.error("Unable to open file '" + weatherFile.getName() + "'. "+ex.getMessage(), ex);
        } catch(IOException ex) {
            LOG.error("Error reading file '" + weatherFile.getName() + "'. "+ex.getMessage(), ex);
        }
    }
    
    public String getWMO() {
    		return wmoid;
    }

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getCountry() {
		return country;
	}

	public String getSource() {
		return source;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public Double getElevation() {
		return elevation;
	}

	public Double getTimeZone() {
		return timeZone;
	}

	public String getZipCode() { return zipCode; }
}
