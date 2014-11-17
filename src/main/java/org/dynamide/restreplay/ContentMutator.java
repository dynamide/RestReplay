package org.dynamide.restreplay;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

    public String getID(){
        return " ["+p+"] no-"+current;
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

    public static void main(String[]args) throws Exception {
        String fn = "/Users/vcrocla/src/RestReplay/src/test/resources/test-data/restreplay/_self_test/content-mutator-test.json";
        ContentMutator mutator = new ContentMutator(fn);
        System.out.println(mutator.mutate());
    }
}
