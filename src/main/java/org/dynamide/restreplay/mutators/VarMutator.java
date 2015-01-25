package org.dynamide.restreplay.mutators;

import org.dom4j.Node;
import org.dynamide.interpreters.EvalResult;
import org.dynamide.restreplay.Eval;
import org.dynamide.restreplay.ResourceManager;
import org.dynamide.restreplay.ServiceResult;

import java.io.IOException;
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

    private Map<String, Object> clonedMasterVars;

    public String mutate(Map<String, Object> clonedMasterVars, Eval evalStruct, ServiceResult serviceResult){
        if (index+1>spaces.size()){
            return null;
        }

        List<VarTemplate> localVarList = spaces.get(index);
        for (VarTemplate vt: localVarList) {
            clonedMasterVars.put(vt.ID, vt.template);
        }
        this.clonedMasterVars = clonedMasterVars;

        index++;
        return getContentRaw(evalStruct, serviceResult);
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
        System.out.println("------> funny safeID, i: "+i+" size: "+sz+" spaceIDs:"+spaceIDs);
        return ""+(index-1);
    }

    public String getID(){
        return safeID();
    }

    @Override
    public int getIndex(){
        return index-1;
    }

    //==================== for VarMutator these must be different ================================================

    private ResourceManager resourceManager = null;
    private String contentRawFilename = "";
    private String contentRawRelResourceName = "";

    @Override
    public void init(String relResourceName, String theFileName, ResourceManager resourceManager)
    throws IOException {
        this.contentRawFilename = theFileName;
        this.contentRawRelResourceName = relResourceName;
        this.resourceManager = resourceManager;
    }

    public String getContentRaw(Eval evalStruct, ServiceResult serviceResult){
        try {
            if (resourceManager != null) {
                EvalResult filanameRelEvalResult  = evalStruct.eval("expand req. filenameRel:" + contentRawRelResourceName,
                                                                          contentRawRelResourceName,
                                                                          clonedMasterVars);
                serviceResult.alerts.addAll(filanameRelEvalResult.alerts);
                String requestPayloadFilenameRelExp = filanameRelEvalResult.getResultString();


                return resourceManager.readResource("ContentMutator:constructor",
                        requestPayloadFilenameRelExp,
                        contentRawFilename);//new String(FileUtils.readFileToByteArray(new File(contentRawFilename)));
            }
        } catch (Exception e){
            System.out.println("ERROR in getContentRaw()"+e);
            return "";
        }
        return "";
    }

}
