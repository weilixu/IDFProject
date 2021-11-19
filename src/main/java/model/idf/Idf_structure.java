package main.java.model.idf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Idf_structure")
public class Idf_structure {
    @XmlElement(name = "Level_1")
    private List<Level_1> level_1s = new ArrayList<Level_1>();
    
    public List<Level_1> getLevel_1Nodes(){
        return level_1s;
    }
}











