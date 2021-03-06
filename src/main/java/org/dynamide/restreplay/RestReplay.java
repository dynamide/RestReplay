package org.dynamide.restreplay;

import com.google.gson.Gson;

import org.dynamide.interpreters.*;
import org.dynamide.restreplay.RestReplayReport.Header;
import org.dynamide.restreplay.mutators.IMutator;
import org.dynamide.restreplay.mutators.MutatorFactory;
import org.dynamide.restreplay.TreeWalkResults.TreeWalkEntry.STATUS;
import org.dynamide.util.FileTools;
import org.dynamide.util.Tools;
import org.dom4j.*;

import java.io.*;
import java.util.*;

import org.dynamide.interpreters.Alert.LEVEL;

/**
 * This class is used to replay a request to the Services layer, by sending the XML or JSON payload
 * in an appropriate Multipart request.  This class is a runtime representation of a control file, which starts with the XML
 * element &lt;restReplay>.
 * See example usage in the calling class {@see Main}, and the calling maven surefire classes RestReplayTest and RestReplaySelfTest.
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

    public static final String REL_PATH_TO_DB = "db";

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

    private String masterEnvsFileLocation;
    public String getMasterEnvsFileLocation() {
        return masterEnvsFileLocation;
    }
    public void setMasterEnvsFileLocation(String masterEnvsFileLocation) {
        this.masterEnvsFileLocation = masterEnvsFileLocation;
    }

    private String masterVarsFileLocation;
    public String getMasterVarsFileLocation() {
        return masterVarsFileLocation;
    }
    public void setMasterVarsFileLocation(String masterVarsFileLocation) {
        this.masterVarsFileLocation = masterVarsFileLocation;
    }

    private String relToMasterPath = "";
    private String relToMaster = "";
    public String getRelToMaster(){
        return relToMaster;
    }
    public String getRelToMasterURL(){
        return relToMasterPath+relToMaster;
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

    private Map<String, ServiceResult> masterNamespace = null;
    public void setMasterNamespace(Map<String, ServiceResult> namespace){
        masterNamespace = namespace;
    }
    public Map<String, ServiceResult> getMasterNamespace(){
        return masterNamespace;
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
    public List<ServiceResult> runTests(String testGroupID, String testID, String runID, Integer runHashCount,
                                        List<RestReplayReport.Header>testGroups) throws Exception {
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
                this.getMasterFilename(),
                runID,
                runHashCount,
                testGroups);
        return result;
    }

    public List<ServiceResult> autoDelete(String logName) {
        return autoDelete(this.serviceResultsMap, logName);
    }

    /**
     * Use this method to clean up resources created on the server that returned CSIDs, if you have
     * specified autoDeletePOSTS==false, which means you are managing the cleanup yourself.
     * POST and PUT requests automatically should be able to be autodeleted,
     *  they will cause an error if they don't have a deleteURL.
     * GET requests will not cause an error if they don't have a deleteURL.
     *
     * @param theServiceResultsMap a Map of ServiceResult objects, which will contain ServiceResult.deleteURL.
     * @return a List<ServiceResult> including error result for which URLs could not be deleted.
     */
    public List<ServiceResult> autoDelete(Map<String, ServiceResult> theServiceResultsMap, String logName) {
        List<ServiceResult> results = new ArrayList<ServiceResult>();
        for (ServiceResult pr : theServiceResultsMap.values()) {
            try {
                if (Tools.notEmpty(pr.deleteURL)) {
                    ServiceResult deleteResult = new ServiceResult(pr.getRunOptions());
                    deleteResult.controlFileName = controlFileName;
                    deleteResult.isAutodelete = true;
                    deleteResult.testID = pr.testID+"_autodelete";
                    deleteResult.testGroupID = pr.testGroupID;
                    deleteResult.connectionTimeout = pr.connectionTimeout;
                    deleteResult.socketTimeout = pr.socketTimeout;
                    long startTime = System.currentTimeMillis();
                    deleteResult = Transport.doDELETE(deleteResult, pr.deleteURL, pr.auth, pr.testID, "[autodelete:" + logName + "]");
                    deleteResult.time = (System.currentTimeMillis() - startTime);
                    writeRowToConsoleWDump(deleteResult, true, deleteResult.testID);
                    results.add(deleteResult);
                } else {
                    if (pr.method.equals(Transport.POST)) {
                        ServiceResult errorResult = new ServiceResult(pr.getRunOptions());
                        errorResult.addError("deleteURL not found, could not autodelete.");
                        errorResult.controlFileName = controlFileName;
                        errorResult.isAutodelete = true;
                        errorResult.method = "DELETE";
                        errorResult.fullURL = pr.fullURL;
                        errorResult.testID = pr.testID + "_autodelete";
                        errorResult.testGroupID = pr.testGroupID;
                        errorResult.fromTestID = pr.fromTestID;
                        errorResult.overrideGotExpectedResult();
                        results.add(errorResult);
                        writeRowToConsoleWDump(errorResult, true, errorResult.testID);
                        //System.out.println("DONE AUTODELETE (errorResult): ==>" + pr.deleteURL + "<== : " + errorResult);
                    }
                }
            } catch (Throwable t) {
                String s = (pr != null) ? "ERROR while attempting to autodelete a ServiceResult: " + pr + ",   using URL:  \"" + pr.deleteURL + "\",  Exception: " + t
                        : "ERROR while attempting to autodelete a ServiceResult (with a null ServiceResult): " + t;
                System.err.println(s);
                String theTestID = (pr != null) ? pr.testID : "test_ID_unknown";
                ServiceResult errorResult = new ServiceResult(null);
                errorResult.controlFileName = controlFileName;
                errorResult.testID = theTestID+"_autodelete";
                errorResult.isAutodelete = true;
                errorResult.fullURL = pr.fullURL;
                errorResult.testGroupID = pr.testGroupID;
                errorResult.fromTestID = pr.fromTestID;
                errorResult.testGroupID = pr.testGroupID;
                errorResult.addError(s, t);
                results.add(errorResult);
                writeRowToConsoleWDump(errorResult, true, errorResult.testID);
                //System.out.println("DONE AUTODELETE (Throwable): ==>" + pr.deleteURL + "<== : " + errorResult + " t:" + t);
            }
        }
        return results;
    }

    private static class PartsStruct {
        public List<Map<String, Object>> varsList = new ArrayList<Map<String, Object>>();
        public List<String> uploadFilenames = new ArrayList<String>();
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
        public static PartsStruct readParts(Node testNode,
                                            final String testID,
                                            String testdir,
                                            boolean isResponse,
                                            ResourceManager rm,
                                            ServiceResult serviceResult,
                                            Eval evalStruct,
                                            Map<String, Object> masterVars) {
            PartsStruct resultPartsStruct = new PartsStruct();
            resultPartsStruct.isResponse = isResponse;
            resultPartsStruct.startElement = testNode.valueOf("startElement");
            resultPartsStruct.label = testNode.valueOf("label");
            resultPartsStruct.testdir = testdir;
            String filename = testNode.valueOf("filename");

            //TODO: pass in evalStruct so you can eval these filenames to allow for vars.

            if (isResponse) {
                resultPartsStruct.validator = testNode.valueOf("validator");  //the actual script, hopefully in a CDATA.
                resultPartsStruct.validatorFilenameRel = testNode.valueOf("validator/@filename");
                resultPartsStruct.validatorLang = testNode.valueOf("validator/@lang");
            }

            if (!isResponse) {
                List<Node> uploadFilenameNodes = testNode.selectNodes("upload/filename");
                if (uploadFilenameNodes != null && uploadFilenameNodes.size()>0){
                    for (Node var: uploadFilenameNodes){
                        String value = var.getText();
                        resultPartsStruct.uploadFilenames.add(value);
                    }
                }
            }

            if (Tools.notEmpty(filename)) {
                if (isResponse) {
                    resultPartsStruct.expectedResponseFilename = testdir + '/' + filename;
                    resultPartsStruct.expectedResponseFilenameRel = filename;
                } else {
                    resultPartsStruct.requestPayloadFilename = testdir + '/' + filename;
                    resultPartsStruct.requestPayloadFilenameRel = filename;
                }
                resultPartsStruct.varsList.add(readVars(testNode, rm, serviceResult, evalStruct, masterVars));
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

    protected void runValidatorScriptSinglePayload(ServiceResult serviceResult,
                                                   PartsStruct expectedResponseParts,
                                                   Eval evalStruct)
    throws Exception {
        serviceResult.beginTrappingExports();
        int errorCountBefore = serviceResult.alertsCount(LEVEL.ERROR);
        int warnCountBefore = serviceResult.alertsCount(LEVEL.WARN);
        EvalResult validationResult = runValidatorScript(serviceResult, expectedResponseParts, evalStruct);
        String validationResultStr = validationResult != null ? validationResult.toString() : "";
        if (validationResult != null) {
            List<String> exports = serviceResult.endTrappingExports();
            int errorCountAfter = serviceResult.alertsCount(LEVEL.ERROR);
            int warnCountAfter = serviceResult.alertsCount(LEVEL.WARN);

            String validatorDisplayName = expectedResponseParts.validatorFilenameRel;
            if (Tools.notBlank(expectedResponseParts.validator)) {
                validatorDisplayName = "[inline]";
            }
            String counts = "";
            if ((errorCountAfter - errorCountBefore) > 0) {
                counts = "<div><b>errors:</b> " + (errorCountAfter - errorCountBefore) + "</div>";

            }
            if ((warnCountAfter - warnCountBefore) > 0) {
                counts += "<div><b>warnings:</b> " + (warnCountAfter - warnCountBefore) + "</div>";
            }
            String exportString = exports.size() > 0
                    ? "<div><b>exports:</b> <div class='validator-exports'>" + exports + "</div></div>"
                    : "";
            String validatorResultBlock = Tools.notBlank(validationResultStr)
                    ? "<b>validator result:</b><br /><span class='validator-result'>" + validationResultStr + "</span>"
                    : "";
            serviceResult.addAlert(validatorResultBlock
                            + exportString
                            + counts,
                    "<b>validator:</b> " + validatorDisplayName,
                    validationResult.worstLevel
            );
        }
    }

    protected String validateResponse(ServiceResult serviceResult,
                                      PartsStruct expectedResponseParts,
                                      Eval evalStruct) {
        String OK = "";
        if (expectedResponseParts == null) return OK;
        if (serviceResult == null) return OK;
        //NEWcomment. if (serviceResult.getResult().length() == 0) return OK;
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

                evalResult = evalStruct.eval("expanding:"+expFilename,
                    expFilename,
                    varsDup);
                expFilename = evalResult.getResultString();


                ResourceManager.Resource expectedPartContentRES
                        = getResourceManager().readResource("validateResponseSinglePayload",
                                                             expFilenameRel,
                                                             expFilename);
                if (expectedPartContentRES.provider == ResourceManager.Resource.SOURCE.NOTFOUND) {
                    serviceResult.addAlertError("Resource not found: "+expectedPartContentRES.toString(),
                                                "executeTestNode:POST/PUT:" + serviceResult.testIDLabel);
                    expectedPartContent = "";
                } else {
                    expectedPartContent = expectedPartContentRES.contents;
                }
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
            runValidatorScriptSinglePayload(serviceResult, expectedResponseParts, evalStruct);//this is also run below...
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
                if (serviceResult.method.equalsIgnoreCase(Transport.NOOP)){

                } else {
                    System.out.println("ERROR: " + serviceResult.testID + " expectedPartContent but no xmlResult sr:" + serviceResult
                            + " CONTENT:" + RestReplayReport.dotdotdot(expectedPartContent));
                }
            }
        } else {
            System.out.println("expectedPartContent blank in test "+serviceResult.testID);
        }

        runValidatorScriptSinglePayload(serviceResult, expectedResponseParts, evalStruct);//this is also run above in case of early return.

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
        String fullPath = FileTools.join(testdir, scriptFilename);

        String source = null;
        String resourceName = "";

        if (Tools.notBlank(scriptBody)) {
            source = scriptBody;
            resourceName = "inline validator";
        } else if (Tools.notBlank(scriptFilename)) {
            ResourceManager.Resource rez
                = getResourceManager().readResource("runValidatorScript", scriptFilename, fullPath);

            if (rez.provider == ResourceManager.Resource.SOURCE.NOTFOUND) {
                serviceResult.addAlertError("Resource not found: "+rez.toString(),
                        "executeTestNode:POST/PUT:" + serviceResult.testIDLabel);
                source = "";
            } else {
                source = rez.contents;
            }
            resourceName = FileTools.join(testdir, scriptFilename);
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

    /** script-facing class known in context as "loop" so that the values for key, value and index may be
     * retrieved with loop.key, loop.value, and loop.index, for each iteration of the test.
     */
    public static class Loop {
        public Loop(int i, String k, Object v, Object o){
            key = k;
            value = v;
            index = i;
            object = o;
        }

        public Loop(Eval evalStruct){
            Object res = evalStruct.jc.get("loop.index");
            if (null == res ){
                return;
            }
            index = (Integer.parseInt(res.toString()));

            value = evalStruct.jc.get("loop.value");
            this.object = evalStruct.jc.get("loop.object");

            res = evalStruct.jc.get("loop.key");
            if (null == res){
                return;
            }
            key = res.toString();
        }
        public String toString(){
            return "{"
                    +"\"index\":"+index
                    +",\"key\":"+key
                    +(value==null ? "null" : ",\"value\":\""+value.toString()+"\"")
                    +(object==null ? "null" : ",\"object\":\""+object.toString()+"\"")
                    +"}";
        }
        public int index = -1;
        public Object value = null;
        public Object object = null;
        public String key = "";
    }

    private EvalResult evalJavascript(Eval evalStruct, String resourceName, String source, ServiceResult serviceResult) {
        RhinoInterpreter interpreter = new RhinoInterpreter(getRunOptions().dumpJavascriptEvals);
        interpreter.setVariable("result", serviceResult);
        interpreter.setVariable("serviceResult", serviceResult);
        interpreter.setVariable("serviceResultsMap", serviceResultsMap);
        interpreter.setVariable("kit", KIT);
        interpreter.setVariable("tools", TOOLS);
        Loop loop = new Loop(evalStruct);
        interpreter.setVariable("loop", loop);
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
            } else {
                interpreter.setVariable(entry.getKey(), entry.getValue());
                //System.out.println("adding ServiceResults: "+entry.getKey());
            }
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

    public String dumpMasterNamespace() {
        Map<String, ServiceResult> masterNamespace = getMasterNamespace();
        if (masterNamespace == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, ServiceResult> entry : masterNamespace.entrySet()) {
            buffer.append("\r\n        ")
                  .append(entry.getKey())
                  .append(" --> ServiceResult: ")
                  .append(entry.getValue().testIDLabel)
                  .append(" seq:"+entry.getValue().getSequence());
        }
        return buffer.toString();
    }

    private String getFilterImportsDump(Map<String,ServiceResult> map){
        //if (map==null || map.size()==0){
        if (map.size()==0){
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append("   imports: {\r\n");
        for (Map.Entry<String, ServiceResult> entry : map.entrySet()) {
            b.append("       " + entry.getKey())
             .append(" --> ");
            ServiceResult val = entry.getValue();
            if (val==null) {
                b.append("NULL\r\n");
            } else {
                b.append(val.controlFileName)
                 .append(":")
                 .append(val.testIDLabel)
                 .append(" [")
                 .append(val.getSequence())
                 .append("]")
                 .append("\r\n");
            }
        }
        b.append("   }");
        return b.toString();
    }

    /** This class handles an import element in the control file, for example:
     * <code>&lt;import ID="myImportedTokenTest" control="_self_test/self-test.xml" testGroup="login" test="token" /></code>
     */
    protected static class ImportFilter {
        Map<String,ServiceResult> map;
        String key = "";
        String ID = "";
        String control = "";
        String runID = "";
        String testGroup = "";
        String test = "";
        String failureMessage = "";
        boolean failure = false;

        public Map<String,ServiceResult> filter(Node testgroup, Map<String,ServiceResult> masterNamespace) {
            map = new LinkedHashMap<String,ServiceResult>();
            List<Node> nodeList = testgroup.selectNodes("imports/import");
            for (Node importNode : nodeList) {
                ID = importNode.valueOf("@ID");
                control = importNode.valueOf("@control");
                testGroup = importNode.valueOf("@testGroup");
                runID = importNode.valueOf("@runID");
                test = importNode.valueOf("@test");
                key = makeImportsKey(control, testGroup, test, runID);
                ServiceResult sr = masterNamespace.get(key);
                if (sr!=null){
                    map.put(ID, sr);
                } else {
                    map.put(ID, null);
                    failure = true;
                    failureMessage += "\timport missing: \""+key+"\"\r\n";
                }
            }
            return map;
        }
        public static String makeImportsKey(String controlFileName, String testGroup, String test, String runID){
            String runIDSegment = Tools.notBlank(runID) ? ":"+runID : "";
            return controlFileName+':'+testGroup+runIDSegment+'.'+test;
        }
    }

    private boolean handleImports(Eval evalStruct,
                               RestReplayReport report,
                               Node testgroup,
                               String currentTestGroupID)
    {
        String RUNINFOLINE = "========================================================================";

        ImportFilter importFilter = new ImportFilter();
        importFilter.filter(testgroup, masterNamespace);

        evalStruct.importsNamespace = importFilter.map;
        String filterImportsStr = getFilterImportsDump(importFilter.map);

        if (Tools.notBlank(filterImportsStr)){
            report.addRunInfo(filterImportsStr);
        }
        System.out.println(RUNINFOLINE);
        System.out.print(report.getRunInfo());
        System.out.println(RUNINFOLINE+"\n\r");

        if (importFilter.failure) {
            String errstring = "ERROR: Bad import, skipping test group: ["+controlFileName+":"+currentTestGroupID+"]"
                    +"\r\n  failureMessage: \r\n"+importFilter.failureMessage+"\r\n";
            report.addFailure(errstring);
            System.out.println(errstring);
            return false;
        }
        return true;
    }

    //================= runRestReplayFile ======================================================

    private static int gTestGroupID = 0;

    public List<ServiceResult> runRestReplayFile(
            String testdir,
            String controlFileName,
            String testGroupID,
            String oneTestID,
            Map<String, Object> masterVarsOverride,
            boolean param_autoDeletePOSTS,
            String protoHostPortParam,
            AuthsMap authsFromMaster,
            List<String> reportsList,
            String reportsDir,
            String relativePathFromReportsDir,
            String masterFilenameInfo,
            String runID,
            Integer runHashCount,
            List<RestReplayReport.Header> testGroups)
            throws Exception {

        if (masterVarsOverride!=null){
            masterVars = masterVarsOverride;
        }
        //Internally, we maintain two collections of ServiceResult:
        //  the first, 'results', is the return value of this method.
        //  the second is this.serviceResultsMap, which is used for keeping track of CSIDs created by POSTs, for later reference by DELETE, etc.
        List<ServiceResult> results = new ArrayList<ServiceResult>();

        //RestReplayReport report = new RestReplayReport(reportsDir);

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

        boolean noMaster = false;
        if (Tools.isBlank(masterFilenameInfo)) {
            masterFilenameInfo = "AUTOGENERATED-MASTER";
            this.relToMaster = (RestReplayReport.calculateMasterReportRelname(reportsDir, masterFilenameInfo, this.getEnvID())).relname;
            noMaster = true;
        }

        String restReplayHeader =
                  "RestReplay running:"
                + "\r\n   version: " + ResourceManager.getRestReplayVersion()
                + "\r\n   runID: " + runID
                + "\r\n   runHashCount,: " + runHashCount
                + "\r\n   testGroup: " + testGroupID
                + "\r\n   controlFile: " + controlFileName
                + "\r\n   Master: " + masterFilenameInfo
                + "\r\n   env: " + relativePathFromReportsDir
                + (Tools.notEmpty(masterEnvsFileLocation) ? "\r\n   masterEnvsFile: " + masterEnvsFileLocation : "")
                + (Tools.notEmpty(masterVarsFileLocation) ? "\r\n   masterVarsFile: " + masterVarsFileLocation : "")
                + "\r\n   reports directory: " + reportsDir
                + "\r\n   protoHostPort: " + protoHostPort + "    " + protoHostPortFrom
                + (Tools.notEmpty(oneTestID) ? "\r\n   oneTestID: " + oneTestID : "")
                + "\r\n   auths map: " + authsMapINFO
                + "\r\n   masterVars: " + dumpMasterVars(masterVars)
                + "\r\n   param_autoDeletePOSTS: " + param_autoDeletePOSTS
                + "\r\n   Dump info: " + getDump()
                + ((getRunOptions().dumpRunOptions) ? "\r\n   RunOptions: " + getRunOptions() : "\r\n   RunOptions.dumpRunOptions: false" )
                ;

        String autoDeletePOSTS = "";

        Eval evalStruct = new Eval();
        evalStruct.runOptions = this.getRunOptions();
        evalStruct.serviceResultsMap = this.serviceResultsMap;

        String OUTERTestGroupID = testGroupID;

        List<Node> testgroupNodes;
        if (Tools.notEmpty(testGroupID)) {
            testgroupNodes = document.selectNodes("//testGroup[@ID='" + testGroupID + "']");   //If you specify the testGroup then the loop will only happen once.
        } else {
            testgroupNodes = document.selectNodes("//testGroup");   //When the command line has -control but no -testGroup argument.
        }


        OUTER:
        for (Node testgroup : testgroupNodes) {
            String currentTestGroupID = testgroup.valueOf("@ID");

            Node commentNode = testgroup.selectSingleNode("comment");
            String comment = "";
            if (commentNode!=null) {
                comment = commentNode.asXML();
            }
            RestReplayReport report = new RestReplayReport(reportsDir, relativePathFromReportsDir);

            testGroupID = currentTestGroupID;

            report.clearRunInfo();
            evalStruct.resetContext();    // Get a new JexlContext for each test group.


            int idx = -1;
            Header foundHdr = Header.findInList(testGroups, currentTestGroupID);
            if (foundHdr!=null){
                idx = foundHdr.index+1;
            }
            String anchor = (idx > -1) ? currentTestGroupID+'_'+(idx+1) : currentTestGroupID;
            anchor = anchor + "_"+(gTestGroupID++);
            RestReplayReport.Header hdr = report.addTestGroup(currentTestGroupID, controlFileName, runID, comment, anchor, idx);
            testGroups.add(hdr);

            //vars var = get control file vars and merge masterVars into it, replacing
            Map<String, Object> testGroupVars = readVars(testgroup, getResourceManager(), null, evalStruct, masterVars);
            Map<String, Object> clonedMasterVars = new LinkedHashMap<String, Object>();
            if (null != masterVars) {
                clonedMasterVars.putAll(masterVars);
            }
            clonedMasterVars.putAll(testGroupVars);

            report.addRunInfo(restReplayHeader);
            if (!currentTestGroupID.equals(OUTERTestGroupID)) {
                report.addRunInfo("   currentTestGroupID: " + currentTestGroupID);
            }

            boolean importOK = handleImports(evalStruct, report, testgroup, currentTestGroupID);
            if (importOK ) {
                autoDeletePOSTS = testgroup.valueOf("@autoDeletePOSTS");
                List<Node> tests;
                if (Tools.notEmpty(oneTestID)) {
                    tests = testgroup.selectNodes("test[@ID='" + oneTestID + "']");
                } else {
                    tests = testgroup.selectNodes("test");
                }
                int testElementIndex = -1;
                TESTNODES:
                for (Node testNode : tests) {
                    ServiceResult serviceResult = null;
                    String testID = "";
                    try {
                        testID = testNode.valueOf("@ID");
                        EvalResult token = evalStruct.setCurrentTestIDLabel(testGroupID + '.' + testID + " <span class='LABEL'>(preflight)</span>");
                        LoopHelper loopHelper = LoopHelper.getIterationsLoop(testElementIndex, testGroupID, testNode, evalStruct, clonedMasterVars, getRunOptions(), report, results);
                        evalStruct.popLastEvalReportItemIfUnused(token);
                        if (loopHelper.error) {
                            continue TESTNODES;  //syntax error in test/@loop, Go to the next test. getIterationsLoop creates an error ServiceResult, adds it to the reports and map.
                        }
                        if (loopHelper.numIterations==0){
                            serviceResult = new ServiceResult(getRunOptions());
                            serviceResult.controlFileName = controlFileName;
                            serviceResult.testID = testID;
                            serviceResult.addAlertOK("ZERO loops run:"+loopHelper.toString());
                            serviceResult.expectedFailure = true;
                            serviceResult.failureReason = "";
                            report.addTestResult(serviceResult);
                        }
                        for (int itnum = 0; itnum < loopHelper.numIterations; itnum++) {
                            serviceResult = new ServiceResult(getRunOptions());
                            serviceResult.controlFileName = controlFileName;
                            loopHelper.setGlobalVariablesForLooping(serviceResult, evalStruct, itnum);
                            serviceResultsMap.remove("result");  //special value so deleteURL can reference ${result.got(...)}.  "result" gets added after each of GET, POST, PUT, DELETE, LIST.
                            testElementIndex++;
                            serviceResultsMap.put("this", serviceResult);
                            String namespaceKey = ImportFilter.makeImportsKey(controlFileName, testGroupID, testID, runID);
                            if (getMasterNamespace() != null) {
                                getMasterNamespace().put(namespaceKey, serviceResult);
                            }
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
                            if (getRunOptions().outputServiceResultDB) {
                                saveServiceResultToJSON(serviceResult);
                            }
                        }
                    } catch (Exception e){
                        if (serviceResult==null){
                            serviceResult = new ServiceResult(getRunOptions());
                            serviceResult.testID = testID;
                        }
                        serviceResult.addError("Exception caught attempting to run testNode", e);
                        report.addTestResult(serviceResult);
                    }
                }
                serviceResultsMap.remove("result");
                if (Tools.isTrue(autoDeletePOSTS) && param_autoDeletePOSTS) {
                    List<ServiceResult> deleteResults = autoDelete(serviceResultsMap, "default");
                    for (ServiceResult r : deleteResults) {
                        serviceResultsMap.put(r.testID + "_delete", r);
                        report.addTestResult(r);
                    }
                }
            } // else, still produce a report, so that import errors will show.

            this.evalReport = evalStruct.getEvalReport();

            //=== Now spit out the HTML report file ===
            //    This will happen for each testGroup.
            File m = new File(controlFileName);  //don't instantiate, just use File to extract file name without directory.
            File parentFile = m.getParentFile();
            String relpath = parentFile!=null?parentFile.toString():"";
            this.relToMasterPath = calculateElipses(relpath);
            String runIDSegment = "";
            if (Tools.notBlank(runID)) {
                runIDSegment = "-"+runID;
            } else {
                runIDSegment = "-"+runHashCount;
            }
            String reportName = controlFileName + '-' + testGroupID + runIDSegment + ".html";

            File resultFile = report.saveReport(testdir, reportsDir, reportName, this, testGroupID);
            if (resultFile != null) {
                String toc = report.getTOC(relativePathFromReportsDir + reportName);
                reportsList.add(toc);
            }

        }        //END OUTER loop over testGroups

        if (noMaster){
            RestReplayReport.saveIndexNoMaster(
                    getResourceManager(),
                    testdir,
                    reportsDir,
                    masterFilenameInfo,
                    reportsList
            );
        }

        if (getRunOptions().dumpResourceManagerSummary) {
            System.out.println(getResourceManager().formatSummaryPlain());
        }
        if (false) System.out.println(dumpMasterNamespace());
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
            Map<String,Object>  temp = readVars(testNode, getResourceManager(), serviceResult, evalStruct, clonedMasterVars);
            clonedMasterVarsWTest.putAll(temp);
            if (mutatorScopeVars!=null){
                clonedMasterVarsWTest.putAll(mutatorScopeVars);
            }

            String testID = testNode.valueOf("@ID")
                               + (Tools.notBlank(idFromMutator) ? "_" + idFromMutator : "")
                               + serviceResult.getLoopQualifier(evalStruct);

            lastTestID = testID;
            String testIDLabel = Tools.notEmpty(testID) ? (testGroupID + '.' + testID) : (testGroupID + '.' + testElementIndex)
                    + "mut:" + idFromMutator + ";";
            this.setCurrentTestIDLabel(testIDLabel);
            lastTestLabel = testIDLabel;
            String method = testNode.valueOf("method");
            if (Tools.isBlank(method)){
                method = "GET";
            }
            String uri = testNode.valueOf("uri");
            String mutatorType = testNode.valueOf("mutator/@type");
            boolean mutatorSkipParent = Tools.isTrue(testNode.valueOf("mutator/@skipParent"));

            Node commentNode = testNode.selectSingleNode("comment");
            serviceResult.comment = commentNode!=null?commentNode.asXML():"";

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
            String inheritHeaders = testNode.valueOf("@inheritHeaders");
            boolean skipInheritHeaders = Tools.notBlank(inheritHeaders) && inheritHeaders.equalsIgnoreCase("FALSE");
            if (!skipInheritHeaders) {
                readHeaders(testGroupNode, evalStruct, serviceResult, serviceResult.requestHeaderMap, clonedMasterVarsWTest);  //inserts into requestHeaderMap, condensing multiples.
            }
            readHeaders(testNode, evalStruct, serviceResult, serviceResult.requestHeaderMap, clonedMasterVarsWTest);  //inserts into requestHeaderMap, condensing multiples.
            //========END Headers=====================

            String oneProtoHostPort = protoHostPort;
            if (oneProtoHostPort.indexOf("$") > -1) {
                //TODO: strange, this first call seems to run all the evals.
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
                expectedResponseParts = PartsStruct.readParts(responseNode, testID, testdir, true, getResourceManager(), serviceResult, evalStruct, clonedMasterVars);
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
                serviceResult = Transport.doGET(serviceResult, fullURL, authForTest, testIDLabel);
                results.add(serviceResult);
                serviceResult.time = (System.currentTimeMillis() - startTime);
                serviceResultsMap.put(testID, serviceResult);
                serviceResultsMap.put("result", serviceResult);
            } else if (method.equalsIgnoreCase("LIST")) {
                String listQueryParams = ""; //TODO: empty for now, later may pick up from XML control file.
                serviceResult = Transport.doLIST(serviceResult, fullURL, listQueryParams, authForTest, testIDLabel);
                results.add(serviceResult);
                serviceResult.time = (System.currentTimeMillis() - startTime);
                serviceResultsMap.put(testID, serviceResult);
                serviceResultsMap.put("result", serviceResult);
            } else if (method.equalsIgnoreCase(Transport.NOOP)) {
                serviceResult.overrideExpectedResult = true;
                System.out.println("Processing "+Transport.NOOP+" request for side-effects.");
            } else {
                throw new Exception("HTTP method not supported by RestReplay: " + method);
            }

            if (Tools.isEmpty(serviceResult.testID)) serviceResult.testID = testIDLabel;
            if (Tools.isEmpty(serviceResult.testGroupID)) serviceResult.testGroupID = testGroupID;

            List<Node> failures = testNode.selectNodes("response/expected/failure");
            if (failures.size()>0){
                serviceResult.expectedFailure = true;
            }
            Node expectedLevel = testNode.selectSingleNode("response/expected");
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
                    test.fromTestID);
            serviceResult.time = (System.currentTimeMillis() - test.startTime);
            serviceResult.fromTestID = fromTestID;
            test.results.add(serviceResult);
            if (serviceResult.codeInSuccessRange(serviceResult.responseCode)) {  //gotExpectedResult depends on serviceResult.expectedCodes.
                //TODO: not sure about this one
                serviceResultsMap.remove(fromTestID);
            }
        } else {
            if (Tools.notEmpty(fromTestID)) {
                serviceResult = new ServiceResult(getRunOptions());
                serviceResult.controlFileName = controlFileName;
                serviceResult.responseCode = 0;
                serviceResult.addError("ID not found in element fromTestID: " + fromTestID);
                System.err.println("****\r\nServiceResult: " + serviceResult.getError() + ". SKIPPING TEST. Full URL: " + test.fullURL);
            } else {
                serviceResult = Transport.doDELETE(serviceResult, test.fullURL, test.authForTest, test.testID, fromTestID);
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

        PartsStruct parts = PartsStruct.readParts(test.testNode, test.testID, testdir, false, getResourceManager(), test.serviceResult, test.evalStruct, clonedMasterVars);
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
            String contentType = serviceResult.requestHeaderMap.get("Content-Type");
            if (Tools.isBlank(contentType)) {
                contentType = contentTypeFromRequestPart(requestPayloadFilenameExp);
            }
            String contentRaw = "";
            if (contentRawFromMutator == null) {

                ResourceManager.Resource cachedResource
                        = getResourceManager().readResource("executeTestNode:POST/PUT",
                            requestPayloadFilenameRelExp,
                            requestPayloadFilenameExp);

                if (cachedResource.provider == ResourceManager.Resource.SOURCE.NOTFOUND) {
                    serviceResult.addAlertError("Resource not found: "+cachedResource.toString(),
                                                 "executeTestNode:POST/PUT:" + test.testIDLabel);
                    contentRaw = "";
                } else {
                    contentRaw = cachedResource.contents;
                }
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
                    parts.uploadFilenames
            );
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
                        childResult.controlFileName = controlFileName;
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
                                    test.protoHostPort,//String
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
                            childResult.controlFileName = controlFileName;
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
            Map<String, Object> exports = readVars(exportsNode, getResourceManager(), serviceResult, evalStruct, clonedMasterVarsWTest);
            Map<String, Object> exportsEvald = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Object> entry : exports.entrySet()) {
                String exportID = entry.getKey();
                Object expr = entry.getValue();
                boolean ebes = getRunOptions().errorsBecomeEmptyStrings;
                try {
                    getRunOptions().errorsBecomeEmptyStrings = false;
                    //System.out.println("---->eval export: "+expr);
                    EvalResult evalResult = evalStruct.eval("export vars: <b>"+exportID+"</b>", ""+expr, clonedMasterVarsWTest);
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
            return Transport.APPLICATION_JSON;
        } else if (filename.toUpperCase().endsWith(".XML")) {
            return Transport.APPLICATION_XML;
        }
        return Transport.APPLICATION_XML;
    }

    public File saveServiceResultToJSON(ServiceResult serviceResult)
    throws IOException {
        serviceResult.populateSerializedFields();
        Gson gson = new Gson();
        String json = gson.toJson(serviceResult);
        String dir = "";
        int lastdot = serviceResult.controlFileName.lastIndexOf('.');
        if (lastdot>0){
            dir = serviceResult.controlFileName.substring(0, lastdot);
        }
        dir = FileTools.join(reportsDir, FileTools.join(REL_PATH_TO_DB,dir));

        File result = FileTools.saveFile(dir, serviceResult.testIDLabel+'.'+serviceResult.getSequence()+".json", json, true);
        System.out.println("ServiceResult saved to DB: "+result.getCanonicalPath());
        return result;
    }



}

