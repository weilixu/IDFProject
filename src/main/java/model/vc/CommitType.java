package main.java.model.vc;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum CommitType {
    MERGED {
        @Override
        public int getDBId() {
            return 1;
        }
    },
    COMMIT {
        @Override
        public int getDBId() {
            return 3;
        }
    },
    CLONE {
        @Override
        public int getDBId() {
            return 4;
        }
    },
    PULL {
        @Override
        public int getDBId() {
            return 5;
        }
    },
    INIT {
        @Override
        public int getDBId() {
            return 6;
        }
    },
    DELETED {
        @Override
        public int getDBId() {
            return 7;
        }
    },
    FORKED{
        @Override
        public int getDBId() {
            return 8;
        }
    },
    EDITED{
        @Override
        public int getDBId() {
            return 9;
        }
    },
    UNKNOWN{
        @Override
        public int getDBId() {
            return -1;
        }        
    };
    
    public abstract int getDBId();
    
    private static Map<Integer, CommitType> map;
    static{
        map = new HashMap<>();
        for(CommitType us : EnumSet.allOf(CommitType.class)){
            map.put(us.getDBId(), us);
        }
    }
    
    public static CommitType getCommitType(int dbId){
        return map.get(dbId);
    }
}
