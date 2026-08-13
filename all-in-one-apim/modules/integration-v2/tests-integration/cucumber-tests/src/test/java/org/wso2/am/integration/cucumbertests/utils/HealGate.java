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
import org.wso2.am.integration.test.utils.Constants;

import java.io.IOException;

/**
 * Waits for a propagated state and, if the wait expires, lets the caller reconcile and re-fire the action whose
 * event was dropped. APIM's propagation events are at-most-once, so a lost one can only be cured by re-firing.
 *
 * <p>The probe classifies rather than returning a boolean: {@link NotReady} keeps polling, {@link Fatal} fails
 * immediately. Transport transients are not handled here — {@code SimpleHTTPClient} already retries the
 * management API's transient {@code 900967} 5xx, and a refused connection arrives as {@link IOException}, which
 * this maps to {@code NotReady}.
 */
public final class HealGate {

    private static final Log log = LogFactory.getLog(HealGate.class);

    /** Window length for attempts after the first. */
    private static final long RETRY_WINDOW_MILLIS = 60_000L;
    /** A passing first window slower than this logs the slow-pass watch line. */
    private static final long SLOW_PASS_MILLIS = 60_000L;

    private HealGate() {
    }

    /** What a {@link Probe} or {@link Heal} observed. */
    public sealed interface Verdict permits Ready, NotReady, Fatal {
    }

    /** The awaited state is observable. */
    public record Ready() implements Verdict {
    }

    /** Not yet; {@code observed} is reported if the gate never converges. */
    public record NotReady(String observed) implements Verdict {
    }

    /** Will never become ready; {@code why} is the failure message. */
    public record Fatal(String why) implements Verdict {
    }

    /** Reads the awaited state. */
    @FunctionalInterface
    public interface Probe {
        Verdict check() throws Exception;
    }

    /** Reconciles stale state and re-fires the action; {@link Fatal} aborts the gate. */
    @FunctionalInterface
    public interface Heal {
        Verdict reconcileAndRetrigger(int attempt) throws Exception;
    }

    /** Waits with the shared propagation timeout for the first window and a minute for each retry. */
    public static void awaitOrHeal(String what, Probe probe, Heal heal, int maxAttempts)
            throws InterruptedException {
        awaitOrHeal(what, probe, heal, maxAttempts, Constants.RUNTIME_PROPAGATION_TIMEOUT, RETRY_WINDOW_MILLIS,
                Constants.RETRY_INTERVAL_TIME);
    }

    /** Timing-injectable form, so {@code HealGateVerificationTest} can drive the heal and fatal paths. */
    static void awaitOrHeal(String what, Probe probe, Heal heal, int maxAttempts, long firstWindowMillis,
                            long retryWindowMillis, long pollIntervalMillis) throws InterruptedException {

        String lastObserved = "nothing observed";
        int healsFired = 0;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long windowStart = System.currentTimeMillis();
            long deadline = windowStart + (attempt == 1 ? firstWindowMillis : retryWindowMillis);
            while (true) {
                Verdict verdict;
                try {
                    verdict = probe.check();
                } catch (InterruptedException interrupted) {
                    // Propagated, not swallowed: otherwise the gate keeps firing mutating re-triggers after
                    // cancellation (the throw already cleared the interrupt flag).
                    Thread.currentThread().interrupt();
                    throw interrupted;
                } catch (IOException notListeningYet) {
                    verdict = new NotReady("connection failed: " + notListeningYet.getMessage());
                } catch (Exception probeFailed) {
                    verdict = new NotReady("probe threw: " + probeFailed);
                }

                if (verdict instanceof Ready) {
                    long waited = (System.currentTimeMillis() - windowStart) / 1000;
                    if (healsFired > 0) {
                        log.warn("self-heal: " + what + " became ready after re-trigger (attempt " + attempt + "/"
                                + maxAttempts + ", " + waited + "s into the window)");
                    } else if (waited * 1000 >= SLOW_PASS_MILLIS) {
                        log.warn("self-heal-watch: " + what + " became ready only after " + waited
                                + "s (no re-trigger needed — delayed, not dropped)");
                    }
                    return;
                }
                if (verdict instanceof Fatal fatal) {
                    Assert.fail(what + " cannot become ready: " + fatal.why()
                            + " (failed fast rather than waiting out the propagation window; " + healsFired
                            + " heal(s) fired)");
                }
                lastObserved = ((NotReady) verdict).observed();
                if (System.currentTimeMillis() >= deadline) {
                    break;
                }
                Utils.pollPause(windowStart, pollIntervalMillis);
            }

            if (attempt < maxAttempts) {
                log.warn("self-heal: re-triggering " + what + " (attempt " + (attempt + 1) + "/" + maxAttempts
                        + ") — runtime-propagation event presumed dropped after an exhausted wait window; last "
                        + "observed: " + lastObserved);
                Verdict healVerdict;
                try {
                    healVerdict = heal.reconcileAndRetrigger(attempt + 1);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw interrupted;
                } catch (Exception healFailed) {
                    healVerdict = new Fatal("self-heal re-trigger failed: " + healFailed);
                }
                if (healVerdict instanceof Fatal fatal) {
                    Assert.fail("self-heal of " + what + " aborted: " + fatal.why());
                }
                healsFired++;
            }
        }
        Assert.fail(what + " did not become ready within " + maxAttempts + " attempt(s) including " + healsFired
                + " self-heal re-trigger(s); last observed: " + lastObserved);
    }
}
