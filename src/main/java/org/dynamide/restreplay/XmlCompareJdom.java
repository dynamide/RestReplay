package org.dynamide.restreplay;

import org.dynamide.util.Tools;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;


import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dynamide.restreplay.TreeWalkResults.TreeWalkEntry;
import org.jdom.output.XMLOutputter;

public class XmlCompareJdom {

private static final String DEFAULT_SAX_DRIVER_CLASS = "org.apache.xerces.parsers.SAXParser";

    public static org.jdom.Document getDocumentFromContent(String source) throws IOException, JDOMException {
        org.jdom.Document doc;
        SAXBuilder builder;
        builder = new SAXBuilder();
        builder.setValidation(false); //has no effect, I think.
        doc = builder.build(new StringReader(source));
        return doc;
    }

    public static TreeWalkResults compareParts(String expectedContent, String leftID, String actualPartContent, String rightID, String startElement, TreeWalkResults.MatchSpec matchSpec){
        TreeWalkResults list = new TreeWalkResults();
        try {

            list.leftID = leftID;
            list.rightID = rightID;
            TreeWalkResults.TreeWalkEntry infoentry = new TreeWalkResults.TreeWalkEntry();
            infoentry.expected = expectedContent;
            infoentry.actual = actualPartContent;
            infoentry.status = TreeWalkResults.TreeWalkEntry.STATUS.INFO;
            infoentry.message = "\r\n    LEFT file: "+leftID+"\r\n    RIGHT file: "+rightID;
            list.add(infoentry);
            if (Tools.isEmpty(expectedContent)){
                TreeWalkEntry entry = new TreeWalkEntry();
                entry.status = TreeWalkEntry.STATUS.DOC_ERROR;
                entry.errmessage = "L dom was empty.";
                list.add(entry);
            } else if (Tools.isEmpty(actualPartContent)){
                TreeWalkEntry entry = new TreeWalkEntry();
                entry.errmessage = "R dom was empty.";
                entry.status = TreeWalkEntry.STATUS.DOC_ERROR;
                list.add(entry);
            } else {
                Document expected = getDocumentFromContent(expectedContent);
                Document actual = getDocumentFromContent(actualPartContent);
                treeWalk(expected, actual, list, startElement, matchSpec);
            }
        } catch (Throwable t){
            String msg = "ERROR in RestReplay.compareParts(): "+t;
            System.out.println(msg);
            TreeWalkEntry entry = new TreeWalkEntry();
                entry.status = TreeWalkEntry.STATUS.DOC_ERROR;
                entry.errmessage = msg;
                list.add(entry);
        }
        return list;
    }

    public static List select(Element element, String xpathExpression, Namespace namespace) throws Exception {
        XPath xpath = new JDOMXPath(xpathExpression);
        String prefix = namespace.getPrefix();
        String uri = namespace.getURI();
        xpath.addNamespace(prefix, uri);
        return xpath.selectNodes(element);
    }

    public static Object selectSingleNode(Element element, String xpathExpression, Namespace namespace) throws Exception {
        XPath xpath = new JDOMXPath(xpathExpression);
        if (namespace != null) {
            String prefix = namespace.getPrefix();
            String uri = namespace.getURI();
            xpath.addNamespace(prefix, uri);
        }
        return xpath.selectSingleNode(element);
    }

    public static Object selectSingleNode(String docSource, String xpathExpression, Namespace namespace) throws Exception {
        Document doc = getDocumentFromContent(docSource);
        Element element = doc.getRootElement();
        XPath xpath = new JDOMXPath(xpathExpression);
        if (namespace != null) {
            String prefix = namespace.getPrefix();
            String uri = namespace.getURI();
            xpath.addNamespace(prefix, uri);
        }
        return xpath.selectSingleNode(element);
    }

    /*   MAYBE DEAL WITH NAMESPACES IN THIS KIND OF APPROACH.

    for (Element el : doc.getRootElement().getDescendants(new ElementFilter())) {
    if (el.getNamespace() != null) el.setNamespace(null);

    xpath.addNamespace("x", d.getRootElement().getNamespaceUri());
    */
    public static boolean treeWalk(Document left, Document right, TreeWalkResults list, String startElement, TreeWalkResults.MatchSpec matchSpec) throws Exception {
        Element leftElement = left.getRootElement();
        Element rightElement = right.getRootElement();
        if (Tools.notBlank(startElement)) {
            XPath xpath = new JDOMXPath(startElement);
            Object test = xpath.selectSingleNode(leftElement);
            if (test!=null){
                leftElement = (Element)test;
            }
            Object rtest = xpath.selectSingleNode(rightElement);
            if (rtest!=null){
                rightElement = (Element)rtest;
            }
        }
        boolean res = treeWalk(leftElement, rightElement, "/", list, matchSpec);
        return res;
    }

    public static boolean treeWalk(Element left, Element right, String parentPath, TreeWalkResults msgList, TreeWalkResults.MatchSpec matchSpec) throws Exception {
        String SPACE = "     ";
        if (left == null && right == null){
            return true;
        }
        if (left == null){
            return false;
        }
        if (right == null){
            return false;
        }
        List l = left.getChildren();
        Map foundRightMap = new LinkedHashMap();
        List<String> foundRepeatingList = new ArrayList<String>();
        boolean result = true;
        for (Object o : l) {
            if (!(o instanceof Element)){
                continue;
            }
            Element leftChild = (Element)o;
            //String leftChildName = leftChild.getName();
            String leftChildName = leftChild.getQualifiedName();
            if (Tools.isEmpty(leftChildName)){
                continue;
            }

            Namespace namespace =  leftChild.getNamespace();

            String leftChildPath = Tools.glue(parentPath, "/", leftChildName);

            if (foundRepeatingList.indexOf(leftChildPath)>=0){
                continue;
            }
            List leftlist = select(left, leftChildName, namespace);
            if (leftlist != null && leftlist.size() > 1){
                //System.out.println("-----------------doRepeating------"+leftChildPath);
                foundRepeatingList.add(leftChildPath);
                boolean repeatingIdentical =
                    doRepeatingFieldComparison(leftlist, leftChildPath, leftChildName, left, right, msgList, namespace, matchSpec) ; //todo: deal with foundRightMap in this repeating field block.
                if ( ! repeatingIdentical ){
                    //System.out.println("\r\n\r\n\r\n*****************************\r\nOne repeating field failed: "+msgList);
                    return false;
                }
                foundRightMap.put(leftChildName, "OK");
            } else {
                Element rightChild  = (Element)selectSingleNode(right,leftChildName, namespace);
                if (rightChild == null){
                    TreeWalkEntry entry = new TreeWalkEntry();
                    entry.lpath = leftChildPath;                  //this works, but is questionable: selectSingleNode(right, "//*[local-name() = \"objectexit_common\"]")
                    entry.status = TreeWalkEntry.STATUS.MISSING;
                    msgList.add(entry);
                    continue;
                }
                foundRightMap.put(leftChildName, "OK");
                String leftChildTextTrim = leftChild.getText().trim();
                String rightChildTextTrim = rightChild.getText().trim();
                TreeWalkEntry entry = new TreeWalkEntry();
                entry.ltextTrimmed = leftChildTextTrim;
                entry.rtextTrimmed = rightChildTextTrim;
                entry.lpath = leftChildPath;
                entry.rpath = leftChildPath; //same

                if (leftChildTextTrim.equals(rightChildTextTrim)){
                    entry.status = TreeWalkEntry.STATUS.MATCHED;
                    msgList.add(entry);
                } else {
                    entry.status = TreeWalkEntry.STATUS.TEXT_DIFFERENT;
                    msgList.add(entry);
                }
                //============ DIVE !! =====================================================
                result = result && treeWalk( leftChild, rightChild, leftChildPath, msgList, matchSpec);
            }
        }
        for (Object r : right.getChildren()){
            if (!(r instanceof Element)){
                continue;
            }
            Element rightChild = (Element)r;
            String rname = rightChild.getQualifiedName();
            if (null==foundRightMap.get(rname)){
                String rightChildPath = Tools.glue(parentPath, "/", rname);

                TreeWalkEntry entry = new TreeWalkEntry();
                entry.rpath = rightChildPath;
                entry.status = TreeWalkEntry.STATUS.ADDED;
                msgList.add(entry);
            }
        }
        return true;
    }

    private static void dumpXML_OUT(Element el) throws Exception {
        XMLOutputter outputter = new XMLOutputter();
        outputter.output(el, System.out);
    }
    private static String dumpXML(Element el) throws Exception {
        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(el);
    }

    public static boolean doRepeatingFieldComparison(List leftList,
                                                                                     String leftChildPath,
                                                                                     String leftChildName,
                                                                                     Element left,
                                                                                     Element right,
                                                                                     TreeWalkResults msgList,
                                                                                     Namespace namespace,
                                                                                     TreeWalkResults.MatchSpec matchSpec)
    throws Exception {
        //todo: deal with foundRightMap in this repeating field block.
        List rightList = select(right, leftChildName, namespace);
        if (rightList == null || rightList.size() == 0 || rightList.size() < leftList.size()){
            TreeWalkEntry twe = new TreeWalkEntry();
            twe.lpath = leftChildPath;
            twe.status = TreeWalkEntry.STATUS.MISSING;
            String rmsg = (rightList == null)
                    ? " Right: 0"
                    : " Right: "+rightList.size();
            twe.message = "Repeating field count not matched. Field: "+leftChildPath+" Left: "+leftList.size()+rmsg;
            msgList.add(twe);
            return false;
        }
        if (rightList.size() > leftList.size()){
            TreeWalkEntry twe = new TreeWalkEntry();
            twe.lpath = leftChildPath;
            twe.status = TreeWalkEntry.STATUS.ADDED;
            twe.message = "Repeating field count not matched. Field: "+leftChildPath+" Left: "+leftList.size()+" Right: "+rightList.size();
            msgList.add(twe);
            //LC 20110429 return false;
        }

        for (Object le : leftList){
            boolean found = false;
            Element leftEl = (Element)le;
            //pl("left", leftEl);
            for(Object re : rightList){
                Element rightEl = (Element)re;
                //pl("right", rightEl);
                TreeWalkResults msgListInner = new TreeWalkResults();
                //========== DIVE !!! =======================
                treeWalk(leftEl, rightEl, leftChildPath, msgListInner, matchSpec);
                //========================================

                if (msgListInner.treesMatch(matchSpec)){   //if (msgListInner.isStrictMatch()){
                    found = true;
                    TreeWalkEntry twe = new TreeWalkEntry();
                    twe.lpath = leftChildPath;
                    twe.status = TreeWalkEntry.STATUS.MATCHED;
                    msgList.add(twe);
                    rightList.remove(re); //found it, don't need to inspect this element again.  Since we are breaking from loop, removing element won't mess up iterator--we get a new one on the next loop.
                    break;
                } else {
                    TreeWalkEntry twe = new TreeWalkEntry();
                    twe.lpath = leftChildPath;
                    twe.status = TreeWalkEntry.STATUS.NESTED_ERROR;
                    twe.nested = msgListInner;
                    msgList.add(twe);
                    //String line = "\r\n\r\n*********************************\r\n";
                    //System.out.println(line+"TreeWalkResults: from walking rightEl: "+rightEl+" leftEl: "+leftEl + " msgListInner:"+ msgListInner+line);
                }
            }  // END for(rightList)
            if ( ! found){
                TreeWalkEntry twe = new TreeWalkEntry();
                twe.lpath = leftChildPath;
                twe.status = TreeWalkEntry.STATUS.MISSING;
                twe.message = "Repeating field not matched. Source: {"+dumpXML(leftEl)+"}";
                msgList.add(twe);
                return false;
            }
        }  // END for(leftLlist)
        return true;
    }

    private static void pl(String name, Element el) throws Exception {
        Namespace namespace = el.getNamespace();
        Object lobid = selectSingleNode(el, "@ID", namespace);
        String lid = "";
        if (lobid!=null){
            lid = lobid.toString();
        }

        System.out.println(name+": "+lid);
        dumpXML_OUT(el);
        System.out.println();

    }

    public static void main(String[]args) throws Exception {
        String s = "<root><status>success</status><data>1.0|idm|idm|piid=ffffffff540e662de4b01ef1c4c50faf&amp;sessid=401d6f4ee6bb4795af0861ebaa1f6c58|2014-11-05T01:01:16+00:00|2014-11-05T04:01:16+00:00|cbc0b7e17f07a90f70a88a0eab57d9ce</data></root>";
        Object o = selectSingleNode(s, "//data", null);
        //Object o = selectSingleNode("<root><result>OK</result><nested><courseId>course1234</courseId></nested><method>GET</method></root>", "//courseId", null);
    }
}
