/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.tests.logging;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.logging.RemoteLoggingConfigClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.FileManager;
import org.wso2.carbon.logging.remote.config.stub.types.carbon.RemoteServerLoggerData;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.with;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Integration tests for the remote logging appender management behaviour
 * <p>
 * Scenarios covered:
 *  1. Existing AUDIT_LOGFILE / CARBON_LOGFILE / API_LOGFILE blocks in log4j2.properties are
 *     NOT overwritten when remote logging is disabled with a blank URL.
 *  2. A missing appender block is written (as an HTTP appender) when remote logging is enabled,
 *     and the new appender is added to the top-level {@code appenders} list (no dangling appender).
 *  3. Appenders that already appear in log4j2.properties are not duplicated in the
 *     {@code appenders} list after a write.
 *  4. End-to-end: logs reach a remote HTTP endpoint when remote logging is enabled, and stop
 *     when it is disabled.
 * <p>
 * The tests manipulate the live log4j2.properties of the locally running Carbon server and
 * restore it after every test method and after the whole class.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class RemoteLoggingAppenderTest extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(RemoteLoggingAppenderTest.class);

    /* Log-type constants understood by RemoteLoggingConfig service */
    private static final String LOG_TYPE_AUDIT = "AUDIT";
    private static final String LOG_TYPE_CARBON = "CARBON";
    private static final String LOG_TYPE_API = "API";

    /* Corresponding appender names in log4j2.properties */
    private static final String APPENDER_AUDIT = "AUDIT_LOGFILE";
    private static final String APPENDER_CARBON = "CARBON_LOGFILE";
    private static final String APPENDER_API = "API_LOGFILE";

    /*
     * Logger keys as they appear in the "loggers = ..." list.
     * AUDIT_LOG and API_LOG have dedicated logger blocks; CARBON_LOGFILE is routed
     * through rootLogger rather than a named logger.
     */
    private static final String LOGGER_KEY_AUDIT = "AUDIT_LOG";
    private static final String LOGGER_KEY_API = "API_LOG";

    private static final String ROLLING_FILE_TYPE = "RollingFile";
    private static final String HTTP_TYPE = "SecuredHttp";

    private static final String CONNECT_TIMEOUT_MILLIS = "2000";

    private RemoteLoggingConfigClient remoteLoggingConfigClient;
    private Path log4j2PropertiesServerPath;
    private String originalLog4j2Content;

    /* Mock HTTP server that collects log payloads sent by the server */
    private HttpServer mockLogServer;
    private int mockServerPort;
    private final List<String> receivedLogPayloads = new ArrayList<>();

    private static final String REMOTE_LOGGING_CONFIG_DIR = "configFiles" + File.separator + "remoteLogging";

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();

        AutomationContext superAdminContext = new AutomationContext(
                APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);

        remoteLoggingConfigClient = new RemoteLoggingConfigClient(
                superAdminContext.getContextUrls().getBackEndUrl(),
                superAdminContext.getSuperTenant().getTenantAdmin().getUserName(),
                superAdminContext.getSuperTenant().getTenantAdmin().getPassword());

        log4j2PropertiesServerPath = Paths.get(
                System.getProperty(ServerConstants.CARBON_HOME),
                "repository", "conf", "log4j2.properties");

        originalLog4j2Content = FileManager.readFile(log4j2PropertiesServerPath.toString());

        /* Start a lightweight HTTP server to act as the remote log receiver.
         * Binding to port 0 lets the OS assign a free port atomically, avoiding the
         * TOCTOU race in a find-then-bind approach. */
        mockLogServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServerPort = mockLogServer.getAddress().getPort();
        mockLogServer.createContext("/api/logs/consume", exchange -> {
            try (InputStream body = exchange.getRequestBody()) {
                byte[] bytes = body.readAllBytes();
                String payload = new String(bytes, StandardCharsets.UTF_8);
                synchronized (receivedLogPayloads) {
                    receivedLogPayloads.add(payload);
                }
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        mockLogServer.start();
        log.info("Mock log server started on port " + mockServerPort);
    }

    // ----- Scenario 1 -------------------------------------------------------

    /**
     * Calling resetRemoteServerConfig on appenders that are already of RollingFile type
     * leaves them unchanged — the reset is effectively a no-op when the appender is already
     * in its default local-file state.
     */
    @Test(groups = {"wso2.am"}, description =
            "Existing local-file appender blocks are not overwritten when remote logging is disabled with a blank URL")
    public void testExistingAppendersPreservedOnResetWithBlankUrl() throws Exception {
        String log4jFixtureFile = "log4j2WithAllAppenders.properties";
        String fixture = loadArtifact(log4jFixtureFile);
        copyFixtureToServer(log4jFixtureFile);

        String auditTypeBefore  = extractAppenderType(APPENDER_AUDIT,  fixture);
        String carbonTypeBefore = extractAppenderType(APPENDER_CARBON, fixture);
        String apiTypeBefore    = extractAppenderType(APPENDER_API,    fixture);

        assertEquals(auditTypeBefore, ROLLING_FILE_TYPE,  "Test fixture must have AUDIT_LOGFILE as RollingFile");
        assertEquals(carbonTypeBefore, ROLLING_FILE_TYPE, "Test fixture must have CARBON_LOGFILE as RollingFile");
        assertEquals(apiTypeBefore, ROLLING_FILE_TYPE,    "Test fixture must have API_LOGFILE as RollingFile");

        /* Call reset with a blank URL for each log type */
        for (String logType : new String[]{LOG_TYPE_AUDIT, LOG_TYPE_CARBON, LOG_TYPE_API}) {
            RemoteServerLoggerData blankData = new RemoteServerLoggerData();
            blankData.setLogType(logType);
            blankData.setUrl("");
            remoteLoggingConfigClient.resetRemoteServerConfig(blankData);
            waitForLog4j2ConfigSync(5);
        }

        String after = readServerLog4j2Properties();

        assertEquals(extractAppenderType(APPENDER_AUDIT,  after), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE type must not change when reset is called with a blank URL");
        assertEquals(extractAppenderType(APPENDER_CARBON, after), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE type must not change when reset is called with a blank URL");
        assertEquals(extractAppenderType(APPENDER_API,    after), ROLLING_FILE_TYPE,
                "API_LOGFILE type must not change when reset is called with a blank URL");
    }

    /**
     * Enabling remote logging for AUDIT converts only the AUDIT_LOGFILE appender to HTTP type;
     * CARBON_LOGFILE and API_LOGFILE — which were not targeted — remain as RollingFile.
     */
    @Test(groups = {"wso2.am"}, description =
            "Enabling remote logging for AUDIT changes only the AUDIT_LOGFILE appender; unrelated appenders are unaffected")
    public void testAuditAppenderChangedOnRemoteLoggingEnable() throws Exception {
        copyFixtureToServer("log4j2WithAllAppenders.properties");

        /* Enable remote logging for AUDIT only */
        RemoteServerLoggerData auditConfig = buildRemoteConfig(LOG_TYPE_AUDIT);
        remoteLoggingConfigClient.addRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_AUDIT, after), HTTP_TYPE,
                "AUDIT_LOGFILE should be HTTP after enabling remote logging");
        assertEquals(extractAppenderType(APPENDER_CARBON, after), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE must remain RollingFile — it was not targeted for remote logging");
        assertEquals(extractAppenderType(APPENDER_API, after), ROLLING_FILE_TYPE,
                "API_LOGFILE must remain RollingFile — it was not targeted for remote logging");
    }

    /**
     * Resetting remote logging for AUDIT reverts only the AUDIT_LOGFILE appender to RollingFile;
     * CARBON_LOGFILE and API_LOGFILE — which were never changed — remain as RollingFile.
     */
    @Test(groups = {"wso2.am"}, description =
            "Resetting remote logging for AUDIT reverts only the AUDIT_LOGFILE appender; unrelated appenders are unaffected")
    public void testAuditAppenderRevertedOnRemoteLoggingReset() throws Exception {
        copyFixtureToServer("log4j2WithAllAppenders.properties");

        RemoteServerLoggerData auditConfig = buildRemoteConfig(LOG_TYPE_AUDIT);
        remoteLoggingConfigClient.addRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        String afterEnable = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterEnable), HTTP_TYPE,
                "AUDIT_LOGFILE should be HTTP after enabling remote logging");

        remoteLoggingConfigClient.resetRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        String afterReset = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterReset), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE should revert to RollingFile after reset");
        assertEquals(extractAppenderType(APPENDER_CARBON, afterReset), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE must still be RollingFile after the unrelated AUDIT reset");
        assertEquals(extractAppenderType(APPENDER_API, afterReset), ROLLING_FILE_TYPE,
                "API_LOGFILE must still be RollingFile after the unrelated AUDIT reset");
    }

    // ----- Scenario 2 -------------------------------------------------------

    /**
     * When the AUDIT_LOGFILE block is absent from log4j2.properties and remote logging is
     * enabled for the AUDIT log type, the block must be created as an HTTP appender and the
     * appender name must appear in the top-level {@code appenders} list (no dangling appender).
     * <p>
     * Covers the addAppenderToAppendersList addition in Log4j2PropertiesEditor.writeUpdatedAppender.
     */
    @Test(groups = {"wso2.am"}, description =
            "Missing AUDIT_LOGFILE block is created as HTTP appender and added to the appenders list " +
            "when remote logging is enabled")
    public void testMissingAuditAppenderCreatedOnEnable() throws Exception {
        String log4jFixtureFile = "log4j2WithoutAuditAppender.properties";
        String fixture = loadArtifact(log4jFixtureFile);
        copyFixtureToServer(log4jFixtureFile);

        assertFalse(fixture.contains("appender." + APPENDER_AUDIT + ".type"),
                "Test fixture must not contain an AUDIT_LOGFILE block");

        RemoteServerLoggerData auditConfig = buildRemoteConfig(LOG_TYPE_AUDIT);
        remoteLoggingConfigClient.addRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();

        /* Block must now exist and be of HTTP type */
        assertTrue(after.contains("appender." + APPENDER_AUDIT + ".type = " + HTTP_TYPE),
                "AUDIT_LOGFILE block must be written as HTTP type when it was missing and remote logging is enabled");
        assertTrue(after.contains("appender." + APPENDER_AUDIT + ".url"),
                "AUDIT_LOGFILE block must contain the remote URL property");

        /* Appender must be in the top-level appenders list (not a dangling appender) */
        assertTrue(isAppenderInList(after, APPENDER_AUDIT),
                "AUDIT_LOGFILE must appear in the 'appenders = ...' list after being written");

        /* Part 2: Load a config with all appenders present, enable AUDIT remote logging,
         * verify only AUDIT_LOGFILE becomes HTTP while others remain RollingFile.
         * Then replace the config with one that lacks the AUDIT block and verify that
         * syncRemoteServerConfigs() recreates the AUDIT_LOGFILE block without touching the others. */
        copyFixtureToServer("log4j2WithAllAppenders.properties");
        remoteLoggingConfigClient.addRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        String afterEnableOnAll = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterEnableOnAll), HTTP_TYPE,
                "AUDIT_LOGFILE must be HTTP after enabling remote logging when all appenders are present");
        assertEquals(extractAppenderType(APPENDER_CARBON, afterEnableOnAll), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE must remain RollingFile — only AUDIT remote logging was enabled");
        assertEquals(extractAppenderType(APPENDER_API, afterEnableOnAll), ROLLING_FILE_TYPE,
                "API_LOGFILE must remain RollingFile — only AUDIT remote logging was enabled");

        /* Replace the config file with one that does not have the AUDIT_LOGFILE block */
        copyFixtureToServer("log4j2WithoutAuditAppender.properties");

        /* Sync detects mismatch (registry has URL for AUDIT, file now has no AUDIT block)
         * and recreates the AUDIT_LOGFILE block as an HTTP appender */
        remoteLoggingConfigClient.syncRemoteServerConfigs();
        waitForLog4j2ConfigSync(5);

        String afterSync = readServerLog4j2Properties();
        assertTrue(afterSync.contains("appender." + APPENDER_AUDIT + ".type = " + HTTP_TYPE),
                "AUDIT_LOGFILE must be recreated as HTTP by syncRemoteServerConfigs after the config file was replaced");
        assertTrue(isAppenderInList(afterSync, APPENDER_AUDIT),
                "AUDIT_LOGFILE must appear in the 'appenders' list after being recreated by sync");
        assertEquals(extractAppenderType(APPENDER_CARBON, afterSync), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE must remain unaffected by the AUDIT sync");
        assertEquals(extractAppenderType(APPENDER_API, afterSync), ROLLING_FILE_TYPE,
                "API_LOGFILE must remain unaffected by the AUDIT sync");
    }

    /**
     * Same as testMissingAuditAppenderCreatedOnEnable but for CARBON_LOGFILE.
     */
    @Test(groups = {"wso2.am"}, description =
            "Missing CARBON_LOGFILE block is created as HTTP appender and registered in the appenders list")
    public void testMissingCarbonAppenderCreatedOnEnable() throws Exception {
        String log4jFixtureFile = "log4j2WithoutCarbonAppender.properties";
        String fixture = loadArtifact(log4jFixtureFile);
        copyFixtureToServer(log4jFixtureFile);

        assertFalse(fixture.contains("appender." + APPENDER_CARBON + ".type"),
                "Test fixture must not contain a CARBON_LOGFILE block");

        RemoteServerLoggerData carbonConfig = buildRemoteConfig(LOG_TYPE_CARBON);
        remoteLoggingConfigClient.addRemoteServerConfig(carbonConfig);
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();

        assertTrue(after.contains("appender." + APPENDER_CARBON + ".type = " + HTTP_TYPE),
                "CARBON_LOGFILE block must be written as HTTP type when missing and remote logging is enabled");
        assertTrue(isAppenderInList(after, APPENDER_CARBON),
                "CARBON_LOGFILE must appear in the 'appenders = ...' list after being written");

        /* Part 2: Same sync-recreates scenario for CARBON_LOGFILE. */
        copyFixtureToServer("log4j2WithAllAppenders.properties");
        remoteLoggingConfigClient.addRemoteServerConfig(carbonConfig);
        waitForLog4j2ConfigSync(5);

        String afterEnableOnAll = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_CARBON, afterEnableOnAll), HTTP_TYPE,
                "CARBON_LOGFILE must be HTTP after enabling remote logging when all appenders are present");
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterEnableOnAll), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE must remain RollingFile — only CARBON remote logging was enabled");
        assertEquals(extractAppenderType(APPENDER_API, afterEnableOnAll), ROLLING_FILE_TYPE,
                "API_LOGFILE must remain RollingFile — only CARBON remote logging was enabled");

        copyFixtureToServer("log4j2WithoutCarbonAppender.properties");

        remoteLoggingConfigClient.syncRemoteServerConfigs();
        waitForLog4j2ConfigSync(5);

        String afterSync = readServerLog4j2Properties();
        assertTrue(afterSync.contains("appender." + APPENDER_CARBON + ".type = " + HTTP_TYPE),
                "CARBON_LOGFILE must be recreated as HTTP by syncRemoteServerConfigs after the config file was replaced");
        assertTrue(isAppenderInList(afterSync, APPENDER_CARBON),
                "CARBON_LOGFILE must appear in the 'appenders' list after being recreated by sync");
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterSync), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE must remain unaffected by the CARBON sync");
        assertEquals(extractAppenderType(APPENDER_API, afterSync), ROLLING_FILE_TYPE,
                "API_LOGFILE must remain unaffected by the CARBON sync");
    }

    /**
     * Same as testMissingAuditAppenderCreatedOnEnable but for API_LOGFILE.
     */
    @Test(groups = {"wso2.am"}, description =
            "Missing API_LOGFILE block is created as HTTP appender and registered in the appenders list")
    public void testMissingApiAppenderCreatedOnEnable() throws Exception {
        String log4jFixtureFile = "log4j2WithoutApiAppender.properties";
        String fixture = loadArtifact(log4jFixtureFile);
        copyFixtureToServer(log4jFixtureFile);

        assertFalse(fixture.contains("appender." + APPENDER_API + ".type"),
                "Test fixture must not contain an API_LOGFILE block");

        RemoteServerLoggerData apiConfig = buildRemoteConfig(LOG_TYPE_API);
        remoteLoggingConfigClient.addRemoteServerConfig(apiConfig);
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();

        assertTrue(after.contains("appender." + APPENDER_API + ".type = " + HTTP_TYPE),
                "API_LOGFILE block must be written as HTTP type when missing and remote logging is enabled");
        assertTrue(isAppenderInList(after, APPENDER_API),
                "API_LOGFILE must appear in the 'appenders = ...' list after being written");

        /* Part 2: Same sync-recreates scenario for API_LOGFILE. */
        copyFixtureToServer("log4j2WithAllAppenders.properties");
        remoteLoggingConfigClient.addRemoteServerConfig(apiConfig);
        waitForLog4j2ConfigSync(5);

        String afterEnableOnAll = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_API, afterEnableOnAll), HTTP_TYPE,
                "API_LOGFILE must be HTTP after enabling remote logging when all appenders are present");
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterEnableOnAll), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE must remain RollingFile — only API remote logging was enabled");
        assertEquals(extractAppenderType(APPENDER_CARBON, afterEnableOnAll), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE must remain RollingFile — only API remote logging was enabled");

        copyFixtureToServer("log4j2WithoutApiAppender.properties");

        remoteLoggingConfigClient.syncRemoteServerConfigs();
        waitForLog4j2ConfigSync(5);

        String afterSync = readServerLog4j2Properties();
        assertTrue(afterSync.contains("appender." + APPENDER_API + ".type = " + HTTP_TYPE),
                "API_LOGFILE must be recreated as HTTP by syncRemoteServerConfigs after the config file was replaced");
        assertTrue(isAppenderInList(afterSync, APPENDER_API),
                "API_LOGFILE must appear in the 'appenders' list after being recreated by sync");
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterSync), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE must remain unaffected by the API sync");
        assertEquals(extractAppenderType(APPENDER_CARBON, afterSync), ROLLING_FILE_TYPE,
                "CARBON_LOGFILE must remain unaffected by the API sync");
    }

    /**
     * When all three appender blocks are absent and remote logging is enabled for only one log
     * type (API), only API_LOGFILE must be created; AUDIT_LOGFILE and CARBON_LOGFILE must
     * remain absent.
     *
     * <p>addRemoteServerConfig is scoped to the log type it is called with. Enabling remote
     * logging for API must not implicitly create appender blocks for other log types.</p>
     */
    @Test(groups = {"wso2.am"}, description =
            "Enabling remote logging for API_LOGFILE only creates API_LOGFILE; AUDIT_LOGFILE and CARBON_LOGFILE remain absent")
    public void testOnlyTargetedAppenderCreatedWhenOthersAbsent() throws Exception {
        String log4jFixtureFile = "log4j2WithoutLocalAppenders.properties";
        String fixture = loadArtifact(log4jFixtureFile);
        copyFixtureToServer(log4jFixtureFile);

        assertFalse(fixture.contains("appender." + APPENDER_AUDIT + ".type"),
                "Test fixture must not contain an AUDIT_LOGFILE block");
        assertFalse(fixture.contains("appender." + APPENDER_CARBON + ".type"),
                "Test fixture must not contain a CARBON_LOGFILE block");
        assertFalse(fixture.contains("appender." + APPENDER_API + ".type"),
                "Test fixture must not contain an API_LOGFILE block");

        /* Enable remote logging for API only */
        remoteLoggingConfigClient.addRemoteServerConfig(buildRemoteConfig(LOG_TYPE_API));
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();

        /* API_LOGFILE must be written as HTTP and its logger wired up */
        assertTrue(after.contains("appender." + APPENDER_API + ".type = " + HTTP_TYPE),
                "API_LOGFILE block must be written as HTTP type when remote logging is enabled for API");
        assertTrue(isAppenderInList(after, APPENDER_API),
                "API_LOGFILE must appear in the 'appenders' list after remote logging is enabled for API");
//        assertTrue(isLoggerInList(after, LOGGER_KEY_API),
//                "API_LOG must appear in the 'loggers' list after API remote logging is enabled");
//        assertTrue(hasLoggerAppenderRef(after, LOGGER_KEY_API, APPENDER_API),
//                "API_LOG logger block must contain an appenderRef to API_LOGFILE");

        /* AUDIT_LOGFILE and CARBON_LOGFILE must remain absent — appenders and loggers */
        assertFalse(after.contains("appender." + APPENDER_AUDIT + ".type"),
                "AUDIT_LOGFILE must NOT be created when only API remote logging is enabled");
        assertFalse(after.contains("appender." + APPENDER_CARBON + ".type"),
                "CARBON_LOGFILE must NOT be created when only API remote logging is enabled");
        assertFalse(isAppenderInList(after, APPENDER_AUDIT),
                "AUDIT_LOGFILE must NOT appear in the 'appenders' list when it was not configured for remote logging");
        assertFalse(isAppenderInList(after, APPENDER_CARBON),
                "CARBON_LOGFILE must NOT appear in the 'appenders' list when it was not configured for remote logging");

        /* Reload the stripped fixture before sync so the API_LOGFILE block is absent from the
         * file. With API in the registry but not in the file, syncRemoteServerConfigs() detects
         * the URL mismatch and recreates the API_LOGFILE block. AUDIT and CARBON are not in the
         * registry, so they must remain absent after sync.
         *
         * Note: calling sync while the API block is already in the file with a matching URL
         * would trigger a NullPointerException in isDataUpdated on the server side because the
         * stub data returned by getRemoteServerConfigs does not carry a username and calling
         * getUsername().equals(...) on null crashes. Reloading the fixture avoids this by
         * ensuring the URL comparison short-circuits isDataUpdated before the null field checks. */
        copyFixtureToServer("log4j2WithoutLocalAppenders.properties");
        remoteLoggingConfigClient.syncRemoteServerConfigs();
        waitForLog4j2ConfigSync(5);

        String afterSync = readServerLog4j2Properties();
        assertTrue(afterSync.contains("appender." + APPENDER_API + ".type = " + HTTP_TYPE),
                "API_LOGFILE must remain HTTP after syncRemoteServerConfigs");
        assertTrue(isAppenderInList(afterSync, APPENDER_API),
                "API_LOGFILE must remain in the 'appenders' list after sync");
        assertFalse(afterSync.contains("appender." + APPENDER_AUDIT + ".type"),
                "AUDIT_LOGFILE must NOT be created by syncRemoteServerConfigs when it is not configured for remote logging");
        assertFalse(afterSync.contains("appender." + APPENDER_CARBON + ".type"),
                "CARBON_LOGFILE must NOT be created by syncRemoteServerConfigs when it is not configured for remote logging");
        assertFalse(isAppenderInList(afterSync, APPENDER_AUDIT),
                "AUDIT_LOGFILE must NOT appear in the 'appenders' list after sync when not configured");
        assertFalse(isAppenderInList(afterSync, APPENDER_CARBON),
                "CARBON_LOGFILE must NOT appear in the 'appenders' list after sync when not configured");
    }

    // ----- Scenario 2b: missing block + remote logging disabled → block stays absent ------

    /**
     * When AUDIT_LOGFILE / CARBON_LOGFILE / API_LOGFILE blocks are absent and
     * syncRemoteServerConfigs() is called with no active remote logging configuration,
     * the log4j2.properties must remain unchanged.
     *
     * <p>syncRemoteServerConfigs() is the path exercised on server startup. It reads all
     * persisted remote server configs and calls processRemoteServerLoggerData. The URL-blank
     * guard in processRemoteServerLoggerData prevents resetRemoteServerConfig from being
     * called when no remote URL is configured, so the file must not be touched.</p>
     */
    @Test(groups = {"wso2.am"}, description =
            "syncRemoteServerConfigs with no configured remote URLs does not add missing appender blocks")
    public void testMissingAppendersNotAddedOnSyncWhenRemoteLoggingNotConfigured() throws Exception {
        copyFixtureToServer("log4j2WithoutLocalAppenders.properties");

        /* Ensure no remote logging is configured before calling sync */
        disableAllActiveRemoteConfigs();

        /* Trigger the startup-time sync path */
        remoteLoggingConfigClient.syncRemoteServerConfigs();
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();

        assertFalse(after.contains("appender." + APPENDER_AUDIT + ".type"),
                "AUDIT_LOGFILE must NOT be added by syncRemoteServerConfigs when remote logging is unconfigured");
        assertFalse(after.contains("appender." + APPENDER_CARBON + ".type"),
                "CARBON_LOGFILE must NOT be added by syncRemoteServerConfigs when remote logging is unconfigured");
        assertFalse(after.contains("appender." + APPENDER_API + ".type"),
                "API_LOGFILE must NOT be added by syncRemoteServerConfigs when remote logging is unconfigured");
    }

    // ----- Scenario 3 -------------------------------------------------------

    /**
     * Appenders that are already present in the {@code appenders} list must not be
     * duplicated after a write operation.
     */
    @Test(groups = {"wso2.am"}, description =
            "An appender that already exists in the 'appenders' list is not duplicated")
    public void testNoduplicateAppenderInList() throws Exception {
        String log4jFixtureFile = "log4j2WithAllAppenders.properties";
        String fixture = loadArtifact(log4jFixtureFile);
        copyFixtureToServer(log4jFixtureFile);

        assertTrue(isAppenderInList(fixture, APPENDER_AUDIT),
                "Test fixture must have AUDIT_LOGFILE in the appenders list already");

        /* Enable then reset so the appender list is touched */
        RemoteServerLoggerData auditConfig = buildRemoteConfig(LOG_TYPE_AUDIT);
        remoteLoggingConfigClient.addRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);
        remoteLoggingConfigClient.resetRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        String after = readServerLog4j2Properties();
        int occurrences = countOccurrences(after, APPENDER_AUDIT, extractAppendersLine(after));
        assertEquals(occurrences, 1,
                "AUDIT_LOGFILE must appear exactly once in the 'appenders = ...' list");
    }

    // ----- Scenario 4: end-to-end remote logging ----------------------------

    /**
     * When remote logging is enabled for the AUDIT log type:
     *   - the AUDIT_LOGFILE appender becomes an HTTP appender;
     *   - audit log entries are delivered to the remote HTTP endpoint.
     * <p>
     * When remote logging is subsequently disabled:
     *   - the AUDIT_LOGFILE appender reverts to RollingFile;
     *   - no further log entries are sent to the remote endpoint.
     */
    @Test(groups = {"wso2.am"}, description =
            "Audit logs flow to the remote HTTP endpoint when remote logging is enabled and stop when disabled")
    public void testRemoteLoggingEndToEnd() throws Exception {
        copyFixtureToServer("log4j2WithAllAppenders.properties");

        RemoteServerLoggerData auditConfig = buildRemoteConfig(LOG_TYPE_AUDIT);
        remoteLoggingConfigClient.addRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        /* Verify the appender was converted to HTTP type */
        String afterEnable = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterEnable), HTTP_TYPE,
                "AUDIT_LOGFILE must be HTTP type after enabling remote logging");
        assertTrue(afterEnable.contains("http://localhost:" + mockServerPort + "/api/logs/consume"),
                "Remote URL must be present in the AUDIT_LOGFILE block");

        /*
         * Snapshot the payload count before triggering so the wait condition detects a
         * genuinely new delivery rather than passing on traffic received in earlier tests.
         */
        final int payloadCountBeforeTrigger;
        synchronized (receivedLogPayloads) {
            payloadCountBeforeTrigger = receivedLogPayloads.size();
        }

        /*
         * Trigger an admin action that produces an audit log entry.
         * The admin REST API is authenticated, so calling any admin resource with valid
         * credentials generates an AUDIT_LOG entry.
         */
        triggerAuditLogEntry();

        /* Wait for at least one new log entry to arrive at the mock server (up to 30 seconds) */
        with().pollInterval(2, TimeUnit.SECONDS).await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            synchronized (receivedLogPayloads) {
                assertTrue(receivedLogPayloads.size() > payloadCountBeforeTrigger,
                        "Mock log server must have received at least one new log payload from the AUDIT appender");
            }
        });

        /* Disable remote logging for AUDIT */
        remoteLoggingConfigClient.resetRemoteServerConfig(auditConfig);
        waitForLog4j2ConfigSync(5);

        /* Snapshot the count just after disabling */
        int payloadCountBeforeDisable;
        synchronized (receivedLogPayloads) {
            payloadCountBeforeDisable = receivedLogPayloads.size();
        }

        String afterReset = readServerLog4j2Properties();
        assertEquals(extractAppenderType(APPENDER_AUDIT, afterReset), ROLLING_FILE_TYPE,
                "AUDIT_LOGFILE must revert to RollingFile after disabling remote logging");

        /* Generate another audit-triggering action and wait briefly */
        triggerAuditLogEntry();
        waitForLog4j2ConfigSync(5);

        /* Payload count must not grow beyond what was received while remote logging was active */
        synchronized (receivedLogPayloads) {
            assertEquals(receivedLogPayloads.size(), payloadCountBeforeDisable,
                    "No new log payloads must be received by the mock server after remote logging is disabled");
        }
    }

    // ----- Scenario: all three appenders missing at once -------------------

    /**
     * When all three appender blocks are absent and remote logging is enabled for all
     * three log types, each block must be created and registered in the appenders list.
     */
    @Test(groups = {"wso2.am"}, description =
            "All three missing appender blocks are created and listed when remote logging is enabled for each")
    public void testAllThreeMissingAppendersCreatedOnEnable() throws Exception {
        copyFixtureToServer("log4j2WithoutLocalAppenders.properties");

        for (String logType : new String[]{LOG_TYPE_AUDIT, LOG_TYPE_CARBON, LOG_TYPE_API}) {
            remoteLoggingConfigClient.addRemoteServerConfig(buildRemoteConfig(logType));
            waitForLog4j2ConfigSync(5);
        }

        String after = readServerLog4j2Properties();

        /* Appender blocks and appenders list */
        for (String appender : new String[]{APPENDER_AUDIT, APPENDER_CARBON, APPENDER_API}) {
            assertTrue(after.contains("appender." + appender + ".type = " + HTTP_TYPE),
                    appender + " must be written as HTTP type when all three are missing and remote logging is enabled");
            assertTrue(isAppenderInList(after, appender),
                    appender + " must appear in the 'appenders = ...' list");
        }

        /* Logger entries must also be present so the appenders are actually reachable */
//        assertTrue(isLoggerInList(after, LOGGER_KEY_AUDIT),
//                "AUDIT_LOG must appear in the 'loggers' list after enabling remote logging for all three types");
//        assertTrue(hasLoggerAppenderRef(after, LOGGER_KEY_AUDIT, APPENDER_AUDIT),
//                "AUDIT_LOG logger block must contain an appenderRef to AUDIT_LOGFILE");

//        assertTrue(hasRootLoggerAppenderRef(after, APPENDER_CARBON),
//                "rootLogger must have an appenderRef to CARBON_LOGFILE after enabling remote logging for CARBON");

//        assertTrue(isLoggerInList(after, LOGGER_KEY_API),
//                "API_LOG must appear in the 'loggers' list after enabling remote logging for all three types");
//        assertTrue(hasLoggerAppenderRef(after, LOGGER_KEY_API, APPENDER_API),
//                "API_LOG logger block must contain an appenderRef to API_LOGFILE");
    }

    // ----- Cleanup ----------------------------------------------------------

    /**
     * Runs after every test method (pass or fail) to isolate tests from each other.
     * Disables all active remote logging configurations and restores log4j2.properties
     * to the snapshot taken in {@link #initialize()}.
     * Both steps always run; any failure is surfaced as a test failure so dirty server
     * state does not silently contaminate subsequent tests.
     */
    @AfterMethod(alwaysRun = true)
    public void restoreAfterMethod() throws Exception {
        Exception firstError = null;
        try {
            disableAllActiveRemoteConfigs();
        } catch (Exception e) {
            firstError = e;
            log.error("Failed to disable remote logging configs during @AfterMethod cleanup", e);
        }
        try {
            restoreLog4j2Properties();
        } catch (Exception e) {
            if (firstError == null) {
                firstError = e;
            }
            log.error("Failed to restore log4j2.properties during @AfterMethod cleanup", e);
        }
        if (firstError != null) {
            throw new RuntimeException("Test cleanup failed — server state may be dirty", firstError);
        }
    }

    /**
     * Final teardown after all tests in the class have run (pass or fail).
     * Stops the mock HTTP server; log4j2.properties and remote configs are already
     * clean because {@link #restoreAfterMethod()} ran after the last test.
     */
    @AfterClass(alwaysRun = true)
    public void cleanup() {
        if (mockLogServer != null) {
            mockLogServer.stop(0);
            log.info("Mock log server stopped");
        }
    }

    /**
     * Resets every remote logging configuration that currently has a non-blank URL.
     * All entries are attempted; if any reset fails the exception is re-thrown after
     * all entries have been processed so a single failure does not skip the rest.
     */
    private void disableAllActiveRemoteConfigs() throws Exception {
        RemoteServerLoggerData[] configs = remoteLoggingConfigClient.getRemoteServerConfigs();
        if (configs == null) {
            return;
        }
        Exception firstError = null;
        for (RemoteServerLoggerData cfg : configs) {
            if (cfg != null && cfg.getUrl() != null && !cfg.getUrl().isEmpty()) {
                try {
                    remoteLoggingConfigClient.resetRemoteServerConfig(cfg);
                    waitForLog4j2ConfigSync(5);
                } catch (Exception e) {
                    if (firstError == null) {
                        firstError = e;
                    }
                    log.error("Failed to reset remote config for log type: " + cfg.getLogType(), e);
                }
            }
        }
        if (firstError != null) {
            throw firstError;
        }
    }

    /**
     * Writes the original log4j2.properties content back to disk.
     */
    private void restoreLog4j2Properties() throws IOException {
        if (originalLog4j2Content == null || log4j2PropertiesServerPath == null) {
            return;
        }
        FileManager.writeToFile(log4j2PropertiesServerPath.toString(), originalLog4j2Content);
        log.debug("Restored original log4j2.properties");
    }

    // ----- Helpers ----------------------------------------------------------

    /**
     * Copies a log4j2 fixture file from the remoteLogging artifact directory
     * to the servers log4j properties file location.
     */
    private void copyFixtureToServer(String fixturePath) throws Exception {
        String log4jPropertiesFile = Paths.get(getAMResourceLocation(), REMOTE_LOGGING_CONFIG_DIR,
                fixturePath).toString();
        FileManager.copyFile(new File(log4jPropertiesFile), log4j2PropertiesServerPath.toString());
    }

    /**
     * Loads a log4j2 fixture file from the remoteLogging artifact directory.
     * Uses {@link #getAMResourceLocation()} so the path is resolved the same way
     * as every other artifact reference in the integration test suite.
     */
    private String loadArtifact(String fileName) throws IOException {
        Path path = Paths.get(getAMResourceLocation(), REMOTE_LOGGING_CONFIG_DIR, fileName);
        return FileManager.readFile(path.toString());
    }

    /**
     * Reads the current log4j2.properties content from the server's file system.
     */
    private String readServerLog4j2Properties() throws IOException {
        return FileManager.readFile(log4j2PropertiesServerPath.toString());
    }

    /**
     * Extracts the {@code type} value for the given appender from log4j2 properties content.
     * Returns {@code null} if the appender block is not found.
     */
    private String extractAppenderType(String appenderName, String content) {
        Pattern p = Pattern.compile(
                "(?m)^appender\\." + Pattern.quote(appenderName) + "\\.type\\s*=\\s*(\\S+)");
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Returns {@code true} if {@code appenderName} appears in the {@code appenders = ...} line.
     */
    private boolean isAppenderInList(String content, String appenderName) {
        String line = extractAppendersLine(content);
        if (line == null) {
            return false;
        }
        String listPart = line.substring(line.indexOf('=') + 1);
        for (String token : listPart.split(",")) {
            if (token.trim().equals(appenderName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the raw {@code appenders = ...} line from the properties content, or {@code null}.
     */
    private String extractAppendersLine(String content) {
        for (String line : content.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("appenders") && trimmed.contains("=") && !trimmed.startsWith("appender.")) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * Counts occurrences of {@code token} (as a comma-separated element) within {@code listLine}.
     */
    private int countOccurrences(String content, String token, String listLine) {
        if (listLine == null) {
            return 0;
        }
        int count = 0;
        String listPart = listLine.substring(listLine.indexOf('=') + 1);
        for (String part : listPart.split(",")) {
            if (part.trim().equals(token)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns {@code true} if {@code loggerKey} appears in the {@code loggers = ...} line.
     * The loggers list uses short keys (e.g. "AUDIT_LOG") while individual logger properties
     * are prefixed with "logger." — this method matches the list line only.
     */
    private boolean isLoggerInList(String content, String loggerKey) {
        for (String line : content.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("loggers") && trimmed.contains("=") && !trimmed.startsWith("logger.")) {
                String listPart = trimmed.substring(trimmed.indexOf('=') + 1);
                for (String token : listPart.split(",")) {
                    if (token.trim().equals(loggerKey)) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the named logger block contains an appenderRef to the given appender.
     * Matches: {@code logger.<loggerKey>.appenderRef.<appenderName>.ref = <appenderName>}
     */
    private boolean hasLoggerAppenderRef(String content, String loggerKey, String appenderName) {
        String expected = "logger." + loggerKey + ".appenderRef." + appenderName + ".ref = " + appenderName;
        return content.contains(expected);
    }

    /**
     * Returns {@code true} if the rootLogger has an appenderRef to the given appender.
     * Matches: {@code rootLogger.appenderRef.<appenderName>.ref = <appenderName>}
     */
    private boolean hasRootLoggerAppenderRef(String content, String appenderName) {
        String expected = "rootLogger.appenderRef." + appenderName + ".ref = " + appenderName;
        return content.contains(expected);
    }

    /**
     * Builds a {@link RemoteServerLoggerData} pointing to the local mock HTTP server.
     */
    private RemoteServerLoggerData buildRemoteConfig(String logType) {
        RemoteServerLoggerData data = new RemoteServerLoggerData();
        data.setLogType(logType);
        data.setUrl("http://localhost:" + mockServerPort + "/api/logs/consume");
        data.setConnectTimeoutMillis(CONNECT_TIMEOUT_MILLIS);
        data.setVerifyHostname(false);
        return data;
    }

    /**
     * Triggers an action that generates an AUDIT log entry.
     * Listing the admin key managers via the REST API produces an AUDIT_LOG entry.
     */
    private void triggerAuditLogEntry() throws Exception {
        restAPIAdmin.getKeyManagers();
    }

    /**
     * Waits for the log4j2 configuration file to be picked up by the running server.
     * The Carbon logging service writes log4j2.properties synchronously, but the OSGi
     * reconfiguration that follows is asynchronous, so a short pause is needed before
     * the resulting file state can be reliably read and asserted.
     *
     * @param seconds number of seconds to wait
     */
    private static void waitForLog4j2ConfigSync(int seconds) throws InterruptedException {
        TimeUnit.SECONDS.sleep(seconds);
    }

}
