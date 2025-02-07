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
import static org.testng.Assert.assertEquals;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerPermissionsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

public class KeyManagersTestCase extends APIMIntegrationBaseTest {
    private AdminApiTestHelper adminApiTestHelper;
    private KeyManagerDTO keyManagerDTO;
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String USER_TEST = "test";
    private final String USER_TEST_PASSWORD = "test123";
    private String apiEndPointUrl;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private APIIdentifier apiIdentifier;
    private String API_NAME = "DummyApi";
    private String apiId;
    private String appId;
    private String applicationId;
    private String API_SUBSCRIBER = "APISubscriberRole";
    private String apiCreatorStoreDomain;
    private RestAPIStoreImpl restAPIStoreClient1;
    private String[] API_SUBSCRIBER_PERMISSIONS = {
            "/permission/admin/login",
            "/permission/admin/manage/api/create",
            "/permission/admin/manage/api/subscriber"
    };
    String[] ROLE_LIST = { "Internal/publisher", "Internal/subscriber", "Internal/everyone"};
    private APICreationRequestBean apiCreationRequestBean;

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
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
        apiIdentifier = new APIIdentifier(USER_TEST, API_NAME, API_VERSION_1_0_0);
        userManagementClient.addUser(USER_TEST, USER_TEST_PASSWORD, ROLE_LIST, USER_TEST);
        userManagementClient.addRole(API_SUBSCRIBER, new String[]{ USER_TEST }, API_SUBSCRIBER_PERMISSIONS);
    }

    //1. Auth0 Key Manager
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
        String issuer = "https://dev-ted144kt.us.auth0.com";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("audience", "audienceValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, authorizeEndpoint,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the Auth0 key manager
        ApiResponse<KeyManagerDTO> addedKeyManagers = restAPIAdmin.addKeyManager(keyManagerDTO);
        Assert.assertEquals(addedKeyManagers.getStatusCode(), HttpStatus.SC_CREATED);
        KeyManagerDTO addedKeyManagerDTO = addedKeyManagers.getData();
        String keyManagerId = addedKeyManagerDTO.getId();
        waitForKeyManagerDeployment(user.getUserDomain(), keyManagerDTO.getName());
        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(keyManagerDTO.getAdditionalProperties(),
                addedKeyManagerDTO.getAdditionalProperties());
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
        restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        waitForKeyManagerUnDeployment(user.getUserDomain(), keyManagerDTO.getName());
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Auth0 type without a mandatory parameter",
            dependsOnMethods = "testAddKeyManagerWithAuth0")
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
        String issuer = "https://dev-ted144kt.us.auth0.com";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id, client secret and audience are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        String certificateValue = "";
        KeyManagerCertificatesDTO keyManagerCertificates =
                DtoFactory.createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum.PEM, certificateValue);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, authorizeEndpoint,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                keyManagerCertificates);

        //Add the Auth0 key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Auth0 type with mandatory " +
            "and some optional parameters", dependsOnMethods = "testAddKeyManagerWithAuth0WithoutMandatoryParam")
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
        waitForKeyManagerDeployment(user.getUserDomain(), keyManagerDTO.getName());
        //Assert the status code and key manager ID
        Assert.assertNotNull(keyManagerId, "The Key Manager ID cannot be null or empty");
        keyManagerDTO.setId(keyManagerId);
        //Verify the created key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, addedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test get key manager with Auth0 type",
            dependsOnMethods = "testAddKeyManagerWithAuth0WithOptionalParams")
    public void testGetKeyManagerWithAuth0() throws Exception {
        //Get the added Key manager
        String keyManagerId = keyManagerDTO.getId();
        ApiResponse<KeyManagerDTO> retrievedKeyManager = restAPIAdmin.getKeyManager(keyManagerId);
        KeyManagerDTO retrievedKeyManagerDTO = retrievedKeyManager.getData();
        Assert.assertEquals(retrievedKeyManager.getStatusCode(), HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "*****");
        jsonObject.addProperty("audience", "audienceValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object expectedAdditionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Verify the added key manager additional properties
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(expectedAdditionalProperties,
                retrievedKeyManagerDTO.getAdditionalProperties());
        //Verify the added key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, retrievedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update key manager with Auth0 type",
            dependsOnMethods = "testGetKeyManagerWithAuth0")
    public void testUpdateKeyManagerWithAuth0() throws Exception {
        //Update the key manager
        String updatedDescription = "This is a updated test key manager";
        keyManagerDTO.setDescription(updatedDescription);
        ApiResponse<KeyManagerDTO> updatedKeyManager =
                restAPIAdmin.updateKeyManager(keyManagerDTO.getId(), keyManagerDTO);
        KeyManagerDTO updatedKeyManagerDTO = updatedKeyManager.getData();
        Assert.assertEquals(updatedKeyManager.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, updatedKeyManagerDTO);
        waitForKeyManagerDeployment(user.getUserDomain(), keyManagerDTO.getName());
    }

    @Test(groups = {"wso2.am"}, description = "Test delete key manager with Auth0 type",
            dependsOnMethods = "testUpdateKeyManagerWithAuth0")
    public void testDeleteKeyManagerWithAuth0() throws Exception {
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete non existing key manager - not found
        try {
            apiResponse = restAPIAdmin.deleteKeyManager(UUID.randomUUID().toString());
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    //2. WSO2 IS Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with WSO2IS type with only mandatory parameters",
            dependsOnMethods = "testDeleteKeyManagerWithAuth0")
    public void testAddKeyManagerWithWso2IS() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type with only Mandatory parameters
        String name = "Wso2ISKeyManagerOne";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String tokenEndpoint = "https://wso2is.com:9444/oauth2/token";
        String revokeEndpoint = "https://wso2is.com:9444/oauth2/revoke";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Username", "admin");
        jsonObject.addProperty("Password", "admin");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
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
        restAPIAdmin.deleteKeyManager(keyManagerId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with WSO2IS type without a mandatory parameter",
            dependsOnMethods = "testAddKeyManagerWithWso2IS")
    public void testAddKeyManagerWithWso2ISWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type without Connector Configurations (Mandatory parameter)
        String name = "Wso2ISKeyManagerTwo";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String tokenEndpoint = "https://wso2is.com:9444/oauth2/token";
        String revokeEndpoint = "https://wso2is.com:9444/oauth2/revoke";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - username and password are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);

        //Add the WSO2 IS key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with WSO2IS type with mandatory " +
            "and some optional parameters", dependsOnMethods = "testAddKeyManagerWithWso2ISWithoutMandatoryParam")
    public void testAddKeyManagerWithWso2ISWithOptionalParams() throws Exception {
        //Create the key manager DTO with WSO2 IS key manager type with mandatory and some optional parameters
        String name = "Wso2ISKeyManagerThree";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String tokenEndpoint = "https://wso2is.com:9444/oauth2/token";
        String revokeEndpoint = "https://wso2is.com:9444/oauth2/revoke";
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
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
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

    @Test(groups = {"wso2.am"}, description = "Test get key manager with Wso2IS type",
            dependsOnMethods = "testAddKeyManagerWithWso2ISWithOptionalParams")
    public void testGetKeyManagerWithWso2IS() throws Exception {
        //Get the added Key manager
        String keyManagerId = keyManagerDTO.getId();
        ApiResponse<KeyManagerDTO> retrievedKeyManager = restAPIAdmin.getKeyManager(keyManagerId);
        KeyManagerDTO retrievedKeyManagerDTO = retrievedKeyManager.getData();
        Assert.assertEquals(retrievedKeyManager.getStatusCode(), HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Username", "admin");
        jsonObject.addProperty("Password", "*****");
        jsonObject.addProperty("self_validate_jwt", true);
        Object expectedAdditionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Verify the added key manager additional properties
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(expectedAdditionalProperties,
                retrievedKeyManagerDTO.getAdditionalProperties());
        //Verify the added key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, retrievedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update key manager With Wso2IS type",
            dependsOnMethods = "testGetKeyManagerWithWso2IS")
    public void testUpdateKeyManagerWithWso2IS() throws Exception {
        //Update the key manager
        String updatedDescription = "This is a updated test key manager";
        keyManagerDTO.setDescription(updatedDescription);
        ApiResponse<KeyManagerDTO> updatedKeyManager =
                restAPIAdmin.updateKeyManager(keyManagerDTO.getId(), keyManagerDTO);
        KeyManagerDTO updatedKeyManagerDTO = updatedKeyManager.getData();
        Assert.assertEquals(updatedKeyManager.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, updatedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete key manager With Wso2IS type",
            dependsOnMethods = "testUpdateKeyManagerWithWso2IS")
    public void testDeleteKeyManagerWithWso2IS() throws Exception {
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete non existing key manager - not found
        try {
            apiResponse = restAPIAdmin.deleteKeyManager(UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    //3. Keycloak Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with Keycloak type with only mandatory parameters",
            dependsOnMethods = "testDeleteKeyManagerWithWso2IS")
    public void testAddKeyManagerWithKeycloak() throws Exception {
        //Create the key manager DTO with Keycloak key manager type with only Mandatory parameters
        String name = "KeycloakKeyManagerOne";
        String type = "KeyCloak";
        String displayName = "Test Key Manager Keycloak";
        String issuer = "https://localhost:8443/auth/realms/master";
        String introspectionEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token/introspect";
        String clientRegistrationEndpoint = "https://localhost:8443/auth/realms/master/clients-registrations/openid-connect";
        String tokenEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token";
        String revoke = "https://localhost:8443/auth/realms/master/protocol/openid-connect/revoke";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revoke, null, null,
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
        restAPIAdmin.deleteKeyManager(keyManagerId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Keycloak type without a mandatory parameter",
            dependsOnMethods = "testAddKeyManagerWithKeycloak")
    public void testAddKeyManagerWithKeycloakWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with Keycloak key manager type without Connector Configurations (Mandatory parameter)
        String name = "KeycloakKeyManagerTwo";
        String type = "KeyCloak";
        String displayName = "Test Key Manager Keycloak";
        String issuer = "https://localhost:8443/auth/realms/master";
        String introspectionEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token/introspect";
        String clientRegistrationEndpoint = "https://localhost:8443/auth/realms/master/clients-registrations/openid-connect";
        String tokenEndpoint = "https://localhost:8443/auth/realms/master/protocol/openid-connect/token";
        String revoke = "https://localhost:8443/auth/realms/master/protocol/openid-connect/revoke";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id and client secret are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revoke, null, null,
                null, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);
        //Add the Keycloak key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Keycloak type with mandatory " +
            "and some optional parameters", dependsOnMethods = "testAddKeyManagerWithKeycloakWithoutMandatoryParam")
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

    @Test(groups = {"wso2.am"}, description = "Test get key manager with Keycloak type",
            dependsOnMethods = "testAddKeyManagerWithKeycloakWithOptionalParams")
    public void testGetKeyManagerWithKeycloak() throws Exception {
        //Get the added Key manager
        String keyManagerId = keyManagerDTO.getId();
        ApiResponse<KeyManagerDTO> retrievedKeyManager = restAPIAdmin.getKeyManager(keyManagerId);
        KeyManagerDTO retrievedKeyManagerDTO = retrievedKeyManager.getData();
        Assert.assertEquals(retrievedKeyManager.getStatusCode(), HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "*****");
        jsonObject.addProperty("self_validate_jwt", true);
        Object expectedAdditionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Verify the added key manager additional properties
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(expectedAdditionalProperties,
                retrievedKeyManagerDTO.getAdditionalProperties());
        //Verify the added key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, retrievedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update key manager with Keycloak type",
            dependsOnMethods = "testGetKeyManagerWithKeycloak")
    public void testUpdateKeyManagerWithKeycloak() throws Exception {
        //Update the key manager
        String updatedDescription = "This is a updated test key manager";
        keyManagerDTO.setDescription(updatedDescription);
        ApiResponse<KeyManagerDTO> updatedKeyManager =
                restAPIAdmin.updateKeyManager(keyManagerDTO.getId(), keyManagerDTO);
        KeyManagerDTO updatedKeyManagerDTO = updatedKeyManager.getData();
        Assert.assertEquals(updatedKeyManager.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, updatedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete key manager With Keycloak type",
            dependsOnMethods = "testUpdateKeyManagerWithKeycloak")
    public void testDeleteKeyManagerWithKeycloak() throws Exception {
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete non existing key manager - not found
        try {
            apiResponse = restAPIAdmin.deleteKeyManager(UUID.randomUUID().toString());
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    //4. Okta Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with Okta type with only mandatory parameters",
            dependsOnMethods = "testDeleteKeyManagerWithKeycloak")
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
        restAPIAdmin.deleteKeyManager(keyManagerId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Okta type without a mandatory parameter",
            dependsOnMethods = "testAddKeyManagerWithOkta")
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
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with Okta type with mandatory " +
            "and some optional parameters", dependsOnMethods = "testAddKeyManagerWithOktaWithoutMandatoryParam")
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

    @Test(groups = {"wso2.am"}, description = "Test get key manager with Okta type",
            dependsOnMethods = "testAddKeyManagerWithOktaWithOptionalParams")
    public void testGetKeyManagerWithOkta() throws Exception {
        //Get the added Key manager
        String keyManagerId = keyManagerDTO.getId();
        ApiResponse<KeyManagerDTO> retrievedKeyManager = restAPIAdmin.getKeyManager(keyManagerId);
        KeyManagerDTO retrievedKeyManagerDTO = retrievedKeyManager.getData();
        Assert.assertEquals(retrievedKeyManager.getStatusCode(), HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("apiKey", "*****");
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "*****");
        jsonObject.addProperty("self_validate_jwt", true);
        Object expectedAdditionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Verify the added key manager additional properties
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(expectedAdditionalProperties,
                retrievedKeyManagerDTO.getAdditionalProperties());
        //Verify the added key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, retrievedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update key manager with Okta type",
            dependsOnMethods = "testGetKeyManagerWithOkta")
    public void testUpdateKeyManagerWithOkta() throws Exception {
        //Update the key manager
        String updatedDescription = "This is a updated test key manager";
        keyManagerDTO.setDescription(updatedDescription);
        ApiResponse<KeyManagerDTO> updatedKeyManager =
                restAPIAdmin.updateKeyManager(keyManagerDTO.getId(), keyManagerDTO);
        KeyManagerDTO updatedKeyManagerDTO = updatedKeyManager.getData();
        Assert.assertEquals(updatedKeyManager.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, updatedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete key manager With Okta type",
            dependsOnMethods = "testUpdateKeyManagerWithOkta")
    public void testDeleteKeyManagerWithOkta() throws Exception {
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete non existing key manager - not found
        try {
            apiResponse = restAPIAdmin.deleteKeyManager(UUID.randomUUID().toString());
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    //5. PingFederate Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with PingFederate type with only mandatory parameters",
            dependsOnMethods = "testDeleteKeyManagerWithOkta")
    public void testAddKeyManagerWithPingFederate() throws Exception {
        //Create the key manager DTO with PingFederate key manager type with only Mandatory parameters
        String name = "PingFederateKeyManagerOne";
        String type = "PingFederate";
        String displayName = "Test Key Manager PingFederate";
        String issuer = "https://localhost:9031";
        String introspectionEndpoint = "https://localhost:9031/as/introspect.oauth2";
        String clientRegistrationEndpoint = "https://localhost:9031/pf-ws/rest/oauth/clients";
        String tokenEndpoint = "https://localhost:9031/as/token.oauth2";
        String revokeEndpoint = "https://localhost:9031/as/revoke.oauth2";
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
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null,
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
        restAPIAdmin.deleteKeyManager(keyManagerId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with PingFederate type without a mandatory parameter",
            dependsOnMethods = "testAddKeyManagerWithPingFederate")
    public void testAddKeyManagerWithPingFederateWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with PingFederate key manager type without Connector Configurations (Mandatory parameter)
        String name = "PingFederateKeyManagerTwo";
        String type = "PingFederate";
        String displayName = "Test Key Manager PingFederate";
        String issuer = "https://localhost:9031";
        String introspectionEndpoint = "https://localhost:9031/as/introspect.oauth2";
        String clientRegistrationEndpoint = "https://localhost:9031/pf-ws/rest/oauth/clients";
        String tokenEndpoint = "https://localhost:9031/as/token.oauth2";
        String revokeEndpoint = "https://localhost:9031/as/revoke.oauth2";
        String consumerKeyClaim = "client_id_name";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id, client secret, username and password are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null,
                null, null, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, null);
        //Add the PingFederate key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with PingFederate type with mandatory " +
            "and some optional parameters", dependsOnMethods = "testAddKeyManagerWithPingFederateWithoutMandatoryParam")
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

    @Test(groups = {"wso2.am"}, description = "Test get key manager with PingFederate type",
            dependsOnMethods = "testAddKeyManagerWithPingFederateWithOptionalParams")
    public void testGetKeyManagerWithPingFederate() throws Exception {
        //Get the added Key manager
        String keyManagerId = keyManagerDTO.getId();
        ApiResponse<KeyManagerDTO> retrievedKeyManager = restAPIAdmin.getKeyManager(keyManagerId);
        KeyManagerDTO retrievedKeyManagerDTO = retrievedKeyManager.getData();
        Assert.assertEquals(retrievedKeyManager.getStatusCode(), HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", "admin");
        jsonObject.addProperty("password", "*****");
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "*****");
        jsonObject.addProperty("self_validate_jwt", true);
        Object expectedAdditionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Verify the added key manager additional properties
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(expectedAdditionalProperties,
                retrievedKeyManagerDTO.getAdditionalProperties());
        //Verify the added key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, retrievedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update key manager with PingFederate type",
            dependsOnMethods = "testGetKeyManagerWithPingFederate")
    public void testUpdateKeyManagerWithPingFederate() throws Exception {
        //Update the key manager
        String updatedDescription = "This is a updated test key manager";
        keyManagerDTO.setDescription(updatedDescription);
        ApiResponse<KeyManagerDTO> updatedKeyManager =
                restAPIAdmin.updateKeyManager(keyManagerDTO.getId(), keyManagerDTO);
        KeyManagerDTO updatedKeyManagerDTO = updatedKeyManager.getData();
        Assert.assertEquals(updatedKeyManager.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, updatedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete key manager with PingFederate type",
            dependsOnMethods = "testUpdateKeyManagerWithPingFederate")
    public void testDeleteKeyManagerWithPingFederate() throws Exception {
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete non existing key manager - not found
        try {
            apiResponse = restAPIAdmin.deleteKeyManager(UUID.randomUUID().toString());
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    //6. ForgeRock Key Manager
    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type with only mandatory parameters",
            dependsOnMethods = "testDeleteKeyManagerWithPingFederate")
    public void testAddKeyManagerWithForgeRock() throws Exception {
        //Create the key manager DTO with ForgeRock key manager type with only Mandatory parameters
        String name = "ForgeRockKeyManagerOne";
        String type = "Forgerock";
        String displayName = "Test Key Manager ForgeRock";
        String issuer = "http://localhost:8080/openam/oauth2";
        String introspectionEndpoint = "http://localhost:8080/openam/oauth2/introspect";
        String clientRegistrationEndpoint = "http://localhost:8080/openam/oauth2/register";
        String tokenEndpoint = "http://localhost:8080/openam/oauth2/access_token";
        String revokeEndpoint = "http://localhost:8080/openam/oauth2/revoke_token";
        String consumerKeyClaim = "aud";
        String scopesClaim = "scp";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "clientSecretValue");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null,
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
        restAPIAdmin.deleteKeyManager(keyManagerId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type without a mandatory parameter",
            dependsOnMethods = "testAddKeyManagerWithForgeRock")
    public void testAddKeyManagerWithForgeRockWithoutMandatoryParam() throws Exception {
        //Create the key manager DTO with ForgeRock key manager type without Connector Configurations (Mandatory parameter)
        String name = "ForgeRockKeyManagerTwo";
        String type = "Forgerock";
        String displayName = "Test Key Manager ForgeRock";
        String issuer = "http://localhost:8080/openam/oauth2";
        String introspectionEndpoint = "http://localhost:8080/openam/oauth2/introspect";
        String clientRegistrationEndpoint = "http://localhost:8080/openam/oauth2/register";
        String tokenEndpoint = "http://localhost:8080/openam/oauth2/access_token";
        String revokeEndpoint = "http://localhost:8080/openam/oauth2/revoke_token";
        String consumerKeyClaim = "aud";
        String scopesClaim = "scp";
        List<String> availableGrantTypes = Collections.emptyList();
        //Connector Configurations - client Id and client secret are not provided
        JsonObject jsonObject = new JsonObject();
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                issuer, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null,
                null, null, consumerKeyClaim, scopesClaim, availableGrantTypes,
                additionalProperties, null);
        //Add the ForgeRock key manager - This should fail
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type with mandatory " +
            "and some optional parameters", dependsOnMethods = "testAddKeyManagerWithForgeRockWithoutMandatoryParam")
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
        waitForKeyManagerDeployment(user.getUserDomain(), keyManagerDTO.getName());
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with ForgeRock type with only mandatory parameters",
            dependsOnMethods = "testAddKeyManagerWithForgeRockWithOptionalParams")
    public void testGetKeyManagerWithForgeRock() throws Exception {
        //Get the added Key manager
        String keyManagerId = keyManagerDTO.getId();
        ApiResponse<KeyManagerDTO> retrievedKeyManager = restAPIAdmin.getKeyManager(keyManagerId);
        KeyManagerDTO retrievedKeyManagerDTO = retrievedKeyManager.getData();
        Assert.assertEquals(retrievedKeyManager.getStatusCode(), HttpStatus.SC_OK);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", "clientIdValue");
        jsonObject.addProperty("client_secret", "*****");
        jsonObject.addProperty("self_validate_jwt", true);
        Object expectedAdditionalProperties = new Gson().fromJson(jsonObject, Map.class);
        //Verify the added key manager additional properties
        adminApiTestHelper.verifyKeyManagerAdditionalProperties(expectedAdditionalProperties,
                retrievedKeyManagerDTO.getAdditionalProperties());
        //Verify the added key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, retrievedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update key manager with ForgeRock type",
            dependsOnMethods = "testGetKeyManagerWithForgeRock")
    public void testUpdateKeyManagerWithForgeRock() throws Exception {
        //Update the key manager
        String updatedDescription = "This is a updated test key manager";
        keyManagerDTO.setDescription(updatedDescription);
        ApiResponse<KeyManagerDTO> updatedKeyManager =
                restAPIAdmin.updateKeyManager(keyManagerDTO.getId(), keyManagerDTO);
        KeyManagerDTO updatedKeyManagerDTO = updatedKeyManager.getData();
        Assert.assertEquals(updatedKeyManager.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated key manager DTO
        adminApiTestHelper.verifyKeyManagerDTO(keyManagerDTO, updatedKeyManagerDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete key manager with ForgeRock type",
            dependsOnMethods = "testAddKeyManagerWithExistingKeyManagerName")
    public void testDeleteKeyManagerWithForgeRock() throws Exception {
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteKeyManager(keyManagerDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete non existing key manager - not found
        try {
            apiResponse = restAPIAdmin.deleteKeyManager(UUID.randomUUID().toString());
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
        waitForKeyManagerUnDeployment(user.getUserDomain(), keyManagerDTO.getName());
    }

    @Test(groups = {"wso2.am"}, description = "Test add key manager with existing key manager name",
            dependsOnMethods = "testUpdateKeyManagerWithForgeRock")
    public void testAddKeyManagerWithExistingKeyManagerName() throws ApiException {
        //Exception occurs when adding a key manager with an existing key manager name. The status code
        //in the Exception object is used to assert this scenario
        try {
            restAPIAdmin.addKeyManager(keyManagerDTO);
            Assert.fail();
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test key manager permissions with WSO2IS with permissions"
            ,dependsOnMethods = "testDeleteKeyManagerWithAuth0")
    public void testKeyManagerPermissions() throws Exception {

        String providerName = user.getUserName();

        APIRequest apiRequest;
        apiRequest = new APIRequest("KMPermissionTestAPI", "KMPermissionTest", new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(providerName);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);

        //add KMPermissionTestAPI api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        //publish KMPermissionTestAPI api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        String name = "Wso2ISKeyManagerWithPermission";
        String type = "WSO2-IS";
        String displayName = "Test Key Manager Permissions WSO2IS";
        String introspectionEndpoint = "https://localhost:9444/oauth2/introspect";
        String clientRegistrationEndpoint = "https://localhost:9444/keymanager-operations/dcr/register";
        String scopeManagementEndpoint = "https://wso2is.com:9444/api/identity/oauth2/v1.0/scopes";
        String tokenEndpoint = "https://wso2is.com:9444/oauth2/token";
        String revokeEndpoint = "https://wso2is.com:9444/oauth2/revoke";
        String consumerKeyClaim = "azp";
        String scopesClaim = "scope";
        List<String> availableGrantTypes = Collections.emptyList();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Username", "admin");
        jsonObject.addProperty("Password", "admin");
        jsonObject.addProperty("self_validate_jwt", true);
        Object additionalProperties = new Gson().fromJson(jsonObject, Map.class);
        List<String> rolesList = new ArrayList<>();
        rolesList.add(API_SUBSCRIBER);
        KeyManagerPermissionsDTO keyManagerPermissionsDTO = new KeyManagerPermissionsDTO();
        keyManagerPermissionsDTO.setPermissionType(KeyManagerPermissionsDTO.PermissionTypeEnum.DENY);
        keyManagerPermissionsDTO.setRoles(rolesList);
        keyManagerDTO = DtoFactory.createKeyManagerDTO(name, null, type, displayName, introspectionEndpoint,
                null, clientRegistrationEndpoint, tokenEndpoint, revokeEndpoint, null, null,
                scopeManagementEndpoint, consumerKeyClaim, scopesClaim, availableGrantTypes, additionalProperties,
                null);
        keyManagerDTO.setPermissions(keyManagerPermissionsDTO);

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
        restAPIStore = new RestAPIStoreImpl(USER_TEST, USER_TEST_PASSWORD,
                this.storeContext.getContextTenant().getDomain(), this.storeURLHttps);
        HttpResponse applicationResponse = restAPIStore.createApplication("KMPermissionApplication7",
                "KMPermissionTestApp", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), org.apache.commons.httpclient.HttpStatus.SC_OK, "Response code is not as expected");
        appId = applicationResponse.getData();

        SubscriptionDTO subscriptionDto = restAPIStore.subscribeToAPI(apiId, appId, APIMIntegrationConstants.API_TIER.GOLD);

        org.wso2.am.integration.clients.store.api.ApiResponse<ApplicationKeyDTO> generateKeyResponse;
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        try {
            generateKeyResponse = restAPIStore.generateKeysWithApiResponse(appId, "3600", null,
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                    grantTypes, null, keyManagerId);
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
        }
        restAPIAdmin.deleteKeyManager(keyManagerId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        userManagementClient.deleteUser(USER_TEST);
        userManagementClient.deleteRole(API_SUBSCRIBER);
        super.cleanUp();
    }
}
