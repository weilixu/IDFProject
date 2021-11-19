package main.java.util;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.*;

public class KeyStoreReader {
    private File keystoreFile;
    private String keyStoreType;
    private char[] keyStorePassword;
    private char[] keyPassword;
    private String alias;
    private File exportedFile;

    public void export() throws Exception {
        KeyStore keystore = KeyStore.getInstance(keyStoreType);
        Encoder encoder = Base64.getEncoder();
        keystore.load(new FileInputStream(keystoreFile), keyStorePassword);
        Key key = keystore.getKey(alias, keyPassword);
        String encoded = new String(encoder.encode(key.getEncoded()));
        FileWriter fw = new FileWriter(exportedFile);
        fw.write("---BEGIN PRIVATE KEY---\n");
        fw.write(encoded);
        fw.write("\n");
        fw.write("---END PRIVATE KEY---");
        fw.close();
    }

    public static void main(String args[]) throws Exception {
        String pathToKeyStore = "D:\\TestOut\\tomcat.keystore";
        String keyStoreType = "JCEKS";
        String keyStorePass = "PASS";
        String alias = "tomcat";
        String keyPass = "PASS";
        String pathToExport = "D:\\TestOut\\tomcat.pem";

        KeyStoreReader export = new KeyStoreReader();
        export.keystoreFile = new File(pathToKeyStore);
        export.keyStoreType = keyStoreType;
        export.keyStorePassword = keyStorePass.toCharArray();
        export.alias = alias;
        export.keyPassword = keyPass.toCharArray();
        export.exportedFile = new File(pathToExport);
        export.export();
    }
}