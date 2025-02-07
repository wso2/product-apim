/*
* Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
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
*
*/
package org.wso2.am.integration.tests.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyReGenerateResponseDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import javax.ws.rs.core.Response;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class ApplicationConsumerSecretRegenerateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(ApplicationConsumerSecretRegenerateTestCase.class);
    private final String APP_NAME = "CallBackUrlUpdateTestApp";
    private final String APP_DESCRIPTION = "description";
    private final String APP_CALLBACK_URL = "http://wso2.com/";

    private String consumerKey;
    private String consumerSecret;
    private RestAPIStoreImpl restAPIStore;
    private String applicationID;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationConsumerSecretRegenerateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        restAPIStore = new RestAPIStoreImpl(user.getUserName(), user.getPassword(),
                publisherContext.getContextTenant().getDomain(), storeUrls.getWebAppURLHttps());
    }

    @Test(groups = { "wso2.am" }, description = "Sample Application creation")
    public void testApplicationCreation() throws Exception {

        ApplicationDTO application = restAPIStore.addApplication(APP_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, APP_CALLBACK_URL,
                APP_DESCRIPTION);
        applicationID = application.getApplicationId();
        //generate keys for the subscription
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        ApplicationKeyDTO generatedKeys = restAPIStore.generateKeys(applicationID, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        consumerKey = generatedKeys.getConsumerKey();
        consumerSecret = generatedKeys.getConsumerSecret();
        Assert.assertNotNull(consumerKey, "Error in generating keys for application: " + APP_NAME);
    }

    @Test(groups = {"wso2.am"}, description = "Test regenerate consumer secret after application key generate",
            dependsOnMethods = "testApplicationCreation")
    public void testRegenerateConsumerSecret() throws Exception {

        ApiResponse<ApplicationKeyReGenerateResponseDTO> regenerateResponse = restAPIStore
                    .regenerateConsumerSecret(applicationID, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        Assert.assertEquals(regenerateResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Error when re-generating consumer secret for application: " + applicationID);
        Assert.assertNotNull(regenerateResponse.getData().getConsumerSecret());
        Assert.assertNotEquals(consumerSecret, regenerateResponse.getData().getConsumerSecret());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},};
    }
}
