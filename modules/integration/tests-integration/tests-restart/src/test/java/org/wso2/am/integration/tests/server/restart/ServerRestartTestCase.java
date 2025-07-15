/*
 *Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.restart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServerRestartTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ServerRestartTestCase.class);
    private String apiThrottleApplicationId;
    private String apiThrottleApiId;
    private String defaultVersionApplicationID;
    private String defaultVersionApiId;
    private String jwtRevocationAppId;
    private String jwtRevocationApiId;
    private String apiRevisionApiId;
    private String accessibilityOfBlockApiId;
    private String accessibilityOfBlockApplicationId;
    private String jwtBandwidthApiId;
    private String jwtBandwidthApiPolicyId;
    private String jwtBandwidthAppPolicyId;
    private String jwtBandwidthSubPolicyId;
    private ApplicationDTO burstControlApplicationDTO;
    private String burstControlApiId;
    private SubscriptionThrottlePolicyDTO burstControlSubscriptionThrottlePolicyDTO1;
    private SubscriptionThrottlePolicyDTO burstControlSubscriptionThrottlePolicyDTO2;
    private String graphQLAPIId;
    private String apiLoggingApiId;
    private String apiLoggingApplicationId;
    private static final String BASIC_AUTH_HEADER = "admin:admin";

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}};
    }

    @Factory(dataProvider = "userModeDataProvider")
    public ServerRestartTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeSuite(alwaysRun = true)
    public void setEnvironment(ITestContext ctx) throws Exception {
        super.init(userMode);

        String API_VERSION_1_0_0 = "1.0.0";

        /*
          populate data for API Throttling Test Case
         */
        String apiThrottleBackendURL = getSuperTenantAPIInvocationURLHttp("api_throttle_backend", "1.0");

        List<APIOperationsDTO> apiThrottleApiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiThrottleApiOperationsDTO = new APIOperationsDTO();
        apiThrottleApiOperationsDTO.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiThrottleApiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType());
        apiThrottleApiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiThrottleApiOperationsDTO.setTarget("/test");
        apiThrottleApiOperationsDTOS.add(apiThrottleApiOperationsDTO);

        APIRequest apiThrottleApiRequest = new APIRequest("APIThrottleAPI", "api_throttle",
                new URL(apiThrottleBackendURL));
        apiThrottleApiRequest.setVersion("1.0.0");
        apiThrottleApiRequest.setProvider(user.getUserName());
        apiThrottleApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiThrottleApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiThrottleApiRequest.setOperationsDTOS(apiThrottleApiOperationsDTOS);
        apiThrottleApiRequest.setTags("token, throttling");
        apiThrottleApiRequest.setDescription("This is test API created by API manager integration test");

        //Create application
        HttpResponse apiThrottleApplicationResponse = restAPIStore.createApplication("APIThrottle-application",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        Assert.assertEquals(apiThrottleApplicationResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");

        apiThrottleApplicationId = apiThrottleApplicationResponse.getData();
        Assert.assertNotNull(apiThrottleApplicationId);

        apiThrottleApiId = createPublishAndSubscribeToAPIUsingRest(apiThrottleApiRequest, restAPIPublisher,
                restAPIStore, apiThrottleApplicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        Assert.assertNotNull(apiThrottleApiId);

        waitForAPIDeploymentSync(user.getUserName(), "APIThrottleAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        ArrayList apiThrottleGrantTypes = new ArrayList();
        apiThrottleGrantTypes.add("client_credentials");

        //get access token
        ApplicationKeyDTO apiThrottleApplicationKeyDTO = restAPIStore.generateKeys(apiThrottleApplicationId, "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, apiThrottleGrantTypes);
        Assert.assertNotNull(apiThrottleApplicationKeyDTO.getToken());
        String apiThrottleAccessToken = apiThrottleApplicationKeyDTO.getToken().getAccessToken();

        ctx.setAttribute("apiThrottleAccessToken", apiThrottleAccessToken);

        /*
          populate data for Default Version API Test Case
         */

        //Add an Application in the Store.
        HttpResponse defaultVersionApplicationResponse = restAPIStore
                .createApplication("DefaultVersionAPP", "Default version testing application",
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);

        defaultVersionApplicationID = defaultVersionApplicationResponse.getData();
        //Generate production token and invoke with that
        ArrayList defaultVersionGrantTypes = new ArrayList();
        defaultVersionGrantTypes.add("client_credentials");
        ApplicationKeyDTO defaultVersionApplicationKeyDTO = restAPIStore.generateKeys(defaultVersionApplicationID,
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, defaultVersionGrantTypes);
        Assert.assertNotNull(defaultVersionApplicationKeyDTO.getToken());
        String defaultVersionAccessToken = defaultVersionApplicationKeyDTO.getToken().getAccessToken();

        String defaultVersionBackendUrl = getGatewayURLNhttp() + "version1";

        //Create the api creation request object
        APIRequest defaultVersionApiRequest = new APIRequest("DefaultVersionAPI", "defaultversion",
                new URL(defaultVersionBackendUrl));
        defaultVersionApiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());
        defaultVersionApiRequest.setVersion("1.0.0");
        defaultVersionApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        defaultVersionApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);


        //Create api and subscribe the API to the DefaultApplication
        defaultVersionApiId = createPublishAndSubscribeToAPIUsingRest(defaultVersionApiRequest, restAPIPublisher,
                restAPIStore, defaultVersionApplicationID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(defaultVersionApiRequest.getProvider(), defaultVersionApiRequest.getName(),
                defaultVersionApiRequest.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
        APIDTO defaultVersionStoreAPI = restAPIStore.getAPI(defaultVersionApiId);
        List<APIEndpointURLsDTO> defaultVersionBackendURLs = defaultVersionStoreAPI.getEndpointURLs();
        Assert.assertNotNull(defaultVersionBackendURLs);
        Assert.assertEquals(defaultVersionBackendURLs.size(), 1);
        APIDefaultVersionURLsDTO defaultVersionURLs = defaultVersionBackendURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNull(defaultVersionURLs.getHttp());
        Assert.assertNull(defaultVersionURLs.getHttps());
        Assert.assertNull(defaultVersionURLs.getWs());
        Assert.assertNull(defaultVersionURLs.getWss());
        String versionAPIInvocationUrl = getAPIInvocationURLHttp("defaultversion", "1.0.0");
        //Going to access the API without the version in the request url.
        HttpResponse defaultVersionDirectResponse = invokeWithGet(defaultVersionBackendUrl, new HashMap<>());

        Map<String, String> defaultVersionRequestHeaders = new HashMap<>();
        defaultVersionRequestHeaders.put("Authorization", "Bearer " + defaultVersionAccessToken);

        HttpResponse defaultVersionHttpResponse = invokeWithGet(versionAPIInvocationUrl, defaultVersionRequestHeaders);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        Assert.assertEquals(defaultVersionHttpResponse.getData(), defaultVersionDirectResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        // Invoke Default API and check if theres any default API deployed.
        String defaultVersionAPIInvocationUrl = getAPIInvocationURLHttp("defaultversion");
        HttpResponse defaultHttpResponse = invokeWithGet(defaultVersionAPIInvocationUrl, defaultVersionRequestHeaders);
        // No default API deployed. hence 404
        Assert.assertEquals(defaultHttpResponse.getResponseCode(), 404);
        // Updating API with DefaultVersion Check
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO defaultVersionApiv1 =
                restAPIPublisher.getAPIByID(defaultVersionApiId);
        defaultVersionApiv1.setIsDefaultVersion(true);
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO defaultVersionApidto =
                restAPIPublisher.updateAPI(defaultVersionApiv1);
        Assert.assertNotNull(defaultVersionApidto);
        Assert.assertTrue(defaultVersionApidto.isIsDefaultVersion());
        APIDTO defaultVersionStoreAPIAfterUpdate = restAPIStore.getAPI(defaultVersionApiId);
        defaultVersionBackendURLs = defaultVersionStoreAPIAfterUpdate.getEndpointURLs();
        Assert.assertNotNull(defaultVersionBackendURLs);
        Assert.assertEquals(defaultVersionBackendURLs.size(), 1);
        defaultVersionURLs = defaultVersionBackendURLs.get(0).getDefaultVersionURLs();
        Assert.assertNotNull(defaultVersionURLs);
        Assert.assertNotNull(defaultVersionURLs.getHttp());
        Assert.assertNotNull(defaultVersionURLs.getHttps());
        waitForAPIDeploymentSync(defaultVersionApiRequest.getProvider(), defaultVersionApiRequest.getName(),
                defaultVersionApiRequest.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse defaultVersionHttpResponse1 = invokeDefaultAPIWithWait(defaultVersionAPIInvocationUrl,
                defaultVersionRequestHeaders, 200);

        Assert.assertEquals(defaultVersionHttpResponse1.getData(), defaultVersionDirectResponse.getData(),
                "Default version API test failed while " + "invoking the API.");
        log.info("version : " + defaultVersionHttpResponse1.getHeaders().get("Version"));
        Assert.assertEquals(defaultVersionHttpResponse1.getHeaders().get("Version"), "v1");

        ctx.setAttribute("defaultVersionApiId", defaultVersionApiId);
        ctx.setAttribute("defaultVersionAccessToken", defaultVersionAccessToken);

        /*
          populate data for JWT Revocation Test Case
         */
        //Create an Application with TokenType as JWT
        HttpResponse jwtRevocationApplicationResponse = restAPIStore.createApplication("JWTTokenRevocationTest-Application",
                "This is a test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        jwtRevocationAppId = jwtRevocationApplicationResponse.getData();

        //Create the api creation request object
        String jwtRevocationBackendUrl = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest jwtRevocationApiRequest = new APIRequest("JWTTokenTestAPI", "jwtTokenTestAPI",
                new URL(jwtRevocationBackendUrl));
        jwtRevocationApiRequest.setVersion("1.0.0");
        jwtRevocationApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        jwtRevocationApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        jwtRevocationApiId = createPublishAndSubscribeToAPIUsingRest(jwtRevocationApiRequest, restAPIPublisher,
                restAPIStore, jwtRevocationAppId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        waitForAPIDeploymentSync(user.getUserName(), "JWTTokenTestAPI", API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        ctx.setAttribute("jwtRevocationAppId", jwtRevocationAppId);
        ctx.setAttribute("jwtRevocationApiId", jwtRevocationApiId);

        /*
          Populate data for API Revision Test Case
         */
        String apiRevisionBackendUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
        // Create the API creation request object
        APIRequest apiRevisionApiRequest;
        apiRevisionApiRequest = new APIRequest("RevisionTestAPI", "revisiontestapi", new URL(apiRevisionBackendUrl));
        apiRevisionApiRequest.setVersion(API_VERSION_1_0_0);
        apiRevisionApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRevisionApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Add the API using the API Publisher.
        HttpResponse apiRevisionApiResponse = restAPIPublisher.addAPI(apiRevisionApiRequest);
        apiRevisionApiId = apiRevisionApiResponse.getData();
        Assert.assertEquals(apiRevisionApiResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionApiId);

        // Verify the API in API Publisher
        HttpResponse apiRevisionApiDto = restAPIPublisher.getAPI(apiRevisionApiResponse.getData());
        Assert.assertTrue(StringUtils.isNotEmpty(apiRevisionApiDto.getData()),
                "Added Api is not available in APi Publisher. API ID " + apiRevisionApiId);

        ctx.setAttribute("apiRevisionApiId", apiRevisionApiId);

        /*
          Populate data for Accessibility of Block API Test Case
         */
        String accessibilityOfBlockApiBackendUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";

        HttpResponse accessibilityOfBlockApplicationResponse = restAPIStore.createApplication("AccessibilityOfBlockAPITestCase",
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        accessibilityOfBlockApplicationId = accessibilityOfBlockApplicationResponse.getData();
        //Create the api creation request object
        APIRequest accessibilityOfBlockApiRequest;
        accessibilityOfBlockApiRequest = new APIRequest("BlockAPITest", "BlockAPI", new URL(accessibilityOfBlockApiBackendUrl));

        accessibilityOfBlockApiRequest.setVersion(API_VERSION_1_0_0);
        accessibilityOfBlockApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        accessibilityOfBlockApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        accessibilityOfBlockApiRequest.setProvider(user.getUserName());
        //Create, Publish and Subscribe
        accessibilityOfBlockApiId = createPublishAndSubscribeToAPIUsingRest(accessibilityOfBlockApiRequest, restAPIPublisher,
                restAPIStore, accessibilityOfBlockApplicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        waitForAPIDeploymentSync(user.getUserName(), "BlockAPITest", API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        ctx.setAttribute("accessibilityOfBlockApplicationId", accessibilityOfBlockApplicationId);
        ctx.setAttribute("accessibilityOfBlockApiId", accessibilityOfBlockApiId);

        /*
          Populate data for JWT Bandwidth Throttling Test Case
         */
        List<String> jwtBandwidthRoleList = new ArrayList<>();
        SubscriptionThrottlePolicyPermissionDTO jwtBandwidthPermissions;
        BandwidthLimitDTO jwtBandwidthLimit = DtoFactory.createBandwidthLimitDTO("min", 1, 1L, "KB");
        ThrottleLimitDTO jwtBandwidthDefaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.BANDWIDTHLIMIT, null, jwtBandwidthLimit);
        jwtBandwidthRoleList.add("Internal/everyone");
        jwtBandwidthPermissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                        PermissionTypeEnum.ALLOW, jwtBandwidthRoleList);
        //Create the application level policy with bandwidth quota type
        ApplicationThrottlePolicyDTO jwtBandwidthApplicationPolicyDTO = DtoFactory
                .createApplicationThrottlePolicyDTO("AppPolicyWithBandwidth", "", "",
                        false, jwtBandwidthDefaultLimit);
        ApiResponse<ApplicationThrottlePolicyDTO> jwtBandwidthAddedApplicationPolicy =
                restAPIAdmin.addApplicationThrottlingPolicy(jwtBandwidthApplicationPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(jwtBandwidthAddedApplicationPolicy.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        ApplicationThrottlePolicyDTO jwtBandwidthAddedApplicationPolicyDTO = jwtBandwidthAddedApplicationPolicy.getData();
        jwtBandwidthAppPolicyId = jwtBandwidthAddedApplicationPolicyDTO.getPolicyId();
        Assert.assertNotNull(jwtBandwidthAppPolicyId, "The policy ID cannot be null or empty");

        //Create the subscription level policy with bandwidth quota type
        SubscriptionThrottlePolicyDTO jwtBandwidthSubscriptionPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO("SubPolicyWithBandwidth", "", "",
                        false, jwtBandwidthDefaultLimit, -1, -1, 100, "min", new ArrayList<>(),
                        true, "", 0, jwtBandwidthPermissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> jwtBandwidthAddedSubscriptionPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(jwtBandwidthSubscriptionPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(jwtBandwidthAddedSubscriptionPolicy.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO jwtBandwidthAddedSubscriptionPolicyDTO = jwtBandwidthAddedSubscriptionPolicy.getData();
        jwtBandwidthSubPolicyId = jwtBandwidthAddedSubscriptionPolicyDTO.getPolicyId();
        Assert.assertNotNull(jwtBandwidthSubPolicyId, "The policy ID cannot be null or empty");

        //Create the advanced throttling policy with bandwidth quota type
        AdvancedThrottlePolicyDTO jwtBandwidthAdvancedPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO("APIPolicyWithBandwidth", "", "", false, jwtBandwidthDefaultLimit,
                        new ArrayList<>());
        ApiResponse<AdvancedThrottlePolicyDTO> jwtBandwidthAddedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(jwtBandwidthAdvancedPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(jwtBandwidthAddedPolicy.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO jwtBandwidthAddedAdvancedPolicyDTO = jwtBandwidthAddedPolicy.getData();
        jwtBandwidthApiPolicyId = jwtBandwidthAddedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(jwtBandwidthApiPolicyId, "The policy ID cannot be null or empty");

        String jwtBandwidthBackendUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        // create api
        APIRequest jwtBandwidthApiRequest = new APIRequest("BandwidthTestAPI", "bandwithtestapi",
                new URL(jwtBandwidthBackendUrl), new URL(jwtBandwidthBackendUrl));
        jwtBandwidthApiRequest.setTags("youtube, token, media");
        jwtBandwidthApiRequest.setDescription("This is test API create by API manager integration test");
        jwtBandwidthApiRequest.setVersion("1.0.0");
        jwtBandwidthApiRequest.setSandbox(jwtBandwidthBackendUrl);
        jwtBandwidthApiRequest.setProvider(user.getUserName());
        jwtBandwidthApiRequest.setTiersCollection(Constants.TIERS_UNLIMITED + "," + "SubPolicyWithBandwidth");
        List<APIOperationsDTO> jwtBandwidthOperations = new ArrayList<>();
        APIOperationsDTO jwtBandwidthApiOperationsDTO = new APIOperationsDTO();
        jwtBandwidthApiOperationsDTO.setVerb("POST");
        jwtBandwidthApiOperationsDTO.setTarget("/*");
        jwtBandwidthApiOperationsDTO.setAuthType("Application & Application User");
        jwtBandwidthApiOperationsDTO.setThrottlingPolicy("Unlimited");
        jwtBandwidthOperations.add(jwtBandwidthApiOperationsDTO);
        jwtBandwidthApiRequest.setOperationsDTOS(jwtBandwidthOperations);

        HttpResponse jwtBandwidthServiceResponse = restAPIPublisher.addAPI(jwtBandwidthApiRequest);
        jwtBandwidthApiId = jwtBandwidthServiceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(jwtBandwidthApiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(jwtBandwidthApiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), "BandwidthTestAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);
        String jwtBandwidthGatewayUrl = getAPIInvocationURLHttps("bandwithtestapi" + "/" + "1.0.0" + "/");

        // check backend
        Map<String, String> jwtBandwidthRequestHeaders = new HashMap<>();
        HttpResponse jwtBandwidthResponse = HttpRequestUtil.doGet(jwtBandwidthBackendUrl, jwtBandwidthRequestHeaders);
        Assert.assertEquals(jwtBandwidthResponse.getResponseCode(), 200, "Backend (dummy_api.xml) is not up and running");

        ctx.setAttribute("jwtBandwidthApiId", jwtBandwidthApiId);
        ctx.setAttribute("jwtBandwidthGatewayUrl", jwtBandwidthGatewayUrl);

        /*
          Populate data for Burst Control Test Case
         */
        List<String> burstControlRoleList = new ArrayList<>();
        SubscriptionThrottlePolicyPermissionDTO burstControlPermissions;
        // Add subscription policy - 1
        RequestCountLimitDTO burstControlRequestCountLimit =
                DtoFactory.createRequestCountLimitDTO("min", 1, 1000L);
        ThrottleLimitDTO burstControlDefaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                        burstControlRequestCountLimit, null);
        burstControlRoleList.add("Internal/everyone");
        burstControlPermissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                        PermissionTypeEnum.ALLOW, burstControlRoleList);
        burstControlSubscriptionThrottlePolicyDTO1 =
                DtoFactory.createSubscriptionThrottlePolicyDTO("SubscriptionTier5RPMburst",
                        "subscriptionTier5RPMburst",
                        "1000 request per min with burst of 5 requests per minute",
                        false, burstControlDefaultLimit, 0, 0, 5,
                        "min", null, true, "COMMERCIAL",
                        0, burstControlPermissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> burstControlApiResponse =
                restAPIAdmin.addSubscriptionThrottlingPolicy(burstControlSubscriptionThrottlePolicyDTO1);
        Assert.assertEquals(burstControlApiResponse.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        burstControlSubscriptionThrottlePolicyDTO1 = burstControlApiResponse.getData();

        // Add subscription policy - 2
        burstControlSubscriptionThrottlePolicyDTO2 =
                DtoFactory.createSubscriptionThrottlePolicyDTO("SubscriptionTier25RPMburst",
                        "subscriptionTier25RPMburst",
                        "1000 request per min with burst of 25 requests per minute",
                        false, burstControlDefaultLimit, 0, 0, 25,
                        "min", null, true, "COMMERCIAL",
                        0, burstControlPermissions);
        burstControlApiResponse = restAPIAdmin.addSubscriptionThrottlingPolicy(burstControlSubscriptionThrottlePolicyDTO2);
        Assert.assertEquals(burstControlApiResponse.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        burstControlSubscriptionThrottlePolicyDTO2 = burstControlApiResponse.getData();

        // create api
        String burstControlBackendUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        APIRequest burstControlApiRequest = new APIRequest("APIThrottleBurstAPI", "api_burst", new URL(burstControlBackendUrl),
                new URL(burstControlBackendUrl));
        burstControlApiRequest.setVersion("1.0.0");
        burstControlApiRequest.setSandbox(burstControlBackendUrl);
        burstControlApiRequest.setProvider(user.getUserName());
        burstControlApiRequest.setTiersCollection(Constants.TIERS_UNLIMITED + "," + "SubscriptionTier5RPMburst" + ","
                + "SubscriptionTier25RPMburst");
        List<APIOperationsDTO> burstControlOperations = new ArrayList<>();
        APIOperationsDTO burstControlApiOperationsDTO = new APIOperationsDTO();
        burstControlApiOperationsDTO.setVerb("POST");
        burstControlApiOperationsDTO.setTarget("/*");
        burstControlApiOperationsDTO.setAuthType("Application & Application User");
        burstControlApiOperationsDTO.setThrottlingPolicy("Unlimited");
        burstControlOperations.add(burstControlApiOperationsDTO);
        burstControlApiRequest.setOperationsDTOS(burstControlOperations);

        HttpResponse burstControlServiceResponse = restAPIPublisher.addAPI(burstControlApiRequest);
        burstControlApiId = burstControlServiceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(burstControlApiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(burstControlApiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), "APIThrottleBurstAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        // check backend
        Map<String, String> burstControlRequestHeaders = new HashMap<>();
        HttpResponse burstControlResponse = HttpRequestUtil.doGet(burstControlBackendUrl, burstControlRequestHeaders);
        Assert.assertEquals(burstControlResponse.getResponseCode(), 200, "Backend (dummy_api.xml) is not up and running");

        // Create an Application
        burstControlApplicationDTO = restAPIStore.addApplication("APIThrottleBurst-application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");

        ctx.setAttribute("burstControlApplicationDTO", burstControlApplicationDTO);
        ctx.setAttribute("burstControlApiId", burstControlApiId);

        /*
          Populate data for Graphql Test Case
         */
        userManagementClient.addUser("graphqluser", "graphqlUser", new String[]{}, null);
        userManagementClient.addRole("graphqlrole", new String[]{"graphqluser"}, new String[]{});
        String graphQLSchemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                StandardCharsets.UTF_8);

        File graphQLFile = getTempFileWithContent(graphQLSchemaDefinition);
        GraphQLValidationResponseDTO graphQLResponseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(graphQLFile);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = graphQLResponseApiDto.getGraphQLInfo();
        Assert.assertNotNull(graphQLInfo);
        String graphQLArrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray graphQLOperations = new JSONArray(graphQLArrayToJson);

        ArrayList<String> graphQLEnvironment = new ArrayList<>();
        graphQLEnvironment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> graphQLPolicies = new ArrayList<>();
        graphQLPolicies.add("Unlimited");

        JSONObject graphQLAdditionalPropertiesObj = new JSONObject();
        graphQLAdditionalPropertiesObj.put("name", "CountriesGraphqlAPI");
        graphQLAdditionalPropertiesObj.put("context", "info");
        graphQLAdditionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject graphQLUrl = new JSONObject();
        graphQLUrl.put("url", "https://localhost:9943/am-graphQL-sample/api/graphql/");
        JSONObject graphQLEndpointConfig = new JSONObject();
        graphQLEndpointConfig.put("endpoint_type", "http");
        graphQLEndpointConfig.put("sandbox_endpoints", graphQLUrl);
        graphQLEndpointConfig.put("production_endpoints", graphQLUrl);
        graphQLAdditionalPropertiesObj.put("endpointConfig", graphQLEndpointConfig);
        graphQLAdditionalPropertiesObj.put("policies", graphQLPolicies);
        graphQLAdditionalPropertiesObj.put("operations", graphQLOperations);

        // create Graphql API
        org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO graphQLApidto =
                restAPIPublisher.importGraphqlSchemaDefinition(graphQLFile, graphQLAdditionalPropertiesObj.toString());
        graphQLAPIId = graphQLApidto.getId();
        HttpResponse graphQLCreatedApiResponse = restAPIPublisher.getAPI(graphQLAPIId);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), graphQLCreatedApiResponse.getResponseCode(),
                "CountriesGraphqlAPI" + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphQLAPIId, restAPIPublisher);
        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphQLAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), "CountriesGraphqlAPI", API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        ctx.setAttribute("graphQLSchemaDefinition", graphQLSchemaDefinition);
        ctx.setAttribute("graphQLAPIId", graphQLAPIId);

        /*
          Populate Data for Custom Throttling Policy Test Case
         */
        AdminApiTestHelper customThrottlingAdminApiTestHelper = new AdminApiTestHelper();
        //Create the custom throttling policy DTO
        String customThrottlingPolicyName = "TestPolicy";
        String customThrottlingDescription = "This is a test custom throttle policy";
        String customThrottlingSiddhiQuery = "FROM RequestStream\nSELECT userId, ( userId == 'admin@carbon.super' ) AS isEligible, " +
                "str:concat('admin@carbon.super','') as throttleKey\nINSERT INTO EligibilityStream; \n\nFROM " +
                "EligibilityStream[isEligible==true]#throttler:timeBatch(1 min) \nSELECT throttleKey, (count(userId) >= 10) " +
                "as isThrottled, expiryTimeStamp group by throttleKey \nINSERT ALL EVENTS into ResultStream;";
        String customThrottlingKeyTemplate = "$userId";
        CustomRuleDTO customThrottlingRuleDTO = DtoFactory.createCustomThrottlePolicyDTO(customThrottlingPolicyName, customThrottlingDescription,
                false, customThrottlingSiddhiQuery, customThrottlingKeyTemplate);

        ApiResponse<CustomRuleDTO> customThrottlingAddedPolicy;
        //Add the custom throttling policy
        customThrottlingAddedPolicy = restAPIAdmin.addCustomThrottlingPolicy(customThrottlingRuleDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(customThrottlingAddedPolicy.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        CustomRuleDTO customThrottlingAddedPolicyDTO = customThrottlingAddedPolicy.getData();
        String customThrottlingPolicyId = customThrottlingAddedPolicyDTO.getPolicyId();
        Assert.assertNotNull(customThrottlingPolicyId, "The policy ID cannot be null or empty");

        customThrottlingRuleDTO.setPolicyId(customThrottlingPolicyId);
        customThrottlingRuleDTO.setIsDeployed(true);
        //Verify the created custom throttling policy DTO
        customThrottlingAdminApiTestHelper.verifyCustomThrottlePolicyDTO(customThrottlingRuleDTO, customThrottlingAddedPolicyDTO);

        ctx.setAttribute("customThrottlingPolicyId", customThrottlingPolicyId);
        ctx.setAttribute("customThrottlingRuleDTO", customThrottlingRuleDTO);
        ctx.setAttribute("customThrottlingAdminApiTestHelper", customThrottlingAdminApiTestHelper);

        /*
          Populate data for API Logging Test Case
         */
        // Get list of APIs without any API
        Map<String, String> apiLoggingHeader = new HashMap<>();
        byte[] apiLoggingEncodedBytes = Base64.encodeBase64(BASIC_AUTH_HEADER
                .getBytes(StandardCharsets.UTF_8));
        apiLoggingHeader.put("Authorization", "Basic " + new String(apiLoggingEncodedBytes, StandardCharsets.UTF_8));
        apiLoggingHeader.put("Content-Type", "application/json");

        // Create an application
        HttpResponse apiLoggingApplicationResponse = restAPIStore.createApplication("APILoggingTestApp",
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        apiLoggingApplicationId = apiLoggingApplicationResponse.getData();

        // Create an API and subscribe to it using created application
        APIRequest apiLoggingApiRequest;
        String apiLoggingBackendUrl = getAPIInvocationURLHttp("xmlapi", "1.0.0");
        apiLoggingApiRequest = new APIRequest("APILoggingTestAPI", "apiloggingtest", new URL(apiLoggingBackendUrl));
        apiLoggingApiRequest.setVersion("1.0.0");
        apiLoggingApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiLoggingApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiLoggingApiRequest.setTags("testTag1, testTag2, testTag3");
        apiLoggingApiRequest.setProvider(user.getUserName());
        apiLoggingApiId = createPublishAndSubscribeToAPIUsingRest(apiLoggingApiRequest, restAPIPublisher, restAPIStore,
                apiLoggingApplicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(user.getUserName(), "APILoggingTestAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        // Change logLevel to FULL
        String addNewLoggerPayload = "{ \"logLevel\": \"FULL\" }";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v0/tenant-logs/carbon.super/apis/" + apiLoggingApiId, apiLoggingHeader,
                addNewLoggerPayload);

        // Get list of APIs which have log-level=FULL
        HttpResponse apiLoggingLoggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis?log-level=full", apiLoggingHeader);
        Assert.assertEquals(apiLoggingLoggingResponse.getData(), "{\"apis\":[{\"context\":\"/" + "apiloggingtest" + "/" + "1.0.0" + "\","
                + "\"logLevel\":\"FULL\",\"apiId\":\"" + apiLoggingApiId + "\",\"resourceMethod\":null,\"resourcePath\":null" +
                "}]}");

        ctx.setAttribute("apiLoggingApplicationId", apiLoggingApplicationId);
        /*
          Call Restart Server function
         */
        Thread.sleep(5000);
        restartServer();
        Thread.sleep(5000);
        restartServer();
    }

    private void restartServer() throws Exception {
        AutomationContext superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.restartGracefully();
    }

    private HttpResponse invokeDefaultAPIWithWait(String invocationUrl, Map<String, String> headers,
                                                  int statusCode) throws IOException, InterruptedException {
        HttpResponse response = invokeWithGet(invocationUrl, headers);
        int count = 0;
        if (response.getResponseCode() != statusCode) {
            do {
                Thread.sleep(10000);
                response = invokeWithGet(invocationUrl, headers);
                if (response.getResponseCode() == 200) {
                    return response;
                }
                count++;
            } while (count > 6);
        }
        return response;
    }

    private HttpResponse invokeWithGet(String url, Map<String, String> headers) throws IOException {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        headers.forEach(get::addHeader);
        org.apache.http.HttpResponse response = httpclient.execute(get);
        InputStream stream = response.getEntity().getContent();
        String content = IOUtils.toString(stream);
        Map<String, String> outputHeaders = new HashMap();
        for (Header header : response.getAllHeaders()) {
            outputHeaders.put(header.getName(), header.getValue());
        }
        return new HttpResponse(content, response.getStatusLine().getStatusCode(), outputHeaders);
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    @AfterSuite(alwaysRun = true)
    void destroy() throws Exception {
        restAPIStore.deleteApplication(apiThrottleApplicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiThrottleApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiThrottleApiId);

        restAPIStore.deleteApplication(defaultVersionApplicationID);
        restAPIPublisher.deleteAPI(defaultVersionApiId);

        restAPIStore.deleteApplication(jwtRevocationAppId);
        undeployAndDeleteAPIRevisionsUsingRest(jwtRevocationApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(jwtRevocationApiId);

        restAPIPublisher.deleteAPI(apiRevisionApiId);

        restAPIStore.deleteApplication(accessibilityOfBlockApplicationId);
        restAPIPublisher.deleteAPI(accessibilityOfBlockApiId);

        undeployAndDeleteAPIRevisionsUsingRest(jwtBandwidthApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(jwtBandwidthApiId);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(jwtBandwidthApiPolicyId);
        restAPIAdmin.deleteApplicationThrottlingPolicy(jwtBandwidthAppPolicyId);
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(jwtBandwidthSubPolicyId);

        restAPIStore.deleteApplication(burstControlApplicationDTO.getApplicationId());
        restAPIPublisher.deleteAPI(burstControlApiId);
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(burstControlSubscriptionThrottlePolicyDTO1.getPolicyId());
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(burstControlSubscriptionThrottlePolicyDTO2.getPolicyId());

        undeployAndDeleteAPIRevisionsUsingRest(graphQLAPIId, restAPIPublisher);
        restAPIPublisher.deleteAPI(graphQLAPIId);

        restAPIStore.deleteApplication(apiLoggingApplicationId);
        restAPIPublisher.deleteAPI(apiLoggingApiId);

        super.cleanUp();
    }

}
