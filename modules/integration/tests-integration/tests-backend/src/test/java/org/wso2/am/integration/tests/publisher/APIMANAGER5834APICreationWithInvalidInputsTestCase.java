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

import org.json.JSONObject;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class APIMANAGER5834APICreationWithInvalidInputsTestCase extends APIMIntegrationBaseTest {
    private final String apiNameTest = "APIM18PublisherTest";
    private APIPublisherRestClient apiPublisher;

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
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = { "wso2.am" }, description = "Test API creation with invalid context")
    public void testAPICreationWithInvalidContext() throws Exception {
        String backendEndPoint = getBackendEndServiceEndPointHttp("jaxrs_basic/services/customers/customerservice");
        APIRequest apiRequest = new APIRequest(apiNameTest, "/", new URL(backendEndPoint));
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        JSONObject apiResponse = new JSONObject(serviceResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), "API creation should get an error when creating an API with / context.");
        assertEquals(apiResponse.get("message"), " Context cannot end with '/' character" , "API creation should get an error when creating an API with / context.");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        super.cleanUp();
    }
}