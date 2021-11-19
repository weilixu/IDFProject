package main.java.model.weather;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.ServerConfig;

public class WeatherFileToWeatherZone {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherFileToWeatherZone.class);
    
    //private static Map<String, String> fileToZone = new HashMap<>();
    
    public static String getZone(String country, String state, String fileName){
        File climateZones = null;
        String platform = ServerConfig.readProperty("platform");
        //TODO Read weatherfile in local.
//        if(platform.equalsIgnoreCase("aws")){
//            String key = country+"/"+state;
//            S3FileDownloader fileDownloader = new S3FileDownloader(null);
//            climateZones = fileDownloader.download(ServerConfig.readProperty("WeatherFileS3"), key, "climateZones.txt");
//        }else if(platform.equalsIgnoreCase("azure")){
//            String key = country+"/"+state;
//            AzureFileDownloader fileDownloader = new AzureFileDownloader();
//            climateZones = fileDownloader.download(ServerConfig.readProperty("WeatherFileAzure"), key, "climateZones.txt");
//        }else {
//            climateZones = new File(ServerConfig.readProperty("WeatherFilePath")+country+"\\"+state+"\\climateZones.txt");
//        }

        try(FileInputStream fis = new FileInputStream(climateZones);
                InputStreamReader isr = new InputStreamReader(fis, "utf-8");
                BufferedReader br = new BufferedReader(isr)){
            String line;
            while((line=br.readLine()) != null){
                if(line.startsWith(fileName)){
                    String weatherZone = line.substring(line.indexOf("|")+1);
                    //fileToZone.put(fileName, weatherZone);
                    return weatherZone;
                }
            }
        }catch (IOException e){
            LOG.error("Read climateZones text file failed", e);
        }
        
        return null;
    }
}
