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
 */
package org.wso2.am.integration.tests.header;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test CORS Access-Control-Allow-Credentials functionality
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE })
public class CORSAccessControlAllowCredentialsHeaderTestCase extends APIManagerLifecycleBaseTest {

    private String publisherURLHttp;
    private APIPublisherRestClient apiPublisher;

    private static final String API_NAME_1 = "CorsACACHeadersTestAPI_1";
    private static final String API_NAME_2 = "CorsACACHeadersTestAPI_2";
    private static final String APPLICATION_NAME_1 = "CorsACACApp_1";
    private static final String APPLICATION_NAME_2 = "CorsACACApp_2";
    private static final String API_CONTEXT_1 = "corsACACHeadersTestAPI_1";
    private static final String API_CONTEXT_2 = "corsACACHeadersTestAPI_2";
    private static final String API_VERSION = "1.0.0";
    private static final String TAGS = "ACAC, cors, test";
    private static final String DESCRIPTION = "This is test API create by API manager integration test";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_ALL = "*";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_LOCALHOST = "http://localhost";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";

    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifier;
    private String accessToken;
    private ServerConfigurationManager serverConfigurationManager;

    Log log = LogFactory.getLog(CORSAccessControlAllowCredentialsHeaderTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            String gatewaySessionCookie = createSession(gatewayContextMgt);
            //Load the back-end dummy API
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                                                  + File.separator + "synapseconfigs" + File.separator + "rest"
                                                  + File.separator + "dummy_api.xml", gatewayContextMgt,
                                                  gatewaySessionCookie);
        }

        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
    }

    @Test(groups = {"wso2.am"}, description = "Checking Access-Control-Allow-Credentials header in response " +
                                              "when Access-Control-Allow-Origin is '*'")
    public void CheckAccessControlAllowCredentialsHeadersWithAnyOrigin() throws Exception {

        accessToken = createPublishAndSubscribeToApi(user, API_NAME_1, API_CONTEXT_1, API_VERSION, APPLICATION_NAME_1);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT_1, API_VERSION));
        get.addHeader("Origin", "http://localhost");
        get.addHeader("Authorization", "Bearer " + accessToken);

        org.apache.http.HttpResponse response = httpclient.execute(get);
        List<Integer> responseCodes = new ArrayList<Integer>();
        responseCodes.add(HTTP_RESPONSE_CODE_OK);
        responseCodes.add(HTTP_RESPONSE_CODE_CREATED);
        assertTrue(responseCodes.contains(response.getStatusLine().getStatusCode()), "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckAccessControlAllowCredentialsHeadersWithAnyOrigin");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_ALL,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                   ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                   "but it should not be.");

    }

    @Test(groups = {"wso2.am"}, description = "Checking Access-Control-Allow-Credentials header in response " +
            "when Access-Control-Allow-Origin is 'http://localhost'",
            dependsOnMethods = "CheckAccessControlAllowCredentialsHeadersWithAnyOrigin")
    public void CheckAccessControlAllowCredentialsHeadersWithSpecificOrigin() throws Exception {
        //Enable CORS Access Control Allow Credentials with Origin 'http://localhost'
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                    + File.separator + "configFiles" + File.separator
                    + "corsACACTest" + File.separator + "deployment.toml"));
            serverConfigurationManager.restartGracefully();
            super.init();
        }

        accessToken = createPublishAndSubscribeToApi(user, API_NAME_2, API_CONTEXT_2, API_VERSION, APPLICATION_NAME_2);

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT_2, API_VERSION));
        get.addHeader("Origin", ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_LOCALHOST);
        get.addHeader("Authorization", "Bearer " + accessToken);

        org.apache.http.HttpResponse response = httpclient.execute(get);

        List<Integer> responseCodes = new ArrayList<Integer>();
        responseCodes.add(HTTP_RESPONSE_CODE_OK);
        responseCodes.add(HTTP_RESPONSE_CODE_CREATED);
        assertTrue(responseCodes.contains(response.getStatusLine().getStatusCode()), "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckAccessControlAllowCredentialsHeadersWithAnyOrigin");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE_LOCALHOST,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        assertNotNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                   ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is not available in the response.");
    }

    private String createPublishAndSubscribeToApi(User user, String apiName, String apiContext, String apiVersion,
                                                  String appName)
            throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        publisherURLHttp = getPublisherURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());

        String providerName = user.getUserName();
        URL endpointUrl = new URL(getSuperTenantAPIInvocationURLHttp("response", "1.0.0"));
        ArrayList<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                                                 APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER,
                                                 APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/*"));
        apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, providerName,
                                                            endpointUrl, resourceBeanList);
        apiCreationRequestBean.setTags(TAGS);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, apiName, apiVersion);
        apiIdentifier.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        //Create application
        apiStoreClientUser1.addApplication(appName,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "", "");
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1,
                                       apiStoreClientUser1, appName);
        return generateApplicationKeys(apiStoreClientUser1, appName).getAccessToken();
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                    + File.separator + "configFiles" + File.separator
                    + "corsACACTest" + File.separator + "original" + File.separator + "deployment.toml"));
            serverConfigurationManager.restartGracefully();
        }
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] {TestUserMode.SUPER_TENANT_ADMIN },
                                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CORSAccessControlAllowCredentialsHeaderTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
