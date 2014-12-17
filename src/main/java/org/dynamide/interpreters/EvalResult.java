package org.dynamide.interpreters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvalResult {
    public String context = "";     //informational
    public String expression = "";  //informational
    public String testIDLabel = ""; //informational
    public boolean isDummy = false;
    public int nestingLevel = 0;

    public Object result = "";
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