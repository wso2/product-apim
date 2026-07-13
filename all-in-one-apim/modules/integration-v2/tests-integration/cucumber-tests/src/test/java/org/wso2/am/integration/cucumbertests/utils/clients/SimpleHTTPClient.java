/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils.clients;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.Constants;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.HttpEntity;
import java.io.File;


import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class SimpleHTTPClient {

    private static final Log logger = LogFactory.getLog(SimpleHTTPClient.class);
    private final CloseableHttpClient client;

    private SimpleHTTPClient()  {

        try {
            // Initialize SSL Context to trust all certificates. Seed it with an empty in-memory trust
            // store so the JDK never reads the default cacerts: on some CI runners that keystore fails to
            // open (UnrecoverableKeyException: Password verification failed), which would abort client init.
            // TrustAllStrategy remains the sole trust decision (it trusts everything), so behaviour is the same.
            KeyStore emptyTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyTrustStore.load(null, null);
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(emptyTrustStore, new TrustAllStrategy())
                    .build();

            // Disable hostname mismatch checks
            SSLConnectionSocketFactory sslSocketFactory =
                    new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register(Constants.HTTP_SCHEME, PlainConnectionSocketFactory.getSocketFactory())
                            .register(Constants.HTTPS_SCHEME, sslSocketFactory)
                            .build();

            PoolingHttpClientConnectionManager connManager =
                    new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connManager.setMaxTotal(1000);        // total max connections
            connManager.setDefaultMaxPerRoute(100);   // max connections per route

            RequestConfig requestConfig = RequestConfig.custom()
                    .setRedirectsEnabled(false) // Disable redirects
                    .build();

            this.client = HttpClients.custom()
                    .setConnectionManager(connManager)
                    .setDefaultRequestConfig(requestConfig)
                    .evictExpiredConnections()
                    .disableCookieManagement() // Disable sending or storing cookies
                    .build();

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException
                | java.security.cert.CertificateException | java.io.IOException e) {
            throw new RuntimeException("Failed to initialize SimpleHTTPClient with SSL context", e);
        }
    }

    private static class InstanceHolder  {
        private static final SimpleHTTPClient INSTANCE = new SimpleHTTPClient();
    }

    public static SimpleHTTPClient getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Send a HTTP GET request to the specified URL
     *
     * @param url     Target endpoint URL
     * @param headers Any HTTP headers that should be added to the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doGet(String url, Map<String, String> headers)
            throws IOException {

        HttpGet request = new HttpGet(url);
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP HEAD request to the specified URL. Used for existence-check endpoints that respond with a
     * status code and no body (e.g. publisher role validation {@code HEAD /roles/{roleId}}).
     *
     * @param url     Target endpoint URL
     * @param headers Any HTTP headers that should be added to the request
     * @return Returned HTTP response (status code; body is empty for HEAD)
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doHead(String url, Map<String, String> headers)
            throws IOException {

        HttpHead request = new HttpHead(url);
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP GET request WITHOUT URI normalization, so a percent-encoded path (e.g. {@code %28}/{@code %29})
     * is sent to the server verbatim rather than being decoded by the client. Needed to test how the gateway
     * routes an encoded URI path segment — the default {@link #doGet} lets Apache HttpClient normalize/decode the
     * path, changing what the gateway receives.
     *
     * @param url     Target endpoint URL (with any percent-encoding already applied)
     * @param headers Any HTTP headers that should be added to the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doGetRaw(String url,
            Map<String, String> headers) throws IOException {

        HttpGet request = new HttpGet(url);
        request.setConfig(RequestConfig.custom().setNormalizeUri(false).build());
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTPS GET presenting a CLIENT CERTIFICATE (mutual SSL). Builds a transient HttpClient whose
     * SSLContext loads the given JKS keystore's KEY material (the client cert + private key) — so the client
     * offers that cert during the TLS handshake — while still trusting the gateway's server cert (trust-all).
     * Used to invoke an API whose securityScheme is {@code mutualssl}/{@code mutualssl_mandatory}. The singleton
     * client can't do this (it loads no key material), so this is a per-call client keyed to the keystore.
     *
     * @param clientKeyStorePath filesystem path to the client JKS keystore
     * @param keyStorePassword   the keystore (and key) password
     * @param url                target gateway HTTPS URL
     * @param headers            request headers
     * @return the HTTP response
     * @throws IOException on connectivity or keystore/SSL setup failure
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doMutualSSLGet(
            String clientKeyStorePath, String keyStorePassword, String url, Map<String, String> headers)
            throws IOException {
        try {
            KeyStore clientKeyStore = KeyStore.getInstance("JKS");
            try (InputStream in = new FileInputStream(clientKeyStorePath)) {
                clientKeyStore.load(in, keyStorePassword.toCharArray());
            }
            KeyStore emptyTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyTrustStore.load(null, null);
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(emptyTrustStore, new TrustAllStrategy())
                    .loadKeyMaterial(clientKeyStore, keyStorePassword.toCharArray())
                    .build();
            SSLConnectionSocketFactory sslSocketFactory =
                    new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            try (CloseableHttpClient mtlsClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setDefaultRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build())
                    .disableCookieManagement()
                    .build()) {
                HttpGet request = new HttpGet(url);
                setHeaders(headers, request);
                try (CloseableHttpResponse response = mtlsClient.execute(request)) {
                    return constructResponse(response);
                }
            }
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Failed to build mutual-SSL client for keystore " + clientKeyStorePath, e);
        }
    }

    /**
     * Send a HTTP DELETE request to the specified URL
     *
     * @param url     Target endpoint URL
     * @param headers Any HTTP headers that should be added to the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doDelete(
            String url, final Map<String, String> headers) throws IOException {

        HttpDelete request = new HttpDelete(url);
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP POST request to the specified URL
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param payload     Content payload that should be sent
     * @param contentType Content-type of the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPost(
            String url, final Map<String, String> headers, final String payload, String contentType)
            throws IOException {

        HttpPost request = new HttpPost(url);
        setHeaders(headers, request);
        boolean zip = headers != null && "gzip".equals(headers.get(HttpHeaders.CONTENT_ENCODING));

        if (payload != null) {
            EntityTemplate ent = getEntityTemplate(payload, contentType, zip);
            request.setEntity(ent);
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP POST request with multipart/form-data to the specified URL with multiple files
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param files       Map of field names to File objects (e.g., "policySpecFile" -> File)
     * @param formFields  Additional form fields (key-value pairs)
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPostMultipartWithFiles(
            String url, final Map<String, String> headers, final Map<String, File> files,
            final Map<String, String> formFields) throws IOException {

        HttpPost request = new HttpPost(url);

        setHeaders(headers, request);

        // Remove Content-Type header - let MultipartEntityBuilder set it with boundary
        request.removeHeaders("Content-Type");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);

        // Add files with specific field names - preserve original filenames
        if (files != null) {
            for (Map.Entry<String, File> fileEntry : files.entrySet()) {
                File file = fileEntry.getValue();
                if (file != null) {
                    String fileName = file.getName();
                    builder.addBinaryBody(
                            fileEntry.getKey(),
                            file,
                            ContentType.APPLICATION_OCTET_STREAM,
                            fileName
                    );
                }
            }
        }

        // Add form fields
        if (formFields != null) {
            for (Map.Entry<String, String> field : formFields.entrySet()) {
                builder.addTextBody(field.getKey(), field.getValue(),
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
            }
        }

        HttpEntity multipartEntity = builder.build();
        request.setEntity(multipartEntity);

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP PUT request to the specified URL
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param payload     Content payload that should be sent
     * @param contentType Content-type of the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPut(
            String url, final Map<String, String> headers, final String payload, String contentType)
            throws IOException {

        HttpPut request = new HttpPut(url);
        setHeaders(headers, request);
        final boolean zip = headers != null && "gzip".equals(headers.get(HttpHeaders.CONTENT_ENCODING));

        if (payload != null) {
            EntityTemplate ent = getEntityTemplate(payload, contentType, zip);
            request.setEntity(ent);
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP put request with multipart/form-data to the specified URL with multiple files
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param files       Map of field names to File objects (e.g., "policySpecFile" -> File)
     * @param formFields  Additional form fields (key-value pairs)
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPutMultipartWithFiles(
            String url, final Map<String, String> headers, final Map<String, File> files,
            final Map<String, String> formFields) throws IOException {

        HttpPut request = new HttpPut(url);

        setHeaders(headers, request);

        // Remove Content-Type header - let MultipartEntityBuilder set it with boundary
        request.removeHeaders("Content-Type");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);

        // Add files with specific field names - preserve original filenames
        if (files != null) {
            for (Map.Entry<String, File> fileEntry : files.entrySet()) {
                File file = fileEntry.getValue();
                if (file != null) {
                    String fileName = file.getName();
                    builder.addBinaryBody(
                            fileEntry.getKey(),
                            file,
                            ContentType.APPLICATION_OCTET_STREAM,
                            fileName
                    );
                }
            }
        }

        // Add form fields
        if (formFields != null) {
            for (Map.Entry<String, String> field : formFields.entrySet()) {
                builder.addTextBody(field.getKey(), field.getValue(),
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
            }
        }

        HttpEntity multipartEntity = builder.build();
        request.setEntity(multipartEntity);

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    /**
     * Send a HTTP PATCH request to the specified URL
     *
     * @param url         Target endpoint URL
     * @param headers     Any HTTP headers that should be added to the request
     * @param payload     Content payload that should be sent
     * @param contentType Content-type of the request
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPatch(String url, final Map<String, String> headers, final String payload, String contentType)
            throws IOException {

        HttpPatch request = new HttpPatch(url);
        setHeaders(headers, request);
        final boolean zip = headers != null && "gzip".equals(headers.get(HttpHeaders.CONTENT_ENCODING));

        if (payload != null) {
            EntityTemplate ent = getEntityTemplate(payload, contentType, zip);
            request.setEntity(ent);
        }

        try (CloseableHttpResponse response = client.execute(request)) {
            return constructResponse(response);
        }
    }

    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse sendSoapRequest(String url, String payload, String soapAction, String adminUsername,
                                 String adminPassword) throws IOException {

        String encodedAuth = Base64.getEncoder().encodeToString((adminUsername + ":" + adminPassword)
                .getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encodedAuth);
        headers.put("SOAPAction", soapAction);

        return this.doPost(url, headers, payload,
                Constants.CONTENT_TYPES.TEXT_XML);
    }

    /**
     * Builds an {@link EntityTemplate} that writes the given payload into the request body.
     *
     * @param payload     the request body content
     * @param contentType the MIME type of the request body (defaults to application/json if null)
     * @param zip         whether to gzip-compress the payload
     * @return configured EntityTemplate for use in an HTTP request
     */
    private static EntityTemplate getEntityTemplate(String payload, String contentType, boolean zip) {

        EntityTemplate ent = new EntityTemplate(outputStream -> {
            OutputStream out = zip ? new GZIPOutputStream(outputStream) : outputStream;
            try {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                if (zip) {
                    out.close();
                }
            }
        });

        ent.setContentType(contentType != null ? contentType : Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (zip) {
            ent.setContentEncoding("gzip");
        }
        return ent;
    }

    /**
     * Sets all headers from the given map onto the HTTP request.
     *
     * @param headers map of header names and values
     * @param request the request to update
     */
    private void setHeaders(Map<String, String> headers, HttpUriRequest request) {

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
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
        String body = responseEntityBodyToString(response);
        Header[] headers = response.getAllHeaders();
        Map<String, String> heads = new HashMap<>();
        for (Header header : headers) {
            heads.put(header.getName(), header.getValue());
        }
        return new org.wso2.carbon.automation.test.utils.http.client.HttpResponse(
                body, code, heads);
    }

    /**
     * read the response body as String
     *
     * @param response http response with type org.apache.http.HttpResponse
     * @return String of the response body
     * @throws IOException throws if any error occurred
     */
    public static String responseEntityBodyToString(HttpResponse response) throws IOException {
        if (response != null && response.getEntity() != null) {
            try (InputStream inputStreamContent = response.getEntity().getContent()) {
                return IOUtils.toString(inputStreamContent, StandardCharsets.UTF_8);
            }
        }
        // Return "" (not null) for a bodyless response (204/304, or a bodyless error). Callers
        // dereference HttpResponse.getData() unguarded (getData().contains(...), new JSONObject(getData())),
        // so a null body would surface as an opaque NPE instead of a meaningful assertion failure; an empty
        // string makes contains(...) behave and turns a JSON parse into a descriptive JSONException.
        return "";
    }
}
