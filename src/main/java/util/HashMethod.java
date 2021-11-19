package main.java.util;

public enum HashMethod {
    MD5 {
        @Override
        public String getMethod() {
            return "MD5";
        }
    },
    SHA1 {
        @Override
        public String getMethod() {
            return "SHA-1";
        }
    },
    SHA256 {
        @Override
        public String getMethod() {
            return "SHA-256";
        }
    };
    
    public abstract String getMethod();
}
