package main.java.tools.idd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IDDWrapper {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private Map<String, Element> eleMap;
    
    public IDDWrapper(){
        this.eleMap = new HashMap<>();
    }
    
    public void read(Document iddXML, String version){
        List<Element> eles = iddXML.getRootElement().getChildren();
        for(Element ele : eles){
            if(eleMap.put(ele.getChildText("Name"), ele) != null){
                LOG.warn("Duplicate object name "+ele.getChildText("Name")+" in version "+version);
            }
        }
    }
    
    public Element get(String eleName){
        return eleMap.get(eleName);
    }
}
