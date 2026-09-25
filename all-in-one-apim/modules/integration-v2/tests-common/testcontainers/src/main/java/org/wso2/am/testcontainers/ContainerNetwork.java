/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import org.testcontainers.containers.Network;

/**
 * Networks with a JVM lifetime. Per-block isolation networks are NOT here — {@code BlockLifecycleListener}
 * creates one {@link Network} per block (APIM plus that block's IS and Solace join it, so the
 * {@code wso2am}/{@code wso2is}/{@code apimforsolace} aliases are network-scoped and never collide across
 * concurrent blocks) and closes it at block teardown.
 */
public class ContainerNetwork {

    /**
     * Home network of the shared {@link NodeAppServer} backend singleton. The backend starts here once and is
     * additionally multi-homed onto each block's private network on demand (see
     * {@link NodeAppServer#attachToNetwork}).
     *
     * <p>Multi-homing is safe for the backend and ONLY for the backend: it is a stateless upstream that resolves
     * no peer names — traffic is always APIM→backend — so being present on several block networks at once
     * introduces no DNS ambiguity. A container that must RESOLVE a name pointing at APIM (the Solace shim's
     * {@code apimforsolace}, IS's {@code wso2am}) cannot be shared this way, because the name would resolve on
     * every attached network; those are per-block containers instead.
     *
     * <p>Lifetime: created on first use and reaped by Ryuk at JVM exit, like the backend container itself. With
     * {@code TESTCONTAINERS_RYUK_DISABLED=true} neither is reaped, and both persist until removed by hand.
     */
    public static final Network BACKEND_HOME_NETWORK = Network.newNetwork();

    private ContainerNetwork() {
    }
}
