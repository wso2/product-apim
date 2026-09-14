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

package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Shared topology-aware implementation of the Carbon graceful restart operation. */
public final class GracefulServerRestart {

    private GracefulServerRestart() {
    }

    /** Restarts the node that serves Gateway traffic (the unified node in all-in-one topology). */
    public static void gateway() throws Exception {
        restart(Utils.getBaseGatewayManagementUrl(), false);
    }

    /** Restarts the Control Plane node (the unified node in all-in-one topology). */
    public static void controlPlane() throws Exception {
        restart(Utils.getBaseUrl(), true);
    }

    private static void restart(String baseUrl, boolean controlPlane) throws Exception {
        String endpoint = baseUrl + "services/ServerAdmin.ServerAdminHttpsSoap11Endpoint/";
        String soapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"http://org.apache.axis2/xsd\"><soapenv:Header/><soapenv:Body>"
                + "<xsd:restartGracefully/></soapenv:Body></soapenv:Envelope>";

        String credentials = Constants.SUPER_TENANT_ADMIN_USERNAME + ":" + Constants.SUPER_TENANT_ADMIN_PASSWORD;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + basicAuth);
        headers.put("SOAPAction", "urn:restartGracefully");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(endpoint, headers, soapBody,
                "text/xml;charset=UTF-8");
        Assert.assertNotNull(response, "ServerAdmin restartGracefully returned no response");
        Assert.assertEquals(response.getResponseCode(), 200,
                "ServerAdmin restartGracefully call failed: " + response.getData());
        Assert.assertTrue(response.getData() != null && !response.getData().isBlank(),
                "ServerAdmin restartGracefully returned no SOAP body to inspect");
        Assert.assertTrue(response.getData().contains("<ns:return>true</ns:return>"),
                "ServerAdmin restartGracefully did not return true; response: " + response.getData());

        boolean restarted = controlPlane
                ? ServerReadiness.awaitControlPlaneRestart(baseUrl)
                : ServerReadiness.awaitRestart(baseUrl);
        Assert.assertTrue(restarted, "APIM server did not come back ready within "
                + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s after a graceful restart");
    }
}
