/**
 * This document is a part of the source code and related artifacts
 * for CollectionSpace, an open source collections management system
 * for museums and related institutions:
 *
 * http://www.collectionspace.org
 * http://wiki.collectionspace.org
 *
 * Copyright (c) 2009 Regents of the University of California
 *
 * Licensed under the Educational Community License (ECL), Version 2.0.
 * You may not use this file except in compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at
 * https://source.collectionspace.org/collection-space/LICENSE.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.dynamide.restreplay;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;

import java.util.*;

import org.dynamide.restreplay.ServiceResult.Alert.LEVEL;
import org.dynamide.restreplay.RunOptions;
import org.dynamide.util.Tools;

/**
 * User: laramie
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class Eval {
    public Map<String, ServiceResult> serviceResultsMap;
    public JexlEngine jexl = new JexlEngine();   // Used for expression language expansion from uri field.
    public JexlContext jc;
    public static Tools TOOLS = new Tools();
    public RunOptions runOptions;

    public void resetContext(){
        jc = new Eval.MapContextWKeys();//MapContext();
    }

    public static class EvalResult {
        public String result = "";
        public List<ServiceResult.Alert> alerts = new ArrayList<ServiceResult.Alert>();
        public void addAlert(String m, String p, LEVEL l){
            this.alerts.add(new ServiceResult.Alert(m,p,l));
        }
    }

    /**
     * You may pass in a Jexl 2 expression, e.g. ${foo.bar} and it will be eval'd for you.
     * We are looking at some URI like so: ${newOrgAuthority.CSID}
     * The idea here is that the XML control file may bind to this namespace, and
     * this module may find those values and any future extensions, specifically
     * when someone says "I want to bind to ${CSID} and ${SUBRESOURCE.CSID}
     * The code here is easy to extend, but the test cases build up, so you don't
     * want to break all the config files by not being backward compatible.  Binding
     * to context variables like this makes it easy.
     * EXAMPLE USAGE: <br />
     * String uri = "/cspace-services/orgauthorities/${OrgAuth1.CSID}/items/${Org1.CSID}";   <br />
     * uri = eval(uri, serviceResultsMap, jexl, jc);  <br />
     * RESULT:    "/cspace-services/orgauthorities/43a2739c-4f40-49c8-a6d5/items/"
     *
     * @return EvalResult . It is the responsibility of the caller to check EvalResult.alerts which
     *          is a List&lt;Alert>.  The list will have size()==0 if there are not alerts.  If there
     *          are alerts, It is up to the caller to decide whether to continue processing.
     *          This method continues processing each eval, which may include multiple WARN or ERROR
     *          alerts, so the caller should use Collection.addAll(EvalResult.alerts) or similar to grab all Alerts.
     */
    public EvalResult eval(String context,
                                  String inputJexlExpression,
                                  Map<String,String> vars) {
        EvalResult result = new EvalResult();
        Map<String, ServiceResult> serviceResultsMap = this.serviceResultsMap;

        try {
            jc.set("itemCSID", "${itemCSID}"); //noiseless passthru.
            for (Map.Entry<String,ServiceResult> entry: serviceResultsMap.entrySet()) {
                jc.set(entry.getKey(), entry.getValue());
            }
            if (vars!=null){
                for (Map.Entry<String,String> entry: vars.entrySet()) {
                    String value = entry.getValue();
                    String key = entry.getKey();
                    try {
                        EvalResult innerResult = parse(context+", key:"+key, value);
                        value = innerResult.result;
                        if (innerResult.alerts.size()>0){
                            result.alerts.addAll(innerResult.alerts);
                        }
                        vars.put(key, value); //replace template value with actual value.
                    } catch (Exception e){
                        value = "ERROR_IN_EVAL: "+e;
                        String ctx = context + " key: "+key+" value:"+value;
                        result.addAlert(value, ctx, LEVEL.WARN);
                    }
                    jc.set(key, value);
                }
            }
            jc.set("tools", TOOLS);
            EvalResult innerResult2 = parse(context, inputJexlExpression);
            if (innerResult2.alerts.size()>0){
                result.alerts.addAll(innerResult2.alerts);
            }
            result.result = innerResult2.result;
        } catch (Throwable t) {
            String errmsg = "ERROR: could not eval jexl expression. " + t;
            System.err.println(errmsg+" Expression: "+inputJexlExpression);
            result.addAlert(errmsg, inputJexlExpression, ServiceResult.Alert.LEVEL.ERROR);
            result.result = "";
        }
        return result;
    }

    private EvalResult parse(String context,
                                    String in) {
        EvalResult evalResult = new EvalResult();
        StringBuffer result = new StringBuffer();
        String s = in;
        String var = "";
        int start, end, len;
        len = in.length();
        start = 0;
        int cursor = 0;
        String front = "";
        while (start < len) {
            start = in.indexOf("${", start);
            end = in.indexOf("}", start);
            if (start < 0) {
                String tail = in.substring(cursor);
                result.append(tail);
                break;
            }
            if (end < 0) {
                evalResult.addAlert("Unbalanced ${} braces",
                        "context:"+ context + ", expr:" + in,
                        LEVEL.ERROR);
                if (runOptions.errorsBecomeEmptyStrings){
                    evalResult.result = "";
                } else {
                    evalResult.result = in;
                }
                return evalResult;
            }
            front = in.substring(cursor, start);
            result.append(front);
            cursor = end + 1;                   //bump past close brace
            var = in.substring(start + 2, end);  //+2 bump past open brace ${ and then "end" is indexed just before the close brace }
            //s   = s.substring(end+1);         //bump past close brace
            start = cursor;

            Expression expr = jexl.createExpression(var);
            Object resultObj = null;
            try {
                resultObj = expr.evaluate(jc); //REM - 5/9/2011 - Usually calls back to fields and methods in ServiceResult class to do the evaluation -e.g., the "got" method.
            } catch (Throwable ex){
                System.out.println("\r\n\r\n\r\n~~~~~~~~~~~~~~~~~~ error ");
                evalResult.addAlert("Exception while evaluating variable: '"+var+"'.",// Exception: "+ex,
                        dumpContext(jc),
                        LEVEL.ERROR);
                resultObj = null;
            }
            String resultStr;
            if (null == resultObj){
                evalResult.addAlert("null found while evaluating variable: '"+var+"'",
                        dumpContext(jc),
                        LEVEL.WARN);
                if (runOptions.errorsBecomeEmptyStrings){
                    resultStr = "";
                } else {
                    resultStr = "${"+var+"}";
                }
            } else {
                resultStr = resultObj.toString();
            }
            result.append(resultStr);
        }
        evalResult.result = result.toString();
        return evalResult;
    }

    protected static String dumpContext(JexlContext jc){
        String result = "";
        if (jc instanceof MapContextWKeys){
            Set keys = ((MapContextWKeys)jc).getKeys();
            result = "keys:"+keys.toString();
        }  else {
            result = "jc:"+jc.toString();
        }
        return result;
    }

    public static class MapContextWKeys extends MapContext implements JexlContext {
        private Map<String,Object> map = new HashMap();
        public Set getKeys(){
            return this.map.keySet();
        }
    }


}
