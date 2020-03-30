package org.wso2.am.integration.tests.throttling;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import com.google.gson.Gson;

public class JWTBandwidthThrottlingTestCase extends APIMIntegrationBaseTest {
    private AdminDashboardRestClient adminDashboardRestClient;
    private String appPolicyName = "AppPolicyWithBandwidth";
    private String subPolicyName = "SubPolicyWithBandwidth";
    private String apiPolicyName = "APIPolicyWithBandwidth";
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
        // create application level policy with bandwidth quota type
        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());
        adminDashboardRestClient.login(user.getUserName(), user.getPassword());
        HttpResponse addPolicyResponse = adminDashboardRestClient.addApplicationPolicyWithBandwidthType(appPolicyName,
                1, "KB", 1, "min");
        verifyResponse(addPolicyResponse);

        // create subscription level policy with bandwidth quota type
        addPolicyResponse = adminDashboardRestClient.addSubscriptionPolicyWithBandwidthType(subPolicyName, 1, "KB", 1,
                "min", true, 100, "min");
        verifyResponse(addPolicyResponse);

        String throttlingPolicyJSON = "{\"policyName\":\"" + apiPolicyName
                + "\",\"policyDescription\":\"\",\"executionFlows\":[],\"defaultQuotaPolicy\":"
                + "{\"type\":\"bandwidthVolume\",\"limit\":{\"requestCount\":0,\"timeUnit\":\"min\","
                + "\"dataAmount\":\"1\",\"dataUnit\":\"KB\",\"unitTime\":\"1\"}}}";
        addPolicyResponse = adminDashboardRestClient.addThrottlingPolicy(throttlingPolicyJSON);
        verifyResponse(addPolicyResponse);

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
        
        JSONObject tierInfo = (JSONObject)jwtObject.get("application");
        Assert.assertEquals(tierInfo.has("tierQuotaType"), true, "tierQuotaType property does not exist in the JWT");
        Assert.assertEquals(tierInfo.get("tierQuotaType"), "bandwidthVolume",
                "tierQuotaType property does not match 'bandwidthVolume' in the JWT");
        
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
        JSONObject tierInfo = (JSONObject)((JSONObject)jwtObject.get("tierInfo")).get(subPolicyName);
        Assert.assertEquals(tierInfo.has("tierQuotaType"), true, "tierQuotaType property does not exist in the JWT");
        Assert.assertEquals(tierInfo.get("tierQuotaType"), "bandwidthVolume",
                "tierQuotaType property does not match 'bandwidthVolume' in the JWT");
        
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
        restAPIPublisher.deleteAPI(apiId);
        adminDashboardRestClient.deleteAPIPolicy(apiPolicyName);
        adminDashboardRestClient.deleteApplicationPolicy(appPolicyName);
        adminDashboardRestClient.deleteSubscriptionPolicy(subPolicyName);
    }
}
