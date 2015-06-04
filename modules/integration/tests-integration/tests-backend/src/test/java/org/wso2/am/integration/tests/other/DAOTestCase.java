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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.extensions.servers.utils.ClientConnectionUtil;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.ServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DAOTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(DAOTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String providerName ;


    @Factory(dataProvider = "userModeDataProvider")
    public DAOTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttps());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

    }

    private void copySampleFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the other into Jaggery server", e);
        }
    }

    private String computeDestPath(String fileName) {
        String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
        String deploymentPath = serverRoot + "/repository/deployment/server/jaggeryapps/testapp";
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : "
                    + deploymentPath);
        }
        return deploymentPath + File.separator + fileName;
    }

    private String computeSourcePath(String fileName) {

        return getAMResourceLocation()
                + File.separator + "jaggery/" + fileName;
    }

    @Test(groups = { "wso2.am" }, description = "API Life cycle test case")
    public void testDAOTestCase() throws Exception {
        String APIName = "DAOTestAPI";
        String APIContext = "DAOTestAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";

        String APIVersion = "1.0.0";
        String apiContextAddedValue = APIContext + "/" + APIVersion;

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteAPI(APIName, APIVersion, providerName);
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMTestCaseUtils
                .getAPIBeanFromHttpResponse(apiPublisher.getAPI(APIName, providerName));
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        //Test API properties
        assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        assertTrue(apiBean.getContext().contains(apiContextAddedValue), "API context mismatch");
        assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        assertEquals(apiBean.getId().getProviderName(), providerName,
                "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        assertEquals(apiBean.getDescription(), description, "API description mismatch");

        apiStore.addApplication("DAOTestAPI-Application", "Gold", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                storeContext.getContextTenant()
                        .getContextUser()
                        .getUserName().replace("@", "-AT-"));
        subscriptionRequest.setApplicationName("DAOTestAPI-Application");
        apiStore.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("DAOTestAPI-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        Thread.sleep(2000);

    }

    @Test(groups = { "wso2.am" }, description = "Test application object")
    public void testApplication() {
        String fileName = "testPublisher.jag";
        String sourcePath = computeSourcePath(fileName);
        String destinationPath = computeDestPath(fileName);
        copySampleFile(sourcePath, destinationPath);
        ClientConnectionUtil.waitForPort(9763, "");

        String finalOutput = null;

        try {
            URL jaggeryURL = new URL(publisherUrls.getWebAppURLHttp()+"testapp/testPublisher.jag");
            URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    jaggeryServerConnection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                finalOutput = inputLine;
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //    assertNotNull(finalOutput, "Result cannot be null");
        }

    }

    @Test(groups = { "wso2.am" }, description = "Test application operations")
    public void testApplicationOperations() {
        ClientConnectionUtil.waitForPort(9763, "");

        String finalOutput = null;

        try {
            URL jaggeryURL = new URL(publisherUrls.getWebAppURLHttp()+"testapp/testPublisher.jag");
            URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    jaggeryServerConnection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                finalOutput = inputLine;
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //   assertEquals(finalOutput, "test jaggery application value");
        }

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("DAOTestAPI-Application");
//        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
//                      gatewayContext.getContextTenant().getContextUser().getPassword(),
//                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}