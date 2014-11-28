package org.dynamide.restreplay;

import org.dom4j.Node;
import org.dynamide.util.Tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected String basedir = ".";  //set from constructor.
    public String getBaseDir(){
        return basedir;
    }

    //TODO: make sure that the report gets all the alerts
    //TODO: check breaking scenarios and RunOptions.
    //TODO: config from master control file.
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

    public static Map<String,String> readVars(Node nodeWVars){
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

    public static Map<String,String> readHeaders(Node testNode,
                                                  Eval evalStruct,
                                                  ServiceResult serviceResult){
        Map<String,String> headerMap = new HashMap<String,String>();
        List<Node> headers = testNode.selectNodes("headers/header");
        for (Node header: headers){
            String headerValue = header.getStringValue();
            String headerName = header.valueOf("@name");
            //System.out.println("header from control file: "+headerName +": "+ headerValue);
            if (headerValue.indexOf("$")>-1){
                Eval.EvalResult evalResult = evalStruct.eval(headerName, headerValue, null);
                headerValue = evalResult.result;
                serviceResult.alerts.addAll(evalResult.alerts);
            }
            //System.out.println("eval'd header from control file: "+headerName +": "+ headerValue);
            headerMap.put(headerName, headerValue);
        }
        return headerMap;
    }
}
