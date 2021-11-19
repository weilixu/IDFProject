package main.java.model.idf.html;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.java.report.templateReport.ModelResultsUtility;

public class HTMLProcessor {
    /**
     * Shared code with simulation engine, when updated, please also update the code in simulation engine
     */
    public static void processHTML(Document rawHTMLDoc){
        String reportName = "";
        String reportFor = "";
        String tableName = "";
        
        Element body = rawHTMLDoc.body();        
        Elements all = body.children();
        //System.out.println(all.size());
        Iterator<Element> iter = all.iterator();
        while(iter.hasNext()){
            Element ele = iter.next();
            String nodeName = ele.nodeName();
            
            switch(nodeName){
                case "p":
                    String ownText = ele.ownText().trim();
                    
                    if(ownText.equals("Report:")){
                        reportName = ele.select("b").text().trim().replaceAll("\\W", "");
                    }else if(ownText.equals("For:")){
                        reportFor = ele.select("b").text().trim().replaceAll("\\W", "");
                    }
                    break;
                case "b":
                    tableName = ele.text().trim().replaceAll("\\W", "");
                    break;
                case "table":
                    String tableId = reportName+":"+reportFor+":"+tableName;
                    ele.attr("id", tableId);
                    ele.attr(ModelResultsUtility.TAG, tableId);
                    ele.attr(ModelResultsUtility.TYPE, reportName+":" +reportFor);
                    ele.attr(ModelResultsUtility.NAME, tableName);
                    ele.attr(ModelResultsUtility.REPORT, reportName);
                    ele.attr(ModelResultsUtility.CAT, reportFor);
                    
                    //we assume one table display at a time - utilities will give table a nice export feature
                    ele.attr("class", "table table-striped table-bordered table-hover dataTables-utilities");
                    break;
            }
        }
    }
    
    
    public static void processHTMLTableList(Document rawHTMLDoc, HashMap<String, ArrayList<String>> tableMap){
        String reportOriginName = "";
        String tableOriginName = "";
        String reportOriginFor = "";
        
        Element body = rawHTMLDoc.body();        
        Elements all = body.children();
        //System.out.println(all.size());
        Iterator<Element> iter = all.iterator();
        while(iter.hasNext()){
            Element ele = iter.next();
            String nodeName = ele.nodeName();
            
            switch(nodeName){
                case "p":
                    String ownText = ele.ownText().trim();
                    
                    if(ownText.equals("Report:")){
                    	    reportOriginName = ele.select("b").text().trim();
                    }else if(ownText.equals("For:")) {
                    		reportOriginFor = ele.select("b").text().trim();
                    }
                    break;
                case "b":
                		tableOriginName = ele.text().trim();
                    break;
                case "table":
                		if(!tableMap.containsKey(reportOriginName)) {
                			tableMap.put(reportOriginName, new ArrayList<String>());
                		}
                		
                		String tableName = reportOriginName + " : " +reportOriginFor +" : " + tableOriginName;
            		    tableMap.get(reportOriginName).add(tableName);
                    break;
            }
        }
    }
}
