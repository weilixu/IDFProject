package main.java.model.idf;

import com.google.gson.JsonObject;
import main.java.config.ServerConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IDFParser {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    /**
     * Parse IDF file object from text based .idf file
     *
     * @param idfFile
     * @return
     */
    public JsonObject parseIDFFromIDFFile(File idfFile, IDFFileObject idfFileObj) {
        //debug save, all uploaded file
        /*try {
            FileUtils.copyFile(idfFile, new File("D:\\TestOut\\read.idf"));
        } catch (Exception e) {
            LOG.warn("Debug save file exception: "+e.getMessage());
        }*/

        try (FileInputStream fis = new FileInputStream(idfFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {

            return readAndTrimIDF(br, idfFileObj);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);

            JsonObject res = new JsonObject();
            res.addProperty("status", "error");
            res.addProperty("error_msg", e.getMessage());
            return res;
        }
    }

    /**
     * for debug purpose. It can read idf directly from local machine
     *
     * @param idfFile
     * @return
     */
    public JsonObject parseIDFFromLocalMachine(File idfFile, IDFFileObject idfFileObj) {
        try (FileInputStream fis = new FileInputStream(idfFile);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader br = new BufferedReader(isr)) {

            return this.readAndTrimIDF(br, idfFileObj);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);

            JsonObject res = new JsonObject();
            res.addProperty("status", "error");
            res.addProperty("error_msg", e.getMessage());
            return res;
        }
    }

    public IDFFileObject parseIDFFromeSavedFile(File savedFile) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            fis = new FileInputStream(savedFile);
            ois = new ObjectInputStream(fis);
            Object read = ois.readObject();
            return (IDFFileObject) read;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
            }
            try {
                fis.close();
            } catch (IOException e) {
            }
        }

        LOG.error("Read IDFFileObject from saved serializable object file failed");
        return null;
    }

    private JsonObject readAndTrimIDF(BufferedReader reader, IDFFileObject idfFileObj) {
        JsonObject res = new JsonObject();

        String line, comment = null, unit = "";

        ArrayList<String> lines = new ArrayList<>(),
                units = new ArrayList<>(),
                commentNoUnit = new ArrayList<>(),
                topComments = new ArrayList<>();

        int ExclamationIndex;
        int pad = 2;

        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                ExclamationIndex = line.indexOf("!");
                if (ExclamationIndex == 0) {
                    topComments.add(line);
                    continue; //don't process comments
                }

                // if there is no exclamation mark, it is the head of IDF line
                if (ExclamationIndex > 0) {
                    //extract unit enclosed by {}
                    comment = line.substring(ExclamationIndex);
                    comment = comment.substring(comment.indexOf(" ") + 1).trim();  // comment starts with '!- ' or '! '
                    
                    /*int leftCurlyBraces = comment.indexOf('{');
                    if(leftCurlyBraces>=0){
                        unit = comment.substring(leftCurlyBraces+1, comment.indexOf('}'));
                        comment = comment.substring(0, leftCurlyBraces).trim();
                    }else {
                        unit = "";
                    }*/

                    //line only has values
                    line = line.substring(0, ExclamationIndex).trim();
                }

                if (!line.isEmpty()) {
                    if (line.equalsIgnoreCase("lead input;")
                            || line.equalsIgnoreCase("end lead input;")
                            || line.equalsIgnoreCase("simulation data;")
                            || line.equalsIgnoreCase("end simulation data;")) {
                        continue;
                    }

                    //process every statement line (delimited by ;)
                    String[] values = line.substring(0, line.length() - 1).split(",");
                    int len = values.length;

                    for (int i = 0; i < len - 1; i++) {
                        lines.add(values[i]);
                        units.add(unit);
                        commentNoUnit.add("");
                    }

                    lines.add(values[len - 1]);
                    units.add(unit);
                    commentNoUnit.add(comment);

                    if (line.endsWith(";")) {
                        IDFObject idfObj = new IDFObject(lines, units, commentNoUnit, topComments);

                        String flag = idfFileObj.addObject(idfObj);
                        if (flag == null || !flag.isEmpty()) {
                            LOG.warn("Insert idf object " + idfObj.getObjLabel() + "=>" + idfObj.getName() + " content: " + idfObj.printStatement(0));

                            if (flag != null && !flag.isEmpty()) {
                                res.addProperty("status", "error");
                                res.addProperty("error_msg", "Duplication is detected in " + flag + ", please contact us if there are special cases.");
                                return res;
                            }
                        }

                        /*if (pad < idfObj.getMaxLineLen()) {
                            pad = idfObj.getMaxLineLen();
                        }*/

                        lines.clear();
                        units.clear();
                        commentNoUnit.clear();
                        topComments.clear();
                    }

                    comment = "";
                }
            }

            idfFileObj.setValueCommentPad(pad);

            //ignore end of file comments

            res.addProperty("status", "success");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);

            res.addProperty("status", "error");
            res.addProperty("error_msg", "Server error: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                }
            }
        }

        return res;
    }

    private Boolean IsIncluded(List<String> filters, String value) {
        if (filters == null) {
            return false;
        }

        for (int i = 0; i < filters.size(); i++) {
            if (filters.get(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
