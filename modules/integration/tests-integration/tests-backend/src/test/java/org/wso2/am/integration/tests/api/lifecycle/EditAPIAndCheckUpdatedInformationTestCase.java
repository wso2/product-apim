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
import org.wso2.am.integration.test.utils.bean.APIBean;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Edit the API information and check whether it is correctly updated.
 */
public class EditAPIAndCheckUpdatedInformationTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "EditAPIAndCheckUpdatedInformationTest";
    private static final String API_CONTEXT = "EditAPIAndCheckUpdatedInformation";
    private static final String API_TAGS = "testTag1, testTag2, testTag3";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String NEW_API_TAG = "newTag";
    private static final String NEW_API_DESCRIPTION = API_DESCRIPTION + " New Description";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APICreationRequestBean apiCreationRequestBean;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        APIStoreRestClient apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }


    @Test(groups = {"wso2.am"}, description = "Edit the API Information")
    public void testEditAPIInformation() throws APIManagerIntegrationTestException {
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, false);
        //Edit the api
        String apiNewTags = API_TAGS + ", " + NEW_API_TAG;
        apiCreationRequestBean.setTags(apiNewTags);
        apiCreationRequestBean.setDescription(NEW_API_DESCRIPTION);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Error in API Update in " + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + updateAPIHTTPResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test whether the updated information available in the publisher ",
            dependsOnMethods = "testEditAPIInformation")
    public void testUpdatedAPIInformationFromAPIPublisher() throws APIManagerIntegrationTestException {
        APIBean apiBeanAfterUpdate =
                APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherClientUser1.getApi(
                        API_NAME, providerName, API_VERSION_1_0_0));
        assertEquals(apiBeanAfterUpdate.getDescription(), NEW_API_DESCRIPTION, "Updated Description is not available");
        assertTrue(apiBeanAfterUpdate.getTags().contains(NEW_API_TAG), "Newly added Tag is not available");

    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }

}
