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
import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API. Copy and create a new version, publish  the new version and deprecate the old version,
 * test invocGation of both old and new API versions."
 */
public class AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
        extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "DeprecatedAPITest";
    private final String API_CONTEXT = "DeprecatedAPI";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_VERSION_2_0_0 = "2.0.0";
    private final String API_VERSION_3_0_0 = "3.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private String apiEndPointUrl;
    private String providerName;

    APIIdentifier apiIdentifierAPI1Version1;
    APIIdentifier apiIdentifierAPI1Version2;
    APIIdentifier apiIdentifierAPI1Version3;
    private String apiId, apiId2, apiId3;
    private String applicationID;
    private String subscriptionId1, subscriptionId2;
    private APIDTO apiDto;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiIdentifierAPI1Version1 = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 = new APIIdentifier(providerName, API_NAME, API_VERSION_2_0_0);
        apiIdentifierAPI1Version3 = new APIIdentifier(providerName, API_NAME, API_VERSION_3_0_0);
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old API version before deprecate the old version")
    public void testSubscribeOldVersionBeforeDeprecate() throws APIManagerIntegrationTestException, MalformedURLException, ApiException, JSONException {


        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/customers/{id}");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiRequest.setOperationsDTOS(operationsDTOS);

        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);

        apiId = apiResponse.getData();


        APIRequest apiRequest2;
        apiRequest2 = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest2.setVersion(API_VERSION_2_0_0);
        apiRequest2.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setOperationsDTOS(operationsDTOS);

        HttpResponse apiResponse2 = restAPIPublisher.addAPI(apiRequest2);
        apiId2 = apiResponse2.getData();

        //create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);

        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction(), null);


        HttpResponse oldVersionSubscribeResponse = restAPIStore.createSubscription(apiId, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED);

        subscriptionId1 = oldVersionSubscribeResponse.getData();

        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful");
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of new API version before deprecate the old version",
            dependsOnMethods = "testSubscribeOldVersionBeforeDeprecate")
    public void testSubscribeNewVersion() throws APIManagerIntegrationTestException {
        HttpResponse newVersionSubscribeResponse = restAPIStore.createSubscription(apiId2, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED);

        subscriptionId2 = newVersionSubscribeResponse.getData();
        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful ");
    }


    @Test(groups = {"wso2.am"}, description = "Test deprecate old api version",
            dependsOnMethods = "testSubscribeNewVersion")
    public void testDeprecateOldVersion() throws ApiException, APIManagerIntegrationTestException {
        HttpResponse deprecateAPIResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPRECATE.getAction(), null);
        assertEquals(deprecateAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,

                "API deprecate Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version1));
        HttpResponse lcStateRsponse = restAPIPublisher.getLifecycleStatus(apiId);

        assertEquals(lcStateRsponse.getData(), APILifeCycleState.DEPRECATED.getState(),
                "API deprecate status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + lcStateRsponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API deprecate.",
            dependsOnMethods = "testDeprecateOldVersion")
    public void testVisibilityOfOldAPIInStoreAfterDeprecate()
            throws APIManagerIntegrationTestException, XPathExpressionException {
        //Verify the API in API Store

        waitForAPIDeploymentSync(user.getUserName(), apiIdentifierAPI1Version1.getApiName(),
                apiIdentifierAPI1Version1.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_2_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);


//        List<APIIdentifier> apiStoreAPIIdentifierList =
//                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI());
//        DisplayMultipleVersions property in api_manager.xml set to false in order to run the test on cluster
//        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList),
//                "Old API version is not visible in API Store after deprecate." +
//                        getAPIIdentifierString(apiIdentifierAPI1Version1));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API deprecate.",
            dependsOnMethods = "testVisibilityOfOldAPIInStoreAfterDeprecate")
    public void testVisibilityOfNewAPIInStore() throws org.wso2.am.integration.clients.store.api.ApiException {
        //Verify the API in API Store
        apiDto = restAPIStore.getAPI(apiId2);
        assertTrue(StringUtils.isNotEmpty(apiDto.getId()),
                "New API version is not visible in API Store after deprecate the old version." +
                        getAPIIdentifierString(apiIdentifierAPI1Version2));

    }


    @Test(groups = {"wso2.am"}, description = "Test the subscription of deprecated API version.",
            dependsOnMethods = "testVisibilityOfNewAPIInStore")
    public void testSubscribeOldVersionAfterDeprecate() throws APIManagerIntegrationTestException {
        //subscribe deprecated old version
        HttpResponse oldVersionSubscribeResponse = restAPIStore.createSubscription(apiId, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED);

        assertEquals(oldVersionSubscribeResponse, null,
                "Subscribe of old API version  after deprecate success, which should fail." +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of both deprecated old and  " +
            "publish new API versions", dependsOnMethods = "testSubscribeOldVersionAfterDeprecate")
    public void testAccessibilityOfDeprecateOldAPIAndPublishedCopyAPI() throws Exception {
        //get access token
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        //get access token
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(),
                HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");
        //Invoke new version
        HttpResponse newVersionInvokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_2_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

    }

    @Test(groups = {"wso2.am"}, description = "Test the previous API versions deprecation while publishing the  " +
            "new version", dependsOnMethods = "testAccessibilityOfDeprecateOldAPIAndPublishedCopyAPI")
    public void testOlderAPIDeprecationWithNewVersionPublish() throws Exception {

        //Create new API version 3.0.0  from 2.0.0
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_2_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(API_VERSION_3_0_0, apiId, null);
        assertEquals(newVersionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Copy API request code is invalid.");

        apiId3 = newVersionResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId3, restAPIPublisher);

        String lifeCycleChecklist = "Deprecate old versions after publishing the API:true";

        // Publish the newly created API and deprecate the old APIs at once
        HttpResponse publishAPIResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiId3, APILifeCycleAction.PUBLISH.getAction(), lifeCycleChecklist);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API publish Response code is invalid ");

        HttpResponse lcStateResponseOfAPI2 = restAPIPublisher.getLifecycleStatus(apiId2);
        HttpResponse lcStateResponseOfAPI3 = restAPIPublisher.getLifecycleStatus(apiId3);

        assertEquals(lcStateResponseOfAPI2.getData(), APILifeCycleState.DEPRECATED.getState(),
                "API deprecate status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + lcStateResponseOfAPI2.getData());
        assertEquals(lcStateResponseOfAPI3.getData(), APILifeCycleState.PUBLISHED.getState(),
                "API publish status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version3) +
                        "Response Data:" + lcStateResponseOfAPI2.getData());
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        Thread.sleep(2000);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId2, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId3, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
    }


}
