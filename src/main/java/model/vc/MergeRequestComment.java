package main.java.model.vc;

import main.java.common.JsonableResult;

public class MergeRequestComment extends JsonableResult{
    public String mergeRequestId;
    public String commentUserId;
    public String commentUserName;
    public String comment;
    public String date;
    
    public MergeRequestComment(){}
}
