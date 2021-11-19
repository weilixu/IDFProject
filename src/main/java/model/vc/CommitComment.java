package main.java.model.vc;

import java.util.ArrayList;
import java.util.List;

public class CommitComment {
    private int id = -1;
    private int parentId = -1;

    private List<CommitComment> replies = null; 
    private String displayContent = null;
    
    public CommitComment(int id, String displayContent){
        this.id = id;
        replies = new ArrayList<CommitComment>();
        this.displayContent = displayContent;
    }
    
    public int getId(){
        return id;
    }
    
    public int getParentId(){
        return parentId;
    }
    
    public void setParentId(int id){
        parentId = id;
    }
    
    public void addReply(CommitComment reply){
        replies.add(reply);
    }

    public List<CommitComment> getReplies() {
        return replies;
    }

    public String getDisplayContent() {
        return displayContent;
    }
}
