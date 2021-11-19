package main.java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DBUtil.class);
    
    public static Blob convertToBlob(Object o, Connection conn){
        if(o==null){
            LOG.error("Object argument is null");
            return null;
        }
        
        try {
            if(conn==null || conn.isClosed()){
                LOG.error("Connection is null or closed already.");
                return null;
            }
        } catch (SQLException e) {
            LOG.error("Test connection validity is failed, "+e.getMessage());
            return null;
        }
        
        Blob blob = null;
        try{
            blob = conn.createBlob();
            try(OutputStream os = blob.setBinaryStream(1);
                    ObjectOutputStream out = new ObjectOutputStream(os)){
                
                out.writeObject(o);
                out.flush();
            } catch (IOException e) {
                LOG.error("Create Blob failed when writing data to blob, "+e.getMessage(), e);
                blob = null;
            }
        }catch (SQLException e){
            LOG.error("Create Blob failed when create blob from connection, "+e.getMessage(), e);
        }
        
        return blob;
    }
    
    public static Object readToObject(Blob blob){
        if(blob==null){
            return null;
        }
        
        Object res;
        
        try(InputStream is = blob.getBinaryStream();
                ObjectInputStream ois = new ObjectInputStream(is)){
            res = ois.readObject();
        } catch (IOException e) {
            LOG.error("Read blob data into object failed (I/O), "+e.getMessage(), e);
            res = null;
        } catch (SQLException e) {
            LOG.error("Get inputstream from blob failed, "+e.getMessage(), e);
            res = null;
        } catch (ClassNotFoundException e) {
            LOG.error("Read blob data into object failed (class not found), "+e.getMessage(), e);
            res = null;
        } finally {
            try {
                blob.free();
            } catch (SQLException e) {}
        }
        
        return res;
    }
}
