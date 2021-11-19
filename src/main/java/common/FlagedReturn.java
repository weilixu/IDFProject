package main.java.common;

public class FlagedReturn<T> {
    private T data;
    private String flag;
    
    public FlagedReturn(String flag, T data){
        this.flag = flag;
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public String getFlag() {
        return flag;
    }
}
