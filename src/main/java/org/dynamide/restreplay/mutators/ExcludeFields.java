package org.dynamide.restreplay.mutators;

import org.dynamide.interpreters.Eval;
import org.dynamide.restreplay.ResourceManager;
import org.dynamide.restreplay.RunOptions;
import org.dynamide.restreplay.ServiceResult;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExcludeFields extends ContentMutator {
    public void init(String relResourceName, String theFileName, ResourceManager resourceManager) throws IOException {
        super.init(relResourceName, theFileName, resourceManager);
        jo = new JSONObject(getContentRaw());
        names = JSONObject.getNames(jo);
        max = names.length;
        dest = new String[max-1];
    }

    String[] dest;
    private JSONObject jo;
    private String[] names;
    private int p = -1;
    private int max;
    private String current;

    /** @return null when list is exhausted.*/
    public String mutate(Map<String, Object> clonedMasterVars,
                         Eval evalStruct,
                         ServiceResult serviceResult){
        p++;
        if (p>=max){
            return null;
        }
        System.arraycopy(names, 0, dest, 0, p - 0);
        System.arraycopy(names, p + 1, dest, p, max - p - 1);
        current = names[p];
        //System.out.println(String.format("names%s, dest%s, p%d, max%d, names.length%d", Arrays.toString(names), Arrays.toString(dest), p, max, names.length));
        JSONObject subset = new JSONObject(jo, dest);
        return subset.toString();
    }

    public String getMutationID(){
        return current;
    }

    public String getID(){
        return " ["+p+"] no-"+current;
    }

    @Override
    public int getIndex(){
        return p;
    }

    public String toString(){
        StringBuffer b = new StringBuffer();
        b.append("ExcludeFields["+getContentRawFilename()+"]:")
                .append("names:" + Arrays.toString(names))
                .append(";idRanges:" + idRangesToString());
        return b.toString();
    }

    public static void main(String[]args) throws Exception {
        String relResourceName = "_self_test/content-mutator-test.json";
        String fn = "/Users/vcrocla/src/RestReplay/src/main/resources/restreplay/_self_test/content-mutator-test.json";
        ResourceManager standaloneResourceManager = ResourceManager.createRootResourceManager();
        ServiceResult serviceResult = new ServiceResult(new RunOptions());
        Eval evalStruct = new Eval();
        IMutator mutator
            = MutatorFactory.createMutator("ExcludeFields",
                                            relResourceName,
                                            fn,
                                            standaloneResourceManager,
                                            null);

        Map<String, Object> clonedMasterVars = new LinkedHashMap<String, Object>();
        String m = mutator.mutate(clonedMasterVars, evalStruct, serviceResult);
        while(m!=null){
            System.out.println(m);
            m = mutator.mutate(clonedMasterVars, evalStruct, serviceResult);
        }
        System.out.println("main done.  ResourceManager.formatSummaryPlain: "+standaloneResourceManager.formatSummaryPlain());
    }

}
