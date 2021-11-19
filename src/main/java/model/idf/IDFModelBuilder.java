package main.java.model.idf;

import java.io.File;

import main.java.util.StringUtil;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import main.java.util.FileUtil;
import main.java.file.xml.XMLUtil;
import main.java.model.gbXML.ReverseTranslator;

public class IDFModelBuilder {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
            
    public IDFModelBuilder(){}
    
    public JsonObject buildModel(File file, IDFFileObject idfFileObj){
        String suffix = FileUtil.getSuffix(file);
        JsonObject res = new JsonObject();

        switch(suffix){
            case "idf":    //idf file
                IDFParser parser = new IDFParser();
                
                res = parser.parseIDFFromIDFFile(file, idfFileObj);
                break;
            case "xml":    //gbXML file
                Document doc = XMLUtil.readXML(file);
                if(doc!=null){
                    try {
                        ReverseTranslator translator = new ReverseTranslator(doc, idfFileObj);
                        translator.convert();
                        translator.getIDFFileObject();

                        res.addProperty("status", "success");
                    }catch (Exception e){
                        LOG.error(e.getMessage(), e);
                        res.addProperty("status", "error");
                        res.addProperty("error_msg", "Translating gbXML encounters error, please contact us.");
                        res.addProperty("no_ide", true);
                    }
                }else {
                    res.addProperty("status", "error");
                    res.addProperty("error_msg", "Cannot read gbXML file.");
                }
               
                break;
            default:
                LOG.error("Unrecognized model file suffix: "+suffix);
                res.addProperty("status", "error");
                res.addProperty("error_msg", "Unrecognized model file type, "+suffix);
        }

        res.addProperty("file_type", suffix);
        return res;
    }
}
