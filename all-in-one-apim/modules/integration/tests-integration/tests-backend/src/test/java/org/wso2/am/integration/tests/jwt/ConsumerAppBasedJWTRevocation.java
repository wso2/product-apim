/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.jwt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

public class ConsumerAppBasedJWTRevocation extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ConsumerAppBasedJWTRevocation.class);
    private String consumerKey;
    private String consumerSecret;
    private String appId;
    private String accessToken;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();

        // Create an application in the DevPortal
        HttpResponse applicationResponse = restAPIStore.createApplication("JWTRevocationTestApp",
                "Test Application for JWT Revocation", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        appId = applicationResponse.getData();

    }

    @Test(groups = "wso2.am", description = "Test JWT token revocation by revoking the consumer application")
    public void testConsumerAppBasedJWTRevocation() throws Exception {
        // Generate keys for the application
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();

        // Generate an access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Step 1: Introspect the token and verify it's active
        validateIntrospectionResponse(user, accessToken, consumerKey, true);

        // Step 2: Revoke the consumer application
        oAuthAdminServiceClient.updateOAuthApplicationState(consumerKey, "REVOKED");

        // Step 3: Introspect the token again and verify it's inactive
        validateIntrospectionResponse(user, accessToken, consumerKey, false);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId);
    }

    private void validateIntrospectionResponse(User user, String accessToken, String clientId,
                                               boolean expectedToBeActive) throws Exception {
        String introspectionUrl = "https://localhost:9943/oauth2/introspect";
        if (!"carbon.super".equals(user.getUserDomain())) {
            introspectionUrl = "https://localhost:9943/t/" + user.getUserDomain() + "/oauth2/introspect";
        }
        try (CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost();
            httpPost.addHeader("Authorization",
                    "Basic " + waffle.util.Base64.encode(user.getUserName().concat(":").concat(user.getPassword()).getBytes()));
            httpPost.setURI(URI.create(introspectionUrl));
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair(
                    "token", accessToken)));
            httpPost.setEntity(urlEncodedFormEntity);
            try (CloseableHttpResponse response = closeableHttpClient.execute(httpPost)) {
                Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
                HttpEntity entity = response.getEntity();
                JSONObject jsonPayload = (JSONObject) new JSONParser().parse(new InputStreamReader(entity.getContent()));
                if (expectedToBeActive) {
                    Assert.assertTrue((Boolean) jsonPayload.get("active"));
                    Assert.assertNotNull(jsonPayload.get("client_id"));
                    Assert.assertEquals(jsonPayload.get("client_id"), clientId);
                } else {
                    Assert.assertFalse((Boolean) jsonPayload.get("active"));
                }
            }
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            throw new Exception(e);
        }
    }

}