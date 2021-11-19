package main.java.model.result;

public class HTMLResultUtil {
    private static String[] unitMarker    = {"[","(","{"};
    private static String[] unitMarkerEnd = {"]",")","}"};
    public static String extractUnit(String title, String text){
        if (text.equalsIgnoreCase(title)) {
            return "";
        }

        for(int i=0;i<unitMarker.length;i++){
            if (text.startsWith(title + " " + unitMarker[i])) {
                return text.substring(text.indexOf(unitMarker[i]) + 1, text.indexOf(unitMarkerEnd[i]));
            }
        }

        return null;
    }

    public static String extractUnit(String text){
        for(int i=0;i<unitMarker.length;i++){
            if (text.indexOf(unitMarker[i])>-1 && text.indexOf(unitMarkerEnd[i])>0) {
                return text.substring(text.indexOf(unitMarker[i]) + 1, text.indexOf(unitMarkerEnd[i]));
            }
        }

        return null;
    }

    public static String processTableId(String tableId){
        String[] tableIdElements = tableId.split(":");
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<tableIdElements.length; i++) {
            sb.append(tableIdElements[i].trim().replaceAll("\\W", ""));
            if(i<tableIdElements.length-1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }
}
