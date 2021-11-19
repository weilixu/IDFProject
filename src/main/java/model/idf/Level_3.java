package main.java.model.idf;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "Level_3")
@XmlType(propOrder = {"label"})
public class Level_3{
    @XmlElement(name = "Label")
    private String label ="";
    
    public String getLabel(){
        return label;
    }
}
