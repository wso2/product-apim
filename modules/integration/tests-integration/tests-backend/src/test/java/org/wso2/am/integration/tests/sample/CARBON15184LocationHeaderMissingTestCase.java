/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.publisher.utils.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.utils.FileManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class will create an api in tenant mode, invoke the created api and
 * looks for the location header of the response
 */

public class CARBON15184LocationHeaderMissingTestCase extends AMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String applicationName = "User-Details";
    private String webAppTargetPath;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        String publisherURLHttp = getPublisherServerURLHttp();
        String storeURLHttp = getStoreServerURLHttp();

        TenantManagementServiceClient tenantManagementServiceClient = new TenantManagementServiceClient(
                apimContext.getContextUrls().getBackEndUrl(), sessionCookie);
        tenantManagementServiceClient.addTenant("wso2.com", "admin", "admin", "Demo");

        String webAppSourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                        File.separator + "webapps" + File.separator + "war" + File.separator + "RESTfulExample.war";
        webAppTargetPath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator
                        + "deployment" + File.separator + "server" + File.separator + "webapps";

        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(apimContext);
        // Copying the web application to CARBON_HOME/repository/deployment/server/webapps.
        FileManager.copyResourceToFileSystem(webAppSourcePath, webAppTargetPath, "RESTfulExample.war");

        // Restarting the server after copying the file to server.
        serverConfigurationManager.restartGracefully();
        // Login back again to the server.
        super.init();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

    }

    @Test(groups = { "wso2.am" }, description = "Test the application header comes in the response")
    public void testLocationHeaderInResponse() throws Exception {
        String provider = "admin-AT-wso2.com";
        String tenantUser = "admin@wso2.com";
        String password = "admin";
        String apiName = "UserDetails";

        APIRequest apiRequest = new APIRequest(apiName, "userdetails",
                new URL("http://localhost:9763/RESTfulExample/users/vip"));

        apiPublisher.login(tenantUser, password);
        apiStore.login(tenantUser, password);

        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, tenantUser,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
        apiStore.addApplication(applicationName, "Gold", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, provider);
        subscriptionRequest.setApplicationName(applicationName);
        apiStore.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest(applicationName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

        Thread.sleep(2000);

        URL obj = new URL(getGatewayServerURLHttp() + "/t/wso2.com/userdetails/1.0.0");
        HttpURLConnection myURLConnection = (HttpURLConnection) obj.openConnection();
        myURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        String header = myURLConnection.getHeaderField("Location");
        assertNotNull(header, "Location header is missing from the response");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(applicationName);
        String webAppFilepathWithFileName = webAppTargetPath + File.separator + "RESTfulExample.war";
        assertTrue(FileManager.deleteFile(webAppFilepathWithFileName), "Web app deleted successfully");
        super.cleanup();
    }
}