package main.java.tools.idd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.config.ServerConfig;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.file.xml.XMLUtil;

public class ObjectFields {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectFields.class);
    
    private static Map<String, IDDWrapper> docs = new HashMap<>();
    public static List<String[]> getFieldsNameAndUnit(String version, String objName){
        if(!docs.containsKey(version)){
            synchronized(ObjectFields.class){
                if(!docs.containsKey(version)){
                    IDDWrapper wrapper = new IDDWrapper();
                    wrapper.read(XMLUtil.readXML(ServerConfig.readProperty("ResourcePath")+"idf_idd_v"+version+".xml"), version);
                    docs.put(version, wrapper);
                }
            }
        }
        
        /*XPathFactory xFactory = XPathFactory.instance();
        XPathExpression<Element> expr = xFactory.compile("//Object[Name='"+objName+"']", Filters.element());
        List<Element> eles = expr.evaluate(docs.get(version));
        
        if(eles.size()>1){
            LOG.warn(objName+" in "+version+" has more than one definition in IDD.");
        }
        if(eles.isEmpty()){
            LOG.warn(objName+" in "+version+" has no definition in IDD.");
        }*/
        
        Element objEle = docs.get(version).get(objName);
        if(objEle==null){
            LOG.warn("No object with name "+objName+" in version "+version);
            return null;
        }
        
        List<String[]> res = new ArrayList<>();
        List<Element> fields = objEle.getChildren("Field");
        for(Element field : fields){
            res.add(new String[]{field.getChildText("Name"), field.getChild("Unit")!=null ? field.getChildText("Unit") : null});
        }
        
        return res;
    }
    
    public static void main(String[] args){}
}
