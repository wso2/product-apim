/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * This class tests the behaviour of API when there is choice of selection between oauth2 and mutual ssl in API Manager.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APISecurityTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "mutualsslAPI";
    private final String API_NAME_2 = "mutualsslAPI2";
    private final String API_CONTEXT = "mutualsslAPI";
    private final String API_CONTEXT_2 = "mutualsslAPI2";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private String accessToken;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String applicationId;
    private String apiId1, apiId2;

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, IOException, ApiException, org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException, AutomationUtilException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest1 = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest1.setVersion(API_VERSION_1_0_0);
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTags(API_TAGS);
        apiRequest1.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest1.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("mutualssl");
        apiRequest1.setSecurityScheme(securitySchemes);

        HttpResponse response1 = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response1.getData();

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certOne), "example", apiId1, APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIRequest apiRequest2 = new APIRequest(API_NAME_2, API_CONTEXT_2, new URL(apiEndPointUrl));
        apiRequest2.setVersion(API_VERSION_1_0_0);
        apiRequest2.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTags(API_TAGS);
        apiRequest2.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest2.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes2 = new ArrayList<>();
        securitySchemes2.add("mutualssl");
        securitySchemes2.add("oauth2");
        apiRequest2.setSecurityScheme(securitySchemes2);

        HttpResponse response2 = restAPIPublisher.addAPI(apiRequest2);
        apiId2 = response2.getData();

        String certTwo = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "abcde.crt";
        restAPIPublisher.uploadCertificate(new File(certTwo), "abcde", apiId2, APIMIntegrationConstants.API_TIER.UNLIMITED);

        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction());
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);

        applicationId = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiId2, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(description = "This test case tests the behaviour of APIs that are protected with mutual SSL and OAuth2 "
            + "when the client certificate is not presented but OAuth2 token is presented.")
    public void testCreateAndPublishAPIWithOAuth2() throws XPathExpressionException, IOException, JSONException {
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        JSONObject response = new JSONObject(apiResponse.getData());
        //fix test failure due to error code changes introduced in product-apim pull #7106
        assertEquals(response.getJSONObject("fault").getInt("code"), 900901,
                "API invocation succeeded with the access token without need for mutual ssl");
        apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

//    @Test(description =  "This method tests the behaviour of APIs that are protected with mutual SSL and when the "
//            + "authentication is done using mutual SSL", dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
//    public void testAPIInvocationWithMutualSSL()
//            throws IOException, XPathExpressionException, InterruptedException,
//            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
//        String expectedResponseData = "<id>123</id><name>John</name></Customer>";
//        // We need to wait till the relevant listener reloads.
//        Thread.sleep(60000);
//        Map<String, String> requestHeaders = new HashMap<>();
//        requestHeaders.put("accept", "text/xml");
//        // Check with the correct client certificate for an API that is only protected with mutual ssl.
//        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
//                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
//                        + File.separator + "new-keystore.jks",
//                getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
//        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
//                "Mutual SSL Authentication has not succeeded");
//        Assert.assertTrue(response.getData().contains(expectedResponseData), "Expected payload did not match");
//        /* Check with the wrong client certificate for an API that is protected with mutual ssl and oauth2, without
//         an access token.*/
//        response = HTTPSClientUtils.doMutulSSLGet(
//                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
//                        + File.separator + "new-keystore.jks",
//                getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
//        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
//                "Mutual SSL Authentication has succeeded for a different certificate");
//        /* Check with the correct client certificate for an API that is protected with mutual ssl and oauth2, without
//         an access token.*/
//        response = HTTPSClientUtils.doMutulSSLGet(
//                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
//                        + File.separator + "test.jks",
//                getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
//        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
//                "Mutual SSL Authentication has not succeeded");
//        Assert.assertTrue(response.getData().contains(expectedResponseData), "Expected payload did not match");
//
//         /* Check with the wrong client certificate for an API that is protected with mutual ssl and oauth2, with a
//         correct access token.*/
//        requestHeaders.put("Authorization", "Bearer " + accessToken);
//        response = HTTPSClientUtils.doMutulSSLGet(
//                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
//                        + File.separator + "new-keystore.jks",
//                getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
//        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
//                "OAuth2 authentication was not checked in the event of mutual SSL failure");
//        Assert.assertTrue(response.getData().contains(expectedResponseData), "Expected payload did not match");
//    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws IOException, AutomationUtilException, ApiException {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
    }

}
