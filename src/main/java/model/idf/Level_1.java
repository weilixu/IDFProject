package main.java.model.idf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Level_1")
@XmlType(propOrder = {"label", "level_2s"})
public class Level_1{
    @XmlElement(name = "Label")
    private String label = "";
    @XmlElement(name = "Level_2")
    private List<Level_2> level_2s = new ArrayList<Level_2>();
    
    public String getLabel(){
        return label;
    }
    
    public List<Level_2> getLevel_2NodesList(){
        return level_2s;
    }

}