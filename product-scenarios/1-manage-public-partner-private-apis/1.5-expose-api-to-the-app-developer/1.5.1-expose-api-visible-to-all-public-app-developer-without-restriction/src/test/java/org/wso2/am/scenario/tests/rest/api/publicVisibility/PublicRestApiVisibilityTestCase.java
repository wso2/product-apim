/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.tests.rest.api.publicVisibility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIRequest;

import java.net.URL;
import java.util.Properties;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PublicRestApiVisibilityTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURL;
    private String storeURL;
    private APIRequest apiRequest;
    private Properties infraProperties;

    private String apiName = "PhoneVerification1";
    private String apiContext = "/verify";
    private String apiResource = "/find";
    private String admin = "admin";
    private String apiVisibility = "public";
    private String apiVersion = "1.0.0";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private String tenantDomain = "carbon.super";
    private static final Log log = LogFactory.getLog(PublicRestApiVisibilityTestCase.class);
    private final String ADMIN_LOGIN_USERNAME = "admin";
    private static final long WAIT_TIME = 3 * 1000;
    private long currentTime = System.currentTimeMillis();
    private long waitTime = currentTime + WAIT_TIME;
    private APIStoreRestClient apiStoreClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        storeURL = infraProperties.getProperty(STORE_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }

        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.5.1.1 and 1.5.1.2")
    public void testVisibilityOfPublicAPIsWithoutLogin() throws Exception {

        apiName = "PhoneVerification1";
        apiContext = "/verify";
        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));

        //Design API with name,context,version,visibility,apiResource and with all optional values
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);

        APILifeCycleStateRequest updateLifeCycle =
                new APILifeCycleStateRequest(apiName, ADMIN_LOGIN_USERNAME, APILifeCycleState.PUBLISHED);

        HttpResponse apiPublishResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishResponse);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);

        apiStoreClient = new APIStoreRestClient(storeURL);
        HttpResponse apiResponseStore;

        while (waitTime > System.currentTimeMillis()) {
            HttpResponse response = null;
            apiResponseStore = apiStoreClient.getAPIListFromStoreAsAnonymousUser(tenantDomain);

            log.info("WAIT for availability of API: " + apiName);

            if (apiResponseStore != null) {
                log.info("Data: " + response.getData());
                if (response.getData().contains(apiName)) {
                    verifyResponse(apiResponseStore);
                    assertTrue(apiResponseStore.getData().contains(apiName));
                    verifyResponse(apiResponseStore);
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }

    @Test(description = "1.5.1.2")
    public void testVisibilityOfPublicAPIResourcesWithoutLogin() throws Exception {
        //Create an API
        apiName = "PhoneVerification2";
        apiContext = "/findVerification";
        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;

        //Design API with name,context,version,visibility,apiResource and with all optional values
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);

        APILifeCycleStateRequest updateLifeCycle =
                new APILifeCycleStateRequest(apiName, ADMIN_LOGIN_USERNAME, APILifeCycleState.PUBLISHED);

        HttpResponse apiPublishResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishResponse);

        apiStoreClient = new APIStoreRestClient(storeURL);
        HttpResponse apiResponseStore;

        while (waitTime > System.currentTimeMillis()) {
            apiResponseStore = apiStoreClient.getSwaggerDocumentWithoutLogin(admin, apiName, apiVersion);

            log.info("WAIT for availability of resource in API: " + apiName);

            if (apiResponseStore != null) {
                log.info("Data: " + apiResponseStore.getData());
                if (apiResponseStore.getData().contains(apiResource)) {
                    assertTrue(apiResponseStore.getData().contains(apiResource));
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {

        apiPublisher.deleteAPI("PhoneVerification1", apiVersion, admin);
        apiPublisher.deleteAPI("PhoneVerification2", apiVersion, admin);
    }
}
