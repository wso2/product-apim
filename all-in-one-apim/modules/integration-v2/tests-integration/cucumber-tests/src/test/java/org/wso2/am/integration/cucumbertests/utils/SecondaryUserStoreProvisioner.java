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
import org.wso2.am.testcontainers.DynamicApimContainer;

/**
 * Framework facility that stands up a JDBC secondary user store ({@code SECONDARY.COM}) entirely at RUNTIME —
 * no seeded {@code .mv.db} fixture and no boot-time {@code serverFilesToCopy}. Invoked by
 * {@code BlockLifecycleListener} when a block sets {@code initSecondaryUserStore=true}, AFTER tenant/user
 * provisioning (the store registration uses each tenant admin's SOAP credentials).
 *
 * <p>Steps (see the design doc {@code docs/devs/secondary-userstore-framework-architecture.md}):
 * <ol>
 *   <li><b>Schema</b> — create the usermgt {@code UM_*} tables in a fresh embedded H2 DB using the PRODUCT'S OWN
 *       shipped {@code dbscripts/h2.sql} run in-container via the bundled H2 engine (the framework owns zero DDL).
 *       Done ONCE — one DB backs every tenant's store, isolated by the {@code UM_TENANT_ID} column.</li>
 *   <li><b>Register</b> — {@code UserStoreConfigAdminService.addUserStore} (SOAP) per tenant. The domain MUST be
 *       dotted ({@code SECONDARY.COM}); a bare {@code SECONDARY} is rejected for a runtime-added store.</li>
 *   <li><b>Wait</b> — addUserStore deploys ASYNCHRONOUSLY (~10s), so poll until the domain resolves.</li>
 * </ol>
 * The H2 strategy is intentionally isolated here so a MySQL/Postgres strategy can slot in later (the suite's DB
 * type follows {@code API_MANAGER_DATABASE_*}); only schema-creation differs across DBs.
 */
public final class SecondaryUserStoreProvisioner {

    private static final Log logger = LogFactory.getLog(SecondaryUserStoreProvisioner.class);

    /** Runtime-added JDBC secondary user store domain (dotted — a bare "SECONDARY" is rejected at runtime). */
    public static final String SECONDARY_DOMAIN = "SECONDARY.COM";
    /** H2 db path relative to the server home (one shared DB across tenants; UM_TENANT_ID isolates their users). */
    private static final String DB_RELATIVE_PATH = "repository/database/WSO2SEC_DB";
    /** DB_CLOSE_ON_EXIT=FALSE keeps the embedded H2 DB open for the store's connection pool. */
    private static final String JDBC_URL = "jdbc:h2:./" + DB_RELATIVE_PATH + ";DB_CLOSE_ON_EXIT=FALSE";

    /**
     * Framework-seeded store-user actor keys. The key deliberately carries the store domain so a feature's
     * {@code Examples} table is self-documenting — a reader sees the actor lives in the secondary store. It is
     * ALSO the store-local username, so the two never drift; the auth layer appends {@code @<tenant>} to form the
     * full login name. Resolvable per tenant via {@code Identity.resolveActor}, e.g.
     * {@code "SECONDARY.COM/secondaryUser@tenant1.com"} (resolveActor splits on {@code @}, which follows the key)
     * — giving the ×4 matrix axis (2 tenants × 2 store users). The same store-local names are seeded into BOTH
     * tenants; the shared store DB keeps them distinct by UM_TENANT_ID.
     */
    // The seeded set MIRRORS the primary store's least-privilege archetypes (kept SEPARATE, one role each) so the
    // actor vocabulary is parallel across stores — a test targets either store by swapping the key prefix.
    /** Least-privilege publisher (create/publish, NOT admin) — mirrors primary {@code publisherUser}. */
    public static final String STORE_PUBLISHER_KEY = SECONDARY_DOMAIN + "/publisherUser1";
    private static final String STORE_PUBLISHER_ROLES = "Internal/creator,Internal/publisher";
    /** Least-privilege consumer — mirrors primary {@code subscriberUser}. */
    public static final String STORE_SUBSCRIBER_KEY = SECONDARY_DOMAIN + "/subscriberUser1";
    private static final String STORE_SUBSCRIBER_ROLES = "Internal/subscriber";
    // NO true-admin store user is seeded — proven not achievable (probe): addUser ACCEPTS the primary `admin`
    // role on a secondary-store user (returns 2xx), but the resulting account cannot even authenticate — DCR
    // basic-auth returns 401 (retrying to the startup deadline), so it is unusable as an actor. In the same run
    // the Internal/ hybrid-role users (publisher/subscriber) authenticate fine; only the primary `admin` role
    // poisons the account. Admin-plane coverage therefore stays on the primary-store admins. See
    // docs/devs/secondary-userstore-framework-architecture.md.
    /** Satisfies the store PasswordJavaRegEx ^[\S]{5,30}$ (no spaces). */
    private static final String STORE_USER_PASSWORD = "Secondary@123";

    private SecondaryUserStoreProvisioner() {
    }

    /**
     * Creates the schema once, then for each tenant: registers + activates the {@code SECONDARY.COM} store and
     * seeds the least-privilege publisher + subscriber store users AS ACTORS (so scenarios can act as them), plus
     * a best-effort true-admin probe. One shared H2 DB backs every tenant's store; UM_TENANT_ID isolates users.
     *
     * @param container     the block's running container (for the in-container schema RunScript)
     * @param tenantDomains tenants to register the store into (e.g. {@code carbon.super}, {@code tenant1.com})
     */
    public static void provision(DynamicApimContainer container, String... tenantDomains) throws Exception {
        container.createSecondaryUserStoreH2Schema(DB_RELATIVE_PATH);
        for (String tenant : tenantDomains) {
            TenantUserProvisioner.addSecondaryUserStore(tenant, SECONDARY_DOMAIN, JDBC_URL);
            TenantUserProvisioner.waitUntilStoreActive(tenant, SECONDARY_DOMAIN);
            // Key == store-local username (differ only by the @tenant the auth layer appends), so features can
            // reference the actor by a self-documenting store-qualified key.
            TenantUserProvisioner.addStoreUserAsActor(tenant, STORE_PUBLISHER_KEY, STORE_PUBLISHER_KEY,
                    STORE_USER_PASSWORD, STORE_PUBLISHER_ROLES);
            TenantUserProvisioner.addStoreUserAsActor(tenant, STORE_SUBSCRIBER_KEY, STORE_SUBSCRIBER_KEY,
                    STORE_USER_PASSWORD, STORE_SUBSCRIBER_ROLES);
            logger.info("Secondary user store '" + SECONDARY_DOMAIN + "' active + actors seeded in tenant '"
                    + tenant + "'");
        }
    }
}
