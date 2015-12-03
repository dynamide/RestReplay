package org.dynamide.interpreters;

public class VarInfo {
    public String name = "";
    public String context = "";
    public String expression = "";
    public String testIDLabel = "";
    public int nestingLevel = 0;
    public int notFound = 0;
    public int error = 0;
    public String toString(){
        return "{"+name+","+testIDLabel+","+context+",["+error+","+notFound+"]}";
    }
}
