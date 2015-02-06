package org.dynamide.restreplay;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dynamide.interpreters.Eval;
import org.dynamide.util.Tools;
import org.dynamide.interpreters.EvalResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** This is the superclass of Master and RestReplay.
 *      RestReplay reads one control file at a time.
 *      Master reads a master file, which spawns RestReplays for every control file it runs.
 */
public class ConfigFile {
    private ResourceManager resourceManager;
    public ResourceManager getResourceManager(){
        return resourceManager;
    }
    protected void setResourceManager(ResourceManager manager){
        resourceManager = manager;
    }

    protected String reportsDir = "";
    public String getReportsDir(){
        return reportsDir;
    }

    private String testdir = ".";  //set from constructor.
    public String getTestDir(){
        return testdir;
    }
    protected void setTestDir(String value){
       testdir = value;
    }

    private RunOptions runOptions;
    public RunOptions getRunOptions(){
        return runOptions;
    }
    protected void setRunOptions(RunOptions options){
        this.runOptions = options;
    }

    private boolean autoDeletePOSTS = true;
    public boolean isAutoDeletePOSTS() {
        return autoDeletePOSTS;
    }
    public void setAutoDeletePOSTS(boolean autoDeletePOSTS) {
        this.autoDeletePOSTS = autoDeletePOSTS;
    }

    private String protoHostPort = "";
    public String getProtoHostPort() {
        return protoHostPort;
    }
    public void setProtoHostPort(String protoHostPort) {
        this.protoHostPort = protoHostPort;
    }

    AuthsMap defaultAuthsMap;
    public AuthsMap getDefaultAuthsMap(){
        return defaultAuthsMap;
    }
    public void setDefaultAuthsMap(AuthsMap authsMap){
        defaultAuthsMap = authsMap;
    }

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

    private List<String> reportsList;
    public  List<String> getReportsList(){
        return reportsList;
    }
    public void setReportsList(List<String> list){
        this.reportsList = list;
    }

    private String relativePathFromReportsDir = "";
    /** There is no setter.  It is set when you call setEnvID() */
    public String getRelativePathFromReportsDir(){
        return relativePathFromReportsDir;
    }

    private Dump dump;
    public Dump getDump() {
        return dump;
    }
    public void setDump(Dump dump) {
        this.dump = dump;
    }

    public static Dump readDumpOptions(org.dom4j.Document document){
        Dump dump = Dump.getDumpConfig();
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

    public static AuthsMap readAuths(org.dom4j.Document document){
        Map<String, String> map = new LinkedHashMap<String, String>();
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

    public static Map<String,Object> readVars(Node nodeWVars){
        Map<String,Object> vars = new LinkedHashMap<String,Object>();
        List<Node> varNodes = nodeWVars.selectNodes("vars/var");
        return readVars(varNodes, vars);
    }

    private static Map<String,Object> readVars(List<Node> varNodes, Map<String,Object> vars){
        for (Node var: varNodes){
            String ID = var.valueOf("@ID");
            String value = var.getText();
            vars.put(ID, value);
        }
        return vars;
    }

    public static Map<String,String> readHeaders(Node testNode,
                                                  Eval evalStruct,
                                                  ServiceResult serviceResult){
        Map<String,String> headerMap = new LinkedHashMap<String,String>();
        List<Node> headers = testNode.selectNodes("headers/header");
        for (Node header: headers){
            String headerValue = header.getStringValue();
            String headerName = header.valueOf("@name");
            //System.out.println("header from control file: "+headerName +": "+ headerValue);
            if (headerValue.indexOf("$")>-1){
                EvalResult evalResult = evalStruct.eval(headerName, headerValue, null);
                headerValue = evalResult.getResultString();
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            //System.out.println("eval'd header from control file: "+headerName +": "+ headerValue);
            headerMap.put(headerName, headerValue);
        }
        return headerMap;
    }

    /** Prerequisites: ResourceManager has been set, testdir has been set.
     */
    public void readDefaultRunOptions() {
        setRunOptions(new RunOptions());
        try {
            Document doc = getResourceManager().getDocument("RestReplay:constructor:runOptions",
                                                            testdir,
                                                            RunOptions.RUN_OPTIONS_FILENAME);
            if (doc != null) {
                getRunOptions().addRunOptions(doc.getRootElement(), "default");
            }
        } catch (DocumentException de) {
            System.err.println("ERROR: could not read default runOptions.xml");
        }
    }
}
