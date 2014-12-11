package org.dynamide.interpreters;

import java.util.List;

public class AlertError extends Error {
    public AlertError(String msg, Alert triggeringAlert, List<Alert> allAlerts){
        super(msg);
        this.triggeringAlert = triggeringAlert;
        this.allAlerts = allAlerts;
    }
    public Alert triggeringAlert;
    public List<Alert> allAlerts;
}

