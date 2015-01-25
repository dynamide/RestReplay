package org.dynamide.restreplay.mutators;

import org.dom4j.Node;
import org.dynamide.restreplay.Eval;
import org.dynamide.restreplay.ResourceManager;
import org.dynamide.restreplay.ServiceResult;

import java.io.IOException;
import java.util.Map;

public interface IMutator {
    public void init(String requestPayloadFilenameRel, String requestPayloadFilename, ResourceManager resourceManager) throws IOException;

    public void setOptions(Node node);

    public boolean hasRangeForId(String mutationId);

    public String expectedRangeForID(String mutationId);

    /** Example:
     *    ranges[no_fieldA,no_fieldB,no_fieldC]->[[200-205,300-301,4xx], [200-205], [300-390,4xx]]
     */
    public boolean valueInRangeForId(int value, String mutationId);

    //
    /** IMutator descendent implementations should override this method.
     *  @return null when list is exhausted.*/
    public String mutate(Map<String, Object> clonedMasterVars, Eval evalStruct, ServiceResult serviceResult);

    /** IMutator descendent implementations should override this method. **/
    public String getMutationID();

    /** IMutator descendent implementations should override this method. **/
    public String getID();

    public int getIndex();
}
