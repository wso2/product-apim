/*
 *
 *   Copyright (c) 2025, WSO2 LLc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 LLc. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.organization;

import static org.junit.Assert.assertFalse;
import static org.testng.Assert.assertEquals;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.OrganizationDTO;
import org.wso2.am.integration.clients.admin.api.dto.OrganizationListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OrganizationPoliciesDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ConsumerOrganizationVisibilityTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ConsumerOrganizationVisibilityTestCase.class);
    private final String DEFAULT_PROFILE = "default";
    private String enduserPassword = "password@123";
    
    private String orgId = "123-456-789";
    private String orgName = "Super";
    private String subOrg1Id =  "123-456-001";
    private String subOrg1Name = "org1";
    private String subOrg1UUID;
    private String subOrg2Id = "123-456-002";
    private String subOrg2Name = "org2";
    private String subOrg2UUID;
    
    private String orgAdmin = "orgadmin";
    private String orgPublisher = "orgpublisher";
    private String orgDevUser = "orgdevuser";
    private String subOrg1DevUser1 = "suborg1devuser1";
    private String subOrg1DevUser2 = "suborg1devuser2";
    private String subOrg2DevUser = "suborg2devuser";
    
    private RestAPIAdminImpl restAPIAdminClient;
    private RestAPIPublisherImpl restAPIPublisher;
    private RestAPIStoreImpl restAPIStore;
    private RestAPIStoreImpl suborg1user1APIStore;
    private RestAPIStoreImpl suborg1user2APIStore;
    private RestAPIStoreImpl suborg2user1APIStore;
    private RestAPIStoreImpl anonymousRestAPIImpl;
    
    
    private final String API_NAME = "OrganizationVisibilityAPI";
    private final String API_CONTEXT = "org-visibility";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "OrganizationVisibilityAPP";
    private final String APPLICATION_DESCRIPTION = "OrganizationVisibilityAPP";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId;
    private String applicationId;
    private String keyManagerId;
    
    @Factory(dataProvider = "userModeDataProvider")
    public ConsumerOrganizationVisibilityTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }
    
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        remoteClaimMetaDataMgtAdminClient.addOrganizationLocalClaim();
        // Parent Org
        addUser(orgAdmin, orgId, orgName, new String[] {"admin"});
        addUser(orgPublisher, orgId, orgName, new String[] {"Internal/creator", "Internal/publisher"});
        addUser(orgDevUser, orgId, orgName, new String[] {"Internal/subscriber"});
        // Suborg 1 user 1 and user 2
        addUser(subOrg1DevUser1, subOrg1Id, subOrg1Name, new String[] {"Internal/subscriber"});
        addUser(subOrg1DevUser2, subOrg1Id, subOrg1Name, new String[] {"Internal/subscriber"});
        // Suborg 2 user
        addUser(subOrg2DevUser, subOrg2Id, subOrg2Name, new String[] {"Internal/subscriber"});
        
        restAPIAdminClient = new RestAPIAdminImpl(orgAdmin, enduserPassword, "carbon.super", adminURLHttps);
        restAPIPublisher = new RestAPIPublisherImpl(orgPublisher, enduserPassword,
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        restAPIStore = new RestAPIStoreImpl(orgDevUser, enduserPassword, storeContext.getContextTenant().getDomain(),
                storeURLHttps);
        
        suborg1user1APIStore = new RestAPIStoreImpl(subOrg1DevUser1, enduserPassword, storeContext.getContextTenant().getDomain(),
                storeURLHttps);
        suborg1user2APIStore = new RestAPIStoreImpl(subOrg1DevUser2, enduserPassword, storeContext.getContextTenant().getDomain(),
                storeURLHttps);
        suborg2user1APIStore = new RestAPIStoreImpl(subOrg2DevUser, enduserPassword, storeContext.getContextTenant().getDomain(),
                storeURLHttps);

        //get a rest api client for anonymous user
        anonymousRestAPIImpl = getRestAPIStoreForAnonymousUser(keyManagerContext.getContextTenant().getDomain());
    }
    
    @Test(groups = {"wso2.am"}, description = "Add organization")
    public void testAddOrganization() throws Exception {
        // Add organizations
        OrganizationDTO org = new OrganizationDTO();
        org.setExternalOrganizationId(subOrg1Id);
        org.setDisplayName("Organization 1");
        ApiResponse<OrganizationDTO> response = restAPIAdminClient.addOrganization(org);
        subOrg1UUID = response.getData().getOrganizationId();

        org = new OrganizationDTO();
        org.setExternalOrganizationId(subOrg2Id);
        org.setDisplayName("Organization 2");
        restAPIAdminClient.addOrganization(org);
        response = restAPIAdminClient.addOrganization(org);
        subOrg2UUID = response.getData().getOrganizationId();
        
        OrganizationListDTO list = restAPIAdminClient.getOrganizations();
        Assert.assertEquals("Organization count mismatch", 2, list.getCount().intValue());
    }

    @Test(groups = {
            "wso2.am" }, description = "Test setting organization visibility to current org to API", dependsOnMethods = "testAddOrganization")
    public void testSetOrganizationVisibilityNoneToAPI() throws Exception {

        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(orgPublisher);

        //Create the API
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        Assert.assertEquals("Default visibility is not 'none'", "none", api.getVisibleOrganizations().get(0));
        
        //waitForAPI(api, restAPIStore);
        waitForAPIDeployment();
        
        // Check whether anonymous users can view the API in dev portal
        Assert.assertFalse("API is visible in devportal to anonymous user in list view",
                isAPIVisibleInMarketPlace(anonymousRestAPIImpl));
        assertAPIForbidden(apiId, anonymousRestAPIImpl);
        
        // Check whether parent org users can view the API in dev portal
        Assert.assertTrue("API is not visible in devportal to parentOrg user in list view",
                isAPIVisibleInMarketPlace(restAPIStore));
        Assert.assertTrue("API is not visible in devportal to parentOrg user",
                canViewAPIInDevPortal(apiId, restAPIStore));

        // Check whether suborg users can view the API in dev portal
        Assert.assertFalse("API is visible in devportal to subOrg user in list view",
                isAPIVisibleInMarketPlace(suborg1user1APIStore));
        assertAPIForbidden(apiId, suborg1user1APIStore);
    }

    @Test(groups = {
            "wso2.am" }, description = "Test setting organization visibility to specific organization", 
                    dependsOnMethods = "testSetOrganizationVisibilityNoneToAPI")
    public void testSetOrganizationVisibilityForAnOrgToAPI() throws Exception {
        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        List<String> visibleOrganizations = new ArrayList<String>();
        visibleOrganizations.add(subOrg1UUID);
        api.setVisibleOrganizations(visibleOrganizations);
        restAPIPublisher.updateAPI(api, apiId);
        api = restAPIPublisher.getAPIByID(apiId);
        Assert.assertEquals("Organization visibility is not " + subOrg1UUID, subOrg1UUID,
                api.getVisibleOrganizations().get(0));
        waitForAPIDeployment();
        
        // Check whether anonymous users can view the API in dev portal
        Assert.assertFalse("API is visible in devportal to anonymous user in list view",
                isAPIVisibleInMarketPlace(anonymousRestAPIImpl));
        assertAPIForbidden(apiId, anonymousRestAPIImpl);
        
        // Check whether parent org users can view the API in dev portal
        Assert.assertTrue("API is not visible in devportal to parentOrg user in list view",
                isAPIVisibleInMarketPlace(restAPIStore));
        Assert.assertTrue("API is not visible in devportal to parentOrg user",
                canViewAPIInDevPortal(apiId, restAPIStore));

        // Check whether suborg1 users can view the API in dev portal
        Assert.assertTrue("API is not visible in devportal to subOrg 1 user in list view",
                isAPIVisibleInMarketPlace(suborg1user1APIStore));
        Assert.assertTrue("API is not visible in devportal to subOrg 1 user",
                canViewAPIInDevPortal(apiId, suborg1user1APIStore));
        
        // Check whether suborg2 users can view the API in dev portal
        Assert.assertFalse("API is visible in devportal to subOrg 2 user in list view",
                isAPIVisibleInMarketPlace(suborg2user1APIStore));
        assertAPIForbidden(apiId, suborg2user1APIStore);
    }

    @Test(groups = {
            "wso2.am" }, description = "Test setting organization visibility to all organizations", 
                    dependsOnMethods = "testSetOrganizationVisibilityNoneToAPI")
    public void testSetOrganizationVisibilityForAllOrgsToAPI() throws Exception {
        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        List<String> visibleOrganizations = new ArrayList<String>();
        visibleOrganizations.add("all");
        api.setVisibleOrganizations(visibleOrganizations);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(api, apiId);
        Assert.assertEquals("Default visibility is not to 'all'", "all", updatedAPI.getVisibleOrganizations().get(0));
        
        waitForAPIDeployment();
        
        // Check whether anonymous users can view the API in dev portal
        Assert.assertTrue("API is not visible in devportal to anonymous user in list view",
                isAPIVisibleInMarketPlace(anonymousRestAPIImpl));
        Assert.assertTrue("API is not visible in devportal to anonymous user",
                canViewAPIInDevPortal(apiId, anonymousRestAPIImpl));
        
        // Check whether parent org users can view the API in dev portal
        Assert.assertTrue("API is not visible in devportal to parentOrg user in list view",
                isAPIVisibleInMarketPlace(restAPIStore));
        Assert.assertTrue("API is not visible in devportal to parentOrg user",
                canViewAPIInDevPortal(apiId, restAPIStore));

        // Check whether suborg1 users can view the API in dev portal
        Assert.assertTrue("API is not visible in devportal to subOrg 1 user in list view",
                isAPIVisibleInMarketPlace(suborg1user1APIStore));
        Assert.assertTrue("API is not visible in devportal to subOrg 1 user",
                canViewAPIInDevPortal(apiId, suborg1user1APIStore));
        
        // Check whether suborg2 users can view the API in dev portal
        Assert.assertTrue("API is visible in devportal to subOrg 2 user in list view",
                isAPIVisibleInMarketPlace(suborg2user1APIStore));
        Assert.assertTrue("API is not visible in devportal to subOrg 2 user",
                canViewAPIInDevPortal(apiId, suborg2user1APIStore));
    }
    
    @Test(groups = {
            "wso2.am" }, description = "Test keymanager visibility for organizations", dependsOnMethods = "testAddOrganization")
    public void testKeymanagerVisibility() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type with mandatory and some optional parameters
        String name = "OrgKeyManager";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String tokenEndpoint = "https://wso2is.com:9444/oauth2/token";
        String revokeEndpoint = "https://wso2is.com:9444/oauth2/revoke";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Username", "admin");
        jsonObject.addProperty("Password", "admin");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional parameters
        String description = "This is a test key manager";
        String issuer = "https://localhost:9444/services";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token");
        String certificateValue = "https://localhost:9443/oauth2/jwks";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.JWKS, certificateValue);

        KeyManagerDTO keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);
        List<String> allowedOrgs = new ArrayList<String>();
        allowedOrgs.add(subOrg1UUID); // Set keymanager visibility to suborg
        keyManagerDTO.setAllowedOrganizations(allowedOrgs);

        //Add the WSO2 IS key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdminClient.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        
        // Check sub org 1 devportal user can view the keymanager
        KeyManagerListDTO devPortalKeymanagers = suborg1user1APIStore.getKeyManagers();
        boolean keymanagerAvailable = false;
        for (KeyManagerInfoDTO keyManagerInfoDTO : devPortalKeymanagers.getList()) {
            if (keyManagerId.equals(keyManagerInfoDTO.getId())) {
                keymanagerAvailable = true;
                break;
            }
        }
        Assert.assertTrue("Org keymanager not visible to organization user", keymanagerAvailable);
        
        // Check sub org 2 devportal user can view the keymanager
        devPortalKeymanagers = suborg2user1APIStore.getKeyManagers();
        keymanagerAvailable = false;
        for (KeyManagerInfoDTO keyManagerInfoDTO : devPortalKeymanagers.getList()) {
            if (keyManagerId.equals(keyManagerInfoDTO.getId())) {
                keymanagerAvailable = true;
                break;
            }
        }
        Assert.assertFalse("Org keymanager visible to unintended organization user", keymanagerAvailable);
        
        allowedOrgs = new ArrayList<String>();
        allowedOrgs.add("none"); // Set keymanager visibility to none to make it not available for org users
        keyManagerDTO.setAllowedOrganizations(allowedOrgs);
        ApiResponse<KeyManagerDTO> updateKM = restAPIAdminClient.updateKeyManager(keyManagerId, keyManagerDTO);
        Assert.assertEquals("Keymanager organization visibility not changed", "none",
                updateKM.getData().getAllowedOrganizations().get(0));
        
        // Check sub org 1 devportal user can view the keymanager
        devPortalKeymanagers = suborg1user1APIStore.getKeyManagers();
        keymanagerAvailable = false;
        for (KeyManagerInfoDTO keyManagerInfoDTO : devPortalKeymanagers.getList()) {
            if (keyManagerId.equals(keyManagerInfoDTO.getId())) {
                keymanagerAvailable = true;
                break;
            }
        }
        Assert.assertFalse("Org keymanager visible to unintended organization user", keymanagerAvailable);
        
        // Check sub org 2 devportal user can view the keymanager
        devPortalKeymanagers = suborg2user1APIStore.getKeyManagers();
        keymanagerAvailable = false;
        for (KeyManagerInfoDTO keyManagerInfoDTO : devPortalKeymanagers.getList()) {
            if (keyManagerId.equals(keyManagerInfoDTO.getId())) {
                keymanagerAvailable = true;
                break;
            }
        }
        Assert.assertFalse("Org keymanager visible to unintended organization user", keymanagerAvailable);
        
    }
    
    @Test(groups = {
            "wso2.am" }, description = "Test application sharing between organization", dependsOnMethods = "testAddOrganization" )
    public void testApplicationSharingBetweenOrganizations() throws Exception {
        // Create application using suborg 1 user 1 and share with organization
        HttpResponse applicationResponse = suborg1user1APIStore.createApplicationWithOrganizationSharing(
                APPLICATION_NAME, APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT, ApplicationDTO.VisibilityEnum.SHARED_WITH_ORG);
        applicationId = applicationResponse.getData();
        // Check application using suborg1 user 2
        HttpResponse response = suborg1user2APIStore.getApplicationByIdWithHttpResponse(applicationId);
        Assert.assertEquals("Application is not visible to organization user", 200, response.getResponseCode());
        
        // Check whether application is visible to suborg 2
        response = suborg2user1APIStore.getApplicationByIdWithHttpResponse(applicationId);
        Assert.assertEquals("Application is visible to different org user", 403, response.getResponseCode());
        
        // Update application and remove organization sharing
        applicationResponse = suborg1user1APIStore.updateApplicationByID(applicationId, APPLICATION_NAME,
                APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT, ApplicationDTO.VisibilityEnum.PRIVATE);
        // Check application using suborg1 user 2
        response = suborg1user2APIStore.getApplicationByIdWithHttpResponse(applicationId);
        Assert.assertEquals("Application is still visible to organization user", 403, response.getResponseCode());
    }
    
    @Test(groups = { "wso2.am" }, description = "Test organization specific subscription policies", dependsOnMethods = {
            "testSetOrganizationVisibilityNoneToAPI", "testApplicationSharingBetweenOrganizations" })
    public void testOrgSpecificSubscriptionPolicies() throws Exception {

        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        List<String> visibleOrganizations = new ArrayList<String>();
        visibleOrganizations.add(subOrg1UUID);
        api.setVisibleOrganizations(visibleOrganizations);
        
        // Set bronze tier as sub organization business plan.
        List<OrganizationPoliciesDTO> orgPolicies = new ArrayList<OrganizationPoliciesDTO>();
        OrganizationPoliciesDTO policyDto = new OrganizationPoliciesDTO();
        policyDto.setOrganizationID(subOrg1UUID);
        List<String> orgPoliciesList = new ArrayList<String>();
        orgPoliciesList.add(APIMIntegrationConstants.API_TIER.BRONZE);
        policyDto.setPolicies(orgPoliciesList);
        orgPolicies.add(policyDto);

        api.setOrganizationPolicies(orgPolicies);
        restAPIPublisher.updateAPI(api, apiId);
        api = restAPIPublisher.getAPIByID(apiId);
        Assert.assertEquals("Organization visibility is not " + subOrg1UUID, subOrg1UUID,
                api.getVisibleOrganizations().get(0));
        Assert.assertEquals("Organization policy ID is not " + subOrg1UUID, subOrg1UUID,
                api.getOrganizationPolicies().get(0).getOrganizationID());
        Assert.assertEquals("Organization policy is not " + APIMIntegrationConstants.API_TIER.BRONZE,
                APIMIntegrationConstants.API_TIER.BRONZE, api.getOrganizationPolicies().get(0).getPolicies().get(0));
        waitForAPIDeployment();
        
        // Check parent org has unlimited business plan
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO orgAPI = restAPIStore.getAPI(apiId);
        Assert.assertEquals("Available policy count mismatch for parent org", 1, orgAPI.getTiers().size());
        Assert.assertEquals("Available policy mismatch for parent org", APIMIntegrationConstants.API_TIER.UNLIMITED,
                orgAPI.getTiers().get(0).getTierName());
        
        // Check sub org 1 has only bronze business plan for subscription
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO subOrgAPI = suborg1user1APIStore.getAPI(apiId);
        Assert.assertEquals("Available policy count mismatch for suborg", 1, subOrgAPI.getTiers().size());
        Assert.assertEquals("Available policy mismatch for suborg", APIMIntegrationConstants.API_TIER.BRONZE,
                subOrgAPI.getTiers().get(0).getTierName());

        // Subscribe to the API using bronze by suborg user
         HttpResponse response = suborg1user1APIStore
                .subscribeToAPIWithResponse(apiId, applicationId, APIMIntegrationConstants.API_TIER.BRONZE);

        Assert.assertEquals(HttpStatus.SC_CREATED, response.getResponseCode());
        response = suborg1user1APIStore.removeSubscriptionWithHttpInfo(response.getData());
        Assert.assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        
        // Subscribe to the API using parent org's plan by suborg user
        response = suborg1user1APIStore.subscribeToAPIWithResponse(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getResponseCode());
    }
    
    private boolean isAPIVisibleInMarketPlace(RestAPIStoreImpl storeImpl) throws ApiException {
        APIListDTO apiListDto = storeImpl.getAllAPIs();
        boolean isVisible = false;
        for (APIInfoDTO apiInfoDTO : apiListDto.getList()) {
            if (apiId.equals(apiInfoDTO.getId())) {
                isVisible = true;
            }
        }
        return isVisible;
    }

    private boolean canViewAPIInDevPortal(String apiUUID, RestAPIStoreImpl storeImpl) throws ApiException {
        boolean canView = false;
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO api = storeImpl.getAPI(apiUUID);
        if (api != null) {
           if(API_NAME.equals(api.getName())){
               canView = true;
           }
        }
        return canView;
    }
    private void assertAPIForbidden(String apiUUID, RestAPIStoreImpl storeImpl) throws ApiException {
        try {
            storeImpl.getAPI(apiId);
            assertFalse("API is visible in developer portal.", true);
        } catch (ApiException e) {
            assertEquals(e.getCode(), HTTP_RESPONSE_CODE_FORBIDDEN, "Response code mismatch.");
        }
    }
    
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        suborg1user1APIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIAdminClient.deleteKeyManager(keyManagerId);
        restAPIAdminClient.deleteOrganization(subOrg1UUID);
        restAPIAdminClient.deleteOrganization(subOrg2UUID);
    }
    private void addUser(String username, String organizationId, String organization, String[] roles)
            throws UserStoreException, RemoteException, RemoteUserStoreManagerServiceUserStoreExceptionException {

        remoteUserStoreManagerServiceClient.addUser(username, enduserPassword, roles, new ClaimValue[] {},
                DEFAULT_PROFILE, false);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/givenname",
                "first name".concat(username), DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/lastname",
                "last name".concat(username), DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/organization",
                organization, DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/organizationId",
                organizationId, DEFAULT_PROFILE);

    }
}
