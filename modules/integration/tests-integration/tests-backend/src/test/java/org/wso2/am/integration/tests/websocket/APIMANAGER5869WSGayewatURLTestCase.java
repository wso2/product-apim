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
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
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

import static org.testng.Assert.*;

/**
 * This class is used to test gateway urls of WS APIs
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
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
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String endpointUrl;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER5869WSGayewatURLTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttps = publisherUrls.getWebAppURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttps);
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean res1 = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, "/add");
        resList.add(res1);

        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(description = "Publish WebSocket API", dependsOnMethods = "testAPICreation")
    public void publishWebSocketAPI() throws Exception {
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        String provider = user.getUserName();

        URI endpointUri = new URI("ws://echo.websocket.org");

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(WS_API_NAME, WS_API_CONTEXT, endpointUri, endpointUri);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(TIER_COLLECTION);
        apiRequest.setProvider(provider);
        apiRequest.setType("WS");
        apiPublisher.login(user.getUserName(), user.getPassword());
        HttpResponse addAPIResponse = apiPublisher.addAPI(apiRequest);

        verifyResponse(addAPIResponse);

        //publishing API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(WS_API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        waitForAPIDeploymentSync(user.getUserName(), WS_API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(provider, WS_API_NAME, API_VERSION);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        // replace port with inbound endpoint port
        String apiEndPoint;
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndPoint = getWebSocketAPIInvocationURL(WS_API_CONTEXT, API_VERSION);
        } else {
            apiEndPoint = getWebSocketTenantAPIInvocationURL(WS_API_CONTEXT, API_VERSION, user.getUserDomain());
        }
        log.info("API Endpoint URL" + apiEndPoint);

        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, publisherAPIList),
                "Published API is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, storeAPIList),
                "Published API is visible in API Store.");
    }

    @Test(groups = { "wso2.am" }, description = "Test API gateway urls", dependsOnMethods = "publishWebSocketAPI")
    public void testApiGatewayUrlsTest() throws Exception {
        String provider = user.getUserName();
        HttpResponse serviceResponse = HTTPSClientUtils
                .doGet(getStoreURLHttps() + "store/apis/info?name=" + API_NAME + "&version=" + API_VERSION
                        + "&provider=" + provider, null);
        assertEquals(serviceResponse.getResponseCode(), 200, "HTTP status code mismatched");

        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            assertTrue(serviceResponse.getData()
                    .matches("(.)*\"http://(.)+:[0-9]+/" + API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
            assertTrue(serviceResponse.getData()
                    .matches("(.)*\"https://(.)+:[0-9]+/" + API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
        } else {
            assertTrue(serviceResponse.getData()
                    .matches("(.)*\"http://(.)+:[0-9]+/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
            assertTrue(serviceResponse.getData()
                    .matches("(.)*\"https://(.)+:[0-9]+/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
        }
        serviceResponse = HTTPSClientUtils
                .doGet(getStoreURLHttps() + "store/apis/info?name=" + WS_API_NAME + "&version=" + API_VERSION
                        + "&provider=" + provider, null);
        assertEquals(serviceResponse.getResponseCode(), 200, "HTTP status code mismatched");
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            assertTrue(serviceResponse.getData()
                    .matches("(.)*\"ws://(.)+:[0-9]+/" + WS_API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
        } else {
            assertTrue(serviceResponse.getData()
                    .matches("(.)*\"ws://(.)+:[0-9]+/t/wso2.com/" + WS_API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
        }

    }

    @Test(groups = { "wso2.am" }, description = "Test WS API gateway urls", dependsOnMethods = "testApiGatewayUrlsTest")
    public void testApiGatewayUrlsAfterConfigChangeTest() throws Exception {
        //change the api-manager.xml for new gateway urls
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            serverConfigurationManager.applyConfiguration(new File(
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "webSocketTest"
                            + File.separator + "api-manager.xml"));
        }

        String provider = user.getUserName();
        HttpResponse serviceResponse = HTTPSClientUtils
                .doGet(getStoreURLHttps() + "store/apis/info?name=" + API_NAME + "&version=" + API_VERSION
                        + "&provider=" + provider, null);
        assertEquals(serviceResponse.getResponseCode(), 200, "HTTP status code mismatched");

        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            assertTrue(
                    serviceResponse.getData().contains("https://serverhost:9898/" + API_CONTEXT + "/" + API_VERSION));
            assertFalse(serviceResponse.getData()
                    .matches("(.)*\"http://(.)+:[0-9]+/" + API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
        } else {
            assertTrue(serviceResponse.getData()
                    .contains("https://serverhost:9898/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION));
            assertFalse(serviceResponse.getData()
                    .matches("(.)*\"http://(.)+:[0-9]+/t/wso2.com/" + API_CONTEXT + "/" + API_VERSION + "\"(.)*"));
        }
        serviceResponse = HTTPSClientUtils
                .doGet(getStoreURLHttps() + "store/apis/info?name=" + WS_API_NAME + "&version=" + API_VERSION
                        + "&provider=" + provider, null);
        assertEquals(serviceResponse.getResponseCode(), 200, "HTTP status code mismatched");
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            assertTrue(
                    serviceResponse.getData().contains("ws://wsserverhost:9797/" + WS_API_CONTEXT + "/" + API_VERSION));
        } else {
            assertTrue(serviceResponse.getData()
                    .contains("ws://wsserverhost:9797/t/wso2.com/" + WS_API_CONTEXT + "/" + API_VERSION));
        }

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

}
