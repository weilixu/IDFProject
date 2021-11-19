package main.java.tools.idfTreeStructure;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;

import main.java.config.ServerConfig;
import main.java.file.xml.XMLUtil;
import main.java.model.idf.ModelTreeStructure;
import main.java.model.vc.BranchType;

public class ReadEnergyPlusTreeStructureExcel {
    /**
     * Suppose the content is in the first sheet
     * @param excelPath
     */
    public void convertToXML(String excelPath, String version, int maxLevel, BranchType type){
        Element[] levels = new Element[maxLevel+1];
        
        Element root = new Element("Idf_structure");
        levels[0] = root;
        Document doc = new Document(root, new DocType("project"));
        
        try(InputStream is = new FileInputStream(excelPath)){
            try(XSSFWorkbook workbook = new XSSFWorkbook(is)){
                
                XSSFSheet sheet = workbook.getSheetAt(0);
                int rowStart = sheet.getFirstRowNum();
                int rowEnd = sheet.getLastRowNum();
                
                for(int i=rowStart;i<rowEnd;i++){
                    XSSFRow row = sheet.getRow(i);
                    int cellStart = row.getFirstCellNum();
                    
                    for(int j=cellStart;j<maxLevel;j++){
                        String labelText = row.getCell(j).getStringCellValue();
                        if(labelText.isEmpty()){
                            continue;
                        }
                        
                        Element newEle = new Element("Level_"+(j+1));
                        Element label = new Element("Label");
                        label.setText(labelText);
                        newEle.addContent(label);
                        
                        levels[j+1] = newEle;
                        levels[j].addContent(newEle);
                    }
                }
                
                //add id
                ModelTreeStructure.addId(doc);

                XMLUtil.saveXMLToFile(doc, ServerConfig.readProperty("ResourcePath")+type.toString()+"_structure_"+version+".xml");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args){
        ReadEnergyPlusTreeStructureExcel test = new ReadEnergyPlusTreeStructureExcel();
        
        ServerConfig.setConfigPath("/Users/weilixu/Documents/GitHub/BuildSimHub/WebContent/WEB-INF/server.config");
        test.convertToXML("/Users/weilixu/Documents/BuildSimHub/TreeStructure/energyplus tree - v9.4.xlsx", "v9.3", 3, BranchType.idf);
    }
}
