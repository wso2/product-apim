/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.application;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceException;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceStub;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;

import javax.xml.xpath.XPathExpressionException;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This test case is used to test Auth application update
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class AuthApplicationUpdateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(AuthApplicationUpdateTestCase.class);
    private final String APP_NAME = "AuthApplicationUpdateApp";
    private final String APP_NAME_TO_UPDATE = "AuthApplicationNameToUpdateApp";
    private final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private final String GRANT_TYPE_PASSWORD = "password";
    private final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private final String GRANT_TYPE_IMPLICIT = "implicit";
    private final String APP_DESCRIPTION = "description";
    private final String APP_CALLBACK_URL = "http://wso2.com/";
    private final String UPDATE_APP_CALLBACK_URL = "https://www.google.lk/";

    private String storeURLHttp;
    private APIStoreRestClient apiStore;
    private String consumerKey;

    @Factory(dataProvider = "userModeDataProvider")
    public AuthApplicationUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        storeURLHttp = getStoreURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = { "wso2.am" }, description = "Sample Application creation")
    public void testApplicationCreation() throws Exception {
        apiStore.addApplication(APP_NAME, APIMIntegrationConstants.APPLICATION_TIER.LARGE, APP_CALLBACK_URL,
                APP_DESCRIPTION);
        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
        HttpResponse response = apiStore.generateApplicationKey(generateAppKeyRequest);
        verifyResponse(response);
        String responseString = response.getData();
        consumerKey = getConsumerKey(responseString);
        Assert.assertNotNull(consumerKey);
        OAuthConsumerAppDTO authApp = getAuthAppDetails(consumerKey);
        String grantTypes = authApp.getGrantTypes();
        Assert.assertTrue(grantTypes.contains(GRANT_TYPE_AUTHORIZATION_CODE));
        Assert.assertTrue(grantTypes.contains(GRANT_TYPE_CLIENT_CREDENTIALS));
        Assert.assertTrue(grantTypes.contains(GRANT_TYPE_PASSWORD));
        Assert.assertTrue(grantTypes.contains(GRANT_TYPE_REFRESH_TOKEN));
        Assert.assertTrue(grantTypes.contains(GRANT_TYPE_IMPLICIT));
    }

    @Test(groups = {
            "wso2.am" }, description = "Sample Auth Application update", dependsOnMethods = "testApplicationCreation")
    public void testApplicationUpdate() throws Exception {

        String application = APP_NAME;
        String keyType = "PRODUCTION";
        String authorizedDomains = "ALL";
        String retryAfterFailure = String.valueOf(false);
        String jsonParams = "{\"grant_types\":\"urn:ietf:params:oauth:grant-type:saml2-bearer,iwa:ntlm\"}";

        String callbackUrl = UPDATE_APP_CALLBACK_URL;
        HttpResponse response = apiStore
                .updateClientApplication(application, keyType, authorizedDomains, retryAfterFailure, jsonParams,
                        callbackUrl);
        verifyResponse(response);
    }

    @Test(groups = { "wso2.am" }, description = "Sample Auth Application Properties check",
            dependsOnMethods = "testApplicationUpdate")
    public void testApplicationGrantTypeAfterUpdate() throws Exception {
        OAuthConsumerAppDTO authApp = getAuthAppDetails(consumerKey);
        String grantTypes = authApp.getGrantTypes();
        Assert.assertNotNull(grantTypes);
        Assert.assertFalse(grantTypes.contains(GRANT_TYPE_AUTHORIZATION_CODE));
        Assert.assertFalse(grantTypes.contains(GRANT_TYPE_CLIENT_CREDENTIALS));
        Assert.assertFalse(grantTypes.contains(GRANT_TYPE_PASSWORD));
        Assert.assertFalse(grantTypes.contains(GRANT_TYPE_REFRESH_TOKEN));
        Assert.assertFalse(grantTypes.contains(GRANT_TYPE_IMPLICIT));
        Assert.assertEquals(authApp.getCallbackUrl(), UPDATE_APP_CALLBACK_URL);
    }

    @Test(groups = { "wso2.am" }, description = "Test Application name update after key generate",
            dependsOnMethods = "testApplicationGrantTypeAfterUpdate")
    public void testApplicationUpdateAndTestKeyGeneration() throws Exception {
        HttpResponse response = apiStore
                .updateApplication(APP_NAME, APP_NAME_TO_UPDATE, UPDATE_APP_CALLBACK_URL, APP_DESCRIPTION,
                        APIMIntegrationConstants.APPLICATION_TIER.LARGE);
        verifyResponse(response);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(APP_NAME);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    /**
     * get the consumer key from the key generated response
     *
     * @param response key generated response
     * @return consumer key
     */
    private String getConsumerKey(String response) {
        Pattern pattern = Pattern.compile("\"consumerKey\" : \"(\\w|\\d)+\"");
        Matcher matcher = pattern.matcher(response);
        String key = null;
        if (matcher.find()) {
            key = matcher.group(0).split(":")[1].trim().replace("\"", "");
        }
        return key;
    }

    /**
     * Invoke OAuthAdminService admin service for get the Auth application details
     *
     * @param consumerKey Auth application consumer key
     * @return return OAuthConsumerAppDTO
     * @throws RemoteException            occur if connection error occurred
     * @throws OAuthAdminServiceException occur if OAuthAdminService invocation error occurred
     * @throws XPathExpressionException   occurred if xpath evaluation occurred
     */
    private OAuthConsumerAppDTO getAuthAppDetails(String consumerKey)
            throws RemoteException, OAuthAdminServiceException, XPathExpressionException {

        OAuthAdminServiceStub stub = new OAuthAdminServiceStub(getKeyManagerURLHttps() + "services/OAuthAdminService");
        ServiceClient client = stub._getServiceClient();
        Options client_options = client.getOptions();
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(user.getUserName());
        authenticator.setPassword(user.getPassword());
        authenticator.setPreemptiveAuthentication(true);
        client_options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
        client.setOptions(client_options);

        return stub.getOAuthApplicationData(consumerKey);
    }
}
