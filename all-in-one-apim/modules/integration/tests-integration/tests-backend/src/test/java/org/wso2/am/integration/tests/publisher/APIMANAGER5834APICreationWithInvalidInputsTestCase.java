/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.wso2.am.integration.tests.publisher;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class APIMANAGER5834APICreationWithInvalidInputsTestCase extends APIMIntegrationBaseTest {
    private final String apiNameTest = "APIM18PublisherTest";
    private final String apiProductionEndpointPostfixUrl =
            "jaxrs_basic/services/customers/customerservice/customers/123";
    private final String contextMisMatchErrorMsg = "API Context does not exist";
    private String apiProductionEndPointUrl;
    private String apiId;
    private String apiProviderName;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER5834APICreationWithInvalidInputsTestCase(TestUserMode userMode) {
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
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
    }

    @Test(groups = { "wso2.am" }, description = "Test API creation with invalid context", expectedExceptions = {ApiException.class})
    public void testAPICreationWithInvalidContext()
            throws MalformedURLException, APIManagerIntegrationTestException, ApiException {
        String backendEndPoint = getBackendEndServiceEndPointHttp("jaxrs_basic/services/customers/customerservice");
        APIRequest apiRequest = new APIRequest(apiNameTest, "/", new URL(backendEndPoint));
        restAPIPublisher.addAPI(apiRequest);
    }

    @Test(groups = { "wso2.am" }, description = "Validate if the context matches the previous API version(s)")
    public void testContextMatchesPreviousAPIVersions()
            throws ApiException, MalformedURLException, APIManagerIntegrationTestException {

        APIRequest apiRequest = new APIRequest(apiNameTest, "/test/v1.0.0", new URL(apiProductionEndPointUrl));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(apiProviderName);
        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiCreationResponse.getData();

        APIRequest duplicateRequest = new APIRequest(apiNameTest, "/test/v2.0.0", new URL(apiProductionEndPointUrl));
        duplicateRequest.setVersion("2.0.0");
        duplicateRequest.setProvider(apiProviderName);
        try {
            HttpResponse duplicateApiCreationResponse = restAPIPublisher.addAPI(duplicateRequest);
            restAPIPublisher.deleteAPI(duplicateApiCreationResponse.getData());
            fail("Added an API with invalid context");
        } catch (ApiException e) {
            ApiException apiException = (ApiException) e.getCause();
            assertTrue(apiException.getResponseBody().contains(contextMisMatchErrorMsg), "Invalid API Context");
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        if (apiId != null) {
            restAPIPublisher.deleteAPI(apiId);
        }
    }
}