package org.dynamide.restreplay;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;

/** This class exists to overcome a shortcoming of org.json.* where there is no superclass
 * or interface to represent a JSON object OR a JSON array, which happens if someone sends
 * you arbitrary, legal JSON which can start with an object or an array.  There is no factory
 * or constructor in org.json.* which will do this.  Simply call construct a JSONSuper,
 * then you can ask it what type it is:
 * <pre>
 * JSONSuper sup = new JSONSuper(payloadString);
 * if (sup.type) == JSONSuper.TYPE.OBJECT {do something with sup.jsonObject ...}
 * if (sup.type) == JSONSuper.TYPE.ARRAY {do something with sup.jsonArray ...}
 * </pre>
 */
public class JSONSuper {
    public JSONObject jsonObject;
    public JSONArray jsonArray;
    public String payload;
    public enum TYPE {OBJECT, ARRAY, STRING}
    public TYPE type;
    public String toXMLString(){
        switch (this.type) {
            case ARRAY:
                return "<root>"+ XML.toString(this.jsonArray)+"</root>";
            case OBJECT:
                return "<root>"+XML.toString(this.jsonObject)+"</root>";
            case STRING:
            default:
                return payload;
        }
    }
    public JSONSuper(String payload){
        this.payload = payload;
        Object json = new JSONTokener(payload).nextValue();
        if (json instanceof JSONObject) {
            JSONObject jsonobject = new JSONObject(payload);
            this.jsonObject = jsonobject;
            this.type = JSONSuper.TYPE.OBJECT;
        } else if (json instanceof JSONArray) {
            JSONArray jsonarray = new JSONArray(payload);
            this.jsonArray = jsonarray;
            this.type = JSONSuper.TYPE.ARRAY;
        }
    }
    public static void unescape(JSONObject obj){
    }
}


