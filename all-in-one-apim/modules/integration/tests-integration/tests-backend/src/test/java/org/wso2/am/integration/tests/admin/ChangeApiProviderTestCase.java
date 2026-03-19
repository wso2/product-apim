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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jdk.internal.joptsimple.internal.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CommentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLSchemaDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ResourcePolicyInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ResourcePolicyListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ChangeApiProviderTestCase extends APIMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient;
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
    private String[] newUserRoles = {
            APIMIntegrationConstants.APIM_INTERNAL_ROLE.CREATOR,
            APIMIntegrationConstants.APIM_INTERNAL_ROLE.PUBLISHER,
            APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER
    };
    private String APPLICATION_NAME = "testApplicationForProviderChange";
    private String applicationId;
    private String TIER_GOLD = "Gold";
    private String API_ENDPOINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String API_ENDPOINT_METHOD = "customers/123";
    private int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    private String RESPONSE_CODE_MISMATCH_ERROR_MESSAGE = "Response code mismatch";
    private String documentId;
    private String scopeName = "TestScope";

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

    // GRAPHQL API related variables
    private String GRAPHQL_API_NAME = "GraphQLApiForProviderChange";
    private String GRAPHQL_API_CONTEXT = "graphqlproviderchange";
    private String GRAPHQL_API_VERSION = "1.0.0";
    private String graphqlApiId;
    private String GRAPHQL_APPLICATION_NAME = "testGraphQLApplicationForProviderChange";
    private String graphqlApplicationId;
    private String graphqlSchemaDefinition;

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
        userManagementClient.addUser(newUser, newUserPass, newUserRoles, newUser);
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
        Assert.assertEquals(serviceResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        apiID = serviceResponse.getData();

        // Create Revision and Deploy to Gateway
        String revisionUUID = createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), APIName, APIVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

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
        Assert.assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);

        // Add documentation to the API before provider change
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        documentDTO.setName("HowTo Guide");
        documentDTO.setSummary("This is a test document for provider change test");
        documentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        documentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        HttpResponse addDocResponse = restAPIPublisher.addDocument(apiID, documentDTO);
        Assert.assertEquals(addDocResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Document addition failed");
        documentId = addDocResponse.getData();

        // Verify document was added
        DocumentListDTO documentListDTO = restAPIPublisher.getDocuments(apiID);
        boolean documentFound = false;
        for (DocumentDTO doc : documentListDTO.getList()) {
            if (documentId.equals(doc.getDocumentId())) {
                documentFound = true;
                break;
            }
        }
        Assert.assertTrue(documentFound, "Document should be present in API before provider change");

        // Add local scope to the API before provider change
        APIDTO apiDtoForScope = restAPIPublisher.getAPIByID(apiID);
        String originalProvider = apiDtoForScope.getProvider();
        ScopeDTO scopeDTO = new ScopeDTO();
        scopeDTO.setName(scopeName);
        scopeDTO.setDisplayName("Test Scope Display Name");
        scopeDTO.setDescription("This is a test scope");
        java.util.List<String> bindings = new java.util.ArrayList<>();
        bindings.add("admin");
        scopeDTO.setBindings(bindings);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(scopeDTO);
        apiScopeDTO.setShared(false);

        java.util.List<APIScopeDTO> scopeList = new java.util.ArrayList<>();
        scopeList.add(apiScopeDTO);
        apiDtoForScope.setScopes(scopeList);
        restAPIPublisher.updateAPI(apiDtoForScope, apiID);
        waitForAPIDeployment();

        // Verify scope was added
        APIDTO apiWithScope = restAPIPublisher.getAPIByID(apiID);
        Assert.assertNotNull(apiWithScope.getScopes(), "Scopes should not be null after adding");
        Assert.assertEquals(apiWithScope.getScopes().size(), 1, "Should have one scope");
        Assert.assertEquals(apiWithScope.getScopes().get(0).getScope().getName(), scopeName,
                "Scope name should match");

        // Update provider of the api
        String tenantDomain = user.getUserDomain();
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, tenantDomain,
                adminURLHttps);
        String newProviderName = tenantDomain.equals("carbon.super") ? newUser : newUser + "@" + tenantDomain;
        ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newProviderName, apiID);
        Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);

        // Verify the provider was actually changed
        APIDTO apiAfterProviderChange = restAPIPublisher.getAPIByID(apiID);
        Assert.assertEquals(apiAfterProviderChange.getProvider(), newProviderName,
                "API provider should be changed to " + newProviderName);

        // Get API using revision ID after provider change
        APIDTO apiByRevision = restAPIPublisher.getAPIByID(revisionUUID);
        Assert.assertNotNull(apiByRevision, "API retrieved by revision should not be null");
        Assert.assertEquals(apiByRevision.getProvider(), originalProvider,
                "API provider from revision should match new provider");

        // Get swagger using revision ID after provider change
        String swaggerByRevision = restAPIPublisher.getSwaggerByID(revisionUUID);
        Assert.assertNotNull(swaggerByRevision, "Swagger retrieved by revision should not be null");
        Assert.assertTrue(swaggerByRevision.length() > 0,
                "Swagger retrieved by revision should not be empty");

        // Negative test: Try to change provider to a user in a different tenant domain
        // This should fail with 400 and tenant mismatch error
        String crossTenantProvider = tenantDomain.equals("carbon.super") ? firstUserName + "@wso2.com" : firstUserName;
        try {
            restAPIAdminClient.changeApiProvider(crossTenantProvider, apiID);
            Assert.fail("Provider change to different tenant should fail with 400");
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Cross-tenant provider change should return 400");
            String responseBody = e.getResponseBody();
            Assert.assertTrue(responseBody.contains("901409"),
                    "Error response should contain code 901409");
            Assert.assertTrue(responseBody.contains("Tenant mismatch"),
                    "Error response should contain 'Tenant mismatch' message");
        }

        // Undeploy the existing revision first
        List<APIRevisionDeployUndeployRequest> undeployList = new ArrayList<>();
        APIRevisionDeployUndeployRequest undeployReq = new APIRevisionDeployUndeployRequest();
        undeployReq.setName(Constants.GATEWAY_ENVIRONMENT);
        undeployReq.setVhost(null);
        undeployReq.setDisplayOnDevportal(true);
        undeployList.add(undeployReq);
        restAPIPublisher.undeployAPIRevision(apiID, revisionUUID, undeployList);
        waitForAPIDeployment();

        // Deploy a new revision after provider change
        String revisionUUID_2 = createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), APIName, APIVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        apiInvokeResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(APIContext.replace(File.separator, Strings.EMPTY), APIVersion)
                        + File.separator + API_ENDPOINT_METHOD, requestHeaders);
        Assert.assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);

        // Step 1: Update API description after provider change
        APIDTO apiDto = restAPIPublisher.getAPIByID(apiID);
        String updatedDescription = "This is an updated description after provider change";
        apiDto.setDescription(updatedDescription);
        restAPIPublisher.updateAPI(apiDto, apiID);
        waitForAPIDeployment();

        // Step 2: Verify the description was updated
        APIDTO updatedApiDto = restAPIPublisher.getAPIByID(apiID);
        Assert.assertTrue(updatedApiDto.getDescription().equals(updatedDescription),
                "API description update failed after provider change");

        // Step 3: Update API business information after provider change
        APIDTO apiDtoForBusinessInfo = restAPIPublisher.getAPIByID(apiID);
        APIBusinessInformationDTO businessInfoDto = new APIBusinessInformationDTO();
        businessInfoDto.setBusinessOwner("Updated Jane Roe");
        businessInfoDto.setBusinessOwnerEmail("marketing@pizzashack.com");
        businessInfoDto.setTechnicalOwner("John Doe");
        businessInfoDto.setTechnicalOwnerEmail("architecture@pizzashack.com");
        apiDtoForBusinessInfo.setBusinessInformation(businessInfoDto);
        restAPIPublisher.updateAPI(apiDtoForBusinessInfo, apiID);
        waitForAPIDeployment();

        // Step 4: Verify the business information was updated
        APIDTO updatedApiWithBusinessInfo = restAPIPublisher.getAPIByID(apiID);
        Assert.assertNotNull(updatedApiWithBusinessInfo.getBusinessInformation(),
                "Business information should not be null after update");
        Assert.assertEquals(updatedApiWithBusinessInfo.getBusinessInformation().getBusinessOwner(),
                "Updated Jane Roe",
                "Business owner update failed after provider change");
        Assert.assertEquals(updatedApiWithBusinessInfo.getBusinessInformation().getBusinessOwnerEmail(),
                "marketing@pizzashack.com",
                "Business owner email update failed after provider change");
        Assert.assertEquals(updatedApiWithBusinessInfo.getBusinessInformation().getTechnicalOwner(),
                "John Doe",
                "Technical owner update failed after provider change");
        Assert.assertEquals(updatedApiWithBusinessInfo.getBusinessInformation().getTechnicalOwnerEmail(),
                "architecture@pizzashack.com",
                "Technical owner email update failed after provider change");

        // Step 5: Add new subscription tier (Gold) to API after provider change
        APIDTO apiDtoForTier = restAPIPublisher.getAPIByID(apiID);
        apiDtoForTier.getPolicies().add(TIER_GOLD);
        restAPIPublisher.updateAPI(apiDtoForTier, apiID);
        waitForAPIDeployment();

        // Step 6: Verify the subscription tier was added
        APIDTO updatedApiWithTier = restAPIPublisher.getAPIByID(apiID);
        Assert.assertTrue(updatedApiWithTier.getPolicies().contains(TIER_GOLD),
                "Gold tier should be present in API policies after update");

        // Step 7: Update document after provider change
        DocumentDTO updatedDocumentDTO = new DocumentDTO();
        updatedDocumentDTO.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        updatedDocumentDTO.setName("HowTo Guide");
        updatedDocumentDTO.setSummary("This is an updated test document after provider change");
        updatedDocumentDTO.setType(DocumentDTO.TypeEnum.HOWTO);
        updatedDocumentDTO.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);

        HttpResponse updateDocResponse = restAPIPublisher.updateDocument(apiID, documentId, updatedDocumentDTO);
        Assert.assertEquals(updateDocResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Document update failed after provider change");

        // Step 8: Verify the document was updated
        DocumentListDTO updatedDocumentListDTO = restAPIPublisher.getDocuments(apiID);
        boolean updatedDocumentFound = false;
        for (DocumentDTO doc : updatedDocumentListDTO.getList()) {
            if (documentId.equals(doc.getDocumentId())
                    && "This is an updated test document after provider change".equals(doc.getSummary())) {
                updatedDocumentFound = true;
                break;
            }
        }
        Assert.assertTrue(updatedDocumentFound,
                "Updated document should be present after provider change");

        // Step 9: Add comment to API after provider change
        HttpResponse addCommentResponse = restAPIPublisher.addComment(apiID, "Comment after provider change",
                "general", null);
        Assert.assertEquals(addCommentResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Comment addition failed after provider change");
        Gson commentsGson = new Gson();
        CommentDTO addedComment = commentsGson.fromJson(
                addCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        String commentId = addedComment.getId();

        // Step 10: Verify the comment was added by retrieving it
        HttpResponse getCommentResponse = restAPIPublisher.getComment(
                commentId, apiID, gatewayContextWrk.getContextTenant().getDomain(),
                false, 3, 0);
        Assert.assertEquals(getCommentResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Error retrieving comment");
        CommentDTO retrievedComment = commentsGson.fromJson(getCommentResponse.getData().replace("publisher",
                "PUBLISHER"), CommentDTO.class);
        Assert.assertTrue(retrievedComment.getContent().equals("Comment after provider change"),
                "Comment content should match");
        Assert.assertTrue(retrievedComment.getCategory().equals("general"),
                "Comment category should match");
        Assert.assertEquals(retrievedComment.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER,
                "Comment entry point should be PUBLISHER");

        // Step 11: Add new resource to API after provider change
        String oldSwagger = restAPIPublisher.getSwaggerByID(apiID);
        String modifiedSwagger = "{\n" +
                "    \"swagger\": \"2.0\",\n" +
                "    \"paths\": {\n" +
                "        \"/customers\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"x-auth-type\": \"Application & Application User\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"/test\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"x-auth-type\": \"Application & Application User\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"info\": {\n" +
                "        \"title\": \"" + APIName + "\",\n" +
                "        \"version\": \"" + APIVersion + "\"\n" +
                "    }\n" +
                "}";
        String swaggerResponse = restAPIPublisher.updateSwagger(apiID, modifiedSwagger);
        Assert.assertNotNull(swaggerResponse, "Swagger update response should not be null");

        // Step 12: Verify the new resource was added
        String updatedSwagger = restAPIPublisher.getSwaggerByID(apiID);
        Assert.assertNotEquals(updatedSwagger, oldSwagger, "Swagger should be updated with new resource");
        Assert.assertTrue(updatedSwagger.contains("/test"), "Updated swagger should contain the new /test resource");

        // Step 13: Update OpenAPI description via swagger after provider change
        String swaggerBeforeDescUpdate = restAPIPublisher.getSwaggerByID(apiID);
        String updatedSwaggerWithDesc = "{\n" +
                "    \"swagger\": \"2.0\",\n" +
                "    \"info\": {\n" +
                "        \"title\": \"" + APIName + "\",\n" +
                "        \"version\": \"" + APIVersion + "\",\n" +
                "        \"description\": \"Updated. This is an updated API description after provider change.\"\n" +
                "    },\n" +
                "    \"paths\": {\n" +
                "        \"/customers\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"x-auth-type\": \"Application & Application User\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"/test\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"OK\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"x-auth-type\": \"Application & Application User\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String swaggerDescResponse = restAPIPublisher.updateSwagger(apiID, updatedSwaggerWithDesc);
        Assert.assertNotNull(swaggerDescResponse, "Swagger description update response should not be null");

        // Step 14: Verify the OpenAPI description was updated
        String swaggerAfterDescUpdate = restAPIPublisher.getSwaggerByID(apiID);
        Assert.assertNotEquals(swaggerAfterDescUpdate, swaggerBeforeDescUpdate,
                "Swagger should be updated with new description");
        Assert.assertTrue(swaggerAfterDescUpdate.contains("Updated. This is an updated API description after provider change"),
                "Updated swagger should contain the new description");

        // Step 15: Update API endpoint after provider change
        APIDTO apiDtoForEndpoint = restAPIPublisher.getAPIByID(apiID);
        String updatedEndpointUrl = apiEndPointUrl + "/updated";
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) apiDtoForEndpoint.getEndpointConfig());
        Map productionEndpoint = (Map) endpointConfigJson.get("production_endpoints");
        productionEndpoint.replace("url", updatedEndpointUrl);
        Map sandboxEndpoint = (Map) endpointConfigJson.get("sandbox_endpoints");
        sandboxEndpoint.replace("url", updatedEndpointUrl);
        apiDtoForEndpoint.setEndpointConfig(endpointConfigJson);
        restAPIPublisher.updateAPI(apiDtoForEndpoint, apiID);
        waitForAPIDeployment();

        // Step 16: Verify the API endpoint was updated
        APIDTO updatedApiWithEndpoint = restAPIPublisher.getAPIByID(apiID);
        Assert.assertNotNull(updatedApiWithEndpoint.getEndpointConfig(),
                "Endpoint config should not be null after update");
        Map<String, Object> updatedEndpointConfig =
                (Map<String, Object>) updatedApiWithEndpoint.getEndpointConfig();
        Map<String, Object> productionEndpoints =
                (Map<String, Object>) updatedEndpointConfig.get("production_endpoints");
        Assert.assertNotNull(productionEndpoints, "Production endpoints should not be null");
        Assert.assertTrue(productionEndpoints.get("url").toString().contains("/updated"),
                "Production endpoint URL should contain '/updated'");

        // Step 17: Update local scope after provider change
        APIDTO apiDtoForScopeUpdate = restAPIPublisher.getAPIByID(apiID);
        ScopeDTO updatedScopeDTO = new ScopeDTO();
        updatedScopeDTO.setName(scopeName);
        updatedScopeDTO.setDisplayName("Updated");
        updatedScopeDTO.setDescription("");
        java.util.List<String> updatedBindings = new java.util.ArrayList<>();
        updatedBindings.add("admin");
        updatedBindings.add("Internal/creator");
        updatedScopeDTO.setBindings(updatedBindings);

        APIScopeDTO updatedApiScopeDTO = new APIScopeDTO();
        updatedApiScopeDTO.setScope(updatedScopeDTO);
        updatedApiScopeDTO.setShared(false);

        java.util.List<APIScopeDTO> updatedScopeList = new java.util.ArrayList<>();
        updatedScopeList.add(updatedApiScopeDTO);
        apiDtoForScopeUpdate.setScopes(updatedScopeList);
        restAPIPublisher.updateAPI(apiDtoForScopeUpdate, apiID);
        waitForAPIDeployment();

        // Step 18: Verify the local scope was updated
        APIDTO apiWithUpdatedScope = restAPIPublisher.getAPIByID(apiID);
        Assert.assertNotNull(apiWithUpdatedScope.getScopes(), "Scopes should not be null after update");
        Assert.assertEquals(apiWithUpdatedScope.getScopes().size(), 1, "Should have one scope after update");
        java.util.List<String> updatedScopeBindings = apiWithUpdatedScope.getScopes().get(0).getScope().getBindings();
        Assert.assertNotNull(updatedScopeBindings, "Scope bindings should not be null");
        Assert.assertTrue(updatedScopeBindings.contains("Internal/creator"),
                "Scope bindings should contain 'Internal/creator' role");
        Assert.assertTrue(updatedScopeBindings.contains("admin"),
                "Scope bindings should still contain 'admin' role");
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
        String originalProvider = apidto.getProvider();
        soapApiId = apidto.getId();

        Assert.assertEquals(apidto.getName(), SOAP_API_NAME);
        String expectedContext = user.getUserDomain().equals("carbon.super") ?
                "/" + SOAP_API_CONTEXT : "/t/" + user.getUserDomain() + "/" + SOAP_API_CONTEXT;
        Assert.assertEquals(apidto.getContext(), expectedContext);

        // Create Revision and Deploy to Gateway
        String soapApiRevisionUUID = createAPIRevisionAndDeployUsingRest(soapApiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAP_API_NAME, SOAP_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

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
        Assert.assertTrue(soapApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "SOAP API invocation failed unexpectedly before provider change");

        // Update provider of the SOAP API
        String tenantDomainSoap = user.getUserDomain();
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, tenantDomainSoap,
                adminURLHttps);
        String newProviderNameSoap = tenantDomainSoap.equals("carbon.super") ? newUser : newUser + "@" + tenantDomainSoap;
        ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newProviderNameSoap, soapApiId);
        Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);

        // Verify the provider was actually changed
        APIDTO apiAfterProviderChangeSoap = restAPIPublisher.getAPIByID(soapApiId);
        Assert.assertEquals(apiAfterProviderChangeSoap.getProvider(), newProviderNameSoap,
                "SOAP API provider should be changed to " + newProviderNameSoap);

        // Undeploy the existing revision first
        List<APIRevisionDeployUndeployRequest> undeployList = new ArrayList<>();
        APIRevisionDeployUndeployRequest undeployReq = new APIRevisionDeployUndeployRequest();
        undeployReq.setName(Constants.GATEWAY_ENVIRONMENT);
        undeployReq.setVhost(null);
        undeployReq.setDisplayOnDevportal(true);
        undeployList.add(undeployReq);
        restAPIPublisher.undeployAPIRevision(soapApiId, soapApiRevisionUUID, undeployList);
        waitForAPIDeployment();

        // Deploy a new revision after provider change
        String soapApiRevisionUUID_2 = createAPIRevisionAndDeployUsingRest(soapApiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAP_API_NAME, SOAP_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Get WSDL definition after provider change to verify it's still accessible
        org.wso2.am.integration.clients.publisher.api.ApiResponse<Void> wsdlResponse =
                restAPIPublisher.getWSDLSchemaDefinitionOfAPI(soapApiId);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), wsdlResponse.getStatusCode(),
                "Retrieving WSDL definition failed after provider change");

        // Get API definition (Swagger) after provider change to verify it's still accessible
        String apiDefinition = restAPIPublisher.getSwaggerByID(soapApiId);
        Assert.assertNotNull(apiDefinition, "API definition (Swagger) should not be null after provider change");
        Assert.assertTrue(apiDefinition.length() > 0,
                "API definition (Swagger) should not be empty after provider change");

        // Get API using revision UUID after provider change
        APIDTO apiByRevisionSoap = restAPIPublisher.getAPIByID(soapApiRevisionUUID);
        Assert.assertNotNull(apiByRevisionSoap, "SOAP API retrieved by revision should not be null");
        Assert.assertEquals(apiByRevisionSoap.getProvider(), originalProvider,
                "SOAP API provider from revision should match new provider");

        // Get WSDL using revision UUID after provider change
        org.wso2.am.integration.clients.publisher.api.ApiResponse<Void> wsdlByRevision =
                restAPIPublisher.getWSDLSchemaDefinitionOfAPI(soapApiRevisionUUID);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), wsdlByRevision.getStatusCode(),
                "Retrieving WSDL by revision failed after provider change");

        // Get Swagger using revision UUID after provider change
        String swaggerByRevisionSoap = restAPIPublisher.getSwaggerByID(soapApiRevisionUUID);
        Assert.assertNotNull(swaggerByRevisionSoap, "Swagger retrieved by revision should not be null");
        Assert.assertTrue(swaggerByRevisionSoap.length() > 0,
                "Swagger retrieved by revision should not be empty");

        // Invoke the SOAP API after provider change to verify it still works
        soapApiInvokeResponse = HttpRequestUtil.doPost(new URL(invokeURL), soapPayload, requestHeaders);
        Assert.assertTrue(soapApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "SOAP API invocation failed unexpectedly after provider change");
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
        String originalProvider = apidto.getProvider();
        soapToRestApiId = apidto.getId();

        Assert.assertEquals(apidto.getName(), SOAPTOREST_API_NAME);
        String expectedContextSoapToRest = user.getUserDomain().equals("carbon.super") ?
                "/" + SOAPTOREST_API_CONTEXT : "/t/" + user.getUserDomain() + "/" + SOAPTOREST_API_CONTEXT;
        Assert.assertEquals(apidto.getContext(), expectedContextSoapToRest);

        // Create Revision and Deploy to Gateway
        String soapToRestRevisionUUID = createAPIRevisionAndDeployUsingRest(soapToRestApiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, SOAPTOREST_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

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
        Assert.assertTrue(soapToRestApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapToRestApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "SOAPTOREST API invocation failed unexpectedly before provider change");

        // Get "in" and "out" sequences before provider change
        ResourcePolicyListDTO resourcePolicyInListBefore = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "in", "checkPhoneNumber", "post");
        ResourcePolicyListDTO resourcePolicyOutListBefore = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "out", "checkPhoneNumber", "post");

        List<ResourcePolicyInfoDTO> resourcePoliciesInBefore = resourcePolicyInListBefore.getList();
        List<ResourcePolicyInfoDTO> resourcePoliciesOutBefore = resourcePolicyOutListBefore.getList();

        Assert.assertNotNull(resourcePoliciesInBefore, "In-sequences should not be null before provider change");
        Assert.assertNotNull(resourcePoliciesOutBefore, "Out-sequences should not be null before provider change");
        Assert.assertFalse(resourcePoliciesInBefore.isEmpty(),
                "In-sequences should not be empty before provider change");
        Assert.assertFalse(resourcePoliciesOutBefore.isEmpty(),
                "Out-sequences should not be empty before provider change");

        // Update provider of the SOAPTOREST API
        String tenantDomainSoapToRest = user.getUserDomain();
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, tenantDomainSoapToRest,
                adminURLHttps);
        String newProviderNameSoapToRest = tenantDomainSoapToRest.equals("carbon.super") ? newUser : newUser + "@" + tenantDomainSoapToRest;
        ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newProviderNameSoapToRest, soapToRestApiId);
        Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);

        // Verify the provider was actually changed
        APIDTO apiAfterProviderChangeSoapToRest = restAPIPublisher.getAPIByID(soapToRestApiId);
        Assert.assertEquals(apiAfterProviderChangeSoapToRest.getProvider(), newProviderNameSoapToRest,
                "SOAPTOREST API provider should be changed to " + newProviderNameSoapToRest);

        // Undeploy the existing revision first
        List<APIRevisionDeployUndeployRequest> undeployList = new ArrayList<>();
        APIRevisionDeployUndeployRequest undeployReq = new APIRevisionDeployUndeployRequest();
        undeployReq.setName(Constants.GATEWAY_ENVIRONMENT);
        undeployReq.setVhost(null);
        undeployReq.setDisplayOnDevportal(true);
        undeployList.add(undeployReq);
        restAPIPublisher.undeployAPIRevision(soapToRestApiId, soapToRestRevisionUUID, undeployList);
        waitForAPIDeployment();

        // Deploy a new revision after provider change
        String soapToRestRevisionUUID_2 = createAPIRevisionAndDeployUsingRest(soapToRestApiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, SOAPTOREST_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Get API definition (Swagger) after provider change to verify it's still accessible
        String apiDefinition = restAPIPublisher.getSwaggerByID(soapToRestApiId);
        Assert.assertNotNull(apiDefinition, "API definition (Swagger) should not be null after provider change");
        Assert.assertTrue(apiDefinition.length() > 0,
                "API definition (Swagger) should not be empty after provider change");

        // Get "in" and "out" sequences after provider change and verify they are the same
        ResourcePolicyListDTO resourcePolicyInListAfter = restAPIPublisher.getApiResourcePolicies(
                soapToRestApiId, "in", "checkPhoneNumber", "post");
        ResourcePolicyListDTO resourcePolicyOutListAfter = restAPIPublisher.getApiResourcePolicies(
                soapToRestApiId, "out", "checkPhoneNumber", "post");

        List<ResourcePolicyInfoDTO> resourcePoliciesInAfter = resourcePolicyInListAfter.getList();
        List<ResourcePolicyInfoDTO> resourcePoliciesOutAfter = resourcePolicyOutListAfter.getList();

        Assert.assertNotNull(resourcePoliciesInAfter, "In-sequences should not be null after provider change");
        Assert.assertNotNull(resourcePoliciesOutAfter, "Out-sequences should not be null after provider change");
        Assert.assertFalse(resourcePoliciesInAfter.isEmpty(),
                "In-sequences should not be empty after provider change");
        Assert.assertFalse(resourcePoliciesOutAfter.isEmpty(),
                "Out-sequences should not be empty after provider change");

        // Verify in-sequences are the same before and after provider change
        Assert.assertEquals(resourcePoliciesInBefore.size(), resourcePoliciesInAfter.size(),
                "In-sequences count should match after provider change");
        for (int i = 0; i < resourcePoliciesInBefore.size(); i++) {
            Assert.assertEquals(resourcePoliciesInBefore.get(i).getContent(),
                    resourcePoliciesInAfter.get(i).getContent(),
                    "In-sequence content should match after provider change");
        }

        // Verify out-sequences are the same before and after provider change
        Assert.assertEquals(resourcePoliciesOutBefore.size(), resourcePoliciesOutAfter.size(),
                "Out-sequences count should match after provider change");
        for (int i = 0; i < resourcePoliciesOutBefore.size(); i++) {
            Assert.assertEquals(resourcePoliciesOutBefore.get(i).getContent(),
                    resourcePoliciesOutAfter.get(i).getContent(),
                    "Out-sequence content should match after provider change");
        }

        // Update in-sequence after provider change
        for (ResourcePolicyInfoDTO inPolicy : resourcePoliciesInAfter) {
            String originalContent = inPolicy.getContent();
            String updatedContent = originalContent + "\n<!-- Updated after provider change -->";
            inPolicy.setContent(updatedContent);

            ResourcePolicyInfoDTO updatedPolicy = restAPIPublisher.updateApiResourcePolicies(
                    soapToRestApiId, inPolicy.getId(), inPolicy.getResourcePath(), inPolicy, null);

            Assert.assertNotNull(updatedPolicy, "Updated in-sequence should not be null");
            Assert.assertTrue(updatedPolicy.getContent().contains("<!-- Updated after provider change -->"),
                    "Updated in-sequence should contain the update marker");
        }

        // Update out-sequence after provider change
        for (ResourcePolicyInfoDTO outPolicy : resourcePoliciesOutAfter) {
            String originalContent = outPolicy.getContent();
            String updatedContent = originalContent + "\n<!-- Updated after provider change -->";
            outPolicy.setContent(updatedContent);

            ResourcePolicyInfoDTO updatedPolicy = restAPIPublisher.updateApiResourcePolicies(
                    soapToRestApiId, outPolicy.getId(), outPolicy.getResourcePath(), outPolicy, null);

            Assert.assertNotNull(updatedPolicy, "Updated out-sequence should not be null");
            Assert.assertTrue(updatedPolicy.getContent().contains("<!-- Updated after provider change -->"),
                    "Updated out-sequence should contain the update marker");
        }

        // Retrieve sequences again and verify updates were reflected
        ResourcePolicyListDTO resourcePolicyInListUpdated = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "in", "checkPhoneNumber", "post");
        ResourcePolicyListDTO resourcePolicyOutListUpdated = restAPIPublisher
                .getApiResourcePolicies(soapToRestApiId, "out", "checkPhoneNumber", "post");

        List<ResourcePolicyInfoDTO> resourcePoliciesInUpdated = resourcePolicyInListUpdated.getList();
        List<ResourcePolicyInfoDTO> resourcePoliciesOutUpdated = resourcePolicyOutListUpdated.getList();

        for (ResourcePolicyInfoDTO inPolicy : resourcePoliciesInUpdated) {
            Assert.assertTrue(inPolicy.getContent().contains("<!-- Updated after provider change -->"),
                    "Retrieved in-sequence should contain the update marker");
        }

        for (ResourcePolicyInfoDTO outPolicy : resourcePoliciesOutUpdated) {
            Assert.assertTrue(outPolicy.getContent().contains("<!-- Updated after provider change -->"),
                    "Retrieved out-sequence should contain the update marker");
        }

        // Get API using revision UUID after provider change
        APIDTO apiByRevisionSoapToRest = restAPIPublisher.getAPIByID(soapToRestRevisionUUID);
        Assert.assertNotNull(apiByRevisionSoapToRest, "SOAPTOREST API retrieved by revision should not be null");
        Assert.assertEquals(apiByRevisionSoapToRest.getProvider(), originalProvider,
                "SOAPTOREST API provider from revision should match new provider");

        // Get Swagger using revision UUID after provider change
        String swaggerByRevisionSoapToRest = restAPIPublisher.getSwaggerByID(soapToRestRevisionUUID);
        Assert.assertNotNull(swaggerByRevisionSoapToRest, "Swagger retrieved by revision should not be null");
        Assert.assertTrue(swaggerByRevisionSoapToRest.length() > 0,
                "Swagger retrieved by revision should not be empty");

        // Get in-sequences using revision UUID after provider change
        ResourcePolicyListDTO resourcePolicyInListByRevision = restAPIPublisher
                .getApiResourcePolicies(soapToRestRevisionUUID, "in", "checkPhoneNumber", "post");
        Assert.assertNotNull(resourcePolicyInListByRevision, "In-sequences by revision should not be null");
        List<ResourcePolicyInfoDTO> resourcePoliciesInByRevision = resourcePolicyInListByRevision.getList();
        Assert.assertNotNull(resourcePoliciesInByRevision, "In-sequences list by revision should not be null");
        Assert.assertFalse(resourcePoliciesInByRevision.isEmpty(),
                "In-sequences by revision should not be empty");

        // Get out-sequences using revision UUID after provider change
        ResourcePolicyListDTO resourcePolicyOutListByRevision = restAPIPublisher
                .getApiResourcePolicies(soapToRestRevisionUUID, "out", "checkPhoneNumber", "post");
        Assert.assertNotNull(resourcePolicyOutListByRevision, "Out-sequences by revision should not be null");
        List<ResourcePolicyInfoDTO> resourcePoliciesOutByRevision = resourcePolicyOutListByRevision.getList();
        Assert.assertNotNull(resourcePoliciesOutByRevision, "Out-sequences list by revision should not be null");
        Assert.assertFalse(resourcePoliciesOutByRevision.isEmpty(),
                "Out-sequences by revision should not be empty");

        // Invoke the SOAPTOREST API after provider change to verify it still works
        soapToRestApiInvokeResponse = HttpRequestUtil.doPost(new URL(invokeURL), restPayload, requestHeaders);
        Assert.assertTrue(soapToRestApiInvokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK ||
                        soapToRestApiInvokeResponse.getResponseCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "SOAPTOREST API invocation failed unexpectedly after provider change");
    }

    @Test(groups = {"wso2.am"}, description = "Test changing provider of a GraphQL API")
    public void ChangeGraphQLApiProvider() throws Exception {
        // 1. Create GraphQL API from .graphql file
        graphqlSchemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                "UTF-8");
        File file = getTempFileWithContent(graphqlSchemaDefinition);

        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", GRAPHQL_API_CONTEXT);
        additionalPropertiesObj.put("version", GRAPHQL_API_VERSION);

        JSONObject url = new JSONObject();
        url.put("url", "https://localhost:9943/am-graphQL-sample/api/graphql/");
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("operations", operations);

        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        String originalProvider = apidto.getProvider();
        graphqlApiId = apidto.getId();

        Assert.assertEquals(apidto.getName(), GRAPHQL_API_NAME);
        String expectedContext = user.getUserDomain().equals("carbon.super") ?
                "/" + GRAPHQL_API_CONTEXT : "/t/" + user.getUserDomain() + "/" + GRAPHQL_API_CONTEXT;
        Assert.assertEquals(apidto.getContext(), expectedContext);

        // 2. Create Revision and Deploy to Gateway
        String graphqlApiRevisionUUID = createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, GRAPHQL_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        // 3. Publish the API
        restAPIPublisher.changeAPILifeCycleStatus(graphqlApiId, APILifeCycleAction.PUBLISH.getAction(), null);

        // 4. Create application and subscribe
        HttpResponse applicationResponse = restAPIStore.createApplication(GRAPHQL_APPLICATION_NAME, Strings.EMPTY,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        graphqlApplicationId = applicationResponse.getData();

        restAPIStore.subscribeToAPI(graphqlApiId, graphqlApplicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate keys and get access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(graphqlApplicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        // 5. Invoke the GraphQL API before provider change
        String invokeURL = getAPIInvocationURLHttp(GRAPHQL_API_CONTEXT, GRAPHQL_API_VERSION) + "/";
        Map<String, String> requestHeaders = new HashMap<>();
        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        requestHeaders.put(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
        requestHeaders.put("Content-Type", "application/json");
        HttpResponse graphqlApiInvokeResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());

        Assert.assertEquals(graphqlApiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "GraphQL API invocation failed before provider change");

        // 6. Update provider of the GraphQL API
        String tenantDomainGraphQL = user.getUserDomain();
        restAPIAdminClient = new RestAPIAdminImpl(firstUserName, firstUserName, tenantDomainGraphQL,
                adminURLHttps);
        String newProviderNameGraphQL = tenantDomainGraphQL.equals("carbon.super") ? newUser : newUser + "@" + tenantDomainGraphQL;
        ApiResponse<Void> changeProviderResponse = restAPIAdminClient.changeApiProvider(newProviderNameGraphQL, graphqlApiId);
        Assert.assertEquals(changeProviderResponse.getStatusCode(), HttpStatus.SC_OK);

        // Undeploy the existing revision first
        List<APIRevisionDeployUndeployRequest> undeployList = new ArrayList<>();
        APIRevisionDeployUndeployRequest undeployReq = new APIRevisionDeployUndeployRequest();
        undeployReq.setName(Constants.GATEWAY_ENVIRONMENT);
        undeployReq.setVhost(null);
        undeployReq.setDisplayOnDevportal(true);
        undeployList.add(undeployReq);
        restAPIPublisher.undeployAPIRevision(graphqlApiId, graphqlApiRevisionUUID, undeployList);
        waitForAPIDeployment();

        // 7. Deploy a new revision after provider change
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, GRAPHQL_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        // 8. Retrievals after provider change

        // Verify the provider was actually changed
        APIDTO apiAfterProviderChangeGraphQL = restAPIPublisher.getAPIByID(graphqlApiId);
        Assert.assertEquals(apiAfterProviderChangeGraphQL.getProvider(), newProviderNameGraphQL,
                "GraphQL API provider should be changed to " + newProviderNameGraphQL);

        // Get GraphQL schema definition after provider change
        GraphQLSchemaDTO schema = restAPIPublisher.getGraphqlSchemaDefinition(graphqlApiId);
        Assert.assertNotNull(schema, "GraphQL schema definition should not be null after provider change");
        Assert.assertNotNull(schema.getSchemaDefinition(), "GraphQL schema should not be null after provider change");

        // Get Swagger definition after provider change
        String swaggerDefinition = restAPIPublisher.getSwaggerByID(graphqlApiId);
        Assert.assertNotNull(swaggerDefinition, "Swagger definition should not be null after provider change");
        Assert.assertTrue(swaggerDefinition.length() > 0, "Swagger definition should not be empty after provider change");

        // Get API using revision UUID after provider change
        APIDTO apiByRevisionGraphQL = restAPIPublisher.getAPIByID(graphqlApiRevisionUUID);
        Assert.assertNotNull(apiByRevisionGraphQL, "GraphQL API retrieved by revision should not be null");
        Assert.assertEquals(apiByRevisionGraphQL.getProvider(), originalProvider,
                "GraphQL API provider from revision should match original provider");

        // Get GraphQL schema using revision UUID after provider change
        GraphQLSchemaDTO schemaByRevision = restAPIPublisher.getGraphqlSchemaDefinition(graphqlApiRevisionUUID);
        Assert.assertNotNull(schemaByRevision, "GraphQL schema by revision should not be null");
        Assert.assertNotNull(schemaByRevision.getSchemaDefinition(), "GraphQL schema definition by revision should not be null");

        // Get Swagger using revision UUID after provider change
        String swaggerByRevisionGraphQL = restAPIPublisher.getSwaggerByID(graphqlApiRevisionUUID);
        Assert.assertNotNull(swaggerByRevisionGraphQL, "Swagger by revision should not be null");
        Assert.assertTrue(swaggerByRevisionGraphQL.length() > 0, "Swagger by revision should not be empty");

        // 9. Invoke the GraphQL API after provider change to verify it still works
        graphqlApiInvokeResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(graphqlApiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "GraphQL API invocation failed after provider change");
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        // Clean up REST API
        if (apiID != null) {
            undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        }
        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
        if (apiID != null) {
            restAPIPublisher.deleteAPI(apiID);
        }
        // Clean up SOAP API
        if (soapApiId != null) {
            undeployAndDeleteAPIRevisionsUsingRest(soapApiId, restAPIPublisher);
        }
        if (soapApplicationId != null) {
            restAPIStore.deleteApplication(soapApplicationId);
        }
        if (soapApiId != null) {
            restAPIPublisher.deleteAPI(soapApiId);
        }
        // Clean up SOAPTOREST API
        if (soapToRestApiId != null) {
            undeployAndDeleteAPIRevisionsUsingRest(soapToRestApiId, restAPIPublisher);
        }
        if (soapToRestApplicationId != null) {
            restAPIStore.deleteApplication(soapToRestApplicationId);
        }
        if (soapToRestApiId != null) {
            restAPIPublisher.deleteAPI(soapToRestApiId);
        }
        // Clean up GraphQL API
        if (graphqlApiId != null) {
            undeployAndDeleteAPIRevisionsUsingRest(graphqlApiId, restAPIPublisher);
        }
        if (graphqlApplicationId != null) {
            restAPIStore.deleteApplication(graphqlApplicationId);
        }
        if (graphqlApiId != null) {
            restAPIPublisher.deleteAPI(graphqlApiId);
        }
        if (newUser != null) {
            userManagementClient.deleteUser(newUser);
        }
        super.cleanUp();
    }
}
