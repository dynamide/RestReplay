package org.dynamide.restreplay;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Node;

public class ContentMutator {

    public ContentMutator(String theFileName) throws IOException {
        this.fileName = theFileName;
        contentRaw = new String(FileUtils.readFileToByteArray(new File(fileName)));
        JSONObject jo = new JSONObject(contentRaw);
        names = JSONObject.getNames(jo);
        max = names.length;
    }
    private String[] names;
    private String contentRaw = "";
    private int p = -1;
    private int max = 0;
    private String current;
    private String fileName = "";
    private Map<String,Range> idRanges = new HashMap<String, Range>();
    public String getMutationID(){
        return "no_"+current;
    }

    public String shortName(){
        return "ContentMutator";
    }

    public String toString(){
        StringBuffer b = new StringBuffer();
        b.append("ContentMutator["+fileName+"]:")
         .append("names:" + Arrays.toString(names))
         .append(";idRanges:" + idRangesToString());
        return b.toString();
    }

    public String idRangesToString(){
        StringBuffer b = new StringBuffer();
        for (Map.Entry<String,Range> entry: idRanges.entrySet()){
            b.append(entry.getKey()+":"+entry.getValue()+";");
        }
        return b.toString();
    }

    public String getID(){
        return " ["+p+"] no-"+current;
    }

    public boolean valueInRangeForId(int value, String mutationId){
        Range range = idRanges.get(mutationId);
        if (range!=null) {
            return range.valueInRange(value);
        }
        if (idRanges.get("*")!=null){
            return true; //for the wildcard, EVERYTHING is found.
        }
        return false;
    }

    public boolean hasRangeForId(String mutationId){
        if (idRanges.get("*")!=null){
            return true; //for the wildcard, EVERYTHING is found.
        }
        return (idRanges.get(mutationId) != null);
    }

    public String expectedRangeForID(String mutationId){
        Range range = idRanges.get(mutationId);
        if (range != null) {
            return range.toString();
        }
        range = idRanges.get("*");  //for the wildcard, EVERYTHING is found after we can't find mutationId.
        if (range!=null){
            return range.toString();
        }
        return "";
    }

    /** @return null when list is exhausted.*/
    public String mutate(){
        p++;
        if (p>=max){
            return null;
        }
        String[] dest = new String[max-1];
        System.arraycopy(names, 0, dest, 0, p - 0);
        System.arraycopy(names, p + 1, dest, p, max - p - 1);
        current = names[p];
        //System.out.println(String.format("names%s, dest%s, p%d, max%d, names.length%d", Arrays.toString(names), Arrays.toString(dest), p, max, names.length));
        JSONObject jo = new JSONObject(contentRaw);
        JSONObject subset = new JSONObject(jo, dest);
        return subset.toString();
    }

    public void setOptions(Node testNode){
        List<Node> nodes = testNode.selectNodes("mutator/expected/code");
        for (Node codeNode: nodes){
            Range range = new Range(codeNode.valueOf("@range"));
            String[] ids = codeNode.getStringValue().trim().split("\\s*,\\s*");
            for (String id: ids) {
                if (null != idRanges.get(id)) {
                    throw new IllegalArgumentException("Test node defines exclusion test case with multiple ranges: "+id
                                                      +" Please make sure this id appears in only one &lt;code> element.");
                }
                idRanges.put(id, range);
            }
        }
        //System.out.println("\r\n\r\n[[[[[["+fileName+"]]]]]]]]]]]]]]]]]]" + toString());
    }

    public static void main(String[]args) throws Exception {
        String fn = "/Users/vcrocla/src/RestReplay/src/test/resources/test-data/restreplay/_self_test/content-mutator-test.json";
        ContentMutator mutator = new ContentMutator(fn);
        System.out.println(mutator.mutate());
    }
}
