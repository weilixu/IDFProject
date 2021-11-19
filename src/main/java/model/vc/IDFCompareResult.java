package main.java.model.vc;

public enum IDFCompareResult {
    OBJECT_SKIP,
    OBJECT_SAME,
    OBJECT_DIFF,
    OBJECT_BASE_DEL,
    OBJECT_BASE_NEW,
    OBJECT_CMP_DEL,
    OBJECT_CMP_NEW,
    OBJECT_VALUE_SAME,
    OBJECT_VALUE_DIFF,
    OBJECT_ERROR_DIFF_LABEL,
    OBJECT_ERROR_DIFF_NAME,
    
    LINE_DELETED,
    LINE_ADDED,
    LINE_DIFF,
    LINE_SKIP;
    
    @Override
    public String toString(){
        return this.name();
    }
}
