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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.GracefulServerRestart;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;

/**
 * Steps that act on the APIM server process itself (as opposed to its REST resources). Used by features that
 * must survive a server restart — e.g. token persistence. The block hosting these MUST run sequentially
 * ({@code thread-count=1}) since a restart bounces the shared container's server out from under any concurrent
 * class, and its container overlay must enable {@code [server] enable_restart_from_api}.
 */
public class ServerLifecycleSteps {

    /**
     * Gracefully restarts the APIM server in place via the Carbon {@code ServerAdmin} admin service
     * ({@code restartGracefully}). The container is NOT touched — only the carbon JVM bounces — so host ports
     * and the (in-container) database survive, which is what makes token-persistence-across-restart testable.
     * Blocks until the server has gone down and come back ready (see {@link ServerReadiness#awaitRestart});
     * fails if it does not return within {@code SERVER_STARTUP_WAIT_TIME}.
     */
    @When("I gracefully restart the API Manager server")
    public void iGracefullyRestartTheApiManagerServer() throws Exception {

        // Restart the component that serves Gateway traffic. In all-in-one this resolves to the same unified
        // APIM URL; in distributed topology it resolves to the Gateway management listener rather than CP.
        GracefulServerRestart.gateway();
    }

    /** Gracefully restarts the distributed Control Plane, or the unified APIM server in all-in-one topology. */
    @When("I gracefully restart the Control Plane")
    public void iGracefullyRestartTheControlPlane() throws Exception {
        GracefulServerRestart.controlPlane();
    }
}
