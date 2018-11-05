/*
*Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.integration.tests.versioning;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class HttpClient {
    private static Log log = LogFactory.getLog(HttpRequestUtil.class);

    public HttpClient() {
    }

    public static HttpResponse doGet(String url, Map<String, String> headers) throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        org.apache.http.HttpResponse response = sendGetRequest(httpClient, url, headers);
        return constructResponse(response);
    }

    public static HttpResponse doPost(String url, Map<String, String> headers, List<NameValuePair> urlParameters)
            throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        org.apache.http.HttpResponse response = sendPOSTMessage(httpClient, url, headers, urlParameters);
        return constructResponse(response);
    }

    public static HttpResponse doMutulSSLGet(String path, String url, Map<String, String> headers) throws IOException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        CloseableHttpClient httpClient = getMutualSSLHttpsClient(path);
        org.apache.http.HttpResponse response = sendGetRequest(httpClient, url, headers);
        return constructResponse(response);
    }

    public static HttpResponse doPost(String url, Map<String, String> headers, String payload) throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        org.apache.http.HttpResponse response = sendPOSTMessage(httpClient, url, headers, payload);
        return constructResponse(response);
    }

    public static HttpResponse doPut(String url, Map<String, String> headers, String payload) throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        org.apache.http.HttpResponse response = sendPUTMessage(httpClient, url, headers, payload);
        return constructResponse(response);
    }

    public static HttpResponse doPost(URL url, Map<String, String> headers, String json) throws IOException {
        CloseableHttpClient httpClient = getHttpsClient();
        org.apache.http.HttpResponse response = sendPOSTMessage(httpClient, url.toString(), headers, json);
        return constructResponse(response);
    }

    public static HttpResponse doPost(URL url, String urlParams, Map<String, String> headers) throws IOException {
        List<NameValuePair> urlParameters = new ArrayList();
        if (urlParams != null && urlParams.contains("=")) {
            String[] paramList = urlParams.split("&");
            String[] arr$ = paramList;
            int len$ = paramList.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String pair = arr$[i$];
                if (pair.contains("=")) {
                    String[] pairList = pair.split("=");
                    String key = pairList[0];
                    String value = pairList.length > 1 ? pairList[1] : "";
                    urlParameters.add(new BasicNameValuePair(key, URLDecoder.decode(value, "UTF-8")));
                }
            }
        }

        return doPost((String) url.toString(), (Map) headers, (List) urlParameters);
    }

    public static CloseableHttpClient getHttpsClient() {
        int timeout = 7;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 10000).
                setConnectionRequestTimeout(timeout * 10000).setSocketTimeout(timeout * 10000).build();
        CloseableHttpClient httpClient = HttpClients.custom().disableRedirectHandling().
                setDefaultRequestConfig(config).setHostnameVerifier(SSLConnectionSocketFactory.
                ALLOW_ALL_HOSTNAME_VERIFIER).build();
        return httpClient;
    }

    private static CloseableHttpClient getMutualSSLHttpsClient(String keyStorePath) throws KeyStoreException,
            NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
        int timeout = 7;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 10000).
                setConnectionRequestTimeout(timeout * 10000).setSocketTimeout(timeout * 10000).build();
        KeyStore trustStore = KeyStore.getInstance("JKS");

        try {
            InputStream is = Files.newInputStream(Paths.get(keyStorePath, new String[0]), new OpenOption[0]);
            Throwable var5 = null;

            try {
                trustStore.load(is, "password".toCharArray());
            } catch (Throwable var15) {
                var5 = var15;
                throw var15;
            } finally {
                if (is != null) {
                    if (var5 != null) {
                        try {
                            is.close();
                        } catch (Throwable var14) {
                            var5.addSuppressed(var14);
                        }
                    } else {
                        is.close();
                    }
                }

            }
        } catch (CertificateException | IOException var17) {
            log.error("Error while loading keystore", var17);
        }

        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore,
                new TrustSelfSignedStrategy()).loadKeyMaterial(trustStore, "password".toCharArray()).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[]{"TLSv1"},
                (String[]) null, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return HttpClients.custom().setSSLSocketFactory(sslsf).disableRedirectHandling().
                setDefaultRequestConfig(config).setHostnameVerifier(SSLConnectionSocketFactory.
                ALLOW_ALL_HOSTNAME_VERIFIER).build();
    }

    private static org.apache.http.HttpResponse sendGetRequest(CloseableHttpClient httpClient, String url, Map<String,
            String> headers) throws IOException {
        HttpGet request = new HttpGet(url);
        if (headers != null) {
            Iterator i$ = headers.entrySet().iterator();

            while (i$.hasNext()) {
                Map.Entry<String, String> head = (Map.Entry) i$.next();
                request.addHeader((String) head.getKey(), (String) head.getValue());
            }
        }

        return httpClient.execute(request);
    }

    private static org.apache.http.HttpResponse sendPOSTMessage(CloseableHttpClient httpClient, String url, Map<String,
            String> headers, List<NameValuePair> urlParameters) throws IOException {
        HttpPost post = new HttpPost(url);
        if (headers != null) {
            Iterator i$ = headers.entrySet().iterator();

            while (i$.hasNext()) {
                Map.Entry<String, String> head = (Map.Entry) i$.next();
                post.addHeader((String) head.getKey(), (String) head.getValue());
            }
        }

        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        return httpClient.execute(post);
    }

    private static org.apache.http.HttpResponse sendPOSTMessage(CloseableHttpClient httpClient, String url, Map<String,
            String> headers, String body) throws IOException {
        HttpPost post = new HttpPost(url);
        if (headers != null) {
            Iterator i$ = headers.entrySet().iterator();

            while (i$.hasNext()) {
                Map.Entry<String, String> head = (Map.Entry) i$.next();
                post.addHeader((String) head.getKey(), (String) head.getValue());
            }
        }

        post.setEntity(new StringEntity(body));
        return httpClient.execute(post);
    }

    private static org.apache.http.HttpResponse sendPUTMessage(CloseableHttpClient httpClient, String url,
                                                               Map<String, String> headers, String body)
            throws IOException {
        HttpPut put = new HttpPut(url);
        if (headers != null) {
            Iterator i$ = headers.entrySet().iterator();

            while (i$.hasNext()) {
                Map.Entry<String, String> head = (Map.Entry) i$.next();
                put.addHeader((String) head.getKey(), (String) head.getValue());
            }
        }

        put.setEntity(new StringEntity(body));
        return httpClient.execute(put);
    }

    private static HttpResponse constructResponse(org.apache.http.HttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        String body = getResponseBody(response);
        Header[] headers = response.getAllHeaders();

        Map<String, String> heads = new HashMap();
        Header[] arr$ = headers;
        int len$ = headers.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Header header = arr$[i$];
            if (heads.get(header.getName()) != null) {
                heads.put(header.getName(), heads.get(header.getName()).concat("; " + header.getValue()));
            } else {
                heads.put(header.getName(), header.getValue());
            }
        }

        HttpResponse res = new HttpResponse(body, code, heads);
        return res;
    }

    public static String getResponseBody(org.apache.http.HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
                "UTF-8"));
        StringBuffer sb = new StringBuffer();

        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }

        rd.close();
        return sb.toString();
    }
}
