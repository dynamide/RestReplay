package org.dynamide.interpreters;

public class Alert {
    public Alert(String message, String context, LEVEL level){
        this.message = message;
        this.context = context;
        this.level = level;
    }
    public enum LEVEL {OK, WARN, ERROR}
    public LEVEL level;
    public String message = "";
    public String context = "";
    public static Alert alert(String m, String p, LEVEL l){
        return new Alert(m,p,l);
    }
    public String toString(){
        return ""+level+':'+context+':'+message;
    }

}
