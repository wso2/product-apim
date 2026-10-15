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

package org.wso2.am.testcontainers;

import com.github.dockerjava.api.command.CreateContainerCmd;

import java.util.Optional;

/**
 * Hard CPU cap for a container, equivalent to {@code docker run --cpus=<n>}: the container may use at most
 * {@code n} CPUs' worth of time per scheduling period (fractions allowed, e.g. {@code 1.5}). Docker carries the
 * value as {@code HostConfig.NanoCpus}, i.e. {@code n × 10^9}.
 */
public final class CpuLimit {

    private static final long NANO_CPUS_PER_CPU = 1_000_000_000L;

    private final double cpus;

    private CpuLimit(double cpus) {
        this.cpus = cpus;
    }

    /**
     * Creates a limit of {@code cpus} CPUs.
     *
     * @throws IllegalArgumentException if {@code cpus} is not a finite number greater than zero, or is too small
     *                                  to be represented in whole nano-CPUs
     */
    public static CpuLimit of(double cpus) {
        if (!Double.isFinite(cpus) || cpus <= 0) {
            throw new IllegalArgumentException("CPU limit must be a finite number greater than 0, got: " + cpus);
        }
        if (Math.round(cpus * NANO_CPUS_PER_CPU) < 1) {
            throw new IllegalArgumentException("CPU limit is below Docker's 1 nano-CPU resolution: " + cpus);
        }
        return new CpuLimit(cpus);
    }

    /**
     * Parses a CPU limit from configuration text. A {@code null} or blank value means "no limit" and yields an
     * empty result; anything else must be a positive decimal number of CPUs.
     *
     * @throws IllegalArgumentException if the value is present but not a valid CPU limit
     */
    public static Optional<CpuLimit> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        double cpus;
        try {
            cpus = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CPU limit must be a number of CPUs (e.g. 1 or 1.5), got: '"
                    + value + "'", e);
        }
        return Optional.of(of(cpus));
    }

    public double cpus() {
        return cpus;
    }

    /** The limit in Docker's {@code HostConfig.NanoCpus} unit. */
    public long nanoCpus() {
        return Math.round(cpus * NANO_CPUS_PER_CPU);
    }

    /** Applies the limit to a container-create command; use as a Testcontainers create-command modifier. */
    public void applyTo(CreateContainerCmd cmd) {
        cmd.getHostConfig().withNanoCPUs(nanoCpus());
    }

    @Override
    public String toString() {
        return cpus + " CPU(s)";
    }
}
