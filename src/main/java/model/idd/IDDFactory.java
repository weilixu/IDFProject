package main.java.model.idd;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IDDFactory {
    private final static ConcurrentMap<String, IDDParser> instances = new ConcurrentHashMap<>();
    
    /**
     * IDDParser not share-able after validate IDFFileObject yet, don't use this factory for validation
     * @param version
     * @return
     */
    public static IDDParser getParser(String version){
        IDDParser parser = instances.get(version);
        if(parser==null){
            synchronized(IDDFactory.class){
                parser = instances.get(version);
                if(parser==null){
                    parser = new IDDParser(version);
                    instances.put(version, parser);
                }
            }
        } 
        return parser;
    }
}
