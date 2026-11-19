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
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Explicit parity steps for the API version-ordering/pagination regression. The legacy test creates five
 * four-version families and observes the latest published version after each transition. These steps retain
 * those version-string partitions while scoping list membership assertions to the APIs owned by this scenario,
 * so unrelated APIs in a shared test container cannot make a global count flaky.
 */
public class VersioningParitySteps {

    private final BaseSteps baseSteps = new BaseSteps();
    private final PublisherBaseSteps publisherSteps = new PublisherBaseSteps();
    private final ApplicationBaseSteps applicationSteps = new ApplicationBaseSteps();

    @When("I exercise API version ordering for these version families:")
    public void exerciseApiVersionOrdering(DataTable table) throws Exception {

        List<Map<String, String>> families = table.asMaps(String.class, String.class);
        Assert.assertFalse(families.isEmpty(), "At least one version family is required");
        List<String> publisherIds = new ArrayList<>();
        List<String> latestStoreIds = new ArrayList<>();

        for (Map<String, String> family : families) {
            String familyKey = family.get("family");
            String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            // Keep API name/context well below product validation limits even for the longest family key, while
            // retaining a per-scenario random suffix so parallel blocks cannot collide.
            String apiName = "VParity" + familyKey + "_" + uniqueSuffix;
            String apiContext = "vparity-" + familyKey + "-" + uniqueSuffix;
            String payloadKey = "versioningParityPayload";
            String v1 = "versioningParityV1";
            String v2 = "versioningParityV2";
            String v3 = "versioningParityV3";
            String v4 = "versioningParityV4";

            baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_api.json", payloadKey);
            baseSteps.iSetFieldInPayload("name", apiName, payloadKey);
            baseSteps.iSetFieldInPayload("context", apiContext, payloadKey);
            baseSteps.iSetFieldInPayload("version", family.get("v1"), payloadKey);
            publisherSteps.iCreateAnAPIWithPayloadAs("apis", payloadKey, v1);
            baseSteps.theResponseStatusCodeShouldBe(201);
            publishAndAssertLatest(v1, apiName, family.get("v1"));

            publisherSteps.iCreateANewVersionOfAPI(family.get("v2"), "apis", v1, "false", v2);
            baseSteps.theResponseStatusCodeShouldBe(201);
            publishAndAssertLatest(v2, apiName, family.get("v2"));

            // Create an older version AFTER the latest version. Before and after publishing it, the Store must
            // continue to expose v2 as latest. This is the late-publish branch that was absent from the earlier
            // V2 comparator-only case.
            publisherSteps.iCreateANewVersionOfAPI(family.get("v3"), "apis", v2, "false", v3);
            baseSteps.theResponseStatusCodeShouldBe(201);
            assertLatest(apiName, family.get("v2"));
            publisherSteps.iDeployAPI(v3);
            baseSteps.theResponseStatusCodeShouldBe(201);
            publisherSteps.iPublishTheResource("apis", v3);
            publisherSteps.theLifecycleStatusShouldBe(v3, "Published");
            assertLatest(apiName, family.get("v2"));

            // Removing the comparator-latest v2 must promote the surviving v3, and a later v4 must become latest.
            publisherSteps.iDeleteTheResource("apis", v2);
            baseSteps.theResponseStatusCodeShouldBe(200);
            assertLatest(apiName, family.get("v3"));
            publisherIds.add(String.valueOf(TestContext.resolve(v1)));
            publisherIds.add(String.valueOf(TestContext.resolve(v3)));

            publisherSteps.iCreateANewVersionOfAPI(family.get("v4"), "apis", v3, "false", v4);
            baseSteps.theResponseStatusCodeShouldBe(201);
            publisherSteps.iDeployAPI(v4);
            baseSteps.theResponseStatusCodeShouldBe(201);
            publisherSteps.iPublishTheResource("apis", v4);
            publisherSteps.theLifecycleStatusShouldBe(v4, "Published");
            assertLatest(apiName, family.get("v4"));
            publisherIds.add(String.valueOf(TestContext.resolve(v4)));
            latestStoreIds.add(String.valueOf(TestContext.resolve(v4)));
        }

        Assert.assertEquals(publisherIds.size(), families.size() * 3,
                "Each family must retain base, late-older, and final versions after deleting the intermediate latest");
        TestContext.set("versioningParityPublisherIds", publisherIds);
        TestContext.set("versioningParityLatestStoreIds", latestStoreIds);
    }

    @Then("the Publisher and DevPortal paginated listings should contain the version-family artifacts exactly once")
    public void assertVersionFamilyPagination() throws IOException {

        List<String> publisherIds = stringList(TestContext.get("versioningParityPublisherIds"));
        List<String> latestStoreIds = stringList(TestContext.get("versioningParityLatestStoreIds"));
        List<String> publisherListing = readAllIds("publisher", 4);
        List<String> storeListing = readAllIds("devportal", 2);

        assertExactlyOnce(publisherListing, publisherIds, "Publisher");
        assertExactlyOnce(storeListing, latestStoreIds, "DevPortal");
        List<String> supersededIds = publisherIds.stream().filter(id -> !latestStoreIds.contains(id)).toList();
        for (String id : supersededIds) {
            Assert.assertEquals(storeListing.stream().filter(id::equals).count(), 0L,
                    "The grouped DevPortal listing must not expose a non-latest version " + id);
        }
    }

    private void publishAndAssertLatest(String apiIdKey, String apiName, String expectedVersion)
            throws IOException, InterruptedException {

        publisherSteps.iDeployAPI(apiIdKey);
        baseSteps.theResponseStatusCodeShouldBe(201);
        publisherSteps.iPublishTheResource("apis", apiIdKey);
        publisherSteps.theLifecycleStatusShouldBe(apiIdKey, "Published");
        assertLatest(apiName, expectedVersion);
    }

    private void assertLatest(String apiName, String expectedVersion) throws IOException, InterruptedException {

        applicationSteps.theDevportalListingShouldListApiOnce(apiName, 1, expectedVersion, 60);
    }

    private List<String> readAllIds(String plane, int pageSize) throws IOException {

        boolean publisherPlane = "publisher".equals(plane);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer "
                + (publisherPlane ? Identity.publisherToken() : Identity.devportalToken()));
        List<String> ids = new ArrayList<>();
        int offset = 0;
        Integer total = null;
        while (total == null || offset < total) {
            String url = publisherPlane
                    ? Utils.getAPISearchEndpointURL(Utils.getBaseUrl(), null, pageSize, offset)
                    : Utils.getDevportalApiListURL(Utils.getBaseUrl(), pageSize, offset);
            HttpResponse response = Requests.get(url, headers);
            Assert.assertNotNull(response, plane + " listing request returned no response at offset " + offset);
            Assert.assertEquals(response.getResponseCode(), 200,
                    plane + " listing failed at offset " + offset + ": " + response.getData());
            JSONObject body = new JSONObject(response.getData());
            JSONArray list = body.optJSONArray("list");
            JSONObject pagination = body.optJSONObject("pagination");
            Assert.assertNotNull(list, plane + " response has no list array: " + response.getData());
            Assert.assertNotNull(pagination, plane + " response has no pagination object: " + response.getData());
            Assert.assertEquals(pagination.optInt("offset", -1), offset,
                    plane + " pagination offset does not match the requested page");
            Assert.assertEquals(pagination.optInt("limit", -1), pageSize,
                    plane + " pagination limit does not match the requested page size");
            int pageTotal = pagination.optInt("total", -1);
            Assert.assertTrue(pageTotal >= 0, plane + " pagination total is missing or invalid");
            if (total == null) {
                total = pageTotal;
            } else {
                Assert.assertEquals(pageTotal, total.intValue(),
                        plane + " pagination total changed while traversing stable scenario data");
            }
            for (int index = 0; index < list.length(); index++) {
                ids.add(list.getJSONObject(index).getString("id"));
            }
            Assert.assertEquals(list.length(), Math.min(pageSize, Math.max(0, total - offset)),
                    plane + " page boundary returned an unexpected number of entries at offset " + offset);
            offset += list.length();
            if (list.length() == 0) {
                break;
            }
        }
        return ids;
    }

    private void assertExactlyOnce(List<String> actualIds, List<String> expectedIds, String plane) {

        for (String id : expectedIds) {
            Assert.assertEquals(actualIds.stream().filter(id::equals).count(), 1L,
                    plane + " paginated listing must contain scenario-owned API " + id + " exactly once");
        }
    }

    private List<String> stringList(Object value) {

        Assert.assertTrue(value instanceof List<?>, "Expected a list of scenario-owned version IDs");
        return ((List<?>) value).stream().map(String::valueOf).toList();
    }
}
