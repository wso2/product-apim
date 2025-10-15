package org.wso2.am.integration.tests.application;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.xsd.Property;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionValidationDisableTestCase extends APIManagerLifecycleBaseTest {
    private static final String APPLICATION_NAME = "subscriptionValidationApp";
    private final String SP_NAME = "externalSP";
    private String applicationId;
    private String apiId;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_ENDPOINT_METHOD = "/customers/123";
    private final String API_NAME = "SubValidationDisabledAPI";
    private final String API_CONTEXT = "subValidationDisabledAPI";
    private String accessToken;
    private String accessTokenExternal;
    private String consumerKey;
    private String consumerSecret;

    @Factory(dataProvider = "userModeDataProvider")
    public SubscriptionValidationDisableTestCase(TestUserMode userMode) {
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
        createAPIAndApp();
        createServiceProvider();
    }

    @Test(groups = {"wso2.am"}, description = "Test API invocation without subscriptions when subscription " +
            "validation is disabled")
    public void testSubscriptionValidationDisablingForAPI() throws Exception {
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);
        List<String> tiers = new ArrayList<>();
        apidto.setPolicies(tiers);
        restAPIPublisher.updateAPI(apidto);

        APIDTO updatedApi = restAPIPublisher.getAPIByID(apiId);
        List<String> updatedTiers = updatedApi.getPolicies();
        Assert.assertEquals(updatedTiers.size(), 1,
                "The default internal subscription policy is not applied for the API");
        Assert.assertEquals(updatedTiers.get(0), APIMIntegrationConstants.API_TIER.DEFAULT_SUBSCRIPTIONLESSS,
                "The default internal subscription policy is not applied for the API");
    }

    @Test(groups = {"wso2.am"}, description = "Test API invocation without subscriptions when subscription " +
            "validation is disabled", dependsOnMethods = "testSubscriptionValidationDisablingForAPI")
    public void testAPIInvocationWithoutSubscription() throws Exception {
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKey = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKey, "Application key generation failed");
        accessToken = applicationKey.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_ENDPOINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "API invocation failed without subscription");

        SubscriptionListDTO listDTO = restAPIStore.getSubscription(apiId, applicationId, "", null);
        Assert.assertNotNull(listDTO, "Subscription retrieval failed");
        Assert.assertEquals(1, listDTO.getList().size(), "Internal subscription" +
                " not created when subscription validation was disabled");
        Assert.assertEquals(listDTO.getList().get(0).getThrottlingPolicy(),
                APIMIntegrationConstants.API_TIER.DEFAULT_SUBSCRIPTIONLESSS,
                "Default policy not applied to internal subscription");
    }

    @Test(groups = {"wso2.am"}, description = "Test API invocation with an external token",
            dependsOnMethods = "testAPIInvocationWithoutSubscription")
    public void testAPIInvocationWithExternalToken() throws Exception {
        String credentials = TokenUtils.getBase64EncodedAppCredentials(consumerKey, consumerSecret);

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("grant_type",
                APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL));
        Map<String, String> tokenHeaders = new HashMap<>();
        tokenHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Basic " + credentials);
        HttpResponse tokenResponse = HTTPSClientUtils.doPost(getKeyManagerURLHttps() + "/oauth2/token",
                tokenHeaders, urlParameters);
        Assert.assertEquals(tokenResponse.getResponseCode(), HttpStatus.SC_OK,
                "External token request failed");
        JSONObject jsonObject = new JSONObject(tokenResponse.getData());
        accessTokenExternal = jsonObject.getString("access_token");
        Assert.assertNotNull(accessTokenExternal, "Couldn't find the access token");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessTokenExternal);
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_ENDPOINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "API invocation failed for the external token");

        SubscriptionListDTO listDTO = restAPIStore.getSubscription(apiId, applicationId, "", null);
        Assert.assertNotNull(listDTO, "Subscription retrieval failed");
        Assert.assertEquals(1, listDTO.getList().size(),
                "Subscription count changed when invoked with external token");
    }

    @Test(groups = {"wso2.am"}, description = "Test API invocation after re-enabling subscription validation",
            dependsOnMethods = "testAPIInvocationWithExternalToken")
    public void testAPIInvocationAfterEnablingSubscriptionValidation() throws Exception {
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);
        apidto.getPolicies().add(APIMIntegrationConstants.API_TIER.UNLIMITED);
        restAPIPublisher.updateAPI(apidto);

        // Need to wait for a while until the API update event is received by the gateway
        Thread.sleep(60000);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_ENDPOINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200,
                "API invocation failed after re-enabling subscription validation");

        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessTokenExternal);
        requestHeaders2.put("accept", "text/xml");
        HttpResponse response2 = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                        API_ENDPOINT_METHOD, requestHeaders2);
        Assert.assertEquals(response2.getResponseCode(), 403,
                "API invocation successful for the external token after re-enabling subscription validation");
    }

    private void createAPIAndApp() throws Exception {
        // Create an API
        APIRequest apiRequest;
        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());

        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());

        // Create an application
        HttpResponse appCreationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Sub validation test app", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = appCreationResponse.getData();
    }

    private void createServiceProvider() throws Exception {
        OAuthConsumerAppDTO appDTO = new OAuthConsumerAppDTO();
        appDTO.setApplicationName(SP_NAME);
        appDTO.setCallbackUrl("http://localhost:9999");
        appDTO.setOAuthVersion("OAuth-2.0");
        appDTO.setGrantTypes("client_credentials");
        appDTO.setTokenType("JWT");

        oAuthAdminServiceClient.registerOAuthApplicationData(appDTO);
        OAuthConsumerAppDTO createdApp = oAuthAdminServiceClient.getOAuthAppByName(SP_NAME);
        Assert.assertNotNull(createdApp);

        consumerKey = createdApp.getOauthConsumerKey();
        consumerSecret = createdApp.getOauthConsumerSecret();

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(SP_NAME);
        applicationManagementClient.createApplication(serviceProvider);
        serviceProvider = applicationManagementClient.getApplication(SP_NAME);

        InboundAuthenticationRequestConfig requestConfig = new InboundAuthenticationRequestConfig();
        requestConfig.setInboundAuthKey(consumerKey);
        requestConfig.setInboundAuthType("oauth2");
        if (StringUtils.isNotBlank(consumerSecret)) {
            Property property = new Property();
            property.setName("oauthConsumerSecret");
            property.setValue(consumerSecret);
            Property[] properties = {property};
            requestConfig.setProperties(properties);
        }

        InboundAuthenticationConfig inboundAuthenticationConfig = new InboundAuthenticationConfig();
        inboundAuthenticationConfig
                .setInboundAuthenticationRequestConfigs(new InboundAuthenticationRequestConfig[]{requestConfig});
        serviceProvider.setInboundAuthenticationConfig(inboundAuthenticationConfig);
        applicationManagementClient.updateApplication(serviceProvider);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
        if (apiId != null) {
            restAPIPublisher.deleteAPI(apiId);
        }
    }
}
