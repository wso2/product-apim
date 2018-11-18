/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.ClientCertificateCreationBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * This class tests the behaviour of API when there is choice of selection between oauth2 and mutual ssl in API Manager.
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE})
public class APISecurityTestCase extends APIManagerLifecycleBaseTest {

    private final String API_CONTEXT = "mutualsslAPI";
    private final String API_CONTEXT_2 = "mutualsslAPI2";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private APIIdentifier apiIdentifier1;
    private APIIdentifier apiIdentifier2;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private ServerConfigurationManager serverConfigurationManager;
    private String accessToken;

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, XPathExpressionException, IOException, AutomationUtilException {
        super.init();
        startServerWithConfigChanges();
        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreURLHttp());
        publishAPI(API_CONTEXT, API_CONTEXT, "", "example.crt");
        publishAPI(API_CONTEXT_2, API_CONTEXT_2, "oauth2", "abcde.crt");

        apiIdentifier1 = new APIIdentifier(user.getUserName(), API_CONTEXT, API_VERSION_1_0_0);
        apiIdentifier2 = new APIIdentifier(user.getUserName(), API_CONTEXT_2, API_VERSION_1_0_0);

        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiStoreClientUser1
                .addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        subscribeToAPI(apiIdentifier2, APPLICATION_NAME, apiStoreClientUser1);
        //get access token
        accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
    }

    @Test(description = "This test case tests the behaviour of APIs that are protected with mutual SSL and OAuth2 "
            + "when the client certificate is not presented but OAuth2 token is presented.")
    public void testCreateAndPublishAPIWithOAuth2() throws XPathExpressionException, IOException, JSONException {
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        JSONObject response = new JSONObject(apiResponse.getData());
        assertEquals(response.getJSONObject("fault").getInt("code"), 900911,
                "API invocation succeeded with the access token without need for mutual ssl");
        apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API invocation failed for a test case with valid access token when the API is protected with "
                        + "both mutual sso and oauth2");
    }

    @Test(description =  "This method tests the behaviour of APIs that are protected with mutual SSL and when the "
            + "authentication is done using mutual SSL", dependsOnMethods = "testCreateAndPublishAPIWithOAuth2")
    public void testAPIInvocationWithMutualSSL()
            throws IOException, XPathExpressionException, InterruptedException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        String expectedResponseData = "<id>123</id><name>John</name></Customer>";
        // We need to wait till the relevant listener reloads.
        Thread.sleep(60000);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        // Check with the correct client certificate for an API that is only protected with mutual ssl.
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeeded");
        Assert.assertTrue(response.getData().contains(expectedResponseData), "Expected payload did not match");
        /* Check with the wrong client certificate for an API that is protected with mutual ssl and oauth2, without
         an access token.*/
        response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Mutual SSL Authentication has succeeded for a different certificate");
        /* Check with the correct client certificate for an API that is protected with mutual ssl and oauth2, without
         an access token.*/
        response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeeded");
        Assert.assertTrue(response.getData().contains(expectedResponseData), "Expected payload did not match");

         /* Check with the wrong client certificate for an API that is protected with mutual ssl and oauth2, with a
         correct access token.*/
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(API_CONTEXT_2, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "OAuth2 authentication was not checked in the event of mutual SSL failure");
        Assert.assertTrue(response.getData().contains(expectedResponseData), "Expected payload did not match");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, IOException, AutomationUtilException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier1, apiPublisherClientUser1);
        deleteAPI(apiIdentifier2, apiPublisherClientUser1);
        serverConfigurationManager.restoreToLastConfiguration(true);
    }

    /**
     * To start the server after making config changes.
     *
     * @throws AutomationUtilException  Automation Util Exception.
     * @throws XPathExpressionException XPath Expression Exception.
     * @throws IOException              IO Exception.
     */
    private void startServerWithConfigChanges() throws AutomationUtilException, XPathExpressionException, IOException {
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "api-manager.xml"));
        String axis2SourceFile =
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "axis2.xml";
        String axis2TargetFile =
                CARBON_HOME + File.separator + "repository" + File.separator + "conf" + File.separator + "axis2"
                        + File.separator + "axis2.xml";
        serverConfigurationManager
                .applyConfigurationWithoutRestart(new File(axis2SourceFile), new File(axis2TargetFile), true);
        serverConfigurationManager.restartGracefully();
    }

    /**
     * To get the base64 encoded certificate.
     *
     * @param certificateFileName Relevant file name of the certificate.
     * @return Base64 encoded certificate.
     */
    private String getBase64EncodedCertificate(String certificateFileName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + certificateFileName);
        StringWriter writer = new StringWriter();
        IOUtils.copy(fileInputStream, writer, StandardCharsets.UTF_8);
        return new String(Base64.encodeBase64(writer.toString().getBytes()));
    }

    /**
     * To publish an API.
     *
     * @param apiName         Name of the API.
     * @param apiContext      API Context.
     * @param oauth2          To indicate whether oauth2 security enabled.
     * @param certificateName Name of the certificate.
     * @throws IOException                        IO Exception.
     * @throws APIManagerIntegrationTestException API Manager Inegration Test Exception.
     */
    private void publishAPI(String apiName, String apiContext, String oauth2, String certificateName)
            throws IOException, APIManagerIntegrationTestException {
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
        String apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        String providerName = user.getUserName();
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext,
                API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setMutualSSLChecked("mutualssl");
        apiCreationRequestBean.setOauth2Checked(oauth2);

        APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
        apiPublisherClientUser1.addAPI(apiCreationRequestBean);
        String certificate = getBase64EncodedCertificate(certificateName);
        ClientCertificateCreationBean clientCertificateCreationBean = new ClientCertificateCreationBean(apiName,
                providerName, API_VERSION_1_0_0, certificate, "Unlimited", apiName);
        apiPublisherClientUser1.uploadCertificate(clientCertificateCreationBean);
        apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
    }
}
