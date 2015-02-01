package org.dynamide.restreplay;

import org.apache.commons.jexl2.*;

import java.util.*;

import org.dynamide.interpreters.EvalResult;
import org.dynamide.interpreters.Alert.LEVEL;
import org.dynamide.util.Tools;

public class Eval {
    public Map<String, ServiceResult> serviceResultsMap;
    public JexlEngine jexl = new JexlEngine();   // Used for expression language expansion from uri field.
    public JexlContext jc;
    public static Tools TOOLS = new Tools();
    public static Kit KIT = new Kit();
    public RunOptions runOptions;
    private List<EvalResult> evalReport = new ArrayList<EvalResult>();
    public List<EvalResult> getEvalReport() {
        return evalReport;
    }
    protected void addToEvalReport(EvalResult result){
        evalReport.add(result);
    }
    private String currentTestIDLabel = "";
    public void setCurrentTestIDLabel(String val){
        currentTestIDLabel =  val;

        EvalResult label = new EvalResult();
        label.isDummy = true;
        label.testIDLabel = val;
        addToEvalReport(label);
    }

    public void resetContext(){
        jc = new Eval.MapContextWKeys();//MapContext();
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
    public EvalResult eval(String logname,
                                  String inputJexlExpression,
                                  Map<String,Object> vars) {
        EvalResult result = new EvalResult();
        result.context = logname;
        result.expression = inputJexlExpression;
        try {
            jc.set("itemCSID", "${itemCSID}"); //noiseless passthru.
            jc.set("tools", TOOLS);
            jc.set("kit", KIT);
            if (serviceResultsMap!=null) {
                for (Map.Entry <String,ServiceResult> entry:serviceResultsMap.entrySet()){
                    jc.set(entry.getKey(), entry.getValue());
                }
            }
            if (vars!=null){
                for (Map.Entry<String,Object> entry: vars.entrySet()) {
                    //loop over vars.  Don't overwrite, but add all the vars so that vars can themselves reference vars.
                    Object value = entry.getValue();
                    String key = entry.getKey();
                    if (jc.get(key)==null){
                        jc.set(key, value);
                    }
                }
                for (Map.Entry<String,Object> entry: vars.entrySet()) {
                    Object value = entry.getValue();
                    String key = entry.getKey();
                    try {
                        EvalResult innerResult = parse(logname+", ID: <b>"+key+"</b>", ""+value);
                        innerResult.nestingLevel = 1;
                        if (innerResult.useResultAsObject) {
                            value = innerResult.result;
                        } else {
                            value = innerResult.getResultString();
                        }
                        if (innerResult.alerts.size()>0){
                            result.alerts.addAll(innerResult.alerts);
                        }
                        vars.put(key, value); //replace template value with actual value.
                        addToEvalReport(innerResult);
                    } catch (Exception e){
                        value = "ERROR_IN_EVAL: "+e;
                        String ctx = logname + " ID: <b>"+key+"</b> value:"+value;
                        result.addAlert(""+value, ctx, LEVEL.WARN);
                    }
                    jc.set(key, value);
                }
            }
            EvalResult innerResult2 = parse(logname, inputJexlExpression);
            if (innerResult2.alerts.size()>0){
                result.alerts.addAll(innerResult2.alerts);
            }
            result.result = innerResult2.result;
            addToEvalReport(result);
        } catch (Throwable t) {
            String errmsg = "ERROR: could not eval jexl expression. " + t;
            System.err.println(errmsg+" Expression: "+inputJexlExpression+"\n"+Tools.getStackTrace(t, 10));
            result.addAlert(errmsg, inputJexlExpression, LEVEL.ERROR);
            result.result = "";
            addToEvalReport(result);
        }
        return result;
    }

    private EvalResult parse(String context,
                                    String in) {
        EvalResult evalResult = new EvalResult();
        evalResult.context = context;
        evalResult.expression = in;
        StringBuffer result = new StringBuffer();
        String s = in;
        String var = "";
        int start, end, len;
        len = in.length();
        start = 0;
        int cursor = 0;
        String front = "";
        Object singleResultObj = null;
        boolean doingStringConcat = false;

        while (start < len) {
            start = in.indexOf("${", start);  //+1 for the $ sign.
            if (start < 0) {
                String tail = in.substring(cursor);
                result.append(tail);
                break;
            }
            end = findMatchingCloseBrace(in, start+2);
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
            if (Tools.notBlank(front)){
                doingStringConcat = true;
            }
            cursor = end;                   //bump past close brace
            var = in.substring(start + 2, end-1);  //+2 bump past open brace ${ and then "end" is indexed just before the close brace }
            start = cursor;

            //works: Expression expr = jexl.createExpression(var);
            //experimental:
            Script script = jexl.createScript(var);   //http://commons.apache.org/proper/commons-jexl/apidocs/org/apache/commons/jexl2/Script.html

            Object resultObj = null;
            try {
                //jc.set("out", System.out);
                //works: resultObj = expr.evaluate(jc); but replacing with script and not just expression:
                resultObj = script.execute(jc);
            } catch (Throwable ex){
                System.out.println("\r\n\r\n\r\n~~~~~~~~~~~~~~~~~~ error "+ex);
                evalResult.addAlert("Exception while evaluating variable: '"+var+"'.",// Exception: "+ex,
                        dumpContext(jc),
                        LEVEL.ERROR);
                resultObj = null;
            }
            String resultStr;
            if (null == resultObj){
                evalResult.addAlert("Variable not found: '"+var+"'",
                        dumpContext(jc),
                        LEVEL.WARN);
                if (runOptions.errorsBecomeEmptyStrings){
                    resultStr = "";
                } else {
                    resultStr = "${"+var+"}";
                }
            } else {
                resultStr = resultObj.toString();
                if (resultObj.getClass().getName().equals("java.lang.String")) {

                } else {
                    singleResultObj = resultObj;
                    if (resultObj instanceof String[]){
                        resultStr = Arrays.toString((String[])resultObj);
                    }
                }
            }
            result.append(resultStr);
        }
        if (singleResultObj!=null){
            if (doingStringConcat){
                evalResult.result = result.toString();
            } else {
                evalResult.result = singleResultObj;
                evalResult.useResultAsObject = true;
            }
        } else {
            evalResult.result = result.toString();
        }
        return evalResult;
    }

    /** @return the string up to (but not including) the matching close brace. */
    protected static int findMatchingCloseBrace(String in, int start){
        int len = in.length();
        if (len<=0){
            return -1;
        }
        int i = start;
        int braces = 0;
        char c;
        while (i < len){
            c = in.charAt(i);
            i++;
            switch (c){
                case '\\':
                    i++;  //once more to bump past escape char and escaped char.
                    break;
                case '{':
                    braces++;
                    break;
                case '}':
                    if (braces>0) {
                        braces--;
                        break;
                    }
                    //System.out.println("\nin: =="+in+"==,              out: =="+in.substring(start, i-1)+"==");
                    return i;
                default:
                    break;
            }
        }
        return -1;
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
        private Map<String,Object> map = new LinkedHashMap();
        public Set getKeys(){
            return this.map.keySet();
        }
    }

    private static void test(String in, int start){
        System.out.println("test("+in+','+start+"): "+findMatchingCloseBrace(in, start));
    }

    private static EvalResult testEval(String expression, Map<String,Object> vars) {
        Eval ev = new Eval();
        ev.resetContext();
        return ev.eval("test",
                expression,
                vars);
    }


    public static class MyVar {
        public String id;
        public Date startdate;
    }

    public static void main(String[] args){
        test("abc", 0);
        test("abc", 1);
        test("abc", 2);
        test("abc", 3);

        test("{abc", 0);
        test("{abc", 1);

        test("abc}", 0);
        test("{abc}", 0);
        test("{abc}", 1);
        test("{abc{def}}", 1);
        test("{abc}{ghi}", 1);
        test("{{abc}{ghi}}", 1);
        test("{a\\{bc}", 1);
        test("{a\\{bc}}", 1);

        Map<String,Object> vars = new HashMap<String, Object>();
        MyVar myvar = new MyVar();
            myvar.id = "testmyvar1";
            myvar.startdate = new Date();
        vars.put("myvar", myvar);
        EvalResult result = testEval("${3+2} date: ${kit.now()} myvar: ${myvar}", vars);
        System.out.println("evalresult: "+result);
    }


}
