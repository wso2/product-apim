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
package org.wso2.carbon.apimgt.rest.integration.tests.store;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.ApplicationIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Application;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeyGenerateRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationKeys;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationToken;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.ApplicationTokenGenerateRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

import java.text.ParseException;
import java.util.Arrays;

public class ApplicationJWTTestCaseIT {

    ApplicationIndividualApi applicationIndividualApi;
    Application application;

    @BeforeClass
    public void setUp() throws Exception {
        application = new Application().name("ApplicationJWTTestCaseIT").throttlingTier("Unlimited").description
                ("this is ApplicationJWTTestCaseIT");
        applicationIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser("user4"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(ApplicationIndividualApi.class);
        application = applicationIndividualApi.applicationsPost(application);
    }

    @Test
    public void testGenerateApplicationJWTToken() throws ParseException {
        ApplicationKeyGenerateRequest applicationKeyGenerateRequest = new ApplicationKeyGenerateRequest()
                .grantTypesToBeSupported(Arrays.asList("client_credentials", "password")).tokenType
                        (ApplicationKeyGenerateRequest.TokenTypeEnum.JWT).keyType(ApplicationKeyGenerateRequest
                        .KeyTypeEnum.PRODUCTION);
        ApplicationKeys applicationKeys = applicationIndividualApi.applicationsApplicationIdGenerateKeysPost
                (application.getApplicationId(), applicationKeyGenerateRequest);
        Assert.assertEquals(applicationKeys.getTokenType().getValue(), applicationKeyGenerateRequest.getTokenType()
                .getValue());
        Assert.assertNotNull(applicationKeys.getConsumerKey());
        Assert.assertNotNull(applicationKeys.getConsumerSecret());
        ApplicationToken applicationToken = applicationIndividualApi.applicationsApplicationIdGenerateTokenPost
                (application.getApplicationId(), new ApplicationTokenGenerateRequest().consumerKey(applicationKeys
                        .getConsumerKey()).consumerSecret(applicationKeys.getConsumerSecret()).scopes("default")
                        .validityPeriod(3600), "", "");
        Assert.assertNotNull(applicationToken);
        Assert.assertNotNull(applicationToken.getAccessToken());
        Assert.assertNotNull(applicationToken.getTokenScopes());
        Assert.assertEquals(applicationToken.getTokenScopes(), "default");
        Assert.assertTrue(applicationToken.getAccessToken().split("\\.").length > 1);
        SignedJWT signedJWT = SignedJWT.parse(applicationToken.getAccessToken());
        ReadOnlyJWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        Assert.assertTrue(jwtClaimsSet.getExpirationTime().after(jwtClaimsSet.getIssueTime()));
        Assert.assertEquals(jwtClaimsSet.getIssuer(), "https://localhost:9443/oauth2/token");
        ApplicationKeys retrievedApplicationKeys = applicationIndividualApi.applicationsApplicationIdKeysKeyTypeGet
                (application.getApplicationId(), ApplicationKeyGenerateRequest.KeyTypeEnum.PRODUCTION.getValue());
        Assert.assertEquals(retrievedApplicationKeys.getConsumerKey(), applicationKeys.getConsumerKey());
        Assert.assertEquals(retrievedApplicationKeys.getConsumerSecret(), applicationKeys.getConsumerSecret());
        Assert.assertEquals(retrievedApplicationKeys.getTokenType(), applicationKeys.getTokenType());
        retrievedApplicationKeys.setTokenType(ApplicationKeys.TokenTypeEnum.OAUTH);
        ApplicationKeys updatedApplicationKeys = applicationIndividualApi.applicationsApplicationIdKeysKeyTypePut
                (application.getApplicationId(), ApplicationKeyGenerateRequest.KeyTypeEnum.PRODUCTION.getValue(),
                        retrievedApplicationKeys);
        Assert.assertEquals(updatedApplicationKeys.getConsumerKey(), applicationKeys.getConsumerKey());
        Assert.assertEquals(updatedApplicationKeys.getConsumerSecret(), applicationKeys.getConsumerSecret());
        Assert.assertEquals(updatedApplicationKeys.getTokenType().getValue(), "OAUTH");
        ApplicationToken retrievedToken = applicationIndividualApi.applicationsApplicationIdGenerateTokenPost
                (application.getApplicationId(), new ApplicationTokenGenerateRequest().consumerKey(applicationKeys
                        .getConsumerKey()).consumerSecret(applicationKeys.getConsumerSecret()).scopes("default")
                        .validityPeriod(3600), "", "");
        Assert.assertTrue(retrievedToken.getAccessToken().split("\\.").length <= 1);
    }

}
