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

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;

public class RefreshGrantTestCaseIT {

    private static Logger logger = LoggerFactory.getLogger(RefreshGrantTestCaseIT.class);

    DCRClientInfo dcrClientInfo;

    @BeforeClass
    public void setup() throws APIManagementException, IOException {

        dcrClientInfo = new DCRClientInfo();
        dcrClientInfo.setGrantTypes(Arrays.asList("client_credentials", "password", "refresh_token"));
        dcrClientInfo.setClientName("RefreshGrantTestCaseIT");
        DCRMServiceStub dcrmServiceStub = TestUtil.getDcrmServiceStub("admin", "admin");
        Response response = dcrmServiceStub.registerApplication(dcrClientInfo);
        dcrClientInfo = TestUtil.getDCRClientInfo(response);
        Assert.assertNotNull(dcrClientInfo);
    }

    @Test
    public void testGenerateTokenFromRefreshToken() throws AMIntegrationTestException {

        OAuth2TokenInfo tokenInfo = TestUtil.generateToken(dcrClientInfo.getClientId(), dcrClientInfo.getClientSecret(),
                "user1", TestUtil.getUser("user1"), "apim:api_view");
        Assert.assertNotNull(tokenInfo);
        Assert.assertTrue(tokenInfo.getScope().contains("apim:api_view"));
        Assert.assertNotNull(tokenInfo.getRefreshToken());
        OAuth2TokenInfo refreshTokenInfo = TestUtil.generateToken(dcrClientInfo.getClientId(), dcrClientInfo
                .getClientSecret(), tokenInfo.getRefreshToken(), null);
        Assert.assertNotNull(refreshTokenInfo);
        Assert.assertTrue(refreshTokenInfo.getScope().contains("apim:api_view"));
        OAuth2TokenInfo refreshTokenInfoWithScopes = TestUtil.generateToken(dcrClientInfo.getClientId(), dcrClientInfo
                .getClientSecret(), refreshTokenInfo.getRefreshToken(), "apim:api_view apim:api_create");
        Assert.assertNotNull(refreshTokenInfo);
        Assert.assertTrue(refreshTokenInfoWithScopes.getScope().contains("apim:api_view"));
        Assert.assertTrue(refreshTokenInfoWithScopes.getScope().contains("apim:api_create"));
        logger.info(refreshTokenInfo.toString());
    }

}
