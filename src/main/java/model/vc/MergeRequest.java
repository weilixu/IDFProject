package main.java.model.vc;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import main.java.common.JsonableResult;

public class MergeRequest extends JsonableResult{
    public String mergeRequestId;
    public String projectId;
    public String targetBranchId;
    public String targetBranchName;
    public String requestBranchId;
    public String requestBranchName;
    public String requestCommitId;
    public String requestCommitName;
    public String message;
    public MergeRequestStatus status;
    public String requestDateTime;
    
    /*
     * 0: full merge, 1: partial merge
     */
    public int isPartial;
    public String partialSelectJson;
    
    public MergeRequest(){}
    
    public void setDateTime(Date date){
        //TODO GMT
        DateFormat df = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        requestDateTime = df.format(date);
    }
}
