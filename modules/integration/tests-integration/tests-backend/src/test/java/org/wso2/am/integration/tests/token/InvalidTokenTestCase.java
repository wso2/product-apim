/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.token;

import junit.framework.Assert;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Test case for testing for errors when accessing an API using an invalid access token
 */
public class InvalidTokenTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(InvalidTokenTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private String provider;

    private static final String API_NAME = "InvalidTokenAPI";

    private static final String API_VERSION = "1.0.0";

    @Factory(dataProvider = "userModeDataProvider")
    public InvalidTokenTestCase(TestUserMode userMode) {
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
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        provider = user.getUserName();
    }

    @Test(groups = "wso2.am", description = "Check functionality of API access with invalid token")
    public void testAPIAccessWithInvalidToken() throws XPathExpressionException, APIManagerIntegrationTestException {

        //Login to the API Publisher
        apiPublisher.login(user.getUserName(),
                user.getPassword());

        // Adding API
        String apiContext = "invalidtokenapi";
        String endpointUrl = "http://localhost:8280/response";

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(API_NAME, apiContext, new URL(endpointUrl));
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl, e);
            //Fail the test case
            assertTrue(false);
        }
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");
        apiRequest.setProvider(provider);

        apiPublisher.addAPI(apiRequest);

        //publishing API
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        String  apiInvocationUrl = getAPIInvocationURLHttp(apiContext, API_VERSION);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + "abcdefgh");

        try {
            HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, headers);
            //Fail test if response is null
            Assert.assertNotNull(httpResponse);
            //Fail test if response code != 401
            Assert.assertEquals(401, httpResponse.getResponseCode());

            String responsePayload = httpResponse.getData();
            Assert.assertNotNull(responsePayload);
            OMElement element = AXIOMUtil.stringToOM(responsePayload);
            AXIOMXPath xpath = new AXIOMXPath("/ams:fault/ams:description");
            xpath.addNamespace("ams", "http://wso2.org/apimanager/security");
            Object descriptionElement = xpath.selectSingleNode(element);
            Assert.assertNotNull("Error message doesn't contain a 'description'", descriptionElement);
            String description = ((OMElement)descriptionElement).getText();
            Assert.assertTrue("Unexpected error response string. Expected to have 'Make sure you have given the " +
                            "correct access token' but received '" + description + "'",
                    description.contains("Make sure you have given the correct access token"));
        } catch (IOException e) {
            log.error("Error sending request to endpoint " + apiInvocationUrl, e);
            Assert.assertTrue("Could not send request to endpoint " + apiInvocationUrl + ": " + e.getMessage(), false);
        } catch (XMLStreamException e) {
            log.error("Error parsing response XML ", e);
            Assert.assertTrue("Error parsing response XML " + e.getMessage(), false);
        } catch (JaxenException e) {
            log.error("XPath error when searching for 'description' element ", e);
            Assert.assertTrue("XPath error when searching for 'description' element " + e.getMessage(), false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (apiPublisher != null) {
            apiPublisher.deleteAPI(API_NAME, API_VERSION, provider);
        }

        super.cleanUp();
    }
}
