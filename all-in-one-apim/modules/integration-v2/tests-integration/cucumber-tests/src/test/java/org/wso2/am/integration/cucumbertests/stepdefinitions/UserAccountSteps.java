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
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;

import java.io.IOException;

/**
 * Steps that provision and MUTATE a scenario-owned carbon user account in the acting actor's tenant.
 *
 * <p>Why a scenario needs its own user at all: the block seeds a fixed actor set at boot ({@code admin},
 * {@code publisherUser}, {@code subscriberUser} per tenant) and every runner authenticates as those actors
 * concurrently. A scenario that CHANGES a user's credential therefore cannot use one of them — the change would
 * invalidate the tokens of every parallel scenario acting as that actor. So such a scenario provisions a
 * uniquely-named user of its own (CLAUDE.md §4) and registers it for the {@code CREATED_USER_NAMES} teardown sweep
 * (§5), which deletes it as the owning tenant's admin.
 *
 * <p>The user is created and mutated through the {@code RemoteUserStoreManagerService} SOAP admin service — the only
 * interface this pack exposes for an admin-side user create/credential-reset — via
 * {@code TenantUserProvisioner}. That is the narrow SOAP-helper exception of §14: it is called FROM a step (never a
 * listener hook) and authenticates as the acting actor's tenant admin, so the resource is sweepable as the
 * principal that created it.
 */
public class UserAccountSteps {

    /**
     * Provisions a uniquely-named carbon user in the ACTING actor's tenant, granted the given comma-separated
     * Internal roles, and registers it for the teardown sweep. The name is generated (never hardcoded) so
     * concurrent runners cannot collide on it, and the caller reads it back from context rather than assuming it.
     *
     * <p>TWO context keys are stored, because the two forms of the name are not interchangeable:
     * <ul>
     *   <li><b>{@code <usernameKey>}</b> — the STORE name, unqualified. This is what the user-store admin
     *       operations take: they are already scoped to a tenant by the admin credentials they authenticate with,
     *       and passing a {@code @domain}-qualified name to them fails.</li>
     *   <li><b>{@code <usernameKey>LoginName}</b> — the CREDENTIAL name, {@code user@tenant}. This is what
     *       authentication takes (the OAuth2 token endpoint, gateway Basic auth), because that is how the
     *       credential is routed to the right tenant — the same fully-qualified form the {@code Identity} actor
     *       beans store. Pinned live: the unqualified name is accepted for {@code carbon.super} but the token
     *       endpoint answers {@code 400 invalid_grant "Authentication failed"} for a tenant user.</li>
     * </ul>
     *
     * @param namePrefix  prefix for the generated username (e.g. {@code pwdChangeUser})
     * @param password    the user's initial password
     * @param roles       comma-separated Internal roles (e.g. {@code Internal/subscriber})
     * @param usernameKey context key for the store name; the credential name goes to {@code <usernameKey>LoginName}
     */
    @When("I provision a user with name prefix {string} password {string} and roles {string} storing the username as {string}")
    public void iProvisionAScenarioUser(String namePrefix, String password, String roles, String usernameKey)
            throws IOException {

        provisionUser(Names.unique(namePrefix), password, roles, usernameKey);
    }

    /**
     * Provisions a scenario-owned user whose STORE NAME IS ITSELF AN EMAIL ADDRESS — {@code <unique>@<emailDomain>}
     * — rather than a bare name. Same contract and context keys as the step above; the only difference is the shape
     * of the generated name.
     *
     * <p>This is a distinct product surface, not a cosmetic variation. A username containing {@code @} makes the
     * store name and the tenant qualifier ambiguous, and the gateway's Basic authenticator resolves that ambiguity
     * by splitting on the LAST {@code @}: {@code BasicAuthAuthenticator#authenticate} derives the end user via
     * {@code MultitenantUtils.getTenantAwareUsername/getTenantDomain} and then REJECTS the call with
     * {@code 900908 Forbidden} unless that derived tenant equals the API publisher's tenant. So the credential must
     * be the doubly-qualified {@code user@emailDomain@tenantDomain} form, which is exactly what
     * {@code <usernameKey>LoginName} carries here — and an email-form name presented WITHOUT the tenant suffix is a
     * 403, not a 401. Legacy {@code APISecurityTestCase} exercised this with the hardcoded store names
     * {@code apisecUser2@wso2.com} / {@code apisecUser2@abc.com}; the email domain is a step parameter here so the
     * name stays uniquely generated (CLAUDE.md §4) instead of hardcoded.
     *
     * <p>Deliberately NOT an {@code Identity} actor: this provisions a gateway CREDENTIAL, and an actor would
     * additionally need token issuance, publisher-plane resolution and per-actor cleanup. That broader
     * email-username actor fan-out is tracked separately and is out of scope — nothing here builds a
     * partial version of it.
     *
     * @param namePrefix  prefix for the generated local part (e.g. {@code apisecEmailUser})
     * @param emailDomain the domain that follows the {@code @} INSIDE the username (e.g. {@code wso2.com}) — this is
     *                    part of the name, and is unrelated to the tenant the user lives in
     * @param password    the user's initial password
     * @param roles       comma-separated Internal roles (e.g. {@code Internal/subscriber})
     * @param usernameKey context key for the store name; the credential name goes to {@code <usernameKey>LoginName}
     */
    @When("I provision a user with name prefix {string} and email domain {string} password {string} and roles {string} storing the username as {string}")
    public void iProvisionAnEmailFormScenarioUser(String namePrefix, String emailDomain, String password, String roles,
                                                  String usernameKey) throws IOException {

        provisionUser(Names.unique(namePrefix) + Constants.CHAR_AT + emailDomain, password, roles, usernameKey);
    }

    /**
     * Shared body of the two provisioning steps above — creates the user in the acting actor's tenant, registers it
     * for the teardown sweep, and publishes both name forms to context. The steps differ ONLY in how they build
     * {@code username}, so that is the single parameter; keeping one implementation means the cleanup registration
     * and the store-name/credential-name split cannot drift between them.
     */
    private void provisionUser(String username, String password, String roles, String usernameKey) throws IOException {

        String tenantDomain = Identity.actingTenantDomain();
        TenantUserProvisioner.addUserInStore(tenantDomain, username, password, roles);
        ResourceCleanup.register(ResourceCleanup.CREATED_USER_NAMES, username);
        String key = Utils.normalizeContextKey(usernameKey);
        TestContext.set(key, username);
        TestContext.set(key + "LoginName", username + Constants.CHAR_AT + tenantDomain);
    }

    /**
     * Resets a user's password AS THE TENANT ADMIN (the admin-side credential reset, no old password needed). The
     * product behaviour under test afterwards is that this invalidates credentials already issued to that user —
     * both the access tokens minted before the change and the old Basic credential.
     *
     * @param usernameKey context key holding the username (as stored by the provisioning step)
     * @param newPassword the new password
     */
    @When("I change the password of user {string} to {string} as the tenant admin")
    public void iChangeTheUserPassword(String usernameKey, String newPassword) throws IOException {

        TenantUserProvisioner.updateCredentialByAdmin(Identity.actingTenantDomain(),
                TestContext.resolve(usernameKey).toString(), newPassword);
    }
}
