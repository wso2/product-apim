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

package org.wso2.am.testcontainers;

/** Compares adjacent real-time and monotonic samples; elapsed time alone cannot reveal a backward adjustment. */
public final class ClockSampleDetector {
    private static final long MAX_DISCONTINUITY_MILLIS = 50;
    private static final long MAX_SAMPLE_GAP_MILLIS = 10_000;
    private long samples;
    private long lastWallMillis;
    private long lastMonotonicNanos;
    private long lastMonotonicAfterNanos;
    private long largestDiscontinuityMillis;
    private String failure;

    public synchronized void accept(long wallMillis, long monotonicNanos) {
        accept(wallMillis, monotonicNanos, monotonicNanos);
    }

    public synchronized void accept(long wallMillis, long monotonicNanos, long monotonicAfterNanos) {
        if (monotonicAfterNanos < monotonicNanos
                || monotonicAfterNanos - monotonicNanos > 1_000_000_000L) {
            fail("Docker host clock sample has an invalid or imprecise monotonic bracket");
        }
        if (samples > 0) {
            long monotonicDelta = monotonicNanos - lastMonotonicNanos;
            long elapsedMillis = monotonicDelta / 1_000_000;
            long wallDelta = wallMillis - lastWallMillis;
            long minimumElapsed = (monotonicNanos - lastMonotonicAfterNanos) / 1_000_000;
            long maximumElapsed = (monotonicAfterNanos - lastMonotonicNanos) / 1_000_000;
            long discontinuity = wallDelta < minimumElapsed ? wallDelta - minimumElapsed
                    : wallDelta > maximumElapsed ? wallDelta - maximumElapsed : 0;
            largestDiscontinuityMillis = Math.max(largestDiscontinuityMillis, Math.abs(discontinuity));
            if (monotonicDelta <= 0 || elapsedMillis > MAX_SAMPLE_GAP_MILLIS) {
                fail("Docker host clock sampling interrupted: monotonic interval=" + elapsedMillis + "ms");
            } else if (wallDelta < 0 || Math.abs(discontinuity) > MAX_DISCONTINUITY_MILLIS) {
                fail("Docker host clock discontinuity: wall interval=" + wallDelta + "ms, monotonic interval="
                        + elapsedMillis + "ms, adjustment=" + discontinuity + "ms");
            }
        }
        samples++;
        lastWallMillis = wallMillis;
        lastMonotonicNanos = monotonicNanos;
        lastMonotonicAfterNanos = monotonicAfterNanos;
    }

    private void fail(String message) {
        if (failure == null) {
            failure = message;
        }
    }

    public synchronized String failure() {
        return failure;
    }

    public synchronized long samples() {
        return samples;
    }

    public synchronized long largestDiscontinuityMillis() {
        return largestDiscontinuityMillis;
    }
}
