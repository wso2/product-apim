
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
package org.wso2.am.scenario.tests.rest.api.restrictedVisibility;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.Properties;
import java.util.UUID;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class RESTApiVisibilityRestrictedByRolesNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURL;
    private String storeURL;
    private String keyManagerURL;
    private Properties infraProperties;

    private String apiName = UUID.randomUUID().toString();
    private String apiContext = "/" + UUID.randomUUID();
    private String apiVersion = "1.0.0";
    private String apiVisibility = "restricted";
    private String apiResource = "/find";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String TENANT_SUBSCRIBER_USERNAME = "subscriberUser2";
    private final String TENANT_SUBSCRIBER_PASSWORD = "password@123";
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String CREATOR= "Creator";
    private final String SUBSCRIBER = "Subscriber";
    private final String HEALTH_API_PUBLISHER = "Health-Publisher";
    private APIStoreRestClient apiStoreClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        storeURL = infraProperties.getProperty(STORE_URL);
        keyManagerURL = infraProperties.getProperty(KEYAMANAGER_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }
        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }

        if (keyManagerURL == null) {
            keyManagerURL = "https://localhost:9443/services/";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiStoreClient = new APIStoreRestClient(storeURL);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.5.2.1")
    public void testVisibilityOfAPISLoginUserWithIncompatibleRole() throws Exception {

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, HEALTH_API_PUBLISHER);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, SUBSCRIBER);

        createUser(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD, new String[]{HEALTH_API_PUBLISHER},
                ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);

        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverify";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, SUBSCRIBER, apiVersion, apiResource,
                tierCollection, new URL(backendEndPoint));

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(apiCreationResponse);

        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        APILifeCycleStateRequest updateLifeCycle =
                new APILifeCycleStateRequest(apiName, ADMIN_LOGIN_USERNAME, APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishStatusResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishStatusResponse);
        assertTrue(apiPublishStatusResponse.getData().contains("PUBLISHED"));

        apiStoreClient.login(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD);
        HttpResponse apiResponseStore = apiStoreClient.getAllPublishedAPIs();
        verifyResponse(apiResponseStore);
        assertFalse(apiResponseStore.getData().contains(apiName));
    }

    @Test(description = "1.5.2.2")
    public void testCreateAPIWithInvalidRole() throws Exception {

        HttpResponse checkValidationRole = apiPublisher.validateRoles(CREATOR);
        assertFalse(checkValidationRole.getData().contains("true"));
        verifyResponse(checkValidationRole);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiName, apiVersion, ADMIN_LOGIN_USERNAME);
        deleteUser(TENANT_SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteRole(HEALTH_API_PUBLISHER, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteRole(SUBSCRIBER, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }
}

