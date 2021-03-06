package org.dynamide.interpreters;

import java.util.*;

public class EvalResult {
    public String context = "";     //informational
    public String expression = "";  //informational
    public String testIDLabel = ""; //informational
    public boolean isDummy = false;
    public boolean isError = false;
    public int nestingLevel = 0;
    public Map<String,List<VarInfo>> vars = new HashMap<String,List<VarInfo>>();

    public Object result = "";
    public boolean useResultAsObject = false;
    public Alert.LEVEL worstLevel = Alert.LEVEL.OK;
    public String getResultString(){
        if (result!=null){
            return result.toString();
        }
        return "";
    }
    public List<Alert> alerts = new ArrayList<Alert>();
    public void addAlert(String m, String p, Alert.LEVEL l){
        if (worstLevel.compareTo(l)<0){
           worstLevel = l;
        }
        this.alerts.add(new Alert(m,p,l));
    }
    public void addAllAlerts(List<Alert> otherAlerts){
        for (Alert otherAlert: otherAlerts){
            this.alerts.add(otherAlert);
            if (worstLevel.compareTo(otherAlert.level)<0){
                this.worstLevel = otherAlert.level;
            }
        }
    }
    public String toString(){
        int size =alerts.size();
        Alert[] alertArray = new Alert[size];
        Alert[] array = alerts.toArray(alertArray);
        String result = Arrays.toString(array);
        if (array.length==0){
            return getResultString();
        }
        return "EvalResult: {result: "+getResultString()+"; alerts: "+result+"}";
    }
}