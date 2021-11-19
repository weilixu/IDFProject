package main.java.model.vc;

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * IDF file object data is NOT stored in commit instance, the data is stored in cache service.
 * @author wanghp18
 *
 */
public class Commit implements Serializable {
    private static final long serialVersionUID = 2771009918324209327L;

    private String commitId = null;
    private CommitType commitType = CommitType.UNKNOWN;
    
    private String branchId = null;
    private Date commitDate = null;
    
    private String fileHash = null;
    
    private String commitComment = null;
    private ArrayList<CommitComment> comments = null;
    
    private String commitUserId = null;
    private String commitUserName = null;
    
    private String otherBranchId = null;
    private String otherBranchName = null;
    
    private String otherCommitId = null;
    private String otherCommitComment = null;
    
    private String simErrorLen = null;
    private String simHTMLLen = null;
    private String simWeather = null;
    private String simCSVLen = null;
    private String simESOLen = null;

    public Commit(){}
    
    public Commit(String commitUserName,
                String commitId,
                String branchId,
                Date commitDate){
        this.commitUserName = commitUserName;
        this.commitId = commitId;
        this.branchId = branchId;
        this.commitDate = commitDate;
    }
    
    /**
     * If type is MERGE_SRC, otherBranch is base branch, own branch is compare branch<br/>
     * If type is MERGE_DEST, otherBranch is compare branch, own branch is base branch<br/>
     * If type is COMMIT, otherBranch is own branch<br/>
     * If type is CLONE, otherBranch is copied branch<br/>
     * If type is PULL, otherBranch is pulled branch<br/>
     * If type is INIT, otherBranch is null<br/>
     */
    public Commit(String commitId,
                  CommitType commitType, 
                  String branchId, 
                  Date commitDate, 
                  String fileHash,
                  String commitComment,
                  String commitUserId,
                  String commitUserName,
                  String otherBranchId,
                  String otherBranchName,
                  String otherCommitId,
                  String otherCommitComment){
        this.commitId = commitId;
        this.commitType = commitType;
        
        this.branchId = branchId;
        this.commitDate = commitDate;
        
        this.fileHash = fileHash;
        
        this.commitComment = commitComment;
        this.comments = new ArrayList<>();
        this.commitUserId = commitUserId;
        this.commitUserName = commitUserName;
        
        this.otherBranchId = otherBranchId;
        this.otherBranchName = otherBranchName;
        this.otherCommitId = otherCommitId;
        this.otherCommitComment = otherCommitComment;
    }
    
    public void addComment(CommitComment comment){
        comments.add(comment);
    }
    
    public void addComments(List<CommitComment> comments){
        this.comments.addAll(comments);
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setComments(ArrayList<CommitComment> comments) {
        this.comments = comments;
    }
    
    public String getCommitComment(){
        return this.commitComment;
    }

    public ArrayList<CommitComment> getComments() {
        return comments;
    }

    public CommitType getCommitType() {
        return commitType;
    }
    
    public Date getCommitDate(){
        return commitDate;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getCommitUserId() {
        return commitUserId;
    }

    public String getOtherBranchId() {
        return otherBranchId;
    }

    public String getOtherBranchName() {
        return otherBranchName;
    }

    public String getOtherCommitId() {
        return otherCommitId;
    }

    public String getOtherCommitComment() {
        return otherCommitComment;
    }

    public String getCommitUserName() {
        return commitUserName;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getSimErrorLen() {
        return simErrorLen;
    }

    public void setSimErrorLen(String simErrorLen) {
        this.simErrorLen = simErrorLen;
    }

    public String getSimHTMLLen() {
        return simHTMLLen;
    }

    public void setSimHTMLLen(String simHTMLLen) {
        this.simHTMLLen = simHTMLLen;
    }

    public String getSimWeather() {
        return simWeather;
    }

    public void setSimWeather(String simWeather) {
        this.simWeather = simWeather;
    }
    
    public String getSimCSVLen() {
        return simCSVLen;
    }

    public void setSimCSVLen(String simCSVLen) {
        this.simCSVLen = simCSVLen;
    }

    public String getSimESOLen() {
        return simESOLen;
    }

    public void setSimESOLen(String simESOLen) {
        this.simESOLen = simESOLen;
    }
}
