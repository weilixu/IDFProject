package main.java.model.idf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Level_2")
@XmlType(propOrder = {"label", "level_3s"})
public class Level_2{
    @XmlElement(name = "Label")
    private String label ="";
    @XmlElement(name = "Level_3")
    private List<Level_3> level_3s = new ArrayList<Level_3>();
    
    public String getLabel(){
        return label;
    }
    
    public List<Level_3> getLevel_3NodesList(){
        return level_3s;
    }
    
    
}
