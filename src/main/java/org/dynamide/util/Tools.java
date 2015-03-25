package org.dynamide.util;

import java.io.File;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** General utility methods.
 *   Laramie Crocker
 * v.1.4
 */
public class Tools {
    /** useful for gluing URLs together, since this uses '/' as the glue character to call glue(String,String,String)*/
    public static String glue(String first, String second){
        return glue(first, "/", second);
    }
    /** @return first glued to second with the separator string, at most one time - useful for appending paths.
     */
    public static String glue(String first, String separator, String second){
        if (first==null) { first = ""; }
        if (second==null) { second = ""; }
        if (separator==null) { separator = ""; }
        if (first.startsWith(separator) && second.startsWith(separator)){
            return first.substring(0, first.length()-separator.length()) + second;
        }
        if (first.endsWith(separator) || second.startsWith(separator)){
            return first+second;
        }
        return first+separator+second;
    }

    public static String joinArray(String s, Object... a) {
        return a.length == 0 ? "" : a[0] + (a.length == 1 ? "" : s + joinArray(s, Arrays.copyOfRange(a, 1, a.length)));
    }

    public static String join(String delim, List<String> l) {
        int idx = 0;
        StringBuilder b = new StringBuilder();
        for (String s: l){
            if (idx>0) {
                b.append(delim);
            }
            b.append(s);
            idx++;
        }
        return b.toString();
    }


    public static String chop(String source, int length){
        return source.substring(0, Math.min(source.length(), length));
    }

    /** Remove all whitespace from a String.  */
    public static String squeeze(String s) {
        return s.replaceAll("\\s+", "");
    }

    /** Milliseconds from start time as defined by the Date class. */
    public static Long now(){
        return new Long((new java.util.Date()).getTime());
    }

     public static String nowLocale(){
        java.util.Date date = new java.util.Date();
        String result = java.text.DateFormat.getDateTimeInstance().format(date);
        date = null;
        return result;
    }

    /** Handles null strings as empty.  */
    public static boolean isEmpty(String str){
        return !notEmpty(str);
    }

    /** nulls, empty strings, and empty after trim() are considered blank. */
    public static boolean isBlank(String str){
        return !notBlank(str);
    }

    /** Handles null strings as empty.  */
    public static boolean notEmpty(String str){
        if (str==null) return false;
        if (str.length()==0) return false;
        return true;
    }
    public static boolean notBlank(String str){
        if (str==null) return false;
        if (str.length()==0) return false;
        if (str.trim().length()==0){
            return false;
        }
        return true;
    }

    /** Creates a TreeMap with a case-insenstive collator using the US Locale.
     */
    public static Map createSortedCaseInsensitiveMap(){
        java.text.Collator usCollator = java.text.Collator.getInstance(java.util.Locale.US);
        usCollator.setStrength(java.text.Collator.PRIMARY);
        Map m = new TreeMap(usCollator);
        return m;
    }

    /** Handles null strings as false.  */
    public static boolean isTrue(String test){
        return notEmpty(test) && (new Boolean(test)).booleanValue();
    }

                    /*  Example usage of searchAndReplace:
                        for (Map.Entry<String,String> entry : variablesMap.entrySet()){
                            String key = entry.getKey();
                            String replace = entry.getValue();
                            String find = "\\$\\{"+key+"\\}";   //must add expression escapes
                                                                //because $ and braces are "special", and we want to find "${object.CSID}"
                            uri = Tools.searchAndReplace(uri, find, replace);
                            System.out.println("---- REPLACE.uri:        "+initURI);
                            System.out.println("---- REPLACE.find:       "+find);
                            System.out.println("---- REPLACE.replace:    "+replace);
                            System.out.println("---- REPLACE.uri result: "+uri);
                        }
                    */
    public static String  searchAndReplace(String source, String find, String replace){
        Pattern pattern = Pattern.compile(find);
        Matcher matcher = pattern.matcher(source);
        String output = matcher.replaceAll(replace);
        return output;
    }
    
    public static String searchAndReplaceWithQuoteReplacement(String source, String find, String replace){
        Pattern pattern = Pattern.compile(find);
        Matcher matcher = pattern.matcher(source);
        String output = matcher.replaceAll(matcher.quoteReplacement(replace));
        return output;
    }


    public static String getStackTrace(Throwable e){
        return getStackTrace(e, 0);
    }

    public static String getStackTrace(){
        return getStackTrace(new Exception("Getting StackTrace"), 0);
    }

    public static String implode(String strings[], String sep) {
		String implodedString;
		if (strings.length == 0) {
			implodedString = "";
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append(strings[0]);
			for (int i = 1; i < strings.length; i++) {
				if (strings[i] != null && !strings[i].trim().isEmpty()) {
					sb.append(sep);
					sb.append(strings[i]);
				}
			}
			implodedString = sb.toString();
		}
		return implodedString;
	}
		



    /** @param includeLines if zero, returns all lines */
    public static String getStackTrace(Throwable e, int includeLines){
        if (e==null){
            return "";
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(bos);
        e.printStackTrace(ps);
        String result = bos.toString();
        try {
            if(bos!=null)bos.reset();
            else System.out.println("bos was null, not closing");
        } catch (Exception e2)  {System.out.println("ERROR: couldn't reset() bos in Tools "+e2);}

        if (includeLines == 0){
            int iLeader = result.indexOf("\tat ");
            if (iLeader>-1){
                return result.substring(0, iLeader)+ "<pre>"+result.substring(iLeader)+"</pre>";
            } else {
                return "<pre>" + result + "</pre>";   //return all.
            }
        }
        StringBuffer sb = new StringBuffer();
        int i = 0;
        String[] foo = result.split(System.lineSeparator());
        sb.append("<pre>\r\n");
        for (String line: foo){
            i++;
            if (i>includeLines){
                sb.append("  ...first "+i+" lines. "+(foo.length-i)+" more.\r\n");
                sb.append("</pre>\r\n");
                return sb.toString();
            }
            sb.append(line).append("\r\n");
        }
        sb.append("</pre>\r\n");
        return sb.toString();
    }

    public static String getStackTraceTop(Throwable t, int lo, int hi, String delimiter){
        if ( t == null ) {
            return "";
        }
            StringBuffer result = new StringBuffer();
            StackTraceElement[] elements = t.getStackTrace();
            int elements_size = elements.length;
            StackTraceElement element;
            String test;
            for (int i=lo; i < elements_size; i++) {
                if (i>=hi){
                    break;
                }
                element = elements[i];
                test = element.toString();
                result.append(test);
                result.append(delimiter);
            }
            return result.toString();
    }

    public static String errorToString(Throwable e, boolean stackTraceOnException){
        return errorToString(e, stackTraceOnException, 0);
    }

    /** Takes an Exception object and formats a message that provides more debug information
      * suitable for developers for printing to System.out or for logging.  Not suitable for
      * presentation of error messages to clients.
     * @param includeLines if zero, return all lines of stack trace, otherwise return number of lines from top.
      */
    public static String errorToString(Throwable e, boolean stackTraceOnException, int includeLines){
        if (e==null){
            return "";
        }
        String s = e.toString() + "\r\n  -- message: " + e.getMessage();

        StringBuffer causeBuffer = new StringBuffer();
        Throwable cause = e.getCause();
        while (cause != null){
            causeBuffer.append(cause.getClass().getName()+"::"+cause.getMessage()+"\r\n");
            cause = cause.getCause();
        }
        if (causeBuffer.length()>0) s = s + "\r\n  -- Causes: "+causeBuffer.toString();


        s = s + "\r\n  -- Stack Trace: \r\n  --      " + getStackTrace(e, includeLines);
        return s;
    }

    public static String encodeURLString(String s){
        return URLEncoder.encode(s);
    }

    public static String decodeURLString(String URLString){
        if ( URLString == null ) {
            return "";
        }
        return URLDecoder.decodeURLString(URLString);
    }

    public static void testStatic(){
        System.out.println(getStackTrace());
        System.out.println(getStackTrace(new Exception("Created exception."), 2));
        List list = new ArrayList();
        list.add("item1");
        System.out.println("one item: "+join(" ", list));
        list.add("item2");
        System.out.println("two items: "+join(" ", list));
    }

    public static void main(String[]args)
    throws Exception {
        testStatic();
    }
}
