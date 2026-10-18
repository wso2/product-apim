/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.wso2.am.integration.cucumbertests.utils.clients;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/** Selected only by the focused HTTP client verification suite. */
public class SimpleHTTPClientConcurrencyTest {
    private static final String ORIGIN = "https://localhost:9443";
    private static final String ERROR = "{\"code\":900967,\"message\":\"General Error\"}";

    @DataProvider
    public Object[][] providerRoutes() {
        return new Object[][] {
                {"/api/am/devportal/v3/applications"},
                {"/api/am/devportal/v3/applications/one/generate-keys"},
                {"/api/am/devportal/v3/applications/two/oauth-keys/key/cleanup-registration"},
                {"/api/am/devportal/v3/applications/import"},
                {"/api/am/admin/v4/applications/one/change-owner"},
                {"/client-registration/v0.17/register"},
                {"/api/identity/oauth2/dcr/v1.1/register"},
                {"/api/identity/oauth2/dcr/v1.1/register/client-id"},
                {"/services/IdentityApplicationManagementService"},
                {"/services/OAuthAdminService"},
                {"/t/tenant1.com/services/IdentityApplicationManagementService"},
                {"/t/tenant2.com/services/OAuthAdminService"},
                {"/api/server/v1/applications"},
                {"/api/server/v1/applications/id/inbound-protocols/oidc"}
        };
    }

    @Test(dataProvider = "providerRoutes")
    public void providerMutationsShareThePublisherServerLock(String path) {
        Lock publisher = lock(new HttpPost(ORIGIN + "/api/am/publisher/v4/apis"));
        Assert.assertSame(lock(new HttpPost(ORIGIN + path)), publisher);
        Assert.assertSame(lock(new HttpDelete(ORIGIN + path)), publisher);
    }

    @DataProvider
    public Object[][] unguardedRoutes() {
        return new Object[][] {
                {"/oauth2/token"}, {"/token"}, {"/test-api/1.0.0/orders"},
                {"/api/am/gateway/v2/api-artifacts"}, {"/api/am/devportal/v3/subscriptions"},
                {"/api/am/devportal/v3/applications-other"}, {"/services/RemoteUserStoreManagerService"}
        };
    }

    @Test(dataProvider = "unguardedRoutes")
    public void gatewayAndUnrelatedTrafficRemainConcurrent(String path) {
        Assert.assertNull(lock(new HttpPost(ORIGIN + path)));
    }

    @Test
    public void differentServersHaveIndependentLocksAndDefaultPortsAreNormalized() {
        Assert.assertNotSame(lock(new HttpPost(ORIGIN + "/api/am/devportal/v3/applications")),
                lock(new HttpPost("https://localhost:9444/api/am/devportal/v3/applications")));
        Assert.assertSame(lock(new HttpPost("https://LOCALHOST/api/am/devportal/v3/applications")),
                lock(new HttpPost("https://localhost:443/services/OAuthAdminService")));
    }

    @Test
    public void concurrentReadersExcludeWritersAcrossDifferentTenantsAndApis() throws Exception {
        Lock read = lock(new HttpGet(ORIGIN + "/api/am/publisher/v4/apis"));
        read.lock();
        var executor = Executors.newSingleThreadExecutor();
        try {
            Assert.assertTrue(executor.submit(() -> tryLock(lock(new HttpGet(
                    ORIGIN + "/t/tenant1.com/api/am/devportal/v3/applications")))).get(5, TimeUnit.SECONDS));
            Assert.assertFalse(executor.submit(() -> tryLock(lock(new HttpPost(
                    ORIGIN + "/t/tenant2.com/services/IdentityApplicationManagementService"))))
                    .get(5, TimeUnit.SECONDS));
        } finally {
            read.unlock();
            executor.shutdownNow();
        }
    }

    @Test
    public void writerExcludesOtherApplicationMutationsAndReads() throws Exception {
        Lock write = lock(new HttpPost(ORIGIN + "/api/am/devportal/v3/applications/one/generate-keys"));
        write.lock();
        var executor = Executors.newSingleThreadExecutor();
        try {
            Assert.assertFalse(executor.submit(() -> tryLock(lock(new HttpDelete(
                    ORIGIN + "/t/tenant2.com/api/identity/oauth2/dcr/v1.1/register/other"))))
                    .get(5, TimeUnit.SECONDS));
            Assert.assertFalse(executor.submit(() -> tryLock(lock(new HttpGet(
                    ORIGIN + "/api/am/devportal/v3/applications/two")))).get(5, TimeUnit.SECONDS));
        } finally {
            write.unlock();
            executor.shutdownNow();
        }
    }

    @DataProvider
    public Object[][] mutationMethods() {
        return new Object[][] {{"POST"}, {"PUT"}, {"PATCH"}, {"DELETE"}, {"MULTIPART_POST"},
                {"MULTIPART_PUT"}, {"JSON_MULTIPART_POST"}, {"JSON_MULTIPART_PUT"}, {"SOAP"}};
    }

    @Test(dataProvider = "mutationMethods")
    public void failedMutationsReturnTheOriginalErrorWithoutReplay(String method) throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = errorServer(attempts);
        try {
            String url = "http://localhost:" + server.getAddress().getPort()
                    + "/api/am/devportal/v3/applications/one/generate-keys";
            SimpleHTTPClient client = SimpleHTTPClient.getInstance();
            HttpResponse response = switch (method) {
                case "POST" -> client.doPost(url, Map.of(), "{}", "application/json");
                case "PUT" -> client.doPut(url, Map.of(), "{}", "application/json");
                case "PATCH" -> client.doPatch(url, Map.of(), "{}", "application/json");
                case "DELETE" -> client.doDelete(url, Map.of());
                case "MULTIPART_POST" -> client.doPostMultipartWithFiles(url, Map.of(), Map.of(), Map.of());
                case "MULTIPART_PUT" -> client.doPutMultipartWithFiles(url, Map.of(), Map.of(), Map.of());
                case "JSON_MULTIPART_POST" -> client.doPostMultipartWithJsonFields(
                        url, Map.of(), Map.of(), Map.of(), Map.of());
                case "JSON_MULTIPART_PUT" -> client.doPutMultipartWithJsonFields(
                        url, Map.of(), Map.of(), Map.of(), Map.of());
                case "SOAP" -> client.sendSoapRequest(url, "<Envelope/>", "urn:updateApplication", "u", "p");
                default -> throw new AssertionError(method);
            };
            Assert.assertEquals(response.getResponseCode(), 500);
            Assert.assertEquals(response.getData(), ERROR);
            Assert.assertEquals(attempts.get(), 1, "A failed mutation must not be replayed");
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void safeReadStillRetriesTheGenericError() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        HttpServer server = errorServer(attempts);
        try {
            HttpResponse response = SimpleHTTPClient.getInstance().doGet(
                    "http://localhost:" + server.getAddress().getPort() + "/api/am/publisher/v4/apis", Map.of());
            Assert.assertEquals(response.getResponseCode(), 200);
            Assert.assertEquals(attempts.get(), 2);
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer errorServer(AtomicInteger attempts) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            int status = attempts.incrementAndGet() == 1 ? 500 : 200;
            byte[] body = (status == 500 ? ERROR : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(body);
            }
        });
        server.start();
        return server;
    }

    private static Lock lock(HttpUriRequest request) {
        return SimpleHTTPClient.managementLockFor(request);
    }

    private static boolean tryLock(Lock lock) {
        if (!lock.tryLock()) {
            return false;
        }
        lock.unlock();
        return true;
    }
}
