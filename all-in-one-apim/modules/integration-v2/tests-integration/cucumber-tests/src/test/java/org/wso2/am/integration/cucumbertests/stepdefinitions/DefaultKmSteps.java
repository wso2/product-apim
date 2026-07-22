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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.DefaultKmProvisioner;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;

/**
 * Steps for the WSO2-IS-7 default-key-manager blocks (plan items #24/#31). They drive APIM's tenant-sharing
 * notify endpoint and inspect the provisioned tenant's key managers - see {@link DefaultKmProvisioner}.
 */
public class DefaultKmSteps {

    private static final String KM_LIST_KEY = "dkmKeyManagerList";
    private static final String ADMIN_SCOPE = "apim:admin_operations";
    private static final String APP_SCOPE = "apim:subscribe apim:app_manage";

    @When("I synchronize a new tenant {string} with admin password {string} via the tenant-sharing notify endpoint")
    public void iSynchronizeTenant(String tenantDomain, String adminPassword) throws IOException {
        TestContext.remove("httpResponse");
        HttpResponse response = DefaultKmProvisioner.notifyTenantCreated(tenantDomain, adminPassword);
        TestContext.set("httpResponse", response);
    }

    @Then("the key manager list for admin {string} password {string} has {int} entries")
    public void theKeyManagerListHasEntries(String adminUser, String password, int expectedCount)
            throws IOException {
        String token = DefaultKmProvisioner.awaitAdminToken(adminUser, password, ADMIN_SCOPE);
        HttpResponse response = DefaultKmProvisioner.listKeyManagers(token);
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null,
                "Key manager list failed for " + adminUser + ": got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject body = new JSONObject(response.getData());
        Assert.assertEquals(body.getInt("count"), expectedCount,
                "Unexpected key manager count for " + adminUser + " - list=" + response.getData());
        TestContext.set(KM_LIST_KEY, response.getData());
    }

    @Then("the key manager list includes a {string} key manager")
    public void theKeyManagerListIncludesType(String type) {
        Object list = TestContext.get(KM_LIST_KEY);
        Assert.assertNotNull(list, "No key manager list captured; assert the count first");
        JSONArray managers = new JSONObject(list.toString()).getJSONArray("list");
        boolean found = false;
        for (int i = 0; i < managers.length(); i++) {
            if (type.equals(managers.getJSONObject(i).optString("type"))) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "No key manager of type '" + type + "' in " + list);
    }

    @When("I attempt application key generation as admin {string} password {string}")
    public void iAttemptKeyGeneration(String adminUser, String password) throws IOException {
        String token = DefaultKmProvisioner.awaitAdminToken(adminUser, password, APP_SCOPE);
        HttpResponse appResponse = DefaultKmProvisioner.createApplication(token, Names.unique("dkm-nokm-app"));
        Assert.assertTrue(appResponse != null && appResponse.getResponseCode() == 201 && appResponse.getData() != null,
                "Application create failed: got="
                        + (appResponse == null ? "null" : appResponse.getResponseCode() + "/" + appResponse.getData()));
        String applicationId = new JSONObject(appResponse.getData()).getString("applicationId");

        TestContext.remove("httpResponse");
        HttpResponse response = DefaultKmProvisioner.generateKeys(token, applicationId);
        TestContext.set("httpResponse", response);
    }
}
