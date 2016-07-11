/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.other;

import org.apache.commons.httpclient.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;

import java.net.URL;

public class APIMANAGER2611EndpointValidationTestCase extends APIManagerLifecycleBaseTest {

    private APIIdentifier apiIdentifier;
    private APIPublisherRestClient apiPublisherRestClient;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER2611EndpointValidationTestCase(TestUserMode userMode) {
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
        apiPublisherRestClient = new APIPublisherRestClient(getPublisherURLHttp());
        apiPublisherRestClient.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = {"wso2.am"}, description = "Validate endpoint with Http Head not support End point")
    public void checkEndpointValidation() throws Exception {
        String APIName = "APIMANAGER2611testAPI";
        String APIContext = "/testEndpointValid";
        //Service which does not support HTTP HEAD
        String endPointToValidate = getGatewayURLHttp() + "oauth2/token";
        String providerName = user.getUserName();
        String APIVersion = "1.0.0";
        apiIdentifier = new APIIdentifier(providerName, APIName, APIVersion);

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(endPointToValidate));
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //Adding the API to the publisher
        apiPublisherRestClient.addAPI(apiRequest);

        HttpResponse response = apiPublisherRestClient
                .checkValidEndpoint("http", endPointToValidate, providerName, APIName, APIVersion);
        int statusCode = response.getResponseCode();
        Assert.assertEquals(statusCode, HttpStatus.SC_OK, "response code mismatched");
        String responseString = response.getData();
        Assert.assertTrue(responseString.contains("success"), "Invalid end point " + endPointToValidate
                                                              + ":" + responseString);

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        deleteAPI(apiIdentifier, apiPublisherRestClient);
        super.cleanUp();
    }
}
