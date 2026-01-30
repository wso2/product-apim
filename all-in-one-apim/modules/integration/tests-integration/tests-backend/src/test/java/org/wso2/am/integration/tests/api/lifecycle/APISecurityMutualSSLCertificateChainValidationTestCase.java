/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APISecurityMutualSSLCertificateChainValidationTestCase extends APIManagerLifecycleBaseTest {

    private final String rootCertAPI = "rootCertAPI";
    private final String intermediateCertAPI = "intermediateCertAPI";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId1, apiId2;

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public APISecurityMutualSSLCertificateChainValidationTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, IOException, ApiException,
            org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException, AutomationUtilException,
            InterruptedException, JSONException, RemoteUserStoreManagerServiceUserStoreExceptionException,
            UserStoreException {

        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest1 = new APIRequest(rootCertAPI, rootCertAPI, new URL(apiEndPointUrl));
        apiRequest1.setVersion(API_VERSION_1_0_0);
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTags(API_TAGS);
        apiRequest1.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest1.setProvider(user.getUserName());
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest1.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("mutualssl");
        securitySchemes.add("mutualssl_mandatory");
        apiRequest1.setSecurityScheme(securitySchemes);
        apiRequest1.setDefault_version("true");
        apiRequest1.setHttps_checked("https");
        apiRequest1.setHttp_checked(null);
        apiRequest1.setDefault_version_checked("true");
        HttpResponse response1 = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response1.getData();

        String rootCertPath = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "cert_chain_root.cer";
        restAPIPublisher.uploadCertificate(new File(rootCertPath), "cert_chain_root", apiId1,
                APIMIntegrationConstants.API_TIER.UNLIMITED, APIMIntegrationConstants.KEY_TYPE.SANDBOX);
        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);

        APIRequest apiRequest2 = new APIRequest(intermediateCertAPI, intermediateCertAPI, new URL(apiEndPointUrl));
        apiRequest2.setVersion(API_VERSION_1_0_0);
        apiRequest2.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest2.setTags(API_TAGS);
        apiRequest2.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest2.setProvider(user.getUserName());
        apiRequest2.setOperationsDTOS(operationsDTOS);
        apiRequest2.setSecurityScheme(securitySchemes);
        apiRequest2.setDefault_version("true");
        apiRequest2.setHttps_checked("https");
        apiRequest2.setHttp_checked(null);
        apiRequest2.setDefault_version_checked("true");
        HttpResponse response2 = restAPIPublisher.addAPI(apiRequest2);
        apiId2 = response2.getData();

        String intermediateCertPath = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "cert_chain_intermediate.cer";
        restAPIPublisher.uploadCertificate(new File(intermediateCertPath), "cert_chain_intermediate", apiId2,
                APIMIntegrationConstants.API_TIER.UNLIMITED, APIMIntegrationConstants.KEY_TYPE.SANDBOX);
        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);

        waitForAPIDeploymentSync(user.getUserName(), rootCertAPI, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), intermediateCertAPI, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        // wait until certificates loaded
        Thread.sleep(120000);
    }

    @Test(description = "Invoke mutual SSL only API with not supported certificate")
    public void testAPIInvocationWithMutualSSLOnlyAPINegative()
            throws IOException, XPathExpressionException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks",
                getAPIInvocationURLHttps(rootCertAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "test.jks", getAPIInvocationURLHttps(rootCertAPI) + API_END_POINT_METHOD,
                requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
    }

    @Test(description = "API invocation with mutual ssl mandatory", dependsOnMethods = "testAPIInvocationWithMutualSSLOnlyAPINegative")
    public void testAPIInvocationWithMutualSSLMandatory()
            throws IOException, XPathExpressionException, NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, UnrecoverableKeyException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");

        // Using root certificate
        HttpResponse rootCertResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "cert_chain_root.jks",
                getAPIInvocationURLHttps(rootCertAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(rootCertResponse.getResponseCode(), HttpStatus.SC_OK, "Mutual SSL Authentication has not succeed");

        // Using client certificate with certificate chain
        HttpResponse clientCertResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "cert_chain_client.jks",
                getAPIInvocationURLHttps(rootCertAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(clientCertResponse.getResponseCode(), HttpStatus.SC_OK, "Mutual SSL Authentication has not succeed");

        // Using client certificate with head only exported certificate
        HttpResponse headOnlyClientCertResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "cert_chain_client_head_only.jks",
                getAPIInvocationURLHttps(rootCertAPI, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(headOnlyClientCertResponse.getResponseCode(), HttpStatus.SC_OK, "Mutual SSL Authentication has not succeed");

        // For default API version with root certificate
        HttpResponse defaultRootCertResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "cert_chain_root.jks",
                getAPIInvocationURLHttps(rootCertAPI) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(defaultRootCertResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");

        // For default API version with client certificate
        HttpResponse defaultClientCertResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "cert_chain_client.jks",
                getAPIInvocationURLHttps(rootCertAPI) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(defaultClientCertResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
    }
}
