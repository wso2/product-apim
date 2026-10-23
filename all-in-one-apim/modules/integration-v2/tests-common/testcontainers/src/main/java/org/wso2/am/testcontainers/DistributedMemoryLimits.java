/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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

/** Memory limits shared by the distributed integration-test containers. */
public final class DistributedMemoryLimits {

    private static final long MEBIBYTE = 1024L * 1024;
    private static final long MAX_SWAP_ALLOWANCE_BYTES = 512L * MEBIBYTE;

    private DistributedMemoryLimits() {
    }

    /**
     * Docker's MemorySwap is the combined memory and swap ceiling. Allow each distributed container up to 25% of its
     * memory cap in swap, bounded at 512 MiB. A parent cgroup limits aggregate distributed-suite swap to 5 GiB.
     *
     * @param memoryLimitBytes container's hard RAM limit, including charged cache and kernel memory
     * @return Docker's combined memory-plus-swap limit
     */
    public static long memoryAndSwapLimitBytes(long memoryLimitBytes) {
        if (memoryLimitBytes <= 0) {
            throw new IllegalArgumentException("Container memory limit must be positive");
        }
        long swapAllowanceBytes = Math.min(memoryLimitBytes / 4, MAX_SWAP_ALLOWANCE_BYTES);
        return Math.addExact(memoryLimitBytes, swapAllowanceBytes);
    }
}
