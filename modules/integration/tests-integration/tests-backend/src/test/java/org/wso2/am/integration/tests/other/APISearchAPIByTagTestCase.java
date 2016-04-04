/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This test case is used to test the API Store search API by API's TAG
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class APISearchAPIByTagTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APISearchAPIByTagTestCase.class);
    private final String API_NAME_1 = "APISearchAPIByTagAPIName_1";
    private final String API_NAME_2 = "APISearchAPIByTagAPIName_2";
    private final String API_CONTEXT_1 = "PISearchAPIByTagContext_1";
    private final String API_CONTEXT_2 = "PISearchAPIByTagContext_2";
    private final String TAG_API_1 = "api_1_tag";
    private final String TAG_API_2 = "api_2_tag";
    private final String TAG_API = "api_common_tag";
    private final String TAG_NOT_EXIST = "no_such_tag";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private static final long WAIT_TIME = 45 * 1000;
    private String publisherURLHttps;
    private String storeURLHttp;
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String tags;
    private String tierCollection;
    private String endpointUrl;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @Factory(dataProvider = "userModeDataProvider")
    public APISearchAPIByTagTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttps = publisherUrls.getWebAppURLHttps();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttps);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());

        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean addResource = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.BASIC, "/add");

        //implement API 1
        apiCreationRequestBean = new APICreationRequestBean(API_NAME_1, API_CONTEXT_1, API_VERSION, user.getUserName(),
                new URL(endpointUrl));
        tags = TAG_API + "," + TAG_API_1;
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        resList.add(addResource);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api 1
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api 1
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME_1, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_1, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        //implement API 2
        apiCreationRequestBean = new APICreationRequestBean(API_NAME_2, API_CONTEXT_2, API_VERSION, user.getUserName(),
                new URL(endpointUrl));
        tags = TAG_API + "," + TAG_API_2;
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        resList.add(addResource);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api 2
        serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api 2
        updateRequest = new APILifeCycleStateRequest(API_NAME_2, user.getUserName(), APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_2, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(API_NAME_1, API_VERSION, user.getUserName());
        apiPublisher.deleteAPI(API_NAME_2, API_VERSION, user.getUserName());
    }

    @Test(groups = { "wso2.am" }, description = "API search by TAG")
    public void testAPISearchByTag() throws Exception {
        String searchTerm;
        HttpResponse response;
        JSONObject results;
        JSONArray resultArray;
        //wait for APIs to appear in Search API
        watForAPIsAvailableOnSearchApi();

        //search for common tags
        searchTerm = "tag:" + TAG_API;
        response = apiStore.searchPaginateAPIs(user.getUserDomain(), "0", "10", searchTerm);
        verifyResponse(response);
        results = new JSONObject(response.getData());
        resultArray = results.getJSONArray("result");
        Assert.assertEquals(resultArray.length(), 2, "Search API return invalid APIs");
        Assert.assertTrue(response.getData().contains(API_NAME_1), "API with searched tag not returned");
        Assert.assertTrue(response.getData().contains(API_NAME_2), "API with searched tag not returned");

        //search for one out of two API
        searchTerm = "tag:" + TAG_API_1;
        response = apiStore.searchPaginateAPIs(user.getUserDomain(), "0", "10", searchTerm);
        verifyResponse(response);
        results = new JSONObject(response.getData());
        resultArray = results.getJSONArray("result");
        Assert.assertEquals(resultArray.length(), 1, "Search API return invalid APIs");
        Assert.assertTrue(response.getData().contains(API_NAME_1), "API with searched tag not returned");
        Assert.assertFalse(response.getData().contains(API_NAME_2), "Result contain API without the requested Tag");

        //search for non-exist TAG
        searchTerm = "tag:" + TAG_NOT_EXIST;
        response = apiStore.searchPaginateAPIs(user.getUserDomain(), "0", "10", searchTerm);
        verifyResponse(response);
        results = new JSONObject(response.getData());
        resultArray = results.getJSONArray("result");
        Assert.assertEquals(resultArray.length(), 0, "Search API return invalid APIs");
    }

    /**
     * Used to wait until published apis are appear in the Store API search API
     *
     * @throws Exception if search API throws any exceptions
     */
    public void watForAPIsAvailableOnSearchApi() throws Exception {
        String searchTerm = "";
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        HttpResponse response;
        while (waitTime > System.currentTimeMillis()) {
            response = apiStore.searchPaginateAPIs(user.getUserDomain(), "0", "10", searchTerm);
            verifyResponse(response);
            log.info("WAIT for availability of API : " + API_NAME_1 + " and " + API_NAME_2
                    + " found on Store search API");
            if (response != null) {
                if (response.getData().contains(API_NAME_1) && response.getData().contains(API_NAME_2)) {
                    log.info("API :" + API_NAME_1 + " and " + API_NAME_2 + " found");
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

}
