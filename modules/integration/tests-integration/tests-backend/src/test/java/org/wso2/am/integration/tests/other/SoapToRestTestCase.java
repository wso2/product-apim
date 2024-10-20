/*
 *
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ResourcePolicyInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ResourcePolicyListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertNotEquals;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class SoapToRestTestCase extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(SoapToRestTestCase.class);

    private final String SOAPTOREST_API_NAME = "PhoneVerification";
    private final String API_CONTEXT = "phoneverify";
    private final String API_VERSION_1_0_0 = "1.0";
    private static final String SOAPTOREST_TEST_USER = "soaptorestuser";
    private static final String SOAPTOREST_TEST_USER_PASSWORD = "soaptorestuser";
    private static final String SOAPTOREST_ROLE = "soaptorestrole";

    private String endpointHost = "http://localhost";
    private int endpointPort;
    private int lowerPortLimit = 9950;
    private int upperPortLimit = 9999;
    private String wsdlDefinition;
    private String soapToRestAPIId;
    private WireMockServer wireMockServer;
    private String apiEndPointURL;
    private String wsdlURL;
    private String testAppId1;
    private String testAppId2;
    private String testAppId3;
    private String testAppId4;
    private String testAppId5;
    private String testAppId6;
    private String payload = "{\n" + "   \"CheckPhoneNumber\":{\n" + "      \"PhoneNumber\":\"18006785432\",\n"
            + "      \"LicenseKey\":\"0\"\n" + "   }\n" + "}";
    private String resourceName = "/checkPhoneNumber";
    private String responseBody;
    private APIDTO apidto;
    private List<ResourcePolicyInfoDTO> resourcePoliciesIn;
    private List<ResourcePolicyInfoDTO> resourcePoliciesOut;

    @Factory(dataProvider = "userModeDataProvider")
    public SoapToRestTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        userManagementClient.addUser(SOAPTOREST_TEST_USER, SOAPTOREST_TEST_USER_PASSWORD, new String[]{}, null);
        userManagementClient.addRole(SOAPTOREST_ROLE, new String[]{SOAPTOREST_TEST_USER}, new String[]{});
        wsdlDefinition = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator
                + "phoneverify.wsdl");
        responseBody = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator
                + "checkPhoneNumberResponseBody.xml");

        // Start wiremock server
        startWiremockServer();
        apiEndPointURL = endpointHost + ":" + endpointPort + "/phoneverify";
        wsdlURL = endpointHost + ":" + endpointPort + "/phoneverify/wsdl";

        File file = getTempFileWithContent(wsdlDefinition);
        restAPIPublisher.validateWsdlDefinition(null, file);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", SOAPTOREST_API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject endpointObject = new JSONObject();
        endpointObject.put("type", "address");
        endpointObject.put("url", apiEndPointURL);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "address");
        endpointConfig.put("sandbox_endpoints", endpointObject);
        endpointConfig.put("production_endpoints", endpointObject);

        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);

        // Create SOAPTOREST API
        apidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, null, additionalPropertiesObj.toString(), "SOAPTOREST");
        soapToRestAPIId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(soapToRestAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                SOAPTOREST_API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        // Publish API
        restAPIPublisher.changeAPILifeCycleStatus(soapToRestAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = { "wso2.com" }, description = "Created API resources validation test case")
    public void testValidateCreatedResources()
            throws Exception {

        String[] expectedResources = {"/checkPhoneNumbers", "/checkPhoneNumber"};

        HttpResponse response = restAPIPublisher.getAPI(soapToRestAPIId);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        List<APIOperationsDTO> operations = apidto.getOperations();
        String[] actualResources = new String[operations.size()];
        int index;
        for (index = 0; index < operations.size(); index++) {
            String resource = operations.get(index).getTarget();
            actualResources[index] = resource;
        }

        // Check the number of resources
        assertEquals(actualResources.length, expectedResources.length,
                "Unexpected number of resources in the created API");

        // Check all resources validity
        assertEqualsNoOrder(actualResources, expectedResources, "Invalid set of resources");
    }

    @Test(groups = "wso2.am", description = "In/Out sequence validation test case", dependsOnMethods = {
            "testValidateCreatedResources" })
    public void testValidateInOutSequence()
            throws Exception {

        // Validate in-sequence
        String inSequence = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                "artifacts" + File.separator + "AM" + File.separator + "soap" + File.separator
                        + "in-sequence-check-phone-numbers.xml"), "UTF-8");
        String inSequenceRandomOrderedElement = removeSpacesInSequence(inSequence);
        ResourcePolicyListDTO resourcePolicyInListDTO = restAPIPublisher
                .getApiResourcePolicies(soapToRestAPIId, "in", "checkPhoneNumbers", "post");
        resourcePoliciesIn = resourcePolicyInListDTO.getList();
        resourcePoliciesIn.forEach((item) -> {
                try {
                        String itemElement = removeSpacesInSequence(item.getContent());
                        log.info("Generated In sequence: " + item.getContent());
                        boolean equals = inSequenceRandomOrderedElement.equals(itemElement);
                        assertEquals(equals, true, "Invalid In-Sequence");
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
        });

        // Validate out-sequence
        String outSequence = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                "artifacts" + File.separator + "AM" + File.separator + "soap" + File.separator
                        + "out-sequence-check-phone-numbers.xml"), "UTF-8");
        String outSequenceElement = removeSpacesInSequence(outSequence);
        ResourcePolicyListDTO resourcePolicyOutListDTO = restAPIPublisher
                .getApiResourcePolicies(soapToRestAPIId, "out", "checkPhoneNumbers", "post");
        resourcePoliciesOut = resourcePolicyOutListDTO.getList();
        resourcePoliciesOut.forEach((item) -> {
                try {
                        String itemElement = removeSpacesInSequence(item.getContent());
                        log.info("Generated out sequence: " + item.getContent());
                        assertEquals(itemElement, outSequenceElement, "Invalid Out-Sequence");
                } catch (Exception e) {
                        log.error(e.getMessage());
                }
        });
    }

    @Test(groups = {"wso2.am"}, description = "Invocation of default API",
            dependsOnMethods = {"testValidateCreatedResources"})
    public void testDefaultAPIInvocation() throws Exception {

        apidto.setIsDefaultVersion(true);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        createAPIRevisionAndDeployUsingRest(updatedAPI.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);

        String soapToRestAppName = "PhoneVerificationDefaultApp";
        testAppId1 = createSoapToRestAppAndSubscribeToAPI(soapToRestAppName, "OAUTH", soapToRestAPIId);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(testAppId1, "36000", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                        grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "Invocation of a revisioned and deployed API",
            dependsOnMethods = {"testValidateCreatedResources"})
    public void testRevisionedAPIInvocation() throws Exception {
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);

        String soapToRestAppName = "PhoneVerificationRevisionedApp";
        testAppId2 = createSoapToRestAppAndSubscribeToAPI(soapToRestAppName, "OAUTH", soapToRestAPIId);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(testAppId2, "36000", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                        grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "API invocation using JWT App")
    public void testInvokeSoapToRestAPIUsingJWTApplication() throws Exception {
        String graphqlJwtAppName = "PhoneVerificationJWTApp";
        testAppId3 = createSoapToRestAppAndSubscribeToAPI(graphqlJwtAppName, "JWT", soapToRestAPIId);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId3, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "API invocation using oauth App")
    public void testInvokeSoapToRestAPIUsingOAuthApplication() throws Exception {
        String soapToRestOAUTHAppName = "PhoneVerificationOauthApp";
        testAppId4 = createSoapToRestAppAndSubscribeToAPI(soapToRestOAUTHAppName, "OAUTH", soapToRestAPIId);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId4, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    @Test(groups = { "wso2.am" }, description = "Oauth Scopes", dependsOnMethods = {
            "testInvokeSoapToRestAPIUsingOAuthApplication",
            "testInvokeSoapToRestAPIUsingJWTApplication" })
    public void testOperationalLevelOAuthScopesForSoapToRest()
            throws Exception {
        ArrayList role = new ArrayList();
        role.add(SOAPTOREST_ROLE);

        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName("subscriber");
        scopeObject.setBindings(role);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(scopeObject);

        ArrayList apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeDTO);

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(soapToRestAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(apiScopeList);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);

        ArrayList scope = new ArrayList();
        scope.add("subscriber");
        List<APIOperationsDTO> operations = updatedAPI.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals(resourceName)) {
                        item.setScopes(scope);
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        undeployAndDeleteAPIRevisionsUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);

        testAppId5 = createSoapToRestAppAndSubscribeToAPI("testOperationalLevelOAuthScopesForSoapToRest", "OAUTH",
                soapToRestAPIId);
        // Keep sufficient time to update map
        Thread.sleep(10000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/json");

        // Invoke api without authorized scope
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId5, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        log.info("Access Token response without scope: " + accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_FORBIDDEN,
                "Response code is not as expected");

        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;
        String username = SOAPTOREST_TEST_USER;
        // Obtain user access token for Admin
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            username = username.concat("@").concat(user.getUserDomain());
        }
        requestBody =
                "grant_type=password&username=" + username + "&password=" + SOAPTOREST_TEST_USER_PASSWORD +
                        "&scope=subscriber";

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        log.info("Access Token response with scope: " + response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");
        tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "Oauth Scopes", dependsOnMethods = {
            "testOperationalLevelOAuthScopesForSoapToRest"})
    public void testOperationalLevelSecurityForSoapToRest() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(soapToRestAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals(resourceName)) {
                        item.setAuthType("None");
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        undeployAndDeleteAPIRevisionsUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/json");

        // Invoke api without security
        String accessToken = "";
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    @Test(groups = "wso2.am", description = "In/Out sequence validation test case", dependsOnMethods = {
            "testValidateInOutSequence", "testOperationalLevelSecurityForSoapToRest" })
    public void testUpdateInOutSequence() throws Exception {

        // Update in-sequence
        String updatedInSequence = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                "artifacts" + File.separator + "AM" + File.separator + "soap" + File.separator
                        + "updated-in-sequence-check-phone-numbers.xml"), "UTF-8");
        String inSequenceRandomOrderedElement = removeSpacesInSequence(updatedInSequence);
        resourcePoliciesIn.forEach((item) -> {
            ResourcePolicyInfoDTO updatedResourcePoliciesIn = null;
            item.setContent(updatedInSequence);
            try {
                updatedResourcePoliciesIn = restAPIPublisher
                        .updateApiResourcePolicies(soapToRestAPIId, item.getId(), item.getResourcePath(), item, null);
            } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
                log.error(e.getMessage());
            }
            try {
                String itemElement = removeSpacesInSequence(updatedResourcePoliciesIn.getContent());
                log.info("Generated updated in sequence: " + updatedResourcePoliciesIn.getContent());
                boolean equals = inSequenceRandomOrderedElement.equals(itemElement);
                assertEquals(equals, true, "In-Sequence not updated");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });

        // Update out-sequence
        String updatedOutSequence = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                "artifacts" + File.separator + "AM" + File.separator + "soap" + File.separator
                        + "updated-out-sequence-check-phone-numbers.xml"), "UTF-8");
        String updatedOutSequenceElement = removeSpacesInSequence(updatedOutSequence);
        resourcePoliciesOut.forEach((item) -> {
                item.setContent(updatedOutSequence);
                ResourcePolicyInfoDTO updatedResourcePoliciesOut = null;
                try {
                        updatedResourcePoliciesOut = restAPIPublisher
                                .updateApiResourcePolicies(soapToRestAPIId, item.getId(), item.getResourcePath(), item, null);
                        log.info("Generated updated out sequence: " + updatedResourcePoliciesOut.getContent());
                        String updatedResourcePoliciesOutElement = removeSpacesInSequence(updatedResourcePoliciesOut.getContent());
                        assertEquals(updatedResourcePoliciesOutElement, updatedOutSequenceElement, "Out-Sequence not updated");
                } catch (Exception e) {
                        log.error(e.getMessage());
                }
        });
    }

    // Test if the response is 400 when the content-type of the request is not application/json
    @Test(groups = {"wso2.am"}, description = "Invocation of a API with invalid content-type", dependsOnMethods = {
            "testUpdateInOutSequence" })
    public void testDefaultAPIInvocationWithInvalidContentType() throws Exception {

        String soapToRestAppName = "PhoneVerificationAppInvalidContentType";
        testAppId6 = createSoapToRestAppAndSubscribeToAPI(soapToRestAppName, "OAUTH", soapToRestAPIId);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(testAppId6, "36000", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                        grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "text/plain");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_BAD_REQUEST, "Response code is not as expected");
    }

    private void startWiremockServer() {
        endpointPort = getAvailablePort();
        assertNotEquals(endpointPort, -1, "No available port in the range " + lowerPortLimit + "-" +
                upperPortLimit + " was found");
        wireMockServer = new WireMockServer(options().port(endpointPort));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/phoneverify/wsdl")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/xml").withBody(wsdlDefinition)));
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/phoneverify")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/xml").withBody(responseBody)));
        wireMockServer.start();
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @return Available Port Number
     */
    private int getAvailablePort() {
        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit++;
        }
        return -1;
    }

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {
        Socket s = null;
        try {
            s = new Socket(endpointHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    private String createSoapToRestAppAndSubscribeToAPI(String appName, String tokenType, String apiId) throws ApiException, APIManagerIntegrationTestException {
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(appName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                "test app for SOAPTOREST API", tokenType);
        String testApiId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(apiId, testApiId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        return testApiId;
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("phoneverify", ".wsdl");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    public static String removeSpacesInSequence(String xml) throws Exception {
        String closedXml = "<root>" + xml + "</root>";
        InputStream stream = new ByteArrayInputStream(closedXml.getBytes());
        XMLStreamReader parser;
        StAXOMBuilder builder;
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            parser = factory.createXMLStreamReader(stream);
            XMLStreamReader filteredReader = factory.createFilteredReader(parser, new StreamFilter() {
                public boolean accept(XMLStreamReader r) {
                    return !r.isWhiteSpace();
                }
            });
            builder = new StAXOMBuilder(filteredReader);
        } catch (XMLStreamException e) {
            String msg = "Error in initializing the parser.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        return builder.getDocumentElement().toString().replace(" ","");
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        userManagementClient.deleteRole(SOAPTOREST_ROLE);
        userManagementClient.deleteUser(SOAPTOREST_TEST_USER);
        restAPIStore.deleteApplication(testAppId1);
        restAPIStore.deleteApplication(testAppId2);
        restAPIStore.deleteApplication(testAppId3);
        restAPIStore.deleteApplication(testAppId4);
        restAPIStore.deleteApplication(testAppId5);
        restAPIStore.deleteApplication(testAppId6);
        undeployAndDeleteAPIRevisionsUsingRest(soapToRestAPIId, restAPIPublisher);
        restAPIPublisher.deleteAPI(soapToRestAPIId);
        wireMockServer.stop();
    }
}

