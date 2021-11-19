package main.java.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import main.java.config.ServerConfig;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class POSTRequestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(POSTRequestUtil.class);
    
    private HttpsURLConnection httpsConn;
    private HttpURLConnection httpConn;
    private boolean isHTTPs = false;
    
    private StringBuilder params = null;
    
    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }
    
    //ignore certificate validation
    private static SSLSocketFactory sslSocketFactory = null;
    private static final TrustManager[] ALL_TRUSTING_TRUST_MANAGER = new TrustManager[] {
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };
    private static final HostnameVerifier ALL_TRUSTING_HOSTNAME_VERIFIER  = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    
    public POSTRequestUtil(){
        params = new StringBuilder();
    }
    
    public boolean setup(String requestURL){
        URL url = null;
        try {
            url = new URL(requestURL);
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
        
        if(url.getProtocol().equals("https")){
            isHTTPs = true;
        }
        
        URLConnection conn = null;
        if(isHTTPs){
            try {
                httpsConn = (HttpsURLConnection)url.openConnection();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                return false;
            }
            
            String validateSSL = ServerConfig.readProperty("ValidateSSLCertificate");
            if(validateSSL!=null && validateSSL.equals("No")){
                if( null == sslSocketFactory) {
                    SSLContext sc = null;
                    try {
                        sc = SSLContext.getInstance("SSL");
                        sc.init(null, ALL_TRUSTING_TRUST_MANAGER, new java.security.SecureRandom());
                    } catch (NoSuchAlgorithmException | KeyManagementException e) {
                        LOG.error(e.getMessage(), e);
                        return false;
                    }
                    sslSocketFactory = sc.getSocketFactory();
                }

                httpsConn.setSSLSocketFactory(sslSocketFactory);
                
                // Since we may be using a cert with a different name, we need to ignore
                // the hostname as well.
                httpsConn.setHostnameVerifier(ALL_TRUSTING_HOSTNAME_VERIFIER);
            }
            
            try {
                httpsConn.setRequestMethod("POST");
            } catch (ProtocolException e) {
                LOG.error(e.getMessage(), e);
            }
            
            conn = httpsConn;
        }else {
            try {
                httpConn = (HttpURLConnection)url.openConnection();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                return false;
            }
            
            try {
                httpConn.setRequestMethod("POST");
            } catch (ProtocolException e) {
                LOG.error(e.getMessage(), e);
            }
            
            conn = httpConn;
        }
        
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        
        conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
        conn.setRequestProperty( "charset", "utf-8");
        
        return true;
    }
    
    public void addParameter(String name, String value){
        try {
            params.append(URLEncoder.encode(name, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
    }
    
    public String send(){
        String urlParameters = params.toString();
        urlParameters = urlParameters.substring(0, urlParameters.length()-1); //delete tailing &
        
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        
        if(isHTTPs){
            httpsConn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            try(DataOutputStream wr = new DataOutputStream(httpsConn.getOutputStream())) {
                wr.write( postData );
                wr.flush();
                wr.close();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                httpsConn.disconnect();
                return null;
            }
            
            try {
                int status = httpsConn.getResponseCode();
                if(status == HttpURLConnection.HTTP_OK){
                    try(BufferedInputStream bis = new BufferedInputStream(httpsConn.getInputStream());
                            InputStreamReader isReader = new InputStreamReader(bis);
                            BufferedReader reader = new BufferedReader(isReader)){
                        String line = "";
                        StringBuilder sb = new StringBuilder();
                        
                        while((line=reader.readLine()) != null){
                            sb.append(line).append("\n");
                        }
                        
                        httpsConn.disconnect();
                        
                        return sb.toString();
                    }
                }else {
                    LOG.error("HTTPS connection, Server returned abnormal status: "+status);
                    LOG.error(httpConn.getResponseMessage(), new IllegalStateException());
                    httpsConn.disconnect();
                    return null;
                }
            } catch (IOException e) {
                httpsConn.disconnect();
                LOG.error(e.getMessage(), e);
                return null;
            }
        }else {
            httpConn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            try(DataOutputStream wr = new DataOutputStream(httpConn.getOutputStream())) {
                wr.write( postData );
                wr.flush();
                wr.close();
            } catch (IOException e) {
                httpConn.disconnect();
                LOG.error(e.getMessage(), e);
                return null;
            }
            
            try {
                int status = httpConn.getResponseCode();
                if(status == HttpURLConnection.HTTP_OK){
                    try(BufferedInputStream bis = new BufferedInputStream(httpConn.getInputStream());
                            InputStreamReader isReader = new InputStreamReader(bis);
                            BufferedReader reader = new BufferedReader(isReader)){
                        String line = "";
                        StringBuilder sb = new StringBuilder();
                        
                        while((line=reader.readLine()) != null){
                            sb.append(line).append("\n");
                        }
                        
                        httpConn.disconnect();
                        
                        return sb.toString();
                    }
                }else {
                    LOG.error("HTTP connection, Server returned abnormal status: "+status);
                    String errorStream = IOUtils.toString(httpConn.getErrorStream(), "utf-8");

                    LOG.error(errorStream, new IllegalStateException());
                    httpConn.disconnect();
                    return null;
                }
            } catch (IOException e) {
                httpConn.disconnect();
                LOG.error(e.getMessage(), e);
                return null;
            }
        }
    }
}
