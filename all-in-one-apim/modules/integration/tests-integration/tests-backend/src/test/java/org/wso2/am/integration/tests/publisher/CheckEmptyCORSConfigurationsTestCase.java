/*
 *
 *   Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.net.URL;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Check if empty arrays are returned when CORS configurations are null
 *
 */

public class CheckEmptyCORSConfigurationsTestCase
        extends APIMIntegrationBaseTest {

    private final String apiNameTest = "CORSPublisherTest";
    private final String apiVersion = "1.0.0";
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public CheckEmptyCORSConfigurationsTestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String gatewayUrl;
        if (gatewayContextWrk.getContextTenant().getDomain().equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        apiEndPointUrl = gatewayUrl + "jaxrs_basic/services/customers/customerservice";
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API though the publisher rest API and check empty CORS configs ")
    public void testCheckEmptyCORSConfigurations() throws Exception {

        String apiContext = "CORSPublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tagCORS-1, tagCORS-2, tagCORS-3";
        //Create an API
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiNameTest, apiContext, apiVersion,
                apiProvider, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("apiCORSb");
        apiCreationRequestBean.setBizOwnerMail("apiCORSb@ee.com");
        apiCreationRequestBean.setTechOwner("apiCORSt");
        apiCreationRequestBean.setTechOwnerMail("apiCORSt@ww.com");
        JSONObject testCORSConfiguratoins = new JSONObject();
        testCORSConfiguratoins.put("corsConfigurationEnabled", "false");
        testCORSConfiguratoins.put("accessControlAllowOrigins", "null");
        testCORSConfiguratoins.put("accessControlAllowCredentials", "false");
        testCORSConfiguratoins.put("accessControlAllowHeaders", "null");
        testCORSConfiguratoins.put("accessControlAllowMethods", "null");
        apiCreationRequestBean.setCorsConfiguration(testCORSConfiguratoins);

        APIDTO apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        String status = apiCreationResponse.getLifeCycleStatus();
        apiId = apiCreationResponse.getId();
        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        assertEquals(apiResponsePublisher.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(apiNameTest.equals(apiCreationResponse.getName()), apiNameTest + " is not visible in publisher");
        assertTrue(APILifeCycleState.CREATED.getState().equalsIgnoreCase(status), "Status of the " + apiNameTest +
                "is not a valid status");

        //Check if empty arrays are returned for null CORS configurations
        JSONObject apiDetails = new JSONObject(apiResponsePublisher.getData());
        JSONObject corsConfigs = apiDetails.getJSONObject("corsConfiguration");
        assertEquals(corsConfigs.getString("accessControlAllowOrigins"), "[]");
        assertEquals(corsConfigs.getString("accessControlAllowHeaders"), "[]");
        assertEquals(corsConfigs.getString("accessControlAllowMethods"), "[]");

    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
