package main.java.model.result;

import com.google.gson.JsonObject;

import main.java.model.idf.IDFObject;
import main.java.model.result.compare.CompareResult;
import main.java.model.result.view.ModelViewResult;
import main.java.util.StringUtil;


public class IDFViewerResult {
    private CompareResult res = null;
    private ModelViewResult view = null;
    
    public IDFViewerResult(){
        res = new CompareResult();
        view = new ModelViewResult();
    }
    
    public void addLine(String line){
        res.addNormalLine(line);
    }
    
    public void addPadLine(){
        res.addPadLine();
    }
    
    public void addIDFObj(IDFObject obj, int pad){
        int size = obj.getObjLen();
        
        view.addObjFold(res.getCurLine(), obj.getObjLen()-1);
        
        for(int i=0;i<size;i++){
            res.addNormalLine(obj.getLine(i, pad, false));
        }
        res.addPadLine();
    }
    
    public void assembleJsonObject(JsonObject jo){
        jo.addProperty("content", res.getContentString());
        jo.addProperty("objTypes", StringUtil.combineListsToJsonDictionary(view.getObjTypeLines(), view.getObjContentLens()));
    }
    
    public String getContent(){
        return res.getContentString();
    }
}
