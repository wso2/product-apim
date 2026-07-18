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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps that act on the APIM server process itself (as opposed to its REST resources). Used by features that
 * must survive a server restart — e.g. token persistence. The block hosting these MUST run sequentially
 * ({@code thread-count=1}) since a restart bounces the shared container's server out from under any concurrent
 * class, and its container overlay must enable {@code [server] enable_restart_from_api}.
 */
public class ServerLifecycleSteps {

    /**
     * Gracefully restarts the APIM server in place via the Carbon {@code ServerAdmin} admin service
     * ({@code restartGracefully}). The container is NOT touched — only the carbon JVM bounces — so host ports
     * and the (in-container) database survive, which is what makes token-persistence-across-restart testable.
     * Blocks until the server has gone down and come back ready (see {@link ServerReadiness#awaitRestart});
     * fails if it does not return within {@code SERVER_STARTUP_WAIT_TIME}.
     */
    @When("I gracefully restart the API Manager server")
    public void iGracefullyRestartTheApiManagerServer() throws Exception {

        String baseUrl = TestContext.get("baseUrl").toString();
        String endpoint = baseUrl + "services/ServerAdmin.ServerAdminHttpsSoap11Endpoint/";

        String soapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"http://org.apache.axis2/xsd\"><soapenv:Header/><soapenv:Body>"
                + "<xsd:restartGracefully/></soapenv:Body></soapenv:Envelope>";

        String credentials = Constants.SUPER_TENANT_ADMIN_USERNAME + ":" + Constants.SUPER_TENANT_ADMIN_PASSWORD;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + basicAuth);
        headers.put("SOAPAction", "urn:restartGracefully");

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(endpoint, headers, soapBody, "text/xml;charset=UTF-8");

        Assert.assertEquals(response.getResponseCode(), 200,
                "ServerAdmin restartGracefully call failed: " + response.getData());
        Assert.assertTrue(response.getData().contains("true"),
                "ServerAdmin restartGracefully did not return true: " + response.getData());

        boolean restarted = ServerReadiness.awaitRestart(baseUrl);
        Assert.assertTrue(restarted, "APIM server did not come back ready within "
                + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s after a graceful restart");
    }
}
