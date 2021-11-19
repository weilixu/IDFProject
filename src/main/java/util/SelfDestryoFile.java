package main.java.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SelfDestryoFile extends File {
	private static final long serialVersionUID = -1345108234928057207L;
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private boolean includeParent;

    public SelfDestryoFile(String pathName, boolean includeParent){
        super(pathName);
        this.includeParent = includeParent;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
        	String path = this.getAbsolutePath();
            if (includeParent) {
                File parentFolder = this.getParentFile();
                this.delete();
                if (parentFolder != null) {
                    parentFolder.delete();
                }
            }
            //LOG.info("SelfDestroyFile deleted: "+path);
        }catch(Exception e){
            LOG.warn("Delete SelfDestroyFile encounters error, "+e.getMessage(), e);
        }
        finally {
            super.finalize();
        }
    }
}
