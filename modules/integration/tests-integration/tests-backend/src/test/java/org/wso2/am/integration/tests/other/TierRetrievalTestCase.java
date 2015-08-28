/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Related to Patch Automation  https://wso2.org/jira/browse/APIMANAGER-3394
 * This test class tests the outcome of the API call which is designed to fetch the tier list
 */
public class TierRetrievalTestCase extends APIMIntegrationBaseTest {

    private String[] tierList = {"Unlimited", "Gold", "Silver", "Bronze"};

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException {
        super.init();
    }

    @Test(groups = {"wso2.am"}, description = "Tier retrieval test case")
    public void testTierRetrieval() {

        boolean isLoginSuccess = false;
        boolean isTierRetrievalSuccess = false;
        boolean tiersAvailable = false;
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);

        //send the request to log-in to the api store
        HttpResponse storeLoginResponse = null;
        try {
            storeLoginResponse = apiStore.login("admin", "admin");
        } catch (APIManagerIntegrationTestException e) {
            //if an error occurs while login, the test is failed
            assertTrue(false, "Login to API Store in tier retrieval test case has failed.");
        }
        String loginResponseCookie = storeLoginResponse.getHeaders().get("Set-Cookie");

        String successResponse = "{\"error\" : false}";
        if (successResponse.equals(storeLoginResponse.getData())) {
            isLoginSuccess = true;
        }

        //the second request to get tiers, is sent only if the login is successful
        if (isLoginSuccess) {
            String tierRequestUrl = storeURLHttp + "store/site/blocks/item-add/ajax/add.jag?action=getTiers";
            Map<String, String> tierRequestHeaders = new HashMap<String, String>();
            tierRequestHeaders.put("Cookie", loginResponseCookie);
            HttpResponse tierFetchResponse = null;
            try {
                tierFetchResponse = HttpRequestUtil.doGet(tierRequestUrl, tierRequestHeaders);
            } catch (IOException e) {
                //if an error occurs while getting the response, the test is failed
                assertTrue(false, "Retrieving tiers is tier retrieval test case has failed.");
            }
            String tierDataText = "";
            if(tierFetchResponse != null) {
                tierDataText = tierFetchResponse.getData();
            }
            JsonElement tierDataElement = new JsonParser().parse(tierDataText);
            JsonObject tierDataObject = tierDataElement.getAsJsonObject();

            // the data in response is evaluated further only if the response indicates successful tier retrieval
            // that is response for the "error" tag should be "false"
            if ("false".equals(tierDataObject.get("error").toString())) {
                isTierRetrievalSuccess = true;
                JsonArray tiersArray = tierDataObject.getAsJsonArray("tiers");

                //scan and compare the returned tier list with the expected tier list
                for (int i = 0; i < tiersArray.size(); i++) {
                    JsonElement tierElement = tiersArray.get(i).getAsJsonObject().get("tierName");
                    String tierNameWithCommas = tierElement.toString();
                    String tierNameWithoutCommas = "";
                    //remove the commas in the text
                    if (tierNameWithCommas != null && tierNameWithCommas.length() > 2) {
                        tierNameWithoutCommas = tierNameWithCommas.substring(1, tierNameWithCommas.length() - 1);
                    }
                    if (Arrays.asList(tierList).contains(tierNameWithoutCommas)) {
                        tiersAvailable = true;
                    } else {
                        //if at least one non-existing tier has found, the flag is set to false
                        tiersAvailable = false;
                        break;
                    }
                }
            }
        }
        assertTrue(isLoginSuccess && isTierRetrievalSuccess && tiersAvailable, "Tier retrieval test case failed.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}
