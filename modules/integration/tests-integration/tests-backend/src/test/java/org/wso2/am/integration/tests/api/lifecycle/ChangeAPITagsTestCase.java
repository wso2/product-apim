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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;


/**
 * Change the API Tags and  check how the API are listed under tags.
 */
public class ChangeAPITagsTestCase extends APIManagerLifecycleBaseTest {
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_END_POINT_METHOD = "/most_popular";
    private static final String API_RESPONSE_DATA = "<feed";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private String providerName;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private Map<String, String> apiTagsMapBeforeChange;
    private Map<String, String> apiTagsMapAfterChange;
    private static final String TEST_TAG = "Tag3";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        providerName = apimContext.getContextTenant().getContextUser().getUserName();
        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreServerURLHttp());

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiTagsMapBeforeChange = new HashMap<String, String>();
        apiTagsMapBeforeChange.put("APITagTest1", "Tag1, Tag2, Tag3");
        apiTagsMapBeforeChange.put("APITagTest2", "Tag2, Tag3, Tag4");
        apiTagsMapBeforeChange.put("APITagTest3", "Tag1, Tag3, Tag5");
        apiTagsMapBeforeChange.put("APITagTest4", "Tag1, Tag2");

        apiTagsMapAfterChange = new HashMap<String, String>();
        apiTagsMapAfterChange.put("APITagTest1", "Tag1, Tag2");
        apiTagsMapAfterChange.put("APITagTest2", "Tag2, Tag4");
        apiTagsMapAfterChange.put("APITagTest3", "Tag1, Tag5");
        apiTagsMapAfterChange.put("APITagTest4", "Tag1, Tag2");

    }


    @Test(groups = {"wso2.am"}, description = "Test the filter by Tags before changing the Tags")
    public void testFilterByTagsBeforeTagChange() throws Exception {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            String apiTags = apiTagEntry.getValue();
            String apiContext = apiName.toLowerCase();
            APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
            APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
            apiCreationRequestBean.setTags(apiTags);
            apiCreationRequestBean.setDescription(API_DESCRIPTION);
            createAndPublishAPIWithoutRequireReSubscription(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1);

        }

        HttpResponse apiPageFilteredWithTagsResponse = apiStoreClientUser1.getAPIPageFilteredWithTags("Tag3");
        assertEquals(apiPageFilteredWithTagsResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "");
        String apiPageFilteredWithTagsResponseString = apiPageFilteredWithTagsResponse.getData();

        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {

            String apiLinkToTestInPage = "/store/apis/info?name=" + apiTagEntry.getKey() + "&version=" + API_VERSION_1_0_0 +
                    "&provider=" + providerName + "&tenant=carbon.super&tag=" + TEST_TAG + "";
            if (apiTagEntry.getValue().contains(TEST_TAG)) {
                //API Link should be in page
                assertTrue(apiPageFilteredWithTagsResponseString.contains(apiLinkToTestInPage),
                        "API is not listed  with correct tag, API:" + apiTagEntry.getKey() + " Tag:" + TEST_TAG);
            } else {
                //API Link should not be in page
                assertFalse(apiPageFilteredWithTagsResponseString.contains(apiLinkToTestInPage),
                        "API is  listed with incorrect tag, API:" + apiTagEntry.getKey() + " Tag:" + TEST_TAG);
            }
        }
    }


    @Test(groups = {"wso2.am"}, description = "Test the filter by Tags After changing the Tags",
            dependsOnMethods = "testFilterByTagsBeforeTagChange")
    public void testUpdateTagsAndFilterByTags() throws Exception {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapAfterChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            String apiTags = apiTagEntry.getValue();
            String apiContext = apiName.toLowerCase();
            APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
            apiCreationRequestBean.setTags(apiTags);
            apiCreationRequestBean.setDescription(API_DESCRIPTION);
            //Update API with Edited Tags
            HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
            assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Update API Response Code is invalid. API Name:" + apiName);
            assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                    "Error in API Update in API Name:" + apiName +
                            "Response Data:" + updateAPIHTTPResponse.getData());

        }
        HttpResponse apiPageFilteredWithTagsResponse = apiStoreClientUser1.getAPIPageFilteredWithTags("Tag3");
        assertEquals(apiPageFilteredWithTagsResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "");
        String apiPageFilteredWithTagsResponseString = apiPageFilteredWithTagsResponse.getData();

        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {

            String apiLinkToTestInPage = "/store/apis/info?name=" + apiTagEntry.getKey() + "&version=" + API_VERSION_1_0_0 +
                    "&provider=" + providerName + "&tenant=carbon.super&tag=" + TEST_TAG + "";
            if (apiTagEntry.getValue().contains(TEST_TAG)) {
                //API Link should be in page
                assertTrue(apiPageFilteredWithTagsResponseString.contains(apiLinkToTestInPage),
                        " API is not listed  with correct tag, API:" + apiTagEntry.getKey() + " Tag:" + TEST_TAG);
            } else {
                //API Link should not be in page
                assertFalse(apiPageFilteredWithTagsResponseString.contains(apiLinkToTestInPage),
                        "API is  listed with incorrect tag, API:" + apiTagEntry.getKey() + " Tag:" + TEST_TAG);
            }

        }

    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
            deleteAPI(apiIdentifier, apiPublisherClientUser1);
        }
    }


}


