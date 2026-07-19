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

import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Step definitions for the JWT (jwt-bearer) grant against APIM's resident key manager with a registered trusted
 * IdP. Ports JWTGrantTestCase. Registers/updates a trusted IdP over the Carbon IdentityProviderMgtService SOAP
 * admin service, mints RS256-signed JWT assertions from a committed test keystore (JDK crypto only — no extra
 * dependency), and exchanges them at {@code /oauth2/token} with grant_type
 * {@code urn:ietf:params:oauth:grant-type:jwt-bearer}. Kept in this NEW class so shared step classes are untouched.
 */
public class JwtGrantSteps {

    private static final String JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String IDP_MGT_NS = "http://mgt.idp.carbon.wso2.org";
    private static final String IDP_MODEL_NS = "http://model.common.application.identity.carbon.wso2.org/xsd";

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    private static String b64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private KeyStore loadKeyStore(String classpathResource, String storePass) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(classpathResource)) {
            Assert.assertNotNull(in, "Keystore not found on classpath: " + classpathResource);
            ks.load(in, storePass.toCharArray());
        }
        return ks;
    }

    /**
     * Mints an RS256-signed JWT assertion from a committed JKS keystore and stores it under {@code targetKey}.
     * Header carries alg/typ/kid/x5t(SHA-1 thumbprint); body carries iss/sub/aud/azp/iat/nbf/exp/jti plus any
     * extra claims. {@code notBeforeOffsetMillis} shifts nbf/exp (negative → expired token). {@code extraClaimsJson}
     * (may be empty) adds claims, e.g. an IdP role claim for the role-mapped-scope flow.
     */
    @When("I mint a signed JWT from keystore {string} pass {string} alias {string} with issuer {string} audience {string} subject {string} notBeforeOffsetMillis {long} extraClaims {string} as {string}")
    public void iMintSignedJwt(String keystore, String storePass, String alias, String issuer, String audience,
                               String subject, long notBeforeOffsetMillis, String extraClaimsJson, String targetKey)
            throws Exception {
        // Resolve any {{contextKey}} placeholders (issuer/audience are minted uniquely per scenario).
        issuer = Utils.resolveContextPlaceholders(issuer);
        audience = Utils.resolveContextPlaceholders(audience);
        subject = Utils.resolveContextPlaceholders(subject);
        extraClaimsJson = Utils.resolveContextPlaceholders(extraClaimsJson);
        KeyStore ks = loadKeyStore(keystore, storePass);
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, storePass.toCharArray());
        Assert.assertNotNull(privateKey, "No private key for alias '" + alias + "' in " + keystore);
        Certificate cert = ks.getCertificate(alias);
        byte[] thumb = MessageDigest.getInstance("SHA-1").digest(cert.getEncoded());

        JSONObject header = new JSONObject().put("alg", "RS256").put("typ", "JWT")
                .put("kid", UUID.randomUUID().toString()).put("x5t", b64url(thumb));
        long now = System.currentTimeMillis();
        long nbf = now + notBeforeOffsetMillis;
        JSONObject body = new JSONObject()
                .put("iss", issuer).put("sub", subject).put("aud", audience).put("azp", audience)
                .put("iat", now / 1000).put("nbf", nbf / 1000).put("exp", (nbf + 15 * 60 * 1000) / 1000)
                .put("jti", UUID.randomUUID().toString());
        if (extraClaimsJson != null && !extraClaimsJson.trim().isEmpty()) {
            JSONObject extra = new JSONObject(extraClaimsJson);
            for (String k : extra.keySet()) {
                body.put(k, extra.get(k));
            }
        }
        String signingInput = b64url(header.toString().getBytes(StandardCharsets.UTF_8)) + "."
                + b64url(body.toString().getBytes(StandardCharsets.UTF_8));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        String jwt = signingInput + "." + b64url(sig.sign());
        TestContext.set(targetKey, jwt);
    }

    /**
     * Produces a TAMPERED copy of a signed JWT: decodes the payload, replaces {@code oldSub} with {@code newSub},
     * re-encodes the payload but keeps the ORIGINAL signature — so the signature no longer matches the body.
     */
    @When("I tamper the JWT {string} replacing subject {string} with {string} as {string}")
    public void iTamperJwt(String jwtKey, String oldSub, String newSub, String targetKey) {
        String jwt = TestContext.resolve(jwtKey).toString();
        String[] parts = jwt.split("\\.");
        Assert.assertEquals(parts.length, 3, "Not a signed JWT: " + jwt);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        payload = payload.replace(oldSub, newSub);
        String tampered = parts[0] + "." + b64url(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];
        TestContext.set(targetKey, tampered);
    }

    /**
     * Registers a trusted IdP for JWT-grant validation via the IdentityProviderMgtService {@code addIdP} SOAP
     * operation: an enabled IdP named {@code issuer} (the JWT iss), with {@code alias} (the JWT aud) and the
     * signing {@code certificate} (base64 DER, from the keystore's public cert). addIdP can return a 500-styled
     * fault even on success, so this does NOT assert the addIdP response — the feature verifies with getIdPByName.
     */
    @When("I register a trusted IdP named {string} with alias {string} using cert from keystore {string} pass {string} alias {string}")
    public void iRegisterTrustedIdp(String issuer, String audience, String keystore, String storePass,
                                    String certAlias) throws Exception {
        issuer = Utils.resolveContextPlaceholders(issuer);
        audience = Utils.resolveContextPlaceholders(audience);
        KeyStore ks = loadKeyStore(keystore, storePass);
        String certB64 = Base64.getEncoder().encodeToString(ks.getCertificate(certAlias).getEncoded());
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:mgt=\"" + IDP_MGT_NS + "\" xmlns:xsd=\"" + IDP_MODEL_NS + "\"><soapenv:Header/><soapenv:Body>"
                + "<mgt:addIdP><mgt:identityProvider>"
                + "<xsd:alias>" + audience + "</xsd:alias>"
                + "<xsd:certificate>" + certB64 + "</xsd:certificate>"
                + "<xsd:displayName>jwtgrant_idp</xsd:displayName>"
                + "<xsd:enable>true</xsd:enable>"
                + "<xsd:identityProviderName>" + issuer + "</xsd:identityProviderName>"
                + "<xsd:primary>false</xsd:primary>"
                + "</mgt:identityProvider></mgt:addIdP></soapenv:Body></soapenv:Envelope>";
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                Utils.getIdentityProviderMgtServiceURL(getBaseUrl()), envelope, "urn:addIdP",
                Identity.actingActor().getUserName(), Identity.actingActor().getPassword());
        TestContext.set("httpResponse", response);
    }

    /** Verifies a trusted IdP exists by name via getIdPByName; publishes the response for assertion. */
    @When("I retrieve the trusted IdP named {string}")
    public void iRetrieveTrustedIdp(String issuer) throws Exception {
        issuer = Utils.resolveContextPlaceholders(issuer);
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:mgt=\"" + IDP_MGT_NS + "\"><soapenv:Header/><soapenv:Body>"
                + "<mgt:getIdPByName><mgt:idPName>" + issuer + "</mgt:idPName></mgt:getIdPByName>"
                + "</soapenv:Body></soapenv:Envelope>";
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                Utils.getIdentityProviderMgtServiceURL(getBaseUrl()), envelope, "urn:getIdPByName",
                Identity.actingActor().getUserName(), Identity.actingActor().getPassword());
        TestContext.set("httpResponse", response);
    }

    /**
     * Exchanges a signed JWT assertion for a token via the jwt-bearer grant at {@code /oauth2/token}, authenticated
     * with the application's Basic credentials (consumerKey/consumerSecret from context). Optional space-separated
     * {@code scope} (empty for none). Publishes the token response for assertion.
     */
    @When("I exchange the JWT assertion {string} for a token using consumer key {string} secret {string} scope {string}")
    public void iExchangeJwtForToken(String jwtKey, String ckRef, String csRef, String scope) throws Exception {
        String jwt = TestContext.resolve(jwtKey).toString();
        String ck = TestContext.resolve(ckRef).toString();
        String cs = TestContext.resolve(csRef).toString();
        scope = Utils.resolveContextPlaceholders(scope);
        String creds = Base64.getEncoder().encodeToString((ck + ":" + cs).getBytes(StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder("grant_type=")
                .append(java.net.URLEncoder.encode(JWT_BEARER_GRANT, StandardCharsets.UTF_8))
                .append("&assertion=").append(java.net.URLEncoder.encode(jwt, StandardCharsets.UTF_8));
        if (scope != null && !scope.trim().isEmpty()) {
            body.append("&scope=").append(java.net.URLEncoder.encode(scope, StandardCharsets.UTF_8));
        }
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + creds);
        Requests.post(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers, body.toString(),
                "application/x-www-form-urlencoded");
    }

    /**
     * Generates a runner-unique value from {@code base} (via {@link Names#unique}) and stores it under
     * {@code targetKey}. Used to give each scenario a distinct IdP issuer/audience so parallel runners never
     * register a colliding Carbon IdP. (The generic "put value" step does not expand {@code ${UNIQUE:}}, and
     * the trusted-IdP name is not a JSON payload, so uniqueness is minted here.)
     */
    @When("I put a unique value from base {string} in context as {string}")
    public void iPutUniqueValueInContext(String base, String targetKey) {
        TestContext.set(Utils.normalizeContextKey(targetKey), Names.unique(base));
    }

    /**
     * Creates a shared scope whose access is restricted to a single local {@code role} binding, under a
     * runner-unique name (the role-mapped-scope flow needs a scope gated by the local 'admin' role). Mirrors
     * the positive shared-scope create in PublisherBaseSteps (publisher token, same endpoint, id registered
     * for teardown) but sets the binding — kept here as a NEW self-contained step so the shared step class is
     * untouched. Publishes the create response for assertion.
     */
    @When("I create a new shared scope as {string} bound to role {string}")
    public void iCreateSharedScopeBoundToRole(String scopeName, String role) throws Exception {
        String uniqueName = Names.unique(scopeName);
        JSONObject payload = new JSONObject()
                .put("name", uniqueName)
                .put("displayName", uniqueName)
                .put("description", "JWT grant role-mapped scope")
                .put("bindings", new org.json.JSONArray().put(role));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.post(Utils.getAPIScopes(getBaseUrl()), headers, payload.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object scopeId = Utils.extractValueFromPayload(response.getData(), "id");
            if (scopeId != null) {
                ResourceCleanup.register(Constants.CREATED_SHARED_SCOPE_IDS, scopeId);
            }
        }
    }

    /**
     * Updates a registered trusted IdP with a role mapping so an external IdP role claim maps onto a local
     * role, enabling role-restricted scope issuance through the JWT grant. Mirrors
     * JWTGrantTestCase#updateIdentityProviderWithRoleMappings: sets a claim mapping (remote role claim -> local
     * role claim {@code http://wso2.org/claims/role}) and a role mapping ({@code remoteRole -> localRole}), then
     * PUTs the full IdP model via the {@code updateIdP} SOAP operation (oldIdPName + the identityProvider with
     * its name/alias/cert re-sent plus claimConfig and permissionAndRoleConfig). Cert is re-loaded from the
     * keystore so the model stays complete. Publishes the response for assertion.
     */
    @When("I update the trusted IdP named {string} with alias {string} cert from keystore {string} pass {string} alias {string} adding role mapping from remote role {string} to local role {string} with remote role claim {string}")
    public void iUpdateTrustedIdpWithRoleMapping(String issuer, String audience, String keystore, String storePass,
                                                 String certAlias, String remoteRole, String localRole,
                                                 String remoteRoleClaim) throws Exception {
        issuer = Utils.resolveContextPlaceholders(issuer);
        audience = Utils.resolveContextPlaceholders(audience);
        KeyStore ks = loadKeyStore(keystore, storePass);
        String certB64 = Base64.getEncoder().encodeToString(ks.getCertificate(certAlias).getEncoded());
        String localRoleClaim = "http://wso2.org/claims/role";
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:mgt=\"" + IDP_MGT_NS + "\" xmlns:xsd=\"" + IDP_MODEL_NS + "\"><soapenv:Header/><soapenv:Body>"
                + "<mgt:updateIdP>"
                + "<mgt:oldIdPName>" + issuer + "</mgt:oldIdPName>"
                + "<mgt:identityProvider>"
                + "<xsd:alias>" + audience + "</xsd:alias>"
                + "<xsd:certificate>" + certB64 + "</xsd:certificate>"
                + "<xsd:claimConfig>"
                + "<xsd:claimMappings>"
                + "<xsd:localClaim><xsd:claimUri>" + localRoleClaim + "</xsd:claimUri></xsd:localClaim>"
                + "<xsd:remoteClaim><xsd:claimUri>" + remoteRoleClaim + "</xsd:claimUri></xsd:remoteClaim>"
                + "</xsd:claimMappings>"
                + "<xsd:idpClaims><xsd:claimUri>" + remoteRoleClaim + "</xsd:claimUri></xsd:idpClaims>"
                + "<xsd:roleClaimURI>" + remoteRoleClaim + "</xsd:roleClaimURI>"
                + "</xsd:claimConfig>"
                + "<xsd:displayName>jwtgrant_idp</xsd:displayName>"
                + "<xsd:enable>true</xsd:enable>"
                + "<xsd:identityProviderName>" + issuer + "</xsd:identityProviderName>"
                + "<xsd:permissionAndRoleConfig>"
                + "<xsd:idpRoles>" + remoteRole + "</xsd:idpRoles>"
                + "<xsd:roleMappings>"
                + "<xsd:localRole><xsd:localRoleName>" + localRole + "</xsd:localRoleName></xsd:localRole>"
                + "<xsd:remoteRole>" + remoteRole + "</xsd:remoteRole>"
                + "</xsd:roleMappings>"
                + "</xsd:permissionAndRoleConfig>"
                + "<xsd:primary>false</xsd:primary>"
                + "</mgt:identityProvider></mgt:updateIdP></soapenv:Body></soapenv:Envelope>";
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                Utils.getIdentityProviderMgtServiceURL(getBaseUrl()), envelope, "urn:updateIdP",
                Identity.actingActor().getUserName(), Identity.actingActor().getPassword());
        TestContext.set("httpResponse", response);
    }
}
