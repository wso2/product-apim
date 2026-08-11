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

package org.wso2.am.integration.cucumbertests.verification;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.HealGate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verifies {@link HealGate}'s own behaviour. Its heal and fatal paths cannot be reached on a healthy container
 * run, so this is the only place they are exercised; the timing-injectable overload keeps the whole class under a
 * second and needs no container.
 */
public class HealGateVerificationTest {

    private static final int MAX_ATTEMPTS = 3;

    /** Drives the timing-injectable overload with millisecond windows. */
    private void run(String what, HealGate.Probe probe, HealGate.Heal heal, int maxAttempts) throws Exception {
        Method m = HealGate.class.getDeclaredMethod("awaitOrHeal", String.class, HealGate.Probe.class,
                HealGate.Heal.class, int.class, long.class, long.class, long.class);
        m.setAccessible(true);
        try {
            m.invoke(null, what, probe, heal, maxAttempts, 40L, 40L, 5L);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Error error) {
                throw error;             // AssertionError — the outcome under test
            }
            throw (Exception) e.getCause();
        }
    }

    @Test(description = "A probe that is ready immediately neither fails nor heals")
    public void readyFirstPollDoesNotHeal() throws Exception {
        AtomicInteger heals = new AtomicInteger();
        run("ready-thing", () -> new HealGate.Ready(),
                attempt -> { heals.incrementAndGet(); return new HealGate.Ready(); }, MAX_ATTEMPTS);
        Assert.assertEquals(heals.get(), 0, "A ready probe must not fire a re-trigger");
    }

    @Test(description = "FATAL fails immediately: no heal fires and the window is abandoned, not waited out")
    public void fatalFailsFastWithoutHealing() throws Exception {
        AtomicInteger polls = new AtomicInteger();
        AtomicInteger heals = new AtomicInteger();
        long start = System.currentTimeMillis();
        try {
            run("fatal-thing", () -> {
                polls.incrementAndGet();
                return new HealGate.Fatal("HTTP 401 from the artifact endpoint");
            }, attempt -> { heals.incrementAndGet(); return new HealGate.Ready(); }, MAX_ATTEMPTS);
            Assert.fail("Expected the gate to fail fast on a Fatal verdict");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("HTTP 401"),
                    "The failure must name the real cause, got: " + expected.getMessage());
            Assert.assertTrue(expected.getMessage().contains("failed fast"),
                    "The failure must say it did not wait out the window, got: " + expected.getMessage());
        }
        Assert.assertEquals(heals.get(), 0, "A Fatal verdict must NOT fire a mutating re-trigger");
        Assert.assertEquals(polls.get(), 1, "A Fatal verdict must abandon the window on the first observation");
        Assert.assertTrue(System.currentTimeMillis() - start < 3000,
                "Fatal must not consume the wait windows");
    }

    @Test(description = "A dropped event heals: window expires, re-trigger fires, next window sees it ready")
    public void notReadyThenHealThenReady() throws Exception {
        AtomicInteger heals = new AtomicInteger();
        AtomicInteger polls = new AtomicInteger();
        run("dropped-event-thing", () -> {
            polls.incrementAndGet();
            return heals.get() == 0 ? new HealGate.NotReady("404") : new HealGate.Ready();
        }, attempt -> { heals.incrementAndGet(); return new HealGate.Ready(); }, MAX_ATTEMPTS);
        Assert.assertEquals(heals.get(), 1, "Exactly one re-trigger should have been needed");
        Assert.assertTrue(polls.get() > 1, "The gate must have polled before healing");
    }

    @Test(description = "Never ready: fails after maxAttempts, reporting the heal count and last observation")
    public void neverReadyReportsHealsAndLastObserved() throws Exception {
        AtomicInteger heals = new AtomicInteger();
        try {
            run("never-ready-thing", () -> new HealGate.NotReady("state=Created"),
                    attempt -> { heals.incrementAndGet(); return new HealGate.Ready(); }, MAX_ATTEMPTS);
            Assert.fail("Expected exhaustion to fail");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("state=Created"),
                    "Must report what it last observed, got: " + expected.getMessage());
            Assert.assertTrue(expected.getMessage().contains("self-heal re-trigger"),
                    "Must report that heals were attempted, got: " + expected.getMessage());
        }
        Assert.assertEquals(heals.get(), MAX_ATTEMPTS - 1,
                "One re-trigger between each pair of windows");
    }

    @Test(description = "A re-trigger that reports FATAL aborts the gate instead of burning another window")
    public void fatalFromHealAborts() throws Exception {
        AtomicInteger heals = new AtomicInteger();
        try {
            run("bad-retrigger-thing", () -> new HealGate.NotReady("404"),
                    attempt -> {
                        heals.incrementAndGet();
                        return new HealGate.Fatal("deploy-revision returned 400 action not allowed");
                    }, MAX_ATTEMPTS);
            Assert.fail("Expected the gate to abort on a Fatal heal verdict");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("action not allowed"),
                    "Must name the re-trigger's own failure, got: " + expected.getMessage());
        }
        Assert.assertEquals(heals.get(), 1, "The gate must stop after the first aborting re-trigger");
    }

    @Test(description = "IOException from the probe is warm-up, not fatal: it keeps polling")
    public void ioExceptionIsNotReadyNotFatal() throws Exception {
        AtomicInteger polls = new AtomicInteger();
        run("warming-up-thing", () -> {
            if (polls.incrementAndGet() < 3) {
                throw new IOException("Connection refused");
            }
            return new HealGate.Ready();
        }, attempt -> new HealGate.Ready(), MAX_ATTEMPTS);
        Assert.assertTrue(polls.get() >= 3, "Connection failures must be polled through, not treated as fatal");
    }

    @Test(description = "A throwing re-trigger is reported as an aborted heal, not a dropped event")
    public void throwingHealIsReportedAsHealFailure() throws Exception {
        try {
            run("throwing-retrigger-thing", () -> new HealGate.NotReady("404"),
                    attempt -> { throw new IllegalStateException("no revision in context"); }, MAX_ATTEMPTS);
            Assert.fail("Expected the gate to abort");
        } catch (AssertionError expected) {
            Assert.assertTrue(expected.getMessage().contains("re-trigger failed"),
                    "Must attribute the failure to the re-trigger, got: " + expected.getMessage());
        }
    }

    @Test(description = "Interruption stops the gate rather than firing further mutating re-triggers")
    public void interruptionPropagates() throws Exception {
        List<String> healsFired = new ArrayList<>();
        Thread worker = new Thread(() -> {
            try {
                run("interrupted-thing", () -> {
                    throw new InterruptedException("cancelled");
                }, attempt -> { healsFired.add("fired"); return new HealGate.Ready(); }, MAX_ATTEMPTS);
            } catch (InterruptedException expected) {
                // the contract: propagated, not swallowed
            } catch (Exception other) {
                throw new RuntimeException(other);
            }
        });
        worker.start();
        worker.join(5000);
        Assert.assertFalse(worker.isAlive(), "The gate must stop on interruption");
        Assert.assertTrue(healsFired.isEmpty(), "No mutating re-trigger may fire after cancellation");
    }
}
