package main.java.model.result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import main.java.model.result.compare.CompareResult;
import main.java.model.result.view.ModelViewResult;
import main.java.model.vc.IDFCompareResult;
import main.java.util.Global;
import main.java.util.StringUtil;

public class SideBySideResult {
    private CompareResult baseRes = null;
    private CompareResult cmpRes = null;
    
    private ModelViewResult baseView = null;
    private ModelViewResult cmpView = null;
    
    private Set<String> diffObjs = null;
    
    public SideBySideResult(){
        baseRes = new CompareResult();
        cmpRes = new CompareResult();
        
        baseView = new ModelViewResult();
        cmpView = new ModelViewResult();
        
        diffObjs = new HashSet<>();
    }
    
    /**
     * null value means pad
     */
    public void addLines(String baseLine, String cmpLine, IDFCompareResult cmpFlag){
        switch(cmpFlag){
            case OBJECT_BASE_DEL:
                baseRes.addDeletedLine(baseLine);
                cmpRes.addPadLine();
                break;
            case OBJECT_BASE_NEW:
                baseRes.addAddedLine(baseLine);
                cmpRes.addPadLine();
                break;
            case OBJECT_CMP_DEL:
                baseRes.addPadLine();
                cmpRes.addDeletedLine(cmpLine);
                break;
            case OBJECT_CMP_NEW:
                baseRes.addPadLine();
                cmpRes.addAddedLine(cmpLine);
                break;
            case OBJECT_DIFF:
            case OBJECT_VALUE_DIFF:
                if(baseLine != null){
                    baseRes.addDiffLine(baseLine);
                }else {
                    baseRes.addPadLine();
                }
                
                if(cmpLine != null){
                    cmpRes.addDiffLine(cmpLine);
                }else {
                    cmpRes.addPadLine();
                }
                
                break;
            case OBJECT_SAME:
            case OBJECT_VALUE_SAME:
            default:
                if(baseLine != null){
                    baseRes.addNormalLine(baseLine);
                }else {
                    baseRes.addPadLine();
                }
                
                if(cmpLine != null){
                    cmpRes.addNormalLine(cmpLine);
                }else {
                    cmpRes.addPadLine();
                }
        }
    }
    
    public void addBaseObjView(int contentLen){
        baseView.addObjFold(baseRes.getCurLine(), contentLen);
    }
    
    public void addCompareObjView(int contentLen){
        cmpView.addObjFold(cmpRes.getCurLine(), contentLen);
    }
    
    public void addDiffObj(String objLabel){
        if(objLabel != null){
            diffObjs.add(objLabel);
        }    
    }
    
    public void assembleJsonObject(JsonObject jo){
        jo.addProperty("baseContent", getBaseContentString());
        jo.addProperty("cmpContent", getCmpContentString());
        
        jo.addProperty("baseHighlights", StringUtil.mergeListToJsonList(getBaseHighlights()));
        jo.addProperty("cmpHighlights", StringUtil.mergeListToJsonList(getCmpHighlights()));
        
        jo.addProperty("baseHighlightTypes", StringUtil.mergeListToJsonList(getBaseHighlightTypes()));
        jo.addProperty("cmpHighlightTypes", StringUtil.mergeListToJsonList(getCmpHighlightTypes()));
        
        jo.addProperty("baseSkips", StringUtil.mergeListToJsonList(getBaseSkips()));
        jo.addProperty("cmpSkips", StringUtil.mergeListToJsonList(getCmpSkips()));
        
        jo.addProperty("baseTypes", StringUtil.combineListsToJsonDictionary(getBaseTypeLines(), getBaseContentLens()));
        jo.addProperty("cmpTypes", StringUtil.combineListsToJsonDictionary(getCompareTypeLines(), getCompareContentLens()));
        
        jo.addProperty("differentObject", getDiffObjListString());
    }
    
    private String assembleJsonObjectHTMLContent(ArrayList<Integer> skipList, 
                                                 ArrayList<Integer> highlightLinesList,
                                                 ArrayList<IDFCompareResult> highlightTypes,
                                                 ArrayList<String> contentList,
                                                 ArrayList<Integer> otherLabelLines,
                                                 ArrayList<Integer> otherContentLens,
                                                 String type,
                                                 boolean isFold,
                                                 boolean isMerge){
        int skipIdx = 0;
        int skips = skipList.size();
        int highlightLineIdx = 0;
        int highlightLines = highlightLinesList.size();
        int lineIdx=1;
        boolean preSkip = true;  //used to identify obj label
        
        String objNamePadding = StringUtil.spacesHTML(Global.IDF_DISPLAY_PADDING_OBJ_NAME);
        String objFieldPadding = StringUtil.spacesHTML(Global.IDF_DISPLAY_PADDING_OBJ_FIELD);
        
        StringBuilder nums = new StringBuilder();
        StringBuilder contents = new StringBuilder();
        
        int lineNumber = 1;
        
        int otherCursor = 0;
        int otherLabels = otherLabelLines.size();
        int otherLabelLine = otherLabels==0 ? Integer.MAX_VALUE : otherLabelLines.get(otherCursor);
        int otherContentEnds = otherLabels==0 ? Integer.MAX_VALUE : otherLabelLine+otherContentLens.get(otherCursor);
        
        String compareStylePrefix = isMerge ? "" : "compare_";
        
        for(String line : contentList){
            line = StringUtil.escapeBracket(line);
            
            if(skipIdx<skips && lineIdx==skipList.get(skipIdx)){
                //skip line
                String fieldsCls = "";
                String styleCls = "";
                if(lineIdx>=otherLabelLine){
                    if(lineIdx <= otherContentEnds){
                        //block skip lines, spaceholder for other object

                        styleCls = "highlighted_gray";
                        fieldsCls = "field_line ";
                    }else {
                        styleCls = "highlighted_spacer";
                        
                        otherCursor++;
                        while(otherCursor<otherLabels && otherLabelLines.get(otherCursor)<lineIdx){
                            otherCursor++;
                        }
                        
                        if(otherCursor<otherLabels){
                            otherLabelLine = otherLabelLines.get(otherCursor);
                            otherContentEnds = otherLabelLine+otherContentLens.get(otherCursor);
                        }else {
                            otherLabelLine = Integer.MAX_VALUE;
                        }
                    }
                }
                
                nums.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" gutter ").append(fieldsCls).append("\">&nbsp;</div>");
                nums.append("<div class=\"line number").append(lineIdx).append(" ").append(type).append("_placeholder gutter\"></div>");
                
                contents.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" code ").append(compareStylePrefix).append(styleCls).append(" ").append(fieldsCls).append("\" id=\"number").append(lineIdx).append("_").append(type).append("_code\"><code class=\"idf spaces\">&nbsp;&nbsp;&nbsp;&nbsp;</code>&nbsp;</div>");
                contents.append("<div class=\"line number").append(lineIdx).append(" ").append(type).append("_placeholder code\"></div>");
                
                skipIdx++;
                highlightLineIdx++;
                
                preSkip = true;
            }else {
                int commentIdx = line.indexOf('!');
                
                String fieldsCls = commentIdx==0 ? "" : preSkip ? "" : "field_line ";
                
                boolean add = false;
                boolean del = false;
                String styleLabel = "";
                if(highlightLineIdx<highlightLines && lineIdx==highlightLinesList.get(highlightLineIdx)){
                    //highlight line
                    switch(highlightTypes.get(highlightLineIdx)){
                        case LINE_DELETED:
                            styleLabel = "highlighted_delete";
                            del = true;
                            break;
                        case LINE_ADDED:
                            styleLabel = "highlighted_add";
                            add = true;
                            break;
                        case LINE_DIFF:
                            styleLabel = "highlighted_diff";
                            break;
                        default:
                            
                    }
                    
                    highlightLineIdx++;
                }
                
                contents.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" code ").append(fieldsCls);
                nums.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" gutter ").append(fieldsCls);
                
                contents.append(compareStylePrefix).append(styleLabel);
                //nums.append(styleLabel);
                
                nums.append("\">").append(lineNumber).append("</div>");
                nums.append("<div class=\"line number").append(lineIdx).append(" ").append(type).append("_placeholder gutter\"></div>");
                
                contents.append("\" id=\"number").append(lineIdx).append("_").append(type).append("_code\">");
                
                if(commentIdx==0){
                    //comment line
                    contents.append("<code class=\"idf comments\">").append(line).append("</code>");
                }else {
                    int comma = line.indexOf(',');
                    int semiComma = line.indexOf(';');
                    String punctural = ",";
                    
                    int delimiter = -1;
                    if(comma<0){
                        delimiter = semiComma;
                        punctural = ";";
                    }else if(semiComma<0){
                        delimiter = comma;
                        punctural = ",";
                    }else {
                        delimiter = Math.min(comma, semiComma);
                        punctural = comma<semiComma ? "," : ";";
                    }
                    
                    if(preSkip){
                        //label line
                        contents.append("<code class=\"idf spaces\">").append(objNamePadding).append("</code>");
                        
                        contents.append("<code class=\"idf keyword\">");
                        
                        if(isFold){
                            contents.append("<span num=\"").append(lineIdx).append("\" class=\"obj_type obj_type_").append(lineIdx).append("\" style=\"cursor: pointer;\">[+]</span>");
                        }
                        
                        String label = line.substring(0, delimiter).trim();
                        if(isMerge && (add || del)){
                            //add cancel button
                            String cancelTxt = add ? "Remove Compare" : "Keep Base";
                            contents.append("<span num=\"").append(lineIdx).append("\" label=\""+label+"\"  class=\"cancel_obj\" style=\"cursor: pointer;\">["+cancelTxt+"]</span>");
                        }
                        
                        contents.append(label).append("</code>");
                        contents.append("<code class=\"idf plain\">").append(punctural).append("</code>");
                        
                        preSkip = false;
                    }else {
                        //field line
                        contents.append("<code class=\"idf spaces\">").append(objFieldPadding).append("</code>");
                        contents.append("<code class=\"idf plain\">").append(line.substring(0, delimiter+1).trim()).append("</code>");
                    }
                    
                    if(commentIdx>0){
                        contents.append("<code class=\"idf comments\">").append(line.substring(delimiter+1)).append("</code>");
                    }
                }
                
                contents.append("</div>");
                contents.append("<div class=\"line number").append(lineIdx).append(" ").append(type).append("_placeholder code\"></div>");
                
                lineNumber++;
            }
            
            lineIdx++;
        }
        
        StringBuilder res = new StringBuilder();
        if(isMerge && type.equals("cmp")){
            res.append("<div>\n<div class=\"syntaxhighlighter idf\">\n<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n<tbody>\n<tr>\n");
            res.append("<td class=\"code\">\n<div class=\"container\">\n").append(contents);
            res.append("</div>\n</td>\n</tr>\n</tbody>\n</table>\n</div>\n</div>\n");
        }else {
            res.append("<div>\n<div class=\"syntaxhighlighter idf\">\n<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n<tbody>\n<tr>\n<td class=\"gutter\">\n");
            res.append(nums).append("</td>\n<td class=\"code\">\n<div class=\"container\">\n").append(contents);
            res.append("</div>\n</td>\n</tr>\n</tbody>\n</table>\n</div>\n</div>\n");
        }
        
        String result = res.toString();
        nums.setLength(0);
        contents.setLength(0);
        res.setLength(0);

        return result;
    }
    
    protected JsonObject assembleJsonObjectHTMLLinedContent(ArrayList<Integer> skipList, 
                                                          ArrayList<Integer> highlightLinesList,
                                                          ArrayList<IDFCompareResult> highlightTypes,
                                                          ArrayList<String> contentList,
                                                          ArrayList<Integer> otherLabelLines,
                                                          ArrayList<Integer> otherContentLens,
                                                          String type){
        JsonObject jo = new JsonObject();
        
        JsonArray numLines = new JsonArray();
        JsonArray contentLines = new JsonArray();
        //TODO
        
        int skipIdx = 0;
        int skips = skipList.size();
        int highlightLineIdx = 0;
        int highlightLines = highlightLinesList.size();
        int lineIdx=1;
        boolean preSkip = false;  //used to identify obj label
        
        String objNamePadding = StringUtil.spacesHTML(Global.IDF_DISPLAY_PADDING_OBJ_NAME);
        String objFieldPadding = StringUtil.spacesHTML(Global.IDF_DISPLAY_PADDING_OBJ_FIELD);
        
        StringBuilder nums = new StringBuilder();
        StringBuilder contents = new StringBuilder();
        
        int lineNumber = 1;
        
        int otherCursor = 0;
        int otherLabels = otherLabelLines.size();
        int otherLabelLine = otherLabels==0 ? Integer.MAX_VALUE : otherLabelLines.get(otherCursor);
        int otherContentEnds = otherLabels==0 ? Integer.MAX_VALUE : otherLabelLine+otherContentLens.get(otherCursor);
        
        for(String line : contentList){
            line = StringUtil.escapeBracket(line);
            
            if(skipIdx<skips && lineIdx==skipList.get(skipIdx)){
                //skip line
                String fieldsCls = "";
                if(lineIdx>otherLabelLine){
                    if(lineIdx <= otherContentEnds){
                        //block skip lines, spaceholder for other object
                        
                        fieldsCls = "field_line ";
                    }else {
                        otherCursor++;
                        while(otherCursor<otherLabels && otherLabelLines.get(otherCursor)<lineIdx){
                            otherCursor++;
                        }
                        
                        if(otherCursor<otherLabels){
                            otherLabelLine = otherLabelLines.get(otherCursor);
                            otherContentEnds = otherLabelLine+otherContentLens.get(otherCursor);
                        }else {
                            otherLabelLine = Integer.MAX_VALUE;
                        }
                    }
                }
                
                nums.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" gutter highlighted_gray ").append(fieldsCls).append("\">&nbsp;</div>");                
                contents.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" code highlighted_gray ").append(fieldsCls).append("\" id=\"number").append(lineIdx).append("_").append(type).append("_code\"><code class=\"idf spaces\">&nbsp;&nbsp;&nbsp;&nbsp;</code>&nbsp;</div>");
                
                
                skipIdx++;
                highlightLineIdx++;
                
                preSkip = true;
            }else {
                int commentIdx = line.indexOf('!');
                
                String fieldsCls = commentIdx==0 ? "" : preSkip ? "" : "field_line ";
                        
                contents.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" code ").append(fieldsCls);
                nums.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 ").append(type).append(" gutter ").append(fieldsCls);
                
                if(highlightLineIdx<highlightLines && lineIdx==highlightLinesList.get(highlightLineIdx)){
                    //highlight line
                    switch(highlightTypes.get(highlightLineIdx)){
                        case LINE_DELETED:
                            contents.append("highlighted_delete");
                            nums.append("highlighted_delete");
                            break;
                        case LINE_ADDED:
                            contents.append("highlighted_add");
                            nums.append("highlighted_add");
                            break;
                        case LINE_DIFF:
                            contents.append("highlighted_diff");
                            nums.append("highlighted_diff");
                            break;
                        default:
                            
                    }
                    
                    highlightLineIdx++;
                }
                
                nums.append("\">").append(lineNumber).append("</div>");
                
                contents.append("\" id=\"number").append(lineIdx).append("_").append(type).append("_code\">");
                
                if(commentIdx==0){
                    //comment line
                    contents.append("<code class=\"idf comments\">").append(line).append("</code>");
                }else {
                    int comma = line.indexOf(',');
                    int semiComma = line.indexOf(';');
                    String punctural = ",";
                    
                    int delimiter = -1;
                    if(comma<0){
                        delimiter = semiComma;
                        punctural = ";";
                    }else if(semiComma<0){
                        delimiter = comma;
                        punctural = ",";
                    }else {
                        delimiter = Math.min(comma, semiComma);
                        punctural = comma<semiComma ? "," : ";";
                    }
                    
                    if(preSkip){
                        //label line
                        contents.append("<code class=\"idf spaces\">").append(objNamePadding).append("</code>");
                        
                        contents.append("<code class=\"idf keyword\">");
                        contents.append("<span num=\"").append(lineIdx).append("\" class=\"obj_type obj_type_").append(lineIdx).append("\" style=\"cursor: pointer;\">[+]</span>");
                        contents.append(line.substring(0, delimiter).trim()).append("</code>");
                        contents.append("<code class=\"idf plain\">").append(punctural).append("</code>");
                        
                        preSkip = false;
                    }else {
                        //field line
                        contents.append("<code class=\"idf spaces\">").append(objFieldPadding).append("</code>");
                        contents.append("<code class=\"idf plain\">").append(line.substring(0, delimiter+1).trim()).append("</code>");
                    }
                    
                    if(commentIdx>0){
                        contents.append("<code class=\"idf comments\">").append(line.substring(delimiter+1)).append("</code>");
                    }
                }
                
                contents.append("</div>");
                
                lineNumber++;
            }
            
            lineIdx++;
            
            //Add to json array
            numLines.add(nums.toString());
            contentLines.add(contents.toString());
            
            nums.setLength(0);
            contents.setLength(0);
        }
        
        StringBuilder res = new StringBuilder();
        res.append("<div>\n<div class=\"syntaxhighlighter idf\">\n<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n<tbody>\n<tr>\n<td class=\"gutter\">\n");
        res.append(nums).append("</td>\n<td class=\"code\">\n<div class=\"container\">\n").append(contents);
        res.append("</div>\n</td>\n</tr>\n</tbody>\n</table>\n</div>\n</div>\n");
        
        
        //TODO
        jo.add("nums", numLines);
        jo.add("contents", contentLines);
        
        return jo;
    }
    
    private String makeNavMarker(int lineNum, int lastLine, String color){
        double top = lineNum*100.0/lastLine;
        return "<div class='sub_nav_bar_marker' line='"+lineNum+"' style='border:1px solid "+color+";top:"+top+"%;'></div>";
    }
    private String assembleJsonObjectHTMLNav(ArrayList<Integer> lines, ArrayList<IDFCompareResult> highlights){
        StringBuilder res = new StringBuilder();
        
        int len = lines.size();
        if(len>0){
            int lastLine = lines.get(len-1);
            for(int i=0;i<len;i++){
                IDFCompareResult cmpRes = highlights.get(i);
                switch(cmpRes){
                    case LINE_DIFF:
                        res.append(makeNavMarker(lines.get(i), lastLine, "blue"));
                        break;
                    case LINE_DELETED:
                        res.append(makeNavMarker(lines.get(i), lastLine, "red"));
                        break;
                    case LINE_ADDED:
                        res.append(makeNavMarker(lines.get(i), lastLine, "green"));
                        break;
                    default:
                }
            }
        }
        
        return res.toString();
    }
    public void assembleJsonObjectHTML(JsonObject jo){
        //Return highlighted base/cmp content html
        //Return nav base/cmp bars html
        //Return obj label lines
        //Return obj content lines
        ArrayList<String> baseContentList = baseRes.getContentList();
        ArrayList<String> cmpContentList = cmpRes.getContentList();
        
        ArrayList<Integer> baseHighLightLines = baseRes.getHighLights();
        ArrayList<Integer> cmpHighLightLines = cmpRes.getHighLights();
        
        ArrayList<IDFCompareResult> baseHighLightTypes = baseRes.getHighLightTypes();
        ArrayList<IDFCompareResult> cmpHighLightTypes = cmpRes.getHighLightTypes();
        
        ArrayList<Integer> baseSkips = baseRes.getSkips();
        ArrayList<Integer> cmpSkips = cmpRes.getSkips();
        
        jo.addProperty("baseContent", assembleJsonObjectHTMLContent(baseSkips, baseHighLightLines, baseHighLightTypes, baseContentList, getCompareTypeLines(), getCompareContentLens(), "base", true, false));
        jo.addProperty("cmpContent", assembleJsonObjectHTMLContent(cmpSkips, cmpHighLightLines, cmpHighLightTypes, cmpContentList, getBaseTypeLines(), getBaseContentLens(), "cmp", true, false));
        
        jo.addProperty("baseNav", assembleJsonObjectHTMLNav(baseHighLightLines, baseHighLightTypes));
        jo.addProperty("cmpNav", assembleJsonObjectHTMLNav(cmpHighLightLines, cmpHighLightTypes));
        
        jo.addProperty("baseHighlightsTypeLine", StringUtil.combineListsToJsonDictionary(getBaseHighlights(), getBaseHighlightTypes()));
        jo.addProperty("cmpHighlightsTypeLine", StringUtil.combineListsToJsonDictionary(getCmpHighlights(), getCmpHighlightTypes()));
        
        jo.addProperty("baseTypes", StringUtil.combineListsToJsonDictionary(getBaseTypeLines(), getBaseContentLens()));
        jo.addProperty("cmpTypes", StringUtil.combineListsToJsonDictionary(getCompareTypeLines(), getCompareContentLens()));
    }
    
    public void assembleJsonObjectHTMLTreeStructure(JsonObject jo, boolean isMerge){
        ArrayList<Integer> baseSkips = baseRes.getSkips();
        ArrayList<Integer> cmpSkips = cmpRes.getSkips();
        
        ArrayList<Integer> baseHighLightLines = baseRes.getHighLights();
        ArrayList<Integer> cmpHighLightLines = cmpRes.getHighLights();
        
        ArrayList<IDFCompareResult> baseHighLightTypes = baseRes.getHighLightTypes();
        ArrayList<IDFCompareResult> cmpHighLightTypes = cmpRes.getHighLightTypes();
        
        ArrayList<String> baseContentList = baseRes.getContentList();
        ArrayList<String> cmpContentList = cmpRes.getContentList();
        
        jo.addProperty("baseContent", assembleJsonObjectHTMLContent(baseSkips, baseHighLightLines, baseHighLightTypes, baseContentList, getCompareTypeLines(), getCompareContentLens(), "base", false, isMerge));
        jo.addProperty("cmpContent", assembleJsonObjectHTMLContent(cmpSkips, cmpHighLightLines, cmpHighLightTypes, cmpContentList, getBaseTypeLines(), getBaseContentLens(), "cmp", false, isMerge));
        
        jo.addProperty("baseHighlightsTypeLine", StringUtil.combineListsToJsonDictionary(getBaseHighlights(), getBaseHighlightTypes()));
        //jo.addProperty("cmpHighlightsTypeLine", StringUtil.combineListsToJsonDictionary(getCmpHighlights(), getCmpHighlightTypes()));
        
        //jo.addProperty("baseTypes", StringUtil.combineListsToJsonDictionary(getBaseTypeLines(), getBaseContentLens()));
        //jo.addProperty("cmpTypes", StringUtil.combineListsToJsonDictionary(getCompareTypeLines(), getCompareContentLens()));
    }
    
    public String getBaseContentString(){
        return baseRes.getContentString();
    }

    public ArrayList<Integer> getBaseHighlights(){
        return baseRes.getHighLights();
    }
    
    public ArrayList<IDFCompareResult> getBaseHighlightTypes(){
        return baseRes.getHighLightTypes();
    }
    
    public ArrayList<Integer> getBaseSkips(){
        return baseRes.getSkips();
    }
    
    public String getCmpContentString(){
        return cmpRes.getContentString();
    }

    public ArrayList<Integer> getCmpHighlights(){
        return cmpRes.getHighLights();
    }
    
    public ArrayList<IDFCompareResult> getCmpHighlightTypes(){
        return cmpRes.getHighLightTypes();
    }
    
    public ArrayList<Integer> getCmpSkips(){
        return cmpRes.getSkips();
    }
    
    public ArrayList<Integer> getBaseTypeLines(){
        return baseView.getObjTypeLines();
    }
    
    public ArrayList<Integer> getBaseContentLens(){
        return baseView.getObjContentLens();
    }
    
    public ArrayList<Integer> getCompareTypeLines(){
        return cmpView.getObjTypeLines();
    }
    
    public ArrayList<Integer> getCompareContentLens(){
        return cmpView.getObjContentLens();
    }
    
    public String getDiffObjListString(){
        return StringUtil.mergeCollectionToString(diffObjs, ",");
    }
    
    public int getCurrentBaseResLineCount(){
        return baseRes.getCurLine();
    }
}
