package main.java.model.result.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.model.idf.IDFObject;
import main.java.model.idf.ModelTreeStructure;
import main.java.model.meta.ModelFileObject;
import main.java.model.vc.BranchType;
import main.java.util.StringUtil;

public class ModelViewResult {
    private static final Logger LOG = LoggerFactory.getLogger(ModelViewResult.class);
    
    private ArrayList<Integer> objTypeLines = null;
    private ArrayList<Integer> objContentLens = null;
    
    public ModelViewResult(){
        objTypeLines = new ArrayList<>();
        objContentLens = new ArrayList<>();
    }
    
    public void addObjFold(int objTypeLine, int objContentLen){
        this.objTypeLines.add(objTypeLine);
        this.objContentLens.add(objContentLen);
    }
    
    public ArrayList<Integer> getObjTypeLines(){
        return objTypeLines;
    }
    
    public ArrayList<Integer> getObjContentLens(){
        return objContentLens;
    }
    
    public static ModelViewResult buildViewResultForSingleModelFile(ModelFileObject modelFileObj){
        int curLine = 1;
        
        ModelViewResult res = new ModelViewResult();
        
        LinkedHashMap<String, Integer> labelsMap = modelFileObj.getSortedLabelMap();
        Set<String> labels = labelsMap.keySet();  //order guaranteed
        
        for(String label : labels){
            Collection<IDFObject> objs = modelFileObj.getCategoryMap(label).values();
            
            for(IDFObject obj : objs){
                curLine += obj.getTopCommentsLen();
                
                int objContentLen = obj.getObjLen();
                res.addObjFold(curLine, objContentLen-1);
                curLine += objContentLen+1;
            }
        }
        
        return res;
    }
    
    /*public static Map<String, String> buildViewResultHTMLForSingleModelFile(ModelFileObject modelFileObj, boolean isFold, BranchType type){
        Map<String, String> res = new HashMap<>();
        
        Map<String, StringBuilder[]> numAndContent = new HashMap<>();
        Map<String, Integer> idxes = new HashMap<>();
        Map<String, Integer> lineNums = new HashMap<>();
        
        Map<String, String> labelToId = ModelTreeStructure.readLabelToIdMap(modelFileObj.getVersion(), false, type);
        if(labelToId==null){
            return null;
        }
        
        int pad = modelFileObj.getValueCommentPad();
        LinkedHashMap<String, Integer> labelsMap = modelFileObj.getSortedLabelMap();
        Set<String> labels = labelsMap.keySet();  //order guaranteed
        
        String objNamePadding = StringUtil.spacesHTML(Global.IDF_DISPLAY_PADDING_OBJ_NAME);
        String objFieldPadding = StringUtil.spacesHTML(Global.IDF_DISPLAY_PADDING_OBJ_FIELD);
        
        for(String label : labels){
            String id = labelToId.get(label.toLowerCase());
            
            if(id==null){
                LOG.warn(label+" don't have id");
                continue;
            }
            
            if(!numAndContent.containsKey(id)){
                numAndContent.put(id, new StringBuilder[]{new StringBuilder(), new StringBuilder()});
                idxes.put(id, 1);
                lineNums.put(id, 1);
            }
            
            StringBuilder[] htmls = numAndContent.get(id);
            int lineIdx = idxes.get(id);
            int lineNumber = lineNums.get(id);
            
            List<IDFObject> idfObjs = modelFileObj.getCategoryList(label);
            for(IDFObject obj : idfObjs){
                // add comments if any
                String[] comments = obj.getTopComments();
                if(comments!=null){
                    for(String comment : comments){
                        htmls[0].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\">");

                        comment = StringUtil.escapeBracket(comment);
                        htmls[0].append("<code class=\"idf comments\">").append(comment).append("</code></div>");
                        
                        htmls[1].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">").append(lineNumber).append("</div>");
                        
                        lineIdx++;
                        lineNumber++;
                    }
                }
                
                // add label line
                String objLabel = obj.getObjLabel().trim();
                htmls[0].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\">");
                htmls[0].append("<code class=\"idf spaces\">").append(objNamePadding).append("</code>");
                htmls[0].append("<code class=\"idf keyword\">");                
                if(isFold){
                    htmls[0].append("<span num=\"").append(lineIdx).append("\" class=\"obj_type obj_type_").append(lineIdx).append("\" style=\"cursor: pointer;\">[+]</span>");
                }                
                htmls[0].append(objLabel).append("</code>");
                htmls[0].append("<code class=\"idf plain\">,</code></div>");
                
                htmls[1].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">").append(lineNumber).append("</div>");
                
                lineIdx++;
                lineNumber++;
                
                // add contents
                int contentLen = obj.getObjLen();
                for(int i=1;i<contentLen;i++){
                    String line = obj.getLine(i, pad, true);
                    line = StringUtil.escapeBracket(line);
                    
                    int comma = line.indexOf(',');
                    int semiComma = line.indexOf(';');
                    
                    int delimiter = -1;
                    if(comma<0){
                        delimiter = semiComma;
                    }else if(semiComma<0){
                        delimiter = comma;
                    }else {
                        delimiter = Math.min(comma, semiComma);
                    }
                    
                    htmls[0].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\">");
                    htmls[0].append("<code class=\"idf spaces\">").append(objFieldPadding).append("</code>");
                    htmls[0].append("<code class=\"idf plain\">").append(line.substring(0, delimiter+1).trim()).append("</code>");
                    if(line.indexOf('!')>0){
                        htmls[0].append("<code class=\"idf comments\">").append(line.substring(delimiter+1)).append("</code>");
                    }
                    htmls[0].append("</div>");
                    
                    htmls[1].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">").append(lineNumber).append("</div>");
                    
                    lineIdx++;
                    lineNumber++;
                }
                
                // add skip line
                htmls[1].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">&nbsp;</div>");
                
                htmls[0].append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\"><code class=\"idf spaces\">&nbsp;&nbsp;&nbsp;&nbsp;</code>&nbsp;</div>");
                
                lineIdx++;
                
                // update idx and line number
                idxes.put(id, lineIdx);
                lineNums.put(id, lineNumber);
            }
        }
        
        for(String id : numAndContent.keySet()){
            StringBuilder[] htmls = numAndContent.get(id);
            
            StringBuilder sb = new StringBuilder();
            sb.append("<div>\n<div class=\"syntaxhighlighter idf\">\n<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n<tbody>\n<tr>\n<td class=\"gutter\">\n");
            sb.append(htmls[1].toString()).append("</td>\n<td class=\"code\">\n<div class=\"container\">\n").append(htmls[0].toString());
            sb.append("</div>\n</td>\n</tr>\n</tbody>\n</table>\n</div>\n</div>\n");
            
            res.put(id, sb.toString());
        }
        
        return res;
    }*/
    public static String buildViewResultHTMLForSingleLabel(List<IDFObject> idfObjs,
                                                           int pad,
                                                           String objNamePadding,
                                                           String objFieldPadding){
        //overwrite pad
        pad = 2;

        StringBuilder numGutter = new StringBuilder();
        StringBuilder content = new StringBuilder();

        int lineIdx = 1;
        int lineNumber = 1;

        for(IDFObject obj : idfObjs){
            // add comments if any
            String[] comments = obj.getTopComments();
            if(comments!=null){
                for(String comment : comments){
                    content.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\">");

                    comment = StringUtil.escapeBracket(comment);
                    content.append("<code class=\"idf comments\">").append(comment).append("</code></div>");

                    numGutter.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">").append(lineNumber).append("</div>");

                    lineIdx++;
                    lineNumber++;
                }
            }

            // add label line
            String objLabel = obj.getObjLabel().trim();
            content.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\">");
            content.append("<code class=\"idf spaces\">").append(objNamePadding).append("</code>");
            content.append("<code class=\"idf keyword\">");
            content.append(objLabel).append("</code>");
            content.append("<code class=\"idf plain\">,</code></div>");

            numGutter.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">").append(lineNumber).append("</div>");

            lineIdx++;
            lineNumber++;

            // add contents
            int contentLen = obj.getObjLen();
            for(int i=1;i<contentLen;i++){
                String line = obj.getLine(i, pad, true);
                line = StringUtil.escapeBracket(line);

                int comma = line.indexOf(',');
                int semiComma = line.indexOf(';');

                int delimiter = -1;
                if(comma<0){
                    delimiter = semiComma;
                }else if(semiComma<0){
                    delimiter = comma;
                }else {
                    delimiter = Math.min(comma, semiComma);
                }

                content.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\">");
                content.append("<code class=\"idf spaces\">").append(objFieldPadding).append("</code>");
                content.append("<code class=\"idf plain\">").append(line.substring(0, delimiter+1).trim()).append("</code>");
                if(line.indexOf('!')>0){
                    content.append("<code class=\"idf comments\">").append(line.substring(delimiter+1)).append("</code>");
                }
                content.append("</div>");

                numGutter.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">").append(lineNumber).append("</div>");

                lineIdx++;
                lineNumber++;
            }

                // add skip line
            numGutter.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base gutter\">&nbsp;</div>");

            content.append("<div class=\"line number").append(lineIdx).append(" index").append(lineIdx-1).append(" alt1 base code\" id=\"number").append(lineIdx).append("_base_code\"><code class=\"idf spaces\">&nbsp;&nbsp;&nbsp;&nbsp;</code>&nbsp;</div>");

            lineIdx++;
        }

        StringBuilder res = new StringBuilder();
        res.append("<div>\n<div class=\"syntaxhighlighter idf\">\n<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n<tbody>\n<tr>\n<td class=\"gutter\">\n");
        res.append(numGutter.toString()).append("</td>\n<td class=\"code\">\n<div class=\"container\">\n").append(content.toString());
        res.append("</div>\n</td>\n</tr>\n</tbody>\n</table>\n</div>\n</div>\n");

        return res.toString();
    }
    
    public static List<String> listContentLabelIdsForSingleModel(ModelFileObject modeFileObj, BranchType type){
        List<String> res = new ArrayList<>();
        
        Map<String, String> labelToId = ModelTreeStructure.readLabelToIdMap(modeFileObj.getVersion(), false, type);
        if(labelToId==null){
            return null;
        }
        
        LinkedHashMap<String, Integer> labelsMap = modeFileObj.getSortedLabelMap();
        Set<String> labels = labelsMap.keySet();  //order guaranteed
        
        for(String label : labels){
            String id = labelToId.get(label.toLowerCase());
            
            if(id==null){
                LOG.warn(label+" don't have id");
                continue;
            }
            
            res.add(id);
        }
        
        return res;
    }
}
