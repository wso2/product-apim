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

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Edit the API information and check whether it is correctly updated.
 */
public class EditAPIAndCheckUpdatedInformationTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "EditAPIAndCheckUpdatedInformationTest";
    private final String API_CONTEXT = "EditAPIAndCheckUpdatedInformation";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String NEW_API_TAG = "newTag";
    private final String NEW_API_DESCRIPTION = API_DESCRIPTION + " New Description";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIRequest apiRequest;
    private APIIdentifier apiIdentifier;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public EditAPIAndCheckUpdatedInformationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(API_DESCRIPTION);
        apiRequest.setProvider(providerName);
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }

    @Test(groups = { "wso2.am" }, description = "Edit the API Information")
    public void testEditAPIInformation()
            throws APIManagerIntegrationTestException, ApiException, XPathExpressionException, JSONException {
        //add api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        waitForAPIDeploymentSync(providerName, API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        //Edit the api
        String apiNewTags = API_TAGS + ", " + NEW_API_TAG;
        apiRequest.setTags(apiNewTags);
        apiRequest.setDescription(NEW_API_DESCRIPTION);

        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiRequest, apiId);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        waitForAPIDeploymentSync(providerName, API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid. " + getAPIIdentifierString(apiIdentifier));
        assertNotNull(updateAPIHTTPResponse.getData(),
                "Error in API Update in " + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test whether the updated information available in the publisher ",
            dependsOnMethods = "testEditAPIInformation")
    public void testUpdatedAPIInformationFromAPIPublisher() throws APIManagerIntegrationTestException, ApiException {
        HttpResponse httpResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(getValueFromJSON(httpResponse, "description"), NEW_API_DESCRIPTION,
                "Updated Description is not available");
        assertTrue(httpResponse.getData().contains(NEW_API_TAG), "Newly added Tag is not available");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

}
