/*
 *Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.token;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.base.MultitenantConstants;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This test will cover OpenId based access token generation and validation for users
 * Here we will retrieve access tokens with open id scope and use it for user info API
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class OpenIDTokenAPITestCase extends APIMIntegrationBaseTest {
    private APIStoreRestClient apiStore;
    private String consumerKey;
    private String consumerSecret;
    private String userAccessToken;
    private String applicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public OpenIDTokenAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        HttpResponse applicationResponse = restAPIStore.createApplication("OpenIDTokenTestAPIApplication", " Description",
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationId = applicationResponse.getData();

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        grantTypes.add("password");

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample")
    public void testGenerateAccessTokenWithOpenIdScope() throws Exception {
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password="
                + user.getPassword() + "&scope=openid";
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        JSONObject accessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(consumerKey,
            consumerSecret, requestBody, tokenEndpointURL).getData());

        userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String scope = accessTokenGenerationResponse.getString("scope");
        Assert.assertTrue(scope.contains("openid"), "Response data mismatched, openid scope test failed due to " +
                "error in response");
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample",
            dependsOnMethods = "testGenerateAccessTokenWithOpenIdScope")
    public void testCallUserInfoApiWithOpenIdAccessToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        String tokenJti = TokenUtils.getJtiOfJwtToken(userAccessToken);
        requestHeaders.put("Authorization", "Bearer " + tokenJti);

        HttpResponse userInfoResponse = HTTPSClientUtils.doGet(keyManagerHTTPSURL
                + "oauth2/userinfo?schema=openid", requestHeaders);
        Assert.assertEquals(userInfoResponse.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample",
            dependsOnMethods = "testCallUserInfoApiWithOpenIdAccessToken")
    public void testCallUserInfoApiWithOpenIdJWTAccessToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);

        String keyManagerURLSuffix = "oauth2/userinfo?schema=openid";
        String tenantDomain = user.getUserDomain();
        if (!StringUtils.equals(tenantDomain, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            keyManagerURLSuffix = "t/" + tenantDomain + "/" + keyManagerURLSuffix;
        }
        HttpResponse userInfoResponse = HTTPSClientUtils.doGet(keyManagerHTTPSURL + keyManagerURLSuffix,
                requestHeaders);
        Assert.assertEquals(userInfoResponse.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }
}
