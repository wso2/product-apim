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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;


/**
 * Change the API Tags and  check how the API are listed under tags.
 */
public class ChangeAPITagsTestCase extends APIManagerLifecycleBaseTest {

    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String TEST_TAG = "Tag3";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private Map<String, String> apiTagsMapBeforeChange;
    private Map<String, String> apiTagsMapAfterChange;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());

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
    public void testFilterByTagsBeforeTagChange()
            throws APIManagerIntegrationTestException, IOException, InterruptedException,
                   XPathExpressionException {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            String apiTags = apiTagEntry.getValue();
            String apiContext = apiName.toLowerCase();
            APIIdentifier apiIdentifier
                    = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
            APICreationRequestBean apiCreationRequestBean =
                    new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                            new URL(apiEndPointUrl));
            apiCreationRequestBean.setTags(apiTags);
            apiCreationRequestBean.setDescription(API_DESCRIPTION + " with tags " + apiTags );

            createAndPublishAPIWithoutRequireReSubscription(apiIdentifier, apiCreationRequestBean,
                    apiPublisherClientUser1);

            waitForAPIDeploymentSync(apiIdentifier.getProviderName(),
                                     apiIdentifier.getApiName(),
                                     apiIdentifier.getVersion(),
                                     APIMIntegrationConstants.IS_API_EXISTS);

            apiStoreClientUser1.waitForSwaggerDocument(apiIdentifier.getProviderName(),
                                                   apiIdentifier.getApiName(),
                                                   apiIdentifier.getVersion(),
                                                   apiTags, executionMode);
        }
        HttpResponse apiPageFilteredWithTagsResponse =
                apiStoreClientUser1.getAPIPageFilteredWithTags(TEST_TAG);
        assertEquals(apiPageFilteredWithTagsResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code wan not" +
                " Ok:200 for retrieving the API page filtered with tags");
        String apiPageFilteredWithTagsResponseString = apiPageFilteredWithTagsResponse.getData();
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {
            String apiLinkToTestInPage =
                    "/store/apis/info?name=" + apiTagEntry.getKey() + "&version=" + API_VERSION_1_0_0 + "&provider=" +
                            providerName + "&tenant=carbon.super&tag=" + TEST_TAG + "";
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
    public void testUpdateTagsAndFilterByTags() throws APIManagerIntegrationTestException, MalformedURLException {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapAfterChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            String apiTags = apiTagEntry.getValue();
            String apiContext = apiName.toLowerCase();
            APICreationRequestBean apiCreationRequestBean =
                    new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                            new URL(apiEndPointUrl));
            apiCreationRequestBean.setTags(apiTags);
            apiCreationRequestBean.setDescription(API_DESCRIPTION);
            //Update API with Edited Tags
            HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
            waitForAPIDeployment();
            assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Update API Response Code is invalid. API Name:" + apiName);
            assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                    "Error in API Update in API Name:" + apiName +
                            "Response Data:" + updateAPIHTTPResponse.getData());
        }
        try {
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        HttpResponse apiPageFilteredWithTagsResponse = apiStoreClientUser1.getAPIPageFilteredWithTags(TEST_TAG);
        assertEquals(apiPageFilteredWithTagsResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code wan not" +
                " Ok:200 for retrieving the API page filtered with tags");
        String apiPageFilteredWithTagsResponseString = apiPageFilteredWithTagsResponse.getData();
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapAfterChange.entrySet()) {
            String apiLinkToTestInPage = "/store/apis/info?name=" + apiTagEntry.getKey() + "&version=" +
                    API_VERSION_1_0_0 + "&provider=" + providerName + "&tenant=carbon.super&tag=" + TEST_TAG + "";
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
    public void cleanUpArtifacts() throws Exception {
        for (Map.Entry<String, String> apiTagEntry : apiTagsMapBeforeChange.entrySet()) {
            String apiName = apiTagEntry.getKey();
            APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
            deleteAPI(apiIdentifier, apiPublisherClientUser1);
        }
        super.cleanUp();
    }


}


