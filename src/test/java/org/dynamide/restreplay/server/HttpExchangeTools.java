package org.dynamide.restreplay.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

public class HttpExchangeTools {

    public static void writeResponse(HttpExchange t, String response, String mimeType)
    throws IOException {
        writeResponse(t, 200, mimeType, response);
    }

    public static void writeResponse(HttpExchange t, int statusCode, String mimeType, String response)
    throws IOException {
        Headers headers = t.getResponseHeaders();
        List<String> headerVals = new ArrayList<String>();
        headerVals.add(mimeType);
        headers.put("content-type", headerVals);
        t.sendResponseHeaders(statusCode, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public static String readRequestBody(HttpExchange t)
    throws IOException {
        return readStreamToString(t.getRequestBody());
    }

    public static String readStreamToString(InputStream in)
    throws IOException {
        String text = "ERROR in readStreamToString()";
        BufferedReader rd = new BufferedReader(new InputStreamReader(in));
        try {
            StringWriter sw = new StringWriter();
            char[] buffer = new char[1024 * 4];
            int n = 0;
            while (-1 != (n = rd.read(buffer))) {
                sw.write(buffer, 0, n);
            }
            text = sw.toString();
        } finally {
            rd.close();
        }
        return text;
    }

    public static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
        final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
        if (query == null) {
            return query_pairs;
        }
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!query_pairs.containsKey(key)) {
                query_pairs.put(key, new LinkedList<String>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            query_pairs.get(key).add(value);
        }
        return query_pairs;
    }

    public static String extractFirstParam(Map<String, List<String>> paramsMap, String key) {
        List<String> list = paramsMap.get(key);
        if (list == null) {
            return "";
        }
        if (list.size() == 0) {
            return "";
        }
        return list.get(0);
    }

    public static void addHeader(HttpExchange t, String name, String value) {
        Headers headers = t.getResponseHeaders();
        List<String> headerVals = new ArrayList<String>();
        headerVals.add(value);
        headers.put(name, headerVals);
    }

    //=============== debug information ================================================================================

    public static String paramsMapToString(Map<String, List<String>> paramsMap) {
        StringBuffer result = new StringBuffer();
        for (Map.Entry<String, List<String>> entry : paramsMap.entrySet()) {
            result.append(entry.getKey()).append(":");
            int i = 0;
            for (String val : entry.getValue()) {
                result.append("" + i + ":" + val + ", ");
                i++;
            }
        }
        return result.toString();
    }

    public static void dump(HttpExchange t)
    throws IOException {
        URI uri = t.getRequestURI();
        String[] array = t.getRequestHeaders().keySet().toArray(new String[t.getRequestHeaders().keySet().size()]);
        Map<String, List<String>> paramsMap = splitQuery(t.getRequestURI().getRawQuery());
        String response = "Hello, World!"
                + "\nURI: " + uri.toString()
                + "\nURI path: " + uri.getPath().toString()
                + "\nMethod: " + t.getRequestMethod()
                + "\nParams:" + paramsMapToString(paramsMap)
                + "\nBody:" + readRequestBody(t)
                + "\nRawQuery:" + t.getRequestURI().getRawQuery()
                + "\nrequest headers:" + Arrays.toString(array);
        Headers headers = t.getResponseHeaders();
        List<String> hdrList = new ArrayList<String>();
        hdrList.add("mojo");
        hdrList.add("nixon");
        headers.put("x-my-header", hdrList);
        writeResponse(t, response, "text/plain");
    }

}
