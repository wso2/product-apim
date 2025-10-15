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
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
    private Boolean httpLog, jdbcLog, synapseLog, methodCallsLog, correlationIDLog, logHandlerStarted;

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

        log.info("***testRetrieveDefaultCorrelationLoggingConfigsTest:userMode: " + this.userMode);

        String expectedResponse = "{\"components\":[{\"name\":\"http\",\"enabled\":\"false\",\"properties\":[]}," +
            "{\"name\":\"jdbc\",\"enabled\":\"false\",\"properties\":[{\"name\":\"deniedThreads\",\"value\":" +
            "[\"MessageDeliveryTaskThreadPool\",\"HumanTaskServer\",\"BPELServer\",\"CarbonDeploymentSchedulerThread\"]}]}," +
            "{\"name\":\"ldap\",\"enabled\":\"false\",\"properties\":[]}," +
            "{\"name\":\"synapse\",\"enabled\":\"false\",\"properties\":[]}," +
            "{\"name\":\"method-calls\",\"enabled\":\"false\",\"properties\":[]}]}";

        //Retrieve default correlation logs configs from the GET method of the configs resource in devops API
        HttpResponse loggingResponse =
                HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);
        log.info("***testRetrieveDefaultCorrelationLoggingConfigsTest:actualResponse: " + loggingResponse.getData());
        log.info("***testRetrieveDefaultCorrelationLoggingConfigsTest:expectedResponse: " + expectedResponse);
        Assert.assertEquals(loggingResponse.getData(),expectedResponse);

        String logLine;
        while ((logLine = bufferedReader.readLine()) != null) {}
    }


    @Test(groups = {"wso2.am" }, description = "Testing enabling all correlation configs using the devops API ",
    dependsOnMethods = { "testRetrieveDefaultCorrelationLoggingConfigsTest" })
    public void testEnableAllCorrelationLoggingConfigsTest() throws Exception {

        // Check for jdbc is skipped as it introduces test failures due to frequent jdbc logs in correlation.log file
        configureCorrelationLoggingComponent(new String[] {"http", "synapse", "ldap", "method-calls"}, true);
        log.info("***testEnableAllCorrelationLoggingConfigsTest:userMode: " + this.userMode);

        // Validate Correlation Logs
        /* await() is from Awaitility library which is used to read correlation.log file within a timeout period as
        there is varying delay in file writing */
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            resetAllLogs();
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testEnableAllCorrelationLoggingConfigsTest:logLine: " + logLine);
                isSynapseLogLine(logLine);
                isHTTPLogLine(logLine);
                isMethodCallsLogLine(logLine);
                isCorrelationIDLogLine(logLine);
            }
            log.info(String.format("***testEnableAllCorrelationLoggingConfigsTest:Enable: httpLog:%b, synapseLog:%b, " +
                            "methodCallsLog:%b, correlationIDLog:%b", httpLog, synapseLog, methodCallsLog,
                    correlationIDLog));
            assertTrue(httpLog && synapseLog && methodCallsLog && correlationIDLog);
        });

        // Disabling all correlation log components for next tests
        configureCorrelationLoggingComponent(new String[] {"http", "jdbc", "synapse", "ldap", "method-calls"}, false);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            resetAllLogs();
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testEnableAllCorrelationLoggingConfigsTest:logLine: " + logLine);
                isSynapseLogLine(logLine);
                isJDBCLogLine(logLine);
                isHTTPLogLine(logLine);
                isMethodCallsLogLine(logLine);
                isCorrelationIDLogLine(logLine);
            }
            log.info(String.format("***testEnableAllCorrelationLoggingConfigsTest:Disable: httpLog:%b, synapseLog:%b, " +
                            "methodCallsLog:%b, correlationIDLog:%b", httpLog, synapseLog, methodCallsLog,
                    correlationIDLog));
            assertFalse(httpLog || synapseLog || methodCallsLog || correlationIDLog);
        });
    }


    @Test(groups = {"wso2.am" }, description = "Testing enabling specific correlation configs using the devops API ",
            dependsOnMethods = { "testEnableAllCorrelationLoggingConfigsTest" })
    public void testSpecificCorrelationLoggingConfigsTest() throws Exception {

        /* Following is commented out since no HTTP logs related to API invocation are logged when only http correlation
        logs are enabled. Can be uncommented after fixing the issue in the product.
        // Test HTTP
        log.info("Enabling HTTP component correlation logs");
        configureCorrelationLoggingComponent(new String[]{ "http" }, true);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            httpLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Enable http:logLine: " + logLine);
                isHTTPLogLine(logLine);
            }
            assertTrue(httpLog);
        });
        log.info("Disabling HTTP component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "http" }, false);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            httpLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Disable http:logLine: " + logLine);
                isHTTPLogLine(logLine);
            }
            assertFalse(httpLog);
        });
        */

        /*
        Check for jdbc is skipped as it introduces test failures due to frequent jdbc logs in correlation.log file
        // Test JDBC
        log.info("Enabling JDBC component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "jdbc" }, true);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            jdbcLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Enable JDBC:logLine: " + logLine);
                isJDBCLogLine(logLine);
            }
            assertTrue(jdbcLog);
        });
        log.info("Disabling JDBC component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "jdbc" }, false);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            jdbcLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Disable JDBC:logLine: " + logLine);
                isJDBCLogLine(logLine);
            }
            assertFalse(jdbcLog);
        });
        */

        // Test Method-calls
        log.info("Enabling Method-calls component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "method-calls" }, true);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            methodCallsLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Enable method: " + logLine);
                isMethodCallsLogLine(logLine);
            }
            assertTrue(methodCallsLog);
        });
        log.info("Disabling Method-calls component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "method-calls" }, false);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            methodCallsLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Disable method: " + logLine);
                isMethodCallsLogLine(logLine);
            }
            assertFalse(methodCallsLog);
        });

        // Test Synapse
        log.info("Enabling Synapse component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "synapse" }, true);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            synapseLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Enable synapse: " + logLine);
                isSynapseLogLine(logLine);
            }
            assertTrue(synapseLog);
        });
        log.info("Disabling Synapse component correlation logs");
        configureCorrelationLoggingComponent(new String[] { "synapse" }, false);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            synapseLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testSpecificCorrelationLoggingConfigsTest:Disable synapse: " + logLine);
                isSynapseLogLine(logLine);
            }
            assertFalse(synapseLog);
        });
    }

    @Test(groups = {"wso2.am" }, description = "Testing persisted correlation component configurations ",
            dependsOnMethods = { "testSpecificCorrelationLoggingConfigsTest" })
    public void testPersistedCorrelationConfigs() throws Exception {
        log.info("Enabling http, method-calls correlation component logs before a restart");
        configureCorrelationLoggingComponent(new String[] { "http", "method-calls" }, true);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            httpLog = false;
            methodCallsLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testPersistedCorrelationConfigs:Enable:logLine: " + logLine);
                isHTTPLogLine(logLine);
                isMethodCallsLogLine(logLine);
            }
            assertTrue(httpLog && methodCallsLog);
        });

        serverConfigurationManager.restartGracefully();
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            logHandlerStarted = false;
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testPersistedCorrelationConfigs:Check handler start:logLine: " + logLine);
                isLogHandlerStarted(logLine);
            }
            assertTrue(logHandlerStarted);
        });

        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            httpLog = false;
            methodCallsLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testPersistedCorrelationConfigs:Check:logLine: " + logLine);
                isHTTPLogLine(logLine);
                isMethodCallsLogLine(logLine);
            }
            assertTrue(httpLog && methodCallsLog);
        });

        configureCorrelationLoggingComponent(new String[] { "http", "method-calls" }, false);
        with().pollInterval(5, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).untilAsserted(()-> {
            httpLog = false;
            methodCallsLog = false;
            InvokeTestAPI();
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                log.info("***testPersistedCorrelationConfigs:Disable:logLine: " + logLine);
                isHTTPLogLine(logLine);
                isMethodCallsLogLine(logLine);
            }
            assertFalse(httpLog || methodCallsLog);
        });
    }

    private void InvokeTestAPI() throws Exception {
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("activityid", CORRELATION_ID);
        org.apache.http.HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
    }

    private void configureCorrelationLoggingComponent(String[] componentNames, Boolean enable) throws Exception {
        log.info("***configureCorrelationLoggingComponent:args: " + Arrays.asList(componentNames) + ", enable:" + enable);

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
        log.info("***configureCorrelationLoggingComponent:PUT response: " + httpResponse.getData());
        assertEquals(httpResponse.getData(), payload.toString());

        httpResponse =
                HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);
        log.info("***configureCorrelationLoggingComponent:GET response: " + httpResponse.getData());
        assertEquals(httpResponse.getData(), payload.toString());
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
        Boolean status =  (logLine.contains("HTTP"));
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

    private boolean isCorrelationIDLogLine(String logLine) {
        boolean status =  (logLine.contains(CORRELATION_ID));
        if (status) {
            correlationIDLog = true;
        }
        return status;
    }

    private boolean isLogHandlerStarted(String logLine) {
        boolean status =  (logLine.contains("Started log handler"));
        if (status) {
            logHandlerStarted = true;
        }
        return status;
    }

    private void resetAllLogs() {
        httpLog = false;
        synapseLog = false;
        jdbcLog = false;
        methodCallsLog = false;
        correlationIDLog = false;
        logHandlerStarted = false;
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        // Disabling all correlation log components for next tests
        configureCorrelationLoggingComponent(new String[] {"http", "jdbc", "synapse", "ldap", "method-calls"}, false);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
