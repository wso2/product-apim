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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

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
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE = "GET";
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
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        option.addHeader("Origin", "http://localhost");
        HttpResponse response = httpclient.execute(option);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckCORSHeadersInPreFlightResponse");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_METHODS_HEADER_ALL_VALUES,
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                    ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                    "but it should not be.");
    }

    @Test(groups = {"wso2.am"}, description = "Checking CORS headers in response",
            dependsOnMethods = "CheckCORSHeadersInPreFlightResponse")
    public void CheckCORSHeadersInResponse() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        get.addHeader("Origin", "http://localhost");
        get.addHeader("Authorization", "Bearer " + accessToken);

        HttpResponse response = httpclient.execute(get);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckCORSHeadersInResponse");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
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
