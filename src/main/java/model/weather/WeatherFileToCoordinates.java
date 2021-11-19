package main.java.model.weather;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import main.java.config.ServerConfig;

public class WeatherFileToCoordinates {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherFileToCoordinates.class);
    
    public static double[] getCoordinates(String country, String state, String fileName){
        File weatherFile = null;
        String platform = ServerConfig.readProperty("platform");
        //TODO - Read weather file from local

//        if(platform.equalsIgnoreCase("aws")){
//            String key = country+"/"+state;
//            S3FileDownloader fileDownloader = new S3FileDownloader(null);
//            weatherFile = fileDownloader.download(ServerConfig.readProperty("WeatherFileS3"), key, fileName+".stat");
//        }else if(platform.equalsIgnoreCase("azure")){
//            String key = country+"/"+state;
//            AzureFileDownloader fileDownloader = new AzureFileDownloader();
//            weatherFile = fileDownloader.download(ServerConfig.readProperty("WeatherFileAzure"), key, fileName+".stat");
//        }else {
//            weatherFile = new File(ServerConfig.readProperty("WeatherFilePath")+country+"\\"+state+"\\"+fileName+".stat");
//        }
        try(FileInputStream fis = new FileInputStream(weatherFile);
                InputStreamReader isr = new InputStreamReader(fis, "utf-8");
                BufferedReader br = new BufferedReader(isr)){
            String line;
            while((line=br.readLine()) != null){
                line = line.trim();
                if(line.startsWith("{")){
                    String[] coord = line.split("\\{");
                    String lat = coord[1].substring(coord[1].indexOf("{")+1, coord[1].indexOf("}"));
                    String log = coord[2].substring(coord[2].indexOf("{")+1, coord[2].indexOf("}"));

                    return new double[]{readDegree(lat.trim()), readDegree(log.trim())};
                }
            }
        }catch (IOException e){
            LOG.error("Read climateZones text file failed", e);
        }
        
        return null;
    }

    private static double readDegree(String number){
        StringBuilder res = new StringBuilder();

        char[] chars = number.toCharArray();
        if(chars[0]=='S' || chars[0]=='W'){
            res.append("-");
        }

        int i=1;
        while(chars[i]==' '){
            i++;
        }
        for(;i<chars.length;i++){
            if(chars[i]>='0' && chars[i]<='9'){
                res.append(chars[i]);
            }else {
                break;  // skip degree char
            }
        }
        res.append(".");
        i++;
        for(;i<chars.length;i++){
            if(chars[i]>='0' && chars[i]<='9'){
                res.append(chars[i]);
            }
        }

        return Double.parseDouble(res.toString());
    }

    public static void main(String[] args){
        System.out.println(Double.parseDouble("-79.55"));
    }
}
