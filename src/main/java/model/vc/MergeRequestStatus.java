package main.java.model.vc;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum MergeRequestStatus {
    Merged {
        @Override
        public int getDBId() {
            return 1;
        }
    },
    Denied {
        @Override
        public int getDBId() {
            return -1;
        }
    },
    Open {
        @Override
        public int getDBId() {
            return 0;
        }
    },
    Withdrawn {
        @Override
        public int getDBId() {
            return -2;
        }
    },
    Commented {
        @Override
        public int getDBId() {
            return 2;
        }
    };
    
    public abstract int getDBId();
    
    private static Map<Integer, MergeRequestStatus> map;
    static{
        map = new HashMap<>();
        for(MergeRequestStatus us : EnumSet.allOf(MergeRequestStatus.class)){
            map.put(us.getDBId(), us);
        }
    }
    
    public static MergeRequestStatus getMergeRequestStatus(int dbId){
        return map.get(dbId);
    }
}
