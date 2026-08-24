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
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step definitions for the secondary user store (port of SecondaryUserStoreCaseInsensitiveTestCase). The JDBC
 * secondary user store (domain {@code SECONDARY.COM}, case-insensitive usernames) is stood up entirely at RUNTIME by
 * the framework when the block sets {@code initSecondaryUserStore=true} — {@code SecondaryUserStoreProvisioner}
 * creates the usermgt schema from the product's own dbscripts in-container, registers the store via
 * {@code UserStoreConfigAdminService.addUserStore} (SOAP, hot-deploys asynchronously), and polls until the domain is
 * active. There is no seeded {@code .mv.db} and no {@code serverFilesToCopy}. These steps then exercise the
 * case-insensitive-username behaviour: a user added with one case is resolvable (and its roles listable) via a
 * different case of the username. Also used by ChangeApiProviderSecondaryUserStore, which changes an API's provider
 * to a {@code SECONDARY.COM/} user.
 */
public class SecondaryUserStoreSteps {

    /**
     * Adds a user directly in a user store domain (e.g. {@code SECONDARY/testUser1}) with roles, in a tenant. Uses
     * the user-store-manager SOAP service (which resolves the {@code SECONDARY/} domain); retries on the transient
     * "Invalid Domain Name" a freshly-added store can throw while it is still warming up.
     *
     * <p>Store-AGNOSTIC despite the name: an UNQUALIFIED username (no {@code DOMAIN/} prefix) resolves in the
     * tenant's PRIMARY store. That is how {@code gateway/basic_auth_security.feature} seeds the email-style
     * principal it needs (a username whose local part contains an {@code @}) without a block-boot change.
     */
    @When("I provision store user {string} with password {string} and roles {string} in tenant {string}")
    public void iProvisionStoreUser(String userName, String password, String roles, String tenantDomain)
            throws Exception {
        String user = Utils.resolveContextPlaceholders(userName);
        String resolvedRoles = Utils.resolveContextPlaceholders(roles);
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + 60000L;
        while (true) {
            try {
                TenantUserProvisioner.addUserInStore(tenantDomain, user, password, resolvedRoles);
                return;
            } catch (java.io.IOException e) {
                if (System.currentTimeMillis() >= deadline || !String.valueOf(e.getMessage()).contains("Invalid Domain")) {
                    throw e;
                }
                Utils.pollPause(deadlineStart, 3000);
            }
        }
    }

    /**
     * Asserts a store user EXISTS via {@code isExistingUser} — the CORRECT existence check. Never assert existence
     * via a non-empty {@code getRoleListOfUser}: that returns {@code Internal/everyone} for ANY username string
     * (existing or not), so a non-empty role list does not prove the user exists.
     *
     * <p>Store-agnostic: the username may be store-qualified ({@code SECONDARY.COM/testUser1}) or a plain
     * primary-store name, so the user-sign-up approval-workflow feature asserts on its signed-up user with this
     * same step rather than forking a near-duplicate (CLAUDE.md §7).
     */
    @Then("the user {string} in tenant {string} should exist")
    public void theUserShouldExist(String userName, String tenantDomain) throws Exception {
        String body = TenantUserProvisioner.isExistingUser(tenantDomain, Utils.resolveContextPlaceholders(userName));
        Assert.assertTrue(body.contains("<ns:return>true</ns:return>"),
                "isExistingUser for '" + userName + "' did not return true; response: " + body);
    }

    /**
     * Asserts a user does NOT exist in the given tenant — the negative of {@link #theUserShouldExist}.
     * Used to prove UM_TENANT_ID isolation on the shared store DB: a user seeded into one tenant's SECONDARY.COM
     * store is absent from another tenant's SECONDARY.COM store even though both point at the same H2 DB and the
     * same store domain name. Also the user-sign-up workflow's rejection observable: the product DELETES a
     * rejected self-sign-up, so the user must be gone from the primary store.
     */
    @Then("the user {string} in tenant {string} should not exist")
    public void theUserShouldNotExist(String userName, String tenantDomain) throws Exception {
        String body = TenantUserProvisioner.isExistingUser(tenantDomain, Utils.resolveContextPlaceholders(userName));
        Assert.assertTrue(body.contains("<ns:return>false</ns:return>"),
                "isExistingUser for '" + userName + "' in tenant '" + tenantDomain + "' did not return false "
                        + "(expected the user to be absent from this tenant's store); response: " + body);
    }

    /**
     * Asserts that listing the roles of {@code userName} (which may be a DIFFERENT case than the user was added
     * with) returns {@code expectedRole} — proving the secondary store's case-insensitive-username resolution.
     */
    @Then("the roles of store user {string} in tenant {string} should contain {string}")
    public void theRolesShouldContain(String userName, String tenantDomain, String expectedRole) throws Exception {
        String body = TenantUserProvisioner.getRoleListOfUser(tenantDomain,
                Utils.resolveContextPlaceholders(userName));
        String expected = Utils.resolveContextPlaceholders(expectedRole);
        // Exact role VALUE, case-sensitively (see the negative twin's javadoc): a substring check would accept
        // `SECONDARY.COM/userrole10` as proof that `SECONDARY.COM/userrole1` is assigned.
        Assert.assertTrue(assignedRoles(body).contains(expected),
                "Role list of '" + userName + "' did not contain '" + expected + "' (roles are matched "
                        + "case-SENSITIVELY; it is the username that resolves case-insensitively, which is what "
                        + "this step proves); response: " + body);
    }

    /**
     * Asserts that listing the roles of {@code userName} does NOT return {@code unexpectedRole} — the negative of
     * {@link #theRolesShouldContain}, used to prove a DELETED store role has disappeared from the user's role list.
     *
     * <p>Deliberately CASE-INSENSITIVE while the positive assertion is case-SENSITIVE, and that asymmetry is the
     * strict form of each direction: the positive pins the exact casing the store returns, while the negative must
     * prove the role is gone in ANY casing. A case-sensitive absence check would pass vacuously — asserting that
     * {@code SECONDARY.COM/USERROLE1} is absent is trivially true while {@code SECONDARY.COM/userrole1} is still
     * assigned, so it would not notice a delete that silently did nothing.
     */
    @Then("the roles of store user {string} in tenant {string} should not contain {string}")
    public void theRolesShouldNotContain(String userName, String tenantDomain, String unexpectedRole)
            throws Exception {
        String body = TenantUserProvisioner.getRoleListOfUser(tenantDomain,
                Utils.resolveContextPlaceholders(userName));
        String unexpected = Utils.resolveContextPlaceholders(unexpectedRole);
        // Compared per role VALUE, not as a substring of the envelope: `SECONDARY.COM/userrole1` is a substring
        // of `SECONDARY.COM/userrole10`, so a substring check would report a still-assigned role that is not.
        boolean stillAssigned = assignedRoles(body).stream().anyMatch(role -> role.equalsIgnoreCase(unexpected));
        Assert.assertFalse(stillAssigned,
                "Role list of '" + userName + "' still contained '" + unexpected + "' in some casing; "
                        + "response: " + body);
    }

    /** The role values a {@code getRoleListOfUser} response assigns — one per {@code <ns:return>} element. */
    private static List<String> assignedRoles(String soapBody) {

        List<String> roles = new ArrayList<>();
        Matcher matcher = ROLE_RETURN_PATTERN.matcher(soapBody == null ? "" : soapBody);
        while (matcher.find()) {
            roles.add(matcher.group(1).trim());
        }
        return roles;
    }

    private static final Pattern ROLE_RETURN_PATTERN =
            Pattern.compile("<\\w+:return\\b[^>]*>(.*?)</\\w+:return>", Pattern.DOTALL);

    /**
     * Deletes a store ROLE only, leaving the users that carried it in place — so a scenario can assert what the
     * role deletion did to those users' role lists. The user+role teardown variant below cannot express that
     * (it removes the user first, so there is nothing left to query).
     */
    @When("I remove the secondary user store role {string} in tenant {string}")
    public void iRemoveSecondaryUserStoreRole(String roleName, String tenantDomain) throws Exception {
        TenantUserProvisioner.deleteRole(tenantDomain, Utils.resolveContextPlaceholders(roleName));
    }

    /**
     * Best-effort teardown: delete the store user and role in the tenant. The store itself is registered at block
     * boot by the framework and lives for the container's lifetime — the container is discarded after the block, so
     * there is nothing to undeploy.
     */
    @When("I remove the secondary user store user {string} and role {string} in tenant {string}")
    public void iRemoveSecondaryUserStoreArtifacts(String userName, String roleName, String tenantDomain)
            throws Exception {
        TenantUserProvisioner.deleteUser(tenantDomain, Utils.resolveContextPlaceholders(userName));
        TenantUserProvisioner.deleteRole(tenantDomain, Utils.resolveContextPlaceholders(roleName));
    }

    /**
     * Best-effort teardown of just a store user (no store role) — for scenarios that seed a user carrying only
     * global {@code Internal/} hybrid roles (which are shared and must NOT be deleted), so there is no
     * store-specific role to remove.
     */
    @When("I remove the secondary user store user {string} in tenant {string}")
    public void iRemoveSecondaryUserStoreUser(String userName, String tenantDomain) throws Exception {
        TenantUserProvisioner.deleteUser(tenantDomain, Utils.resolveContextPlaceholders(userName));
    }

}
