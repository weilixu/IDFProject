package main.java.model.xml_idf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IDFLevel1")
public class IDFLevel1 {
    @XmlElement(name = "Label")
    private String label = "";
    @XmlElement(name = "Level_2")
    private List<IDFLevel2> IDFLevel2s = new ArrayList<IDFLevel2>();
    
    public String getLabel(){
        return label;
    }
    
    public List<IDFLevel2> getLevel_2NodesList(){
        return IDFLevel2s;
    }
}
