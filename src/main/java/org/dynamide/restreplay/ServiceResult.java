package org.dynamide.restreplay;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.httpclient.Header;
import org.dom4j.Node;
import org.dynamide.interpreters.Alert;
import org.dynamide.interpreters.Eval;
import org.dynamide.restreplay.mutators.IMutator;
import org.dynamide.util.Tools;
import org.dynamide.restreplay.TreeWalkResults.TreeWalkEntry.STATUS;

import org.json.JSONObject;
import org.json.XML;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/** This class represents the entire transaction of a test: the options used, the request, the response, and the validation.
 *  Many of the fields exposed by this class have no getters or setters.  This is to make use within validators and JEXL expressions
 *  as easy as possible.  Also, this class supports serialization, including some calculated fields, which is used to produce
 *  a database of all the JSON representations of this class for all the test cases run.  That database, if requested
 *  by {@link RunOptions#outputServiceResultDB} is present in reports/db/ after a successful run ( {@link RestReplay#getReportsDir()}/{@link RestReplay#REL_PATH_TO_DB} ).
 *
 * @author Laramie Crocker
 */
public class ServiceResult {
    public ServiceResult(RunOptions options){
        runOptions = options;
        sequence = ""+(staticSequence++);
    }

    private static int staticSequence = 0;

    /** The sequence number is generated for each run, so that each ServiceResult is sequentially numbered
     *  from zero; this number appears on the right side of the detail report on the first line of each test,
     *  and in square brackets in the imports dump at the top of the testGroup report.  It is used
     *  to disambiguate reports when the runID is not specified in a run element, and in filenames in the json db report.
     */
    public String getSequence(){
        return sequence;
    }
    public final String sequence;


    private RunOptions runOptions;
    public RunOptions getRunOptions(){
        return runOptions;
    }

    private transient List<ServiceResult>childResults = new ArrayList<ServiceResult>();
    public void addChild(ServiceResult child){
        child.setParent(this);
        childResults.add(child);
    }
    public List<ServiceResult> getChildResults(){
        return childResults;
    }

    private transient ServiceResult parent;
    public ServiceResult getParent(){
        return parent;
    }
    protected void setParent(ServiceResult sr){
        parent = sr;
    }
    public RestReplayReport.ReportMasterLocations reportMaster = new RestReplayReport.ReportMasterLocations();
    public RestReplayReport.ReportDetailLocations reportDetail = new RestReplayReport.ReportDetailLocations();
    public String controlFileName = "";
    public String testID = "test-id-not-set";
    public String testGroupID = "";
    public String testIDLabel = "test-id-label-not-set"; //a place to stash the internal name used by the serviceresult, but shown for info here only.
    public String idFromMutator = ""; //if a mutator turns a test into many tests, each gets a unique mutator id, as a subset of one of the test cases.
    public boolean isMutation = false;
    public String mutatorType = ""; //the value that was in the <mutator></mutator> field, stored here on the parent.
    public transient IMutator mutator = null;
    public IMutator getMutator(){return mutator;}
    public String mutationDetailBlockID = "";
    public boolean mutatorSkipped = false;
    public boolean mutatorSkippedByOpts = false;
    public String comment = "";
    public boolean isAutodelete = false; //set by RestReplay if this record is generated by AutoDelete.
    public boolean parentSkipped = false; //set by RestReplay.doPOSTPUT when it encounters <mutator skipParent="true" for a test.
    public int loopIndex = -1;
    public int getLoopIndex(){
        return loopIndex;
    }
    public String getLoopQualifier(Eval evalStruct){
        String loopQualifier = "";
        if (loopIndex>-1){
            String loopKey = (String) evalStruct.jc.get("loop.key");
            if (Tools.notBlank(loopKey)){
                loopQualifier = "_"+loopKey;
            } else {
                loopQualifier = "_"+loopIndex;
            }
        }
        return loopQualifier;
    }
    public String fullURL = "";
    public String deleteURL = "";
    public String protoHostPort = ""; //informational: for inspection after the test.
    public String location = "";
    public String CSID = "";
    public String subresourceCSID = "";
    public String requestPayloadFilename = "";
    public String requestPayload = "";  //just like requestPayloadRaw, but may have multipart boundary and headers.
    public String requestPayloadsRaw = "";
    private String xmlResult = "";
    public String getXmlResult() {
        if (Tools.isBlank(xmlResult)){
            PRETTY_FORMAT format = contentTypeFromResponse();
            if (Tools.notBlank(result) && format.equals(PRETTY_FORMAT.XML)) {
                    return result;
            }
        }
        return xmlResult;
    }
    //private void setXmlResult(String xmlResult) {
    //    this.xmlResult = xmlResult;
    //}
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
                this.prettyJSON = prettyPrintJSON(result);
            } catch (Exception e){
                String resultString = RestReplayReport.escape(result);
                resultString = resultString.substring(0, Math.min(resultString.length(), RunOptions.MAX_CHARS_FOR_REPORT_LEVEL_SHORT));
                addError("trying to prettyPrintJSON(RESPONSE) which claimed to be format:" + format
                                + ", RESPONSE : <br />   &laquo;" + resultString +"....&raquo; <br />",
                         e);
            }
            try {
                this.xmlResult = payloadJSONtoXML(result);
            } catch (Exception e){
                String stack = Tools.getStackTrace(e);
                String resultString = RestReplayReport.escape(result);
                resultString = resultString.substring(0, Math.min(resultString.length(), RunOptions.MAX_CHARS_FOR_REPORT_LEVEL_SHORT));
                addError("Error trying to convert RESPONSE as JSON to XML, where RESPONSE claimed to be format:" + format
                                + ", RESPONSE : <br />   &laquo;" + resultString +"....&raquo; <br />",
                                e);
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
        String here = "";
        boolean DEBUG_REPORTED_BY = false;
        if (DEBUG_REPORTED_BY) {
            Throwable there = new Throwable("Marker");
            here = Tools.getStackTraceTop(there, 0, 3, " / ");
            here = "<br />\r\nreported by: [" + RestReplayReport.escape(here) + "]\r\n";
        }
        if (null!=t){
            msg += ": "+t.getLocalizedMessage();
            errorDetail += msg + "<br />\r\n"+Tools.getStackTrace(t, 4);
            errorDetail += here;
        }
        error = msg + here;
        addAlert(error, testIDLabel, Alert.LEVEL.ERROR);
    }
    public void addWarning(String msg){
        addAlertWarning(msg, testIDLabel);
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
    }
    public String failureReason = "";
    public boolean expectedFailure = false;
    public boolean expectedContentExpandedWasJson = false;
    public String expectedContentExpanded = "";
    public String expectedContentExpandedAsXml = "";
    public String expectedContentRaw = "";
    public String expectedResponseFilenameUsed = "";
    public String domcheck = "";
    public transient List<Column> expectedTreewalkRangeColumns;
    public transient Map<STATUS,Range> expectedTreewalkRangeMap = new HashMap<STATUS, Range>();
    public String expectedTreewalkRangeMapToString(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<STATUS,Range> rangeEntry: expectedTreewalkRangeMap.entrySet()){
            if (!rangeEntry.getValue().isEmpty()){
                sb.append(rangeEntry.getKey()+":"+rangeEntry.getValue().toString()+",");
            }
        }
        return sb.toString();
    }
    public Map<String,String> requestHeaderMap  = Tools.createSortedCaseInsensitiveMap();
    public Map<String,String> responseHeaderMap = Tools.createSortedCaseInsensitiveMap();
    public Header[] responseHeaders = new Header[0];
    public void setResponseHeaders(Header[] headers){
        responseHeaderMap.clear();
        for (Header header: headers){
            String value = header.getValue();
            String name= header.getName();
            responseHeaderMap.put(name, value);
        }
    }


    public String responseHeadersDump = "";//This is filled in by Transport, because there used to be two types: HttpUrlConnection and the Apache style, so objects are not generic.  This stashes the string result from Transport.
    public List<Range> ranges = new ArrayList<Range>();
    public final static Range DEFAULT_SUCCESS_RANGE = new Range("2x");
    /** if xml sets no expected/code nodes, then DEFAULT_SUCCESS_RANGE is used. */
    public void initExpectedCodeRanges(Node testNode) {
        List<Node> nodes = testNode.selectNodes("expected/code");
        if (nodes.size() > 0) {
            for (Node codeNode : nodes) {
                Range range = new Range(codeNode.valueOf("@range"));
                ranges.add(range);
            }
        } else {
            nodes = testNode.selectNodes("response/expected/code");
            for (Node codeNode : nodes) {
                Range range = new Range(codeNode.valueOf("@range"));
                ranges.add(range);
            }
        }
    }


    private transient String currentValidatorContextName = "";
    public String getCurrentValidatorContextName(){
        return currentValidatorContextName;
    }
    public void setCurrentValidatorContextName(String name){
        currentValidatorContextName = name;
    }

    private Map<String,Object> vars = new LinkedHashMap<String,Object>();
    public Map<String,Object> getVars(){
        return vars;
    }
    public void addVars(Map<String,Object> newVars){
        vars.putAll(newVars);
    }

    private Map<String,Object> exports = new LinkedHashMap<String,Object>();
    public Map<String, Object> getExports() {
        return exports;
    }
    public void addExports(Map<String,Object> newexports){
        trappedExports.addAll(newexports.keySet());
        exports.putAll(newexports);
    }
    public void addExport(String key, Object value){
        trappedExports.add(key);
        exports.put(key, value);
    }

    private transient List<String> trappedExports = new ArrayList<String>();
    public void beginTrappingExports(){
        trappedExports = new ArrayList<String>();
    }
    public List<String> endTrappingExports(){
        return trappedExports;
    }

    public List<Alert> alerts = new ArrayList<Alert>();
    public int alertsCount(Alert.LEVEL level) {
        int count = 0;
        for (Alert alert : alerts) {
            if (alert.level.equals(level)) {
                count++;
            }
        }
        return count;
    }

    private int labelCounter = 0;
    private Map<String, TreeWalkResults> partSummaries = new LinkedHashMap<String, TreeWalkResults>();
    public void addPartSummary(String label, TreeWalkResults list){
        if (Tools.isBlank(label)){
            label = ""+labelCounter++;
        }
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
            if (Tools.notBlank(key)){buf.append(" label:"+key+": ");}
            if (detailed){
                buf.append("\r\n");
                buf.append(value.fullSummary());
            } else {
                buf.append(value.miniSummary());
            }

        }
        return buf.toString();
    }

    private static String encode(String in){
        return in.replaceAll("<", "&lt;");
    }
    public String partsSummaryHTML(boolean detailed){
        StringBuffer buf = new StringBuffer();
        if (!isDomWalkOK()){
            if (detailed) buf.append("\r\nDOM CHECK FAILED:\r\n");
            else buf.append("; DOM CHECK FAILED:");
        }
        for (Map.Entry<String,TreeWalkResults> entry : partSummaries.entrySet()) {
            String key = entry.getKey();
            TreeWalkResults value = entry.getValue();
            if (Tools.notBlank(key)){buf.append(" label:"+encode(key)+": ");}
            if (detailed){
                buf.append("\r\n");
                buf.append(encode(value.fullSummary()));
            } else {
                buf.append(encode(value.miniSummary()));
            }

        }
        return buf.toString();
    }

    public static boolean codeInSuccessRange(int code){
        if (200<=code && code<400){
            return true;
        }
        return false;
    }

    public boolean isSUCCESS() {
        boolean showSUCCESS = gotExpectedResult();
        if (runOptions!=null) {
            if (runOptions.failTestOnErrors) {
                int iErrs = alertsCount(Alert.LEVEL.ERROR);
                showSUCCESS = showSUCCESS && (iErrs == 0);
            }
            if (runOptions.failTestOnWarnings) {
                int iWarns = alertsCount(Alert.LEVEL.WARN);
                showSUCCESS = showSUCCESS && (iWarns == 0);
            }
        }
        return showSUCCESS;
    }

    private static Map<PAYLOAD_STRICTNESS, Map<STATUS,Range>> rangeCache = new HashMap<PAYLOAD_STRICTNESS, Map<STATUS,Range>>();

    public static Map<STATUS,Range> createRangesForLevel(PAYLOAD_STRICTNESS strictness) {
        Map<STATUS,Range> hit = rangeCache.get(strictness);
        if (null!=hit){
            return hit;
        }

        Map<STATUS,Range> result;
        /* createDOMSet(String matched,
                         String missing,
                         String added,
                         String error,
                         String different,
                         String nested);  */
        switch (strictness){
            case STRICT:
                result = TreeWalkResults.createDOMSet("","0","0","0","0","0");
                break;
            case ADDOK:
                result = TreeWalkResults.createDOMSet("","0","","0","0","");
                break;
            case TEXT:
                result = TreeWalkResults.createDOMSet("","","","0","0","");
                break;
            case TREE:
                result = TreeWalkResults.createDOMSet("","0","0","0","","");
                break;
            case TREE_TEXT:
                result = TreeWalkResults.createDOMSet("","0","0","0","0","");
                break;
            case ZERO:
            default:
                result = TreeWalkResults.createDOMSet("","","","","","");
        }
        rangeCache.put(strictness, result);
        return result;
    }

    public boolean isDomWalkOK() {
        if (this.expectedTreewalkRangeMap.size() > 0) {
            if (Tools.notEmpty(payloadStrictness)){
                failureReason = " Use dom= or expected/dom";
                this.domcheck = failureReason;
                return false;
            }
            return isDomWalkOKByRanges(expectedTreewalkRangeMap);
        }
        if (Tools.isEmpty(payloadStrictness)) {
            return true;
        }
        PAYLOAD_STRICTNESS strictness = PAYLOAD_STRICTNESS.valueOf(payloadStrictness);
        return isDomWalkOKByRanges(createRangesForLevel(strictness));
    }

    private boolean checkRange(TreeWalkResults value,
                                      STATUS status,
                                      List<Column> columns,
                                      List<String> notices,
                                      Map<STATUS,Range> theExpectedTreewalkRangeMap){
        int c = value.countFor(status);
        Range r = theExpectedTreewalkRangeMap.get(status);
        Column col = new Column();
        columns.add(col);
        col.name = status.name();
        col.num = ""+c;

        if (r!=null && (!r.isEmpty())) {
            col.exp = r.toString();
            if (r.valueInRange(c)) {
                return true;
            } else {
                col.highlight = "dom-not-matched";
                failureReason = "DOM failed criteria: "+status.name()+" "+c+"/"+r.toString();
                notices.add(status.name()+" "+c+"/"+r.toString());
                return false;
            }
        }
        return true;
    }

    public static class Column {
        String name;
        String num;
        String exp;
        String highlight;
    }

    public boolean isDomWalkOKByRanges(Map<STATUS,Range> expectedRangeMap){
        boolean result = true;
        List<Column> columns = new ArrayList<Column>();
        List<String> notices = new ArrayList<String>();
        // THE ORDER of this method determines the ORDER of the DOM Comparison table widget in the report.
        for (Map.Entry<String,TreeWalkResults> entry : partSummaries.entrySet()) {
            TreeWalkResults value = entry.getValue();
            if (!checkRange(value, STATUS.MATCHED, columns, notices, expectedRangeMap)) {
                result &= false;
            }
            if (!checkRange(value, STATUS.DIFFERENT, columns, notices, expectedRangeMap)) {
                result &= false;
            }
            if (!checkRange(value, STATUS.REMOVED, columns, notices, expectedRangeMap)) {
                result &= false;
            }
            if (!checkRange(value, STATUS.ADDED, columns, notices, expectedRangeMap)) {
                result &= false;
            }
            if (!checkRange(value, STATUS.ERROR, columns, notices, expectedRangeMap)) {
                result &= false;
            }
            if (!checkRange(value, STATUS.NESTED_ERROR, columns, notices, expectedRangeMap)) {
                result &= false;
            }
        }
        this.expectedTreewalkRangeColumns = columns;
        StringBuilder criteria = new StringBuilder();
        if (result==false){
            int i = 0;
            for (String crit: notices) {
                if (i>0) criteria.append(",");
                i++;
                criteria.append(crit);
            }
            failureReason = criteria.toString();
            this.domcheck = failureReason;
        }
        return result;
    }

    public boolean overrideExpectedResult = false;

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
        if (Tools.notBlank(failureReason)){
            return false;
        }
        //if (Tools.notEmpty(failureReason)){
        //    return false;
        //}

        List<Range> rangesList = ranges;

        gotExpectedResultBecause = " test["+testID+']';
        if (null!=mutator && Tools.notEmpty(idFromMutator)){
            //System.out.println("\r\n\r\n>>>>>>>>>> id: "+testID+" mutationID:"+idFromMutator);
            boolean hasRange = mutator.hasRangeForId(idFromMutator);
            if(hasRange) {
                gotExpectedResultBecause = " mutator["+idFromMutator+']';
                boolean found = mutator.valueInRangeForId(responseCode, idFromMutator);
                //System.out.println(" found("+idFromMutator+"): "+found);
                return found;
            } else {
                //System.out.println(" NOT found("+idFromMutator+") ");
            }
            if (this.getParent() != null){
                ServiceResult p = this.getParent();
                if (p.ranges.size()>0){
                    rangesList = p.ranges;
                    //System.out.println("---> ranges: "+ranges + "rangeList: "+rangesList);
                    gotExpectedResultBecause = " parent["+p.testID+']';
                }
            }
        }

        if (ranges.size()==0){
            if (DEFAULT_SUCCESS_RANGE.valueInRange(responseCode)){
                failureReason = "";
                return isDomWalkOK();
            }
        }

        for (Range range: rangesList){
            if (range.valueInRange(responseCode)){
                failureReason = "";
                gotExpectedResultBecause += " range: "+range;
                return isDomWalkOK();
            }
        }

        //failureReason = " : STATUS CODE ("+responseCode+") UNEXPECTED2; ";
        return false;
    }

    public void addRequestHeader(String name, String value){
        requestHeaderMap.put(name, value);
    }


    public static enum DUMP_OPTIONS {minimal, detailed, full, auto};

    public static enum PAYLOAD_STRICTNESS {ZERO, ADDOK, TREE, TEXT, TREE_TEXT, STRICT};

    public String toString(){
        return detail(false);
    }

    private static final String lineHDR(String title){return "\r\n-------------- "+title+" --------------------\r\n";}
    private static final String LINE = "\r\n----------------------------------------------------\r\n";

    public String dumpPayloads(){
        StringBuffer s = new StringBuffer();
        if (Tools.notBlank(this.requestPayload)){
            s.append(lineHDR("request payload"));
            s.append(this.requestPayload);
            s.append(LINE);
        }
        if (Tools.notBlank(this.getResult())){
            s.append(lineHDR("response payload"));
            s.append(this.getResult());
            s.append(LINE);
        }
        return s.toString();
    }
    public String detail(boolean includePayloads){
        int warnings = alertsCount(Alert.LEVEL.WARN);
        int errors =   alertsCount(Alert.LEVEL.ERROR);
        String res =  "{"
                + ( isSUCCESS() ? "SUCCESS" : "FAILURE"  )
                + (Tools.notBlank(failureReason) ? "; failureReason: {"+failureReason+"}":"")
                +"; "+method
                +"; "+responseCode
                + ( (ranges.size()>0) ? "; expected:"+ranges : "" )
                + ( Tools.notEmpty(testID) ? "; testID:"+testID : "" )
                + ( Tools.notEmpty(testGroupID) ? "; testGroupID:"+testGroupID : "" )
                + ( Tools.notEmpty(fromTestID) ? "; fromTestID:"+fromTestID : "" )
                + ( errors>0 ? "; ERRORS:"+errors : "" )
                + ( warnings>0 ? "; WARNINGS:"+warnings : "" )
                + ( Tools.notEmpty(responseMessage) ? "; msg:"+responseMessage : "" )
                +"; URL:"+fullURL
                +"; time:"+time+"ms"
                +"; auth: "+auth
                + ( Tools.notEmpty(deleteURL) ? "; deleteURL:"+deleteURL : "" )
                + ( Tools.notEmpty(location) ? "; location:"+location : "" )
                + ( Tools.notEmpty(error) ? "; ERROR_IN_DETAIL:"+error : "" )
                + "; gotExpected:"+gotExpectedResult()
                + ( "; parts-summary: {"+partsSummary(true)+"}")
                +"}"
                + ( includePayloads  ? dumpPayloads() : "" )
                ;
        return res;
    }

    public String minimal(){
        return minimal(false);
    }

    public String minimal(boolean verbosePartsSummary){
        int warnings = alertsCount(Alert.LEVEL.WARN);
        int errors =   alertsCount(Alert.LEVEL.ERROR);
        String expected = "";
        if (ranges.size()>0){ expected = "; expected:"+ranges; }
        return "{"
                + ( isSUCCESS() ? "SUCCESS" : "FAILURE"  )
                + (Tools.notBlank(failureReason) ? ", reason: {"+ failureReason+"}" : "")
                + ( Tools.notEmpty(testID) ? "; "+testID : "" )
                +"; "+method
                +"; "+responseCode
                + expected
                + ( errors>0 ? "; ERRORS:"+errors : "" )
                + ( warnings>0 ? "; WARNINGS:"+warnings : "" )
                + ( Tools.notEmpty(responseMessage) ? "; msg:"+responseMessage : "" )
                +"; URL:"+fullURL
                +"; time:"+time+"ms"
                //for auth, see detail()   +"; auth: "+auth
                + ( Tools.notEmpty(error) ? "; ERROR_IN_PART:"+error : "" )
                + "; parts-summary: {"+(verbosePartsSummary ? partsSummary(true) : partsSummary(false) )+"}"
                +"}";
    }

    public String tiny(){
        int warnings = alertsCount(Alert.LEVEL.WARN);
        int errors =   alertsCount(Alert.LEVEL.ERROR);
        String expected = "";
        if (ranges.size()>0){ expected = "; expected:"+ranges; }
        return "{"
                + ( isSUCCESS() ? "SUCCESS" : "FAILURE"  )
                + (Tools.notBlank(failureReason) ? ", reason: {"+ failureReason+"}" : "")
                + ( Tools.notEmpty(testID) ? "; "+testID : "" )
                +"; "+method
                +"; "+responseCode
                + expected
                + ( errors>0 ? "; ERRORS:"+errors : "" )
                + ( warnings>0 ? "; WARNINGS:"+warnings : "" )
                +"; time:"+time+"ms"
                + ( Tools.notEmpty(error) ? "; ERROR_IN_PART:"+error : "" )
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

    /** <p>This method may be called from a test case, using a syntax
     *  with XPath like this:
     *      <code>${testID3.got("//refName")}</code>
     *  or with JSON, like this:
     *     <code>${testID3.got("$..refName")}</code></p>
     *
     *  <p>JsonPath docs say ALL expressions start with $.</p>
     *
     *  <p>An XPath could only start with a $ if you named an element like so: <code>$myvariable</code>.  So don't do that.</p>
     *
     *  <p>If you know which syntax you want to use, see these variants: {@see #gotXPath(String)} {@see  #gotJson(String)} </p>
     *
     *  @param pathExpression If this.result is JSON, as determined by contentTypeFromResponse(), then JsonPath is assumed,
     *                        otherwise XPath is assumed. */
    public Object got(String pathExpression) throws Exception {
        if (pathExpression.startsWith("$")
                && Tools.notBlank(result)
                && contentTypeFromResponse().equals(PRETTY_FORMAT.JSON)) {
            return gotJson(pathExpression);  //pathExpression is jsonPath
        }
        return gotXPath(pathExpression);
    }

    public Object gotXPath(String pathExpression) throws Exception {
        if (Tools.isBlank(pathExpression)) {
            addAlertWarning("XPath pathExpression was empty in " + this.testIDLabel);
            return "";
        }
        try {
            String source;
            if (Tools.notBlank(this.getXmlResult())) {
                source = this.getXmlResult();
            } else {
                source = this.result;
            }
            if (Tools.isBlank(source)) {
                return "";
            }
            org.jdom.Element element = (org.jdom.Element) XmlCompareJdom.selectSingleNode(source, pathExpression, null);
            String sr = element != null ? element.getText() : "";
            return sr;
        } catch (Throwable t){
            addAlert("ERROR reading response value: " + t,
                    "xpath:"+pathExpression,                    //pathExpression is xpath
                    Alert.LEVEL.ERROR);
            return "";
        }
    }

    public Object gotJson(String jsonPath){
        if (Tools.isBlank(jsonPath)) {
            addAlertWarning("JsonPath pathExpression was empty in " + this.testIDLabel);
            return "";
        }
        try {
            return JsonPath.read(this.result, jsonPath);
        } catch (Throwable t){
            addAlert("ERROR reading response value: " + t,
                    "JsonPath:"+jsonPath,                    //jsonPath is JsonPath
                    Alert.LEVEL.ERROR);
            return "";
        }
    }

    /** <p></p>This method may be called from a test case, using a syntax
     *  with XPath like this:
     *      <code>${testID3.sent("//refName")}</code>
     *  or with JSON, like this:
     *     <code>${testID3.sent("$..refName")}</code></p>
     *
     *  <p>JsonPath docs say ALL expressions start with $.</p>
     *
     *  <p>An XPath could only start with a $ if you named an element like so: <code>$myvariable</code>.  So don't do that.</p>
     *
     *  <p>If you know which syntax you want to use, see these variants: {@see #sentXPath(String)} {@see  #sentJson(String)} </p>
     *
     *  @param path If this.result is JSON, as determined by contentTypeFromResponse(), then JsonPath is assumed,
     *                        otherwise XPath is assumed. */
    public Object sent(String path) throws Exception {
        if (path.startsWith("$")
                && Tools.notBlank(this.requestPayload) ) {
            return sentJson(path, this.requestPayload);  //pathExpression is jsonPath
        }
        return sentXPath(path, this.requestPayload);
    }

    public Object sentXPath(String path) throws Exception {
        if (this.requestPayload == null){
            return "ERROR:null:requestPayloads";
        }
        return sentXPath(path, this.requestPayload);
    }

    public Object sentXPath(String xpath, String source) throws Exception {
        org.jdom.Element element = (org.jdom.Element) XmlCompareJdom.selectSingleNode(source, xpath, null);   //e.g. "//shortIdentifier");
        String sr = element != null ? element.getText() : "";
        return sr;
    }

    public Object sentJson(String jsonPath) {
        if (this.requestPayload == null){
            return "ERROR:null:requestPayloads";
        }
        return sentJson(jsonPath, this.requestPayload);
    }

    public Object sentJson(String jsonPath, String source){
        try {
            return JsonPath.read(source, jsonPath);
        } catch (Throwable t){
            addAlert("ERROR reading response value: " + t,
                    "JsonPath:"+jsonPath,                    //jsonPath is JsonPath
                    Alert.LEVEL.ERROR);
            return "";
        }
    }

    /* Responding to these string names makes these accessible by Jexl.
    We could also just add getters to any class field, for example, as is done for mutator/getMutator.
    Jexl will case-insensitively read related gettters.*/
    public Object get(String what){
        if ("CSID".equals(what)){
            return CSID;
        } else if ("location".equalsIgnoreCase(what)){
            return location;
        } else if ("testID".equalsIgnoreCase(what)){
            return testID;
        } else if ("testIDLabel".equalsIgnoreCase(what)){
            return testIDLabel;
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
        } else if ("JSESSIONID".equalsIgnoreCase(what)){
            String setcookie =  responseHeaderMap.get("SET-COOKIE");
            if (setcookie==null||setcookie.length()==0){
                return "";
            }
            setcookie = setcookie.substring(0, setcookie.indexOf(';')).trim();
            return setcookie;
        }
        if (vars.containsKey(what)){
            return vars.get(what);
        }
        if (exports.containsKey(what)){
            return exports.get(what);
        }
        if (responseHeaderMap.containsKey(what)){
            return responseHeaderMap.get(what);
        }
        if (requestHeaderMap.containsKey(what)){
            return requestHeaderMap.get(what);
        }
        return "";
    }

    public static String prettyPrintJSON(String payload){
        JSONSuper maybe = new JSONSuper(payload);
        switch (maybe.type){
            case OBJECT:
                return maybe.jsonObject.toString(4);
            case ARRAY:
                return maybe.jsonArray.toString(4);
            default:
                System.out.println("WARNING: json was neither a JSONObject or a JSONArray");
                return payload;
        }
    }

    public static String payloadJSONtoXML(String payload) {
        JSONSuper maybe = new JSONSuper(payload);
        String xml = maybe.toXMLString();
        return xml;
    }

    public static String payloadXMLtoJSON(String payload) {
        try {
            JSONObject json = XML.toJSONObject(payload);
            if ( json.has("root") ) {
                JSONObject root = json.getJSONObject("root");
                return root.toString(4);
            } else {
                return json.toString(4);
            }
        } catch (Exception e){
            return "ERROR converting to JSON: "+e;
        }
    }


    public enum PRETTY_FORMAT {XML, JSON, HTML, NONE}

    public PRETTY_FORMAT contentTypeFromResponse(){
        String uc = "";
        if (Tools.notBlank(this.mimeType)){
            uc = this.mimeType.toUpperCase();
            if (uc.endsWith("/JSON")){
                return PRETTY_FORMAT.JSON;
            } else if (uc.endsWith("/XML") || uc.endsWith("/XHTML")) {
                return PRETTY_FORMAT.XML;
            } else if (uc.endsWith("/HTML")){
                return PRETTY_FORMAT.HTML;
            };
        } else if (Tools.notBlank(this.requestPayloadFilename)){
            uc = this.requestPayloadFilename.toUpperCase();
            if (uc.endsWith(".JSON")){
                return PRETTY_FORMAT.JSON;
            } else if (uc.endsWith(".XML") || uc.endsWith(".XHTML")){
                return PRETTY_FORMAT.XML;
            } else if (uc.endsWith("/HTML")){
                return PRETTY_FORMAT.HTML;
            };
        } else {
            return PRETTY_FORMAT.NONE;
        }
        return PRETTY_FORMAT.NONE;
    }

    public void addAlert(String message, String context, Alert.LEVEL level){
        alerts.add(new Alert(message, context, level));
    }

    public void addAlertError(String message, String context){
        alerts.add(new Alert(message, context, Alert.LEVEL.ERROR));
    }
    public void addAlertWarning(String message, String context){
        alerts.add(new Alert(message, context, Alert.LEVEL.WARN));
    }
    public void addAlertOK(String message, String context){
        alerts.add(new Alert(message, context, Alert.LEVEL.OK));
    }

    public void addAlertError(String message){
        alerts.add(new Alert(message, getCurrentValidatorContextName(), Alert.LEVEL.ERROR));
    }
    public void addAlertWarning(String message){
        alerts.add(new Alert(message, getCurrentValidatorContextName(), Alert.LEVEL.WARN));
    }
    public void addAlertOK(String message){
        alerts.add(new Alert(message, getCurrentValidatorContextName(), Alert.LEVEL.OK));
    }

    // ========= Payload serialization support ========================
    public static class Payload{
        String id;
        String body;
        ServiceResult.PRETTY_FORMAT format;
        String title;
        String subtitle;
        boolean usePRE;
        public String toString(){
            return "id:"+id+";title:"+title+";subtitle:"+subtitle+";usePRE:"+usePRE;
        }
    }
    public Map<String,Payload> payloads = new LinkedHashMap<String, Payload>();

    // ========= calculated fields for serialization ==================
    public boolean calcGotExpectedResult = false;
    public String calcSuccessLabel = "";
    public String calcExpectedTreewalkRangeColumns = "";

    public void populateSerializedFields(){
        RestReplayReport.formatPayloads(this, 0);//tocID==0
        this.calcGotExpectedResult = gotExpectedResult();
        this.calcSuccessLabel = RestReplayReport.formatMutatorSUCCESS(this);
        this.calcExpectedTreewalkRangeColumns = RestReplayReport.formatExpectedTreewalkRangeColumns(this);
    }

}
