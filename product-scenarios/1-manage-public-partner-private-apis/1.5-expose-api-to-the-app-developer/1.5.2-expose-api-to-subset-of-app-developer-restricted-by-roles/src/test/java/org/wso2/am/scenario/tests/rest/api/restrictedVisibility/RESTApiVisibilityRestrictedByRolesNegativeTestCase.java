
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

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.Properties;
import java.util.UUID;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class RESTApiVisibilityRestrictedByRolesNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURL;
    private String storeURL;
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
    private final String CREATOR = "Creator";
    private final String SUBSCRIBER = "Subscriber";
    private final String HEALTH_API_PUBLISHER = "Health-Publisher";
    private UserManagementClient userManagementClient;
    private APIStoreRestClient apiStoreClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        storeURL = infraProperties.getProperty(STORE_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.5.2.1")
    public void testVisibilityOfAPISLoginUserWithIncompatibleRole() throws Exception {

        // create new user in tenant with only subscriber role and login to the Store
        userManagementClient = new UserManagementClient(
                "https://localhost:9443/services/", ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);

        //Check availability of the API in publisher
        userManagementClient.addRole(HEALTH_API_PUBLISHER,
                new String[]{},
                new String[]{"/permission/admin/login",
                        "/permission/admin/manage/api/subscribe"});

        userManagementClient.addRole(SUBSCRIBER,
                new String[]{},
                new String[]{"/permission/admin/login",
                        "/permission/admin/manage/api/subscribe"});

        if (userManagementClient.userNameExists(INTERNAL_ROLE_SUBSCRIBER, TENANT_SUBSCRIBER_USERNAME)) {
            userManagementClient.deleteUser(TENANT_SUBSCRIBER_USERNAME);
        }

        userManagementClient.addUser(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD,
                new String[]{HEALTH_API_PUBLISHER}, "");

        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverify";

        //Create an API request
        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, SUBSCRIBER, apiVersion, apiResource,
                tierCollection, new URL(backendEndPoint));

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        //change APILifeCycleStatus to published
        APILifeCycleStateRequest updateLifeCycle =
                new APILifeCycleStateRequest(apiName, ADMIN_LOGIN_USERNAME, APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishStatusResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishStatusResponse);
        assertTrue(apiPublishStatusResponse.getData().contains("PUBLISHED"));

        //Check availability of the API in store
        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }
        setKeyStoreProperties();
        apiStoreClient = new APIStoreRestClient(storeURL);
        apiStoreClient.login(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD);

        HttpResponse apiResponseStore = apiStoreClient.getAllPublishedAPIs();
        assertFalse(apiResponseStore.getData().contains(apiName));
        verifyResponse(apiResponseStore);
    }

    @Test(description = "1.5.2.2")
    public void testCreateAPIWithInvalidRole() throws Exception {

        HttpResponse checkValidationRole = apiPublisher.validateRoles(CREATOR);
        assertFalse(checkValidationRole.getData().contains("true"));
        verifyResponse(checkValidationRole);

    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiName, apiVersion, ADMIN_LOGIN_USERNAME);
        userManagementClient.deleteUser(TENANT_SUBSCRIBER_USERNAME);
        userManagementClient.deleteRole(HEALTH_API_PUBLISHER);
        userManagementClient.deleteRole(SUBSCRIBER);
        userManagementClient.deleteRole(CREATOR);
    }
}

