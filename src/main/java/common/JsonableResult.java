package main.java.common;

import java.lang.reflect.Field;

import com.google.gson.JsonObject;

/**
 * Note all the field should be public
 * @author wanghp18
 *
 */
public class JsonableResult {
    public JsonObject makeJsonObject(){
        JsonObject jo = new JsonObject();
        
        Field[] fields = this.getClass().getDeclaredFields();
        try {
            for(Field field : fields){
                String fieldName = field.getName();
                Object fieldValue = field.get(this);
            
                jo.addProperty(fieldName, fieldValue==null ? "N/A" : fieldValue.toString());
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        
        return jo;
    }
}
