/**
 * This document is a part of the source code and related artifacts
 * for CollectionSpace, an open source collections management system
 * for museums and related institutions:
 *
 * http://www.collectionspace.org
 * http://wiki.collectionspace.org
 *
 * Copyright (c) 2009 Regents of the University of California
 *
 * Licensed under the Educational Community License (ECL), Version 2.0.
 * You may not use this file except in compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at
 * https://source.collectionspace.org/collection-space/LICENSE.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.dynamide.restreplay;

import org.apache.commons.httpclient.Header;
import org.dynamide.util.Tools;
import org.json.JSONObject;
import org.json.XML;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: laramie
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class ServiceResult {
    public static class Alert {
        public Alert(String m, String p, LEVEL l){
            message = m;
            context = p;
            level = l;
        }
        public enum LEVEL {OK, WARN, ERROR}
        public LEVEL level;
        public String message = "";
        public String context = "";
        public static Alert alert(String m, String p, LEVEL l){
            return new Alert(m,p,l);
        }

    }
    public static class AlertError extends Error {
        public AlertError(String msg, Alert triggeringAlert, List<Alert> allAlerts){
            super(msg);
            this.triggeringAlert = triggeringAlert;
            this.allAlerts = allAlerts;
        }
        Alert triggeringAlert;
        List<Alert> allAlerts;
    }

    private List<ServiceResult>childResults = new ArrayList<ServiceResult>();
    public void addChild(ServiceResult child){
        child.setParent(this);
        childResults.add(child);
    }
    public List<ServiceResult> getChildResults(){
        return childResults;
    }

    private ServiceResult parent;
    public ServiceResult getParent(){
        return parent;
    }
    protected void setParent(ServiceResult sr){
        parent = sr;
    }
    public String testID = "test-id-not-set";
    public String testGroupID = "";
    public String testIDLabel = ""; //a place to stash the internal name used by the serviceresult, but shown for info here only.
    public String idFromMutator = ""; //if a mutator turns a test into many tests, each gets a unique mutator id, as a subset of one of the test cases.
    public boolean isMutation = false;
    public String mutatorType = ""; //the value that was in the <mutator></mutator> field, stored here on the parent.
    public ContentMutator mutator = null;
    public String fullURL = "";
    public String deleteURL = "";
    public String AMONG = "A"; //informational: for inspection after the test.
    public String protoHostPort = ""; //informational: for inspection after the test.
    public String location = "";
    public String CSID = "";
    public String subresourceCSID = "";
    public String requestPayloadFilename = "";
    public String requestPayload = "";  //just like requestPayloadRaw, but may have multipart boundary and headers.
    public String requestPayloadsRaw = "";
    public String xmlResult = "";
    public String prettyJSON = "";
    private String result = "";
    public String getResult() {
        return result;
    }
    public void setResultWMime(String result, String mimeType){
        this.result = result;
        setMimeType(mimeType);
        PRETTY_FORMAT format = contentTypeFromResponse();
        if (Tools.notBlank(result) && format.equals(PRETTY_FORMAT.JSON)){
            try {
                this.xmlResult = payloadJSONtoXML(result);

                this.prettyJSON = prettyPrintJSON(result);
            } catch (Exception e){
                addError("trying to format string: -->"+result+"&lt;--", e);
            }
        }
    }
    public long time = 0;
    public int responseCode = 0;
    public String responseMessage = "";
    public String method = "";

    private String error = "";
    public String getError(){
        return error;
    }
    private String errorDetail = "";
    public String getErrorDetail(){
        return errorDetail;
    }
    public void addError(String msg){
        addError(msg, null);
    }
    public void addError(String msg, Throwable t){
        if (null!=t){
            msg += ": "+t.getLocalizedMessage();
            errorDetail += msg + "<br />\r\n"+Tools.getStackTrace(t, 4);
        }
        error = (Tools.notEmpty(error))
                ? error+ "<br />\r\n"+msg
                : msg;
        addAlert(error, testIDLabel, Alert.LEVEL.ERROR);
    }
    public int socketTimeout = 30000;  //millis
    public int connectionTimeout = 30000;   //millis
    public String fromTestID = "";
    public String auth = "";
    public String boundary = "";
    public String payloadStrictness = "";
    public long contentLength = 0;
    private String mimeType = "";
    public String getMimeType(){
        return mimeType;
    }
    public void setMimeType(String mimeHeader){
        //this gets called twice: once in getContentHeader from post, and once in readStream, and readErrorStream.  But so what.
        if (mimeHeader==null) {
            return;
        }
        int start = mimeHeader.indexOf(';');
        if (start>0){
            mimeType = mimeHeader.substring(0, start).trim();
        } else {
            mimeType = mimeHeader.trim();
        }

        //System.out.println("setting mimeType:"+mimeHeader);
    }
    public String failureReason = "";
    public String expectedContentExpanded = "";
    public List<String> requestHeaders = new ArrayList<String>();  //for building report and dump
    public Map<String,String> headerMap = new HashMap<String, String>();  //for doing autodelete, contains x-authorization, etc.
    public Header[] responseHeaders = new Header[0];
    public String responseHeadersDump = "";//This is filled in by RestReplayTransport, because there are two types: HttpUrlConnection and the Apache style, so objects are not generic.  This stashes the string result from RestReplayTransport.
    public List<Integer> expectedCodes = new ArrayList<Integer>();
    public List<Alert> alerts = new ArrayList<Alert>();

    private Map<String,String> vars = new HashMap<String,String>();
    public Map<String,String> getVars(){
        return vars;
    }
    public void addVars(Map<String,String> newVars){
        vars.putAll(newVars);
    }

    private Map<String,String> exports = new HashMap<String,String>();
    public Map<String, String> getExports() {
        return exports;
    }
    public void addExports(Map<String,String> newexports){
        exports.putAll(newexports);
    }



    private Map<String, TreeWalkResults> partSummaries = new HashMap<String, TreeWalkResults>();
    public void addPartSummary(String label, TreeWalkResults list){
        partSummaries.put(label, list);
    }
    public String partsSummary(boolean detailed){
        StringBuffer buf = new StringBuffer();
        if (!isDomWalkOK()){
            if (detailed) buf.append("\r\nDOM CHECK FAILED:\r\n");
            else buf.append("; DOM CHECK FAILED:");
        }
        for (Map.Entry<String,TreeWalkResults> entry : partSummaries.entrySet()) {
            String key = entry.getKey();
            TreeWalkResults value = entry.getValue();
            buf.append(" label:"+key+": ");
            if (detailed){
                buf.append("\r\n");
                buf.append(value.fullSummary());
            } else {
                buf.append(value.miniSummary());
            }

        }
        return buf.toString();
    }
    public boolean codeInSuccessRange(int code){
        if (200<=code && code<400){
            return true;
        }
        return false;
    }

    public boolean isDomWalkOK(){
        if (Tools.isEmpty(payloadStrictness)){
            return true;
        }
        PAYLOAD_STRICTNESS strictness = PAYLOAD_STRICTNESS.valueOf(payloadStrictness);
        for (Map.Entry<String,TreeWalkResults> entry : partSummaries.entrySet()) {
            String key = entry.getKey();
            TreeWalkResults value = entry.getValue();
            if (value.hasDocErrors()){
                failureReason = " : DOM DOC_ERROR; ";
                return false;
            }
            switch (strictness){
            case STRICT:
                if (!value.isStrictMatch()) {
                    failureReason = " : DOM NOT STRICT; ";
                    return false;
                }
                break;
            case ADDOK:
                if (value.countFor(TreeWalkResults.TreeWalkEntry.STATUS.TEXT_DIFFERENT)>0) {
                    failureReason = " : DOM TEXT_DIFFERENT; ";
                    return false;
                }
                if (value.countFor(TreeWalkResults.TreeWalkEntry.STATUS.R_MISSING)>0){
                    failureReason = " : DOM R_MISSING; ";
                    return false;
                }
                break;
            case TEXT:
                if (value.countFor(TreeWalkResults.TreeWalkEntry.STATUS.TEXT_DIFFERENT)>0) {
                    failureReason = " : DOM TEXT_DIFFERENT; ";
                    return false;
                }
                break;
            case TREE:
                if (!value.treesMatch()) {
                    failureReason = " : DOM TREE MISMATCH; ";
                    return false;
                }
                break;
            case TREE_TEXT:
                if (value.countFor(TreeWalkResults.TreeWalkEntry.STATUS.TEXT_DIFFERENT)>0) {
                    failureReason = " : DOM TEXT_DIFFERENT; ";
                    return false;
                }
                if (!value.treesMatch()) {
                    failureReason = " : DOM TREE MISMATCH; ";
                    return false;
                }
                break;
            case ZERO:
                break;
            }
        }
        return true;
    }

    private boolean overrideExpectedResult = false;

    /** Call this method to create a ServiceResult mock object, for when you are doing autoDelete, and you come
     *  across a GET : GETs don't have a DELETE url, so they don't need to be autoDeleted, so an empty ServiceResult object
     *  signifies this.
     */
    public void overrideGotExpectedResult(){
        overrideExpectedResult = true;
    }

    public String gotExpectedResultBecause = "";

    public boolean gotExpectedResult(){
        if (overrideExpectedResult){
            return true;
        }
        //if (Tools.notEmpty(failureReason)){
        //    return false;
        //}

        List<Integer>  expectedCodesFrom = expectedCodes;

        gotExpectedResultBecause = " test["+testID+']';
        if (null!=mutator && Tools.notEmpty(idFromMutator)){
            //System.out.println("\r\n\r\n>>>>>>>>>> id: "+testID+" mutationID:"+idFromMutator);
            boolean hasRange = mutator.hasRangeForId(idFromMutator);
            if(hasRange) {
                gotExpectedResultBecause = " mutator["+idFromMutator+']';
                boolean found = (mutator.valueInRangeForId(responseCode, idFromMutator));
                //System.out.println(" found("+idFromMutator+"): "+found);
                return found;
            } else {
                //System.out.println(" NOT found("+idFromMutator+") ");
            }
            if (this.getParent() != null){
                ServiceResult p = this.getParent();
                expectedCodesFrom = p.expectedCodes;
                gotExpectedResultBecause = " parent["+p.testID+']';
            }
        }

        for (Integer oneExpected : expectedCodesFrom){
            if (responseCode == oneExpected){
                failureReason = "";
                return isDomWalkOK();
            }
        }
        if ( expectedCodesFrom.size()>0 && codeInSuccessRange(responseCode)){ //none found, but result expected.
            for (Integer oneExpected : expectedCodesFrom){
                if ( ! codeInSuccessRange(oneExpected)){
                    failureReason = "";
                    return isDomWalkOK();
                }
            }
        }
        boolean ok = codeInSuccessRange(responseCode);
        if (ok) {
            failureReason = "";
            return isDomWalkOK();
        }
        failureReason = " : STATUS CODE UNEXPECTED; ";
        return false;
    }

    public void addRequestHeader(String name, String value){
        requestHeaders.add(name+':'+value);
    }

    //public static final String[] DUMP_OPTIONS = {"minimal", "detailed", "full"};
    public static enum DUMP_OPTIONS {minimal, detailed, full, auto};

    public static enum PAYLOAD_STRICTNESS {ZERO, ADDOK, TREE, TEXT, TREE_TEXT, STRICT};

    public String toString(){
        return detail(true);

    }

    private static final String LINE = "\r\n==================================";
    private static final String CRLF = "\r\n";

    public String detail(boolean includePayloads){
        String res =  "{"
                + ( gotExpectedResult() ? "SUCCESS" : "FAILURE"  )
                + failureReason
                +"; "+method
                +"; "+responseCode
                + ( (expectedCodes.size()>0) ? "; expectedCodes:"+expectedCodes : "" )
                + ( Tools.notEmpty(testID) ? "; testID:"+testID : "" )
                + ( Tools.notEmpty(testGroupID) ? "; testGroupID:"+testGroupID : "" )
                + ( Tools.notEmpty(fromTestID) ? "; fromTestID:"+fromTestID : "" )
                + ( Tools.notEmpty(responseMessage) ? "; msg:"+responseMessage : "" )
                +"; URL:"+fullURL
                +"; auth: "+auth
                + ( Tools.notEmpty(deleteURL) ? "; deleteURL:"+deleteURL : "" )
                + ( Tools.notEmpty(location) ? "; location.CSID:"+location : "" )
                + ( Tools.notEmpty(error) ? "; ERROR_IN_DETAIL:"+error : "" )
                + "; gotExpected:"+gotExpectedResult()
                //+";result:"+result+";"
                + ( partsSummary(true))
                +"}"
                + ( includePayloads && Tools.notBlank(requestPayload) ? LINE+"requestPayload:"+LINE+CRLF+requestPayload+LINE : "" )
                + ( includePayloads && Tools.notBlank(result) ? LINE+"result:"+LINE+CRLF+result : "" );
        return res;
    }

    public String minimal(){
        return minimal(false);
    }

    public String minimal(boolean verbosePartsSummary){
        return "{"
                + ( gotExpectedResult() ? "SUCCESS" : "FAILURE"  )
                + failureReason
                + ( Tools.notEmpty(testID) ? "; "+testID : "" )
                +"; "+method
                +"; "+responseCode
                + (expectedCodes.size()>0 ? "; expected:"+expectedCodes : "")
                + ( Tools.notEmpty(responseMessage) ? "; msg:"+responseMessage : "" )
                +"; URL:"+fullURL
                //for auth, see detail()   +"; auth: "+auth
                + ( Tools.notEmpty(error) ? "; ERROR_IN_PART:"+error : "" )
                + (verbosePartsSummary ? partsSummary(true) : partsSummary(false) )
                +"}";
    }
    public String dump(ServiceResult.DUMP_OPTIONS opt, boolean hasError){
        switch (opt){
            case minimal:
                return minimal(false);
            case detailed:
                return detail(false);
            case full:
                return detail(true);
            case auto:
                return minimal(hasError);
            default:
                return toString();
        }
    }

    /** This method may be called from a test case, using a syntax like ${testID3.resValue("persons_common", "//refName")}   */
    public String got(String xpath) throws Exception {
        //PayloadLogger.HttpTraffic traffic = PayloadLogger.readPayloads(this.result, this.boundary, this.contentLength);
        //PayloadLogger.Part partFromServer = traffic.getPart(partName);
        //String source = partFromServer.getContent();
        try {
            String source;

            if (Tools.notBlank(this.xmlResult)) {
                source = this.xmlResult;
            } else {
                source = this.result;
            }
            if (Tools.isBlank(source)) {
                return "";
            }
            org.jdom.Element element = (org.jdom.Element) XmlCompareJdom.selectSingleNode(source, xpath, null);  //todo: passing null for namespace may not work.
            String sr = element != null ? element.getText() : "";
            return sr;
        } catch (Throwable t){
            addAlert("ERROR reading response value: " + t,
                    "xpath:"+xpath,
                    Alert.LEVEL.ERROR);
            return "";
        }
    }

    /** This method may be called from a test case, using a syntax like ${oe9.reqValue("personauthorities_common","//shortIdentifier")}    */
    public String sent(String xpath) throws Exception {
        String source = this.requestPayload; // REM - 5/9/2012 : Changing to requestPayload from requestPayloadsRaw to get actual sent payload
        if (source == null){
            return "ERROR:null:requestPayloadsRaw";
        }
        org.jdom.Element element = (org.jdom.Element) XmlCompareJdom.selectSingleNode(source, xpath, null);   //e.g. "//shortIdentifier");  //todo: passing null for namespace may not work.
        String sr = element != null ? element.getText() : "";
        return sr;
    }

    //TODO: add a way to get at headers in Headers[] either in a separate method or in get().

    //makes these accessible by Jexl. We could also just add getters to each of these.  Jexl will case-insensitively read related gettters.

    public Object get(String what){
        if ("CSID".equals(what)){
            return CSID;
        } else if ("location".equalsIgnoreCase(what)){
            return location;
        } else if ("testID".equalsIgnoreCase(what)){
            return testID;
        } else if ("mutation".equalsIgnoreCase(what)){
            return idFromMutator;
        } else if ("testGroupID".equalsIgnoreCase(what)){
            return testGroupID;
        } else if ("fullURL".equalsIgnoreCase(what)){
            return fullURL;
        } else if ("url".equalsIgnoreCase(what)){
            try {
                return new URL(fullURL);
            } catch (MalformedURLException e) {
                return fullURL;
            }
        } else if ("deleteURL".equalsIgnoreCase(what)){
            return deleteURL;
        } else if ("protoHostPort".equalsIgnoreCase(what)){
            return protoHostPort;
        } else if ("responseCode".equalsIgnoreCase(what)){
            return ""+responseCode;
        } else if ("method".equalsIgnoreCase(what)){
            return method;
        }
        if (vars.containsKey(what)){
            return vars.get(what);
        }
        return "";
    }

    public static String prettyPrintJSON(String payload) {
        JSONObject json = new JSONObject(payload);
        String xml = "<root>" + XML.toString(json) + "</root>";
        return json.toString(4);
    }

    public static String payloadJSONtoXML(String payload) {
        JSONObject json = new JSONObject(payload);
        String xml = "<root>"+XML.toString(json)+"</root>";
        //System.out.println("JSON: "+json.toString(4));
        //System.out.println("PAYLOAD raw:" + payload);
        //System.out.println("PAYLOAD xml:"+xml);
        return xml;
    }

    public enum PRETTY_FORMAT {XML, JSON, NONE}

    public PRETTY_FORMAT contentTypeFromResponse(){
        String uc = "";
        if (Tools.notBlank(this.mimeType)){
            uc = this.mimeType.toUpperCase();
            if (uc.endsWith("/JSON")){
                return PRETTY_FORMAT.JSON;
            } else if (uc.endsWith("/XML") || uc.endsWith("/XHTML") || uc.endsWith("/HTML")){
                return PRETTY_FORMAT.XML;
            };
        } else if (Tools.notBlank(this.requestPayloadFilename)){
            uc = this.requestPayloadFilename.toUpperCase();
            if (uc.endsWith(".JSON")){
                return PRETTY_FORMAT.JSON;
            } else if (uc.endsWith(".XML") || uc.endsWith(".XHTML") || uc.endsWith(".HTML")){
                return PRETTY_FORMAT.XML;
            };
        } else {
            return PRETTY_FORMAT.NONE;
        }
        return PRETTY_FORMAT.NONE;
    }

    public void addAlert(String message, String context, Alert.LEVEL level){
        alerts.add(new Alert(message, context, level));
    }

}
