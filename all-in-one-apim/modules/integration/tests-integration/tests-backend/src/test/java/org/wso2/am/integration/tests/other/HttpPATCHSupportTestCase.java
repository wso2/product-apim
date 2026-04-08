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

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to check the functionality of sending an HTTP PATCH request and receiving a 200 OK response from
 * the backend.
 */

public class HttpPATCHSupportTestCase extends APIMIntegrationBaseTest {

    private String apiId;
    private String applicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public HttpPATCHSupportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String gatewaySessionCookie = createSession(gatewayContextMgt);

        //Load the back-end dummy API
    }

    @Test(groups = "wso2.am", description = "Check functionality of HTTP PATCH support for APIM")
    public void testHttpPatchSupport() throws Exception {

        String apiName = "HttpPatchAPI";
        String apiContext = "patchTestContext";
        String url = getGatewayURLNhttp() + "httpPatchSupportContext";
        String providerName = user.getUserName();
        String apiVersion = "1.0.0";

        String applicationName = "HttpPatchSupportAPP";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("PATCH");
        apiOperationsDTO.setTarget("/*");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiRequest.setOperationsDTOS(operationsDTOS);

        //add api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        //create an application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName, "Test Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        //Subscribe to the new application
        restAPIStore.createSubscription(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        //get access token
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        //Invoke the API by sending a PATCH request;

        HttpClient client = HttpClientBuilder.create().build();
        HttpPatch request = new HttpPatch(getAPIInvocationURLHttp(apiContext, apiVersion));
        request.setHeader("Accept", "application/json");
        request.setHeader("Authorization", "Bearer " + accessToken);
        StringEntity payload = new StringEntity("{\"first\":\"Greg\"}", "UTF-8");
        payload.setContentType("application/json");
        request.setEntity(payload);

        org.apache.http.HttpResponse httpResponsePatch = client.execute(request);

        //Assertion
        assertEquals(httpResponsePatch.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "The response code is not 200 OK");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

}