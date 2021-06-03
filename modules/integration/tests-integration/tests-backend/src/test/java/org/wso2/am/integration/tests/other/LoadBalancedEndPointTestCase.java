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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import java.util.*;
import java.util.List;

import javax.ws.rs.core.Response;

import static org.testng.Assert.*;

public class LoadBalancedEndPointTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(LoadBalancedEndPointTestCase.class);
    private String apiName = "LoadBalanacedAPITestCase";
    private String context = "LoadBalancedAPI";
    private String version = "1.0.0";
    private String providerName;
    private String tier= APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String resTier= APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED;
    private String appTier= APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED;
    private String endPointType = "load_balance";
    private String applicationName = "LoadBalanceAPIApplication";
    private String apiNameSandbox = "LoadBalanceSandboxAPI";
    private String contextSandbox = "LoadBalanceSandboxAPIContext";
    private String applicationNameSandbox = "LoadBalancedSandboxAPIApplication";
    private String firstProductionEndPoint = "";
    private String secondProductionEndPoint = "";
    private String thirdProductionEndPoint = "";
    private String productionEndpointPrefix = "HelloWSO2 from File ";
    private String gatewayUrl;
    private List<APIOperationsDTO> apiOperationsDTOS;
    private String apiId;
    private String sandboxApiId;
    private String appId;
    private String sandboxAppId;

    @Factory(dataProvider = "userModeDataProvider")
    public LoadBalancedEndPointTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        log.info("Test Starting user mode: " + userMode);
        //add resource
        apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.authType("Application & Application User");
        apiOperationsDTO.setThrottlingPolicy(resTier);
        apiOperationsDTO.setTarget("/name");
        apiOperationsDTOS.add(apiOperationsDTO);
        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(groups = { "wso2.am" }, description = "Test Load Balance End Points", priority = 1)
    public void testCreateApiWithDifferentProductionEndpoints() throws Exception {

        String description = "LoadBalancedEnd-point";

        firstProductionEndPoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME;
        secondProductionEndPoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME;
        thirdProductionEndPoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME;

        List<String> endpointLB = new ArrayList<>();
        endpointLB.add(firstProductionEndPoint);
        endpointLB.add(secondProductionEndPoint);
        endpointLB.add(thirdProductionEndPoint);

        // create request for api
        APIRequest apiRequest = new APIRequest(apiName, context, version, endpointLB, null);
        apiRequest.setTiersCollection(tier);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(description);
        apiRequest.setEndpointType(endPointType);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
    }

    @Test(groups = { "wso2.am" }, description = "Verify Round Robin Algorithm by Invoking the Production Endpoint API",
            dependsOnMethods = { "testCreateApiWithDifferentProductionEndpoints" })
    public void testRoundRobinAlgorithmInProductionEndpoints() throws Exception {

        String accessUrl = gatewayUrl + context + "/" + version + "/name";

        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName, appTier, "", "Test App");
        appId =  applicationDTO.getApplicationId();
        subscribeToAPIUsingRest(apiId, appId, tier, restAPIStore);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        //generate keys
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> applicationHeader = new HashMap<String, String>();
        applicationHeader.put("Authorization", " Bearer " + accessToken);

        //Verify Round Robin Algorithm by invoking the api multiple times
        HttpResponse apiInvokeResponse;
        int numberOfEndpoints = 3;
        int requestCount = 10;
        waitForAPIDeployment();
        for (int requestNumber = 1; requestNumber < requestCount; requestNumber++) {
            apiInvokeResponse = HttpRequestUtil.doGet(accessUrl, applicationHeader);
            assertEquals(apiInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatch");

            int remainder = requestNumber % numberOfEndpoints;
            if (remainder == 0) {
                log.info(apiInvokeResponse.getData());
                assertEquals(apiInvokeResponse.getData(), productionEndpointPrefix + "3",
                        "Error in Round Robin Algorithm in cycle " + requestNumber);

            } else {
                log.info(apiInvokeResponse.getData());
                assertEquals(apiInvokeResponse.getData(), productionEndpointPrefix + remainder,
                        "Error in Round Robin Algorithm in Cycle " + requestNumber);
            }
        }
    }

    @Test(groups = {"wso2.am" }, description = "Test Load balanced function with both Production and Sandbox Endpoints",
            priority = 2)
    public void testCreateApiWithBothProdAndSandboxEndpoints() throws Exception {

        String descriptionSandbox = "SandboxEnd-point";

        firstProductionEndPoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME;
        secondProductionEndPoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME;
        thirdProductionEndPoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME;

        //Add production endpoints
        List<String> endpointProd = new ArrayList<String>();
        endpointProd.add(firstProductionEndPoint);
        endpointProd.add(secondProductionEndPoint);
        endpointProd.add(thirdProductionEndPoint);

        //add sandbox endpoints
        String firstSandboxEndpoint =
                backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME;
        String secondSandboxEndpoint =
                backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME;
        String thirdSandboxEndpoint =
                backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME;

        List<String> endpointSandbox = new ArrayList<String>();
        endpointSandbox.add(firstSandboxEndpoint);
        endpointSandbox.add(secondSandboxEndpoint);
        endpointSandbox.add(thirdSandboxEndpoint);

        APIRequest apiRequestSB = new APIRequest(apiNameSandbox, contextSandbox, version, endpointProd,
                endpointSandbox);
        apiRequestSB.setTiersCollection(tier);
        apiRequestSB.setProvider(providerName);
        apiRequestSB.setOperationsDTOS(apiOperationsDTOS);
        apiRequestSB.setDescription(descriptionSandbox);
        apiRequestSB.setEndpointType(endPointType);

        sandboxApiId = createAndPublishAPIUsingRest(apiRequestSB, restAPIPublisher, false);
    }

    @Test(groups = { "wso2.am" }, description = "Verify Round Robin Algorithm by Invoking the Sandbox Endpoint API",
            dependsOnMethods = { "testCreateApiWithBothProdAndSandboxEndpoints" })
    public void testRoundRobinAlgorithmInProductionAndSandboxEndpoints() throws Exception {

        String webAppSandboxResponsePrefix = "HelloWSO2 from File ";
        String webAppSandboxResponseSuffix = "_Sandbox";

        String accessUrl = gatewayUrl + contextSandbox + "/" + version + "/name";

        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationNameSandbox, appTier, "",
                "Test Application");
        sandboxAppId =  applicationDTO.getApplicationId();
        subscribeToAPIUsingRest(sandboxApiId, sandboxAppId, tier, restAPIStore);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        //generate production endpoint key
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(sandboxAppId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessTokenProduction = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> applicationHeaderProduction = new HashMap<String, String>();
        applicationHeaderProduction.put("Authorization", " Bearer " + accessTokenProduction);

        //generate sandbox endpoint key
        ApplicationKeyDTO applicationKeyDTOSandBox = restAPIStore.generateKeys(sandboxAppId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);
        String accessTokenSandbox = applicationKeyDTOSandBox.getToken().getAccessToken();

        Map<String, String> applicationHeaderSandbox = new HashMap<String, String>();
        applicationHeaderSandbox.put("Authorization", " Bearer " + accessTokenSandbox);

        //Verify Round Robin Algorithm by invoking the api multiple times
        HttpResponse apiSandboxInvokeResponse;
        HttpResponse apiProductionInvokeResponse;

        int requestCount = 10;
        int numberOfProductionEndpoints = 3;
        int numberOfSandboxEndpoints = 3;
        int requestNumber;
        waitForAPIDeployment();
        //production end point api invoke with production key
        for (requestNumber = 1; requestNumber < requestCount; requestNumber++) {
            apiProductionInvokeResponse = HttpRequestUtil.doGet(accessUrl, applicationHeaderProduction);
            assertEquals(apiProductionInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatched in Production API invoke");

            int remainder = requestNumber % numberOfProductionEndpoints;
            if (remainder == 0) {
                log.info(apiProductionInvokeResponse.getData());
                assertEquals(apiProductionInvokeResponse.getData(), productionEndpointPrefix + "3",
                        "Error in Round Robin Algorithm in cycle ");
            } else {
                log.info(apiProductionInvokeResponse.getData());
                assertEquals(apiProductionInvokeResponse.getData(), productionEndpointPrefix + remainder,
                        "Error in Production Endpoint Round Robin Algorithm in request: " + requestNumber);
            }
        }
        //sandbox endpoint api invoke with sandbox key
        for (requestNumber = 1; requestNumber < requestCount; requestNumber++) {
            apiSandboxInvokeResponse = HttpRequestUtil.doGet(accessUrl, applicationHeaderSandbox);
            assertEquals(apiSandboxInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatch in Sandbox Endpoint invoke");

            int remainder = requestNumber % numberOfSandboxEndpoints;
            if (remainder == 0) {
                log.info(apiSandboxInvokeResponse.getData());
                assertEquals(apiSandboxInvokeResponse.getData(), webAppSandboxResponsePrefix + "3" +
                        webAppSandboxResponseSuffix, "Error in Round Robin Algorithm in cycle ");
            } else {
                log.info(apiSandboxInvokeResponse.getData());
                assertEquals(apiSandboxInvokeResponse.getData(), webAppSandboxResponsePrefix + remainder +
                        webAppSandboxResponseSuffix, "Error in Round Robin Algorithm in Cycle "
                        + requestNumber);
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId);
        restAPIStore.deleteApplication(sandboxAppId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(sandboxApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(sandboxApiId);
    }
}
