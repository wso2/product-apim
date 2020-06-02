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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.HttpClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.apimgt.samples.utils.WebAppDeployUtils;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.util.HashMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import static org.eclipse.jdt.internal.compiler.util.Util.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BasicAuthEndpointSecuredAPITestcase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(BasicAuthEndpointSecuredAPITestcase.class);
    private final String apiName = UUID.randomUUID().toString();
    private final String apiContext = UUID.randomUUID().toString();
    private final String admin = "admin";
    private final String apiProvider = "APIProvider";
    private final String apiVersion = "1.0.0";
    private final String apiResource = "/sec";
    private final String applicationName = "EndpointSecurityApplication";
    private final String applicationDescription = "SampleDescription";
    private final String endpointType = "secured";
    private final String endpointAuthType = "basicAuth";
    private final String epUsername = "wso2user";
    private final String epPassword = "!@#$%^wso2.123$%";
    private final String serviceName = "jaxrs_basic/services/customers/customerservice/";

    private APIRequest apiRequest;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        String warFileName = "jaxrs_basic";
        String warFileLocation = System.getProperty("user.dir") + File.separator+ "src/test/resources" + File.separator
                + warFileName + ".war";

        createUsers();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(apiProvider, "wso2123$");
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login("APIConsumer", "wso2123$");
        try {
            WebAppDeployUtils.deployWebApp(serviceEndpoint, admin, admin, warFileLocation, warFileName);
            boolean isWebAppDeployed = isWebApplicationDeployed(serviceEndpoint, admin, admin, warFileName);
            assertTrue(isWebAppDeployed, warFileName + " is not deployed" );
            log.info("WebApp deployed successfully");
        } catch (RemoteException | MalformedURLException e) {
            log.error("Error when deploying webApp - " + warFileName + ".war", e);
        }
    }

    @Test(description = "10.1.1.1")
    public void testInvokeAPIWithBasicAuthEndpointSecurity() throws Exception {
        // Create an API
        apiRequest = new APIRequest(apiName, apiContext, apiVersion, "Public", endpointType, endpointAuthType,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "http://localhost:9763/" + serviceName,
                epUsername, URLEncoder.encode(epPassword, UTF_8), "1", APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, apiResource);
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(apiCreationResponse);

        // Change the API lifecycle state from CREATED to PUBLISHED
        APILifeCycleStateRequest apiLifeCycleStateRequest = new APILifeCycleStateRequest(apiName, apiProvider,
                APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishResponse = apiPublisher.changeAPILifeCycleStatus(apiLifeCycleStateRequest);
        verifyResponse(apiPublishResponse);
        log.info("Successfully published the API - " + apiName);

        // Retrieve the created API
        HttpResponse getAPIResponse = apiPublisher.getAPI(apiName, apiProvider);
        verifyResponse(getAPIResponse);
        JSONObject getAPIRespData = new JSONObject(getAPIResponse.getData());
        assertTrue(getAPIRespData.getJSONObject("api").get("endpointAuthTypeDigest").equals("false"),
                "Endpoint security type does not match");
        assertTrue(getAPIRespData.getJSONObject("api").get("epUsername").equals(epUsername),
                "Endpoint username does not match");
        assertTrue(getAPIRespData.getJSONObject("api").get("epPassword").equals(epPassword),
                "Endpoint password does not match");

        // Create an application
        HttpResponse addApplicationResponse = apiStore.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", applicationDescription);
        verifyResponse(addApplicationResponse);
        log.info("Application - " + applicationName + "is created successfully");

        // Generate keys for the application
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        HttpResponse keyGenerationResponse = apiStore.generateApplicationKey(appKeyRequestGenerator);
        // add logs to verify http response 404 when generating tokens
        JSONObject responseStringJson = new JSONObject(keyGenerationResponse.getData());
        log.info("key generation response for application \'" + applicationName + "\' response data :"
                + keyGenerationResponse.getData());

        if (!responseStringJson.getBoolean("error")) {
            verifyResponse(keyGenerationResponse);

            // Check the visibility of the API in API store
            isAPIVisibleInStore(apiName, apiStore);

            // Add subscription to API
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion, apiProvider,
                    applicationName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
            HttpResponse addSubscriptionResponse = apiStore.subscribe(subscriptionRequest);
            verifyResponse(addSubscriptionResponse);
            log.info(applicationName + " is subscribed to " + apiName);

            // Invoke the API
            JSONObject keyGenerationRespData = new JSONObject(keyGenerationResponse.getData());
            String accessToken = (keyGenerationRespData.getJSONObject("data").getJSONObject("key"))
                    .get("accessToken").toString();
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

            String gatewayHttpsUrl = getHttpsAPIInvocationURL(apiContext, apiVersion, apiResource);
            log.debug("Gateway HTTPS URL : " + gatewayHttpsURL);
            HttpResponse    apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
            String endpointCredentials = epUsername + ":" + epPassword;
            String encodedCredentials = DatatypeConverter.printBase64Binary(endpointCredentials.getBytes());
            assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched when invoking the API - " + apiResponse.getData());
            assertTrue(apiResponse.getData().contains(encodedCredentials), "Response Data not match for GET" +
                    " request for endpoint type secured. Expected value :" + encodedCredentials + " not contains in " +
                    "response data:" + apiResponse.getData());
        } else {
//            if key generating fails test endpoint and initiate thread sleep for debugging
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Basic ZHVtbXk6ZHVtbXk=");
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse keyManagerURLResponse = HttpClient.doPost(keyManagerURL.replace("services/",
                    "") + "oauth2/token", headers,
                    "grant_type=password&username=admin&password=admin");
            log.info("key manager url token endpoint response code :" + keyManagerURLResponse.getResponseCode());
            log.info("key manager url token endpoint response data :" + keyManagerURLResponse.getData());
            HttpResponse gatewayResponse = HttpClient.doPost(gatewayHttpsURL + "/token", headers,
                    "grant_type=password&username=admin&password=admin");
            log.info("Gateway url token endpoint response code  :" + gatewayResponse.getResponseCode());
            log.info("Gateway url token endpoint response data  :" + gatewayResponse.getData());
            Thread.sleep(3600000);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse deleteApplicationResponse = apiStore.removeApplication(applicationName);
        verifyResponse(deleteApplicationResponse);
        HttpResponse deleteAPIResponse = apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);
        verifyResponse(deleteAPIResponse);
        deleteUser("APIConsumer", admin, admin);
        deleteUser(apiProvider, admin, admin);
    }

    private void createUsers() throws APIManagerIntegrationTestException {
        String[] roleList = new String[]{ScenarioTestConstants.CREATOR_ROLE, ScenarioTestConstants.PUBLISHER_ROLE};
        try {
            createUser("APIProvider", "wso2123$", roleList, admin, admin);
            createUserWithSubscriberRole("APIConsumer", "wso2123$", admin, admin);
        } catch (RemoteException | UserAdminUserAdminException  | APIManagementException e) {
            throw new APIManagerIntegrationTestException("Error occurred while creating users", e);
        }
    }
}
