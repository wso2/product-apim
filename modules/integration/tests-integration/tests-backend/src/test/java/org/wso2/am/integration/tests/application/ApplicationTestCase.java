/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 Inc. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.am.integration.tests.application;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;
import static org.testng.Assert.assertTrue;

public class ApplicationTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ApplicationTestCase.class);
    private static final String webApp = "jaxrs_basic";
    private final String version = "1.0.0";
    private final String visibility = "public";
    private final String description = "API subscription";
    private final String tier = "Unlimited";
    private final String keyType = "PRODUCTION";
    private final String tags = "subscription";
    private final String applicationName = "NewApplicationTest";
    private final String newApplicationName = "UpdatedApplicationTest";
    private final String endPointType = "http";
    private String apiName = "SubscriptionAPITest";
    private String apiContext = "subscriptionapicontext";
    private String applicationId;
    private String apiId;
    private ArrayList<String> grantTypes;
    private ApplicationDTO applicationDTO;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        grantTypes = new ArrayList<>();
        String uri = "customers/{id}/";
        String endpoint = "/services/customers/customerservice";

        String endpointUrl = gatewayUrlsWrk.getWebAppURLHttp() + webApp + endpoint;
        String providerName;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(tier);
        apiOperationsDTO.setTarget(uri);
        apiOperationsDTOS.add(apiOperationsDTO);

        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));

        apiRequest.setVersion(version);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setEndpointType(endPointType);
        apiRequest.setTiersCollection(tier);
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVisibility(visibility);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "Test Application", tier,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

    }

    @Test(groups = {"webapp"}, description = "Get Application By Application Id")
    public void testGetApplicationById() throws Exception {
        applicationDTO = restAPIStore.getApplicationById(applicationId);
        assertTrue(StringUtils.isNotEmpty(applicationDTO.getApplicationId()), "Adding application failed");
    }

    @Test(groups = {
            "webapp" }, description = "Application Key Generation By Application Id", dependsOnMethods = "testGetApplicationById")
    public void testApplicationKeyGenerationById() throws Exception {
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken().getAccessToken());
    }

    @Test(groups = {"webapp" }, description = "Update Client Application By Application Id",
            dependsOnMethods = "testApplicationKeyGenerationById")
    public void testUpdateApplicationById() throws Exception {
        String callbackUrl = "test-callback";
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        applicationDTO.setName(newApplicationName);

        ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
        applicationKeyDTO.setKeyType(ApplicationKeyDTO.KeyTypeEnum.PRODUCTION);
        applicationKeyDTO.setCallbackUrl(callbackUrl);
        applicationKeyDTO.setSupportedGrantTypes(grantTypes);

        List<ApplicationKeyDTO> applicationKeyDTOS = new ArrayList<>();
        applicationKeyDTOS.add(applicationKeyDTO);

        applicationDTO.setKeys(applicationKeyDTOS);

        HttpResponse updateResponse = restAPIStore
                .updateClientApplicationById(applicationId, applicationDTO);
        assertEquals(updateResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when adding an application");

        Gson gsonObject = new Gson();
        ApplicationDTO applicationDTOResponse = gsonObject.fromJson(updateResponse.getData(), ApplicationDTO.class);
        assertEquals(applicationDTOResponse.getName(), newApplicationName, "Application has not been updated");
    }


    @Test(groups = {"webapp" },
            description = "Add subscription By Application Id", dependsOnMethods = "testUpdateApplicationById")
    public void testAddSubscriptionApplicationById() throws Exception {
        //subscribe to the api
        HttpResponse subscriptionResponse = subscribeToAPIUsingRest(apiId, applicationId,
                tier, restAPIStore);
        assertEquals(subscriptionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when adding an application");
    }

    @Test(groups = {"webapp" },
            description = "Add subscription By Application Id", dependsOnMethods = "testUpdateApplicationById")
    public void testGetSubscriptionForApplicationById() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        //verify application names response
        boolean isApiAvailable = false;
        for (SubscriptionDTO subscriptionDTO: subsDTO.getList()){
            if (apiId.equals(subscriptionDTO.getApiId())) {
                isApiAvailable = true;
                break;
            }
        }

        assertTrue(isApiAvailable,"Response Error in Api");
    }

    @Test(groups = {"webapp" }, description = "Add subscription By Application Id",
            dependsOnMethods = "testGetSubscriptionForApplicationById")
    public void testCleanupApplicationRegistrationById() throws Exception {
        HttpResponse cleanupAppResponse;
        cleanupAppResponse = restAPIStore.cleanUpApplicationRegistrationByApplicationId(applicationId, keyType);
        assertEquals(cleanupAppResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when cleaning up an application");
    }

    @Test(groups = {"webapp" }, description = "Remove application By Application Id",
            dependsOnMethods = "testCleanupApplicationRegistrationById")
    public void testRemoveApplicationById() {
        HttpResponse removeAppResponse = restAPIStore.deleteApplication(applicationId);
        assertEquals(removeAppResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when deleting an application");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
