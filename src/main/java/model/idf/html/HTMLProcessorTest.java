package main.java.model.idf.html;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

public class HTMLProcessorTest {
	
	public static void main(String[] args) {
		File htmlFile = new File("/Users/weilixu/Desktop/data/design.html");
		
		Document doc;
		try {
			doc = Jsoup.parse(htmlFile,null);
			HTMLProcessor.processHTML(doc);
			String tableId = "LEED Summary : Entire Facility : EAP2-3. EnergyType Summary";
			String[] tableIdElements = tableId.split(":");
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<tableIdElements.length; i++) {
				sb.append(tableIdElements[i].trim().replaceAll("\\W", ""));
				if(i<tableIdElements.length-1) {
					sb.append(":");
				}
			}
			
			tableId = sb.toString();
			System.out.println(tableId);
			Elements eles = doc.getElementsByAttributeValue("tableId", tableId);
			
			Element table = eles.get(0);
			table.removeAttr("border");
			table.removeAttr("cellpadding");
			table.removeAttr("cellspacing");
			Elements trs = table.getElementsByTag("tr");
			
			Elements headElements = trs.get(0).children();
			Elements headReform = new Elements();
			
			for(Element headTemp : headElements) {
				Element headNew = new Element(Tag.valueOf("th"),"");
				headNew.text(headTemp.text());
				headReform.add(headNew);
			}
			
			Element head = new Element(Tag.valueOf("thead"), "");
			head.append(headReform.outerHtml());
			Element foot = new Element(Tag.valueOf("tfoot"),"");
			foot.append(headReform.outerHtml());
			
			trs.get(0).remove();//remove the original header
			//insert to the first
			table.children().first().before(head.outerHtml());
			table.children().last().after(foot.outerHtml());
			
			System.out.println(table.outerHtml());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}

}
