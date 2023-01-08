/*
 * Copyright (c) 2022, WSO2 LLC (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class CorrelationLoggingTest extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(CorrelationLoggingTest.class);
    private String apiId;
    private String applicationId;
    private ServerConfigurationManager serverConfigurationManager;

    private Map<String, String> header = new HashMap<>();
    BufferedReader bufferedReader;

    private final String API_CONTEXT = "correlationloggingtest";
    private final String API_VERSION = "1.0.0";
    private final String APPLICATION_NAME = "CorrelationTestApp";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";
    private final String CORRELATION_CONFIG_PATH = "api/am/devops/v0/config/correlation";
    private final String CORRELATION_ID = "9e3ec6ed-2a37-4b20-8dd4-d5fbc754a7d9";
    private String accessToken;
    private Boolean httpLog,jdbcLog,synapseLog,methodCallsLog,correlationIDLog;

    @Factory(dataProvider = "userModeDataProvider")
    public CorrelationLoggingTest(TestUserMode userMode) throws Exception {
        this.userMode = userMode;
        byte[] encodedBytes = Base64.encodeBase64(RESTAPITestConstants.BASIC_AUTH_HEADER.getBytes(StandardCharsets.UTF_8));
        header.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        header.put("Content-Type", "application/json");
        header.put("Accept","application/json" );
        String correlationLogFilePath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator
                        + "logs" + File.separator + "correlation.log";
        bufferedReader = new BufferedReader(new FileReader(correlationLogFilePath));
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}, new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        AutomationContext superTenantKeyManagerContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        // Create an application
        log.info("Creating an application");
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application for Correlation Logs Test", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Create an API and subscribe to it using created application
        log.info("Creating a test API for CorrelationLoggingTest ");
        APIRequest apiRequest;
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION);
        log.info("Creating an API request ");
        String API_NAME = "CorrelationLoggingTestAPI";
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        String API_TAGS = "testTag1, testTag2, testTag3";
        apiRequest.setTags(API_TAGS);
        apiRequest.setProvider(user.getUserName());
        log.info("Creating an API and Publishing");
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore,
                applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        log.info("Generating keys");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken());
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(groups = {"wso2.am" }, description = "Testing the default correlation configs using the devops API ")
    public void testRetrieveDefaultCorrelationLoggingConfigsTest() throws Exception {

        //Retrieve default correlation logs configs from the GET method of the configs resource in devops API
        HttpResponse loggingResponse =
                HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);

        String expectedResponse = "{\"components\":[{\"name\":\"http\",\"enabled\":\"false\",\"properties\":[]}," +
            "{\"name\":\"jdbc\",\"enabled\":\"false\",\"properties\":[{\"name\":\"deniedThreads\",\"value\":" +
            "[\"MessageDeliveryTaskThreadPool\",\"HumanTaskServer\",\"BPELServer\",\"CarbonDeploymentSchedulerThread\"]}]}," +
            "{\"name\":\"ldap\",\"enabled\":\"false\",\"properties\":[]}," +
            "{\"name\":\"synapse\",\"enabled\":\"false\",\"properties\":[]}," +
            "{\"name\":\"method-calls\",\"enabled\":\"false\",\"properties\":[]}]}";
        Assert.assertEquals(loggingResponse.getData(),expectedResponse);

        String logLine;
        while ((logLine = bufferedReader.readLine()) != null) {}
    }


    @Test(groups = {"wso2.am" }, description = "Testing enabling all correlation configs using the devops API ",
    dependsOnMethods = { "testRetrieveDefaultCorrelationLoggingConfigsTest" })
    public void testEnableAllCorrelationLoggingConfigsTest() throws Exception {

        configureCorrelationLoggingComponent(new String[] {"http", "jdbc", "synapse", "ldap", "method-calls"}, true);

        InvokeTestAPI();
        // Validate Correlation Logs
        String logLine = bufferedReader.readLine();
        log.info(logLine);
        HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);
        Thread.sleep(5000);
        resetAllLogs();
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isSynapseLogLine(logLine) || isHTTPLogLine(logLine) ||
                    isJDBCLogLine(logLine) || isMethodCallsLogLine(logLine) || logLine.contains("Started log handler"));
            if (logLine.contains(CORRELATION_ID)) {
                correlationIDLog = true;
            }
        }
        assertTrue(httpLog && jdbcLog && synapseLog && methodCallsLog && correlationIDLog);

        configureCorrelationLoggingComponent(new String[] {"http", "jdbc", "synapse", "ldap", "method-calls"}, false);
        Thread.sleep(5000);
        while ((logLine = bufferedReader.readLine()) != null) { }
    }


    @Test(groups = {"wso2.am" }, description = "Testing enabling specific correlation configs using the devops API ",
            dependsOnMethods = { "testEnableAllCorrelationLoggingConfigsTest" })
    public void testSpecificCorrelationLoggingConfigsTest() throws Exception {

        //Test HTTP
        log.info("Enabling HTTP component correlation logs");
        configureCorrelationLoggingComponent(new String[]{"http"}, true);
        String logLine;
        InvokeTestAPI();
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isHTTPLogLine(logLine));
        }
        log.info("Disabling HTTP component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "http" }, false);
        Thread.sleep(1000);
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isHTTPLogLine(logLine));
        }

        //test JDBC
        log.info("Enabling JDBC component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "jdbc" }, true);
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isJDBCLogLine(logLine));
        }
        log.info("Disabling JDBC component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "jdbc" }, false);
        Thread.sleep(1000);
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isJDBCLogLine(logLine));
        }

        //test Method-calls
        log.info("Enabling Method-calls component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "method-calls" }, true);
        InvokeTestAPI();
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isMethodCallsLogLine(logLine));
        }
        log.info("Disabling Method-calls component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "method-calls" }, false);
        Thread.sleep(1000);
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isMethodCallsLogLine(logLine));
        }

        //test Synapse
        log.info("Enabling Synapse component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "synapse" }, true);
        InvokeTestAPI();
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isSynapseLogLine(logLine));
        }
        log.info("Disabling Synapse component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "synapse" }, false);
        Thread.sleep(1000);
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isSynapseLogLine(logLine));
        }
    }

    @Test(groups = {"wso2.am" }, description = "Testing persisted correlation component configurations ",
            dependsOnMethods = { "testSpecificCorrelationLoggingConfigsTest" })
    public void testPersistedCorrelationConfigs() throws Exception {
        log.info("Enabling http, method-calls correlation component logs before a restart");
        configureCorrelationLoggingComponent(new String[] { "http", "method-calls" }, true);
        HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);
        InvokeTestAPI();
        String logLine;
        resetAllLogs();
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isHTTPLogLine(logLine) || isMethodCallsLogLine(logLine));
        }

        serverConfigurationManager.restartGracefully();
        Thread.sleep(10000);

        while ((logLine = bufferedReader.readLine()) != null) {
            if (logLine.contains("Started log handler")) {
                break;
            }
        }

        resetAllLogs();
        InvokeTestAPI();
        
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isHTTPLogLine(logLine) || isMethodCallsLogLine(logLine));
        }
        assertTrue(httpLog && methodCallsLog);

        configureCorrelationLoggingComponent(new String[] { "http", "method-calls" }, false);
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(isHTTPLogLine(logLine) || isMethodCallsLogLine(logLine));
        }

        // To check whehther no logs are printing after disabling
        resetAllLogs();
        InvokeTestAPI();
        while ((logLine = bufferedReader.readLine()) != null) {
            assertFalse(isHTTPLogLine(logLine) || isMethodCallsLogLine(logLine));
        }
    }

    private void InvokeTestAPI() throws Exception {
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("activityid", CORRELATION_ID);
        org.apache.http.HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        Thread.sleep(500);
    }

    private void configureCorrelationLoggingComponent(String[] componentNames, Boolean enable) throws Exception {
        String[] DEFAULT_DENIED_THREADS = {"MessageDeliveryTaskThreadPool", "HumanTaskServer",
                "BPELServer", "CarbonDeploymentSchedulerThread"};
        String[] DEFAULT_COMPONENTS  = {"http", "jdbc", "ldap", "synapse", "method-calls"};

        List<String> components = Arrays.asList(componentNames);

        JSONObject payload = new JSONObject();
        JSONArray componentArray = new JSONArray();
        for (String c: DEFAULT_COMPONENTS) {
            JSONObject componentConfigs = new JSONObject();
            componentConfigs.put("name", c);
            if (components.contains(c)) {
                componentConfigs.put("enabled", Boolean.toString(enable));
            } else {
                componentConfigs.put("enabled", "false");
            }

            JSONArray properties = new JSONArray();
            if (c.equals("jdbc")) {
                JSONObject property = new JSONObject();
                JSONArray deniedThreads = new JSONArray();
                property.put("name", "deniedThreads");
                for (String s : DEFAULT_DENIED_THREADS) {
                    deniedThreads.put(s);
                }
                property.put("value", deniedThreads);
                properties.put(property);
            }
            componentConfigs.put("properties", properties);
            componentArray.put(componentConfigs);
        }

        payload.put("components", componentArray);
        HttpResponse httpResponse =  HTTPSClientUtils.doPut(getStoreURLHttps() + CORRELATION_CONFIG_PATH,
                header, payload.toString());
        assertEquals(httpResponse.getData(), payload.toString());

        httpResponse =
                HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);
        assertEquals(httpResponse.getData(), payload.toString());
        Thread.sleep(1000);
    }


    private boolean isSynapseLogLine(String logLine) {
        Boolean status =  (logLine.contains("HTTP State Transition")   ||  logLine.contains("ROUND-TRIP LATENCY")  ||
                logLine.contains("Thread switch latency")   ||  logLine.contains("BACKEND LATENCY"));
        if (status) {
            synapseLog = true;
        }
        return status;
    }

    private boolean isHTTPLogLine(String logLine) {
        Boolean status =  (logLine.contains("HTTP-In-"));
        if (status) {
            httpLog = true;
        }
        return status;
    }

    private boolean isJDBCLogLine(String logLine) {
        Boolean status =  (logLine.contains("jdbc"));
        if (status) {
            jdbcLog = true;
        }
        return status;
    }

    private boolean isMethodCallsLogLine(String logLine) {
        boolean status =  (logLine.contains("METHOD") || logLine.contains(APPLICATION_NAME));
        if (status) {
            methodCallsLog = true;
        }
        return status;
    }

    private void resetAllLogs() {
        httpLog = false;
        synapseLog = false;
        jdbcLog = false;
        methodCallsLog = false;
        correlationIDLog = false;
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
