/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;


import java.io.File;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Add new Log mediation to the in-flow and check the logs to verify the  added mediation is working.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class AddNewMediationAndInvokeAPITestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "AddNewMediationAndInvokeAPITest";
    private final String API_CONTEXT = "AddNewMediationAndInvokeAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_END_POINT_POSTFIX_URL = "CxfRestService-1.0.0-SNAPSHOT/rest/employeeservices/10/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AddNewMediationAndInvokeAPI";
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifier;
    private String accessToken;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        String apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        String providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");

        String sessionId = createSession(gatewayContextMgt);
        deployArrService(gatewayContextMgt.getContextUrls().getBackEndUrl(), sessionId);

        boolean isWebAppDeployed = WebAppDeploymentUtil.isWebApplicationDeployed(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                                                                                 sessionId, "CxfRestService-1.0.0-SNAPSHOT");
        assertTrue(isWebAppDeployed, "Web Application Not Deployed Correctly.");

        accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
    }


    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the log mediation")
    public void testAPIInvocationBeforeAddingNewMediation() throws Exception    {
        //Create publish and subscribe a API
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);

        //Send GET Request

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization" , "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response.getHeaders("Content-Type")[0].getValue(), "application/xml");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the log mediation",
            dependsOnMethods = "testAPIInvocationBeforeAddingNewMediation")
    public void testAPIInvocationAfterAddingNewMediation() throws Exception  {
        apiCreationRequestBean.setOutSequence("xml_to_json_out_message");
        apiPublisherClientUser1.updateAPI(apiCreationRequestBean);

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization" , "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response.getHeaders("Content-Type")[0].getValue(), "application/json; charset=UTF-8");
    }


    @Test(groups = {"wso2.am"}, description = "IInvoke the API after removing the log mediation",
            dependsOnMethods = "testAPIInvocationAfterAddingNewMediation")
    public void testAPIInvocationBeforeRemovingNewMediation() throws Exception {
        apiCreationRequestBean.setOutSequence("");
        apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        //Send GET Request
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization" , "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response.getHeaders("Content-Type")[0].getValue(), "application/xml");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }

    public void deployArrService(String backEndUrl, String sessionCookie) throws  Exception {

        String filePath = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                          File.separator + "sequence" + File.separator + "CxfRestService-1.0.0-SNAPSHOT.war";

        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(backEndUrl, sessionCookie);
        webAppAdminClient.uploadWarFile(filePath);


    }


}
