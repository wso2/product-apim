/*
 *Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.jwt.jwtdecoding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test for the API invocation failing when enabling the backend JWT and with the username pattern [string].[string]
 * It performs the following actions:
 *  1. Calls an API using an API key and checks the response status code.
 *  2. Repeats the API call and check status code as this was failing before the fix.
 *  3. Calls the same API using a Bearer token and checks the response status code.
 *  4. Repeats the Bearer token API call.
 */
public class JWTDecodingTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(JWTDecodingTestCase.class);
    private final String decodingApiContext = "jwtdecodingTest";
    private final String apiVersion = "1.0.0";
    String enduserName = "admin.abc";
    String enduserPassword = "password@123";
    URL tokenEndpointURL;
    private String decodingApplicationId;
    private String decodingApiId;

    @Factory(dataProvider = "userModeDataProvider") public JWTDecodingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true) public void setEnvironment() throws Exception {
        super.init(userMode);
        tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String providerName = user.getUserName();
        String endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
        Map<String, String> attributes = new HashMap<>();
        attributes.put("Sandbox access required", "Yes");
        attributes.put("Production access required", "Yes");

        //create JWT Base App with custom attributes
        String decodingApplicationName = "JWTAppFOrJWTDecodingTest";
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse decodingApplicationDTO =
                restAPIStore.createApplicationWithCustomAttribute(
                decodingApplicationName, "JWT decoding Application",
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.JWT, attributes);
        decodingApplicationId = decodingApplicationDTO.getData();

        String decodingApiName = "JWTDecodingAPI";
        APIRequest decodingApiRequest = new APIRequest(decodingApiName, decodingApiContext, new URL(endpointURL));
        decodingApiRequest.setVersion(apiVersion);
        decodingApiRequest.setVisibility("public");
        decodingApiRequest.setProvider(providerName);
        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("api_key");
        securitySchemes.add("oauth2");
        securitySchemes.add("oauth_basic_auth_api_key_mandatory");
        decodingApiRequest.setSecurityScheme(securitySchemes);
        decodingApiId = createAndPublishAPIUsingRest(decodingApiRequest, restAPIPublisher, false);
        restAPIStore.subscribeToAPI(decodingApiId, decodingApplicationId, TIER_GOLD);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        //generate keys
        restAPIStore.generateKeys(decodingApplicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        createUser();
        waitForAPIDeploymentSync(user.getUserName(), decodingApiRequest.getName(), decodingApiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = { "wso2.am" }, description = "Generate keys and invoking custom application")
    public void testJWTDecodingforCustomApplication() throws Exception {

        //Call the API with apikey
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(decodingApplicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), 36000, null, null);
        HttpClient decodingKeyHttpClient = HttpClientBuilder.create().build();
        HttpGet decodingFirstGet = new HttpGet(getAPIInvocationURLHttp(decodingApiContext, apiVersion));
        decodingFirstGet.addHeader("apikey", apiKeyDTO.getApikey());
        HttpResponse decodingFirstResponse = decodingKeyHttpClient.execute(decodingFirstGet);
        Assert.assertEquals(decodingFirstResponse.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        Thread.sleep(1000);
        HttpResponse decodingSecondResponse = decodingKeyHttpClient.execute(decodingFirstGet);
        Assert.assertEquals(decodingSecondResponse.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        //Call the API with Bearer token
        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType = restAPIStore.getApplicationKeysByKeyType(
                decodingApplicationId, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        String accessToken = generateUserToken(applicationKeyDTO.getConsumerKey(),
                applicationKeyDTO.getConsumerSecret(), enduserName, enduserPassword);
        log.info("Acess Token Generated in JWT ==" + accessToken);
        HttpClient decodingTokenHttpClient = HttpClientBuilder.create().build();
        HttpGet decodingThirdGet = new HttpGet(getAPIInvocationURLHttp(decodingApiContext, apiVersion));
        decodingThirdGet.addHeader("Authorization", "Bearer " + accessToken);
        HttpResponse decodingThirdResponse = decodingTokenHttpClient.execute(decodingThirdGet);
        Assert.assertEquals(decodingThirdResponse.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        Thread.sleep(1000);
        HttpResponse decodingFourthResponse = decodingTokenHttpClient.execute(decodingThirdGet);
        Assert.assertEquals(decodingFourthResponse.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
    }

    @AfterClass(alwaysRun = true) public void destroy() throws Exception {
        userManagementClient.deleteUser(enduserName);
        restAPIStore.deleteApplication(decodingApplicationId);
        undeployAndDeleteAPIRevisionsUsingRest(decodingApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(decodingApiId);
    }

    private void createUser()
            throws RemoteException, RemoteUserStoreManagerServiceUserStoreExceptionException, UserStoreException {

        String DEFAULT_PROFILE = "default";
        remoteUserStoreManagerServiceClient.addUser(enduserName, enduserPassword, new String[] {}, new ClaimValue[] {},
                DEFAULT_PROFILE, false);
        remoteUserStoreManagerServiceClient.setUserClaimValue(enduserName, "http://wso2.org/claims/givenname",
                "first name", DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(enduserName, "http://wso2.org/claims/lastname",
                "last name", DEFAULT_PROFILE);

    }

    private String generateUserToken(String consumerKey, String consumerSecret, String enduserName,
            String enduserPassword) throws APIManagerIntegrationTestException, JSONException {

        String username = enduserName;
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            username = username.concat("@").concat(user.getUserDomain());
        }
        String requestBody = "grant_type=password&username=" + username + "&password=" + enduserPassword;

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse = restAPIStore.generateUserAccessKey(
                consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        return accessTokenGenerationResponse.getString("access_token");

    }
}
