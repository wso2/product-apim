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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps for the user-onboarding / credential-lifecycle features: DevPortal <b>self sign-up</b> through the
 * product's self-registration API, a throwaway subscriber's DevPortal self-service password change and the
 * password-grant re-authentication that proves it. The observable in each case is what the resulting credential
 * can do — whether a password-grant token is issued for it, and which planes that token then reaches.
 *
 * <p>The EMAIL-FORM-USERNAME login flow ({@code key-manager/email_username_login.feature}) needs no step of its
 * own and adds none: an email-form username is a block MODE, not a per-scenario operation — the
 * {@code emailUserMode} block parameter makes the framework provision every user with an email-form physical
 * username (see {@code BlockLifecycleListener.PARAM_EMAIL_USER_MODE}), after which the standard auth composites
 * and {@link #aPasswordGrantReturnsStatus} below are exactly the right observables for it.
 *
 * <p>Per CLAUDE.md §14, users these steps bring into being are runtime ACTORS in the tenant bean — created here
 * over SOAP ({@link TenantUserProvisioner#addUser}) or created by the PRODUCT and then adopted
 * ({@link TenantUserProvisioner#registerRuntimeUserAsActor}, the self-sign-up case) — so the standard auth
 * composites, {@link Identity} token getters and {@code ResourceCleanup} all apply. A throwaway subscriber is
 * used for the password-change flow (never the shared {@code subscriberUser}, whose password mutation would
 * poison parallel scenarios).
 */
public class LoginSteps {

    /** The product's self-registration ("sign me up") endpoint, relative to the block's base URL. */
    private static final String SELF_REGISTRATION_PATH = "api/identity/user/v1.0/me";

    /**
     * Provisions a throwaway subscriber (a unique username derived from {@code userKeyBase}) in the given tenant
     * via the user-admin SOAP service, and registers it as a resolvable runtime actor under
     * {@code <uniqueName>@<tenant>}. The generated username is stored under {@code userKeyBase} so the feature
     * references it as {@code {{userKeyBase}}}. The initial password is stored under {@code <userKeyBase>Password}.
     * Used for the password-change flow, which mutates the user's password and so must never touch a shared actor.
     *
     * @param userKeyBase  base name; the unique generated username is stored here and is also the actor key
     * @param tenantDomain tenant to provision the user into (its admin performs the SOAP create)
     * @param password     the subscriber's initial password
     */
    @When("I provision a throwaway subscriber {string} in tenant {string} with password {string}")
    public void iProvisionThrowawaySubscriber(String userKeyBase, String tenantDomain, String password)
            throws Exception {

        String username = Names.unique(userKeyBase).replaceAll("[^A-Za-z0-9]", "");
        TestContext.set(userKeyBase, username);
        TestContext.set(userKeyBase + "Password", password);
        TenantUserProvisioner.addUser(tenantDomain, userKeyBase, username, password, "Internal/subscriber");
    }

    /**
     * Self-signs-up a new DevPortal user through the product's self-registration API
     * ({@code POST api/identity/user/v1.0/me}) — the real consumer-facing onboarding path, and the trigger for the
     * {@code AM_USER_SIGNUP} workflow (APIM's {@code userPostSelfRegistration} event handler fires the workflow
     * for a POST_ADD_USER whose roles carry {@code Internal/selfsignup} but not the subscriber role). A product
     * operation, so it runs as the acting actor (its Basic credentials authenticate the call, mirroring the legacy
     * {@code UserManagementUtils.signupUser}) and the response is published for the feature's exact status
     * assertion.
     *
     * <p>On a successful create it does the two things a new runtime principal needs (CLAUDE.md §5/§14):
     * <ol>
     *   <li>registers the username under {@link ResourceCleanup#CREATED_SIGNUP_USERNAMES} — tagged with the
     *       CURRENT acting actor, i.e. the one who signed up, so teardown deletes it in the right tenant; and</li>
     *   <li>registers the user as a runtime ACTOR under {@code actorRef}, so the standard DCR/token composites and
     *       {@code I act as} work for it without any side-channel credential handling.</li>
     * </ol>
     * The username is unique-by-construction ({@link Names#unique}, non-alphanumerics stripped so it satisfies the
     * store's {@code UsernameJavaRegEx}) and stored under {@code usernameKey} for the feature to reference as
     * {@code {{usernameKey}}} — the {@code tenantAwareUserName} the pending workflow task is matched on. The actor
     * KEY is a literal label (a context handle, not a product resource), so scenarios in this block use distinct
     * keys to avoid shadowing each other's registration in the shared tenant bean.
     *
     * @param password    the sign-up password (must satisfy the store's PasswordJavaRegEx)
     * @param actorRef    actor key to register the new user under
     * @param usernameKey context key to store the generated username under
     */
    @When("I self-sign-up a DevPortal user with password {string} as actor {string} storing the username as {string}")
    public void iSelfSignUpDevportalUser(String password, String actorRef, String usernameKey) throws IOException {
        selfSignUp(password, actorRef, usernameKey, false);
    }

    /**
     * As {@link #iSelfSignUpDevportalUser} but the generated username contains a HYPHEN — the specific regression
     * ApplicationBlockSubscriptionTestCase guards, which signed up {@code test-user} and had it own {@code test-app}.
     * A hyphen is legal in the store's {@code UsernameJavaRegEx}, so the plain variant's blanket
     * non-alphanumeric strip is what makes it unreachable there rather than any product constraint; keeping the
     * strip as the default and adding this variant leaves every other caller's username shape untouched.
     *
     * <p>Worth its own step because the downstream key is composite: a blocked subscription is looked up at the
     * gateway by the application's OWNER plus its NAME, so a separator character in either is exactly what a
     * parsing regression would break, and no other scenario in the suite produces one.
     */
    @When("I self-sign-up a DevPortal user with a hyphenated username and password {string} as actor {string} storing the username as {string}")
    public void iSelfSignUpDevportalUserWithHyphen(String password, String actorRef, String usernameKey)
            throws IOException {
        selfSignUp(password, actorRef, usernameKey, true);
    }

    private void selfSignUp(String password, String actorRef, String usernameKey, boolean hyphenated)
            throws IOException {

        String tenantDomain = Identity.actingTenantDomain();
        // actorRef is a context handle and may carry "@tenant"; that suffix is routing metadata, not part of the
        // physical username. Including it makes the generated tenant username exceed UsernameJavaRegEx's limit.
        String actorBase = actorRef.contains(Constants.CHAR_AT)
                ? actorRef.substring(0, actorRef.indexOf(Constants.CHAR_AT)) : actorRef;
        String username = Names.unique(actorBase).replaceAll("[^A-Za-z0-9]", "");
        if (hyphenated) {
            // A single interior hyphen: enough to exercise the separator, and it keeps the name inside the store's
            // UsernameJavaRegEx (which allows '-' but not, say, ':' or '/').
            username = "sub-" + username;
        }
        String body = new JSONObject()
                .put("user", new JSONObject()
                        // The identity API models these as separate fields. Putting "@tenant" in username makes
                        // the primary store's UsernameJavaRegEx reject an otherwise valid tenant registration.
                        .put("username", username)
                        .put("password", password)
                        .put("claims", new JSONArray()
                                .put(signUpClaim("http://wso2.org/claims/givenname", username))
                                .put(signUpClaim("http://wso2.org/claims/emailaddress", username + "@wso2.test"))
                                .put(signUpClaim("http://wso2.org/claims/organization", "wso2"))))
                .put("properties", new JSONArray())
                .toString();

        // Carbon selects a tenant for identity endpoints through the standard /t/<domain>/ URL prefix. Without
        // that prefix the request is handled in carbon.super, where tenant-admin Basic credentials are invalid;
        // qualifying the username instead is also wrong because '@' is then checked by UsernameJavaRegEx.
        String tenantPath = Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? SELF_REGISTRATION_PATH : "t/" + tenantDomain + "/" + SELF_REGISTRATION_PATH;
        HttpResponse response = Requests.post(Utils.getBaseUrl() + tenantPath,
                Identity.actingBasicAuthHeaders(), body, Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response != null && response.getResponseCode() == 201) {
            TestContext.set(Utils.normalizeContextKey(usernameKey), username);
            ResourceCleanup.register(ResourceCleanup.CREATED_SIGNUP_USERNAMES, username);
            TenantUserProvisioner.registerRuntimeUserAsActor(tenantDomain, actorBase, username, password);
        }
    }

    /** One {@code {uri,value}} entry of the self-registration payload's {@code claims} array. */
    private static JSONObject signUpClaim(String uri, String value) {
        return new JSONObject().put("uri", uri).put("value", value);
    }

    /**
     * Calls the DevPortal self-service change-password endpoint ({@code POST me/change-password}) as the acting
     * actor, using its cached DevPortal token, sending the given current/new passwords. Publishes the response
     * for the following status assertion. On a successful (200) change, updates the acting actor's in-memory
     * password so a later no-arg password-grant for the same actor uses the NEW password.
     *
     * @param currentPassword the actor's current password
     * @param newPassword     the new password to set
     */
    @When("I change the acting user's password from {string} to {string} via the DevPortal")
    public void iChangePassword(String currentPassword, String newPassword) throws IOException {

        String body = new JSONObject()
                .put("currentPassword", currentPassword)
                .put("newPassword", newPassword)
                .toString();
        HttpResponse response = Requests.post(
                Utils.getBaseUrl() + Constants.DEFAULT_DEVPORTAL + "me/change-password",
                Identity.devportalHeaders(), body, Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response != null && response.getResponseCode() == 200) {
            // Keep the actor bean in sync so a subsequent no-arg password grant for this actor uses the new
            // password (the change-password endpoint doesn't rotate the cached DevPortal token, so re-auth is
            // the meaningful proof).
            Identity.actingActor().setPassword(newPassword);
        }
    }

    /**
     * Attempts a fresh DCR + password-grant round trip for the acting actor with an EXPLICIT password, and
     * asserts the token endpoint answers the given exact status. A raw, side-channel-free round trip (never a
     * cached actor token — the point is to exercise these exact credentials from scratch): DCR is an idempotent
     * upsert by clientName, so a per-attempt unique clientName keeps parallel blocks isolated. Used to prove a
     * changed password (NEW → 200, OLD → 400) at the credential level, which is the real regression signal.
     *
     * @param password       the password to authenticate with
     * @param expectedStatus the exact expected token-endpoint status
     */
    @Then("a password-grant token request for the acting user with password {string} returns status {int}")
    public void aPasswordGrantReturnsStatus(String password, int expectedStatus) throws IOException {

        User actor = Identity.actingActor();
        HttpResponse response = passwordGrant(actor.getUserName(), password);
        Assert.assertNotNull(response, "No response from the token endpoint for " + actor.getUserName());
        Assert.assertEquals(response.getResponseCode(), expectedStatus,
                "Expected password-grant status " + expectedStatus + " for " + actor.getUserName()
                        + " with the given password, but got=" + response.getResponseCode() + "/"
                        + response.getData());
    }

    /**
     * A fresh DCR + password-grant round trip with explicit credentials, returning the token response (or the
     * DCR response if DCR itself is refused). Mirrors {@code TenantSharingSteps#attemptAdminToken}: raw
     * {@link SimpleHTTPClient} round trip consumed locally (not published to {@code httpResponse}); a unique DCR
     * clientName per call keeps parallel blocks isolated; only {@link IOException} propagates.
     */
    private HttpResponse passwordGrant(String username, String password) throws IOException {

        Map<String, String> dcrBasic = Identity.basicAuthHeaders(username, password);
        String dcrBody = new JSONObject()
                .put("callbackUrl", "www.google.lk")
                .put("clientName", "loginProbe_" + Names.unique("c").replaceAll("[^A-Za-z0-9]", ""))
                .put("grantType", "password")
                .put("saasApp", true)
                .put("owner", username)
                .toString();
        HttpResponse dcr = SimpleHTTPClient.getInstance().doPost(Utils.getDCREndpointURL(Utils.getBaseUrl()),
                dcrBasic, dcrBody, Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (dcr == null || dcr.getResponseCode() != 200 || dcr.getData() == null || dcr.getData().isBlank()) {
            // DCR (the credential's own basic-auth) is the first credential gate — a rejected password is
            // refused here. Return that response so the caller pins its exact status.
            return dcr;
        }
        JSONObject creds = new JSONObject(dcr.getData());
        String tokenBasic = Base64.getEncoder().encodeToString((creds.getString("clientId") + ":"
                + creds.getString("clientSecret")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, String> tokenHeaders = new HashMap<>();
        tokenHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + tokenBasic);
        String form = "grant_type=password&username=" + Utils.urlEncode(username)
                + "&password=" + Utils.urlEncode(password) + "&scope=openid";
        return SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                tokenHeaders, form, Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
    }
}
