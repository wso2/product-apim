/*
 *Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.server.restart;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.utils.ServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class APILoggingServerRestartTest extends APIManagerLifecycleBaseTest {

    private String apiLoggingApplicationId;

    @BeforeClass(alwaysRun = true)
    public void initialize(ITestContext ctx) throws Exception {
        super.init();
        apiLoggingApplicationId = (String) ctx.getAttribute("apiLoggingApplicationId");
    }

    @Test(groups = {"wso2.am" }, description = "Sending http request to per API logging enabled API: ")
    public void testAPIPerAPILoggingTestcase() throws Exception {

        String API_NAME = "APILoggingTestAPI";
        String API_CONTEXT = "apiloggingtest";
        String API_VERSION = "1.0.0";

        waitForAPIDeploymentSync(user.getUserName(), "APILoggingTestAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        // Invoke the API
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(apiLoggingApplicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        // Validate API Logs
        String apiLogFilePath = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository"
                + File.separator + "logs" + File.separator + "api.log";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(apiLogFilePath));
        String logLine;
        while ((logLine = bufferedReader.readLine()) != null) {
            Assert.assertTrue(logLine.contains("INFO {API_LOG} " + API_NAME));
        }
    }
}
