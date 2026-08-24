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
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class SimpleHTTPClient {

    private static final Log logger = LogFactory.getLog(SimpleHTTPClient.class);
    private final CloseableHttpClient client;

    /**
     * The management REST API (publisher/admin/devportal) surfaces a transient server-side failure as this
     * generic error — HTTP 5xx with body {@code {"code":900967,"message":"General Error","description":"Server
     * Error Occurred",...}}. Under the concurrent-boot load of the parallel-block lane this occasionally hits an
     * otherwise-valid mutate (e.g. an API create returning 500/900967), so every request below retries ONLY on
     * this exact signature. See {@link #withGeneralErrorRetry}.
     */
    private static final int GENERAL_ERROR_CODE = 900967;
    private static final int GENERAL_ERROR_RETRIES = 3;
    private static final long GENERAL_ERROR_RETRY_WAIT_MS = 2000L;

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

    /** A deferred HTTP call (build request -> execute -> construct response), rebuilt fresh on each retry. */
    @FunctionalInterface
    private interface HttpCall {
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse invoke() throws IOException;
    }

    /**
     * Runs {@code call} and retries it — up to {@value #GENERAL_ERROR_RETRIES} attempts,
     * {@value #GENERAL_ERROR_RETRY_WAIT_MS} ms apart — ONLY when the response is the management API's transient
     * "General Error" (see {@link #GENERAL_ERROR_CODE}). Retrying that removes the load-induced flake WITHOUT
     * masking a real result: any other outcome — a 2xx, a deterministic 4xx, or even a DIFFERENT 5xx such as the
     * descriptive {@code "already exists"} / {@code "validation failure"} 500s some tests deliberately assert —
     * is NOT a 900967, so it returns on the first attempt. {@code call} rebuilds its request each attempt, so a
     * request entity (POST/PUT body, multipart) is re-sent cleanly rather than being read from a consumed stream.
     */
    private org.wso2.carbon.automation.test.utils.http.client.HttpResponse withGeneralErrorRetry(HttpCall call)
            throws IOException {
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse response = null;
        for (int attempt = 1; attempt <= GENERAL_ERROR_RETRIES; attempt++) {
            response = call.invoke();
            if (!isTransientGeneralError(response) || attempt == GENERAL_ERROR_RETRIES) {
                break;
            }
            logger.warn("Transient " + GENERAL_ERROR_CODE + " General Error on attempt " + attempt + "/"
                    + GENERAL_ERROR_RETRIES + "; retrying in " + GENERAL_ERROR_RETRY_WAIT_MS + "ms");
            try {
                Thread.sleep(GENERAL_ERROR_RETRY_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return response;
    }

    /**
     * True iff the response is the management API's transient {@code 5xx / code:900967 "General Error"}. Requires
     * BOTH the code and the "General Error" message so a gateway invocation fault (which is 5xx but carries a
     * different code — e.g. 601000 malformed request, 303001 endpoint suspended — and no such message) is never
     * mistaken for it and needlessly retried.
     */
    private static boolean isTransientGeneralError(
            org.wso2.carbon.automation.test.utils.http.client.HttpResponse response) {
        if (response == null || response.getResponseCode() < 500 || response.getData() == null) {
            return false;
        }
        String body = response.getData();
        return body.contains(String.valueOf(GENERAL_ERROR_CODE)) && body.contains("General Error");
    }

    /**
     * {@link DownloadResult} variant of {@link #isTransientGeneralError}: a 5xx whose file body (the error JSON
     * {@link #doGetToFileOnce} writes on a non-2xx) carries code 900967 + "General Error". Guarded on {@code >= 500}
     * so a successful (2xx) binary payload is never read back off disk.
     */
    private static boolean isTransientGeneralError(DownloadResult result) {
        if (result == null || result.getStatusCode() < 500 || result.getFile() == null) {
            return false;
        }
        try {
            String body = new String(java.nio.file.Files.readAllBytes(result.getFile().toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            return body.contains(String.valueOf(GENERAL_ERROR_CODE)) && body.contains("General Error");
        } catch (IOException e) {
            return false;
        }
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

        return withGeneralErrorRetry(() -> {
            HttpGet request = new HttpGet(url);
            setHeaders(headers, request);
            try (CloseableHttpResponse response = client.execute(request)) {
                return constructResponse(response);
            }
        });
    }

    /**
     * GETs a URL and writes the raw response body BYTES to a temp file, returning it. Needed for binary payloads
     * (e.g. an API export archive zip) that {@link #doGet} would corrupt by decoding the body as a UTF-8 String.
     * The HTTP status is returned separately so callers can assert it. On a non-2xx status the (text) error body
     * is written to the file too, so the caller can inspect it.
     *
     * @param url     target endpoint URL
     * @param headers request headers
     * @param suffix  temp-file suffix (e.g. ".zip")
     * @return a DownloadResult carrying the status code and the temp file
     * @throws IOException if the request or file write fails
     */
    public DownloadResult doGetToFile(String url, Map<String, String> headers, String suffix) throws IOException {
        // Downloads bypass the HttpResponse funnels, so apply the SAME transient 900967 "General Error" retry here
        // (see withGeneralErrorRetry): load flakiness on an export/download is a given and is absorbed at the client
        // level, not per step. Only a 5xx whose body carries code 900967 + "General Error" is retried; any other
        // outcome (2xx, a deterministic 4xx, a different 5xx) returns on the first attempt.
        DownloadResult result = null;
        for (int attempt = 1; attempt <= GENERAL_ERROR_RETRIES; attempt++) {
            result = doGetToFileOnce(url, headers, suffix);
            if (attempt == GENERAL_ERROR_RETRIES || !isTransientGeneralError(result)) {
                break;
            }
            logger.warn("Transient " + GENERAL_ERROR_CODE + " General Error on download attempt " + attempt + "/"
                    + GENERAL_ERROR_RETRIES + "; retrying in " + GENERAL_ERROR_RETRY_WAIT_MS + "ms");
            try {
                Thread.sleep(GENERAL_ERROR_RETRY_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return result;
    }

    private DownloadResult doGetToFileOnce(String url, Map<String, String> headers, String suffix) throws IOException {
        HttpGet request = new HttpGet(url);
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            org.apache.http.Header ct = response.getFirstHeader("Content-Type");
            org.apache.http.Header cd = response.getFirstHeader("Content-Disposition");
            File file = File.createTempFile("download", suffix);
            file.deleteOnExit();
            if (response.getEntity() != null) {
                try (InputStream in = response.getEntity().getContent();
                     FileOutputStream out = new FileOutputStream(file)) {
                    in.transferTo(out);
                }
            }
            return new DownloadResult(code, file,
                    ct == null ? null : ct.getValue(), cd == null ? null : cd.getValue());
        }
    }

    /** Result of {@link #doGetToFile}: the HTTP status and the temp file holding the response body bytes. */
    public static final class DownloadResult {
        private final int statusCode;
        private final File file;
        private final String contentType;
        private final String contentDisposition;

        public DownloadResult(int statusCode, File file) {
            this(statusCode, file, null, null);
        }

        public DownloadResult(int statusCode, File file, String contentType, String contentDisposition) {
            this.statusCode = statusCode;
            this.file = file;
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public File getFile() {
            return file;
        }

        /** Raw {@code Content-Type} response header (e.g. {@code application/zip}), or null if absent. */
        public String getContentType() {
            return contentType;
        }

        /** Raw {@code Content-Disposition} response header, or null if absent. */
        public String getContentDisposition() {
            return contentDisposition;
        }

        /** The {@code filename="..."} value from {@code Content-Disposition}, or null if absent/unparseable. */
        public String getDownloadFilename() {
            if (contentDisposition == null) {
                return null;
            }
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("filename=\"?([^\";]+)\"?").matcher(contentDisposition);
            return m.find() ? m.group(1) : null;
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

        return withGeneralErrorRetry(() -> {
            HttpHead request = new HttpHead(url);
            setHeaders(headers, request);
            try (CloseableHttpResponse response = client.execute(request)) {
                return constructResponse(response);
            }
        });
    }

    /**
     * Send a HTTP OPTIONS request. Used to exercise CORS pre-flight handling at the gateway (the gateway either
     * answers the pre-flight itself when CORS is enabled, or forwards it to the backend), so the caller can assert
     * the returned Access-Control-* response headers.
     *
     * @param url     Target endpoint URL
     * @param headers Any HTTP headers that should be added to the request (e.g. Origin, Access-Control-Request-*)
     * @return Returned HTTP response
     * @throws IOException If an error occurs while making the invocation
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doOptions(String url,
            Map<String, String> headers) throws IOException {

        return withGeneralErrorRetry(() -> {
            HttpOptions request = new HttpOptions(url);
            setHeaders(headers, request);
            try (CloseableHttpResponse response = client.execute(request)) {
                return constructResponse(response);
            }
        });
    }

    /**
     * Send a HTTP GET request WITHOUT URI normalization, so a percent-encoded path (e.g. {@code %28}/{@code %29})
     * is sent to the server verbatim rather than being decoded by the client. Needed to test how the gateway
     * routes an encoded URI path segment — the default {@link #doGet} lets Apache HttpClient normalize/decode the
     * path, changing what the gateway receives.
     *
     * <p>Gateway-invocation primitive (used by {@code APIInvocationSteps}) — intentionally NOT wrapped in the
     * {@link #withGeneralErrorRetry} 900967 retry, which is a management-plane transient; runtime gateway
     * flakiness is absorbed by the invocation envelope ({@code Utils.retryUntil}), not here.
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
     * Sends a request and returns ALL values of one response header, DUPLICATES PRESERVED, next to the status and
     * body. The shared {@link org.wso2.carbon.automation.test.utils.http.client.HttpResponse} collapses headers
     * into a {@code Map<String,String>}, which structurally cannot represent a backend that emits the same header
     * twice (two {@code Set-Cookie}s, a duplicated hop-by-hop {@code Transfer-Encoding}) — hence this variant.
     *
     * <p>Gateway-invocation primitive (used by {@code APIInvocationSteps}) — like {@link #doGetRaw} it is
     * intentionally NOT wrapped in the {@link #withGeneralErrorRetry} 900967 retry (a management-plane transient);
     * runtime gateway flakiness is absorbed by the invocation envelope ({@code Utils.retryUntil}).
     *
     * <p>It rides this shared Apache client on purpose: the container's certificate is {@code CN=localhost} with
     * only {@code DNSName: localhost} in its SAN, so any client that performs hostname verification can NEVER
     * reach the gateway when testcontainers publishes it on an IP (e.g. a colima/remote-docker host, where the
     * mapped URL is {@code https://<ip>:<port>}). This client pairs trust-all with {@code NoopHostnameVerifier};
     * the JDK {@code java.net.http.HttpClient} always forces {@code endpointIdentificationAlgorithm=HTTPS} and
     * has no API to turn it off, so it failed every attempt there with an SSLHandshakeException.
     *
     * @param method     HTTP method (e.g. {@code GET})
     * @param url        target endpoint URL
     * @param headers    request headers
     * @param headerName response header whose values are collected
     * @return status code, every value of {@code headerName} in wire order, and the response body
     * @throws IOException if the request fails
     */
    public MultiValuedHeaderResult doRequestCollectingHeader(String method, String url, Map<String, String> headers,
            String headerName) throws IOException {

        HttpUriRequest request = RequestBuilder.create(method.toUpperCase(Locale.ROOT)).setUri(url).build();
        setHeaders(headers, request);
        try (CloseableHttpResponse response = client.execute(request)) {
            List<String> values = new ArrayList<>();
            for (Header header : response.getHeaders(headerName)) {
                values.add(header.getValue());
            }
            return new MultiValuedHeaderResult(response.getStatusLine().getStatusCode(), values,
                    responseEntityBodyToString(response));
        }
    }

    /** Result of {@link #doRequestCollectingHeader}: status, every value of the requested header, and the body. */
    public static final class MultiValuedHeaderResult {
        private final int statusCode;
        private final List<String> headerValues;
        private final String body;

        public MultiValuedHeaderResult(int statusCode, List<String> headerValues, String body) {
            this.statusCode = statusCode;
            this.headerValues = headerValues;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        /** Every value of the requested response header, in wire order (duplicates preserved). */
        public List<String> getHeaderValues() {
            return headerValues;
        }

        public String getBody() {
            return body;
        }
    }

    /** JKS magic (SUN provider): 0xFEEDFEED. */
    private static final int MAGIC_JKS = 0xFEEDFEED;

    /** JCEKS magic (SunJCE provider): 0xCECECECE. Distinct from JKS and NOT loadable as it. */
    private static final int MAGIC_JCEKS = 0xCECECECE;

    /**
     * The keystore type of {@code path}, chosen from the file's own MAGIC BYTES rather than its extension —
     * fixtures here are named {@code .jks} or {@code .p12} but the name is not authoritative.
     *
     * <p>Hardcoding {@code "JKS"} worked only through the JDK's {@code keystore.type.compat} shim, which silently
     * accepts a PKCS#12 file for a JKS request. Choosing explicitly means a hardened {@code java.security} that
     * disables that shim cannot break these tests.</p>
     *
     * <p>JKS and JCEKS are the two magic-prefixed formats; anything else is DER (PKCS#12 begins 0x30 0x82).
     * JCEKS is enumerated because it is NOT interchangeable with JKS — treating it as PKCS#12 would fail to load.
     */
    private static String keystoreTypeOf(String path) throws IOException {
        try (InputStream in = new FileInputStream(path)) {
            byte[] magic = in.readNBytes(4);
            if (magic.length < 4) {
                throw new IOException("Keystore '" + path + "' is too short to identify (" + magic.length
                        + " bytes); expected a JKS, JCEKS or PKCS#12 file.");
            }
            int header = ((magic[0] & 0xFF) << 24) | ((magic[1] & 0xFF) << 16)
                    | ((magic[2] & 0xFF) << 8) | (magic[3] & 0xFF);
            if (header == MAGIC_JKS) {
                return "JKS";
            }
            if (header == MAGIC_JCEKS) {
                return "JCEKS";
            }
            return "PKCS12";
        }
    }

    /**
     * Send a HTTPS GET presenting a CLIENT CERTIFICATE (mutual SSL). Builds a transient HttpClient whose
     * SSLContext loads the given JKS keystore's KEY material (the client cert + private key) — so the client
     * offers that cert during the TLS handshake — while still trusting the gateway's server cert (trust-all).
     * Used to invoke an API whose securityScheme is {@code mutualssl}/{@code mutualssl_mandatory}. The singleton
     * client can't do this (it loads no key material), so this is a per-call client keyed to the keystore.
     *
     * <p>Gateway-invocation primitive (used by {@code MutualSslSteps}) — intentionally NOT wrapped in the
     * {@link #withGeneralErrorRetry} 900967 retry (a management-plane transient); it also runs on a per-call
     * mTLS client, not the shared one.
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
            KeyStore clientKeyStore = KeyStore.getInstance(keystoreTypeOf(clientKeyStorePath));
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

        return withGeneralErrorRetry(() -> {
            HttpDelete request = new HttpDelete(url);
            setHeaders(headers, request);
            try (CloseableHttpResponse response = client.execute(request)) {
                return constructResponse(response);
            }
        });
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

        return withGeneralErrorRetry(() -> {
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
        });
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

        return withGeneralErrorRetry(() -> {
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
        });
    }

    /**
     * Multipart POST where some form fields are sent as {@code application/json} parts rather than {@code
     * text/plain}. Needed by endpoints (e.g. the Service Catalog {@code serviceMetadata} part) that reject a
     * text/plain JSON field with a 500 and require the part's Content-Type to be application/json. {@code files}
     * are binary parts, {@code textFields} are text/plain, {@code jsonFields} are application/json.
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPostMultipartWithJsonFields(
            String url, final Map<String, String> headers, final Map<String, File> files,
            final Map<String, String> textFields, final Map<String, String> jsonFields) throws IOException {

        return withGeneralErrorRetry(() -> sendMultipartWithJsonFields(new HttpPost(url), headers, files, textFields,
                jsonFields));
    }

    /**
     * PUT counterpart of {@link #doPostMultipartWithJsonFields} — the Service Catalog update endpoint takes the
     * same multipart shape as its create (a {@code serviceMetadata} application/json part plus a binary
     * {@code definitionFile}), and a text/plain metadata part is rejected there too.
     */
    public org.wso2.carbon.automation.test.utils.http.client.HttpResponse doPutMultipartWithJsonFields(
            String url, final Map<String, String> headers, final Map<String, File> files,
            final Map<String, String> textFields, final Map<String, String> jsonFields) throws IOException {

        return withGeneralErrorRetry(() -> sendMultipartWithJsonFields(new HttpPut(url), headers, files, textFields,
                jsonFields));
    }

    /**
     * Builds and sends the mixed-content-type multipart body shared by the POST/PUT json-fields variants:
     * {@code files} as binary parts, {@code textFields} as text/plain, {@code jsonFields} as application/json.
     * Kept as one method so the two verbs cannot drift in how a part's Content-Type is set.
     */
    private org.wso2.carbon.automation.test.utils.http.client.HttpResponse sendMultipartWithJsonFields(
            HttpEntityEnclosingRequestBase request, Map<String, String> headers, Map<String, File> files,
            Map<String, String> textFields, Map<String, String> jsonFields) throws IOException {

        setHeaders(headers, request);
        // Remove Content-Type header - let MultipartEntityBuilder set it with boundary
        request.removeHeaders("Content-Type");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);

        if (files != null) {
            for (Map.Entry<String, File> fileEntry : files.entrySet()) {
                File file = fileEntry.getValue();
                if (file != null) {
                    builder.addBinaryBody(fileEntry.getKey(), file, ContentType.APPLICATION_OCTET_STREAM,
                            file.getName());
                }
            }
        }
        if (textFields != null) {
            for (Map.Entry<String, String> field : textFields.entrySet()) {
                builder.addTextBody(field.getKey(), field.getValue(),
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
            }
        }
        if (jsonFields != null) {
            for (Map.Entry<String, String> field : jsonFields.entrySet()) {
                builder.addTextBody(field.getKey(), field.getValue(),
                        ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
            }
        }

        request.setEntity(builder.build());
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

        return withGeneralErrorRetry(() -> {
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
        });
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

        return withGeneralErrorRetry(() -> {
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
        });
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

        return withGeneralErrorRetry(() -> {
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
        });
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
     * <p>The status line's REASON PHRASE is carried across into the response's {@code responseMessage}. Only the
     * status CODE is normally consumed, but a backend may answer with a non-standard reason phrase that the
     * gateway must pass through verbatim (the custom-status backend on {@code :3024} emits
     * {@code HTTP/1.1 400 Custom response}), and that is asserted on the phrase, not the code. Capturing it here
     * — the single raw-HTTP chokepoint — keeps it available on every response without a side channel.
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
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse constructed =
                new org.wso2.carbon.automation.test.utils.http.client.HttpResponse(body, code, heads);
        constructed.setResponseMessage(response.getStatusLine().getReasonPhrase());
        return constructed;
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
