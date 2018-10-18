/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.rest.integration.tests.oauth;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import feign.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.core.auth.DCRMServiceStub;
import org.wso2.carbon.apimgt.core.auth.dto.DCRClientInfo;
import org.wso2.carbon.apimgt.core.auth.dto.OAuth2TokenInfo;
import org.wso2.carbon.apimgt.core.exception.APIManagementException;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

public class JWTTokenGrantTestCaseIT {


    DCRClientInfo dcrClientInfo;

    @BeforeClass
    public void setup() throws APIManagementException, IOException {

        dcrClientInfo = new DCRClientInfo();
        dcrClientInfo.setGrantTypes(Arrays.asList("client_credentials", "password", "refresh_token"));
        dcrClientInfo.setClientName("JWTTokenGrantTestCaseIT");
        dcrClientInfo.setTokenType("JWT");
        dcrClientInfo.setAudiences(Arrays.asList("http://org.wso2.apimgt/gateway"));
        DCRMServiceStub dcrmServiceStub = TestUtil.getDcrmServiceStub("admin", "admin");
        Response response = dcrmServiceStub.registerApplication(dcrClientInfo);
        dcrClientInfo = TestUtil.getDCRClientInfo(response);
        Assert.assertNotNull(dcrClientInfo);
        Assert.assertEquals(dcrClientInfo.getAudiences(), Arrays.asList("http://org.wso2.apimgt/gateway"));
    }

    @Test
    public void testGenerateTokenFromRefreshToken() throws AMIntegrationTestException, ParseException {

        OAuth2TokenInfo tokenInfo = TestUtil.generateToken(dcrClientInfo.getClientId(), dcrClientInfo.getClientSecret(),
                "user1", TestUtil.getUser("user1"), "default");
        Assert.assertNotNull(tokenInfo);
        Assert.assertNotNull(tokenInfo.getRefreshToken());
        SignedJWT signedJWT = SignedJWT.parse(tokenInfo.getAccessToken());
        ReadOnlyJWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertTrue(jwtClaimsSet.getExpirationTime().after(jwtClaimsSet.getIssueTime()));
        Assert.assertEquals(jwtClaimsSet.getClaim("scope"), "default");
        Assert.assertEquals(jwtClaimsSet.getIssuer(), "https://localhost:9443/oauth2/token");
        Assert.assertEquals(jwtClaimsSet.getAudience(), Arrays.asList("http://org.wso2.apimgt/gateway"));
        OAuth2TokenInfo refreshTokenInfo = TestUtil.generateToken(dcrClientInfo.getClientId(), dcrClientInfo
                .getClientSecret(), tokenInfo.getRefreshToken(), "apim:api_view");
        Assert.assertNotNull(refreshTokenInfo);
        signedJWT = SignedJWT.parse(refreshTokenInfo.getAccessToken());
        jwtClaimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertTrue(jwtClaimsSet.getExpirationTime().after(jwtClaimsSet.getIssueTime()));
        Assert.assertEquals(jwtClaimsSet.getIssuer(), "https://localhost:9443/oauth2/token");
        Assert.assertEquals(jwtClaimsSet.getClaim("scope"), "apim:api_view");
        Assert.assertEquals(jwtClaimsSet.getAudience(), Arrays.asList("http://org.wso2.apimgt/gateway"));
    }

}
