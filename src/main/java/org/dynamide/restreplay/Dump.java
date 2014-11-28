package org.dynamide.restreplay;

public class Dump {
    public boolean payloads = false;
    //public static final ServiceResult.DUMP_OPTIONS dumpServiceResultOptions = ServiceResult.DUMP_OPTIONS;
    public ServiceResult.DUMP_OPTIONS dumpServiceResult = ServiceResult.DUMP_OPTIONS.minimal;
    public String toString(){
        return "payloads: "+payloads+", dumpServiceResult: "+dumpServiceResult;
    }
    public static Dump getDumpConfig(){
        return new Dump();
    }

}
