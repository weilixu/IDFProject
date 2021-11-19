package main.java.model.xml_idf;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "IDFLevel3")
public class IDFLevel3 {
    @XmlElement(name = "Label")
    private String label = "";
    
    public String getLabel(){
        return label;
    }
}
