/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.testng.annotations.Factory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import java.io.File;
import java.net.URL;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class APISecurityAuditTestCase extends APIManagerLifecycleBaseTest {

    private final String AUDIT_API_NAME = "TestAuditAPI";
    private final String API_CONTEXT = "audit";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String ENDPOINT_URL = "https://localhost:9443/am-auditAPI-sample/api/auditapi/";

    private ServerConfigurationManager serverConfigurationManager;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APISecurityAuditTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String apiManagerXml =
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "apiSecurityAudit" +
                        File.separator + "deployment.toml";
        configureServer(apiManagerXml);
    }
    @Test(groups = {"wso2.am"}, description = "get security audit report for api")
    public void testGetAuditAPI() throws Exception {
//        String apiName = "AuditAPI";
//        String apiContext = "testAuditAPI";
//        String apiVersion = "1.0";
//        String url = getGatewayURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test for API Security Audit";
        APIRequest apiRequest = new APIRequest(AUDIT_API_NAME, API_CONTEXT, new URL(ENDPOINT_URL));
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(user.getUserName());
        //Create and Publish API
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        waitForAPIDeploymentSync(user.getUserName(), AUDIT_API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);
        //Get Security Audit Report Response
        HttpResponse auditReportResponse = restAPIPublisher.getAuditApi(apiId);
        assertNotNull(auditReportResponse);
        assertEquals(auditReportResponse.getResponseCode(), 200);
    }
    private void configureServer(String apiManagerXml) throws Exception {
        try {
            serverConfigurationManager = new ServerConfigurationManager(publisherContext);
            serverConfigurationManager.applyConfiguration(new File(apiManagerXml));
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Error while changing server configuration", e);
        }
    }
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}