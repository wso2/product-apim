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

package org.wso2.am.integration.cucumbertests.verification.steps;

import io.cucumber.java.en.Then;
import org.jaxen.JaxenException;
import org.testcontainers.containers.Container;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Framework-state probe steps for the parallel-on-shared-container lane (Phase 4.3). These assert only
 * that {@code BlockLifecycleListener} published the block's shared scope - the readiness step is reused
 * from {@code BaseSteps} ("I wait for the APIM server to be ready"). No server-flow logic lives here, so
 * the probe runners stay tiny and the 4.4-4.12 gates can reason purely about lifecycle/concurrency.
 *
 * <p>The recording step (Phase 4.4+) appends one observation line per probe execution to a fixed file
 * under the module's {@code target/} so the shell gates can assert, after the run, properties no single
 * JVM could prove alone: that all probe classes in a block saw the <b>same</b> container id and shared
 * URLs (boot-once), and - via timestamps - how block/class executions overlapped (concurrency).
 */
public class BlockProbeSteps {

    /** Reused across 4.4-4.12; each gate's script truncates it before the run and parses it after. */
    static final String OBSERVATIONS_FILE = "fv-block-observations.txt";

    private static final Object OBSERVATION_LOCK = new Object();

    @Then("the shared baseUrl is present")
    public void theSharedBaseUrlIsPresent() {
        assertPublished("baseUrl");
    }

    /** 4.8: deliberate assertion failure (after the container booted) to prove onFinish still releases. */
    @Then("I deliberately fail the probe")
    public void iDeliberatelyFailTheProbe() {
        Assert.fail("intentional probe failure (Phase 4.8): onFinish must still stop the block container");
    }

    /** 4.8: deliberate error (uncaught exception) to prove onFinish releases on ERROR as well as FAILURE. */
    @Then("I deliberately error the probe")
    public void iDeliberatelyErrorTheProbe() {
        throw new IllegalStateException(
                "intentional probe error (Phase 4.8): onFinish must still stop the block container");
    }

    /** 4.10: stop the block container from inside the probe so onFinish's stop() becomes a double-stop. */
    @Then("I stop the block container")
    public void iStopTheBlockContainer() {
        Object container = TestContext.get("blockApimContainer");
        Assert.assertTrue(container instanceof DynamicApimContainer,
                "no block container in scope to stop");
        ((DynamicApimContainer) container).stop();
    }

    @Then("the shared gateway URL is present")
    public void theSharedGatewayUrlIsPresent() {
        assertPublished("baseGatewayUrl");
    }

    /**
     * 4.14 (overlay): assert the running container's deployment.toml reflects the supplied overlay. We
     * cat the file inside the live container - not the local source - so this proves the listener actually
     * shipped the overlay into the booted server, not merely that the overlay file exists on disk.
     */
    @Then("the in-container deployment.toml contains the marker {string}")
    public void theInContainerTomlContainsMarker(String marker) throws Exception {
        Assert.assertTrue(readInContainerToml().contains(marker),
                "expected the running container's deployment.toml to contain the overlay marker: " + marker);
    }

    /**
     * 4.14 (defaults): with no overlay param the block must fall back to the base toml - which carries no
     * marker - and still boot ready (asserted by the readiness step). This proves the no-param path
     * defaults safely rather than NPE-ing on an absent overlay.
     */
    @Then("the in-container deployment.toml does not contain the marker {string}")
    public void theInContainerTomlDoesNotContainMarker(String marker) throws Exception {
        Assert.assertFalse(readInContainerToml().contains(marker),
                "the default (base) toml unexpectedly contained the overlay marker: " + marker);
    }

    private String readInContainerToml() throws Exception {
        Object container = TestContext.get("blockApimContainer");
        Assert.assertTrue(container instanceof DynamicApimContainer,
                "no block container in scope to read the deployment.toml from");
        DynamicApimContainer apim = (DynamicApimContainer) container;
        Container.ExecResult result = apim.execInContainer("cat", apim.getContainerTomlPath());
        Assert.assertEquals(result.getExitCode(), 0,
                "cat of in-container deployment.toml failed: " + result.getStderr());
        return result.getStdout();
    }

    @Then("I record the block observation")
    public void iRecordTheBlockObservation() throws IOException {
        Object container = TestContext.get("blockApimContainer");
        String containerId = container instanceof DynamicApimContainer dynamic
                ? String.valueOf(dynamic.getContainerId()) : "none";
        String line = String.join("|",
                Long.toString(System.currentTimeMillis()),
                Thread.currentThread().getName(),
                containerId,
                String.valueOf(TestContext.get("baseUrl")),
                String.valueOf(TestContext.get("baseGatewayUrl"))) + System.lineSeparator();

        String moduleDir = ModulePathResolver.getModuleDir(BlockProbeSteps.class);
        Path file = Paths.get(moduleDir, "target", OBSERVATIONS_FILE);
        synchronized (OBSERVATION_LOCK) {
            Files.write(file, line.getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    /**
     * 5.3: the tenant bean the lifecycle provisioner created is readable from the block's shared scope
     * under the tenant-domain key, and carries an admin user. Proves provisioning's {@code setShared}
     * writes landed on the composite block key the probe classes read.
     */
    @Then("the shared tenant {string} has an admin user")
    public void theSharedTenantHasAnAdminUser(String tenantDomain) {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        Assert.assertNotNull(tenant.getTenantAdmin(),
                "tenant '" + tenantDomain + "' has no admin user in shared scope");
        Assert.assertNotNull(tenant.getTenantAdmin().getUserName(),
                "tenant '" + tenantDomain + "' admin has no username in shared scope");
    }

    /** 5.3: a provisioned tenant user (by key) is attached to the tenant bean in shared scope. */
    @Then("the shared tenant {string} has user with key {string}")
    public void theSharedTenantHasUserWithKey(String tenantDomain, String userKey) {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User user = tenant.getTenantUser(userKey);
        Assert.assertNotNull(user,
                "tenant '" + tenantDomain + "' has no user with key '" + userKey + "' in shared scope");
    }

    /**
     * 5.3: the probe resolves {@code CURRENT_TENANT} exactly as the publisher runners do - pick the admin
     * or a keyed user off the shared tenant bean, set it as the context user, publish CURRENT_TENANT, and
     * read it back. Proves the provisioned beans are usable by downstream server-flow tests.
     */
    @Then("I can resolve CURRENT_TENANT for tenant {string} and user key {string}")
    public void iCanResolveCurrentTenant(String tenantDomain, String userKey) {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User user = Constants.ADMIN_USER_KEY.equals(userKey)
                ? tenant.getTenantAdmin()
                : tenant.getTenantUser(userKey);
        Assert.assertNotNull(user, "could not resolve user key '" + userKey
                + "' on tenant '" + tenantDomain + "'");
        tenant.setContextUser(user);
        TestContext.set(Constants.CURRENT_TENANT, tenant);
        try {
            Tenant current = (Tenant) TestContext.get(Constants.CURRENT_TENANT);
            Assert.assertNotNull(current, "CURRENT_TENANT was not readable after being set");
            Assert.assertEquals(current.getDomain(), tenantDomain, "CURRENT_TENANT resolved to wrong domain");
            Assert.assertNotNull(current.getContextUser(), "CURRENT_TENANT has no context user");
        } finally {
            TestContext.remove(Constants.CURRENT_TENANT);
        }
    }

    /**
     * 5.3: the freshly booted container actually has the tenant - asserted against the live server, by
     * parsing the response of the legacy {@code "I retrieve existing tenant details"} step (same SOAP).
     */
    @Then("the retrieved tenants include {string}")
    public void theRetrievedTenantsInclude(String tenantDomain) throws Exception {
        HttpResponse response = (HttpResponse) TestContext.get("existingTenantsResponse");
        Assert.assertNotNull(response,
                "no tenant-retrieval response in context - run 'I retrieve existing tenant details' first");
        List<String> domains = Utils.getNodeTextsByXPath(response.getData(),
                "//*[local-name()='tenantDomain']");
        Assert.assertTrue(domains.contains(tenantDomain),
                "freshly booted container is missing provisioned tenant '" + tenantDomain + "': " + domains);
    }

    /**
     * 5.3: the freshly booted container actually has the user - asserted against the live server, by
     * parsing the response of the legacy {@code "I retrieve all existing users ..."} step (same SOAP).
     */
    @Then("the retrieved users include {string}")
    public void theRetrievedUsersInclude(String username) throws Exception {
        HttpResponse response = (HttpResponse) TestContext.get("existingTenantUsersResponse");
        Assert.assertNotNull(response,
                "no user-retrieval response in context - run 'I retrieve all existing users ...' first");
        List<String> users = Utils.getNodeTextsByXPath(response.getData(),
                "//*[local-name()='listUsersResponse']/*[local-name()='return']");
        Assert.assertTrue(users.contains(username),
                "freshly booted container is missing provisioned user '" + username + "': " + users);
    }

    /**
     * 5.7: re-run the DEFAULT tenant set provisioning against the block's OWN container. The lifecycle
     * already provisioned it once (initTenantUsers=true), so every create here must hit the provisioner's
     * "skip if exists" branch and no-op. If idempotency were broken, addTenant/addUser would attempt a
     * re-create and the server would answer non-200, making this step throw - so a clean return IS the
     * idempotency proof. Reads baseUrl/tenant beans from shared scope exactly as onStart's provisioning did.
     */
    @Then("I provision the default tenant set again")
    public void iProvisionTheDefaultTenantSetAgain() throws IOException, JaxenException {
        String roles = "Internal/creator, Internal/publisher, Internal/subscriber";
        TenantUserProvisioner.addSuperTenant();
        TenantUserProvisioner.addTenant("tenant1.com", "admin", "admin", "First", "Tenant",
                "admin@tenant1.com");
        TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, "userKey1",
                "testUser1", "testUser1", roles);
        TenantUserProvisioner.addUser("tenant1.com", "userKey1", "testUser11", "testUser11", roles);
    }

    /**
     * 5.7: the re-provisioning created no duplicate - the user appears exactly once in the live server's
     * user list. Parses the same {@code listUsers} response the legacy retrieve step stores. A broken
     * skip-if-exists that double-created would surface here as a count > 1.
     */
    @Then("the retrieved users include {string} exactly once")
    public void theRetrievedUsersIncludeExactlyOnce(String username) throws Exception {
        HttpResponse response = (HttpResponse) TestContext.get("existingTenantUsersResponse");
        Assert.assertNotNull(response,
                "no user-retrieval response in context - run 'I retrieve all existing users ...' first");
        List<String> users = Utils.getNodeTextsByXPath(response.getData(),
                "//*[local-name()='listUsersResponse']/*[local-name()='return']");
        long count = users.stream().filter(username::equals).count();
        Assert.assertEquals(count, 1L,
                "expected user '" + username + "' to exist exactly once after re-provisioning, saw " + count
                        + ": " + users);
    }

    private void assertPublished(String key) {
        Object value = TestContext.get(key);
        Assert.assertNotNull(value, "shared '" + key + "' was not published into the block scope");
        Assert.assertFalse(value.toString().isBlank(),
                "shared '" + key + "' is present but blank");
    }
}
