/*
 *Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import org.apache.axis2.AxisFault;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;

public class AllowedScopesTestWithCorsDisabled extends APIManagerLifecycleBaseTest {
    private static final String EXAMPLE_API_CONTEXT = "exampleapi";
    private static final String PRODUCTS_CATALOG_1_METHOD = "/products/catalog/1";
    private static final String PRODUCTS_POPULAR_METHOD = "/products/popular";
    private static final String PRODUCTS_WILDCARD = "/products/noexactmatch";
    private static final String ORDERS = "/orders";
    private static final String WILDCARD = "/noexactmatch";

    private ServerConfigurationManager serverConfigurationManager;
    private String apiImportId;
    private String applicationImportId;

    @Factory(dataProvider = "userModeDataProvider")
    public AllowedScopesTestWithCorsDisabled(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init(userMode);
        initializeServerConfiguration();
        initializeUserManagementClient();
        importApiDefinitionAndDeploy();
    }

    @Test(description = "Generate access token for white listed scopes and invoke APIs")
    public void testGenerateAccessTokenAndInvokeExampleAPIForScopesValidation() throws Exception {
        // Create application
        HttpResponse applicationResponse = restAPIStore.createApplication("TestAppScopeExampleAPI",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationImportId = applicationResponse.getData();

        // Subscribe to API
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(apiImportId, applicationImportId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of import API version request not successful " +
                        " API Name: ExampleAPI API Version: 1_0_0 API Provider Name :" + user.getUserName());

        // Generate Keys
        ApplicationKeyDTO applicationKeyDTO = generateKeysForApplication(applicationImportId);

        // Get Consumer Key and Consumer Secret
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();

        // Generate token for Scope A
        Map<String, String> requestHeadersWithScopeA = generateAccessTokenHeaderForScope(
                "ScopeA", consumerKey, consumerSecret);

        // Invoke API using token of Scope A
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_CATALOG_1_METHOD, requestHeadersWithScopeA,
                HttpStatus.SC_OK);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_POPULAR_METHOD, requestHeadersWithScopeA,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_WILDCARD, requestHeadersWithScopeA,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, ORDERS, requestHeadersWithScopeA, HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, WILDCARD, requestHeadersWithScopeA, HttpStatus.SC_FORBIDDEN);

        // Generate token for Scope B
        Map<String, String> requestHeadersWithScopeB = generateAccessTokenHeaderForScope(
                "ScopeB", consumerKey, consumerSecret);

        // Invoke API using token of Scope B
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_CATALOG_1_METHOD, requestHeadersWithScopeB,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_POPULAR_METHOD, requestHeadersWithScopeB,
                HttpStatus.SC_OK);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_WILDCARD, requestHeadersWithScopeB,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, ORDERS, requestHeadersWithScopeB, HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, WILDCARD, requestHeadersWithScopeB, HttpStatus.SC_FORBIDDEN);

        // Generate token for Scope C
        Map<String, String> requestHeadersWithScopeC = generateAccessTokenHeaderForScope(
                "ScopeC", consumerKey, consumerSecret);

        // Invoke API using token of Scope C
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_CATALOG_1_METHOD, requestHeadersWithScopeC,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_POPULAR_METHOD, requestHeadersWithScopeC,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_WILDCARD, requestHeadersWithScopeC,
                HttpStatus.SC_OK);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, ORDERS, requestHeadersWithScopeC, HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, WILDCARD, requestHeadersWithScopeC, HttpStatus.SC_FORBIDDEN);

        // Generate token for Scope D
        Map<String, String> requestHeadersWithScopeD = generateAccessTokenHeaderForScope(
                "ScopeD", consumerKey, consumerSecret);

        // Invoke API using token of Scope D
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_CATALOG_1_METHOD, requestHeadersWithScopeD,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_POPULAR_METHOD, requestHeadersWithScopeD,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_WILDCARD, requestHeadersWithScopeD,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, ORDERS, requestHeadersWithScopeD, HttpStatus.SC_OK);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, WILDCARD, requestHeadersWithScopeD, HttpStatus.SC_FORBIDDEN);

        // Generate token for Scope E
        Map<String, String> requestHeadersWithScopeE = generateAccessTokenHeaderForScope(
                "ScopeE", consumerKey, consumerSecret);

        // Invoke API using token of Scope E
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_CATALOG_1_METHOD, requestHeadersWithScopeE,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_POPULAR_METHOD, requestHeadersWithScopeE,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, PRODUCTS_WILDCARD, requestHeadersWithScopeE,
                HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, ORDERS, requestHeadersWithScopeE, HttpStatus.SC_FORBIDDEN);
        invokeAPI(EXAMPLE_API_CONTEXT, API_VERSION_1_0_0, WILDCARD, requestHeadersWithScopeE, HttpStatus.SC_OK);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationImportId);
        restAPIPublisher.deleteAPI(apiImportId);
        serverConfigurationManager.restoreToLastConfiguration();
        super.cleanUp();
    }

    private void initializeServerConfiguration() throws Exception {
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        File configFile = new File(getAMResourceLocation() +
                "/configFiles/allowedScopesWithCorsDisabled/deployment.toml");
        serverConfigurationManager.applyConfiguration(configFile);
    }

    private void initializeUserManagementClient() throws AxisFault, XPathExpressionException {
        String backEndUrl = keyManagerContext.getContextUrls().getBackEndUrl();
        String username = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        String password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        userManagementClient = new UserManagementClient(backEndUrl, username, password);
    }

    private void importApiDefinitionAndDeploy() throws Exception {
        String context = determineApiContext();
        JSONObject additionalProperties = loadApiAdditionalProperties(context);

        File definitionFile = getTempFileWithContent(loadApiDefinition());
        APIDTO apidtoOAS = restAPIPublisher.importOASDefinition(definitionFile, additionalProperties.toString());
        apiImportId = apidtoOAS.getId();

        createAPIRevisionAndDeployUsingRest(apiImportId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiImportId, false);
        waitForAPIDeploymentSync(apidtoOAS.getProvider(), apidtoOAS.getName(), apidtoOAS.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apidtoOAS.getProvider(), apidtoOAS.getName(), apidtoOAS.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    private String determineApiContext() {
        String context = EXAMPLE_API_CONTEXT;
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            context = "/t/" + user.getUserDomain() + context;
        }
        return context;
    }

    private JSONObject loadApiAdditionalProperties(String context) throws IOException, JSONException {

        String resourcePath = "oas/v3/scope-validation";
        String additionalPropertiesJson =
                IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourcePath +
                        "/additionalProperties.json"), "UTF-8");
        JSONObject additionalPropertiesObj = new JSONObject(additionalPropertiesJson);
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("context", context);
        return additionalPropertiesObj;
    }

    private String loadApiDefinition() throws IOException {

        String resourcePath = "oas/v3/scope-validation";
        return IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream(resourcePath + "/oas_import.json"), "UTF-8");
    }

    private ApplicationKeyDTO generateKeysForApplication(String appId) throws Exception {
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        return restAPIStore.generateKeys(appId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
    }

    public Map<String, String> generateAccessTokenHeaderForScope(String scopeName, String consumerKey, String consumerSecret)
            throws Exception {
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String requestBody = "grant_type=password&username=" + user.getUserName() +
                "&password=" + user.getPassword() + "&scope=" + scopeName;
        JSONObject accessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(
                consumerKey, consumerSecret, requestBody, tokenEndpointURL).getData());

        // Validate access token
        Assert.assertNotNull(accessTokenGenerationResponse);
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains(scopeName));
        Assert.assertTrue(accessTokenGenerationResponse.getString("expires_in").equals("3600"));

        String accessTokenWithScope = accessTokenGenerationResponse.getString("access_token");

        Map<String, String> requestHeadersWithScope = new HashMap<String, String>();
        requestHeadersWithScope.put("Authorization", "Bearer " + accessTokenWithScope);
        requestHeadersWithScope.put("accept", "*/*");
        return requestHeadersWithScope;
    }

    public void invokeAPI(String apiContext, String apiVersion, String method,
                          Map<String, String> requestHeaders, int expectedResponseCode) throws Exception {
        // Invoke API
        HttpResponse apiResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(apiContext, apiVersion) + method, requestHeaders);

        // Validate response
        assertEquals(apiResponse.getResponseCode(), expectedResponseCode,
                "API invocation failed for " + method + " with expected status code " + expectedResponseCode);

    }

    private File getTempFileWithContent(String swagger) throws Exception {
        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(swagger);
        out.close();
        return temp;
    }
}
