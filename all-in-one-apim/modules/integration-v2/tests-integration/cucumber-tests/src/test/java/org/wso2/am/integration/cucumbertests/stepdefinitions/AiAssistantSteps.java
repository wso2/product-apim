/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps for the AI assistant REST endpoints (Marketplace Assistant on the DevPortal plane; Design Assistant on the
 * Publisher plane). Each step issues the request through {@link Requests} (which publishes the result to the shared
 * {@code httpResponse} context key) and asserts nothing itself — the feature asserts the exact status/body, because
 * the expected outcome varies by configuration (400 bad request, 500 on a broken implementation class, 204 when the
 * assistant is unconfigured, 201/200 against a configured backend).
 */
public class AiAssistantSteps {

    private String getBaseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    private String resolvePayload(String payloadKey) {
        return Utils.resolveContextPlaceholders(TestContext.resolve(payloadKey).toString());
    }

    private Map<String, String> authHeaders(String bearerToken) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + bearerToken);
        return headers;
    }

    @When("I send an AI Marketplace Assistant chat request with payload {string}")
    public void sendMarketplaceAssistantChat(String payloadKey) throws IOException {
        // Requests.post clears, calls, publishes the result to "httpResponse" and returns it — the feature's
        // "Then The response status code should be {int}" reads it back.
        Requests.post(Utils.getMarketplaceAssistantChatURL(getBaseUrl()), authHeaders(Identity.devportalToken()),
                resolvePayload(payloadKey), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    @When("I request the AI Marketplace Assistant API count")
    public void requestMarketplaceAssistantApiCount() throws IOException {
        Requests.get(Utils.getMarketplaceAssistantApiCountURL(getBaseUrl()), authHeaders(Identity.devportalToken()));
    }

    @When("I send an AI Design Assistant generate-payload request with payload {string}")
    public void sendDesignAssistantGeneratePayload(String payloadKey) throws IOException {
        Requests.post(Utils.getDesignAssistantGeneratePayloadURL(getBaseUrl()),
                authHeaders(Identity.publisherToken()), resolvePayload(payloadKey),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    @When("I send an AI Design Assistant chat request with payload {string}")
    public void sendDesignAssistantChat(String payloadKey) throws IOException {
        Requests.post(Utils.getDesignAssistantChatURL(getBaseUrl()), authHeaders(Identity.publisherToken()),
                resolvePayload(payloadKey), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    @When("I send an AI API Chat {word} request for API {string} with payload {string}")
    public void sendApiChatRequest(String action, String apiIdKey, String payloadKey) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Requests.post(Utils.getApiChatURL(getBaseUrl(), apiId, action), authHeaders(Identity.devportalToken()),
                resolvePayload(payloadKey), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }
}
