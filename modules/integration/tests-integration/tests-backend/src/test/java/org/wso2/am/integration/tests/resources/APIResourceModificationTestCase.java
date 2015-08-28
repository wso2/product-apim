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

package org.wso2.am.integration.tests.resources;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;

public class APIResourceModificationTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private String APIName = "APIResourceTestAPI";
    private String APIVersion = "1.0.0";
    private String providerName = "";

    @Factory(dataProvider = "userModeDataProvider")
    public APIResourceModificationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String publisherURLHttp = publisherUrls.getWebAppURLHttps();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(groups = {"wso2.am"}, description = "add scope to resource test case")
    public void testSetScopeToResourceTestCase() throws Exception {

        String APIContext = "testResAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";

        //add all option methods
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiRequest.setProvider(providerName);
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteAPI(APIName, APIVersion, providerName);
        //add assertion
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        // resource are modified by using swagger doc. create the swagger doc
        // with modified
        // information. Similar thing happens when using UI to modify the
        // resources

        String modifiedResource = "{\"paths\":{\"/*\":{\"put\":{\"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"None\",\"x-throttling-tier\":\"Unlimited\"},\"post\":{\"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"None\",\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"scopeKey\"},\"get\":{\"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"None\",\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"scopeKey\"},\"delete\":{\"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"None\",\"x-throttling-tier\":\"Unlimited\"},\"options\":{\"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"None\",\"x-throttling-tier\":\"Unlimited\"}}}," +
                "\"swagger\":\"2.0\",\"info\":{\"title\":\"APIResourceTestAPI\",\"version\":\"1.0.0\"}," +
                "\"x-wso2-security\":{\"apim\":{\"x-wso2-scopes\":[{\"name\":\"testscope\",\"description\":\"\",\"key\":\"scopeKey\",\"roles\":\"internal/subscriber\"}]}}}";

        HttpResponse response = apiPublisher.updateResourceOfAPI(providerName, APIName, APIVersion, modifiedResource);

        JSONObject jsonObject = new JSONObject(response.getData());
        boolean error = (Boolean) jsonObject.get("error");
        assertEquals(error, false, "Modifying resources failed for API");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                      gatewayContext.getContextTenant().getContextUser().getPassword(),
                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}
