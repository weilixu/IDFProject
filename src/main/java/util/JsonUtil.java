package main.java.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonUtil {
    private static final Gson gson = new Gson();

    public static <T> T clone(T jo, Class<T> cls){
        return gson.fromJson(gson.toJson(jo, cls), cls);
    }

    public static String readValue(JsonElement je, String key, String defaultVal){
        if(je==null || je.isJsonNull()){
            return defaultVal;
        }

        try{
            JsonObject jo = je.getAsJsonObject();
            if(jo.has(key) && !jo.get(key).isJsonNull()){
                return jo.get(key).getAsString();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return defaultVal;
    }

    public static JsonObject mapToJson(Map<String, JsonObject> map){
        JsonObject jo = new JsonObject();
        map.forEach(jo::add);
        return jo;
    }

    public static Map<String, JsonObject> jsonToMap(JsonObject jo){
        Map<String, JsonObject> map = new HashMap<>();

        Set<Map.Entry<String, JsonElement>> data = jo.entrySet();
        for(Map.Entry<String, JsonElement> ele : data){
            map.put(ele.getKey(), ele.getValue().getAsJsonObject());
        }

        return map;
    }

    public static JsonObject readJsonArray(JsonArray ja, int idx, boolean isDefautNull){
        if(ja!=null && ja.size()>idx){
            JsonElement je = ja.get(idx);
            if(je!=null && !je.isJsonNull() && je.isJsonObject()){
                return je.getAsJsonObject();
            }
        }

        if(isDefautNull){
            return null;
        }
        return new JsonObject();
    }
}
