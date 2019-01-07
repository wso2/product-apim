/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import java.util.Properties;
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
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.apimgt.samples.utils.WebAppDeployUtils;

import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import static org.eclipse.jdt.internal.compiler.util.Util.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BasicAuthEndpointSecuredAPITestcase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(BasicAuthEndpointSecuredAPITestcase.class);
    private final String apiName = UUID.randomUUID().toString();
    private final String apiContext = "/" + UUID.randomUUID().toString();
    private final String admin = "admin";
    private final String apiVersion = "1.0.0";
    private final String apiResource = "/sec";
    private final String applicationName = "EndpointSecurityApplication";
    private final String applicationDescription = "SampleDescription";
    private final String endpointType = "secured";
    private final String endpointAuthType = "basicAuth";
    private final String epUsername = "wso2user";
    private final String epPassword = "!@#$%^wso2.123$%";
    private final String endpointURL = "https://localhost:9443/jaxrs_basic/services/customers/customerservice/";

    private APIRequest apiRequest;
    private Properties infraProperties;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        infraProperties = getDeploymentProperties();
        String publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        String storeURL = infraProperties.getProperty(STORE_URL);
        String warFileName = "jaxrs_basic";
        String warFileLocation = System.getProperty("user.dir") + File.separator+ "src/test/resources" + File.separator
                + warFileName + ".war";
        String serviceEndpoint = "https://localhost:9443/services/";

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }

        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(admin, admin);
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(admin, admin);
        try {
            WebAppDeployUtils.deployWebApp(serviceEndpoint, admin, admin, warFileLocation,
                    warFileName);
            log.info("WebApp deployed successfully");
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test(description = "10.1.1.1")
    public void testInvokeAPIWithBasicAuthEndpointSecurity() throws Exception {
        // Create an API
        apiRequest = new APIRequest(apiName, apiContext, apiVersion, endpointType, endpointAuthType,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, endpointURL, epUsername, URLEncoder.encode(epPassword, UTF_8), "0",
                APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, apiResource);
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(apiCreationResponse);

        // Change the API lifecycle state from CREATED to PUBLISHED
        APILifeCycleStateRequest apiLifeCycleStateRequest = new APILifeCycleStateRequest(apiName, admin,
                APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishResponse = apiPublisher.changeAPILifeCycleStatus(apiLifeCycleStateRequest);
        verifyResponse(apiPublishResponse);
        log.info("Successfully published the API - " + apiName);

        // Retrieve the created API
        HttpResponse getAPIResponse = apiPublisher.getAPI(apiName, admin);
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
        verifyResponse(keyGenerationResponse);

        // Add subscription to API
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion, "admin", applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse addSubscriptionResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(addSubscriptionResponse);
        log.info(applicationName + " is subscribed to " + apiName);

        // Invoke the API
        JSONObject keyGenerationRespData = new JSONObject(keyGenerationResponse.getData());
        String accessToken = (keyGenerationRespData.getJSONObject("data").getJSONObject("key"))
                .get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        HttpResponse apiResponse = HttpClient.doGet(getHttpsAPIInvocationURL(apiContext, apiVersion, apiResource) ,
                requestHeaders);
        String endpointCredentials = epUsername + ":" + epPassword;
        String encodedCredentials = DatatypeConverter.printBase64Binary(endpointCredentials.getBytes());
        assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when invoking the API");
        assertTrue(apiResponse.getData().contains(encodedCredentials), "Response Data not match for GET" +
                " request for endpoint type secured. Expected value :" + encodedCredentials + " not contains in " +
                "response data:" + apiResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse deleteApplicationResponse = apiStore.removeApplication(applicationName);
        verifyResponse(deleteApplicationResponse);
        HttpResponse deleteAPIResponse = apiPublisher.deleteAPI(apiName, apiVersion, admin);
        verifyResponse(deleteAPIResponse);
    }
}
