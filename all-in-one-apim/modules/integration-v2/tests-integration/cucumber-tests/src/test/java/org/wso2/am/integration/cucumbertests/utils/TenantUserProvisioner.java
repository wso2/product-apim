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
}
