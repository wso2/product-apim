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

import org.json.JSONObject;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Test-side JWT plumbing shared by the grant/token-exchange/key-manager glue: base64url segment handling,
 * RS256 assertion signing, signature-invalidating tamper helpers, and SIGNATURE VERIFICATION of a
 * product-issued assertion. Consolidates the per-file copies that previously lived in ApplicationBaseSteps,
 * TokenExchangeSteps and JwtGrantSteps.
 *
 * <p>Verification lives here beside the assembly helpers because both operate on the same segment/key
 * primitives. It is NOT a re-implementation of the product's validation: it exists so a test can prove the
 * gateway-issued backend JWT is cryptographically sound, which no payload/header assertion can do (a token
 * with {@code alg=RS256} and a garbage signature satisfies every one of them). The verification key must be
 * obtained at RUNTIME from the product under test (its JWKS endpoint) — never a committed PEM/keystore
 * copy, which would drift from the key the product actually signs with.
 */
public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    /** Base64url-encodes (unpadded) raw bytes — signature, digest or thumbprint material. */
    public static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Base64url-encodes (unpadded) a JSON/string segment for JWT assembly. */
    public static String base64Url(String segment) {
        return base64Url(segment.getBytes(StandardCharsets.UTF_8));
    }

    /** Decodes a JWT's header (segment 0) to its JSON text. Throws when the token is not dot-separated. */
    public static String decodeHeader(String jwt) {
        return decodeSegment(jwt, 0);
    }

    /** Decodes a JWT's payload (segment 1) to its JSON text. Throws when the token is not dot-separated. */
    public static String decodePayload(String jwt) {
        return decodeSegment(jwt, 1);
    }

    private static String decodeSegment(String jwt, int index) {
        String[] parts = jwt.split("\\.");
        if (parts.length <= index) {
            throw new IllegalArgumentException("Token has only " + parts.length
                    + " dot-separated segment(s), cannot read segment " + index + ": " + jwt);
        }
        return new String(Base64.getUrlDecoder().decode(parts[index]), StandardCharsets.UTF_8);
    }

    /** Loads an RSA private key from PKCS#8 PEM text (strips the BEGIN/END armour and whitespace). */
    public static PrivateKey rsaPrivateKeyFromPem(String pkcs8Pem) {
        byte[] der = Base64.getMimeDecoder().decode(
                pkcs8Pem.replaceAll("-----[A-Z ]+-----", ""));
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to load RSA private key from PKCS#8 PEM", e);
        }
    }

    /** RS256-signs the signing input ({@code base64url(header).base64url(claims)}), returning the signature segment. */
    public static String signRs256(String signingInput, PrivateKey privateKey) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return base64Url(signer.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to RS256-sign the JWT signing input", e);
        }
    }

    /** Assembles a complete RS256-signed JWT from header/claims JSON and the signing key. */
    public static String buildRs256Jwt(String headerJson, String claimsJson, PrivateKey privateKey) {
        String signingInput = base64Url(headerJson) + "." + base64Url(claimsJson);
        return signingInput + "." + signRs256(signingInput, privateKey);
    }

    /**
     * Builds the JWT-format APIM API key used by the legacy AI API invocation test. This is intentionally
     * different from a DevPortal-generated opaque API key: it is signed by the standard APIM test keystore and
     * carries the application claims consumed by the gateway's API-key validator.
     */
    public static String buildLegacyJwtApiKey(String username, String issuer, String applicationName,
                                               String applicationTier, int applicationId, String applicationUuid,
                                               String applicationOwner) {
        return buildLegacyJwtApiKey(username, issuer, applicationName, applicationTier, applicationId,
                applicationUuid, applicationOwner, "PRODUCTION", null, null);
    }

    /**
     * Builds the JWT-format APIM API key with an explicit key type and the optional {@code permittedIP} /
     * {@code permittedReferer} restriction claims the gateway's API-key validator enforces. A blank restriction
     * value omits the claim, which is how an unrestricted key is represented.
     */
    public static String buildLegacyJwtApiKey(String username, String issuer, String applicationName,
                                               String applicationTier, int applicationId, String applicationUuid,
                                               String applicationOwner, String keyType, String permittedIp,
                                               String permittedReferer) {
        String resource = "/artifacts/keystores/wso2carbon.jks";
        try (InputStream input = JwtTestUtils.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing APIM test signing keystore resource " + resource);
            }
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(input, "wso2carbon".toCharArray());
            String alias = "wso2carbon";
            java.security.cert.Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                throw new IllegalStateException("APIM test signing keystore has no certificate alias '" + alias + "'");
            }
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, "wso2carbon".toCharArray());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String thumbprint = base64Url(digest.digest(certificate.getEncoded()));
            JSONObject header = new JSONObject()
                    .put("alg", "RS256")
                    .put("typ", "JWT")
                    .put("x5t#S256", thumbprint)
                    .put("kid", alias);

            long issuedAt = System.currentTimeMillis() / 1000L;
            JSONObject application = new JSONObject()
                    .put("name", applicationName)
                    .put("tier", applicationTier)
                    .put("id", applicationId)
                    .put("uuid", applicationUuid)
                    .put("owner", applicationOwner);
            JSONObject claims = new JSONObject()
                    .put("jti", UUID.randomUUID().toString())
                    .put("iss", issuer)
                    .put("sub", username)
                    .put("iat", issuedAt)
                    .put("exp", issuedAt + 3600L)
                    .put("end_username", username)
                    .put("keytype", keyType)
                    .put("tokentype", "apiKey")
                    .put("application", application);
            if (permittedIp != null && !permittedIp.isBlank()) {
                claims.put("permittedIP", permittedIp);
            }
            if (permittedReferer != null && !permittedReferer.isBlank()) {
                claims.put("permittedReferer", permittedReferer);
            }
            return buildRs256Jwt(header.toString(), claims.toString(), privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build the legacy JWT-format APIM API key", e);
        }
    }

    /**
     * Builds an RSA public key from a JWKS entry's base64url modulus/exponent ({@code n}/{@code e}) — the
     * runtime way to obtain a product's signing key without committing any key material to the repo.
     */
    public static PublicKey rsaPublicKeyFromJwk(String modulusBase64Url, String exponentBase64Url) {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(modulusBase64Url));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(exponentBase64Url));
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to build an RSA public key from the JWKS modulus/exponent", e);
        }
    }

    /**
     * Verifies a three-segment JWS: checks that {@code signature} really is the given key's signature over the
     * transmitted {@code <header>.<payload>} string. Returns {@code false} for a bad signature (the caller
     * asserts), and THROWS for a structurally impossible token or an algorithm this helper cannot verify — a
     * silent {@code false} there would read as "tampered" and hide the real cause.
     *
     * <p>The signature segment is decoded url-safe first then standard, because the gateway's
     * {@code [apim.jwt] encoding} option emits either alphabet while the signing input is always the segment
     * text exactly as transmitted.
     *
     * @param jwt            the complete assertion
     * @param joseAlgorithm  the JOSE {@code alg} header value (e.g. {@code RS256})
     * @param publicKey      the verification key
     * @return whether the signature verifies
     */
    public static boolean verifyJwsSignature(String jwt, String joseAlgorithm, PublicKey publicKey) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3 || parts[2].isEmpty()) {
            throw new IllegalArgumentException("Not a 3-part SIGNED JWT (an unsigned assertion has an empty "
                    + "signature segment): " + jwt);
        }
        String jcaAlgorithm;
        switch (joseAlgorithm) {
            case "RS256": jcaAlgorithm = "SHA256withRSA"; break;
            case "RS384": jcaAlgorithm = "SHA384withRSA"; break;
            case "RS512": jcaAlgorithm = "SHA512withRSA"; break;
            default:
                throw new IllegalArgumentException("Cannot verify JOSE algorithm '" + joseAlgorithm + "'");
        }
        try {
            Signature verifier = Signature.getInstance(jcaAlgorithm);
            verifier.initVerify(publicKey);
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            return verifier.verify(decodeBase64Segment(parts[2]));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to verify the JWT signature with " + jcaAlgorithm, e);
        }
    }

    /** Base64-decodes a JWT segment, url-safe alphabet first then standard (the gateway emits either). */
    private static byte[] decodeBase64Segment(String segment) {
        try {
            return Base64.getUrlDecoder().decode(segment);
        } catch (IllegalArgumentException e) {
            return Base64.getDecoder().decode(segment);
        }
    }

    /**
     * Rewrites ONE payload claim (prefixing its value with {@code tampered-}) while KEEPING the original
     * signature — the payload no longer matches the signature, driving the invalid-signature negatives.
     */
    public static String tamperClaim(String jwt, String claim) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Not a 3-part signed JWT: " + jwt);
        }
        JSONObject payload = new JSONObject(decodePayload(jwt));
        payload.put(claim, "tampered-" + payload.optString(claim, "value"));
        return parts[0] + "." + base64Url(payload.toString()) + "." + parts[2];
    }

    /**
     * String-replaces within the decoded payload while KEEPING the original signature — the free-form
     * counterpart of {@link #tamperClaim} for tampering a specific literal (e.g. swapping the subject).
     */
    public static String replaceInPayloadKeepingSignature(String jwt, String target, String replacement) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Not a 3-part signed JWT: " + jwt);
        }
        String payload = decodePayload(jwt).replace(target, replacement);
        return parts[0] + "." + base64Url(payload) + "." + parts[2];
    }

    /**
     * Replaces only the JWS signature segment while retaining the issued header and all claims. This produces the
     * same invalid-signature condition as the legacy negative test without accidentally testing missing/expired
     * claims instead.
     */
    public static String invalidateSignature(String jwt) {
        String[] parts = jwt.split("\\.", -1);
        if (parts.length != 3 || parts[0].isEmpty() || parts[1].isEmpty() || parts[2].isEmpty()) {
            throw new IllegalArgumentException("Not a complete signed JWT: " + jwt);
        }
        return parts[0] + "." + parts[1] + "." + base64Url("invalid-signature");
    }

    /**
     * Builds the legacy AdvancedConfigurationsTestCase's HS256 invalid-signature token without assuming that the
     * current topology issues JWT-formatted access tokens (distributed deployments may issue opaque tokens).
     */
    public static String buildInvalidSignatureJwt(String subject, String issuer) {
        long now = System.currentTimeMillis() / 1000;
        JSONObject header = new JSONObject().put("alg", "HS256").put("typ", "JWT");
        JSONObject claims = new JSONObject()
                .put("sub", subject)
                .put("iss", issuer)
                .put("exp", now + 3600)
                .put("iat", now)
                .put("scope", "openid apim:admin")
                .put("azp", "x")
                .put("aud", issuer);
        String signingInput = base64Url(header.toString()) + "." + base64Url(claims.toString());
        return signingInput + "." + base64Url("invalid_signature");
    }
}
