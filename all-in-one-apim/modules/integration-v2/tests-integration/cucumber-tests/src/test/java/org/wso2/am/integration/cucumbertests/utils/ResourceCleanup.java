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

package org.wso2.am.integration.cucumbertests.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Best-effort, idempotent teardown of the APIs and applications a test registered via
 * {@link TestContext#addToList} under {@link Constants#CREATED_API_IDS} /
 * {@link Constants#CREATED_APPLICATION_IDS}. Shared by two callers driving different teardown granularities:
 *
 * <ul>
 *   <li>the {@code @After("@cleanup")} hook — per-scenario teardown, for self-contained single-scenario
 *       features that create and tear down their own resources; and</li>
 *   <li>the per-runner {@code @AfterClass} sweep on {@code BaseBlockRunner} — for the {@code _setup_*}
 *       fixture pattern, where a setup feature creates resources that several later scenarios in the SAME
 *       runner consume, so teardown must run once at the end of the runner rather than after each scenario
 *       (a per-scenario hook would delete the shared fixtures out from under the scenarios that follow).</li>
 * </ul>
 *
 * Applications are deleted before APIs because removing an application also removes its subscriptions, which
 * would otherwise block deletion of a subscribed API with a 409. Deletes are best-effort: a resource the
 * scenario already deleted itself simply 404s and is ignored. The id lists are cleared afterwards so ids do
 * not leak into a later scenario/runner reusing the same scope.
 */
public final class ResourceCleanup {

    private static final Log logger = LogFactory.getLog(ResourceCleanup.class);

    private ResourceCleanup() {
    }

    /** Deletes all registered applications then APIs from the context's current scope. No-op if no baseUrl. */
    public static void deleteRegisteredResources() {

        Object baseUrlObj = TestContext.get("baseUrl");
        if (baseUrlObj == null) {
            return;
        }

        // Nothing was registered (e.g. a framework-probe block that boots a container but creates no resources
        // and never sets an acting actor). Skip before resolving any actor-scoped token key — doing so would
        // call Identity.defaultActor() and fail with "Tenant not found in context" for an actor-less block.
        if (TestContext.getList(Constants.CREATED_APPLICATION_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_API_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_OPERATION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_SHARED_SCOPE_IDS).isEmpty()) {
            return;
        }
        String baseUrl = baseUrlObj.toString();

        try {
            deleteResources(Constants.CREATED_APPLICATION_IDS, Identity.devportalTokenKey(Identity.defaultActor()),
                    id -> Utils.getApplicationEndpointURL(baseUrl, id));
            deleteResources(Constants.CREATED_API_IDS, Identity.publisherTokenKey(Identity.defaultActor()),
                    id -> Utils.getResourceEndpointURL(baseUrl, "apis", id));
            // Common (reusable) operation policies are tenant-global, so they outlive their API and must be
            // swept explicitly (API-specific policies are removed when their API is deleted above).
            deleteResources(Constants.CREATED_OPERATION_POLICY_IDS, Identity.publisherTokenKey(Identity.defaultActor()),
                    id -> Utils.getResourceEndpointURL(baseUrl, "operation-policies", id));
            // Shared scopes last: an API that references a scope must be deleted before the scope itself.
            deleteResources(Constants.CREATED_SHARED_SCOPE_IDS, Identity.publisherTokenKey(Identity.defaultActor()),
                    id -> Utils.getAPIScopesById(baseUrl, id));
        } finally {
            TestContext.remove(Constants.CREATED_API_IDS);
            TestContext.remove(Constants.CREATED_APPLICATION_IDS);
            TestContext.remove(Constants.CREATED_SHARED_SCOPE_IDS);
            TestContext.remove(Constants.CREATED_OPERATION_POLICY_IDS);
        }
    }

    private static void deleteResources(String contextKey, String tokenKey, Function<String, String> urlBuilder) {

        Object tokenObj = TestContext.get(tokenKey);
        if (tokenObj == null) {
            return;
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + tokenObj);

        List<Object> ids = TestContext.getList(contextKey);
        for (Object id : ids) {
            if (id == null) {
                continue;
            }
            try {
                SimpleHTTPClient.getInstance().doDelete(urlBuilder.apply(id.toString()), headers);
            } catch (Exception e) {
                // Teardown is best-effort: a resource may already be deleted by the scenario itself.
                logger.warn("Cleanup failed to delete resource " + id + " (" + contextKey + "): " + e.getMessage());
            }
        }
    }
}
