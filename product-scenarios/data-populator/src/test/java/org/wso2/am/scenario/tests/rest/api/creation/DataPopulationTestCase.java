/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.tests.rest.api.creation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DataPopulationTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(APIRequest.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private String tierCollection = "Unlimited";
    private String tag = "APICreationTag";
    private String description = "This is a API creation description";
    private boolean default_version_checked = true;
    private final String APPLICATION_DESCRIPTION = "ApplicationDescription";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String resourcePathLocation
            = "/Users/prasanna/work/apim/product-apim/product-scenarios/scenarios-common/src/main/resources";

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider")
    public DataPopulationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws XPathExpressionException {

        System.setProperty("framework.resource.location", resourcePathLocation + "/");
        //create store server instance based on configuration given at automation.xml
        storeContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_STORE_INSTANCE, userMode);
        storeUrls = new APIMURLBean(storeContext.getContextUrls());

        //create publisher server instance based on configuration given at automation.xml
        publisherContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
        publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

        //create gateway server instance based on configuration given at automation.xml
        gatewayContextMgt =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, userMode);
        gatewayUrlsMgt = new APIMURLBean(gatewayContextMgt.getContextUrls());

        gatewayContextWrk =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, userMode);
        gatewayUrlsWrk = new APIMURLBean(gatewayContextWrk.getContextUrls());

        keyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, userMode);
        keyMangerUrl = new APIMURLBean(keyManagerContext.getContextUrls());

        backEndServer = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.BACKEND_SERVER_INSTANCE, userMode);
        backEndServerUrl = new APIMURLBean(backEndServer.getContextUrls());
        setup();
        System.setProperty("javax.net.ssl.trustStore", resourcePathLocation + "/keystores/client-truststore.jks");
    }

    @Test(description = "populateData")
    public void populateData() throws Exception {
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);

        if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
            //Add and activate wso2.com tenant
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                    TENANT_ADMIN_PW);
        }

        String dcrURL = gatewayUrlsMgt.getWebAppURLHttps() + "client-registration/v0.16/register";
        //DCR call for publisher app
        DCRParamRequest publisherParamRequest = new DCRParamRequest(RestAPIPublisherImpl.appName, RestAPIPublisherImpl.callBackURL,
                RestAPIPublisherImpl.tokenScope, RestAPIPublisherImpl.appOwner, RestAPIPublisherImpl.grantType, dcrURL,
                RestAPIPublisherImpl.username, RestAPIPublisherImpl.password,
                APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(publisherParamRequest);
        //DCR call for dev portal app
        DCRParamRequest devPortalParamRequest = new DCRParamRequest(RestAPIStoreImpl.appName, RestAPIStoreImpl.callBackURL,
                RestAPIStoreImpl.tokenScope, RestAPIStoreImpl.appOwner, RestAPIStoreImpl.grantType, dcrURL,
                RestAPIStoreImpl.username, RestAPIStoreImpl.password,
                APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(devPortalParamRequest);

        restAPIPublisher = new RestAPIPublisherImpl(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                ScenarioTestConstants.TENANT_WSO2, baseUrl);

        restAPIStore = new RestAPIStoreImpl(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW,
                ScenarioTestConstants.TENANT_WSO2, baseUrl);

        String apiId = createAPI("SampleAPI", "/customers", "/", "1.0.0",
                API_CREATOR_PUBLISHER_USERNAME, restAPIPublisher);

        publishAPI(apiId, restAPIPublisher);
        String applicationID = createApplication("SampleApplication", restAPIStore);
        String subscriptionId = createSubscription(apiId, applicationID, restAPIStore);
        String accessToken = generateKeys(applicationID, restAPIStore);

        log.info("API ID: " + apiId);
        log.info("APPLICATION ID: " + applicationID);
        log.info("SUBSCRIPTION ID: " + subscriptionId);
        log.info("ACCESS TOKEN: " + accessToken);

        System.out.println("Done");
    }

    private String createAPI(String apiName, String apiContext, String apiResource, String apiVersion,
                             String apiProviderName, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, ParseException, MalformedURLException {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);
        apiOperationsDTOs.add(apiOperationsDTO);

        List<String> tags = new ArrayList<>();
        tags.add(tag);

        List<String> tiersCollectionList = new ArrayList<>();
        tiersCollectionList.add(tierCollection);

        List<String> transports = new ArrayList<>();
        transports.add("http");
        transports.add("https");

        APIDTO apiCreationDTO = new APIDTO();
        apiCreationDTO.setName(apiName);
        apiCreationDTO.setContext(apiContext);
        apiCreationDTO.setVersion(apiVersion);
        apiCreationDTO.setProvider(apiProviderName);
        apiCreationDTO.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        apiCreationDTO.setOperations(apiOperationsDTOs);
        apiCreationDTO.setDescription(description);
        apiCreationDTO.setTags(tags);
        apiCreationDTO.policies(tiersCollectionList);
//        apiCreationDTO.setCacheTimeout(Integer.parseInt(cacheTimeout));
//        apiCreationDTO.setResponseCachingEnabled(false);
//        apiCreationDTO.setEndpointSecurity(securityDTO);
//        apiCreationDTO.setBusinessInformation(businessDTO);
//        apiCreationDTO.setSubscriptionAvailableTenants(subscriptionTenants);
        apiCreationDTO.setIsDefaultVersion(default_version_checked);
        apiCreationDTO.setTransport(transports);

        String endpointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        URL endpoint = new URL(endpointUrl);
        JSONParser parser = new JSONParser();
        String endPointString = "{\n" +
                "  \"production_endpoints\": {\n" +
                "    \"template_not_supported\": false,\n" +
                "    \"config\": null,\n" +
                "    \"url\": \"" + endpointUrl + "\"\n" +
                "  \"legacy-encoding\": \"" + endpoint + "\"\n" +
                "  },\n" +
                "  \"sandbox_endpoints\": {\n" +
                "    \"url\": \"" + endpointUrl + "\",\n" +
                "    \"config\": null,\n" +
                "    \"template_not_supported\": false\n" +
                "  \"legacy-encoding\": \"" + endpoint + "\"\n" +
                "  },\n" +
                "  \"endpoint_type\": \"http\"\n" +
                "}";

        Object jsonObject = parser.parse(endPointString);
        apiCreationDTO.setEndpointConfig(jsonObject);

        //Design API with name,context,version,visibility,apiResource and with all optional values
        APIDTO apidto = restAPIPublisher.addAPI(apiCreationDTO, apiVersion);
        return apidto.getId();
    }

    private void publishAPI(String apiId, RestAPIPublisherImpl restAPIPublisher) throws ApiException {
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
    }

    private String createApplication(String applicationName, RestAPIStoreImpl restAPIStore) {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        return applicationResponse.getData();
    }

    private String createSubscription(String apiId, String applicationId, RestAPIStoreImpl restAPIStore) {
        HttpResponse subscription = restAPIStore.createSubscription(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        return subscription.getData();

    }

    private String generateKeys(String applicationId, RestAPIStoreImpl restAPIStore)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        grantTypes.add("password");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600"
                , null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        return applicationKeyDTO.getToken().getAccessToken();
    }

    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER}
        };
    }
}
