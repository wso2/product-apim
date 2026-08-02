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
 * Static networks with a JVM lifetime. Per-block isolation networks are NOT here — {@code BlockLifecycleListener}
 * creates one {@link Network} per block (APIM + its IS join it under the {@code wso2am}/{@code wso2is} aliases,
 * scoped per network so blocks never collide) and closes it at block teardown.
 */
public class ContainerNetwork {

    /**
     * Home network of the shared {@link NodeAppServer} backend singleton. The backend starts here once and is
     * additionally multi-homed onto each block's private network on demand (see
     * {@link NodeAppServer#attachToNetwork}) — it is a stateless upstream, so one instance reachable from every
     * block network is safe. Reaped by Ryuk at JVM exit (like the backend itself).
     */
    public static final Network BACKEND_HOME_NETWORK = Network.newNetwork();
}
