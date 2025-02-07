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

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.net.URL;

import static org.testng.Assert.*;


/**
 * Change the API Tags and  check how the API are listed under tags.
 */
public class ChangeAPITagsTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "ChangeAPITagsTestCaseAPI";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String OLD_TAGS = "Tag1, Tag2, Tag3";
    private final String NEW_TAGS = "Tag1, Tag2";
    private final String TEST_TAG = "Tag3";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private String apiId;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_NAME.toLowerCase(), new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(API_DESCRIPTION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(OLD_TAGS);

        apiId = createAndPublishAPIWithoutRequireReSubscriptionUsingRest(apiRequest, restAPIPublisher);

        waitForAPIDeploymentSync(apiRequest.getProvider(),
                apiRequest.getName(),
                apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

    }

    @Test(groups = {"wso2.am"}, description = "Test the filter by Tags before changing the Tags")
    public void testFilterByTagsBeforeTagChange()
            throws APIManagerIntegrationTestException, ApiException, InterruptedException {

        APIListDTO apiFilteredWithTagsDTOs;
        boolean isAvailable = false;
        int maxRetry = 10;
        int currentTry = 0;
        do {
            Thread.sleep(2000);
            apiFilteredWithTagsDTOs = restAPIStore.getAPIsFilteredWithTags(TEST_TAG);
            if (apiFilteredWithTagsDTOs != null) {
                isAvailable = true;
                break;
            }

            currentTry++;
        } while (currentTry <= maxRetry);

        assertTrue(isAvailable,
                "API: "+ API_NAME +" is not visible for tag: " + TEST_TAG);
    }


    @Test(groups = {"wso2.am"}, description = "Test the filter by Tags After changing the Tags",
            dependsOnMethods = "testFilterByTagsBeforeTagChange")
    public void testUpdateTagsAndFilterByTags() throws Exception {

        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_NAME.toLowerCase(), new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(API_DESCRIPTION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(NEW_TAGS);
        //Update API with Edited Tags
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiRequest, apiId);
        waitForAPIDeployment();
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid. API Name:" + API_NAME);
        assertTrue(StringUtils.isNotEmpty(updateAPIHTTPResponse.getData()),
                "Error in API Update in API Name:" + API_NAME);
        try {
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        APIListDTO apiFilteredWithTagsDTOs = restAPIStore.getAPIsFilteredWithTags(TEST_TAG);
        assertTrue((apiFilteredWithTagsDTOs == null),
                "API: "+ API_NAME +" is visible for tag: " + TEST_TAG);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }


}


