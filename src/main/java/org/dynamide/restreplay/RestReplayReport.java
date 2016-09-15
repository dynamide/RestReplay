package org.dynamide.restreplay;

import org.dynamide.interpreters.EvalResult;
import org.dynamide.interpreters.VarInfo;
import org.dynamide.util.XmlTools;
import org.dynamide.util.FileTools;
import org.dynamide.util.Tools;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.dynamide.interpreters.Alert;
import org.dynamide.interpreters.Alert.LEVEL;


/**  Format a report based on RestReplay ServiceResult object from a test group.
 * @author  Laramie Crocker
 */
public class RestReplayReport {
    public static final String INCLUDES_DIR = "_includes";

    protected static final String TOPLINKS_FILE = "_includes/toplinks.html";
    protected static final String EVAL_REPORT_LINKS_FILE = "_includes/eval-report-links.html";

    public    static final String POWERED_BY = "<div style='margin: 6px; margin-top: 20px; background-color: white; border-radius: 5px; padding: 10px; border: 1px solid blue; text-align: center;'><span style='font-size: 70%;'>powered by</span> <a href='http://dynamide.org/RestReplay/'>RestReplay</a></div>";
    protected static final String HTML_PAGE_END = POWERED_BY+"</body></html>";

    /* The LIVE_SECTION is a div that can be passed to the javascript functions which collapse and show things.
     *  Outside of the LIVE_SECTION, javascript functions will not select, so things in the Legend don't collapse just
     *  because the user collapsed all Headers, say.
     */
    protected static final String LIVE_SECTION_BEGIN = "<div id='LIVE_SECTION'>";
    protected static final String LIVE_SECTION_END = "</div>";


    protected static final String HTML_TEST_START = "<div class='TESTCASE'>";
    protected static final String HTML_TEST_END = "</div>";

    protected static final String GROUP_START = "<div class='TESTGROUP'>";
    protected static final String GROUP_END = "</div>";

    protected static final String RUNINFO_START = "<div class='RUNINFO'>";
    protected static final String RUNINFO_END = "</div>";


    protected static final String DIV_END = "</div>";

    protected static final String WRAP_START = "<div class='SUMMARY_WRAP'>";
    protected static final String WRAP_END = "</div>";

    protected static final String PRE_START = "<pre class='SUMMARY'>";
    protected static final String PRE_END = "</pre>";
    protected static final String BR = "<br />\r\n";
    protected static final String HR = "<br /><hr />\r\n";

    protected static final String DETAIL_HDR = "<a name='TestDetail'></a><h2 class='DETAIL_HDR'>Test Details</h2>";
    protected static final String DETAIL_LINESEP = "</td></tr>\r\n<tr><td>";
    protected static final String DETAIL_END = "</td></tr></table>";

    protected static final String EVAL_REPORT_HDR = "<a name='EvalReport'></a><h2 class='DETAIL_HDR'>Eval Report</h2>";

    protected static final String TOC_START = "<a name='SummaryTOC'></a><table border='1' class='TOC_TABLE'><tr><td colspan='8' class='TOC_HDR'>Summary</td></tr>"
                                              +"<tr><th>testID</th><th>method</th><th>code</th><th>time(ms)</th><th>status</th><th>warn</th><th>error</th><th>DOM</th></tr>\r\n";
    protected static final String TOC_LINESEP = "\r\n";
    protected static final String TOC_CELLSEP = "</td><td>";
    protected static final String TOC_END = "</table>";
    protected static final String TOC_CELLSTART = "\r\n<tr class='%s'><td class='%s'>";
    protected static final String TOC_CELLEND = "</td></tr>";

    protected static final String HDRBEGIN = "<span class='HEADERBLOCK'>";
    protected static final String HDREND = "</span>";

    private static final String SP = "&nbsp;&nbsp;&nbsp;";

    /** @param relativePathFromReportsDir is relative to the uri the master is served from, and in the current implementation, is the env so that each env can have equivalent output file directories.
     *  @param reportsDir is the physical location of the reports output directory where master index files, and environments (as directories), and also the db output if turned on.
     */
    public RestReplayReport(String reportsDir, String relativePathFromReportsDir) {
        this.reportsDir = reportsDir;
        this.relativePathFromReportsDir = relativePathFromReportsDir;
    }

    private String reportsDir = "";

    public String getReportsDir() {
        return reportsDir;
    }

    private String relativePathFromReportsDir = "";
    public String getRelativePathFromReportsDir(){
        return relativePathFromReportsDir;
    }

    protected static String formatCollapse(String myDivID, String linkText) {
        return formatCollapse(myDivID, linkText, "");
    }

    protected static String formatCollapse(String myDivID, String linkText, String subtitle) {
        return "<span class='payload-link' ID='"+myDivID+"_link"+"'><a href='javascript:;' onmousedown=\"toggleDiv('" + myDivID + "');\">" + linkText + "</a>"
              +"</span>"

              +"<div ID='" + myDivID + "' class='PAYLOAD' style='display:none'>"
              +( Tools.notBlank(subtitle)?"<div class='payload-subtitle' style='border: 1px solid black;'>"+subtitle+"</div>"
                :"");
    }

    private List<ServiceResult> reportsList = new ArrayList<ServiceResult>();

    private List<String> runInfo = new ArrayList<String>();

    public String getPage(String testdir, RestReplay restReplay, String testGroupID) throws IOException {
        String env = restReplay.getEnvID();
        String envTitle = Tools.notBlank(env) ? '['+env + "] " : "";
        String pageTitle = envTitle+"RestReplay testGroup: "+testGroupID;
        return formatPageStart(testdir, restReplay.getResourceManager(), pageTitle)
                + "<div class='REPORTTIME'><b>RestReplay</b> "+lbl(" run on")+" " + Tools.nowLocale() + "&nbsp;&nbsp;&nbsp;"+lbl("test group")+testGroupID+"&nbsp;&nbsp;<a href='"+restReplay.getRelToMasterURL()+"'>Back to Master</a>"+"</div>"
                + header.toString()
                + LIVE_SECTION_BEGIN
                + this.getRunInfoHTML()
                + BR
                + reportsListToTOC()
                + BR
                + DETAIL_HDR
                + getInclude(restReplay, TOPLINKS_FILE)
                + reportsListToString()
                + HR
                + evalReportToString(restReplay)
                + LIVE_SECTION_END
                +restReplay.getResourceManager().readResource("RestReplayReport.getPage.readFooter", "_includes/html-footer.html", "_includes/html-footer.html").contents
                + HTML_PAGE_END;
    }

    private String getInclude(RestReplay replay, String resourceName){
        try {
            ResourceManager.Resource rez = replay.getResourceManager().readResource("RestReplayReport.getInclude", resourceName, resourceName);
            if (rez.provider == ResourceManager.Resource.SOURCE.NOTFOUND) {
                System.out.println("ERROR reading resource("+resourceName+"): not found.");
                return "ERROR reading "+resourceName;
            } else {
                return rez.contents;
            }
        } catch (IOException ioe){
            System.out.println("ERROR reading resource("+resourceName+"): "+ioe);
            return "ERROR reading "+resourceName;
        }

    }

    private String reportsListToTOC(){
        int i = 0;
        for (ServiceResult serviceResult: reportsList) {
            TOC toc = new TOC();
            toc.tocID = i++;//tocID;
            toc.testID = serviceResult.testID;
            toc.time = serviceResult.time;
            toc.method = serviceResult.method;
            toc.isAutodelete = serviceResult.isAutodelete;
            if (serviceResult.isSUCCESS()) {
                toc.detail = ok(formatMutatorSUCCESS(serviceResult));
            } else {
                if (serviceResult.expectedFailure){
                    toc.detail = brown("EXPECTED");
                } else {
                    toc.detail = red("FAILURE");
                }

            }
            toc.warnings = serviceResult.alertsCount(LEVEL.WARN);
            toc.errors = serviceResult.alertsCount(LEVEL.ERROR);
            toc.domcheck = serviceResult.domcheck;
            toc.responseCode = serviceResult.responseCode;
            toc.isMutation = serviceResult.isMutation;
            toc.idFromMutator = serviceResult.idFromMutator;
            if (serviceResult.mutatorSkipped) {
                toc.children = "<span class='toc-warn'>Mutator Skipped</span>";
            } else if (serviceResult.mutatorSkippedByOpts){
                toc.children = "<span class='toc-info'>Mutator Skipped (opts)</span>";
            }  else {
                MutatorChildBlock block = formatMutatorChildrenBlock(serviceResult);
                serviceResult.mutationDetailBlockID = block.detailID;
                toc.children = block.tocHTML;
            }
            tocList.add(toc);
        }
        return getTOC("").toString();
    }

    public static String formatMutatorSUCCESS(ServiceResult serviceResult) {
        if (serviceResult.getChildResults().size() > 0) {
            int numGotExpected = 0;
            int numResults = 0;
            for (ServiceResult cr : serviceResult.getChildResults()) {
                numResults++;
                if (cr.gotExpectedResult()) {
                    numGotExpected++;
                }
            }
            return "SUCCESS +" + numGotExpected + '/' + numResults;
        } else {
            return "SUCCESS";
        }
    }

    private static class MutatorChildBlock {
        public String tocHTML = "";
        public String detailID = "";
        public String rowID = "";
    }

    private MutatorChildBlock formatMutatorChildrenBlock(ServiceResult serviceResult) {
        MutatorChildBlock block = new MutatorChildBlock();
        if (serviceResult.getChildResults().size() > 0) {
            int numGotExpected = 0;
            int numErrors = 0;
            int numWarnings = 0;
            int numResults = 0;
            for (ServiceResult childResult : serviceResult.getChildResults()) {
                numResults++;
                if (childResult.gotExpectedResult()) {
                    numGotExpected++;
                }
                numErrors += childResult.alertsCount(LEVEL.WARN);
                numWarnings += childResult.alertsCount(LEVEL.ERROR);
            }
            assert numResults == serviceResult.getChildResults().size();
            String rowid = "childresults_" + serviceResult.testGroupID+'_'+serviceResult.testID;
            //Don't change parentage of these tags, there is javascript that does el.parentElement.parentElement.
            block.tocHTML = "<span class='childResults' onclick='hideresults(\"" + rowid + "\", this);'>"
                    + "<table id='" + rowid + "' class='child-results-summary'>"
                    + "<tr>"
                    + "<td><span class='childstate'>hide</span></td>"
                    + "<td>" + numGotExpected + '/' + numResults + "</td>"
                    + "<td>" + numWarnings + "</td>"
                    + "<td>" + numErrors + "</td>"
                    + "</tr></table></span>";
            block.detailID = rowid;
            block.rowID = rowid;
        }
        return block;
    }

    private boolean seenAutodelete = false;

    private String reportsListToString(){
        StringBuffer buffer = new StringBuffer();
        ServiceResult leftoverServiceResult = null;
        Iterator<ServiceResult> it = reportsList.iterator();
        while ((leftoverServiceResult!=null) || it.hasNext()) {
            ServiceResult sr;
            if (leftoverServiceResult!=null){
                sr = leftoverServiceResult;
                leftoverServiceResult = null;
            } else {
                sr = it.next();
            }
            if (sr.getChildResults().size()>0) {
                ServiceResult mutatorParent = sr;
                buffer.append("<div id='mutation_parent_" + sr.testIDLabel + "'>");  //todo: there is a dot in the testIDLabel. Not sure what that will break (js, etc.)
                appendServiceResult(sr, buffer);
                buffer.append("<div id='" + sr.mutationDetailBlockID + "' class='mutation-detail-block'>");
                if (it.hasNext()) {
                    sr = it.next();
                    boolean keepGoing = true;
                    leftoverServiceResult = null;
                    while (keepGoing) {
                        keepGoing = (sr.getParent() == mutatorParent);
                        if (!keepGoing) break;
                        appendServiceResult(sr, buffer);
                        if (it.hasNext()) {
                            sr = it.next();
                            keepGoing = true;
                            if (sr.getParent() != mutatorParent) {
                                keepGoing = false;
                                leftoverServiceResult = sr;
                            }
                        } else {
                            break;
                        }
                    }
                }
                buffer.append("</div>");
                buffer.append("</div>");
            } else {
                if (sr.isAutodelete){
                    if (!seenAutodelete) {
                        seenAutodelete = true;
                        buffer.append("<div class='autodelete-header'><b>Autodeleted</b></div>");
                    }
                }
                appendServiceResult(sr, buffer);
            }
        }
        return buffer.toString();
    }

    public static String dotdotdot(String value, int newlen){
        if (value.length()>newlen){
            value = value.substring(0, newlen) + "...";
        }
        return value;
    }

    public static String dotdotdot(String value){
        return dotdotdot(value, RunOptions.MAX_CHARS_FOR_REPORT_LEVEL_SHORT);
    }

    private static void accumulateVars(Map<String,List<VarInfo>> accumulator, Map<String,List<VarInfo>> vars) {
        for (String key: vars.keySet()){
            List<VarInfo> destlist = accumulator.get(key);
            List<VarInfo> srclist = vars.get(key);
            if (destlist==null){
                destlist = new ArrayList<VarInfo>();
            }
            destlist.addAll(srclist);
            accumulator.put(key, destlist);
        }
    }

    private static String formatVarsReport(Map<String,List<VarInfo>> vars){
        StringBuilder sb = new StringBuilder();
        StringBuilder sbImports = new StringBuilder();
        for (Map.Entry<String,List<VarInfo>> entrySet: vars.entrySet()){
            String name = entrySet.getKey();
            if (name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")){
                List<VarInfo> list = entrySet.getValue();
                sb.append("<div class='evalReportVarName'>")
                  .append(name)
                  .append(" &nbsp;&nbsp;&nbsp;<i class='SMALL'>["+list.size()+"]</i> ")
                  .append("</div>");
            } else {
                sbImports.append("<div class='evalReportReference'>")
                  .append(dotdotdot(name, 35))
                  .append("</div>");
            }
        }
        String v="", r="";
        if (sb.length()>1) {
            v = "<span class='SMALL'>vars:</span><br />" + sb.toString() + "</span>";
        }
        if (sbImports.length()>1){
            r = "<span style=''><span class='SMALL'>references:</span><br />"+sbImports.toString()+"</span>";
        }
        return v + " "+r;
    }


    public String evalReportToString(RestReplay replay){
        if (replay.getRunOptions().evalReportLevel.equals(RunOptions.EVAL_REPORT_LEVEL.NONE)){
            return "<p>Eval Report is off. Set runOptions::evalReportLevel to SHORT or ALL to enable.</p>";
        }
        StringBuffer b = new StringBuffer();
        b.append(EVAL_REPORT_HDR);
        b.append(getInclude(replay, EVAL_REPORT_LINKS_FILE));
        b.append("<div class='evalReport'>");
        Map<String,List<VarInfo>> accumulator = new HashMap<String, List<VarInfo>>();
        for (EvalResult evalResult: replay.evalReport){
            String trimmedExpression = escape(evalResult.expression);
            String trimmedResult = evalResult.getResultString();
            trimmedResult = escape(trimmedResult);

            switch (replay.getRunOptions().evalReportLevel) {
                case ALL:
                    break;
                case SHORT:
                    trimmedExpression = dotdotdot(trimmedExpression, RunOptions.MAX_CHARS_FOR_REPORT_LEVEL_SHORT);
                    trimmedResult = dotdotdot(trimmedResult, RunOptions.MAX_CHARS_FOR_REPORT_LEVEL_SHORT);
                    break;
            }

            String nestClass = "evalReport-level0";
            if (evalResult.nestingLevel == 1){
                nestClass = " evalReport-level1";
            }
            Map<String,List<VarInfo>> vars = evalResult.vars;
            if (evalResult.isDummy) {
                b.append("<a name='"+evalResult.testIDLabel+"_EvalReport' ></a>");
                b.append("<div><div class='evalReportTestIDLabel'>"+evalResult.testIDLabel+"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a style='font-size:50%;' href='#"+evalResult.testIDLabel+"'>go to test</a></div></div>");
                b.append("  ");
                b.append("<div class='evalReportTestIDVars'>"+formatVarsReport(vars)+"</div>");
                accumulateVars(accumulator, vars);
            } else {
                String evalReportValueClass = "evalReportValue";
                if (evalResult.isError){
                    evalReportValueClass = "evalReportValueERROR";
                }
                b.append("<div class='evalReportRow "+nestClass+"'>")
                 .append("<span class='evalReportContext'>")
                 .append(evalResult.context)
                 .append("</span><span class='evalReportExpression'>")
                 .append(escape(trimmedExpression))
                 .append("</span><span class='"+evalReportValueClass+"'>")
                 .append(trimmedResult)
                 .append("</span> </div>");
            }
        }
        b.append("<div class='evalReportSummaryLabel'>Accumulated vars and references for TestGroup</div>");
        b.append("<div class='evalReportTestIDVars'>"+formatVarsReport(accumulator)+"</div>");
        b.append("</div>");
        return b.toString();
    }

    private String methodBox(String method){
        if (method.equalsIgnoreCase("GET")) {
            return "<span class='http-get'> GET</span>";
        } else if (method.equalsIgnoreCase("POST")){
            return "<span class='http-post'> POST</span>";
        } else if (method.equalsIgnoreCase("PUT")){
            return "<span class='http-put'> PUT</span>";
        } else if (method.equalsIgnoreCase("DELETE")){
            return "<span class='http-delete'> DELE</span>";
        }
        return "<span class='http-method'>"+method+"</span>";
    }


    public String getTOC(String reportName) {

        String relativeReportName = reportName; //extractRelPath(reportName).filenameOnly;

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
            String cssClassTR = toc.isMutation ? "mutationTR" : "";
            String cssClassTD = toc.isMutation ? "mutationTD" : "";
            String sep = String.format(TOC_LINESEP, cssClassTR, cssClassTD);
            String rowstart = String.format(TOC_CELLSTART, cssClassTR, cssClassTD);
            String autodeleteBullet = toc.isAutodelete ? " &#10034; " : "";  // or just &bull;
            if (count>0){tocBuffer.append(sep);}
            count++;
            //tocBuffer.append("<a href='" + reportName + "#TOC" + toc.tocID + "'>" + toc.testID/*+toc.idFromMutator*/ + "</a> ")
            tocBuffer.append(rowstart);
            tocBuffer.append(autodeleteBullet)
                     .append("<a href='" + relativeReportName + "#"+ toc.testID /*"#TOC" +toc.tocID*/ + "'>" + toc.testID/*+toc.idFromMutator*/ + "</a> ")
                     .append((toc.children))
                     .append(TOC_CELLSEP)
                     .append(methodBox(toc.method))
                     .append(TOC_CELLSEP)
                     .append(toc.responseCode)
                     .append(TOC_CELLSEP)
                     .append(toc.time)
                     .append(TOC_CELLSEP)
                     .append(toc.detail)
                     .append(TOC_CELLSEP)
                     .append(tocWarn(toc.warnings))
                     .append(TOC_CELLSEP)
                     .append(tocError(toc.errors))
                     .append(TOC_CELLSEP)
                     .append("<span class='summary-domcheck'>" + toc.domcheck + "</span>");
            tocBuffer.append(TOC_CELLEND);
        }
        tocBuffer.append(TOC_END);
        tocBuffer.append(BR);
        if (Tools.notBlank(reportName)) {
        } else {
            // We are generating a single report file, so all links are relative to this file, and we should have the TOPLINKS which allow things like showAllPayloads..
            //20150205-moved.   tocBuffer.append(BR).append(TOPLINKS).append(BR);
        }
        return tocBuffer.toString();
    }

    public void addRunInfo(String text) {
        this.runInfo.add(text);
    }

    public String getRunInfo(){
        StringBuilder b = new StringBuilder();
        for (String s: runInfo){
            b.append(s).append("\r\n");
        }
        return b.toString();
    }
    public String getRunInfoHTML(){
        StringBuilder b = new StringBuilder();
        b.append(RUNINFO_START);
        for (String text: runInfo){
            b.append(text).append("\r\n");
        }
        b.append(RUNINFO_END);
        return b.toString();
    }

    public void clearRunInfo(){
        this.runInfo.clear();
        header.groupID = "";
        header.controlFile = "";
        header.failureMessage = "";
        header.failure = false;
    }

    public static class Header {
        public boolean failure = false;
        public String failureMessage = "";
        public String groupID="";
        public String controlFile;
        public String reportNameLink;
        public String comment = "";
        public int index = -1;
        public String anchor = "";
        public String runID = "";
        public String toString(){
           StringBuffer sb = new StringBuffer();
            sb.append("<a name='"+anchor+"'></a>");
            sb.append(GROUP_START);
            String groupIDLink = (Tools.isEmpty(reportNameLink))
                                 ? groupID
                                 : "<a href='"+reportNameLink+"'>"+groupID+"</a>";
            sb.append(lbl("Test Group")).append(groupIDLink).append(SP)
                     .append(lbl("Control File")).append(controlFile);
            if (Tools.notBlank(runID)) {
                sb.append(SP).append(lbl("runID")).append(runID);
            }
            if (failure) {
                sb.append("<div class='group-failure'>"+failureMessage+"</div>");//it is white-space: pre, so \r\n will be OK.
            }

            sb.append(GROUP_END);
            if (Tools.notBlank(comment)){
                sb.append("<div class='comment'>"+comment+"</div>");
            }
            return sb.toString();
        }
        public static Header findInList(List<Header>list, String findID){
            if(list==null){
                return null;
            }
            for(Header hdr:list){
              if(hdr!=null&&hdr.groupID.equals(findID)){
                  return hdr;
              }
            }
            return null;
        }
    }
    private Header header = new Header();

    public Header addTestGroup(String groupID, String controlFile, String runID, String comment, String anchor, int idx) {
        header.groupID = groupID;
        header.controlFile = controlFile;
        header.comment = comment;
        header.anchor = anchor;
        header.index = idx;
        header.runID = runID;
        return header;
    }

    public void addFailure(String msg){
        header.failureMessage = msg;
        header.failure = true;
    }

    private int divID = 0;

    public void addTestResult(ServiceResult serviceResult) {
        reportsList.add(serviceResult);
    }

    private static void appendTestAnchor(StringBuffer buffer, int tocID, ServiceResult serviceResult){
        buffer.append("<a name='TOC" + tocID + "'></a>");
        buffer.append("<a name='" + serviceResult.testID + "'></a>");
        buffer.append("<a name='" + serviceResult.testIDLabel + "'></a>"); //belt and suspenders.  EvalReport knows context as IDLabel, (with group name).
    }

    private void appendServiceResult(ServiceResult serviceResult, StringBuffer buffer){
        int tocID = divID++;
        appendTestAnchor(buffer, tocID, serviceResult);
        buffer.append(HTML_TEST_START);
        String cssClass = serviceResult.isMutation
                ?"payloads mutation"
                :"payloads";
        buffer.append("<div class='"+cssClass+"'>");
        buffer.append(formatSummary(serviceResult, tocID));
        buffer.append(formatPayloads(serviceResult, tocID));
        buffer.append("<span class='payload-link'><a href='#"+serviceResult.testIDLabel+"_EvalReport'>EvalReport</a></span>");
        buffer.append("</div>");
        buffer.append(HTML_TEST_END);
    }

    public static class TOC {
        public int tocID;
        public String testID;
        public String detail;
        public String method;
        public int warnings = 0;
        public int errors = 0;
        public long time = 0;
        public int responseCode = 0;
        public boolean isMutation = false;
        public String idFromMutator = "";
        public boolean isAutodelete = false;
        public String children = "";
        public String domcheck = "";
    }

    private List<TOC> tocList = new ArrayList<TOC>();

    public static String formatPageStart(String testdir, ResourceManager rm,
                                         String pageTitle) throws IOException {

        String script = rm.readResource("formatPageStart", INCLUDES_DIR + "/reports-include.js", testdir + "/" + INCLUDES_DIR + "/reports-include.js").contents;
        String style  = rm.readResource("formatPageStart", INCLUDES_DIR + "/reports-include.css", testdir + "/" + INCLUDES_DIR + "/reports-include.css").contents;
        //String script = FileTools.readFile(testdir, INCLUDES_DIR + "/reports-include.js");
        //String style = FileTools.readFile(testdir, INCLUDES_DIR + "/reports-include.css");
        return "<html><head><title>"+pageTitle+"</title><script type='text/javascript'>\r\n"
                + script
                + "\r\n</script>\r\n<style>\r\n"
                + style
                + "\r\n</style></head><body>";
    }

    public static class FilePath {
        public String relPath = "";
        public String filenameOnly = "";
    }
    private FilePath extractRelPath(String reportName){
        //System.out.println("=====extractRelPath========>>> reportName:"+reportName);
        FilePath result = new FilePath();
        File f = new File(reportName);
        result.filenameOnly = f.getName();
        result.relPath = FileTools.getFilenamePath(f.getPath());
        //System.out.println("=====extractRelPath========>>> result.relPath:"+result.relPath);
        //System.out.println("=====extractRelPath========>>> result.filenameOnly:"+result.filenameOnly);
        return result;
    }

    public File saveReport(String testdir, String reportsDir, String reportName, RestReplay restReplay, String testGroupID) {
        try {
            FilePath filePath = extractRelPath(reportName);
            String relPath = filePath.relPath;
            String detailDirectory = FileTools.join(reportsDir, relPath);
            File testfn = new File(detailDirectory);
            String canonical = testfn.getCanonicalPath();  //try to get the full name to look like /Users/vcrocla or /home/jenkins/RestReplay/reports or somesuch.

            ReportMasterLocations reportMaster = new ReportMasterLocations();
            reportMaster.directory = detailDirectory;
            reportMaster.relname = reportName;
            reportMaster.fullname = canonical;
            reportMaster.relPath = filePath.relPath;
            reportMaster.filenameOnly = filePath.filenameOnly;

            ReportDetailLocations reportDetail = new ReportDetailLocations();
            reportDetail.relativeDirectory = detailDirectory;
            reportDetail.filenameOnly = filePath.filenameOnly;
            reportDetail.reportName = reportName;
            reportDetail.uri = Tools.glue(this.relativePathFromReportsDir, reportName);

            if (reportsList!=null){
                for (ServiceResult sr: reportsList){
                    sr.reportDetail = reportDetail;
                    sr.reportMaster = reportMaster;
                }
            }

            File resultFile = FileTools.saveFile(detailDirectory, filePath.filenameOnly, this.getPage(testdir, restReplay, testGroupID), true);
            if (resultFile != null) {
                String resultFileName = resultFile.getCanonicalPath();
                //System.out.println("RestReplay summary report output:"
                //                  +"\n detailDirectory:"+detailDirectory
                //                  +"\n reportFilenameNameOnly:"+filePath.filenameOnly
                //                  +"\n testdir:"+testdir
                //                  +"\n reportsDir:"+reportsDir
                //                  +"\n reportName:"+reportName);
                System.out.println("RestReplay summary report: " + resultFileName+"\r\n");
                return resultFile;
            }
        } catch (Exception e) {
            System.out.println("ERROR saving RestReplay report in testdir: " + reportsDir + " reportName: " + reportName + " error: " + e+Tools.errorToString(e, true));
        }
        return null;
    }

    public static class ReportDetailLocations {
        public String relativeDirectory = "";
        public String filenameOnly = "";
        public String uri = "";
        public String reportName = "";
    }
    public static class ReportMasterLocations {
        public String directory;
        public String relname;
        public String fullname = "";
        public String relPath = "";
        public String filenameOnly = "";
        public String toString(){
            return    "{directory:"+directory
                    +", relname:"+relname
                    +", fullname:"+fullname
                    +", relPath:"+relPath
                    +", filenameOnly:"+filenameOnly
                    +"}";
        }
    }
    public static ReportMasterLocations calculateMasterReportRelname(String reportsDir,
                                                                     String localMasterFilename,
                                                                     String envID){
        File f = new File(localMasterFilename);
        String relPath = FileTools.getFilenamePath(f.getPath());

        String relPathNameComponent = Tools.notBlank(relPath)
                ? FileTools.safeFilename(relPath)+'.'
                : "";
        String masterFilenameNameOnly = "index."
                +(Tools.notBlank(envID)?envID+'.':"")
                +relPathNameComponent
                + f.getName();
        if (masterFilenameNameOnly.endsWith(".xml")){
            masterFilenameNameOnly = masterFilenameNameOnly.substring(0, masterFilenameNameOnly.length()-4);
        }
        String masterFilenameNameFull = masterFilenameNameOnly+".html";
        String masterFilenameDirectory =  reportsDir;// this doesn't get propogated to control file reports. :(   Tools.join(reportsDir, masterFilenameNameOnly);;
        ReportMasterLocations tupple = new ReportMasterLocations();
        tupple.directory = masterFilenameDirectory;
        tupple.relname = masterFilenameNameFull;
        tupple.filenameOnly = f.getName();
        return tupple;
    }

    /**
     * @param localMasterFilename should be a local filename for the index of each restReplay master control file, e.g. objectexit.xml
     *                            so what gets written to disk will be something like index.objectexit.xml.html . The actual filename will be available from
     *                            the returned File object if successful.
     * @return File if successful, else returns null.
     */
    public static File saveIndexForMaster(String testdir,
                                          String reportsDir,
                                          String localMasterFilename,
                                          List<String> reportsList,
                                          String envID,
                                          Map<String,Object> masterVars,
                                          List<Header> testGroups,
                                          Master master,
                                          List<List<ServiceResult>> list,
                                          List<String> masterAlertStrings) {

        ReportMasterLocations tupple = calculateMasterReportRelname(reportsDir, localMasterFilename, envID);
        String relname = Tools.glue(tupple.directory, tupple.relname);
        String canonical = relname;  //default to the glued name, which will look like "./reports/...."
        try {
            File testfn = new File(relname);
            canonical = testfn.getCanonicalPath();  //try to get the full name to look like /Users/vcrocla or /home/jenkins/RestReplay/reports or somesuch.
            tupple.fullname = canonical;
        } catch (IOException e){
            System.out.println("ERROR: could not determine canonical path of report: "+e);
        }

        //System.out.println("RestReplay summary report output ***********>>>(saveIndexForMaster):"
        //                +"\n testdir:"+testdir
        //                +"\n reportsDir:"+reportsDir
        //                +"\n localMasterFilename:"+localMasterFilename
        //                +"\n relPath:"+relPath
        //                +"\n masterFilenameNameOnly:"+masterFilenameNameOnly);

        try {
            int numSUCCESS = 0;
            int numFAILURE = 0;
            int numTests = 0;
            StringBuffer masterSummary = new StringBuffer();
            for (List<ServiceResult>srlist:  list){
                for (ServiceResult sr: srlist){
                    numTests++;
                    if (sr.isSUCCESS()){
                        numSUCCESS++;
                    } else {
                        if (sr.expectedFailure) {
                            numSUCCESS++;
                        } else {
                            numFAILURE++;
                        }
                    }
                    if (master.getRunOptions().dumpMasterSummary){
                        masterSummary.append(sr.tiny()).append('\n');
                    }
                    sr.reportMaster = tupple;
                }
            }

            String masterSummaryLine = "TESTS: "+numTests+" SUCCESS: "+numSUCCESS + " FAILURE: "+numFAILURE;
            String masterSummaryLineHTML = smallblack("TESTS: ")+numTests+' '+ok("SUCCESS:")+numSUCCESS + ' '+(numFAILURE>0?red("FAILURE:"):noerror("FAILURE:")) +numFAILURE;

            String pageTitle = "RestReplay "+localMasterFilename+(Tools.notBlank(envID)?" ("+envID+")":"");
            StringBuffer sb = new StringBuffer(formatPageStart(testdir, master.getResourceManager(), pageTitle));
            String dateStr = Tools.nowLocale();
            sb.append("<div class='REPORTTIME'><b>RestReplay</b> " + lbl(" run on") + " " + dateStr + "<span class='header-label-master'>Master:</span>" + localMasterFilename + "</div>");

            sb.append("<div class='toc-toc'><b>Totals</b>");
            sb.append("<br />"+masterSummaryLineHTML);
            sb.append("</div>");

            sb.append("<br />");

            sb.append("<div class='masterVars'>")
              .append("<span class='LABEL'>environment:</span> <span class='env'>"+envID+"</span><br />")
              .append(formatMasterVars(masterVars)).append("</div>");


            Master.ScriptEventResult onFailureSummary = master.fireOnFailureSummary();
            String sresultFS = onFailureSummary.result;
            if (Tools.notBlank(sresultFS)){
                sb.append("<br /><div class='toc-toc'><b>FAILURE Report</b>");
                sb.append(sresultFS);
                sb.append("</div>");
            }



            Master.ScriptEventResult onMasterSummaryTable = master.fireOnMasterSummaryTable();
            String sresultMT = onMasterSummaryTable.result;
            if (Tools.notBlank(sresultMT)){
                sb.append("<br /><div class='toc-toc'><b>Master Summary</b>");
                sb.append(sresultMT);
                sb.append("</div>");
            }

            Master.ScriptEventResult onAnalysis = master.fireOnAnalysis();
            String sresultAn = onAnalysis.result;
            if (Tools.notBlank(sresultAn)){
                sb.append("<br /><div class='toc-toc'><b>Master Analysis</b>");
                sb.append(sresultAn);
                sb.append("</div>");
                String foo = tupple.toString();
                String filenameOnly = tupple.filenameOnly;
                if (filenameOnly.endsWith(".xml")){
                    filenameOnly = filenameOnly.substring(0, filenameOnly.length()-4);
                }
                String jsonName = "stats."+master.getEnvID()+'.'+filenameOnly+".json";
                File fSaved = FileTools.saveFile(tupple.directory, jsonName, sresultAn, true);
                System.out.println("\r\nAnalysis file saved (tupple: "+foo+") for Master Run: "+fSaved.getCanonicalPath());
            }

            if (masterAlertStrings!=null&&masterAlertStrings.size()>0) {
                sb.append("<br /><div class='toc-toc'><b>Alerts in master file</b><br />");
                for (String oneAlertLine : masterAlertStrings) {
                    sb.append(oneAlertLine).append("<br />");
                }
                sb.append("</div>");
            }


            sb.append("<br /><br /><hr />");
            sb.append("<h2>All Tests by TestGroup (execution order)</h2>");

            for (String oneToc : reportsList) {
                sb.append(oneToc);
                sb.append("<hr />");
            }


            sb.append("<br /><br /><hr />");
            sb.append("<h2>Test Groups Run</h2>");
            sb.append("<div class='toc-toc'><table border='0'>");
            for (Header testGroup: testGroups) {
                sb.append("<tr><td><small>"+testGroup.controlFile+"</small></td><td><a href='#"+testGroup.anchor+"'>"+testGroup.groupID+"</a></td></tr>");
            }
            sb.append("</table></div>");


            sb.append("<hr />");
            sb.append("<h2>Run Options</h2>");
            sb.append("<div class='run-options'>"+master.getRunOptions().toHTML()+"</div>");


            sb.append("<br /><br /><hr />");
            if (master.getRunOptions().reportResourceManagerSummary){
                sb.append("<h2>ResourceManager Summary</h2>");
                sb.append(master.getResourceManager().formatSummary());
            } else {
                sb.append("<p>ResourceManager Summary off. To see summary, set reportResourceManagerSummary=\"true\" in master::runOptions or runOptions.xml.</p>");
            }

            Master.ScriptEventResult onSummary = master.fireOnSummary();
            String sresult = onSummary.result;
            if (Tools.notBlank(sresult)){
                sb.append("<hr />");
                sb.append("<h2>Master onSummary event results</h2>");
                sb.append("<div ID='onSummaryScriptResult' class='white-box'>");
                sb.append("<div class='payload-subtitle' style='border: 1px solid black;'>"+onSummary.event.name+"</div>");
                sb.append(sresult);
                sb.append("</div>");
            }

            sb.append(HTML_PAGE_END);

            if (master.getRunOptions().dumpMasterSummary) {
                System.out.println("\nMaster Summary:\n");
                System.out.println(masterSummary+"\n");
            }

            System.out.println("====|\r\n====|  Master Report Index:       "+canonical+"\r\n====|\n\n");

            System.out.println(masterSummaryLine);


            return FileTools.saveFile(tupple.directory, tupple.relname, sb.toString(), true);
        } catch (Exception e) {
            System.out.println("ERROR saving RestReplay report index: in  testdir: " + reportsDir + "localMasterFilename: " + localMasterFilename +" directory:"+tupple.directory+ " masterFilename: " + tupple.relname + " list: " + reportsList + " error: " + e);
            return null;
        }
    }

    /**
     * @param localMasterFilename should be a local filename for the index of each restReplay master control file, e.g. objectexit.xml
     *                            so what gets written to disk will be something like index.objectexit.xml.html . The actual filename will be available from
     *                            the returned File object if successful.
     * @return File if successful, else returns null.
     */
    public static File saveIndexNoMaster( ResourceManager rm,
                                          String testdir,
                                          String reportsDir,
                                          String localMasterFilename,
                                          List<String> reportsList) {

        ReportMasterLocations tupple = calculateMasterReportRelname(reportsDir, localMasterFilename, "");
        try {
            String dateStr = Tools.nowLocale();
            String pageTitle = "RestReplay "+localMasterFilename;
            StringBuffer sb = new StringBuffer();
            sb.append(formatPageStart(testdir, rm, pageTitle))
              .append("<html><head><title>"+pageTitle+"</title></head><body>")
              .append("<div class='REPORTTIME'><b>RestReplay</b> " + lbl(" run on") + " " + dateStr + "<span class='header-label-master'>Master:</span>" + localMasterFilename + "</div>")
              .append("<div class='masterVars'><span class='LABEL'>environment:</span><br />")
              .append(" <p class='SMALLBLACK'>Report was generated with no <b>-master</b>, so <b>-env</b> is not used.</p>")
              .append("</div>");
            for (String oneToc : reportsList) {
                sb.append(oneToc);
                sb.append("<hr />");
            }
            sb.append(HTML_PAGE_END);
            System.out.println("====|\r\n====|  Master Report Index:       "+Tools.glue(tupple.directory, tupple.relname)+"\r\n====|\n\n");
            return FileTools.saveFile(tupple.directory, tupple.relname, sb.toString(), true);
        } catch (Exception e) {
            System.out.println("ERROR saving RestReplay report index: in  testdir: " + reportsDir + "localMasterFilename: " + localMasterFilename +" directory:"+tupple.directory+ " masterFilename: " + tupple.relname + " list: " + reportsList + " error: " + e);
            System.out.println(Tools.getStackTrace(e));
            return null;
        }
    }
    protected static String formatMasterVars(Map<String, Object> masterVars) {
        if (masterVars==null){
            return "";
        }
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, Object> entry : masterVars.entrySet()) {
            buffer.append("\r\n<div class='varslist'>")
                  .append(entry.getKey()).append(": ").append(entry.getValue())
                  .append("</div>");
        }
        return buffer.toString();
    }

    /** Note: anchor tag is spit out by appendTestAnchor() before this because this gets wrapped in divs by appendServiceResult() */
    public String formatSummary(ServiceResult serviceResult, int tocID) {
        StringBuffer fb = new StringBuffer();
        boolean includePartSummary = true;
        fb.append(detail(serviceResult, false, includePartSummary, DETAIL_LINESEP, DETAIL_END, tocID));
        return fb.toString();
    }

    public static String formatPayloads(ServiceResult serviceResult, int tocID) {
        StringBuffer buffer = new StringBuffer();
        ServiceResult.PRETTY_FORMAT respType = serviceResult.contentTypeFromResponse();
        appendPayload(serviceResult, buffer, serviceResult.requestPayloadsRaw, respType, "REQUEST (raw) ", "REQUESTRAW" + tocID, serviceResult.requestPayloadFilename);
        appendPayload(serviceResult, buffer, safeJSONToString(serviceResult.requestPayload), respType, "REQUEST (expanded)", "REQUEST" + tocID, "", false);
        if (serviceResult.getRunOptions().reportResponseRaw) {
            appendPayload(serviceResult, buffer, serviceResult.getResult(), respType, "RESPONSE (raw)", "RESPONSERAW" + tocID, "");
        } else if (respType.equals(ServiceResult.PRETTY_FORMAT.HTML)){
            appendPayload(serviceResult, buffer, serviceResult.getResult(), respType, "RESPONSE (html)", "RESPONSEHTML" + tocID, "", false);
        } else {
            appendPayload(serviceResult, buffer, serviceResult.getResult(), respType, "RESPONSE (unknown)", "RESPONSERAW" + tocID, "", false);
        }
        appendPayload(serviceResult, buffer, serviceResult.prettyJSON, respType, "RESPONSE", "RESPONSE" + tocID, "", false);
        if (serviceResult.getRunOptions().reportPayloadsAsXML) {
            if (Tools.notBlank(serviceResult.getXmlResult())) {
                appendPayload(serviceResult, buffer, serviceResult.getXmlResult(), ServiceResult.PRETTY_FORMAT.XML, "RESPONSE (as xml)", "RESPONSEXML" + tocID, "", false);
            }
        }
        if (Tools.notBlank(serviceResult.expectedContentRaw)) {
            appendPayload(serviceResult, buffer, serviceResult.expectedContentRaw, respType, "EXPECTED (raw)", "EXPECTEDraw" + tocID, serviceResult.expectedResponseFilenameUsed);
            appendPayload(serviceResult, buffer, serviceResult.expectedContentExpanded, respType, "EXPECTED (expanded)", "EXPECTEDexpanded" + tocID, "");
            if (serviceResult.getRunOptions().reportPayloadsAsXML) {
                appendPayload(serviceResult, buffer, serviceResult.expectedContentExpandedAsXml, ServiceResult.PRETTY_FORMAT.XML, "EXPECTED (as xml)", "EXPECTEDasxml" + tocID, "");
            }
            if (!serviceResult.expectedContentExpandedWasJson) {
                appendPayload(serviceResult, buffer, ServiceResult.payloadXMLtoJSON(serviceResult.expectedContentExpanded), ServiceResult.PRETTY_FORMAT.JSON, "EXPECTED (as JSON)", "EXPECTEDJSON" + tocID, "");
            }
        }
        String partSummary = serviceResult.partsSummaryHTML(true);//true for detailed.
        appendPayload(serviceResult, buffer, partSummary, ServiceResult.PRETTY_FORMAT.NONE, "DOM Comparison", "DOMComparison" + tocID, "");
        return buffer.toString();
    }
    private static String safeJSONToString(String in){
        try {
            String out = ServiceResult.prettyPrintJSON(in);
            if (out == null) {
                return in;
            }
            return out;
        } catch (Throwable t){
            return in;
        }
    }

    protected static void appendPayload(ServiceResult serviceResult, StringBuffer fb, String payload, ServiceResult.PRETTY_FORMAT format, String title, String theDivID, String subtitle) {
        appendPayload(serviceResult, fb, payload, format, title, theDivID, subtitle, true);
    }


    protected static void appendPayload(ServiceResult serviceResult, StringBuffer fb, String payload, ServiceResult.PRETTY_FORMAT format, String title, String theDivID, String subtitle, boolean usePRE) {
        if (Tools.notBlank(payload)) {
            //fb.append(BR+title+":"+BR);
            ServiceResult.Payload pl = new ServiceResult.Payload();
            pl.body = payload;
            pl.format = format;
            pl.id = theDivID;
            pl.title = title;
            pl.subtitle = subtitle;
            pl.usePRE = usePRE;
            serviceResult.payloads.put(theDivID, pl);

            try {
                String pre_start,
                       pre_end;
                if (usePRE){
                    pre_start = PRE_START;
                    pre_end = PRE_END;
                } else {
                    pre_start = WRAP_START;
                    pre_end = WRAP_END;
                }
                switch (format) {
                    case XML:
                        //System.out.println("PAYLOAD:"+payloadJSONtoXML(payload));
                        //JSONObject json = new JSONObject(payload);
                        //String xml = XML.toString(json);
                        //System.out.println("PAYLOAD xml:"+xml);
                        //System.out.println("PAYLOAD raw:" + payload);
                        String pretty = prettyPrint(payload);
                        fb.append(formatCollapse(theDivID, title, subtitle));  //starts a div.
                        fb.append(pre_start);
                        fb.append(escape(pretty));
                        fb.append(pre_end);
                        fb.append(DIV_END);//ends that div.
                        break;
                    case JSON:
                    case NONE:
                        //System.out.println("PAYLOAD raw:" + payload);
                        fb.append(formatCollapse(theDivID, title, subtitle));  //starts a div.
                        fb.append(pre_start);
                        fb.append(escape(payload));
                        fb.append(pre_end);
                        fb.append(DIV_END); //ends that div.
                        break;
                    case HTML:
                        fb.append(formatCollapse(theDivID, title, subtitle));  //starts a div.
                        fb.append(pre_start);
                        fb.append(escape(payload));
                        fb.append(pre_end);
                        fb.append(DIV_END); //ends that div.
                        break;
                    default:
                        System.err.println("ERROR: Unhandled enum type in RestReplayReport.appendPayload(" + title + "): " + format.toString());
                }
            } catch (Exception e) {
                //System.out.println(Tools.getStackTrace());
                fb.append("<b>" + title + "</b>");
                fb.append(BR);
                fb.append("<div style='border:1px solid black; background-color: white;'><font color='red'>ERROR_IN_APPEND_PAYLOAD: ("
                             + e.getClass().getName() + ':' + escape(e.getLocalizedMessage()) + ")</font> ");
                fb.append(BR);
                fb.append("payload raw: " + escape(payload));
                fb.append(BR);
                fb.append("payload:"+escape(payload));
                fb.append("</div>");
            }
        }
    }

    public static String escape(String source) {
        try {
            return Tools.searchAndReplace(source, "<", "&lt;");
        } catch (Exception e) {
            return "ERROR escaping requestPayload" + e;
        }
    }

    private static String prettyPrint(String rawXml) throws Exception {
        Document document = DocumentHelper.parseText(rawXml);
        return XmlTools.prettyPrint(document, "    ");
    }

    private static final String LINE = "<hr />\r\n";
    private static final String CRLF = "<br />\r\n";

    protected static String red(String label) {
        return "<span class='ERROR'>" + label + "</span> ";
    }

    protected static String brown(String label) {
        return "<span class='EXPECTED'>" + label + "</span> ";
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

    protected static String smallblack(String label) {
        return "<span class='SMALLBLACK'>" + label + "</span> ";
    }

    protected static String noerror(String label) {
        return "<span class='NOERROR'>" + label + "</span> ";
    }


    protected String units(String label) {
        return "<span class='LABEL'>" + label + "</span> ";
    }

    protected String alertError(String msg) {
        return "<span class='AlertError'>" + msg + "</span> ";
    }

    public static String makeShort(String theComment, String blockID) {
        if (Tools.isBlank(theComment)){
            return "";
        }
        String result = theComment;
        if (theComment.length() > RunOptions.MAX_CHARS_FOR_COMMENT_SHORT) {
            int endLen = Math.min(theComment.length(), RunOptions.MAX_CHARS_FOR_COMMENT_SHORT);
            result =     "<span class='comment-short' id='comment_short_"+blockID+"'>"
                        + theComment.substring(0, endLen)
                        +" <nobr><a href=\"javascript:showBlock('#LIVE_SECTION #comment_full_"+blockID+"','block');hideBlock('#LIVE_SECTION #comment_short_"+blockID+"')\">more &raquo;</a></nobr>"
                        +"</span>"
                        +"<span class='comment-full' id='comment_full_"+blockID+"'>"
                        + theComment
                        +" <nobr><a href=\"javascript:showBlock('#LIVE_SECTION #comment_short_"+blockID+"','inline');hideBlock('#LIVE_SECTION #comment_full_"+blockID+"')\">&laquo; less</a></nobr>"
                        +"</span>";
        } else {
                result = "<span class='comment-all'>"
                        + theComment
                        +"</span>";
        }
        return result;
    }

    public static String scriptifyName(String name){
        name = name.replace('.','_');
        return name;
    }

    public String detail(ServiceResult s, boolean includePayloads, boolean includePartSummary,String linesep, String end, int tocID) {
        String mutationClass = s.isMutation ? " mutation" : "";
        String autodeleteClass = s.isAutodelete ? " autodelete" : "";
        String start = "<table border='1' class='DETAIL_TABLE "+mutationClass+autodeleteClass+"'><tr><td>\r\n";

        boolean detailedPartSummary = false;//includes expected parts bodies.  These are shown in the PartSummary blocks under the detail, along with payloads.
        String partSummary = includePartSummary ? s.partsSummaryHTML(detailedPartSummary) : "";
        String idNoMutatorID = (Tools.notEmpty(s.idFromMutator) && Tools.notEmpty(s.testID))
                                 ? s.testID.substring(0, (s.testID.length() - s.idFromMutator.length()) )
                                 : s.testID;
        String SUCCESS = formatMutatorSUCCESS(s);
        String statusLabel;
        boolean showSUCCESS = s.isSUCCESS();
        if (showSUCCESS) {
            statusLabel = lbl(SUCCESS);
        } else {
            if (s.expectedFailure){
                statusLabel = "<span class='EXPECTED'>EXPECTED</span>";
            } else {
                statusLabel = "<span class='ERROR'>FAILURE</b></span>";
            }
        }
        String shortComment = makeShort(s.comment, scriptifyName(s.testIDLabel));
        String res =
                start
                + statusLabel
                + SP + (Tools.notEmpty(idNoMutatorID) ?idNoMutatorID : "")+ "<span class='mutationsubscript'>"+s.idFromMutator + "</span>  "
                + SP + (Tools.notBlank(shortComment) ? SP+smallblack(shortComment) : "")
                + "<span style='float:right; margin-right:2px;'>"+lbl("seq")+s.getSequence()+"</span>"
                + linesep
                + s.method + SP + "<a class='URL_A' href='" + s.fullURL + "'>" + s.fullURL + "</a>" + linesep
                + formatResponseCodeBlock(s) +  linesep
                + (Tools.notBlank(s.failureReason) ? s.failureReason + linesep : "")

                //+ ((s.mutator != null) ? s.mutator.toString() : "") + linesep

                //+ ( Tools.notEmpty(s.testGroupID) ? "testGroupID:"+s.testGroupID+linesep : "" )
                //THIS WORKS, BUT IS VERBOSE: + ( Tools.notEmpty(s.fromTestID) ? "fromTestID:"+s.fromTestID+linesep : "" )
                + (Tools.notEmpty(s.responseMessage) ? lbl("msg") + s.responseMessage + linesep : "")
                + (Tools.notBlank(s.auth) ? lbl("auth") + s.auth + linesep:"")
                + lbl("alerts") + alertsToHtml(s.alerts) + linesep
                +(s.parentSkipped
                   ?    ""
                   :
                        (s.requestHeaderMap.size()>0?HDRBEGIN + lbl("req-headers") + requestHeadersToHtml(s.requestHeaderMap) + HDREND + linesep:"")
                        + (Tools.notBlank(s.responseHeadersDump)?  HDRBEGIN + lbl("resp-headers") + s.responseHeadersDump + HDREND + linesep:"")
                 )
                + (Tools.notEmpty(s.deleteURL) ? lbl("deleteURL") + small(s.deleteURL) + linesep : "")
                + (Tools.notEmpty(s.location) ? lbl("location") + small(s.location) + linesep : "")
                //+ (Tools.notEmpty(s.getError()) ?  lbl("more alerts") + alertError(s.getError()) + linesep : "")
                //+ (Tools.notEmpty(s.getErrorDetail()) ?  lbl("even more alert detail") + alertError(s.getErrorDetail()) + linesep : "")
                + ((s.getVars().size()>0) ? lbl("vars")+varsToHtml(s) + linesep : "")
                + ((s.getExports().size()>0) ? lbl("exports")+exportsToHtml(s) + linesep : "")
                + ((includePartSummary && Tools.notBlank(partSummary))
                ?   ((s.expectedTreewalkRangeColumns!=null)
                        ?  formatExpectedTreewalkRangeColumns(s) + linesep
                        :  lbl("part summary") + smallblack(partSummary) + linesep
                    )
                : ""
                )
                + (includePayloads && Tools.notBlank(s.requestPayload) ? LINE + lbl("requestPayload") + LINE + CRLF + s.requestPayload + LINE : "")
                + (includePayloads && Tools.notBlank(s.getResult()) ? LINE + lbl("result") + LINE + CRLF + s.getResult() : "")
                + end;
        return res;
    }

    public static String formatExpectedTreewalkRangeColumns(ServiceResult s){
        if (s == null || s.expectedTreewalkRangeColumns==null){
            return "";
        }
        String strictness = "";
        if (Tools.notBlank(s.payloadStrictness)){
            strictness = " (<span style='font-size: 120%'>"+s.payloadStrictness+"<span>)";
        }
        StringBuilder sb = new StringBuilder();
        String cls = "";
        sb.append("<table class='dom-match-table'><tr>");
        sb.append("<th>DOM Comparison</th>");
        for (ServiceResult.Column col: s.expectedTreewalkRangeColumns){
            sb.append("<th>");
            sb.append(col.name);
            sb.append("</th>");
        }
        sb.append("</tr><tr>");
        sb.append("<th>actual</th>");
        for (ServiceResult.Column col: s.expectedTreewalkRangeColumns){
            cls = "";
            if (Tools.notBlank(col.highlight)){
                cls = " class='"+col.highlight+"' ";
            }
            sb.append("<td"+cls+">");
            sb.append(col.num);
            sb.append("</td>");
        }
        sb.append("</tr><tr>");
        sb.append("<th>expected"+strictness+"</th>");
        for (ServiceResult.Column col: s.expectedTreewalkRangeColumns){
            cls = "";
            if (Tools.notBlank(col.highlight)){
                cls = " class='"+col.highlight+"' ";
            }
            sb.append("<td"+cls+">");
            if (col.exp!=null){
                sb.append(col.exp);
            }
            sb.append("</td>");
        }
        sb.append("</tr></table>");
        return sb.toString();
    }

    private String formatResponseCodeBlock(ServiceResult s){
        String sExpected = "";
        if (s.ranges.size()>0) {
            sExpected = lbl("  expected")+s.ranges;
        }
        if (s.mutator!=null && Tools.notBlank(s.idFromMutator)) {
            sExpected = lbl("  expected") + s.mutator.expectedRangeForID(s.idFromMutator)
            + small("  from "+s.gotExpectedResultBecause);
        }

        if (s.parentSkipped){
            return lbl("skipped")+"true"
                    +SP + lbl("type")+smallblack(s.mutatorType)
                    +SP + lbl("child results")+smallblack(""+s.getChildResults().size());
        }
        return  s.responseCode
                + SP + lbl(" gotExpected")+s.gotExpectedResult()
                + SP + sExpected
                + SP + (s.getParent()!=null?lbl("type")+smallblack(s.getParent().mutatorType):"")
                + SP + (s.loopIndex>-1?lbl("loop")+smallblack(""+s.loopIndex):"")
                + SP + lbl(" time")+s.time + units("ms") ;
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
        String alertClass = "AlertOK";
        for(Alert alert: alerts){
            switch (alert.level){
                case OK:
                    alertClass = "AlertOK";
                    break;
                case WARN:
                    alertClass = "AlertWarn";
                    break;
                case ERROR:
                    alertClass = "AlertError";
                    break;
            }
            buffer.append("<span class='"+alertClass+"'>")
                    .append("<span class='AlertLevel'>").append(alert.level).append("</span>")
                    .append("<span class='AlertContext'>").append(alert.context).append("</span>")
                    .append("<span class='AlertMessage'>").append(alert.message).append("</span>")
                    .append("</span>");
        }
        return buffer.toString();
    }

    private String varsToHtml(ServiceResult result){
        StringBuffer b = new StringBuffer();
        for (Map.Entry<String,Object> entry: result.getVars().entrySet()){
            String val;
            Object obj = entry.getValue();
            if (obj instanceof String[]){
                val = Arrays.toString((String[])obj);
            } else {
                val = obj.toString();
            }

            b.append("<span class='vars'>")
             .append(entry.getKey())
             .append(": ")
             .append(val)
             .append("<br />")
             .append("</span>");
        }
        return b.toString();
    }

    private String exportsToHtml(ServiceResult result){
        StringBuffer b = new StringBuffer();
        for (Map.Entry<String,Object> entry: result.getExports().entrySet()){
            b.append("<span class='exports'>")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("<br />")
                    .append("</span>");
        }
        return b.toString();
    }
}

