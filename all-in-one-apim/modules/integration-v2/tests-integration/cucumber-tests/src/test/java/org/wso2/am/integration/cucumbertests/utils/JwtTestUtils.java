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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Test-side JWT plumbing shared by the grant/token-exchange/key-manager glue: base64url segment handling,
 * RS256 assertion signing, and signature-invalidating tamper helpers. This is ASSEMBLY and MUTATION of test
 * tokens only — never validation (the product under test does that). Consolidates the per-file copies that
 * previously lived in ApplicationBaseSteps, TokenExchangeSteps and JwtGrantSteps.
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
}
