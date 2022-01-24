/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.other;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.testng.annotations.Test;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import static org.testng.Assert.assertEquals;

/**
 * This test case is used to test the health check APIs of APIM runtime
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class APIMRuntimeHealthChecksTestCase extends APIManagerLifecycleBaseTest {
    private final String PROTOCOL_HTTP = "http";
    private final String PROTOCOL_HTTPS = "https";
    private final String GATEWAY_HTTP_PORT = "10263";
    private final String GATEWAY_HTTPS_PORT = "9943";
    private final String TRAFFIC_MANAGER_HTTP_PORT = "5672";
    private final String TRAFFIC_MANAGER_HTTPS_PORT = "10111";
    private final String KEY_MANAGER_HTTP_PORT = "10173";
    private final String KEY_MANAGER_HTTPS_PORT = "9943";
    private final String WEB_SOCKET_HTTP_PORT = "9599";
    private final String WEB_SOCKET_HTTPS_PORT = "8599";
    private final String URL = "%s://localhost:%s/services/Version";
    private final String GET = "GET";
    private final OkHttpClient client = new OkHttpClient().newBuilder().build();

    @Test(groups = {"wso2.am"}, description = "Testing gateway ports")
    public void testGatewayPortsHealthCheckAPI() throws Exception {
        Request httpRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTP, GATEWAY_HTTP_PORT))
                .method(GET, null)
                .build();
        Response httpResponse = client.newCall(httpRequest).execute();
        assertEquals(httpResponse.code(), 200);

        Request httpsRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTPS, GATEWAY_HTTPS_PORT))
                .method(GET, null)
                .build();
        Response httpsResponse = client.newCall(httpsRequest).execute();
        assertEquals(httpsResponse.code(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Testing traffic manager ports")
    public void testTrafficManagerPortsHealthCheckAPI() throws Exception {
        Request httpRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTP, TRAFFIC_MANAGER_HTTP_PORT))
                .method(GET, null)
                .build();
        Response httpResponse = client.newCall(httpRequest).execute();
        assertEquals(httpResponse.code(), 200);

        Request httpsRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTPS, TRAFFIC_MANAGER_HTTPS_PORT))
                .method(GET, null)
                .build();
        Response httpsResponse = client.newCall(httpsRequest).execute();
        assertEquals(httpsResponse.code(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Testing key manager ports")
    public void testKeyManagerPortsHealthCheckAPI() throws Exception {
        Request httpRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTP, KEY_MANAGER_HTTP_PORT))
                .method(GET, null)
                .build();
        Response httpResponse = client.newCall(httpRequest).execute();
        assertEquals(httpResponse.code(), 200);

        Request httpsRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTPS, KEY_MANAGER_HTTPS_PORT))
                .method(GET, null)
                .build();
        Response httpsResponse = client.newCall(httpsRequest).execute();
        assertEquals(httpsResponse.code(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Testing web socket ports")
    public void testWebSocketPortsHealthCheckAPI() throws Exception {
        Request httpRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTP, WEB_SOCKET_HTTP_PORT))
                .method(GET, null)
                .build();
        Response httpResponse = client.newCall(httpRequest).execute();
        assertEquals(httpResponse.code(), 200);

        Request httpsRequest = new Request.Builder()
                .url(String.format(URL, PROTOCOL_HTTPS, WEB_SOCKET_HTTPS_PORT))
                .method(GET, null)
                .build();
        Response httpsResponse = client.newCall(httpsRequest).execute();
        assertEquals(httpsResponse.code(), 200);
    }
}
