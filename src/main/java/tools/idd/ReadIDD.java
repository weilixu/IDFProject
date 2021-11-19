package main.java.tools.idd;

import main.java.config.ServerConfig;
import main.java.file.xml.XMLUtil;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ReadIDD {
    private final Logger LOG = LoggerFactory.getLogger(ReadIDD.class);

    public static void main(String[] args) {
        ReadIDD ri = new ReadIDD();

        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");
        ri.readIDDGenXML("/Users/weilixu/Documents/GitHub/BuildSimHub/resource/idd_v9.3", "v9.3");
    }

    public void readIDDGenXML(String iddPath, String version) {
        Element root = new Element("IDF_IDD");

        String line;
        boolean isInObj = false;
        Element obj = null, field = null, fieldType = null, fieldRangeMin = null, fieldRangeMax = null;
        try (FileInputStream fis = new FileInputStream(new File(iddPath));
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    if (isInObj) {
                        root.addContent(obj);
                        field = null;
                        isInObj = false;
                    }
                    continue;
                }

                if (isInObj) {
                    if (line.startsWith("\\")) {
                        String command = line.substring(1);
                        String commandContent = null;

                        int space = command.indexOf(" ");
                        if (space > -1) {
                            commandContent = command.substring(space + 1).trim();
                            command = command.substring(0, space);
                        }

                        if (field == null) {
                            //for Object
                            switch (command) {
                                case "unique-object":
                                    obj.setAttribute("isUnique", "true");
                                    break;
                                case "required-object":
                                    obj.setAttribute("isRequired", "true");
                                    break;
                            }
                        } else {
                            //for field
                            switch (command) {
                                case "default":
                                    Element d = new Element("DefaultValue");
                                    d.setText(commandContent);
                                    field.addContent(d);
                                    break;
                                case "type":
                                    fieldType.setText(commandContent);
                                    break;
                                case "key":
                                    Element o = new Element("Option");
                                    o.setText(commandContent);
                                    field.addContent(o);
                                    break;
                                case "units":
                                    Element uSI = new Element("Unit");
                                    uSI.setAttribute("type", "SI");
                                    uSI.setText(commandContent);
                                    field.addContent(uSI);
                                    break;
                                case "minimum":
                                    fieldRangeMin.setAttribute("inclusive", "true");
                                    fieldRangeMin.setText(commandContent);
                                    break;
                                case "minimum>":
                                    fieldRangeMin.setAttribute("inclusive", "false");
                                    fieldRangeMin.setText(commandContent);
                                    break;
                                case "maximum":
                                    fieldRangeMax.setAttribute("inclusive", "true");
                                    fieldRangeMax.setText(commandContent);
                                    break;
                                case "maximum<":
                                    fieldRangeMax.setAttribute("inclusive", "false");
                                    fieldRangeMax.setText(commandContent);
                                    break;
                                case "reference":
                                    Element rN = new Element("ReferenceName");
                                    rN.setText(commandContent);
                                    field.addContent(rN);
                                    break;
                                case "autocalculatable":
                                    field.setAttribute("isAutoCalculateable", "true");
                                    break;
                                case "autosizable":
                                    field.setAttribute("isAutosizable", "true");
                                    break;
                                case "object-list":
                                    Element rT = new Element("ReferenceTo");
                                    rT.setText(commandContent);
                                    field.addContent(rT);
                                    break;
                                case "external-list":
                                    Element e = new Element("ExternalList");
                                    e.setText(commandContent);
                                    field.addContent(e);
                                    break;
                                case "ip-units":
                                    Element uIP = new Element("Unit");
                                    uIP.setAttribute("type", "IP");
                                    uIP.setText(commandContent);
                                    field.addContent(uIP);
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else {
                        //new field
                        String fieldName = line.replaceFirst(".*?field", "").trim();
                        field = new Element("Field");
                        Element fName = new Element("Name");
                        fName.setText(fieldName);
                        field.addContent(fName);
                        obj.addContent(field);

                        fieldType = new Element("Type");
                        if (line.startsWith("A")) {
                            fieldType.setText("string");
                        } else if (line.startsWith("N")) {
                            //numerical value, may have specified range
                            fieldType.setText("number");

                            Element fieldRange = new Element("Range");
                            fieldRangeMin = new Element("Min");
                            fieldRangeMax = new Element("Max");
                            fieldRange.addContent(fieldRangeMin);
                            fieldRange.addContent(fieldRangeMax);
                            field.addContent(fieldRange);
                        }
                        field.addContent(fieldType);
                    }
                } else if (line.endsWith(",") && !line.startsWith("!") && !line.startsWith("\\")) {
                    //IDF object label

                    String label = line.substring(0, line.length() - 1);
                    if (label.contains(",")) {
                        continue;
                    }

                    isInObj = true;
                    obj = new Element("Object");
                    Element objName = new Element("Name");
                    objName.setText(label);
                    obj.addContent(objName);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        Document doc = new Document(root, new DocType("project"));
        XMLUtil.saveXMLToFile(doc, ServerConfig.readProperty("ResourcePath") + "idf_idd_" + version + ".xml");
    }
}
