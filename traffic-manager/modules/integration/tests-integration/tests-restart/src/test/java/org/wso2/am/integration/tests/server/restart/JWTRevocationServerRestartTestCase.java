/*
 *Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.restart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JWTRevocationServerRestartTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private String consumerKey;
    private String consumerSecret;
    private Map<String, String> requestHeaders;
    private String apiInvocationUrl;
    private String accessToken;
    private String jwtRevocationAppId;

    @BeforeClass
    public void initialize(ITestContext ctx) throws Exception {
        super.init();

        jwtRevocationAppId = (String) ctx.getAttribute("jwtRevocationAppId");
        //Generate production access token
        ArrayList<String> jwtRevocationGrantTypes = new ArrayList<>();
        jwtRevocationGrantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO jwtRevocationApplicationKeyDTO = restAPIStore
                .generateKeys(jwtRevocationAppId, APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, null,
                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, jwtRevocationGrantTypes);
        Assert.assertNotNull(jwtRevocationApplicationKeyDTO.getToken());
        accessToken = jwtRevocationApplicationKeyDTO.getToken().getAccessToken();
        consumerKey = jwtRevocationApplicationKeyDTO.getConsumerKey();
        consumerSecret = jwtRevocationApplicationKeyDTO.getConsumerSecret();

        apiInvocationUrl = getAPIInvocationURLHttp("jwtTokenTestAPI/1.0.0/customers/123");
        requestHeaders = new HashMap<>();
        requestHeaders.put("accept", MediaType.TEXT_XML);
        requestHeaders.put("Authorization", "Bearer " + accessToken);
    }

    @Test(groups = "wso2.am", description = "testing jwt token revocation")
    public void testJWTTokenRevocation() throws Exception {

        waitForAPIDeploymentSync(user.getUserName(), "JWTTokenTestAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        // Test JWT token validity before revocation
        HttpResponse invocationResponse =
                HttpRequestUtil.doGet(apiInvocationUrl, requestHeaders);
        Assert.assertEquals(invocationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before Retire");
        Assert.assertTrue(invocationResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before Retire" +
                        " Response Data:" + invocationResponse.getData());

        //Call the revoke Endpoint
        Map<String, String> revokeRequestHeaders = new HashMap<>();
        String basicAuthHeader = consumerKey + ":" + consumerSecret;
        byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes(StandardCharsets.UTF_8));
        revokeRequestHeaders.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        String input = "token=" + accessToken;
        URL revokeEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/revoke");
        HttpResponse revokeResponse;
        try {
            revokeResponse = HTTPSClientUtils.doPost(revokeEndpointURL, input, revokeRequestHeaders);
            Assert.assertEquals(revokeResponse.getResponseCode(), 200);
            Assert.assertEquals(revokeResponse.getHeaders().get("RevokedAccessToken"), accessToken,
                    "Access token is not revoked correctly");
        } catch (Exception e) {
            org.junit.Assert.fail("Should not throw any exceptions" + e);
        }

        // Test JWT token validity after sending the revoke request
        boolean isTokenValid;
        HttpResponse invocationResponseAfterRevoked;
        int counter = 1;
        do {
            // Wait while the JMS message is received to the related JMS topic
            Thread.sleep(1000L);
            invocationResponseAfterRevoked = HttpRequestUtil.doGet(apiInvocationUrl,
                    requestHeaders);
            int invocationResponseCodeAfterRevoked = invocationResponseAfterRevoked.getResponseCode();

            if (invocationResponseCodeAfterRevoked == HTTP_RESPONSE_CODE_UNAUTHORIZED) {
                isTokenValid = false;
            } else if (invocationResponseCodeAfterRevoked == HTTP_RESPONSE_CODE_OK) {
                isTokenValid = true;
            } else {
                throw new APIManagerIntegrationTestException("Unexpected response received when invoking the API. " +
                        "Response received :" + invocationResponseAfterRevoked.getData() + ":" +
                        invocationResponseAfterRevoked.getResponseMessage());
            }
            counter++;
        } while (isTokenValid && counter < 20);

        Assert.assertFalse(isTokenValid, "Access token revocation failed. API invocation response code is expected to" +
                " be : " + HTTP_RESPONSE_CODE_UNAUTHORIZED + ", but got " + invocationResponseAfterRevoked.getResponseCode());

    }

}
