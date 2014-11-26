package org.dynamide.restreplay;

import org.dom4j.Node;
import org.dynamide.util.Tools;

import java.util.List;

public class RunOptions {
    public static final String RUN_OPTIONS_FILENAME = "runOptions.xml";
    public int connectionTimeout = 30000;   //millis until gives up on establishing a connection.
    public int socketTimeout = 30000;  //millis until gives up on data bytes transmitted, apache docs say "timeout for waiting for data".
    public boolean errorsBecomeEmptyStrings = true;
    public ServiceResult.Alert.LEVEL acceptAlertLevel = ServiceResult.Alert.LEVEL.OK;  //OK means breaks on WARN and ERROR.
    public boolean skipMutators = false;
    public boolean dumpResourceManagerSummary = true;
    public boolean breakNow(ServiceResult.Alert alert) {
        return (alert.level.compareTo(this.acceptAlertLevel) > 0);
    }
    public boolean breakNow(List<ServiceResult.Alert> alerts) {
        for (ServiceResult.Alert alert : alerts) {
            if (this.breakNow(alert)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" +
                "connectionTimeout=" + connectionTimeout +
                ", socketTimeout=" + socketTimeout +
                ", errorsBecomeEmptyStrings=" + errorsBecomeEmptyStrings +
                ", acceptAlertLevel=" + acceptAlertLevel +
                ", dumpResourceManagerSummary=" + dumpResourceManagerSummary +
                ", skipMutators=" + skipMutators +
                '}';
    }

    public String toHTML() {
        String BR = "<br />\r\n";
        String C = ",";
        return "<div class='RunOptions'>{" +BR+
                "connectionTimeout=" + connectionTimeout +C+BR+
                "socketTimeout=" + socketTimeout +C+BR+
                "errorsBecomeEmptyStrings=" + errorsBecomeEmptyStrings +C+BR+
                "acceptAlertLevel=" + acceptAlertLevel +C+BR+
                "dumpResourceManagerSummary=" + dumpResourceManagerSummary +C+BR+
                "skipMutators=" + skipMutators +BR+
                "}</div>";
    }



    // from xml file as xpath: "/restReplayMaster/runOptions"
    public void addRunOptions(Node runOptionsNode, String context){
        String connectionTimeout = runOptionsNode.valueOf("connectionTimeout");
        String socketTimeout = runOptionsNode.valueOf("socketTimeout");
        String errorsBecomeEmptyStrings = runOptionsNode.valueOf("errorsBecomeEmptyStrings");
        String dumpResourceManagerSummary = runOptionsNode.valueOf("dumpResourceManagerSummary");
        String skipMutators = runOptionsNode.valueOf("skipMutators");
        if (Tools.notBlank(connectionTimeout)) {
            this.connectionTimeout = Integer.parseInt(connectionTimeout);
        }
        if (Tools.notBlank(socketTimeout)) {
            this.socketTimeout = Integer.parseInt(socketTimeout);
        }
        if (Tools.notBlank(errorsBecomeEmptyStrings)) {
            this.errorsBecomeEmptyStrings = Tools.isTrue(errorsBecomeEmptyStrings);
        }
        if (Tools.notBlank(dumpResourceManagerSummary)) {
            this.dumpResourceManagerSummary = Tools.isTrue(dumpResourceManagerSummary);
        }
        if (Tools.notBlank(skipMutators)) {
            this.skipMutators = Tools.isTrue(skipMutators);
        }
        System.out.println("set RunOptions ("+context+"): "+toString());
    }
}
