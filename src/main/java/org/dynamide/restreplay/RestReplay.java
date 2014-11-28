package org.dynamide.restreplay;

import org.apache.commons.cli.*;

import org.apache.commons.jexl2.JexlEngine;
import org.dynamide.util.Tools;
import org.dom4j.*;

import java.io.*;
import java.util.*;

import org.dynamide.restreplay.ServiceResult.Alert;
import org.dynamide.restreplay.ServiceResult.Alert.LEVEL;
import org.dynamide.restreplay.Eval.EvalResult;
import org.dynamide.restreplay.ServiceResult.AlertError;

/**  This class is used to replay a request to the Services layer, by sending the XML payload
 *   in an appropriate Multipart request.
 *   See example usage in calling class RestReplayTest in services/IntegrationTests, and also in main() in this class.
 *   @author Laramie Crocker
 */
public class RestReplay {

    public RestReplay(String basedir, String reportsDir, ResourceManager manager, RunOptions parentRunOptions){
        this.basedir = basedir;
        this.serviceResultsMap = createResultsMap();
        this.reportsList = new ArrayList<String>();
        this.reportsDir = reportsDir;
        this.resourceManager = manager;
        if (parentRunOptions!=null) {
            this.runOptions = parentRunOptions;
        } else {
            this.runOptions = new RunOptions();
            try {
                Document doc = this.resourceManager.getDocument("RestReplay:constructor:runOptions", basedir, RunOptions.RUN_OPTIONS_FILENAME);
                if (doc != null) {
                    this.runOptions.addRunOptions(doc.getRootElement(), "default");
                }
            } catch (DocumentException de) {
                System.err.println("ERROR: could not read default runOptions.xml");
            }
        }
    }

    //TODO: make sure that the report gets all the alerts
    //TODO: check breaking scenarios and RunOptions.
    //TODO: config from master control file.
    private RunOptions runOptions;

    public RunOptions getRunOptions(){
        return runOptions;
    }
    private void setRunOptions(RunOptions options){
        this.runOptions = options;
    }

    private ResourceManager resourceManager;
    public ResourceManager getResourceManager(){
        return resourceManager;
    }

    public static final String DEFAULT_CONTROL = "rest-replay-control.xml";
    public static final String DEFAULT_MASTER_CONTROL = "master.xml";
    public static final String DEFAULT_DEV_MASTER_CONTROL = "dev-master.xml";

    private String envID = "";
    public String getEnvID() {
        return envID;
    }
    public void setEnvID(String envID) {
        this.envID = envID;
        if (Tools.notBlank(envID)) {
            this.relativePathFromReportsDir = envID + '/';
        } else {
            this.relativePathFromReportsDir = "";
        }
    }

    private String reportsDir = "";
    public String getReportsDir(){
        return reportsDir;
    }

    private String relativePathFromReportsDir = "";
    /** There is no setter.  It is set when you call setEnvID() */
    public String getRelativePathFromReportsDir(){
        return relativePathFromReportsDir;
    }

    private String basedir = ".";  //set from constructor.
    public String getBaseDir(){
        return basedir;
    }
    
    private String controlFileName = DEFAULT_CONTROL;
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
    public void setMasterFilename(String val){
        this.masterFilename = val;
    }

    private String protoHostPort = "";
    public String getProtoHostPort() {
        return protoHostPort;
    }
    public void setProtoHostPort(String protoHostPort) {
        this.protoHostPort = protoHostPort;
    }

    private boolean autoDeletePOSTS = true;
    public boolean isAutoDeletePOSTS() {
        return autoDeletePOSTS;
    }
    public void setAutoDeletePOSTS(boolean autoDeletePOSTS) {
        this.autoDeletePOSTS = autoDeletePOSTS;
    }

    private Dump dump;
    public Dump getDump() {
        return dump;
    }
    public void setDump(Dump dump) {
        this.dump = dump;
    }

    AuthsMap defaultAuthsMap;
    public AuthsMap getDefaultAuthsMap(){
        return defaultAuthsMap;
    }
    public void setDefaultAuthsMap(AuthsMap authsMap){
        defaultAuthsMap = authsMap;
    }

    public Map<String,String> masterVars = new HashMap<String,String>();
    public Map<String, String> getMasterVars() {
        return masterVars;
    }
    public void setMasterVars(Map<String, String> masterVars) {
        this.masterVars = masterVars;
    }

    private Map<String, ServiceResult> serviceResultsMap;
    public Map<String, ServiceResult> getServiceResultsMap(){
        return serviceResultsMap;
    }
    public static Map<String, ServiceResult> createResultsMap(){
        return new HashMap<String, ServiceResult>();
    }

    private List<String> reportsList;
    public  List<String> getReportsList(){
        return reportsList;
    }

    public String toString(){
        return "RestReplay{"+this.basedir+", "+this.defaultAuthsMap+", "+this.dump+", "+this.reportsDir+'}';
    }

    public static String testToString(Node testNode) {
        return testNode.valueOf("@ID");
    }

    // ============== METHODS ===========================================================

    /** Optional information method: call this method after instantiating this class using the constructor RestReplay(String), which sets the basedir.  Then you
     *   pass in your relative masterFilename to that basedir to this method, which will return true if the file is readable, valid xml, etc.
     *   Do this in preference to  just seeing if File.exists(), because there are rules to finding the file relative to the maven test dir, yada, yada.
     *   This method makes it easy to have a development test file that you don't check in, so that dev tests can be missing gracefully, etc.
     */
    public boolean masterConfigFileExists(String masterFilename){
        try {
            org.dom4j.Document doc = openMasterConfigFile("masterConfigFileExists", masterFilename);
            if (doc==null){
                return false;
            }
            return true;
        } catch (Throwable t){
            return false;
        }
    }

    public org.dom4j.Document openMasterConfigFile(String reason, String masterFilename) throws FileNotFoundException {
        try {
            return getResourceManager().getDocument("openMasterConfigFile:" + reason, basedir, masterFilename);
        } catch (DocumentException de) {
            System.out.println("$$$$$$ ERROR: " + de);
            throw new FileNotFoundException("RestReplay master control file (" + masterFilename + ") contains error or not found in basedir: " + basedir + ". Exiting test. " + de);
        }
    }

    /** specify the master config file, relative to getBaseDir(), but ignore any tests or testGroups in the master.
     *  Depends on this.getEnvID() being set before this method is called, otherwise uses default envID found in master.
     *  @return a Document object, which you don't need to use: all options will be stored in this RestReplay instance.
     */
    public org.dom4j.Document readOptionsFromMasterConfigFile(String masterFilename) throws FileNotFoundException {
        org.dom4j.Document document = openMasterConfigFile("readOptionsFromMasterConfigFile", masterFilename);
        if (document == null){
            throw new FileNotFoundException(masterFilename);
        }
        Node node = document.selectSingleNode("/restReplayMaster/protoHostPort");
        if (null!=node) {
            protoHostPort = node.getText().trim();
        }
        AuthsMap authsMap = readAuths(document);
        setDefaultAuthsMap(authsMap);
        Dump dump = RestReplay.readDumpOptions(document);
        setDump(dump);

        String desiredEnv = this.getEnvID();
        Node masterNode = document.selectSingleNode("/restReplayMaster");
        EnvResult res = selectEnv(masterNode, desiredEnv);
        Node nodeWVars = res.nodeWEnvs;
        if (null!=nodeWVars){
            this.setEnvID(res.envID);
        } else {
            nodeWVars = masterNode;
        }
        setMasterVars(readVars(nodeWVars));


        this.runOptions.addRunOptions(document.selectSingleNode("/restReplayMaster/runOptions"), "master");
        return document;
    }

    private static class EnvResult {
        public Node nodeWEnvs;
        public String envID;
    }
    /** @return a Node that contains vars/var elements, based on the desired environment string passed in.
     *          Sends back the node identified by default="true" if no name match on desiredEnv.
     *          Only returns null if envs/env struct is not found in master file, or if no node was marked default AND
     *          no node ID matched. */
    private EnvResult selectEnv(Node nodeWEnvs, String desiredEnv){
        Node defaultEnv = null;
        EnvResult defaultResult = new EnvResult();
        List<Node> envNodes = nodeWEnvs.selectNodes("envs/env");
        for (Node env: envNodes){
            String ID = env.valueOf("@ID");
            String isDefault = env.valueOf("@default");
            if (Tools.notBlank(isDefault)){
                defaultResult.nodeWEnvs = env;
                defaultResult.envID = ID;
            }
            if (desiredEnv.equalsIgnoreCase(ID)){
                EnvResult result = new EnvResult();
                result.envID = ID;
                result.nodeWEnvs = env;
                return result;
            }
        }
        return defaultResult;
    }

    public List<List<ServiceResult>> runMaster(String masterFilename) throws Exception {
        return runMaster(masterFilename, true);
    }

    /** Creates new instances of RestReplay, one for each controlFile specified in the master,
     *  and setting defaults from this instance, but not sharing ServiceResult objects or maps. */
    public List<List<ServiceResult>> runMaster(String masterFilename, boolean readOptionsFromMaster) throws Exception {
        //System.out.println(">>> masterFilename: "+masterFilename);
        List<List<ServiceResult>> list = new ArrayList<List<ServiceResult>>();
        org.dom4j.Document document;
        if (readOptionsFromMaster){
            document = readOptionsFromMasterConfigFile(masterFilename);  //side-effects: sets fields of "this".
        } else {
            document = openMasterConfigFile("runMaster", masterFilename);
        }
        if (document==null){
            throw new FileNotFoundException(masterFilename);
        }

        String saveReportsDir = this.reportsDir;

        String controlFile, testGroup, test;
        List<Node> runNodes;
        runNodes = document.selectNodes("/restReplayMaster/run");
        for (Node runNode : runNodes) {
            controlFile = runNode.valueOf("@controlFile");
            testGroup = runNode.valueOf("@testGroup");
            test = runNode.valueOf("@test"); //may be empty

            String envReportsDir = saveReportsDir;
            //Create a new instance and clone only config values, not any results maps.
            if (Tools.notBlank(this.getEnvID())){
                envReportsDir = Tools.glue(saveReportsDir,"/",this.relativePathFromReportsDir);
            }
            RestReplay replay = new RestReplay(basedir, envReportsDir, this.getResourceManager(), this.runOptions);//this.reportsDir);
            replay.setEnvID(this.envID);  //internally sets replay.relativePathFromReportsDir
            replay.setControlFileName(controlFile);
            replay.setProtoHostPort(protoHostPort);
            replay.setAutoDeletePOSTS(isAutoDeletePOSTS());
            replay.setDump(dump);
            replay.setDefaultAuthsMap(getDefaultAuthsMap());
            replay.setRunOptions(this.runOptions);
            replay.setMasterFilename(masterFilename);

            Map<String,String> runVars = readVars(runNode);
            Map<String,String> masterVarsDup = new HashMap<String,String>();
            masterVarsDup.putAll(masterVars);
            masterVarsDup.putAll(runVars);
            replay.setMasterVars(masterVarsDup);

            //======================== Now run *that* instance. ======================
                List<ServiceResult> results = replay.runTests(testGroup, test);
            //========================================================================

            list.add(results);
            this.reportsList.addAll(replay.getReportsList());   //Add all the reports from the inner replay, to our master replay's reportsList, to generate the index.html file.
        }
        RestReplayReport.saveIndexForMaster(basedir, reportsDir, masterFilename, this.reportsList, this.getEnvID(), masterVars, this);
        return list;
    }

    /** Use this if you wish to run named tests within a testGroup, otherwise call runTestGroup(). */
    public List<ServiceResult>  runTests(String testGroupID, String testID) throws Exception {
        List<ServiceResult> result = runRestReplayFile(
                                                        this.basedir,
                                                        this.controlFileName,
                                                        testGroupID,
                                                        testID,
                                                        this.masterVars,
                                                        this.autoDeletePOSTS,
                                                        this.protoHostPort,
                                                        this.defaultAuthsMap,
                                                        this.reportsList,
                                                        this.reportsDir,
                                                        this.relativePathFromReportsDir,
                                                        this.getMasterFilename());
        return result;
    }

    /** Use this if you wish to specify just ONE test to run within a testGroup, otherwise call runTestGroup(). */
    public ServiceResult  runTest(String testGroupID, String testID) throws Exception {
        List<ServiceResult> result = runRestReplayFile(
                                                        this.basedir,
                                                        this.controlFileName,
                                                        testGroupID,
                                                        testID,
                                                        null, //masterVars -- for now, when running stand-alone test, there are no masterVars.
                                                        this.autoDeletePOSTS,
                                                        this.protoHostPort,
                                                        this.defaultAuthsMap,
                                                        this.reportsList,
                                                        this.reportsDir,
                                                        this.relativePathFromReportsDir,
                                                        this.getMasterFilename());
        if (result.size()>1){
            throw new IndexOutOfBoundsException("Multiple ("+result.size()+") tests with ID='"+testID+"' were found within test group '"+testGroupID+"', but there should only be one test per ID attribute.");
        }
        return result.get(0);
    }

    /** Use this if you wish to run all tests within a testGroup.*/
    public List<ServiceResult> runTestGroup(String testGroupID) throws Exception {
        //NOTE: calling runTest with empty testID runs all tests in a test group, but don't expose this fact.
        // Expose this method (runTestGroup) instead.
        return runTests(testGroupID, "");
    }

    public List<ServiceResult>  autoDelete(String logName){
        return autoDelete(this.serviceResultsMap, logName);
    }

    /** Use this method to clean up resources created on the server that returned CSIDs, if you have
     *  specified autoDeletePOSTS==false, which means you are managing the cleanup yourself.
     * @param serviceResultsMap a Map of ServiceResult objects, which will contain ServiceResult.deleteURL.
     * @return a List<String> of debug info about which URLs could not be deleted.
     */
    public static List<ServiceResult> autoDelete(Map<String, ServiceResult> serviceResultsMap, String logName){
        List<ServiceResult> results = new ArrayList<ServiceResult>();
        for (ServiceResult pr : serviceResultsMap.values()){
            try {
                if (Tools.notEmpty(pr.deleteURL)){
                    ServiceResult deleteResult = new ServiceResult();
                    deleteResult.connectionTimeout = pr.connectionTimeout;
                    deleteResult.socketTimeout = pr.socketTimeout;
                    System.out.println("ATTEMPTING AUTODELETE: ==>"+pr.deleteURL+"<==");
                    deleteResult = Transport.doDELETE(deleteResult, pr.deleteURL, pr.auth, pr.testID, "[autodelete:" + logName + "]", pr.headerMap);
                    System.out.println("DONE AUTODELETE: ==>"+pr.deleteURL+"<== : "+deleteResult);
                    results.add(deleteResult);
                } else {
                    ServiceResult errorResult = new ServiceResult();
                    errorResult.fullURL = pr.fullURL;
                    errorResult.testGroupID = pr.testGroupID;
                    errorResult.fromTestID = pr.fromTestID;
                    errorResult.overrideGotExpectedResult();
                    results.add(errorResult);
                    System.out.println("DONE AUTODELETE (errorResult): ==>" + pr.deleteURL + "<== : " + errorResult);
                }
            } catch (Throwable t){
                String s = (pr!=null) ? "ERROR while cleaning up ServiceResult map: "+pr+" for "+pr.deleteURL+" :: "+t
                                      : "ERROR while cleaning up ServiceResult map (null ServiceResult): "+t;
                System.err.println(s);
                ServiceResult errorResult = new ServiceResult();
                errorResult.fullURL = pr.fullURL;
                errorResult.testGroupID = pr.testGroupID;
                errorResult.fromTestID = pr.fromTestID;
                errorResult.addError(s,t);
                results.add(errorResult);
                System.out.println("DONE AUTODELETE (Throwable): ==>"+pr.deleteURL+"<== : "+errorResult+" t:"+t);
            }
        }
        return results;
    }

    public static class AuthsMap {
        Map<String,String> map;
        String defaultID="";
        public String getDefaultAuth(){
            return map.get(defaultID);
        }
        public String toString(){
            return "AuthsMap: {default='"+defaultID+"'; "+map.keySet()+'}';
        }
    }

    public static AuthsMap readAuths(org.dom4j.Document document){
        Map<String, String> map = new HashMap<String, String>();
        List<Node> authNodes = document.selectNodes("//auths/auth");
        for (Node auth : authNodes) {
            map.put(auth.valueOf("@ID"), auth.getStringValue());
        }
        AuthsMap authsMap = new AuthsMap();
        Node auths = document.selectSingleNode("//auths");
        String defaultID = "";
        if (auths != null){
            defaultID = auths.valueOf("@default");
        }
        authsMap.map = map;
        authsMap.defaultID = defaultID;
        return authsMap;
    }

    public static class Dump {
        public boolean payloads = false;
        //public static final ServiceResult.DUMP_OPTIONS dumpServiceResultOptions = ServiceResult.DUMP_OPTIONS;
        public ServiceResult.DUMP_OPTIONS dumpServiceResult = ServiceResult.DUMP_OPTIONS.minimal;
        public String toString(){
            return "payloads: "+payloads+", dumpServiceResult: "+dumpServiceResult;
        }
    }

    public static Dump getDumpConfig(){
        return new Dump();
    }

    public static Dump readDumpOptions(org.dom4j.Document document){
        Dump dump = getDumpConfig();
        Node dumpNode = document.selectSingleNode("//dump");
        if (dumpNode != null){
            dump.payloads = Tools.isTrue(dumpNode.valueOf("@payloads"));
            String dumpServiceResultStr = dumpNode.valueOf("@dumpServiceResult");
            if (Tools.notEmpty(dumpServiceResultStr)){
                dump.dumpServiceResult = ServiceResult.DUMP_OPTIONS.valueOf(dumpServiceResultStr);
            }
        }
        return dump;
    }

    private static class PartsStruct {
        public List<Map<String,String>> varsList = new ArrayList<Map<String,String>>();
        String requestPayloadFilename = "";
        String requestPayloadFilenameRel = "";
        String expectedResponseFilename = "";
        String expectedResponseFilenameRel = "";
        String overrideTestID = "";
        String startElement = "";
        String label = "";

        //This method is overloaded with the isResponse boolean.  If isResponse, we are reading test/response/filename,
        //  otherwise we are reading test/filename. These should be split into two functions, because the XML structs
        //  are different--they just have the /vars element in common.
        public static PartsStruct readParts(Node testNode, final String testID, String basedir, boolean isResponse){
            PartsStruct resultPartsStruct = new PartsStruct();
            resultPartsStruct.startElement = testNode.valueOf("startElement");
            resultPartsStruct.label = testNode.valueOf("label");
            String filename = testNode.valueOf("filename");
            if (Tools.notEmpty(filename)){
                if (isResponse) {
                    resultPartsStruct.expectedResponseFilename = basedir + '/' + filename;
                    resultPartsStruct.expectedResponseFilenameRel = filename;
                } else {
                    resultPartsStruct.requestPayloadFilename = basedir + '/' + filename;
                    resultPartsStruct.requestPayloadFilenameRel = filename;
                }
                resultPartsStruct.varsList.add(readVars(testNode));
            }
            return resultPartsStruct;
        }

        /*private static void readVars(Node testNode, List<Node> varNodes, PartsStruct resultPartsStruct){
            Map<String,String> vars = new HashMap<String,String>();
            resultPartsStruct.varsList.add(vars);
            for (Node var: varNodes){
                String ID = var.valueOf("@ID");
                String value = var.getText();
                //System.out.println("ID: "+ID+" value: "+value);
                vars.put(ID, value); //vars is already part of resultPartsStruct.varsList
            }
        }*/
    }

    private static Map<String,String> readVars(Node nodeWVars){
        Map<String,String> vars = new HashMap<String,String>();
        List<Node> varNodes = nodeWVars.selectNodes("vars/var");
        return readVars(varNodes, vars);
    }

    private static Map<String,String> readVars(List<Node> varNodes, Map<String,String> vars){
        for (Node var: varNodes){
            String ID = var.valueOf("@ID");
            String value = var.getText();
            vars.put(ID, value);
        }
        return vars;
    }

    private static String fixupFullURL(String protoHostPort, String uri){
        String fullURL;
        if (uri.startsWith("http")){
            return uri;
        }
        if ( ! uri.startsWith(protoHostPort)){
            fullURL = Tools.glue(protoHostPort, "/", uri);
        } else {
            fullURL = uri;
        }
        return fullURL;
    }

    private static String fromTestID(String fullURL, Node testNode, Map<String, ServiceResult> serviceResultsMap){
        String fromTestID = testNode.valueOf("fromTestID");
        if (Tools.notEmpty(fromTestID)){
            ServiceResult sr = serviceResultsMap.get(fromTestID);
            if (sr != null){
                fullURL = sr.location;
            }
        }
        return fullURL;
    }

    private static String CSIDfromTestID(Node testNode, Map<String, ServiceResult> serviceResultsMap){
        String result = "";
        String fromTestID = testNode.valueOf("fromTestID");
        if (Tools.notEmpty(fromTestID)){
            ServiceResult getPR = serviceResultsMap.get(fromTestID);
            if (getPR != null){
                result = getPR.location;
            }
        }
        return result;
    }



    /* See, for example of <expected> : test/resources/test-data/restreplay/objectexit/object-exit.xml */
    protected String validateResponseSinglePayload(ServiceResult serviceResult,
                                                 PartsStruct expectedResponseParts,
                                                 Eval evalStruct)
    throws Exception {
        String OK = "";
        String expectedPartContent = getResourceManager().readResource("validateResponseSinglePayload", expectedResponseParts.expectedResponseFilenameRel,
                expectedResponseParts.expectedResponseFilename);
        Map<String,String> vars = expectedResponseParts.varsList.get(0);  //just one part, so just one varsList.  Must be there, even if empty.
        EvalResult evalResult = evalStruct.eval(expectedResponseParts.expectedResponseFilenameRel,
                                                expectedPartContent,
                                                vars);

        expectedPartContent = evalResult.result;
        serviceResult.alerts.addAll(evalResult.alerts);
        //TODO: may need to break if runoptions dictate.  But what to set/return?...  if (runOptions.breakNow(evalResult.alerts))

        serviceResult.expectedContentExpanded = expectedPartContent;
        String label = "NOLABEL";
        String leftID  = "{from expected part, label:"+label+" filename: "+expectedResponseParts.expectedResponseFilename+"}";
        String rightID = "{from server, label:"+label
                            +" fromTestID: "+serviceResult.fromTestID
                            +" URL: "+serviceResult.fullURL
                            +"}";
        String startElement = expectedResponseParts.startElement;
        String partLabel = expectedResponseParts.label;
        if (Tools.isBlank(startElement)){
            if (Tools.notBlank(partLabel))
            startElement = "/document/*[local-name()='"+partLabel+"']";
        }
        TreeWalkResults.MatchSpec matchSpec = TreeWalkResults.MatchSpec.createDefault();
        TreeWalkResults list =
            XmlCompareJdom.compareParts(expectedPartContent,
                    leftID,
                    serviceResult.getResult(),
                    rightID,
                    startElement,
                    matchSpec);
        serviceResult.addPartSummary(label, list);
        return OK;
    }

    protected String validateResponse(ServiceResult serviceResult,
                                             PartsStruct expectedResponseParts,
                                             Eval evalStruct){
        String OK = "";
        if (expectedResponseParts == null) return OK;
        if (serviceResult == null) return OK;
        if (serviceResult.getResult().length() == 0) return OK;
        try {
            return validateResponseSinglePayload(serviceResult, expectedResponseParts, evalStruct);
        } catch (Exception e){
            String err = "ERROR in RestReplay.validateResponse() : "+e;
            return err  ;
        }
    }

    private static Map<String,String> readHeaders(Node testNode,
                                                  Eval evalStruct,
                                                  ServiceResult serviceResult){
        Map<String,String> headerMap = new HashMap<String,String>();
        List<Node> headers = testNode.selectNodes("headers/header");
        for (Node header: headers){
            String headerValue = header.getStringValue();
            String headerName = header.valueOf("@name");
            //System.out.println("header from control file: "+headerName +": "+ headerValue);
            if (headerValue.indexOf("$")>-1){
                EvalResult evalResult = evalStruct.eval(headerName, headerValue, null);
                headerValue = evalResult.result;
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            //System.out.println("eval'd header from control file: "+headerName +": "+ headerValue);
            headerMap.put(headerName, headerValue);
        }
        return headerMap;
    }

    private static String dumpMasterVars(Map<String, String> masterVars){
        if (masterVars == null){
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String,String> entry: masterVars.entrySet()){
           buffer.append("\r\n        ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return buffer.toString();
    }

    //================= runRestReplayFile ======================================================

    public List<ServiceResult> runRestReplayFile(
                                          String basedir,
                                          String controlFileName,
                                          String testGroupID,
                                          String oneTestID,
                                          Map<String, String> masterVars,
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
        document = getResourceManager().getDocument("runRestReplayFile:"+controlFileName+", test:"+testGroupID, basedir, controlFileName); //will check full path first, then checks relative to PWD.
        if (document==null){
            throw new FileNotFoundException("RestReplay control file ("+controlFileName+") not found in classpath, or basedir: "+basedir+" Exiting test.");
        }

        String protoHostPortFrom = "from restReplay Master.";
        String protoHostPort = protoHostPortParam;  //default to the one passed in, which comes from master.
        Node n = document.selectSingleNode("/restReplay/protoHostPort");  //see if control file has override.
        if (null != n){
            protoHostPort = n.getText().trim();
            //System.out.println("Using protoHostPort ('"+protoHostPort+"') from restReplay file ('"+controlFileName+"'), not master.");
            protoHostPortFrom = "from control file.";
        }

        String authsMapINFO;
        AuthsMap authsMap = readAuths(document);
        if (authsMap.map.size()==0){
            if (authsFromMaster!=null) {
                authsMap = authsFromMaster;
                authsMapINFO = "Using auths from master file: " + authsFromMaster;
            } else {
                authsMapINFO = "No auths in control file (and no master)";
            }
        } else {
            authsMapINFO = "Using auths from control file: "+authsMap;
        }

        report.addTestGroup(testGroupID, controlFileName);   //controlFileName is just the short name, without the full path.
        String restReplayHeader = "========================================================================"
                          +"\r\nRestReplay running:"
                          +"\r\n   controlFile: "+controlFileName
                          +"\r\n   Master: "+ masterFilenameInfo
                          +"\r\n   reports directory: "+reportsDir
                          +"\r\n   env: "+relativePathFromReportsDir
                          +"\r\n   protoHostPort: "+protoHostPort+"    "+protoHostPortFrom
                          +"\r\n   testGroup: "+testGroupID
                          + (Tools.notEmpty(oneTestID) ? "\r\n   oneTestID: "+oneTestID : "")
                          +"\r\n   auths map: "+authsMapINFO
                          +"\r\n   masterVars: "+dumpMasterVars(masterVars)
                          +"\r\n   param_autoDeletePOSTS: "+param_autoDeletePOSTS
                          +"\r\n   Dump info: "+dump
                          +"\r\n   RunOptions: "+getRunOptions().toString()
                          +"\r\n========================================================================"
                          +"\r\n";
        report.addRunInfo(restReplayHeader);

        System.out.println(restReplayHeader);

        String autoDeletePOSTS = "";
        List<Node> testgroupNodes;
        if (Tools.notEmpty(testGroupID)){
            testgroupNodes = document.selectNodes("//testGroup[@ID='"+testGroupID+"']");
        } else {
            testgroupNodes = document.selectNodes("//testGroup");
        }

        Eval evalStruct = new Eval();
        evalStruct.runOptions = this.runOptions;
        evalStruct.serviceResultsMap = this.serviceResultsMap;

        for (Node testgroup : testgroupNodes) {
            evalStruct.resetContext();    // Get a new JexlContext for each test group.

            //vars var = get control file vars and merge masterVars into it, replacing
            Map<String,String> testGroupVars = readVars(testgroup);
            Map<String,String> clonedMasterVars = new HashMap<String, String>();
            if (null != masterVars){
                clonedMasterVars.putAll(masterVars);
            }
            clonedMasterVars.putAll(testGroupVars);

            autoDeletePOSTS = testgroup.valueOf("@autoDeletePOSTS");
            List<Node> tests;
            if (Tools.notEmpty(oneTestID)){
                tests = testgroup.selectNodes("test[@ID='"+oneTestID+"']");
            } else {
                tests = testgroup.selectNodes("test");
            }
            int testElementIndex = -1;
            for (Node testNode : tests) {
                serviceResultsMap.remove("result");  //special value so deleteURL can reference ${result.got(...)}.  "result" gets added after each of GET, POST, PUT, DELETE, LIST.
                testElementIndex++;
                ServiceResult serviceResult = new ServiceResult();
                serviceResultsMap.put("this", serviceResult);
                executeTestNode(serviceResult,
                        null,
                        null,
                        testNode,
                        testgroup,
                        protoHostPort,
                        clonedMasterVars,
                        testElementIndex,
                        testGroupID,
                        evalStruct,
                        authsMap,
                        authsFromMaster,
                        basedir,
                        report,
                        results);
                serviceResultsMap.remove("this");
            }
            serviceResultsMap.remove("result");
            if (Tools.isTrue(autoDeletePOSTS)&&param_autoDeletePOSTS){
                autoDelete(serviceResultsMap, "default");
            }
        }

        //=== Now spit out the HTML report file ===
        File m = new File(controlFileName);
        String localName = m.getName();//don't instantiate, just use File to extract file name without directory.

        //String reportName = localName+'-'+testGroupID+".html";
        String reportName = controlFileName+'-'+testGroupID+".html";
        //System.out.println("=======================>>> report name "+reportName);

        File resultFile = report.saveReport(basedir, reportsDir, reportName, this);
        if (resultFile!=null) {
            String toc = report.getTOC(relativePathFromReportsDir+reportName);
            reportsList.add(toc);
        }
        //================================
        if (runOptions.dumpResourceManagerSummary){
            System.out.println(getResourceManager().formatSummaryPlain());
        }

        return results;
    }

    private ServiceResult executeTestNode(
                                        ServiceResult serviceResult,
                                        String contentRawFromMutator,
                                        ContentMutator mutator,
                                        Node testNode,
                                        Node testGroupNode,
                                        String protoHostPort,
                                        Map<String,String> clonedMasterVars,
                                        int testElementIndex,
                                        String testGroupID,
                                        Eval evalStruct,
                                        AuthsMap authsMap,
                                        AuthsMap defaultAuths,
                                        String basedir,
                                        RestReplayReport report,
                                        List<ServiceResult> results){

        final String idFromMutator = (null!=mutator) ? mutator.getMutationID() : "";
        long startTime = System.currentTimeMillis();
        String lastTestID = "";
        String lastTestLabel = "";

        report.addTestResult(serviceResult);

        if (contentRawFromMutator!=null){
            serviceResult.isMutation = true;
        }
        serviceResult.mutator = mutator;

        Map<String,String> clonedMasterVarsWTest = new HashMap<String, String>();

        try {
            clonedMasterVarsWTest.putAll(clonedMasterVars);
            clonedMasterVarsWTest.putAll(readVars(testNode));
            String testID = testNode.valueOf("@ID")+(Tools.notBlank(idFromMutator)? "_"+idFromMutator : "");
            lastTestID = testID;
            String testIDLabel = Tools.notEmpty(testID) ? (testGroupID+'.'+testID) : (testGroupID+'.'+testElementIndex)
                                 +"mut:"+idFromMutator+";";
            lastTestLabel = testIDLabel;
            String method = testNode.valueOf("method");
            String uri = testNode.valueOf("uri");
            String mutatorType = testNode.valueOf("mutator/@type");

            //get default timeouts from master config file.
            serviceResult.connectionTimeout = runOptions.connectionTimeout;
            serviceResult.socketTimeout = runOptions.socketTimeout;
            String authIDForTest = testNode.valueOf("@auth");
            String currentAuthForTest = (authsMap!=null) ? authsMap.map.get(authIDForTest) : "";

            String authForTest = "";
            if (Tools.notEmpty(currentAuthForTest)){
                authForTest = currentAuthForTest; //else just run with current from last loop;
            }
            if (Tools.isEmpty(authForTest) && (defaultAuths!=null)){
                authForTest = defaultAuths.getDefaultAuth();
            }

            //AFTER this, evals will happen, so fields on "this" must be updated:
            serviceResult.testID = testID;
            serviceResult.testIDLabel = testIDLabel;
            serviceResult.idFromMutator = idFromMutator;
            serviceResult.auth = authForTest;
            serviceResult.method = method;


            //====Headers==========================
            Map<String, String> headerMap = readHeaders(testNode, evalStruct, serviceResult);
            String inheritHeaders = testNode.valueOf("@inheritHeaders");
            boolean skipInheritHeaders = Tools.notBlank(inheritHeaders) && inheritHeaders.equalsIgnoreCase("FALSE");
            if ( ! skipInheritHeaders) {
                Map<String, String> headerMapFromTestGroup = readHeaders(testGroupNode, evalStruct, serviceResult);
                headerMap.putAll(headerMapFromTestGroup);
            }

            serviceResult.headerMap = headerMap;
            //========END Headers=====================

            String oneProtoHostPort = protoHostPort;
            if (oneProtoHostPort.indexOf("$")>-1){
                EvalResult evalResult = evalStruct.eval("vars to protoHostPort", oneProtoHostPort, clonedMasterVarsWTest);
                oneProtoHostPort = evalResult.result;
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            if (uri.indexOf("$")>-1){
                EvalResult evalResult = evalStruct.eval("FULLURL", uri, clonedMasterVarsWTest);
                uri = evalResult.result;
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            String fullURL = fixupFullURL(oneProtoHostPort, uri);
            serviceResult.fullURL = fullURL;
            serviceResult.protoHostPort = oneProtoHostPort;

            if (mutator == null) {
                List<Integer> expectedCodes = new ArrayList<Integer>();
                String expectedCodesStr = testNode.valueOf("expectedCodes");
                if (Tools.notEmpty(expectedCodesStr)) {
                    String[] codesArray = expectedCodesStr.split(",");
                    for (String code : codesArray) {
                        expectedCodes.add(new Integer(code.trim()));
                    }
                }
                serviceResult.expectedCodes = expectedCodes;
            }

            Node responseNode = testNode.selectSingleNode("response");
            PartsStruct expectedResponseParts = null;
            if (responseNode!=null){
                expectedResponseParts = PartsStruct.readParts(responseNode, testID, basedir, true);
                //System.out.println("response parts: >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+expectedResponseParts);
            }

            if (clonedMasterVarsWTest!=null) {
                serviceResult.addVars(clonedMasterVarsWTest);
            }

            boolean isPOST = method.equalsIgnoreCase("POST");
            boolean isPUT =  method.equalsIgnoreCase("PUT");
            if ( isPOST || isPUT ) {
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
                                authForTest,
                                startTime),
                            isPUT,
                            method,
                            contentRawFromMutator,
                            mutatorType,
                            basedir,
                            clonedMasterVars,
                            testElementIndex,
                            authsMap,
                            defaultAuths,
                            report);
            } else if (method.equalsIgnoreCase("DELETE")){
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
                        authForTest,
                        startTime));
            } else if (method.equalsIgnoreCase("GET")){
                fullURL = fromTestID(fullURL, testNode, serviceResultsMap);
                serviceResult.fullURL = fullURL;
                serviceResult = Transport.doGET(serviceResult, fullURL, authForTest, testIDLabel, headerMap);
                results.add(serviceResult);
                serviceResult.time = (System.currentTimeMillis()-startTime);
                serviceResultsMap.put(testID, serviceResult);
                serviceResultsMap.put("result", serviceResult);
            } else if (method.equalsIgnoreCase("LIST")){
                String listQueryParams = ""; //TODO: empty for now, later may pick up from XML control file.
                serviceResult = Transport.doLIST(serviceResult, fullURL, listQueryParams, authForTest, testIDLabel, headerMap);
                results.add(serviceResult);
                serviceResult.time = (System.currentTimeMillis()-startTime);
                serviceResultsMap.put(testID, serviceResult);
                serviceResultsMap.put("result", serviceResult);
            } else {
                throw new Exception("HTTP method not supported by RestReplay: "+method);
            }

            if (Tools.isEmpty(serviceResult.testID)) serviceResult.testID = testIDLabel;
            if (Tools.isEmpty(serviceResult.testGroupID)) serviceResult.testGroupID = testGroupID;

            Node expectedLevel = testNode.selectSingleNode("response/expected");
            if (expectedLevel!=null){
                String level = expectedLevel.valueOf("@level");
                serviceResult.payloadStrictness = level;
            }

            handleExports(serviceResult, testNode, evalStruct, clonedMasterVarsWTest);

            //=====================================================
            //  ALL VALIDATION FOR ALL REQUESTS IS DONE HERE:
            //=====================================================
            boolean hasError = false;

            String vError = validateResponse(serviceResult, expectedResponseParts, evalStruct);

            if (Tools.notEmpty(vError)){
                serviceResult.addError(vError);
                serviceResult.failureReason = " : VALIDATION ERROR; ";
                hasError = true;
            }
            if (hasError == false){
                hasError = ! serviceResult.gotExpectedResult();
            }
            if (!hasError){
                doDeleteURL(serviceResult,  testNode, evalStruct, testID, testIDLabel, clonedMasterVarsWTest);
            }
            writeRowToConsoleWDump(serviceResult, hasError, testIDLabel);
        } catch (Throwable t) {
            serviceResultsMap.remove("result");
            String msg = "ERROR: RestReplay experienced an error in a test node ("+testToString(testNode)+"). Throwable: "+Tools.getStackTrace(t);
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


    public static class OneTest {
        public OneTest(
                ServiceResult serviceResult,
                List<ServiceResult> results,
                Node testNode,
                Node testgroup,
                Map<String,String> headerMap,
                Eval evalStruct,
                String testIDLabel,
                String testID,
                String testGroupID,
                String fullURL,
                String authForTest,
                long startTime
        ){
            this.serviceResult = serviceResult;
            this.results = results;
            this.testNode = testNode;
            this.testgroup = testgroup;
            this.headerMap = headerMap;
            this.evalStruct = evalStruct;
            this.testIDLabel = testIDLabel;
            this.testID = testID;
            this.testGroupID =  testGroupID;
            this.fullURL = fullURL;
            this.authForTest = authForTest;
            this.startTime = startTime;
        }
        ServiceResult serviceResult;
        List<ServiceResult> results;
        Node testNode;
        Node testgroup;
        Map<String,String> headerMap;
        Eval evalStruct;
        String testIDLabel;
        String testID;
        String testGroupID;
        String fromTestID;
        String fullURL;
        String authForTest;
        long startTime;
    }
    
    private void doDELETE(OneTest test) {
        String fromTestID = test.testNode.valueOf("fromTestID");
        ServiceResult pr = serviceResultsMap.get(fromTestID);
        ServiceResult serviceResult = test.serviceResult;
        if (pr!=null){
            serviceResult = Transport.doDELETE(serviceResult,
                    pr.deleteURL,
                    test.authForTest,
                    test.testIDLabel,
                    test.fromTestID,
                    test.headerMap);
            serviceResult.time = (System.currentTimeMillis()-test.startTime);
            serviceResult.fromTestID = fromTestID;
            test.results.add(serviceResult);
            if (serviceResult.codeInSuccessRange(serviceResult.responseCode)){  //gotExpectedResult depends on serviceResult.expectedCodes.
                serviceResultsMap.remove(fromTestID);
            }
        } else {
            if (Tools.notEmpty(fromTestID)){
                serviceResult = new ServiceResult();
                serviceResult.responseCode = 0;
                serviceResult.addError("ID not found in element fromTestID: "+fromTestID);
                System.err.println("****\r\nServiceResult: "+serviceResult.getError()+". SKIPPING TEST. Full URL: "+test.fullURL);
            } else {
                serviceResult = Transport.doDELETE(serviceResult, test.fullURL, test.authForTest, test.testID, fromTestID, test.headerMap);
            }
            serviceResult.time = (System.currentTimeMillis()-test.startTime);
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
                           String basedir,
                           Map<String,String> clonedMasterVars,
                           int testElementIndex,
                           AuthsMap authsMap,
                           AuthsMap defaultAuths,
                           RestReplayReport report)
    throws IOException {
        ServiceResult serviceResult = test.serviceResult;

        PartsStruct parts = PartsStruct.readParts(test.testNode, test.testID, basedir, false);
        if (Tools.notEmpty(parts.overrideTestID)) {
            test.testID = parts.overrideTestID;
        }
        if (isPUT) {
            test.fullURL = fromTestID(test.fullURL, test.testNode, serviceResultsMap);
            serviceResult.fullURL = test.fullURL;
        }
        //vars only make sense in two contexts: POST/PUT, because you are submitting another file with internal expressions,
        // and in <response> nodes. For GET, DELETE, there is no payload, so all the URLs with potential expressions are right there in the testNode.
        Map<String,String> vars = null;
        if (parts.varsList.size()>0){
            vars = parts.varsList.get(0);
        }
        String contentType = contentTypeFromRequestPart(parts.requestPayloadFilename);
        String contentRaw = "";
        String fileName = parts.requestPayloadFilename;
        if (contentRawFromMutator == null) {
            contentRaw = getResourceManager().readResource("executeTestNode:POST/PUT:"+test.testIDLabel,
                    parts.requestPayloadFilenameRel,
                    parts.requestPayloadFilename);
        } else {
            contentRaw = contentRawFromMutator;
        }
        //TODO: confirm current behavior: why do I add vars AFTER the call?
        if (vars!=null) {
            serviceResult.addVars(vars);
        }
        serviceResult = doPOST_PUTFromXML(
                serviceResult,
                contentRaw, //partsmutator,
                fileName,
                vars,
                test.fullURL,
                method,
                contentType,
                test.evalStruct,
                test.authForTest,
                test.testIDLabel,
                test.headerMap,
                runOptions);
        test.results.add(serviceResult);
        serviceResultsMap.put(test.testID, serviceResult);
        serviceResultsMap.put("result", serviceResult);
        serviceResult.time = (System.currentTimeMillis()-test.startTime);

        if (   Tools.notBlank(mutatorType)  && (contentRawFromMutator==null)) {
            if ( ! serviceResult.gotExpectedResult()) {
                serviceResult.mutatorSkipped = true;
            } else if (runOptions.skipMutators){
                serviceResult.mutatorSkippedByOpts = true;
            } else {
                ContentMutator contentMutator = new ContentMutator(parts.requestPayloadFilenameRel, parts.requestPayloadFilename, getResourceManager());
                contentMutator.setOptions(test.testNode);
                serviceResult.mutatorType = mutatorType;
                serviceResult.mutator = contentMutator;


                ServiceResult holdThis = serviceResultsMap.get("this");
                try {
                    String content = contentMutator.mutate();
                    while (content != null) {
                        ServiceResult childResult = new ServiceResult();
                        serviceResult.addChild(childResult);
                        serviceResultsMap.put("this", childResult);

                        executeTestNode(
                                childResult,
                                content,
                                contentMutator,
                                test.testNode,//Node
                                test.testgroup,//Node
                                protoHostPort,//String
                                clonedMasterVars,//Map<String,String>
                                testElementIndex,//int
                                test.testGroupID,//String
                                test.evalStruct,//Eval
                                authsMap,//AuthsMap
                                defaultAuths,//AuthsMap
                                basedir,//String
                                report,//RestReplayReport
                                test.results);//List<ServiceResult> results)

                        content = contentMutator.mutate();
                    }
                } finally {
                    if (holdThis != null) serviceResultsMap.put("this", holdThis);
                }
            }
        }

    }

    private void writeRowToConsoleWDump(ServiceResult serviceResult, boolean hasError, String testIDLabel){
        boolean doingAuto = (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.auto);
        String serviceResultRow = serviceResult.dump(dump.dumpServiceResult, hasError);
        String leader = (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.detailed) ? "RestReplay:"+testIDLabel+": ": "";

        if (   (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.detailed)
                || (dump.dumpServiceResult == ServiceResult.DUMP_OPTIONS.full)         ){
            System.out.println("\r\n#\r\n#========= "+testIDLabel+" ============#\r\n#");
        }

        System.out.println(timeString()+" "+leader+serviceResultRow+"\r\n");

        if (dump.payloads && (doingAuto&&hasError) ) {
            //the call to serviceResultRow = serviceResult.dump(...) does something similar under non-error conditions.
            // here we are handling error conditions with "auto" and forcing out payloads.
            System.out.println(serviceResult.dumpPayloads());
        }
    }

    private void doDeleteURL(ServiceResult serviceResult, Node testNode, Eval evalStruct, String testID, String testIDLabel, Map<String,String> clonedMasterVarsWTest) {
        String deleteURL = testNode.valueOf("deleteURL");
        if (Tools.notBlank(deleteURL)) {
            EvalResult evalResult = null;
            evalResult = evalStruct.eval("deleteURL", deleteURL, clonedMasterVarsWTest);
            serviceResult.alerts.addAll(evalResult.alerts);

            if (Tools.notBlank(serviceResult.deleteURL)) {
                serviceResult.addAlert("deleteURL computed by Location (" + serviceResult.deleteURL + ")"
                                + " is being replaced by " + testID + ".deleteURL value: " + deleteURL
                                + " which evaluates to: " + evalResult.result,
                        testIDLabel,
                        LEVEL.WARN
                );
            }
            serviceResult.deleteURL = evalResult.result;
        }
    }

    private void handleExports(ServiceResult serviceResult, Node testNode, Eval evalStruct, Map<String,String> clonedMasterVarsWTest){
        Node exportsNode = testNode.selectSingleNode("exports");
        if (exportsNode!=null) {
            Map<String, String> exports = readVars(exportsNode);
            Map<String, String> exportsEvald = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : exports.entrySet()) {
                String exportID = entry.getKey();
                String expr = entry.getValue();
                boolean ebes = runOptions.errorsBecomeEmptyStrings;
                try {
                    runOptions.errorsBecomeEmptyStrings = false;
                    //System.out.println("---->eval export: "+expr);
                    EvalResult evalResult = evalStruct.eval("export vars", expr, clonedMasterVarsWTest);
                    //System.out.println("      ---->"+evalResult.result+"<--"+evalResult.alerts+serviceResult.xmlResult);
                    exportsEvald.put(exportID, evalResult.result);
                    serviceResult.alerts.addAll(evalResult.alerts);
                } finally {
                    runOptions.errorsBecomeEmptyStrings = ebes;
                }
            }
            serviceResult.addExports(exportsEvald);
        }
    }

    @SuppressWarnings({"unchecked"})
    private static final void addAlertErrorToAlerts(List<Alert> alerts, Object t){
        if (t instanceof ServiceResult.AlertError){
            alerts.addAll(((AlertError) t).allAlerts);
        }
    }

    /** Use this overload for NON-multipart messages, that is, regular POSTs. */
    public static ServiceResult doPOST_PUTFromXML(ServiceResult result,
                                                  String contentRaw,
                                                  String filename_mutationID,
                                                  Map<String,String> vars,
                                                  String fullURL,
                                                  String method,
                                                  String contentType,
                                                  Eval evalStruct,
                                                  String authForTest,
                                                  String fromTestID,
                                                  Map<String, String> headerMap,
                                                  RunOptions options)
     {
        Eval.EvalResult evalResult = evalStruct.eval("filename:"+filename_mutationID, contentRaw, vars);
        String contentSubstituted = evalResult.result;
        result.alerts.addAll(evalResult.alerts);
        return Transport.doPOST_PUT(
                result,  //brings in existing list of Alerts
                fullURL,
                contentSubstituted,
                contentRaw,
                Transport.BOUNDARY,
                method,
                contentType,
                authForTest,
                fromTestID,
                filename_mutationID,
                headerMap); //method is POST or PUT.
    }

    private static String timeString() {
        java.util.Date date= new java.util.Date();
        java.sql.Timestamp ts = new java.sql.Timestamp(date.getTime());
        return ts.toString();
    }

    private static String contentTypeFromRequestPart(String filename){
        if (filename.toUpperCase().endsWith(".JSON")){
            return "application/json";
        } else if (filename.toUpperCase().endsWith(".XML")){
            return "application/xml";
        }
        return "application/xml";
    }
		

    //======================== MAIN ===================================================================

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("basedir", true, "default/basedir");
        options.addOption("reports", true, "default/reports");
        options.addOption("testGroupID", true, "default/testGroupID");
        options.addOption("testID", true, "default/testID");
        options.addOption("envID", true, "dev");
        options.addOption("autoDeletePOSTS", true, "true");
        options.addOption("dumpResults", true, "true");
        options.addOption("control", true, "control.xml");
        options.addOption("master", true, "master.xml");

        return options;
    }

    public static String usage(){
        String result = "org.dynamide.restreplay.RestReplay {args}\r\n"
                        +" args: \r\n"
                        +"  -basedir <dir> \r\n"
                        +"  -reports <dir> \r\n"
                        +"  -restReplayMaster <filename> \r\n"
                        +"  -control true|false \r\n"
                        +"  -testGroupID <ID> \r\n"
                        +"  -testID <ID> \r\n"
                        +"  -dumpResults true|false \r\n"
                        +"  -envID <ID> \r\n"
                        +"  -autoDeletePOSTS true|false \r\n"
                        +"   \r\n"
                        +" Note: -DautoDeletePOSTS won't force deletion if set to false in control file."
                        +"   \r\n"
                        +" You may also override these with system args, e.g.: \r\n"
                        +"   -Dbasedir=/path/to/dir \r\n"
                        +"   \r\n"
                        +" These may also be passed in via the POM.\r\n"
                        +" You can also set these system args, e.g.: \r\n"
                        +"  -DtestGroupID=<oneID> \r\n";

        return result;
    }

    private static String opt(CommandLine line, String option){
        String result;
        String fromProps = System.getProperty(option);
        if (Tools.notEmpty(fromProps)){
            return fromProps;
        }
        if (line==null){
            return "";
        }
        result = line.getOptionValue(option);
        if (result == null){
            result = "";
        }
        return result;
    }

    public static void main(String[]args) throws Exception {
        Options options = createOptions();
        //System.out.println("System CLASSPATH: "+prop.getProperty("java.class.path", null));
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            String basedir = opt(line, "basedir");
            String reportsDir = opt(line, "reports");
            String testGroupID      = opt(line, "testGroupID");
            String testID           = opt(line, "testID");
            String envID            = opt(line, "envID");
            String autoDeletePOSTS  = opt(line, "autoDeletePOSTS");
            String dumpResultsFromCmdLine      = opt(line, "dumpResults");
            String controlFilename   = opt(line, "control");
            String restReplayMaster  = opt(line, "master");

            if (Tools.isBlank(reportsDir)){
                reportsDir = basedir + '/'+ RestReplayTest.REPORTS_DIRNAME;
            }
            reportsDir = Tools.fixFilename(reportsDir);
            basedir = Tools.fixFilename(basedir);
            controlFilename = Tools.fixFilename(controlFilename);

            boolean bAutoDeletePOSTS = true;
            if (Tools.notEmpty(autoDeletePOSTS)) {
                bAutoDeletePOSTS = Tools.isTrue(autoDeletePOSTS);
            }
            boolean bDumpResults = false;
            if (Tools.notEmpty(dumpResultsFromCmdLine)) {
                bDumpResults = Tools.isTrue(dumpResultsFromCmdLine);
            }
            if (Tools.isEmpty(basedir)){
                System.err.println("ERROR: basedir was not specified.");
                return;
            }
            File f = null, fMaster = null;
            if (Tools.isEmpty(restReplayMaster)) {
                if (Tools.isEmpty(controlFilename)){
                    System.err.println("Exiting.  No Master file (empty) and Control file not found (empty)");
                    return;
                }
                f = new File(Tools.glue(basedir, "/", controlFilename));
                if ( !f.exists()) {
                    System.err.println("Control file not found: " + f.getCanonicalPath());
                    return;
                }
            } else {
                fMaster = new File(Tools.glue(basedir, "/", restReplayMaster));
                if (Tools.notEmpty(restReplayMaster) && !fMaster.exists()) {
                    System.err.println("Master file not found: " + fMaster.getCanonicalPath());
                    return;
                }
            }

            String basedirResolved = (new File(basedir)).getCanonicalPath();
            System.out.println("RestReplay ::"
                            + "\r\n    basedir: "+basedir
                            + "\r\n    basedir(resolved): "+basedirResolved
                            + "\r\n    control: "+controlFilename
                            + "\r\n    master: "+restReplayMaster
                            + "\r\n    testGroupID: "+testGroupID
                            + "\r\n    testID: "+testID
                            + "\r\n    envID: "+envID
                            + "\r\n    autoDeletePOSTS: "+bAutoDeletePOSTS
                            + (Tools.notEmpty(restReplayMaster)
                                       ? ("\r\n    will use master file: "+fMaster.getCanonicalPath())
                                       : ("\r\n    will use control file: "+f.getCanonicalPath()) )
                             );

            ResourceManager rootResourceManager = ResourceManager.createRootResourceManager();

            if (Tools.notEmpty(restReplayMaster)){
                if (Tools.notEmpty(controlFilename)){
                    //TODO: DOCO: I think this means you can run a control file directly from the command line (rather than a master).  This may be historical?  where do global options come from?
                    System.out.println("WARN: control: "+controlFilename+" will not be used because master was specified.  Running master: "+restReplayMaster);
                }
                RestReplay replay = new RestReplay(basedirResolved, reportsDir, rootResourceManager, null);
                replay.setEnvID(envID);
                replay.readOptionsFromMasterConfigFile(restReplayMaster);
                replay.setAutoDeletePOSTS(bAutoDeletePOSTS);
                Dump dumpFromMaster = replay.getDump();
                if (Tools.notEmpty(dumpResultsFromCmdLine)){
                    dumpFromMaster.payloads = Tools.isTrue(dumpResultsFromCmdLine);
                }
                replay.setDump(dumpFromMaster);
                replay.runMaster(restReplayMaster, false); //false, because we already just read the options, and override a few.
            } else {
                Dump dump = getDumpConfig();
                if (Tools.notEmpty(dumpResultsFromCmdLine)){
                    dump.payloads = Tools.isTrue(dumpResultsFromCmdLine);
                }
                List<String> reportsList = new ArrayList<String>();

                RestReplay restReplay = new RestReplay(basedirResolved, reportsDir, rootResourceManager, null);
                restReplay.setDump(dump);
                restReplay.runRestReplayFile(basedirResolved,
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
            // oops, something went wrong
            System.err.println("Cmd-line parsing failed.  Reason: " + exp.getMessage());
            System.err.println(usage());
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            e.printStackTrace();
        }
    }

}
