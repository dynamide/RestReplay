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

    public void setOptions(Node testNode){
        super.setOptions(testNode);
        List<Node> vars = testNode.selectNodes("mutator/vars");
        for (Node oneVarsNode: vars){
            List<Node> nodes = oneVarsNode.selectNodes("var");
            List<VarTemplate> varList = new ArrayList<VarTemplate>();
            for (Node varNode : nodes) {
                String idbase = varNode.valueOf("@idbase");
                String template = varNode.valueOf(".");
                varList.add(new VarTemplate(idbase, template));
            }
            spaces.add(varList);
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
        return "varGroup_"+(index-1);
    }

    public String getID(){
        return " ["+(index-1)+"] var"; //TODO. Make descriptive once you sort this out.
    }

    @Override
    public int getIndex(){
        return index-1;
    }

}
