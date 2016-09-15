package org.dynamide.interpreters;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;

import org.dynamide.util.Tools;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class RhinoInterpreter {
    public RhinoInterpreter(boolean dumpJavascriptEvals){
        this.dumpJavascriptEvals = dumpJavascriptEvals;
    }

    private boolean dumpJavascriptEvals = false;

    private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    private Context m_Interpreter;
    private ScriptableObject m_globalScope;

    private Map<String, Object> m_variables = new LinkedHashMap<String, Object>();

    public void setVariable(String name, Object value) {
        m_variables.put(name, value);
    }

    public void unsetVariable(String name) {
        m_globalScope.delete(name);
        m_variables.remove(name);
    }

    public EvalResult eval(String resourceName, String source) {
        EvalResult result = new EvalResult();
        result.context = resourceName;
        result.expression = source;
        if (dumpJavascriptEvals){System.out.println("=======javascript eval======= \r\nresourceName : "+resourceName+" source: \r\n"+source+"\r\n==================");}
        try{
            getInterpreter();
            Context.enter();
            synchronized (m_Interpreter){
                //Now add all the variables in m_variables (added by setVariable) into the bindings for the eval call.
                Object obj;
                Iterator<String> en = m_variables.keySet().iterator();
                for (String key: m_variables.keySet()) {
                    obj = m_variables.get(key);
                    m_globalScope.put(key, m_globalScope, obj);
                }

                //special Rhino in Java7 trick: ctx.setAttribute(ScriptEngine.FILENAME, resourceName, ScriptContext.ENGINE_SCOPE);

                m_globalScope.put("__FILE__", m_globalScope, resourceName);
                try {
                    Object evalResult = m_Interpreter.evaluateString(m_globalScope,
                                                 source,
                                                 resourceName,
                                                 0, //startline
                                                 null);
                    if (evalResult != Context.getUndefinedValue()) {
                        result.result = (Context.toString(evalResult));
                    }
                } finally {
                    for (String key: m_variables.keySet()) {
                        m_globalScope.delete(key);
                    }
                    Context.exit();
                }
            }
        } catch (Exception e2){
            System.out.println("ERROR in eval(): " + e2.toString() + Tools.getStackTrace(e2));
            result.addAlert("ERROR in eval(): " + e2.toString(), Tools.getStackTrace(e2), Alert.LEVEL.ERROR);
        }
        return result;
    }

    private Context getInterpreter() throws Exception {
        if (m_Interpreter == null){
            if (dumpJavascriptEvals){System.out.println("=== RestReplay using Rhino Javascript interpreter. ===");}
            Context cx = Context.enter();  //binds Context object to this Thread.
            m_globalScope = cx.initStandardObjects();
            m_globalScope.put("stdout", m_globalScope, System.out);
            m_Interpreter = cx;
            Context.exit();
        }
        return m_Interpreter;
    }

    /* This was the hacked way to get Java 8 to accept Rhino as a ScriptEngine, but it's a bit flakey.
    private ScriptEngine getInterpreterRhinoHack() throws Exception {

        if (m_Interpreter == null){
            m_Interpreter = scriptEngineManager.getEngineByName("rhino");   // specifically look for "rhino" engine, not nashorn.
            if (m_Interpreter==null) {
                System.out.println("could not load rhino engine "+Tools.getStackTrace(new Exception("null m_Interpreter")));
                return null;
            }
            Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("stdout", System.out);
        }
        return m_Interpreter;
    }
    */

    public static void main(String[]args) throws Exception {
        RhinoInterpreter interp = new RhinoInterpreter(true);
        Object out;
        if (args.length>0){
            // for example, run with these as program args, with the quotes:
            //     "var res={}; res.name='mojo'; JSON.stringify(res);"
            out = interp.eval("args", args[0]);
        } else {
            out = interp.eval("main()", "stdout.println('hello');");
        }
        System.out.println("out: "+out);
    }
}
