package org.dynamide.util;

import java.io.ByteArrayOutputStream;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

/** This class really only decodes the query portion of a URL.
 *  It does not parse the protocol, host, port, path, or target.
 */
public class URLDecoder {
    private URLDecoder(){
    }

    /** You can pass in a full URL String or everything after and including the question mark,
     * e.g. "?foo=1&bar=2"  and  "http://dynamide.com:8080/mojo/nixon?foo=1&bar=2" are equivalent.
     */
    public URLDecoder(String url){
        //setURL(url);
        setContent(url);
    }

    public String toString(){
        return "URLDecoder: "+content_cache+" fields: "+fieldsVector.toString();
    }

    //private String  m_url = "";
    //public String  getURLString(){return m_url;}
    //public void setURL(String  new_value){m_url = new_value;}

    //public URL getURL()
    //throws Exception {
    //    return new URL(m_url);
    //}

    /**  Use this method to get the name-value pairs in a CGI string.  That string
     *   should have been passed to the constructor of this class.  The names are
     *   the even-indexed elements (starting at zero) in the enumeration, and the
     *   values are the odd-indexed elements (starting at one).
     */
    public Enumeration getFields(){
        return fieldsVector.elements();
    }

    public Enumeration getFieldNames(){
        return fieldNamesVector.elements();
    }


    /**  Use this method to get the named values in a CGI string.  That string
     *   should have been passed to the constructor of this class.
     */
    public String getFieldValue(String name){
        String aName;
        for (Enumeration nameValues = getFields(); nameValues.hasMoreElements(); ){
            aName = (String)nameValues.nextElement();
            if ( aName.equals(name) ){
                return (String)nameValues.nextElement();
            }
        }
        return "";
    }

    public boolean hasField(String name){
        return fieldNamesVector.indexOf(name) > -1;
    }

    private String content_cache = "";

    public String getContent(){return content_cache;}

    private String m_path = "";
    public String getPath(){return m_path;}
    public void setPath(String new_value){m_path = new_value;}

    private String m_rawValue = "";
    public String getRawValue(){return m_rawValue;}
    public void setRawValue(String new_value){m_rawValue = new_value;}

    protected void setContent(String new_cache){
        m_rawValue = new_cache;
        try {
            int iPound = new_cache.indexOf("#");
            if ( iPound > -1 ) {
                new_cache = new_cache.substring(0, iPound);
            }
            int iQuestion = new_cache.indexOf("?");
            if ( iQuestion == -1 ) {
                //seems to be working.  no need for warning. //Log.warn(URLDecoder.class, "setContent didn't find a question mark '"+new_cache+"' -- Treating as a path");
                m_path = new_cache;
                return;
            }
            m_path = new_cache.substring(0, iQuestion);
            String tcache = new_cache.substring(iQuestion+1);    //If not found, iQuestion+1 == 0, which is OK.
            content_cache = decodeURLString(tcache);
            updateFieldsVector();
        } catch (Exception e)  {
            System.err.println("URLDecoder.class, setContent caught exception parsing '"+new_cache+"' " +  e);
        }
    }

    /*protected void setContent_OLD(String new_cache){
        try {
            int iQuestion = new_cache.indexOf("?");
            //System.out.println("iQuestion: "+iQuestion+ " new_cache: "+new_cache);
            if ( iQuestion == 0 ) {
                //URI can't handle something like this: "?MOJO=foo"
                String tcache = new_cache.substring(iQuestion+1);
                content_cache = decodeURLString(tcache);
                updateFieldsVector();
            } else {
                URI u = new URI(new_cache); // %% Java 1.4 only %%
                m_URI = u.getPath();
                String query = u.getQuery();
                if ( query != null && query.length()>0) {
                    content_cache = decodeURLString(query);
                    updateFieldsVector();
                }
            }
        } catch (Exception e)  {
            Log.error(URLDecoder.class, "setContent caught exception parsing '"+new_cache+"' ", e);
        }
    } */

    private Vector fieldsVector = new Vector();

    private Vector fieldNamesVector = new Vector();

    private void updateFieldsVector(){
        StringTokenizer st = new StringTokenizer(getContent(), "&");
        fieldsVector.removeAllElements();
        fieldNamesVector.removeAllElements();
        String name, value, namevalue;
        int iEquals;
        while (st.hasMoreTokens()){
            namevalue = st.nextToken();
            iEquals = namevalue.indexOf('=');
            if (iEquals > -1 ){
                name = namevalue.substring(0, iEquals);
                if (iEquals+1 > namevalue.length())
                    value = "";
                else
                    value = namevalue.substring(iEquals+1);
                fieldsVector.addElement(name);
                fieldsVector.addElement(value);
                fieldNamesVector.addElement(name);
            }
        }
    }

    /** Translates String from x-www-form-urlEncoded format into text.
      * The procedure was copied from O'Reilly Java Network Programming.
      * @param s String to be translated
      * @return the translated String.
      */
    public static String decodeURLString(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());

        for (int i = 0; i < s.length(); i++) {
          int c = (int) s.charAt(i);
          if (c == '+') {
            out.write(' ');
          } else if (c == '%') {
            if (s.length()==(i+1)){
                //malformed -- browser sent blabla%
                out.write('%');
            } else if (s.length()==(i+2)){
                //malformed -- browser sent blabla%1 (% followed by one digit`)
                out.write('%');
                out.write(s.charAt(++i));
            } else {
                //OK, browser sent blabla%11 (% followed by two digits)
                int c1 = Character.digit(s.charAt(++i), 16);
                int c2 = Character.digit(s.charAt(++i), 16);
                out.write((char) (c1 * 16 + c2));
            }
          }
          else if (c == ' '){ //Laramie 981103 Banish anything after a space - a space ends a URL.
            break;
          }
          else {
            out.write(c);
          }
        } // end for

        String result = out.toString();
        try {
            if(out!=null)out.reset();
            else System.out.println("out was null, not closing");
        } catch (Exception e)  {System.out.println("ERROR: couldn't reset() out in URLDecoder "+e);}
        return result;

    }


}
