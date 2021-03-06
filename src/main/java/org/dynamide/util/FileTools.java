package org.dynamide.util;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 * This class is designed to avoid dependencies, so it does not include logging, or apache commons.
 * @author Laramie Crocker
 */
public class FileTools {
    
    public static String DEFAULT_ENCODING = "";
    public static String UTF8_ENCODING = "UTF-8";
    public static Charset UTF8_CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    public static boolean FORCE_CREATE_PARENT_DIRS = true;
    private static String JAVA_TEMP_DIR_PROPERTY = "java.io.tmpdir";

    /**
     * getObjectFromStream get object of given class from given inputstream
     * @param jaxbClass
     * @param is stream to read to construct the object
     * @return
     * @throws Exception
     */
    static protected Object getObjectFromStream(Class<?> jaxbClass, InputStream is) throws Exception {
        JAXBContext context = JAXBContext.newInstance(jaxbClass);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        //note: setting schema to null will turn validator off
        unmarshaller.setSchema(null);
        return jaxbClass.cast(unmarshaller.unmarshal(is));
    }

    static public Object getJaxbObjectFromFile(Class<?> jaxbClass, String fileName)
            throws Exception {

        JAXBContext context = JAXBContext.newInstance(jaxbClass);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        //note: setting schema to null will turn validator off
        unmarshaller.setSchema(null);
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        InputStream is = tccl.getResourceAsStream(fileName);
        return getObjectFromStream(jaxbClass, is);
    }


    //TODO: add parameter for: String encoding
    public static String convertStreamToString2(java.io.InputStream is) {
        if (is == null) return "";
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

	public static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * Reader.read(char[] buffer) method. We iterate until the
		 * Reader return -1 which means there's no more data to
		 * read. We use the StringWriter class to produce the string.
		 */
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return writer.toString();
		} else {       
			return "";
		}
	}
	
    public static void forceParentDirectories(String filename) throws IOException {
        File theFile = new File(filename);
        String parent = theFile.getParent();
        if (parent != null){
            File p = new File(parent);
            p.mkdirs();
            //System.out.println("Making directory: "+p.getCanonicalPath());
        }
    }

    public static boolean copyFile(String sourceFileName, String destFileName, boolean forceParentDirs) throws IOException {
        if (sourceFileName == null || destFileName == null)
            return false;
        if (sourceFileName.equals(destFileName))
            return false;
        if (forceParentDirs)
            forceParentDirectories(destFileName);
        try{
            java.io.FileInputStream in = new java.io.FileInputStream(sourceFileName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(destFileName);
            try {
                byte[] buf = new byte[31000];
                int read = in.read(buf);
                while (read > -1){
                    out.write(buf, 0, read);
                    read = in.read(buf);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }
        return true;
    }

    public static String readFile(String dir, String relPath){
        File theFile = new File(dir, relPath);
        return readFile(theFile);

    }
    
    public static String readFile(File theFile){
        try {
            FileInputStream fis = new FileInputStream(theFile);
            byte[] theData = new byte[(int) theFile.length()];
            // need to check the number of bytes read here
            int howmany = fis.read(theData);
            if (howmany != theData.length){
                System.out.println("ERROR: Couldn't read all of stream!  filesize: "+theData.length+"  read: "+howmany);
            }
            fis.close();
            return new String(theData);
        } catch (Exception e) {  // can't find the file
            System.out.println("ERROR: "+e);
            return null;
        }
    }

    public static List<String> readFileAsLines(String filePath) {
        List<String> lines = new ArrayList<String>();
        try {
            Path path = Paths.get(filePath);
            lines = Files.readAllLines(path, UTF8_CHARSET);
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
            return null;
        }
        return lines;
    }
    
    public static void writeFileFromLines(String filePath, Iterable<? extends CharSequence> lines) {
        try {
            Path path = Paths.get(filePath);
            Files.write(path, lines, UTF8_CHARSET, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
        }
    }

    public static File saveFile(String dir, String relativeName, String content, boolean forceParentDirs) {
        return saveFile(dir, relativeName, content, forceParentDirs, DEFAULT_ENCODING);
    }
    
    public static File saveFile(String dir, String relativeName, String content, boolean forceParentDirs, String encoding) {
        File result = null;
        PrintWriter writer;
        try{
            if (forceParentDirs) forceParentDirectories(dir+'/'+relativeName);
            result = new File(dir,relativeName);
            if (Tools.notBlank(encoding)) {
                writer = new PrintWriter(result, encoding);
            } else {
                writer = new PrintWriter(result);
            }
        }catch (Exception e){
            System.out.println("Can't write to file in FileTools.saveFile: " + relativeName + " :: " + e);
            return null;
        }
        writer.write(content);
        writer.close();
        return result;
    }

    /** If you have Java 7, you can simply call java.nio.file.Files.createTempDirectory() */
    public static File createTmpDir(String filePrefix){
        String tmpDir = System.getProperty(JAVA_TEMP_DIR_PROPERTY);
		File result = new File(tmpDir, filePrefix + UUID.randomUUID().toString());
		return result;
    }
    
    /**
     * Returns information about the Java temporary directory,
     * including its path and selected access rights of the
     * current code to that directory.
     * 
     * This can potentially be helpful when troubleshooting issues
     * related to code that uses that temporary directory, as per CSPACE-5766.
     * 
     * @return information about the Java temporary directory.
     */
    public static String getJavaTmpDirInfo() {
        StringBuffer strBuf = new StringBuffer("");
        String tmpDirProperty = System.getProperty(JAVA_TEMP_DIR_PROPERTY);
        strBuf.append("\n");
        if (Tools.notBlank(tmpDirProperty)) {
            strBuf.append("Java temporary directory property=");
            strBuf.append(tmpDirProperty);
            strBuf.append("\n");
        } else {
            strBuf.append("Could not get Java temporary directory property");
            strBuf.append("\n");
            return strBuf.toString();
        }
        File tmpDir = new File(tmpDirProperty); // Throws only NPE, if tmpDirProperty is null
        boolean tmpDirExists = false;
        boolean tmpDirIsDirectory = false;
        try {
            tmpDirExists = tmpDir.exists();
            strBuf.append("Temporary directory exists=");
            strBuf.append(tmpDirExists);
            strBuf.append("\n");
            tmpDirIsDirectory = tmpDir.isDirectory();
            strBuf.append("Temporary directory is actually a directory=");
            strBuf.append(tmpDirIsDirectory);
            strBuf.append("\n");           
        } catch (SecurityException se) {
            strBuf.append("Security manager settings prohibit reading temporary directory: ");
            strBuf.append(se.getMessage());
            strBuf.append("\n");
            return strBuf.toString();
        }
        if (tmpDirExists && tmpDirIsDirectory) {
            try {
                boolean tmpDirIsWriteable = tmpDir.canWrite();
                strBuf.append("Temporary directory is writeable by application=");
                strBuf.append(tmpDirIsWriteable);
            } catch (SecurityException se) {
                strBuf.append("Security manager settings prohibit writing to temporary directory: ");
                strBuf.append(se.getMessage());
           }           
        }
        return strBuf.toString();
    }

    static boolean m_fileSystemIsDOS = "\\".equals(File.separator);
    static boolean m_fileSystemIsMac = ":".equals(File.separator);

    public final static String FILE_EXTENSION_SEPARATOR = ".";

    public static boolean fileSystemIsDOS(){return m_fileSystemIsDOS;}
    public static boolean fileSystemIsMac(){return m_fileSystemIsMac;}

    public static String fixFilename(String filename){
        if ( m_fileSystemIsDOS ) {
            return filename.replace('/', '\\');
        }
        if ( m_fileSystemIsMac ) {
            String t = filename.replace('/', ':');
            t = t.replace('\\', ':');
            return t;
        }
        return filename.replace('\\','/');
    }

    public static String join(String dir, String file){
        if ( dir.length() == 0 ) {
            return file;
        }
        dir = fixFilename(dir);
        file = fixFilename(file);
        if ( ! dir.endsWith(File.separator) ) {
            dir += File.separator;
        }
        if ( file.startsWith(File.separator) ) {
            file = file.substring(1);
        }
        return dir + file;
    }

    public static String getFilenameExtension(String filename) {
        int dot = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        return (dot>=0)?filename.substring(dot + 1):null;
    }

    public static String getFilenameBase(String filename) {
        int dot = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        if(dot<0)
            dot = filename.length();
        int sep = filename.lastIndexOf(File.separator); // Note: if -1, then sep+1=0, which is right
        return filename.substring(sep + 1, dot);
    }

    /** @return path without last slash and without filename, returns "" if slash not found (slash is File.separator).*/
    public static String getFilenamePath(String filename) {
        int slash = filename.lastIndexOf(File.separator);
        return (slash>=0)?filename.substring(0, slash):"";
    }

    public static String safeFilename(String relPath){
        return relPath.replaceAll ("[\\/\\\\:\\.\\ ]", "_");
    }


}
