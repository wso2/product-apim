/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.utils.clients;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpPost;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleHTTPClientDeadlineTest {
    @DataProvider
    public Object[][] stalledPhases() {
        return new Object[][]{{false}, {true}};
    }

    @Test(dataProvider = "stalledPhases")
    public void abortsHeaderAndBodyStallsAndRestoresTheEnclosingScope(boolean headersSent) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/slow", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                if (headersSent) {
                    exchange.sendResponseHeaders(200, 2);
                    exchange.getResponseBody().write('a');
                    exchange.getResponseBody().flush();
                }
                release.await(3, TimeUnit.SECONDS);
                if (!headersSent) {
                    exchange.sendResponseHeaders(200, 2);
                    exchange.getResponseBody().write('a');
                }
                exchange.getResponseBody().write('b');
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException expectedAfterClientAbort) {
                // The response deliberately outlives the client's deadline.
            } finally {
                exchange.close();
            }
        });
        server.createContext("/fast", exchange -> {
            exchange.sendResponseHeaders(200, 2);
            exchange.getResponseBody().write("ok".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            exchange.close();
        });
        server.start();
        SimpleHTTPClient client = SimpleHTTPClient.getInstance();
        String base = "http://localhost:" + server.getAddress().getPort();
        long started = System.nanoTime();
        try {
            Assert.expectThrows(IOException.class, () -> client.withRequestDeadline(150,
                    () -> client.withRequestDeadline(3000, () -> client.withSocketTimeout(3000,
                            () -> client.withoutAutomaticRetries(() -> client.doGet(base + "/slow", Map.of()))))));
            Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1500,
                    "A longer nested scope or socket timeout must not extend the enclosing deadline");
            release.countDown();
            Assert.assertEquals(client.withRequestDeadline(2000,
                    () -> client.doGet(base + "/fast", Map.of())).getResponseCode(), 200);
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    @Test
    public void boundsManagementLockAcquisitionWithoutSendingTheMutation() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.close();
        });
        server.start();
        String url = "http://localhost:" + server.getAddress().getPort() + "/api/am/devportal/v3/applications";
        var lock = SimpleHTTPClient.managementLockFor(new HttpPost(url));
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        var owner = executor.submit(() -> {
            lock.lock();
            try {
                locked.countDown();
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        try {
            Assert.assertTrue(locked.await(2, TimeUnit.SECONDS));
            long start = System.nanoTime();
            SimpleHTTPClient client = SimpleHTTPClient.getInstance();
            Assert.expectThrows(IOException.class, () -> client.withRequestDeadline(150,
                    () -> client.doPost(url, Map.of(), "{}", "application/json")));
            Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1500);
            Assert.assertEquals(requests.get(), 0);
        } finally {
            release.countDown();
            owner.get(3, TimeUnit.SECONDS);
            executor.shutdownNow();
            server.stop(0);
        }
    }
}
