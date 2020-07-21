
/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.scenario.tests.basic.auth.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIEndpointSecurityDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.HttpClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BasicAuthEndpointSecuredAPITestcase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(BasicAuthEndpointSecuredAPITestcase.class);
    private final String apiName = "BasicAuthEndpointSecuredAPITestcase API";
    private final String TEST_API_1_CONTEXT = "security";
    private final String TEST_API_1_CONTEXT_TENANT = "t/wso2.com/" + TEST_API_1_CONTEXT;
    private final String apiVersion = "1.0.0";
    private final String apiResource = "/sec";
    private final String applicationName = UUID.randomUUID().toString();
    private final String applicationDescription = "SampleDescription";
    private final String epUsername = "wso2user";
    private final String epPassword = "!@#$%^wso2.123$%";
    private final String serviceName = "jaxrs_basic/services/customers/customerservice/";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private static String apiID;
    private static String applicationId;


    @Factory(dataProvider = "userModeDataProvider")
    public BasicAuthEndpointSecuredAPITestcase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
//                Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "10.1.1.1")
    public void testInvokeAPIWithBasicAuthEndpointSecurity() throws Exception {
        // Create an API
        APIDTO apiDto = new APIDTO();
        apiDto.setName(apiName);
        apiDto.setContext(TEST_API_1_CONTEXT);
        apiDto.setVersion(apiVersion);
        APIEndpointSecurityDTO endpointSecurity = new APIEndpointSecurityDTO();
        endpointSecurity.setType(APIEndpointSecurityDTO.TypeEnum.BASIC);
        endpointSecurity.setUsername(epUsername);
        endpointSecurity.setPassword(epPassword);
        apiDto.setEndpointSecurity(endpointSecurity);
        List<APIOperationsDTO> operations = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setTarget(apiResource);
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setAuthType("Application & Application User");
        operations.add(apiOperationsDTO);
        apiDto.setOperations(operations);

        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        jsonObject.put("endpoint_type", "http");
        org.json.simple.JSONObject sandUrl = new org.json.simple.JSONObject();
        sandUrl.put("url", "http://localhost:9763/" + serviceName);
        jsonObject.put("sandbox_endpoints", sandUrl);
        jsonObject.put("production_endpoints", sandUrl);
        apiDto.setEndpointConfig(jsonObject);
        ArrayList<String> gatewayEnvironments = new ArrayList<>();
        gatewayEnvironments.add("Production and Sandbox");
        apiDto.setGatewayEnvironments(gatewayEnvironments);
        List<String> policies = new ArrayList<>();
        policies.add("Gold");
        policies.add("Unlimited");
        apiDto.setPolicies(policies);
        APIDTO apiDtoResponse = restAPIPublisher.addAPI(apiDto, "3.0");

        apiID = apiDtoResponse.getId();
        // Change the API lifecycle state from CREATED to PUBLISHED
        HttpResponse response = restAPIPublisher
                .changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);
        if (response != null) {
            log.info("Successfully published the API - " + apiName);
        }
        // Create an application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        if (applicationResponse != null) {
            applicationId = applicationResponse.getData();
            log.info("Application - " + applicationName + "is created successfully");
        }
        // Generate keys for the application         n
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        //get access token
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Add subscription to API
        HttpResponse subscription = restAPIStore.createSubscription(apiID, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        if (subscription != null) {
            log.info(applicationName + " is subscribed to " + apiName);
        }
        // Invoke the API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, apiVersion,
                    apiResource);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, apiVersion,
                    apiResource);
        }
        log.debug("Gateway HTTPS URL : " + gatewayHttpsURL);
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        String endpointCredentials = epUsername + ":" + epPassword;
        String encodedCredentials = DatatypeConverter.printBase64Binary(endpointCredentials.getBytes());
        assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when invoking the API - " + apiResponse.getData());
        assertTrue(apiResponse.getData().contains(encodedCredentials), "Response Data not match for GET" +
                " request for endpoint type secured. Expected value :" + encodedCredentials + " not contains in " +
                "response data:" + apiResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiID);
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }


    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                // new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
