/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.test.utils.http;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is high level implementation for the HTTP client for Secure connection
 */
public class HTTPSClientUtils {

    /**
     * do HTTP GET operation for the given URL
     *
     * @param url     request URL
     * @param headers headers to be send
     * @return org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     * @throws IOException if connection issue occurred
     */
    public static org.wso2.carbon.automation.test.utils.http.client.HttpResponse doGet(String url,
            Map<String, String> headers) throws IOException {

        CloseableHttpClient httpClient = getHttpsClient();
        HttpResponse response = sendGetRequest(httpClient, url, headers);
        return constructResponse(response);
    }

    /**
     * do HTTP POST operation for the given URL
     *
     * @param url           request URL
     * @param headers       headers to be send
     * @param urlParameters parameters to be sent as payload
     * @return org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     * @throws IOException if connection issue occurred
     */
    public static org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPost(String url,
            Map<String, String> headers, List<NameValuePair> urlParameters) throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        HttpResponse response = sendPOSTMessage(httpClient, url, headers, urlParameters);
        return constructResponse(response);
    }

    /**
     * do HTTP POST operation for the given URL
     *
     * @param url           request URL
     * @param headers       headers to be send
     * @param urlParams parameter string to be sent as payload
     * @return org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     * @throws IOException if connection issue occurred
     */
    public static org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPost(URL url, String urlParams,
            Map<String, String> headers) throws IOException {
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        if(urlParams != null && urlParams.contains("=")){
            String[] paramList = urlParams.split("&");
            for (String pair : paramList) {
                if(pair.contains("=")) {
                    String[] pairList = pair.split("=");
                    urlParameters.add(new BasicNameValuePair(pairList[0], pairList[1]));
                }
            }
        }
        return doPost(url.toString(), headers, urlParameters);
    }

    /**
     * get the HTTP Client
     *
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient getHttpsClient() {
        CloseableHttpClient httpClient = HttpClients.custom().disableRedirectHandling()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        return httpClient;
    }

    /**
     * GET function implementation
     *
     * @param httpClient http client to use
     * @param url        request URL
     * @param headers    headers to be send
     * @return org.apache.http.HttpResponse
     * @throws IOException if connection issue occurred
     */
    private static HttpResponse sendGetRequest(CloseableHttpClient httpClient, String url, Map<String, String> headers)
            throws IOException {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            for (Map.Entry<String, String> head : headers.entrySet()) {
                request.addHeader(head.getKey(), head.getValue());
            }
        }
        return httpClient.execute(request);
    }

    /**
     * POST function implementation
     *
     * @param httpClient    http client to use
     * @param url           request URL
     * @param headers       headers to be send
     * @param urlParameters parameters to be sent as payload
     * @return org.apache.http.HttpResponse
     * @throws IOException if connection issue occurred
     */
    private static HttpResponse sendPOSTMessage(CloseableHttpClient httpClient, String url, Map<String, String> headers,
            List<NameValuePair> urlParameters) throws IOException {
        HttpPost post = new HttpPost(url);
        if (headers != null) {
            for (Map.Entry<String, String> head : headers.entrySet()) {
                post.addHeader(head.getKey(), head.getValue());
            }
        }
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        return httpClient.execute(post);
    }

    /**
     * Construct the org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     *
     * @param response org.apache.http.HttpResponse
     * @return org.wso2.carbon.automation.test.utils.http.client.HttpResponse
     * @throws IOException if any exception occurred when reading payload
     */
    private static org.wso2.carbon.automation.test.utils.http.client.HttpResponse constructResponse(
            HttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        String body = getResponseBody(response);
        Header[] headers = response.getAllHeaders();
        Map<String, String> heads = new HashMap<String, String>();
        for (Header header : headers) {
            heads.put(header.getName(), header.getValue());
        }
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse res = new org.wso2.carbon.automation.test.utils.http.client.HttpResponse(
                body, code, heads);
        return res;
    }

    /**
     * read the response body as String
     *
     * @param response http response with type org.apache.http.HttpResponse
     * @return String of the response body
     * @throws IOException throws if any error occurred
     */
    public static String getResponseBody(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String line;
        StringBuffer sb = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        return sb.toString();
    }

}
