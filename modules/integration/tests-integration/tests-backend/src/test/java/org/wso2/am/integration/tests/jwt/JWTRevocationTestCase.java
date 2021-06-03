/* * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License. */

package org.wso2.am.integration.tests.jwt;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JWTRevocationTestCase extends APIManagerLifecycleBaseTest {

    private String consumerKey;
    private String consumerSecret;
    private String apiId;
    private String appId;
    private static final String APP_NAME = "JWTTokenRevocationTest-Application";
    private static final String API_NAME = "JWTTokenTestAPI";
    private static final String API_CONTEXT = "jwtTokenTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String API_END_POINT_RESOURCE = "/customers/123";
    private static final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private Map<String, String> apiInvocationRequestHeaders;
    String apiInvocationUrl;
    String accessToken;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();

        //Create an Application with TokenType as JWT
        HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                "This is a test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        appId = applicationResponse.getData();

        //Create the api creation request object
        String endpointUrl = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, appId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Generate production access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(appId, APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, null,
                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();

        apiInvocationUrl = getAPIInvocationURLHttp(API_CONTEXT + "/" + API_VERSION + API_END_POINT_RESOURCE);
        apiInvocationRequestHeaders = new HashMap<String, String>();
        apiInvocationRequestHeaders.put("accept", MediaType.TEXT_XML);
        apiInvocationRequestHeaders.put("Authorization", "Bearer " + accessToken);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = "wso2.am", description = "testing jwt token revocation")
    public void testJWTTokenRevocation() throws Exception {

        // Test JWT token validity before revocation
        HttpResponse invocationResponse =
                HttpRequestUtil.doGet(apiInvocationUrl, apiInvocationRequestHeaders);
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
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse revokeResponse;
        try {
            revokeResponse = HTTPSClientUtils.doPost(revokeEndpointURL, input, revokeRequestHeaders);
            Assert.assertEquals(revokeResponse.getResponseCode(), 200);
            Assert.assertEquals(revokeResponse.getHeaders().get("RevokedAccessToken"), accessToken,
                    "Access token is not revoked correctly");
        } catch (Exception e) {
            org.junit.Assert.fail("Should not throw any exceptions" + e);
        }

        // Test JWT token validity after sending the revoke request
        boolean isTokenValid = true;
        HttpResponse invocationResponseAfterRevoked;
        int counter = 1;
        do {
            // Wait while the JMS message is received to the related JMS topic
            Thread.sleep(1000L);
            invocationResponseAfterRevoked = HttpRequestUtil.doGet(apiInvocationUrl,
                    apiInvocationRequestHeaders);
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

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(appId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
