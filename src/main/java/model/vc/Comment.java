package main.java.model.vc;

import java.util.Date;
import java.util.List;


public class Comment {
  public String id;
  public String userId;
  public String userName;
  public Date date;
  public String profileUrl;
  public String text;
  public String replyId;
  public List<Comment> children;
  public String replyName;
  public String replyUrl;
  public String picData;
  public int level = 0;

  public Comment(String id, String userId, String userName, Date date, String profileUrl, String text, String replyId, String picData) {
    this.id = id;
    this.userId = userId;
    this.userName = userName;
    this.date = date;
    this.profileUrl = profileUrl;
    this.text = text;
    this.replyId = replyId;
    this.children = null;
    this.replyUrl=null;
    this.picData = picData;
  }

  public String GetId(){
      return id;
  }
  
  public String GetReplyId(){
      return replyId;
  }
  
  public String GetUserId(){
      return userId;
  }
  
  public String GetUserName(){
      return userName;
  }
  
  public Date GetDate(){
      return date;
  }
  
  public String GetProfileUrl(){
      return profileUrl;
  }
  
  public String GetText(){
      return text;
  }
  
  
  public String GetPicData() {
    return picData;
  }

  @Override 
  public String toString() {
    StringBuilder comment = new StringBuilder();
    comment.append("ID: ").append(id).append("; ")
        .append("Name: ").append(userName).append("; ")
        .append("date: ").append(date).append("; ")
        .append("profileUrl: ").append(profileUrl).append("; ")
        .append("text: ").append(text).append("; ")
        .append("replyId: ").append(replyId).append("; ");
    return comment.toString();

  }
  
  public void htmlFormat(){
      if(text!=null && !text.isEmpty()){
          text = text.replaceAll("\\\\n", "\r\n");
      }
      
      if(children!=null){
          for(Comment c : children){
              c.htmlFormat();
          }
      }
  }
  
  public static void main(String[] args){}
}
