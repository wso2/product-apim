package org.wso2.am.integration.tests.logging;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
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
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.ServerConstants;

@SetEnvironment(executionEnvironments = {
        ExecutionEnvironment.STANDALONE }) public class CorrelationLoggingSystemEnabledTest
        extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(CorrelationLoggingSystemEnabledTest.class);
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String CORRELATION_CONFIG_PATH = "api/am/devops/v0/config/correlation";
    private final String CORRELATION_ID = "9e3ec6ed-2a37-4b20-8dd4-d5fbc754a7d9";
    // Activity id sent with each invocation; the disabling phase uses its own so it cannot match earlier lines.
    private String correlationId = CORRELATION_ID;
    private static final long LOG_POLL_INTERVAL_MS = 2000;
    private static final long LOG_POLL_TIMEOUT_MS = 30000;
    private final String API_END_POINT_POSTFIX_URL = "am/sample/pizzashack/v1/api/menu";
    private final String correlationLogFilePath =
            System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator + "logs"
                    + File.separator + "correlation.log";
    BufferedReader bufferedReader;
    private String apiId;

    private String applicationId;
    private String accessToken;
    private String context = "context_correlation";
    private Map<String, String> header = new HashMap<>();
    private Map<String, String> requestHeaders = new HashMap<>();
    private String apiEndPointUrl, providerName;
    private Boolean httpLog, jdbcLog, synapseLog, methodCallsLog, correlationIDLog;
    public static final String BASIC_AUTH_HEADER = "admin:admin";

    @Factory(dataProvider = "userModeDataProvider") public CorrelationLoggingSystemEnabledTest(TestUserMode userMode)
            throws Exception {
        this.userMode = userMode;
        byte[] encodedBytes =
                Base64.encodeBase64(BASIC_AUTH_HEADER.getBytes(StandardCharsets.UTF_8));
        header.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json");
    }

    @DataProvider public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true) public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        CreateTestAPI();
    }

    @Test(groups = {
            "wso2.am" }, description = "Testing the default correlation configs using the devops API ") public void testRetrieveDefaultCorrelationLoggingConfigsTest()
            throws Exception {

        //Retrieve default correlation logs configs from the GET method of the configs resource in devops API
        HttpResponse loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header);

        String expectedResponse = "{\"components\":[{\"name\":\"http\",\"enabled\":\"false\",\"properties\":[]},"
                + "{\"name\":\"jdbc\",\"enabled\":\"false\",\"properties\":[{\"name\":\"deniedThreads\",\"value\":"
                + "[\"MessageDeliveryTaskThreadPool\",\"HumanTaskServer\",\"BPELServer\",\"CarbonDeploymentSchedulerThread\"]}]},"
                + "{\"name\":\"ldap\",\"enabled\":\"false\",\"properties\":[]},"
                + "{\"name\":\"synapse\",\"enabled\":\"false\",\"properties\":[]},"
                + "{\"name\":\"method-calls\",\"enabled\":\"false\",\"properties\":[]}]}";
        Assert.assertEquals(loggingResponse.getData(), expectedResponse);

        InvokeTestAPI();
        Thread.sleep(5000);
        bufferedReader = new BufferedReader(new FileReader(correlationLogFilePath));
        while (bufferedReader.readLine() != null) {
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Testing the priroity of the correlation logging system parameter ", dependsOnMethods = {
            "testRetrieveDefaultCorrelationLoggingConfigsTest" }) public void testPriorityCorrelationLoggingConfigsTest()
            throws Exception {
        // Enabling all the correlation components using the DevOps API and testing whether logs are still logging.
        configureCorrelationLoggingComponent(new String[] { "http", "jdbc", "synapse", "ldap", "method-calls" }, true);
        InvokeTestAPI();
        resetAllLogs();
        awaitAllCorrelationCategories();
        assertAllCorrelationCategoriesLogged("after ENABLING all correlation components via the DevOps API");

        // Disabling all the correlation components using the DevOps API and testing whether logs are still logging.
        configureCorrelationLoggingComponent(new String[] { "http", "jdbc", "synapse", "ldap", "method-calls" }, false);
        // Only count lines written after the components were disabled.
        skipToEndOfLog();
        correlationId = UUID.randomUUID().toString();
        InvokeTestAPI();
        resetAllLogs();
        awaitAllCorrelationCategories();
        assertAllCorrelationCategoriesLogged(
                "after DISABLING all correlation components via the DevOps API "
                        + "(the system property must take priority, so logging is expected to continue)");
    }

    /**
     * Reads the correlation log until every category has been observed, or the timeout expires.
     *
     * <p>The correlation log is written asynchronously, so the categories produced by an invocation do not
     * all appear at once. {@code bufferedReader} is a tail over the log file, so successive reads pick up
     * newly appended lines and the observed flags accumulate; there is no need to re-invoke anything.</p>
     *
     * <p>This replaces a fixed {@code Thread.sleep(10000)} followed by a single read. Polling returns as
     * soon as all categories are present — typically well inside a second — instead of always paying the
     * full wait, while tolerating a slow flush up to {@link #LOG_POLL_TIMEOUT_MS}. A read is always
     * performed immediately before the timeout is evaluated, so lines that arrive during the final sleep
     * are still counted.</p>
     *
     * <p>On timeout this returns normally rather than failing; the caller's per-category assertions then
     * report which categories were missing.</p>
     */
    private void awaitAllCorrelationCategories() throws Exception {
        String logLine;
        long deadline = System.currentTimeMillis() + LOG_POLL_TIMEOUT_MS;
        while (true) {
            while ((logLine = bufferedReader.readLine()) != null) {
                assertTrue(isSynapseLogLine(logLine) || isHTTPLogLine(logLine) || isJDBCLogLine(logLine)
                                || isMethodCallsLogLine(logLine) || logLine.contains("Started log handler"),
                        "Unrecognised correlation log line: " + logLine);
                if (logLine.contains(correlationId)) {
                    correlationIDLog = true;
                }
            }
            if (allCorrelationCategoriesSeen() || System.currentTimeMillis() >= deadline) {
                return;
            }
            Thread.sleep(LOG_POLL_INTERVAL_MS);
        }
    }

    /**
     * Reads and discards every correlation log line written so far.
     */
    private void skipToEndOfLog() throws Exception {
        while (bufferedReader.readLine() != null) {
        }
    }

    private boolean allCorrelationCategoriesSeen() {
        return httpLog && jdbcLog && synapseLog && methodCallsLog && correlationIDLog;
    }

    /**
     * Asserts that every correlation log category was observed since the last {@link #resetAllLogs()}.
     *
     * <p>Each category is asserted separately so a failure names the category that was missing. A single
     * combined assertion reports only "expected [true] but found [false]", which gives no indication of
     * which of the five signals was absent.</p>
     *
     * @param phase description of the point in the test at which the check is made, included in the
     *              failure message
     */
    private void assertAllCorrelationCategoriesLogged(String phase) {
        assertTrue(httpLog, "No http correlation log line seen " + phase);
        assertTrue(jdbcLog, "No jdbc correlation log line seen " + phase);
        assertTrue(synapseLog, "No synapse correlation log line seen " + phase);
        assertTrue(methodCallsLog, "No method-calls correlation log line seen " + phase);
        assertTrue(correlationIDLog,
                "Correlation id '" + correlationId + "' not found in any log line " + phase);
    }

    private void InvokeTestAPI() throws Exception {
        requestHeaders.put("activityID", correlationId);
        HttpResponse invokeResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttps(context, API_VERSION_1_0_0) + "", requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), 200, "Response code mismatched");
    }

    private void CreateTestAPI() throws Exception {

        APIRequest apiRequest;
        String apiName = "CorrelationLogTest";

        ArrayList grantTypes = new ArrayList();
        apiRequest = new APIRequest(apiName, context, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        assertEquals(apiResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Create API Response Code is invalid." + apiId);
        assertNotNull(apiId, "Api ID is null");

        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        HttpResponse applicationResponse =
                restAPIStore.createApplication("Application_Test", "Test Application For Correlation Logs",
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
        assertNotNull(applicationId);
        restAPIStore.subscribeToAPI(apiId, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        ApplicationKeyDTO apiKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(apiKeyDTO);
        accessToken = apiKeyDTO.getToken().getAccessToken();
        assertNotNull(accessToken);
        requestHeaders.put("Authorization", "Bearer " + accessToken);
    }

    private void configureCorrelationLoggingComponent(String[] componentNames, Boolean enable) throws Exception {
        String[] DEFAULT_DENIED_THREADS =
                { "MessageDeliveryTaskThreadPool", "HumanTaskServer", "BPELServer", "CarbonDeploymentSchedulerThread" };
        String[] DEFAULT_COMPONENTS = { "http", "jdbc", "ldap", "synapse", "method-calls" };

        List<String> components = Arrays.asList(componentNames);

        JSONObject payload = new JSONObject();
        JSONArray componentArray = new JSONArray();
        for (String c : DEFAULT_COMPONENTS) {
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
        HttpResponse httpResponse =
                HTTPSClientUtils.doPut(getStoreURLHttps() + CORRELATION_CONFIG_PATH, header, payload.toString());
        assertEquals(httpResponse.getData(), payload.toString());

        Thread.sleep(1000);
    }

    private boolean isSynapseLogLine(String logLine) {
        Boolean status = (logLine.contains("HTTP State Transition") || logLine.contains("ROUND-TRIP LATENCY")
                || logLine.contains("Thread switch latency") || logLine.contains("BACKEND LATENCY"));
        if (status) {
            synapseLog = true;
        }
        return status;
    }

    private boolean isHTTPLogLine(String logLine) {
        Boolean status = (logLine.contains("HTTP-In-"));
        if (status) {
            httpLog = true;
        }
        return status;
    }

    private boolean isJDBCLogLine(String logLine) {
        Boolean status = (logLine.contains("jdbc"));
        if (status) {
            jdbcLog = true;
        }
        return status;
    }

    private boolean isMethodCallsLogLine(String logLine) {
        boolean status = (logLine.contains("METHOD") || logLine.contains("Application_Test"));
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

    @AfterClass(alwaysRun = true) void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
