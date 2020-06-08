/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.thirdparty;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.ClaimMappingEntryDTO;
import org.wso2.am.integration.clients.admin.api.dto.ErrorDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerConfigurationDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.SettingsDTO;
import org.wso2.am.integration.clients.admin.api.dto.SettingsKeyManagerConfigurationDTO;
import org.wso2.am.integration.clients.admin.api.dto.TokenValidationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerApplicationConfigurationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;

public class ThirdPartyKeyManagerRegistrationTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ThirdPartyKeyManagerRegistrationTestCase.class);
    private String keyManagerId, keymanager2Id;
    private String keymanager3Id;
    private String apiEndPointUrl;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiId1;
    private String jwtAppId;
    String oauthAppId;
    ApplicationKeyDTO defaultKeyManagerApplicationKey;
    private ApplicationKeyDTO keyManager1ApplicationKey;
    private ApplicationKeyDTO keyManager1ApplicationKey2;
    private ApplicationKeyDTO oauthApplicationKey;
    private String context = "thirdpartykm";
    private final String API_END_POINT_METHOD = "/customers/123";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        createAndPublishAPI();
        ApplicationDTO applicationDTO =
                restAPIStore.addApplicationWithTokenType("thirdpartykmapp", TIER_UNLIMITED, "","","JWT");
        jwtAppId = applicationDTO.getApplicationId();
        applicationDTO =
                restAPIStore.addApplicationWithTokenType("thirdpartykmapp2", TIER_UNLIMITED, "","", "OAUTH");
        oauthAppId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(apiId1, jwtAppId, TIER_UNLIMITED);
        restAPIStore.subscribeToAPI(apiId1, oauthAppId, TIER_UNLIMITED);
    }

    @Test(groups = {"wso2.am"}, description = "Check settings retrieved From Key Manager")
    public void testGetKeyManagerSettings() throws Exception {

        SettingsDTO settings = restAPIAdmin.getSettings();
        Assert.assertNotNull(settings.getKeyManagerConfiguration());
        Assert.assertEquals(settings.getKeyManagerConfiguration().size(), 1);
        SettingsKeyManagerConfigurationDTO settingsKeyManagerConfigurationDTO =
                settings.getKeyManagerConfiguration().get(0);
        Assert.assertEquals(settingsKeyManagerConfigurationDTO.getType(), "okta");
        List<KeyManagerConfigurationDTO> configurations = settingsKeyManagerConfigurationDTO.getConfigurations();
        for (KeyManagerConfigurationDTO configuration : configurations) {
            if ("apiKey".equals(configuration.getName())) {
                Assert.assertEquals(configuration.getLabel(), "API KEY");
                Assert.assertEquals(configuration.getTooltip(), "API Key Generated From Okta UI");
                Assert.assertEquals(configuration.getType(), "input");
                Assert.assertFalse(configuration.isMultiple());
                Assert.assertTrue(configuration.isRequired());
                Assert.assertTrue(configuration.isMask());
                Assert.assertEquals(configuration.getDefault(), "");
                Assert.assertEquals(configuration.getValues().size(), 0);
            } else if ("client_id".equals(configuration.getName())) {
                Assert.assertEquals(configuration.getLabel(), "Client ID");
                Assert.assertEquals(configuration.getTooltip(), "Client ID of service Application");
                Assert.assertEquals(configuration.getType(), "input");
                Assert.assertFalse(configuration.isMultiple());
                Assert.assertTrue(configuration.isRequired());
                Assert.assertFalse(configuration.isMask());
                Assert.assertEquals(configuration.getDefault(), "");
                Assert.assertEquals(configuration.getValues().size(), 0);
            } else if ("client_secret".equals(configuration.getName())) {
                Assert.assertEquals(configuration.getLabel(), "Client Secret");
                Assert.assertEquals(configuration.getTooltip(), "Client Secret of service Application");
                Assert.assertEquals(configuration.getType(), "input");
                Assert.assertFalse(configuration.isMultiple());
                Assert.assertTrue(configuration.isRequired());
                Assert.assertTrue(configuration.isMask());
                Assert.assertEquals(configuration.getDefault(), "");
                Assert.assertEquals(configuration.getValues().size(), 0);
            } else {
                Assert.fail("None of the Required configurations not exist");
            }
        }
    }

    @Test(groups = {
            "wso2.am"}, description = "Create Key Manager from Defined Key Manager", dependsOnMethods =
            "testGetKeyManagerSettings")
    public void testCreateKeyManager() throws Exception {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("okta");
        keyManagerDTO.setName("Key Manager 1");
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setClientRegistrationEndpoint("https://dev-876785.okta.com/oauth2/v1/clients");
        keyManagerDTO.setIntrospectionEndpoint("https://dev-876785.okta.com/oauth2/default/v1/introspect");
        keyManagerDTO.setTokenEndpoint("https://dev-876785.okta.com/oauth2/default/v1/token");
        keyManagerDTO.setRevokeEndpoint("https://dev-876785.okta.com/oauth2/default/v1/revoke");
        keyManagerDTO.setIssuer("https://dev-876785.okta.com/oauth2/default");
        keyManagerDTO.setJwksEndpoint("https://dev-876785.okta.com/oauth2/default/v1/keys");
        keyManagerDTO.setEnableMapOAuthConsumerApps(false);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        keyManagerDTO.addTokenValidationItem(tokenValidationDTO);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("apiKey", "00676qG9mlmgTMWhu9pCPaooq4aMQv7e-CkMJteVFS");
        additionalProperties.put("client_id", "0oaavvnfuqhOI47FX4x6");
        additionalProperties.put("client_secret", "ZLUNkL_ePp7F-xF1_ZBNXTQjeYIenvBFxIC1fHg2");
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("cid").localClaim("azp"));
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("scp").localClaim("scope"));
        ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 201);
        KeyManagerDTO retrievedData = keyManagerDTOApiResponse.getData();
        Assert.assertNotNull(retrievedData.getId());
        Assert.assertEquals(retrievedData.getName(), keyManagerDTO.getName());
        keyManagerId = retrievedData.getId();
    }

    @Test(groups = {
            "wso2.am"}, description = "Create Key Manager from Defined Key Manager", dependsOnMethods =
            "testGetKeyManagerSettings")
    public void testCreateKeyManager2() throws Exception {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("okta");
        keyManagerDTO.setName("Key Manager 2");
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setClientRegistrationEndpoint("https://dev-306722.okta.com/oauth2/v1/clients");
        keyManagerDTO.setIntrospectionEndpoint("https://dev-306722.okta.com/oauth2/default/v1/introspect");
        keyManagerDTO.setTokenEndpoint("https://dev-306722.okta.com/oauth2/default/v1/token");
        keyManagerDTO.setRevokeEndpoint("https://dev-306722.okta.com/oauth2/default/v1/revoke");
        keyManagerDTO.setIssuer("https://dev-306722.okta.com/oauth2/default");
        keyManagerDTO.setJwksEndpoint("https://dev-306722.okta.com/oauth2/default/v1/keys");
        keyManagerDTO.setEnableMapOAuthConsumerApps(false);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        keyManagerDTO.addTokenValidationItem(tokenValidationDTO);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("apiKey", "0037c9KwT48hVMU44hIy2Tdvkib051jmGsqj2pLrEW");
        additionalProperties.put("client_id", "0oac4qs4cosoL76va4x6");
        additionalProperties.put("client_secret", "HJOmt2gNJJG7aECq9OuHLWOGceFlXbeTuzmomVMr");
        additionalProperties.put("self_validate_jwt", true);
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("cid").localClaim("azp"));
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("scp").localClaim("scope"));
        ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 201);
        KeyManagerDTO retrievedData = keyManagerDTOApiResponse.getData();
        Assert.assertNotNull(retrievedData.getId());
        Assert.assertEquals(retrievedData.getName(), keyManagerDTO.getName());
        keymanager2Id = retrievedData.getId();
    }

    @Test(groups = {
            "wso2.am"}, description = "Create Key Manager from Defined Key Manager", dependsOnMethods =
            "testGetKeyManagerSettings")
    public void testCreateKeyManagerDisabledState() throws Exception {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("okta");
        keyManagerDTO.setName("Key Manager 3");
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(false);
        keyManagerDTO.setClientRegistrationEndpoint("https://dev-306722.okta.com/oauth2/v1/clients");
        keyManagerDTO.setIntrospectionEndpoint("https://dev-306722.okta.com/oauth2/default/v1/introspect");
        keyManagerDTO.setTokenEndpoint("https://dev-306722.okta.com/oauth2/default/v1/token");
        keyManagerDTO.setRevokeEndpoint("https://dev-306722.okta.com/oauth2/default/v1/revoke");
        keyManagerDTO.setIssuer("https://dev-306722.okta.com/oauth2/default");
        keyManagerDTO.setJwksEndpoint("https://dev-306722.okta.com/oauth2/default/v1/keys");
        keyManagerDTO.setEnableMapOAuthConsumerApps(false);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        keyManagerDTO.addTokenValidationItem(tokenValidationDTO);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("apiKey", "0037c9KwT48hVMU44hIy2Tdvkib051jmGsqj2pLrEW");
        additionalProperties.put("client_id", "0oac4qs4cosoL76va4x6");
        additionalProperties.put("client_secret", "HJOmt2gNJJG7aECq9OuHLWOGceFlXbeTuzmomVMr");
        additionalProperties.put("self_validate_jwt", true);
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("cid").localClaim("azp"));
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("scp").localClaim("scope"));
        ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 201);
        KeyManagerDTO retrievedData = keyManagerDTOApiResponse.getData();
        Assert.assertNotNull(retrievedData.getId());
        Assert.assertEquals(retrievedData.getName(), keyManagerDTO.getName());
        Assert.assertEquals(retrievedData.isEnabled(), keyManagerDTO.isEnabled());
        keymanager3Id = retrievedData.getId();
    }

    @Test(groups = {
            "wso2.am"}, description = "Create Key Manager from Defined Key Manager", dependsOnMethods =
            "testCreateKeyManagerDisabledState")
    public void testUpdateKeyManager() throws Exception {

        KeyManagerDTO keyManager = restAPIAdmin.getKeyManager(keymanager3Id);
        Assert.assertEquals(keyManager.getName(), "Key Manager 3");
        Assert.assertEquals(keyManager.getType(), "okta");
        Assert.assertFalse(keyManager.isEnabled());
        keyManager.setDescription("This is Key Manager Disabled");
        KeyManagerDTO updateKeyManager = restAPIAdmin.updateKeyManager(keymanager3Id, keyManager);
        Assert.assertEquals(updateKeyManager.getName(), "Key Manager 3");
        Assert.assertEquals(updateKeyManager.getType(), "okta");
        Assert.assertFalse(updateKeyManager.isEnabled());
        Assert.assertEquals(updateKeyManager.getDescription(), "This is Key Manager Disabled");
    }

    @Test(groups = {"wso2.am"}, description = "Create Key Manager from not defined keymanager type")
    public void testCreateKeyManagerNegativeTest1() {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("okta1");
        keyManagerDTO.setName("Key Manager 4");
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(false);
        keyManagerDTO.setClientRegistrationEndpoint("https://dev-306722.okta.com/oauth2/v1/clients");
        keyManagerDTO.setIntrospectionEndpoint("https://dev-306722.okta.com/oauth2/default/v1/introspect");
        keyManagerDTO.setTokenEndpoint("https://dev-306722.okta.com/oauth2/default/v1/token");
        keyManagerDTO.setRevokeEndpoint("https://dev-306722.okta.com/oauth2/default/v1/revoke");
        keyManagerDTO.setIssuer("https://dev-306722.okta.com/oauth2/default");
        keyManagerDTO.setJwksEndpoint("https://dev-306722.okta.com/oauth2/default/v1/keys");
        keyManagerDTO.setEnableMapOAuthConsumerApps(false);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        keyManagerDTO.addTokenValidationItem(tokenValidationDTO);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("apiKey", "0037c9KwT48hVMU44hIy2Tdvkib051jmGsqj2pLrEW");
        additionalProperties.put("client_id", "0oac4qs4cosoL76va4x6");
        additionalProperties.put("client_secret", "HJOmt2gNJJG7aECq9OuHLWOGceFlXbeTuzmomVMr");
        additionalProperties.put("self_validate_jwt", true);
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("cid").localClaim("azp"));
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("scp").localClaim("scope"));
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail("Key Manager Created with not defined Key Manager Type");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            String responseBody = e.getResponseBody();
            log.info("response Body== " + responseBody);
            ErrorDTO errorDTO = new Gson().fromJson(responseBody, ErrorDTO.class);
            Assert.assertEquals(errorDTO.getCode().longValue(), 901400);
            Assert.assertEquals(errorDTO.getMessage(), "Key Manager Type not configured");
            Assert.assertEquals(errorDTO.getDescription(), "Key Manager Type not configured");
        }
    }

    @Test(groups = {
            "wso2.am"}, description = "Create Key Manager with existing name", dependsOnMethods =
            "testCreateKeyManager")
    public void testCreateKeyManagerNegativeTest2() {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("okta");
        keyManagerDTO.setName("Key Manager 1");
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(false);
        keyManagerDTO.setClientRegistrationEndpoint("https://dev-306722.okta.com/oauth2/v1/clients");
        keyManagerDTO.setIntrospectionEndpoint("https://dev-306722.okta.com/oauth2/default/v1/introspect");
        keyManagerDTO.setTokenEndpoint("https://dev-306722.okta.com/oauth2/default/v1/token");
        keyManagerDTO.setRevokeEndpoint("https://dev-306722.okta.com/oauth2/default/v1/revoke");
        keyManagerDTO.setIssuer("https://dev-306722.okta.com/oauth2/default");
        keyManagerDTO.setJwksEndpoint("https://dev-306722.okta.com/oauth2/default/v1/keys");
        keyManagerDTO.setEnableMapOAuthConsumerApps(false);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        keyManagerDTO.addTokenValidationItem(tokenValidationDTO);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("apiKey", "0037c9KwT48hVMU44hIy2Tdvkib051jmGsqj2pLrEW");
        additionalProperties.put("client_id", "0oac4qs4cosoL76va4x6");
        additionalProperties.put("client_secret", "HJOmt2gNJJG7aECq9OuHLWOGceFlXbeTuzmomVMr");
        additionalProperties.put("self_validate_jwt", true);
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("cid").localClaim("azp"));
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("scp").localClaim("scope"));
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail("Key Manager Created with not defined Key Manager Type");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 409);
            String responseBody = e.getResponseBody();
            log.info("response Body == " + responseBody);
            ErrorDTO errorDTO = new Gson().fromJson(responseBody, ErrorDTO.class);
            Assert.assertEquals(errorDTO.getCode().longValue(), 901402);
            Assert.assertEquals(errorDTO.getMessage(), "Key Manager Already Exist");
            Assert.assertEquals(errorDTO.getDescription(), "Key Manager Already Exist");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create Key Manager without having required Param")
    public void testCreateKeyManagerNegativeTest3() {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("okta");
        keyManagerDTO.setName("Key Manager -a");
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(false);
        keyManagerDTO.setClientRegistrationEndpoint("https://dev-306722.okta.com/oauth2/v1/clients");
        keyManagerDTO.setIntrospectionEndpoint("https://dev-306722.okta.com/oauth2/default/v1/introspect");
        keyManagerDTO.setTokenEndpoint("https://dev-306722.okta.com/oauth2/default/v1/token");
        keyManagerDTO.setRevokeEndpoint("https://dev-306722.okta.com/oauth2/default/v1/revoke");
        keyManagerDTO.setIssuer("https://dev-306722.okta.com/oauth2/default");
        keyManagerDTO.setJwksEndpoint("https://dev-306722.okta.com/oauth2/default/v1/keys");
        keyManagerDTO.setEnableMapOAuthConsumerApps(false);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        keyManagerDTO.addTokenValidationItem(tokenValidationDTO);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("client_id", "0oac4qs4cosoL76va4x6");
        additionalProperties.put("client_secret", "HJOmt2gNJJG7aECq9OuHLWOGceFlXbeTuzmomVMr");
        additionalProperties.put("self_validate_jwt", true);
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("cid").localClaim("azp"));
        keyManagerDTO.addClaimMappingItem(new ClaimMappingEntryDTO().remoteClaim("scp").localClaim("scope"));
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail("Key Manager Created with not defined Key Manager Type");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            String responseBody = e.getResponseBody();
            log.info("response Body == " + responseBody);
            ErrorDTO errorDTO = new Gson().fromJson(responseBody, ErrorDTO.class);
            Assert.assertEquals(errorDTO.getCode().longValue(), 901401);
            Assert.assertEquals(errorDTO.getMessage(), "Required Key Manager configuration missing");
            Assert.assertEquals(errorDTO.getDescription(), "Missing required configuration");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testUpdateKeyManager"})
    public void testRetrieveKeyManagersFromStore() throws org.wso2.am.integration.clients.store.api.ApiException {

        KeyManagerListDTO keyManagers = restAPIStore.getKeyManagers();
        log.info("Keymanager info ==== " + keyManagers.toString());
        Assert.assertEquals(keyManagers.getCount().intValue(), 4);
        for (KeyManagerInfoDTO keyManagerInfoDTO : keyManagers.getList()) {
            if ("Key Manager 1".equals(keyManagerInfoDTO.getName())) {
                Assert.assertEquals(keyManagerInfoDTO.getId(), keyManagerId);
                Assert.assertEquals(keyManagerInfoDTO.getType(), "okta");
                List<KeyManagerApplicationConfigurationDTO> applicationConfiguration =
                        keyManagerInfoDTO.getApplicationConfiguration();
                for (KeyManagerApplicationConfigurationDTO configuration :
                        applicationConfiguration) {
                    if ("application_type".equals(configuration.getName())) {
                        Assert.assertEquals(configuration.getLabel(), "Application Type");
                        Assert.assertEquals(configuration.getType(), "select");
                        Assert.assertEquals(configuration.getTooltip(), "Type Of Application to create");
                        Assert.assertFalse(configuration.isMultiple());
                        Assert.assertFalse(configuration.isRequired());
                        Assert.assertFalse(configuration.isMask());
                        Assert.assertEquals(configuration.getDefault(), "web");
                        Assert.assertEquals(configuration.getValues().size(), 4);
                        Assert.assertEquals(configuration.getValues(),
                                Arrays.asList("web", "native", "service", "browser"));

                    } else if ("response_types".equals(configuration.getName())) {
                        Assert.assertEquals(configuration.getLabel(), "Response Type");
                        Assert.assertEquals(configuration.getType(), "input");
                        Assert.assertEquals(configuration.getTooltip(), "Type Of Token response");
                        Assert.assertTrue(configuration.isMultiple());
                        Assert.assertTrue(configuration.isRequired());
                        Assert.assertFalse(configuration.isMask());
                        Assert.assertEquals(configuration.getDefault(), "");
                        Assert.assertEquals(configuration.getValues().size(), 3);
                        Assert.assertEquals(configuration.getValues(), Arrays.asList("code", "token", "id_token"));
                    } else if ("token_endpoint_auth_method".equals(configuration.getName())) {
                        Assert.assertEquals(configuration.getLabel(), "Token endpoint Authentication Method");
                        Assert.assertEquals(configuration.getType(), "select");
                        Assert.assertEquals(configuration.getTooltip(), "How to Authenticate Token Endpoint");
                        Assert.assertFalse(configuration.isMultiple());
                        Assert.assertTrue(configuration.isRequired());
                        Assert.assertTrue(configuration.isMask());
                        Assert.assertEquals(configuration.getDefault(), "client_secret_basic");
                        Assert.assertEquals(configuration.getValues().size(), 3);
                        Assert.assertEquals(configuration.getValues(),
                                Arrays.asList("client_secret_basic", "client_secret_post", "client_secret_jwt"));
                    }
                }
            } else if ("Key Manager 2".equals(keyManagerInfoDTO.getName())) {
                Assert.assertEquals(keyManagerInfoDTO.getId(), keymanager2Id);
                Assert.assertEquals(keyManagerInfoDTO.getType(), "okta");
                Assert.assertEquals(keyManagerInfoDTO.getDescription(), "This is Key Manager");
                List<KeyManagerApplicationConfigurationDTO> applicationConfiguration =
                        keyManagerInfoDTO.getApplicationConfiguration();
                for (KeyManagerApplicationConfigurationDTO configuration :
                        applicationConfiguration) {
                    if ("application_type".equals(configuration.getName())) {
                        Assert.assertEquals(configuration.getLabel(), "Application Type");
                        Assert.assertEquals(configuration.getType(), "select");
                        Assert.assertEquals(configuration.getTooltip(), "Type Of Application to create");
                        Assert.assertFalse(configuration.isMultiple());
                        Assert.assertFalse(configuration.isRequired());
                        Assert.assertFalse(configuration.isMask());
                        Assert.assertEquals(configuration.getDefault(), "web");
                        Assert.assertEquals(configuration.getValues().size(), 4);
                        Assert.assertEquals(configuration.getValues(),
                                Arrays.asList("web", "native", "service", "browser"));

                    } else if ("response_types".equals(configuration.getName())) {
                        Assert.assertEquals(configuration.getLabel(), "Response Type");
                        Assert.assertEquals(configuration.getType(), "input");
                        Assert.assertEquals(configuration.getTooltip(), "Type Of Token response");
                        Assert.assertTrue(configuration.isMultiple());
                        Assert.assertTrue(configuration.isRequired());
                        Assert.assertFalse(configuration.isMask());
                        Assert.assertEquals(configuration.getDefault(), "");
                        Assert.assertEquals(configuration.getValues().size(), 3);
                        Assert.assertEquals(configuration.getValues(), Arrays.asList("code", "token", "id_token"));
                    } else if ("token_endpoint_auth_method".equals(configuration.getName())) {
                        Assert.assertEquals(configuration.getLabel(), "Token endpoint Authentication Method");
                        Assert.assertEquals(configuration.getType(), "select");
                        Assert.assertEquals(configuration.getTooltip(), "How to Authenticate Token Endpoint");
                        Assert.assertFalse(configuration.isMultiple());
                        Assert.assertTrue(configuration.isRequired());
                        Assert.assertTrue(configuration.isMask());
                        Assert.assertEquals(configuration.getDefault(), "client_secret_basic");
                        Assert.assertEquals(configuration.getValues().size(), 3);
                        Assert.assertEquals(configuration.getValues(),
                                Arrays.asList("client_secret_basic", "client_secret_post", "client_secret_jwt"));
                    }
                }
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testUpdateKeyManager", "testRetrieveKeyManagersFromStore"})
    public void testGenerateTokenForDefaultKeyManager() throws org.wso2.am.integration.clients.store.api.ApiException {

        Map<String, Object> additionalProperties = new HashMap<>();
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(jwtAppId, "3600", "https://localhost",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"), Arrays.asList(
                        "password,client_credentials,authorization_code"),
                additionalProperties, "Default");
        Assert.assertNotNull(applicationKeyDTO.getConsumerKey());
        Assert.assertNotNull(applicationKeyDTO.getConsumerSecret());
        Assert.assertNotNull(applicationKeyDTO.getKeyMappingId());
        Assert.assertEquals(applicationKeyDTO.getKeyManager(), "Default");
        defaultKeyManagerApplicationKey = applicationKeyDTO;
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testRetrieveKeyManagersFromStore", "testUpdateKeyManager"})
    public void testGenerateTokenForKeyManager1()
            throws org.wso2.am.integration.clients.store.api.ApiException, InterruptedException {

        Thread.sleep(500);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("application_type", "web");
        additionalProperties.put("response_types", Arrays.asList("code", "token"));
        additionalProperties.put("token_endpoint_auth_method", "client_secret_basic");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(jwtAppId, "3600", "https://localhost",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"), Arrays.asList(
                        "authorization_code", "password", "client_credentials", "implicit"),
                additionalProperties, "Key Manager 1");
        Assert.assertNotNull(applicationKeyDTO.getConsumerKey());
        Assert.assertNotNull(applicationKeyDTO.getConsumerSecret());
        Assert.assertNotNull(applicationKeyDTO.getKeyMappingId());
        Assert.assertEquals(applicationKeyDTO.getKeyManager(), "Key Manager 1");
        this.keyManager1ApplicationKey = applicationKeyDTO;
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testRetrieveKeyManagersFromStore", "testUpdateKeyManager"})
    public void testGenerateTokenForKeyManager2()
            throws org.wso2.am.integration.clients.store.api.ApiException, InterruptedException {

        Thread.sleep(500);
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("application_type", "web");
        additionalProperties.put("response_types", Arrays.asList("code"));
        additionalProperties.put("token_endpoint_auth_method", "client_secret_basic");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(jwtAppId, "3600", "https://localhost",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"), Arrays.asList(
                        "authorization_code", "password", "client_credentials"),
                additionalProperties, "Key Manager 2");
        Assert.assertNotNull(applicationKeyDTO.getConsumerKey());
        Assert.assertNotNull(applicationKeyDTO.getConsumerSecret());
        Assert.assertNotNull(applicationKeyDTO.getKeyMappingId());
        Assert.assertEquals(applicationKeyDTO.getKeyManager(), "Key Manager 2");
        this.keyManager1ApplicationKey2 = applicationKeyDTO;
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testRetrieveKeyManagersFromStore", "testUpdateKeyManager"})
    public void testGenerateTokenForKeyManagerDefaultOauthApp()
            throws org.wso2.am.integration.clients.store.api.ApiException {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("application_type", "web");
        additionalProperties.put("response_types", Arrays.asList("code"));
        additionalProperties.put("token_endpoint_auth_method", "client_secret_basic");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(oauthAppId, "3600", "https://localhost",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"), Arrays.asList(
                        "authorization_code", "password", "client_credentials"),
                additionalProperties, "Default");
        Assert.assertNotNull(applicationKeyDTO.getConsumerKey());
        Assert.assertNotNull(applicationKeyDTO.getConsumerSecret());
        Assert.assertNotNull(applicationKeyDTO.getKeyMappingId());
        Assert.assertEquals(applicationKeyDTO.getKeyManager(), "Default");
        this.oauthApplicationKey = applicationKeyDTO;
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testRetrieveKeyManagersFromStore", "testUpdateKeyManager"})
    public void testGenerateTokenForKeyManagerNegative1() {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("application_type", "web");
        additionalProperties.put("response_types", Arrays.asList("code"));
        additionalProperties.put("token_endpoint_auth_method", "client_secret_basic");
        try {
            restAPIStore.generateKeysWithApiResponse(jwtAppId, "3600", "https://localhost",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"), Arrays.asList(
                            "authorization_code", "password", "client_credentials"),
                    additionalProperties, "Key Manager negative");
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            String responseBody = e.getResponseBody();
            log.info("response Body == " + responseBody);
            ErrorDTO errorDTO = new Gson().fromJson(responseBody, ErrorDTO.class);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Generate Keys for App with key manager1 And Retrieve",
            dependsOnMethods = {"testCreateKeyManager", "testCreateKeyManager2", "testCreateKeyManagerDisabledState",
                    "testRetrieveKeyManagersFromStore", "testUpdateKeyManager"})
    public void testGenerateTokenForKeyManagerNegative2() {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("token_endpoint_auth_method", "client_secret_basic");
        try {
            restAPIStore.generateKeysWithApiResponse(oauthAppId, "3600", "https://localhost",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"), Arrays.asList(
                            "authorization_code", "password", "client_credentials"),
                    additionalProperties, "Key Manager 1");
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            String responseBody = e.getResponseBody();
            log.info("response Body == " + responseBody);
            ErrorDTO errorDTO = new Gson().fromJson(responseBody, ErrorDTO.class);
        }
    }

    @Test(description = "Invoke API with Default KeyManager token", dependsOnMethods = {
            "testGenerateTokenForDefaultKeyManager"})
    public void testInvokeDefaultKeyManager() throws XPathExpressionException, IOException {

        HttpResponse apiResponse = invokeAPI(defaultKeyManagerApplicationKey.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Invoke API with Default KeyManager token", dependsOnMethods = {
            "testGenerateTokenForKeyManager1"})
    public void testInvokeKeyManager1() throws XPathExpressionException, IOException {

        HttpResponse apiResponse = invokeAPI(keyManager1ApplicationKey.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Invoke API with Default KeyManager token", dependsOnMethods = {
            "testGenerateTokenForKeyManager2"})
    public void testInvokeKeyManager2() throws XPathExpressionException, IOException {

        HttpResponse apiResponse = invokeAPI(keyManager1ApplicationKey2.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Invoke API with Default KeyManager token", dependsOnMethods = {
            "testGenerateTokenForKeyManagerDefaultOauthApp"})
    public void testInvokeKeyManagerOauthApp() throws XPathExpressionException, IOException {

        HttpResponse apiResponse = invokeAPI(oauthApplicationKey.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Invoke API with KeyManager1 Token when keymanager1NotAvailable", dependsOnMethods = {
            "testInvokeDefaultKeyManager", "testInvokeKeyManager1", "testInvokeKeyManager2",
            "testInvokeKeyManagerOauthApp"})
    public void testInvokeKeyManagerAfterUpdateOauthAppNegative()
            throws XPathExpressionException, IOException, org.wso2.am.integration.clients.publisher.api.ApiException {
        // Create requestHeaders
        APIDTO api = restAPIPublisher.getAPIByID(apiId1);
        api.setKeyManagers(Arrays.asList("Default"));
        restAPIPublisher.updateAPI(api);
        HttpResponse apiResponse = invokeAPI(keyManager1ApplicationKey.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
        apiResponse = invokeAPI(keyManager1ApplicationKey2.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
        apiResponse = invokeAPI(defaultKeyManagerApplicationKey.getToken().getAccessToken());
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description = "Update Key Manager ", dependsOnMethods = {"testInvokeKeyManagerAfterUpdateOauthAppNegative"})
    public void testGetKeys() throws org.wso2.am.integration.clients.store.api.ApiException {

        ApplicationKeyListDTO applicationKeyListDTO = restAPIStore.getApplicationKeysByAppId(jwtAppId);
        Assert.assertEquals(applicationKeyListDTO.getCount().intValue(), 3);
        for (ApplicationKeyDTO key : applicationKeyListDTO.getList()) {
            if (keyManager1ApplicationKey2.getKeyMappingId().equals(key.getKeyMappingId())) {
                Assert.assertEquals(key.getConsumerKey(), keyManager1ApplicationKey2.getConsumerKey());
                Assert.assertEquals(key.getKeyManager(), keyManager1ApplicationKey2.getKeyManager());
            } else if (keyManager1ApplicationKey.getKeyMappingId().equals(key.getKeyMappingId())) {
                Assert.assertEquals(key.getConsumerKey(), keyManager1ApplicationKey.getConsumerKey());
                Assert.assertEquals(key.getKeyManager(), keyManager1ApplicationKey.getKeyManager());
            } else if (defaultKeyManagerApplicationKey.getKeyMappingId().equals(key.getKeyMappingId())) {
                Assert.assertEquals(key.getConsumerKey(), defaultKeyManagerApplicationKey.getConsumerKey());
                Assert.assertEquals(key.getKeyManager(), defaultKeyManagerApplicationKey.getKeyManager());
            }
        }
    }

    @Test(description = "Update Key Manager ", dependsOnMethods = {"testGetKeys"})
    public void testUpdateKey() throws org.wso2.am.integration.clients.store.api.ApiException {

        ApplicationKeyDTO applicationKeyByKeyMappingId =
                restAPIStore.getApplicationKeyByKeyMappingId(jwtAppId, keyManager1ApplicationKey.getKeyMappingId());
        applicationKeyByKeyMappingId
                .setSupportedGrantTypes(Arrays.asList("client_credentials", "password", "refresh_token",
                        "authorization_code"));
        Map additionalProperties = (Map) applicationKeyByKeyMappingId.getAdditionalProperties();
        additionalProperties.put("response_types", Arrays.asList("code"));
        applicationKeyByKeyMappingId.setAdditionalProperties(additionalProperties);
        restAPIStore.updateApplicationKeyByKeyMappingId(jwtAppId, keyManager1ApplicationKey.getKeyMappingId(),
                applicationKeyByKeyMappingId);
        ApplicationKeyDTO upDatedKeyMapping =
                restAPIStore.getApplicationKeyByKeyMappingId(jwtAppId, keyManager1ApplicationKey.getKeyMappingId());
        Assert.assertEquals(upDatedKeyMapping.getKeyManager(), applicationKeyByKeyMappingId.getKeyManager());
        Assert.assertFalse(upDatedKeyMapping.getSupportedGrantTypes().contains("implicit"));
    }

    private HttpResponse invokeAPI(String accessToken) throws XPathExpressionException, IOException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        return HttpRequestUtil.doGet(getAPIInvocationURLHttps(context, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(jwtAppId);
        restAPIStore.deleteApplication(oauthAppId);
        restAPIAdmin.deleteKeyManager(keyManagerId);
        restAPIAdmin.deleteKeyManager(keymanager2Id);
        restAPIAdmin.deleteKeyManager(keymanager3Id);
        restAPIPublisher.deleteAPI(apiId1);
        super.cleanUp();

    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public ThirdPartyKeyManagerRegistrationTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    private void createAndPublishAPI() throws MalformedURLException, APIManagerIntegrationTestException,
            org.wso2.am.integration.clients.publisher.api.ApiException {

        APIRequest apiRequest1 = new APIRequest("thirdpatykm", "thirdpartykm", new URL(apiEndPointUrl));
        apiRequest1.setVersion(API_VERSION_1_0_0);
        apiRequest1.setProvider(user.getUserName());
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTags(API_TAGS);
        apiRequest1.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest1.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("oauth2");
        apiRequest1.setSecurityScheme(securitySchemes);
        apiRequest1.setDefault_version("true");
        apiRequest1.setHttps_checked("https");
        apiRequest1.setHttp_checked(null);
        apiRequest1.setDefault_version_checked("true");
        HttpResponse response1 = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response1.getData();
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId1, false);
    }
}
