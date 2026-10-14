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

/**
 * Tells a deploy's synapse artifact from one already on the gateway, using the gateway log.
 *
 * <p>The gateway artifact endpoint is keyed on name and version only, so after a RE-deploy it answers 200 from the
 * artifact that was already there. Synapse logs {@code <type> : <artifact> was added to the Synapse configuration
 * successfully} each time it hot-swaps an artifact in, so "the log gained this artifact's line since the deploy" is
 * true only once the new artifact is live. A deploy step {@link #mark marks} the log length (keyed by resource id)
 * before deploying; the readiness gate then {@link #artifactAddedSinceMark checks} the appended part.</p>
 *
 * <p>Best-effort by design: no mark, no container, or an unreadable log yields {@link HealGate.Ready}, which is the
 * artifact-existence behaviour the gate has without this check. It can only make the gate stricter.</p>
 */
public final class GatewaySynapseLog {

    private static final Log logger = LogFactory.getLog(GatewaySynapseLog.class);

    /** Context key prefix for the gateway-log offset captured immediately before a resource's deploy. */
    private static final String MARK_PREFIX = "deployGatewayLogMark::";

    /** Synapse's per-artifact hot-swap line. */
    public static final String SYNAPSE_ADDED = "was added to the Synapse configuration successfully";

    private GatewaySynapseLog() {
    }

    /** Records the gateway log's current length for {@code resourceId}, ahead of deploying that resource. */
    public static void mark(String resourceId) {
        try {
            Object candidate = TestContext.get("blockApimContainer");
            if (candidate instanceof ApimRuntime runtime) {
                TestContext.set(MARK_PREFIX + resourceId,
                        runtime.readGatewayLogFile(ContainerLogDiagnostics.SERVER_LOG).length());
            }
        } catch (Throwable unreadable) {
            // Deliberately Throwable: this only sharpens a later gate; it must never break the deploy itself.
            logger.warn("Could not mark the gateway log before deploying " + resourceId + " (the readiness gate"
                    + " will fall back to artifact existence): " + unreadable);
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
        String log;
        try {
            Object candidate = TestContext.get("blockApimContainer");
            if (!(candidate instanceof ApimRuntime runtime)) {
                return new HealGate.Ready();
            }
            log = runtime.readGatewayLogFile(ContainerLogDiagnostics.SERVER_LOG);
        } catch (Throwable unreadable) {
            logger.warn("Could not read the gateway log to confirm the synapse hot-swap of " + artifact
                    + " (accepting the artifact-endpoint 200): " + unreadable);
            return new HealGate.Ready();
        }
        if (log == null) {
            return new HealGate.Ready();
        }
        int from = Integer.parseInt(mark.toString());
        // A rotated/truncated log can leave the mark past the end; scan the whole file rather than throw.
        String appended = from <= log.length() ? log.substring(from) : log;
        for (String line : appended.split("\\R")) {
            if (line.contains(artifact) && line.contains(SYNAPSE_ADDED)) {
                return new HealGate.Ready();
            }
        }
        return new HealGate.NotReady("the gateway holds an artifact, but synapse has not logged adding THIS"
                + " deploy's artifact (" + artifact + ") yet — the 200 is the previously deployed artifact");
    }
}
