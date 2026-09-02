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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Provisions the WSO2 IS 7.x side of the RFC 8693 token-exchange flow and the APIM-side trusted identity
 * provider that validates the subject token's signature. Two signature-validation shapes are supported and both
 * are exercised by the feature: a static PEM certificate and a JWKS endpoint.
 *
 * <p>Empirically-derived facts this class encodes (probed against APIM 9.33.147 + IS 7.3.0):
 * <ul>
 *   <li>Subject-token trust is a TRUSTED IdP registered on APIM's embedded IS (via SOAP {@code addIdP} - APIM
 *       exposes no REST IdP API), NOT the key-manager registration. The token-exchange grant handler resolves
 *       the IdP by the metadata property {@code idpIssuerName} = the subject token's {@code iss}.</li>
 *   <li>The IdP {@code alias} must be a value present in the subject token's {@code aud}. IS client-credentials
 *       JWTs carry {@code aud} = the client id, so the alias is set to the IS app's client id.</li>
 *   <li>PEM = the IdP's {@code certificate}; JWKS = an IdP metadata property {@code jwksUri} and no certificate.
 *       The PEM is taken from IS's own JWKS ({@code x5c[0]}), so it always matches the live signing key.</li>
 *   <li>The IS app must issue JWT (not opaque) access tokens - set {@code accessToken.type=JWT} via a PUT after
 *       create (inline in the create request fails with 500).</li>
 * </ul>
 */
public final class TokenExchangeProvisioner {

    /** Metadata-property name the token-exchange handler matches against the subject token's {@code iss}. */
    private static final String IDP_ISSUER_NAME = "idpIssuerName";
    private static final String JWKS_URI = "jwksUri";

    /**
     * Issuer / audience of the hand-signed multi-value-claim subject token (see
     * {@code TokenExchangeSteps#iObtainMultiValueSubjectJwt}). The subject token here is NOT minted by IS (an IS
     * client-credentials token carries no user claims); it is assembled and RS256-signed by the test with the
     * committed {@link #TRUSTED_IDP_CERT_RESOURCE} key pair, so its {@code iss}/{@code aud} are ours to pin. The
     * trusted IdP registered for it validates against that committed certificate (not IS's live JWKS).
     */
    public static final String MULTI_VALUE_IDP_ISSUER = "https://external-idp.apim.integration";
    public static final String MULTI_VALUE_IDP_ALIAS = "external-api";
    /** Local claim URIs the subject token's {@code groups} / {@code preferred_username} are mapped to (as legacy). */
    private static final String GROUPS_CLAIM_URI = "http://wso2.org/claims/groups";
    private static final String DISPLAY_NAME_CLAIM_URI = "http://wso2.org/claims/displayName";
    /** X.509 cert (and its PKCS#8 key) whose key pair signs the hand-crafted multi-value subject token. */
    private static final String TRUSTED_IDP_CERT_RESOURCE = "artifacts/certs/is7trustedidp/idp-cert.pem";
    public static final String TRUSTED_IDP_KEY_RESOURCE = "artifacts/certs/is7trustedidp/idp-key.pem";
    private static final String SOAP_ENV_OPEN =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">";
    private static final String SOAP_ENV_CLOSE = "</soapenv:Envelope>";

    private TokenExchangeProvisioner() {
    }

    /** The external IS management base URL, via the IS integration actor (fails fast if IS was not booted). */
    private static String isBase() {
        return IntegrationActors.baseUrl(IntegrationActors.IS);
    }

    private static String apimBase() {
        Object v = TestContext.get("baseUrl");
        if (v == null) {
            throw new IllegalStateException("baseUrl not in context; the block must be booted first");
        }
        return v.toString();
    }

    /** The IS integration actor's auth headers (CLAUDE.md §14 — IS's own principal, not an APIM actor). */
    private static Map<String, String> superAdminBasicAuth() {
        return IntegrationActors.authHeaders(IntegrationActors.IS);
    }

    /**
     * Creates an IS OIDC application with the client-credentials grant that issues JWT access tokens, and returns
     * {@code [clientId, clientSecret]}. The JWT token type is set with a follow-up PUT because the IS create API
     * rejects it inline (500).
     */
    public static String[] createIsJwtClientCredentialsApp(String appName) throws IOException {
        return createIsJwtClientCredentialsApp(appName, 0);
    }

    /**
     * As {@link #createIsJwtClientCredentialsApp(String)} but with a short application-access-token expiry (when
     * {@code expirySeconds > 0}), so a minted subject token can be exercised as an EXPIRED token.
     */
    public static String[] createIsJwtClientCredentialsApp(String appName, int expirySeconds) throws IOException {
        String base = isBase();
        String createPayload = new JSONObject()
                .put("name", appName)
                .put("templateId", "b9c5e11e-fc78-484b-9bec-015d247561b8")
                .put("inboundProtocolConfiguration", new JSONObject().put("oidc", new JSONObject()
                        .put("grantTypes", new JSONArray().put("client_credentials"))
                        .put("publicClient", false)))
                .toString();
        HttpResponse create = SimpleHTTPClient.getInstance().doPost(base + "api/server/v1/applications",
                superAdminBasicAuth(), createPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(create != null && create.getResponseCode() == 201,
                "IS token-exchange app create failed: got=" + (create == null ? "null"
                        : create.getResponseCode() + "/" + create.getData()));
        String appId = locationId(create);
        Assert.assertNotNull(appId, "Could not read created IS app id from Location header");
        // Register for the IS-side teardown sweep BEFORE the follow-up OIDC config calls, so an aborted
        // configuration still leaves the app swept (as the IS integration actor — see ISResourceCleanup).
        ISResourceCleanup.registerApplication(appId);

        HttpResponse oidcResp = SimpleHTTPClient.getInstance().doGet(
                base + "api/server/v1/applications/" + appId + "/inbound-protocols/oidc", superAdminBasicAuth());
        Assert.assertTrue(oidcResp != null && oidcResp.getResponseCode() == 200 && oidcResp.getData() != null
                        && !oidcResp.getData().isBlank(),
                "IS OIDC inbound fetch failed: got=" + (oidcResp == null ? "null"
                        : oidcResp.getResponseCode() + "/" + oidcResp.getData()));
        JSONObject oidc = new JSONObject(oidcResp.getData());
        oidc.getJSONObject("accessToken").put("type", "JWT");
        if (expirySeconds > 0) {
            oidc.getJSONObject("accessToken").put("applicationAccessTokenExpiryInSeconds", expirySeconds);
        }
        HttpResponse put = SimpleHTTPClient.getInstance().doPut(
                base + "api/server/v1/applications/" + appId + "/inbound-protocols/oidc",
                superAdminBasicAuth(), oidc.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(put != null && put.getResponseCode() >= 200 && put.getResponseCode() < 300,
                "IS OIDC JWT-token-type PUT failed: got=" + (put == null ? "null"
                        : put.getResponseCode() + "/" + put.getData()));
        return new String[]{oidc.getString("clientId"), oidc.getString("clientSecret")};
    }

    /** IS's advertised token issuer (the subject token's {@code iss}) on the shared network. */
    public static String isTokenIssuer() {
        return "https://" + org.wso2.am.testcontainers.DynamicISContainer.NETWORK_ALIAS + ":9443/oauth2/token";
    }

    /**
     * Registers (idempotently) the APIM-side trusted IdP for the PEM approach: {@code idpIssuerName} = IS's token
     * issuer, {@code alias} = the IS app client id (the subject token's audience), and the IS signing certificate
     * (fetched live from IS's JWKS {@code x5c}) as the IdP certificate.
     */
    public static void registerTrustedIdpPem(IdpScope scope, String idpName, String isAppClientId)
            throws IOException {
        registerTrustedIdpWithCert(scope, idpName, isAppClientId, fetchIsSigningCertBase64Der());
    }

    /**
     * Registers the PEM trusted IdP pinned to a certificate of a DIFFERENT key pair than IS's current signing key
     * (the committed test cert) - modelling the post-key-rotation state where the pinned certificate no longer
     * matches the live signer. A live subject token then fails PEM validation, while JWKS validation (which
     * re-fetches IS's keys) still succeeds - the key-rotation canary distinguishing the two approaches.
     */
    public static void registerTrustedIdpStalePem(IdpScope scope, String idpName, String isAppClientId)
            throws IOException {
        registerTrustedIdpWithCert(scope, idpName, isAppClientId, staleCertBase64Der());
    }

    private static void registerTrustedIdpWithCert(IdpScope scope, String idpName, String isAppClientId,
                                                   String certB64Der) throws IOException {
        String body = "<m:certificate>" + certB64Der + "</m:certificate>"
                + idpProperty(IDP_ISSUER_NAME, isTokenIssuer());
        addOrReplaceIdp(scope, idpName, isAppClientId, body);
    }

    /**
     * ATTEMPTS to register the trusted IdP with a certificate field that is NOT a certificate at all (base64 of
     * arbitrary bytes - e.g. what an operator gets pasting a PKCS12/keystore blob or truncated content instead
     * of the X.509 cert). Non-asserting: APIM validates the certificate at registration and REFUSES the IdP
     * (verified), so the caller asserts via {@link #trustedIdpExists} that nothing was created - the
     * wrong-format pitfall is caught at configuration time, before any exchange.
     */
    public static void attemptRegisterTrustedIdpMalformedCert(IdpScope scope, String idpName, String isAppClientId)
            throws IOException {
        deleteIdpIfExists(scope, idpName);
        String notACert = Base64.getEncoder().encodeToString(
                "this-is-not-an-x509-certificate".getBytes(StandardCharsets.UTF_8));
        String body = "<m:certificate>" + notACert + "</m:certificate>"
                + idpProperty(IDP_ISSUER_NAME, isTokenIssuer());
        soap(scope, "urn:addIdP", SOAP_ENV_OPEN
                + "<soapenv:Body><ns:addIdP xmlns:ns=\"http://mgt.idp.carbon.wso2.org\" "
                + "xmlns:m=\"http://model.common.application.identity.carbon.wso2.org/xsd\">"
                + "<ns:identityProvider>"
                + "<m:alias>" + Utils.escapeXml(isAppClientId) + "</m:alias>"
                + "<m:enable>true</m:enable>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + body
                + "</ns:identityProvider></ns:addIdP></soapenv:Body>" + SOAP_ENV_CLOSE);
    }

    /** Whether the named trusted IdP exists on APIM (for the malformed-cert registration-refused assertion). */
    public static boolean trustedIdpExists(IdpScope scope, String idpName) throws IOException {
        return idpExists(scope, idpName);
    }

    private static String staleCertBase64Der() throws IOException {
        // The committed CN=is7-jwt-bearer-test-idp certificate: a DIFFERENT key pair than IS's live signer, so
        // it models the stale-pin canary; it is the SAME cert whose key signs the hand-crafted multi-value
        // subject token, so registerMultiValueClaimIdp validates against it (see TRUSTED_IDP_CERT_RESOURCE).
        return certBase64Der(TRUSTED_IDP_CERT_RESOURCE);
    }

    /**
     * Registers (idempotently) the APIM-side trusted IdP for the JWKS approach: {@code idpIssuerName} = IS's
     * token issuer, {@code alias} = the IS app client id, and a {@code jwksUri} metadata property pointing at
     * IS's JWKS endpoint (no static certificate).
     */
    public static void registerTrustedIdpJwks(IdpScope scope, String idpName, String isAppClientId)
            throws IOException {
        String body = idpProperty(IDP_ISSUER_NAME, isTokenIssuer())
                + idpProperty(JWKS_URI, "https://"
                        + org.wso2.am.testcontainers.DynamicISContainer.NETWORK_ALIAS + ":9443/oauth2/jwks");
        addOrReplaceIdp(scope, idpName, isAppClientId, body);
    }

    /**
     * Registers the trusted IdP for the hand-signed multi-value-claim subject token: validates against the
     * committed {@link #TRUSTED_IDP_CERT_RESOURCE} certificate (whose key signs that token), pins
     * {@code idpIssuerName} = {@link #MULTI_VALUE_IDP_ISSUER} (the subject token's {@code iss}) and {@code alias}
     * = {@link #MULTI_VALUE_IDP_ALIAS} (present in its {@code aud}), and declares IdP claim mappings so the
     * federated {@code groups} / {@code preferred_username} remote claims are recognised and carried through.
     */
    public static void registerMultiValueClaimIdp(IdpScope scope, String idpName) throws IOException {
        String cert = certBase64Der(TRUSTED_IDP_CERT_RESOURCE);
        String claimConfig = "<m:claimConfig>"
                + idpClaimMapping("groups", GROUPS_CLAIM_URI)
                + idpClaimMapping("preferred_username", DISPLAY_NAME_CLAIM_URI)
                + "<m:idpClaims><m:claimUri>groups</m:claimUri></m:idpClaims>"
                + "<m:idpClaims><m:claimUri>preferred_username</m:claimUri></m:idpClaims>"
                + "<m:localClaimDialect>false</m:localClaimDialect>"
                + "</m:claimConfig>";
        String body = "<m:certificate>" + cert + "</m:certificate>"
                + idpProperty(IDP_ISSUER_NAME, MULTI_VALUE_IDP_ISSUER)
                + claimConfig;
        addOrReplaceIdp(scope, idpName, MULTI_VALUE_IDP_ALIAS, body);
    }

    private static String idpClaimMapping(String remoteClaim, String localClaimUri) {
        return "<m:claimMappings><m:localClaim><m:claimUri>" + Utils.escapeXml(localClaimUri) + "</m:claimUri>"
                + "</m:localClaim><m:remoteClaim><m:claimUri>" + Utils.escapeXml(remoteClaim) + "</m:claimUri>"
                + "</m:remoteClaim></m:claimMappings>";
    }

    /**
     * Configures the exchanging application's service provider (resolved from its Resident-KM consumer key) to
     * REQUEST and MANDATE the multi-value {@code groups} and single-valued {@code displayName} local claims, so
     * the token issued for that SP carries them - the v2 equivalent of the legacy
     * {@code applicationManagementClient.updateApplication} claim-config step (SOAP {@code
     * IdentityApplicationManagementService}; APIM exposes no REST for SP claim config).
     *
     * <p>Reads the SP's numeric applicationID and its existing OAuth inbound-auth key, then re-sends an {@code
     * updateApplication} carrying only those plus the claim config - preserving the inbound OAuth wiring the
     * exchange depends on (clobbering it would break the exchange) rather than round-tripping the whole SP XML
     * (whose axis2 namespace prefixes are unstable across payloads).
     */
    public static void requestSubjectClaimsOnApp(IdpScope scope, String consumerKey) throws IOException {
        String appName = oauthAppName(scope, consumerKey);
        String appXml = getApplicationXml(scope, appName);
        String appId = between(appXml, "applicationID>", "</");
        Assert.assertNotNull(appId, "Could not read applicationID for SP '" + appName + "': " + appXml);
        String updateBody = SOAP_ENV_OPEN
                + "<soapenv:Body><axis2:updateApplication xmlns:axis2=\"http://org.apache.axis2/xsd\">"
                + "<axis2:serviceProvider "
                + "xmlns:m=\"http://model.common.application.identity.carbon.wso2.org/xsd\">"
                + "<m:applicationID>" + Utils.escapeXml(appId) + "</m:applicationID>"
                + "<m:applicationName>" + Utils.escapeXml(appName) + "</m:applicationName>"
                + "<m:claimConfig>"
                + "<m:localClaimDialect>true</m:localClaimDialect>"
                + spRequestedClaim(GROUPS_CLAIM_URI)
                + spRequestedClaim(DISPLAY_NAME_CLAIM_URI)
                + "</m:claimConfig>"
                + "<m:inboundAuthenticationConfig><m:inboundAuthenticationRequestConfigs>"
                + "<m:inboundAuthKey>" + Utils.escapeXml(consumerKey) + "</m:inboundAuthKey>"
                + "<m:inboundAuthType>oauth2</m:inboundAuthType>"
                + "</m:inboundAuthenticationRequestConfigs></m:inboundAuthenticationConfig>"
                + "</axis2:serviceProvider></axis2:updateApplication></soapenv:Body>" + SOAP_ENV_CLOSE;
        HttpResponse r = appSoap(scope, "urn:updateApplication", updateBody);
        Assert.assertTrue(r != null && r.getResponseCode() >= 200 && r.getResponseCode() < 300
                        && r.getData() != null && !r.getData().contains("faultstring"),
                "Configuring requested claims on SP '" + appName + "' failed: got="
                        + (r == null ? "null" : r.getResponseCode() + "/" + r.getData()));
    }

    private static String spRequestedClaim(String uri) {
        return "<m:claimMappings>"
                + "<m:localClaim><m:claimUri>" + Utils.escapeXml(uri) + "</m:claimUri></m:localClaim>"
                + "<m:remoteClaim><m:claimUri>" + Utils.escapeXml(uri) + "</m:claimUri></m:remoteClaim>"
                + "<m:requested>true</m:requested><m:mandatory>true</m:mandatory>"
                + "</m:claimMappings>";
    }

    /** Reads the OAuth app (SP) name for a consumer key via the {@code OAuthAdminService}. */
    private static String oauthAppName(IdpScope scope, String consumerKey) throws IOException {
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns:getOAuthApplicationData xmlns:ns=\"http://org.apache.axis2/xsd\">"
                + "<ns:consumerKey>" + Utils.escapeXml(consumerKey) + "</ns:consumerKey>"
                + "</ns:getOAuthApplicationData></soapenv:Body>" + SOAP_ENV_CLOSE;
        HttpResponse r = SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + scope.pathSegment + "services/OAuthAdminService", body,
                "urn:getOAuthApplicationData", scope.adminUser, scope.adminPass);
        Assert.assertTrue(r != null && r.getData() != null && !r.getData().isBlank(),
                "OAuthAdminService getOAuthApplicationData failed for key " + consumerKey);
        String name = between(r.getData(), "applicationName>", "</");
        Assert.assertNotNull(name, "Could not read SP name for consumer key " + consumerKey + ": " + r.getData());
        return name;
    }

    /** Fetches the full SP XML via {@code IdentityApplicationManagementService.getApplication(name)}. */
    private static String getApplicationXml(IdpScope scope, String appName) throws IOException {
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns1:getApplication xmlns:ns1=\"http://org.apache.axis2/xsd\">"
                + "<ns1:applicationName>" + Utils.escapeXml(appName) + "</ns1:applicationName>"
                + "</ns1:getApplication></soapenv:Body>" + SOAP_ENV_CLOSE;
        HttpResponse r = appSoap(scope, "urn:getApplication", body);
        Assert.assertTrue(r != null && r.getData() != null && r.getData().contains("getApplicationResponse"),
                "IdentityApplicationManagementService getApplication failed for '" + appName + "': got="
                        + (r == null ? "null" : r.getResponseCode() + "/" + r.getData()));
        return r.getData();
    }

    private static HttpResponse appSoap(IdpScope scope, String action, String body) throws IOException {
        return SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + scope.pathSegment + "services/IdentityApplicationManagementService", body, action,
                scope.adminUser, scope.adminPass);
    }

    private static String between(String s, String start, String end) {
        int i = s.indexOf(start);
        if (i < 0) {
            return null;
        }
        i += start.length();
        int j = s.indexOf(end, i);
        return j < 0 ? null : s.substring(i, j);
    }

    private static String certBase64Der(String resource) throws IOException {
        String pem = Utils.readClasspathResource(resource);
        return pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "").replaceAll("\\s", "");
    }

    /** Deletes the named IdP if present, then creates it (so PEM/JWKS scenarios never collide on idpIssuerName). */
    private static void addOrReplaceIdp(IdpScope scope, String idpName, String alias, String extraBody)
            throws IOException {
        deleteIdpIfExists(scope, idpName);
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns:addIdP xmlns:ns=\"http://mgt.idp.carbon.wso2.org\" "
                + "xmlns:m=\"http://model.common.application.identity.carbon.wso2.org/xsd\">"
                + "<ns:identityProvider>"
                + "<m:alias>" + Utils.escapeXml(alias) + "</m:alias>"
                + "<m:enable>true</m:enable>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + extraBody
                + "</ns:identityProvider></ns:addIdP></soapenv:Body>" + SOAP_ENV_CLOSE;
        soap(scope, "urn:addIdP", body);
        Assert.assertTrue(idpExists(scope, idpName), "Trusted IdP '" + idpName + "' was not created on APIM");
    }

    private static void deleteIdpIfExists(IdpScope scope, String idpName) throws IOException {
        if (!idpExists(scope, idpName)) {
            return;
        }
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns:deleteIdP xmlns:ns=\"http://mgt.idp.carbon.wso2.org\">"
                + "<ns:idPName>" + Utils.escapeXml(idpName) + "</ns:idPName></ns:deleteIdP></soapenv:Body>" + SOAP_ENV_CLOSE;
        soap(scope, "urn:deleteIdP", body);
    }

    private static boolean idpExists(IdpScope scope, String idpName) throws IOException {
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns:getIdPByName xmlns:ns=\"http://mgt.idp.carbon.wso2.org\">"
                + "<ns:idPName>" + Utils.escapeXml(idpName) + "</ns:idPName></ns:getIdPByName></soapenv:Body>" + SOAP_ENV_CLOSE;
        HttpResponse r = soap(scope, "urn:getIdPByName", body);
        // A transport-level failure must FAIL the existence check, never read as "absent": the caller's
        // negative assertion (malformed-cert registration refused -> IdP should not exist) would false-pass
        // on a broken SOAP channel. A real body lacking the name (incl. an empty getIdPByNameResponse for a
        // missing IdP) genuinely means absent.
        if (r == null || r.getData() == null || r.getData().isBlank()) {
            throw new IllegalStateException("IdP existence check failed for '" + idpName + "': got "
                    + (r == null ? "no response" : r.getResponseCode() + " / blank body"));
        }
        return r.getData().contains("identityProviderName>" + idpName);
    }

    /** Deletes the named IdP (used by teardown). */
    public static void deleteIdp(IdpScope scope, String idpName) throws IOException {
        deleteIdpIfExists(scope, idpName);
    }

    /**
     * Fetches IS's token-signing certificate live from its JWKS endpoint and returns the base64 DER of the first
     * key's {@code x5c[0]} - the exact cert that signs the subject tokens, so PEM validation always matches the
     * live key (and avoids committing a container cert).
     */
    private static String fetchIsSigningCertBase64Der() throws IOException {
        HttpResponse r = SimpleHTTPClient.getInstance().doGet(isBase() + "oauth2/jwks", new HashMap<>());
        Assert.assertTrue(r != null && r.getResponseCode() == 200 && r.getData() != null
                        && !r.getData().isBlank(),
                "Fetching IS JWKS failed: got=" + (r == null ? "null" : r.getResponseCode() + "/" + r.getData()));
        JSONArray keys = new JSONObject(r.getData()).getJSONArray("keys");
        for (int i = 0; i < keys.length(); i++) {
            JSONObject key = keys.getJSONObject(i);
            if (key.has("x5c") && key.getJSONArray("x5c").length() > 0) {
                return key.getJSONArray("x5c").getString(0);
            }
        }
        throw new IllegalStateException("IS JWKS has no x5c certificate to use for PEM validation: " + r.getData());
    }

    private static String idpProperty(String name, String value) {
        return "<m:idpProperties><m:name>" + name + "</m:name><m:value>" + Utils.escapeXml(value) + "</m:value></m:idpProperties>";
    }

    /**
     * The tenant whose embedded-IS IdP registry a trusted-IdP operation targets: the SOAP admin-service path
     * segment ({@code ""} for the super tenant, {@code "t/<domain>/"} for a tenant) and that tenant's admin
     * credentials. Built from the acting actor so PEM/JWKS/exchange scenarios register the trusted IdP in the
     * same tenant they exchange in.
     */
    public static final class IdpScope {
        private final String pathSegment;
        private final String adminUser;
        private final String adminPass;

        private IdpScope(String pathSegment, String adminUser, String adminPass) {
            this.pathSegment = pathSegment;
            this.adminUser = adminUser;
            this.adminPass = adminPass;
        }

        /** @param tenantDomain the tenant ({@code carbon.super} or e.g. {@code tenant1.com}) */
        public static IdpScope of(String tenantDomain, String adminUser, String adminPass) {
            boolean superTenant = tenantDomain == null || tenantDomain.isBlank()
                    || Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain);
            return new IdpScope(superTenant ? "" : "t/" + tenantDomain + "/", adminUser, adminPass);
        }
    }

    private static HttpResponse soap(IdpScope scope, String action, String body) throws IOException {
        return SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + scope.pathSegment + "services/IdentityProviderMgtService", body, action,
                scope.adminUser, scope.adminPass);
    }

    private static String locationId(HttpResponse resp) {
        if (resp == null || resp.getHeaders() == null) {
            return null;
        }
        String location = resp.getHeaders().get("Location");
        if (location == null) {
            location = resp.getHeaders().get("location");
        }
        if (location == null) {
            return null;
        }
        int slash = location.lastIndexOf('/');
        return slash >= 0 ? location.substring(slash + 1) : location;
    }
}
