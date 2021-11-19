package main.java.util;

public class BooleanUtil {
    public static boolean readBooleanString(String str){
        if(str==null){
            return false;
        }

        return "true".equalsIgnoreCase(str.trim());
    }
}
