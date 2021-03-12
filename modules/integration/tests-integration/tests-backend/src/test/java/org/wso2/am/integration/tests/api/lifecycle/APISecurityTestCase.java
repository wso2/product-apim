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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.internal.api.dto.RevokedJWTDTO;
import org.wso2.am.integration.clients.internal.api.dto.RevokedJWTListDTO;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.RestAPIInternalImpl;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.text.ParseException;
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
    private final String apiKeySecuredAPI = "apiKeySecuredAPI";
    private final String mutualSSLOnlyAPIContext = "mutualsslOnlyAPI";
    private final String mutualSSLWithOAuthAPIContext = "mutualSSLWithOAuthAPI";
    private final String mutualSSLandOAuthMandatoryAPIContext = "mutualSSLandOAuthMandatoryAPI";
    private final String apiKeySecuredAPIContext = "apiKeySecuredAPI";
    private final String basicAuthSecuredAPI = "BasicAuthSecuredAPI";
    private final String basicAuthSecuredAPIContext = "BasicAuthSecuredAPI";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private String accessToken;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String applicationId;
    private String consumerKey;
    private String consumerSecret;
    private String apiId1, apiId2;
    private String apiId3, apiId4;
    private String apiId5;
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    String users[] = {"apisecUser", "apisecUser2@wso2.com", "apisecUser2@abc.com"};
    String endUserPassword = "password@123";

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    private void createUser() throws RemoteException,
            RemoteUserStoreManagerServiceUserStoreExceptionException, UserStoreException {

        for (String user : users) {
            remoteUserStoreManagerServiceClient.addUser(user, endUserPassword, new String[]{}, new ClaimValue[]{},
                    "default", false);
        }

    }


    @Factory(dataProvider = "userModeDataProvider")
    public APISecurityTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, IOException, ApiException,
            org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException, AutomationUtilException,
            InterruptedException, JSONException, RemoteUserStoreManagerServiceUserStoreExceptionException,
            UserStoreException {
        super.init(userMode);
        createUser();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest1 = new APIRequest(mutualSSLOnlyAPIName, mutualSSLOnlyAPIContext, new URL(apiEndPointUrl));
        apiRequest1.setVersion(API_VERSION_1_0_0);
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTags(API_TAGS);
        apiRequest1.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest1.setProvider(user.getUserName());
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
        restAPIPublisher.uploadCertificate(new File(certOne), "example", apiId1,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIRequest apiRequest2 = new APIRequest(mutualSSLWithOAuthAPI, mutualSSLWithOAuthAPIContext,
                new URL(apiEndPointUrl));
        apiRequest2.setVersion(API_VERSION_1_0_0);
        apiRequest2.setProvider(user.getUserName());
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
        restAPIPublisher.uploadCertificate(new File(certTwo), "abcde", apiId2,
                APIMIntegrationConstants.API_TIER.UNLIMITED);


        APIRequest apiRequest3 = new APIRequest(mutualSSLandOauthMandatoryAPI, mutualSSLandOAuthMandatoryAPIContext,
                new URL(apiEndPointUrl));
        apiRequest3.setVersion(API_VERSION_1_0_0);
        apiRequest3.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest3.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest3.setTags(API_TAGS);
        apiRequest3.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest3.setOperationsDTOS(operationsDTOS);
        apiRequest3.setProvider(user.getUserName());

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

        // Add an API Secured with APIKey only
        APIRequest apiRequest4 = new APIRequest(apiKeySecuredAPI, apiKeySecuredAPIContext, new URL(apiEndPointUrl));
        apiRequest4.setVersion(API_VERSION_1_0_0);
        apiRequest4.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest4.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest4.setTags(API_TAGS);
        apiRequest4.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest4.setOperationsDTOS(operationsDTOS);
        apiRequest4.setDefault_version("true");
        apiRequest4.setHttps_checked("https");
        apiRequest4.setHttp_checked(null);
        apiRequest4.setProvider(user.getUserName());
        apiRequest4.setDefault_version_checked("true");
        List<String> securitySchemes4 = new ArrayList<>();
        securitySchemes4.add("api_key");
        securitySchemes4.add("oauth_basic_auth_api_key_mandatory");
        apiRequest4.setSecurityScheme(securitySchemes4);
        apiRequest4.setSandbox(apiEndPointUrl);

        HttpResponse response4 = restAPIPublisher.addAPI(apiRequest4);
        apiId4 = response4.getData();

        APIRequest apiRequest5 = new APIRequest(basicAuthSecuredAPI, basicAuthSecuredAPIContext,
                new URL(apiEndPointUrl));
        apiRequest5.setVersion(API_VERSION_1_0_0);
        apiRequest5.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest5.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest5.setTags(API_TAGS);
        apiRequest5.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest5.setOperationsDTOS(operationsDTOS);
        apiRequest5.setProvider(user.getUserName());

        List<String> securitySchemes5 = new ArrayList<>();
        securitySchemes5.add("basic_auth");
        securitySchemes5.add("oauth_basic_auth_api_key_mandatory");
        apiRequest5.setSecurityScheme(securitySchemes5);
        apiRequest5.setDefault_version("true");
        apiRequest5.setHttps_checked("https");
        apiRequest5.setHttp_checked(null);
        HttpResponse response5 = restAPIPublisher.addAPI(apiRequest5);
        apiId5 = response5.getData();
        createAPIRevisionAndDeployUsingRest(apiId5, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId5, false);
        waitForAPIDeploymentSync(apiRequest5.getProvider(), apiRequest5.getName(), apiRequest5.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(description = "This test case tests the behaviour of internal Key token on Created API with authentication " +
            "types")
    public void testCreateAndDeployRevisionWithInternalKeyTesting() throws JSONException, ApiException,
            XPathExpressionException, APIManagerIntegrationTestException, IOException,
            org.wso2.am.integration.clients.store.api.ApiException, InterruptedException {
        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);
        APIDTO api1 = restAPIPublisher.getAPIByID(apiId1);
        waitForAPIDeploymentSync(api1.getProvider(), api1.getName(), api1.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse1 =
                restAPIPublisher.generateInternalApiKey(apiId1);
        Assert.assertEquals(keyDTOApiResponse1.getStatusCode(), 200);
        HttpResponse httpResponse1 = invokeApiWithInternalKey(mutualSSLOnlyAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse1.getData().getApikey());
        Assert.assertEquals(httpResponse1.getResponseCode(), 200);
        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction());
        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);
        APIDTO api2 = restAPIPublisher.getAPIByID(apiId2);
        waitForAPIDeploymentSync(api2.getProvider(), api2.getName(), api2.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse2 =
                restAPIPublisher.generateInternalApiKey(apiId2);
        HttpResponse httpResponse2 = invokeApiWithInternalKey(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse2.getData().getApikey());
        Assert.assertEquals(httpResponse2.getResponseCode(), 200);
        HttpResponse httpResponse3 = invokeApiWithInternalKey(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse1.getData().getApikey());
        Assert.assertEquals(httpResponse3.getResponseCode(), 403);
        // verify internal key authentication after publish
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());
        httpResponse2 = invokeApiWithInternalKey(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse2.getData().getApikey());
        Assert.assertEquals(httpResponse2.getResponseCode(), 200);

        createAPIRevisionAndDeployUsingRest(apiId3, restAPIPublisher);
        APIDTO api3 = restAPIPublisher.getAPIByID(apiId3);
        waitForAPIDeploymentSync(api3.getProvider(), api3.getName(), api3.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse3 =
                restAPIPublisher.generateInternalApiKey(apiId3);
        HttpResponse httpResponse4 = invokeApiWithInternalKey(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse3.getData().getApikey());
        Assert.assertEquals(httpResponse4.getResponseCode(), 200);
        restAPIPublisher.changeAPILifeCycleStatus(apiId3, APILifeCycleAction.PUBLISH.getAction());

        createAPIRevisionAndDeployUsingRest(apiId4, restAPIPublisher);
        APIDTO api4 = restAPIPublisher.getAPIByID(apiId4);
        waitForAPIDeploymentSync(api4.getProvider(), api4.getName(), api4.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse4 =
                restAPIPublisher.generateInternalApiKey(apiId3);
        HttpResponse httpResponse5 = invokeApiWithInternalKey(mutualSSLandOAuthMandatoryAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse4.getData().getApikey());
        Assert.assertEquals(httpResponse5.getResponseCode(), 200);
        restAPIPublisher.changeAPILifeCycleStatus(apiId4, APILifeCycleAction.PUBLISH.getAction());
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiId3, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        restAPIStore.subscribeToAPI(apiId2, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        restAPIStore.subscribeToAPI(apiId4, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        HttpResponse httpResponseAfterPublish = invokeApiWithInternalKey(mutualSSLOnlyAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse1.getData().getApikey());
        Assert.assertEquals(httpResponseAfterPublish.getResponseCode(), 200);

        // wait until certificates loaded
        Thread.sleep(120000);
    }

    private HttpResponse invokeApiWithInternalKey(String context, String version, String resource,
                                                  String internalKey) throws XPathExpressionException, IOException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Internal-Key", internalKey);
        return HttpRequestUtil.doGet(getAPIInvocationURLHttps(context, version) + resource, requestHeaders);
    }

    @Test(description = "This test case tests the behaviour of APIs that are protected with mutual SSL and OAuth2 "
            + "when the client certificate is not presented but OAuth2 token is presented.", dependsOnMethods =
            {"testCreateAndDeployRevisionWithInternalKeyTesting"})
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
        assertEquals(response.getString("code"), "900901",
                "API invocation succeeded with the access token without need for mutual ssl");
        apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Testing the invocation with Basic Auth for Oauth2 Only API", dependsOnMethods = {
            "testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithBasicAuthForOauthOnlyAPINegative() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Basic abcce");
        HttpResponse response = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLandOauthMandatoryAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
    }

    @Test(description = "Testing the invocation with API Keys", dependsOnMethods = {
            "testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeys() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("apikey", apiKeyDTO.getApikey());
        HttpResponse response = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(apiKeySecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
    }

    @Test(description = "Testing the invocation with Basic Auth for APIKey Only API", dependsOnMethods = {
            "testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithBasicAuthFoAPIKeyNegative() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Basic abcce");
        HttpResponse response = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(apiKeySecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
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
        assertEquals(response3.getResponseCode(), HttpStatus.SC_OK);


        // matches against permitted referer which matches urls of a specific sub domain of any domain
        // using wild cards
        Map<String, String> requestHeaders4 = createRequestHeadersForAPIKey(apiKeyDTO.getApikey(),
                null, "example.gef.com/path1");
        HttpResponse response4 = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPI,
                API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders4);
        Assert.assertEquals(response4.getResponseCode(), HttpStatus.SC_OK);
    }

    @Test(description = "Testing the invocation of API Secured only with API Keys", dependsOnMethods = {
            "testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithApiKeysOnly() throws Exception {
        APIKeyDTO apiKeyDTO1 = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), -1,
                        null, null);

        assertNotNull(apiKeyDTO1, "API Key generation failed");
        // matches against a permitted referer which matches an exact referer path
        Map<String, String> requestHeaders1 = createRequestHeadersForAPIKey(apiKeyDTO1.getApikey(), null, null);
        HttpResponse response1 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(apiKeySecuredAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders1);
        Assert.assertEquals(response1.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api with production endpoint");

        APIKeyDTO apiKeyDTO2 = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX.toString(), -1,
                        null, null);

        assertNotNull(apiKeyDTO2, "API Key generation failed");
        // matches against a permitted referer which matches an exact referer path
        Map<String, String> requestHeaders2 = createRequestHeadersForAPIKey(apiKeyDTO1.getApikey(), null, null);
        HttpResponse response2 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(apiKeySecuredAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders2);
        Assert.assertEquals(response2.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api with sandbox endpoint");
        Assert.assertTrue(response2.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke with sandbox endpoint" + " Response Data:" + response2.getData()
                        + ". Expected Response Data: " + API_RESPONSE_DATA);

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

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvocationWithRevokedApiKeys() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum
                .PRODUCTION.toString(), -1, null, null);
        assertNotNull(apiKeyDTO, "API Key generation failed");

        restAPIStore.revokeAPIKey(applicationId, apiKeyDTO.getApikey());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("apikey", apiKeyDTO.getApikey());
        requestHeader.put("accept", "text/xml");

        boolean isApiKeyValid = true;
        HttpResponse invocationResponseAfterRevoked;
        int counter = 1;
        do {
            // Wait while the JMS message is received to the related JMS topic
            Thread.sleep(1000L);
            invocationResponseAfterRevoked = HTTPSClientUtils.doGet(
                    getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                    requestHeader);
            int responseCodeAfterRevoked = invocationResponseAfterRevoked.getResponseCode();

            if (responseCodeAfterRevoked == HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                isApiKeyValid = false;
            } else if (responseCodeAfterRevoked == HTTP_RESPONSE_CODE_OK) {
                isApiKeyValid = true;
            } else {
                throw new APIManagerIntegrationTestException("Unexpected response received when invoking the API. " +
                        "Response received :" + invocationResponseAfterRevoked.getData() + ":" +
                        invocationResponseAfterRevoked.getResponseMessage());
            }
            counter++;
        } while (isApiKeyValid && counter < 25);
        Assert.assertFalse(isApiKeyValid, "API Key revocation failed. " +
                "API invocation response code is expected to be : " + HTTP_RESPONSE_CODE_UNAUTHORIZED +
                ", but got " + invocationResponseAfterRevoked.getResponseCode());
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeApiKeyAsJWTNegative() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum
                .PRODUCTION.toString(), -1, null, null);
        assertNotNull(apiKeyDTO, "API Key generation failed");

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("apikey", accessToken);
        requestHeader.put("accept", "text/xml");

        HttpResponse response = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeader);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeJWTAsAPIKeyNegative() throws Exception {

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String subsAccessTokenPayload = APIMTestCaseUtils.getPayloadForPasswordGrant(user.getUserName(),
                user.getPassword());
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(consumerKey,
                consumerSecret, subsAccessTokenPayload, tokenEndpointURL).getData());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("apikey", subsAccessTokenGenerationResponse.getString("access_token"));
        requestHeader.put("accept", "text/xml");

        HttpResponse response = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeader);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeInternalKeyAsAPIKeyNegative() throws Exception {
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse1 =
                restAPIPublisher.generateInternalApiKey(apiId2);
        Assert.assertEquals(keyDTOApiResponse1.getStatusCode(), 200);
        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("apikey", keyDTOApiResponse1.getData().getApikey());
        requestHeader.put("accept", "text/xml");

        HttpResponse response = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeader);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeInternalKeyAsJWTNegative() throws Exception {
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse1 =
                restAPIPublisher.generateInternalApiKey(apiId2);
        Assert.assertEquals(keyDTOApiResponse1.getStatusCode(), 200);
        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("Authorization", "Bearer " + keyDTOApiResponse1.getData().getApikey());
        requestHeader.put("accept", "text/xml");

        HttpResponse response = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(mutualSSLWithOAuthAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeader);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeJWTasInternalKeyNegative() throws Exception {
        HttpResponse response = invokeApiWithInternalKey(mutualSSLWithOAuthAPI, API_VERSION_1_0_0,
                API_END_POINT_METHOD, accessToken);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeAPIKeyAsInternalKeyNegative() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum
                .PRODUCTION.toString(), -1, null, null);
        HttpResponse response = invokeApiWithInternalKey(mutualSSLWithOAuthAPI, API_VERSION_1_0_0,
                API_END_POINT_METHOD, apiKeyDTO.getApikey());
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Revoked API Keys", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeInternalKeyForBasicAuthOnlyAPI() throws Exception {
        ApiResponse<org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO> keyDTOApiResponse1 =
                restAPIPublisher.generateInternalApiKey(apiId5);
        Assert.assertEquals(keyDTOApiResponse1.getStatusCode(), 200);
        HttpResponse response = invokeApiWithInternalKey(basicAuthSecuredAPIContext, API_VERSION_1_0_0,
                API_END_POINT_METHOD, keyDTOApiResponse1.getData().getApikey());
        Assert.assertEquals(response.getResponseCode(), 200);
    }

    @Test(description = "Testing the invocation with BasicAuth", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeBasicAuth() throws Exception {
        String user1 = users[0];
        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("Authorization",
                "Basic " + Base64.encodeBase64String(user1.concat("@").concat(this.user.getUserDomain()).concat(":")
                        .concat("randomPassword1").getBytes()));
        HttpResponse response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders1);
        Assert.assertEquals(response.getResponseCode(), 401);
        for (String user : users) {
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Authorization",
                    "Basic " + Base64.encodeBase64String(user.concat("@").concat(this.user.getUserDomain()).concat(
                            ":").concat(endUserPassword).getBytes()));
            response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                    API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
            Assert.assertEquals(response.getResponseCode(), 200);
        }
        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("Authorization",
                "Basic " + Base64.encodeBase64String(user1.concat("@").concat(this.user.getUserDomain()).concat(":")
                        .concat("randomPassword1").getBytes()));
        response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders2);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with BasicAuth Invalid user ", dependsOnMethods =
            {"testInvokeBasicAuth"})
    public void testInvokeBasicAuthInvalidCredentials2() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization",
                "Basic " + Base64.encodeBase64String("random@".concat(user.getUserDomain()).concat(":").concat(
                        "randomPassword").getBytes()));
        HttpResponse response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with Oauth Token for BasicAuth api", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeBearerTokenForBasicNegative() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the invocation with APIkey Token for BasicAuth api", dependsOnMethods =
            {"testCreateAndPublishAPIWithOAuth2"})
    public void testInvokeAPIKeyForBasicOauthAPINegative() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum
                .PRODUCTION.toString(), -1, null, null);
        assertNotNull(apiKeyDTO, "API Key generation failed");

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("apikey", apiKeyDTO.getApikey());
        requestHeader.put("accept", "text/xml");

        HttpResponse response = HTTPSClientUtils.doGet(
                getAPIInvocationURLHttps(basicAuthSecuredAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeader);
        Assert.assertEquals(response.getResponseCode(), 401);
    }

    @Test(description = "Testing the User Token Invocation and Password Reset", dependsOnMethods = {
            "testInvokeBasicAuth"})
    public void testInvokeJWTUserToken() throws XPathExpressionException, IOException, JSONException,
            APIManagerIntegrationTestException, RemoteUserStoreManagerServiceUserStoreExceptionException,
            ParseException, InterruptedException, org.wso2.am.integration.clients.internal.ApiException {
        // Create requestHeaders
        String user1 = users[0];
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String subsAccessTokenPayload =
                APIMTestCaseUtils.getPayloadForPasswordGrant(user1.concat("@").concat(user.getUserDomain()),
                        endUserPassword);
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(consumerKey,
                consumerSecret, subsAccessTokenPayload, tokenEndpointURL).getData());
        String accessToken1 = subsAccessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken1);
        HttpResponse apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
        // Change the User Credentials
        remoteUserStoreManagerServiceClient.updateUser(user1, "changeme");
        verifyRevokedTokenAvailable(TokenUtils.getJtiOfJwtToken(accessToken1));
        Thread.sleep(10000);
        apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(mutualSSLWithOAuthAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "API Invocation pass for Revoked Token");
    }

    @Test(description = "Testing the invocation with BasicAuth", dependsOnMethods =
            {"testInvokeJWTUserToken"})
    public void testInvokeBasicAuthAfterCredentialsInvalid() throws Exception {
        String user1 = users[0];
        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("Authorization",
                "Basic " + Base64.encodeBase64String(user1.concat("@").concat(this.user.getUserDomain()).concat(":")
                        .concat(endUserPassword).getBytes()));
        HttpResponse response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders1);
        Assert.assertEquals(response.getResponseCode(), 401);
        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("Authorization",
                "Basic " + Base64.encodeBase64String(user1.concat("@").concat(this.user.getUserDomain()).concat(":")
                        .concat("changeme").getBytes()));
        response = HttpRequestUtil.doGet(getAPIInvocationURLHttps(basicAuthSecuredAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders2);
        Assert.assertEquals(response.getResponseCode(), 200);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
        restAPIPublisher.deleteAPI(apiId4);
        restAPIPublisher.deleteAPI(apiId5);
        removeUsers();
    }

    public String generateBase64EncodedCertificate() throws IOException {

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        String base64EncodedString = IOUtils.toString(new FileInputStream(certOne));
        base64EncodedString = Base64.encodeBase64URLSafeString(base64EncodedString.getBytes());
        return base64EncodedString;
    }

    private void removeUsers() throws RemoteException, RemoteUserStoreManagerServiceUserStoreExceptionException {
        for (String user : users) {
            remoteUserStoreManagerServiceClient.removeUser(user);
        }
    }

    private void verifyRevokedTokenAvailable(String alias)
            throws org.wso2.am.integration.clients.internal.ApiException, InterruptedException {
        int retryCount = 0;
        RevokedJWTDTO selectedRevokedJWTDTO = null;
        do {
            RevokedJWTListDTO revokedJWTListDTOS = restAPIInternal.retrieveRevokedList();
            for (RevokedJWTDTO revokedJWTDTO : revokedJWTListDTOS) {
                if (alias.equals(revokedJWTDTO.getJwtSignature())) {
                    selectedRevokedJWTDTO = revokedJWTDTO;
                    break;
                }
            }
            if (selectedRevokedJWTDTO != null) {
                break;
            }
            retryCount++;
            Thread.sleep(5000);
        } while (retryCount < 20);
        Assert.assertNotNull(selectedRevokedJWTDTO, "Revoked Token didn't store in database");
    }

}
