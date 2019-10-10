/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import com.google.gson.Gson;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;


public class NewCopyWithDefaultVersion extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(NewCopyWithDefaultVersion.class);
    private APIPublisherRestClient apiPublisher;
    private String API_NAME = "DefaultVersionAPITest";
    private String apiContext = "DefaultVersionAPI";
    private String version = "1.0.0";
    private static String newVersion = "2.0.0";
    private String TAGS = "testtag1, testtag2";
    private String providerName;
    private String visibility = "public";
    private String description = "Test Description";
    private String tier = APIMIntegrationConstants.API_TIER.GOLD;
    private String resTier = APIMIntegrationConstants.RESOURCE_TIER.TENK_PER_MIN;
    private String endPointType = "http";
    private String resourceMethodAuthType = "Application & Application User";
    private String uriTemplate = "customers/{id}/";
    private String apiId;
    private String defaultApiId;

    @Factory(dataProvider = "userModeDataProvider")
    public NewCopyWithDefaultVersion(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"webapp"}, description = "New Copy with Default Version")
    public void setDefaultVersionToNewcopy() throws Exception {
        String gatewayUrl;
        if (gatewayContextWrk.getContextTenant().getDomain().equals("carbon.super")) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        String endpointUrl = gatewayUrl + "jaxrs_basic/services/customers/customerservice";
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        //API request
        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URL(endpointUrl));
        apiRequest.setTags(TAGS);
        apiRequest.setDescription(description);
        apiRequest.setVersion(version);
        apiRequest.setProvider(providerName);
        apiRequest.setEndpointType(endPointType);
        apiRequest.setResourceMethodAuthType(resourceMethodAuthType);
        apiRequest.setTier(tier);
        apiRequest.setResourceMethodThrottlingTier(resTier);
        apiRequest.setUriTemplate(uriTemplate);
        apiRequest.setVisibility(visibility);

        //Add API
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        assertEquals(serviceResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Invalid Response Code");

        //copy api with default version
        HttpResponse apiCopyResponse = restAPIPublisher.copyAPI(newVersion, apiId, true);
        defaultApiId = apiCopyResponse.getData();
        assertEquals(apiCopyResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch");

        HttpResponse newVersionApi = restAPIPublisher.getAPI(defaultApiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(newVersionApi.getData(), APIDTO.class);
        boolean version = apidto.isIsDefaultVersion();

        assertEquals(version, true, "Copied API is not the default version");
        assertEquals(apidto.getName(), API_NAME, "API name is mismatched");
        assertEquals(apidto.getVersion(), newVersion, "API Version is mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(defaultApiId);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}
