package org.dynamide.restreplay.mutators;

import org.dom4j.Node;
import org.dynamide.restreplay.ResourceManager;

import java.util.HashMap;
import java.util.Map;

public class MutatorFactory {

    private static Map<String, String> mTypes = new HashMap<String, String>();

    static {
        registerMutator("ExcludeFields", "org.dynamide.restreplay.mutators.ExcludeFields");
        registerMutator("VarMutator",    "org.dynamide.restreplay.mutators.VarMutator");
    }

    public static IMutator createMutator(String type,
                                         String requestPayloadFilenameRel,
                                         String requestPayloadFilename,
                                         ResourceManager resourceManager,
                                         Node optionsNode)
    throws Exception {
        IMutator mutator = createMutatorNoInit(type);
        mutator.init(requestPayloadFilenameRel, requestPayloadFilename, resourceManager);
        if (optionsNode!=null){
            mutator.setOptions(optionsNode);
        }
        return mutator;
    }

    @SuppressWarnings({"unchecked"})
    public static IMutator createMutatorNoInit(String type)
    throws Exception {
        if (mTypes.containsKey(type)){
            Object o = Class.forName(mTypes.get(type)).newInstance();
            if (o instanceof IMutator){
                return (IMutator)o;
            }
        }
        throw new Exception("Mutator type not registered: "+type);
    }

    public static void registerMutator(String type, String classname){
        mTypes.put(type, classname);
    }
}
