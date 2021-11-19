package main.java.util;

import java.nio.CharBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StringUtil {
    private static Map<Integer, String> spaces = new HashMap<>();
    private static Map<Integer, String> spacesHTML = new HashMap<>();

    public static String spaces(int n){
        if(n<=0){
            n = 1;
        }
        if(spaces.containsKey(n)){
            return spaces.get(n);
        }
        String res = CharBuffer.allocate(n).toString().replace('\0', ' ');
        if(n<100) {
            spaces.put(n, res);
        }
        return res;
    }
    
    public static String spacesHTML(int n){
        if(n<=0){
            n = 1;
        }
        if(spacesHTML.containsKey(n)){
            return spacesHTML.get(n);
        }
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<n;i++){
            sb.append("&nbsp;");
        }
        if(n<100) {
            spacesHTML.put(n, sb.toString());
        }
        return sb.toString();
    }
    
    public static String mergeListToJsonList(List<?> list){
        if(list.isEmpty()){
            return null;
        }
        
        StringBuilder sb = new StringBuilder("['");
        sb.append(mergeCollectionToString(list, "','"));
        sb.append("']");
        
        return sb.toString();
    }
    
    public static String mergeCollectionToString(Collection<?> list, String delimiter){
        return list.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }
    
    public static String combineListsToJsonDictionary(List<?> keys, List<?> values){
        int keyLen = keys.size();
        int valLen = values.size();
        
        StringBuilder sb = new StringBuilder("{");
        for(int i=0;i<keyLen&&i<valLen;i++){
            sb.append("\"").append(keys.get(i)).append("\":\"").append(values.get(i)).append("\",");
        }
        
        if(keyLen>0 && valLen>0){
            sb.deleteCharAt(sb.length()-1);
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    public static boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
        }

        return false;
    }

    public static String replaceAllRaw(String originalString, String partToMatch, String replacement){
        StringBuilder buffer = new StringBuilder( originalString.length() );
        buffer.append( originalString );

        int indexOf = buffer.indexOf(partToMatch);
        while (indexOf != -1) {
            buffer = buffer.replace(indexOf, indexOf + partToMatch.length(), replacement);
            indexOf = buffer.indexOf(partToMatch, indexOf + replacement.length());
        }

        return buffer.toString();
    }

    public static String escapeBracket(String str){
        StringBuilder buffer = new StringBuilder( str.length() );
        buffer.append( str );

        int lIdx = buffer.indexOf("<");
        int rIdx = buffer.indexOf(">");

        while(lIdx!=-1 && rIdx!=-1){
            if(lIdx<rIdx){
                buffer = buffer.replace(lIdx, lIdx + 1, "&lt;");
                lIdx = buffer.indexOf("<", lIdx + 4);
                rIdx += 3;
            }else {
                buffer = buffer.replace(rIdx, rIdx + 1, "&gt;");
                rIdx = buffer.indexOf(">", rIdx + 4);
                lIdx += 3;
            }
        }

        while (lIdx != -1) {
            buffer = buffer.replace(lIdx, lIdx + 1, "&lt;");
            lIdx = buffer.indexOf("<", lIdx + 4);
        }

        while(rIdx != -1){
            buffer = buffer.replace(rIdx, rIdx + 1, "&gt;");
            rIdx = buffer.indexOf(">", rIdx + 4);
        }

        return buffer.toString();
    }

    public static boolean isNullOrEmpty(String str){
        return str==null || str.isEmpty();
    }

    public static String checkNullAndEmpty(String str, String defVal){
        if(isNullOrEmpty(str)){
            return defVal;
        }
        return str;
    }

    public static String cleanNonWord(String str, String replacement){
        return str.replaceAll("\\W", replacement);
    }

    public static Map<String, String> processQueryStr(String queryStr){
        Map<String, String> res = new HashMap<>();
        if(queryStr.startsWith("?")){
            queryStr = queryStr.substring(1);
        }

        String[] split = queryStr.split("&");
        for(String pair : split){
            if(!isNullOrEmpty(pair)){
                String[] process = pair.split("=");
                if(process.length>1) {
                    res.put(process[0], process[1]);
                }
            }
        }

        return res;
    }

    public static String convertToCamel(String name){
        return name.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    public static void main(String[] args){}
}
