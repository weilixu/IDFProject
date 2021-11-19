package main.java.tools.idd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ReadIDDFile {
    public static void main(String[] args){
        String iddPath = "/Users/weilixu/Documents/GitHub/BuildSimHub/resource/idd_v9.4";
        String outPath = "/Users/weilixu/Documents/BuildSimHub/TreeStructure/iddv9.4 object list.txt";
        
        StringBuilder sb = new StringBuilder();
        String line;
        try(FileInputStream fis = new FileInputStream(new File(iddPath));
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr)){
            while((line=br.readLine()) != null){
                if(!line.isEmpty()){
                    char c = line.charAt(0);
                    if(c!='\\' && c!=' ' && c!='!'){
                        sb.append(line).append("\n");
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        
        try(FileWriter fw = new FileWriter(outPath);
                BufferedWriter bw = new BufferedWriter(fw)){
            bw.write(sb.toString());
            bw.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
