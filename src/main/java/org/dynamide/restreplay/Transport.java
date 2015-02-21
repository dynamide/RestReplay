package org.dynamide.restreplay;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.*;
import java.util.Arrays;
import java.util.Map;

import org.dynamide.util.Tools;

/**
 *   @author Laramie Crocker
 */
public class Transport {

    public static String BOUNDARY = "34d97c83-0d61-4958-80ab-6bf8d362290f";
    public static final String MULTIPART_MIXED = "multipart/mixed";
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_JSON = "application/json";


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

    public static ServiceResult doGET(ServiceResult result, String urlString, String authForTest, String fromTestID,
                                      Map<String, String> headerMap) throws Exception {
        result.fromTestID = fromTestID;
        result.headerMap = headerMap;
        result.method = "GET";
        HttpClient client = new HttpClient();
        setTimeouts(client, result);
        GetMethod getMethod = new GetMethod(urlString);
        getMethod.addRequestHeader("Accept", "multipart/mixed");
        getMethod.addRequestHeader("Accept", APPLICATION_XML);
               result.addRequestHeader("Accept", APPLICATION_XML);
        getMethod.addRequestHeader("Accept", APPLICATION_JSON);
               result.addRequestHeader("Accept", APPLICATION_JSON);
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
            result.responseMessage = getMethod.getStatusText();
            //the full lines (HTTP 1.1 OK, rather than just OK) is: getMethod.getStatusLine();
            Header[] headers = getMethod.getResponseHeaders();
            dumpResponseHeaders(headers, result);
            result.setResponseHeaders(headers);
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
        deleteMethod.addRequestHeader("Accept", Transport.APPLICATION_XML);
                  result.addRequestHeader("Accept",Transport.APPLICATION_XML);
        deleteMethod.addRequestHeader("Accept", Transport.APPLICATION_JSON);  //TODO: test whether this impl sets multiple accepts correctly.
                  result.addRequestHeader("Accept", Transport.APPLICATION_JSON);

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


    /** This does not deal with boundary or multipart since being ported. See code at bottom.  */
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
        HttpMethodBase httpMethod;
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
            String responseBody = httpMethod.getResponseBodyAsString();
            result.setResultWMime(responseBody, getResponseContentType(httpMethod));
            result.responseMessage = httpMethod.getStatusText();
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
        int idx = contentType.indexOf(';');
        if (idx > -1){
            return contentType.substring(0, idx);
        } else {
            return contentType;
        }
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
        Old code removed that dealt with HttpURLConnection in commit  on 2015-01-26.
        commit 37633d028c73b3df891447a4e49a8f93e91a5b79
        Author: Crocker, Laramie
        Date:   Mon Jan 26 17:41:36 2015 -0800
     */

    /** TODO: Use this overload for multipart messages. */
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
