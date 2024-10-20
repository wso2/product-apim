/*
 *Copyright (c) 2005-2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.throttling;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This will API Throttling for APIs.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class APIThrottlingTestCase extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(APIThrottlingTestCase.class);

    private String apiName = "APIThrottleAPI";
    private String apiContext = "api_throttle";
    private String tags = "token, throttling";
    private String description = "This is test API created by API manager integration test";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String applicationName = "APIThrottle-application";
    private String backendURL;
    private String apiId;
    private String applicationId;

    String subscriberUser = "subscriberUser2";
    String subscriberUserWithTenantDomain;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        backendURL = getSuperTenantAPIInvocationURLHttp("api_throttle_backend", "1.0");
        providerName = user.getUserName();
    }

    @Test(groups = {"throttling"}, description = "API Throttling Test", enabled = true)
    public void testAPIThrottling_1() throws APIManagerIntegrationTestException, XPathExpressionException, IOException,
            ApiException, org.wso2.am.integration.clients.store.api.ApiException, ParseException {

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiOperationsDTO.setTarget("/test");
        apiOperationsDTOS.add(apiOperationsDTO);

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(backendURL));

        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);

        //Create application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        Assert.assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);


        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        //get access token
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Assert.assertNotNull("Access Token not found. return token: ", accessToken);

        String invokeURL = getAPIInvocationURLHttps(apiContext);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        log.info("=============================== Headers : " + requestHeaders);
        log.info("=============================== invokeURL : " + invokeURL);

        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/1.0.0/test", requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        //verify throttling
        checkThrottling(accessToken, invokeURL, requestHeaders);
    }

    /**
     * @param accessToken
     * @param invokeURL
     * @param requestHeaders
     */
    private void checkThrottling(String accessToken, String invokeURL, Map<String, String> requestHeaders) {
        int count = 0;
        int limit = 4;
        int numberOfIterations = 4;
        for (; count < numberOfIterations; ++count) {
            try {
                log.info(" =================================== Number of time API Invoked : " + count);
                if (count == limit) {
                    Thread.sleep(10000);
                }
                HttpResponse serviceResponse = callAPI(invokeURL, requestHeaders);
                if (count == limit) {
                    Assert.assertEquals(serviceResponse.getResponseCode(), 429, "Response code is not as expected");
                } else {
                    Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
                }

            } catch (Exception ex) {
                log.error("Error occurred while calling API : " + ex);
                break;
            }
        }
    }

    private HttpResponse callAPI(String invokeURL, Map<String, String> requestHeaders) throws Exception {
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/1.0.0/test", requestHeaders);
        return serviceResponse;

    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}};
    }

    @Factory(dataProvider = "userModeDataProvider")
    public APIThrottlingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
