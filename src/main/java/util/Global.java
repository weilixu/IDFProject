package main.java.util;

import java.util.regex.Pattern;

public class Global {
    //Univeral global variable
    public static final String UNIVERSAL_LINE_DELIMITER = "\r\n";
    
    //email validator
    public static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    
    //IDF display padding settings
    public static final int IDF_DISPLAY_PADDING_OBJ_NAME = 2;
    public static final int IDF_DISPLAY_PADDING_OBJ_FIELD = 4;
    public static final int IDF_DISPLAY_PADDING_VALUE_COMMENT = 4;
}
