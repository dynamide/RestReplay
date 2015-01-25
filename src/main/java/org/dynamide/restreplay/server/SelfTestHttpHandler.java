package org.dynamide.restreplay.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.dynamide.util.Tools;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

// Statically import all tools methods:
//     addHeader, writeReponse, dump, splitQuery, readRequestBody, extractFirstParam
import static org.dynamide.restreplay.server.HttpExchangeTools.*;

public class SelfTestHttpHandler implements HttpHandler {

    public SelfTestHttpHandler(EmbeddedServer s){
        this.s = s;
    }

    private final EmbeddedServer s;

    public final static String JSON = "application/json";

    private final static String JSON_COMPARE_RIGHT = "{\"a\": \"one\",\"b\": {\"c\": \"two\"}}";

    public void handle(HttpExchange t) throws IOException {
        //System.out.println("self-test web server received URI: "+t.getRequestURI());
        URI uri = t.getRequestURI();
        Map<String, List<String>> paramsMap = splitQuery(t.getRequestURI().getRawQuery());
        String path = uri.getPath();
        String method = t.getRequestMethod();
        String body;

        //System.out.println("****************** handler path: "+path);
        String mock = extractFirstParam(paramsMap, "mock");

        if (path.equals("/dump")) {
            dump(t);
        } else if (path.equals("/tagonomy")) {
            if (mock.equals("token")) {
                body = "{\"status\":\"success\",\"data\":\"1.0|idm|idm|piid=ffffffff540e662de4b01ef1c4c50faf&sessid=401d6f4ee6bb4795af0861ebaa1f6c58|2014-11-05T01:01:16+00:00|2014-11-05T04:01:16+00:00|cbc0b7e17f07a90f70a88a0eab57d9ce\"}";
                addHeader(t, "content-type", JSON);
                writeResponse(t, JSON, body);
            } else {
                if (method.equals("GET")) {
                    addHeader(t, "Location", "http://localhost:18080/tagonomy?mock=true");
                    writeResponse(t, JSON, "{\"result\":\"OK\", \"method\":\"" + method + "\", \"nested\":{\"courseId\":\"course1234\"}}");
                } else if (method.equals("DELETE")) {
                    addHeader(t, "Location", "http://localhost:18080/tagonomy?mock=true");
                    addHeader(t, "x-foobar", "http://Foo.bar/in-application.xml");
                    writeResponse(t, JSON, "{\"result\":\"OK\", \"method\":\"" + method + "\"}");
                } else if (method.equals("POST")) {
                    body = readRequestBody(t);
                    writeResponse(t, JSON, "{\"result\":\"OK\", \"method\":\"" + method + "\", \n\"req\": " + body + "}");
                } else if (method.equals("PUT")) {
                    body = readRequestBody(t);
                    if (mock.equals("501")) {
                        writeResponse(t, 501, JSON, "{\"result\":\"ERROR\", \"method\":\"" + method + "\", \n\"req\": " + body + "}}}");
                    }
                    String mutation = extractFirstParam(paramsMap, "mutation");
                    if (mutation != null && mutation.length() > 0) {
                        if (mutation.equals("no_optionalField")) {
                            writeResponse(t, 202, JSON, "{\"result\":\"ERROR\", \"method\":\"" + method + "\",\n \"mutation\":\"" + mutation + "\", \n\"req\": " + body + "}}}");
                        } else {
                            writeResponse(t, 406, JSON, "{\"result\":\"ERROR\", \"method\":\"" + method + "\",\n \"mutation\":\"" + mutation + "\", \n\"req\": " + body + "}}}");
                        }
                    } else {
                        int code = 200;
                        String forceCode = extractFirstParam(paramsMap, "emptyMutationResponseCode");
                        if (Tools.notBlank(forceCode)) {
                            code = Integer.parseInt(forceCode);
                        }

                        addHeader(t, "Location", "http://localhost:28080/tagonomy?mock=true");
                        writeResponse(t, code, JSON, "{\"result\":\"OK\", \"method\":\"" + method + "\", \n\"req\": " + body + "}");
                    }
                } else {
                    writeResponse(t, JSON, "{\"result\":\"NO HANDLER for method " + method + "\"}");
                }
            }
        } else if (path.equals("/jsonCompare1")){
            String outputType = extractFirstParam(paramsMap, "mimeOut");
            if (Tools.isBlank(outputType)){
                outputType = JSON;
            }
            body = readRequestBody(t);
            writeResponse(t, 200, outputType, body);
        } else {
            writeResponse(t, JSON, "{\"result\":\"NO HANDLER for path "+path+"\"}");
        }
    }
}

