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
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.ContainerLogHistory;

/**
 * Tells a deploy's synapse artifact from one already on the gateway, using the gateway log.
 *
 * <p>The gateway artifact endpoint is keyed on name and version only, so after a RE-deploy it answers 200 from the
 * artifact that was already there. Synapse logs {@code <type> : <artifact> was added to the Synapse configuration
 * successfully} each time it hot-swaps an artifact in, so "the log gained this artifact's line since the deploy" is
 * true only once the new artifact is live. A deploy step {@link #mark marks} the log end (keyed by resource id)
 * before deploying; the readiness gate then {@link #artifactAddedSinceMark checks} the appended part.</p>
 *
 * <p>Without a block container or a deploy mark, the gate uses artifact existence. A marked deploy requires
 * readable history after that mark; a lost mark, or a mark from another container or log file, cannot confirm the
 * deploy. After a restart in the same container and log file, the whole later boot follows the mark. Container
 * transport failures keep polling within the existing deadline.</p>
 */
public final class GatewaySynapseLog {

    private static final Log logger = LogFactory.getLog(GatewaySynapseLog.class);

    /** Context key prefix for the gateway-log position captured immediately before a resource's deploy. */
    private static final String MARK_PREFIX = "deployGatewayLogMark::";

    /** Synapse's per-artifact hot-swap line. */
    public static final String SYNAPSE_ADDED = "was added to the Synapse configuration successfully";

    private GatewaySynapseLog() {
    }

    /** Records the gateway log's current end for {@code resourceId}, ahead of deploying that resource. */
    public static void mark(String resourceId) {
        mark(resourceId, Constants.RUNTIME_PROPAGATION_TIMEOUT, Constants.RETRY_INTERVAL_TIME);
    }

    static void mark(String resourceId, long timeoutMillis, long intervalMillis) {
        Object candidate = TestContext.get("blockApimContainer");
        if (!(candidate instanceof ApimRuntime runtime)) {
            return;
        }
        long start = System.currentTimeMillis();
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (true) {
            try {
                TestContext.set(MARK_PREFIX + resourceId,
                        runtime.readGatewayLogHistory(ContainerLogDiagnostics.SERVER_LOG).mark());
                return;
            } catch (ContainerLogHistory.ReadUnavailableException unavailable) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadline) {
                    throw new IllegalStateException("Could not mark gateway log before deploying " + resourceId,
                            unavailable);
                }
                try {
                    Utils.pollPause(start, intervalMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted marking gateway log before deploying " + resourceId,
                            interrupted);
                }
            }
        }
    }

    /**
     * {@link HealGate.Ready} once the gateway log has gained {@code artifact}'s synapse hot-swap line since the mark
     * taken for {@code resourceId}; {@link HealGate.NotReady} while it has not. Matching is on the full synapse
     * artifact name, which is scenario-unique because resource names are unique by construction: block classes share
     * one container and one log, so anything less specific could match a sibling scenario's deploy.
     */
    public static HealGate.Verdict artifactAddedSinceMark(String resourceId, String artifact) {
        Object mark = TestContext.get(MARK_PREFIX + resourceId);
        if (mark == null) {
            return new HealGate.Ready();
        }
        ContainerLogHistory.Snapshot log;
        try {
            Object candidate = TestContext.get("blockApimContainer");
            if (!(candidate instanceof ApimRuntime runtime)) {
                return new HealGate.Fatal("Marked deploy has no block runtime for its gateway log");
            }
            log = runtime.readGatewayLogHistory(ContainerLogDiagnostics.SERVER_LOG);
        } catch (ContainerLogHistory.ReadUnavailableException unreadable) {
            logger.warn("Could not read the gateway log to confirm the synapse hot-swap of " + artifact, unreadable);
            return new HealGate.NotReady("Could not read marked gateway log: " + unreadable);
        } catch (RuntimeException invalid) {
            return new HealGate.Fatal("Invalid marked gateway log: " + invalid);
        }
        if (log == null) {
            return new HealGate.NotReady("Marked gateway log is unavailable");
        }
        if (!(mark instanceof ContainerLogHistory.Mark)) {
            return new HealGate.Fatal("Invalid gateway log mark for " + resourceId);
        }
        String appended;
        try {
            appended = log.since((ContainerLogHistory.Mark) mark);
        } catch (IllegalStateException invalid) {
            return new HealGate.Fatal("Invalid gateway log mark for " + resourceId + ": " + invalid.getMessage());
        }
        for (String line : appended.split("\\R")) {
            if (line.contains(artifact) && line.contains(SYNAPSE_ADDED)) {
                return new HealGate.Ready();
            }
        }
        return new HealGate.NotReady("the gateway holds an artifact, but synapse has not logged adding THIS"
                + " deploy's artifact (" + artifact + ") yet — the 200 is the previously deployed artifact");
    }
}
