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
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.List;

/**
 * Teardown sweep for resources tests create ON the external Identity Server (the {@link IntegrationActors#IS}
 * system) — the IS-side counterpart of {@link ResourceCleanup}, deleting each registered resource as the IS
 * integration actor via IS's own management APIs (an APIM actor token cannot address them). Invoked from
 * {@link ResourceCleanup#deleteRegisteredResources()}, so it rides both teardown paths (the per-scenario
 * {@code @cleanup} hook and the runner's AfterClass sweep).
 *
 * <p>Today the shared IS container is Ryuk-reaped at JVM exit, so this sweep buys zero-residue WITHIN a run;
 * its real purpose is the CLAUDE.md §14 integration-actor contract — the moment an integration is a SaaS
 * rather than an ephemeral container, per-resource cleanup as the registered principal is the only thing
 * standing between the suite and unbounded residue in a persistent account.
 *
 * <p>Per the §5 rules: creates {@code register…} on success; a non-2xx delete is logged as WARN (a real leak
 * signal); lists are cleared either way so a failed sweep cannot double-fire.
 */
public final class ISResourceCleanup {

    private static final Log logger = LogFactory.getLog(ISResourceCleanup.class);

    /** IS OIDC applications created via {@code api/server/v1/applications} (ids from the Location header). */
    public static final String CREATED_IS_APPLICATION_IDS = "createdIsApplicationIds";
    /** IS users created via {@code scim2/Users} (SCIM ids). */
    public static final String CREATED_IS_USER_IDS = "createdIsUserIds";

    private ISResourceCleanup() {
    }

    /** Registers an IS OIDC application (by id) for the teardown sweep. */
    public static void registerApplication(Object appId) {
        TestContext.addToList(CREATED_IS_APPLICATION_IDS, String.valueOf(appId));
    }

    /** Registers an IS SCIM2 user (by id) for the teardown sweep. */
    public static void registerUser(Object userId) {
        TestContext.addToList(CREATED_IS_USER_IDS, String.valueOf(userId));
    }

    /** True if nothing is registered (lets callers skip without resolving the integration actor). */
    public static boolean isEmpty() {
        return TestContext.getList(CREATED_IS_APPLICATION_IDS).isEmpty()
                && TestContext.getList(CREATED_IS_USER_IDS).isEmpty();
    }

    /**
     * Deletes every registered IS resource as the IS integration actor. No-op when nothing is registered;
     * defensive no-op (with a WARN, since registered ids imply IS was booted) if the actor is absent.
     */
    public static void sweep() {
        if (isEmpty()) {
            return;
        }
        if (!IntegrationActors.isRegistered(IntegrationActors.IS)) {
            logger.warn("IS resources are registered for cleanup but no IS integration actor is present; "
                    + "skipping the IS sweep (resources will die with the IS container)");
            clear();
            return;
        }
        String base = IntegrationActors.baseUrl(IntegrationActors.IS);
        deleteAll(CREATED_IS_APPLICATION_IDS, base + "api/server/v1/applications/", "IS application");
        deleteAll(CREATED_IS_USER_IDS, base + "scim2/Users/", "IS user");
    }

    private static void deleteAll(String listKey, String urlPrefix, String what) {
        List<Object> ids = TestContext.getList(listKey);
        if (!ids.isEmpty()) {
            // Success is otherwise silent; log the sweep so "no WARNs" provably means "swept", not "never ran".
            logger.info("Sweeping " + ids.size() + " " + what + "(s) on the external IS");
        }
        for (Object id : ids) {
            try {
                HttpResponse resp = SimpleHTTPClient.getInstance().doDelete(urlPrefix + id,
                        IntegrationActors.authHeaders(IntegrationActors.IS));
                if (resp == null || resp.getResponseCode() < 200 || resp.getResponseCode() >= 300) {
                    logger.warn("Failed to delete " + what + " '" + id + "' on the external IS: got "
                            + (resp == null ? "no response" : resp.getResponseCode() + " / " + resp.getData()));
                }
            } catch (Exception e) {
                logger.warn("Failed to delete " + what + " '" + id + "' on the external IS", e);
            }
        }
        ids.clear();
    }

    private static void clear() {
        TestContext.getList(CREATED_IS_APPLICATION_IDS).clear();
        TestContext.getList(CREATED_IS_USER_IDS).clear();
    }
}
