package org.dynamide.restreplay;

import org.dynamide.util.DateUtils;
import org.dynamide.util.GregorianCalendarDateTimeUtils;
import org.dynamide.util.Tools;
import org.dynamide.util.XmlTools;

import java.io.PrintStream;

public class Kit {
    private static Tools TOOLS = new Tools();
    private static XmlTools XMLTOOLS = new XmlTools();
    private static GregorianCalendarDateTimeUtils GREGORIAN = new GregorianCalendarDateTimeUtils();
    private static DateUtils DATEUTILS = new DateUtils();

    public static Tools getTools() {
        return TOOLS;
    }

    public static XmlTools getXmlTools() {
        return XMLTOOLS;
    }

    public static GregorianCalendarDateTimeUtils getGregorian(){
        return GREGORIAN;
    }

    public static DateUtils getDateUtils(){
        return DATEUTILS;
    }

    public static DateUtils getDates(){
        return DATEUTILS;
    }

    /** Milliseconds from start time as definded by the Date class. */
    public static Long now(){
        return new Long((new java.util.Date()).getTime());
    }

    public static PrintStream getOut(){
        return System.out;
    }

    public static PrintStream getErr(){
        return System.err;
    }

}
