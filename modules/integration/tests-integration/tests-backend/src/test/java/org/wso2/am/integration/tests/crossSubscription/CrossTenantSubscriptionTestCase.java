package org.wso2.am.integration.tests.crossSubscription;

import jdk.internal.joptsimple.internal.Strings;
import org.apache.http.HttpStatus;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyPermissionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APITiersDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ThrottlingPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ThrottlingPolicyListDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.um.ws.api.stub.PermissionDTO;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.xpath.XPathExpressionException;

import static org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO.SubscriptionAvailabilityEnum.ALL_TENANTS;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class CrossTenantSubscriptionTestCase extends APIManagerLifecycleBaseTest {

    private static final String tenant1Name = "crosstenant1.com";
    private static final String tenant2Name = "crosstenant2.com";
    private static RestAPIPublisherImpl apiPublisherRestClientTenant1;
    private static RestAPIPublisherImpl apiPublisherRestClientTenant2;
    private static RestAPIStoreImpl apiStoreRestClientTenant1;
    private static RestAPIStoreImpl apiStoreRestClientTenant2;
    private static RestAPIAdminImpl restAPIAdminTenant1;
    private static RestAPIAdminImpl restAPIAdminTenant2;
    private final String apiName = "CrossTenantSubscriptionAPI";
    private final String apiContext = "crossTenantSubscriptionAPI";
    private final String apiVersion = "1.0.0";
    String apiPrototypeEndpointPostfixUrl = "am/sample/pizzashack/v1/api";
    private String apiEndPointUrl;
    String apiId2;
    String apiId1;
    private final String timeUnit = "min";
    private final Integer unitTime = 1;
    private final Integer rateLimitCount = -1;
    private final String rateLimitTimeUnit = "NA";
    ApplicationDTO tenant1Application;
    ApplicationDTO tenant2Application;
    String policyIdTenant1;
    String policyIdTenant2;
    private KeyManagerDTO keymanagerTenant1;
    private KeyManagerDTO keymanagerTenant2;
    private String residentKMTenant1;
    private String residentKMTenant2;
    private ApplicationKeyDTO tenant1AppTenant2Store;
    private ApplicationKeyDTO tenant1AppTenant1Store;
    private ApplicationKeyDTO tenant2AppTenant2Store;
    private RemoteUserStoreManagerServiceClient tenant1UserStoreManager;
    private RemoteUserStoreManagerServiceClient tenant2UserStoreManager;
    private ApplicationDTO tenant3Application;
    private ApplicationDTO tenant4Application;
    private ApplicationDTO tenant5Application;
    private final String tenantApp5 = "Tenant5App";
    private final String tenant2AppPolicy = "Tenant2AppPolicy";
    private final String errorMessageKeyGeneration = "Error occurred while generating keys";
    private final String errorMessageTokenGeneration = "Error occurred while generating access token";
    private SubscriptionThrottlePolicyDTO testPolicyTenant1Public;
    private SubscriptionThrottlePolicyDTO testPolicyTenant2Public;
    private SubscriptionThrottlePolicyDTO testPolicyTenant1Restricted;
    private SubscriptionThrottlePolicyDTO testPolicyTenant2Restricted;

    @Factory(dataProvider = "userModeDataProvider")
    public CrossTenantSubscriptionTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiPrototypeEndpointPostfixUrl;

        tenantManagementServiceClient.addTenant(tenant1Name, "wso2carbon",
                "firstTenantAdmin", "demo");
        tenantManagementServiceClient.addTenant(tenant2Name, "wso2carbon",
                "secondTenantAdmin", "demo");
        apiPublisherRestClientTenant1 = new RestAPIPublisherImpl("firstTenantAdmin", "wso2carbon", tenant1Name,
                publisherURLHttps);
        apiPublisherRestClientTenant2 = new RestAPIPublisherImpl("secondTenantAdmin", "wso2carbon", tenant2Name,
                publisherURLHttps);
        apiStoreRestClientTenant1 = new RestAPIStoreImpl("firstTenantAdmin", "wso2carbon", tenant1Name,
                publisherURLHttps);
        apiStoreRestClientTenant2 = new RestAPIStoreImpl("secondTenantAdmin", "wso2carbon", tenant2Name,
                publisherURLHttps);
        restAPIAdminTenant1 = new RestAPIAdminImpl("firstTenantAdmin", "wso2carbon", tenant1Name, publisherURLHttps);
        restAPIAdminTenant2 = new RestAPIAdminImpl("secondTenantAdmin", "wso2carbon", tenant2Name, publisherURLHttps);
        tenant1UserStoreManager =
                new RemoteUserStoreManagerServiceClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                        "firstTenantAdmin@".concat(tenant1Name), "wso2carbon");
        tenant2UserStoreManager =
                new RemoteUserStoreManagerServiceClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                        "secondTenantAdmin@".concat(tenant2Name), "wso2carbon");
        tenant1UserStoreManager.addRole("role1", new String[]{"firstTenantAdmin"}, new PermissionDTO[0]);
        tenant2UserStoreManager.addRole("role1", new String[]{"secondTenantAdmin"}, new PermissionDTO[0]);
        createSubscriptionPolicyForTenant1();
        createSubscriptionPolicyForTenant2();
        createSubscriptionPolicyForTenant1RoleRestricted();
        createSubscriptionPolicyForTenant2Restricted();
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider("firstTenantAdmin@".concat(tenant1Name));
        apiRequest.setSubscriptionAvailability(ALL_TENANTS.toString());
        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("oauth2");
        securitySchemes.add("api_key");
        apiRequest.setSecurityScheme(securitySchemes);
        apiRequest.setTiersCollection(String.join(",", "TestPolicyTenant1Public", "TestPolicyTenant1Restricted"));
        apiId1 = createAndPublishAPIUsingRest(apiRequest, apiPublisherRestClientTenant1, false);
        // Create API2 in Tenant2.
        apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider("secondTenantAdmin@".concat(tenant2Name));
        apiRequest.setSecurityScheme(securitySchemes);
        apiRequest.setTiersCollection(String.join(",", "TestPolicyTenant2Public", "TestPolicyTenant2Restricted"));
        apiRequest.setSubscriptionAvailability(ALL_TENANTS.toString());
        apiId2 = createAndPublishAPIUsingRest(apiRequest, apiPublisherRestClientTenant2, false);
        Long requestCount = 5L;
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        ApplicationThrottlePolicyDTO requestCountPolicyDTO = DtoFactory
                .createApplicationThrottlePolicyDTO("Tenant1AppPolicy", "Tenant1AppPolicy", "description", false,
                        defaultLimit);

        //Add the application throttling policy to tenant1
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy1 =
                restAPIAdminTenant1.addApplicationThrottlingPolicy(requestCountPolicyDTO);
        Assert.assertEquals(addedPolicy1.getStatusCode(), HttpStatus.SC_CREATED);
        ApplicationThrottlePolicyDTO addedPolicyDTO = addedPolicy1.getData();
        policyIdTenant1 = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyIdTenant1, "The policy ID cannot be null or empty");
        //Add the application throttling policy to tenant2
        requestCountPolicyDTO.setPolicyName("Tenant2AppPolicy");
        requestCountPolicyDTO.setDisplayName("Tenant2AppPolicy");
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy2 =
                restAPIAdminTenant2.addApplicationThrottlingPolicy(requestCountPolicyDTO);
        Assert.assertEquals(addedPolicy2.getStatusCode(), HttpStatus.SC_CREATED);
        addedPolicyDTO = addedPolicy2.getData();
        policyIdTenant2 = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyIdTenant2, "The policy ID cannot be null or empty");
        // Create KM in tenant1.
        KeyManagerCertificatesDTO keyManagerCertificatesDTO = new KeyManagerCertificatesDTO();
        keyManagerCertificatesDTO.setType(KeyManagerCertificatesDTO.TypeEnum.JWKS);
        keyManagerCertificatesDTO.setValue("https://localhost:8743/jwks/1.0");
        KeyManagerDTO keyManagerDTO = DtoFactory.createKeyManagerDTO("Tenant1KM", "Tenant1KM", "custom", "Tenant1KM",
                "https://teant1.com", "azp", "scopes", Collections.singletonList("client_credentials"),
                keyManagerCertificatesDTO);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(false);
        keyManagerDTO.setEnableOAuthAppCreation(false);
        ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse = restAPIAdminTenant1.addKeyManager(keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 201);
        keymanagerTenant1 = keyManagerDTOApiResponse.getData();
        // Create KM in tenant2.
        KeyManagerDTO keyManagerDTO2 = DtoFactory.createKeyManagerDTO("Tenant2KM", "Tenant1KM", "custom", "Tenant1KM",
                "https://teant2.com", "azp", "scopes", Collections.singletonList("client_credentials"),
                keyManagerCertificatesDTO);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(false);
        keyManagerDTO.setEnableOAuthAppCreation(false);
        keyManagerDTOApiResponse = restAPIAdminTenant2.addKeyManager(keyManagerDTO2);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 201);
        keymanagerTenant2 = keyManagerDTOApiResponse.getData();
        for (org.wso2.am.integration.clients.admin.api.dto.KeyManagerInfoDTO keyManagerInfoDTO :
                restAPIAdminTenant1.getKeyManagers().getList()) {
            if ("Resident Key Manager".equals(keyManagerInfoDTO.getName())) {
                residentKMTenant1 = keyManagerInfoDTO.getId();
            }
        }
        for (org.wso2.am.integration.clients.admin.api.dto.KeyManagerInfoDTO keyManagerInfoDTO :
                restAPIAdminTenant2.getKeyManagers().getList()) {
            if ("Resident Key Manager".equals(keyManagerInfoDTO.getName())) {
                residentKMTenant2 = keyManagerInfoDTO.getId();
            }
        }
    }

    public void createSubscriptionPolicyForTenant1() throws org.wso2.am.integration.clients.admin.ApiException {
        //Create the subscription throttling policy DTO with request count limit
        String policyName = "TestPolicyTenant1Public";
        Long requestCount = 10L;
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        SubscriptionThrottlePolicyDTO requestCountPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, policyName, "description", true, defaultLimit,
                        rateLimitCount, rateLimitTimeUnit, true, null);

        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdminTenant1.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        testPolicyTenant1Public = addedPolicy.getData();
        String policyId = testPolicyTenant1Public.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

    }

    public void createSubscriptionPolicyForTenant2() throws org.wso2.am.integration.clients.admin.ApiException {
        //Create the subscription throttling policy DTO with request count limit
        String policyName = "TestPolicyTenant2Public";
        Long requestCount = 10L;
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        SubscriptionThrottlePolicyDTO requestCountPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, policyName, "description", true, defaultLimit,
                        rateLimitCount, rateLimitTimeUnit, true, null);

        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdminTenant2.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        testPolicyTenant2Public = addedPolicy.getData();
        String policyId = testPolicyTenant2Public.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");
    }

    public void createSubscriptionPolicyForTenant1RoleRestricted() throws org.wso2.am.integration.clients.admin.ApiException {
        //Create the subscription throttling policy DTO with request count limit
        String policyName = "TestPolicyTenant1Restricted";
        Long requestCount = 10L;
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        SubscriptionThrottlePolicyPermissionDTO permissions =
                new SubscriptionThrottlePolicyPermissionDTO().permissionType(SubscriptionThrottlePolicyPermissionDTO.PermissionTypeEnum.ALLOW).roles(Collections.singletonList("role1"));
        SubscriptionThrottlePolicyDTO requestCountPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, policyName, "description", true, defaultLimit,
                        rateLimitCount, rateLimitTimeUnit, true, permissions);

        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdminTenant1.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        testPolicyTenant1Restricted = addedPolicy.getData();
        String policyId = testPolicyTenant1Restricted.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

    }

    public void createSubscriptionPolicyForTenant2Restricted() throws org.wso2.am.integration.clients.admin.ApiException {
        //Create the subscription throttling policy DTO with request count limit
        String policyName = "TestPolicyTenant2Restricted";
        Long requestCount = 10L;
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        SubscriptionThrottlePolicyPermissionDTO permissions =
                new SubscriptionThrottlePolicyPermissionDTO().permissionType(SubscriptionThrottlePolicyPermissionDTO.PermissionTypeEnum.ALLOW).roles(Collections.singletonList("role1"));
        SubscriptionThrottlePolicyDTO requestCountPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, policyName, "description", true, defaultLimit,
                        rateLimitCount, rateLimitTimeUnit, true, permissions);

        //Add the subscription throttling policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdminTenant2.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        testPolicyTenant2Restricted = addedPolicy.getData();
        String policyId = testPolicyTenant2Restricted.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of API from cross Tenant")
    public void testVisibilityOfAPIFromOtherDomain() throws ApiException {

        APIListDTO allAPIs = apiStoreRestClientTenant1.getAllAPIs(tenant2Name);
        Assert.assertNotNull(allAPIs);
        assert allAPIs.getCount() != null;
        Assert.assertEquals(allAPIs.getCount().intValue(), 1);
        boolean found = false;
        assert allAPIs.getList() != null;
        for (APIInfoDTO apiInfoDTO : allAPIs.getList()) {
            if (apiId2.equals(apiInfoDTO.getId())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "API with ID" + apiId1 + "not found");
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of API from cross Tenant")
    public void testVisibilityOfAPIFromOtherDomain2() throws ApiException, InterruptedException {

        boolean found = false;
        int tries = 0;
        while (!found && tries < 15) {
            APIListDTO allAPIs = apiStoreRestClientTenant2.getAllAPIs(tenant1Name);
            Assert.assertNotNull(allAPIs);
            assert allAPIs.getCount() != null;
            if (allAPIs.getCount() == 1) {
                assert allAPIs.getList() != null;
                for (APIInfoDTO apiInfoDTO : allAPIs.getList()) {
                    if (apiId1.equals(apiInfoDTO.getId())) {
                        found = true;
                        break;
                    }
                }
            }
            tries++;
            if (!found) {
                Thread.sleep(5000);
            }
        }
        Assert.assertTrue(found, "API with ID" + apiId1 + "not found");
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of API from cross Tenant")
    public void testDirectAPIAvailability() throws ApiException {

        APIDTO api = apiStoreRestClientTenant1.getAPI(apiId2, tenant2Name);
        Assert.assertNotNull(api);
        Assert.assertEquals(api.getName(), apiName);
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of API from cross Tenant")
    public void testDirectAPIAvailability2() throws ApiException {

        APIDTO api = apiStoreRestClientTenant2.getAPI(apiId1, tenant1Name);
        Assert.assertNotNull(api);
        Assert.assertEquals(api.getName(), apiName);
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of ApplicationPolicy from cross Tenant")
    public void testApplicationPolicyAvailabilityInTenant2() throws ApiException {

        ThrottlingPolicyListDTO applicationPolicies = apiStoreRestClientTenant2.getApplicationPolicies(tenant1Name);
        Assert.assertNotNull(applicationPolicies);
        boolean tenant1AppPolicyFound = false;
        boolean tenant2AppPolicyFound = false;
        assert applicationPolicies.getList() != null;
        for (ThrottlingPolicyDTO applicationPolicy : applicationPolicies.getList()) {
            if ("Tenant1AppPolicy".equals(applicationPolicy.getName())) {
                tenant1AppPolicyFound = true;
            }
            if ("Tenant2AppPolicy".equals(applicationPolicy.getName())) {
                tenant2AppPolicyFound = true;
            }
        }
        Assert.assertTrue(tenant2AppPolicyFound, "Tenant2AppPolicy didn't found");
        Assert.assertFalse(tenant1AppPolicyFound, "Tenant1AppPolicy found");

    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of ApplicationPolicy from cross Tenant")
    public void testApplicationPolicyAvailabilityInTenant1() throws ApiException {

        ThrottlingPolicyListDTO applicationPolicies = apiStoreRestClientTenant1.getApplicationPolicies(tenant2Name);
        Assert.assertNotNull(applicationPolicies);
        boolean tenant1AppPolicyFound = false;
        boolean tenant2AppPolicyFound = false;
        assert applicationPolicies.getList() != null;
        for (ThrottlingPolicyDTO applicationPolicy : applicationPolicies.getList()) {
            if ("Tenant1AppPolicy".equals(applicationPolicy.getName())) {
                tenant1AppPolicyFound = true;
            }
            if ("Tenant2AppPolicy".equals(applicationPolicy.getName())) {
                tenant2AppPolicyFound = true;
            }
        }
        Assert.assertFalse(tenant2AppPolicyFound, "Tenant2AppPolicy found");
        Assert.assertTrue(tenant1AppPolicyFound, "Tenant1AppPolicy didn't found");
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of SubscriptionPolicy from cross Tenant")
    public void testSubscriptionPolicyAvailabilityInTenant1() throws ApiException {

        APIDTO api = apiStoreRestClientTenant2.getAPI(apiId1, tenant1Name);
        Assert.assertNotNull(api);
        boolean publicPolicyFound = false;
        boolean restrictedPolicyFound = false;
        List<APITiersDTO> tiers = api.getTiers();
        assert tiers != null;
        for (APITiersDTO tier : tiers) {
            if ("TestPolicyTenant1Public".equals(tier.getTierName())) {
                publicPolicyFound = true;
            }
            if ("TestPolicyTenant1Restricted".equals(tier.getTierName())) {
                restrictedPolicyFound = true;
            }
        }
        Assert.assertTrue(publicPolicyFound, "TestPolicyTenant1Public not found");
        Assert.assertFalse(restrictedPolicyFound, "TestPolicyTenant1Restricted found");
    }

    @Test(groups = {"wso2.am"}, description = "Check Visibility of SubscriptionPolicy from cross Tenant")
    public void testSubscriptionPolicyAvailabilityInTenant2() throws ApiException {

        APIDTO api = apiStoreRestClientTenant1.getAPI(apiId2, tenant2Name);
        Assert.assertNotNull(api);
        boolean publicPolicyFound = false;
        boolean restrictedPolicyFound = false;
        List<APITiersDTO> tiers = api.getTiers();
        assert tiers != null;
        for (APITiersDTO tier : tiers) {
            if ("TestPolicyTenant2Public".equals(tier.getTierName())) {
                publicPolicyFound = true;
            }
            if ("TestPolicyTenant2Restricted".equals(tier.getTierName())) {
                restrictedPolicyFound = true;
            }
        }
        Assert.assertTrue(publicPolicyFound, "TestPolicyTenant2Public not found");
        Assert.assertFalse(restrictedPolicyFound, "TestPolicyTenant2Restricted found");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant")
    public void testCreateApplicationInTenant1FromTenant2User() throws APIManagerIntegrationTestException,
            ApiException {

        tenant2Application = apiStoreRestClientTenant2.addApplication("Tenant2App", "Tenant2AppPolicy", ""
                , "");
        tenant3Application = apiStoreRestClientTenant2.addApplication("Tenant3App", "Tenant2AppPolicy", ""
                , "");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant")
    public void testCreateApplicationInTenant2FromTenant1User() throws APIManagerIntegrationTestException,
            ApiException {

        tenant1Application = apiStoreRestClientTenant1.addApplication("Tenant1App", "Tenant1AppPolicy", ""
                , "");
        tenant4Application = apiStoreRestClientTenant1.addApplication("Tenant4App", "Tenant1AppPolicy", ""
                , "");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant using same tenant Application " +
            "Policy")
    public void testCreateApplicationInTenant1FromTenant2UserNegative() throws APIManagerIntegrationTestException {

        try {
            apiStoreRestClientTenant2.applicationsPostWithHttpInfo("Tenant2AppNegative", "Tenant1AppPolicy",
                    "");
            Assert.fail("Application created with other tenant applicationPolicy");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant same tenant Application Policy")
    public void testCreateApplicationInTenant2FromTenant1UserNegative() throws APIManagerIntegrationTestException {

        try {
            apiStoreRestClientTenant1.applicationsPostWithHttpInfo("Tenant1AppNegative", "Tenant2AppPolicy", "");
            Assert.fail("Application created with other tenant applicationPolicy");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create Subscription to tenant1 API from Tenant2 App", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User"})
    public void testCreateSubscriptionFromTenant2AppToTenant1API() throws APIManagerIntegrationTestException,
            ApiException {

        apiStoreRestClientTenant2.subscribeToAPI(apiId1, tenant2Application.getApplicationId(),
                "TestPolicyTenant1Public", tenant1Name);
        apiStoreRestClientTenant2.subscribeToAPI(apiId2, tenant2Application.getApplicationId(),
                "TestPolicyTenant2Public", tenant2Name);
    }

    @Test(groups = {"wso2.am"}, description = "Create Subscription to tenant2 API from Tenant1 App", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User"})
    public void testCreateSubscriptionFromTenant1AppToTenant2API() throws APIManagerIntegrationTestException,
            ApiException {

        apiStoreRestClientTenant1.subscribeToAPI(apiId2, tenant1Application.getApplicationId(),
                "TestPolicyTenant2Public", tenant2Name);
        apiStoreRestClientTenant1.subscribeToAPI(apiId1, tenant1Application.getApplicationId(),
                "TestPolicyTenant1Public", tenant1Name);
    }

    @Test(groups = {"wso2.am"}, description = "Create Subscription to tenant1 API from Tenant2 App", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User"})
    public void testCreateSubscriptionFromTenant2AppToTenant1APIRestrictedPolicy()
            throws APIManagerIntegrationTestException {

        try {
            apiStoreRestClientTenant2.subscribeToAPI(apiId1, tenant3Application.getApplicationId(),
                    "TestPolicyTenant1Restricted", tenant1Name);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 403L);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create Subscription to tenant2 API from Tenant1 App", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User"})
    public void testCreateSubscriptionFromTenant1AppToTenant2APIRestrictedPolicy()
            throws APIManagerIntegrationTestException {

        try {
            apiStoreRestClientTenant1.subscribeToAPI(apiId2, tenant4Application.getApplicationId(),
                    "TestPolicyTenant2Restricted", tenant2Name);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 403L);
        }

    }

    @Test(groups = { "wso2.am" },
            description = "Create new application and generate access token using an already subscribed application",
            dependsOnMethods = { "testCreateSubscriptionFromTenant2AppToTenant1API" })
    public void testCreateNewApplicationAndGenerateTokenSubscribedApplication()
            throws APIManagerIntegrationTestException, ApiException, MalformedURLException, JSONException {

        tenant5Application = apiStoreRestClientTenant2.addApplication(tenantApp5, tenant2AppPolicy, Strings.EMPTY,
                                                                      Strings.EMPTY);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        tenant2AppTenant2Store =
                apiStoreRestClientTenant2.generateKeys(tenant2Application.getApplicationId(),
                                                       APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                                                       Strings.EMPTY,
                                                       ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                                                       new ArrayList<>(),
                                                       grantTypes,
                                                       residentKMTenant1);
        Assert.assertNotNull(tenant2AppTenant2Store, errorMessageKeyGeneration);

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + IdentityConstants.OAuth.TOKEN);
        HttpResponse httpResponse = restAPIStore.generateUserAccessKey(tenant2AppTenant2Store.getConsumerKey(),
                                                                       tenant2AppTenant2Store.getConsumerSecret(),
                                                                       AuthConstants.CLIENT_CRED_GRANT_TYPE,
                                                                       tokenEndpointURL);
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        String accessToken = subsAccessTokenGenerationResponse.getString(AuthConstants.ACCESS_TOKEN);
        Assert.assertNotNull(accessToken, errorMessageTokenGeneration);
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant")
    public void getKeyManagersFromTenant1FromTenant2User() throws
            ApiException {

        KeyManagerListDTO keyManagers = apiStoreRestClientTenant2.getKeyManagers(tenant1Name);
        Assert.assertNotNull(keyManagers);
        boolean tenant1KMFound = false;
        boolean tenant2KMFound = false;
        assert keyManagers.getList() != null;
        for (KeyManagerInfoDTO keyManagerInfoDTO : keyManagers.getList()) {
            if (Objects.equals(keymanagerTenant1.getName(), keyManagerInfoDTO.getName()) && Objects.equals(keymanagerTenant1.getId(), keyManagerInfoDTO.getId())) {
                tenant1KMFound = true;
            }
            if (Objects.equals(keymanagerTenant2.getName(), keyManagerInfoDTO.getName()) && keymanagerTenant2.getId().equals(keyManagerInfoDTO.getId())) {
                tenant2KMFound = true;
            }
        }
        Assert.assertTrue(tenant1KMFound, "Tenant1 KM not found");
        Assert.assertFalse(tenant2KMFound, "Tenant2 KM found");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant")
    public void getKeyManagersFromTenant2FromTenant1User() throws ApiException {

        KeyManagerListDTO keyManagers = apiStoreRestClientTenant1.getKeyManagers(tenant2Name);
        Assert.assertNotNull(keyManagers);
        boolean tenant1KMFound = false;
        boolean tenant2KMFound = false;
        assert keyManagers.getList() != null;
        for (KeyManagerInfoDTO keyManagerInfoDTO : keyManagers.getList()) {
            if (Objects.equals(keymanagerTenant2.getName(), keyManagerInfoDTO.getName()) && Objects.equals(keymanagerTenant2.getId(), keyManagerInfoDTO.getId())) {
                tenant2KMFound = true;
            }
            if (Objects.equals(keymanagerTenant1.getName(), keyManagerInfoDTO.getName()) && Objects.equals(keymanagerTenant1.getId(), keyManagerInfoDTO.getId())) {
                tenant1KMFound = true;
            }
        }
        Assert.assertFalse(tenant1KMFound, "Tenant1 KM found");
        Assert.assertTrue(tenant2KMFound, "Tenant2 KM not found");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testCreateSubscriptionFromTenant2AppToTenant1API",
                    "testCreateSubscriptionFromTenant1AppToTenant2API"})
    public void testGenerateKeysFromTenant1AppInTenant2Store() throws APIManagerIntegrationTestException,
            ApiException {

        tenant1AppTenant2Store =
                apiStoreRestClientTenant1.generateKeys(tenant1Application.getApplicationId(),
                        APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, ""
                        , ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), Arrays.asList(
                                "client_credentials", "password"), residentKMTenant2);
        Assert.assertNotNull(tenant1AppTenant2Store);
        Assert.assertNotNull(tenant1AppTenant2Store.getConsumerKey());
        Assert.assertNotNull(tenant1AppTenant2Store.getConsumerSecret());
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testCreateSubscriptionFromTenant2AppToTenant1API",
                    "testCreateSubscriptionFromTenant1AppToTenant2API"})
    public void testGenerateKeysFromTenant1AppInTenant1Store() throws APIManagerIntegrationTestException,
            ApiException {

        tenant1AppTenant1Store =
                apiStoreRestClientTenant1.generateKeys(tenant1Application.getApplicationId(),
                        APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, ""
                        , ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), Arrays.asList(
                                "client_credentials", "password"), residentKMTenant1);
        Assert.assertNotNull(tenant1AppTenant1Store);
        Assert.assertNotNull(tenant1AppTenant1Store.getConsumerKey());
        Assert.assertNotNull(tenant1AppTenant1Store.getConsumerSecret());
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testGenerateKeysFromTenant1AppInTenant2Store", "testGenerateKeysFromTenant1AppInTenant1Store"})
    public void testRetrieveOauthKeysFromTenant1Store() throws ApiException {

        ApplicationKeyListDTO applicationOauthKeys =
                apiStoreRestClientTenant1.getApplicationOauthKeys(tenant1Application.getApplicationId(), tenant1Name);
        Assert.assertNotNull(applicationOauthKeys);
        Assert.assertNotNull(applicationOauthKeys.getList());
        boolean foundtenant1AppTenant1Store = false;
        boolean foundtenant1AppTenant2Store = false;
        for (ApplicationKeyDTO applicationKeyDTO : applicationOauthKeys.getList()) {
            if (tenant1AppTenant1Store.getConsumerKey().equals(applicationKeyDTO.getConsumerKey())) {
                if (tenant1AppTenant1Store.getKeyMappingId().equals(applicationKeyDTO.getKeyMappingId())) {
                    foundtenant1AppTenant1Store = true;
                }
            }
            if (tenant1AppTenant2Store.getConsumerKey().equals(applicationKeyDTO.getConsumerKey())) {
                if (tenant1AppTenant2Store.getKeyMappingId().equals(applicationKeyDTO.getKeyMappingId())) {
                    foundtenant1AppTenant2Store = true;
                }
            }
        }
        Assert.assertTrue(foundtenant1AppTenant1Store, "Tenant1 Application Keys can't see");
        Assert.assertFalse(foundtenant1AppTenant2Store, "Tenant2 Application Keys can see");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testGenerateKeysFromTenant1AppInTenant2Store", "testGenerateKeysFromTenant1AppInTenant1Store"})
    public void testRetrieveOauthKeysFromTenant2() throws ApiException {

        ApplicationKeyListDTO applicationOauthKeys =
                apiStoreRestClientTenant1.getApplicationOauthKeys(tenant1Application.getApplicationId(), tenant2Name);
        Assert.assertNotNull(applicationOauthKeys);
        Assert.assertNotNull(applicationOauthKeys.getList());
        boolean foundtenant1AppTenant1Store = false;
        boolean foundtenant1AppTenant2Store = false;
        for (ApplicationKeyDTO applicationKeyDTO : applicationOauthKeys.getList()) {
            if (tenant1AppTenant1Store.getConsumerKey().equals(applicationKeyDTO.getConsumerKey())) {
                if (tenant1AppTenant1Store.getKeyMappingId().equals(applicationKeyDTO.getKeyMappingId())) {
                    foundtenant1AppTenant1Store = true;
                }
            }
            if (tenant1AppTenant2Store.getConsumerKey().equals(applicationKeyDTO.getConsumerKey())) {
                if (tenant1AppTenant2Store.getKeyMappingId().equals(applicationKeyDTO.getKeyMappingId())) {
                    foundtenant1AppTenant2Store = true;
                }
            }
        }
        Assert.assertFalse(foundtenant1AppTenant1Store, "Tenant2 Application Keys can see");
        Assert.assertTrue(foundtenant1AppTenant2Store, "Tenant2 Application Keys can't see");
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testGenerateKeysFromTenant1AppInTenant2Store", "testGenerateKeysFromTenant1AppInTenant1Store"})
    public void invokeFromTokenInSameTenant() throws IOException, APIManagerIntegrationTestException, JSONException,
            XPathExpressionException, ParseException {

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String requestBody = "grant_type=client_credentials";
        HttpResponse httpResponse =
                apiStoreRestClientTenant1.generateUserAccessKey(tenant1AppTenant1Store.getConsumerKey(),
                        tenant1AppTenant1Store.getConsumerSecret(), requestBody, tokenEndpointURL);
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        Map<String, String> requestHeader = new HashMap<>();
        String accessToken = subsAccessTokenGenerationResponse.getString("access_token");
        requestHeader.put("Authorization", "Bearer " + accessToken);
        requestHeader.put("accept", "application/json");
        HttpResponse response =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps("t/".concat(tenant1Name).concat("/").concat(apiContext), apiVersion) + "/menu", requestHeader);
        Assert.assertEquals(response.getResponseCode(), 200);
        String opaqueToken = TokenUtils.getJtiOfJwtToken(accessToken);
        requestHeader.put("Authorization", "Bearer " + opaqueToken);
        response =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps("t/".concat(tenant1Name).concat("/").concat(apiContext), apiVersion) + "/menu", requestHeader);
        Assert.assertEquals(response.getResponseCode(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testGenerateKeysFromTenant1AppInTenant2Store", "testGenerateKeysFromTenant1AppInTenant1Store"})
    public void invokeFromTokenInOtherTenant() throws IOException, APIManagerIntegrationTestException, JSONException,
            XPathExpressionException, ParseException {

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String requestBody = "grant_type=client_credentials";
        HttpResponse httpResponse =
                apiStoreRestClientTenant1.generateUserAccessKey(tenant1AppTenant2Store.getConsumerKey(),
                        tenant1AppTenant2Store.getConsumerSecret(), requestBody, tokenEndpointURL);
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        Map<String, String> requestHeader = new HashMap<>();
        String accessToken = subsAccessTokenGenerationResponse.getString("access_token");
        requestHeader.put("Authorization", "Bearer " + accessToken);
        requestHeader.put("accept", "application/json");
        HttpResponse response =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps("t/".concat(tenant2Name).concat("/").concat(apiContext), apiVersion) + "/menu", requestHeader);
        Assert.assertEquals(response.getResponseCode(), 200);
        String opaqueToken = TokenUtils.getJtiOfJwtToken(accessToken);

        requestHeader.put("Authorization", "Bearer " + opaqueToken);
        requestHeader.put("accept", "application/json");
        response =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps("t/".concat(tenant2Name).concat("/").concat(apiContext), apiVersion) + "/menu", requestHeader);
        Assert.assertEquals(response.getResponseCode(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testGenerateKeysFromTenant1AppInTenant2Store", "testGenerateKeysFromTenant1AppInTenant1Store"})
    public void testRetrieveSubscriptionsFromApplicationId() throws ApiException {

        SubscriptionListDTO allSubscriptionsOfApplication =
                apiStoreRestClientTenant1.getAllSubscriptionsOfApplication(tenant1Application.getApplicationId(),
                        tenant1Name);
        Assert.assertNotNull(allSubscriptionsOfApplication);
        Assert.assertNotNull(allSubscriptionsOfApplication.getList());
        Assert.assertEquals(allSubscriptionsOfApplication.getCount().intValue(), 1);
        boolean foundAPI1 = false;
        boolean foundAPI2 = false;
        for (SubscriptionDTO subscriptionDTO : allSubscriptionsOfApplication.getList()) {
            if (apiId1.equals(subscriptionDTO.getApiId())) {
                foundAPI1 = true;
            }
            if (apiId2.equals(subscriptionDTO.getApiId())) {
                foundAPI2 = true;
            }
        }
        Assert.assertTrue(foundAPI1, "Subscription for API1 not found");
        Assert.assertFalse(foundAPI2, "Subscription for API2 found");
        ApplicationDTO applicationById =
                apiStoreRestClientTenant1.getApplicationById(tenant1Application.getApplicationId(), tenant2Name);
        Assert.assertNotNull(applicationById);
        Assert.assertEquals(applicationById.getSubscriptionCount().intValue(), 1);
        applicationById =
                apiStoreRestClientTenant1.getApplicationById(tenant1Application.getApplicationId(), tenant1Name);
        Assert.assertNotNull(applicationById);
        Assert.assertEquals(applicationById.getSubscriptionCount().intValue(), 1);
    }

    @Test(groups = {"wso2.am"}, description = "Create Application from other tenant", dependsOnMethods =
            {"testCreateApplicationInTenant1FromTenant2User", "testCreateApplicationInTenant2FromTenant1User",
                    "testGenerateKeysFromTenant1AppInTenant2Store", "testGenerateKeysFromTenant1AppInTenant1Store"})
    public void testRetrieveSubscriptionsFromApplicationId2() throws ApiException {

        SubscriptionListDTO allSubscriptionsOfApplication =
                apiStoreRestClientTenant1.getAllSubscriptionsOfApplication(tenant1Application.getApplicationId(),
                        tenant2Name);
        Assert.assertNotNull(allSubscriptionsOfApplication);
        Assert.assertNotNull(allSubscriptionsOfApplication.getList());
        Assert.assertEquals(allSubscriptionsOfApplication.getCount().intValue(), 1);
        boolean foundAPI1 = false;
        boolean foundAPI2 = false;
        for (SubscriptionDTO subscriptionDTO : allSubscriptionsOfApplication.getList()) {
            if (apiId1.equals(subscriptionDTO.getApiId())) {
                foundAPI1 = true;
            }
            if (apiId2.equals(subscriptionDTO.getApiId())) {
                foundAPI2 = true;
            }
        }
        Assert.assertFalse(foundAPI1, "Subscription for API1 found");
        Assert.assertTrue(foundAPI2, "Subscription for API2 not found");
        ApplicationDTO applicationById =
                apiStoreRestClientTenant2.getApplicationById(tenant2Application.getApplicationId(), tenant2Name);
        Assert.assertNotNull(applicationById);
        Assert.assertEquals(applicationById.getSubscriptionCount().intValue(), 1);
        applicationById =
                apiStoreRestClientTenant2.getApplicationById(tenant2Application.getApplicationId(), tenant1Name);
        Assert.assertNotNull(applicationById);
        Assert.assertEquals(applicationById.getSubscriptionCount().intValue(), 1);
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {

        apiStoreRestClientTenant1.deleteApplication(tenant1Application.getApplicationId());
        apiStoreRestClientTenant2.deleteApplication(tenant2Application.getApplicationId());
        apiStoreRestClientTenant2.deleteApplication(tenant3Application.getApplicationId());
        apiStoreRestClientTenant1.deleteApplication(tenant4Application.getApplicationId());
        apiStoreRestClientTenant2.deleteApplication(tenant5Application.getApplicationId());
        apiPublisherRestClientTenant1.deleteAPI(apiId1);
        apiPublisherRestClientTenant2.deleteAPI(apiId2);
        restAPIAdminTenant1.deleteApplicationThrottlingPolicy(policyIdTenant1);
        restAPIAdminTenant2.deleteApplicationThrottlingPolicy(policyIdTenant2);
        restAPIAdminTenant1.deleteSubscriptionThrottlingPolicy(testPolicyTenant1Restricted.getPolicyId());
        restAPIAdminTenant1.deleteSubscriptionThrottlingPolicy(testPolicyTenant1Public.getPolicyId());
        restAPIAdminTenant2.deleteSubscriptionThrottlingPolicy(testPolicyTenant2Restricted.getPolicyId());
        restAPIAdminTenant2.deleteSubscriptionThrottlingPolicy(testPolicyTenant2Public.getPolicyId());
        restAPIAdminTenant1.deleteKeyManager(keymanagerTenant1.getId());
        restAPIAdminTenant2.deleteKeyManager(keymanagerTenant2.getId());
        tenant1UserStoreManager.deleteRole("role1");
        tenant2UserStoreManager.deleteRole("role1");
        tenantManagementServiceClient.deleteTenant(tenant1Name);
        tenantManagementServiceClient.deleteTenant(tenant2Name);
        super.cleanUp();
    }
}
