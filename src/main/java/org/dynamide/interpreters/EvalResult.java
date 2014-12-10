package org.dynamide.interpreters;

import java.util.ArrayList;
import java.util.List;

public class EvalResult {
    public Object result = "";
    public String getResultString(){
        if (result!=null){
            return result.toString();
        }
        return "";
    }
    public List<Alert> alerts = new ArrayList<Alert>();
    public void addAlert(String m, String p, Alert.LEVEL l){
        this.alerts.add(new Alert(m,p,l));
    }
}