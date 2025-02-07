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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BurstControlServerRestartTestCase extends APIManagerLifecycleBaseTest {

    private String burstControlApiId;
    private ApplicationDTO burstControlApplicationDTO;
    private final Log log = LogFactory.getLog(BurstControlServerRestartTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment(ITestContext ctx) throws Exception {
        super.init();
        burstControlApiId = (String) ctx.getAttribute("burstControlApiId");
        burstControlApplicationDTO = (ApplicationDTO) ctx.getAttribute("burstControlApplicationDTO");
    }

    @Test(groups = { "wso2.am" }, description = "Test changing the burst limit of an API subscription by subscribing "
            + "to a different subscription policy with different burst limit")
    public void testBurstLimitChange() throws Exception {

        waitForAPIDeploymentSync(user.getUserName(), "APIThrottleBurstAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        //subscribe to API
        String subscriptionTier5RPMburst = "SubscriptionTier5RPMburst";
        SubscriptionDTO subscriptionDTO1 = restAPIStore.subscribeToAPI(burstControlApiId, burstControlApplicationDTO.getApplicationId(),
                subscriptionTier5RPMburst);
        Assert.assertEquals(subscriptionTier5RPMburst, subscriptionDTO1.getThrottlingPolicy(), "Error occurred "
                + "while subscribing to the api. Subscribed policy is not as expected as "
                + subscriptionTier5RPMburst);

        // generate keys and token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(burstControlApplicationDTO.getApplicationId(), "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        String APIVersion = "1.0.0";
        String APIContext = "api_burst";
        String apiInvocationUrl = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");

        // verify burst control on subscription tier "subscriptionTier5RPMburst"
        int burstLimit1 = 5;
        checkThrottling(apiInvocationUrl, requestHeaders, burstLimit1);

        // remove previous subscription
        log.info("Old subscription id:" + subscriptionDTO1.getSubscriptionId());
        HttpResponse httpResponse = restAPIStore.removeSubscription(subscriptionDTO1);
        log.info("httpResponse of removeSubscription ====== : " + httpResponse);
        log.info("AAA= " + subscriptionDTO1);
        Thread.sleep(5000);

        // add new subscription
        String subscriptionTier25RPMburst = "SubscriptionTier25RPMburst";
        SubscriptionDTO subscriptionDTO2 = restAPIStore.subscribeToAPI(burstControlApiId, burstControlApplicationDTO.getApplicationId(),
                subscriptionTier25RPMburst);
        Assert.assertEquals(subscriptionTier25RPMburst, subscriptionDTO2.getThrottlingPolicy(), "Error occurred "
                + "while subscribing to the api. Subscribed policy is not as expected as "
                + subscriptionTier25RPMburst);
        Thread.sleep(60000); // wait until throttled period ends
        // verify burst control on subscription tier "subscriptionTier25RPMburst"
        int burstLimit2 = 25;
        checkThrottling(apiInvocationUrl, requestHeaders, burstLimit2);
    }

    private void checkThrottling(String invokeURL, Map<String, String> requestHeaders, int limit)
            throws IOException, InterruptedException {
        HttpResponse apiCallResponse;
        for (int count = 1; count < limit + 2; count++) {
            String payload = "{\"payload\" : \"test\"}";
            if (count > limit) {
                boolean isThrottled = false;
                for (; count < limit + 10; count++) { // invoke 10 more times to bear the throttle delay
                    apiCallResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
                    if (apiCallResponse.getResponseCode() == 429) {
                        isThrottled = true;
                        break;
                    }
                    Thread.sleep(500);
                }
                Assert.assertTrue(isThrottled, "Throttling has't happened at the expected count");
            } else {
                apiCallResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
                if (apiCallResponse.getResponseCode() == 429) {
                    Assert.fail("Throttling has happened at the count : " + count
                            + ". But expected to throttle after the request count " + limit);
                }
                else {
                    Assert.assertEquals(apiCallResponse.getResponseCode(), org.apache.commons.httpclient.HttpStatus.SC_OK,
                            "API invocation Response code is not as expected");
                }
            }
        }
    }
}
