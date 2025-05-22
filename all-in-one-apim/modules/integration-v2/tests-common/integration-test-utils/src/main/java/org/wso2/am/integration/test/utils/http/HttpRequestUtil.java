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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
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
     * @throws IOException If an error occurs while sending the GET request
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

    public static HttpResponse doPost(URL endpoint, String postBody, Map<String, String> headers)
            throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPost httpPost = new HttpPost(endpoint.toURI());
            httpPost.setEntity(new StringEntity(postBody));
            //setting headers
            if (headers != null && headers.size() > 0) {
                for (String key : headers.keySet()) {
                    if (key != null) {
                        httpPost.addHeader(key, headers.get(key));
                    }
                }
            }
            org.apache.http.HttpResponse httpResponse = httpClient.execute(httpPost);
            InputStream content = httpResponse.getEntity().getContent();
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : httpResponse.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            return new HttpResponse(IOUtils.toString(content), httpResponse.getStatusLine().getStatusCode(),
                    responseHeaders);
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        }
    }

    public static HttpResponse doGet(String endpoint, Map<String, String> headers) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(endpoint);
        //setting headers
        if (headers != null && headers.size() > 0) {
            for (String key : headers.keySet()) {
                if (key != null) {
                    httpGet.addHeader(key, headers.get(key));
                }
            }
        }
        org.apache.http.HttpResponse httpResponse = httpClient.execute(httpGet);
        try (InputStream content = httpResponse.getEntity().getContent()) {
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : httpResponse.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            return new HttpResponse(IOUtils.toString(content), httpResponse.getStatusLine().getStatusCode(),
                    responseHeaders);
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
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPut httpPut = new HttpPut(endpoint.toURI());
            httpPut.setEntity(new StringEntity(putBody));
            //setting headers
            if (headers != null && headers.size() > 0) {
                for (String key : headers.keySet()) {
                    if (key != null) {
                        httpPut.addHeader(key, headers.get(key));
                    }
                }
            }
            org.apache.http.HttpResponse httpResponse = httpClient.execute(httpPut);
            InputStream content = httpResponse.getEntity().getContent();
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : httpResponse.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            return new HttpResponse(IOUtils.toString(content), httpResponse.getStatusLine().getStatusCode(),
                    responseHeaders);
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        }
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
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpDelete httpDelete = new HttpDelete(endpoint.toURI());
            //setting headers
            if (headers != null && headers.size() > 0) {
                for (String key : headers.keySet()) {
                    if (key != null) {
                        httpDelete.addHeader(key, headers.get(key));
                    }
                }
            }
            org.apache.http.HttpResponse httpResponse = httpClient.execute(httpDelete);
            InputStream content = httpResponse.getEntity().getContent();
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : httpResponse.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            return httpResponse.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new Exception("Connection error (is server running at " + endpoint + " ?): " + e);
        }
    }


    /**
     * Pipes everything from the reader to the writer via a buffer
     *
     * @param reader Reader
     * @param writer Writer
     * @throws IOException If piping fails
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


