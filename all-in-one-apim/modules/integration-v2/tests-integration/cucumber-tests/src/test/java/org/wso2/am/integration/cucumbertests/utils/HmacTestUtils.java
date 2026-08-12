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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.security.GeneralSecurityException;
import java.util.Locale;

/**
 * Shared owner of RAW keyed-MAC digests for the test side, and of the WebSub {@code x-hub-signature} convention
 * built on them. Ports the legacy {@code StreamingApiTestUtils.calculateRFC2104HMAC} (and the {@code "sha1=" + …}
 * concatenation copied beside it in {@code WebhookSender} and {@code SecretValidationTestCase}).
 *
 * <p>Deliberately SEPARATE from {@link JwtTestUtils}, which is scoped to JWT assembly/mutation (base64url
 * segments, RS256 assertion signing) — a hex MAC over an arbitrary body is neither. Both are ASSEMBLY of test
 * material only, never validation of the product's own output beyond recomputing the expected value.
 */
public final class HmacTestUtils {

    private HmacTestUtils() {
    }

    /**
     * RFC 2104 HMAC of {@code data} keyed by {@code key}, rendered as LOWER-CASE hex — the digest form the WebSub
     * signature header carries.
     *
     * @param macAlgorithm JCA MAC name, e.g. {@code HmacSHA1} / {@code HmacSHA256}
     */
    public static String hmacHex(String macAlgorithm, String data, String key) {
        try {
            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), macAlgorithm));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // HexFormat (JDK 17+) rather than a per-byte String.format: same lower-case output, no formatter
            // allocated per byte. Lower case is not cosmetic — it is the digest form the x-hub-signature carries.
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute an " + macAlgorithm + " MAC over the given data", e);
        }
    }

    /**
     * The {@code x-hub-signature} header value WebSub prescribes: {@code <algorithm>=<hex-digest>}, with the
     * algorithm name lower-cased (e.g. {@code sha1=8f2b…}). {@code signingAlgorithm} is the value carried by an
     * API's {@code websubSubscriptionConfiguration.signingAlgorithm} (e.g. {@code SHA1}, {@code SHA256}).
     *
     * <p>Used from BOTH sides of a WebSub arc: the event SOURCE signs the content it POSTs to the hub's
     * event-receiver inbound with the API's own secret, and the test recomputes the value the hub must have
     * re-signed the fan-out delivery with, using the SUBSCRIBER's {@code hub.secret}.
     */
    public static String hubSignature(String signingAlgorithm, String body, String secret) {
        String normalized = signingAlgorithm.replace("-", "").toUpperCase(Locale.ROOT);
        return normalized.toLowerCase(Locale.ROOT) + "=" + hmacHex("Hmac" + normalized, body, secret);
    }
}
