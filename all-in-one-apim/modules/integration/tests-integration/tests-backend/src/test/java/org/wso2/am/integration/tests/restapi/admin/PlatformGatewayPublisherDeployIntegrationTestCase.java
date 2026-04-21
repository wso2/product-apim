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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.restapi.admin;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CreatePlatformGatewayRequestDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayResponseWithTokenDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end checks for Publisher revision deploy to a Universal (platform) gateway by environment
 * name, and visibility of that deployment on internal gateway sync APIs ({@code GET /deployments},
 * {@code POST /deployments/fetch-batch}).
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class PlatformGatewayPublisherDeployIntegrationTestCase extends APIMIntegrationBaseTest {

    private static final String INTERNAL_DATA_V1 = "https://localhost:9943/internal/data/v1";
    private static final long DEPLOYMENT_SYNC_TIMEOUT_MS = 120_000L;
    private static final long POLL_INTERVAL_MS = 500L;

    private String apiEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public PlatformGatewayPublisherDeployIntegrationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
    }

    private String uniqueGatewayName() {
        return "igw-" + System.currentTimeMillis();
    }

    private CreatePlatformGatewayRequestDTO newCreateRequest(String name) {
        CreatePlatformGatewayRequestDTO dto = new CreatePlatformGatewayRequestDTO();
        dto.setName(name);
        dto.setDisplayName("Publisher deploy integration gateway");
        dto.setDescription("PlatformGatewayPublisherDeployIntegrationTestCase");
        dto.setVhost(URI.create("https://localhost:9999"));
        return dto;
    }

    private String findDeploymentIdForApi(String registrationToken, String apiUuid) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", registrationToken);
        HttpResponse res = HTTPSClientUtils.doGet(INTERNAL_DATA_V1 + "/deployments", headers);
        if (res.getResponseCode() != HttpStatus.SC_OK) {
            return null;
        }
        JSONObject json = new JSONObject(res.getData());
        JSONArray arr = json.optJSONArray("deployments");
        if (arr == null) {
            return null;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject d = arr.getJSONObject(i);
            if (apiUuid.equals(d.optString("artifactId"))) {
                String id = d.optString("deploymentId");
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    private String awaitDeploymentIdForApi(String registrationToken, String apiUuid, long timeoutMs)
            throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String id = findDeploymentIdForApi(registrationToken, apiUuid);
            if (id != null) {
                return id;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        Assert.fail("Timed out waiting for internal /deployments to list API " + apiUuid);
        return null;
    }

    private void undeployRevisionFromUniversal(String apiId, String revisionUUID, String gatewayName)
            throws Exception {

        List<APIRevisionDeployUndeployRequest> list = new ArrayList<>();
        APIRevisionDeployUndeployRequest u = new APIRevisionDeployUndeployRequest();
        u.setName(gatewayName);
        u.setVhost(null);
        u.setDisplayOnDevportal(true);
        list.add(u);
        HttpResponse r = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID, list);
        Assert.assertEquals(r.getResponseCode(), HttpStatus.SC_CREATED,
                "Undeploy failed: " + r.getData());
    }

    private void cleanupApiAndGateway(String apiId, String revisionUUID, String gatewayName, String gatewayId) {
        if (apiId != null && revisionUUID != null && gatewayName != null) {
            try {
                undeployRevisionFromUniversal(apiId, revisionUUID, gatewayName);
            } catch (Exception ignored) {
            }
            try {
                restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
            } catch (Exception ignored) {
            }
        }
        if (apiId != null) {
            try {
                restAPIPublisher.deleteAPI(apiId);
            } catch (Exception ignored) {
            }
        }
        if (gatewayId != null) {
            try {
                restAPIAdmin.deletePlatformGateway(gatewayId);
            } catch (ApiException ignored) {
            }
        }
    }

    @Test
    public void testPublisherDeployToUniversalSyncsInternalDeploymentsAndFetchBatch() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();
        String token = created.getData().getRegistrationToken();

        String apiId = null;
        String revisionUUID = null;
        try {
            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgPublisherDeployAPI_" + suffix, "pgpubrev" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("platform universal deploy");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());

            String deploymentId = awaitDeploymentIdForApi(token, apiId, DEPLOYMENT_SYNC_TIMEOUT_MS);
            Assert.assertNotNull(deploymentId);

            Map<String, String> headers = new HashMap<>();
            headers.put("api-key", token);
            headers.put("Content-Type", "application/json");
            String payload = "{\"deploymentIds\":[\"" + deploymentId + "\"]}";
            HttpResponse batch =
                    HTTPSClientUtils.doPost(INTERNAL_DATA_V1 + "/deployments/fetch-batch", headers, payload);
            Assert.assertEquals(batch.getResponseCode(), HttpStatus.SC_OK,
                    "fetch-batch should succeed for a listed deployment id");
            String body = batch.getData();
            Assert.assertNotNull(body);
            // Body is read as UTF-8 text by HTTPSClientUtils; tar entries still contain ASCII ustar magic.
            Assert.assertTrue(body.contains("ustar"), "fetch-batch should return a tar archive payload");
        } finally {
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }

    @Test
    public void testDeleteUniversalGatewayWithRevisionDeployedReturnsConflict() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();

        String apiId = null;
        String revisionUUID = null;
        try {
            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgGatewayDeleteAPI_" + suffix, "pgdelgw" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("block gateway delete");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());

            try {
                restAPIAdmin.deletePlatformGateway(gatewayId);
                Assert.fail("Expected conflict when deleting gateway with deployed revisions");
            } catch (ApiException e) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT,
                        "Deleting gateway with active revision deployments should return 409");
            }
        } finally {
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }
}
