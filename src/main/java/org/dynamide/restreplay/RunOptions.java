package org.dynamide.restreplay;

import org.dom4j.Node;
import org.dynamide.interpreters.Alert;
import org.dynamide.util.Tools;

import java.util.List;

public class RunOptions {
    public static enum EVAL_REPORT_LEVEL {NONE, SHORT, ALL};
    public final static int MAX_CHARS_FOR_REPORT_LEVEL_SHORT = 300;

    public EVAL_REPORT_LEVEL evalReportLevel = EVAL_REPORT_LEVEL.SHORT;
    public static final String RUN_OPTIONS_FILENAME = "runOptions.xml";
    public int connectionTimeout = 30000;   //millis until gives up on establishing a connection.
    public int socketTimeout = 30000;  //millis until gives up on data bytes transmitted, apache docs say "timeout for waiting for data".
    public boolean errorsBecomeEmptyStrings = true;
    public Alert.LEVEL acceptAlertLevel = Alert.LEVEL.OK;  //OK means breaks on WARN and ERROR.
    public boolean skipMutators = false;
    public boolean skipMutatorsOnFailure = true;
    public boolean dumpResourceManagerSummary = true;
    public boolean reportResourceManagerSummary = true;
    public boolean failTestOnErrors = true;   //for one test, do we report SUCCESS or FAILURE.
    public boolean failTestOnWarnings = true; //for one test, do we report SUCCESS or FAILURE.


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
        String CR = ",\n\t\t";
        return "{" +
                "connectionTimeout=" + connectionTimeout + CR +
                " socketTimeout=" + socketTimeout + CR+
                " errorsBecomeEmptyStrings=" + errorsBecomeEmptyStrings + CR+
                " acceptAlertLevel=" + acceptAlertLevel + CR+
                " failTestOnWarnings=" + failTestOnWarnings + CR+
                " failTestOnErrors=" + failTestOnErrors + CR+
                " evalReportLevel=" + evalReportLevel +CR+
                " dumpResourceManagerSummary=" + dumpResourceManagerSummary + CR+
                " reportResourceManagerSummary=" + reportResourceManagerSummary +CR+
                " skipMutatorsOnFailure=" + skipMutatorsOnFailure + CR+
                " skipMutators=" + skipMutators + "\n\t\t}";
    }

    public String toHTML() {
        String BR = "<br />\r\n";
        String C = ",";
        return "<div class='RunOptions'>{" +BR+
                "connectionTimeout=" + connectionTimeout +C+BR+
                "socketTimeout=" + socketTimeout +C+BR+
                "errorsBecomeEmptyStrings=" + errorsBecomeEmptyStrings +C+BR+
                "acceptAlertLevel=" + acceptAlertLevel +C+BR+
                "failTestOnWarnings=" + failTestOnWarnings +C+BR+
                "failTestOnErrors=" + failTestOnErrors +C+BR+
                "evalReportLevel=" + evalReportLevel +C+BR+
                "dumpResourceManagerSummary=" + dumpResourceManagerSummary +C+BR+
                "reportResourceManagerSummary=" + reportResourceManagerSummary +C+BR+
                "skipMutatorsOnFailure=" + skipMutatorsOnFailure +BR+
                "skipMutators=" + skipMutators +BR+
                "}</div>";
    }



    // from xml file as xpath: "/restReplayMaster/runOptions"
    public void addRunOptions(Node runOptionsNode, String context){
        String connectionTimeout = runOptionsNode.valueOf("connectionTimeout");
        String socketTimeout = runOptionsNode.valueOf("socketTimeout");
        String errorsBecomeEmptyStrings = runOptionsNode.valueOf("errorsBecomeEmptyStrings");
        String acceptAlertLevel = runOptionsNode.valueOf("acceptAlertLevel");
        String failTestOnWarnings = runOptionsNode.valueOf("failTestOnWarnings");
        String failTestOnErrors = runOptionsNode.valueOf("failTestOnErrors");
        String dumpResourceManagerSummary = runOptionsNode.valueOf("dumpResourceManagerSummary");
        String reportResourceManagerSummary = runOptionsNode.valueOf("reportResourceManagerSummary");
        String skipMutators = runOptionsNode.valueOf("skipMutators");
        String skipMutatorsOnFailure = runOptionsNode.valueOf("skipMutatorsOnFailure");
        String evalReportLevel = runOptionsNode.valueOf("evalReportLevel");

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
        if (Tools.notBlank(dumpResourceManagerSummary)) {
            this.dumpResourceManagerSummary = Tools.isTrue(dumpResourceManagerSummary);
        }
        if (Tools.notBlank(reportResourceManagerSummary)) {
            this.reportResourceManagerSummary = Tools.isTrue(reportResourceManagerSummary);
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
        System.out.println("set RunOptions ("+context+"): "+toString());
    }
}
