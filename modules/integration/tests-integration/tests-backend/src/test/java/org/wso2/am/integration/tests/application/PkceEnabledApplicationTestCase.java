package org.wso2.am.integration.tests.application;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class PkceEnabledApplicationTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(PkceEnabledApplicationTestCase.class);

    private final String API_NAME = "GrantTypeTokenGenerateAPIName";
    private final String API_CONTEXT = "GrantTypeTokenGenerateContext";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME_ONE = "TokenGenerateAppWithPkce";
    private final String APP_NAME_TWO = "TokenGenerateAppWithPkcePlainText";
    private final String APP_NAME_THREE = "TokenGenerateAppWithPkcePlainTextByPassSecret";
    private final String CALLBACK_URL = "https://localhost:9443/store/";
    private final String TAGS = "grantType,implicitly,code";
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String endpointUrl;
    private String consumerKey, consumerSecret;
    private String apiId;
    private String applicationIdOne;
    private String applicationIdTwo;
    private String applicationIdThree;
    private ArrayList<String> grantTypes = new ArrayList<>();
    private APIRequest apiRequest;

    @Factory(dataProvider = "userModeDataProvider")
    public PkceEnabledApplicationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";

        //create Application one
        HttpResponse applicationResponseOne = restAPIStore.createApplication(APP_NAME_ONE,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponseOne.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationIdOne = applicationResponseOne.getData();

        //create Application one
        HttpResponse applicationResponseTwo = restAPIStore.createApplication(APP_NAME_TWO,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponseTwo.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationIdTwo = applicationResponseTwo.getData();

        //create Application one
        HttpResponse applicationResponseThree = restAPIStore.createApplication(APP_NAME_THREE,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponseThree.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationIdThree = applicationResponseThree.getData();

        String providerName = user.getUserName();

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiOperationsDTO.setTarget("/add");
        apiOperationsDTOS.add(apiOperationsDTO);

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));

        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setTiersCollection(TIER_COLLECTION);
        apiRequest.setTags(TAGS);
        apiRequest.setDescription(DESCRIPTION);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationIdOne,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.AUTHORIZATION_CODE);
    }


    @Test(groups = {"wso2.am"}, description = "Test Application Creation and key generations")
    public void testApplicationCreationKeyGenerationWithPkce() throws Exception {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("pkceMandatory", "true");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeysWithAdditionalProperties(applicationIdOne, "3600", CALLBACK_URL, ApplicationKeyGenerateRequestDTO
                        .KeyTypeEnum.PRODUCTION, null, grantTypes, additionalProperties);

        assertNotNull(applicationKeyDTO.getToken().getAccessToken());

        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        Map<String, Object> additionalPropertyResponse = (Map<String, Object>) applicationKeyDTO.getAdditionalProperties();
        Assert.assertEquals(Boolean.TRUE, additionalPropertyResponse.get("pkceMandatory"));
        Assert.assertEquals(Boolean.FALSE, additionalPropertyResponse.get("pkceSupportPlain"));
        Assert.assertEquals(Boolean.FALSE, additionalPropertyResponse.get("bypassClientCredentials"));
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");
    }

    @Test(groups = {"wso2.am"}, description = "Test Application Creation and key generations")
    public void testApplicationCreationKeyGenerationWithPkcePlainText() throws Exception {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("pkceMandatory", "true");
        additionalProperties.put("pkceSupportPlain", "true");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeysWithAdditionalProperties(applicationIdTwo, "3600", CALLBACK_URL, ApplicationKeyGenerateRequestDTO
                        .KeyTypeEnum.PRODUCTION, null, grantTypes, additionalProperties);

        assertNotNull(applicationKeyDTO.getToken().getAccessToken());

        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();

        Map<String, Object> additionalPropertyResponse = (Map<String, Object>) applicationKeyDTO.getAdditionalProperties();
        Assert.assertEquals(Boolean.TRUE, additionalPropertyResponse.get("pkceMandatory"));
        Assert.assertEquals(Boolean.TRUE, additionalPropertyResponse.get("pkceSupportPlain"));
        Assert.assertEquals(Boolean.FALSE, additionalPropertyResponse.get("bypassClientCredentials"));
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");
    }

    @Test(groups = {"wso2.am"}, description = "Test Application Creation and key generations")
    public void testApplicationCreationKeyGenerationWithPkcePlainTextByPassSecret() throws Exception {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("pkceMandatory", "true");
        additionalProperties.put("pkceSupportPlain", "true");
        additionalProperties.put("bypassClientCredentials", "true");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeysWithAdditionalProperties(applicationIdThree, "3600", CALLBACK_URL, ApplicationKeyGenerateRequestDTO
                        .KeyTypeEnum.PRODUCTION, null, grantTypes, additionalProperties);
        assertNotNull(applicationKeyDTO.getToken().getAccessToken());

        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        Map<String, Object> additionalPropertyResponse = (Map<String, Object>) applicationKeyDTO.getAdditionalProperties();
        Assert.assertEquals(Boolean.TRUE, additionalPropertyResponse.get("pkceMandatory"));
        Assert.assertEquals(Boolean.TRUE, additionalPropertyResponse.get("pkceSupportPlain"));
        Assert.assertEquals(Boolean.TRUE, additionalPropertyResponse.get("bypassClientCredentials"));
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationIdOne);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}};
    }
}
