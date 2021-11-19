package main.java.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.ServerConfig;

public class MultipartUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MultipartUtil.class);
    
    //private DataOutputStream dos;
    
    private HttpsURLConnection httpsConn;
    private HttpURLConnection httpConn;
    private boolean isHTTPs = false;
    
    private StringBuilder postData = new StringBuilder();
    
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
            httpsConn = null;
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
        //conn.setDoInput(true);
        
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
        conn.setRequestProperty("charset", "utf-8");
        
        /*try {
            dos = new DataOutputStream(conn.getOutputStream());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }*/
        
        return true;
    }
    
    public boolean addFormField(String name, String value){
        /*try {
            dos.writeUTF("--*****\r\n");
            dos.writeUTF("Content-Disposition: form-data; name=\""+name+"\"\r\n");
            dos.writeUTF("Content-Type: text/plain; charset=UTF-8\r\n\r\n"+value+"\r\n");

            dos.flush();
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }*/
        
        postData.append("--*****\r\n");
        postData.append("Content-Disposition: form-data; name=\""+name+"\"\r\n");
        postData.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n"+value+"\r\n");
        
        return true;
    }
    
    public boolean addFilePart(String name, String fileContent){
        //String fileName = file.getName();
        
        /*try {
            dos.writeUTF("--*****\r\n");
            dos.writeUTF("Content-Disposition: form-data; name=\""+name+"\";filename=\""+fileName+"\"\r\n\r\n");
            dos.write(Files.readAllBytes(file.toPath()));
            
            dos.flush();
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }*/
        
        postData.append("--*****\r\n");
        postData.append("Content-Disposition: form-data; name=\""+name+"\";filename=\"uploaded\"\r\n\r\n");
        /*try {
            postData.append(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }*/
        postData.append(fileContent);
        
        return true;
    }
    
    public String send(){
        /*try {
            dos.writeUTF("\r\n--*****--\r\n");
            dos.flush();
            dos.close();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }*/
        
        postData.append("\r\n--*****--\r\n");

        if(isHTTPs){
            try {
                try( DataOutputStream wr = new DataOutputStream( httpsConn.getOutputStream())) {
                    wr.write( postData.toString().getBytes(StandardCharsets.UTF_8) );
                }
                
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
                    LOG.error("Server returned abnormal status: "+status);
                    return null;
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }else {
            try {
                try( DataOutputStream wr = new DataOutputStream( httpConn.getOutputStream())) {
                    wr.write( postData.toString().getBytes(StandardCharsets.UTF_8) );
                }
                
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
                    LOG.error("Server returned abnormal status: "+status+", URL: "+httpConn.getURL().toString());
                    return null;
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }
    }
}
