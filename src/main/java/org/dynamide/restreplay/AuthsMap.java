package org.dynamide.restreplay;

import java.util.Map;

public class AuthsMap {
    Map<String,String> map;
    String defaultID="";
    public String getDefaultAuth(){
        return map.get(defaultID);
    }
    public String toString(){
        return "AuthsMap: {default='"+defaultID+"'; "+map.keySet()+'}';
    }
}
