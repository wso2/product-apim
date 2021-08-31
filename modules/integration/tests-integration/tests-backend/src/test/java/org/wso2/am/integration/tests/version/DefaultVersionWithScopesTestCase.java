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

package org.wso2.am.integration.tests.version;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class DefaultVersionWithScopesTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(DefaultVersionWithScopesTestCase.class);

    private static final String API_NAME = "DefaultVersionScopeAPI";

    private static final String API_VERSION = "1.0.0";

    private static final String APP_NAME = "DefVersionScopeApp";

    private static final String USER_SAM = "sam";

    private static final String USER_MIKE = "mike";

    private static final String SUBSCRIBER_ROLE = "subscriber";

    private String apiId;
    private String applicationID;

    @Factory(dataProvider = "userModeDataProvider")
    public DefaultVersionWithScopesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = "wso2.am", description = "Check functionality of the default version API with scopes")
    public void testDefaultVersionAPIWithScopes()
            throws XPathExpressionException, APIManagerIntegrationTestException {

        String user_mike_password = "mike123";
        String user_sam_password = "sam123";
        String apiContext = "defaultversionscope";
        String user_scope = "user_scope";
        String endpointUrl = getGatewayURLNhttp() + "response";
        String userAccessToken;
        //Add a user called mike and assign him to the subscriber role.
        try {
            //adding new role subscriber
            userManagementClient.addRole(SUBSCRIBER_ROLE, new String[]{}, new String[]{"/permission/admin/login",
                    "/permission/admin/manage/api/subscribe"});
            //creating user mike
            userManagementClient.addUser(USER_MIKE, user_mike_password, new String[]{}, USER_MIKE);
            //creating user sam
            userManagementClient.addUser(USER_SAM, user_sam_password, new String[]{SUBSCRIBER_ROLE}, USER_SAM);

        } catch (AxisFault axisFault) {
            log.error("Error while creating UserManagementClient " + axisFault.getMessage());
            fail(axisFault.getMessage());
        } catch (RemoteException | UserAdminUserAdminException e) {
            log.error("Error while adding roles or users" + e.getMessage());
            fail(e.getMessage());
        }

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(API_NAME, apiContext, new URL(endpointUrl));
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl, e);
            fail(e.getMessage());
        }

        apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());
        apiRequest.setDefault_version("default_version");
        apiRequest.setDefault_version_checked("true");
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        try {
            HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                    "Default version testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                    ApplicationDTO.TokenTypeEnum.JWT);
            applicationID = applicationResponse.getData();

            apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationID,
                    APIMIntegrationConstants.API_TIER.UNLIMITED);

            String resourcePath = "oas" + File.separator + "v3" + File.separator + "defaultVersionScopes.json";
            String modifiedResource = IOUtils.toString(getClass().getClassLoader()
                            .getResourceAsStream(resourcePath), StandardCharsets.UTF_8);
            restAPIPublisher.updateSwagger(apiId, modifiedResource);

            ArrayList grantTypes = new ArrayList();
            grantTypes.add("client_credentials");
            grantTypes.add("password");

            ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "3600", null,
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
            String consumerKey = applicationKeyDTO.getConsumerKey();
            String consumerSecret = applicationKeyDTO.getConsumerSecret();

            String requestBody = "grant_type=password&username=" + USER_SAM + "@" + storeContext.getContextTenant()
                    .getDomain() + "&password=" + user_sam_password + "&scope=" + user_scope;
            URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
            JSONObject accessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(consumerKey,
                    consumerSecret, requestBody, tokenEndpointURL).getData());
            userAccessToken = accessTokenGenerationResponse.getString("access_token");

            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Authorization", "Bearer " + userAccessToken);

            String apiInvocationUrl = getAPIInvocationURLHttp(apiContext);
            //Accessing GET method without the version in the URL using the token sam received
            HttpResponse response = HttpRequestUtil.doGet(apiInvocationUrl, requestHeaders);
            assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                    USER_SAM + " cannot access the GET Method. Response = "
                            + response.getData());

            //Obtaining user access token for mike, request scope 'user_scope'
            requestBody = "grant_type=password&username=" + USER_MIKE + "@"
                    + storeContext.getContextTenant().getDomain()
                    + "&password=" + user_mike_password + "&scope=" + user_scope;
            accessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(
                    consumerKey, consumerSecret, requestBody, tokenEndpointURL).getData());
            userAccessToken = accessTokenGenerationResponse.getString("access_token");

            requestHeaders = new HashMap<>();
            requestHeaders.put("Authorization", "Bearer " + userAccessToken);

            //Accessing GET method without the version in the URL using the token mike received.
            response = HttpRequestUtil.doGet(apiInvocationUrl, requestHeaders);
            assertEquals(response.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    USER_MIKE + " should receive an HTTP 403 when trying to access"
                            + " the GET resource. But the response code was " + response.getResponseCode());
        }
        //Catching generic Exception since apiPublisher and apiStore classes throw Exception from their methods.
        catch (Exception e) {
            log.error("Error while executing test case " + e.getMessage(), e);
            fail(e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationID);
        for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_SAM);
            userManagementClient.deleteUser(USER_MIKE);
            userManagementClient.deleteRole(SUBSCRIBER_ROLE);
        }
        super.cleanUp();
    }
}
