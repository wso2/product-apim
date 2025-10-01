/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.version;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * publish new copy using "Deprecate old versions" from the below options and Invoke Deprecated API and Published
 * API from Store
 * Propagate Changes to API Gateway
 * Deprecate Old Versions
 * Require Re-Subscription
 */
public class APIM366PublishNewCopyGivenDeprecateOldVersionTestCase extends APIMIntegrationBaseTest {

    private final String apiName = "APIM366DeprecateTestAPI";
    private final String apiVersionOld = "1.0.0";
    private final String apiVersionNew = "1.0.1";
    private final String applicationName = "APIM366DeprecateAPI";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String providerNameApi;
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifierStoreOldVersion;
    private APIIdentifier apiIdentifierStoreNewVersion;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM366PublishNewCopyGivenDeprecateOldVersionTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiEndpointPostfixUrlJaxrs = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiEndpointPostfixUrlJaxrs;

        providerNameApi = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifierStoreOldVersion = new APIIdentifier(providerNameApi, apiName, apiVersionOld);
        apiIdentifierStoreNewVersion = new APIIdentifier(providerNameApi, apiName, apiVersionNew);

    }

    @Test(groups = {"wso2.am"}, description = "Check whether user can publish new copy for the " +
            " given deprecate old versions option")

    public void testPublishNewCopyGivenDeprecateOldVersion() throws Exception {

        String apiContext = "apim366deprecateTestAPI";

        String apiDescription = "This is Test API Created by API Manager " +
                "Integration Test";
        String apiTag = "tag366-1, tag366-2, tag366-3";
        String defaultVersion = "default_version";
        String apiResponse = "<Customer><id>123</id><name>John</name></Customer>";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersionOld, providerNameApi,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setDefault_version(defaultVersion);
        apiCreationRequestBean.setDefaultVersionChecked(defaultVersion);

        //Check the availability of the API after creation in Publisher
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject jsonObjectCreation = new JSONObject(apiCreationResponse.getData());
        assertFalse(jsonObjectCreation.getBoolean("error"), apiName + " is not created");

        //Check the status update of the API after publishing
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PUBLISHED"),
                apiName + " has not been created in publisher");

        //Check Availability of the Application in Store
        HttpResponse appResponse = apiStore.addApplication(applicationName, "Gold", "", "");
        JSONObject appJsonObject = new JSONObject(appResponse.getData());
        assertFalse(appJsonObject.getBoolean("error"), applicationName + " is not created in Store");
        assertEquals(appJsonObject.getString("status"), "APPROVED",
                applicationName + " is not created in Store");

        //Check whether API has subscribed in Store
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, providerNameApi);
        subscriptionRequest.setApplicationName(applicationName);
        HttpResponse subscriptionResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiName + " has not been subscribed");

        //Create a new copy of the API and validate the result
        JSONObject jsonObjectCopy = new JSONObject(apiPublisher.copyAPI
                (providerNameApi,apiName,apiVersionOld,apiVersionNew,defaultVersion).getData());
        assertFalse(jsonObjectCopy.getBoolean("error"),apiName + " is not copied");

        //Again Publish the API after copying
        APILifeCycleStateRequest lifeCycleStateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);
        lifeCycleStateRequest.setVersion(apiVersionNew);
        lifeCycleStateRequest.setDeprecateOldVersions("true");
        apiPublisher.changeAPILifeCycleStatus(lifeCycleStateRequest);

        //Validate Status and Version of the Original API and New Copy of the API in Publisher
        HttpResponse originalApiResponse = apiPublisher.
                getAPI(apiName, providerNameApi, apiVersionOld);
        assertTrue(originalApiResponse.getData().contains(apiVersionOld),
                "Version of the original" +apiName + " has been updated with new version");
        assertTrue(originalApiResponse.getData().contains("DEPRECATED"),
                "Status of the API has not been changed as DEPRECATED");

        HttpResponse newApiResponse = apiPublisher.getAPI(apiName, providerNameApi, apiVersionNew);
        assertTrue(newApiResponse.getData().contains(apiVersionNew),
                "Version of the Copied API has not been updated with the new version");
        assertTrue(newApiResponse.getData().contains("PUBLISHED"),
                "Status of the API has not been changed as PUBLISHED");

        //Validate availability of the API with the new version in Store
        List<APIIdentifier> publishedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierStoreNewVersion, publishedAPIList),
                "API with the new Version is not available in store");
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierStoreOldVersion, publishedAPIList),
                "API with the Old Version is available in store");

        //Validate the Version and Status of the Original API under Subscribed API
        JSONObject subscribedApi = new JSONObject(apiStore.getSubscribedAPIs(applicationName).
                getData()).getJSONObject("subscriptions");
        JSONArray jsonArray = subscribedApi.getJSONArray("applications");
        JSONObject jsonObjectDepApi = (JSONObject) jsonArray.get(0);
        JSONArray jsonArrayApiList = jsonObjectDepApi.getJSONArray("subscriptions");
        JSONObject deprecateApi = (JSONObject) jsonArrayApiList.get(0);
        assertEquals(deprecateApi.getString("status"), "DEPRECATED",
                "Invalid Status of the Deprecated API");
        assertEquals(deprecateApi.getString("version"), apiVersionOld,
                "Invalid Version of the Deprecated API");
        assertEquals(deprecateApi.getString("name"), apiName,
                "Invalid name of the Deprecated API");

        //Validate the Version and Status of the New API under Subscribed API
        JSONObject publishApi = (JSONObject) jsonArrayApiList.get(1);
        assertEquals(publishApi.getString("status"), "PUBLISHED",
                "Invalid Status of the Published API");
        assertEquals(publishApi.getString("version"), apiVersionNew,
                "Invalid Version of the Published API");
        assertEquals(publishApi.getString("name"), apiName,
                "Invalid name of the Published API");

        //Generate Key for the Published API
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String response = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();
        JSONObject jsonObject = new JSONObject(response);

        String accessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");

        Thread.sleep(4000);

        //Invoke old version of the API
        HttpResponse oldApiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                ( apiContext + "/" + apiVersionOld), requestHeaders);
        assertEquals(oldApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when old version of the api invocation");
        assertTrue(oldApiResponse.getData().contains(apiResponse),
                "Response data mismatched when old version of the api invocation for GET Request");

        Thread.sleep(5000);

        //Invoke new version of the API
        HttpResponse newVersionApiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                (apiContext + "/" + apiVersionNew), requestHeaders);
        assertEquals(newVersionApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when new version of the api invocation");
        assertTrue(oldApiResponse.getData().contains(apiResponse),
                "Response data mismatched when new version of the api invocation for GET Request");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiStore.removeAPISubscriptionByName
                (apiName, apiVersionOld, providerNameApi, applicationName);
        apiStore.removeAPISubscriptionByName
                (apiName, apiVersionNew, providerNameApi, applicationName);
        apiStore.removeApplication(applicationName);
        apiPublisher.deleteAPI(apiName, apiVersionOld, providerNameApi);
        apiPublisher.deleteAPI(apiName, apiVersionNew, providerNameApi);
        super.cleanUp();
    }


}
