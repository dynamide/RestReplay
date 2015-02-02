package org.dynamide.restreplay;

import org.apache.commons.cli.*;

import org.dynamide.interpreters.EvalResult;
import org.dynamide.interpreters.RhinoInterpreter;
import org.dynamide.restreplay.mutators.IMutator;
import org.dynamide.restreplay.mutators.MutatorFactory;
import org.dynamide.restreplay.server.EmbeddedServer;
import org.dynamide.restreplay.TreeWalkResults.TreeWalkEntry.STATUS;
import org.dynamide.util.Tools;
import org.dom4j.*;

import java.io.*;
import java.util.*;

import org.dynamide.interpreters.Alert;
import org.dynamide.interpreters.Alert.LEVEL;
import org.dynamide.interpreters.AlertError;

/**
 * This class is used to replay a request to the Services layer, by sending the XML or JSON payload
 * in an appropriate Multipart request.
 * See example usage in calling class RestReplayTest and RestReplaySelfTest, and also in main() in this class.
 *
 * @author Laramie Crocker
 */
public class RestReplay extends ConfigFile {

    public RestReplay(String testdir, String reportsDir, ResourceManager manager, RunOptions parentRunOptions) {
        setTestDir(testdir);
        this.serviceResultsMap = createResultsMap();
        this.reportsDir = reportsDir;
        setResourceManager(manager);
        if (parentRunOptions != null) {
            setRunOptions(parentRunOptions);
        }
    }

    private String controlFileName = "";
    public String getControlFileName() {
        return controlFileName;
    }
    public void setControlFileName(String controlFileName) {
        this.controlFileName = controlFileName;
    }

    private String masterFilename;
    public String getMasterFilename() {
        return masterFilename;
    }
    public void setMasterFilename(String val) {
        this.masterFilename = val;
    }

    private String relToMaster = "";
    public String getRelToMaster(){
        return relToMaster;
    }
    public void setRelToMaster(String val){
        relToMaster = val;
    }

    public Map<String, Object> masterVars = new LinkedHashMap<String, Object>();
    public Map<String, Object> getMasterVars() {
        return masterVars;
    }
    public void setMasterVars(Map<String, Object> masterVars) {
        this.masterVars = masterVars;
    }

    private Map<String, ServiceResult> serviceResultsMap;
    public Map<String, ServiceResult> getServiceResultsMap() {
        return serviceResultsMap;
    }
    public static Map<String, ServiceResult> createResultsMap() {
        return new LinkedHashMap<String, ServiceResult>();
    }

    public List<EvalResult> evalReport;

    public static Tools TOOLS = new Tools();
    public static Kit KIT = new Kit();

    public String toString() {
        return "RestReplay{" + getTestDir() + ", " + defaultAuthsMap + ", " + getDump() + ", " + reportsDir + '}';
    }

    public static String testToString(Node testNode) {
        return testNode.valueOf("@ID");
    }

    // ============== METHODS ===========================================================

    /**
     * Use this if you wish to run named tests within a testGroup.
     */
    public List<ServiceResult> runTests(String testGroupID, String testID) throws Exception {
        List<ServiceResult> result = runRestReplayFile(
                this.getTestDir(),
                this.controlFileName,
                testGroupID,
                testID,
                this.masterVars,
                this.isAutoDeletePOSTS(),
                this.getProtoHostPort(),
                this.defaultAuthsMap,
                this.getReportsList(),
                this.reportsDir,
                this.getRelativePathFromReportsDir(),
                this.getMasterFilename());
        return result;
    }

    public List<ServiceResult> autoDelete(String logName) {
        return autoDelete(this.serviceResultsMap, logName);
    }

    /**
     * Use this method to clean up resources created on the server that returned CSIDs, if you have
     * specified autoDeletePOSTS==false, which means you are managing the cleanup yourself.
     *
     * @param serviceResultsMap a Map of ServiceResult objects, which will contain ServiceResult.deleteURL.
     * @return a List<String> of debug info about which URLs could not be deleted.
     */
    public static List<ServiceResult> autoDelete(Map<String, ServiceResult> serviceResultsMap, String logName) {
        List<ServiceResult> results = new ArrayList<ServiceResult>();
        for (ServiceResult pr : serviceResultsMap.values()) {
            try {
                if (Tools.notEmpty(pr.deleteURL)) {
                    ServiceResult deleteResult = new ServiceResult(pr.getRunOptions());
                    deleteResult.connectionTimeout = pr.connectionTimeout;
                    deleteResult.socketTimeout = pr.socketTimeout;
                    System.out.println("ATTEMPTING AUTODELETE: ==>" + pr.deleteURL + "<==");
                    deleteResult = Transport.doDELETE(deleteResult, pr.deleteURL, pr.auth, pr.testID, "[autodelete:" + logName + "]", pr.headerMap);
                    System.out.println("DONE AUTODELETE: ==>" + pr.deleteURL + "<== : " + deleteResult);
                    results.add(deleteResult);
                } else {
                    ServiceResult errorResult = new ServiceResult(pr.getRunOptions());
                    errorResult.fullURL = pr.fullURL;
                    errorResult.testGroupID = pr.testGroupID;
                    errorResult.fromTestID = pr.fromTestID;
                    errorResult.overrideGotExpectedResult();
                    results.add(errorResult);
                    System.out.println("DONE AUTODELETE (errorResult): ==>" + pr.deleteURL + "<== : " + errorResult);
                }
            } catch (Throwable t) {
                String s = (pr != null) ? "ERROR while cleaning up ServiceResult map: " + pr + " for " + pr.deleteURL + " :: " + t
                        : "ERROR while cleaning up ServiceResult map (null ServiceResult): " + t;
                System.err.println(s);
                ServiceResult errorResult = new ServiceResult(null);
                errorResult.fullURL = pr.fullURL;
                errorResult.testGroupID = pr.testGroupID;
                errorResult.fromTestID = pr.fromTestID;
                errorResult.addError(s, t);
                results.add(errorResult);
                System.out.println("DONE AUTODELETE (Throwable): ==>" + pr.deleteURL + "<== : " + errorResult + " t:" + t);
            }
        }
        return results;
    }

    private static class PartsStruct {
        public List<Map<String, Object>> varsList = new ArrayList<Map<String, Object>>();
        String requestPayloadFilename = "";
        String requestPayloadFilenameRel = "";
        String expectedResponseFilename = "";
        String expectedResponseFilenameRel = "";
        String overrideTestID = "";
        String startElement = "";
        String label = "";
        String validator = "";
        String validatorFilenameRel = "";
        String validatorLang = "";
        String testdir = "";
        boolean isResponse = false;
        public static boolean fnIsXML(String fn){
            if (Tools.notBlank(fn) && fn.toUpperCase().endsWith(".XML")){
                return true;
            }
            return false;
        }
        public static boolean fnIsJSON(String fn){
            if (Tools.notBlank(fn) && fn.toUpperCase().endsWith(".JSON")){
                return true;
            }
            return false;
        }
        public boolean isXML(){
            if (isResponse) {
                if (fnIsXML(expectedResponseFilename)){
                    return true;
                } else if (fnIsXML(expectedResponseFilenameRel)){
                    return true;
                }
                return false;
            } else {
                if (fnIsXML(requestPayloadFilename)){
                    return true;
                } else if (fnIsXML(requestPayloadFilenameRel)){
                    return true;
                }
                return false;
            }
        }

        //This method is overloaded with the isResponse boolean.  If isResponse, we are reading test/response/filename,
        //  otherwise we are reading test/filename. These should be split into two functions, because the XML structs
        //  are different--they just have the /vars element in common.
        public static PartsStruct readParts(Node testNode, final String testID, String testdir, boolean isResponse) {
            PartsStruct resultPartsStruct = new PartsStruct();
            resultPartsStruct.isResponse = isResponse;
            resultPartsStruct.startElement = testNode.valueOf("startElement");
            resultPartsStruct.label = testNode.valueOf("label");
            resultPartsStruct.testdir = testdir;
            String filename = testNode.valueOf("filename");

            if (isResponse) {
                resultPartsStruct.validator = testNode.valueOf("validator");  //the actual script, hopefully in a CDATA.
                resultPartsStruct.validatorFilenameRel = testNode.valueOf("validator/@filename");
                resultPartsStruct.validatorLang = testNode.valueOf("validator/@lang");
            }

            if (Tools.notEmpty(filename)) {
                if (isResponse) {
                    resultPartsStruct.expectedResponseFilename = testdir + '/' + filename;
                    resultPartsStruct.expectedResponseFilenameRel = filename;
                } else {
                    resultPartsStruct.requestPayloadFilename = testdir + '/' + filename;
                    resultPartsStruct.requestPayloadFilenameRel = filename;
                }
                resultPartsStruct.varsList.add(readVars(testNode));
            }
            return resultPartsStruct;
        }
    }

    private static String fixupFullURL(String protoHostPort, String uri) {
        String fullURL;
        if (uri.startsWith("http")) {
            return uri;
        }
        if (!uri.startsWith(protoHostPort)) {
            fullURL = Tools.glue(protoHostPort, "/", uri);
        } else {
            fullURL = uri;
        }
        return fullURL;
    }

    private static String fromTestID(String fullURL, Node testNode, Map<String, ServiceResult> serviceResultsMap) {
        String fromTestID = testNode.valueOf("fromTestID");
        if (Tools.notEmpty(fromTestID)) {
            ServiceResult sr = serviceResultsMap.get(fromTestID);
            if (sr != null) {
                fullURL = sr.location;
            }
        }
        return fullURL;
    }

    private static String locationFromTestID(Node testNode, Map<String, ServiceResult> serviceResultsMap) {
        String result = "";
        String fromTestID = testNode.valueOf("fromTestID");
        if (Tools.notEmpty(fromTestID)) {
            ServiceResult getPR = serviceResultsMap.get(fromTestID);
            if (getPR != null) {
                result = getPR.location;
            }
        }
        return result;
    }

    protected String validateResponse(ServiceResult serviceResult,
                                      PartsStruct expectedResponseParts,
                                      Eval evalStruct) {
        String OK = "";
        if (expectedResponseParts == null) return OK;
        if (serviceResult == null) return OK;
        if (serviceResult.getResult().length() == 0) return OK;
        try {
            return validateResponseSinglePayload(serviceResult, expectedResponseParts, evalStruct);
        } catch (Exception e) {
            String err = "ERROR in RestReplay.validateResponse() : " + Tools.errorToString(e, true);
            return err;
        }
    }

    /* See, for example of <expected> : test/resources/test-data/restreplay/objectexit/object-exit.xml */
    protected String validateResponseSinglePayload(ServiceResult serviceResult,
                                                   PartsStruct expectedResponseParts,
                                                   Eval evalStruct)
    throws Exception {
        String OK = "";
        serviceResult.beginTrappingExports();
        int errorCountBefore = serviceResult.alertsCount(LEVEL.ERROR);
        int warnCountBefore = serviceResult.alertsCount(LEVEL.WARN);
        EvalResult validationResult = runValidatorScript(serviceResult, expectedResponseParts, evalStruct);
        String validationResultStr = validationResult != null ? validationResult.toString() : "";
        if (Tools.notBlank(expectedResponseParts.validator) && validationResult != null){
            List<String> exports = serviceResult.endTrappingExports();
            int errorCountAfter = serviceResult.alertsCount(LEVEL.ERROR);
            int warnCountAfter = serviceResult.alertsCount(LEVEL.WARN);

            String validatorDisplayName = expectedResponseParts.validatorFilenameRel;
            if (Tools.notBlank(expectedResponseParts.validator)){
                validatorDisplayName = "[inline]";
            }
            String counts = "";
            if ((errorCountAfter - errorCountBefore) > 0){
                counts = "<div><b>errors:</b> " + (errorCountAfter - errorCountBefore)+"</div>";

            }
            if ((warnCountAfter - warnCountBefore)>0){
                counts += "<div><b>warnings:</b> " + (warnCountAfter - warnCountBefore)+"</div>";
            }
            String exportString = exports.size()>0
                    ? "<div><b>exports:</b> <div class='validator-exports'>"+exports+"</div></div>"
                    : "";
            String validatorResultBlock = Tools.notBlank(validationResultStr)
                    ? "<b>validator result:</b> <span class='validator-result'>"+validationResultStr+"</span>"
                    : "";
            serviceResult.addAlert(validatorResultBlock
                                      +exportString
                                      +counts,
                                    "<b>validator:</b> "+validatorDisplayName,
                                    validationResult.worstLevel);
        }

        boolean expectedPartContentWasJSON = false;
        String expectedResponseFilenameUsed = "";

        //The deal is: you may specify expectedResponseFilenameRel OR expectedResponseFilename, so we have to do this weird logic with expectedPartContent.
        String expectedPartContent = "";
        if ( Tools.notBlank(expectedResponseParts.expectedResponseFilenameRel)
           ||Tools.notBlank(expectedResponseParts.expectedResponseFilename)){

                String expFilenameRel = expectedResponseParts.expectedResponseFilenameRel;
                String expFilename    = expectedResponseParts.expectedResponseFilename;
                Map<String,Object> varsDup = new HashMap<String, Object>();
                varsDup.putAll(serviceResult.getVars());
                varsDup.putAll(serviceResult.getExports());
                EvalResult evalResult = evalStruct.eval("expanding:"+expFilenameRel,
                                                        expFilenameRel,
                                                        varsDup);
                expFilenameRel = evalResult.getResultString();
                expectedPartContent = getResourceManager().readResource("validateResponseSinglePayload",
                                                                        expFilenameRel,
                                                                        expFilename);
                //System.out.println("\r\n====expectedPartContent: "+expFilenameRel+"::"+expectedPartContent+"\r\n");

                expectedResponseFilenameUsed = expFilenameRel;
                serviceResult.expectedContentRaw = expectedPartContent;
                if (RestReplay.PartsStruct.fnIsJSON(expFilenameRel)){
                    expectedPartContentWasJSON = true;
                    serviceResult.expectedContentExpandedWasJson = expectedPartContentWasJSON;
                }
            if (Tools.isBlank(expectedPartContent)){
                serviceResult.addAlert("expectedResponseParts specified a file, but the file was empty. "
                                          +"filenameUsed: '"+expectedResponseFilenameUsed+"' filename: '"
                                                           +expectedResponseParts.expectedResponseFilename+"'",
                                        "validateResponseSinglePayload",
                                        LEVEL.WARN
                                      );
            }
        }
        if (Tools.isBlank(expectedPartContent)){
            return OK;
        }

        Map<String, Object> vars = null;
        if (expectedResponseParts.varsList.size()>0) {
            vars = expectedResponseParts.varsList.get(0);  //just one part, so just one varsList.
        }
        String theContext = expectedResponseFilenameUsed;

        //=======  expand the expectedPartContent ====
        EvalResult evalResult = evalStruct.eval(theContext,
                expectedPartContent,
                vars);
        serviceResult.alerts.addAll(evalResult.alerts);
        expectedPartContent = evalResult.getResultString();
        //============================================

        serviceResult.expectedContentExpanded = expectedPartContent;
        serviceResult.expectedResponseFilenameUsed = expectedResponseFilenameUsed;

        if(expectedPartContentWasJSON){
            expectedPartContent = ServiceResult.payloadJSONtoXML(expectedPartContent);
            serviceResult.expectedContentExpandedAsXml = expectedPartContent;
        }

        String label = "";//NOLABEL";
        String leftID = "{from expected part, label:" + label + " filename: " + expectedResponseFilenameUsed + "}";
        String rightID = "{from server, label:" + label
                + " fromTestID: " + serviceResult.fromTestID
                + " URL: " + serviceResult.fullURL
                + "}";

        String startElement = expectedResponseParts.startElement;
        String partLabel = expectedResponseParts.label;
        if (Tools.isBlank(startElement)) {
            if (Tools.notBlank(partLabel))
                startElement = "/document/*[local-name()='" + partLabel + "']";
        }
        if (Tools.notBlank(expectedPartContent) /*&& expectedResponseParts.isXML()*/  ) {
            if (Tools.notBlank(serviceResult.getXmlResult())) {
                TreeWalkResults.MatchSpec matchSpec = TreeWalkResults.MatchSpec.createDefault();
                matchSpec.leftEmptyMatchesAllText = true; //TODO: make reader in expected/dom set this with an option to add a match string, such as "*"
                TreeWalkResults list =
                        XmlCompareJdom.compareParts(expectedPartContent,
                                leftID,
                                serviceResult.getXmlResult(),
                                rightID,
                                startElement,
                                matchSpec);
                serviceResult.addPartSummary(label, list);
            } else {
                System.out.println("ERROR: "+serviceResult.testID+" expectedPartContent but no xmlResult sr:"+serviceResult+ " CONTENT:"+expectedPartContent);
            }
        } else {
            System.out.println("expectedPartContent blank in test "+serviceResult.testID);
        }
        return OK;
    }

    private void p(String val){
        System.out.println(val);
    }

    protected EvalResult  runValidatorScript(ServiceResult serviceResult,
                                        PartsStruct expectedResponseParts,
                                        Eval evalStruct)
    throws IOException {
        String scriptFilename = expectedResponseParts.validatorFilenameRel;
        String scriptBody = expectedResponseParts.validator;
        String testdir = expectedResponseParts.testdir;
        String lang = expectedResponseParts.validatorLang;
        String fullPath = Tools.join(testdir, scriptFilename);

        String source = null;
        String resourceName = "";

        if (Tools.notBlank(scriptBody)) {
            source = scriptBody;
            resourceName = "inline validator";
        } else if (Tools.notBlank(scriptFilename)) {
            source = getResourceManager().readResource("runValidatorScript", scriptFilename, fullPath);
            resourceName = Tools.join(testdir, scriptFilename);
        }
        if (Tools.notBlank(source)) {
            serviceResult.setCurrentValidatorContextName(scriptFilename);
            try {
                if (Tools.notBlank(lang) && lang.equalsIgnoreCase("JAVASCRIPT")) {
                    return evalJavascript(evalStruct, resourceName, source, serviceResult);
                } else if (Tools.notBlank(lang) && lang.equalsIgnoreCase("JEXL")
                        || Tools.isBlank(lang)) {
                    //default to JEXL.
                    EvalResult evalResult = evalJexl(evalStruct, fullPath, source, null);
                    return evalResult;
                }
            } finally {
                serviceResult.setCurrentValidatorContextName("");
            }
        }
        return null;
    }

    private EvalResult evalJexl(Eval evalStruct, String context, String source, Map<String,Object> vars) {
        return evalStruct.eval(context, source, vars);
    }

    private EvalResult evalJavascript(Eval evalStruct, String resourceName, String source, ServiceResult serviceResult) {
        RhinoInterpreter interpreter = new RhinoInterpreter();
        interpreter.setVariable("result", serviceResult);
        interpreter.setVariable("serviceResult", serviceResult);
        interpreter.setVariable("serviceResultsMap", serviceResultsMap);
        interpreter.setVariable("kit", KIT);
        interpreter.setVariable("tools", TOOLS);
        //System.out.println("evalJavascript TO: "+serviceResult.testIDLabel);
        //System.out.println("evalJavascript exports: "+serviceResult.getExports());
        for (Map.Entry<String,Object> entry: serviceResult.getVars().entrySet()){
            interpreter.setVariable(entry.getKey(), entry.getValue());
            //System.out.println("adding vars: "+entry.getKey());
        }
        for (Map.Entry<String,Object> entry: serviceResult.getExports().entrySet()){
            interpreter.setVariable(entry.getKey(), entry.getValue());
            //System.out.println("adding exports: "+entry.getKey());
        }
        for (Map.Entry<String,ServiceResult> entry: serviceResultsMap.entrySet()){
            if (entry.getKey().equals("this")){
                //skip "this" because it conflicts with javascript.
            }
            interpreter.setVariable(entry.getKey(), entry.getValue());
            //System.out.println("adding ServiceResults: "+entry.getKey());
        }
        EvalResult result = interpreter.eval(resourceName, source);
        evalStruct.addToEvalReport(result);
        return result;
    }


    private static String dumpMasterVars(Map<String, Object> masterVars) {
        if (masterVars == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, Object> entry : masterVars.entrySet()) {
            buffer.append("\r\n        ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return buffer.toString();
    }

    //================= runRestReplayFile ======================================================

    public List<ServiceResult> runRestReplayFile(
            String testdir,
            String controlFileName,
            String testGroupID,
            String oneTestID,
            Map<String, Object> masterVars,
            boolean param_autoDeletePOSTS,
            String protoHostPortParam,
            AuthsMap authsFromMaster,
            List<String> reportsList,
            String reportsDir,
            String relativePathFromReportsDir,
            String masterFilenameInfo)
            throws Exception {
        //Internally, we maintain two collections of ServiceResult:
        //  the first is the return value of this method.
        //  the second is this.serviceResultsMap, which is used for keeping track of CSIDs created by POSTs, for later reference by DELETE, etc.
        List<ServiceResult> results = new ArrayList<ServiceResult>();

        RestReplayReport report = new RestReplayReport(reportsDir);

        org.dom4j.Document document;
        document = getResourceManager().getDocument("runRestReplayFile:" + controlFileName + ", test:" + testGroupID, testdir, controlFileName); //will check full path first, then checks relative to PWD.
        if (document == null) {
            throw new FileNotFoundException("RestReplay control file (" + controlFileName + ") not found in classpath, or testdir: " + testdir + " Exiting test.");
        }

        String protoHostPortFrom = "from restReplay Master.";
        String protoHostPort = protoHostPortParam;  //default to the one passed in, which comes from master.
        Node n = document.selectSingleNode("/restReplay/protoHostPort");  //see if control file has override.
        if (null != n) {
            protoHostPort = n.getText().trim();
            //System.out.println("Using protoHostPort ('"+protoHostPort+"') from restReplay file ('"+controlFileName+"'), not master.");
            protoHostPortFrom = "from control file.";
        }

        String authsMapINFO;
        AuthsMap authsMap = readAuths(document);
        if (authsMap.map.size() == 0) {
            if (authsFromMaster != null) {
                authsMap = authsFromMaster;
                authsMapINFO = "Using auths from master file: " + authsFromMaster;
            } else {
                authsMapINFO = "No auths in control file (and no master)";
            }
        } else {
            authsMapINFO = "Using auths from control file: " + authsMap;
        }

        report.addTestGroup(testGroupID, controlFileName);   //controlFileName is just the short name, without the full path.
        String restReplayHeader = "========================================================================"
                + "\r\nRestReplay running:"
                + "\r\n   controlFile: " + controlFileName
                + "\r\n   Master: " + masterFilenameInfo
                + "\r\n   reports directory: " + reportsDir
                + "\r\n   env: " + relativePathFromReportsDir
                + "\r\n   protoHostPort: " + protoHostPort + "    " + protoHostPortFrom
                + "\r\n   testGroup: " + testGroupID
                + (Tools.notEmpty(oneTestID) ? "\r\n   oneTestID: " + oneTestID : "")
                + "\r\n   auths map: " + authsMapINFO
                + "\r\n   masterVars: " + dumpMasterVars(masterVars)
                + "\r\n   param_autoDeletePOSTS: " + param_autoDeletePOSTS
                + "\r\n   Dump info: " + getDump()
                + "\r\n   RunOptions: " + getRunOptions()
                + "\r\n========================================================================"
                + "\r\n";
        report.addRunInfo(restReplayHeader);

        System.out.println(restReplayHeader);

        String autoDeletePOSTS = "";
        List<Node> testgroupNodes;
        if (Tools.notEmpty(testGroupID)) {
            testgroupNodes = document.selectNodes("//testGroup[@ID='" + testGroupID + "']");
        } else {
            testgroupNodes = document.selectNodes("//testGroup");
        }

        Eval evalStruct = new Eval();
        evalStruct.runOptions = this.getRunOptions();
        evalStruct.serviceResultsMap = this.serviceResultsMap;

        OUTER:
        for (Node testgroup : testgroupNodes) {
            evalStruct.resetContext();    // Get a new JexlContext for each test group.

            //vars var = get control file vars and merge masterVars into it, replacing
            Map<String, Object> testGroupVars = readVars(testgroup);
            Map<String, Object> clonedMasterVars = new LinkedHashMap<String, Object>();
            if (null != masterVars) {
                clonedMasterVars.putAll(masterVars);
            }
            clonedMasterVars.putAll(testGroupVars);

            autoDeletePOSTS = testgroup.valueOf("@autoDeletePOSTS");
            List<Node> tests;
            if (Tools.notEmpty(oneTestID)) {
                tests = testgroup.selectNodes("test[@ID='" + oneTestID + "']");
            } else {
                tests = testgroup.selectNodes("test");
            }
            int testElementIndex = -1;
            for (Node testNode : tests) {


                String iterations = testNode.valueOf("@loop");  //try as an attribute
                if (Tools.isBlank(iterations)){
                    iterations = testNode.valueOf("loop"); //try as an element (supports multi-line expressions).
                }
                boolean doingIterations = false;
                int iIterations = 1;
                Map<String,Object> loopMap = null;
                Collection loopCollection = null;
                String[] loopArray = null;
                Object[] loopObjArray = null;

                if (Tools.notBlank(iterations)){
                    doingIterations = true;
                    EvalResult evalResult = null;
                    try {
                        evalResult = evalStruct.eval("calculate @loop", iterations, clonedMasterVars);
                        if (   evalResult.worstLevel.equals(LEVEL.WARN)
                            || evalResult.worstLevel.equals(LEVEL.ERROR)){
                            throw new Exception(" expression: "+iterations);
                        }
                        Object resultResult = evalResult.result;
                        //serviceResult.alerts.addAll(evalResult.alerts);
                        if (resultResult instanceof String[] ){
                            iIterations = ((String[])resultResult).length;
                            evalStruct.jc.set("loop", resultResult);
                            loopArray = (String[])resultResult;
                        } else if (resultResult !=null && resultResult.getClass().isArray()) {
                            loopObjArray = (Object[])resultResult;
                            iIterations = ((Object[])resultResult).length;
                            evalStruct.jc.set("loop", resultResult);
                        } else if (resultResult instanceof Map) {
                            loopMap = (Map)resultResult;
                            iIterations = loopMap.size();
                            evalStruct.jc.set("loop", resultResult);
                        } else if (resultResult instanceof Collection) {
                            loopCollection = (Collection)resultResult;
                            iIterations = loopCollection.size();
                            evalStruct.jc.set("loop", resultResult);
                        } else {
                            iterations = evalResult.getResultString();
                            iIterations = Integer.parseInt(iterations);
                        }
                    } catch (Throwable t){
                        System.out.println("\n======NOT doing iterations because loop expression failed:"+iterations+"\n");
                        ServiceResult serviceResult = new ServiceResult(getRunOptions());
                        if (evalResult!=null)evalResult.alerts.addAll(evalResult.alerts);
                        serviceResult.testID = testNode.valueOf("@ID");
                        serviceResult.testIDLabel = Tools.notEmpty(serviceResult.testID) ? (testGroupID + '.' + serviceResult.testID) : (testGroupID + '.' + testElementIndex);
                        String msg = "ERROR calculating loop";
                        serviceResult.addError(msg, t);
                        serviceResult.failureReason = msg+t.getMessage();
                        List<Node> failures = testNode.selectNodes("response/expected/failure"); //TODO: get in sync with expected/failure handling elsewhere.
                        if (failures.size()>0){
                            serviceResult.expectedFailure = true;
                        }
                        report.addTestResult(serviceResult);
                        results.add(serviceResult);
                        continue OUTER;
                    }
                }

                Map.Entry entry;
                Iterator<Map.Entry<String,Object>> mapIt = null;
                Set<Map.Entry<String,Object>> set = null;
                Iterator colIt = null;
                if (loopMap!=null) {
                    set = loopMap.entrySet();
                    mapIt = set.iterator();
                }
                if (loopCollection!=null){
                    colIt = loopCollection.iterator();

                }
                for (int itnum = 0; itnum < iIterations; itnum++) {
                    if (mapIt!=null){
                        entry = mapIt.next();
                        evalStruct.jc.set("loop.key", entry.getKey());
                        evalStruct.jc.set("loop.value", entry.getValue());
                    }
                    if (colIt!=null){
                        Object loopObject =colIt.next();
                        evalStruct.jc.set("loop.value", loopObject);
                    }
                    if (loopArray != null){
                        evalStruct.jc.set("loop.value", loopArray[itnum]);
                    }
                    if (loopObjArray != null){
                        evalStruct.jc.set("loop.value", loopObjArray[itnum]);
                    }
                    serviceResultsMap.remove("result");  //special value so deleteURL can reference ${result.got(...)}.  "result" gets added after each of GET, POST, PUT, DELETE, LIST.
                    testElementIndex++;
                    ServiceResult serviceResult = new ServiceResult(getRunOptions());
                    if (doingIterations) {
                        serviceResult.loopIndex = itnum;
                        evalStruct.jc.set("loop.index", itnum);
                    }
                    serviceResultsMap.put("this", serviceResult);
                    executeTestNode(serviceResult,
                            null,
                            null,
                            testNode,
                            testgroup,
                            protoHostPort,
                            clonedMasterVars,
                            null,
                            testElementIndex,
                            testGroupID,
                            evalStruct,
                            authsMap,
                            authsFromMaster,
                            testdir,
                            report,
                            results);
                    serviceResultsMap.remove("this");
                }
            }
            serviceResultsMap.remove("result");
            if (Tools.isTrue(autoDeletePOSTS) && param_autoDeletePOSTS) {
                autoDelete(serviceResultsMap, "default");
            }
        }

        this.evalReport = evalStruct.getEvalReport();

        //=== Now spit out the HTML report file ===
        File m = new File(controlFileName);  //don't instantiate, just use File to extract file name without directory.
        String relpath = m.getParentFile().toString();
        this.relToMaster = calculateElipses(relpath)+this.relToMaster;
        String reportName = controlFileName + '-' + testGroupID + ".html";

        File resultFile = report.saveReport(testdir, reportsDir, reportName, this, testGroupID);
        if (resultFile != null) {
            String toc = report.getTOC(relativePathFromReportsDir + reportName);
            reportsList.add(toc);
        }
        //================================
        if (getRunOptions().dumpResourceManagerSummary) {
            System.out.println(getResourceManager().formatSummaryPlain());
        }

        return results;
    }

    public String calculateElipses(String relpath) {
        String result = "";
        int count = relpath.length() - relpath.replace("/", "").length();  //calc how many '/' chars there are.
        for (int i = 0; i < count; i++) {
            result += "../"; //one for each nested folder within test dir.
        }
        if (Tools.notBlank(this.getEnvID())){
            return "../../"+result;
        }
        return "../"+result; // first directory is the name of the test dir, so master is above that.
    }

    private ServiceResult executeTestNode(
            ServiceResult serviceResult,
            String contentRawFromMutator,
            IMutator mutator,
            Node testNode,
            Node testGroupNode,
            String protoHostPort,
            Map<String, Object> clonedMasterVars,
            Map<String, Object> mutatorScopeVars,
            int testElementIndex,
            String testGroupID,
            Eval evalStruct,
            AuthsMap authsMap,
            AuthsMap defaultAuths,
            String testdir,
            RestReplayReport report,
            List<ServiceResult> results) {

        final String idFromMutator = (null != mutator) ? mutator.getMutationID() : "";
        long startTime = System.currentTimeMillis();
        String lastTestID = "";
        String lastTestLabel = "";

        report.addTestResult(serviceResult);

        if (contentRawFromMutator != null) {
            serviceResult.isMutation = true;
        }

        //NOPE. TODO: make sure this is done by parent.  20141215. was:     serviceResult.mutator = mutator;

        Map<String, Object> clonedMasterVarsWTest = new LinkedHashMap<String, Object>();

        try {
            clonedMasterVarsWTest.putAll(clonedMasterVars);
            clonedMasterVarsWTest.putAll(readVars(testNode));
            if (mutatorScopeVars!=null){
                clonedMasterVarsWTest.putAll(mutatorScopeVars);
            }
            int loopIndex =  serviceResult.getLoopIndex();
            String testID = testNode.valueOf("@ID")
                               + (Tools.notBlank(idFromMutator) ? "_" + idFromMutator : "")
                               + (loopIndex > -1 ? "_"+loopIndex : "");
            lastTestID = testID;
            String testIDLabel = Tools.notEmpty(testID) ? (testGroupID + '.' + testID) : (testGroupID + '.' + testElementIndex)
                    + "mut:" + idFromMutator + ";";
            lastTestLabel = testIDLabel;
            String method = testNode.valueOf("method");
            String uri = testNode.valueOf("uri");
            String mutatorType = testNode.valueOf("mutator/@type");
            boolean mutatorSkipParent = Tools.isTrue(testNode.valueOf("mutator/@skipParent"));

            //get default timeouts from master config file.
            serviceResult.connectionTimeout = getRunOptions().connectionTimeout;
            serviceResult.socketTimeout = getRunOptions().socketTimeout;
            String authIDForTest = testNode.valueOf("@auth");
            String currentAuthForTest = (authsMap != null) ? authsMap.map.get(authIDForTest) : "";

            String authForTest = "";
            if (Tools.notEmpty(currentAuthForTest)) {
                authForTest = currentAuthForTest; //else just run with current from last loop;
            }
            if (Tools.isEmpty(authForTest) && (defaultAuths != null)) {
                authForTest = defaultAuths.getDefaultAuth();
            }

            //AFTER this, evals will happen, so fields on "this" must be updated:
            serviceResult.testID = testID;
            serviceResult.testIDLabel = testIDLabel;
            serviceResult.idFromMutator = idFromMutator;
            serviceResult.auth = authForTest;
            serviceResult.method = method;
            evalStruct.setCurrentTestIDLabel(serviceResult.testIDLabel);


            //====Headers==========================
            Map<String, String> headerMap = readHeaders(testNode, evalStruct, serviceResult);
            String inheritHeaders = testNode.valueOf("@inheritHeaders");
            boolean skipInheritHeaders = Tools.notBlank(inheritHeaders) && inheritHeaders.equalsIgnoreCase("FALSE");
            if (!skipInheritHeaders) {
                Map<String, String> headerMapFromTestGroup = readHeaders(testGroupNode, evalStruct, serviceResult);
                headerMap.putAll(headerMapFromTestGroup);
            }
            serviceResult.headerMap = headerMap;
            //========END Headers=====================

            String oneProtoHostPort = protoHostPort;
            if (oneProtoHostPort.indexOf("$") > -1) {
                EvalResult evalResult = evalStruct.eval("vars to protoHostPort", oneProtoHostPort, clonedMasterVarsWTest);
                oneProtoHostPort = evalResult.getResultString();
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            if (uri.indexOf("$") > -1) {
                EvalResult evalResult = evalStruct.eval("FULLURL", uri, clonedMasterVarsWTest);
                uri = evalResult.getResultString();
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            String fullURL = fixupFullURL(oneProtoHostPort, uri);
            serviceResult.fullURL = fullURL;
            serviceResult.protoHostPort = oneProtoHostPort;

            if (mutator == null) {
                serviceResult.initExpectedCodeRanges(testNode);
            }

            Node responseNode = testNode.selectSingleNode("response");
            PartsStruct expectedResponseParts = null;
            if (responseNode != null) {
                expectedResponseParts = PartsStruct.readParts(responseNode, testID, testdir, true);
                Node failure = responseNode.selectSingleNode("expected/failure");
                serviceResult.expectedFailure = (failure != null);
            }

            if (clonedMasterVarsWTest != null) {
                serviceResult.addVars(clonedMasterVarsWTest);
            }

            boolean isPOST = method.equalsIgnoreCase("POST");
            boolean isPUT = method.equalsIgnoreCase("PUT");
            if (isPOST || isPUT) {
                doPOSTPUT(new OneTest(
                                serviceResult,
                                results,
                                testNode,
                                testGroupNode,
                                headerMap,
                                evalStruct,
                                testIDLabel,
                                testID,
                                testGroupID,
                                fullURL,
                                oneProtoHostPort,
                                authForTest,
                                startTime),
                        isPUT,
                        method,
                        contentRawFromMutator,
                        mutatorType,
                        mutatorSkipParent,
                        testdir,
                        clonedMasterVarsWTest,
                        testElementIndex,
                        authsMap,
                        defaultAuths,
                        report
                );
            } else if (method.equalsIgnoreCase("DELETE")) {
                doDELETE(new OneTest(
                        serviceResult,
                        results,
                        testNode,
                        testGroupNode, //not needed
                        headerMap,
                        evalStruct,
                        testIDLabel,
                        testID,
                        "", //testGroupID
                        fullURL,
                        oneProtoHostPort,
                        authForTest,
                        startTime));
            } else if (method.equalsIgnoreCase("GET")) {
                fullURL = fromTestID(fullURL, testNode, serviceResultsMap);
                serviceResult.fullURL = fullURL;
                serviceResult = Transport.doGET(serviceResult, fullURL, authForTest, testIDLabel, headerMap);
                results.add(serviceResult);
                serviceResult.time = (System.currentTimeMillis() - startTime);
                serviceResultsMap.put(testID, serviceResult);
                serviceResultsMap.put("result", serviceResult);
            } else if (method.equalsIgnoreCase("LIST")) {
                String listQueryParams = ""; //TODO: empty for now, later may pick up from XML control file.
                serviceResult = Transport.doLIST(serviceResult, fullURL, listQueryParams, authForTest, testIDLabel, headerMap);
                results.add(serviceResult);
                serviceResult.time = (System.currentTimeMillis() - startTime);
                serviceResultsMap.put(testID, serviceResult);
                serviceResultsMap.put("result", serviceResult);
            } else {
                throw new Exception("HTTP method not supported by RestReplay: " + method);
            }

            if (Tools.isEmpty(serviceResult.testID)) serviceResult.testID = testIDLabel;
            if (Tools.isEmpty(serviceResult.testGroupID)) serviceResult.testGroupID = testGroupID;

            Node expectedLevel = testNode.selectSingleNode("response/expected");  //TODO: sync with RestReplay::runRestReplayFile() where I select on response/expected/failure.
            /*  attempting to map expected/dom to each mutation, but it gets messy. Removed for now.
            ServiceResult par = serviceResult.getParent();
            if (par!=null) {
                Node mutatorExpectedLevel = testNode.selectSingleNode("mutator/expected");
                if (mutatorExpectedLevel != null) {
                    expectedLevel = mutatorExpectedLevel;
                }
            }
            */
            if (expectedLevel != null) {
                String domLevel = expectedLevel.valueOf("@dom");
                serviceResult.payloadStrictness = domLevel;

                Node dom = expectedLevel.selectSingleNode("dom");
                if (dom !=null){
                    Map<STATUS,Range> rangeMap = new HashMap<STATUS, Range>();
                    rangeMap.put(STATUS.MATCHED, new Range(dom.valueOf("MATCHED/@range")));
                    rangeMap.put(STATUS.REMOVED, new Range(dom.valueOf("REMOVED/@range")));
                    rangeMap.put(STATUS.ADDED, new Range(dom.valueOf("ADDED/@range")));
                    rangeMap.put(STATUS.ERROR, new Range(dom.valueOf("ERROR/@range")));
                    rangeMap.put(STATUS.DIFFERENT, new Range(dom.valueOf("DIFFERENT/@range")));
                    rangeMap.put(STATUS.NESTED_ERROR, new Range(dom.valueOf("NESTED_ERROR/@range")));
                    serviceResult.expectedTreewalkRangeMap = rangeMap;
                }
            }

            handleExports(serviceResult, testNode, evalStruct, clonedMasterVarsWTest);

            //=====================================================
            //  ALL VALIDATION FOR ALL REQUESTS IS DONE HERE:
            //=====================================================
            boolean hasError = false;

            String vError = validateResponse(serviceResult, expectedResponseParts, evalStruct);  //note that serviceResult is also available in evalStruct as "result".

            if (Tools.notEmpty(vError)) {
                serviceResult.addError(vError);
                serviceResult.failureReason = " : VALIDATION ERROR; ";
                hasError = true;
            }
            if (hasError == false) {
                hasError = !serviceResult.gotExpectedResult();
            }
            if (!hasError) {
                doDeleteURL(serviceResult, testNode, evalStruct, testID, testIDLabel, clonedMasterVarsWTest);
            }
            writeRowToConsoleWDump(serviceResult, hasError, testIDLabel);
        } catch (Throwable t) {
            serviceResultsMap.remove("result");
            String msg = "ERROR: RestReplay experienced an error in a test node (" + testToString(testNode) + "). Throwable: " + Tools.getStackTrace(t);
            System.out.println(msg);
            serviceResult.addError(msg, t);
            serviceResult.failureReason = " : SYSTEM ERROR; ";
            if (Tools.isEmpty(serviceResult.testID)) serviceResult.testID = lastTestID;
            if (Tools.isEmpty(serviceResult.testID)) serviceResult.testID = lastTestLabel;
            addAlertErrorToAlerts(serviceResult.alerts, t);
            report.addTestResult(serviceResult);
            results.add(serviceResult);
        }
        return serviceResult;
    }

    protected static class OneTest {
        public OneTest(
                ServiceResult serviceResult,
                List<ServiceResult> results,
                Node testNode,
                Node testgroup,
                Map<String, String> headerMap,
                Eval evalStruct,
                String testIDLabel,
                String testID,
                String testGroupID,
                String fullURL,
                String protoHostPort,
                String authForTest,
                long startTime
        ) {
            this.serviceResult = serviceResult;
            this.results = results;
            this.testNode = testNode;
            this.testgroup = testgroup;
            this.headerMap = headerMap;
            this.evalStruct = evalStruct;
            this.testIDLabel = testIDLabel;
            this.testID = testID;
            this.testGroupID = testGroupID;
            this.fullURL = fullURL;
            this.protoHostPort = protoHostPort;
            this.authForTest = authForTest;
            this.startTime = startTime;
        }

        ServiceResult serviceResult;
        List<ServiceResult> results;
        Node testNode;
        Node testgroup;
        Map<String, String> headerMap;
        Eval evalStruct;
        String testIDLabel;
        String testID;
        String testGroupID;
        String fromTestID;
        String fullURL;
        String protoHostPort;
        String authForTest;
        long startTime;
    }

    private void doDELETE(OneTest test) {
        String fromTestID = test.testNode.valueOf("fromTestID");
        ServiceResult pr = serviceResultsMap.get(fromTestID);
        ServiceResult serviceResult = test.serviceResult;
        if (pr != null) {
            serviceResult = Transport.doDELETE(serviceResult,
                    pr.deleteURL,
                    test.authForTest,
                    test.testIDLabel,
                    test.fromTestID,
                    test.headerMap);
            serviceResult.time = (System.currentTimeMillis() - test.startTime);
            serviceResult.fromTestID = fromTestID;
            test.results.add(serviceResult);
            if (serviceResult.codeInSuccessRange(serviceResult.responseCode)) {  //gotExpectedResult depends on serviceResult.expectedCodes.
                serviceResultsMap.remove(fromTestID);
            }
        } else {
            if (Tools.notEmpty(fromTestID)) {
                serviceResult = new ServiceResult(getRunOptions());
                serviceResult.responseCode = 0;
                serviceResult.addError("ID not found in element fromTestID: " + fromTestID);
                System.err.println("****\r\nServiceResult: " + serviceResult.getError() + ". SKIPPING TEST. Full URL: " + test.fullURL);
            } else {
                serviceResult = Transport.doDELETE(serviceResult, test.fullURL, test.authForTest, test.testID, fromTestID, test.headerMap);
            }
            serviceResult.time = (System.currentTimeMillis() - test.startTime);
            serviceResult.fromTestID = fromTestID;
            test.results.add(serviceResult);
        }
        serviceResultsMap.put("result", serviceResult);  //DELETES are not supposed to be available in serviceResultsMap,
        // but "result" is supposed to be available until end of test.
    }

    private void doPOSTPUT(OneTest test,
                           boolean isPUT,
                           String method,
                           String contentRawFromMutator,
                           String mutatorType,
                           boolean mutatorSkipParent,
                           String testdir,
                           Map<String, Object> clonedMasterVars,
                           int testElementIndex,
                           AuthsMap authsMap,
                           AuthsMap defaultAuths,
                           RestReplayReport report)
            throws IOException {
        ServiceResult serviceResult = test.serviceResult;

        PartsStruct parts = PartsStruct.readParts(test.testNode, test.testID, testdir, false);
        if (Tools.notEmpty(parts.overrideTestID)) {
            test.testID = parts.overrideTestID;
        }
        if (isPUT) {
            test.fullURL = fromTestID(test.fullURL, test.testNode, serviceResultsMap);
            serviceResult.fullURL = test.fullURL;
        }
        //vars only make sense in two contexts: POST/PUT, because you are submitting another file with internal expressions,
        // and in <response> nodes. For GET, DELETE, there is no payload, so all the URLs with potential expressions are right there in the testNode.
        Map<String, Object> vars = null;
        if (parts.varsList.size() > 0) {
            vars = parts.varsList.get(0);
        }

        EvalResult filanameRelEvalResult  = test.evalStruct.eval("expand req. filenameRel:" + parts.requestPayloadFilenameRel, parts.requestPayloadFilenameRel, vars);
        serviceResult.alerts.addAll(filanameRelEvalResult.alerts);
        String requestPayloadFilenameRelExp = filanameRelEvalResult.getResultString();

        EvalResult filenameEvalResult = test.evalStruct.eval("expand req. filename:" + parts.requestPayloadFilename, parts.requestPayloadFilename, vars);
        serviceResult.alerts.addAll(filenameEvalResult.alerts);
        String requestPayloadFilenameExp = filenameEvalResult.getResultString();

        boolean callTransport = true;
        boolean thisIsAMutationParent = false;
        if (Tools.notBlank(mutatorType) && (contentRawFromMutator == null)) {
            //means we have a mutator, but we are not in a nested call already.
            thisIsAMutationParent = true;
            if (mutatorSkipParent) {
                callTransport = false; //skip because mutator@skipParentrunParent is true, so mutators will run actual calls
                serviceResult.overrideGotExpectedResult();  //still gotta have the parent, saying it gotExpected for the children to run.
                serviceResult.parentSkipped = true;    //informational for the RestReplayReport line.
                serviceResult.failureReason = "";
            }
        }

        if (callTransport) {
            String contentType = contentTypeFromRequestPart(requestPayloadFilenameExp);
            String contentRaw = "";
            if (contentRawFromMutator == null) {
                contentRaw = getResourceManager().readResource("executeTestNode:POST/PUT:" + test.testIDLabel,
                        requestPayloadFilenameRelExp,
                        requestPayloadFilenameExp);
            } else {
                contentRaw = contentRawFromMutator;
                vars = clonedMasterVars;
            }
            if (vars != null) {
                serviceResult.addVars(vars);
            }
            EvalResult evalResult = test.evalStruct.eval("expand req. file:" + requestPayloadFilenameRelExp, contentRaw, vars);
            String contentSubstituted = evalResult.getResultString();
            serviceResult.alerts.addAll(evalResult.alerts);

            serviceResult.requestPayloadFilename = requestPayloadFilenameRelExp;

            /** Use this function for NON-multipart messages, that is, regular POSTs. */
            serviceResult = Transport.doPOST_PUT(
                    serviceResult,  //brings in existing list of Alerts
                    test.fullURL,
                    contentSubstituted,
                    contentRaw,
                    Transport.BOUNDARY,
                    method,
                    contentType,
                    test.authForTest,
                    test.testIDLabel,
                    serviceResult.requestPayloadFilename,//it just sets it back to this for us.
                    test.headerMap);
        }

        test.results.add(serviceResult);
        serviceResultsMap.put(test.testID, serviceResult);
        serviceResultsMap.put("result", serviceResult);
        serviceResult.time = (System.currentTimeMillis() - test.startTime);

        if (Tools.notBlank(mutatorType) && (contentRawFromMutator == null)) {  //means we have a mutator, but we are not in a nested call already.
            if (!serviceResult.gotExpectedResult() && getRunOptions().skipMutatorsOnFailure) {
                serviceResult.mutatorSkipped = true;
            } else if (getRunOptions().skipMutators) {
                serviceResult.mutatorSkippedByOpts = true;
            } else {
                try {
                    IMutator mutator
                         = MutatorFactory.createMutator(mutatorType,
                                                        requestPayloadFilenameRelExp,
                                                        requestPayloadFilenameExp,
                                                        getResourceManager(),
                                                        test.testNode);
                    serviceResult.mutatorType = mutatorType;
                    serviceResult.mutator = mutator;
                    serviceResult.requestPayloadFilename = requestPayloadFilenameExp;

                    Map<String, Object> mutatorScopeVars = new LinkedHashMap<String, Object>();

                    ServiceResult holdThis = serviceResultsMap.get("this");
                    try {
                        ServiceResult childResult;
                        childResult = new ServiceResult(getRunOptions());
                        String content = mutator.mutate(mutatorScopeVars, test.evalStruct, childResult);
                        while (content != null) {
                            serviceResult.addChild(childResult);
                            serviceResultsMap.put("this", childResult);
                            childResult.mutator = mutator;

                            executeTestNode(
                                    childResult,
                                    content,  //only in a mutation is this parameter sent.  We are sending it here.
                                    mutator,
                                    test.testNode,//Node
                                    test.testgroup,//Node
                                    test.protoHostPort,//String    TODO!!
                                    clonedMasterVars,//Map<String,String>
                                    mutatorScopeVars,//Map<String,String>
                                    testElementIndex,//int
                                    test.testGroupID,//String
                                    test.evalStruct,//Eval
                                    authsMap,//AuthsMap
                                    defaultAuths,//AuthsMap
                                    testdir,//String
                                    report,//RestReplayReport
                                    test.results);//List<ServiceResult> results)

                            childResult = new ServiceResult(getRunOptions());
                            content = mutator.mutate(mutatorScopeVars, test.evalStruct, childResult);
                        }
                    } finally {
                        if (holdThis != null) serviceResultsMap.put("this", holdThis);
                    }
                } catch (Exception e){
                    serviceResult.addAlert("Could not create ContentMutator from factory",  Tools.getStackTrace(e), LEVEL.ERROR);
                    System.out.println("Could not create ContentMutator from factory\r\n "+Tools.getStackTrace(e));
                }
            }
        }

    }

    private void writeRowToConsoleWDump(ServiceResult serviceResult, boolean hasError, String testIDLabel) {
        Dump dump = getDump();
        boolean doingAuto = (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.auto);
        String serviceResultRow = serviceResult.dump(dump.dumpServiceResult, hasError);
        String leader = (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.detailed) ? "RestReplay:" + testIDLabel + ": " : "";

        if ((dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.detailed)
                || (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.full)) {
            System.out.println("\r\n#\r\n#========= " + testIDLabel + " ============#\r\n#");
        }

        System.out.println(timeString() + " " + leader + serviceResultRow + "\r\n");

        if (dump.payloads && (doingAuto && hasError)) {
            //the call to serviceResultRow = serviceResult.dump(...) does something similar under non-error conditions.
            // here we are handling error conditions with "auto" and forcing out payloads.
            System.out.println(serviceResult.dumpPayloads());
        }
    }

    private void doDeleteURL(ServiceResult serviceResult, Node testNode, Eval evalStruct, String testID, String testIDLabel, Map<String, Object> clonedMasterVarsWTest) {
        String deleteURL = testNode.valueOf("deleteURL");
        if (Tools.notBlank(deleteURL)) {
            EvalResult evalResult = null;
            evalResult = evalStruct.eval("deleteURL", deleteURL, clonedMasterVarsWTest);
            serviceResult.alerts.addAll(evalResult.alerts);

            if (Tools.notBlank(serviceResult.deleteURL)) {
                serviceResult.addAlert("deleteURL computed by Location (" + serviceResult.deleteURL + ")"
                                + " is being replaced by " + testID + ".deleteURL value: " + deleteURL
                                + " which evaluates to: " + evalResult.getResultString(),
                        testIDLabel,
                        LEVEL.WARN
                );
            }
            serviceResult.deleteURL = evalResult.getResultString();
        }
    }

    private void handleExports(ServiceResult serviceResult, Node testNode, Eval evalStruct, Map<String, Object> clonedMasterVarsWTest) {
        Node exportsNode = testNode.selectSingleNode("exports");
        if (exportsNode != null) {
            Map<String, Object> exports = readVars(exportsNode);
            Map<String, Object> exportsEvald = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Object> entry : exports.entrySet()) {
                String exportID = entry.getKey();
                Object expr = entry.getValue();
                boolean ebes = getRunOptions().errorsBecomeEmptyStrings;
                try {
                    getRunOptions().errorsBecomeEmptyStrings = false;
                    //System.out.println("---->eval export: "+expr);
                    EvalResult evalResult = evalStruct.eval("export vars", ""+expr, clonedMasterVarsWTest);
                    //System.out.println("      ---->"+evalResult.getResultString()+"<--"+evalResult.alerts+serviceResult.xmlResult);
                    //exportsEvald.put(exportID, evalResult.getResultString());
                    exportsEvald.put(exportID, evalResult.result);
                    serviceResult.alerts.addAll(evalResult.alerts);
                } finally {
                    getRunOptions().errorsBecomeEmptyStrings = ebes;
                }
            }
            serviceResult.addExports(exportsEvald);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static final void addAlertErrorToAlerts(List<Alert> alerts, Object t) {
        if (t instanceof AlertError) {
            alerts.addAll(((AlertError) t).allAlerts);
        }
    }

    private static String timeString() {
        java.util.Date date = new java.util.Date();
        java.sql.Timestamp ts = new java.sql.Timestamp(date.getTime());
        return ts.toString();
    }

    private static String contentTypeFromRequestPart(String filename) {
        if (filename.toUpperCase().endsWith(".JSON")) {
            return "application/json";
        } else if (filename.toUpperCase().endsWith(".XML")) {
            return Transport.APPLICATION_XML;
        }
        return Transport.APPLICATION_XML;
    }

    //======================== MAIN ===================================================================

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("help", false, "RestReplay Help");
        options.addOption("selftest", false, "RestReplay selftest");
        options.addOption("pause", false, "RestReplay pause before selftest");
        options.addOption("port", true, "RestReplay selftest port");
        options.addOption("testdir", true, "default/testdir");
        options.addOption("reports", true, "default/reports");
        options.addOption("testGroup", true, "default/testGroup");
        options.addOption("test", true, "default/test");
        options.addOption("env", true, "dev");
        options.addOption("autoDeletePOSTS", true, "true");
        options.addOption("dumpResults", true, "true");
        options.addOption("control", true, "control.xml");
        options.addOption("master", true, "master.xml");

        return options;
    }

    //TODO: this is obviated......Need to set the default arg names in Apache https://commons.apache.org/proper/commons-cli/apidocs/org/apache/commons/cli/HelpFormatter.html
    public static String usage() {
        String result = "org.dynamide.restreplay.RestReplay {args}\r\n"
                + " args: \r\n"
                + "  -testdir <dir> \r\n"
                + "  -reports <dir> \r\n"
                + "  -master <filename> \r\n"
                + "  -control <filename> \r\n"
                + "  -testGroup <ID> \r\n"
                + "  -test <ID> \r\n"
                + "  -env <ID> \r\n"
                + "  -dumpResults true|false \r\n"
                + "  -autoDeletePOSTS true|false \r\n"
                + "  -selftest \r\n"
                + "  -pause \r\n"
                + "  -port <selftest-server-port>\r\n"
                + "   \r\n"
                + " Note: -DautoDeletePOSTS won't force deletion if set to false in control file."
                + "   \r\n"
                + "   \r\n"
                + " You may also override these program args with system args, e.g.: \r\n"
                + "   -Dtestdir=/path/to/dir \r\n"
                + "   \r\n";
        return result;
    }

    public static void printHelp(Options options){
        String header = "Run a RestReplay test, control, or master\n\n";
        String footer = "\nDocumentation at https://github.com/dynamide/RestReplay";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("RestReplay", header, options, footer, true);

        System.out.println("\r\n"
                + " Note: -DautoDeletePOSTS won't force deletion if set to false in control file."
                + "   \r\n"
                + "   \r\n"
                + " You may also override these program args with system args, e.g.: \r\n"
                + "   -Dtestdir=/path/to/dir \r\n"
                + "   \r\n");

    }

    private static String opt(CommandLine line, String option) {
        String result;
        String fromProps = System.getProperty(option);
        if (Tools.notEmpty(fromProps)) {
            return fromProps;
        }
        if (line == null) {
            return "";
        }
        result = line.getOptionValue(option);
        if (result == null) {
            result = "";
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        Options options = createOptions();
        EmbeddedServer selfTestServer = null;
        //System.out.println("System CLASSPATH: "+prop.getProperty("java.class.path", null));
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            String testdir = opt(line, "testdir");
            String reportsDir = opt(line, "reports");
            String testGroupID = opt(line, "testGroup");
            String testID = opt(line, "test");
            String envID = opt(line, "env");
            String autoDeletePOSTS = opt(line, "autoDeletePOSTS");
            String dumpResultsFromCmdLine = opt(line, "dumpResults");
            String controlFilename = opt(line, "control");
            String restReplayMaster = opt(line, "master");
            String selfTestPort = opt(line, "port");
            String pause = opt(line, "pause");
            if (line.hasOption("help")){
               //System.out.println(usage());
               printHelp(options);
               System.exit(0);
            }

            if (line.hasOption("selftest")){
                restReplayMaster = "_self_test/master-self-test.xml";
                if (Tools.isBlank(selfTestPort)){
                    selfTestPort = ""+EmbeddedServer.DEFAULT_PORT;
                }
                if (line.hasOption("pause")){
                    //Go and start jvisualvm or jconsole now...  then type a character and hit Enter.
                    DataInputStream in = new DataInputStream(System.in);
                    System.out.println("Start debugging tool, then press Enter to resume.");
                    byte b = in.readByte();
                    char ch = (char) b;
                    System.out.println("Char : " + ch);
                }
                selfTestServer = new EmbeddedServer();
                selfTestServer.startServer(selfTestPort);
                System.out.println("selftest server started on port: "+selfTestPort);
            }

            if (Tools.isBlank(reportsDir)) {
                reportsDir = testdir + '/' + RestReplayTest.REPORTS_DIRNAME;
            }
            reportsDir = Tools.fixFilename(reportsDir);
            testdir = Tools.fixFilename(testdir);
            controlFilename = Tools.fixFilename(controlFilename);

            boolean bAutoDeletePOSTS = true;
            if (Tools.notEmpty(autoDeletePOSTS)) {
                bAutoDeletePOSTS = Tools.isTrue(autoDeletePOSTS);
            }
            boolean bDumpResults = false;
            if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                bDumpResults = Tools.isTrue(dumpResultsFromCmdLine);
            }
            if (Tools.isEmpty(testdir)) {
                System.err.println("ERROR: testdir was not specified.");
                return;
            }
            File f = null, fMaster = null;
            String fMasterPath = "";
            if (Tools.isEmpty(restReplayMaster)) {
                if (Tools.isEmpty(controlFilename)) {
                    System.err.println("Exiting.  No Master file (empty) and Control file not found (empty)");
                    return;
                }
                f = new File(Tools.glue(testdir, "/", controlFilename));
                if (!f.exists()) {
                    System.err.println("Control file not found: " + f.getCanonicalPath());
                    return;
                }
            } else {
                if (selfTestServer==null) {
                    //if running selfTestServer, then it loads from classpath.  Only construct full path if NOT doing selftest.
                    fMaster = new File(Tools.glue(testdir, "/", restReplayMaster));
                    if (Tools.notEmpty(restReplayMaster) && !fMaster.exists()) {
                        System.err.println("Master file not found: " + fMaster.getCanonicalPath());
                        return;
                    }
                    fMasterPath =  fMaster.getCanonicalPath();
                }
            }

            String testdirResolved = (new File(testdir)).getCanonicalPath();
            System.out.println("RestReplay ::"
                            + "\r\n    testdir: " + testdir
                            + "\r\n    testdir(resolved): " + testdirResolved
                            + "\r\n    control: " + controlFilename
                            + "\r\n    master: " + restReplayMaster
                            + "\r\n    testGroup: " + testGroupID
                            + "\r\n    test: " + testID
                            + "\r\n    env: " + envID
                            + "\r\n    autoDeletePOSTS: " + bAutoDeletePOSTS
                            + (Tools.notEmpty(restReplayMaster)
                            ? ("\r\n    will use master file: " + fMasterPath)
                            : ("\r\n    will use control file: " + f.getCanonicalPath()))
            );

            ResourceManager rootResourceManager = ResourceManager.createRootResourceManager();

            if (Tools.notEmpty(restReplayMaster)) {
                //****************** RUNNING MASTER ******************************************
                Master master = new Master(testdirResolved, reportsDir, rootResourceManager);
                if (Tools.notBlank(selfTestPort)){
                    master.getVars().put("SELFTEST_PORT", selfTestPort);
                }
                master.setEnvID(envID);
                master.readOptionsFromMasterConfigFile(restReplayMaster);
                master.setAutoDeletePOSTS(bAutoDeletePOSTS);
                Dump dumpFromMaster = master.getDump();
                if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                    dumpFromMaster.payloads = bDumpResults;
                }
                master.setDump(dumpFromMaster);
                if (Tools.notEmpty(controlFilename)) {
                    //******* RUN CONTROL, using MASTER
                    System.out.println("INFO: control: " + controlFilename + " will be used within master specified: " + restReplayMaster);
                    master.runMaster(restReplayMaster,
                            false,
                            controlFilename,
                            testGroupID,
                            testID);
                } else {
                    //******** RUN MASTER
                    master.runMaster(restReplayMaster, false); //false, because we already just read the options, and override a few.
                }
            } else {
                //****************** RUNNING CONTROL, NO MASTER ******************************
                RestReplay restReplay = new RestReplay(testdirResolved, reportsDir, rootResourceManager, null);
                restReplay.readDefaultRunOptions();
                Dump dump = Dump.getDumpConfig();
                if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                    dump.payloads = bDumpResults;
                }
                restReplay.setDump(dump);
                List<String> reportsList = new ArrayList<String>();
                restReplay.runRestReplayFile(
                        testdirResolved,
                        controlFilename,
                        testGroupID,
                        testID,
                        null,
                        bAutoDeletePOSTS,
                        "",
                        null,
                        reportsList,
                        reportsDir,
                        ""/*no master, so no env*/,
                        "");
                //No need to dump the reportsList because we were just running one test, and its report gets created and reported on command line OK.
                // System.out.println("DEPRECATED: reportsList is generated, but not dumped: "+reportsList.toString());
            }
        } catch (ParseException exp) {
            System.err.println("Cmd-line parsing failed.  Reason: " + exp.getMessage());
            //System.err.println(usage());
            printHelp(options);
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (selfTestServer!=null){
                selfTestServer.stopServer();
            }
        }
    }
}

