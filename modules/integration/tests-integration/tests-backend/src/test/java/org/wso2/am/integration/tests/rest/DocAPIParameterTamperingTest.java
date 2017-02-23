/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.rest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

/**
 * Test class is used to test the stack trace exposure of Doc API when the 
 * parameters are tampered.
 */
public class DocAPIParameterTamperingTest extends APIMIntegrationBaseTest{
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    
    @Factory(dataProvider = "userModeDataProvider")
    public DocAPIParameterTamperingTest(TestUserMode userMode) {
        this.userMode = userMode;
    }
    
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }
    
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
    }
    
    @Test(groups = { "wso2.am" }, description = "Test whether the response expose the stack trace")
    public void testParameterTampaeredResponseOfDocAPI() throws Exception {
        String requestURL = publisherURLHttp + "publisher/site/blocks/documentation/ajax/docs.jag?action="
                + "getInlineContent&=&apiName=%3Balert%281%29%27%22%3C%3E&version=1.0.0&docName=asd";
        HttpResponse response = apiPublisher.login(user.getUserName(), user.getPassword());
        
        String cookie = response.getHeaders().get("Set-Cookie");
        requestHeaders.put("Cookie", cookie);
        
        HttpResponse serviceResponse = HttpRequestUtil.doGet(requestURL, requestHeaders);
        JSONObject jsonObject = new JSONObject(serviceResponse.getData());
        System.out.println(serviceResponse.getData());
        assertFalse(serviceResponse.getData().contains("Exception"), "Stack trace is exposed in the error");
        assertEquals(jsonObject.get("error"), true, "Error message is not properly returned");
    }

}
