package org.dynamide.restreplay;

import org.dom4j.Node;
import org.dynamide.interpreters.Alert;
import org.dynamide.util.Tools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * All global configurable run-time options are here,
 * and are set by either the runOptions.xml file in the RestReplay directory,
 * or the &lt;runOptions> elements in the master or control file.
 * @author Laramie Crocker
 */
public class RunOptions {
    public RunOptions(){
        setCondensedHeadersString(DEFAULT_CONDENSE_HEADERS);

    }
    public static enum EVAL_REPORT_LEVEL {NONE, SHORT, ALL};
    public final static int MAX_CHARS_FOR_REPORT_LEVEL_SHORT = 300;
    public final static int MAX_CHARS_FOR_COMMENT_SHORT = 120;
    public final static String DEFAULT_CONDENSE_HEADERS = "ACCEPT,COOKIE";

    public EVAL_REPORT_LEVEL evalReportLevel = EVAL_REPORT_LEVEL.SHORT;
    public static final String RUN_OPTIONS_FILENAME = "runOptions.xml";

    public int connectionTimeout = 30000;   //millis until gives up on establishing a connection.
    public int socketTimeout = 30000;  //millis until gives up on data bytes transmitted, apache docs say "timeout for waiting for data".
    public boolean errorsBecomeEmptyStrings = true;
    public Alert.LEVEL acceptAlertLevel = Alert.LEVEL.OK;  //OK means breaks on WARN and ERROR.
    public boolean skipMutators = false;
    public boolean skipMutatorsOnFailure = true;
    public boolean dumpMasterSummary = false;
    public boolean dumpRunOptions = false;
    public boolean dumpResourceManagerSummary = true;
    public boolean dumpJavascriptEvals = true;
    public boolean reportResourceManagerSummary = true;
    public boolean reportResponseRaw = false;
    public boolean reportPayloadsAsXML = false;
    public boolean failTestOnErrors = true;   //for one test, do we report SUCCESS or FAILURE.
    public boolean failTestOnWarnings = true; //for one test, do we report SUCCESS or FAILURE.
    public boolean outputServiceResultDB = false;

    public Map<String,String> condensedHeaders= Tools.createSortedCaseInsensitiveMap();
    public boolean emitRestReplayHeaders = true;

    public boolean breakNow(Alert alert) {
        return (alert.level.compareTo(this.acceptAlertLevel) > 0);
    }
    public boolean breakNow(List<Alert> alerts) {
        for (Alert alert : alerts) {
            if (this.breakNow(alert)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String CR0 = "\r\n   ";
        String CR = ",\r\n   ";
        return "{" + CR0 +
                "    connectionTimeout=" + connectionTimeout + CR +
                "    socketTimeout=" + socketTimeout + CR+
                "    errorsBecomeEmptyStrings=" + errorsBecomeEmptyStrings + CR+
                "    acceptAlertLevel=" + acceptAlertLevel + CR+
                "    failTestOnWarnings=" + failTestOnWarnings + CR+
                "    failTestOnErrors=" + failTestOnErrors + CR+
                "    evalReportLevel=" + evalReportLevel +CR+
                "    dumpMasterSummary=" + dumpMasterSummary + CR+
                "    dumpRunOptions=" + dumpRunOptions + CR+
                "    dumpResourceManagerSummary=" + dumpResourceManagerSummary + CR+
                "    dumpJavascriptEvals=" + dumpJavascriptEvals + CR+
                "    reportResourceManagerSummary=" + reportResourceManagerSummary +CR+
                "    reportResponseRaw=" + reportResponseRaw +CR+
                "    reportPayloadsAsXML=" + reportPayloadsAsXML +CR+
                "    skipMutatorsOnFailure=" + skipMutatorsOnFailure + CR+
                "    skipMutators=" + skipMutators + CR+
                "    condensedHeaders=" + condensedHeaders.keySet().toString() +CR+
                "    emitRestReplayHeaders=" + emitRestReplayHeaders +CR+
                "    outputServiceResultDB=" + outputServiceResultDB + "\r\n   }";
    }

    public String toHTML() {
        String BR = ",<br />\r\n";
        String BE = "<br />\r\n";
        return "<div class='RunOptions'>{" +BE+
                "connectionTimeout=" + connectionTimeout +BR+
                "socketTimeout=" + socketTimeout +BR+
                "errorsBecomeEmptyStrings=" + errorsBecomeEmptyStrings +BR+
                "acceptAlertLevel=" + acceptAlertLevel +BR+
                "failTestOnWarnings=" + failTestOnWarnings +BR+
                "failTestOnErrors=" + failTestOnErrors +BR+
                "evalReportLevel=" + evalReportLevel +BR+
                "dumpMasterSummary=" + dumpMasterSummary +BR+
                "dumpRunOptions=" + dumpRunOptions +BR+
                "dumpResourceManagerSummary=" + dumpResourceManagerSummary +BR+
                "dumpJavascriptEvals=" + dumpJavascriptEvals +BR+
                "reportResourceManagerSummary=" + reportResourceManagerSummary +BR+
                "reportResponseRaw=" + reportResponseRaw +BR+
                "reportPayloadsAsXML=" + reportPayloadsAsXML +BR+
                "skipMutatorsOnFailure=" + skipMutatorsOnFailure +BR+
                "skipMutators=" + skipMutators +BR+
                "condensedHeaders=" + condensedHeaders.keySet().toString() +BR+
                "emitRestReplayHeaders=" + emitRestReplayHeaders +BR+
                "outputServiceResultDB=" + outputServiceResultDB +BE+
                "}</div>";
    }

    protected void setCondensedHeadersString(String sCondensedHeaders){
        if (Tools.notBlank(sCondensedHeaders)) {
            List<String> items = Arrays.asList(sCondensedHeaders.split("\\s*,\\s*"));
            for (String s : items) {
                if (Tools.notBlank(s) && s.equalsIgnoreCase("NONE")) {
                    condensedHeaders.clear();
                } else {
                    condensedHeaders.put(s, s);
                }
            }
        }
    }


    // from xml file as xpath: "/restReplayMaster/runOptions"
    public void addRunOptions(Node runOptionsNode, String context){
        if (runOptionsNode==null){
            return;
        }
        String connectionTimeout = runOptionsNode.valueOf("connectionTimeout");
        String socketTimeout = runOptionsNode.valueOf("socketTimeout");
        String errorsBecomeEmptyStrings = runOptionsNode.valueOf("errorsBecomeEmptyStrings");
        String acceptAlertLevel = runOptionsNode.valueOf("acceptAlertLevel");
        String failTestOnWarnings = runOptionsNode.valueOf("failTestOnWarnings");
        String failTestOnErrors = runOptionsNode.valueOf("failTestOnErrors");
        String dumpMasterSummary = runOptionsNode.valueOf("dumpMasterSummary");
        String dumpRunOptions = runOptionsNode.valueOf("dumpRunOptions");
        String dumpResourceManagerSummary = runOptionsNode.valueOf("dumpResourceManagerSummary");
        String dumpJavascriptEvals = runOptionsNode.valueOf("dumpJavascriptEvals");
        String reportResourceManagerSummary = runOptionsNode.valueOf("reportResourceManagerSummary");
        String reportResponseRaw = runOptionsNode.valueOf("reportResponseRaw");
        String reportPayloadsAsXML = runOptionsNode.valueOf("reportPayloadsAsXML");
        String skipMutators = runOptionsNode.valueOf("skipMutators");
        String skipMutatorsOnFailure = runOptionsNode.valueOf("skipMutatorsOnFailure");
        String evalReportLevel = runOptionsNode.valueOf("evalReportLevel");
        String outputServiceResultDB = runOptionsNode.valueOf("outputServiceResultDB");
        String sCondensedHeaders = runOptionsNode.valueOf("condensedHeaders");
        String sEmitRestReplayHeaders = runOptionsNode.valueOf("emitRestReplayHeaders");

        if (Tools.notBlank(connectionTimeout)) {
            this.connectionTimeout = Integer.parseInt(connectionTimeout);
        }
        if (Tools.notBlank(socketTimeout)) {
            this.socketTimeout = Integer.parseInt(socketTimeout);
        }
        if (Tools.notBlank(errorsBecomeEmptyStrings)) {
            this.errorsBecomeEmptyStrings = Tools.isTrue(errorsBecomeEmptyStrings);
        }
        if (Tools.notBlank(acceptAlertLevel)) {
            this.acceptAlertLevel = Alert.LEVEL.valueOf(acceptAlertLevel);
        }
        if (Tools.notBlank(failTestOnWarnings)) {
            this.failTestOnWarnings = Tools.isTrue(failTestOnWarnings);
        }
        if (Tools.notBlank(failTestOnErrors)) {
            this.failTestOnErrors = Tools.isTrue(failTestOnErrors);
        }
        if (Tools.notBlank(dumpMasterSummary)) {
            this.dumpMasterSummary = Tools.isTrue(dumpMasterSummary);
        }
        if (Tools.notBlank(dumpRunOptions)) {
            this.dumpRunOptions = Tools.isTrue(dumpRunOptions);
        }
        if (Tools.notBlank(dumpResourceManagerSummary)) {
            this.dumpResourceManagerSummary = Tools.isTrue(dumpResourceManagerSummary);
        }
        if (Tools.notBlank(dumpJavascriptEvals)) {
            this.dumpJavascriptEvals = Tools.isTrue(dumpJavascriptEvals);
        }
        if (Tools.notBlank(reportResourceManagerSummary)) {
            this.reportResourceManagerSummary = Tools.isTrue(reportResourceManagerSummary);
        }
        if (Tools.notBlank(reportResponseRaw)) {
            this.reportResponseRaw = Tools.isTrue(reportResponseRaw);
        }
        if (Tools.notBlank(reportPayloadsAsXML)) {
            this.reportPayloadsAsXML = Tools.isTrue(reportPayloadsAsXML);
        }
        if (Tools.notBlank(evalReportLevel)) {
            this.evalReportLevel = EVAL_REPORT_LEVEL.valueOf(evalReportLevel);
        }
        if (Tools.notBlank(skipMutators)) {
            this.skipMutators = Tools.isTrue(skipMutators);
        }
        if (Tools.notBlank(skipMutatorsOnFailure)) {
            this.skipMutatorsOnFailure = Tools.isTrue(skipMutatorsOnFailure);
        }
        if (Tools.notBlank(outputServiceResultDB)) {
            this.outputServiceResultDB = Tools.isTrue(outputServiceResultDB);
        }

        if (Tools.notBlank(sEmitRestReplayHeaders)) {
            this.emitRestReplayHeaders = Tools.isTrue(sEmitRestReplayHeaders);
        }
        if (Tools.notBlank(sCondensedHeaders)) {
            setCondensedHeadersString(sCondensedHeaders);
        }
        System.out.println("set RunOptions ("+context+"): "+toString());
    }
}
