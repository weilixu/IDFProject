package main.java.model.idf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;
import org.jsoup.parser.Tag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.file.xml.XMLUtil;
import main.java.model.vc.BranchType;

public class ModelTreeStructure {
    private static final String delimiter = "-";
    
    public static Map<String, String> readLabelToIdMap(String version, boolean keepCase, BranchType type){
        Map<String, String> res = new HashMap<>();
        
        Document xmlDoc = XMLUtil.readTreeStructureXML(type, version);
        if(xmlDoc==null){
            return null;
        }
        
        Filter<Element> filter = new ElementFilter("Label");
        Iterator<Element> labels = xmlDoc.getRootElement().getDescendants(filter);
        while(labels.hasNext()){
            Element label = labels.next();
            
            String labelStr = keepCase ? label.getText() : label.getText().toLowerCase();
            res.put(labelStr, label.getAttributeValue("id"));
        }
        
        return res;
    }
    
    public static Map<String, String> readIdToLabelMap(String version, boolean keepCase, BranchType type){
        Map<String, String> res = new HashMap<>();
        
        Document xmlDoc = XMLUtil.readTreeStructureXML(type, version);
        if(xmlDoc==null){
            return null;
        }
        
        Filter<Element> filter = new ElementFilter("Label");
        Iterator<Element> labels = xmlDoc.getRootElement().getDescendants(filter);
        while(labels.hasNext()){
            Element label = labels.next();
            
            String labelStr = keepCase ? label.getText() : label.getText().toLowerCase();
            res.put(label.getAttributeValue("id"), labelStr);
        }
        
        return res;
    }
    
    public static Set<String> readLabelIds(String version, BranchType type){
        Set<String> res = new HashSet<>();
        
        Document xmlDoc = XMLUtil.readTreeStructureXML(type, version);
        if(xmlDoc==null){
            return null;
        }
        
        Filter<Element> filter = new ElementFilter("Label");
        Iterator<Element> labels = xmlDoc.getRootElement().getDescendants(filter);
        while(labels.hasNext()){
            res.add(labels.next().getAttributeValue("id"));
        }
        
        return res;
    }
    
    protected static void addId(List<Element> level, String pre){
        int id=1;
        for(int i=1;i<level.size();i++){
            Element ele = level.get(i);
            
            Element label = ele.getChild("Label");
            
            String idStr = pre+id;
            label.setAttribute("id", idStr);
            
            List<Element> subLevels = ele.getChildren();
            if(subLevels.size()>1){
                addId(subLevels, idStr+delimiter);
            }
            
            id++;
        }
    }
    
    public static Document addId(Document xmlDoc){
        List<Element> level = xmlDoc.getRootElement().getChildren();
        int id = 1;
        for(Element ele : level){
            Element label = ele.getChild("Label");
            
            String idStr = id+"";
            label.setAttribute("id", idStr);
            
            List<Element> subLevels = ele.getChildren();
            if(subLevels.size()>1){
                addId(subLevels, idStr+delimiter);
            }
            
            id++;
        }
        
        return xmlDoc;
    }
    
    public static List<String> splitPath(String id){
        List<String> res = new ArrayList<>();
        
        int idx = id.lastIndexOf(delimiter);
        while(idx>0){
            res.add(id);
            id = id.substring(0, idx);
            idx = id.lastIndexOf(delimiter);
        }
        res.add(id);
        
        return res;
    }
    
    private static JsoupElement assembleHTML(List<Element> children){
        JsoupElement root = new JsoupElement(Tag.valueOf("ul"), "");
        for(int i=1;i<children.size();i++){
            Element ele = children.get(i);
            
            Element label = ele.getChild("Label");
            
            String name = label.getText();
            String id = label.getAttributeValue("id");
            
            JsoupElement li = new JsoupElement(Tag.valueOf("li"), "");
            li.attr("id", id);
            li.text(name);
            
            List<Element> subChildren = ele.getChildren();
            if(subChildren.size()>1){
                li.appendChild(assembleHTML(subChildren));
            }
            
            root.appendChild(li);
        }
        return root;
    }
    public static String assembleHTML(Document treeXML){
        List<Element> level = treeXML.getRootElement().getChildren();
        
        JsoupElement root = new JsoupElement(Tag.valueOf("ul"), "");
        for(Element ele : level){
            Element label = ele.getChild("Label");
            
            String name = label.getText();
            String id = label.getAttributeValue("id");
            
            JsoupElement li = new JsoupElement(Tag.valueOf("li"), "");
            li.attr("id", id);
            li.text(name);
            
            List<Element> children = ele.getChildren();
            if(children.size()>1){
                li.appendChild(assembleHTML(children));
            }
            
            root.appendChild(li);
        }
        
        return root.toString();
    }
    
    private static void assembleTreeStruction(List<Element> list, String parentId, JsonArray ja, Set<String> noContentIds){
        for(int i=1;i<list.size();i++){
            Element ele = list.get(i);
            
            Element label = ele.getChild("Label");            
            String name = label.getText();
            String id = label.getAttributeValue("id");
            
            if(noContentIds.contains(id)){
                continue;
            }
            
            JsonObject levelJo = new JsonObject();
            levelJo.addProperty("id", id);
            levelJo.addProperty("parent", parentId);
            levelJo.addProperty("text", name);
            
            JsonObject state = new JsonObject();
            state.addProperty("selected", true);
            levelJo.add("state", state);
            
            ja.add(levelJo);
            
            List<Element> subChildren = ele.getChildren();
            if(subChildren.size()>1){
                assembleTreeStruction(subChildren, id, ja, noContentIds);
            }
        }
    }
    public static JsonArray assembleTreeStructure(Document treeXML, Set<String> noContentIds){
        JsonArray ja = new JsonArray();
        
        //{"id":""+totalNodeCount+"", "parent":temp, "text":""+zone["zoneName"]+"", "li_attr":{"type":"zone", "zone":""+zone["zoneName"]+""}});
        List<Element> level = treeXML.getRootElement().getChildren();        
        for(Element ele : level){
            Element label = ele.getChild("Label");            
            String name = label.getText();
            String id = label.getAttributeValue("id");
            
            if(noContentIds.contains(id)){
                continue;
            }
            
            JsonObject levelJo = new JsonObject();
            levelJo.addProperty("id", id);
            levelJo.addProperty("parent", "#");
            levelJo.addProperty("text", name);
            
            JsonObject state = new JsonObject();
            state.addProperty("selected", true);
            levelJo.add("state", state);
            
            ja.add(levelJo);
            
            List<Element> children = ele.getChildren();
            if(children.size()>1){
                assembleTreeStruction(children, id, ja, noContentIds);
            }
        }
        
        return ja;
    }
    
    public static List<Integer> splitNode(String treePath){
        List<Integer> res = new ArrayList<>();
        
        String[] nodes = treePath.split(delimiter);
        for(String node : nodes){
            res.add(Integer.valueOf(node));
        }
        
        return res;
    }
    
    public static JsonObject assembleTreeSearch(Document treeXML){
        JsonObject res = new JsonObject();
        
        Filter<Element> filter = new ElementFilter("Label");
        Iterator<Element> labels = treeXML.getRootElement().getDescendants(filter);
        while(labels.hasNext()){
            Element label = labels.next();
            
            res.addProperty(label.getText(), label.getAttributeValue("id"));
        }
        
        return res;
    }
    
    public static void main(String[] args){}
}
