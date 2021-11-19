package main.java.model.result.compare;

import java.util.ArrayList;

import main.java.model.vc.IDFCompareResult;
import main.java.util.StringUtil;

public class CompareResult {
    private ArrayList<String> content = null;
    
    private ArrayList<Integer> highlights = null;
    private ArrayList<IDFCompareResult> highlightTypes = null;
    private ArrayList<Integer> skips = null;
    
    private int lineCounter = 1;
    
    public CompareResult(){
        content = new ArrayList<>();
        highlights = new ArrayList<>();
        highlightTypes = new ArrayList<>();
        skips = new ArrayList<>();
    }
    
    public void addNormalLine(String line){
        content.add(line);
        
        lineCounter++;
    }
    
    public void addPadLine(){
        skips.add(lineCounter);
        
        addLine("\t", IDFCompareResult.LINE_SKIP);
    }
    
    public void addDeletedLine(String line){
        addLine(line, IDFCompareResult.LINE_DELETED);
    }
    
    public void addAddedLine(String line){
        addLine(line, IDFCompareResult.LINE_ADDED);
    }
    
    public void addDiffLine(String line){
        addLine(line, IDFCompareResult.LINE_DIFF);
    }
    
    private void addLine(String line, IDFCompareResult flag){
        highlights.add(lineCounter);
        highlightTypes.add(flag);
        addNormalLine(line);
    }
    
    public String getContentString(){
        return StringUtil.mergeCollectionToString(content, "\n");
    }
    
    public ArrayList<String> getContentList(){
        return content;
    }
    
    public ArrayList<Integer> getSkips(){
        return skips;
    }
    
    public ArrayList<Integer> getHighLights(){
        return highlights;
    }
    
    public ArrayList<IDFCompareResult> getHighLightTypes(){
        return highlightTypes;
    }
    
    public int getCurLine(){
        return lineCounter;
    }
}
