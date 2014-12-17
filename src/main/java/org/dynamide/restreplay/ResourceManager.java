package org.dynamide.restreplay;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dynamide.util.FileTools;
import org.dynamide.util.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceManager {

    public static class Resource{
        public enum SOURCE {FILE,CLASSPATH,STREAM,URL,ZIP,NOTFOUND}
        public Document document;
        public String name = "";
        public String relPath = "";
        public String base = "";
        public String foundPath = "";
        public SOURCE provider;
        public String context = "";
        public boolean cached = false;
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
                    +", found:"+foundPath
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

    private final Map<String,String> cache = new LinkedHashMap<String, String>();

    private final Map<String,CachedDoc> docCache = new LinkedHashMap<String, CachedDoc>();

    public static ResourceManager createRootResourceManager(){
        return new ResourceManager();
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

    public String readResource(String context, String relResourcePath, String fullPath) throws IOException {
        boolean markForCache = false;
        String cachedResource = cache.get(relResourcePath);
        //implement a cache that caches on the *second* access.
        if (cachedResource != null  ){
            if (cachedResource.length()==0){
                markForCache = true;
            } else {
                return cachedResource;
            }
        }

        org.dom4j.Document document;
        Resource resource = new Resource();
        resource.context = context;
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
                if (markForCache){
                    resource.cached = true;
                    //System.out.println("====adding to cache===>>>"+relResourcePath);
                    cache.put(relResourcePath, res);  //second time actually cache it.
                } else {
                    cache.put(relResourcePath, "");  //first time is empty string
                }
                return res;
            }
        }
        //System.out.println("======> using File for fullPath: "+fullPath);
        resource.foundPath = fullPath;
        File fullPathFile = new File(fullPath);
        if (fullPathFile.exists()){
            byte[] b = FileUtils.readFileToByteArray(fullPathFile);
            String res = new String(b);
            resource.provider = Resource.SOURCE.FILE;
            if (markForCache) {
                //System.out.println("====adding to cache===>>>"+relResourcePath);
                cache.put(relResourcePath, res);  //second time actually cache it.
                resource.cached = true;
            } else {
                cache.put(relResourcePath, "");  //first time is empty string
            }
            return res;
        } else {
            resource.provider = Resource.SOURCE.NOTFOUND;
            return "";
        }
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
            if (notFound) {
                css = "class='resource-not-found'";
            } else {
               css = resource.cached ? "class='resource-cached'" : "";
            }
            b.append("<tr><td>")
                    .append("<b " + css + ">" + resource.relPath + "</b>")
                    .append(formatSummaryLine("SMALL res-mananger-caption", "base:", resource.base))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "foundPath:",resource.foundPath))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "source: ",
                                (notFound
                                 ?   "<span class='resource-not-found'>"+resource.provider.toString()+"</span>"
                                 :                                       resource.provider.toString()
                                )))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "context:", resource.context))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "doc:", resource.getDocumentSnippet()))
                    .append(formatSummaryLine("SMALL res-mananger-caption", "cached:", resource.cached?"true":""))
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
                    .append(formatSummaryLinePlain("cached:", resource.cached ? "true" : ""));
        }
        return b.toString();
    }

}
