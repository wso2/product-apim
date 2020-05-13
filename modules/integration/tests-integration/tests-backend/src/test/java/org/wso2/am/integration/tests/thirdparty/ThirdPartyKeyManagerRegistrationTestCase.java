/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.thirdparty;

import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.TokenValidationDTO;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

public class ThirdPartyKeyManagerRegistrationTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ThirdPartyKeyManagerRegistrationTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Create Key Manager from Defined Key Manager")
    public void testCreateKeyManager() throws Exception {

        KeyManagerDTO keyManagerDTO  = new KeyManagerDTO();
        keyManagerDTO.setType("custom1");
        keyManagerDTO.setName("Key Manager 1");
        keyManagerDTO.setClientRegistrationEndpoint("");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setIntrospectionEndpoint("https://localhost");
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setType(TokenValidationDTO.TypeEnum.REGEX);
        tokenValidationDTO.setValue(
                "custom1:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
        keyManagerDTO.setTokenValidation(tokenValidationDTO);
        keyManagerDTO.setAdditionalProperties(new JsonObject());
        ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(),201);
        KeyManagerDTO retrievedData = keyManagerDTOApiResponse.getData();
        Assert.assertNotNull(retrievedData.getId());
        Assert.assertEquals(retrievedData.getName(),keyManagerDTO.getName());
    }



    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        super.cleanUp();

    }



    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @Factory(dataProvider = "userModeDataProvider")
    public ThirdPartyKeyManagerRegistrationTestCase(String providerName, TestUserMode userMode) {

        this.userMode = userMode;
    }

}
