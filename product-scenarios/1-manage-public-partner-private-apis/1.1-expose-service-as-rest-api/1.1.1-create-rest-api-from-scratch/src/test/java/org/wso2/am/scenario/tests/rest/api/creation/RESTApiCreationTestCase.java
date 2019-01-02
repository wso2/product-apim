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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.rmi.RemoteException;
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
    private String endpointAuthType = "basicAuth";
    private String epUsername = "wso2";
    private String epPassword = "wso2123";
    private String default_version_checked = "default_version";
    private String responseCache = "enabled";
    private String cacheTimeout = "300";
    private String subscriptions = "all_tenants";
    private String http_checked = "http";
    private String https_checked = "";
    private String inSequence = "debug_in_flow";
    private String outSequence = "debug_out_flow";

    private String username;
    private String password;
    private String adminUsername;
    private String adminPassword;

    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider") public RESTApiCreationTestCase(String username, String password,
            String adminUsername, String adminPassword) {
        this.username = username;
        this.password = password;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(username, password);
    }

    @Test(description = "1.1.1.1", dataProvider = "apiNames", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPICreationWithMandatoryValues(String apiName) throws Exception {

        apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility,
                apiVersion, apiResource);

        //Design API with name,context,version,visibility and apiResource
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);
        verifyAPIName(apiName, username);
    }

    @Test(description = "1.1.1.2")
    public void testRESTAPICreationWithOptionalValues() throws Exception {
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverifyOptionaladd";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, description, tag,
                tierCollection, backendEndPoint, bizOwner, bizOwnerMail, techOwner, techOwnerMail, endpointType,
                endpointAuthType, epUsername, epPassword, default_version_checked, responseCache, cacheTimeout,
                subscriptions, http_checked, https_checked, inSequence, outSequence);

        //Design API with name,context,version,visibility,apiResource and with all optional values
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        HttpResponse serviceResponseGetApi = apiPublisher.getAPI(apiName, username);
        validateOptionalFiled(serviceResponseGetApi);
    }

    @Test(description = "1.1.1.3")
    public void testRESTAPICreationWithwildCardResource() throws Exception {
        apiName = "APIWildCard";
        apiContext = "apiwildcard";
        apiResource = "/*";

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Design API with name,context,version,visibility and wildcard apiResource
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);
        verifyAPIName(apiName, username);
    }

    private void validateOptionalFiled(HttpResponse response) throws APIManagerIntegrationTestException {
        JSONObject responseJson = new JSONObject(response.getData());
        Assert.assertEquals(responseJson.getJSONObject("api").get("bizOwner").toString(), bizOwner, "Expected bizOwner value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("bizOwnerMail").toString(), bizOwnerMail, "Expected bizOwnerMail value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("techOwner").toString(), techOwner, "Expected techOwner value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("techOwnerMail").toString(), techOwnerMail, "Expected techOwnerMail value not match with the actual value");
        Assert.assertEquals(responseJson.getJSONObject("api").get("endpointTypeSecured").toString(), "true", "Expected endpointType value not match with the actual value");
        Assert.assertEquals(responseJson.getJSONObject("api").get("endpointAuthTypeDigest").toString(), "false", "Expected endpointAuthType value not match with the actual value");
        Assert.assertEquals(responseJson.getJSONObject("api").get("epUsername").toString(), epUsername, "Expected epUsername value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("epPassword").toString(), epPassword, "Expected epPassword value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("isDefaultVersion").toString(), "true", "Expected default_version_checked value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("responseCache").toString(), "Enabled", "Expected responseCache: value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("cacheTimeout").toString(), cacheTimeout, "Expected cacheTimeout value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("subscriptionAvailability").toString(), subscriptions, "Expected subscriptions value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("transport_http").toString(), "checked", "Expected http_checked value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("transport_https").toString(), "", "Expected https_checked value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("inSequence").toString(), inSequence, "Expected inSequence: value not match");
        Assert.assertEquals(responseJson.getJSONObject("api").get("outSequence").toString(), outSequence, "Expected outSequence value not match");

    }

    private void verifyAPIName(String apiName, String provider) throws APIManagerIntegrationTestException {
        HttpResponse getApi = apiPublisher.getAPI(apiName, provider);
        JSONObject response = new JSONObject(getApi.getData());
        Assert.assertEquals(response.getJSONObject("api").get("name").toString(), apiName,
                "Expected API name value not match");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI("PhoneVerification", apiVersion, username);
        apiPublisher.deleteAPI("123567890", apiVersion, username);
        apiPublisher.deleteAPI("eñe", apiVersion, username);
        apiPublisher.deleteAPI("Pho_ne-verify?api.", apiVersion, username);
        apiPublisher.deleteAPI("PhoneVerificationOptionalAdd", apiVersion, username);
        apiPublisher.deleteAPI("APIWildCard", apiVersion, username);

        deleteUser(MultitenantUtils.getTenantAwareUsername(username), adminUsername, adminPassword  );
    }

    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        // create userManagementClients for super tenant and wso2.com tenant
        UserManagementClient userManagementClientSuperTenant = getRemoteUserManagerClient("admin@carbon.super",
                "admin");
        UserManagementClient userManagementClientTenant = getRemoteUserManagerClient(
                "admin@" + ScenarioTestConstants.TENANT_WSO2, "admin");

        //Add and activate wso2.com tenant
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, "admin", "admin");

        // create user in super tenant
        createUserWithCreatorRole("micheal", "Micheal#123", "admin", "admin");

        // create user in wso2.com tenant
        createUserWithCreatorRole("andrew", "Andrew#123", "admin@wso2.com", "admin");

        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][] { { "micheal", "Micheal#123", "admin", "admin"},
                { "andrew@" + ScenarioTestConstants.TENANT_WSO2, "Andrew#123", "admin@wso2.com", "admin" } };
    }
}
