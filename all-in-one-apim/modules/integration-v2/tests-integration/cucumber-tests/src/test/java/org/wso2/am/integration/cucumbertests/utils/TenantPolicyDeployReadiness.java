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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Readiness for a newly created tenant's default throttle-policy deployment.
 *
 * <p>Creating a tenant publishes one policy event per default throttle policy. The Traffic Manager handles each by
 * fetching the policy for that tenant and deploying it as a Siddhi execution plan named {@code <domain>_<level>_...},
 * which reads the tenant's state while it runs. These deployments continue for seconds after the tenant already
 * issues tokens. A tenant deactivation that lands among them loses its cache invalidation: the user core clears
 * the tenant cache before committing {@code UM_ACTIVE='0'}, a concurrent tenant read re-caches the active row, and
 * the deactivated tenant keeps issuing tokens. Scenarios that deactivate a freshly synced tenant therefore wait here
 * until its policy deployment has finished.</p>
 *
 * <p>The number of execution plans depends on the product's default policy set and its built-in skip list, so the
 * gate does not expect a count. It is ready once the tenant has at least one deployed plan and no further plan has
 * been deployed for {@link #QUIET_MILLIS}. Consecutive deployments for one tenant are at most 0.4 s apart on a
 * 4-CPU CI runner, so the quiet window is more than ten times the largest observed gap.</p>
 */
public final class TenantPolicyDeployReadiness {

    private static final Log logger = LogFactory.getLog(TenantPolicyDeployReadiness.class);

    private static final String SERVER_LOG = "wso2carbon.log";
    /** How long the tenant's execution-plan count must stay unchanged to count as settled. */
    public static final long QUIET_MILLIS = 5000;
    private static final long POLL_INTERVAL_MILLIS = 500;

    private TenantPolicyDeployReadiness() {
    }

    /**
     * Polls the Traffic Manager log until {@code tenantDomain}'s execution-plan deployments have settled, or
     * {@code timeoutSeconds} elapse. The returned result says which.
     */
    public static Result await(ApimRuntime runtime, String tenantDomain, int timeoutSeconds)
            throws InterruptedException {

        Pattern deployed = Pattern.compile("Execution plan is deployed successfully and in active state\\s*:\\s*"
                + Pattern.quote(tenantDomain) + "_(\\S+)");
        long start = System.currentTimeMillis();
        long[] lastChange = {start};
        int[] lastCount = {-1};
        Result result = Utils.retryUntilWithInterval(timeoutSeconds * 1000L, POLL_INTERVAL_MILLIS, () -> {
            Sample sample = sample(runtime.readTrafficManagerLogFile(SERVER_LOG), deployed);
            long now = System.currentTimeMillis();
            if (sample.count() != lastCount[0]) {
                lastCount[0] = sample.count();
                lastChange[0] = now;
            }
            boolean settled = sample.count() > 0 && now - lastChange[0] >= QUIET_MILLIS;
            return new Result(settled, sample.count(), sample.lastPlan(), now - start);
        }, Result::settled);
        if (result != null && result.settled()) {
            logger.info("Throttle policy deployment for tenant " + tenantDomain + " settled: " + result.plans()
                    + " execution plans, last " + tenantDomain + "_" + result.lastPlan() + ", after "
                    + result.waitedMillis() + " ms");
        }
        return result;
    }

    private static Sample sample(String log, Pattern deployed) {

        int count = 0;
        String lastPlan = null;
        Matcher matcher = deployed.matcher(log == null ? "" : log);
        while (matcher.find()) {
            count++;
            lastPlan = matcher.group(1);
        }
        return new Sample(count, lastPlan);
    }

    private record Sample(int count, String lastPlan) {
    }

    /**
     * One observation: whether the deployment has settled, how many of the tenant's execution plans are deployed,
     * the most recent one, and how long the gate has waited.
     */
    public record Result(boolean settled, int plans, String lastPlan, long waitedMillis) {

        public String diagnostics() {
            return plans + " execution plans deployed" + (lastPlan == null ? "" : ", last " + lastPlan)
                    + ", observed for " + waitedMillis + " ms";
        }
    }
}
