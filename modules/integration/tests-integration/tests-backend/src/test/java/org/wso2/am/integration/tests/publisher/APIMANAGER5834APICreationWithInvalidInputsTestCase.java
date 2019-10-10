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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.MalformedURLException;
import java.net.URL;


public class APIMANAGER5834APICreationWithInvalidInputsTestCase extends APIMIntegrationBaseTest {
    private final String apiNameTest = "APIM18PublisherTest";

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

    }

    @Test(groups = { "wso2.am" }, description = "Test API creation with invalid context", expectedExceptions = {ApiException.class})
    public void testAPICreationWithInvalidContext()
            throws MalformedURLException, APIManagerIntegrationTestException, ApiException {
        String backendEndPoint = getBackendEndServiceEndPointHttp("jaxrs_basic/services/customers/customerservice");
        APIRequest apiRequest = new APIRequest(apiNameTest, "/", new URL(backendEndPoint));
        restAPIPublisher.addAPI(apiRequest);
    }

}