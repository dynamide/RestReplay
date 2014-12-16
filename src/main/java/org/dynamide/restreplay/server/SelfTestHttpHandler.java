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

    public void handle(HttpExchange t) throws IOException {
        URI uri = t.getRequestURI();
        Map<String, List<String>> paramsMap = splitQuery(t.getRequestURI().getRawQuery());
        String path = uri.getPath();
        String method = t.getRequestMethod();
        String body;

        String mock = extractFirstParam(paramsMap, "mock");

        if (path.equals("/dump")) {
            dump(t);
        } else if (path.equals("/tagonomy")) {
            if (mock.equals("token")) {
                body = "{\"status\":\"success\",\"data\":\"1.0|idm|idm|piid=ffffffff540e662de4b01ef1c4c50faf&sessid=401d6f4ee6bb4795af0861ebaa1f6c58|2014-11-05T01:01:16+00:00|2014-11-05T04:01:16+00:00|cbc0b7e17f07a90f70a88a0eab57d9ce\"}";
                addHeader(t, "content-type", JSON);
                writeResponse(t, body, JSON);
            } else {
                if (method.equals("GET")) {
                    addHeader(t, "Location", "http://localhost:18080/tagonomy?mock=true");
                    writeResponse(t, "{\"result\":\"OK\", \"method\":\"" + method + "\", \"nested\":{\"courseId\":\"course1234\"}}", JSON);
                } else if (method.equals("DELETE")) {
                    addHeader(t, "Location", "http://localhost:18080/tagonomy?mock=true");
                    addHeader(t, "x-foobar", "http://Foo.bar/in-application.xml");
                    writeResponse(t, "{\"result\":\"OK\", \"method\":\"" + method + "\"}", JSON);
                } else if (method.equals("POST")) {
                    body = readRequestBody(t);
                    writeResponse(t, "{\"result\":\"OK\", \"method\":\"" + method + "\", \n\"req\": " + body + "}", JSON);
                } else if (method.equals("PUT")) {
                    body = readRequestBody(t);
                    if (mock.equals("501")) {
                        writeResponse(t, 501, JSON, "{\"result\":\"ERROR\", \"method\":\"" + method + "\", \n\"req\": " + body + "}}}");
                    }
                    String mutation = extractFirstParam(paramsMap, "mutation");
                    if (mutation != null && mutation.length() > 0) {
                        int code = 400;
                        String forceCode = extractFirstParam(paramsMap, "forceCode");
                        if (Tools.notBlank(forceCode)){
                            code = Integer.parseInt(forceCode);
                        }
                        writeResponse(t, code, JSON, "{\"result\":\"ERROR\", \"method\":\"" + method + "\", \n\"req\": " + body + "}}}");
                    } else {
                        addHeader(t, "Location", "http://localhost:28080/tagonomy?mock=true");
                        writeResponse(t, "{\"result\":\"OK\", \"method\":\"" + method + "\", \n\"req\": " + body + "}", JSON);
                    }
                } else {
                    writeResponse(t, "{\"result\":\"NO HANDLER for method "+method+"\"}", JSON);
                }
            }
        } else {
            writeResponse(t, "{\"result\":\"NO HANDLER for path "+path+"\"}", JSON);
        }
    }
}

