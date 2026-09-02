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

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.IntegrationActors;
import org.wso2.am.integration.cucumbertests.utils.MultiTenantSsoProvisioner;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.PlaywrightSsoClient;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.utils.Constants;

import java.util.List;
import java.util.Locale;

/**
 * Steps for the multi-tenant console-SSO journey: provisioning the nested broker topology, and driving the
 * resulting login in a real browser.
 *
 * <p>Provisioning drives SOAP admin services that have no REST equivalent (tenant creation, identity-provider
 * registration, service-provider authentication steps), which CLAUDE.md §14 permits through a helper — here
 * {@link MultiTenantSsoProvisioner} — provided it is called from a {@code _setup_} step as an actor rather than
 * from a listener hook.
 */
public class MultiTenantSsoSteps {

    /** One real browser per scenario; its cookie jar carries the session across the broker chain. */
    private PlaywrightSsoClient browser;

    @After("@rule:multitenant-sso")
    public void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Provisioning (the _setup_ feature)
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Creates the tenant in the IDENTITY SERVER and waits for it to be synchronized and activated in API
     * Manager. The tenant is created once, on the identity server side, and allowed to propagate — the path the
     * multi-tenant guide prescribes.
     */
    @When("I provision the SSO tenant {string} on the identity server with admin password {string}")
    public void iProvisionTheSsoTenant(String domain, String adminPassword) throws Exception {
        MultiTenantSsoProvisioner.createIsTenant(domain, "admin", adminPassword, "sso-admin@" + domain);
        MultiTenantSsoProvisioner.awaitApimTenantSynced(domain);
        TestContext.set("mtTenantDomain", domain);
        TestContext.set("mtTenantAdmin", "admin");
        TestContext.set("mtTenantAdminPassword", adminPassword);
    }

    /** Creates the identity server application inside the tenant that the tenant's own service provider federates to. */
    @And("I provision the SSO identity server application in tenant {string}")
    public void iProvisionTheIsApplication(String domain) throws Exception {
        String appName = Names.unique("MT_SSO_APP");
        String[] credentials = MultiTenantSsoProvisioner.createTenantOidcApp(domain, appName,
                TestContext.resolve("mtTenantAdmin").toString(),
                TestContext.resolve("mtTenantAdminPassword").toString());
        TestContext.set("mtIsAppName", appName);
        TestContext.set("mtIsClientId", credentials[0]);
        TestContext.set("mtIsClientSecret", credentials[1]);
    }

    /** Creates the federated user inside the tenant, in the groups the chain maps onto API Manager roles. */
    @And("I provision the SSO user {string} with password {string} in groups {string} in tenant {string}")
    public void iProvisionTheSsoUser(String user, String password, String groups, String domain) throws Exception {
        String uniqueUser = Names.unique(user);
        MultiTenantSsoProvisioner.createTenantUser(domain, uniqueUser, password, uniqueUser + "@" + domain,
                List.of(groups.split("\\s*,\\s*")),
                TestContext.resolve("mtTenantAdmin").toString(),
                TestContext.resolve("mtTenantAdminPassword").toString());
        TestContext.set("mtUser", uniqueUser);
        TestContext.set("mtUserPassword", password);
    }

    /** Registers, in the tenant, the OIDC identity provider that federates to the tenant's identity server app. */
    @And("I configure the SSO tenant identity provider in tenant {string}")
    public void iConfigureTheTenantIdp(String domain) throws Exception {
        String idpName = Names.unique("MT_TENANT_IDP");
        MultiTenantSsoProvisioner.registerTenantOidcIdp(domain, idpName,
                TestContext.resolve("mtIsClientId").toString(),
                TestContext.resolve("mtIsClientSecret").toString(),
                TestContext.resolve("mtTenantAdmin").toString(),
                TestContext.resolve("mtTenantAdminPassword").toString());
        TestContext.set("mtTenantIdpName", idpName);
    }

    /** Creates the tenant's common service provider — the broker's entry point into that tenant. */
    @And("I configure the SSO common service provider in tenant {string}")
    public void iConfigureTheCommonServiceProvider(String domain) throws Exception {
        // The name the multi-tenant guide fixes for this service provider in every tenant, and which the
        // broker resolves the tenant's service provider by.
        String spName = "commonsp";
        MultiTenantSsoProvisioner.setupCommonServiceProvider(domain, spName,
                TestContext.resolve("mtTenantIdpName").toString(), "OpenIDConnectAuthenticator",
                TestContext.resolve("mtTenantAdmin").toString(),
                TestContext.resolve("mtTenantAdminPassword").toString());
        TestContext.set("mtCommonSpName", spName);
    }

    /**
     * Creates the super tenant's common service provider, federated to the broker identity provider. The broker
     * enters whichever tenant is chosen through the service provider of this name, so the super tenant needs one
     * as much as any other tenant does.
     */
    @And("I configure the SSO common service provider in the super tenant")
    public void iConfigureTheSuperTenantCommonServiceProvider() throws Exception {
        MultiTenantSsoProvisioner.setupCommonServiceProvider(Constants.SUPER_TENANT_DOMAIN,
                TestContext.resolve("mtCommonSpName").toString(),
                TestContext.resolve("mtBrokerIdpName").toString(), "multiTenantAuthenticator",
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
    }

    /**
     * Initializes all three console service providers before broker wiring. Publisher and Admin register from an
     * unauthenticated login-endpoint request; DevPortal is an SPA and must complete its local bootstrap login so
     * its DCR creates {@code apim_devportal}. This is setup only; the actual federated journeys run later.
     */
    @And("I initialize all console service providers")
    public void initializeAllConsoleServiceProviders() {
        MultiTenantSsoProvisioner.initializeConsoleServiceProviders();
        Object gatewayBase = TestContext.get("baseGatewayUrl");
        try (PlaywrightSsoClient setupBrowser = new PlaywrightSsoClient(TestContext.get("baseUrl").toString(),
                IntegrationActors.baseUrl(IntegrationActors.IS),
                gatewayBase == null ? null : gatewayBase.toString())) {
            setupBrowser.openConsoleLoginPage("devportal");
            setupBrowser.authenticateWithLocalAuthenticator(Constants.SUPER_TENANT_ADMIN_USERNAME,
                    Constants.SUPER_TENANT_ADMIN_PASSWORD);
            setupBrowser.assertLandedInConsole("devportal");
        }
    }

    /** The lazy-creation checkpoint is deliberately separate from broker wiring for an attributable failure. */
    @And("I assert all console service providers exist before wiring them")
    public void assertAllConsoleServiceProvidersExist() throws Exception {
        MultiTenantSsoProvisioner.assertConsoleServiceProviderExists("publisher", "apim_publisher");
        MultiTenantSsoProvisioner.assertConsoleServiceProviderExists("admin", "apim_admin_portal");
        MultiTenantSsoProvisioner.assertConsoleServiceProviderExists("devportal", "apim_devportal");
    }

    /** Registers the super tenant's broker identity provider, which renders tenant selection. */
    @And("I configure the SSO multi-tenant broker identity provider")
    public void iConfigureTheBrokerIdp() throws Exception {
        String idpName = Names.unique("MT_BROKER_IDP");
        MultiTenantSsoProvisioner.registerBrokerIdp(idpName,
                TestContext.resolve("mtCommonSpName").toString(),
                TestContext.resolve("mtIsClientId").toString(),
                TestContext.resolve("mtIsClientSecret").toString());
        TestContext.set("mtBrokerIdpName", idpName);
    }

    /** Points a console's service provider at the broker, so opening it starts the multi-tenant chain. */
    @And("I wire the {string} console to the multi-tenant broker")
    public void iWireTheConsoleToTheBroker(String console) throws Exception {
        MultiTenantSsoProvisioner.wireConsoleToBroker(console, consoleSpName(console),
                TestContext.resolve("mtBrokerIdpName").toString());
    }

    // ---------------------------------------------------------------------------------------------------------
    // The journey
    // ---------------------------------------------------------------------------------------------------------

    /** Creates the real browser session for the multi-tenant journey, proxied to this block's mapped ports. */
    @And("a real browser session for the multi-tenant SSO journey")
    public void aRealBrowserSession() {
        Object gatewayBase = TestContext.get("baseGatewayUrl");
        this.browser = new PlaywrightSsoClient(TestContext.get("baseUrl").toString(),
                IntegrationActors.baseUrl(IntegrationActors.IS),
                gatewayBase == null ? null : gatewayBase.toString());
    }

    @When("I open the {string} console login page for multi-tenant SSO")
    public void iOpenTheConsoleLoginPage(String console) {
        if ("devportal".equals(console) && TestContext.get("mtTenantDomain") != null) {
            browser.setDevportalTenant(TestContext.resolve("mtTenantDomain").toString());
        }
        browser.openConsoleLoginPage(console);
    }

    @When("I open the {string} console expecting multi-tenant SSO")
    public void iOpenTheConsoleExpectingSso(String console) {
        browser.openConsoleExpectingSso(console);
    }

    @Then("I should not be prompted to log in again for multi-tenant SSO")
    public void iShouldNotBePromptedToLogInAgain() {
        browser.assertNotPromptedToLoginAgain();
    }

    @When("I select the tenant {string} on the broker's tenant-selection page")
    public void iSelectTheTenant(String domain) {
        browser.selectTenant(domain);
    }

    @Then("the multi-tenant console login page must carry {string} and offer local plus broker authentication")
    public void theMultiTenantLoginPageMustOfferBothAuthenticators(String multiOptionUri) {
        Assert.assertTrue(browser.pageHasMultiOptionUri(),
                "the MT console login page is not multi-option (no " + multiOptionUri + "): "
                        + browser.pageDiagnostic());
        String brokerIdp = TestContext.resolve("mtBrokerIdpName").toString();
        Assert.assertTrue(browser.pageOffersBothAuthenticators(brokerIdp),
                "the MT console login page does not offer local BasicAuthenticator and broker IdP '"
                        + brokerIdp + "': " + browser.pageDiagnostic());
    }

    @When("I select the multi-tenant broker federated authenticator")
    public void iSelectTheMultiTenantBrokerAuthenticator() {
        browser.selectFederatedIdp(TestContext.resolve("mtBrokerIdpName").toString());
    }

    @Then("the DevPortal login page must offer local plus broker authentication")
    public void theDevportalLoginPageMustOfferLocalPlusBrokerAuthentication() {
        String brokerIdp = TestContext.resolve("mtBrokerIdpName").toString();
        Assert.assertTrue(browser.pageOffersBothAuthenticators(brokerIdp),
                "the DevPortal login endpoint does not offer local BasicAuthenticator and broker IdP '"
                        + brokerIdp + "': " + browser.pageDiagnostic());
    }

    @When("I authenticate with the local authenticator as the {string} tenant administrator")
    public void iAuthenticateWithLocalTenantAdministrator(String tenantDomain) {
        String username = "abc.com".equals(tenantDomain) ? "admin@abc.com" : Constants.SUPER_TENANT_ADMIN_USERNAME;
        String password = "abc.com".equals(tenantDomain) ? "Admin@12345" : Constants.SUPER_TENANT_ADMIN_PASSWORD;
        browser.authenticateWithLocalAuthenticator(username, password);
        TestContext.set("mtLocalUser", username);
    }

    @Then("the {string} console session should belong to the local {string} tenant administrator")
    public void theConsoleSessionShouldBelongToLocalTenantAdministrator(String console, String tenantDomain) {
        String actual = browser.sessionPrincipal(console);
        String expected = TestContext.resolve("mtLocalUser").toString();
        if (!expected.contains("@")) {
            expected = expected + "@" + Constants.SUPER_TENANT_DOMAIN;
        }
        Assert.assertEquals(actual.toLowerCase(Locale.ROOT), expected.toLowerCase(Locale.ROOT),
                "the local login for tenant '" + tenantDomain + "' produced principal '" + actual
                        + "' instead of '" + expected + "'.");
    }

    @When("I log out of the {string} console for multi-tenant SSO")
    public void iLogOutOfTheConsoleForMultiTenantSso(String console) {
        browser.logoutFromConsole(console);
    }

    @Then("the {string} multi-tenant logout should return through its callback")
    public void theMultiTenantLogoutShouldReturnThroughItsCallback(String console) {
        browser.assertLogoutRedirect(console);
    }

    @Then("the {string} console must demand a fresh login after multi-tenant logout")
    public void theConsoleMustDemandFreshLoginAfterMultiTenantLogout(String console) {
        browser.assertConsoleDemandsFreshLogin(console);
    }

    @Then("the Admin console must demand a fresh login after multi-tenant logout")
    public void theAdminConsoleMustDemandFreshLoginAfterMultiTenantLogout() {
        browser.assertConsoleDemandsFreshLogin("admin");
    }

    @Then("the DevPortal session must no longer authorize the MT application")
    public void theDevportalSessionMustNoLongerAuthorizeMtApplication() {
        browser.assertConsoleSessionUnusable("devportal",
                "/api/am/devportal/v3/applications/" + TestContext.resolve("mtAppId"), "/devportal");
    }

    @Then("no multi-tenant console session cookies should remain")
    public void noMultiTenantConsoleSessionCookiesShouldRemain() {
        browser.assertNoSessionCookieResidue();
    }

    @And("I authenticate at the identity server as the tenant's SSO user")
    public void iAuthenticateAsTheTenantUser() {
        browser.authenticateAtExternalIs(TestContext.resolve("mtUser").toString(),
                TestContext.resolve("mtUserPassword").toString());
    }

    /**
     * Asserts the console session belongs to the TENANT's federated user — the whole point of the broker chain,
     * and what distinguishes this from an ordinary super-tenant federated login.
     */
    @Then("the {string} console session should belong to the tenant's SSO user")
    public void theSessionShouldBelongToTheTenantUser(String console) {
        String expected = TestContext.resolve("mtUser").toString();
        String actual = browser.sessionPrincipal(console);
        // Compared bare, because the topology deliberately keeps the tenant OUT of the local subject identifier
        // (useTenantDomainInLocalSubjectIdentifier=false), so the console reports the username unqualified.
        // Appending a tenant here would invent one the product never reported. The username is unique per run,
        // so this does prove the session belongs to THIS federated user rather than to any other principal —
        // but it does not by itself prove which tenant the user was provisioned into.
        Assert.assertEquals(stripTenant(actual).toLowerCase(Locale.ROOT), expected.toLowerCase(Locale.ROOT),
                "the '" + console + "' console session belongs to '" + actual + "' but the multi-tenant login was "
                        + "performed as the tenant's federated user '" + expected + "'.");
    }

    /** A username without any tenant qualifier, so qualified and unqualified forms compare equal. */
    private static String stripTenant(String username) {
        int at = username.indexOf('@');
        return at < 0 ? username : username.substring(0, at);
    }

    /**
     * DIAGNOSTIC (temporary): reports where the federated user was provisioned and what roles it holds, so the
     * two open questions — which tenant, and whether the groups became roles — are answered from the product
     * rather than inferred from a disabled button.
     */
    @Then("I report where the federated user landed and what roles it holds")
    public void reportUserTenantAndRoles() {
        String user = TestContext.resolve("mtUser").toString();
        for (String tenant : new String[] {"carbon.super", "abc.com"}) {
            String roles;
            try {
                roles = (MultiTenantSsoProvisioner.userExists(tenant, user) ? "EXISTS" : "ABSENT")
                        + " roleList=" + MultiTenantSsoProvisioner.rolesOfUser(tenant, user)
                                .replaceAll(".*?(<ns:return>.*</ns:return>).*", "$1");
            } catch (Exception e) {
                roles = "ERROR:" + e.getClass().getSimpleName() + ":" + e.getMessage();
            }
            org.apache.commons.logging.LogFactory.getLog(MultiTenantSsoSteps.class)
                    .info("[MT-DIAG] roles of '" + user + "' in " + tenant + " -> " + roles);
        }
    }

    /**
     * Creates an API through the Publisher as the federated tenant user — work that only a creator/publisher
     * role can do, so it fails outright if the identity server's groups were not mapped onto API Manager roles.
     */
    @And("I create a REST API via the publisher UI as the tenant user named {string}")
    public void iCreateApiAsTheTenantUser(String name) {
        String unique = Names.unique(name);
        String apiId = browser.createRestApi(unique, "/" + unique.toLowerCase(Locale.ROOT), "1.0.0",
                "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice");
        TestContext.set("mtApiId", apiId);
        ResourceCleanup.register(Constants.CREATED_API_IDS, apiId);
        TestContext.set("mtApiName", unique);
        TestContext.set("mtApiContext", "/t/" + TestContext.resolve("mtTenantDomain") + "/"
                + unique.toLowerCase(Locale.ROOT));
    }

    /**
     * Asserts the API the tenant user just created is recorded as belonging to that user IN THE TENANT.
     *
     * <p>This is the assertion that makes the journey multi-tenant. The console reports its session's username
     * unqualified, so the session alone cannot say which tenant the user was provisioned into; the provider API
     * Manager stores against a created resource does carry it. A super-tenant provider here would mean the broker
     * chain authenticated the user but resolved them outside their tenant.
     */
    @Then("the created API should be owned by the tenant's SSO user in tenant {string}")
    public void theCreatedApiShouldBeOwnedByTheTenantUser(String tenantDomain) {
        String expectedUser = TestContext.resolve("mtUser").toString();
        String provider = browser.recordedPrincipal(
                "/api/am/publisher/v4/apis/" + TestContext.resolve("mtApiId"), "/publisher", "provider");
        Assert.assertEquals(stripTenant(provider).toLowerCase(Locale.ROOT),
                expectedUser.toLowerCase(Locale.ROOT),
                "the API was created by '" + provider + "' but the federated tenant user is '" + expectedUser
                        + "'.");
        Assert.assertTrue(provider.toLowerCase(Locale.ROOT).endsWith("@" + tenantDomain.toLowerCase(Locale.ROOT)),
                "the API's provider is '" + provider + "', which does not belong to tenant '" + tenantDomain
                        + "'. The federated user authenticated but was resolved outside their tenant, so this is "
                        + "not multi-tenant single sign-on.");
    }

    @Then("I should land authenticated in the {string} console as a tenant user")
    public void iShouldLandAuthenticated(String console) {
        browser.assertLandedInConsole(console);
    }

    @And("I deploy and publish the created API through the publisher UI")
    public void iDeployAndPublishCreatedApi() {
        browser.deployApi();
        browser.publishApi();
    }

    @And("I create a DevPortal application named {string} as the tenant user")
    public void iCreateDevportalApplication(String name) {
        browser.setDevportalTenant(TestContext.resolve("mtTenantDomain").toString());
        String appName = Names.unique(name);
        String appId = browser.createDevportalApp(appName);
        TestContext.set("mtAppId", appId);
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, appId);
        TestContext.set("mtAppName", appName);
        String owner = browser.recordedPrincipal("/api/am/devportal/v3/applications/" + appId,
                "/devportal", "owner");
        Assert.assertEquals(stripTenant(owner).toLowerCase(Locale.ROOT),
                TestContext.resolve("mtUser").toString().toLowerCase(Locale.ROOT),
                "the DevPortal application owner was '" + owner + "', not the tenant SSO user.");
    }

    @And("I subscribe the DevPortal application to the created API")
    public void iSubscribeDevportalApplication() {
        browser.subscribeAppToApi(TestContext.resolve("mtApiId").toString(),
                TestContext.resolve("mtAppName").toString());
    }

    @And("I generate production keys for the DevPortal application")
    public void iGenerateDevportalApplicationKeys() {
        browser.generateAppKeys();
    }

    @Then("I invoke the created API through the gateway using the DevPortal application")
    public void iInvokeCreatedApiThroughDevportalApplication() {
        int status = browser.invokeApi(TestContext.resolve("mtApiContext").toString(), "1.0.0", "/customers/123");
        Assert.assertEquals(status, 200,
                "the tenant application's generated credentials could not invoke the subscribed API through the gateway.");
    }

    /**
     * Exercises an admin-plane operation with the same federated tenant session used by the browser journey.
     * The target is the tenant's built-in admin account; the acting user is the external SSO user. The exact
     * mapping is verified during tenant IdP read-back; this 200 additionally proves the resulting Admin session
     * can execute the intended tenant admin-plane operation.
     */
    @And("I change the created API provider to the tenant admin through the admin console")
    public void iChangeApiProviderToTenantAdmin() {
        String apiId = TestContext.resolve("mtApiId").toString();
        String tenant = TestContext.resolve("mtTenantDomain").toString();
        int status = browser.changeApiProviderViaAdminSession(apiId, "admin@" + tenant);
        Assert.assertEquals(status, 200, "change-provider via the federated tenant Admin session should return "
                + "200; the external admin group must map to the tenant-scoped API Manager admin role.");
    }

    /** Console context → the service provider API Manager registers for it. */
    private static String consoleSpName(String console) {
        switch (console) {
            case "publisher":
                return "apim_publisher";
            case "admin":
                return "apim_admin_portal";
            case "devportal":
                return "apim_devportal";
            default:
                throw new IllegalArgumentException("Unknown console '" + console
                        + "' (expected publisher/admin/devportal)");
        }
    }
}
