package main.java.model.idf.meta;

import main.java.model.vc.BranchType;

public class ObjectSeqFactory {
    private ObjectSeqFactory(){}
    
    public static ObjectSeq getIDFObjectSeq(String version, BranchType type){
        switch(type){
            case idf:
                switch(version){
                    case "8.0":
                        return new IDFObjectSeq_V8_0();
                    case "8.1":
                        return new IDFObjectSeq_V8_1();
                    case "8.2":
                        return new IDFObjectSeq_V8_2();
                    case "8.3":
                        return new IDFObjectSeq_V8_3();
                    case "8.4":
                    case "8.5":
                        return new IDFObjectSeq_V8_4();
                    case "8.6":
                        return new IDFObjectSeq_V8_6();
                    case "8.7":
                        return new IDFObjectSeq_V8_7();
                    case "8.8":
                        return new IDFObjectSeq_V8_8();
                    case "8.9":
                        return new IDFObjectSeq_V8_9();
                    case "9.0":
                        return new IDFObjectSeq_V9_0();
                    case "9.1":
                        return new IDFObjectSeq_V9_1();
                    case "9.2":
                    		return new IDFObjectSeq_V9_2();
                    case "9.3":
                    		return new IDFObjectSeq_V9_3();
                    case "9.4":
                    		return new IDFObjectSeq_V9_4();
                    default:
                        return null;
                }
            default:
                return null;
        }
       
    }
}
