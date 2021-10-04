/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.restapi.admin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KeyManagersTestCase extends APIMIntegrationBaseTest {
    private AdminApiTestHelper adminApiTestHelper;
    private KeyManagerDTO keyManagerDTO;

    @Factory(dataProvider = "userModeDataProvider")
    public KeyManagersTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    //Auth0 Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with Auth0 type with only mandatory parameters")
    public void testAddKeyManagerWithAuth0() throws Exception {
        //Create the key manager DTO with Auth0 key manager type with only Mandatory parameters
        String name = "Auth0KeyManagerOne";
        String type = "Auth0";
        String displayName = "Test Key Manager Auth0";
        String introspectionEndpoint = "none";
        String revokeEndpoint = "https://dev-ted144kt.us.auth0.com/oauth/revoke";
        String clientRegistrationEndpoint = "https://dev-ted144kt.us.auth0.com/oidc/register";
        String tokenEndpoint = "https://dev-ted144kt.us.auth0.com/oauth/token";
        String authorizeEndpoint = "https://dev-ted144kt.us.auth0.com/authorize";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("audience", "audienceValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, authorizeEndpoint,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the Auth0 key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Auth0 type without a mandatory parameter")
    public void testAddKeyManagerWithAuth0WithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with Auth0 key manager type without Connector Configurations (Mandatory parameter)
        String name = "Auth0KeyManagerTwo";
        String type = "Auth0";
        String displayName = "Test Key Manager Auth0";
        String introspectionEndpoint = "none";
        String revokeEndpoint = "https://dev-ted144kt.us.auth0.com/oauth/revoke";
        String clientRegistrationEndpoint = "https://dev-ted144kt.us.auth0.com/oidc/register";
        String tokenEndpoint = "https://dev-ted144kt.us.auth0.com/oauth/token";
        String authorizeEndpoint = "https://dev-ted144kt.us.auth0.com/authorize";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id, client secret and audience are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        String certificateValue = "";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.PEM, certificateValue);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, authorizeEndpoint,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);

        //Add the Auth0 key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Auth0 type with mandatory " +
            "and some optional parameters")
    public void testAddKeyManagerWithAuth0WithOptionalParams() throws Exception {
        //Create the key manager DTO with Auth0 key manager type with mandatory and some optional parameters
        String name = "Auth0KeyManagerThree";
        String type = "Auth0";
        String displayName = "Test Key Manager Auth0";
        String introspectionEndpoint = "none";
        String revokeEndpoint = "https://dev-ted144kt.us.auth0.com/oauth/revoke";
        String clientRegistrationEndpoint = "https://dev-ted144kt.us.auth0.com/oidc/register";
        String tokenEndpoint = "https://dev-ted144kt.us.auth0.com/oauth/token";
        String authorizeEndpoint = "https://dev-ted144kt.us.auth0.com/authorize";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("audience", "audienceValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional parameters
        String description = "This is a test key manager";
        String issuer = "https://dev-ted144kt.us.auth0.com/";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token");
        String certificateValue = "https://dev-ted144kt.us.auth0.com/.well-known/jwks.json";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.JWKS, certificateValue);

        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, authorizeEndpoint,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);

        //Add the Auth0 key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    //WSO2 IS Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with WSO2IS type with only mandatory parameters")
    public void testAddKeyManagerWithWso2IS() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type with only Mandatory parameters
        String name = "Wso2ISKeyManagerOne";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Username", "admin");
        jsonObject.addProperty("Password", "admin");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, null, null, null, null,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the WSO2 IS key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with WSO2IS type without a mandatory parameter")
    public void testAddKeyManagerWithWso2ISWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type without Connector Configurations (Mandatory parameter)
        String name = "Wso2ISKeyManagerTwo";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - username and password are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, null, null, null, null,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the WSO2 IS key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with WSO2IS type with mandatory " +
            "and some optional parameters")
    public void testAddKeyManagerWithWso2ISWithOptionalParams() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type with mandatory and some optional parameters
        String name = "Wso2ISKeyManagerThree";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Username", "admin");
        jsonObject.addProperty("Password", "admin");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional parameters
        String description = "This is a test key manager";
        String issuer = "https://localhost:9444/services";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token");
        String certificateValue = "https://localhost:9443/oauth2/jwks";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.JWKS, certificateValue);

        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, null, null, null, null,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);

        //Add the WSO2 IS key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    //Keycloak Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with Keycloak type with only mandatory parameters")
    public void testAddKeyManagerWithKeycloak() throws Exception {
        //Create the key manager DTO with Keycloak key manager type with only Mandatory parameters
        String name = "KeycloakKeyManagerOne";
        String type = "KeyCloak";
        String displayName = "Test Key Manager Keycloak";
        String issuer = "https://localhost:8443/auth/realms/master";
        String introspectionEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token/introspect";
        String clientRegistrationEndpoint = "https://localhost:8443/auth/realms/master/clients-registrations/openid-connect";
        String tokenEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, null, null, null,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the Keycloak key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Keycloak type without a mandatory parameter")
    public void testAddKeyManagerWithKeycloakWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with Keycloak key manager type without Connector Configurations (Mandatory parameter)
        String name = "KeycloakKeyManagerTwo";
        String type = "KeyCloak";
        String displayName = "Test Key Manager Keycloak";
        String issuer = "https://localhost:8443/auth/realms/master";
        String introspectionEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token/introspect";
        String clientRegistrationEndpoint = "https://localhost:8443/auth/realms/master/clients-registrations/openid-connect";
        String tokenEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id and client secret are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, null, null, null,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);
        //Add the Keycloak key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Keycloak type with mandatory " +
            "and some optional parameters")
    public void testAddKeyManagerWithKeycloakWithOptionalParams() throws Exception {
        //Create the key manager DTO with Keycloak key manager type with mandatory and some optional parameters
        String name = "KeycloakKeyManagerThree";
        String type = "KeyCloak";
        String displayName = "Test Key Manager Keycloak";
        String issuer = "https://localhost:8443/auth/realms/master";
        String introspectionEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token/introspect";
        String clientRegistrationEndpoint = "https://localhost:8443/auth/realms/master/clients-registrations/openid-connect";
        String tokenEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional Parameters
        String description = "This is a test key manager";
        String revokeEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/revoke";
        String userInfoEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/userinfo";
        String authorizeEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/auth";
        String scopeManagementEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/scopes";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token");
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, userInfoEndpoint, authorizeEndpoint,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);
        //Add the Keycloak key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    //Okta Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with Okta type with only mandatory parameters")
    public void testAddKeyManagerWithOkta() throws Exception {
        //Create the key manager DTO with Okta key manager type with only Mandatory parameters
        String name = "OktaKeyManagerOne";
        String type = "Okta";
        String displayName = "Test Key Manager Okta";
        String introspectionEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/introspect";
        String clientRegistrationEndpoint = "https://dev-599740.okta.com/oauth2/v1/clients";
        String tokenEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/token";
        String revokeEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/revoke";
        String consumerKeyClaim = "cid";
        String scopesClaim = "scp";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("apiKey", "apiKeyValue");
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the Okta key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Okta type without a mandatory parameter")
    public void testAddKeyManagerWithOktaWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with Okta key manager type without Connector Configurations (Mandatory parameter)
        String name = "OktaKeyManagerTwo";
        String type = "Okta";
        String displayName = "Test Key Manager Okta";
        String introspectionEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/introspect";
        String clientRegistrationEndpoint = "https://dev-599740.okta.com/oauth2/v1/clients";
        String tokenEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/token";
        String revokeEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/revoke";
        String consumerKeyClaim = "cid";
        String scopesClaim = "scp";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id, client secret and API key are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);
        //Add the Okta key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Okta type with mandatory " +
            "and some optional parameters")
    public void testAddKeyManagerWithOktaWithOptionalParams() throws Exception {
        //Create the key manager DTO with Okta key manager type with mandatory and some optional parameters
        String name = "OktaKeyManagerThree";
        String type = "Okta";
        String displayName = "Test Key Manager Okta";
        String introspectionEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/introspect";
        String clientRegistrationEndpoint = "https://dev-599740.okta.com/oauth2/v1/clients";
        String tokenEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/token";
        String revokeEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/revoke";
        String consumerKeyClaim = "cid";
        String scopesClaim = "scp";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("apiKey", "apiKeyValue");
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional parameters
        String description = "This is a test key manager";
        String issuer = "https://dev-599740.okta.com/oauth2/default";
        String userInfoEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/userinfo";
        String authorizeEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/authorize";
        String scopeManagementEndpoint = "https://dev-599740.okta.com/oauth2/default/v1/scopes";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token", "authorization_code");
        String certificateValue = "https://dev-599740.okta.com/oauth2/default/v1/keys";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.JWKS, certificateValue);

        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, userInfoEndpoint, authorizeEndpoint,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);
        //Add the Okta key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    //PingFederate Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with PingFederate type with only mandatory parameters")
    public void testAddKeyManagerWithPingFederate() throws Exception {
        //Create the key manager DTO with PingFederate key manager type with only Mandatory parameters
        String name = "PingFederateKeyManagerOne";
        String type = "PingFederate";
        String displayName = "Test Key Manager PingFederate";
        String issuer = "https://localhost:9031";
        String introspectionEndpoint = "https://localhost:9031/as/introspect.oauth2";
        String clientRegistrationEndpoint = "https://localhost:9031/pf-ws/rest/oauth/clients";
        String tokenEndpoint = "https://localhost:9031/as/token.oauth2";
        String consumerKeyClaim = "client_id_name";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", "admin");
        jsonObject.addProperty("password", "admin");
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, null, null,
                null, null, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, null);

        //Add the PingFederate key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with PingFederate type without a mandatory parameter")
    public void testAddKeyManagerWithPingFederateWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with PingFederate key manager type without Connector Configurations (Mandatory parameter)
        String name = "PingFederateKeyManagerTwo";
        String type = "PingFederate";
        String displayName = "Test Key Manager PingFederate";
        String issuer = "https://localhost:9031";
        String introspectionEndpoint = "https://localhost:9031/as/introspect.oauth2";
        String clientRegistrationEndpoint = "https://localhost:9031/pf-ws/rest/oauth/clients";
        String tokenEndpoint = "https://localhost:9031/as/token.oauth2";
        String consumerKeyClaim = "client_id_name";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id, client secret, username and password are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, null, null,
                null, null, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, null);
        //Add the PingFederate key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with PingFederate type with mandatory " +
            "and some optional parameters")
    public void testAddKeyManagerWithPingFederateWithOptionalParams() throws Exception {
        //Create the key manager DTO with PingFederate key manager type with mandatory and some optional parameters
        String name = "PingFederateKeyManagerThree";
        String type = "PingFederate";
        String displayName = "Test Key Manager PingFederate";
        String issuer = "https://localhost:9031";
        String introspectionEndpoint = "https://localhost:9031/as/introspect.oauth2";
        String clientRegistrationEndpoint = "https://localhost:9031/pf-ws/rest/oauth/clients";
        String tokenEndpoint = "https://localhost:9031/as/token.oauth2";
        String consumerKeyClaim = "client_id_name";
        String scopesClaim = "scope";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", "admin");
        jsonObject.addProperty("password", "admin");
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional parameters
        String description = "This is a test key manager";
        String userInfoEndpoint = "https://localhost:9031/as/userinfo.oauth2";
        String authorizeEndpoint = "https://localhost:9031/as/authorization.oauth2";
        String scopeManagementEndpoint = "https://localhost:9031/as/scope.oauth2";
        String revokeEndpoint = "https://localhost:9031/as/revoke_token.oauth2";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token", "authorization_code");
        String certificateValue = "https://localhost:9031/pf/JWKS";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.JWKS, certificateValue);

        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, userInfoEndpoint, authorizeEndpoint,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);
        //Add the PingFederate key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    //ForgeRock Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type with only mandatory parameters")
    public void testAddKeyManagerWithForgeRock() throws Exception {
        //Create the key manager DTO with ForgeRock key manager type with only Mandatory parameters
        String name = "ForgeRockKeyManagerOne";
        String type = "Forgerock";
        String displayName = "Test Key Manager ForgeRock";
        String issuer = "http://localhost:8080/openam/oauth2";
        String introspectionEndpoint = "http://localhost:8080/openam/oauth2/introspect";
        String clientRegistrationEndpoint = "http://localhost:8080/openam/oauth2/register";
        String tokenEndpoint = "http://localhost:8080/openam/oauth2/access_token";
        String consumerKeyClaim = "aud";
        String scopesClaim = "scp";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, null, null,
                null, null, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, null);

        //Add the ForgeRock key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type without a mandatory parameter")
    public void testAddKeyManagerWithForgeRockWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with ForgeRock key manager type without Connector Configurations (Mandatory parameter)
        String name = "ForgeRockKeyManagerTwo";
        String type = "Forgerock";
        String displayName = "Test Key Manager ForgeRock";
        String issuer = "http://localhost:8080/openam/oauth2";
        String introspectionEndpoint = "http://localhost:8080/openam/oauth2/introspect";
        String clientRegistrationEndpoint = "http://localhost:8080/openam/oauth2/register";
        String tokenEndpoint = "http://localhost:8080/openam/oauth2/access_token";
        String consumerKeyClaim = "aud";
        String scopesClaim = "scp";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id and client secret are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, null, null,
                null, null, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, null);
        //Add the ForgeRock key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type with mandatory " +
            "and some optional parameters")
    public void testAddKeyManagerWithForgeRockWithOptionalParams() throws Exception {
        //Create the key manager DTO with ForgeRock key manager type with mandatory and some optional parameters
        String name = "ForgeRockKeyManagerThree";
        String type = "Forgerock";
        String displayName = "Test Key Manager ForgeRock";
        String issuer = "http://localhost:8080/openam/oauth2";
        String introspectionEndpoint = "http://localhost:8080/openam/oauth2/introspect";
        String clientRegistrationEndpoint = "http://localhost:8080/openam/oauth2/register";
        String tokenEndpoint = "http://localhost:8080/openam/oauth2/access_token";
        String consumerKeyClaim = "aud";
        String scopesClaim = "scp";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Optional parameters
        String description = "This is a test key manager";
        String userInfoEndpoint = "http://localhost:8080/openam/oauth2/userinfo";
        String authorizeEndpoint = "http://localhost:8080/openam/oauth2/authorize";
        String scopeManagementEndpoint = "http://localhost:8080/openam/oauth2/scopes";
        String revokeEndpoint = "http://localhost:8080/openam/oauth2/revoke";
        List<String> availableGrantTypes =
                Arrays.asList("client_credentials", "password", "implicit", "refresh_token", "authorization_code");
        String certificateValue = "http://localhost:8080/openam/oauth2/connect/jwk_url";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.JWKS, certificateValue);

        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, description, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, userInfoEndpoint,
                authorizeEndpoint, scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, keyManagerCertificates);

        //Add the ForgeRock key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();

        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    /*TODO:
       Testcase for adding a key manager with existing name
       Testcase for adding a key manager with a name with space (Eg: Auth0 Keymanager)
       Update Key manager
       Delete Key manager
     */

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
