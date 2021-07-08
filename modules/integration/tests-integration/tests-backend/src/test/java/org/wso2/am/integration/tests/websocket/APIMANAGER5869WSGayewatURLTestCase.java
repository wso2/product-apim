/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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
 *
 */
package org.wso2.am.integration.tests.websocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIEndpointURLsDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * This class is used to test gateway urls of WS APIs
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIMANAGER5869WSGayewatURLTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(APIMANAGER5869WSGayewatURLTestCase.class);
    private final String API_NAME = "WSGayewatURLAPIName";
    private final String API_CONTEXT = "WSGayewatURLContext";
    private final String WS_API_NAME = "WSGayewatURLWSAPIName";
    private final String WS_API_CONTEXT = "WSGayewatURLWSContext";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String publisherURLHttps;
    private String storeURLHttp;
    private APIRequest apiCreationRequestBean;
    private List<APIOperationsDTO> resList;
    private String endpointUrl;
    private ServerConfigurationManager serverConfigurationManager;
    private String websocketAPIID;
    private String restAPIId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER5869WSGayewatURLTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
    }

    @Test(groups = {"wso2.am"}, description = "Sample API creation")
    public void testAPICreation() throws Exception {

        apiCreationRequestBean = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);
        apiCreationRequestBean.setVersion(API_VERSION);
        apiCreationRequestBean.setProvider(user.getUserName());
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/add");
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        //define resources
        resList = new ArrayList<>();
        resList.add(apiOperationsDTO);

        apiCreationRequestBean.setOperationsDTOS(resList);

        //add test api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        restAPIId = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(restAPIId, APILifeCycleAction.PUBLISH.getAction(), null);

        //publish the api
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(description = "Publish WebSocket API", dependsOnMethods = "testAPICreation")
    public void publishWebSocketAPI() throws Exception {

        String provider = user.getUserName();

        URI endpointUri = new URI("ws://echo.websocket.org");

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(WS_API_NAME, WS_API_CONTEXT, endpointUri, endpointUri);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(TIER_COLLECTION);
        apiRequest.setProvider(provider);
        apiRequest.setType("WS");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        websocketAPIID = addAPIResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(websocketAPIID, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(websocketAPIID, APILifeCycleAction.PUBLISH.getAction(), null);
        waitForAPIDeploymentSync(user.getUserName(), WS_API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(provider, WS_API_NAME, API_VERSION);

        // replace port with inbound endpoint port
        String apiEndPoint;
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndPoint = getWebSocketAPIInvocationURL(WS_API_CONTEXT, API_VERSION);
        } else {
            apiEndPoint = getWebSocketTenantAPIInvocationURL(WS_API_CONTEXT, API_VERSION, user.getUserDomain());
        }
        log.info("API Endpoint URL" + apiEndPoint);

        APIListDTO apiPublisherAllAPIs = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, apiPublisherAllAPIs),
                "Published API is visible in API Publisher.");
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO restAPIStoreAllAPIs;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs();
        } else {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs(user.getUserDomain());
        }
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifierWebSocket, restAPIStoreAllAPIs),
                "Published API is visible in API Store.");
    }

    @Test(groups = {"wso2.am"}, description = "Test API gateway urls", dependsOnMethods = "publishWebSocketAPI")
    public void testApiGatewayUrlsTest() throws Exception {
            APIDTO restAPIStoreAPI = restAPIStore.getAPI(restAPIId);
        Assert.assertNotNull(restAPIStoreAPI);
        Assert.assertNotNull(restAPIStoreAPI.getEndpointURLs());
        for (APIEndpointURLsDTO endpointURL : restAPIStoreAPI.getEndpointURLs()) {
            Assert.assertNotNull(endpointURL.getUrLs());
            Assert.assertNotNull(endpointURL.getUrLs().getHttp());
            Assert.assertNotNull(endpointURL.getUrLs().getHttps());
            if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
                assertTrue(endpointURL.getUrLs().getHttp().matches("http://(.)+:[0-9]+/" + API_CONTEXT + "/" + API_VERSION));
                assertTrue(endpointURL.getUrLs().getHttps()
                        .matches("https://(.)+:[0-9]+/" + API_CONTEXT + "/" + API_VERSION));
            } else {
                assertTrue(endpointURL.getUrLs().getHttp()
                        .matches("http://(.)+:[0-9]+/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION));
                assertTrue(endpointURL.getUrLs().getHttps()
                        .matches("https://(.)+:[0-9]+/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION ));
            }
        }
        APIDTO wsApidto = restAPIStore.getAPI(websocketAPIID);
        Assert.assertNotNull(wsApidto);
        Assert.assertNotNull(wsApidto.getEndpointURLs());
        Assert.assertTrue(!wsApidto.getEndpointURLs().isEmpty());
        for (APIEndpointURLsDTO endpointURL : wsApidto.getEndpointURLs()) {
            Assert.assertNotNull(endpointURL.getUrLs());
            Assert.assertNotNull(endpointURL.getUrLs().getWs());
            log.info("API Endpoint URL = " + endpointURL.getUrLs().getWs());
            if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
                assertTrue(endpointURL.getUrLs().getWs()
                        .matches("ws://(.)+:[0-9]+/" + WS_API_CONTEXT + "/" + API_VERSION ), "websocketAPI gateway url = " + endpointURL.getUrLs().getWs());
            } else {
                assertTrue(endpointURL.getUrLs().getWs()
                        .matches("ws://(.)+:[0-9]+/t/wso2.com/" + WS_API_CONTEXT + "/" + API_VERSION ), "websocketAPI gateway url = " + endpointURL.getUrLs().getWs());
            }

        }

    }

    @Test(groups = {"wso2.am"}, description = "Test WS API gateway urls", dependsOnMethods =
            "testApiGatewayUrlsTest",enabled = false)
    public void testApiGatewayUrlsAfterConfigChangeTest() throws Exception {
        //change the api-manager.xml for new gateway urls
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            serverConfigurationManager.applyConfiguration(new File(
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "webSocketTest"
                            + File.separator + "deployment.toml"));
        }
        APIDTO restAPIStoreAPI = restAPIStore.getAPI(restAPIId);
        Assert.assertNotNull(restAPIStoreAPI);
        Assert.assertNotNull(restAPIStoreAPI.getEndpointURLs());
        Assert.assertTrue(!restAPIStoreAPI.getEndpointURLs().isEmpty());
        for (APIEndpointURLsDTO endpointURL : restAPIStoreAPI.getEndpointURLs()) {
            Assert.assertNotNull(endpointURL.getUrLs());
            Assert.assertNull(endpointURL.getUrLs().getHttp());
            Assert.assertNotNull(endpointURL.getUrLs().getHttps());
            if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
                assertTrue(
                        endpointURL.getUrLs().getHttps().contains("https://serverhost:9898/" + API_CONTEXT + "/" + API_VERSION));
                assertFalse(endpointURL.getUrLs().getHttp()
                        .matches("http://(.)+:[0-9]+/" + API_CONTEXT + "/" + API_VERSION));
            } else {
                assertTrue(endpointURL.getUrLs().getHttps()
                        .contains("https://serverhost:9898/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION));
                assertFalse(endpointURL.getUrLs().getHttp()
                        .matches("http://(.)+:[0-9]+/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION ));
            }
        }
        APIDTO wsAPIDto = restAPIStore.getAPI(websocketAPIID);
        Assert.assertNotNull(wsAPIDto);
        Assert.assertNotNull(wsAPIDto.getEndpointURLs());
        for (APIEndpointURLsDTO endpointURL : wsAPIDto.getEndpointURLs()) {
            Assert.assertNotNull(endpointURL.getUrLs());
            Assert.assertNotNull(endpointURL.getUrLs().getWs());
            if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
                assertTrue(
                        endpointURL.getUrLs().getWs().contains("ws://localhost:9099/" + WS_API_CONTEXT + "/" + API_VERSION));
            } else {
                assertTrue(endpointURL.getUrLs().getWs()
                        .contains("ws://localhost:9099/t/wso2.com/" + WS_API_CONTEXT + "/" + API_VERSION));
            }

        }

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        super.cleanUp();
//        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
//            serverConfigurationManager.restoreToLastConfiguration(true);
//        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},};
    }

}
