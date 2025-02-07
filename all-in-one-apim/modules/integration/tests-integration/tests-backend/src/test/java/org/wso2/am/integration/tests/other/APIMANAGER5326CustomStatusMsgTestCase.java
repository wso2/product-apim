/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.header.util.SimpleSocketServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;

/**
 * Test to check the custom status message response is not dropped in the response for 400
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class APIMANAGER5326CustomStatusMsgTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER5326CustomStatusMsgTestCase.class);
    public static final int PORT = 1989;
    private SimpleSocketServer simpleSocketServer;
    private String apiId;
    private String appId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String expectedResponse = "HTTP/1.1 400 Custom response\r\nServer: testServer\r\n" +
                "Content-Type: text/xml; charset=UTF-8\r\n Transfer-Encoding: chunked\r\n\r\n"
                + "\"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>";
        simpleSocketServer = new SimpleSocketServer(PORT, expectedResponse);
        simpleSocketServer.start();
        Thread.sleep(10000);
    }

    @Test(groups = "wso2.am", description = "testing error responses")
    public void testAPIErrorResponse() throws Exception {

        String apiName = "ErrorResponseCheckAPI";
        String apiVersion = "1.0.0";
        String apiContext = "message";
        String endpointUrl = "http://" + InetAddress.getLocalHost().getHostName() + ":1989";
        String appName = "testApplication";

        try {
            //Add an Application in the Store
            ApplicationDTO applicationDTO = restAPIStore.addApplication(appName,
                    APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
            appId = applicationDTO.getApplicationId();

            //Create the api creation request object
            APIRequest apiRequest;
            apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
            apiRequest.setVersion(apiVersion);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, appId,
                    APIMIntegrationConstants.API_TIER.UNLIMITED);

            //Generate production access token
            ArrayList<String> grantTypes = new ArrayList<>();
            grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
            ApplicationKeyDTO applicationKeyDTO = restAPIStore
                    .generateKeys(appId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                            null, grantTypes);
            String accessToken = applicationKeyDTO.getToken().getAccessToken();
            String apiInvocationUrl = getAPIInvocationURLHttp(apiContext, apiVersion);

            HttpClient httpclient = new DefaultHttpClient();
            HttpUriRequest getRequest1 = new HttpGet(apiInvocationUrl);
            getRequest1.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));
            waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                    APIMIntegrationConstants.IS_API_EXISTS);

            org.apache.http.HttpResponse httpResponse = httpclient.execute(getRequest1);
            Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 400, "Response Code Mismatched");
            Assert.assertEquals(httpResponse.getStatusLine().toString().contains("Custom response"), true,
                    "Response received with Custom Status Message");

        } catch (APIManagerIntegrationTestException e) {
            log.error("APIManagerIntegrationTestException " + e.getMessage(), e);
            Assert.assertTrue(false);
        } catch (XPathExpressionException e) {
            log.error("XPathExpressionException " + e.getMessage(), e);
            Assert.assertTrue(false);
        } catch (IOException e) {
            log.error("IOException " + e.getMessage(), e);
            Assert.assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
