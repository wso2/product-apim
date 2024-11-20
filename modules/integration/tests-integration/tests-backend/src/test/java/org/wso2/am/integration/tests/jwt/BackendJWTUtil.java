/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.jwt;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;

import static org.testng.AssertJUnit.assertTrue;

import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;

import java.io.UnsupportedEncodingException;

public class BackendJWTUtil {

    /**
     * verify JWT signature
     *
     * @param jwtheader JWT Header Object
     * @throws UnsupportedEncodingException
     */
    public static void verifySignature(Header jwtheader) throws UnsupportedEncodingException {
        String jwtHeader = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
        byte[] jwtSignature = APIMTestCaseUtils.getDecodedJWTSignature(jwtheader.getValue());
        String jwtAssertion = APIMTestCaseUtils.getJWTAssertion(jwtheader.getValue());
        boolean isSignatureValid = APIMTestCaseUtils.isJwtSignatureValid(jwtAssertion, jwtSignature, jwtHeader);
        assertTrue("JWT signature verification failed", isSignatureValid);
    }

    /**
     * verify JWT Header
     *
     * @param decodedJWTHeaderString decoded JWT Header value
     * @param jwksKidClaim           kid claim in JWKS endpoint
     * @throws JSONException if JSON payload is malformed
     */
    public static void verifyJWTHeader(String decodedJWTHeaderString, String jwksKidClaim) throws JSONException {
        JSONObject jsonHeaderObject = new JSONObject(decodedJWTHeaderString);
        Assert.assertEquals(jsonHeaderObject.getString("typ"), "JWT");
        Assert.assertEquals(jsonHeaderObject.getString("alg"), "RS256");

        // Verify kid claim: check if kid claim in JWT header match with that of JWKS endpoint
        Assert.assertTrue(jsonHeaderObject.has("kid"));
        if (jwksKidClaim != null) {
            Assert.assertEquals(jsonHeaderObject.getString("kid"), jwksKidClaim, "kid claim in JWT header " +
                    "does not match with that of JWKS endpoint");
        }
    }

    /**
     * verify whether JWT contains wrong claims
     *
     * @param decodedJWTJSONObject decoded JWT JSON Object
     */
    public static void verifyWrongClaims(JSONObject decodedJWTJSONObject) {
        boolean exceptionOccured = false;
        try {
            decodedJWTJSONObject.getString("http://wso2.org/claims/wrongclaim");
        } catch (JSONException e) {
            exceptionOccured = true;
        }
        assertTrue("JWT claim received is invalid", exceptionOccured);
    }
}
