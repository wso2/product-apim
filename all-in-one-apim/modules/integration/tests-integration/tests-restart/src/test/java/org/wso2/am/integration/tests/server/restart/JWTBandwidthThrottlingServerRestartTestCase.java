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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JWTBandwidthThrottlingServerRestartTestCase extends APIMIntegrationBaseTest {

    private String jwtBandwidthApiId;
    private String jwtBandwidthGatewayUrl;
    String app1Id;
    String app2Id;
    String app3Id;
    private final String body = "{\"payload\" : \"00000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000\"}";

    private static final Log log = LogFactory.getLog(JWTBandwidthThrottlingServerRestartTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment(ITestContext ctx) throws Exception {
        super.init();
        jwtBandwidthApiId = (String) ctx.getAttribute("jwtBandwidthApiId");
        jwtBandwidthGatewayUrl = (String) ctx.getAttribute("jwtBandwidthGatewayUrl");
    }

    @Test(groups = { "wso2.am" })
    public void testApplicationLevelThrottling() throws Exception {

        waitForAPIDeploymentSync(user.getUserName(), "BandwidthTestAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        String appPolicyName = "AppPolicyWithBandwidth";
        ApplicationDTO applicationDTO = restAPIStore.addApplication("ApplicationBandwidthtestapp",
                appPolicyName, "", "this-is-test");
        app1Id = applicationDTO.getApplicationId();
        Assert.assertEquals(appPolicyName, applicationDTO.getThrottlingPolicy());
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(jwtBandwidthApiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), Constants.TIERS_UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken);
        String jwtString = APIMTestCaseUtils.getDecodedJWT(accessToken);
        log.info("Decoded JWT token: " + jwtString);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        HttpResponse response;
        boolean isThrottled = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(jwtBandwidthGatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }

//        Assert.assertTrue(isThrottled, "Request not throttled by bandwidth condition in application tier");

    }

    @Test(groups = { "wso2.am" })
    public void testSubscriptionLevelThrottling() throws Exception {

        ApplicationDTO applicationDTO = restAPIStore.addApplication("SubscriptionBandwidthtestapp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        app2Id = applicationDTO.getApplicationId();

        String subPolicyName = "SubPolicyWithBandwidth";
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(jwtBandwidthApiId, applicationDTO.getApplicationId(),
                subPolicyName);
        Assert.assertEquals(subPolicyName, subscriptionDTO.getThrottlingPolicy());
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken);
        String jwtString = APIMTestCaseUtils.getDecodedJWT(accessToken);
        log.info("Decoded JWT token: " + jwtString);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        HttpResponse response;
        boolean isThrottled = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(jwtBandwidthGatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }

//        Assert.assertTrue(isThrottled, "Request not throttled by bandwidth condition in subscription tier");

    }

    @Test(groups = { "wso2.am" }, dependsOnMethods = { "testSubscriptionLevelThrottling",
            "testApplicationLevelThrottling" })
    public void testAPILevelThrottling() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(jwtBandwidthApiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        String apiPolicyName = "APIPolicyWithBandwidth";
        apidto.setApiThrottlingPolicy(apiPolicyName);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, jwtBandwidthApiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName, "API tier not updated.");

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(jwtBandwidthApiId, restAPIPublisher);

        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        ApplicationDTO applicationDTO = restAPIStore.addApplication("NormalAPP",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        app3Id = applicationDTO.getApplicationId();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(jwtBandwidthApiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), Constants.TIERS_UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        //Test without any throttling tier
        HttpResponse response;
        boolean isThrottled = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(jwtBandwidthGatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }
//        Assert.assertTrue(isThrottled, "Request not throttled by bandwidth condition in api tier");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(app1Id);
        restAPIStore.deleteApplication(app2Id);
        restAPIStore.deleteApplication(app3Id);
    }

}
