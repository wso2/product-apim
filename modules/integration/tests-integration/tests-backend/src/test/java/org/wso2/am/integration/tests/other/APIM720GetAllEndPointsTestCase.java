/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIEndpointURLsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIURLsDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


//APIM2-720:Get all endpoint URLs of a API through the store rest api
//APIM2-722:Add a comment on an API through the store api manager
public class APIM720GetAllEndPointsTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM720GetAllEndPointsTestCase.class);
    private static final String apiName = "EndPointTestAPI";
    private static final String apiVersion = "1.0.0";
    private static final String apiContext = "endpointtestapi";
    private final String tags = "document";
    private String tier= APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String resTier= APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED;
    private final String description = "testApi";
    private String apiProvider;
    private final String endPointType = "http";
    private String gatewayUrl;
    private String apiID;
    private APIIdentifier apiIdentifier;


    @Factory(dataProvider = "userModeDataProvider")
    public APIM720GetAllEndPointsTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();
        String uri = "customers/{id}/";
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", resTier, uri));
        String endpointProduction = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice/production";
        String endpointSandbox = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice/sandbox";

        List<String> prodEndpointList = new ArrayList<>();
        List<String> sandboxEndpointList = new ArrayList<>();
        prodEndpointList.add(endpointProduction);
        sandboxEndpointList.add(endpointSandbox);

        for (int i = 0; i < 3; i++) {
            prodEndpointList.add(endpointProduction + "_" + i);
            sandboxEndpointList.add(endpointSandbox + "_" + i);
        }

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVersion, prodEndpointList, sandboxEndpointList);
        apiRequest.setEndpointType(endPointType);
        apiRequest.setTags(tags);
        apiRequest.setTier(tier);
        apiRequest.setDescription(description);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);

        HttpResponse apiCreateResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(apiCreateResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(), "Error when creating API: "
                + apiName);
        apiID = apiCreateResponse.getData();

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiID);
        assertEquals(getAPIResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error when retrieving API: " + apiName);

        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        //publish API
        HttpResponse changeLCStatusResponse = restAPIPublisher.changeAPILifeCycleStatus(apiID,
                APILifeCycleAction.PUBLISH.getAction(), null);

        assertEquals(changeLCStatusResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Error when publishing the API: " + apiName);

        waitForAPIDeploymentSync(apiProvider, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);
        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
    }

    @Test(description = "Get All Endpoints")
    public void getAllEndpointUrlsTest() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiID);
        assertEquals(getAPIResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Error when " +
                "retrieving API: " + apiName);
        Gson gson = new Gson();
        APIDTO api = gson.fromJson(getAPIResponse.getData(), APIDTO.class);
        LinkedTreeMap endpointConfig = (LinkedTreeMap) api.getEndpointConfig();
        List sandboxEndpoints = (ArrayList) endpointConfig.get("sandbox_endpoints");
        List productionEndpoints = (ArrayList) endpointConfig.get("production_endpoints");

        assertEquals(productionEndpoints.size(), 4, "Mismatch in the number of production endpoints");
        assertEquals(sandboxEndpoints.size(), 4, "Mismatch in the number of sandbox endpoints");

        assertEquals(endpointConfig.get("endpoint_type"), "load_balance", "Endpoint type mismatched");
        assertEquals(endpointConfig.get("algoCombo"), "org.apache.synapse.endpoints.algorithms.RoundRobin",
                "Endpoint selection algorithm mismatched");

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiFromStore = restAPIStore.getAPI(apiID);
        List<APIEndpointURLsDTO> endpointURLs = apiFromStore.getEndpointURLs();
        APIURLsDTO urls = endpointURLs.get(0).getUrLs();

        assertEquals(endpointURLs.get(0).getEnvironmentName(), Constants.GATEWAY_ENVIRONMENT, "Mismatch in " +
                "environment name");
        assertEquals(endpointURLs.get(0).getEnvironmentType(), "hybrid", "Mismatch in environment type");
        assertTrue(urls.getHttp() != null, "HTTP URL is not available");
        assertTrue(urls.getHttps() != null, "HTTPS URL is not available");
        assertEquals(urls.getHttp(), gatewayUrl + apiContext + "/" + apiVersion, "Error in HTTP " +
                "endpoint URL");
        assertEquals(urls.getHttps(), gatewayHTTPSURL + apiContext + "/" + apiVersion, "Error in HTTPS" +
                "endpoint URL");
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponse = restAPIStore.getAPI(apiID);
        assertNotNull(apiResponse);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiID);
    }
}
