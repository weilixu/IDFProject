package main.java.model.result.compare;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.model.idf.IDFObject;
import main.java.model.vc.ConflictResolve;
import main.java.model.vc.IDFCompareResult;

public class ModelObjectCompareResult implements Serializable{
    private static final long serialVersionUID = 3713835537531278277L;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    
    private IDFCompareResult compareFlag = IDFCompareResult.OBJECT_SAME;
    private int size = 0;
    
    private int conflictFlagCursor = -1;
    private IDFCompareResult[] valuesFlags = null;
        
    private IDFObject base = null;
    private IDFObject cmp = null;
    
    private int baseLen = 0;
    private int cmpLen = 0;
    
    public ModelObjectCompareResult(IDFCompareResult flag, int size){
        compareFlag = flag;
        
        if(size>0){
            this.size = size;
            
            this.valuesFlags = new IDFCompareResult[size];
        }
    }
    
    public IDFObject getBase() {
        return base;
    }

    public IDFObject getCmp() {
        return cmp;
    }

    public void setCompareFlag(IDFCompareResult flag){
        compareFlag = flag;
    }
    
    public IDFCompareResult getCompareFlag(){
        return compareFlag;
    }

    public IDFCompareResult getFlag(int index){
        /*
         //For conflict object, label is not highlighted
         if(index==0){
            //For conflict object, label is not highlighted
            if(compareFlag==IDFCompareResult.OBJECT_DIFF){
                return IDFCompareResult.OBJECT_VALUE_SAME;
            }
            return compareFlag;
        }*/
        
        if(index>0 && index<=size){
            return valuesFlags[index-1];
        }
        
        //For conflict object, label is highlighted
        return compareFlag;
    }
    
    public IDFCompareResult[] getFlags(){
        return valuesFlags;
    }
    
    public int getNextConflict(){
        for(conflictFlagCursor++;conflictFlagCursor<size;conflictFlagCursor++){
            if(valuesFlags[conflictFlagCursor] == IDFCompareResult.OBJECT_VALUE_DIFF){
                return conflictFlagCursor;
            }
        }
        
        //no more conflict found
        conflictFlagCursor = -1;
        
        return -1;
    }
    
    public int getCombineSize(){
        return Math.max(baseLen, cmpLen);        
    }

    public void setBase(IDFObject base) {
        this.base = base;
        if(base!=null){
            baseLen = base.getObjLen();
        }
    }

    public void setCmp(IDFObject cmp) {
        this.cmp = cmp;
        if(cmp!=null){
            cmpLen = cmp.getObjLen();
        }
    }

    public void setFlag(int index, IDFCompareResult flag){
        if(index>=0 && index<size){
            valuesFlags[index] = flag;
        }
    }
    
    /**
     * flag is Global.CONFLICT_RESOLVE_PICK_BASE or Global.CONFLICT_RESOLVE_PICK_CMP
     * @param index
     * @param flag
     */
    public void setConflictResolve(int index, ConflictResolve flag){
        if(flag == ConflictResolve.PICK_BASE){
            return;
        }
        if(flag == ConflictResolve.PICK_CMP){
            base.setIndexedData(index, cmp.getIndexedData(index));
            return;
        }
        
        LOG.error("Unknown flag: "+flag);
    }
    
    /**
     * Return if lock is gained
     * @return
     */
    public void resetConflictCursor(){
        this.conflictFlagCursor = -1;
    }
}
