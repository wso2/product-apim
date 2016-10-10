/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.other;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TagsRatingCommentTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
//    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public TagsRatingCommentTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        /*
       This test can point to external API manager deployment without adding any resources to system
        */
//        String sourcePath =
//                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
//                File.separator + "lifecycletest" + File.separator + "jaxrs_basic.war";
//        String targetPath = FrameworkPathUtil.getCarbonHome() + File.separator + "repository" + File.separator + "deployment" +
//                            File.separator + "server" + File.separator + "webapps";

//        serverConfigurationManager = new ServerConfigurationManager(
//                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
//                                      APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN));

//        FileManager.copyResourceToFileSystem(sourcePath, targetPath, "jaxrs_basic.war");
//        serverConfigurationManager.restartGracefully();

        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = {"wso2.am"}, description = "Comment Rating Test case")
    public void testTagsRatingCommentTestCase() throws Exception {
        String APIName = "CommentRatingAPI";
        String APIContext = "commentRating";
        String tags = "youtube, video, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String providerName = user.getUserName();
        String APIVersion = "1.0.0";

        // This is because with the new context version strategy, if the context does not have the {version} param ,
        // then we add the {version} param to the end of the context.
        String apiContextAddedValue = APIContext + "/" + APIVersion;

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMTestCaseUtils
                .getAPIBeanFromHttpResponse(apiPublisher.getAPI(APIName, providerName));
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        //Test API properties
        assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");

        if (!gatewayContextMgt.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            apiContextAddedValue = "t/" + gatewayContextWrk.getContextTenant().getDomain() + "/" + apiContextAddedValue;
        }

        assertEquals(apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1),
                     apiContextAddedValue, "API context mismatch");
        assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        assertEquals(apiBean.getId().getProviderName(), providerName,
                     "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        assertEquals(apiBean.getDescription(), description, "API description mismatch");

        apiStore.addApplication("CommentRatingAPI-Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "",
                "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, storeContext.getContextTenant()
                .getContextUser()
                .getUserName());

        subscriptionRequest.setApplicationName("CommentRatingAPI-Application");
        apiStore.subscribe(subscriptionRequest);

//        APPKeyRequestGenerator generateAppKeyRequest =
//                new APPKeyRequestGenerator("CommentRatingAPI-Application");
//        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
//        JSONObject response = new JSONObject(responseString);
//        String accessToken =
//                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
//        Map<String, String> requestHeaders = new HashMap<String, String>();
//        requestHeaders.put("Authorization", "Bearer " + accessToken);
//        requestHeaders.put("accept", "text/xml");
        //Here add API tags and check same have in response.
        //Here check same tags are there
        //Add some comment to API
        //check comment is there
        //Add rating
        //check rating

//        String gatewayUrl = getAPIInvocationURLHttp("commentRating/1.0.0/customers/123");
//
//        Thread.sleep(2000);
//        for (int i = 0; i < 19; i++) {
//            HttpResponse youTubeResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
//
//            assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
//                         "Response code mismatched");
//            assertTrue(youTubeResponse.getData().contains("John"),
//                       "Response data mismatched");
//            assertTrue(youTubeResponse.getData().contains("<name>"),
//                       "Response data mismatched");
//            assertTrue(youTubeResponse.getData().contains("<Customer>"),
//                       "Response data mismatched");
//        }
//        //Do get,post,put,delete all here
//        //HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("commentRating/1.0.0/most_popular"), requestHeaders);
//
//        Thread.sleep(10000);
//        HttpResponse youTubeResponse1 = HttpRequestUtil.doGet(gatewayUrl, new HashMap<String, String>());
//        assertEquals(youTubeResponse1.getResponseCode(), 401, "Response code mismatched");
        // URL url1 = new URL(url);
        // HttpResponse youTubeResponse2 = HttpRequestUtil.doPost(url1,"-");
        //Remove subscription and then remove API

        apiStore.getAllPublishedAPIs();
        apiStore.getAllApplications();
        apiStore.getPublishedAPIsByApplication("CommentRatingAPI-Application");
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "4");
        apiStore.removeRatingFromAPI(APIName, APIVersion, providerName);
        apiStore.getAllDocumentationOfAPI(APIName, APIVersion, providerName);
        //apiStore.getAllPaginatedPublishedAPIs("carbon.super","0","10");
        //Negative cases
        //add assert
        apiStore.getPublishedAPIsByApplication("CommentRatingAPI-Application-Wrong");
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI("NoAPI", APIVersion, providerName, "4");
        apiStore.removeRatingFromAPI("NoAPI", APIVersion, providerName);
        apiStore.getAllDocumentationOfAPI("NoAPI", APIVersion, providerName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("CommentRatingAPI-Application");
        super.cleanUp();
    }
}
