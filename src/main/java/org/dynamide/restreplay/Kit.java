package org.dynamide.restreplay;

import org.dynamide.util.GregorianCalendarDateTimeUtils;
import org.dynamide.util.Tools;
import org.dynamide.util.XmlTools;

public class Kit {
    private static Tools TOOLS = new Tools();
    private static XmlTools XMLTOOLS = new XmlTools();
    private static GregorianCalendarDateTimeUtils GREGORIAN = new GregorianCalendarDateTimeUtils();

    public static Tools getTools() {
        return TOOLS;
    }

    public static XmlTools getXmlTools() {
        return XMLTOOLS;
    }
    public static GregorianCalendarDateTimeUtils getGregorian(){
        return GREGORIAN;
    }

}
