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

package org.wso2.am.integration.tests.mcp;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationMappingDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.BackendOperationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerOperationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO.TokenTypeEnum;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Regression test for issue #17568 — "900906 error when API is associated with an MCP server".
 * <p>
 * Reproduces the reported flow end-to-end against a running gateway:
 * <ol>
 *     <li>Create an HTTP API with one resource {@code GET /res1}, deploy a revision and publish it.</li>
 *     <li>Generate an MCP server FROM that API (tool mapped to {@code GET /res1}).</li>
 *     <li>Add a second resource {@code GET /res2} to the API and update it.</li>
 *     <li>Deploy a new revision of the API.</li>
 *     <li>Invoke {@code /res1} and {@code /res2} through the gateway.</li>
 * </ol>
 * Before the fix, the API's URI templates were skipped on update whenever the API had an
 * associated MCP server, so {@code /res2} never reached the gateway and returned
 * HTTP 403 / code 900906. This test asserts {@code /res2} is matched at the gateway
 * (HTTP 200, never 900906). It also guards the complementary behaviour: removing the
 * MCP-mapped {@code /res1} must still be rejected with HTTP 403 / code 904001.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class MCPAssociatedAPIResourceUpdateTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(MCPAssociatedAPIResourceUpdateTestCase.class);

    private static final String GROUP_WSO2_AM = "wso2.am";

    private static final String API_NAME = "MCPAssociatedResourceAPI";
    private static final String API_CONTEXT = "mcpAssociatedResourceApi";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION = "API used to verify resource updates when associated with an MCP server";

    private static final String RES_1 = "/res1";
    private static final String RES_2 = "/res2";
    private static final String VERB_GET = "GET";
    private static final String AUTH_TYPE_APP_AND_USER = "Application & Application User";
    private static final String TIER_UNLIMITED = "Unlimited";

    private static final String MCP_SERVER_NAME = "MCPAssociatedResourceServer";
    private static final String MCP_SERVER_CONTEXT = "mcpAssociatedResourceServer";

    private static final String APP_NAME = "MCPAssociatedResourceApp";
    private static final String APP_DESC = "Application for issue 17568 regression test";

    private static final String ERROR_CODE_NO_MATCHING_RESOURCE = "900906";
    private static final String ERROR_CODE_MCP_USAGE_FORBIDDEN = "904001";

    private static final int PORT_RANGE_START = 9950;
    private static final int PORT_RANGE_END_INCLUSIVE = 9999;
    private static final int WAIT_FOR_DEPLOYMENT_IN_MILLISECONDS = 5000;

    private static final String BACKEND_RESPONSE_BODY = "{\"status\":\"ok\"}";

    private String tenantDomain;
    private String apiId;
    private String mcpServerId;
    private String applicationId;
    private String accessToken;
    private int backendPort;
    private WireMockServer backend;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        tenantDomain = storeContext.getContextTenant().getDomain();
        backendPort = findAvailablePort(PORT_RANGE_START, PORT_RANGE_END_INCLUSIVE);
        backend = new WireMockServer(options().port(backendPort));
        backend.stubFor(get(urlPathMatching("/res.*"))
                .willReturn(aResponse().withStatus(HttpStatus.SC_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody(BACKEND_RESPONSE_BODY)));
        backend.start();
        log.info("Started backend WireMock on port " + backendPort);
    }

    @Test(groups = {GROUP_WSO2_AM},
            description = "Create an API with GET /res1, deploy, publish and generate an MCP server from it")
    public void testCreateApiAndGenerateMcpServerFromApi() throws Exception {

        String backendUrl = "http://localhost:" + backendPort;

        APIOperationsDTO res1 = buildResourceOperation(RES_1, VERB_GET);
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(backendUrl), new URL(backendUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setDescription(API_DESCRIPTION);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTier(TIER_UNLIMITED);
        apiRequest.setTiersCollection(TIER_UNLIMITED);
        apiRequest.setOperationsDTOS(Collections.singletonList(res1));

        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertNotNull(addResponse, "API creation failed - null response");
        apiId = addResponse.getData();
        Assert.assertNotNull(apiId, "API creation failed - API id is null");

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        WorkflowResponseDTO publishResponse = restAPIPublisher.changeAPILifeCycleStatus(apiId, "Publish");
        Assert.assertNotNull(publishResponse, "API lifecycle change to Published failed");
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        MCPServerDTO mcpServerRequest = new MCPServerDTO();
        mcpServerRequest.setName(MCP_SERVER_NAME);
        mcpServerRequest.setContext(MCP_SERVER_CONTEXT);
        mcpServerRequest.setVersion(API_VERSION);
        mcpServerRequest.setPolicies(Collections.singletonList(TIER_UNLIMITED));
        mcpServerRequest.setOperations(Collections.singletonList(
                buildToolOpForApi(apiId, RES_1, BackendOperationDTO.VerbEnum.GET)));

        MCPServerDTO mcpServer = restAPIPublisher.createMCPServerFromAPI(mcpServerRequest);
        Assert.assertNotNull(mcpServer, "MCP server generation from API failed - null response");
        mcpServerId = mcpServer.getId();
        Assert.assertNotNull(mcpServerId, "MCP server generation from API failed - MCP server id is null");
    }

    @Test(groups = {GROUP_WSO2_AM},
            description = "Add a new resource to the MCP-associated API, redeploy and invoke both resources "
                    + "through the gateway. The newly added resource must be matched (no 900906).",
            dependsOnMethods = {"testCreateApiAndGenerateMcpServerFromApi"})
    public void testAddResourceRedeployAndInvoke() throws Exception {

        // Add the second resource to the API (the API is now associated with an MCP server).
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);
        List<APIOperationsDTO> operations = new ArrayList<>(apidto.getOperations());
        operations.add(buildResourceOperation(RES_2, VERB_GET));
        apidto.setOperations(operations);
        APIDTO updated = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertTrue(containsResource(updated.getOperations(), RES_2),
                "API update did not persist the newly added resource " + RES_2);

        // Deploy a fresh revision so the gateway picks up the new resource.
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        Thread.sleep(WAIT_FOR_DEPLOYMENT_IN_MILLISECONDS);

        // Subscribe an application and obtain an access token.
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse appResponse =
                restAPIStore.createApplication(APP_NAME, APP_DESC,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, TokenTypeEnum.JWT);
        applicationId = appResponse.getData();

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse subscribeResponse =
                restAPIStore.createSubscription(apiId, applicationId,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        Assert.assertEquals(subscribeResponse.getResponseCode(), HttpStatus.SC_OK, "API subscription failed");

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKey = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKey.getToken().getAccessToken();

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        String invocationBase = getAPIInvocationURLHttps(API_CONTEXT, API_VERSION);

        // The original resource must keep working.
        HttpResponse res1Response = HTTPSClientUtils.doGet(invocationBase + RES_1, headers);
        assertResourceMatched(res1Response, RES_1);

        // The newly added resource must be matched at the gateway - this is the regression for #17568.
        HttpResponse res2Response = HTTPSClientUtils.doGet(invocationBase + RES_2, headers);
        assertResourceMatched(res2Response, RES_2);
    }

    @Test(groups = {GROUP_WSO2_AM},
            description = "Removing the MCP-mapped resource from the API must remain blocked (403 / 904001)",
            dependsOnMethods = {"testAddResourceRedeployAndInvoke"})
    public void testRemovingMcpMappedResourceIsBlocked() throws Exception {

        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);
        // Keep only /res2, attempting to drop the MCP-mapped /res1.
        apidto.setOperations(Collections.singletonList(buildResourceOperation(RES_2, VERB_GET)));

        try {
            restAPIPublisher.updateAPI(apidto, apiId);
            Assert.fail("Removing the MCP-mapped resource " + RES_1 + " should have been rejected");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN,
                    "Expected HTTP 403 when removing an MCP-mapped resource, got " + e.getCode());
            Assert.assertTrue(e.getResponseBody() != null
                            && e.getResponseBody().contains(ERROR_CODE_MCP_USAGE_FORBIDDEN),
                    "Expected error code " + ERROR_CODE_MCP_USAGE_FORBIDDEN + " in response, got: "
                            + e.getResponseBody());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        try {
            if (applicationId != null) {
                SubscriptionListDTO subscriptions =
                        restAPIStore.getAllSubscriptionsOfApplication(applicationId, tenantDomain);
                if (subscriptions != null && subscriptions.getList() != null) {
                    for (SubscriptionDTO subscription : subscriptions.getList()) {
                        restAPIStore.removeSubscription(subscription);
                    }
                }
                restAPIStore.deleteApplication(applicationId);
            }
            if (mcpServerId != null) {
                restAPIPublisher.deleteMCPServer(mcpServerId);
            }
            if (apiId != null) {
                restAPIPublisher.deleteAPI(apiId);
            }
        } finally {
            if (backend != null && backend.isRunning()) {
                backend.stop();
            }
        }
    }

    /**
     * Build a publisher API resource (URI template) operation.
     */
    private static APIOperationsDTO buildResourceOperation(String target, String verb) {

        APIOperationsDTO operation = new APIOperationsDTO();
        operation.setTarget(target);
        operation.setVerb(verb);
        operation.setAuthType(AUTH_TYPE_APP_AND_USER);
        operation.setThrottlingPolicy(TIER_UNLIMITED);
        return operation;
    }

    /**
     * Build a TOOL-feature MCP operation referencing an existing API's backend operation.
     */
    private static MCPServerOperationDTO buildToolOpForApi(String apiId, String target,
                                                           BackendOperationDTO.VerbEnum verb) {

        MCPServerOperationDTO op = new MCPServerOperationDTO();
        op.setFeature(MCPServerOperationDTO.FeatureEnum.TOOL);

        APIOperationMappingDTO mapping = new APIOperationMappingDTO();
        mapping.setApiId(apiId);

        BackendOperationDTO backendOperation = new BackendOperationDTO();
        backendOperation.setTarget(target);
        backendOperation.setVerb(verb);
        mapping.setBackendOperation(backendOperation);

        op.setApiOperationMapping(mapping);
        return op;
    }

    private static boolean containsResource(List<APIOperationsDTO> operations, String target) {

        if (operations == null) {
            return false;
        }
        for (APIOperationsDTO operation : operations) {
            if (operation != null && target.equals(operation.getTarget())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Assert that a gateway invocation matched a resource - i.e. it did not return the
     * "no matching resource" (900906) failure. A reachable backend returns HTTP 200.
     */
    private static void assertResourceMatched(HttpResponse response, String resource) {

        Assert.assertNotNull(response, "Gateway invocation of " + resource + " returned null response");
        String body = response.getData() == null ? "" : response.getData();
        Assert.assertFalse(body.contains(ERROR_CODE_NO_MATCHING_RESOURCE),
                "Resource " + resource + " was not matched at the gateway (900906). Issue #17568 regression. "
                        + "HTTP " + response.getResponseCode() + ", payload=" + body);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Resource " + resource + " invocation did not return HTTP 200. HTTP "
                        + response.getResponseCode() + ", payload=" + body);
    }

    private static int findAvailablePort(int start, int endInclusive) {

        for (int p = start; p <= endInclusive; p++) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress("localhost", p));
                return p;
            } catch (Exception ignore) {
                // try next port
            }
        }
        throw new IllegalStateException("No free port found in range " + start + "-" + endInclusive);
    }
}
