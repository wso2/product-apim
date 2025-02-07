/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;


import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the lifecycle of a copy API to published without deprecating old version.Check whether newest version is
 * listed in store. Check whether old version is still listed under more apis from same creator. Whether users can
 * still subscribe to old version. Test invocation of both old and new API versions.
 */

public class APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase
        extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "APIAccessibilityOfOldAndCopyAPITest";
    private final String API_CONTEXT = "APIAccessibilityOfOldAndCopyAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_VERSION_2_0_0 = "2.0.0";
    private final String APPLICATION_NAME = "APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private String applicationId;
    private String apiId;
    private String newApiId;
    private APIRequest apiRequest;
    private ArrayList<String> grantTypes;
    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        grantTypes = new ArrayList<>();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() +  API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(API_DESCRIPTION);

        //Create and publish API version 1.0.0
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

    }


    @Test(groups = {"wso2.am"}, description = " Test Copy API.Copy API version 1.0.0  to 2.0.0 ")
    public void testCopyAPI() throws Exception {

        //Copy API version 1.0.0  to 2.0.0
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(),
                                 apiRequest.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);

        apiRequest.setVersion(API_VERSION_2_0_0);
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(API_VERSION_2_0_0, apiId, null);


        assertEquals(newVersionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Copy API request code is invalid." + getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(StringUtils.isNotEmpty(newVersionResponse.getData()),
                     "Copy  API response data is invalid" + getAPIIdentifierStringFromAPIRequest(apiRequest));

        newApiId = newVersionResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(newApiId, restAPIPublisher);
    }


    @Test(groups = {"wso2.am"}, description = "Test publish activity of a copied API.", dependsOnMethods = "testCopyAPI")
    public void testPublishCopiedAPI() throws Exception {
        //Publish  version 2.0.0 without re-subscription required
        HttpResponse publishAPIResponse = restAPIPublisher
                .changeAPILifeCycleStatus(newApiId, APILifeCycleAction.PUBLISH.getAction(), null);

        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(APILifeCycleState.PUBLISHED.getState().equals(publishAPIResponse.getData()),
                "API status Change is invalid when retire an API :" + API_NAME + " with API ID ("
                        + apiId + ")" + " Response Code:" + publishAPIResponse.getResponseCode());

    }


    @Test(groups = {"wso2.am"}, description = " Test availability of old and new API versions in the store.",
          dependsOnMethods = "testPublishCopiedAPI")
    public void testAvailabilityOfOldAndNewAPIVersionsInStore()
            throws Exception {
        // Check availability of old API version in API Store


        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(),
                                 API_VERSION_1_0_0,
                                 APIMIntegrationConstants.IS_API_EXISTS);


        APIDTO oldApiDto = restAPIStore.getAPI(apiId);
        apiRequest.setVersion(API_VERSION_1_0_0);
        assertTrue(StringUtils.isNotEmpty(oldApiDto.getId()),
                "Old version of the API is not available in API Store" + getAPIIdentifierStringFromAPIRequest(
                        apiRequest));


        //DisplayMultipleVersions property in api_manager.xml set to false in order to run the test on cluster
        //assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList),
        //        "Old version Api is not visible in API Store after publish new version." +
        //                getAPIIdentifierString(apiIdentifierAPI1Version1));

        // Check availability of new API version in API Store
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(),
                                 API_VERSION_2_0_0,
                                 APIMIntegrationConstants.IS_API_EXISTS);

        APIDTO newApiDto = restAPIStore.getAPI(newApiId);
        apiRequest.setVersion(API_VERSION_2_0_0);
        assertTrue(StringUtils.isNotEmpty(newApiDto.getId()),
                "Old version of the API is not available in API Store" + getAPIIdentifierStringFromAPIRequest(
                        apiRequest));
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old API version.",
          dependsOnMethods = "testAvailabilityOfOldAndNewAPIVersionsInStore")
    public void testSubscribeOldVersion() throws APIManagerIntegrationTestException {
        HttpResponse oldVersionSubscribeResponse = subscribeToAPIUsingRest(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);
        apiRequest.setVersion(API_VERSION_1_0_0);

        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(StringUtils.isNotEmpty(oldVersionSubscribeResponse.getData()),
                "Error in subscribe of old API version" + getAPIIdentifierStringFromAPIRequest(apiRequest));
    }


    @Test(groups = {"wso2.am"}, description = " Test availability of old and new API versions i the store.",
          dependsOnMethods = "testSubscribeOldVersion")
    public void testSubscribeNewVersion() throws APIManagerIntegrationTestException {
        HttpResponse newVersionSubscribeResponse = subscribeToAPIUsingRest(newApiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);
        apiRequest.setVersion(API_VERSION_2_0_0);

        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(StringUtils.isNotEmpty(newVersionSubscribeResponse.getData()),
                "Error in subscribe of old API version" + getAPIIdentifierStringFromAPIRequest(apiRequest));
    }

    @Test(groups = {"wso2.am"}, description = "Publish a API and check its visibility in the API Store. " +
                                              "Copy and create a new version, publish  the new version, test invocation of both old and" +
                                              " new API versions.", dependsOnMethods = "testSubscribeNewVersion")
    public void testAccessibilityOfPublishedOldAPIAndPublishedCopyAPI() throws Exception {
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                      API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

        //Invoke new version
        HttpResponse newVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_2_0_0) + "/" +
                                      API_END_POINT_METHOD, requestHeaders);
        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        for (SubscriptionDTO subscriptionDTO: subsDTO.getList()){
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(newApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newApiId);

    }

}
