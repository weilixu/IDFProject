package main.java.model.xml_idf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "IDFLevel2")
@XmlType(propOrder = {"label", "IDFLevel3s"})
public class IDFLevel2 {
    @XmlElement(name = "Label")
    private String label ="";
    
    @XmlElement(name = "Level_3")
    private List<IDFLevel3> IDFLevel3s = new ArrayList<IDFLevel3>();
    
    public String getLabel(){
        return label;
    }
    
    public List<IDFLevel3> getLevel_3NodesList(){
        return IDFLevel3s;
    }
}
