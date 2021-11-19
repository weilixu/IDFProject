package main.java.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArrayParallelIterator<K, V> {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private K[] arr1;
    private V[] arr2;
    
    private int arr1Len = 0;
    private int arr2Len = 0;
    private int cursor = 0;
    
    public ArrayParallelIterator(K[] arr1, V[] arr2){
        this.arr1 = arr1;
        this.arr2 = arr2;
        
        if(arr1!=null){
            arr1Len = arr1.length;
        }
        if(arr2!=null){
            arr2Len = arr2.length;
        }
    }
    
    public boolean hasNext(){
        return cursor<arr1Len || cursor<arr2Len;
    }
    
    public Object[] next(){
        Object[] res = null;
        
        if(hasNext()){
            res = new Object[2];
            
            if(cursor<arr1Len){
                res[0] = arr1[cursor];
            }
            if(cursor<arr2Len){
                res[1] = arr2[cursor];
            }
            cursor++;
        }else {
            LOG.warn("Called next when there is no next available, "+arr1Len+", "+arr2Len+", "+cursor);
        }
        
        return res;
    }
    
    public boolean hasContent(){
        return arr1Len>0 || arr2Len>0;
    }
}
