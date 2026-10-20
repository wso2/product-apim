/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.sun.net.httpserver.HttpServer;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegistryAdminServiceTest {
    private static final String EVENT = "/_system/governance/event";
    private static final String THROTTLE_SUBSCRIPTIONS = EVENT + "/topics/throttledata/jms.subscriptions";
    private static final List<String> PATHS = List.of("/_system", "/_system/governance", EVENT,
            EVENT + "/topics", EVENT + "/topics/throttledata");

    @Test
    public void existingHierarchyRequiresNoWrites() throws Exception {
        Fixture client = new Fixture();
        Assert.assertFalse(ensure(client, 2000, () -> { }));
        Assert.assertTrue(client.creates.isEmpty());
        Assert.assertEquals(client.reads, PATHS);
    }

    @Test
    public void createsEachMissingCollectionOnceAndVerifiesIt() throws Exception {
        Fixture client = missingHierarchy();
        Assert.assertTrue(ensure(client, 180000, () -> { }));
        Assert.assertEquals(client.present, new HashSet<>(PATHS));
        Assert.assertEquals(client.creates.size(), 3);
        client.creates.values().forEach(count -> Assert.assertEquals(count.intValue(), 1));
        Assert.assertTrue(client.readTimeouts.stream().allMatch(timeout -> timeout > 0 && timeout <= 10000));
        Assert.assertTrue(client.writeTimeouts.stream().allMatch(timeout -> timeout > 0 && timeout <= 60000));
        Assert.assertEquals(client.writeTimeouts.get(0).intValue(), 60000,
                "A mutation may use more than the former ten-second limit while reserving verification time");
    }

    @Test
    public void precreatesDistributedThrottleSubscriptionCollectionBeforeConsumersStart() throws Exception {
        Fixture client = new Fixture();
        Assert.assertFalse(ensure(client, 2000, () -> { }));

        Assert.assertTrue(ensureSubscriptions(client, 2000, () -> { }));
        Assert.assertTrue(client.present.contains(THROTTLE_SUBSCRIPTIONS));
        Assert.assertEquals(client.creates.get(THROTTLE_SUBSCRIPTIONS).intValue(), 1);
        Assert.assertEquals(client.reads.get(client.reads.size() - 1), THROTTLE_SUBSCRIPTIONS);
    }

    @Test
    public void existingThrottleSubscriptionCollectionRequiresNoWrite() throws Exception {
        Fixture client = new Fixture();
        client.present.add(THROTTLE_SUBSCRIPTIONS);

        Assert.assertFalse(ensureSubscriptions(client, 2000, () -> { }));
        Assert.assertTrue(client.creates.isEmpty());
        Assert.assertEquals(client.reads.get(client.reads.size() - 1), THROTTLE_SUBSCRIPTIONS);
    }

    @Test
    public void reconcilesACommittedWriteAfterItsResponseTimesOut() throws Exception {
        Fixture client = missingHierarchy();
        client.uncertainWrite = true;
        AtomicInteger diagnoses = new AtomicInteger();
        Assert.assertTrue(ensure(client, 2000, diagnoses::incrementAndGet));
        Assert.assertEquals(client.creates.get(EVENT).intValue(), 1);
        Assert.assertEquals(diagnoses.get(), 1);
    }

    @Test
    public void awaitsVisibilityWithoutReplayingTheTimedOutWrite() throws Exception {
        Fixture client = missingHierarchy();
        client.uncertainWrite = true;
        client.visibilityDelay = 1;
        Assert.assertTrue(ensure(client, 2000, () -> { }));
        Assert.assertEquals(client.creates.get(EVENT).intValue(), 1);
    }

    @Test
    public void failsAnUnresolvedWriteAtTheSharedDeadlineWithoutReplayingIt() throws Exception {
        Fixture client = missingHierarchy();
        client.uncertainWrite = true;
        client.commit = false;
        long start = System.nanoTime();
        IOException failure = Assert.expectThrows(IOException.class, () -> ensure(client, 300, () -> { }));
        Assert.assertTrue(failure.getMessage().contains("outcome could not be verified"));
        Assert.assertTrue(failure.getCause() instanceof SocketTimeoutException);
        Assert.assertEquals(client.creates.get(EVENT).intValue(), 1);
        Assert.assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1500);
    }

    @Test
    public void retriesTransientReadTimeoutsWithinTheInitializationWindow() throws Exception {
        Fixture client = new Fixture();
        client.readFailures = 1;
        AtomicInteger diagnoses = new AtomicInteger();
        Assert.assertFalse(ensure(client, 2000, diagnoses::incrementAndGet));
        Assert.assertTrue(client.creates.isEmpty());
        Assert.assertEquals(diagnoses.get(), 1);
    }

    @DataProvider
    public Object[][] rejectedReads() {
        return new Object[][]{{401, "denied"}, {403, "denied"}, {500, fault("Database unavailable")},
                {500, fault("Resource does not exist at path /another-path")}};
    }

    @Test(dataProvider = "rejectedReads")
    public void doesNotTreatAuthenticationStorageOrUnrelatedFaultsAsMissingCollections(int status, String body) {
        Fixture client = new Fixture();
        client.forcedRead = new HttpResponse(body, status);
        IOException failure = Assert.expectThrows(IOException.class, () -> ensure(client, 2000, () -> { }));
        Assert.assertTrue(failure.getMessage().contains("could not inspect"));
        Assert.assertTrue(client.creates.isEmpty());
        Assert.assertEquals(client.reads.size(), 1);
    }

    @DataProvider
    public Object[][] invalidMetadata() {
        return new Object[][]{{"<response><resourceType>resource</resourceType></response>"},
                {"<response/>"}, {"<response><resourceType>collection</resourceType>"
                        + "<resourceType>collection</resourceType></response>"}};
    }

    @Test(dataProvider = "invalidMetadata")
    public void rejectsNonCollectionAndAmbiguousMetadata(String body) {
        Fixture client = new Fixture();
        client.forcedRead = new HttpResponse(body, 200);
        Assert.expectThrows(IOException.class, () -> ensure(client, 2000, () -> { }));
        Assert.assertEquals(client.reads.size(), 1);
        Assert.assertTrue(client.creates.isEmpty());
    }

    @Test
    public void doesNotMaskAnExplicitCreationFailureEvenIfTheResourceExistsAfterwards() {
        Fixture client = missingHierarchy();
        client.createStatus = 500;
        IOException failure = Assert.expectThrows(IOException.class, () -> ensure(client, 2000, () -> { }));
        Assert.assertTrue(failure.getMessage().contains("addCollection failed"));
        Assert.assertEquals(client.creates.get(EVENT).intValue(), 1);
    }

    @Test
    public void diagnosticFailureDoesNotChangeSuccessfulReconciliation() throws Exception {
        Fixture client = missingHierarchy();
        client.uncertainWrite = true;
        Assert.assertTrue(ensure(client, 2000, () -> { throw new IllegalStateException("diagnostic unavailable"); }));
        Assert.assertEquals(client.creates.get(EVENT).intValue(), 1);
    }

    @Test
    public void preservesInterruptionAndDoesNotContinueToMutations() {
        Fixture client = new Fixture();
        client.readFailures = 1;
        Thread.currentThread().interrupt();
        try {
            IOException failure = Assert.expectThrows(IOException.class, () -> ensure(client, 2000, () -> { }));
            Assert.assertTrue(failure.getMessage().contains("Interrupted"));
            Assert.assertTrue(Thread.currentThread().isInterrupted());
            Assert.assertTrue(client.creates.isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void realHttpLostCreationResponseIsNotReplayedAndDoesNotReplaceScenarioResponse() throws Exception {
        AtomicInteger creates = new AtomicInteger();
        AtomicBoolean committed = new AtomicBoolean();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/services/ResourceAdminService", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.contains("<ser:addCollection>")) {
                creates.incrementAndGet();
                committed.set(true);
                exchange.close(); // The product committed, but the client receives no HTTP response.
                return;
            }
            boolean missing = body.contains("<ser:paths>" + EVENT + "</ser:paths>") && !committed.get();
            byte[] response = (missing ? fault("Resource does not exist at path " + EVENT)
                    : "<response><resourceType>collection</resourceType></response>")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(missing ? 500 : 200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        HttpResponse sentinel = new HttpResponse("scenario assertion response", 201);
        Object previous = TestContext.get("httpResponse");
        TestContext.set("httpResponse", sentinel);
        try {
            User actor = new User();
            actor.setUserName("admin");
            actor.setPassword("admin");
            Assert.assertTrue(RegistryAdminService.ensureThrottleDataHierarchy(
                    "http://localhost:" + server.getAddress().getPort() + "/", actor, 2000, () -> { }));
            Assert.assertEquals(creates.get(), 1);
            Assert.assertSame(TestContext.get("httpResponse"), sentinel);
        } finally {
            server.stop(0);
            if (previous == null) {
                TestContext.remove("httpResponse");
            } else {
                TestContext.set("httpResponse", previous);
            }
        }
    }

    private static boolean ensure(Fixture client, long timeout, Runnable diagnostics) throws IOException {
        return RegistryAdminService.ensureThrottleDataHierarchy(client, timeout, diagnostics);
    }

    private static boolean ensureSubscriptions(Fixture client, long timeout, Runnable diagnostics)
            throws IOException {
        return RegistryAdminService.ensureThrottleDataSubscriptionsCollection(client, timeout, diagnostics);
    }

    private static Fixture missingHierarchy() {
        Fixture client = new Fixture();
        client.present.removeIf(path -> path.startsWith(EVENT));
        return client;
    }

    private static String fault(String message) {
        return "<Envelope><Body><Fault><faultstring>" + message + "</faultstring></Fault></Body></Envelope>";
    }

    private static final class Fixture implements RegistryAdminService.RegistryClient {
        private final Set<String> present = new HashSet<>(PATHS);
        private final Map<String, Integer> creates = new HashMap<>();
        private final List<String> reads = new ArrayList<>();
        private final List<Integer> readTimeouts = new ArrayList<>();
        private final List<Integer> writeTimeouts = new ArrayList<>();
        private boolean uncertainWrite;
        private boolean commit = true;
        private int visibilityDelay;
        private int readFailures;
        private int createStatus = 200;
        private HttpResponse forcedRead;

        @Override
        public HttpResponse read(String path, int timeout) throws IOException {
            reads.add(path);
            readTimeouts.add(timeout);
            if (readFailures-- > 0) {
                throw new SocketTimeoutException("read timed out");
            }
            if (forcedRead != null) {
                return forcedRead;
            }
            if (path.equals(EVENT) && creates.containsKey(EVENT) && visibilityDelay-- > 0) {
                return new HttpResponse(fault("Resource does not exist at path " + path), 500);
            }
            return present.contains(path)
                    ? new HttpResponse("<response><resourceType>collection</resourceType></response>", 200)
                    : new HttpResponse(fault("Resource does not exist at path " + path), 500);
        }

        @Override
        public HttpResponse create(String parent, String name, int timeout) throws IOException {
            writeTimeouts.add(timeout);
            String path = parent + "/" + name;
            creates.merge(path, 1, Integer::sum);
            Assert.assertTrue(present.contains(parent), "Parent must be verified before creating a child");
            if (commit) {
                present.add(path);
            }
            if (uncertainWrite && path.equals(EVENT)) {
                throw new SocketTimeoutException("write response timed out");
            }
            return new HttpResponse("<created/>", createStatus);
        }
    }
}
