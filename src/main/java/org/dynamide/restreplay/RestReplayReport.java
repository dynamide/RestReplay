package org.dynamide.restreplay;

import org.dynamide.util.XmlTools;
import org.dynamide.util.FileTools;
import org.dynamide.util.Tools;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dynamide.restreplay.ServiceResult.Alert;
import org.dynamide.restreplay.ServiceResult.Alert.LEVEL;

import org.json.JSONObject;
import org.json.XML;


/**  Format a report based on RestReplay ServiceResult object from a test group.
 * @author  laramie
 */
public class RestReplayReport {
    public static final String INCLUDES_DIR = "_includes";

    protected static final String HTML_PAGE_END = "</body></html>";
    protected static final String TOPLINKS = "<a class='TOPLINKS' href='javascript:openAll();'>Show All Payloads</a>"
            + "<a class='TOPLINKS' href='javascript:closeAll();'>Hide All Payloads</a>"
            + "<a class='TOPLINKS' href='javascript:openAllHeaders();'>Show All Headers</a>"
            + "<a class='TOPLINKS' href='javascript:closeAllHeaders();'>Hide All Headers</a>"
            + "<a class='TOPLINKS' href='javascript:openAllMutations();'>Show All Mutations</a>"
            + "<a class='TOPLINKS' href='javascript:closeAllMutations();'>Hide All Mutations</a>";

    protected static final String HTML_TEST_START = "<div class='TESTCASE'>";
    protected static final String HTML_TEST_END = "</div>";

    protected static final String GROUP_START = "<div class='TESTGROUP'>";
    protected static final String GROUP_END = "</div>";

    protected static final String RUNINFO_START = "<div class='RUNINFO'>";
    protected static final String RUNINFO_END = "</div>";


    protected static final String DIV_END = "</div>";

    protected static final String PRE_START = "<pre class='SUMMARY'>";
    protected static final String PRE_END = "</pre>";
    protected static final String BR = "<br />\r\n";

    protected static final String DETAIL_HDR = "<h2 class='DETAIL_HDR'>Test Details</h2>";
    protected static final String DETAIL_START = "<table border='1' class='DETAIL_TABLE'><tr><td>\r\n";
    protected static final String DETAIL_LINESEP = "</td></tr>\r\n<tr><td>";
    protected static final String DETAIL_END = "</td></tr></table>";

    protected static final String TOC_START = "<table border='1' class='TOC_TABLE'><tr><td colspan='6' class='TOC_HDR'>Summary</td></tr>"
                                              +"<tr><th>testID</th><th>time(ms)</th><th>status</th><th>code</th><th>warn</th><th>error</th></td></tr>\r\n"
                                              +"<tr><td>\r\n";
    protected static final String TOC_LINESEP = "</td></tr>\r\n<tr><td class='%s'>";
    protected static final String TOC_CELLSEP = "</td><td>";
    protected static final String TOC_END = "</td></tr></table>";

    protected static final String HDRBEGIN = "<span class='HEADERBLOCK'>";
    protected static final String HDREND = "</span>";

    private static final String SP = "&nbsp;&nbsp;&nbsp;";

    public RestReplayReport(String reportsDir) {
        this.reportsDir = reportsDir;
    }

    private String reportsDir = "";

    public String getReportsDir() {
        return reportsDir;
    }

    protected static String formatCollapse(String myDivID, String linkText) {
        return "<a href='javascript:;' onmousedown=\"toggleDiv('" + myDivID + "');\">" + linkText + "</a>"
                + BR
                + "<div ID='" + myDivID + "' class='PAYLOAD' style='display:none'>";
    }


    //private StringBuffer buffer = new StringBuffer();
    private List<ServiceResult> reportsList = new ArrayList<ServiceResult>();

    private String runInfo = "";

    public String getPage(String basedir) {
        return formatPageStart(basedir)
                + "<div class='REPORTTIME'><b>RestReplay</b> "+lbl(" run on")+" " + Tools.nowLocale() + "</div>"
                + header.toString()
                + this.runInfo
                + BR
                + reportsListToTOC()
                + BR
                + DETAIL_HDR
                + reportsListToString()
                + HTML_PAGE_END;
    }

    private String reportsListToTOC(){
        int i = 0;
        for (ServiceResult serviceResult: reportsList) {
            TOC toc = new TOC();
            toc.tocID = i++;//tocID;
            toc.testID = serviceResult.testID;
            toc.time = serviceResult.time;
            toc.detail = (serviceResult.gotExpectedResult() ? ok("SUCCESS") : red("FAILURE"));
            toc.warnings = alertsCount(serviceResult.alerts, LEVEL.WARN);
            toc.errors = alertsCount(serviceResult.alerts, LEVEL.ERROR);
            toc.responseCode = serviceResult.responseCode;
            toc.isMutation = serviceResult.isMutation;
            toc.idFromMutator = serviceResult.idFromMutator;
            if (serviceResult.getChildResults().size()>0){
                int numGotExpected = 0;
                int numErrors = 0;
                int numWarnings = 0;
                int numResults = 0;
                for (ServiceResult cr: serviceResult.getChildResults()) {
                    numResults++;
                    if (cr.gotExpectedResult()){
                        numGotExpected ++;
                    }
                    numErrors   += alertsCount(serviceResult.alerts, LEVEL.WARN);
                    numWarnings += alertsCount(serviceResult.alerts, LEVEL.ERROR);
                }
                assert numResults == serviceResult.getChildResults().size();
                String rowid = "childresults_"+serviceResult.testID;
                //Don't change parentage of these tags, there is javascript that does el.parentElement.parentElement.
                toc.children = "<br /><div class='childResults' onclick='hideresults(\""+rowid+"\", this);'>[ <span class='childstate'>-</span> ] mutations "
                               +"<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+serviceResult.mutator
                                  +" ("+serviceResult.getChildResults().size()+") "
                               +"<table id='"+rowid+"'"+serviceResult.testID+" class='child-results-summary'><tr><th>success</th><th>warn</th><th>error</th></tr>"
                                +"<tr>"
                                +"<td>"+numGotExpected+'/'+numResults+"</td>"
                                +"<td>"+numWarnings+"</td>"
                                +"<td>"+numErrors+"</td>"
                               +"</tr></table></div>";
            }
            tocList.add(toc);
        }
        return getTOC("").toString();
    }

    private String reportsListToString(){
        StringBuffer buffer = new StringBuffer();
        for (ServiceResult sr: reportsList) {
            appendServiceResult(sr, buffer);
        }
        return buffer.toString();
    }

    public String getTOC(String reportName) {
        StringBuffer tocBuffer = new StringBuffer();

        if (Tools.notBlank(reportName)) {
            // We are generating a TOC for an index.html file that references other report files.
            this.header.reportNameLink = reportName;
            tocBuffer.append(this.header.toString());
        } else {
            // We are generating a single report file, so all links are relative to this file, and we should have the TOPLINKS which allow things like showAllPayloads..
        }
        tocBuffer.append(BR);
        tocBuffer.append(TOC_START);
        int count = 0;
        for (TOC toc : tocList) {
            String cssClass = toc.isMutation ? "mutation" : "";
            String sep = String.format(TOC_LINESEP, cssClass);
            if (count>0){tocBuffer.append(sep);}
            count++;
            tocBuffer.append("<a href='" + reportName + "#TOC" + toc.tocID + "'>" + toc.testID+toc.idFromMutator + "</a> ")
                     .append((toc.children))
                     .append(TOC_CELLSEP)
                    .append(toc.time)
                    .append(TOC_CELLSEP)
                     .append(toc.detail)
                     .append(TOC_CELLSEP)
                    .append(toc.responseCode)
                     .append(TOC_CELLSEP)
                     .append(tocWarn(toc.warnings))
                     .append(TOC_CELLSEP)
                     .append(tocError(toc.errors));
        }
        tocBuffer.append(TOC_END);
        tocBuffer.append(BR);
        if (Tools.notBlank(reportName)) {
        } else {
            // We are generating a single report file, so all links are relative to this file, and we should have the TOPLINKS which allow things like showAllPayloads..
            tocBuffer.append(BR).append(TOPLINKS).append(BR);
        }
        return tocBuffer.toString();
    }

    public void addRunInfo(String text) {
        this.runInfo = RUNINFO_START + text + RUNINFO_END;
        //addText(this.runInfo);
    }

    /**
     * Call this method to insert arbitrary HTML in your report file, at the point after the last call to addTestResult() or addTestGroup().
     */
   // public void addText(String text) {
   //     buffer.append(text);
   // }

    private class Header {
        public String groupID;
        public String controlFile;
        public String reportNameLink;
        public String toString(){
           StringBuffer sb = new StringBuffer();
            sb.append(GROUP_START);
            String groupIDLink = (Tools.isEmpty(reportNameLink))
                                 ? groupID
                                 : "<a href='"+reportNameLink+"'>"+groupID+"</a>";
            sb.append(lbl("Test Group")).append(groupIDLink).append(SP).append(lbl("Control File")).append(controlFile);
            sb.append(GROUP_END);
            return sb.toString();
        }
    }
    private Header header = new Header();

    public void addTestGroup(String groupID, String controlFile) {
        header.groupID = groupID;
        header.controlFile = controlFile;
    }

    private int divID = 0;

    public void addTestResult(ServiceResult serviceResult) {
        reportsList.add(serviceResult);

    }

    private void appendServiceResult(ServiceResult serviceResult, StringBuffer buffer){


        buffer.append(HTML_TEST_START);
        int tocID = divID++;
        buffer.append(formatSummary(serviceResult, tocID));
        buffer.append(formatPayloads(serviceResult, tocID));
        buffer.append(HTML_TEST_END);
    }

    public static class TOC {
        public int tocID;
        public String testID;
        public String detail;
        public int warnings = 0;
        public int errors = 0;
        public long time = 0;
        public int responseCode = 0;
        public boolean isMutation = false;
        public String idFromMutator = "";
        public String children = "";
    }

    private List<TOC> tocList = new ArrayList<TOC>();

    public static String formatPageStart(String restReplayBaseDir) {
        String script = FileTools.readFile(restReplayBaseDir, INCLUDES_DIR + "/reports-include.js");
        String style = FileTools.readFile(restReplayBaseDir, INCLUDES_DIR + "/reports-include.css");
        return "<html><head><script type='text/javascript'>\r\n"
                + script
                + "\r\n</script>\r\n<style>\r\n"
                + style
                + "\r\n</style></head><body>";
    }

    public File saveReport(String restReplayBaseDir, String reportsDir, String reportName) {
        try {
            File resultFile = FileTools.saveFile(reportsDir, reportName, this.getPage(restReplayBaseDir), true);
            if (resultFile != null) {
                String resultFileName = resultFile.getCanonicalPath();
                //System.out.println("RestReplay summary reports saved to directory: "+resultFile.getParent());
                System.out.println("**** \r\n**** RestReplay summary report: " + resultFileName+"\r\n**** ");
                return resultFile;
            }
        } catch (Exception e) {
            System.out.println("ERROR saving RestReplay report in basedir: " + reportsDir + " reportName: " + reportName + " error: " + e);
        }
        return null;
    }

    //public static String getReportsDir(String basename){
    //    return Tools.glue(basename,"/","TEST-REPORTS");
    //}

    /**
     * @param localMasterFilename should be a local filename for the index of each restReplay master control file, e.g. objectexit.xml
     *                            so what gets written to disk will be something like index.objectexit.xml.html . The actual filename will be available from
     *                            the returned File object if successful.
     * @return File if successful, else returns null.
     */
    public static File saveIndexForMaster(String restReplayBaseDir,
                                          String reportsDir,
                                          String localMasterFilename,
                                          List<String> reportsList,
                                          String envID,
                                          Map<String,String> masterVars) {
        String masterFilename = "index."
                +(Tools.notBlank(envID)?envID+'.':"")
                + localMasterFilename + ".html";
        try {
            StringBuffer sb = new StringBuffer(formatPageStart(restReplayBaseDir));
            String dateStr = Tools.nowLocale();
            sb.append("<div class='REPORTTIME'><b>RestReplay</b> "+lbl(" run on")+" " + dateStr + lbl(" master") + localMasterFilename + "</div>");
            sb.append("<div class='masterVars'>")
              .append("<span class='LABEL'>environment:</span> <span class='env'>"+envID+"</span><br />")
              .append(formatMasterVars(masterVars)).append("</div>");
            for (String oneToc : reportsList) {
                sb.append(oneToc);
                sb.append("<hr />");
            }
            sb.append(HTML_PAGE_END);
            System.out.println("====\r\n==== Master Report Index: "+reportsDir+'/'+masterFilename+"\r\n====");
            return FileTools.saveFile(reportsDir, masterFilename, sb.toString(), false);
        } catch (Exception e) {
            System.out.println("ERROR saving RestReplay report index: in  restReplayBaseDir: " + reportsDir + "localMasterFilename: " + localMasterFilename + " masterFilename: " + masterFilename + " list: " + reportsList + " error: " + e);
            return null;
        }
    }

    protected static String formatMasterVars(Map<String, String> masterVars) {
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, String> entry : masterVars.entrySet()) {
            buffer.append("\r\n<div class='varslist'>")
                  .append(entry.getKey()).append(": ").append(entry.getValue())
                  .append("</div>");
        }
        return buffer.toString();
    }

    protected String formatSummary(ServiceResult serviceResult, int tocID) {


        StringBuffer fb = new StringBuffer();
        fb.append("<a name='TOC" + tocID + "'></a>");
        fb.append(detail(serviceResult, false, false, DETAIL_START, DETAIL_LINESEP, DETAIL_END, tocID));
        return fb.toString();
    }

    protected String formatPayloads(ServiceResult serviceResult, int tocID) {
        StringBuffer fb = new StringBuffer();
        fb.append(BR);
        ServiceResult.PRETTY_FORMAT respType = serviceResult.contentTypeFromResponse();
        appendPayload(fb, serviceResult.requestPayloadsRaw, respType, "REQUEST (raw)", "REQUESTRAW" + tocID);
        appendPayload(fb, serviceResult.requestPayload, respType, "REQUEST", "REQUEST" + tocID);
        appendPayload(fb, serviceResult.getResult(), respType, "RESPONSE (raw)", "RESPONSERAW" + tocID);
        appendPayload(fb, serviceResult.prettyJSON, respType, "RESPONSE", "RESPONSE" + tocID);
        appendPayload(fb, serviceResult.expectedContentExpanded, respType, "EXPECTED", "EXPECTED" + tocID);
        fb.append(BR);
        return fb.toString();
    }

    protected void appendPayload(StringBuffer fb, String payload, ServiceResult.PRETTY_FORMAT format, String title, String theDivID) {
        if (Tools.notBlank(payload)) {
            //fb.append(BR+title+":"+BR);
            try {
                switch (format) {
                    case XML:
                        //System.out.println("PAYLOAD:"+payloadJSONtoXML(payload));
                        //JSONObject json = new JSONObject(payload);
                        //String xml = XML.toString(json);
                        //System.out.println("PAYLOAD xml:"+xml);
                        //System.out.println("PAYLOAD raw:" + payload);
                        String pretty = prettyPrint(payload);
                        fb.append(formatCollapse(theDivID, title));  //starts a div.
                        fb.append(PRE_START);
                        fb.append(escape(pretty));
                        fb.append(PRE_END);
                        fb.append(DIV_END);//ends that div.
                        break;
                    case JSON:
                    case NONE:
                        //System.out.println("PAYLOAD raw:" + payload);
                        fb.append(formatCollapse(theDivID, title));  //starts a div.
                        fb.append(PRE_START);
                        fb.append(escape(payload));
                        fb.append(PRE_END);
                        fb.append(DIV_END); //ends that div.
                        break;
                    default:
                        System.err.println("ERROR: Unhandled enum type in RestReplayReport.appendPayload(" + title + "): " + format.toString());
                }
            } catch (Exception e) {
                String error = "<font color='red'>ERROR_IN_APPEND_PAYLOAD: (" + e.getClass().getName() + ':' + e.getLocalizedMessage() + ")</font> " + payload;
                fb.append(error);
                fb.append(BR).append(BR);
                fb.append("payload raw: " + payload);
            }
        }
    }

    private String escape(String source) {
        try {
            return Tools.searchAndReplace(source, "<", "&lt;");
        } catch (Exception e) {
            return "ERROR escaping requestPayload" + e;
        }
    }

    private String prettyPrint(String rawXml) throws Exception {
        Document document = DocumentHelper.parseText(rawXml);
        return XmlTools.prettyPrint(document, "    ");
    }

    private static final String LINE = "<hr />\r\n";
    private static final String CRLF = "<br />\r\n";

    protected String red(String label) {
        return "<span class='ERROR'>" + label + "</span> ";
    }

    protected String tocWarn(int count) {
        if (count == 0){
            return "";
        }
        return "<span class='toc-warn'>WARNINGS: " + count + "</span> ";
    }

    protected static String tocError(int count) {
        if (count == 0){
            return "";
        }
        return "<span class='toc-error'>ERRORS: " + count + "</span> ";
    }

    protected static String ok(String label) {
        return "<span class='OK'>" + label + "</span> ";
    }

    protected static String lbl(String label) {
        return "<span class='LABEL'>" + label + ":</span> ";
    }

    protected static String small(String label) {
        return "<span class='SMALL'>" + label + "</span> ";
    }

    protected String units(String label) {
        return "<span class='LABEL'>" + label + "</span> ";
    }

    protected String alertError(String msg) {
        return "<span class='AlertError'>" + msg + "</span> ";
    }

    public String detail(ServiceResult s, boolean includePayloads, boolean includePartSummary, String start, String linesep, String end, int tocID) {
        String partSummary = s.partsSummary(includePartSummary);
        String res = start
                + (s.gotExpectedResult() ? lbl("SUCCESS") : "<font color='red'><b>FAILURE</b></font>")
                + SP + (Tools.notEmpty(s.testID) ? s.testID : "")+ "<span class='mutationsubscript'>"+s.idFromMutator + "</span>  "
                + SP + linesep
                + s.method + SP + "<a href='" + s.fullURL + "'>" + s.fullURL + "</a>" + linesep
                + s.responseCode + SP
                    + ((s.expectedCodes.size() > 0) ? lbl("  expected") + s.expectedCodes  : "")
                    +  lbl("  gotExpected") + s.gotExpectedResult()
                   //+ lbl(" len:")+s.contentLength + linesep
                    + SP + lbl(" time")+s.time + units("ms") + linesep
                + (Tools.notBlank(s.failureReason) ? s.failureReason + linesep : "")

                //+ ( Tools.notEmpty(s.testGroupID) ? "testGroupID:"+s.testGroupID+linesep : "" )
                //THIS WORKS, BUT IS VERBOSE: + ( Tools.notEmpty(s.fromTestID) ? "fromTestID:"+s.fromTestID+linesep : "" )
                + (Tools.notEmpty(s.responseMessage) ? lbl("msg") + s.responseMessage + linesep : "")
                + (s.auth == null ? "" : lbl("auth") + s.auth + linesep)
                + alertsToHtml(s.alerts) + linesep
                + HDRBEGIN + lbl("req-headers(mime-only)") + requestHeadersToHtml(s.requestHeaders) + HDREND + linesep
                + HDRBEGIN + lbl("req-headers(from-control-file)") + requestHeadersToHtml(s.headerMap) + HDREND + linesep
                + HDRBEGIN + lbl("resp-headers") + s.responseHeadersDump + HDREND + linesep
                + (Tools.notEmpty(s.deleteURL) ? lbl("deleteURL") + small(s.deleteURL) + linesep : "")
                + (Tools.notEmpty(s.location) ? lbl("location") + small(s.location) + linesep : "")
                + (Tools.notEmpty(s.getError()) ?  alertError(s.getError()) + linesep : "")
                + (Tools.notEmpty(s.getErrorDetail()) ?  alertError(s.getErrorDetail()) + linesep : "")
                + ((s.vars.size()>0) ? lbl("vars")+varsToHtml(s) + linesep : "")
                + ((includePartSummary && Tools.notBlank(partSummary)) ? lbl("part summary") + partSummary + linesep : "")
                + (includePayloads && Tools.notBlank(s.requestPayload) ? LINE + lbl("requestPayload") + LINE + CRLF + s.requestPayload + LINE : "")
                + (includePayloads && Tools.notBlank(s.getResult()) ? LINE + lbl("result") + LINE + CRLF + s.getResult() : "")
                + end;
        return res;
    }

    private String requestHeadersToHtml(List<String> hdrs) {
        StringBuffer sb = new StringBuffer();
        for (String hdr : hdrs) {
            sb.append("<span class='header'>").append(hdr).append("</span>");
        }
        return sb.toString();
    }

    private String requestHeadersToHtml(Map<String, String> headerMap) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            sb.append("<span class='header'>")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("</span>");
        }
        return sb.toString();
    }

    private String alertsToHtml(List<Alert> alerts) {
        StringBuffer buffer = new StringBuffer();
        String alertClass = "";
        for(Alert alert: alerts){
            switch (alert.level){
                case WARN:
                    alertClass = "AlertWarn";
                    break;
                case ERROR:
                    alertClass = "AlertError";
                    break;
            }
            buffer.append("<span class='"+alertClass+"'>")
                    .append(alert.level)  .append("<br />")
                    .append(alert.message).append("<br />")
                    .append(alert.context).append("<br />")
                    .append("</span>");
        }
        return buffer.toString();
    }

    private int alertsCount(List<Alert> alerts, LEVEL level) {
        int count = 0;
        for (Alert alert : alerts) {
            if (alert.level.equals(level)) {
                count++;
            }
        }
        return count;
    }

    private String varsToHtml(ServiceResult result){
        StringBuffer b = new StringBuffer();
        for (Map.Entry<String,String> entry: result.vars.entrySet()){
            b.append("<span class='vars'>")
             .append(entry.getKey())
             .append(": ")
             .append(entry.getValue())
             .append("<br />")
             .append("</span>");
        }
        return b.toString();
    }
}

