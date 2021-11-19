package main.java.model.idf;

import java.util.ArrayList;
import java.util.List;

public class TreePath {
    private String[] path;
    private String key;
    
    public TreePath(String[] path){
        this.path = path;
        
        StringBuilder sb = new StringBuilder(path[0]);
        for(int i=1;i<path.length;i++){
            if(path[i] != null){
                sb.append("+").append(path[i]);
            }else {
                break;
            }
        }
        this.key = sb.toString();
    }
    
    public String getPath(int idx){
        try{
            return path[idx];
        }catch (IndexOutOfBoundsException e){
            return null;
        }
    }
    
    public String getKey(){
        return key;
    }
    
    public List<String> getList(){
        List<String> list = new ArrayList<>();
        for(String p : path){
            if(p!=null){
                list.add(p);
            }else {
                break;
            }
        }
        return list;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TreePath){
            return key.equals(((TreePath)obj).getKey());
        }
        
        return super.equals(obj);
    }
}
