/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.search;

import com.google.gson.Gson;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DevPortalSearchTest extends APIMIntegrationBaseTest {
    private String contextUsername = "admin";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice";
    private final String API_GOOGLE = "Google";
    private final String API_PIZZA = "PizzaShackAPI";
    private final String API_OTHER = "Other";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private String api1Id;
    private String api2Id;
    private String api3Id;

    @Factory(dataProvider = "userModeDataProvider")
    public DevPortalSearchTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        userManagementClient = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        /* Create API_GOOGLE */
        APIRequest apiRequest = new APIRequest(API_GOOGLE, API_GOOGLE, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(user.getUserName());

        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        api1Id = response.getData();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(api1Id);
        Gson g = new Gson();
        APIDTO api1dto = g.fromJson(createdApiResponse.getData(), APIDTO.class);

        // List API tags
        List<String> tags = new ArrayList<>();
        tags.add("google");
        tags.add("Sample APIs - New");

        api1dto.setTags(tags);
        // Update the API
        restAPIPublisher.updateAPI(api1dto, api1Id);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(api1Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(api1Id, false);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        /* Create API_PIZZA */
        apiRequest = new APIRequest(API_PIZZA, API_PIZZA, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(user.getUserName());

        response = restAPIPublisher.addAPI(apiRequest);
        api2Id = response.getData();

        createdApiResponse = restAPIPublisher.getAPI(api2Id);
        g = new Gson();
        APIDTO api2dto = g.fromJson(createdApiResponse.getData(), APIDTO.class);

        // List API tags
        tags = new ArrayList<>();
        tags.add("pizza");

        api2dto.setTags(tags);
        // Update the API
        restAPIPublisher.updateAPI(api2dto, api2Id);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(api2Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(api2Id, false);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        /* Create API_OTHER */
        apiRequest = new APIRequest(API_OTHER, API_OTHER, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(user.getUserName());

        response = restAPIPublisher.addAPI(apiRequest);
        api3Id = response.getData();

        createdApiResponse = restAPIPublisher.getAPI(api3Id);
        g = new Gson();
        APIDTO api3dto = g.fromJson(createdApiResponse.getData(), APIDTO.class);

        // List API tags
        tags = new ArrayList<>();
        tags.add("other");

        api3dto.setTags(tags);
        // Update the API
        restAPIPublisher.updateAPI(api3dto, api3Id);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(api3Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(api3Id, false);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(description = "Test the DevPortal search functionality using filters")
    public void testDevPortalAPISearch() throws Exception {
        APIListDTO searchQuery1 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:Sample APIs - New");
        Assert.assertNotNull(searchQuery1.getCount());
        Assert.assertEquals((int) searchQuery1.getCount(), 1);
        Assert.assertNotNull(searchQuery1.getList());
        Assert.assertEquals(searchQuery1.getList().get(0).getName(), "Google");

        APIListDTO searchQuery2 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:Sample APIs - New name:Google");
        Assert.assertNotNull(searchQuery2.getCount());
        Assert.assertEquals((int) searchQuery2.getCount(), 1);
        Assert.assertNotNull(searchQuery2.getList());
        Assert.assertEquals(searchQuery2.getList().get(0).getName(), "Google");

        APIListDTO searchQuery3 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "Google tags:Sample APIs - New");
        Assert.assertNotNull(searchQuery3.getCount());
        Assert.assertEquals((int) searchQuery3.getCount(), 1);
        Assert.assertNotNull(searchQuery3.getList());
        Assert.assertEquals(searchQuery3.getList().get(0).getName(), "Google");

        APIListDTO searchQuery4 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "Google tags:Sample APIs - New tags:pizza");
        Assert.assertNotNull(searchQuery4.getCount());
        Assert.assertEquals((int) searchQuery4.getCount(), 1);
        Assert.assertNotNull(searchQuery4.getList());
        Assert.assertEquals(searchQuery4.getList().get(0).getName(), "Google");

        APIListDTO searchQuery5 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:piZza");
        Assert.assertNull(searchQuery5);

        APIListDTO searchQuery6 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "google tags:Sample APIs - New");
        Assert.assertNotNull(searchQuery6.getCount());
        Assert.assertEquals((int) searchQuery6.getCount(), 1);
        Assert.assertNotNull(searchQuery6.getList());
        Assert.assertEquals(searchQuery6.getList().get(0).getName(), "Google");

        APIListDTO searchQuery7 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "Google tags:Google");
        Assert.assertNull(searchQuery7);

        APIListDTO searchQuery8 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:Sample APIs - New tags:other tags:pizza");
        Assert.assertNotNull(searchQuery8.getCount());
        Assert.assertEquals((int) searchQuery8.getCount(), 3);

        APIListDTO searchQuery9 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "name:Google name:PizzaShackAPI name:Other");
        Assert.assertNotNull(searchQuery9.getCount());
        Assert.assertEquals((int) searchQuery9.getCount(), 3);

        APIListDTO searchQuery10 = restAPIStore.searchPaginatedAPIs(10, 0, MultitenantUtils
                        .getTenantDomain(contextUsername),
                "name:Google name:PizzaShackAPI name:Other tags:Sample APIs - New tags:other");
        Assert.assertNotNull(searchQuery10.getCount());
        Assert.assertEquals((int) searchQuery10.getCount(), 2);

        APIListDTO searchQuery11 = restAPIStore.searchPaginatedAPIs(10, 0, MultitenantUtils
                        .getTenantDomain(contextUsername),
                "name:google name:pizzashackapi name:other tags:Sample APIs - New tags:pizza tags:other");
        Assert.assertNotNull(searchQuery11.getCount());
        Assert.assertEquals((int) searchQuery11.getCount(), 3);

        APIListDTO searchQuery12 = restAPIStore.searchPaginatedAPIs(10, 0, MultitenantUtils
                        .getTenantDomain(contextUsername),
                "name:google tags:Sample APIs - New tags:pizza name:pizzashackapi name:abc");
        Assert.assertNotNull(searchQuery12.getCount());
        Assert.assertEquals((int) searchQuery12.getCount(), 2);

        APIListDTO searchQuery13 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "tag:Sample APIs - New");
        Assert.assertNotNull(searchQuery13.getCount());
        Assert.assertEquals((int) searchQuery13.getCount(), 1);
        Assert.assertNotNull(searchQuery13.getList());
        Assert.assertEquals(searchQuery13.getList().get(0).getName(), "Google");

        APIListDTO searchQuery14 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "tag:Sample APIs - New name:Google");
        Assert.assertNotNull(searchQuery14.getCount());
        Assert.assertEquals((int) searchQuery14.getCount(), 1);
        Assert.assertNotNull(searchQuery14.getList());
        Assert.assertEquals(searchQuery14.getList().get(0).getName(), "Google");

        APIListDTO searchQuery15 = restAPIStore.searchPaginatedAPIs(10, 0,
                MultitenantUtils.getTenantDomain(contextUsername), "google tag:Sample APIs - New");
        Assert.assertNotNull(searchQuery15.getCount());
        Assert.assertEquals((int) searchQuery15.getCount(), 1);
        Assert.assertNotNull(searchQuery15.getList());
        Assert.assertEquals(searchQuery15.getList().get(0).getName(), "Google");
    }

    @Test(description = "Test the DevPortal search functionality using filters")
    public void testDevPortalSubscriptionManagementAPISearch() throws Exception {
        APIListDTO searchQuery1 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:Sample APIs - New");
        Assert.assertNotNull(searchQuery1.getCount());
        Assert.assertEquals((int) searchQuery1.getCount(), 1);
        Assert.assertNotNull(searchQuery1.getList());
        Assert.assertEquals(searchQuery1.getList().get(0).getName(), "Google");

        APIListDTO searchQuery2 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:Sample APIs - New name:Google");
        Assert.assertNotNull(searchQuery2.getCount());
        Assert.assertEquals((int) searchQuery2.getCount(), 1);
        Assert.assertNotNull(searchQuery2.getList());
        Assert.assertEquals(searchQuery2.getList().get(0).getName(), "Google");

        APIListDTO searchQuery3 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "Google tags:Sample APIs - New");
        Assert.assertNotNull(searchQuery3.getCount());
        Assert.assertEquals((int) searchQuery3.getCount(), 1);
        Assert.assertNotNull(searchQuery3.getList());
        Assert.assertEquals(searchQuery3.getList().get(0).getName(), "Google");

        APIListDTO searchQuery4 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "Google tags:Sample APIs - New tags:pizza");
        Assert.assertNotNull(searchQuery4.getCount());
        Assert.assertEquals((int) searchQuery4.getCount(), 1);
        Assert.assertNotNull(searchQuery4.getList());
        Assert.assertEquals(searchQuery4.getList().get(0).getName(), "Google");

        APIListDTO searchQuery5 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:piZza");
        Assert.assertNotNull(searchQuery5);
        Assert.assertNotNull(searchQuery5.getCount());
        Assert.assertEquals((int) searchQuery5.getCount(), 0);

        APIListDTO searchQuery6 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "google tags:Sample APIs - New");
        Assert.assertNotNull(searchQuery6.getCount());
        Assert.assertEquals((int) searchQuery6.getCount(), 1);
        Assert.assertNotNull(searchQuery6.getList());
        Assert.assertEquals(searchQuery6.getList().get(0).getName(), "Google");

        APIListDTO searchQuery7 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "Google tags:Google");
        Assert.assertNotNull(searchQuery7);
        Assert.assertNotNull(searchQuery7.getCount());
        Assert.assertEquals((int) searchQuery7.getCount(), 0);

        APIListDTO searchQuery8 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "tags:Sample APIs - New tags:other tags:pizza");
        Assert.assertNotNull(searchQuery8.getCount());
        Assert.assertEquals((int) searchQuery8.getCount(), 3);

        APIListDTO searchQuery9 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "name:Google name:PizzaShackAPI name:Other");
        Assert.assertNotNull(searchQuery9.getCount());
        Assert.assertEquals((int) searchQuery9.getCount(), 3);

        APIListDTO searchQuery10 = restAPIStore.getAPIs(0, 10, MultitenantUtils
                        .getTenantDomain(contextUsername),
                "name:Google name:PizzaShackAPI name:Other tags:Sample APIs - New tags:other");
        Assert.assertNotNull(searchQuery10.getCount());
        Assert.assertEquals((int) searchQuery10.getCount(), 2);

        APIListDTO searchQuery11 = restAPIStore.getAPIs(0, 10, MultitenantUtils
                        .getTenantDomain(contextUsername),
                "name:google name:pizzashackapi name:other tags:Sample APIs - New tags:pizza tags:other");
        Assert.assertNotNull(searchQuery11.getCount());
        Assert.assertEquals((int) searchQuery11.getCount(), 3);

        APIListDTO searchQuery12 = restAPIStore.getAPIs(0, 10, MultitenantUtils
                        .getTenantDomain(contextUsername),
                "name:google tags:Sample APIs - New tags:pizza name:pizzashackapi name:abc");
        Assert.assertNotNull(searchQuery12.getCount());
        Assert.assertEquals((int) searchQuery12.getCount(), 2);

        APIListDTO searchQuery13 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "tag:Sample APIs - New");
        Assert.assertNotNull(searchQuery13.getCount());
        Assert.assertEquals((int) searchQuery13.getCount(), 1);
        Assert.assertNotNull(searchQuery13.getList());
        Assert.assertEquals(searchQuery13.getList().get(0).getName(), "Google");

        APIListDTO searchQuery14 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "tag:Sample APIs - New name:Google");
        Assert.assertNotNull(searchQuery14.getCount());
        Assert.assertEquals((int) searchQuery14.getCount(), 1);
        Assert.assertNotNull(searchQuery14.getList());
        Assert.assertEquals(searchQuery14.getList().get(0).getName(), "Google");

        APIListDTO searchQuery15 = restAPIStore.getAPIs(0, 10,
                MultitenantUtils.getTenantDomain(contextUsername), "google tag:Sample APIs - New");
        Assert.assertNotNull(searchQuery15.getCount());
        Assert.assertEquals((int) searchQuery15.getCount(), 1);
        Assert.assertNotNull(searchQuery15.getList());
        Assert.assertEquals(searchQuery15.getList().get(0).getName(), "Google");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(api1Id);
        restAPIPublisher.deleteAPI(api2Id);
        restAPIPublisher.deleteAPI(api3Id);
    }
}
