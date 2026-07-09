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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher-plane glue for the API endpoints sub-resource ({@code /apis/{apiId}/endpoints}) — add, list, get,
 * update and delete named endpoints on an API. Ports the endpoint-CRUD half of AIAPITestCase
 * (testAddAiApiEndpoint / testGetApiEndpoints / testGetApiEndpoint / testUpdateApiEndpoint /
 * testDeleteApiEndpoint). Endpoints are especially relevant for AIAPI-subtype APIs, which can carry several
 * named production/sandbox endpoints (the basis for round-robin/failover routing), but the resource is
 * general to any API. All operations use the publisher token; ids are resolved from and stored to context.
 */
public class ApiEndpointSteps {

    private String getBaseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    private Map<String, String> publisherJsonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        return headers;
    }

    /**
     * POST /apis/{apiId}/endpoints — adds a named endpoint (body = APIEndpointDTO: name, deploymentStage,
     * endpointConfig). Stores the created endpoint id under {@code idKey} on 2xx.
     */
    @When("I add an endpoint to API {string} with payload {string} as {string}")
    public void iAddApiEndpoint(String apiId, String payload, String idKey) throws IOException {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApiEndpointsURL(getBaseUrl(), actualApiId), publisherJsonHeaders(), jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            TestContext.set(idKey, Utils.extractValueFromPayload(response.getData(), "id"));
        }
    }

    /** GET /apis/{apiId}/endpoints — lists all endpoints of the API. */
    @When("I retrieve the endpoints of API {string}")
    public void iRetrieveApiEndpoints(String apiId) throws IOException {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApiEndpointsURL(getBaseUrl(), actualApiId), publisherJsonHeaders());
        TestContext.set("httpResponse", response);
    }

    /** GET /apis/{apiId}/endpoints/{endpointId} — retrieves a single endpoint by id. */
    @When("I retrieve endpoint {string} of API {string}")
    public void iRetrieveApiEndpoint(String endpointId, String apiId) throws IOException {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualEndpointId = Utils.resolveFromContext(endpointId).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApiEndpointByIdURL(getBaseUrl(), actualApiId, actualEndpointId),
                        publisherJsonHeaders());
        TestContext.set("httpResponse", response);
    }

    /** PUT /apis/{apiId}/endpoints/{endpointId} — updates a single endpoint (body = full APIEndpointDTO). */
    @When("I update endpoint {string} of API {string} with payload {string}")
    public void iUpdateApiEndpoint(String endpointId, String apiId, String payload) throws IOException {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualEndpointId = Utils.resolveFromContext(endpointId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(Utils.getApiEndpointByIdURL(getBaseUrl(), actualApiId, actualEndpointId),
                        publisherJsonHeaders(), jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** DELETE /apis/{apiId}/endpoints/{endpointId} — removes a single endpoint. */
    @When("I delete endpoint {string} of API {string}")
    public void iDeleteApiEndpoint(String endpointId, String apiId) throws IOException {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualEndpointId = Utils.resolveFromContext(endpointId).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getApiEndpointByIdURL(getBaseUrl(), actualApiId, actualEndpointId),
                        publisherJsonHeaders());
        TestContext.set("httpResponse", response);
    }
}
