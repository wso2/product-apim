/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.GenerateAppKeyRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;

import org.wso2.am.integration.test.utils.publisher.utils.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
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

public class DAOTestCase extends AMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(DAOTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiPublisher = new APIPublisherRestClient(getServerURLHttp());
        apiStore = new APIStoreRestClient(getServerURLHttp());

        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                           apimContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                       apimContext.getContextTenant().getContextUser().getPassword());

    }

    private void copySampleFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the sample into Jaggery server", e);
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

        String sourcePath = getAMResourceLocation()
                            + File.separator + "jaggery/" + fileName;
        return sourcePath;
    }

    @Test(groups = { "wso2.am" }, description = "API Life cycle test case")
    public void testDAOTestCase() throws Exception {
        String APIName = "DAOTestAPI";
        String APIContext = "DAOTestAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";

        // This is because with the new context version strategy, if the context does not have the {version} param ,
        // then we add the {version} param to the end of the context.
        String apiContextAddedValue = APIContext + "/" + APIVersion;

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteApi(APIName, APIVersion, providerName);
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMgtTestUtil
                .getAPIBeanFromHttpResponse(apiPublisher.getApi(APIName, providerName));
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
        //Test API properties
        Assert.assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        Assert.assertEquals(
                apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1),
                apiContextAddedValue, "API context mismatch");
        Assert.assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        Assert.assertEquals(apiBean.getId().getProviderName(), providerName,
                            "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            Assert.assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        Assert.assertEquals(apiBean.getDescription(), description, "API description mismatch");

        apiStore.addApplication("DAOTestAPI-Application", "Gold", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                                                                          apimContext.getContextTenant()
                                                                                  .getContextUser()
                                                                                  .getUserName());
        subscriptionRequest.setApplicationName("DAOTestAPI-Application");
        apiStore.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest =
                new GenerateAppKeyRequest("DAOTestAPI-Application");
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
            URL jaggeryURL = new URL(getPublisherServerURLHttp()+"/testapp/testPublisher.jag");
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
            URL jaggeryURL = new URL(getPublisherServerURLHttp()+"/testapp/testPublisher.jag");
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
        super.cleanup();
    }
}