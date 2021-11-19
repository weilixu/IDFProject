package main.java.model.xml_idf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IDFStructure")
public class IDFStructure {
    @XmlElement(name = "Level_1")
    private List<IDFLevel1> IDFLevel1s = new ArrayList<IDFLevel1>();
    
    public List<IDFLevel1> getLevel_1Nodes(){
        return IDFLevel1s;
    }
}
