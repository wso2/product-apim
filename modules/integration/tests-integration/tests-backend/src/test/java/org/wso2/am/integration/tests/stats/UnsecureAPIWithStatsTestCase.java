/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;

public class UnsecureAPIWithStatsTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(UnsecureAPIWithStatsTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private static final String API_NAME = "UnsecureAPI";

    private static final String API_VERSION = "1.0.0";

    private ServerConfigurationManager serverManager;

    private static final String APIM_CONFIG_XML = "api-manager.xml";

    private static final String DATASOURCE_XML = "master-datasources.xml";

    private static String apiProvider;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();

        apiProvider = publisherContext.getSuperTenant().getContextUser().getUserName();

        String gatewaySessionCookie = createSession(gatewayContextMgt);

        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                File.separator + "artifacts" + File.separator + "AM" + File.separator +
                "configFiles" + File.separator + "stats" + File.separator;

        String apimConfigArtifactLocation = artifactsLocation + APIM_CONFIG_XML;

        String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                File.separator + "conf" + File.separator + APIM_CONFIG_XML;

        String masterDatasourceLocation = artifactsLocation + DATASOURCE_XML;

        String masterDatasourceConfigLocation = carbonHome + File.separator + "repository" +
                File.separator + "conf" + File.separator + "datasources" + File.separator + DATASOURCE_XML;

        File apimConfSourceFile = new File(apimConfigArtifactLocation);
        File apimConfTargetFile = new File(apimRepositoryConfigLocation);

        File dsConfSourceFile = new File(masterDatasourceLocation);
        File dsConfTargetFile = new File(masterDatasourceConfigLocation);

        serverManager = new ServerConfigurationManager(gatewayContextMgt);

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "dummy_api.xml", gatewayContextMgt, gatewaySessionCookie);

        // apply configuration to  api-manager.xml
        serverManager.applyConfigurationWithoutRestart(apimConfSourceFile, apimConfTargetFile, true);
        log.info("api-manager.xml configuration file copied from :" + apimConfigArtifactLocation +
                " to :" + apimRepositoryConfigLocation);

        // apply configuration to  master-datasources.xml
        serverManager.applyConfigurationWithoutRestart(dsConfSourceFile, dsConfTargetFile, true);
        log.info("master-datasources.xml configuration file copied from :" + masterDatasourceLocation +
                " to :" + masterDatasourceConfigLocation);

        serverManager.restartGracefully();

        //Initialize publisher and store.
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
    }

    @Test(groups = "wso2.am", description = "Check if analytics work for non-secured APIs.")
    public void testUnsecureAPIWithStats() {

        // Adding API
        String apiContext = "analyticsapi";
        String endpointUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "response";

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(API_NAME, apiContext, new URL(endpointUrl));
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl, e);
            //Fail the test case
            Assert.assertTrue(false);
        } catch (APIManagerIntegrationTestException e) {
            log.error("Integration Test error occurred ", e);
            //Fail the test case
            Assert.assertTrue(false);
        }
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        try {
            apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                    publisherContext.getContextTenant().getContextUser().getPassword());

            apiPublisher.addAPI(apiRequest);

            //publishing API
            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(
                    API_NAME, apiProvider,
                    APILifeCycleState.PUBLISHED);
            apiPublisher.changeAPILifeCycleStatus(updateRequest);

            //resources are modified using swagger doc.
            // admin_scope(used for POST) :- admin
            // user_scope (used for GET) :- admin,subscriber
            String modifiedResource = "{\"paths\":{ \"/*\":{\"put\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                    "\"x-throttling-tier\":\"Unlimited\" },\"post\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                    "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"admin_scope\"},\"get\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                    "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"user_scope\"},\"delete\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                    "\"x-throttling-tier\":\"Unlimited\"},\"options\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                    "\"x-throttling-tier\":\"Unlimited\"}}},\"swagger\":\"2.0\",\"info\":{\"title\":\"" + API_NAME + "\",\"version\":\"1.0.0\"}," +
                    "\"x-wso2-security\":{\"apim\":{\"x-wso2-scopes\":[{\"name\":\"admin_scope\",\"description\":\"\",\"key\":\"admin_scope\",\"roles\":\"admin\"}," +
                    "{\"name\":\"user_scope\",\"description\":\"\",\"key\":\"user_scope\",\"roles\":\"admin,subscriber\"}]}}}";

            apiPublisher.updateResourceOfAPI(apiProvider, API_NAME, API_VERSION, modifiedResource);

            String apiInvocationUrl;
            if (gatewayContextMgt.getContextTenant().getDomain().equals("carbon.super")) {
                apiInvocationUrl = gatewayUrlsWrk.getWebAppURLNhttp() + apiContext + "/" + API_VERSION;
            } else {
                apiInvocationUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                        gatewayContextMgt.getContextTenant().getDomain() + "/" + apiContext + "/" + API_VERSION;
            }

            HttpResponse directResponse = HttpRequestUtil.doGet(endpointUrl, new HashMap<String, String>());

            //Invoke the API
            HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, new HashMap<String, String>());

            //Check if accessing the back-end directly and accessing it via the API yield the same responses.
            assertEquals(httpResponse.getData(), directResponse.getData(),
                    "Un-secure API test failed since the response from the direct back-end did not match with the " +
                            "response from the server");

        } catch (APIManagerIntegrationTestException e) {
            log.error("Error occurred while executing Test", e);
            //Fail the test case
            Assert.assertTrue(false);
        } catch (XPathExpressionException e) {
            log.error("Error occurred while getting credentials from the publisher/store context ", e);
            //Fail the test case
            Assert.assertTrue(false);
        } catch (IOException e) {
            log.error("IO error occurred when doing an http request");
            //Fail the test case
            Assert.assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiPublisher != null) {
            apiPublisher.deleteAPI(API_NAME, API_VERSION, apiProvider);
        }

        serverManager.restoreToLastConfiguration();
        serverManager.restartGracefully();
        log.info("Restored configuration and restarted gracefully...");
    }

}
