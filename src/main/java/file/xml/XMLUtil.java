package main.java.file.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import main.java.config.ServerConfig;
import org.jdom2.Document;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import main.java.model.vc.BranchType;

public class XMLUtil {
    private static final Logger LOG = LoggerFactory.getLogger(XMLUtil.class);
    
    public static Document readXML(File file){
        Document xmlDoc = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            DOMBuilder domBuilder = new DOMBuilder();
            xmlDoc = domBuilder.build(dBuilder.parse(file));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Reading idf_structure failed, "+e.getMessage(), e);
        }
        
        return xmlDoc;
    }
    
    public static Document readXML(String path){
        File xml = new File(path);
        
        return readXML(xml);
    }
    
    public static Document readTreeStructureXML(BranchType branchType, String version){
        String xmlPath;
        if(branchType == BranchType.osm){
            xmlPath = ServerConfig.readProperty("ResourcePath")+"osm_structure_vlatest.xml";
        }else {
            xmlPath = ServerConfig.readProperty("ResourcePath")+"idf_structure_v"+version+".xml";
        }
        return readXML(xmlPath);
    }
    
    public static void saveXMLToFile(Document doc, String path){
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try(FileOutputStream fos = new FileOutputStream(path)){
            xmlOutputter.output(doc, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
