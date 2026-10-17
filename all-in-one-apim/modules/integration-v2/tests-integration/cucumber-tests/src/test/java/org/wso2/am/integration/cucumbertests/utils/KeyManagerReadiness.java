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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.testcontainers.ApimRuntime;

/** Live readiness of the Gateway's key-manager event consumer on its control-plane broker. */
public final class KeyManagerReadiness {
    private static final Log LOG = LogFactory.getLog(KeyManagerReadiness.class);

    private KeyManagerReadiness() {
    }

    /** Waits for an active subscription belonging to this Gateway before a scenario emits a key-created event. */
    public static Result await(ApimRuntime runtime, int timeoutSeconds) throws InterruptedException {
        Result result = Utils.retryUntil(timeoutSeconds * 1000L, () -> inspect(runtime), Result::ready);
        if (result != null && result.ready()) {
            LOG.info("Gateway key-manager consumer ready: " + result.diagnostics());
        }
        return result;
    }

    /** CP hosts the distributed broker; the all-in-one servlet endpoint serves its embedded broker. */
    public static Result inspect(ApimRuntime runtime) {
        BrokerSubscriptionReadiness.Result subscription = BrokerSubscriptionReadiness.inspect(
                runtime.getServletHttpsUrl(), "keyManager", runtime.getGatewayBrokerClientAddresses());
        return new Result(subscription.ready(), subscription.diagnostics());
    }

    public record Result(boolean ready, String diagnostics) {
    }
}
