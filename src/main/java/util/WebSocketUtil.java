package main.java.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class WebSocketUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketUtil.class);
    
    public static boolean returnJsonResult(Session session, JsonObject jo){
        try {
            Basic basic = session.getBasicRemote();
            basic.sendText(jo.toString());
        } catch (IOException e) {
            LOG.error("Return JsonObject via WebSocket failed", e);
            return false;
        }
        
        return true;
    }
    
    public static Future<?> returnJsonResultAsync(Session session, JsonObject jo){
        Async async = session.getAsyncRemote();
		return async.sendText(jo.toString());
    }
    
    @SuppressWarnings("unchecked")
    public static void addCookie(EndpointConfig config, String key, String value){
        Map<String, Object> props = config.getUserProperties();
        
        List<String> cookies = new ArrayList<>();
        String cookieStr = "";
        Object existCookies = props.get("cookie");
        if(existCookies!=null){
            cookieStr = ((List<String>)existCookies).get(0);
        }
        cookieStr += ";"+key+"=\""+value+"\"";
        cookies.add(cookieStr);
        props.put("cookie", cookies);
    }

    public static String getQueryString(Session session, String key){
        String queryStr = session.getQueryString();
        if(!StringUtil.isNullOrEmpty(queryStr)) {
            Map<String, String> params = StringUtil.processQueryStr(queryStr);
            if (params.containsKey(key)) {
                return params.get(key);
            }
        }

        return null;
    }
    
    public static void main(String[] args){}
}
