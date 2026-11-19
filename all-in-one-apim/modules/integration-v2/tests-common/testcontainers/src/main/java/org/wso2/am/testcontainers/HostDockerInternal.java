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

/**
 * Where {@code host.docker.internal} points inside the containers the suite starts. Containers use it to reach
 * services on the machine running the tests.
 *
 * <p>The target comes from the {@code docker.extra.hosts} system property, the same {@code docker build} option
 * fragment the image build uses (e.g. {@code --add-host=host.docker.internal:172.17.0.2}, or Docker's {@code =}
 * separator form {@code --add-host=host.docker.internal=172.17.0.2}). Without a mapping for
 * {@code host.docker.internal} the target is Docker's {@code host-gateway}, which is the test machine only when it
 * also runs the Docker daemon; a runner that is itself a container beside the daemon passes its own address.</p>
 */
public final class HostDockerInternal {

    public static final String ALIAS = "host.docker.internal";
    static final String EXTRA_HOSTS_PROPERTY = "docker.extra.hosts";
    static final String DEFAULT_TARGET = "host-gateway";

    private static final String ADD_HOST_OPTION = "--add-host";
    private static final String[] MAPPING_PREFIXES = {ALIAS + ":", ALIAS + "="};

    private HostDockerInternal() {
    }

    /** The {@code host.docker.internal} target configured through the {@code docker.extra.hosts} system property. */
    public static String target() {
        return target(System.getProperty(EXTRA_HOSTS_PROPERTY));
    }

    /**
     * The {@code host.docker.internal} target in a {@code docker.extra.hosts} value. Accepts both
     * {@code --add-host=NAME:ADDRESS} and {@code --add-host NAME:ADDRESS}, with {@code :} or {@code =} between name
     * and address; mappings for other names are ignored. Brackets around an IPv6 address are removed.
     *
     * @throws IllegalArgumentException if the value maps {@code host.docker.internal} to an empty address
     */
    static String target(String extraHosts) {
        if (extraHosts == null || extraHosts.isBlank()) {
            return DEFAULT_TARGET;
        }
        String[] tokens = extraHosts.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String mapping = null;
            if (tokens[i].startsWith(ADD_HOST_OPTION + "=")) {
                mapping = tokens[i].substring(ADD_HOST_OPTION.length() + 1);
            } else if (tokens[i].equals(ADD_HOST_OPTION) && i + 1 < tokens.length) {
                mapping = tokens[++i];
            }
            if (mapping == null) {
                continue;
            }
            for (String prefix : MAPPING_PREFIXES) {
                if (mapping.startsWith(prefix)) {
                    String address = mapping.substring(prefix.length());
                    // Docker accepts a bracketed IPv6 address; containers need it without the brackets.
                    if (address.length() >= 2 && address.startsWith("[") && address.endsWith("]")) {
                        address = address.substring(1, address.length() - 1);
                    }
                    if (address.isEmpty()) {
                        throw new IllegalArgumentException(EXTRA_HOSTS_PROPERTY + " maps " + ALIAS
                                + " to an empty address: '" + extraHosts + "'");
                    }
                    return address;
                }
            }
        }
        return DEFAULT_TARGET;
    }
}
