/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminBaseSteps {

    private final String baseUrl;

    public AdminBaseSteps() {

        baseUrl = TestContext.get("baseUrl").toString();
    }

    /**
     * Updates the API provider of an API.
     * This is an administrative operation that changes the ownership of an API from one user to another.
     * 
     * @param providerName The username of the new API provider/owner
     * @param apiID Context key containing the API ID whose provider needs to be updated
     */
    @When("I update the api provider with {string} for {string}")
    public void iUpdateTheApiProviderWithFor(String providerName, String apiID) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("adminAccessToken").toString());

        HttpResponse changeProviderResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIProvider(baseUrl, actualApiId, providerName), headers, null, null);

        TestContext.set("httpResponse", changeProviderResponse);
    }
}
