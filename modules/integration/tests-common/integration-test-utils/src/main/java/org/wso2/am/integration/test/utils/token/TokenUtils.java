/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.integration.test.utils.token;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * A class for token utility operations.
 */
public class TokenUtils {

    /**
     * Parse a given JWT token and return 'jti' of the token.
     *
     * @param jwtToken JWT token
     * @return 'jti' of the token
     * @throws ParseException if an error occurred when parsing the token
     */
    public static String getJtiOfJwtToken(String jwtToken) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(jwtToken);
        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        return jwtClaimsSet.getJWTID();
    }

    /**
     * Returns the base64 encoded client Id:client secret
     *
     * @param clientId Client Id of the application
     * @param clientSecret Client secret of the application
     * @return base64 encoded client Id:client secret
     */
    public static String getBase64EncodedAppCredentials(String clientId, String clientSecret) {
        Map<String, String> authenticationRequestHeaders = new HashMap<>();
        String basicAuthHeader = clientId + ":" + clientSecret;
        byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes(StandardCharsets.UTF_8));
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }
}
