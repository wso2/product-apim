/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;


import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoAdditionalPropertiesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoAdditionalPropertiesMapDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;


public class MandatoryPropertiesTestWithRestart extends APIManagerLifecycleBaseTest {

    private final String apiContextTest = "TestAPIMandatoryProps";
    private final String apiDescription = "This is Test API Created by API Manager Integration Test";
    private final String apiTag = "tag18-4, tag18-5, tag18-6";
    private final String apiName = "MandatoryPropsTestAPI";
    private final String apiVersion = "1.0.0";
    private String apiId;
    private String apiProductionEndPointUrl;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public MandatoryPropertiesTestWithRestart(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }};
    }

    @BeforeTest(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "mandatory-properties"
                        + File.separator + "deployment.toml"));

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";
        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
    }

    @Test(groups = { "wso2.am" }, description = "Test API update without setting mandatory properties")
    public void testCreateAnAPIThroughThePublisherRestWithoutMandatoryProperties() throws Exception {

        APIRequest apiCreationRequestBean;
        apiCreationRequestBean = new APIRequest(apiName, apiContextTest, new URL(apiProductionEndPointUrl));

        apiCreationRequestBean.setVersion(apiVersion);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setTier("Gold");

        HttpResponse response = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId = response.getData();
        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);
        Map<String, APIInfoAdditionalPropertiesMapDTO> additionalPropertiesMap = new HashMap();

        APIInfoAdditionalPropertiesMapDTO prop = new APIInfoAdditionalPropertiesMapDTO();
        prop.setName("PropertyName");
        prop.setValue("");
        prop.setDisplay(true);
        additionalPropertiesMap.put("PropertyName", prop);

        apiDto.setAdditionalPropertiesMap(additionalPropertiesMap);
        try {
            restAPIPublisher.updateAPI(apiDto, apiId);
            Assert.fail("APIs cannot be updated without setting values to mandatory API properties.");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Error while updating API. required properties cannot be empty." );
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test API update with mandatory properties",
            dependsOnMethods = "testCreateAnAPIThroughThePublisherRestWithoutMandatoryProperties")
    public void testCreateAnAPIThroughThePublisherRestWithMandatoryProperties() throws Exception {
        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);
        Map<String, APIInfoAdditionalPropertiesMapDTO> additionalPropertiesMap = new HashMap();

        APIInfoAdditionalPropertiesMapDTO prop = new APIInfoAdditionalPropertiesMapDTO();

        prop.setName("PropertyName");
        prop.setValue("PropertyValue");
        prop.setDisplay(true);
        additionalPropertiesMap.put("PropertyName", prop);

        apiDto.setAdditionalPropertiesMap(additionalPropertiesMap);
        try {
            APIDTO apiData = restAPIPublisher.updateAPI(apiDto, apiId);
            assertEquals(apiData.getAdditionalProperties().get(0).getName(), "PropertyName" );
            assertEquals(apiData.getAdditionalProperties().get(0).getValue(), "PropertyValue" );
        } catch (ApiException e) {
            Assert.fail("APIs should be updated if the mandatory property values are not empty.");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
        restAPIPublisher.deleteAPI(apiId);
    }
}