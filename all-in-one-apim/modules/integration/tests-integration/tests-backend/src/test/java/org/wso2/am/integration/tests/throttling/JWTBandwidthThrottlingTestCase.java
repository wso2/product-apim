package org.wso2.am.integration.tests.throttling;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import com.google.gson.Gson;

public class JWTBandwidthThrottlingTestCase extends APIMIntegrationBaseTest {
    private String appPolicyName = "AppPolicyWithBandwidth";
    private String subPolicyName = "SubPolicyWithBandwidth";
    private String apiPolicyName = "APIPolicyWithBandwidth";
    private String appPolicyId;
    private String subPolicyId;
    private String apiPolicyId;
    private String apiId;
    private String gatewayUrl;
    private String app1Id;
    private String app2Id;
    private String backendEP;

    private String body = "{\"payload\" : \"00000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000\"}";
    private String app3Id;

    private static final Log log = LogFactory.getLog(JWTBandwidthThrottlingTestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public JWTBandwidthThrottlingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String INTERNAL_EVERYONE= "Internal/everyone";
        List<String> roleList = new ArrayList<>();
        SubscriptionThrottlePolicyPermissionDTO permissions;
        BandwidthLimitDTO bandwidthLimit = DtoFactory.createBandwidthLimitDTO("min", 1, 1L, "KB");
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.BANDWIDTHLIMIT, null, bandwidthLimit);
        roleList.add(INTERNAL_EVERYONE);
        permissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                        PermissionTypeEnum.ALLOW, roleList);
        //Create the application level policy with bandwidth quota type
        ApplicationThrottlePolicyDTO bandwidthApplicationPolicyDTO = DtoFactory
                .createApplicationThrottlePolicyDTO(appPolicyName, "", "", false, defaultLimit);
        ApiResponse<ApplicationThrottlePolicyDTO> addedApplicationPolicy =
                restAPIAdmin.addApplicationThrottlingPolicy(bandwidthApplicationPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedApplicationPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        ApplicationThrottlePolicyDTO addedApplicationPolicyDTO = addedApplicationPolicy.getData();
        appPolicyId = addedApplicationPolicyDTO.getPolicyId();
        Assert.assertNotNull(appPolicyId, "The policy ID cannot be null or empty");

        //Create the subscription level policy with bandwidth quota type
        SubscriptionThrottlePolicyDTO bandwidthSubscriptionPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(subPolicyName, "", "", false, defaultLimit,
                        -1, -1, 100, "min", new ArrayList<>(),
                        true, "", 0, permissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedSubscriptionPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(bandwidthSubscriptionPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedSubscriptionPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedSubscriptionPolicyDTO = addedSubscriptionPolicy.getData();
        subPolicyId = addedSubscriptionPolicyDTO.getPolicyId();
        Assert.assertNotNull(subPolicyId, "The policy ID cannot be null or empty");

        //Create the advanced throttling policy with bandwidth quota type
        AdvancedThrottlePolicyDTO bandwidthAdvancedPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(apiPolicyName, "", "", false, defaultLimit,
                        new ArrayList<>());
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(bandwidthAdvancedPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO = addedPolicy.getData();
        apiPolicyId = addedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        backendEP = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        // create api
        String APIName = "BandwidthTestAPI";
        String APIContext = "bandwithtestapi";
        String tags = "youtube, token, media";
        String url = backendEP;
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(Constants.TIERS_UNLIMITED + "," + subPolicyName);
        List<APIOperationsDTO> operations = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("POST");
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTO.setAuthType("Application & Application User");
        apiOperationsDTO.setThrottlingPolicy("Unlimited");
        operations.add(apiOperationsDTO);
        apiRequest.setOperationsDTOS(operations);

        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), APIName, APIVersion, APIMIntegrationConstants.IS_API_EXISTS);
        gatewayUrl = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");

        // check backend
        Map<String, String> requestHeaders = new HashMap<String, String>();
        HttpResponse response = HttpRequestUtil.doGet(backendEP, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Backend (dummy_api.xml) is not up and running");
    }

    @Test(groups = { "wso2.am" }, description = "")
    public void testApplicationLevelThrottling() throws Exception {
        ApplicationDTO applicationDTO = restAPIStore.addApplication("ApplicationBandwidthtestapp", appPolicyName, "",
                "this-is-test");
        app1Id = applicationDTO.getApplicationId();
        Assert.assertEquals(true, applicationDTO.getThrottlingPolicy().equals(appPolicyName));
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(Constants.TIERS_UNLIMITED));

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        String jwtString = APIMTestCaseUtils.getDecodedJWT(accessToken);
        JSONObject jwtObject = new JSONObject(jwtString);
        log.info("Decoded JWT token: " + jwtString);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        HttpResponse response;
        boolean isThrottled = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }

        Assert.assertTrue(isThrottled, "Request not throttled by bandwidth condition in application tier");

    }

    @Test(groups = { "wso2.am" }, description = "")
    public void testSubscriptionLevelThrottling() throws Exception {

        ApplicationDTO applicationDTO = restAPIStore.addApplication("SubscriptionBandwidthtestapp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        app2Id = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                subPolicyName);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(subPolicyName));
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        String jwtString = APIMTestCaseUtils.getDecodedJWT(accessToken);
        JSONObject jwtObject = new JSONObject(jwtString);
        log.info("Decoded JWT token: " + jwtString);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        HttpResponse response;
        boolean isThrottled = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }

        Assert.assertTrue(isThrottled, "Request not throttled by bandwidth condition in subscription tier");

    }

    @Test(groups = { "wso2.am" }, description = "", dependsOnMethods = { "testSubscriptionLevelThrottling",
            "testApplicationLevelThrottling" })
    public void testAPILevelThrottling() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(apiPolicyName);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName, "API tier not updated.");

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        ApplicationDTO applicationDTO = restAPIStore.addApplication("NormalAPP",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        app3Id = applicationDTO.getApplicationId();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(Constants.TIERS_UNLIMITED));
        
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        //Test without any throttling tier
        HttpResponse response;
        boolean isThrottled = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled, "Request not throttled by bandwidth condition in api tier");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(app1Id);
        restAPIStore.deleteApplication(app2Id);
        restAPIStore.deleteApplication(app3Id);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(apiPolicyId);
        restAPIAdmin.deleteApplicationThrottlingPolicy(appPolicyId);
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(subPolicyId);
    }
}
