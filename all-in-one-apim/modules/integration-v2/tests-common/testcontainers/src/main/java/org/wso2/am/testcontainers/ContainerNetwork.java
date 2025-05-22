package org.wso2.am.testcontainers;

import org.testcontainers.containers.Network;

public class ContainerNetwork {
    public static final Network SHARED_NETWORK = Network.newNetwork();
}
