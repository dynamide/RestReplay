package org.dynamide.restreplay;

import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dynamide.util.Tools;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Master extends ConfigFile {

    public Master(String testdir, String reportsDir, ResourceManager manager){
        setTestDir(testdir);
        setReportsList(new ArrayList<String>());
        this.reportsDir = reportsDir;
        setResourceManager(manager);
        readDefaultRunOptions();//prerequisites: ResourceManager has been set.
    }
    public static final String DEFAULT_MASTER_CONTROL = "master.xml";

    public Map<String,String> vars = new HashMap<String,String>();
    public Map<String, String> getVars() {
        return vars;
    }
    public void setVars(Map<String, String> masterVars) {
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
        EnvResult res = selectEnv(masterNode, desiredEnv);
        Node nodeWVars = res.nodeWEnvs;
        if (null!=nodeWVars){
            this.setEnvID(res.envID);
        } else {
            nodeWVars = masterNode;
        }
        setVars(readVars(nodeWVars));
        this.getRunOptions().addRunOptions(document.selectSingleNode("/restReplayMaster/runOptions"), "master");
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

    /** Creates new instances of RestReplay, one for each controlFile specified in the master,
     *  and setting defaults from this instance, but not sharing ServiceResult objects or maps. */
    public List<List<ServiceResult>> runMaster(String masterFilename, boolean readOptionsFromMaster) throws Exception {
        List<List<ServiceResult>> list = new ArrayList<List<ServiceResult>>();
        org.dom4j.Document document = loadDocument(masterFilename, readOptionsFromMaster);
        String controlFile, testGroup, test;
        List<Node> runNodes = document.selectNodes("/restReplayMaster/run");
        for (Node runNode : runNodes) {
            controlFile = runNode.valueOf("@controlFile");
            testGroup = runNode.valueOf("@testGroup");
            test = runNode.valueOf("@test"); //may be empty
            Map<String, String> runVars = readVars(runNode);
            list.add(runTest(masterFilename, controlFile, testGroup, test, runVars));
        }
        RestReplayReport.saveIndexForMaster(getTestDir(), reportsDir, masterFilename, this.getReportsList(), this.getEnvID(), vars, this);
        return list;
    }

    public List<List<ServiceResult>> runMaster(String masterFilename,
                                               boolean readOptionsFromMaster,
                                               String controlFile,
                                               String testGroup,
                                               String test) throws Exception {
        List<List<ServiceResult>> list = new ArrayList<List<ServiceResult>>();
        //org.dom4j.Document document = loadDocument(masterFilename, readOptionsFromMaster);
        runTest(masterFilename, controlFile, testGroup, test, null);
        RestReplayReport.saveIndexForMaster(getTestDir(), reportsDir, masterFilename, this.getReportsList(), this.getEnvID(), vars, this);
        return list;
    }

    private List<ServiceResult> runTest(String masterFilename,
                                        String controlFile,
                                        String testGroup,
                                        String test,
                                        Map<String, String> runVars)
    throws Exception {
        String envReportsDir = this.reportsDir;
        //Create a new instance and clone only config values, not any results maps.
        if (Tools.notBlank(this.getEnvID())) {
            envReportsDir = Tools.glue(this.reportsDir, "/", this.getRelativePathFromReportsDir());
        }
        RestReplay replay = new RestReplay(getTestDir(), envReportsDir, this.getResourceManager(), this.getRunOptions());//this.reportsDir);
        replay.setEnvID(this.getEnvID());  //internally sets replay.relativePathFromReportsDir
        replay.setControlFileName(controlFile);
        replay.setProtoHostPort(getProtoHostPort());
        replay.setAutoDeletePOSTS(isAutoDeletePOSTS());
        replay.setDump(getDump());
        replay.setDefaultAuthsMap(getDefaultAuthsMap());
        replay.setRunOptions(this.getRunOptions());
        replay.setMasterFilename(masterFilename);

        //Map<String, String> runVars = readVars(runNode);
        Map<String, String> masterVarsDup = new HashMap<String, String>();
        masterVarsDup.putAll(getVars());
        if (runVars!=null){
            masterVarsDup.putAll(runVars);
        }
        replay.setMasterVars(masterVarsDup);
        replay.setReportsList(getReportsList());  //they go directly in.  In future, if you want to aggregate by control file, fix it here.
        // Add all the reports from the inner replay, to our master replay's reportsList, to generate the index.html file.


        //======================== Now run *that* instance. ======================
        return replay.runTests(testGroup, test);
        //========================================================================

    }
}
