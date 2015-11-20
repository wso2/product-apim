/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.test.utils.http;

import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A utility for handling HTTP requests
 */
@SuppressWarnings("unused")
public class HttpRequestUtil {
    /**
     * Sends an HTTP GET request to a url
     *
     * @param endpoint          - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2").
     *                          Note: This method will add the question mark (?) to the request - DO NOT add it yourself
     * @return - The response from the end point
     * @throws java.io.IOException If an error occurs while sending the GET request
     */
    public static HttpResponse sendGetRequest(String endpoint,
                                              String requestParameters) throws IOException {
        if (endpoint.startsWith("http://")) {
            String urlStr = endpoint;
            if (requestParameters != null && requestParameters.length() > 0) {
                urlStr += "?" + requestParameters;
            }
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setReadTimeout(10000);
            conn.connect();
            // Get the response
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
            } finally {
                if (rd != null) {
                    rd.close();
                }
            }
            return new HttpResponse(sb.toString(), conn.getResponseCode());
        }
        return null;
    }

    /**
     * Reads data from the data reader and posts it to a server via POST request.
     * data - The data you want to send
     * endpoint - The server's address
     * output - writes the server's response to output
     *
     * @param data     Data to be sent
     * @param endpoint The endpoint to which the data has to be POSTed
     * @param output   Output
     * @throws Exception If an error occurs while POSTing
     */
    public static void sendPostRequest(Reader data, URL endpoint, Writer output) throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("Content-type", "text/xml; charset=" + "UTF-8");
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                pipe(data, writer);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while posting data", e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            InputStream in = urlConnection.getInputStream();
            try {
                Reader reader = new InputStreamReader(in);
                pipe(reader, output);
                reader.close();
            } catch (IOException e) {
                throw new Exception("IOException while reading response", e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static HttpResponse doPost(URL endpoint, String body) throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                writer.write(body);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while posting data", e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            // Get the response
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
            } finally {
                if (rd != null) {
                    rd.close();
                }
            }
            Iterator<String> itr = urlConnection.getHeaderFields().keySet().iterator();
            Map<String, String> headers = new HashMap();
            while (itr.hasNext()) {
                String key = itr.next();
                if (key != null) {
                    headers.put(key, urlConnection.getHeaderField(key));
                }
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode(), headers);
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static HttpResponse doPost(URL endpoint, String postBody, Map<String, String> headers)
            throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            //setting headers
            if (headers != null && headers.size() > 0) {
                Iterator<String> itr = headers.keySet().iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    if (key != null) {
                        urlConnection.setRequestProperty(key, headers.get(key));
                    }
                }
                for (String key : headers.keySet()) {
                    urlConnection.setRequestProperty(key, headers.get(key));
                }

            }
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                writer.write(postBody);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while posting data", e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            // Get the response
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
            } finally {
                if (rd != null) {
                    rd.close();
                }
            }
            Iterator<String> itr = urlConnection.getHeaderFields().keySet().iterator();
            Map<String, String> responseHeaders = new HashMap();
            while (itr.hasNext()) {
                String key = itr.next();
                if (key != null) {
                    responseHeaders.put(key, urlConnection.getHeaderField(key));
                }
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode(), responseHeaders);
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static HttpResponse doGet(String endpoint, Map<String, String> headers) throws IOException {
        HttpResponse httpResponse;
        if (endpoint.startsWith("http://")) {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setReadTimeout(30000);
            //setting headers
            if (headers != null && headers.size() > 0) {
                Iterator<String> itr = headers.keySet().iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    if (key != null) {
                        conn.setRequestProperty(key, headers.get(key));
                    }
                }
                for (String key : headers.keySet()) {
                    conn.setRequestProperty(key, headers.get(key));
                }
            }
            conn.connect();
            // Get the response
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                httpResponse = new HttpResponse(sb.toString(), conn.getResponseCode());
                httpResponse.setResponseMessage(conn.getResponseMessage());
            } catch (IOException ignored) {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                httpResponse = new HttpResponse(sb.toString(), conn.getResponseCode());
                httpResponse.setResponseMessage(conn.getResponseMessage());
            } finally {
                if (rd != null) {
                    rd.close();
                }
            }
            return httpResponse;
        }
        return null;
    }

    /**
     * Reads data from the data reader and posts it to a server via POST request.
     * data - The data you want to send
     * endpoint - The server's address
     * output - writes the server's response to output
     * contentType   content type of the message
     *
     * @param data        Data to be sent
     * @param endpoint    The endpoint to which the data has to be POSTed
     * @param output      Output
     * @param contentType content type of the message
     * @throws Exception If an error occurs while POSTing
     */
    public static void sendPostRequest(Reader data, URL endpoint, Writer output, String contentType)
            throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("Content-type", contentType);
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                pipe(data, writer);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while posting data", e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            InputStream in = urlConnection.getInputStream();
            try {
                Reader reader = new InputStreamReader(in);
                pipe(reader, output);
                reader.close();
            } catch (IOException e) {
                throw new Exception("IOException while reading response", e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }


    /**
     * Reads data from the data reader and posts it to a server via PUT request.
     * data - The data you want to send
     * endpoint - The server's address
     * output - writes the server's response to output
     * contentType   content type of the message
     *
     * @param data        Data to be sent
     * @param endpoint    The endpoint to which the data has to be POSTed
     * @param output      Output
     * @param contentType content type of the message
     * @throws Exception If an error occurs while POSTing
     */
    public static void sendPutRequest(Reader data, URL endpoint, Writer output, String contentType)
            throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("PUT");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support PUT??" +
                        e.getMessage(), e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("Content-type", contentType);
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                pipe(data, writer);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while posting data" + e.getMessage(), e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            InputStream in = urlConnection.getInputStream();
            try {
                Reader reader = new InputStreamReader(in);
                pipe(reader, output);
                reader.close();
            } catch (IOException e) {
                throw new Exception("IOException while reading response" + e.getMessage(), e);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " +
                    e.getMessage(), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * @param endpoint
     * @param putBody
     * @param headers
     * @return
     * @throws Exception
     */

    public static HttpResponse doPut(URL endpoint, String putBody, Map<String, String> headers)
            throws Exception {
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("PUT");
            } catch (ProtocolException e) {
                throw new Exception("Shouldn't happen: HttpURLConnection doesn't support PUT??", e);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            //setting headers
            if (headers != null && headers.size() > 0) {
                Iterator<String> itr = headers.keySet().iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    if (key != null) {
                        urlConnection.setRequestProperty(key, headers.get(key));
                    }
                }
                for (String key : headers.keySet()) {
                    urlConnection.setRequestProperty(key, headers.get(key));
                }

            }
            OutputStream out = urlConnection.getOutputStream();
            try {
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                writer.write(putBody);
                writer.close();
            } catch (IOException e) {
                throw new Exception("IOException while putting data", e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            // Get the response
            StringBuilder sb = new StringBuilder();
            BufferedReader rd = null;
            try {
                rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException ignored) {
            } finally {
                if (rd != null) {
                    rd.close();
                }
            }
            Iterator<String> itr = urlConnection.getHeaderFields().keySet().iterator();
            Map<String, String> responseHeaders = new HashMap();
            while (itr.hasNext()) {
                String key = itr.next();
                if (key != null) {
                    responseHeaders.put(key, urlConnection.getHeaderField(key));
                }
            }
            return new HttpResponse(sb.toString(), urlConnection.getResponseCode(), responseHeaders);
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " +
                    e.getMessage(), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }


    /**
     * Sends an HTTP DELETE request to a url
     *
     * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param contentType - content type of the message
     * @return - The response code from the endpoint
     * @throws java.io.IOException If an error occurs while sending the DELETE request
     */
    public static int sendDeleteRequest(URL endpoint, String contentType) throws Exception {
        HttpURLConnection urlConnection = null;
        int responseCode;
        try {
            urlConnection = (HttpURLConnection)endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("DELETE");
            } catch (ProtocolException var33) {
                throw new Exception(
                        "Shouldn\'t happen: HttpURLConnection doesn\'t support DELETE?? " + var33.getMessage(), var33);
            }
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-type", contentType);
            responseCode = urlConnection.getResponseCode();
        } catch (IOException var36) {
            throw new Exception(
                    "Connection error (is server running at " + endpoint + " ?): " + var36.getMessage(), var36);
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return responseCode;
    }


    /**
     * Send
      * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param headers -
     * @return - The response code from the endpoint
     * @throws Exception - ava.io.IOException If an error occurs while sending the DELETE request
     */

    public static int doDelete(URL endpoint, Map<String, String> headers)
            throws Exception {
        HttpURLConnection urlConnection = null;
        int responseCode;
        try {
            urlConnection = (HttpURLConnection)endpoint.openConnection();
            try {
                urlConnection.setRequestMethod("DELETE");
            } catch (ProtocolException var33) {
                throw new Exception(
                        "Shouldn\'t happen: HttpURLConnection doesn\'t support DELETE?? " + var33.getMessage(), var33);
            }
            urlConnection.setDoOutput(true);
            //setting headers
            if (headers != null && headers.size() > 0) {
                Iterator<String> itr = headers.keySet().iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    if (key != null) {
                        urlConnection.setRequestProperty(key, headers.get(key));
                    }
                }
                for (String key : headers.keySet()) {
                    urlConnection.setRequestProperty(key, headers.get(key));
                }
            }
            responseCode = urlConnection.getResponseCode();
        } catch (IOException var36) {
            throw new Exception(
                    "Connection error (is server running at " + endpoint + " ?): " + var36.getMessage(), var36);
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return responseCode;

    }


    /**
     * Pipes everything from the reader to the writer via a buffer
     *
     * @param reader Reader
     * @param writer Writer
     * @throws java.io.IOException If piping fails
     */
    private static void pipe(Reader reader, Writer writer) throws IOException {
        char[] buf = new char[1024];
        int read;
        while ((read = reader.read(buf)) >= 0) {
            writer.write(buf, 0, read);
        }
        writer.flush();
    }
}


