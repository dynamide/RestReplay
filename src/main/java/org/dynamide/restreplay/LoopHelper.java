package org.dynamide.restreplay;

import org.dom4j.Node;
import org.dynamide.interpreters.Alert;
import org.dynamide.interpreters.Eval;
import org.dynamide.interpreters.EvalResult;
import org.dynamide.util.Tools;

import java.util.*;

/**
* Abstracts all the handling of looping ( <b>loop=</b> attribute, and <b>&lt;loop /></b> nodes),
* and centralizes the setting of global vars related to looping.
*/
public class LoopHelper {
    public boolean error = false;
    public boolean doingIterations = false;
    public String iterations = "";//informational only.
    public int numIterations = -1;
    public Iterator<Map.Entry<String,Object>> mapIterator = null;
    public Iterator collectionIterator = null;
    public String[] loopArray = null;
    public Object[] loopObjectArray = null;
    public Object loopObject = null;
    public String toString(){
        return "{ loops:"+numIterations+", expression: \""+iterations+"\"}";
    }
    public void setGlobalVariablesForLooping(ServiceResult serviceResult, Eval evalStruct, int itnum){
        evalStruct.jc.set("loop.key", "");
        evalStruct.jc.set("loop.value", "");
        evalStruct.jc.set("loop.index", itnum);
        evalStruct.jc.set("loop.object", null);
        if ( ! doingIterations){
            return;
        }
        if (this.mapIterator !=null){
            Map.Entry entry = this.mapIterator.next();
            evalStruct.jc.set("loop.key", entry.getKey());
            evalStruct.jc.set("loop.value", entry.getValue());
            evalStruct.jc.set("loop.object", loopObject);
        }
        if (this.collectionIterator !=null){
            Object value = this.collectionIterator.next();
            evalStruct.jc.set("loop.value", value);
            evalStruct.jc.set("loop.object", loopObject);
        }
        if (this.loopArray != null){
            evalStruct.jc.set("loop.value", this.loopArray[itnum]);
            evalStruct.jc.set("loop.object", loopObject);
        }
        if (this.loopObjectArray != null){
            evalStruct.jc.set("loop.value", this.loopObjectArray[itnum]);
            evalStruct.jc.set("loop.object", loopObject);
        }
        if (this.doingIterations) {
            serviceResult.loopIndex = itnum;
            evalStruct.jc.set("loop.index", itnum);
        }
    }

    public static LoopHelper getIterationsLoop (int testElementIndex,
                                          String testGroupID,
                                          Node testNode,
                                          Eval evalStruct,
                                          Map<String, Object> clonedMasterVars,
                                          RunOptions runOptions,
                                          RestReplayReport report,
                                          List<ServiceResult> results){
        LoopHelper loopHelper = new LoopHelper();

        loopHelper.iterations = testNode.valueOf("@loop");  //try as an attribute
        if (Tools.isBlank(loopHelper.iterations)){
            loopHelper.iterations = testNode.valueOf("loop"); //try as an element (supports multi-line expressions).
        }
        loopHelper.doingIterations = false;
        loopHelper.numIterations = 1;
        Map<String,Object> loopMap = null;
        Collection loopCollection = null;

        if (Tools.notBlank(loopHelper.iterations)){
            loopHelper.doingIterations = true;
            EvalResult evalResult = null;
            try {
                evalResult = evalStruct.eval("calculate @loop", loopHelper.iterations, clonedMasterVars);
                if (   evalResult.worstLevel.equals(Alert.LEVEL.WARN)
                        || evalResult.worstLevel.equals(Alert.LEVEL.ERROR)){
                    throw new Exception(" expression: "+loopHelper.iterations);
                }
                Object resultResult = evalResult.result;
                //serviceResult.alerts.addAll(evalResult.alerts);
                if (resultResult instanceof String[] ){
                    loopHelper.numIterations = ((String[])resultResult).length;
                    evalStruct.jc.set("loop", resultResult);
                    loopHelper.loopArray = (String[])resultResult;
                } else if (resultResult !=null && resultResult.getClass().isArray()) {
                    loopHelper.loopObjectArray = (Object[])resultResult;
                    loopHelper.numIterations = ((Object[])resultResult).length;
                    evalStruct.jc.set("loop", resultResult);
                } else if (resultResult instanceof Map) {
                    loopMap = (Map)resultResult;
                    Set set = loopMap.entrySet();
                    loopHelper.mapIterator = set.iterator();
                    loopHelper.numIterations = loopMap.size();
                    evalStruct.jc.set("loop", resultResult);
                } else if (resultResult instanceof Collection) {
                    loopCollection = (Collection)resultResult;
                    loopHelper.collectionIterator = loopCollection.iterator();
                    loopHelper.numIterations = loopCollection.size();
                    evalStruct.jc.set("loop", resultResult);
                } else {
                    loopHelper.iterations = evalResult.getResultString();
                    loopHelper.numIterations = Integer.parseInt(loopHelper.iterations);
                }
                loopHelper.loopObject = evalStruct.jc.get("loop");
            } catch (Throwable t){
                System.out.println("\n======NOT doing iterations because loop expression failed:"+loopHelper.iterations+"\n");
                ServiceResult serviceResult = new ServiceResult(runOptions);
                if (evalResult!=null)evalResult.alerts.addAll(evalResult.alerts);
                serviceResult.testID = testNode.valueOf("@ID");
                serviceResult.testIDLabel = Tools.notEmpty(serviceResult.testID)
                        ? (testGroupID + '.' + serviceResult.testID)
                        : (testGroupID + '.' + testElementIndex);
                serviceResult.testGroupID = testGroupID;
                String msg = "ERROR calculating loop";
                serviceResult.addError(msg, t);
                serviceResult.failureReason = msg+t.getMessage();
                List<Node> failures = testNode.selectNodes("response/expected/failure"); //TODO: get in sync with expected/failure handling elsewhere.
                if (failures.size()>0){
                    serviceResult.expectedFailure = true;
                }
                report.addTestResult(serviceResult);
                results.add(serviceResult);
                loopHelper.error = true;
            }
        }
        return loopHelper;
    }
}
