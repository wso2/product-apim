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
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
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

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test case for testing for errors when accessing an API using an invalid access token
 */
public class InvalidTokenTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(InvalidTokenTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private String provider;

    private String id;
    private String id2;

    private static final String API_NAME = "InvalidTokenAPI";
    private static final String API_NAME_2 = "InvalidBearerTokenAPI";

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
    public void testAPIAccessWithInvalidToken()
            throws XPathExpressionException, APIManagerIntegrationTestException, ApiException {

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

        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        assertNotNull("API Creation failed", response.getData());
        id = response.getData();
        //publishing API
        restAPIPublisher.changeAPILifeCycleStatus(id, APILifeCycleAction.PUBLISH.getAction(), null);

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
            Assert.assertTrue("Unexpected error response string. Expected to have 'Make sure you have " +
                            "provided the correct security credentials' but received '" + description + "'",
                    description.contains("Make sure you have provided the correct security credentials"));
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

    @Test(groups = "wso2.am", description = "Check functionality of API access with invalid bearer token")
    public void testAPIWithInvalidBearerToken()
            throws XPathExpressionException, APIManagerIntegrationTestException, ApiException {

        // Adding API
        String apiContext2 = "invalidbearertokenapi";
        String endpointUrl2 = "http://localhost:8280/response";

        //Create the api creation request object
        APIRequest apiRequest2 = null;
        try {
            apiRequest2 = new APIRequest(API_NAME_2, apiContext2, new URL(endpointUrl2));
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl2, e);
            //Fail the test case
            assertTrue(false);
        }
        apiRequest2.setVersion(API_VERSION);
        apiRequest2.setTiersCollection("Unlimited");
        apiRequest2.setTier("Unlimited");
        apiRequest2.setProvider(provider);

        HttpResponse response2 = restAPIPublisher.addAPI(apiRequest2);
        assertNotNull("API Creation failed", response2.getData());
        id2 = response2.getData();
        //publishing API
        restAPIPublisher.changeAPILifeCycleStatus(id2, APILifeCycleAction.PUBLISH.getAction(), null);

        waitForAPIDeploymentSync(apiRequest2.getProvider(), apiRequest2.getName(), apiRequest2.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext2, API_VERSION);

        try {

            //Case 01 -  Authorization header = Bearer header.
            Map<String, String> headerCase1 = new HashMap<String, String>();
            headerCase1.put("Authorization", "Bearer " + "adbsejdb.");
            HttpResponse httpResponseScenario1 = HttpRequestUtil.doGet(apiInvocationUrl, headerCase1);
            //Fail test if response is null
            Assert.assertNotNull(httpResponseScenario1);
            //Fail test if response code != 401
            Assert.assertEquals(401, httpResponseScenario1.getResponseCode());
            String responsePayloadCase1 = httpResponseScenario1.getData();
            Assert.assertNotNull(responsePayloadCase1);
            OMElement elementCase1 = AXIOMUtil.stringToOM(responsePayloadCase1);
            AXIOMXPath xpathCase1 = new AXIOMXPath("/ams:fault/ams:description");
            xpathCase1.addNamespace("ams", "http://wso2.org/apimanager/security");
            Object descriptionElementCase1 = xpathCase1.selectSingleNode(elementCase1);
            Assert.assertNotNull("Error message doesn't contain a 'description'", descriptionElementCase1);
            String descriptionCase1 = ((OMElement) descriptionElementCase1).getText();
            Assert.assertTrue(
                    "Unexpected error response string. Expected to have 'Invalid JWT token. Make sure you " +
                            "have provided the correct security credentials' but received '" + descriptionCase1 + "'",
                    descriptionCase1.contains(
                            "Invalid JWT token. Make sure you have provided the correct security credentials"));

            //Case 02 -  Authorization header = Bearer header.payload
            Map<String, String> headerCase2 = new HashMap<String, String>();
            headerCase2.put("Authorization", "Bearer " + "adbsejdb.pqrs");
            HttpResponse httpResponseScenario2 = HttpRequestUtil.doGet(apiInvocationUrl, headerCase2);
            //Fail test if response is null
            Assert.assertNotNull(httpResponseScenario2);
            //Fail test if response code != 401
            Assert.assertEquals(401, httpResponseScenario2.getResponseCode());
            String responsePayloadCase2 = httpResponseScenario2.getData();
            Assert.assertNotNull(responsePayloadCase2);
            OMElement elementCase2 = AXIOMUtil.stringToOM(responsePayloadCase2);
            AXIOMXPath xpathCase2 = new AXIOMXPath("/ams:fault/ams:description");
            xpathCase2.addNamespace("ams", "http://wso2.org/apimanager/security");
            Object descriptionElementCase2 = xpathCase2.selectSingleNode(elementCase2);
            Assert.assertNotNull("Error message doesn't contain a 'description'", descriptionElementCase2);
            String descriptionCase2 = ((OMElement) descriptionElementCase2).getText();
            Assert.assertTrue(
                    "Unexpected error response string. Expected to have 'Invalid JWT token. Make sure you " +
                            "have provided the correct security credentials' but received '" + descriptionCase2 + "'",
                    descriptionCase2.contains(
                            "Invalid JWT token. Make sure you have provided the correct security credentials"));

            //Case 03 -  Authorization header = Bearer header.payload.
            Map<String, String> headerCase3 = new HashMap<String, String>();
            headerCase3.put("Authorization", "Bearer " + "adbsejdb.pqrst.");
            HttpResponse httpResponseScenario3 = HttpRequestUtil.doGet(apiInvocationUrl, headerCase3);
            //Fail test if response is null
            Assert.assertNotNull(httpResponseScenario3);
            //Fail test if response code != 401
            Assert.assertEquals(401, httpResponseScenario3.getResponseCode());
            String responsePayloadCase3 = httpResponseScenario3.getData();
            Assert.assertNotNull(responsePayloadCase3);
            OMElement elementCase3 = AXIOMUtil.stringToOM(responsePayloadCase3);
            AXIOMXPath xpathCase3 = new AXIOMXPath("/ams:fault/ams:description");
            xpathCase3.addNamespace("ams", "http://wso2.org/apimanager/security");
            Object descriptionElementCase3 = xpathCase3.selectSingleNode(elementCase3);
            Assert.assertNotNull("Error message doesn't contain a 'description'", descriptionElementCase3);
            String descriptionCase3 = ((OMElement) descriptionElementCase3).getText();
            Assert.assertTrue(
                    "Unexpected error response string. Expected to have 'Invalid JWT token. Make sure you " +
                            "have provided the correct security credentials' but received '" + descriptionCase3 + "'",
                    descriptionCase3.contains(
                            "Invalid JWT token. Make sure you have provided the correct security credentials"));

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
        restAPIPublisher.deleteAPI(id);
        restAPIPublisher.deleteAPI(id2);
        super.cleanUp();
    }
}
