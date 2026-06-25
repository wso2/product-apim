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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase C.2 verification (Type-A, no Docker): concurrent shared-scope write stress on {@link TestContext}.
 *
 * <p>The parallel-on-shared-container lane relies on a single static {@code TestContext} store keyed by
 * {@code suiteName::testName} (shared) and per-class (local). Under {@code parallel=tests} every block writes
 * its shared vars on its own thread, and under {@code parallel=classes} several class threads in one block
 * write into that block's single shared map at once. This test exercises exactly those two contention
 * dimensions directly, far harder and more deterministically than the TestNG scheduler would: it spins up
 * {@code SCOPES * THREADS_PER_SCOPE} worker threads, holds them all on a {@link CyclicBarrier} so they begin
 * writing at the same instant, and has every thread hammer {@code setShared}/{@code get}/{@code addToList}.
 *
 * <p>It asserts the three guarantees C.2 requires:
 * <ul>
 *   <li><b>No lost updates</b> — every distinct key written by every thread is afterwards readable with its
 *       exact value, both as each thread reads back its own writes and as a final cross-thread sweep over
 *       each scope's whole keyset.</li>
 *   <li><b>No {@code ConcurrentModification}/{@code ClassCast}</b> — interleaved reads during writes and the
 *       {@code getList} cast never throw; any worker throwable is collected and fails the test.</li>
 *   <li><b>Strict per-block isolation under contention</b> — every thread in a scope stamps the same
 *       {@code scopeOwner} sentinel; a thread (and the final sweep) must never observe another scope's owner,
 *       proving the {@code suiteName::testName} namespace does not bleed even when all scopes write at once.
 *       Per-thread local lists likewise stay isolated (each holds exactly its own appends).</li>
 * </ul>
 *
 * <p>Writes a marker file ({@code target/fv-c2-stress.txt}) with the run magnitude and {@code errors=0} so the
 * verify script can confirm the stress actually executed (not silently skipped) at the expected scale.
 */
public class TestContextConcurrencyStressTest {

    private static final Log logger = LogFactory.getLog(TestContextConcurrencyStressTest.class);

    /** Simulated parallel blocks (distinct shared scopes), each contended by several class threads. */
    private static final int SCOPES = 8;
    /** Threads per scope = simulated parallel classes writing into one block's shared map at once. */
    private static final int THREADS_PER_SCOPE = 4;
    /** Distinct shared keys each thread writes (and reads back) under contention. */
    private static final int KEYS_PER_THREAD = 500;

    private static final String OWNER_KEY = "scopeOwner";
    private static final Path MARKER = Paths.get("target", "fv-c2-stress.txt");

    @Test
    public void concurrentSharedScopeWritesAreIsolatedAndLossless() throws Exception {

        final int totalThreads = SCOPES * THREADS_PER_SCOPE;
        final long expectedWrites = (long) totalThreads * KEYS_PER_THREAD;

        CyclicBarrier startGate = new CyclicBarrier(totalThreads);
        CountDownLatch done = new CountDownLatch(totalThreads);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicLong writes = new AtomicLong();
        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);

        for (int s = 0; s < SCOPES; s++) {
            for (int t = 0; t < THREADS_PER_SCOPE; t++) {
                final int scope = s;
                final int thread = t;
                pool.submit(() -> {
                    try {
                        // Each worker pins itself to its own block's shared scope and a per-thread local scope.
                        TestContext.setScope(sharedId(scope), localId(scope, thread));
                        // Every thread in a scope stamps the SAME owner value: cross-scope bleed is detectable,
                        // intra-scope idempotent writes are not a "lost update".
                        TestContext.setShared(OWNER_KEY, scope);

                        startGate.await(30, TimeUnit.SECONDS);   // release all writers simultaneously

                        for (int i = 0; i < KEYS_PER_THREAD; i++) {
                            String key = key(thread, i);          // distinct per (thread,i): no legitimate overwrite
                            String val = val(scope, thread, i);
                            TestContext.setShared(key, val);
                            writes.incrementAndGet();
                            TestContext.addToList("events", val); // per-thread LOCAL list: must stay isolated

                            Object back = TestContext.get(key);   // read during concurrent writes
                            if (!val.equals(back)) {
                                throw new AssertionError("lost/garbled write scope=" + scope
                                        + " key=" + key + " expected=" + val + " got=" + back);
                            }
                            Object owner = TestContext.get(OWNER_KEY);
                            if (!Integer.valueOf(scope).equals(owner)) {
                                throw new AssertionError("ISOLATION BREACH scope=" + scope
                                        + " observed foreign owner=" + owner);
                            }
                        }

                        List<Object> events = TestContext.getList("events");
                        if (events.size() != KEYS_PER_THREAD) {
                            throw new AssertionError("local list contention scope=" + scope + " thread="
                                    + thread + " size=" + events.size() + " (expected " + KEYS_PER_THREAD + ")");
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
        }

        boolean finished = done.await(2, TimeUnit.MINUTES);
        pool.shutdownNow();
        Assert.assertTrue(finished, "stress workers did not finish within 2 minutes (possible deadlock/stall)");
        Assert.assertTrue(errors.isEmpty(), errors.size() + " worker error(s) under contention; first:\n"
                + (errors.isEmpty() ? "" : stackOf(errors.get(0))));
        Assert.assertEquals(writes.get(), expectedWrites, "unexpected total write count");

        // Final cross-thread sweep: every scope's shared map must hold EVERY key from ALL its threads with the
        // exact value (no lost update across threads), the right owner (isolation), and no foreign keys.
        for (int s = 0; s < SCOPES; s++) {
            TestContext.setScope(sharedId(s), "C2-verify-" + s);   // fresh local scope; reads fall through to shared
            Assert.assertEquals(TestContext.get(OWNER_KEY), s, "wrong scopeOwner for scope " + s);
            for (int t = 0; t < THREADS_PER_SCOPE; t++) {
                for (int i = 0; i < KEYS_PER_THREAD; i++) {
                    Assert.assertEquals(TestContext.get(key(t, i)), val(s, t, i),
                            "missing/garbled shared key after contention scope=" + s + " " + key(t, i));
                }
            }
        }

        long scopeCount = TestContext.sharedScopeCount();
        Assert.assertTrue(scopeCount >= SCOPES,
                "expected at least " + SCOPES + " retained shared scopes, got " + scopeCount);

        writeMarker(expectedWrites, scopeCount);
        logger.info("C.2 stress: " + totalThreads + " threads x " + KEYS_PER_THREAD + " keys across "
                + SCOPES + " scopes = " + expectedWrites + " writes, 0 errors, " + scopeCount
                + " shared scopes");
    }

    private static String sharedId(int scope) {
        return "C2-shared-" + scope;
    }

    private static String localId(int scope, int thread) {
        return "C2-local-" + scope + "-" + thread;
    }

    private static String key(int thread, int i) {
        return "k-" + thread + "-" + i;
    }

    private static String val(int scope, int thread, int i) {
        return scope + ":" + thread + ":" + i;
    }

    private static String stackOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void writeMarker(long expectedWrites, long scopeCount) throws Exception {
        if (MARKER.getParent() != null) {
            Files.createDirectories(MARKER.getParent());
        }
        String body = "scopes=" + SCOPES + "\n"
                + "threadsPerScope=" + THREADS_PER_SCOPE + "\n"
                + "keysPerThread=" + KEYS_PER_THREAD + "\n"
                + "totalWrites=" + expectedWrites + "\n"
                + "sharedScopeCount=" + scopeCount + "\n"
                + "errors=0\n";
        Files.writeString(MARKER, body);
    }
}
