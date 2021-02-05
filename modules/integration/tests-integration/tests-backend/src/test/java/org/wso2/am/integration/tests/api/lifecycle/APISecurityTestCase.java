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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This class tests the behaviour of API when there is choice of selection between oauth2 and mutual ssl in API Manager.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APISecurityTestCase extends APIManagerLifecycleBaseTest {

    private final String mutualSSLOnlyAPIName = "mutualsslOnlyAPI";
    private final String mutualSSLWithOAuthAPI = "mutualSSLWithOAuthAPI";
    private final String mutualSSLandOauthMandatoryAPI = "mutualSSLandOAuthMandatoryAPI";
    private final String mutualSSLOnlyAPIContext = "mutualsslOnlyAPI";
    private final String mutualSSLWithOAuthAPIContext = "mutualSSLWithOAuthAPI";
    private final String mutualSSLandOAuthMandatoryAPIContext = "mutualSSLandOAuthMandatoryAPI";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private String accessToken;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String applicationId;
    private String apiId1, apiId2;
    private String apiId3;

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, IOException, ApiException,
            org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException, AutomationUtilException,
            InterruptedException, JSONException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest1 = new APIRequest(mutualSSLOnlyAPIName, mutualSSLOnlyAPIContext, new URL(apiEndPointUrl));
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
        securitySchemes.add("mutualssl_mandatory");
        apiRequest1.setSecurityScheme(securitySchemes);
        apiRequest1.setDefault_version("true");
        apiRequest1.setHttps_checked("https");
        apiRequest1.setHttp_checked(null);
        apiRequest1.setDefault_version_checked("true");
        HttpResponse response1 = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response1.getData();

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certOne), "example", apiId1, APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIRequest apiRequest2 = new APIRequest(mutualSSLWithOAuthAPI, mutualSSLWithOAuthAPIContext, new URL(apiEndPointUrl));
        apiRequest2.setVersion(API_VERSION_1_0_0);
        apiRequest2.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTags(API_TAGS);
        apiRequest2.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest2.setOperationsDTOS(operationsDTOS);
        apiRequest2.setDefault_version("true");
        apiRequest2.setHttps_checked("https");
        apiRequest2.setHttp_checked(null);
        apiRequest2.setDefault_version_checked("true");
        List<String> securitySchemes2 = new ArrayList<>();
        securitySchemes2.add("mutualssl");
        securitySchemes2.add("oauth2");
        securitySchemes2.add("api_key");
        securitySchemes2.add("oauth_basic_auth_api_key_mandatory");
        apiRequest2.setSecurityScheme(securitySchemes2);

        HttpResponse response2 = restAPIPublisher.addAPI(apiRequest2);
        apiId2 = response2.getData();

        String certTwo = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certTwo), "abcde", apiId2, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);
        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);

        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction());
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());

        APIRequest apiRequest3 = new APIRequest(mutualSSLandOauthMandatoryAPI, mutualSSLandOAuthMandatoryAPIContext,
                new URL(apiEndPointUrl));
        apiRequest3.setVersion(API_VERSION_1_0_0);
        apiRequest3.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest3.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest3.setTags(API_TAGS);
        apiRequest3.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest3.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes3 = new ArrayList<>();
        securitySchemes3.add("mutualssl");
        securitySchemes3.add("oauth2");
        securitySchemes3.add("api_key");
        securitySchemes3.add("mutualssl_mandatory");
        securitySchemes3.add("oauth_basic_auth_api_key_mandatory");
        apiRequest3.setSecurityScheme(securitySchemes3);
        apiRequest3.setDefault_version("true");
        apiRequest3.setHttps_checked("https");
        apiRequest3.setHttp_checked(null);
        apiRequest3.setDefault_version_checked("true");
        HttpResponse response3 = restAPIPublisher.addAPI(apiRequest3);
        apiId3 = response3.getData();
        String certThree = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher
                .uploadCertificate(new File(certThree), "abcdef", apiId3, APIMIntegrationConstants.API_TIER.UNLIMITED);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId3, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId3, APILifeCycleAction.PUBLISH.getAction());

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiId3, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        restAPIStore.subscribeToAPI(apiId2, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);




        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        Thread.sleep(120000);
    }

    @Test(description = "This test case tests the behaviour of APIs that are protected with mutual SSL and OAuth2 "
            + "when the client certificate is not presented but OAuth2 token is presented.")
    public void testCreateAndPublishAPIWithOAuth2() throws XPathExpressionException, IOException, JSONException {
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLOnlyAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        JSONObject response = new JSONObject(apiResponse.getData());
        //fix test failure due to error code changes introduced in product-apim pull #7106
        assertEquals(response.getJSONObject("fault").getInt("code"), 900901,
                "API invocation succeeded with the access token without need for mutual ssl");
        apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Testing the invocation with API Keys", dependsOnMethods = {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeys() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore
                    .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                            -1, null, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK);

    }

    @Test(description = "Invoke mutual SSL only API with not supported certificate", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLOnlyAPINegative()
            throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIName) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
    }

    @Test(description = "This method test to validate how application security mandatory and mutual ssl optional api " +
            "behaviour in success scenario",
            dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatory() throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "Test with no application security header with valid cert", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatoryNegative1() throws IOException,
            XPathExpressionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual authentication success for oauth mandatory scenario");
    }

    @Test(description = "This method test to validate how application security mandatory and mutual ssl optional api " +
            "behaviour in success scenario",
            dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatoryNegative2() throws IOException,
            XPathExpressionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual authentication success for oauth mandatory scenario");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual authentication success for oauth mandatory scenario");
    }


    @Test(description = "This method test to validate how application security mandatory and mutual ssl optional api " +
            "behaviour in success scenario",
            dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLWithOauthMandatoryNegative3() throws IOException,
            XPathExpressionException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + UUID.randomUUID().toString());
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual authentication success for oauth mandatory scenario");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual authentication success for oauth mandatory scenario");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLMandatory() throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLMandatoryNeagative1() throws IOException, XPathExpressionException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLMandatoryNegative2() throws IOException, XPathExpressionException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) +
                                API_END_POINT_METHOD,
                        requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                        requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "API invocation with mutual ssl and oauth mandatory", dependsOnMethods =
            "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSLHeader() throws IOException, XPathExpressionException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("X-WSO2-CLIENT-CERTIFICATE", generateBase64EncodedCertificate());
        HttpResponse response = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0) +
                                API_END_POINT_METHOD,
                        requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(mutualSSLandOAuthMandatoryAPIContext) + API_END_POINT_METHOD,
                        requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
    }

    @Test(description = "Testing the invocation with API Keys having IP restriction",
            dependsOnMethods = {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeysWithIPCondition() throws Exception {
        String permittedIP = "152.23.5.6, 192.168.1.2/24, 2001:c00::/23";
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, permittedIP, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");

        // a permitted ipv4 address
        Map<String, String> requestHeaders1 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(), "152.23.5.6", null);
        HttpResponse response1 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders1);
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_OK);

        // a permitted ipv4 address
        Map<String, String> requestHeaders2 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(), "192.168.1.6", null);
        HttpResponse response2 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders2);
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_OK);

        // a forbidden ipv4 address
        Map<String, String> requestHeaders3 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(), "192.168.5.6", null);
        HttpResponse response3 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders3);
        Assert.assertEquals(response3.getResponseCode(), HttpStatus.SC_FORBIDDEN);

        // a permitted ipv6 address
        Map<String, String> requestHeaders4 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                "2001:c00:0:0:0:0:c:4", null);
        HttpResponse response4 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders4);
        Assert.assertEquals(response4.getResponseCode(), HttpStatus.SC_OK);

        // a forbidden ipv6 address
        Map<String, String> requestHeaders5 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                "2061:c00:0:0:0:0:0:0", null);
        HttpResponse response5 = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders5);
        Assert.assertEquals(response5.getResponseCode(), HttpStatus.SC_FORBIDDEN);
    }

    @Test(description = "Testing the invocation with API Keys having Http Referer restriction",
            dependsOnMethods = {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeysWithRefererCondition() throws Exception {
        String permittedReferer = "www.abc.com/path, sub.cds.com/*, *.gef.com/*";
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, permittedReferer);

        assertNotNull(apiKeyDTO, "API Key generation failed");

        // matches against a permitted referer which matches an exact referer path
        Map<String, String> requestHeaders1 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                null, "www.abc.com/path");
        HttpResponse response1 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders1);
        Assert.assertEquals(response1.getResponseCode(), HttpStatus.SC_OK);

        // does not match against any permitted referer
        Map<String, String> requestHeaders2 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                null, "www.abc.com/path2");
        HttpResponse response2 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders2);
        Assert.assertEquals(response2.getResponseCode(), HttpStatus.SC_FORBIDDEN);

        // matches against permitted referer which matches urls of a specific sub domain using a wild card
        Map<String, String> requestHeaders3 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                null, "sub.cds.com/path1/path2");
        HttpResponse response3 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders3);
        Assert.assertEquals(response3.getResponseCode(), HttpStatus.SC_OK);


        // matches against permitted referer which matches urls of a specific sub domain of any domain
        // using wild cards
        Map<String, String> requestHeaders4 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                null, "example.gef.com/path1");
        HttpResponse response4 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders4);
        Assert.assertEquals(response4.getResponseCode(), HttpStatus.SC_OK);
    }

    private Map<String, String> createRequestHeadersForAPIKey(String apiKey, String ip, String referer) {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("apikey", apiKey);
        if (ip != null) {
            requestHeaders.put("X-Forwarded-For", ip);
        }
        if (referer != null) {
            requestHeaders.put("Referer", referer);
        }
        return requestHeaders;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws IOException, AutomationUtilException, ApiException, JSONException {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId1, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId2, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId3, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
    }

    public String generateBase64EncodedCertificate() throws IOException {

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        String base64EncodedString = IOUtils.toString(new FileInputStream(certOne));
        base64EncodedString = Base64.encodeBase64URLSafeString(base64EncodedString.getBytes());
        return base64EncodedString;
    }
}
