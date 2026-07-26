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
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps for the admin-plane API-category REST API ({@code /api/am/admin/v4/api-categories}): create / negatives
 * / update / list / delete. Ports APICategoriesTestCase. Categories are tenant-global, so scenarios use a
 * uniquely-generated name; the create registers the id with {@link ResourceCleanup} so it is swept even if the
 * scenario fails before its inline delete. Requests funnel through {@link Requests}; the created id is read only
 * after the create's success is asserted.
 */
public class ApiCategorySteps {

    private String getBaseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    private Map<String, String> adminHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        return headers;
    }

    /** Creates an API category (admin), asserts 201, registers it for teardown and stores its id. */
    @When("I create an API category with payload {string} as {string}")
    public void iCreateApiCategory(String payload, String categoryIdKey) throws IOException {
        String jsonPayload = Utils.resolveContextPlaceholders(TestContext.resolve(payload).toString());
        HttpResponse response = Requests.post(Utils.getApiCategoriesURL(getBaseUrl()), adminHeaders(), jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object categoryId = Utils.extractValueFromPayload(response.getData(), "id");
        // Register for failure-safe teardown: the feature deletes the category inline on its happy path, but an
        // earlier-step failure would skip that and leak the tenant-global category.
        ResourceCleanup.register(Constants.CREATED_API_CATEGORY_IDS, categoryId);
        TestContext.set(Utils.normalizeContextKey(categoryIdKey), categoryId);
    }

    /**
     * Attempts to create an API category WITHOUT asserting success — for the negatives (no name / special
     * characters / duplicate). The response is published so the feature asserts the status and body.
     */
    @When("I attempt to create an API category with payload {string}")
    public void iAttemptToCreateApiCategory(String payload) throws IOException {
        String jsonPayload = Utils.resolveContextPlaceholders(TestContext.resolve(payload).toString());
        Requests.post(Utils.getApiCategoriesURL(getBaseUrl()), adminHeaders(), jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Updates an API category by id (admin). Non-asserting; the feature confirms the status and reflected body. */
    @When("I update the API category {string} with payload {string}")
    public void iUpdateApiCategory(String categoryIdKey, String payload) throws IOException {
        String categoryId = TestContext.resolve(categoryIdKey).toString();
        String jsonPayload = Utils.resolveContextPlaceholders(TestContext.resolve(payload).toString());
        Requests.put(Utils.getApiCategoryByIdURL(getBaseUrl(), categoryId), adminHeaders(), jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Retrieves all API categories (admin). Non-asserting; the feature confirms the status and contents. */
    @When("I retrieve all API categories")
    public void iRetrieveAllApiCategories() throws IOException {
        Requests.get(Utils.getApiCategoriesURL(getBaseUrl()), adminHeaders());
    }

    /** Deletes an API category by id (admin). Non-asserting; the feature confirms the status. */
    @When("I delete the API category {string}")
    public void iDeleteApiCategory(String categoryIdKey) throws IOException {
        String categoryId = TestContext.resolve(categoryIdKey).toString();
        Requests.delete(Utils.getApiCategoryByIdURL(getBaseUrl(), categoryId), adminHeaders());
    }
}
