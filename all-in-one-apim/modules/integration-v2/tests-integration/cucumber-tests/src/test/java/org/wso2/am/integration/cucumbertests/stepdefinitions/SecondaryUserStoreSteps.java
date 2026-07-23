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

    /** Adds a role directly in a user store domain (e.g. {@code SECONDARY/userrole1}) in the given tenant. */
    @When("I provision store role {string} in tenant {string}")
    public void iProvisionStoreRole(String roleName, String tenantDomain) throws Exception {
        TenantUserProvisioner.addRole(tenantDomain, Utils.resolveContextPlaceholders(roleName));
    }

    /**
     * Adds a user directly in a user store domain (e.g. {@code SECONDARY/testUser1}) with roles, in a tenant. Uses
     * the user-store-manager SOAP service (which resolves the {@code SECONDARY/} domain); retries on the transient
     * "Invalid Domain Name" a freshly-added store can throw while it is still warming up.
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
     */
    @Then("the store user {string} in tenant {string} should exist")
    public void theStoreUserShouldExist(String userName, String tenantDomain) throws Exception {
        String body = TenantUserProvisioner.isExistingUser(tenantDomain, Utils.resolveContextPlaceholders(userName));
        Assert.assertTrue(body.contains("<ns:return>true</ns:return>"),
                "isExistingUser for '" + userName + "' did not return true; response: " + body);
    }

    /**
     * Asserts a store user does NOT exist in the given tenant — the negative of {@link #theStoreUserShouldExist}.
     * Used to prove UM_TENANT_ID isolation on the shared store DB: a user seeded into one tenant's SECONDARY.COM
     * store is absent from another tenant's SECONDARY.COM store even though both point at the same H2 DB and the
     * same store domain name.
     */
    @Then("the store user {string} in tenant {string} should not exist")
    public void theStoreUserShouldNotExist(String userName, String tenantDomain) throws Exception {
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
        Assert.assertTrue(body.contains(expected),
                "Role list of '" + userName + "' did not contain '" + expected + "' (case-insensitive lookup); "
                        + "response: " + body);
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
