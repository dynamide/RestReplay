package org.dynamide.restreplay;

import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dynamide.interpreters.Alert;
import org.dynamide.interpreters.Eval;
import org.dynamide.interpreters.EvalResult;
import org.dynamide.interpreters.RhinoInterpreter;
import org.dynamide.util.Tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * This class represents the master-control file, which simply sets up an environment, and calls multiple control files.
 * @author Laramie Crocker
 */
public class Master extends ConfigFile {

    public Master(String testdir, String reportsDir, ResourceManager manager){
        setTestDir(testdir);
        setReportsList(new ArrayList<String>());
        this.reportsDir = reportsDir;
        this.startTime = Tools.now();
        this.endTime = Tools.now();
        setResourceManager(manager);
        readDefaultRunOptions();//prerequisites: ResourceManager has been set.
    }
    public static final String DEFAULT_MASTER_CONTROL = "master.xml";

    public long startTime;
    public long endTime;

    public String masterEnvsFileLocation = "";
    public String masterVarsFileLocation = "";

    private Map<String, ServiceResult> masterNamespace = new LinkedHashMap<String, ServiceResult>();
    public Map<String, ServiceResult> getMasterNamespace(){
        return masterNamespace;
    }

    List<List<ServiceResult>> serviceResultsListList = new ArrayList<List<ServiceResult>>();

    public Map<String,Object> vars = new LinkedHashMap<String,Object>();
    public Map<String, Object> getVars() {
        return vars;
    }
    public void setVars(Map<String, Object> masterVars) {
        this.vars = masterVars;
    }

    /** Optional information method: call this method after instantiating this class using the constructor RestReplay(String), which sets the testdir.  Then you
     *   pass in your relative masterFilename to that testdir to this method, which will return true if the file is readable, valid xml, etc.
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
            return getResourceManager().getDocument("openMasterConfigFile:" + reason, getTestDir(), masterFilename);
        } catch (DocumentException de) {
            System.out.println("$$$$$$ ERROR: " + de);
            throw new FileNotFoundException("RestReplay master control file (" + masterFilename + ") contains error or not found in testdir: " + getTestDir() + ". Exiting test. " + de);
        }
    }

    /** specify the master config file, relative to getTestDir(), but ignore any tests or testGroups in the master.
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
            setProtoHostPort(node.getText().trim());
        }
        AuthsMap authsMap = readAuths(document);
        setDefaultAuthsMap(authsMap);
        Dump dump = RestReplay.readDumpOptions(document);
        setDump(dump);

        String desiredEnv = this.getEnvID();
        Node masterNode = document.selectSingleNode("/restReplayMaster");

        Node masterProxyNode = masterNode;

        Node envsNode = masterNode.selectSingleNode("envs");
        String envsFilename = envsNode.valueOf("@filename");
        if (Tools.notBlank(envsFilename)){
            try {
                org.dom4j.Document envsDoc = getResourceManager().getDocument("open envsDoc:" + envsFilename, getTestDir(), envsFilename);
                if (envsDoc==null){
                    throw new FileNotFoundException("envs filename was not found: "+envsFilename+" as specified in master: "+masterFilename);
                }
                masterProxyNode = envsDoc.getRootElement();
                this.masterEnvsFileLocation = envsFilename;
            } catch (DocumentException de) {
                System.out.println("$$$$$$ ERROR reading env vars from single file: " + de);
                masterProxyNode = masterNode;
            }
        }

        //get restReplayMaster::vars
        Node theVarsMasterNode;
        if (masterProxyNode.selectSingleNode("vars") == null) {
            theVarsMasterNode = masterNode;
        } else {
            theVarsMasterNode = masterProxyNode;
            this.masterVarsFileLocation = envsFilename;
        }
        getVars().putAll(readVars(theVarsMasterNode));//todo%% may have to do null-check


        //get restReplayMaster::envs/env[desiredEnv]::vars
        EnvResult res = selectEnv(masterProxyNode, desiredEnv);
        Node nodeWVars = res.nodeWEnvs;
        if (null != nodeWVars) {
            this.setEnvID(res.envID);
        } else {
            nodeWVars = masterProxyNode;
        }

        getVars().putAll(readVars(nodeWVars));
        //todo: get run/vars

        this.getRunOptions().addRunOptions(document.selectSingleNode("/restReplayMaster/runOptions"), "master");

        readEvent(masterNode, "onSummary");
        readEvent(masterNode, "onBeginMaster");
        readEvent(masterNode, "onEndMaster");
        readEvent(masterNode, "onMasterSummaryTable");
        readEvent(masterNode, "onFailureSummary");
        readEvent(masterNode, "onAnalysis");

        return document;
    }




    ScriptEventResult fireOnSummary(){
        return fireEvent("onSummary");
    }

    ScriptEventResult fireOnFailureSummary(){
        return fireEvent("onFailureSummary");
    }

    ScriptEventResult fireOnMasterSummaryTable(){
        return fireEvent("onMasterSummaryTable");
    }

    ScriptEventResult fireOnAnalysis(){
        return fireEvent("onAnalysis");
    }


    private void fireOnBeginMaster(){
        fireEvent("onBeginMaster");
    }

    private void fireOnEndMaster(){
        endTime = Tools.now();
        fireEvent("onEndMaster");
    }



    /* supports javascript only, not jexl.*/
    private void readEvent(Node masterNode, String eventID)
    throws  FileNotFoundException {
        String onSummaryScript = "";
        String onSummaryScriptName = "";
        Event event = new Event();
        Node eventNode = masterNode.selectSingleNode("event[@ID='"+eventID+"']");
        if (eventNode==null){
            return;
        }
        String language = eventNode.valueOf("@language");
        if (Tools.notBlank(language)){
            if ( ! language.equalsIgnoreCase("javascript")){
                throw new FileNotFoundException("language not supported for event ("+eventID+") : "+language+". Use language='javascript'");
            }
        }
        event.language = "javascript";
        String filenameRel = eventNode.valueOf("@filename");
        if (Tools.isBlank(filenameRel) && Tools.isBlank(eventNode.valueOf("."))){
            filenameRel = eventID + ".js";
        }
        if (Tools.notBlank(filenameRel)){
            String filenameFull = getTestDir() + '/' + filenameRel;
            try {
                ResourceManager.Resource scriptResource = getResourceManager().readResource("event "+eventID,
                        filenameRel,
                        filenameFull);
                if (scriptResource.provider == ResourceManager.Resource.SOURCE.NOTFOUND) {
                    System.out.println("Master loading "+eventID+" event, but Resource not found: " + scriptResource.toString());
                    onSummaryScript = "";
                } else {
                    onSummaryScript = scriptResource.contents;
                    onSummaryScriptName = filenameRel;
                }
            } catch (IOException e){
                throw new FileNotFoundException("could not load event script: "+filenameRel);
            }
        } else {
            onSummaryScript = eventNode.valueOf(".");
            onSummaryScriptName = "[inline]";
        }
        event.eventID = eventID;
        event.name = onSummaryScriptName;
        event.script = onSummaryScript;
        events.put(eventID, event);
    }

    public static class Event {
        public String eventID = "";
        public String name = "";
        public String script = "";
        public String language = "";  //search the code in this unit if you want languages other than javascript.  For now it must be javascript.
    }

    private Map<String,Event> events = new HashMap<String,Event>();

    public class ScriptEventResult{
        public String result="";
        public Event event=new Event();
    }

    public ScriptEventResult fireEvent(String eventID){
        Event event = events.get(eventID);
        if (event == null || Tools.isBlank(event.script)) {
            return new ScriptEventResult();
        }
        //System.out.println("Firing "+eventID+" event "+event.name);
        Map<String,Object> varsMap = new HashMap<String, Object>();
        varsMap.put("master", this);
        varsMap.put("serviceResultsListList", serviceResultsListList);

        Eval evalStruct = new Eval();
        evalStruct.runOptions = this.getRunOptions();

        RhinoInterpreter interpreter = new RhinoInterpreter(getRunOptions().dumpJavascriptEvals);
        interpreter.setVariable("master", this);
        interpreter.setVariable("serviceResultsListList", serviceResultsListList);
        interpreter.setVariable("kit", RestReplay.KIT);  //with kit they can spit to stdout if they want.
        interpreter.setVariable("tools", RestReplay.TOOLS);
        EvalResult evalResult = interpreter.eval(event.name, event.script);
        evalStruct.addToEvalReport(evalResult);
        ScriptEventResult eventResult = new ScriptEventResult();
        eventResult.event = event;
        eventResult.result = evalResult.getResultString();
        return eventResult;
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
            if (Tools.isTrue(isDefault)){
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

    private org.dom4j.Document loadDocument(String masterFilename, boolean readOptionsFromMaster)
            throws FileNotFoundException {
        org.dom4j.Document document;
        if (readOptionsFromMaster){
            document = readOptionsFromMasterConfigFile(masterFilename);  //side-effects: sets fields of "this".
        } else {
            document = openMasterConfigFile("runMaster", masterFilename);
        }
        if (document==null){
            throw new FileNotFoundException(masterFilename);
        }
        return document;
    }

    public List<List<ServiceResult>> runMaster(String masterFilename) throws Exception {
        return runMaster(masterFilename, true);
    }
    private List<ServiceResult> runTest(String masterFilename,
                                        String controlFile,
                                        String testGroup,
                                        String test,
                                        Map<String, Object> runVars,
                                        String relToMaster,
                                        String runID,
                                        Integer runHashCount,
                                        List<RestReplayReport.Header>testGroups)
    throws Exception {
        String envReportsDir = this.reportsDir;
        if (Tools.notBlank(this.getEnvID())) {
            envReportsDir = Tools.glue(this.reportsDir, "/", this.getRelativePathFromReportsDir());
        }
        //Create a new instance and clone only config values, not any results maps.
        RestReplay replay = new RestReplay(getTestDir(), envReportsDir, this.getResourceManager(), this.getRunOptions());
        replay.setEnvID(this.getEnvID());  //internally sets replay.relativePathFromReportsDir
        replay.setControlFileName(controlFile);
        replay.setProtoHostPort(getProtoHostPort());
        replay.setAutoDeletePOSTS(isAutoDeletePOSTS());
        replay.setDump(getDump());
        replay.setDefaultAuthsMap(getDefaultAuthsMap());
        replay.setRunOptions(this.getRunOptions());
        replay.setMasterFilename(masterFilename);
        replay.setRelToMaster(relToMaster);
        replay.setMasterEnvsFileLocation(masterEnvsFileLocation);
        replay.setMasterVarsFileLocation(masterVarsFileLocation);

        Map<String, Object> masterVarsDup = new LinkedHashMap<String, Object>();
        masterVarsDup.putAll(getVars());
        if (runVars!=null){
            masterVarsDup.putAll(runVars);
        }
        replay.setMasterVars(masterVarsDup);
        replay.setMasterNamespace(this.masterNamespace);
        replay.setReportsList(getReportsList());  //they go directly in.  In future, if you want to aggregate by control file, fix it here.
        // Add all the reports from the inner replay, to our master replay's reportsList, to generate the index.html file.


        //======================== Now run *that* instance. ======================
        return replay.runTests(testGroup, test, runID, runHashCount, testGroups);
        //========================================================================

    }


    /** Creates new instances of RestReplay, one for each controlFile specified in the master,
     *  and setting defaults from this instance, but not sharing ServiceResult objects or maps. */
    public List<List<ServiceResult>> runMaster(String masterFilename, boolean readOptionsFromMaster) throws Exception {
        List<RestReplayReport.Header> testGroups = new ArrayList<RestReplayReport.Header>();
        org.dom4j.Document document = loadDocument(masterFilename, readOptionsFromMaster);

        Map<String, Integer> runSequenceMap = new HashMap<String, Integer>();

        serviceResultsListList.clear();
        fireOnBeginMaster();
        String controlFile, testGroup, test;
        String runID;
        RestReplayReport.ReportMasterLocations tupple = RestReplayReport.calculateMasterReportRelname(reportsDir, masterFilename, this.getEnvID());
        Map<String, Map> allRunIDs = new HashMap<String,Map>();
        List<String> alertStrings = new ArrayList<String>();

        List<Node> runNodes = document.selectNodes("/restReplayMaster/run");
        for (Node runNode : runNodes) {
            controlFile = runNode.valueOf("@controlFile");
            testGroup = runNode.valueOf("@testGroup");
            test = runNode.valueOf("@test"); //may be empty
            runID = runNode.valueOf("@ID"); //may be empty

            String runHash = controlFile+':'+testGroup;
            Integer runHashCount = runSequenceMap.get(runHash);
            if (runHashCount==null){
                runHashCount = new Integer(0);
            } else {
                runHashCount = new Integer(runHashCount + 1);
            }
            runSequenceMap.put(runHash, runHashCount);

            //testGroups.add(testGroup);

            Map<String, Object> runVars = readVars(runNode);
            runVars = expandRunVars(runVars, allRunIDs, masterFilename, testGroup, alertStrings);

            List<ServiceResult> listOTests
               = runTest(masterFilename, controlFile, testGroup, test, runVars, tupple.relname, runID, runHashCount, testGroups);

            if (Tools.notBlank(runID)){
                Map<String, ? extends Object> serviceResultsByName = createMapByTestIDs(listOTests);
                allRunIDs.put(runID, serviceResultsByName);
            }

            serviceResultsListList.add(listOTests);//TODO: remove dups.
        }
        RestReplayReport.saveIndexForMaster(getTestDir(), reportsDir, masterFilename, this.getReportsList(), this.getEnvID(), vars, testGroups, this, serviceResultsListList, alertStrings);
        fireOnEndMaster();
        return serviceResultsListList;
    }

    private Map<String, ServiceResult> createMapByTestIDs(List<ServiceResult> listOTests) {

        Map<String, ServiceResult> result = new HashMap<String, ServiceResult>();
        for (ServiceResult sr: listOTests){
            result.put(sr.testID, sr); //could use testIDLabel, but I think testGroup is required in <run.../>
        }
        return result;
    }

    private Map<String, Object> expandRunVars(Map<String, Object> runVars,
                                              Map<String, Map> srsByName,
                                              String masterFilename,
                                              String currentTestGroup,
                                              List<String> alertStrings){
        Eval evalStruct = new Eval();
        for (Map.Entry<String,Map> entry: srsByName.entrySet()) {
            evalStruct.jc.set(entry.getKey(), entry.getValue());
        }
        //evalStruct.setCurrentTestIDLabel(serviceResult.testIDLabel);
        evalStruct.runOptions = this.getRunOptions();
        Map<String, Object> result = new HashMap<String, Object>();
        EvalResult evalResult;
        for (Map.Entry<String,Object> entry: runVars.entrySet()){
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val!=null) {
                evalResult = evalStruct.eval(currentTestGroup, val.toString(), getVars());
                result.put(key, evalResult.getResultString());
                if (evalResult.alerts.size()>0){
                    for (Alert alert: evalResult.alerts){
                        String oneAlert = "/run/vars/var under ("+currentTestGroup+") : "+alert.toString();
                        System.out.println("ERROR expanding "+masterFilename+"  "+oneAlert+"\r\n\r\n");
                        alertStrings.add(oneAlert);
                    }
                }
                //todo: add alerts to master report somehow.  This is how it is done in RestReplay: serviceResult.alerts.addAll(evalResult.alerts);
            }
        }
        return result;
    }

    public List<List<ServiceResult>> runMaster(String masterFilename,
                                               boolean readOptionsFromMaster,
                                               String controlFile,
                                               String testGroup,
                                               String test) throws Exception {
        serviceResultsListList.clear();
        fireOnBeginMaster();
        //org.dom4j.Document document = loadDocument(masterFilename, readOptionsFromMaster);
        RestReplayReport.ReportMasterLocations tupple = RestReplayReport.calculateMasterReportRelname(reportsDir, masterFilename, this.getEnvID());
        List<RestReplayReport.Header> testGroups = new ArrayList<RestReplayReport.Header>();
        String runID = "";
        Integer runHashCount = 0;
        //runVars is null, which is because we are running the test by control file name, NOT the <run> node in the master.
        serviceResultsListList.add(runTest(masterFilename, controlFile, testGroup, test, null, tupple.relname, runID, runHashCount, testGroups));//TODO: remove dups.
        RestReplayReport.saveIndexForMaster(getTestDir(), reportsDir, masterFilename, this.getReportsList(), this.getEnvID(), vars, testGroups, this, serviceResultsListList, null);
        fireOnEndMaster();
        return serviceResultsListList;
    }

}
