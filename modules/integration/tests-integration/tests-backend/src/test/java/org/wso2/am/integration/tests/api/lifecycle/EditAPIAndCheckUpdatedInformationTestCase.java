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
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.bean.APIBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Edit the API information and check whether it is correctly updated.
 */
public class EditAPIAndCheckUpdatedInformationTestCase extends APIManagerLifecycleBaseTest {
    APIIdentifier apiIdentifierAPI1Version1;


    String newTag;
    String apiNewDescription;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.initialize();
        apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, API1_NAME, API_VERSION1);

    }


    @Test(groups = {"wso2.am"}, description = "Edit the API Information")
    public void testEditAPIInformation() throws Exception {
        //Create and publish API version 1.0.0
        createAndPublishAPIWithoutRequireReSubscription(
                apiIdentifierAPI1Version1, API1_CONTEXT, apiPublisherClientUser1);

        //Edit the api
        apiNewDescription = API1_DESCRIPTION + " New Description";
        newTag = "newtag";
        String apiNewTags = API1_TAGS + ", " + newTag;

        //Create the API Request with new edited information
        APIRequest apiRequestBean = new APIRequest(API1_NAME, API1_CONTEXT, new URL(API1_END_POINT_URL));

        apiRequestBean.setTags(apiNewTags);
        apiRequestBean.setDescription(apiNewDescription);
        apiRequestBean.setVersion(API_VERSION1);
        apiRequestBean.setVisibility("public");
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiRequestBean);

        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid." + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Error in API Update in " + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + updateAPIHTTPResponse.getData());

    }

    @Test(groups = {"wso2.am"}, description = "Test whether the updated information available in the publisher ",
            dependsOnMethods = "testEditAPIInformation")
    public void testUpdatedAPIInformationFromAPIPublisher() throws Exception {

        APIBean apiBeanAfterUpdate =
                APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherClientUser1.getApi(
                        API1_NAME, API1_PROVIDER_NAME, API_VERSION1));
        assertEquals(apiBeanAfterUpdate.getDescription(), apiNewDescription, "Updated Description is not available");
        assertTrue(apiBeanAfterUpdate.getTags().contains(newTag), "Newly added Tag is not available");

    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);

    }

}
