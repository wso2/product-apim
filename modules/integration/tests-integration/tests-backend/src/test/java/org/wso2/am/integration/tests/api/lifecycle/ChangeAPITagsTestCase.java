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
import org.wso2.am.integration.test.utils.bean.APIRequest;
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


    private APIIdentifier apiIdentifierAPI1Version1;
    private Map<String, String> apiTagsMapBeforeChange;
    private Map<String, String> apiTagsMapAfterChange;
    private static final String TEST_TAG = "Tag3";

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.initialize();
        apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, API1_NAME, API_VERSION1);

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
            APIIdentifier apiIdentifier = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);

            createAndPublishAPIWithoutRequireReSubscription(apiIdentifier, apiContext, apiTags, apiPublisherClientUser1);

        }


        HttpResponse apiPageFilteredWithTagsResponse = apiStoreClientUser1.getAPIPageFilteredWithTags("Tag3");
        assertEquals(apiPageFilteredWithTagsResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "");
        String apiPageFilteredWithTagsResponseString = apiPageFilteredWithTagsResponse.getData();

        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {

            String apiLinkToTestInPage = "/store/apis/info?name=" + apiTagEntry.getKey() + "&version=" + API_VERSION1 +
                    "&provider=" + API1_PROVIDER_NAME + "&tenant=carbon.super&tag=" + TEST_TAG + "";
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

            APIRequest apiRequestBean = new APIRequest(apiName, apiContext, new URL(API1_END_POINT_URL));

            apiRequestBean.setTags(apiTags);
            apiRequestBean.setDescription(API1_DESCRIPTION);
            apiRequestBean.setVersion(API_VERSION1);
            apiRequestBean.setVisibility("public");

            //Update API with Edited Tags
            HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiRequestBean);

            assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Update API Response Code is invalid." + getAPIIdentifierString(apiIdentifierAPI1Version1));
            assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                    "Error in API Update in " + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                            "Response Data:" + updateAPIHTTPResponse.getData());

        }


        HttpResponse apiPageFilteredWithTagsResponse = apiStoreClientUser1.getAPIPageFilteredWithTags("Tag3");
        assertEquals(apiPageFilteredWithTagsResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "");
        String apiPageFilteredWithTagsResponseString = apiPageFilteredWithTagsResponse.getData();

        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {

            String apiLinkToTestInPage = "/store/apis/info?name=" + apiTagEntry.getKey() + "&version=" + API_VERSION1 +
                    "&provider=" + API1_PROVIDER_NAME + "&tenant=carbon.super&tag=" + TEST_TAG + "";
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
    public void destroy() throws Exception {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            APIIdentifier apiIdentifier = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);
            deleteAPI(apiIdentifier, apiPublisherClientUser1);
        }
    }


}


