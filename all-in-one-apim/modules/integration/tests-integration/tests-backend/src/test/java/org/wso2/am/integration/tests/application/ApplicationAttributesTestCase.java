/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.application;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationAttributesTestCase extends APIManagerLifecycleBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private String endpointURL;
    private String apiId;
    private String API_NAME = "ApplicationAttributeAPI";
    URL tokenEndpointURL;
    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";
    String applicationId1;
    String applicationId2;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationAttributesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "applicationAttributes"
                        + File.separator + "deployment.toml"));
        endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
        tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "token");

        APIRequest apiRequest = new APIRequest(API_NAME, API_NAME, new URL(endpointURL));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(user.getUserName());

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("oauth2");
        securitySchemes.add("api_key");
        apiRequest.setSecurityScheme(securitySchemes);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

    }

    @Test(description = "Add and verify application attributes in JWT application")
    public void testVerifyApplicationAttributesInJWTApplication() throws Exception {

        Map<String, String> attribute = new HashMap<>();
        attribute.put("External Reference Id", "c1237890");
        HttpResponse applicationResponse = restAPIStore.createApplicationWithCustomAttribute("JWTAppWithAppAttributes",
                "JWT Application with application attributes",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT, attribute);
        applicationId1 = applicationResponse.getData();
        Assert.assertEquals(applicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK);
        ApplicationDTO jwtApp = restAPIStore.getApplicationById(applicationId1);
        Assert.assertEquals(jwtApp.getAttributes().get("External Reference Id"), "c1237890");

        restAPIStore.subscribeToAPI(apiId, applicationId1, TIER_GOLD);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId1, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_NAME, API_VERSION_1_0_0));
        get.addHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode());

        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader);

        String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
        JSONObject jsonJWTObject = new JSONObject(decodedJWTString);
        Assert.assertTrue(jsonJWTObject.getString("http://wso2.org/claims/applicationAttributes").
                equals("{\"External Reference Id\":\"c1237890\"}"));

    }

    @Test(description = "Add and verify application attributes in Oauth application")
    public void testVerifyApplicationAttributesInOauthApplication() throws Exception {

        Map<String, String> attribute = new HashMap<>();
        attribute.put("External Reference Id", "c1237890");
        HttpResponse applicationResponse = restAPIStore.createApplicationWithCustomAttribute("OauthAppWithAppAttributes",
                "Oauth Application with application attributes",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH, attribute);
        applicationId2 = applicationResponse.getData();
        Assert.assertEquals(applicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK);
        ApplicationDTO jwtApp = restAPIStore.getApplicationById(applicationId2);
        Assert.assertEquals(jwtApp.getAttributes().get("External Reference Id"), "c1237890");

        restAPIStore.subscribeToAPI(apiId, applicationId2, TIER_GOLD);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId2, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_NAME, API_VERSION_1_0_0));
        get.addHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode());

        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader);

        String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
        JSONObject jsonJWTObject = new JSONObject(decodedJWTString);
        Assert.assertTrue(jsonJWTObject.getString("http://wso2.org/claims/applicationAttributes").
                equals("{\"External Reference Id\":\"c1237890\"}"));

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId1);
        restAPIStore.deleteApplication(applicationId2);
        restAPIPublisher.deleteAPI(apiId);
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
