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
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.IntegrationActors;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.PlaywrightSsoClient;
import org.wso2.am.integration.cucumbertests.utils.SsoProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;

import java.util.List;

/**
 * Provisioning steps for the external-IdP console-SSO block: registers the APIM-side OIDC identity provider
 * against an IS OIDC app, wires the multi-option (local + federated) authentication step onto the Publisher,
 * Admin and DevPortal console service providers, and creates the JIT-provisioned federated user on IS. These
 * are the {@code Background} prerequisites; the federated-login journey itself is the browser-client layer
 * (a later addition), stubbed here so the feature has no undefined step.
 *
 * <p>Per CLAUDE.md §14 these prerequisites drive SOAP admin services with no REST equivalent (IdP registration,
 * console SP authentication-step editing) via {@link SsoProvisioner}, run super-tenant admin/admin.
 */
public class SsoSteps {

    /** One real (Playwright) browser per scenario; its cookie store is the shared SSO session. */
    private PlaywrightSsoClient browser;

    /** Closes the browser + its CONNECT proxy after each scenario (idempotent; runs even on failure). */
    @After("@rule:external-idp-sso")
    public void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
    }

    /**
     * Console name → URL context. The {@code {string}} console names map identically to their URL contexts
     * ({@code publisher}/{@code admin}/{@code devportal}).
     */
    private static String consoleContext(String console) {
        switch (console) {
            case "publisher":
            case "admin":
            case "devportal":
                return console;
            default:
                throw new IllegalArgumentException("Unknown console '" + console
                        + "' (expected publisher/admin/devportal)");
        }
    }

    /** Console context → the resident-IS console service-provider name APIM auto-creates for it. */
    private static String consoleSpName(String console) {
        switch (console) {
            case "publisher":
                return "apim_publisher";
            case "admin":
                // The Admin console's resident SP is apim_admin_portal (per admin/services/constants.jsp), NOT
                // apim_admin — the latter exists but is not the console's OAuth service provider.
                return "apim_admin_portal";
            case "devportal":
                return "apim_devportal";
            default:
                throw new IllegalArgumentException("Unknown console '" + console
                        + "' (expected publisher/admin/devportal)");
        }
    }

    /**
     * Creates the IS OIDC app the consoles federate to and registers it as the APIM-side OIDC identity provider
     * {@code idpName}. Stores the IdP name and the IS app credentials for the wiring/login steps that follow.
     */
    @And("an external Identity Server is registered as OIDC identity provider {string}")
    public void anExternalIdentityServerIsRegisteredAsIdp(String idpName) throws Exception {
        // The feature's {string} is a readable HANDLE; the actual IS app and IdP are uniquely named per §4 so
        // this block never collides with another IS-provisioning block sharing (a future) IS. The unique IdP
        // name is stored under a fixed key the later wiring/select/authenticate steps resolve.
        String[] creds = SsoProvisioner.createIsSsoApp(Names.unique("APIM_SSO"));
        TestContext.set("ssoIsClientId", creds[0]);
        TestContext.set("ssoIsClientSecret", creds[1]);
        String uniqueIdp = Names.unique(idpName);
        TestContext.set("ssoIdpName", uniqueIdp);
        SsoProvisioner.registerOidcIdp(uniqueIdp, creds[0], creds[1]);
    }

    /**
     * Wires the {@code idpName} IdP as a second login option (alongside the local basic authenticator) onto each
     * of the Publisher, Admin and DevPortal console service providers.
     */
    @And("the {string}, {string} and {string} consoles each offer a multi-option login step with a local authenticator and {string}")
    public void theConsolesEachOfferAMultiOptionLoginStep(String console1, String console2, String console3,
            String idpName) throws Exception {
        // idpName is the readable handle; wire the actual (unique) IdP registered in the previous step.
        String idp = TestContext.resolve("ssoIdpName").toString();
        for (String console : List.of(console1, console2, console3)) {
            SsoProvisioner.wireConsoleMultiOption(console, consoleSpName(console), idp);
        }
    }

    /**
     * Creates a federated user on the external Identity Server placed in the three given groups (the remote roles
     * the IdP maps to local APIM roles). Stores the user and password for the later login scenario.
     */
    @And("a federated user {string} exists on the external Identity Server with the {string}, {string} and {string} roles")
    public void aFederatedUserExistsOnTheExternalIdentityServer(String user, String role1, String role2,
            String role3) throws Exception {
        String password = "Sso@Admin2026";
        // user is the readable handle; the actual IS user is uniquely named per §4 and stored for the
        // authenticate step. (The roles/groups are shared reference roles, not per-test resources.)
        String uniqueUser = Names.unique(user);
        SsoProvisioner.createIsFederatedUser(uniqueUser, password, uniqueUser + "@wso2test.com",
                List.of(role1, role2, role3));
        TestContext.set("ssoUser", uniqueUser);
        TestContext.set("ssoUserPassword", password);
    }

    /**
     * Creates the real (Playwright Chromium, headless) browser session for this scenario, wired through an
     * in-process CONNECT proxy to the block's mapped APIM, Identity Server and gateway ports. Its single browser
     * context (cookie jar) carries the shared IS session across the Publisher/Admin/DevPortal opens so genuine
     * SSO can reuse it, and reaches the gateway for the end-to-end invocation.
     */
    @And("a real browser session for the console SSO journey")
    public void aRealBrowserSessionForTheConsoleSsoJourney() {
        Object gatewayBase = TestContext.get("baseGatewayUrl");
        this.browser = new PlaywrightSsoClient(TestContext.get("baseUrl").toString(),
                IntegrationActors.baseUrl(IntegrationActors.IS),
                gatewayBase == null ? null : gatewayBase.toString());
    }

    /** Drives the console login from {@code /services/auth/login} to its (multi-option) login page. */
    @When("I open the {string} console login page")
    public void iOpenTheConsoleLoginPage(String console) {
        browser.openConsoleLoginPage(consoleContext(console));
    }

    /**
     * Fixture-integrity guard (NOT the assertion): the #17744 bug only exists when the step is multi-option,
     * so a missing {@code multiOptionURI} means the fixture has silently degraded and the test would pass for
     * the wrong reason.
     */
    @Then("the login page must be multi-option carrying {string}")
    public void theLoginPageMustBeMultiOption(String multiOptionUri) {
        Assert.assertTrue(browser.pageHasMultiOptionUri(),
                "SP step is not multi-option (no " + multiOptionUri + ") — fixture degraded; this test cannot "
                        + "detect the #17744 regression. " + browser.pageDiagnostic());
    }

    /** Fixture-integrity guard: the step must offer both a local authenticator and the federated one. */
    @Then("the login page must offer both a local authenticator and the {string} federated authenticator")
    public void theLoginPageMustOfferBothAuthenticators(String idpName) {
        String idp = TestContext.resolve("ssoIdpName").toString();
        Assert.assertTrue(browser.pageOffersBothAuthenticators(idp),
                "The login page does not offer both a local authenticator and the '" + idp
                        + "' federated authenticator — fixture degraded.");
    }

    /** Clicks the federated IdP option — this is the #17744 {@code /commonauth} path. */
    @When("I select the {string} federated identity provider")
    public void iSelectTheFederatedIdentityProvider(String idpName) {
        browser.selectFederatedIdp(TestContext.resolve("ssoIdpName").toString());
    }

    /** POSTs the federated user's credentials to the external Identity Server's {@code /commonauth}. */
    @When("I authenticate at the external Identity Server as {string}")
    public void iAuthenticateAtTheExternalIdentityServer(String user) {
        browser.authenticateAtExternalIs(TestContext.resolve("ssoUser").toString(),
                TestContext.get("ssoUserPassword").toString());
    }

    /** Asserts the journey lands authenticated at the given console (via its authz callback with a code). */
    @Then("I should land authenticated in the {string} console")
    public void iShouldLandAuthenticatedInTheConsole(String console) {
        browser.assertLandedInConsole(consoleContext(console));
    }

    /** Opens a console with the shared session (SSO), recording whether it re-prompts for login. */
    @When("I open the {string} console")
    public void iOpenTheConsole(String console) {
        browser.openConsoleExpectingSso(consoleContext(console));
    }

    /** Asserts genuine SSO: opening the console did NOT show a second login prompt. */
    @Then("I should NOT be prompted to log in again")
    public void iShouldNotBePromptedToLogInAgain() {
        browser.assertNotPromptedToLoginAgain();
    }

    /** Creates a REST API through the publisher SPA under the SSO session and stores its id/name/context. */
    @Then("I create a REST API via the publisher UI named {string}")
    public void iCreateRestApiViaUi(String name) {
        String unique = Names.unique(name);
        String context = "/" + unique.toLowerCase();
        String apiId = browser.createRestApi(unique, context, "1.0.0",
                "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice");
        TestContext.set("ssoApiId", apiId);
        TestContext.set("ssoApiName", unique);
        TestContext.set("ssoApiContext", context);
    }

    /** Deploys the current API's first revision to the Default gateway through the publisher SPA. */
    @Then("I deploy the API via the publisher UI")
    public void iDeployApiViaUi() {
        browser.deployApi();
    }

    /** Publishes the current API through the publisher SPA lifecycle page. */
    @Then("I publish the API via the publisher UI")
    public void iPublishApiViaUi() {
        browser.publishApi();
    }

    /** Redeploys the API (new revision) via the publisher SPA — proving the admin→publisher round-trip needs no login. */
    @Then("I redeploy the API via the publisher UI")
    public void iRedeployApiViaUi() {
        browser.redeployApi();
    }

    /** Creates a DevPortal application via the SPA under the SSO session and stores its id/name. */
    @Then("I create an application via the devportal UI named {string}")
    public void iCreateDevportalApp(String name) {
        String appName = Names.unique(name);
        String appId = browser.createDevportalApp(appName);
        TestContext.set("ssoAppId", appId);
        TestContext.set("ssoAppName", appName);
    }

    /** Subscribes the SSO application to the published API via the devportal credentials page. */
    @Then("I subscribe the application to the API via the devportal UI")
    public void iSubscribeAppToApi() {
        browser.subscribeAppToApi(TestContext.resolve("ssoApiId").toString(),
                TestContext.resolve("ssoAppName").toString());
    }

    /** Generates production OAuth keys for the SSO application via the devportal keys page. */
    @Then("I generate keys for the application via the devportal UI")
    public void iGenerateAppKeys() {
        browser.generateAppKeys();
    }

    /** Invokes the API through the gateway with an app token minted from the devportal-generated keys (end-to-end). */
    @Then("I invoke the API through the gateway using the generated application key")
    public void iInvokeApiViaGateway() {
        int status = browser.invokeApi(TestContext.resolve("ssoApiContext").toString(), "1.0.0", "/customers/123");
        Assert.assertEquals(status, 200, "the SSO-created, subscribed application should invoke the API through "
                + "the gateway and get 200 (end-to-end consumer proof).");
    }

    /**
     * Performs a genuine admin operation — change the current API's provider to another user — using ONLY the SSO
     * session's own admin token (there is no Admin-console UI for provider change), proving the federated admin
     * session authorizes a real admin API call. The target provider is resolved from an actor reference.
     */
    @Then("I change the API provider to actor {string} using the admin SSO session")
    public void iChangeApiProviderViaSsoAdminSession(String actorRef) {
        String apiId = TestContext.resolve("ssoApiId").toString();
        // Super-tenant users are addressed to change-provider by their BARE username (the existing REST feature
        // passes bare names for carbon.super); strip the "@carbon.super" qualifier the actor registry carries.
        String provider = Identity.resolveActor(actorRef).getUserName().replace("@carbon.super", "");
        int status = browser.changeApiProviderViaAdminSession(apiId, provider);
        Assert.assertEquals(status, 200, "change-provider via the SSO admin session should return 200 — the "
                + "federated admin session's token must authorize this admin-only operation.");
        TestContext.set("ssoApiProvider", provider);
    }
}
