package main.java.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SortedMapsIterator<K, V> {
    public static final int SORT_BY_KEY = 1;
    public static final int SORT_BY_VALUE = 2;
    
    private Iterator<Entry<K, V>> m1Iter = null;
    private Iterator<Entry<K, V>> m2Iter = null;
    
    private Entry<K, V> m1Next = null;
    private Entry<K, V> m2Next = null;
    
    private Comparator<Entry<K, V>> comparator = null;
    
    public SortedMapsIterator(Map<K, V> m1, Map<K, V> m2, Comparator<Entry<K, V>> comparator){
        if(m1!=null){
            m1Iter = m1.entrySet().iterator();
            if(m1Iter.hasNext()){
                m1Next = m1Iter.next();
            }
        }
        
        if(m2!=null){
            m2Iter = m2.entrySet().iterator();
            if(m2Iter.hasNext()){
                m2Next = m2Iter.next();
            }
        }
        
        this.comparator = comparator;
    }
    
    public ArrayList<Entry<K, V>> getNext(){
        ArrayList<Entry<K, V>> res = new ArrayList<>(2);
        res.add(null);
        res.add(null);
        
        boolean m1Forward = false;
        boolean m2Forward = false;
        
        if(m1Next == null || m2Next == null){
            if(m1Next != null){
                m1Forward = true;
            }
            if(m2Next != null){
                m2Forward = true;
            }
        }else {
            int cmp = comparator.compare(m1Next, m2Next);
            if(cmp == 0){
                m1Forward = true;
                m2Forward = true;
            }else if(cmp<0){
                m1Forward = true;
            }else {
                m2Forward = true;
            }
        }
        
        if(m1Forward){
            Entry<K, V> val = new AbstractMap.SimpleEntry<K, V>(m1Next.getKey(), m1Next.getValue());
            res.set(0, val);
            m1Next = m1Iter.hasNext() ? m1Iter.next() : null;
        }
        if(m2Forward){
            Entry<K, V> val = new AbstractMap.SimpleEntry<K, V>(m2Next.getKey(), m2Next.getValue());
            res.set(1, val);
            m2Next = m2Iter.hasNext() ? m2Iter.next() : null;
        }
        
        return res;
    }
}
