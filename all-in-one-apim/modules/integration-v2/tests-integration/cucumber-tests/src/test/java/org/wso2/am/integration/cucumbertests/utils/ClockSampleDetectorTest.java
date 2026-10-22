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

package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.am.testcontainers.ClockSampleDetector;

public class ClockSampleDetectorTest {
    @Test
    public void normalClockAndSmallSlewAreAccepted() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(1200, 1_200_000_000);
        detector.accept(1401, 1_400_000_000);
        Assert.assertNull(detector.failure());
        Assert.assertEquals(detector.samples(), 3);
    }

    @Test
    public void schedulerDelayInsideAClockReadIsAccountedFor() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000, 1_001_000_000);
        detector.accept(1290, 1_200_000_000, 1_290_000_000);
        detector.accept(1400, 1_400_000_000, 1_401_000_000);
        Assert.assertNull(detector.failure());
    }

    @Test
    public void excessivelyImpreciseReadFails() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000, 3_000_000_000L);
        Assert.assertTrue(detector.failure().contains("imprecise"));
    }

    @Test
    public void detectsRollbackEvenWhenWallClockStillAdvancedBetweenSamples() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(1100, 1_200_000_000);
        Assert.assertTrue(detector.failure().contains("adjustment=-100ms"));
    }

    @Test
    public void detectsWallTimeGoingBackwards() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(900, 1_200_000_000);
        Assert.assertNotNull(detector.failure());
    }

    @Test
    public void detectsForwardClockStep() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(6200, 1_200_000_000);
        Assert.assertTrue(detector.failure().contains("adjustment=5000ms"));
    }

    @Test
    public void interruptedSamplingFails() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(12000, 12_000_000_000L);
        Assert.assertTrue(detector.failure().contains("sampling interrupted"));
    }

    @Test
    public void nonIncreasingMonotonicClockFails() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(1200, 1_000_000_000);
        Assert.assertNotNull(detector.failure());
    }

    @Test
    public void aLaterHealthySampleCannotEraseAFailure() {
        ClockSampleDetector detector = new ClockSampleDetector();
        detector.accept(1000, 1_000_000_000);
        detector.accept(900, 1_200_000_000);
        String original = detector.failure();
        detector.accept(1100, 1_400_000_000);
        Assert.assertEquals(detector.failure(), original);
    }
}
