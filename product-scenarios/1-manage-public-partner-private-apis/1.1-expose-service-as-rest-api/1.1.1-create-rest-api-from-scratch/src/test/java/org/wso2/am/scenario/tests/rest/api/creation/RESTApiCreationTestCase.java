/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIEndpointSecurityDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RESTApiCreationTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(APIRequest.class);

    private APIPublisherRestClient apiPublisher;
    private APIRequest apiRequest;

    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";
    private String apiResource = "/find";
    private String apiVisibility = "public";

    private String description = "This is a API creation description";
    private String tag = "APICreationTag";
    private String tierCollection = "Silver";
    private String bizOwner = "wso2Test";
    private String bizOwnerMail = "wso2test@gmail.com";
    private String techOwner = "wso2";
    private String techOwnerMail = "wso2@gmail.com";
    private String endpointType = "secured";
    private String endpointAuthType = "BASIC";
    private String epUsername = "wso2";
    private String epPassword = "wso2123";
    private boolean default_version_checked = true;
    private boolean responseCache = true;
    private String cacheTimeout = "300";
    private String subscriptions = "all_tenants";
    private String http_checked = "http";
    private String https_checked = "";
    private String inSequence = "debug_in_flow";
    private String outSequence = "debug_out_flow";
    private String apiProviderName;
    private String apiProductionEndPointUrl;
    private String apiId;
    private  String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";
    private List<String> apiIdList = new ArrayList<>();

    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider")
    public RESTApiCreationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        setup();
        super.init(userMode);

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(description = "1.1.1.1", dataProvider = "apiNames", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPICreationWithMandatoryValues(String apiName) throws Exception {
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(apiName, "/" + apiName, new URL(backendEndPoint));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiCreationResponse.getData();
        apiIdList.add(apiId);

        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(), "Response Code miss matched when creating the API");
        verifyAPIName(apiName, apiId);
    }


    @Test(description = "1.1.1.2")
    public void testRESTAPICreationWithOptionalValues() throws Exception {
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverifyOptionaladd";

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);
        apiOperationsDTOs.add(apiOperationsDTO);

        APIEndpointSecurityDTO securityDTO = new APIEndpointSecurityDTO();
        securityDTO.setType(APIEndpointSecurityDTO.TypeEnum.BASIC);
        securityDTO.setUsername(epUsername);
        securityDTO.setPassword(epPassword);

        APIBusinessInformationDTO businessDTO = new APIBusinessInformationDTO();
        businessDTO.setBusinessOwner(bizOwner);
        businessDTO.setBusinessOwnerEmail(bizOwnerMail);
        businessDTO.setTechnicalOwner(techOwner);
        businessDTO.setTechnicalOwnerEmail(techOwnerMail);

        List<String> tags = new ArrayList<>();
        tags.add(tag);

        List<String> tiersCollectionList = new ArrayList<>();
        tiersCollectionList.add(tierCollection);

        List<String> subscriptionTenants = new ArrayList<>();
        subscriptionTenants.add(subscriptions);

        List<String> transports = new ArrayList<>();
        transports.add(http_checked);
        transports.add(https_checked);

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
        apiCreationDTO.setCacheTimeout(Integer.parseInt(cacheTimeout));
        apiCreationDTO.setResponseCachingEnabled(responseCache);
        apiCreationDTO.setEndpointSecurity(securityDTO);
        apiCreationDTO.setBusinessInformation(businessDTO);
        apiCreationDTO.setSubscriptionAvailableTenants(subscriptionTenants);
        apiCreationDTO.setIsDefaultVersion(default_version_checked);
        apiCreationDTO.setTransport(transports);

        //Design API with name,context,version,visibility,apiResource and with all optional values
        APIDTO apidto = restAPIPublisher.addAPI(apiCreationDTO, "v3");
        apiId = apidto.getId();
        apiIdList.add(apiId);

        HttpResponse serviceResponseGetApi = restAPIPublisher.getAPI(apiId);
        validateOptionalField(serviceResponseGetApi);
    }

    @Test(description = "1.1.1.4")
    public void testRESTAPICreationWithwildCardResource() throws Exception {
        apiName = "APIWildCard";
        apiContext = "apiwildcard";
        apiResource = "/*";

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(apiName, apiContext, new URL(backendEndPoint));
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setVersion(apiVersion);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        apiIdList.add(apiId);

        verifyResponse(serviceResponse);
        verifyAPIName(apiName, apiId);
    }

    private void validateOptionalField(HttpResponse response) throws APIManagerIntegrationTestException {
        JSONObject responseJson = new JSONObject(response.getData());
        assertEquals(responseJson.getJSONObject("businessInformation").get("businessOwner").toString(), bizOwner, "Expected bizOwner value not match");
        assertEquals(responseJson.getJSONObject("businessInformation").get("businessOwnerEmail").toString(), bizOwnerMail, "Expected bizOwnerMail value not match");
        assertEquals(responseJson.getJSONObject("businessInformation").get("technicalOwner").toString(), techOwner, "Expected techOwner value not match");
        assertEquals(responseJson.getJSONObject("businessInformation").get("technicalOwnerEmail").toString(), techOwnerMail, "Expected techOwnerMail value not match");
        assertEquals(responseJson.getJSONObject("endpointSecurity").get("type").toString(), "BASIC", "Expected endpointType value not match with the actual value");
        assertEquals(responseJson.getJSONObject("endpointSecurity").get("username").toString(), epUsername, "Expected epUsername value not match with the actual value");
        assertEquals(responseJson.getJSONArray("subscriptionAvailableTenants").get(0).toString(), subscriptions, "Expected subscriptions value not match");
        assertEquals(responseJson.getJSONArray("transport").get(0).toString(), http_checked, "Expected http_checked value not match");
        assertEquals(responseJson.get("isDefaultVersion"), default_version_checked, "Expected default_version_checked value not match");
        assertEquals(responseJson.get("cacheTimeout").toString(), cacheTimeout, "Expected cacheTimeout value not match");
    }

    private void verifyAPIName(String apiName, String apiId) throws APIManagerIntegrationTestException {
        try{
            HttpResponse getApi = restAPIPublisher.getAPI(apiId);
            JSONObject response = new JSONObject(getApi.getData());
            assertEquals(response.getJSONObject("api").get("name").toString(), apiName,
                    "Expected API name value not match");
        } catch (Exception e) {
        }

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String apiId : apiIdList) {
            restAPIPublisher.deleteAPI(apiId);
        }
    }

    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        //Add and activate wso2.com tenant
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, "admin", "admin");

        // create user in super tenant
        createUserWithCreatorRole("micheal", "Micheal#123", "admin", "admin");

        // create user in wso2.com tenant
        createUserWithCreatorRole("andrew", "Andrew#123", "admin@wso2.com", "admin");

        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
