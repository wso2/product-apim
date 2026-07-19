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
import org.jaxen.JaxenException;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.List;

/**
 * Provisioning helper extracted from {@code TenantUserInitialisationSteps} so the same SOAP-build +
 * "skip if exists" logic can be driven from the parallel-lane {@code BlockLifecycleListener} (in
 * {@code onStart}, outside any Cucumber scenario) as well as from the legacy steps. The bodies are copied
 * verbatim; the only changes are dropping the Cucumber annotations / {@code DataTable} in favour of plain
 * method parameters and folding each "retrieve existing" step into the create method that consumes it.
 *
 * <p>Like the steps, it reads {@code baseUrl} from and writes tenant/user beans to the {@link TestContext}
 * shared scope under the tenant domain key, so existing readers (and the legacy steps) are unaffected.
 * The caller is responsible for having set the scope and published {@code baseUrl} first.
 */
public final class TenantUserProvisioner {

    private static final Log logger = LogFactory.getLog(TenantUserProvisioner.class);

    private TenantUserProvisioner() {
    }

    private static String getBaseUrl() {

        Object baseUrlObj = TestContext.get("baseUrl");
        if (baseUrlObj == null) {
            throw new IllegalStateException("baseUrl is not available in the test context yet");
        }
        return baseUrlObj.toString();
    }

    /**
     * Adds the super tenant (carbon.super) to the shared test context. Copied from the
     * {@code "I add super tenant to context"} step.
     */
    public static void addSuperTenant() {

        Tenant superTenant = new Tenant();
        superTenant.setDomain(Constants.SUPER_TENANT_DOMAIN);
        // Super tenant admin
        User admin = new User();
        admin.setKey(Constants.ADMIN_USER_KEY);
        admin.setUserName(Constants.SUPER_TENANT_ADMIN_USERNAME);
        admin.setPassword(Constants.SUPER_TENANT_ADMIN_PASSWORD);
        superTenant.setTenantAdmin(admin);
        TestContext.setShared(Constants.SUPER_TENANT_DOMAIN, superTenant);
    }

    /**
     * Adds the adpsample tenant (a pre-migrated tenant that already exists in the server's migration
     * dataset) to shared scope. Copied from the {@code "I add adpsample tenant to context"} step - it
     * only builds the bean, with no SOAP creation, because the tenant ships with the migration DB.
     */
    public static void addAdpsampleTenant() {

        Tenant adpTenant = new Tenant();
        adpTenant.setDomain(Constants.ADPSAMPLE_TENANT_DOMAIN);
        //  tenant admin
        User admin = new User();
        admin.setKey(Constants.ADPSAMPLE_USER_KEY);
        admin.setUserName(Constants.ADPSAMPLE_TENANT_ADMIN_USERNAME);
        admin.setPassword(Constants.ADPSAMPLE_TENANT_ADMIN_PASSWORD);
        adpTenant.setTenantAdmin(admin);
        TestContext.setShared(Constants.ADPSAMPLE_TENANT_DOMAIN, adpTenant);
    }

    /**
     * Creates a tenant via the Tenant Management Admin Service, skipping creation if the domain already
     * exists, then publishes the tenant bean to shared scope. Copied from the
     * {@code "I add a new tenant with the following details"} step (with the existing-tenants retrieval,
     * formerly a separate step, folded in).
     */
    public static void addTenant(String tenantDomain, String adminUsername, String adminPassword,
                                 String firstName, String lastName, String email)
            throws IOException, JaxenException {

        List<String> existingTenantDomains = retrieveExistingTenantDomains();

        if (existingTenantDomains.contains(tenantDomain)) {
            logger.info("Tenant with domain " + tenantDomain + " already exists");
        } else {
            String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\" " +
                    "xmlns:xsd=\"http://beans.common.stratos.carbon.wso2.org/xsd\">" +
                    "<soapenv:Header/><soapenv:Body>" +
                    "<ser:addTenant>" +
                    "<ser:tenantInfoBean>" +
                    "<xsd:active>true</xsd:active>" +
                    "<xsd:admin>" + adminUsername + "</xsd:admin>" +
                    "<xsd:adminPassword>" + adminPassword + "</xsd:adminPassword>" +
                    "<xsd:email>" + email + "</xsd:email>" +
                    "<xsd:firstname>" + firstName + "</xsd:firstname>" +
                    "<xsd:lastname>" + lastName + "</xsd:lastname>" +
                    "<xsd:tenantDomain>" + tenantDomain + "</xsd:tenantDomain>" +
                    "</ser:tenantInfoBean>" +
                    "</ser:addTenant>" +
                    "</soapenv:Body></soapenv:Envelope>";

            String url = Utils.getTenantMgtAdminServiceURL(getBaseUrl());
            HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                    url, payload, "urn:addTenant",
                    Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
            Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        }
        User admin = new User();
        admin.setKey(Constants.ADMIN_USER_KEY);
        admin.setUserName(adminUsername + Constants.CHAR_AT + tenantDomain);
        admin.setPassword(adminPassword);
        Tenant tenant = new Tenant();
        tenant.setDomain(tenantDomain);
        tenant.setTenantAdmin(admin);
        TestContext.setShared(tenantDomain, tenant);
    }

    /**
     * Creates a user in the given tenant via the user-admin service, skipping creation if the username
     * already exists, then attaches the user to the tenant bean in shared scope. Copied from the
     * {@code "I add user ... to the tenant domain ..."} step (with the existing-users retrieval, formerly a
     * separate step, folded in).
     */
    public static void addUser(String tenantDomain, String userKey, String username, String password,
                               String roles) throws IOException, JaxenException {

        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        List<String> existingTenantUsers = retrieveExistingUsers(tenant);

        if (existingTenantUsers.contains(username)) {
            logger.info("User with username " + username + " already exists in the tenant");
        } else {
            String[] rolesList = roles.split("\\s*,\\s*");
            StringBuilder rolesXml = new StringBuilder();
            for (String role : rolesList) {
                rolesXml.append("<xsd:roles>").append(role).append("</xsd:roles>");
            }

            String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                    "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                    "<soapenv:Header/><soapenv:Body>" +
                    "<xsd:addUser>" +
                    "<xsd:userName>" + username + "</xsd:userName>" +
                    "<xsd:password>" + password + "</xsd:password>" +
                    rolesXml +
                    "</xsd:addUser>" +
                    "</soapenv:Body></soapenv:Envelope>";

            String url = Utils.getMultipleCredentialsUserAdminServiceURL(getBaseUrl());
            HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:addUser",
                    tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
            Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        }
        User tenantUser = new User();
        tenantUser.setUserName(username + Constants.CHAR_AT + tenantDomain);
        tenantUser.setPassword(password);
        tenantUser.setKey(userKey);
        tenant.addTenantUsers(tenantUser);
        TestContext.setShared(tenantDomain, tenant);
    }

    /**
     * Adds an (empty) internal role to the given tenant's user store via the RemoteUserStoreManagerService SOAP
     * {@code addRole} operation (authenticated as the tenant admin), skipping creation if the role already
     * exists. Enabler for access-control tests: an API restricted to a role can only be authored/exported by a
     * user carrying that role. Idempotent: a duplicate {@code addRole} is a 2xx no-op (not a fault); any non-2xx
     * (auth/service error) fails fast.
     *
     * @param tenantDomain the tenant to create the role in
     * @param roleName     the (unqualified) role name (e.g. {@code apiAccessRole})
     */
    public static void addRole(String tenantDomain, String roleName) throws IOException {
        addRole(tenantDomain, roleName, false);
    }

    /**
     * As {@link #addRole(String, String)} but optionally grants the role the store-login + subscribe permissions.
     * A role used as an API's {@code visibleRoles} (DevPortal store visibility RESTRICTED) MUST carry the
     * {@code /permission/admin/login} permission, or the publisher API-create rejects it with 900610 "Invalid user
     * roles found" — verified live: an empty role works for {@code accessControlRoles} but NOT for
     * {@code visibleRoles}. This mirrors the legacy visibility tests (APITagVisibilityByRole /
     * APIVisibilityWithDirectURL / APIMANAGER4373), which all create the visibility role with exactly these two
     * permissions. Access-control-only roles can stay permissionless.
     *
     * @param withStorePermissions grant {@code /permission/admin/login} + {@code .../manage/api/subscribe}
     */
    public static void addRole(String tenantDomain, String roleName, boolean withStorePermissions)
            throws IOException {

        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String permissionsXml = "";
        if (withStorePermissions) {
            permissionsXml =
                    "<ser:permissions><ser:action>ui.execute</ser:action>"
                            + "<ser:resourceId>/permission/admin/login</ser:resourceId></ser:permissions>"
                            + "<ser:permissions><ser:action>ui.execute</ser:action>"
                            + "<ser:resourceId>/permission/admin/manage/api/subscribe</ser:resourceId></ser:permissions>";
        }
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<ser:addRole>" +
                "<ser:roleName>" + roleName + "</ser:roleName>" +
                permissionsXml +
                "</ser:addRole>" +
                "</soapenv:Body></soapenv:Envelope>";

        String url = Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:addRole",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        // The RemoteUserStoreManagerService addRole returns 2xx (202 Accepted) for BOTH a fresh role and a
        // duplicate — a duplicate is a no-op, not a SOAP fault (verified: create and duplicate both return 202
        // with an empty body). So any 2xx is success/idempotent; a non-2xx (auth failure, service down, or a
        // genuine UserStoreException) is a real provisioning error and must fail fast rather than be silently
        // swallowed (which would surface later as a confusing "role not found").
        int code = response.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("addRole for '" + roleName + "' in tenant '" + tenantDomain + "' failed with "
                    + code + ": " + response.getData());
        }
    }

    /**
     * Returns the roles of a user via RemoteUserStoreManagerService {@code getRoleListOfUser} (the raw response
     * body). Used to verify case-insensitive-username resolution on the secondary store (an UPPERCASE username
     * resolves the same user). Uses the given tenant's admin credentials.
     */
    public static String getRoleListOfUser(String tenantDomain, String userName) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:getRoleListOfUser><ser:userName>" + userName + "</ser:userName></ser:getRoleListOfUser>"
                + "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:getRoleListOfUser",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        return response.getData();
    }

    /**
     * Returns whether a user EXISTS via RemoteUserStoreManagerService {@code isExistingUser} (raw response body,
     * contains {@code <ns:return>true|false</ns:return>}). This is the correct existence check for the secondary
     * store: {@code getRoleListOfUser} returns {@code Internal/everyone} for ANY username string — existing or NOT
     * — so a non-empty role list does NOT prove the user exists. Uses the given tenant's admin credentials.
     */
    public static String isExistingUser(String tenantDomain, String userName) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:isExistingUser><ser:userName>" + userName + "</ser:userName></ser:isExistingUser>"
                + "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:isExistingUser",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        return response.getData();
    }

    /**
     * Adds a user via RemoteUserStoreManagerService {@code addUser} (the user-store-manager service, which resolves
     * a {@code SECONDARY/} domain prefix), rather than the multiple-credentials UserAdmin service used by
     * {@link #addUser} (which rejects a secondary domain with "Invalid Domain Name"). Pinned live: this is the SOAP
     * shape that works for a runtime-added secondary store. Roles are comma-separated.
     */
    public static void addUserInStore(String tenantDomain, String userName, String password, String roles)
            throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        StringBuilder rolesXml = new StringBuilder();
        for (String role : roles.split("\\s*,\\s*")) {
            rolesXml.append("<ser:roleList>").append(role).append("</ser:roleList>");
        }
        // RemoteUserStoreManagerService operations live in the http://service.ws.um.carbon.wso2.org namespace —
        // using the axis2 xsd namespace here fails with "namespace mismatch require .../service.ws.um... found
        // .../org.apache.axis2/xsd". The addUser operation is positional: userName, credential, roleList*, claims*,
        // profileName, requirePasswordChange.
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\"><soapenv:Header/><soapenv:Body>"
                + "<ser:addUser><ser:userName>" + userName + "</ser:userName>"
                + "<ser:credential>" + password + "</ser:credential>"
                + rolesXml
                + "<ser:profileName>default</ser:profileName>"
                + "<ser:requirePasswordChange>false</ser:requirePasswordChange>"
                + "</ser:addUser></soapenv:Body></soapenv:Envelope>";
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl()), payload, "urn:addUser",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        int code = response.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("addUserInStore for '" + userName + "' in tenant '" + tenantDomain
                    + "' failed with " + code + ": " + response.getData());
        }
    }

    /**
     * Seeds a user into a secondary user store (via {@link #addUserInStore}) AND registers it as a resolvable
     * ACTOR in the tenant bean, so a {@code Scenario Outline} can act as e.g. {@code "secondaryUser@tenant1.com"}.
     * The store-local name (e.g. {@code SECONDARY.COM/secondaryUser1}) routes the credential to the secondary
     * store; the actor's full username appends {@code @<tenant>} so token minting (DCR owner + password grant)
     * routes it to the right tenant — the same qualification the primary-store actors use. Because the shared
     * store DB isolates by {@code UM_TENANT_ID}, the same store-local name in two tenants is two distinct users.
     *
     * @param tenantDomain          tenant whose secondary store the user is seeded into (and whose actor set it joins)
     * @param userKey               the actor key used to resolve it (e.g. {@code secondaryUser})
     * @param storeQualifiedName    the store-domain-qualified username (e.g. {@code SECONDARY.COM/secondaryUser1})
     * @param password              login password (must satisfy the store's PasswordJavaRegEx — no spaces)
     * @param roles                 comma-separated Internal roles granting the planes the actor needs
     */
    public static void addStoreUserAsActor(String tenantDomain, String userKey, String storeQualifiedName,
                                           String password, String roles) throws IOException {
        addUserInStore(tenantDomain, storeQualifiedName, password, roles);
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User actor = new User();
        actor.setUserName(storeQualifiedName + Constants.CHAR_AT + tenantDomain);
        actor.setPassword(password);
        actor.setKey(userKey);
        tenant.addTenantUsers(actor);
        TestContext.setShared(tenantDomain, tenant);
    }

    private static String storeProp(String name, String value) {
        return "<xsd:properties><xsd:name>" + name + "</xsd:name><xsd:value>" + value + "</xsd:value></xsd:properties>";
    }

    /**
     * Registers a JDBC secondary user store at RUNTIME via UserStoreConfigAdminService.addUserStore (hot-deploys
     * asynchronously — poll {@link #waitUntilStoreActive} after) for the given tenant. The addUserStore operation
     * element lives in the {@code http://org.apache.axis2/xsd} namespace while the DTO fields are in the store-config
     * DTO namespace; the domain MUST be dotted ({@code SECONDARY.COM}) — a bare name is rejected for a runtime-added
     * store. The schema (UM_* tables) must already exist in {@code jdbcUrl} (created via
     * {@code DynamicApimContainer#createSecondaryUserStoreH2Schema} before this call).
     */
    public static void addSecondaryUserStore(String tenantDomain, String domain, String jdbcUrl) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String props = storeProp("url", jdbcUrl)
                + storeProp("userName", "wso2carbon")
                + storeProp("password", "wso2carbon")
                + storeProp("driverName", "org.h2.Driver")
                + storeProp("Disabled", "false")
                + storeProp("ReadOnly", "false")
                + storeProp("ReadGroups", "true")
                + storeProp("WriteGroups", "true")
                + storeProp("CaseInsensitiveUsername", "true")
                + storeProp("UsernameJavaRegEx", "^[\\S]{2,30}$")
                + storeProp("PasswordJavaRegEx", "^[\\S]{5,30}$")
                + storeProp("RolenameJavaRegEx", "^[\\S]{2,30}$")
                + storeProp("SCIMEnabled", "false")
                + storeProp("PasswordDigest", "SHA-256")
                + storeProp("StoreSaltedPassword", "true")
                + storeProp("MultiAttributeSeparator", ",");
        // The operation element (addUserStore/userStoreDTO) is in the generic axis2 xsd namespace (the service
        // reported: "namespace mismatch require http://org.apache.axis2/xsd"); the DTO FIELDS keep the dto xsd ns.
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://org.apache.axis2/xsd\" "
                + "xmlns:xsd=\"http://dto.configuration.store.user.identity.carbon.wso2.org/xsd\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:addUserStore><ser:userStoreDTO>"
                + "<xsd:className>org.wso2.carbon.user.core.jdbc.UniqueIDJDBCUserStoreManager</xsd:className>"
                + "<xsd:description>Framework secondary user store</xsd:description>"
                + "<xsd:disabled>false</xsd:disabled>"
                + "<xsd:domainId>" + domain + "</xsd:domainId>"
                + props
                // FileBased DAO: addUserStore writes the userstore XML to deployment/server/userstores/, which
                // the live UserStoreConfigurationDeployer HOT-DEPLOYS at runtime (async — poll for active before
                // use). This is the management-console default path.
                + "<xsd:repositoryClass>org.wso2.carbon.identity.user.store.configuration.dao.impl."
                + "FileBasedUserStoreDAOFactory</xsd:repositoryClass>"
                + "</ser:userStoreDTO></ser:addUserStore></soapenv:Body></soapenv:Envelope>";
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                Utils.getUserStoreConfigAdminServiceURL(getBaseUrl()), payload, "urn:addUserStore",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        int code = response.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("addUserStore for domain '" + domain + "' in tenant '" + tenantDomain
                    + "' failed with " + code + ": " + response.getData());
        }
    }

    /**
     * Polls until a runtime-added secondary user store's domain is ACTIVE. addUserStore deploys the store
     * ASYNCHRONOUSLY (the WSO2 docs: "the success message does not imply the user store is added successfully —
     * refresh after a few seconds"), so callers must wait before using the domain. Probes {@code isExistingUser}
     * on a non-existent name in the domain: while the store is not yet deployed the service faults with "Invalid
     * Domain Name"; once active it returns a normal {@code false}. Never sleep-without-polling; retries only the
     * transient not-yet-deployed state.
     */
    public static void waitUntilStoreActive(String tenantDomain, String domain)
            throws IOException, InterruptedException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String url = Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl());
        // Probe isExistingUser on a regex-valid name in the domain — but check the RAW response code directly
        // (do NOT call isExistingUser(), which asserts 200 and throws on the not-yet-deployed 500). While the
        // store is still deploying the service faults 500 "Invalid Domain Name"; once active it returns 200.
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\"><soapenv:Header/><soapenv:Body>"
                + "<ser:isExistingUser><ser:userName>" + domain + "/storeprobe</ser:userName>"
                + "</ser:isExistingUser></soapenv:Body></soapenv:Envelope>";
        // ~10s observed propagation; 60s is a ~6x margin (covers a starved container under full-suite load).
        long deadline = System.currentTimeMillis() + 60000L;
        HttpResponse resp = null;
        while (true) {
            resp = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:isExistingUser",
                    tenantAdmin.getUserName(), tenantAdmin.getPassword());
            if (resp.getResponseCode() == 200) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IOException("Secondary user store '" + domain + "' in tenant '" + tenantDomain
                        + "' did not become active within 60s; last: " + resp.getResponseCode() + " / "
                        + resp.getData());
            }
            Thread.sleep(3000);
        }
    }

    /** Deletes a role (RemoteUserStoreManagerService {@code deleteRole}). Best-effort cleanup. */
    public static void deleteRole(String tenantDomain, String roleName) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:deleteRole><ser:roleName>" + roleName + "</ser:roleName></ser:deleteRole>"
                + "</soapenv:Body></soapenv:Envelope>";
        SimpleHTTPClient.getInstance().sendSoapRequest(Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl()),
                payload, "urn:deleteRole", tenantAdmin.getUserName(), tenantAdmin.getPassword());
    }

    /** Deletes a user (RemoteUserStoreManagerService {@code deleteUser}). Best-effort cleanup. */
    public static void deleteUser(String tenantDomain, String userName) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        User tenantAdmin = tenant.getTenantAdmin();
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:deleteUser><ser:userName>" + userName + "</ser:userName></ser:deleteUser>"
                + "</soapenv:Body></soapenv:Envelope>";
        SimpleHTTPClient.getInstance().sendSoapRequest(Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl()),
                payload, "urn:deleteUser", tenantAdmin.getUserName(), tenantAdmin.getPassword());
    }

    /**
     * Polls the Tenant Management Admin Service until it answers 200, closing the race between gateway
     * readiness ({@link ServerReadiness#awaitReady}) and admin-service deployment: the gateway health-check
     * can pass seconds before the SOAP admin services finish deploying, so firing provisioning immediately
     * can hit a transient 404. Parallel block boots (shared host CPU) widen that window enough to surface it.
     * Probing with the same {@code retrieveTenants} call provisioning makes first means a 200 here guarantees
     * the very endpoint the create steps need is live. Throws if the services never come up within the window.
     */
    public static void awaitTenantMgtServiceReady() {

        long deadline = System.currentTimeMillis() + Constants.SERVER_STARTUP_WAIT_TIME;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (sendRetrieveTenants().getResponseCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                // admin services not accepting the SOAP request yet
            }
            try {
                logger.info("Waiting for the Tenant Mgt admin service to be ready for provisioning...");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException(
                "Tenant Mgt admin service did not become ready for provisioning within "
                        + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s");
    }

    /**
     * Retrieves the domains of all existing tenants via the Tenant Management Admin Service. Copied from
     * the {@code "I retrieve existing tenant details"} step, returning the parsed domains directly.
     */
    private static List<String> retrieveExistingTenantDomains() throws IOException, JaxenException {

        HttpResponse response = sendRetrieveTenants();
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        return Utils.getNodeTextsByXPath(response.getData(), "//*[local-name()='tenantDomain']");
    }

    /** Sends the {@code retrieveTenants} SOAP request (super-tenant admin) and returns the raw response. */
    private static HttpResponse sendRetrieveTenants() throws IOException {

        String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ser:retrieveTenants/>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        String url = Utils.getTenantMgtAdminServiceURL(getBaseUrl());
        return SimpleHTTPClient.getInstance().sendSoapRequest(
                url, payload, "urn:retrieveTenants",
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
    }

    /**
     * Retrieves the usernames of all existing users in the given tenant via the RemoteUserStoreManager
     * service. Copied from the {@code "I retrieve all existing users in the tenant domain ..."} step,
     * returning the parsed usernames directly.
     */
    private static List<String> retrieveExistingUsers(Tenant tenant) throws IOException, JaxenException {

        String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">" +
                        "<soapenv:Header/>" +
                        "<soapenv:Body>" +
                        "<ser:listUsers>" +
                        "<ser:filter>*</ser:filter>" +
                        "<ser:maxItemLimit>-1</ser:maxItemLimit>" +
                        "</ser:listUsers>" +
                        "</soapenv:Body>" +
                        "</soapenv:Envelope>";

        User tenantAdmin = tenant.getTenantAdmin();

        String url = Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:listUsers",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());

        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        return Utils.getNodeTextsByXPath(response.getData(),
                "//*[local-name()='listUsersResponse']/*[local-name()='return']");
    }

    /**
     * Sets a single user-profile claim value on an existing user via the RemoteUserStoreManagerService SOAP
     * {@code setUserClaimValue} operation (authenticated as the tenant admin). Used to populate profile claims
     * (givenname / lastname / mobile / organization) that a downstream flow expects to surface — e.g. in the
     * backend JWT. {@code username} is the bare user name (no tenant suffix for the super tenant).
     *
     * @param tenantDomain the tenant the user belongs to
     * @param username     bare user name (the user store user, without an {@code @tenant} suffix)
     * @param claimUri     the local claim URI (e.g. {@code http://wso2.org/claims/mobile})
     * @param claimValue   the value to set
     */
    public static void setUserClaimValue(String tenantDomain, String username, String claimUri, String claimValue)
            throws IOException {

        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<ser:setUserClaimValue>" +
                "<ser:userName>" + username + "</ser:userName>" +
                "<ser:claimURI>" + claimUri + "</ser:claimURI>" +
                "<ser:claimValue>" + claimValue + "</ser:claimValue>" +
                "<ser:profileName>default</ser:profileName>" +
                "</ser:setUserClaimValue>" +
                "</soapenv:Body></soapenv:Envelope>";

        String url = Utils.getRemoteUserStoreManagerServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:setUserClaimValue",
                tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
        // setUserClaimValue is a void SOAP operation: Axis2 answers 202 Accepted (no response body) on success;
        // a SOAP fault would be 500. Accept either 2xx-with-no-body form. The real verification that the claim
        // actually took is the downstream assertion (e.g. the claim appearing in the backend JWT).
        int code = response.getResponseCode();
        Assert.assertTrue(code == 200 || code == 202,
                "setUserClaimValue for " + claimUri + " failed (" + code + "): " + response.getData());
    }

    /**
     * Registers an OIDC external claim (ClaimMetadataManagementService {@code addExternalClaim}) mapping an OIDC
     * dialect claim to a local claim, so a non-standard claim (e.g. mobile, organization) can be requested and
     * surfaced. Tolerant of a 500 "already exists" fault (idempotent across a shared container). Ports part of
     * the legacy createClaimMapping.
     */
    public static void addOidcExternalClaim(String tenantDomain, String oidcClaimUri, String localClaimUri)
            throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\" " +
                "xmlns:dto=\"http://dto.mgt.metadata.claim.identity.carbon.wso2.org/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:addExternalClaim><xsd:externalClaim>" +
                "<dto:externalClaimDialectURI>http://wso2.org/oidc/claim</dto:externalClaimDialectURI>" +
                "<dto:externalClaimURI>" + oidcClaimUri + "</dto:externalClaimURI>" +
                "<dto:mappedLocalClaimURI>" + localClaimUri + "</dto:mappedLocalClaimURI>" +
                "</xsd:externalClaim></xsd:addExternalClaim>" +
                "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getClaimMetadataManagementServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:addExternalClaim",
                tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
        int code = response.getResponseCode();
        boolean alreadyExists = response.getData() != null && response.getData().toLowerCase().contains("already");
        Assert.assertTrue(code == 200 || code == 202 || alreadyExists,
                "addExternalClaim " + oidcClaimUri + " failed (" + code + "): " + response.getData());
    }

    /**
     * Binds OIDC claims to an OAuth scope (OAuthAdminService {@code updateScope}), so requesting that scope
     * returns those claims. Ports the {@code updateScope("openid", ...)} part of the legacy createClaimMapping.
     */
    public static void updateOidcScopeClaims(String tenantDomain, String scope, String[] addClaims)
            throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        StringBuilder claimsXml = new StringBuilder();
        for (String c : addClaims) {
            claimsXml.append("<xsd:addClaims>").append(c).append("</xsd:addClaims>");
        }
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:updateScope><xsd:scope>" + scope + "</xsd:scope>" + claimsXml + "</xsd:updateScope>" +
                "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getOAuthAdminServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:updateScope",
                tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
        int code = response.getResponseCode();
        Assert.assertTrue(code == 200 || code == 202,
                "updateScope " + scope + " failed (" + code + "): " + response.getData());
    }

    /**
     * Resolves the service-provider (application) name backing an OAuth consumer key, via OAuthAdminService
     * {@code getOAuthApplicationData} (SOAP). The SP name is NOT the DevPortal application name — it is a
     * generated name only obtainable from the OAuth app data. Needed before getApplication/updateApplication.
     */
    public static String getSpNameByConsumerKey(String tenantDomain, String consumerKey) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:getOAuthApplicationData><xsd:consumerKey>" + consumerKey + "</xsd:consumerKey>" +
                "</xsd:getOAuthApplicationData>" +
                "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getOAuthAdminServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload,
                "urn:getOAuthApplicationData", tenant.getTenantAdmin().getUserName(),
                tenant.getTenantAdmin().getPassword());
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<[^>]*applicationName>([^<]*)</[^>]*applicationName>").matcher(response.getData());
        // Body omitted from the message — the getOAuthApplicationData response carries the OAuth consumer
        // secret and must not reach CI logs; the status code is enough to diagnose a failed lookup.
        Assert.assertTrue(m.find(),
                "Could not resolve SP name from getOAuthApplicationData (HTTP " + response.getResponseCode() + ")");
        return m.group(1);
    }

    /**
     * Retrieves an OAuth service provider's full representation via IdentityApplicationManagementService
     * {@code getApplication} (SOAP, super-tenant admin). Returns the raw response body. Used to round-trip the
     * ServiceProvider when adding requested-claim mappings (the backend JWT only surfaces claims the SP requests).
     */
    public static String getServiceProvider(String tenantDomain, String spName) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:getApplication><xsd:applicationName>" + spName + "</xsd:applicationName></xsd:getApplication>" +
                "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getIdentityApplicationManagementServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:getApplication",
                tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
        // Log only the SP name and status — the getApplication body carries the OAuth consumer secret and must
        // not be written to CI output.
        logger.info("getApplication(" + spName + ") -> " + response.getResponseCode());
        return response.getData();
    }

    /**
     * Configures the OAuth service provider (resolved from {@code consumerKey}) to REQUEST the given local user
     * claims, so the backend JWT includes them. The APIM backend JWT only surfaces claims the SP requests, so the
     * SP's ClaimConfig must carry a requested+mandatory claim mapping per URI. Done by round-tripping the full
     * ServiceProvider (IdentityApplicationManagementService getApplication → updateApplication) so the OAuth
     * inbound-auth binding and every other field are preserved; only the claimConfig element is replaced in place
     * (localClaimDialect=false + one requested/mandatory mapping per claim). Ports the legacy
     * updateServiceProviderWithRequiredClaims.
     */
    public static void addRequestedClaimsToServiceProvider(String tenantDomain, String consumerKey,
                                                           String[] claimUris) throws IOException {
        Tenant tenant = Utils.getTenantFromContext(tenantDomain);
        String spName = getSpNameByConsumerKey(tenantDomain, consumerKey);
        String getResp = getServiceProvider(tenantDomain, spName);

        // Extract <p:return ATTRS>INNER</p:return> — ATTRS carries the SP's namespace declarations + xsi:type.
        java.util.regex.Matcher rm = java.util.regex.Pattern
                .compile("<(\\w+):return\\b([^>]*)>(.*)</\\1:return>", java.util.regex.Pattern.DOTALL)
                .matcher(getResp);
        // Body omitted from the message — getResp (the getApplication ServiceProvider body) carries the OAuth
        // consumer secret and must not reach CI logs (see getServiceProvider); the SP name is enough to diagnose.
        Assert.assertTrue(rm.find(), "getApplication returned no service provider for " + spName);
        // Keep the return element's xmlns declarations but DROP its top-level xsi:type="...ServiceProvider":
        // the updateApplication schema already types the serviceProvider param as ServiceProvider, and a
        // self-referential xsi:type on it makes Axis2 ADB recurse resolving the type extension -> StackOverflowError.
        String spAttrs = rm.group(2).replaceAll("\\s+xsi:type=\"[^\"]*\"", "");
        String inner = rm.group(3);

        // The model-namespace prefix (e.g. ax2231) is generated per response — capture it dynamically.
        java.util.regex.Matcher pm = java.util.regex.Pattern
                .compile("xmlns:(\\w+)=\"http://model\\.common\\.application\\.identity\\.carbon\\.wso2\\.org/xsd\"")
                .matcher(spAttrs);
        String ax = pm.find() ? pm.group(1) : "ax2231";

        StringBuilder mappings = new StringBuilder();
        for (String uri : claimUris) {
            mappings.append("<").append(ax).append(":claimMappings xsi:type=\"").append(ax).append(":ClaimMapping\">")
                    .append("<").append(ax).append(":localClaim xsi:type=\"").append(ax).append(":Claim\"><")
                    .append(ax).append(":claimUri>").append(uri).append("</").append(ax).append(":claimUri></")
                    .append(ax).append(":localClaim>")
                    .append("<").append(ax).append(":mandatory>true</").append(ax).append(":mandatory>")
                    .append("<").append(ax).append(":remoteClaim xsi:type=\"").append(ax).append(":Claim\"><")
                    .append(ax).append(":claimUri>").append(uri).append("</").append(ax).append(":claimUri></")
                    .append(ax).append(":remoteClaim>")
                    .append("<").append(ax).append(":requested>true</").append(ax).append(":requested>")
                    .append("</").append(ax).append(":claimMappings>");
        }
        // ClaimConfig children in ADB (alphabetical) order: alwaysSendMappedLocalSubjectId, claimMappings,
        // localClaimDialect, roleClaimURI, userClaimURI. localClaimDialect=false so the explicit mappings apply.
        String newClaimConfig = "<" + ax + ":claimConfig xsi:type=\"" + ax + ":ClaimConfig\">"
                + "<" + ax + ":alwaysSendMappedLocalSubjectId>false</" + ax + ":alwaysSendMappedLocalSubjectId>"
                + mappings
                + "<" + ax + ":localClaimDialect>false</" + ax + ":localClaimDialect>"
                + "<" + ax + ":roleClaimURI xsi:nil=\"true\"/><" + ax + ":userClaimURI xsi:nil=\"true\"/>"
                + "</" + ax + ":claimConfig>";

        // (?s) DOTALL so .*? spans a claimConfig body that may contain newlines (matches the <return> extraction
        // above); the reluctant quantifier + non-nesting claimConfig keeps it matching exactly one element.
        String newInner = inner.replaceAll("(?s)<" + ax + ":claimConfig\\b[^>]*>.*?</" + ax + ":claimConfig>",
                java.util.regex.Matcher.quoteReplacement(newClaimConfig));
        Assert.assertNotEquals(newInner, inner, "claimConfig element not found/replaced in the service provider");
        // Drop ALL xsi:type hints (keep xsi:nil): every field is a concrete type inferable from its element
        // name, and the xsi:type extension resolution is what makes Axis2 ADB recurse -> StackOverflowError.
        newInner = newInner.replaceAll("\\s+xsi:type=\"[^\"]*\"", "");

        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:xsd=\"http://org.apache.axis2/xsd\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<xsd:updateApplication><xsd:serviceProvider" + spAttrs + ">" + newInner +
                "</xsd:serviceProvider></xsd:updateApplication>" +
                "</soapenv:Body></soapenv:Envelope>";
        String url = Utils.getIdentityApplicationManagementServiceURL(getBaseUrl());
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:updateApplication",
                tenant.getTenantAdmin().getUserName(), tenant.getTenantAdmin().getPassword());
        int code = response.getResponseCode();
        Assert.assertTrue(code == 200 || code == 202,
                "updateApplication (requested claims) failed (" + code + "): " + response.getData());
    }

}
