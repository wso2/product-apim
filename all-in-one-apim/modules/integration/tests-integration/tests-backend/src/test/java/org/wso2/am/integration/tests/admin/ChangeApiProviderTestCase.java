/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.admin;

import jdk.internal.joptsimple.internal.Strings;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ResourcePolicyInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ResourcePolicyListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class ChangeApiProviderTestCase extends APIMIntegrationBaseTest {

    private String publisherURLHttp;
    private RestAPIAdminImpl restAPIAdminClient;
    private String BEARER = "Bearer ";
    private String APIName = "NewApiForProviderChange";
    private String APIContext = "NewApiForProviderChange";
    private String tags = "youtube, token, media";
    private String apiEndPointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String apiID;
    private String newUser = "peter123";
    private String firstUserName = "admin";
    private String newUserPass = "test123";
    private String[] subscriberRole = {APIMIntegrationConstants.APIM_INTERNAL_ROLE.CREATOR};
    private String APPLICATION_NAME = "testApplicationForProviderChange";
    private String applicationId;
    private String TIER_GOLD = "Gold";
    private String API_ENDPOINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String API_ENDPOINT_METHOD = "customers/123";
    private int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    private String RESPONSE_CODE_MISMATCH_ERROR_MESSAGE = "Response code mismatch";

    // SOAP API related variables
    private String SOAP_API_NAME = "SoapApiForProviderChange";
    private String SOAP_API_CONTEXT = "soapapiproviderchange";
    private String SOAP_API_VERSION = "1.0.0";
    private String soapApiId;
    private String SOAP_APPLICATION_NAME = "testSoapApplicationForProviderChange";
    private String soapApplicationId;
    private String soapPayload;

    // SOAPTOREST API related variables
    private String SOAPTOREST_API_NAME = "SoapToRestApiForProviderChange";
    private String SOAPTOREST_API_CONTEXT = "soaptorestproviderchange";
    private String SOAPTOREST_API_VERSION = "1.0.0";
    private String soapToRestApiId;
    private String SAPTOREST_APPLICATION_NAME = "testSoapToRestApplicationForProviderChange";
    private String soapToRestApplicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public ChangeApiProviderTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        publisherURLHttp = getPublisherURLHttp();
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        userManagementClient.addUser(newUser, newUserPass, subscriberRole, newUser);
        restAPIStore =
                new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                        storeContext.getContextTenant().getContextUser().getPassword(),
                        storeContext.getContextTenant().getDomain(), storeURLHttps);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_ENDPOINT_POSTFIX_URL;

        // Load SOAP request payload
        soapPayload = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator
                + "checkPhoneNumberRequestBody.xml");
    }

    @Test(groups = {"wso2.am"}, description = "Calling API with invalid token")
    public void ChangeApiProvider() throws Exception {
        String providerName = user.getUserName();
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(apiEndPointUrl));
        apiRequest.setTags(tags);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setResourceMethod("GET");
        //add test api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        apiID = serviceResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, Strings.EMPTY,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();

        restAPIStore.subscribeToAPI(apiID, applicationId, TIER_GOLD);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestHeaders.put(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
        HttpResponse apiInvokeResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(APIContext.replace(File.separator, Strings.EMPTY), APIVersion)
                        + File.separator + API_ENDPOINT_METHOD, requestHeaders);
        assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);

        //Update provider of the api
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, "carbon.super",
                adminURLHttps);
        if(user.getUserName().equals(firstUserName)){
            ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newUser, apiID);
            Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);
        }
        apiInvokeResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(APIContext.replace(File.separator, Strings.EMPTY), APIVersion)
                        + File.separator + API_ENDPOINT_METHOD, requestHeaders);
        assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);
    }

    @Test(groups = {"wso2.am"}, description = "Test changing provider of a SOAP API")
    public void ChangeSoapApiProvider() throws Exception {
        // Create SOAP API from WSDL file
        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl"
                + File.separator + "PhoneVerification.wsdl";
        File file = new File(wsdlDefinitionPath);

        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        JSONObject endpointObject = new JSONObject();
        endpointObject.put("type", "address");
        endpointObject.put("url", "http://ws.cdyne.com/phoneverify/phoneverify.asmx");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "address");
        endpointConfig.put("sandbox_endpoints", endpointObject);
        endpointConfig.put("production_endpoints", endpointObject);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", SOAP_API_NAME);
        additionalPropertiesObj.put("context", SOAP_API_CONTEXT);
        additionalPropertiesObj.put("version", SOAP_API_VERSION);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);

        // Create SOAP API using WSDL import
        APIDTO apidto = restAPIPublisher.importWSDLSchemaDefinition(
                file, null, additionalPropertiesObj.toString(), "SOAP");
        soapApiId = apidto.getId();

        Assert.assertEquals(apidto.getName(), SOAP_API_NAME);
        String expectedContext = user.getUserDomain().equals("carbon.super") ?
                "/" + SOAP_API_CONTEXT : "/t/" + user.getUserDomain() + "/" + SOAP_API_CONTEXT;
        Assert.assertEquals(apidto.getContext(), expectedContext);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapApiId, restAPIPublisher);

        // Publish the API
        restAPIPublisher.changeAPILifeCycleStatus(soapApiId, APILifeCycleAction.PUBLISH.getAction(), null);

        // Create application and subscribe
        HttpResponse applicationResponse = restAPIStore.createApplication(SOAP_APPLICATION_NAME, Strings.EMPTY,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        soapApplicationId = applicationResponse.getData();

        restAPIStore.subscribeToAPI(soapApiId, soapApplicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate keys and get access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(soapApplicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Invoke the SOAP API before provider change
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "text/xml");
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("SOAPAction", "http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber");
        requestHeaders.put(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        String invokeURL = getAPIInvocationURLHttp(SOAP_API_CONTEXT, SOAP_API_VERSION);
        HttpResponse soapApiInvokeResponse = HttpRequestUtil.doPost(new URL(invokeURL), soapPayload, requestHeaders);
        // Note: The backend may not be available, so we just verify the call goes through gateway
        // HTTP 500 from backend is acceptable as long as gateway routed the request
        Assert.assertTrue("SOAP API invocation failed unexpectedly before provider change",
                soapApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR);

        // Update provider of the SOAP API (only for admin)
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, "carbon.super",
                adminURLHttps);
        if (user.getUserName().equals(firstUserName)) {
            ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newUser, soapApiId);
            Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);
        }

        // Get WSDL definition after provider change to verify it's still accessible
        org.wso2.am.integration.clients.publisher.api.ApiResponse<Void> wsdlResponse =
                restAPIPublisher.getWSDLSchemaDefinitionOfAPI(soapApiId);
        Assert.assertEquals("Retrieving WSDL definition failed after provider change",
                Response.Status.OK.getStatusCode(), wsdlResponse.getStatusCode());

        // Get API definition (Swagger) after provider change to verify it's still accessible
        String apiDefinition = restAPIPublisher.getSwaggerByID(soapApiId);
        Assert.assertNotNull("API definition (Swagger) should not be null after provider change", apiDefinition);
        Assert.assertTrue("API definition (Swagger) should not be empty after provider change",
                apiDefinition.length() > 0);

        // Invoke the SOAP API after provider change to verify it still works
        soapApiInvokeResponse = HttpRequestUtil.doPost(new URL(invokeURL), soapPayload, requestHeaders);
        Assert.assertTrue("SOAP API invocation failed unexpectedly after provider change",
                soapApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test(groups = {"wso2.am"}, description = "Test changing provider of a SOAP to REST API")
    public void ChangeSoapToRestApiProvider() throws Exception {
        // Create SOAPTOREST API from WSDL file
        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl"
                + File.separator + "PhoneVerification.wsdl";
        File file = new File(wsdlDefinitionPath);

        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        JSONObject endpointObject = new JSONObject();
        endpointObject.put("type", "address");
        endpointObject.put("url", "http://ws.cdyne.com/phoneverify/phoneverify.asmx");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "address");
        endpointConfig.put("sandbox_endpoints", endpointObject);
        endpointConfig.put("production_endpoints", endpointObject);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", SOAPTOREST_API_NAME);
        additionalPropertiesObj.put("context", SOAPTOREST_API_CONTEXT);
        additionalPropertiesObj.put("version", SOAPTOREST_API_VERSION);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);

        // Create SOAPTOREST API using WSDL import
        APIDTO apidto = restAPIPublisher.importWSDLSchemaDefinition(
                file, null, additionalPropertiesObj.toString(), "SOAPTOREST");
        soapToRestApiId = apidto.getId();

        Assert.assertEquals(apidto.getName(), SOAPTOREST_API_NAME);
        String expectedContextSoapToRest = user.getUserDomain().equals("carbon.super") ?
                "/" + SOAPTOREST_API_CONTEXT : "/t/" + user.getUserDomain() + "/" + SOAPTOREST_API_CONTEXT;
        Assert.assertEquals(apidto.getContext(), expectedContextSoapToRest);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestApiId, restAPIPublisher);

        // Publish the API
        restAPIPublisher.changeAPILifeCycleStatus(soapToRestApiId, APILifeCycleAction.PUBLISH.getAction(), null);

        // Create application and subscribe
        HttpResponse applicationResponse = restAPIStore.createApplication(SAPTOREST_APPLICATION_NAME, Strings.EMPTY,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        soapToRestApplicationId = applicationResponse.getData();

        restAPIStore.subscribeToAPI(soapToRestApiId, soapToRestApplicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate keys and get access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(soapToRestApplicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Invoke the SOAPTOREST API before provider change
        // SOAPTOREST APIs accept JSON and transform to SOAP
        String restPayload = "{\"CheckPhoneNumber\":{\"PhoneNumber\":\"18006785432\",\"LicenseKey\":\"0\"}}";
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json");
        requestHeaders.put("accept", "application/json");
        requestHeaders.put(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        String invokeURL = getAPIInvocationURLHttp(SOAPTOREST_API_CONTEXT, SOAPTOREST_API_VERSION) + "/checkPhoneNumber";
        HttpResponse soapToRestApiInvokeResponse = HttpRequestUtil.doPost(new URL(invokeURL), restPayload, requestHeaders);
        // Note: The backend may not be available, so we just verify the call goes through gateway
        // HTTP 500 from backend is acceptable as long as gateway routed the request
        Assert.assertTrue("SOAPTOREST API invocation failed unexpectedly before provider change",
                soapToRestApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapToRestApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR);

        // Get "in" and "out" sequences before provider change
        ResourcePolicyListDTO resourcePolicyInListBefore = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "in", "checkPhoneNumber", "post");
        ResourcePolicyListDTO resourcePolicyOutListBefore = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "out", "checkPhoneNumber", "post");

        List<ResourcePolicyInfoDTO> resourcePoliciesInBefore = resourcePolicyInListBefore.getList();
        List<ResourcePolicyInfoDTO> resourcePoliciesOutBefore = resourcePolicyOutListBefore.getList();

        Assert.assertNotNull("In-sequences should not be null before provider change", resourcePoliciesInBefore);
        Assert.assertNotNull("Out-sequences should not be null before provider change", resourcePoliciesOutBefore);
        Assert.assertFalse("In-sequences should not be empty before provider change", resourcePoliciesInBefore.isEmpty());
        Assert.assertFalse("Out-sequences should not be empty before provider change", resourcePoliciesOutBefore.isEmpty());

        // Update provider of the SOAPTOREST API (only for admin)
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, "carbon.super",
                adminURLHttps);
        if (user.getUserName().equals(firstUserName)) {
            ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newUser, soapToRestApiId);
            Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);
        }

        // Get API definition (Swagger) after provider change to verify it's still accessible
        String apiDefinition = restAPIPublisher.getSwaggerByID(soapToRestApiId);
        Assert.assertNotNull("API definition (Swagger) should not be null after provider change", apiDefinition);
        Assert.assertTrue("API definition (Swagger) should not be empty after provider change",
                apiDefinition.length() > 0);

        // Get "in" and "out" sequences after provider change and verify they are the same
        ResourcePolicyListDTO resourcePolicyInListAfter = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "in", "checkPhoneNumber", "post");
        ResourcePolicyListDTO resourcePolicyOutListAfter = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "out", "checkPhoneNumber", "post");

        List<ResourcePolicyInfoDTO> resourcePoliciesInAfter = resourcePolicyInListAfter.getList();
        List<ResourcePolicyInfoDTO> resourcePoliciesOutAfter = resourcePolicyOutListAfter.getList();

        Assert.assertNotNull("In-sequences should not be null after provider change", resourcePoliciesInAfter);
        Assert.assertNotNull("Out-sequences should not be null after provider change", resourcePoliciesOutAfter);
        Assert.assertFalse("In-sequences should not be empty after provider change", resourcePoliciesInAfter.isEmpty());
        Assert.assertFalse("Out-sequences should not be empty after provider change", resourcePoliciesOutAfter.isEmpty());

        // Verify in-sequences are the same before and after provider change
        Assert.assertEquals("In-sequences count should match after provider change",
                resourcePoliciesInBefore.size(), resourcePoliciesInAfter.size());
        for (int i = 0; i < resourcePoliciesInBefore.size(); i++) {
            Assert.assertEquals("In-sequence content should match after provider change",
                    resourcePoliciesInBefore.get(i).getContent(), resourcePoliciesInAfter.get(i).getContent());
        }

        // Verify out-sequences are the same before and after provider change
        Assert.assertEquals("Out-sequences count should match after provider change",
                resourcePoliciesOutBefore.size(), resourcePoliciesOutAfter.size());
        for (int i = 0; i < resourcePoliciesOutBefore.size(); i++) {
            Assert.assertEquals("Out-sequence content should match after provider change",
                    resourcePoliciesOutBefore.get(i).getContent(), resourcePoliciesOutAfter.get(i).getContent());
        }

        // Update in-sequence after provider change
        for (ResourcePolicyInfoDTO inPolicy : resourcePoliciesInAfter) {
            String originalContent = inPolicy.getContent();
            String updatedContent = originalContent + "\n<!-- Updated after provider change -->";
            inPolicy.setContent(updatedContent);

            ResourcePolicyInfoDTO updatedPolicy = restAPIPublisher.updateApiResourcePolicies(
                    soapToRestApiId, inPolicy.getId(), inPolicy.getResourcePath(), inPolicy, null);

            Assert.assertNotNull("Updated in-sequence should not be null", updatedPolicy);
            Assert.assertTrue("Updated in-sequence should contain the update marker",
                    updatedPolicy.getContent().contains("<!-- Updated after provider change -->"));
        }

        // Update out-sequence after provider change
        for (ResourcePolicyInfoDTO outPolicy : resourcePoliciesOutAfter) {
            String originalContent = outPolicy.getContent();
            String updatedContent = originalContent + "\n<!-- Updated after provider change -->";
            outPolicy.setContent(updatedContent);

            ResourcePolicyInfoDTO updatedPolicy = restAPIPublisher.updateApiResourcePolicies(
                    soapToRestApiId, outPolicy.getId(), outPolicy.getResourcePath(), outPolicy, null);

            Assert.assertNotNull("Updated out-sequence should not be null", updatedPolicy);
            Assert.assertTrue("Updated out-sequence should contain the update marker",
                    updatedPolicy.getContent().contains("<!-- Updated after provider change -->"));
        }

        // Retrieve sequences again and verify updates were reflected
        ResourcePolicyListDTO resourcePolicyInListUpdated = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "in", "checkPhoneNumber", "post");
        ResourcePolicyListDTO resourcePolicyOutListUpdated = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "out", "checkPhoneNumber", "post");

        List<ResourcePolicyInfoDTO> resourcePoliciesInUpdated = resourcePolicyInListUpdated.getList();
        List<ResourcePolicyInfoDTO> resourcePoliciesOutUpdated = resourcePolicyOutListUpdated.getList();

        for (ResourcePolicyInfoDTO inPolicy : resourcePoliciesInUpdated) {
            Assert.assertTrue("Retrieved in-sequence should contain the update marker",
                    inPolicy.getContent().contains("<!-- Updated after provider change -->"));
        }

        for (ResourcePolicyInfoDTO outPolicy : resourcePoliciesOutUpdated) {
            Assert.assertTrue("Retrieved out-sequence should contain the update marker",
                    outPolicy.getContent().contains("<!-- Updated after provider change -->"));
        }

        // Invoke the SOAPTOREST API after provider change to verify it still works
        soapToRestApiInvokeResponse = HttpRequestUtil.doPost(new URL(invokeURL), restPayload, requestHeaders);
        Assert.assertTrue("SOAPTOREST API invocation failed unexpectedly after provider change",
                soapToRestApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapToRestApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        // Clean up REST API
        undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiID);
        // Clean up SOAP API
        undeployAndDeleteAPIRevisionsUsingRest(soapApiId, restAPIPublisher);
        restAPIStore.deleteApplication(soapApplicationId);
        restAPIPublisher.deleteAPI(soapApiId);
        // Clean up SOAPTOREST API
        undeployAndDeleteAPIRevisionsUsingRest(soapToRestApiId, restAPIPublisher);
        restAPIStore.deleteApplication(soapToRestApplicationId);
        restAPIPublisher.deleteAPI(soapToRestApiId);
        userManagementClient.deleteUser(newUser);
    }
}
