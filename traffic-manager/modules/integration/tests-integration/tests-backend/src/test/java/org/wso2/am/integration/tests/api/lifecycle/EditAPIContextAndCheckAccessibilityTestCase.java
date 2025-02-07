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

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Edit the API context and check its accessibility.
 */
public class EditAPIContextAndCheckAccessibilityTestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "EditAPIContextAndCheckAccessibilityTest";
    private final String API_CONTEXT = "EditAPIContextAndCheckAccessibility";
    private final String API_TAGS = "testTag1, testTag2, testTag3";

    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "EditAPIContextAndCheckAccessibilityTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private Map<String, String> requestHeaders;
    private APICreationRequestBean apiCreationRequestBean;
    private String newContext;
    private String applicationId = null;
    private String apiId;
    private String consumerKey;
    private String consumerSecret;
    private String userAccessToken;

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException, ApiException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));

        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);

        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

    }


    @Test(groups = {"wso2.am"}, description = "Test invoke the API before the context change")
    public void testInvokeAPIBeforeChangeAPIContext() throws Exception {
        //Create and publish  and subscribe API version 1.0.0

        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("GET");
        apiOperationsDTO2.setTarget("/customers/{id}");
        apiOperationsDTO2.setAuthType("Application & Application User");
        apiOperationsDTO2.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        operationsDTOS.add(apiOperationsDTO2);
        apiRequest.setOperationsDTOS(operationsDTOS);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        userAccessToken = applicationKeyDTO.getToken().getAccessToken();

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        //Invoke  old version
        waitForAPIDeploymentSync(user.getUserName(), apiIdentifier.getApiName(), apiIdentifier.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before change the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before change the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test changing of the API context",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPIContext")
    public void testEditAPIContext() throws APIManagerIntegrationTestException, MalformedURLException,
            org.wso2.am.integration.clients.publisher.api.ApiException, JSONException {
        //Create the API Request with new context
        newContext = "new" + API_CONTEXT;
        APIRequest apiBean = new APIRequest(API_NAME, newContext, new URL(apiEndPointUrl));
        apiBean.setVersion(API_VERSION_1_0_0);
        apiCreationRequestBean.setDescription("update " + API_DESCRIPTION);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiBean , apiId);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
    }

    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using new context after Context change",
            dependsOnMethods = "testInvokeAPIAfterChangeAPIContextWithOldContext")
    public void testInvokeAPIAfterChangeAPIContextWithNewContext() throws Exception {
        //Invoke  old context
        waitForAPIDeployment();

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(newContext, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api before changing the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  before changing the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using old context after Context change" +
            " after the API context change", dependsOnMethods = "testEditAPIContext")
    public void testInvokeAPIAfterChangeAPIContextWithOldContext() throws Exception {
        //Invoke  new context
        waitForAPIDeployment();
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api after changing the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  after changing the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }


}