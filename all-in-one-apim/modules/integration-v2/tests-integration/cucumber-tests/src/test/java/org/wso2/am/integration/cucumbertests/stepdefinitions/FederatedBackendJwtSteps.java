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
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.IntegrationActors;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.SsoProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Drives the real external OIDC authorization-code path used by FederatedUserJWTTestCase. */
public class FederatedBackendJwtSteps {

    /** Upper bound on redirects/form posts in one federated authorization; the real chain is about ten hops. */
    private static final int MAX_AUTHORIZATION_HOPS = 25;

    @And("I register an external OIDC IdP for backend JWT in tenant {string}")
    public void registerExternalOidcIdp(String tenant) throws Exception {
        // FederatedUserJWTTestCase registers these non-standard OIDC claim names and adds them to the openid
        // scope. Without both pieces, APIM drops mobile/organization before it builds the backend JWT.
        org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner.addOidcExternalClaim(tenant,
                "mobile", "http://wso2.org/claims/mobile");
        org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner.addOidcExternalClaim(tenant,
                "organization", "http://wso2.org/claims/organization");
        org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner.addOidcExternalClaim(tenant,
                "phone_number", "http://wso2.org/claims/telephone");
        org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner.updateOidcScopeClaims(tenant, "openid",
                new String[] {"given_name", "family_name", "mobile", "organization", "phone_number", "email"});
        String[] creds = SsoProvisioner.createIsSsoApp(Names.unique("FED_JWT_IS_APP"), true);
        String idpName = Names.unique("FED_JWT_IDP");
        SsoProvisioner.registerOidcIdp(tenant, idpName, creds[0], creds[1], true);
        String user = Names.unique("federatedJwtUser");
        String password = "FederatedJwt@123";
        String email = user + "@wso2test.com";
        SsoProvisioner.createIsFederatedUser(user, password, email, java.util.List.of("subscriber"), true);
        // The IdP is left for block teardown: once the OAuth service provider references it, APIM refuses to delete
        // it until that SP is removed, so the pair goes with the block's container.
        TestContext.set("fedJwtIdpName", idpName);
        TestContext.set("fedJwtUser", user);
        TestContext.set("fedJwtEmail", email);
        TestContext.set("fedJwtPassword", password);
    }

    @And("I set the OAuth callback URI for this topology")
    public void setOAuthCallbackForTopology() {
        String callback = TestContext.get("blockApimContainer") instanceof DistributedDynamicApimContainer
                ? "https://apim-cp:9443/services/Version" : "https://localhost:9443/services/Version";
        TestContext.set("fedJwtCallback", callback);
    }

    @And("I configure OAuth consumer key {string} for federated OIDC authentication in tenant {string}")
    public void configureOAuthAppForFederation(String consumerKeyRef, String tenant) throws Exception {
        String consumerKey = TestContext.resolve(consumerKeyRef).toString();
        String provider = TestContext.resolve("fedJwtIdpName").toString();
        SsoProvisioner.configureOAuthApplicationForFederation(tenant, consumerKey, provider);
        String[] claimUris = {
                "http://wso2.org/claims/givenname", "http://wso2.org/claims/lastname",
                "http://wso2.org/claims/mobile", "http://wso2.org/claims/organization",
                "http://wso2.org/claims/emailaddress", "http://wso2.org/claims/telephone"
        };
        org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner
                // Claims returned by a federated provider are requested but optional: marking profile claims
                // mandatory makes APIM interrupt the external authorization-code flow with a claims-entry page
                // whenever the provider omits one, instead of completing the redirect for the existing identity.
                .addRequestedClaimsToServiceProvider(tenant, consumerKey, claimUris, false);
    }

    @When("I obtain an OAuth authorization-code token through the external OIDC IdP")
    public void obtainFederatedAuthorizationCodeToken() throws Exception {
        String callback = TestContext.resolve("fedJwtCallback").toString();
        String consumerKey = TestContext.resolve("consumerKey").toString();
        // Start at the same internal authority APIM uses for its own callback, so the APIM session cookie set by
        // the authorize leg is the one presented when the external IdP returns to APIM's /commonauth.
        String apimInternal = callback.substring(0, callback.indexOf("/services/Version"));
        String authorizeUrl = apimInternal + "/oauth2/authorize"
                + "?response_type=code&client_id=" + Utils.urlEncode(consumerKey)
                + "&redirect_uri=" + Utils.urlEncode(callback) + "&scope=openid";
        String finalRedirect = authorizeThroughExternalIs(authorizeUrl, callback,
                TestContext.resolve("fedJwtUser").toString(), TestContext.resolve("fedJwtPassword").toString());
        String code = Utils.queryParam(finalRedirect, "code");
        Assert.assertNotNull(code, "External OIDC authorization did not return a code: " + finalRedirect);

        String credentials = consumerKey + ":" + TestContext.resolve("consumerSecret");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)));
        String form = "grant_type=authorization_code&code=" + Utils.urlEncode(code)
                + "&redirect_uri=" + Utils.urlEncode(callback);
        HttpResponse response = Requests.post(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()), headers, form,
                "application/x-www-form-urlencoded");
        Assert.assertEquals(response.getResponseCode(), 200,
                "Federated authorization-code exchange failed: " + response.getData());
        JSONObject tokenResponse = new JSONObject(response.getData());
        TestContext.set("generatedAccessToken", tokenResponse.getString("access_token"));
        TestContext.set("tokenResponse", tokenResponse);
    }

    /**
     * Drives APIM's federated OIDC authorization-code flow headlessly, the way a browser would, and returns the final
     * client redirect that carries the authorization code: APIM {@code /oauth2/authorize} redirects to the external
     * Identity Server, the federated user signs in at the IS {@code /commonauth}, the IS returns its code to APIM's
     * {@code /commonauth}, and APIM redirects to the client callback. A consent challenge from either server is
     * approved.
     *
     * <p>The servers address each other by their INTERNAL authorities ({@code localhost:9443} / {@code apim-cp:9443}
     * for APIM, {@code wso2is:9443} for IS). Every hop keeps its internal URL as the logical address and is sent to
     * that server's host-mapped port. Cookies are kept per internal authority, as a browser keeps them per host:
     * both mapped ports are {@code localhost}, and a port-agnostic cookie store would let the IS session cookies
     * overwrite APIM's ({@code commonAuthId} is set by both).
     *
     * <p>Signing in twice fails the flow, so rejected credentials surface as a failure instead of looping.
     */
    private String authorizeThroughExternalIs(String authorizeUrl, String callbackUrl, String username,
                                              String password) throws Exception {

        Map<String, String> mappedBases = internalAuthorityBases();
        Map<String, Map<String, String>> cookiesByAuthority = new HashMap<>();
        String url = authorizeUrl;
        String formBody = null;
        boolean signedIn = false;
        try (CloseableHttpClient http = noRedirectTrustAllClient()) {
            for (int hop = 0; hop < MAX_AUTHORIZATION_HOPS; hop++) {
                URI logical = URI.create(url);
                String authority = logical.getRawAuthority();
                String mappedBase = mappedBases.get(authority);
                Assert.assertNotNull(mappedBase, "The federated authorization left the known servers at " + url);
                String target = mappedBase + logical.getRawPath().replaceFirst("^/", "")
                        + (logical.getRawQuery() == null ? "" : "?" + logical.getRawQuery());
                Map<String, String> cookies = cookiesByAuthority.computeIfAbsent(authority, k -> new LinkedHashMap<>());

                HttpUriRequest request;
                if (formBody != null) {
                    HttpPost post = new HttpPost(target);
                    post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    post.setEntity(new StringEntity(formBody, StandardCharsets.UTF_8));
                    request = post;
                } else {
                    request = new HttpGet(target);
                }
                if (!cookies.isEmpty()) {
                    request.setHeader("Cookie", String.join("; ", cookies.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue()).toList()));
                }
                int status;
                String location;
                String body;
                try (CloseableHttpResponse response = http.execute(request)) {
                    status = response.getStatusLine().getStatusCode();
                    for (Header setCookie : response.getHeaders("Set-Cookie")) {
                        storeCookie(cookies, setCookie.getValue());
                    }
                    Header locationHeader = response.getFirstHeader("Location");
                    location = locationHeader == null ? null : locationHeader.getValue();
                    body = response.getEntity() == null ? ""
                            : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
                Assert.assertTrue(status >= 300 && status < 400 && location != null,
                        "The federated authorization stopped at " + url + " with HTTP " + status + ": "
                                + body.substring(0, Math.min(body.length(), 500)));

                String next = logical.resolve(location).toString();
                String error = Utils.queryParam(next, "error");
                Assert.assertNull(error, "The federated authorization failed with error '" + error + "': "
                        + Utils.queryParam(next, "error_description"));
                if (next.startsWith(callbackUrl) && Utils.queryParam(next, "code") != null) {
                    return next;
                }

                URI nextUri = URI.create(next);
                formBody = null;
                if (nextUri.getPath() != null && nextUri.getPath().endsWith("/login.do")) {
                    Assert.assertEquals(nextUri.getRawAuthority(), "wso2is:9443",
                            "Expected the external Identity Server login, but " + nextUri.getRawAuthority()
                                    + " presented its own login page: " + next);
                    Assert.assertFalse(signedIn, "The external Identity Server asked for the login again after the "
                            + "federated user signed in (credentials rejected?): " + next);
                    String sessionDataKey = Utils.queryParam(next, "sessionDataKey");
                    Assert.assertNotNull(sessionDataKey, "No sessionDataKey on the external IS login: " + next);
                    url = nextUri.resolve("/commonauth").toString();
                    formBody = "username=" + Utils.urlEncode(username) + "&password=" + Utils.urlEncode(password)
                            + "&tocommonauth=true&sessionDataKey=" + Utils.urlEncode(sessionDataKey);
                    signedIn = true;
                } else if (Utils.queryParam(next, "sessionDataKeyConsent") != null) {
                    url = nextUri.resolve("/oauth2/authorize").toString();
                    formBody = "consent=approve&hasApprovedAlways=false&sessionDataKeyConsent="
                            + Utils.urlEncode(Utils.queryParam(next, "sessionDataKeyConsent"));
                } else {
                    url = next;
                }
            }
        }
        throw new AssertionError("The federated authorization did not reach '" + callbackUrl + "' within "
                + MAX_AUTHORIZATION_HOPS + " hops; last URL: " + url);
    }

    /** Maps each server's internal authority, and its host-mapped authority, to its host-mapped base URL. */
    private static Map<String, String> internalAuthorityBases() {
        String apimBase = TestContext.get("baseUrl").toString();
        String isBase = IntegrationActors.baseUrl(IntegrationActors.IS);
        Map<String, String> bases = new HashMap<>();
        bases.put("localhost:9443", apimBase);
        bases.put("wso2am:9443", apimBase);
        bases.put("apim-cp:9443", apimBase);
        bases.put("wso2is:9443", isBase);
        bases.put(URI.create(apimBase).getRawAuthority(), apimBase);
        bases.put(URI.create(isBase).getRawAuthority(), isBase);
        return bases;
    }

    /** Applies one Set-Cookie header to a per-authority jar; an expired or emptied cookie is removed. */
    private static void storeCookie(Map<String, String> cookies, String setCookie) {
        String pair = setCookie.split(";", 2)[0].trim();
        int eq = pair.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String name = pair.substring(0, eq);
        String value = pair.substring(eq + 1);
        String attributes = setCookie.toLowerCase(java.util.Locale.ROOT);
        if (value.isEmpty() || attributes.contains("max-age=0")) {
            cookies.remove(name);
        } else {
            cookies.put(name, value);
        }
    }

    /**
     * Trust-all, no-hostname-verification client that follows no redirects and keeps no cookies of its own (the
     * walk keeps them per internal authority). The shipped wso2carbon certificate is CN=localhost with no IP SAN.
     */
    private static CloseableHttpClient noRedirectTrustAllClient() throws Exception {
        return HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(Utils.trustAllSslContext(),
                        NoopHostnameVerifier.INSTANCE))
                .disableCookieManagement()
                .setDefaultRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build())
                .build();
    }
}
