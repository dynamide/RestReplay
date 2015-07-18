package org.dynamide.restreplay;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dynamide.util.FileTools;
import org.dynamide.util.Tools;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ResourceManager {

    public static class Resource{
        public enum SOURCE {FILE,CLASSPATH,STREAM,URL,ZIP,NOTFOUND}
        public Document document;
        public String contents = "";
        public String name = "";
        public String relPath = "";
        public String base = "";
        public String foundPath = "";
        public SOURCE provider;
        public String context = "";
        public boolean cached = false;
        public boolean relative = true;
        public String getDocumentSnippet(){
            String docstring = "";
            if (document!=null){
                String name = document.getName();
                if (name != null){
                    docstring = name;
                } else {
                    Node root = document.getRootElement();
                    if (root!=null){
                        docstring = "&lt;"+root.getName()+"...";
                    } else {
                        docstring = document.getClass().getSimpleName();
                    }
                }
            }
            return docstring;
        }
        public String toString(){
            return "Resource: {context:"+context
                    +", doc:"+getDocumentSnippet()
                    +", name:"+name
                    +", path:"+relPath
                    +", base:"+base
                    +", fullpath:"+foundPath
                    +", provider:"+provider.toString()
                    +"}";
        }
    }

    private static final class CachedDoc {
        public CachedDoc(Document document){
            this.document = document;
        }
        public Document document;
    }

    private final List<Resource> resourceHistory = new ArrayList<Resource>();

    private final Map<String,Resource> cache = new LinkedHashMap<String, Resource>();

    private final Map<String,CachedDoc> docCache = new LinkedHashMap<String, CachedDoc>();

    public static ResourceManager createRootResourceManager(){
        return new ResourceManager();
    }

    private ConfigFile provider;
    public void setTestContextProvider(ConfigFile provider){
        this.provider = provider;
    }
    protected String getContextProviderContext(){
        if (this.provider != null){
            return provider.getCurrentTestIDLabel();
        }
        return "";
    }

    public static String getRestReplayVersion() {
        Properties properties = readPropertiesFromClasspath();
        Object propObj = properties.get("application.version");
        if (propObj != null) {
            return propObj.toString();
        }
        return "";
    }

    //I tested, and the correct form for this is *without* the slash before a relative resourcename on the classpath.
    //InputStream res2 = RestReplay.class.getClassLoader().getResourceAsStream("/restreplay/"+masterFilename);
    //System.out.println("$$$$$$$$ getResource "+"/restreplay/"+masterFilename+":2-->"+res2+"<--");

    public org.dom4j.Document getDocument(String context, String testdir, String relResourcePath) throws DocumentException {
        boolean markForCache = false;
        CachedDoc cachedDoc = docCache.get(relResourcePath);

        //implement a cache that caches on the *second* access.
        if (cachedDoc != null && cachedDoc.document != null  ){
            if ( cachedDoc.document == null){
                markForCache = true;
            } else {
                return cachedDoc.document;
            }
        }

        org.dom4j.Document document;
        Resource resource = new Resource();
        resource.context = context;
        resource.base = testdir;
        resource.relPath = relResourcePath;
        resourceHistory.add(resource);
        String fullPath = Tools.glue(testdir, "/", relResourcePath);
        if (new File(fullPath).exists()) {
            document = new SAXReader().read(fullPath);
            resource.provider = Resource.SOURCE.FILE;
            resource.foundPath = fullPath;
        } else {
            InputStream stream = RestReplay.class.getClassLoader().getResourceAsStream("restreplay/" + relResourcePath);  // restreplay/ is on the classpath (in the jar).
            if (stream != null) {
                document = new SAXReader().read(stream);
                resource.provider = Resource.SOURCE.CLASSPATH;
                resource.relPath = relResourcePath;
                resource.foundPath = relResourcePath;
            } else {
                resource.provider = Resource.SOURCE.NOTFOUND;
                //System.out.println("ERROR: File does not exist: " + fullPath + ", calculated for: " + relResourcePath);
                return null;
            }
        }
        resource.document = document;

        if (markForCache){
            //System.out.println("====adding to docCache===>>>"+relResourcePath);
            docCache.put(relResourcePath, new CachedDoc(document));  //second time actually cache it.
            resource.cached = true;
        } else {
            docCache.put(relResourcePath, new CachedDoc(null));  //first time is empty string
        }

        return document;
    }

    protected static final Resource CACHE_NEXT_TIME = new Resource();

    /** Internally, this implements a write-on-second-access cache, because we end up reading many small
     *  files once, and a few files many times.
     * @param context
     * @param relResourcePath
     * @param fullPath
     * @return
     * @throws IOException
     */
    public Resource readResource(String context, String relResourcePath, String fullPath) throws IOException {
        //System.out.println("===> resource: "+relResourcePath
        //              +"\r\n     fullPath: "+fullPath);
        boolean markForCache = false;
        Resource cachedResource = cache.get(relResourcePath);
        //implement a cache that caches on the *second* access.
        if (cachedResource != null){
            if (cachedResource != CACHE_NEXT_TIME){
                return cachedResource;
            } else {
                markForCache = true;
            }
        }

        org.dom4j.Document document;
        Resource resource = new Resource();
        resource.context = context+" ["+this.getContextProviderContext()+"]";
        //resource.base = testdir;
        resource.relPath = relResourcePath;
        resourceHistory.add(resource);


        if (Tools.notBlank(relResourcePath)) {
            InputStream stream = RestReplay.class.getClassLoader().getResourceAsStream("restreplay/" + relResourcePath);  // restreplay/ is on the classpath (in the jar).
            if (stream != null) {
                //System.out.println("======> found stream for relResourcePath: -->" + relResourcePath + "<--, not fullPath:-->" + fullPath + "<--");
                String res = FileTools.convertStreamToString(stream);
                //System.out.println("======> stream starts with:" + res.substring(0, Math.min(res.length(), 100)));
                resource.provider = Resource.SOURCE.CLASSPATH;
                resource.relPath = relResourcePath;
                resource.foundPath = relResourcePath;
                resource.contents = res;
                if (markForCache){
                    resource.cached = true;
                    //System.out.println("====adding to cache===>>>"+relResourcePath);
                    cache.put(relResourcePath, resource);  //second time actually cache it.
                } else {
                    cache.put(relResourcePath, CACHE_NEXT_TIME);  //first time is empty string
                }
                return resource;
            }
        }
        //System.out.println("======> using File for fullPath: "+fullPath);
        File fullPathFile = new File(fullPath);
        File relResourcePathFile = new File(relResourcePath);
        File theFile = null;
        if (relResourcePathFile.exists()) {
            theFile = relResourcePathFile;
            resource.foundPath = relResourcePath;
            resource.relative = false;  //We found it using the relative path, which turned out to be a full path.
        } else if (fullPathFile.exists()) {
            theFile = fullPathFile;
            resource.foundPath = fullPath;
            resource.relative = true;  //relPath not found, so fullPath has the test dir already glued on ==> relative.
        }

        if (theFile!=null && theFile.exists()){
            byte[] b = FileUtils.readFileToByteArray(theFile);
            String res = new String(b);
            // posix or somesuch says all text files must end in \n.
            // This hoses POSTs because the \n ends up glued to the last parameter in form encoded fields.
            // Delete newline at end of file for \n, \r\n, and \r. (unix, windows, mac)
            if (res.endsWith("\n")){
                res = res.substring(0, res.length()-1);
            }
            if (res.endsWith("\r")){
                res = res.substring(0, res.length()-1);
            }
            //this works if you want to use NIO, but still reads in the last \n of course:
            // Path path = Paths.get(theFile.getAbsolutePath().toString());
            // byte[] b = Files.readAllBytes(path);
            resource.provider = Resource.SOURCE.FILE;
            resource.contents = res;
            if (markForCache) {
                cache.put(relResourcePath, resource);  //second time actually cache it.
                resource.cached = true;
            } else {
                cache.put(relResourcePath, CACHE_NEXT_TIME);  //first time is empty string
            }
            return resource;
        } else {
            resource.provider = Resource.SOURCE.NOTFOUND;
            return resource;
        }
    }

    public static Properties readPropertiesFromClasspath() {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("application.properties");
        if (stream!=null){
            try {
                prop.load(stream);
            } catch (IOException e){
                System.out.println("ERROR reading properties for application.properties :"+e);
            }
        }
        return prop;
    }

    private static final String formatSummaryLine(String css, String name, String value){
        if (Tools.isBlank(value)){
            return "";
        }
        return "<br />&nbsp;&nbsp;<span class='"+css+"'>"+name+"</span>&nbsp;"+value;
    }

    private static final String formatSummaryLinePlain(String name, String value){
        if (Tools.isBlank(value)){
            return "";
        }
        return "\r\n  "+name+" "+value;
    }

    public String formatSummary(){
        StringBuffer b = new StringBuffer();
        b.append("<table class='resource-history'>");
        for (Resource resource: resourceHistory){
            boolean notFound = resource.provider.equals(Resource.SOURCE.NOTFOUND);
            String css;
            List<String> classes = new ArrayList<String>();

            if (notFound){
                classes.add("resource-not-found");
            }
            if (resource.cached){
                classes.add("resource-cached");
            }
            if (!resource.relative){
                classes.add("resource-fullpath");
            }

            b.append("<tr><td>")
                    .append("<b class='" + Tools.join(" ",classes) + "'>" + resource.relPath + "</b>")
                    .append(formatSummaryLine("SMALL res-mananger-caption", "base:", resource.base))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "foundPath:",resource.foundPath))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "source: ",
                                (notFound
                                 ?   "<span class='resource-not-found'>"+resource.provider.toString()+"</span>"
                                 :                                       resource.provider.toString()
                                )))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "context:", resource.context))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "doc:", resource.getDocumentSnippet()))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "cached:", resource.cached?"<span class='resource-cached'>true</span>":""))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "fullpath:", (!resource.relative)?"<span class='resource-fullpath'>true</span>":""))
                    .append("</td></tr>");
        }
        b.append("</table>");
        return b.toString();
    }

    public String formatSummaryPlain(){
        StringBuffer b = new StringBuffer();
        b.append("ResourceManager History");
        final String BR = "\r\n";
        for (Resource resource: resourceHistory){
            b.append(BR)
                    .append(resource.relPath)
                    .append(formatSummaryLinePlain("base:", resource.base))
                    .append(formatSummaryLinePlain("foundPath:",resource.foundPath))
                    .append(formatSummaryLinePlain("source:", resource.provider.toString()))
                    .append(formatSummaryLinePlain("context:", resource.context))
                    .append(formatSummaryLinePlain("doc:", resource.getDocumentSnippet()))
                    .append(formatSummaryLinePlain("cached:", resource.cached ? "true" : ""))
                    .append(formatSummaryLinePlain("fullpath:", (!resource.relative) ? "true" : ""));
        }
        return b.toString();
    }

}
