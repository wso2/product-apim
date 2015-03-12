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

package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;

public class APIResourceModificationTestCase extends AMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        String publisherURLHttp;
        if (isBuilderEnabled()) {
            publisherURLHttp = getPublisherServerURLHttp();

        } else {
            publisherURLHttp = getPublisherServerURLHttp();
        }
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

    }

    @Test(groups = {"wso2.am"}, description = "add scope to resource test case")
    public void testSetScopeToResourceTestCase() throws Exception {

        String APIName = "APIResourceTestAPI";
        String APIContext = "testResAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";

        //add all option methods
        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/" +
                "products/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteApi(APIName, APIVersion, providerName);
        //add assertion
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        //resource are modified by using swagger doc. create the swagger doc with modified
        //information. Similar thing happens when using UI to modify the resources

        String modifiedResource = "{\"apiVersion\":\"1.0.0\",\"swaggerVersion\":\"1.2\"," +
                "\"authorizations\":{\"oauth2\":{\"scopes\":[{\"description\":\"\",\"name\":" +
                "\"testscope\",\"roles\":\"internal/subscriber\",\"key\":\"scopeKey\"}]," +
                "\"type\":\"oauth2\"}},\"apis\":[{\"index\":0,\"file\":{\"apiVersion\":\"1.0.0\"," +
                "\"swaggerVersion\":\"1.2\",\"resourcePath\":\"/default\",\"apis\":[{\"index\":0," +
                "\"path\":\"/*\",\"operations\":[{\"scope\":\"scopeKey\",\"auth_type\":\"None\"," +
                "\"throttling_tier\":\"Unlimited\",\"method\":\"GET\",\"parameters\":[]}," +
                "{\"scope\":\"scopeKey\",\"auth_type\":\"None\",\"throttling_tier\":\"Unlimited\"," +
                "\"method\":\"POST\",\"parameters\":[]},{\"scope\":\"\",\"auth_type\":\"None\"," +
                "\"throttling_tier\":\"Unlimited\",\"method\":\"PUT\",\"parameters\":[]}," +
                "{\"auth_type\":\"None\",\"throttling_tier\":\"Unlimited\",\"method\":\"DELETE\"," +
                "\"parameters\":[]},{\"auth_type\":\"None\",\"throttling_tier\":\"Unlimited\"," +
                "\"method\":\"OPTIONS\",\"parameters\":[]}]}]},\"description\":\"\",\"path\":" +
                "\"/default\"}],\"info\":{\"title\":\"" + APIName + "\",\"termsOfServiceUrl\":\"" +
                "\",\"description\":\"\",\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}";

        HttpResponse response = apiPublisher.updateResourceOfAPI(providerName, APIName, APIVersion, modifiedResource);

        JSONObject jsonObject = new JSONObject(response.getData());
        boolean error = (Boolean) jsonObject.get("error");
        assertEquals(error, false, "Modifying resources failed for API");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}
