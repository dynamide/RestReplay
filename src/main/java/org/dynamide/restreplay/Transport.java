package org.dynamide.restreplay;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dynamide.util.Tools;

/**
 *   @author Laramie Crocker
 */
public class Transport {

    public static String BOUNDARY = "34d97c83-0d61-4958-80ab-6bf8d362290f";

    private static String DD = "--";
    private static String CRLF = "\r\n";

    private static void setTimeouts(HttpClient client, ServiceResult sr){
        //client.getParams().setParameter("http.socket.timeout", milliseconds);
        //client.getParams().setParameter("http.connection.timeout", milliseconds);
        //client.getParams().setParameter("http.connection-manager.timeout", milliseconds);
        //client.getParams().setParameter("http.protocol.head-body-timeout", milliseconds);
        HttpClientParams params = client.getParams();
        params.setSoTimeout(sr.socketTimeout);
        params.setConnectionManagerTimeout(sr.connectionTimeout);
        //System.out.println("setting timeouts: "+sr.socketTimeout+','+sr.connectionTimeout);
        client.setParams(params);
    }

    private static final int TIMEOUT = 1;

    public static ServiceResult doGET(ServiceResult result, String urlString, String authForTest, String fromTestID,
                                      Map<String, String> headerMap) throws Exception {
        result.fromTestID = fromTestID;
        result.headerMap = headerMap;
        result.method = "GET";
        //HACK for speed testing.
        //pr.CSID = "2";
        //pr.overrideGotExpectedResult();
        //if (true) return pr;
        //END-HACK
        HttpClient client = new HttpClient();
        setTimeouts(client, result);
        GetMethod getMethod = new GetMethod(urlString);
        getMethod.addRequestHeader("Accept", "multipart/mixed");
        getMethod.addRequestHeader("Accept", "application/xml");
               result.addRequestHeader("Accept", "application/xml");
        getMethod.addRequestHeader("Accept", "application/json");
               result.addRequestHeader("Accept", "application/json");
        getMethod.setRequestHeader("Authorization", "Basic " + authForTest); //"dGVzdDp0ZXN0");
        getMethod.setRequestHeader("X-RestReplay-fromTestID", fromTestID);
        for (Map.Entry<String,String> entry: headerMap.entrySet()){
            getMethod.setRequestHeader(entry.getKey(), entry.getValue());
        }
        try {
            int statusCode1 = client.executeMethod(getMethod);
            result.responseCode = statusCode1;
            String resBody = readStreamToString(getMethod, result);
            result.setResultWMime(resBody, getResponseContentType(getMethod));
            result.responseMessage = getMethod.getStatusText() + "::" + getMethod.getStatusLine();
            Header[] headers = getMethod.getResponseHeaders();
            dumpResponseHeaders(headers, result);
            result.responseHeaders = Arrays.copyOf(headers, headers.length);
            Header hdr = getMethod.getResponseHeader("CONTENT-TYPE");
            if (hdr != null) {
                String hdrStr = hdr.toExternalForm();
                result.boundary = PayloadLogger.parseBoundary(hdrStr);
            }

            result.contentLength = getMethod.getResponseContentLength();
            extractLocation(getMethod, urlString, result);
            //result.status = getMethod.getStatusText();
            getMethod.releaseConnection();
        } catch (Throwable t){
            //System.err.println("ERROR getting content from response: "+t);
            result.addError("Error in doGET",t);
            System.out.println(t.getMessage()+"   :: stack trace \r\n" + getStackTrace(t));
        }
        return result;
    }

    public static ServiceResult doDELETE(ServiceResult result,
                                         String urlString, String authForTest, String testID, String fromTestID,
                                         Map<String, String> headerMap)  {
        result.failureReason = "";
        result.method = "DELETE";
        result.fullURL = urlString;
        result.fromTestID = fromTestID;
        result.headerMap = headerMap;
        if (Tools.isEmpty(urlString)){
            result.addError("url was empty.  Check the result for fromTestID: "+fromTestID+". currentTest: "+testID);
            return result;
        }
        HttpClient client = new HttpClient();
        setTimeouts(client, result);
        DeleteMethod deleteMethod = new DeleteMethod(urlString);
        deleteMethod.setRequestHeader("Accept", "multipart/mixed");
        deleteMethod.addRequestHeader("Accept", "application/xml");
                  result.addRequestHeader("Accept", "application/xml");
        deleteMethod.addRequestHeader("Accept", "application/json");
                  result.addRequestHeader("Accept", "application/json");

        deleteMethod.setRequestHeader("Authorization", "Basic " + authForTest);
        deleteMethod.setRequestHeader("X-RestReplay-fromTestID", fromTestID);
        for (Map.Entry<String,String> entry: headerMap.entrySet()){
            deleteMethod.setRequestHeader(entry.getKey(), entry.getValue());
        }
        int statusCode1 = 0;
        String res = "";
        try {
            statusCode1 = client.executeMethod(deleteMethod);
            result.responseCode = statusCode1;
            //System.out.println("statusCode: "+statusCode1+" statusLine ==>" + deleteMethod.getStatusLine());
            result.responseMessage = deleteMethod.getStatusText();
            res = deleteMethod.getResponseBodyAsString();
            deleteMethod.releaseConnection();
        } catch (Throwable t){
            result.addError("Error in doDELETE", t);
            System.out.println(t.getMessage()+"   :: stack trace \r\n" + getStackTrace(t));
        }
        result.setResultWMime(res, getResponseContentType(deleteMethod));
        result.responseCode = statusCode1;
        Header[] headers = deleteMethod.getResponseHeaders();
        dumpResponseHeaders(headers, result);
        extractLocation(deleteMethod, urlString, result);
        return result;
    }

    public static ServiceResult doLIST(ServiceResult result,
                                       String urlString, String listQueryParams, String authForTest, String fromTestID,
                                       Map<String, String> headerMap) throws Exception {
        //String u = Tools.glue(urlString, "/", "items/");
        if (Tools.notEmpty(listQueryParams)){
            urlString = Tools.glue(urlString, "?", listQueryParams);
        }
        return doGET(result, urlString, authForTest, fromTestID, headerMap);
    }

    public static final String MULTIPART_MIXED = "multipart/mixed";
    public static final String APPLICATION_XML = "application/xml";

    /** This will replace doPOST_PUT_HttpURLConnection, with Apache HttpClient 3)
     * It does not deal with boundary or multipart yet.
     */
    public static ServiceResult doPOST_PUT(ServiceResult result,
                                           String urlString,
                                           String content,
                                           String contentRaw,
                                           String boundary,
                                           String method,
                                           String contentType,
                                           String authForTest,
                                           String fromTestID,
                                           String requestPayloadFilename,
                                           Map<String, String> headerMap)
    {
        result.method = method;
        result.headerMap = headerMap;

        HttpClient client = new HttpClient();
        setTimeouts(client, result);
        HttpMethod httpMethod;
        PostMethod postMethod = null;
        PutMethod putMethod = null;
        if ("POST".equalsIgnoreCase(method)) {
            postMethod = new PostMethod(urlString);
            httpMethod = postMethod;
        } else if ("PUT".equalsIgnoreCase(method))  {
            putMethod = new PutMethod(urlString);
            httpMethod = putMethod;
        } else {
            result.addError("Method not supported: "+method);
            return result;
        }

        httpMethod.setRequestHeader  ("Accept", contentType);
        result.addRequestHeader("Accept", contentType);
        httpMethod.setRequestHeader  ("content-type", contentType);
        result.addRequestHeader("content-type", contentType);

        for (Map.Entry<String,String> entry: headerMap.entrySet()){
            httpMethod.setRequestHeader(entry.getKey(), entry.getValue());
        }
        httpMethod.setRequestHeader("Authorization", "Basic " + authForTest);
        httpMethod.setRequestHeader("X-RestReplay-fromTestID", fromTestID);
        httpMethod.setRequestHeader("X-RestReplay-version", "1.0.4");
        if (postMethod!=null){
            postMethod.setRequestBody(content);
        } else if (putMethod!=null){
            putMethod.setRequestBody(content);
        }
        try {
            int iResponseCode = client.executeMethod(httpMethod);
            System.out.println("Response status code: " + result);
            String responseBody = httpMethod.getResponseBodyAsString();
            if (postMethod!=null){
                result.setResultWMime(responseBody, getResponseContentType(postMethod));
            } else if (putMethod!=null){
                result.setResultWMime(responseBody, getResponseContentType(putMethod));
            }
            result.responseMessage = httpMethod.getStatusText() + "::" + httpMethod.getStatusLine();
            result.requestPayloadFilename = requestPayloadFilename;
            result.requestPayload = content;
            result.requestPayloadsRaw = contentRaw;
            result.responseCode = iResponseCode;
            dumpResponseHeaders(httpMethod.getResponseHeaders(), result);
            extractLocation(httpMethod, "", result);
        } catch (java.net.SocketTimeoutException e) {
            result.addError("TIMEOUT. "+e.getLocalizedMessage());
        } catch (Throwable t){
            result.addError("Error in doPOST_PUT. url:"+urlString+" "+t.toString(), t);
            System.out.println(t.getMessage() +" url: "+urlString+"  :: stack trace \r\n" + getStackTrace(t));
        } finally {
            httpMethod.releaseConnection();
        }
        return result;
    }

    private static void dumpResponseHeaders(Header[] headers, ServiceResult result){
        StringBuffer sb = new StringBuffer();
        for (Header oneheader: headers){
            sb.append("<span class='header response'>").append(oneheader.toString()).append("</span>");
        }
        result.responseHeadersDump = sb.toString();
    }

    private static String getResponseContentType(HttpMethodBase method){
        Header hdr = method.getResponseHeader("content-type");
        if (null==hdr){
            return "";
        }
        String contentType = hdr.getValue();
        //System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ response content type: "+contentType);
        int idx = contentType.indexOf(';');
        if (idx > -1){
            return contentType.substring(0, idx);
        } else {
            return contentType;
        }
    }

    private static String getResponseContentType(HttpURLConnection conn){
        return conn.getContentType();
    }

    private static String readStreamToString(HttpMethodBase method, ServiceResult result) throws IOException {
        String text = "ERROR in readStreamToString()";
        BufferedReader rd = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
        try {
            StringWriter sw = new StringWriter();
            char[] buffer = new char[1024 * 4];
            int n = 0;
            while (-1 != (n = rd.read(buffer))) {
                sw.write(buffer, 0, n);
            }
            text = sw.toString();
            Header hdrs[] = method.getRequestHeaders("CONTENT-TYPE");
            if (hdrs.length>0) {
                result.boundary = PayloadLogger.parseBoundary(hdrs[0].getValue());
            }
        } finally {
            rd.close();
        }
        return text;
    }

    private static void extractLocation(HttpMethod method, String urlString, ServiceResult result){
        Header[] headers = method.getResponseHeaders("Location");
        if (headers.length>0) {
            String locationZero = headers[0].getValue();
            if (locationZero != null){
                result.location = locationZero;
                result.deleteURL = locationZero;
            }
        }
    }

    private static String getStackTrace(Throwable t){
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream ps = new java.io.PrintStream(bos);
        t.printStackTrace(ps);
        String result = bos.toString();
        try {
            if(bos!=null)bos.reset();
            else System.out.println("bos was null, not closing");
        } catch (Exception e)  {System.out.println("ERROR: couldn't reset() bos in Tools "+e);}
        return result;
    }


    public static void main(String[]args) throws Exception {
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://localhost:18080/tagonomy?mock=true&theToken=TOKEN");
        getMethod.addRequestHeader("Accept", "application/json");
        int statusCode1 = client.executeMethod(getMethod);
        System.out.println(statusCode1);
    }

    //====================== TODOs ===================================================================

    /*

    private static void readStream(HttpURLConnection  conn, ServiceResult result, String mimeType) throws Throwable {
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        try {
                String line;
                StringBuffer sb = new StringBuffer();
                while ((line = rd.readLine()) != null) {
                    sb.append(line).append("\r\n");
                }
                String msg = sb.toString();
                result.setResultWMime(msg, mimeType);
                result.boundary = PayloadLogger.parseBoundary(conn.getHeaderField("CONTENT-TYPE"));
        } finally {
            rd.close();
        }
    }



    private static void readErrorStream(HttpURLConnection  conn, ServiceResult result, String mimeType) throws Throwable {
        InputStream stream = conn.getErrorStream();
        if (stream == null){
            stream = conn.getInputStream();
        }
        BufferedReader rd = new BufferedReader(new InputStreamReader(stream));
        try {
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            String msg = sb.toString();
            result.setResultWMime(msg, mimeType);
        } finally {
            rd.close();
        }
    }


    private static String extractLocation(List<String> locations, String urlString, ServiceResult result){
        if (locations != null && locations.size()>0) {
            String locationZero = locations.get(0);
            if (locationZero != null){
                result.location = locationZero;
                result.deleteURL = locationZero;
                return locationZero;
            }
        }
        return "";
    }



    private static void dumpRequestHeaders(HttpURLConnection conn, ServiceResult result) {
        //String foo = conn.getHeaderFields();
    }

    private static void dumpResponseHeaders(HttpURLConnection conn, ServiceResult result){
        StringBuffer sb = new StringBuffer();
        Map<String, List<String>> headers = conn.getHeaderFields();
        for (Map.Entry<String, List<String>> oneheader: headers.entrySet()){
            //System.out.println("HEADER: "+oneheader.toString());
            sb.append("<span class='header response'>").append(oneheader.toString()).append("</span>");
        }
        result.responseHeadersDump = sb.toString();
    }



    public static ServiceResult doPOST_PUT_HttpURLConnection(ServiceResult result,
                                           String urlString,
                                           String content,
                                           String contentRaw,
                                           String boundary,
                                           String method,
                                           String contentType,
                                           String authForTest,
                                           String fromTestID,
                                           String requestPayloadFilename,
                                           Map<String, String> headerMap) {
        result.method = method;
        result.headerMap = headerMap;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn;
            conn = (HttpURLConnection) url.openConnection();

            if (MULTIPART_MIXED.equalsIgnoreCase(contentType)){
                conn.setRequestProperty("Accept", "multipart/mixed");
                result.addRequestHeader("Accept", "multipart/mixed");
                conn.setRequestProperty("content-type", "multipart/mixed; boundary=" + boundary);
                result.addRequestHeader("content-type", "multipart/mixed; boundary=" + boundary);
            } else {
                //conn.setRequestProperty("Accept", "application/xml");
                //conn.setRequestProperty("Accept", "application/json");
                //todo: for now, simply set the Accept to mirror the content-type.  Later, add optional xml parameter in control file.
                conn.setRequestProperty("Accept", contentType);
                result.addRequestHeader("Accept", contentType);
                conn.setRequestProperty("content-type", contentType);
                result.addRequestHeader("content-type", contentType);
            }
            for (Map.Entry<String,String> entry: headerMap.entrySet()){
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            conn.setRequestProperty("Authorization", "Basic " + authForTest);  //TODO: remove test user : hard-coded as "dGVzdDp0ZXN0"
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("X-RestReplay-fromTestID", fromTestID);
            conn.setConnectTimeout(result.connectionTimeout);  //todo: soTimeout ignored.
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(method); // "POST" or "PUT"
            dumpRequestHeaders(conn, result);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(content);
            wr.flush();
            wr.close();//20150126
            try {
                result.requestPayloadFilename = requestPayloadFilename;
                result.requestPayload = content;
                result.requestPayloadsRaw = contentRaw;
                result.responseCode = conn.getResponseCode();
                result.setMimeType(getResponseContentType(conn));
                dumpResponseHeaders(conn, result);
                if (400 <= result.responseCode){
                    readErrorStream(conn, result, result.getMimeType());
                    return result;
                }
                readStream(conn, result, result.getMimeType());
            } catch (java.net.SocketTimeoutException e) {
                result.addError("TIMEOUT. "+e.getLocalizedMessage());
            } catch (Throwable t){
                //System.err.println("ERROR getting content from response: "+t);
                result.addError("Error in doPOST_PUT. url:"+urlString+" "+t.toString(), t);
                System.out.println(t.getMessage() +" url: "+urlString+"  :: stack trace \r\n" + getStackTrace(t));
            } finally {
                wr.close();
            }
            Map<String, List<String>> headers = conn.getHeaderFields();
            String lh = extractLocation(headers.get("Location"), urlString, result);
            result.location = lh;
            result.deleteURL = lh;
            result.CSID = lh;
        } catch (Throwable t2){
            result.addError("ERROR in Transport. ", t2);
        }
        return result;
    }

    public static ServiceResult doPOST_PUT_PostMethod(ServiceResult result, String urlString, String content, Map<String,String> contentRaw,
                                                      String boundary, String method, String contentType,
                                                      String authForTest, String fromTestID) throws Exception {
        result.method = method;
        //result.headerMap = headerMap;
        String deleteURL = "";
        String location = "";
        try {
            HttpClient client = new HttpClient();
            setTimeouts(client, result);
            PostMethod postMethod = new PostMethod(urlString);
            //postMethod.setRequestHeader("Accept", "multipart/mixed");
            //postMethod.addRequestHeader("Accept", "application/xml");
            //postMethod.addRequestHeader("Accept", "application/json");

            //todo: for now, simply set the Accept to mirror the content-type.  Later, add optional xml parameter in control file.
            postMethod.addRequestHeader("Accept", contentType);
            result.addRequestHeader("Accept", contentType);
            postMethod.addRequestHeader("content-type", contentType);
            result.addRequestHeader("content-type", contentType);

            postMethod.setRequestHeader("Authorization", "Basic " + authForTest);
            postMethod.setRequestHeader("X-RestReplay-fromTestID", fromTestID);
            //this method takes an array of params.  Not sure what they expect us to do with a raw post:
            //   postMethod.setRequestBody();
            int statusCode1 = 0;
            String res = "";
            try {
                statusCode1 = client.executeMethod(postMethod);
                result.responseCode = statusCode1;
                //System.out.println("statusCode: "+statusCode1+" statusLine ==>" + postMethod.getStatusLine());
                result.responseMessage = postMethod.getStatusText();
                res = readStreamToString(postMethod, result);//getResponseBodyAsString();  //TODO: HttpMethodBase says: Using getResponseBodyAsStream instead is recommended.
                dumpResponseHeaders(postMethod.getResponseHeaders(), result);
                result.setResultWMime(res, getResponseContentType(postMethod));
                extractLocation(postMethod, urlString, result);
                result.contentLength = postMethod.getResponseContentLength();
                postMethod.releaseConnection();
            } catch (Throwable t){
                result.addError(t.toString(),t);
            }
            result.location = location;
            result.deleteURL = deleteURL;
            result.CSID = location;
        } catch (Throwable t2){
            result.addError("ERROR in Transport", t2);
        }
        return result;
    }
    */



    /** Use this overload for multipart messages. */
    /*
    public static ServiceResult doPOST_PUTFromXML_Multipart(List<String> filesList,
                                                            List<String> partsList,
                                                            List<Map<String, String>> varsList,
                                                            String protoHostPort,
                                                            String uri,
                                                            String method,
                                                            Eval evalStruct,
                                                            String authForTest,
                                                            String fromTestID)
            throws Exception {
        if (filesList == null || filesList.size() == 0
                || partsList == null || partsList.size() == 0
                || (partsList.size() != filesList.size())) {
            throw new Exception("filesList and partsList must not be empty and must have the same number of items each.");
        }
        String content = DD + BOUNDARY;
        Map<String, String> contentRaw = new LinkedHashMap<String, String>();
        for (int i = 0; i < partsList.size(); i++) {
            String fileName = filesList.get(i);
            String commonPartName = partsList.get(i);
            byte[] b = FileUtils.readFileToByteArray(new File(fileName));
            String xmlString = new String(b);

            xmlString = evalStruct.eval(xmlString, evalStruct.serviceResultsMap, varsList.get(i), evalStruct.jexl, evalStruct.jc);
            contentRaw.put(commonPartName, xmlString);
            content = content + CRLF + "label: " + commonPartName + CRLF
                    + "Content-Type: application/xml" + CRLF
                    + CRLF
                    + xmlString + CRLF
                    + DD + BOUNDARY;
        }
        content = content + DD;
        String urlString = protoHostPort + uri;
        return doPOST_PUT(urlString, content, contentRaw, BOUNDARY, method, MULTIPART_MIXED, authForTest, fromTestID); //method is POST or PUT.
    }
   */



    //HACK for speed testing in doPOST_PUT.
    //  Result: RestReplay takes 9ms to process one test
    // right up to the point of actually firing an HTTP request.
    // or ~ 120 records per second.
    //result.CSID = "2";
    //result.overrideGotExpectedResult();
    //if (true) return result;
    //END-HACK


}
