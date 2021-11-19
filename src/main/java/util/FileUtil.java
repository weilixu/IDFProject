package main.java.util;

import main.java.config.ServerConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    public static File createTempFile(String fileName) {
        File res;

        String tmpfolder = ServerConfig.readProperty("tmpfolder");
        String randomFolderPath = tmpfolder + "ISToFile_" + "/";
        //String randomFolderPath = tmpfolder + "ISToFile_" + SecurityUtil.genRandomStr() + "/";

        File randomFolder = new File(randomFolderPath);
        randomFolder.mkdir();

        res = new SelfDestryoFile(randomFolderPath + fileName, true);

        return res;
    }

    public static File createFolder(String newDirPath) {
        File newDir = new File(newDirPath);
        newDir.mkdirs();
        return newDir;
    }

    public static File convertInputStreamToFile(InputStream is, String fileName) {
        File res = createTempFile(fileName);

        try (FileOutputStream out = new FileOutputStream(res)) {
            IOUtils.copy(is, out);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return res;
    }

    public static File convertStringToFile(String str) {
        if (str == null) {
            str = "";
        }

        File res = createTempFile("StrToFile.tmp");
        try (FileWriter fw = new FileWriter(res);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(str);
            bw.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return res;
    }

    public static File convertStringToFile(String str, File parent, String fileName) {
        if (str == null) {
            str = "";
        }

        File res = new File(parent, fileName);
        try (FileWriter fw = new FileWriter(res);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(str);
            bw.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return res;
    }

    public static void writeStringToFile(String str, String path) {
        if (str == null) {
            str = "";
        }

        try (FileOutputStream fos = new FileOutputStream(path);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "utf-8");
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(str);
            bw.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static String readStringFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, "utf-8");
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }

    public static String getFileHash(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fis);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return null;
    }

    public static boolean saveFileToFolder(File file, File dir) {
        try (
                RandomAccessFile raf = new RandomAccessFile(dir, "rw");
                FileChannel fc = raf.getChannel();

                FileInputStream fis = new FileInputStream(file);
                ReadableByteChannel rbc = Channels.newChannel(fis);
        ) {
            fc.transferFrom(rbc, 0, file.length());
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return false;
    }

    public static boolean saveFileToPath(File file, String path) {
        try (
                RandomAccessFile raf = new RandomAccessFile(new File(path), "rw");
                FileChannel fc = raf.getChannel();

                FileInputStream fis = new FileInputStream(file);
                ReadableByteChannel rbc = Channels.newChannel(fis);
        ) {
            fc.transferFrom(rbc, 0, file.length());
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * suffix with heading dot
     *
     * @param s
     * @param suffix
     * @return
     */
    public static String makeFileName(String s, String suffix) {
        String fileName = s.replaceAll("\\W", "_");
        int suffixLen = suffix.length();
        if (fileName.length() > 255 - suffixLen) {
            fileName = fileName.substring(0, 255 - suffixLen);
        }
        return fileName + suffix;
    }

    public static List<File> readZipFile(File zipFile) {
        List<File> res = new ArrayList<>();

        // unzip file
        int bytesRead;
        byte[] dataBuffer = new byte[1024];
        try (FileInputStream zipFis = new FileInputStream(zipFile);
             ZipInputStream zipIs = new ZipInputStream(zipFis)) {
            ZipEntry entry = zipIs.getNextEntry();
            while (entry != null) {
                File tmp = FileUtil.createTempFile(entry.getName());
                OutputStream outputStream = new FileOutputStream(tmp);
                while ((bytesRead = zipIs.read(dataBuffer)) != -1) {
                    outputStream.write(dataBuffer, 0, bytesRead);
                }
                outputStream.flush();
                outputStream.close();

                res.add(tmp);
                entry = zipIs.getNextEntry();
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return res;
    }

    public static File makeZipFile(File parent, String fileName, File... files) {
        File zipFile = new File(parent, fileName);

        byte[] b = new byte[1024];
        int count;
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream ous = new ZipOutputStream(fos)) {
            for (File file : files) {
                if (file == null) {
                    continue;
                }
                ous.putNextEntry(new ZipEntry(file.getName()));

                try (FileInputStream fis = new FileInputStream(file)) {
                    while ((count = fis.read(b)) > 0) {
                        ous.write(b, 0, count);
                    }
                    ous.flush();
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            zipFile = null;
        }

        return zipFile;
    }

    public static void saveWordDocument(XWPFDocument doc, String path) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            doc.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File prepareFolder(String path) {
        File folder = new SelfDestryoFile(path, true);
        if (folder.exists()) {
            try {
                FileUtils.cleanDirectory(folder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            folder.mkdirs();
        }

        return folder;
    }

    public static String getSuffix(File file) {
        if (file == null) {
            return null;
        }

        return getSuffix(file.getName());
    }

    public static String getSuffix(String fileName) {
        if (fileName == null) {
            return null;
        }

        fileName = fileName.toLowerCase();
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot == -1) {
            return "NO_TYPE";
        }

        return fileName.substring(lastDot + 1, fileName.length());
    }

    public static boolean deleteFile(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteFile(f);
            }
        } else {
            file.delete();
        }
        return true;
    }

    public static File compressFile(String zipFileName, File file) {
        File zipFile = FileUtil.createTempFile(zipFileName);

        byte[] b = new byte[1024];
        int count;
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream ous = new ZipOutputStream(fos)) {
            ous.putNextEntry(new ZipEntry(file.getName()));

            try (FileInputStream fis = new FileInputStream(file)) {
                while ((count = fis.read(b)) > 0) {
                    ous.write(b, 0, count);
                }
                ous.flush();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            zipFile = null;
        }

        return zipFile;
    }

    public static void writeXMLToFile(String outPath, Document doc) {
        try (FileWriter fw = new FileWriter(outPath);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(new XMLOutputter().outputString(doc));
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
