package org.dynamide.restreplay;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dynamide.util.Tools;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    private List<Resource> resourceHistory = new ArrayList<Resource>();

    public static ResourceManager createRootResourceManager(){
        return new ResourceManager();
    }


    //I tested, and the correct form for this is *without* the slash before a relative resourcename on the classpath.
    //InputStream res2 = RestReplay.class.getClassLoader().getResourceAsStream("/restreplay/"+masterFilename);
    //System.out.println("$$$$$$$$ getResource "+"/restreplay/"+masterFilename+":2-->"+res2+"<--");

    public org.dom4j.Document getDocument(String context, String restReplayBaseDir, String relResourcePath) throws DocumentException {
        org.dom4j.Document document;
        Resource resource = new Resource();
        resource.context = context;
        resourceHistory.add(resource);
        InputStream stream = RestReplay.class.getClassLoader().getResourceAsStream("restreplay/" + relResourcePath);  // restreplay/ is on the classpath (in the jar).
        if (stream != null) {
            document = new SAXReader().read(stream);
            resource.provider = Resource.SOURCE.CLASSPATH;
            resource.relPath = relResourcePath;
            resource.foundPath = relResourcePath;
        } else {
            String fullPath = Tools.glue(restReplayBaseDir, "/", relResourcePath);
            resource.relPath = relResourcePath;
            resource.base = restReplayBaseDir;
            File f = new File(fullPath);
            if (!f.exists()) {
                resource.provider = Resource.SOURCE.NOTFOUND;
                System.out.println("ERROR: File does not exist: " + fullPath + ", calculated for: " + relResourcePath);
                return null;
            }
            document = new SAXReader().read(fullPath);
            resource.provider = Resource.SOURCE.FILE;
            resource.foundPath = fullPath;
        }
        resource.document = document;
        return document;
    }

    private String formatSummaryLine(String css, String name, String value){

        return "&nbsp;&nbsp;<span class='"+css+"'>"+name+"</span>&nbsp;"+value;
    }

    protected String formatSummary(){
        StringBuffer b = new StringBuffer();
        b.append("<table class='resource-history'>");
        final String BR = "<br />&nbsp;&nbsp;&nbsp;";
        for (Resource resource: resourceHistory){
            b.append("<tr><td>")
                    .append("<b>"+resource.relPath+"</b>")
                    .append(BR+formatSummaryLine("SMALL", "base:", resource.base))
                    .append(BR+formatSummaryLine("SMALL", "foundPath:",resource.foundPath))
                    .append(BR+formatSummaryLine("SMALL", "source: ", resource.provider.toString()))
                    .append(BR+formatSummaryLine("SMALL", "context:", resource.context))
                    .append(BR+formatSummaryLine("SMALL", "doc:", resource.getDocumentSnippet()))
                    .append("</td></tr>");
        }
        b.append("</table>");
        return b.toString();
    }

}
