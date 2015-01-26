/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.am.tests.header;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.WireMonitorServer;
import org.wso2.carbon.automation.api.clients.stratos.tenant.mgt.TenantMgtAdminServiceClient;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.LoginLogoutUtil;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;

import java.io.*;
import java.net.URL;

/**
 * Property Mediator FORCE_HTTP_CONTENT_LENGTH Property Test
 */

public class ContentLengthHeaderTestCase extends APIManagerIntegrationTest {
    public WireMonitorServer wireServer;
    private TenantMgtAdminServiceClient tenantMgtAdminServiceClient;
    private EnvironmentVariables amServer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(0);

        int userId = 16;
        userInfo = UserListCsvReader.getUserInfo(userId);
        EnvironmentBuilder builder = new EnvironmentBuilder().am(userId);
        amServer = builder.build().getAm();

        // UserInfo userInfo = UserListCsvReader.getUserInfo(16);
        LoginLogoutUtil loginUtil = new LoginLogoutUtil(9443, "localhost");

        String sessionCookieAdmin = loginUtil.login(userInfo.getUserName(), userInfo.getPassword(),
                amServer.getBackEndUrl());

        // https://localhost:9443/t/abc.com/services

        tenantMgtAdminServiceClient = new TenantMgtAdminServiceClient(amServer.getBackEndUrl(), sessionCookieAdmin);
        tenantMgtAdminServiceClient.addTenant("abc.com", "abc123", "abc", "demo");

        String apiMngrSynapseConfigPath = "/artifacts/AM/synapseconfigs/property/FORCE_HTTP_CONTENT_LENGTH.xml";
        String relativeFilePath = apiMngrSynapseConfigPath.replaceAll("[\\\\/]", File.separator);
        OMElement apiMngrSynapseConfig = esbUtils.loadClasspathResource(relativeFilePath);

        esbUtils.updateESBConfiguration(setEndpoints(apiMngrSynapseConfig), amServer.getBackEndUrl(),
                amServer.getSessionCookie());

        wireServer = new WireMonitorServer(8991);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = "wso2.am", description = "Test for reading the Content-Length header in the request")
    public void testFORCE_HTTP_CONTENT_LENGTHPropertyTest() throws Exception {

        wireServer.start();

        //axis2Client.sendSimpleStockQuoteRequest("http://localhost:8280/t/abc.com/helloabc/1.0.0", null, "WSO2");
        FileInputStream fis = new FileInputStream("/artifacts/AM/synapseconfigs/property/FORCE_HTTP_CONTENT_LENGTH.xml");
        InputStreamReader isr = new InputStreamReader(fis, "UTF8");
        Reader inputReader = new BufferedReader(isr);

        URL postEndpoint = new URL("http://localhost:8280/t/abc.com/stock/1.0.0");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("foo.out")));
        HttpRequestUtil.sendPostRequest(inputReader, postEndpoint, out);
        String response = wireServer.getCapturedMessage();
        Assert.assertTrue(response.contains("Content-Length"), "Content-Length not found in out going message");

    }

    @AfterClass(alwaysRun = true)
    public void stop() throws Exception {
        cleanup();
    }
}

