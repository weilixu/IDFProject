package main.java.model.vc;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Tony on 16/8/6.
 */


public class InLineNote {
  private UUID id;
  private String name;
  private Date date;
  private String profileUrl;
  private String text;
  private int rowNumber;
  private String noteType;

  public InLineNote(UUID id, String name, Date date, String profileUrl, String text,
      int rowNumber, String noteType) {
    this.id = id;
    this.name = name;
    this.date = date;
    this.profileUrl = profileUrl;
    this.text = text;
    this.rowNumber = rowNumber;
    this.noteType =noteType;
  }

  @Override public String toString() {
    StringBuilder comment = new StringBuilder();
    comment.append("ID: ").append(id).append("; ")
        .append("Name: ").append(name).append("; ")
        .append("date: ").append(date).append("; ")
        .append("profileUrl: ").append(profileUrl).append("; ")
        .append("text: ").append(text).append("; ")
        .append("rowNumber: ").append(rowNumber).append("; ")
        .append("noteType: ").append(noteType).append("; ");
    return comment.toString();

  }

public UUID getId() {
    return id;
}

public void setId(UUID id) {
    this.id = id;
}

public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}

public Date getDate() {
    return date;
}

public void setDate(Date date) {
    this.date = date;
}

public String getProfileUrl() {
    return profileUrl;
}

public void setProfileUrl(String profileUrl) {
    this.profileUrl = profileUrl;
}

public String getText() {
    return text;
}

public void setText(String text) {
    this.text = text;
}

public int getRowNumber() {
    return rowNumber;
}

public void setRowNumber(int rowNumber) {
    this.rowNumber = rowNumber;
}

public String getNoteType() {
    return noteType;
}

public void setNoteType(String noteType) {
    this.noteType = noteType;
}
  
}
