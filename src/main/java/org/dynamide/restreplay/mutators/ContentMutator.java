package org.dynamide.restreplay.mutators;

import org.dynamide.restreplay.Range;
import org.dynamide.restreplay.ResourceManager;
import org.dynamide.util.Tools;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Node;

/** ContentMutator implements the basic, common functionality of all IMutators.
 *  Specifically, this class provides logic around the Range class, and stores the raw content from the init.
 *  Mostly IMutator implementors will just want to implement
 *  Use a descendent of this class (or other implementations that fulfill IMutator).
 *  That descendent should be registered in MutatorFactor.registerMutator(...), installed mutators are initialized in the static block in MutatorFactory.
 *  Create an IMutator by calling the factory: MutatorFactory.createMutator(...).
 */
public abstract class ContentMutator implements IMutator {

    public void init(String relResourceName, String theFileName, ResourceManager resourceManager) throws IOException {
        contentRawFilename = theFileName;
        contentRaw = resourceManager.readResource("ContentMutator:constructor", relResourceName, theFileName);//new String(FileUtils.readFileToByteArray(new File(contentRawFilename)));
    }

    private String contentRaw;
    protected String getContentRaw(){
        return contentRaw;
    }

    private String contentRawFilename;
    public String getContentRawFilename(){
        return contentRawFilename;
    }

    private final Map<String,Range> idRanges = new LinkedHashMap<String, Range>();

    public String shortName(){
        return "ContentMutator";
    }

    public String toString(){
        StringBuffer b = new StringBuffer();
        b.append("ContentMutator["+ contentRawFilename +"]:")
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

    /** Example:
     *    ranges[no_fieldA,no_fieldB,no_fieldC]->[[200-205,300-301,4xx], [200-205], [300-390,4xx]]
     */
    public boolean valueInRangeForId(int value, String mutationId){
        Range range = idRanges.get(mutationId);
        if (range!=null) {
            return range.valueInRange(value);
        }
        range = idRanges.get("*");
        if (range!=null){
            return range.valueInRange(value);
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

    public void setOptions(Node testNode){
        List<Node> nodes = testNode.selectNodes("mutator/expected/code");
        for (Node codeNode: nodes){
            Range range = new Range(codeNode.valueOf("@range"));
            String stringRanges = codeNode.getStringValue().trim();
            if (Tools.isBlank(stringRanges)){
                idRanges.put("*", range);
            } else {
                String[] ids = stringRanges.split("\\s*,\\s*");
                for (String id : ids) {
                    if (null != idRanges.get(id)) {
                        throw new IllegalArgumentException("Test node defines exclusion test case with multiple ranges: " + id
                                + " Please make sure this id appears in only one &lt;code> element.");
                    }
                    idRanges.put(id, range);
                }
            }
        }
        //System.out.println("\r\n\r\n[[[[[["+contentRawFilename+"]]]]]]]]]]]]]]]]]]" + toString());
    }


}
