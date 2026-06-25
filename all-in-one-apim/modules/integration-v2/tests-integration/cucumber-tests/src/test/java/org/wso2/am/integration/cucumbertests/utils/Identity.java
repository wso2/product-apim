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

import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;

/**
 * Resolves test actors and their identity-scoped credential cache keys with no mutable "current user"
 * state. Replaces the retired {@code currentTenant}/{@code currentuser} pointer model: instead of one
 * mutable identity that steps mutate and read back, every actor is resolved on demand by reference from
 * the block-provisioned tenant set (published into the shared scope under each tenant's domain key by
 * {@code TenantUserProvisioner}), so access-control variation is a parallel-safe {@code Scenario Outline}
 * over named actors.
 *
 * <p>Credential cache keys are qualified by the actor's <b>full username</b> ({@link User#getUserName()},
 * which carries {@code @domain}) rather than the userKey, because keys such as {@code admin}/{@code userKey1}
 * repeat across tenants — an unqualified key would silently hand one identity another's token (a false pass).
 */
public final class Identity {

    private Identity() {
    }

    /**
     * Resolves an actor by reference. Forms:
     * <ul>
     *   <li>{@code null} / blank / {@code "admin"} — the super-tenant admin (the default actor)</li>
     *   <li>{@code "<userKey>"} — a provisioned user under that key in the super tenant</li>
     *   <li>{@code "admin@<domain>"} — the admin of the given tenant</li>
     *   <li>{@code "<userKey>@<domain>"} — a provisioned user under that key in the given tenant</li>
     * </ul>
     * Resolution is read-only against the shared-scope tenant beans, so it is safe to call concurrently.
     */
    public static User resolveActor(String actorRef) {

        String key = (actorRef == null || actorRef.isBlank()) ? Constants.ADMIN_USER_KEY : actorRef.trim();
        String domain = Constants.SUPER_TENANT_DOMAIN;
        int at = key.indexOf('@');
        if (at >= 0) {
            domain = key.substring(at + 1);
            key = key.substring(0, at);
        }

        Tenant tenant = Utils.getTenantFromContext(domain);
        if (Constants.ADMIN_USER_KEY.equals(key)) {
            return tenant.getTenantAdmin();
        }
        User user = tenant.getTenantUser(key);
        if (user == null) {
            throw new IllegalStateException("No provisioned user with key '" + key + "' in tenant '" + domain
                    + "'. Add it to the block's tenant set or provision it via a _setup_ step.");
        }
        return user;
    }

    /** Context key holding the scenario's acting actor reference (see {@link #setActingActor}). */
    private static final String ACTING_ACTOR_KEY = "actingActorRef";

    /**
     * Records, for the current scenario, which actor subsequent no-arg token lookups resolve to. This is the
     * explicit, scenario-scoped successor to the retired mutable {@code currentuser} pointer: an auth step
     * sets it once (e.g. from a {@code Scenario Outline} actor column), and the no-arg {@link #publisherToken()}
     * / {@link #devportalToken()} / {@link #adminToken()} getters read it — so glue steps need no actor
     * parameter. It lives in the per-scenario {@link TestContext} local scope (scenarios in a runner are
     * sequential), so it never leaks across scenarios. A {@code null}/blank ref clears it back to the default.
     */
    public static void setActingActor(String actorRef) {
        if (actorRef == null || actorRef.isBlank()) {
            TestContext.remove(ACTING_ACTOR_KEY);
        } else {
            TestContext.set(ACTING_ACTOR_KEY, actorRef.trim());
        }
    }

    /**
     * The actor the current scenario is acting as: the one set by {@link #setActingActor}, or the super-tenant
     * admin if none was set. This is the default for all no-arg token lookups.
     */
    public static User defaultActor() {
        Object ref = TestContext.get(ACTING_ACTOR_KEY);
        return resolveActor(ref == null ? null : ref.toString());
    }

    /** The tenant bean owning the given actor (resolved by the actor's domain). */
    public static Tenant tenantOf(User actor) {
        return Utils.getTenantFromContext(actor.getUserDomain());
    }

    /**
     * The tenant admin of the current acting actor's tenant. For operations that require admin credentials
     * regardless of who the scenario is acting as — e.g. querying the gateway-artifact admin endpoint to poll
     * deployment status, which a least-privilege publisher actor cannot authenticate against.
     */
    public static User actingTenantAdmin() {
        return tenantOf(defaultActor()).getTenantAdmin();
    }

    public static String dcrCredentialsKey(User actor) {
        return qualify("dcrCredentials", actor);
    }

    public static String publisherTokenKey(User actor) {
        return qualify("publisherAccessToken", actor);
    }

    public static String devportalTokenKey(User actor) {
        return qualify("devportalAccessToken", actor);
    }

    public static String adminTokenKey(User actor) {
        return qualify("adminAccessToken", actor);
    }

    /** Reads the cached Publisher token for the default actor (super-tenant admin). */
    public static String publisherToken() {
        return publisherToken(defaultActor());
    }

    public static String publisherToken(User actor) {
        return require(publisherTokenKey(actor), "Publisher access token", actor);
    }

    /** Reads the cached DevPortal token for the default actor (super-tenant admin). */
    public static String devportalToken() {
        return devportalToken(defaultActor());
    }

    public static String devportalToken(User actor) {
        return require(devportalTokenKey(actor), "DevPortal access token", actor);
    }

    /** Reads the cached Admin token for the default actor (super-tenant admin). */
    public static String adminToken() {
        return adminToken(defaultActor());
    }

    public static String adminToken(User actor) {
        return require(adminTokenKey(actor), "Admin access token", actor);
    }

    private static String qualify(String base, User actor) {
        return base + "::" + actor.getUserName();
    }

    private static String require(String key, String what, User actor) {
        Object value = TestContext.get(key);
        if (value == null) {
            throw new IllegalStateException(what + " for '" + actor.getUserName()
                    + "' has not been obtained yet (no value under '" + key + "').");
        }
        return value.toString();
    }
}
