/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

/**
 * Utility class for generating API key JWTs in integration tests.
 * Produces tokens with the same claims structure as the server-side API key generator
 * so that tests can create and validate API key tokens independently.
 */
public class JWTGenerator {

    private final String keystorePassword = "wso2carbon";
    private final String keyAlias = "wso2carbon";
    private final String keyPassword = "wso2carbon";
    private final String keystorePath = TestConfigurationProvider.getResourceLocation() + File.separator + "keystores"
            + File.separator + "products" + File.separator + "wso2carbon.jks";

    public JWTGenerator() {
    }

    /**
     * Generates a signed JWT for an API key with the claims defined in {@code tokenInfo}.
     *
     * @param tokenInfo token metadata
     * @return compact, dot-separated signed JWT string (header.payload.signature)
     */
    public String generateToken(JwtTokenInfo tokenInfo)
            throws KeyStoreException, IOException, CertificateException,
                   NoSuchAlgorithmException, UnrecoverableKeyException, JOSEException {

        KeyStore keyStore = getApiKeySignKeyStore();
        Certificate cert = keyStore.getCertificate(keyAlias);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());

        JWSHeader header = buildHeader(cert);
        JWTClaimsSet claimsSet = buildBody(tokenInfo);

        JWSSigner signer = new RSASSASigner((RSAPrivateKey) privateKey);
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private JWSHeader buildHeader(Certificate cert)
            throws CertificateException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(cert.getEncoded());
        byte[] digestBytes = digest.digest();
        String thumbprint = Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes);

        return new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .x509CertSHA256Thumbprint(new Base64URL(thumbprint))
                .keyID(keyAlias)
                .build();
    }

    private JWTClaimsSet buildBody(JwtTokenInfo info) {
        long now = System.currentTimeMillis();

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .issuer(info.getIssuer())
                .subject(info.getSub())
                .issueTime(new Date(now))
                .expirationTime(new Date(now + info.getValidityPeriod() * 1000L))
                .claim("end_username", info.getEndUsername())
                .claim("keytype", info.getKeyType())
                .claim("tokentype", "apiKey");

        Map<String, Object> application = new HashMap<>();
        application.put("name", info.getApplicationName());
        application.put("tier", info.getApplicationTier());
        application.put("id", info.getApplicationId());
        application.put("uuid", info.getApplicationUUID());
        application.put("owner", info.getApplicationOwner());
        builder.claim("application", application);

        if (info.getSubscribedApis() != null) {
            builder.claim("subscribedAPIs", info.getSubscribedApis());
        }
        if (info.getTierInfo() != null) {
            builder.claim("tierInfo", info.getTierInfo());
        }
        if (info.getPermittedIP() != null && !info.getPermittedIP().isEmpty()) {
            builder.claim("permittedIP", info.getPermittedIP());
        }
        if (info.getPermittedReferer() != null && !info.getPermittedReferer().isEmpty()) {
            builder.claim("permittedReferer", info.getPermittedReferer());
        }

        return builder.build();
    }

    private KeyStore getApiKeySignKeyStore()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        return keyStore;
    }

    /**
     * Holds all metadata required to build an API key JWT payload.
     * Use {@link Builder} to construct instances.
     */
    public static class JwtTokenInfo {

        private final String endUsername;
        private final String sub;
        private final String issuer;
        private final long validityPeriod;
        private final String keyType;
        private final String permittedIP;
        private final String permittedReferer;
        private final String applicationName;
        private final String applicationTier;
        private final int applicationId;
        private final String applicationUUID;
        private final String applicationOwner;
        private final List<Map<String, Object>> subscribedApis;
        private final Map<String, Object> tierInfo;

        private JwtTokenInfo(Builder builder) {
            this.endUsername = builder.endUsername;
            this.sub = builder.sub;
            this.issuer = builder.issuer;
            this.validityPeriod = builder.validityPeriod;
            this.keyType = builder.keyType;
            this.permittedIP = builder.permittedIP;
            this.permittedReferer = builder.permittedReferer;
            this.applicationName = builder.applicationName;
            this.applicationTier = builder.applicationTier;
            this.applicationId = builder.applicationId;
            this.applicationUUID = builder.applicationUUID;
            this.applicationOwner = builder.applicationOwner;
            this.subscribedApis = builder.subscribedApis;
            this.tierInfo = builder.tierInfo;
        }

        public String getEndUsername() { return endUsername; }
        public String getSub() { return sub; }
        public String getIssuer() { return issuer; }
        public long getValidityPeriod() { return validityPeriod; }
        public String getKeyType() { return keyType; }
        public String getPermittedIP() { return permittedIP; }
        public String getPermittedReferer() { return permittedReferer; }
        public String getApplicationName() { return applicationName; }
        public String getApplicationTier() { return applicationTier; }
        public int getApplicationId() { return applicationId; }
        public String getApplicationUUID() { return applicationUUID; }
        public String getApplicationOwner() { return applicationOwner; }
        public List<Map<String, Object>> getSubscribedApis() { return subscribedApis; }
        public Map<String, Object> getTierInfo() { return tierInfo; }

        public static class Builder {

            private String endUsername;
            private String sub;
            private String issuer;
            private long validityPeriod = 3600;
            private String keyType = "PRODUCTION";
            private String permittedIP;
            private String permittedReferer;
            private String applicationName;
            private String applicationTier;
            private int applicationId;
            private String applicationUUID;
            private String applicationOwner;
            private List<Map<String, Object>> subscribedApis;
            private Map<String, Object> tierInfo;

            public Builder endUsername(String val) { this.endUsername = val; return this; }
            public Builder sub(String val) { this.sub = val; return this; }
            public Builder issuer(String val) { this.issuer = val; return this; }
            public Builder validityPeriod(long val) { this.validityPeriod = val; return this; }
            public Builder keyType(String val) { this.keyType = val; return this; }
            public Builder permittedIP(String val) { this.permittedIP = val; return this; }
            public Builder permittedReferer(String val) { this.permittedReferer = val; return this; }
            public Builder applicationName(String val) { this.applicationName = val; return this; }
            public Builder applicationTier(String val) { this.applicationTier = val; return this; }
            public Builder applicationId(int val) { this.applicationId = val; return this; }
            public Builder applicationUUID(String val) { this.applicationUUID = val; return this; }
            public Builder applicationOwner(String val) { this.applicationOwner = val; return this; }
            public Builder subscribedApis(List<Map<String, Object>> val) { this.subscribedApis = val; return this; }
            public Builder tierInfo(Map<String, Object> val) { this.tierInfo = val; return this; }

            public JwtTokenInfo build() {
                return new JwtTokenInfo(this);
            }
        }
    }
}

