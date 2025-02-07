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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Test to check some security issues in Error responses
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class ErrorResponseCheckTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ErrorResponseCheckTestCase.class);

    private String apiId;
    private String applicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public ErrorResponseCheckTestCase(TestUserMode userMode) {
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

        //Load the back-end dummy API
    }

    @Test(groups = "wso2.am", description = "testing error responses")
    public void testAPIErrorResponse() throws Exception {
        String apiName = "ErrorResponseSecAPI";
        String apiVersion = "1.0.0";
        String apiContext = "sec";
        String endpointUrl = getGatewayURLNhttp() + "response_error";
        String applicationName = "SecApp";
        String providerName = user.getUserName();

        try {
            //Create the api creation request object

            APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
            apiRequest.setVersion(apiVersion);
            apiRequest.setProvider(providerName);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

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

            // Create requestHeaders
            Map<String, String> requestHeaders = new HashMap<String, String>();
            requestHeaders.put("Authorization", "Bearer " + accessToken);

            //Going to access the API with the version in the request url.
            HttpResponse apiInvokeResponse = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp(apiContext, apiVersion), requestHeaders);
            assertEquals(apiInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatched");


            /* -----------------test 1 : invoke with invalid resource path with invalid context --------------- */

            HttpResponse apiInvokeResponseInvalidContext = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp("invalidContext", apiVersion), requestHeaders);
            assertEquals(apiInvokeResponseInvalidContext.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Response Code Mismatched");
            Assert.assertFalse(apiInvokeResponseInvalidContext.getData().contains("invalidContext/1.0.0"),
                    "The message contains the resource path requested.");

            /* ----------------------------test 2 : invoke with invalid access token ---------------------------- */

            // Modified requestHeaders
            Map<String, String> requestHeadersModified = new HashMap<String, String>();
            requestHeaders.put("Authorization", "Bearer invalid_access_token");

            HttpResponse apiInvokeResponseInvalidToken = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp(apiContext, apiVersion), requestHeadersModified);
            assertEquals(apiInvokeResponseInvalidToken.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                    "Response Code Mismatched");
            Assert.assertFalse(apiInvokeResponseInvalidToken.getData().contains("invalid_access_token"),
                    "Access token entered is valid");

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
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
