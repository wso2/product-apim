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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
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
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test CORS functionality
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL })
public class CORSHeadersTestCase extends APIManagerLifecycleBaseTest {

    private String publisherURLHttp;
    private APIPublisherRestClient apiPublisher;

    private static final String API_NAME = "CorsHeadersTestAPI";
    private static final String APPLICATION_NAME = "CorsHeadersApp";
    private static final String API_CONTEXT = "corsHeadersTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String TAGS = "cors, test";
    private static final String DESCRIPTION = "This is test API create by API manager integration test";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE = "*";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE = "POST";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER_ALL_VALUES = "GET,PUT,POST,DELETE,PATCH,OPTIONS";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE
            = "authorization,Access-Control-Allow-Origin,Content-Type";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";

    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifier;
    private String accessToken;

    Log log = LogFactory.getLog(CORSHeadersTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        //Load the back-end dummy API
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            String gatewaySessionCookie = createSession(gatewayContextMgt);
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                                                  + File.separator + "synapseconfigs" + File.separator + "rest"
                                                  + File.separator + "dummy_api.xml", gatewayContextMgt,
                                                  gatewaySessionCookie);
        }
        publisherURLHttp = getPublisherURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());

        String providerName = user.getUserName();
        URL endpointUrl = new URL(getSuperTenantAPIInvocationURLHttp("response", "1.0.0"));
        ArrayList<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_POST,
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER,
                APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/*"));
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
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
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION);
        apiIdentifier.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.LARGE, "", "");
        accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();

        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1,
                                       apiStoreClientUser1, APPLICATION_NAME);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "Checking CORS headers in pre-flight response")
    public void CheckCORSHeadersInPreFlightResponse() throws Exception {
        URL url = new URL(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("OPTIONS");
        connection.setRequestProperty("Origin", "http://localhost");
        Map<String, List<String>> responseHeaders = connection.getHeaderFields();

        log.info("Response Headers: CheckCORSHeadersInPreFlightResponse");
        for (String header : responseHeaders.keySet()) {
            log.info(header + " : " + responseHeaders.get(header).get(0));
        }

        assertTrue(responseHeaders.containsKey(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                   ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(responseHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER).get(0),
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        assertTrue(responseHeaders.containsKey(ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                   ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
        assertEquals(responseHeaders.get(ACCESS_CONTROL_ALLOW_METHODS_HEADER).get(0),
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER_ALL_VALUES,
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header value mismatch.");

        assertTrue(responseHeaders.containsKey(ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                   ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(responseHeaders.get(ACCESS_CONTROL_ALLOW_HEADERS_HEADER).get(0),
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertFalse(responseHeaders.containsKey(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                    ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                    "but it should not be.");

    }

    @Test(groups = {"wso2.am"}, description = "Checking CORS headers in response",
            dependsOnMethods = "CheckCORSHeadersInPreFlightResponse")
    public void CheckCORSHeadersInResponse() throws Exception {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("Origin", "http://localhost");

        HttpResponse response = HttpRequestUtil.doPost(new URL(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION)), "",
                                                             requestHeaders);
        assertEquals(response.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatch.");

        log.info("Response Headers: CheckCORSHeadersInResponse");
        Map<String, String> headers = response.getHeaders();
        for (String header : headers.keySet()) {
            log.info(header + ":" + headers.get(header));
        }

        assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                   ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(response.getHeaders().get(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER),
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                   ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
        assertEquals(response.getHeaders().get(ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header value mismatch.");

        assertTrue(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                   ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(response.getHeaders().get(ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertFalse(response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                    ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                    "but it should not be.");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] {TestUserMode.SUPER_TENANT_ADMIN },
                                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CORSHeadersTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
