package main.java.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NetworkRequester {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkRequester.class);

    public static String get(String address){
        URL url;
        try {
            url = new URL(address);
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage(), e);
            return "";
        }

        HttpURLConnection httpConn;
        try {
            httpConn = (HttpURLConnection)url.openConnection();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return "";
        }
        httpConn.setUseCaches(false);

        int status;
        try {
            status = httpConn.getResponseCode();
            if(status == HttpURLConnection.HTTP_OK){
                try(BufferedInputStream bis = new BufferedInputStream(httpConn.getInputStream());
                    InputStreamReader isReader = new InputStreamReader(bis);
                    BufferedReader reader = new BufferedReader(isReader)){
                    String line;
                    StringBuilder sb = new StringBuilder();

                    while((line=reader.readLine()) != null){
                        sb.append(line);
                    }

                    httpConn.disconnect();

                    return sb.toString();
                }
            }else {
                LOG.error("HTTP request "+address+", Server returned abnormal status: "+status);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return "";
    }
}
