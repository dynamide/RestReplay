package org.dynamide.interpreters;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;

import org.dynamide.util.Tools;

public class RhinoInterpreter {

    private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    private ScriptEngine m_Interpreter;

    private Map<String, Object> m_variables = new LinkedHashMap<String, Object>();

    public void setVariable(String name, Object value) {
        m_variables.put(name, value);
    }

    public void unsetVariable(String name) {
        Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.remove(name);
        m_variables.remove(name);
    }

    public EvalResult eval(String resourceName, String source) {
        EvalResult result = new EvalResult();
        result.context = resourceName;
        result.expression = source;
        //System.out.println("======================================^^^+++++++++++++ resourceName : "+resourceName+" source: "+source);
        try{
            getInterpreter();
            synchronized (m_Interpreter){
                Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);
                //Now add all the variables in m_variables (added by setVariable) into the bindings for the eval call.
                Object obj;
                Iterator<String> en = m_variables.keySet().iterator();
                for (String key: m_variables.keySet()) {
                    obj = m_variables.get(key);
                    bindings.put(key, obj);
                }

                ScriptContext ctx = m_Interpreter.getContext();
                ctx.setAttribute(ScriptEngine.FILENAME, resourceName, ScriptContext.ENGINE_SCOPE);
                bindings.put("__FILE__", resourceName);
                try {
                     result.result = m_Interpreter.eval(source);  //pull in functions, vars.
                } finally {
                    for (String key: m_variables.keySet()) {
                        bindings.remove(key);
                    }
                }
            }
        } catch (Exception e2){
            System.out.println("ERROR in eval(): " + e2.toString() + Tools.getStackTrace(e2));
            result.addAlert("ERROR in eval(): " + e2.toString(), Tools.getStackTrace(e2), Alert.LEVEL.ERROR);
        }
        return result;
    }

    private ScriptEngine getInterpreter() throws Exception {
        if (m_Interpreter == null){
            m_Interpreter = scriptEngineManager.getEngineByName("javascript");
            Bindings bindings = m_Interpreter.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("stdout", System.out);
        }
        return m_Interpreter;
    }

    public static void main(String[]args) throws Exception {
        RhinoInterpreter interp = new RhinoInterpreter();
        interp.eval("stdout.println('hello');", "main()");
    }
}
