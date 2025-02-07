/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.jwt.idp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.PasswordLookup;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class JWTGeneratorUtil {


    public static String generatedJWT(File privateKeyFile,String kid,String keyAlias, String keyStorePassword,
                                      String keyPassword,
                                      String subject,String issuer, Map<String, Object> attributes)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException, JOSEException {

        return generatedJWT(privateKeyFile, kid, keyAlias, keyStorePassword, keyPassword, subject,
                issuer, System.currentTimeMillis(), attributes);
    }

    public static String generatedJWT(File privateKeyFile, String kid, String keyAlias, String keyStorePassword,
                                      String keyPassword, String subject, String issuer, long notBeforeTimeMillis,
                                      Map<String, Object> attributes)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException, JOSEException {

        JWSHeader header = buildHeader(privateKeyFile,kid ,keyAlias, keyStorePassword);
        JWTClaimsSet jwtClaimsSet = buildBody(issuer, subject, notBeforeTimeMillis, attributes);
        return signJWT(header, jwtClaimsSet, privateKeyFile, keyAlias, keyStorePassword, keyPassword);
    }

    private static String signJWT(JWSHeader header, JWTClaimsSet jwtClaimsSet, File privateKeyLocation,
                                  String keyAlias,
                                  String keyStorePassword, String keyPassword)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException,
            UnrecoverableKeyException, JOSEException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fileInputStream = new FileInputStream(privateKeyLocation);
        keyStore.load(fileInputStream, keyStorePassword.toCharArray());
        Key privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray());
        JWSSigner signer = new RSASSASigner((RSAPrivateKey) privateKey);
        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private static JWTClaimsSet buildBody(String issuer, String subject, long notBeforeTimeMillis,
                                          Map<String, Object> attributes) {

        JWTClaimsSet.Builder jwtClaimSetBuilder = new JWTClaimsSet.Builder();
        jwtClaimSetBuilder.issuer(issuer);
        jwtClaimSetBuilder.issueTime(new Date(System.currentTimeMillis()));
        jwtClaimSetBuilder.jwtID(UUID.randomUUID().toString());
        jwtClaimSetBuilder.subject(subject);
        jwtClaimSetBuilder.notBeforeTime(new Date(notBeforeTimeMillis));
        jwtClaimSetBuilder.expirationTime(new Date(notBeforeTimeMillis + 15 * 60*1000));
        attributes.forEach((key, value) -> {
            jwtClaimSetBuilder.claim(key, value);
        });
        return jwtClaimSetBuilder.build();
    }

    private static JWSHeader buildHeader(File privateKeyFile, String kid, String keyAlias, String keyStorePassword)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fileInputStream = new FileInputStream(privateKeyFile);
        keyStore.load(fileInputStream, keyStorePassword.toCharArray());
        Certificate publicCert = keyStore.getCertificate(keyAlias);
        MessageDigest digestValue = MessageDigest.getInstance("SHA-1");
        byte[] der = publicCert.getEncoded();
        digestValue.update(der);
        byte[] digestInBytes = digestValue.digest();
        String publicCertThumbprint = hexify(digestInBytes);

        JWSHeader.Builder jwsHeaderBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256);
        jwsHeaderBuilder.type(JOSEObjectType.JWT);
        jwsHeaderBuilder.x509CertThumbprint(new Base64URL(publicCertThumbprint));
        jwsHeaderBuilder.keyID(kid);
        return jwsHeaderBuilder.build();
    }

    /**
     * Helper method to hexify a byte array.
     *
     * @param bytes - The input byte array
     * @return hexadecimal representation
     */
    private static String hexify(byte bytes[]) {

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            buf.append(hexDigits[(aByte & 0xf0) >> 4]);
            buf.append(hexDigits[aByte & 0x0f]);
        }
        return buf.toString();
    }

}
