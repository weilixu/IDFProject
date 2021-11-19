package main.java.model.vc;

public class Branch {
    //private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private String projectID = null;
    private String branchID = null;
    
    /*
     * Master branch's parent branch is null
     */
    private String parentBranchID = null;
    
    private int baseCommitId = -1;
    
    private String branchName = null;
    private String branchDes = null;
    
    private BranchType branchType = null;

    private String branchKey = null;

    public Branch(){}
    
    public Branch(String projectID, 
                  String branchID,
                  String branchName,
                  String branchDes,
                  String parentBranchID, 
                  int baseCommitId,
                  BranchType branchType){
        this.projectID = projectID;
        this.branchID = branchID;
        this.branchName = branchName;
        this.branchDes = branchDes;
        this.parentBranchID = parentBranchID;
        this.branchType = branchType;
        
        this.baseCommitId = baseCommitId;
    }
    
    public String getProjectID(){
        return projectID;
    }
    public void setProjectID(String projectID){ this.projectID = projectID; }
    
    public String getBranchID() {
        return branchID;
    }
    public void setBranchID(String branchID){ this.branchID = branchID; }

    public String getParentBranchID() {
        return parentBranchID;
    }

    public int getBaseCommitId() {
        return baseCommitId;
    }

    public String getBranchName() {
        return branchName;
    }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getBranchDes(){
        return branchDes;
    }

    public BranchType getBranchType(){
        return this.branchType;
    }
    public void setBranchType(BranchType branchType){ this.branchType=branchType; }

    public String getBranchKey() { return this.branchKey; }
    public void setBranchKey(String branchKey) { this.branchKey = branchKey; }
}
