package org.dynamide.restreplay.mutators;

import org.dom4j.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VarMutator extends ContentMutator implements IMutator {

    protected static class VarTemplate{
        public VarTemplate(String i, String t){ID=i;template=t;}
        public String ID;
        public String template;
    }

    private int index = 0;

    private List<List<VarTemplate>> spaces = new ArrayList<List<VarTemplate>>();
    private List<String>spaceIDs = new ArrayList<String>();

    public void setOptions(Node testNode){
        super.setOptions(testNode);
        List<Node> vars = testNode.selectNodes("mutator/vars");
        for (Node oneVarsNode: vars){
            String spaceID = oneVarsNode.valueOf("@ID");
            List<Node> nodes = oneVarsNode.selectNodes("var");
            List<VarTemplate> varList = new ArrayList<VarTemplate>();
            for (Node varNode : nodes) {
                String idbase = varNode.valueOf("@idbase");
                String template = varNode.valueOf(".");
                varList.add(new VarTemplate(idbase, template));
            }
            spaces.add(varList);
            spaceIDs.add(spaceID);
        }
    }

    public String mutate(Map<String, String> clonedMasterVars){
        if (index+1>spaces.size()){
            return null;
        }

        List<VarTemplate> localVarList = spaces.get(index);
        for (VarTemplate vt: localVarList) {
            clonedMasterVars.put(vt.ID, vt.template);
        }

        index++;
        return getContentRaw();
    }

    public String getMutationID(){
        return safeID();
    }
    private String safeID(){
        int i = index-1;
        int sz = spaceIDs.size();
        if (sz > 0 && i < sz){
            return spaceIDs.get(i);
        }
        System.out.println("funny i: "+i+" size: "+sz);
        return ""+(index-1);
    }

    public String getID(){
        return safeID();
    }

    @Override
    public int getIndex(){
        return index-1;
    }

}
