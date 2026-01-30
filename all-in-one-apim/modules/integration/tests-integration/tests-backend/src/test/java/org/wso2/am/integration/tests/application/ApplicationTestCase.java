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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.application.ApplicationManagementClient;
import org.wso2.am.admin.clients.oauth.OAuthAdminServiceClient;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyReGenerateResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.xsd.Property;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;

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
    private String applicationId1;
    private String applicationId2;
    private String applicationId3;
    private String app3KeyMappingId;
    private ArrayList<String> grantTypes;
    private ApplicationDTO applicationDTO;
    protected ApplicationManagementClient applicationManagementClient;
    private String oidcApp1ClientId;
    private String oidcApp1ClientSecret;
    private String oidcApp2ClientId;
    private String oidcApp2ClientSecret;

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

        applicationManagementClient =
                new ApplicationManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                        keymanagerSessionCookie);
        oAuthAdminServiceClient =
                new OAuthAdminServiceClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                        keymanagerSessionCookie);
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

    @Test(groups = {"webapp"}, description = "Map application keys negative test case")
    public void mapApplicationKeysNegative() throws Exception {
        OAuthConsumerAppDTO oAuthApplicationData = createOIDCApplication("OauthApp1");
        oidcApp1ClientId = oAuthApplicationData.getOauthConsumerKey();
        oidcApp1ClientSecret = oAuthApplicationData.getOauthConsumerSecret();

        createServiceProvider("OauthApp1", oidcApp1ClientId, oidcApp1ClientSecret);

        HttpResponse applicationDTO =
                restAPIStore.createApplication("DevPortalApp1", "JWT Application",
                        APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId1 = applicationDTO.getData();

        try {
            restAPIStore.generateKeys(applicationId1, "36000", "",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
            restAPIStore.mapConsumerKeyWithApplication(oidcApp1ClientId, oidcApp1ClientSecret, applicationId1,
                    "Resident Key Manager");
        } catch (ApiException e) {
            Assert.assertEquals(409, e.getCode());
            Assert.assertTrue(e.getResponseBody().contains("Key Mappings already exists"));
        }
    }

    @Test(groups = {"webapp"}, description = "Map application keys test case",
            dependsOnMethods = "mapApplicationKeysNegative")
    public void mapApplicationKeys() throws Exception {

        OAuthConsumerAppDTO oAuthApplicationData = createOIDCApplication("OauthApp2");
        oidcApp2ClientId = oAuthApplicationData.getOauthConsumerKey();
        oidcApp2ClientSecret = oAuthApplicationData.getOauthConsumerSecret();

        createServiceProvider("OauthApp2", oidcApp2ClientId, oidcApp2ClientSecret);

        HttpResponse applicationDTO =
                restAPIStore.createApplication("DevPortalApp2", "JWT Application",
                        APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId2 = applicationDTO.getData();

        try {
            //Map application Keys
            ApplicationKeyDTO applicationKeyDTO = restAPIStore.mapConsumerKeyWithApplication(oidcApp2ClientId,
                    oidcApp2ClientSecret, applicationId2, "Resident Key Manager");
            Assert.assertNotNull(applicationKeyDTO);
            restAPIStore.generateKeys(applicationId2, "3600", "",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        } catch (ApiException e) {
            Assert.assertEquals(409, e.getCode());
            Assert.assertTrue(e.getResponseBody().contains("Key Mappings already exists"));
        }
    }

    @Test(groups = {"webapp"}, description = "Fetch Oauth key details by key mapping ID")
    public void testFetchKeyDetailsByKeyMappingID() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication("KeyMappingTestApp",
                "Test Application", tier, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Error while adding test application");

        applicationId3 = applicationResponse.getData();
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId3, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO);
        app3KeyMappingId = applicationKeyDTO.getKeyMappingId();

        ApplicationKeyDTO responseKeyDTO = restAPIStore.getApplicationKeyByKeyMappingId(applicationId3, app3KeyMappingId);
        Assert.assertNotNull(responseKeyDTO);
        Assert.assertNotNull(responseKeyDTO.getConsumerKey(), "Consumer secret is not populated in REST API response");
        Assert.assertEquals(responseKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerKey(),
                "Incorrect consumer key returned");
    }

    @Test(groups = {"webapp" }, description = "Regenerate secret by key mapping ID",
            dependsOnMethods = "testFetchKeyDetailsByKeyMappingID")
    public void testRegenerateSecretForKeyMappingId() throws Exception {
        ApplicationKeyReGenerateResponseDTO reGenerateResponseDTO = restAPIStore
                .regenerateSecretByKeyMappingId(applicationId3, app3KeyMappingId);
        Assert.assertNotNull(reGenerateResponseDTO);
        Assert.assertNotNull(reGenerateResponseDTO.getConsumerSecret(), "Secret is not populated");
    }

    private OAuthConsumerAppDTO createOIDCApplication(String applicationName) throws Exception {

        OAuthConsumerAppDTO appDTO = new OAuthConsumerAppDTO();
        appDTO.setApplicationName(applicationName);
        appDTO.setCallbackUrl("http://localhost:8490/playground2/oauth2clien");
        appDTO.setOAuthVersion("OAuth-2.0");
        appDTO.setGrantTypes("authorization_code");
        appDTO.setBackChannelLogoutUrl("http://localhost:8490/playground2/bclogout");

        oAuthAdminServiceClient.registerOAuthApplicationData(appDTO);
        OAuthConsumerAppDTO createdApp = oAuthAdminServiceClient.getOAuthAppByName(applicationName);
        Assert.assertNotNull(createdApp);
        return createdApp;
    }

    private ServiceProvider createServiceProvider(String applicationName, String clientId, String clientSecret) throws Exception {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(applicationName);
        applicationManagementClient.createApplication(serviceProvider);
        serviceProvider = applicationManagementClient.getApplication(applicationName);

        InboundAuthenticationRequestConfig requestConfig = new InboundAuthenticationRequestConfig();
        requestConfig.setInboundAuthKey(clientId);
        requestConfig.setInboundAuthType("oauth2");
        if (StringUtils.isNotBlank(clientSecret)) {
            Property property = new Property();
            property.setName("oauthConsumerSecret");
            property.setValue(clientSecret);
            Property[] properties = {property};
            requestConfig.setProperties(properties);
        }

        InboundAuthenticationConfig inboundAuthenticationConfig = new InboundAuthenticationConfig();
        inboundAuthenticationConfig
                .setInboundAuthenticationRequestConfigs(new InboundAuthenticationRequestConfig[]{requestConfig});
        serviceProvider.setInboundAuthenticationConfig(inboundAuthenticationConfig);
        applicationManagementClient.updateApplication(serviceProvider);
        return serviceProvider;
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIStore.deleteApplication(applicationId1);
        restAPIStore.deleteApplication(applicationId2);
        applicationManagementClient.deleteApplication("OauthApp1");
        applicationManagementClient.deleteApplication("OauthApp2");
    }
}
