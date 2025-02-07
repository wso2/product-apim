/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.analytics;

import jdk.internal.joptsimple.internal.Strings;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
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
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.FileManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.ws.rs.core.MediaType;

import static org.testng.Assert.assertEquals;

public class APIMAnalyticsTest extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "APITest";
    private static final String API_CONTEXT = "/api";
    private static final String API_VERSION = "1.0.0";
    private static final String API_ENDPOINT_METHOD = "customers/123";
    private static final String API_ENDPOINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String BEARER = "Bearer ";
    private static final String LOG4J_PROPERTIES_FILE = "log4j2.properties";
    private static final String OLD_LOG4J_PROPERTIES_FILE = "log4j2.properties.old";
    private static final String DEPLOYMENT_CONFIG_FILE = "deployment.toml";
    private static final String METRIC_LOG_FILE = "apim_metrics.log";
    private static final String TEST_RESOURCE_LOCATION = "configFiles/logAnalyticsEnabled";
    private static final String CONFIG_PATH = "repository/conf";
    private static final String LOG_PATH = "repository/logs";
    private static final String EXPECTED_LOG_OUTPUT = "INFO ELKCounterMetric apimMetrics: apim:response, properties :"
            + "{\"apiName\":\"APITest\",\"proxyResponseCode\":200,";
    private static final String API_FAILED_TO_DEPLOY_ERROR_MESSAGE =
            "Failed to deploy API: Analytics logs were not printed as expected.";
    private static final String RESPONSE_CODE_MISMATCH_ERROR_MESSAGE = "Response code mismatch";
    private String apiEndPointUrl;
    private String apiId;
    private String applicationId;
    private String log4jPropertiesFilePath;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMAnalyticsTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setUpEnvironment() throws Exception {
        super.init(userMode);
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                                             APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                                                             TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(
                getAMResourceLocation() + File.separator + TEST_RESOURCE_LOCATION + File.separator
                        + DEPLOYMENT_CONFIG_FILE));
        log4jPropertiesFilePath =
                serverConfigurationManager.getCarbonHome() + File.separator + CONFIG_PATH + File.separator
                        + LOG4J_PROPERTIES_FILE;
        FileManager.copyFile(new File(getAMResourceLocation() + File.separator + TEST_RESOURCE_LOCATION + File.separator
                                              + LOG4J_PROPERTIES_FILE), log4jPropertiesFilePath);
        serverConfigurationManager.restartGracefully();

        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_ENDPOINT_POSTFIX_URL;
    }

    @Test(description = "Enable APIM Log Analytics and invoke an API")
    public void invokeAPIWithLogAnalyticsEnabled() throws Exception {

        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(user.getUserName());
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, Strings.EMPTY,
                                                                          APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiId, applicationId, TIER_GOLD);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId,
                                                                        APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                                                                        null,
                                                                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                                                                        null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        requestHeaders.put(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
        HttpResponse apiInvokeResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttps(API_CONTEXT.replace(File.separator, Strings.EMPTY), API_VERSION)
                        + File.separator + API_ENDPOINT_METHOD, requestHeaders);
        assertEquals(apiInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, RESPONSE_CODE_MISMATCH_ERROR_MESSAGE);

        String metricLogs = readFileContent(
                serverConfigurationManager.getCarbonHome() + File.separator + LOG_PATH + File.separator
                        + METRIC_LOG_FILE);
        Assert.assertTrue(API_FAILED_TO_DEPLOY_ERROR_MESSAGE, metricLogs.contains(EXPECTED_LOG_OUTPUT));
    }

    public String readFileContent(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        FileManager.copyFile(new File(getAMResourceLocation() + File.separator + TEST_RESOURCE_LOCATION + File.separator
                                              + OLD_LOG4J_PROPERTIES_FILE), log4jPropertiesFilePath);
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
