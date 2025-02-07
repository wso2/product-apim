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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TagDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TagListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This test case is used to test the API Store search API by API's TAG
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APISearchAPIByTagTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APISearchAPIByTagTestCase.class);
    private final String API_NAME_1 = "APISearchAPIByTagAPIName_1";
    private final String API_NAME_2 = "APISearchAPIByTagAPIName_2";
    private final String API_CONTEXT_1 = "PISearchAPIByTagContext_1";
    private final String API_CONTEXT_2 = "PISearchAPIByTagContext_2";
    private final String TAG_API_1 = "api_1_tag";
    private final String TAG_API_2 = "api_2_tag";
    private final String TAG_API = "api_common_tag";
    private final String API_NAME_CONTEXT_UPPER_CASE = "apiNameContextUpperCaseTag";
    private final String API_NAME_CONTEXT_LOWER_CASE = "apiNameContextLowerCaseTag";
    private final String API_NAME_CONTEXT_WITH_SPACE = "apiNameContextWithSpaceTag";
    private final String API_NAME_CONTEXT_WITHOUT_SPACE = "apiNameContextWithoutSpaceTag";
    private final String TAG_GROUP_UPPER_CASE = "API_tag-group";
    private final String TAG_GROUP_LOWER_CASE = "api_tag-group";
    private final String TAG_GROUP_WITH_SPACE = "api tag-group";
    private final String TAG_GROUP_WITHOUT_SPACE = "apiTag-group";
    private final String TAG_NOT_EXIST = "no_such_tag";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private static final long WAIT_TIME = 45 * 1000;
    private APIRequest apiRequest;
    private String tags;
    private String tierCollection;
    private String endpointUrl;
    private String api1;
    private String api2;
    private String api3;
    private String api4;
    private String api5;
    private String api6;
    String providerName = "";
    @Factory(dataProvider = "userModeDataProvider")
    public APISearchAPIByTagTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        tags = TAG_API + "," + TAG_API_1;
        apiRequest = new APIRequest(API_NAME_1, API_CONTEXT_1, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setProvider(providerName);
        //Add api resource
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/add");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);
        //create and publish test api 1
        api1 =  createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        //implement API 2
        tags = TAG_API + "," + TAG_API_2;
        apiRequest = new APIRequest(API_NAME_2, API_CONTEXT_2, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setProvider(providerName);
        //Add api resource
        apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/add");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);
        //create and publish test api 2
        api2 = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        //publish the api 2
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_2, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(api1, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(api2, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(api3, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(api4, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(api5, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(api6, restAPIPublisher);
        super.cleanUp();
    }

    @Test(groups = { "wso2.am" }, description = "API search by TAG", dependsOnMethods = "testAPICreation")
    public void testAPISearchByTag() throws Exception {
        String searchTerm;
        APIListDTO response;
        //wait for APIs to appear in Search API
        watForAPIsAvailableOnSearchApi();

        //search for common tags
        searchTerm = "tags:" + TAG_API;
        response = restAPIStore.searchPaginatedAPIs( 10,0, user.getUserDomain(), searchTerm);
        Assert.assertEquals(response.getList().size(), 2, "Search API return invalid APIs");
        int expectedAPIs = 0;
        for (APIInfoDTO api : response.getList()) {
            if(api.getName().equals(API_NAME_1) || api.getName().equals(API_NAME_2)) {
                expectedAPIs ++;
            }
        }
        Assert.assertTrue( expectedAPIs == 2 , "API with searched tag not returned");

        //search for one out of two API
        searchTerm = "tags:" + TAG_API_1;
        response = restAPIStore.searchPaginatedAPIs(10, 0, user.getUserDomain(), searchTerm);
        Assert.assertEquals(response.getList().size(), 1, "Search API return invalid APIs");
        for (APIInfoDTO api : response.getList()) {
            Assert.assertTrue(api.getName().equals(API_NAME_1), "API with searched tag not returned");
        }
        for (APIInfoDTO api : response.getList()) {
            Assert.assertFalse(api.getName().equals(API_NAME_2), "Result contain API without the requested Tag");
        }
        //search for non-exist TAG
        searchTerm = "tags:" + TAG_NOT_EXIST;
        response = restAPIStore.searchPaginatedAPIs(10, 0, user.getUserDomain(), searchTerm);
        Assert.assertNull(response, "Search API return invalid APIs");
    }

    @Test(groups = { "wso2.am" }, description = "API search by group TAG", dependsOnMethods = "testAPISearchByTag")
    public void testAPISearchByTagGroup() throws Exception {
        //create test api 1
        apiRequest = new APIRequest(API_NAME_CONTEXT_UPPER_CASE, API_NAME_CONTEXT_UPPER_CASE, new URL(endpointUrl));
        apiRequest.setTags(TAG_GROUP_UPPER_CASE);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setProvider(providerName);
        //Add api resource
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/add");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);

        //add test api 1
        api3 = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME_CONTEXT_UPPER_CASE, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        watForTagsAvailableOnSearchApi(TAG_GROUP_UPPER_CASE);

        //create test api 2
        apiRequest = new APIRequest(API_NAME_CONTEXT_LOWER_CASE, API_NAME_CONTEXT_LOWER_CASE, new URL(endpointUrl));
        apiRequest.setTags(TAG_GROUP_LOWER_CASE);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setProvider(providerName);
        //Add api resource
        apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/add");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);
        //add test api 2
        api4 = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME_CONTEXT_LOWER_CASE, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        watForTagsAvailableOnSearchApi(TAG_GROUP_LOWER_CASE);

        //create test api 3
        apiRequest = new APIRequest(API_NAME_CONTEXT_WITH_SPACE, API_NAME_CONTEXT_WITH_SPACE, new URL(endpointUrl));
        apiRequest.setTags(TAG_GROUP_WITH_SPACE);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setProvider(providerName);
        //Add api resource
        apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/add");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);
        //add test api 3
        api5 =createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME_CONTEXT_WITH_SPACE, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        watForTagsAvailableOnSearchApi(TAG_GROUP_WITH_SPACE);

        //create test api 4
        apiRequest = new APIRequest(API_NAME_CONTEXT_WITHOUT_SPACE, API_NAME_CONTEXT_WITHOUT_SPACE, new URL(endpointUrl));
        apiRequest.setTags(TAG_GROUP_WITHOUT_SPACE);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setProvider(providerName);
        //Add api resource
        apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/add");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);

        //create and publish test api 4
        api6 =createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME_CONTEXT_WITHOUT_SPACE, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        watForTagsAvailableOnSearchApi(TAG_GROUP_WITHOUT_SPACE);

        TagListDTO tagListDTO = restAPIStore.getAllTags();
        log.info("All tags before assert: " + tagListDTO.toString());
        List<TagDTO> tagList = tagListDTO.getList();
        String[] tt = { TAG_GROUP_UPPER_CASE, TAG_GROUP_LOWER_CASE, TAG_GROUP_WITH_SPACE, TAG_GROUP_WITHOUT_SPACE };
        for (String t : tt) {
            boolean found = false;
            for (TagDTO tag:  tagList) {
                if (t.equals(tag.getValue())) {
                    Assert.assertEquals(tag.getCount().intValue(), 1);
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found, "Tag " + t + " is not available on tag cloud");
        }
    }

    /**
     * Used to wait until published apis are appear in the Store API search API
     *
     * @throws Exception if search API throws any exceptions
     */
    public void watForAPIsAvailableOnSearchApi() throws Exception {
        String searchTerm = "";
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        APIListDTO apiList;
        while (waitTime > System.currentTimeMillis()) {
            apiList = restAPIStore.searchPaginatedAPIs(10, 0, user.getUserDomain(), searchTerm);

            log.info("WAIT for availability of API : " + API_NAME_1 + " and " + API_NAME_2
                    + " found on Store search API");
            if (apiList != null) {
                log.info("APIs Count: " + apiList.getCount());
                int returnCount = 0;
                for(APIInfoDTO apiInfo : apiList.getList()) {
                    if(apiInfo.getName().contains(API_NAME_1)) {
                        returnCount ++;
                    }
                    if(apiInfo.getName().contains(API_NAME_2)) {
                        returnCount ++;
                    }
                }
                if (returnCount == 2) {
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

    /**
     * Used to wait until published apis tags are appear in the Store tag cloud API
     *
     * @throws Exception if tag cloud api throws any exceptions
     */
    public void watForTagsAvailableOnSearchApi(String tag) throws Exception {
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        TagListDTO tagsList;
        boolean found = false;
        while (waitTime > System.currentTimeMillis()) {
            tagsList = restAPIStore.getAllTags();
            log.info("WAIT for availability of tags : " + tag + " found on Store tag cloud");
            if (tagsList != null) {
                log.info("Data: " + tagsList.toString());
                if (tagsList.toString().contains(tag)) {
                    log.info("Tag :" + tag + " found");
                    found = true;
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        Assert.assertTrue(found, tag + " :Tag was not found");
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

}
