/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.utils;

import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Centralized funnel for management-plane HTTP requests whose response a following assertion step reads — the
 * counterpart of {@code APIInvocationSteps#execute} (the gateway-invocation funnel). Every method CLEARS the
 * shared {@code httpResponse} context key BEFORE the call, so if the call throws, the context is left WITHOUT a
 * stale response — a later {@code The response status code should be N} then cannot be satisfied by a leftover
 * value from an earlier step. On a real response it publishes it under {@code httpResponse} and returns it.
 *
 * <p>Use these instead of hand-writing {@code TestContext.remove("httpResponse")} /
 * {@code SimpleHTTPClient.getInstance().doX(...)} / {@code TestContext.set("httpResponse", ...)} at each call
 * site (that duplication is the stale-response trap this removes).
 *
 * <p>Use ONLY for the request whose response the step publishes for assertions. An intermediate call whose body
 * is consumed locally (e.g. a GET-then-mutate-then-PUT round trip) must NOT publish {@code httpResponse}, so keep
 * those on {@link SimpleHTTPClient} directly.
 */
public final class Requests {

    private static final String HTTP_RESPONSE_KEY = "httpResponse";

    private Requests() {
    }

    /** A deferred HTTP call, so the funnel can clear {@code httpResponse} BEFORE the call is made. */
    @FunctionalInterface
    private interface Call {
        HttpResponse invoke() throws IOException;
    }

    /** The single clear -> call -> set primitive every method below funnels through. */
    private static HttpResponse execute(Call call) throws IOException {
        TestContext.remove(HTTP_RESPONSE_KEY);
        HttpResponse response = call.invoke();
        TestContext.set(HTTP_RESPONSE_KEY, response);
        return response;
    }

    public static HttpResponse get(String url, Map<String, String> headers) throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doGet(url, headers));
    }

    /**
     * Binary GET whose response bytes are written to a temp file (for archive/zip downloads a String
     * {@code doGet} would corrupt). Clears {@code httpResponse} BEFORE the call like every other funnel method,
     * so a throw leaves no stale response behind; nothing is PUBLISHED to {@code httpResponse} because the status
     * travels on the returned {@link SimpleHTTPClient.DownloadResult} (the caller asserts
     * {@code result.getStatusCode()} directly).
     */
    public static SimpleHTTPClient.DownloadResult getToFile(String url, Map<String, String> headers, String suffix)
            throws IOException {
        TestContext.remove(HTTP_RESPONSE_KEY);
        return SimpleHTTPClient.getInstance().doGetToFile(url, headers, suffix);
    }

    public static HttpResponse delete(String url, Map<String, String> headers) throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doDelete(url, headers));
    }

    public static HttpResponse post(String url, Map<String, String> headers, String payload, String contentType)
            throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doPost(url, headers, payload, contentType));
    }

    public static HttpResponse put(String url, Map<String, String> headers, String payload, String contentType)
            throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doPut(url, headers, payload, contentType));
    }

    public static HttpResponse postMultipart(String url, Map<String, String> headers, Map<String, File> files,
                                             Map<String, String> formFields) throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doPostMultipartWithFiles(url, headers, files, formFields));
    }

    public static HttpResponse putMultipart(String url, Map<String, String> headers, Map<String, File> files,
                                            Map<String, String> formFields) throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doPutMultipartWithFiles(url, headers, files, formFields));
    }

    public static HttpResponse head(String url, Map<String, String> headers) throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doHead(url, headers));
    }

    public static HttpResponse patch(String url, Map<String, String> headers, String payload, String contentType)
            throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance().doPatch(url, headers, payload, contentType));
    }

    public static HttpResponse soap(String url, String payload, String soapAction, String adminUsername,
                                    String adminPassword) throws IOException {
        return execute(() -> SimpleHTTPClient.getInstance()
                .sendSoapRequest(url, payload, soapAction, adminUsername, adminPassword));
    }
}
