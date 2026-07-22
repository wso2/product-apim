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
    private static final String SOAP_ENV_OPEN =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">";
    private static final String SOAP_ENV_CLOSE = "</soapenv:Envelope>";

    private TokenExchangeProvisioner() {
    }

    private static String isBase() {
        Object v = TestContext.get("isBaseUrl");
        if (v == null) {
            throw new IllegalStateException("isBaseUrl not in context; the block must set "
                    + "bootExternalIdentityServer=true so the external Identity Server is started");
        }
        return v.toString();
    }

    private static String apimBase() {
        Object v = TestContext.get("baseUrl");
        if (v == null) {
            throw new IllegalStateException("baseUrl not in context; the block must be booted first");
        }
        return v.toString();
    }

    private static Map<String, String> superAdminBasicAuth() {
        Map<String, String> h = new HashMap<>();
        h.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(
                (Constants.SUPER_TENANT_ADMIN_USERNAME + ":" + Constants.SUPER_TENANT_ADMIN_PASSWORD)
                        .getBytes(StandardCharsets.UTF_8)));
        return h;
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

        HttpResponse oidcResp = SimpleHTTPClient.getInstance().doGet(
                base + "api/server/v1/applications/" + appId + "/inbound-protocols/oidc", superAdminBasicAuth());
        Assert.assertTrue(oidcResp != null && oidcResp.getResponseCode() == 200 && oidcResp.getData() != null,
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
        return "https://" + org.wso2.am.testcontainers.IdentityServerContainer.NETWORK_ALIAS + ":9443/oauth2/token";
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
                + "<m:alias>" + isAppClientId + "</m:alias>"
                + "<m:enable>true</m:enable>"
                + "<m:identityProviderName>" + idpName + "</m:identityProviderName>"
                + body
                + "</ns:identityProvider></ns:addIdP></soapenv:Body>" + SOAP_ENV_CLOSE);
    }

    /** Whether the named trusted IdP exists on APIM (for the malformed-cert registration-refused assertion). */
    public static boolean trustedIdpExists(IdpScope scope, String idpName) throws IOException {
        return idpExists(scope, idpName);
    }

    /** Committed different-key-pair certificate (CN=is7-jwt-bearer-test-idp), as base64 DER, for the stale pin. */
    private static final String STALE_CERT_RESOURCE = "artifacts/certs/is7trustedidp/idp-cert.pem";

    private static String staleCertBase64Der() throws IOException {
        try (java.io.InputStream in = TokenExchangeProvisioner.class.getClassLoader()
                .getResourceAsStream(STALE_CERT_RESOURCE)) {
            if (in == null) {
                throw new java.io.FileNotFoundException("Stale-cert resource not found: " + STALE_CERT_RESOURCE);
            }
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return pem.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "").replaceAll("\\s", "");
        }
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
                        + org.wso2.am.testcontainers.IdentityServerContainer.NETWORK_ALIAS + ":9443/oauth2/jwks");
        addOrReplaceIdp(scope, idpName, isAppClientId, body);
    }

    /** Deletes the named IdP if present, then creates it (so PEM/JWKS scenarios never collide on idpIssuerName). */
    private static void addOrReplaceIdp(IdpScope scope, String idpName, String alias, String extraBody)
            throws IOException {
        deleteIdpIfExists(scope, idpName);
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns:addIdP xmlns:ns=\"http://mgt.idp.carbon.wso2.org\" "
                + "xmlns:m=\"http://model.common.application.identity.carbon.wso2.org/xsd\">"
                + "<ns:identityProvider>"
                + "<m:alias>" + alias + "</m:alias>"
                + "<m:enable>true</m:enable>"
                + "<m:identityProviderName>" + idpName + "</m:identityProviderName>"
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
                + "<ns:idPName>" + idpName + "</ns:idPName></ns:deleteIdP></soapenv:Body>" + SOAP_ENV_CLOSE;
        soap(scope, "urn:deleteIdP", body);
    }

    private static boolean idpExists(IdpScope scope, String idpName) throws IOException {
        String body = SOAP_ENV_OPEN
                + "<soapenv:Body><ns:getIdPByName xmlns:ns=\"http://mgt.idp.carbon.wso2.org\">"
                + "<ns:idPName>" + idpName + "</ns:idPName></ns:getIdPByName></soapenv:Body>" + SOAP_ENV_CLOSE;
        HttpResponse r = soap(scope, "urn:getIdPByName", body);
        return r != null && r.getData() != null && r.getData().contains("identityProviderName>" + idpName);
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
        Assert.assertTrue(r != null && r.getResponseCode() == 200 && r.getData() != null,
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
        return "<m:idpProperties><m:name>" + name + "</m:name><m:value>" + value + "</m:value></m:idpProperties>";
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
