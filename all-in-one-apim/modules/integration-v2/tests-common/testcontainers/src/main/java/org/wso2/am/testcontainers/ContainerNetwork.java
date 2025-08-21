package org.wso2.am.testcontainers;

import org.testcontainers.containers.Network;

public class ContainerNetwork {
    public static final Network SHARED_NETWORK = Network.newNetwork();
//    public static final Network SHARED_NETWORK = Network.builder()
//            .createNetworkCmdModifier(cmd -> cmd.withName("wso2am-450_default"))
//            .build();
}

//docker network connect 58edfb01-0eea-498f-9f5e-afee3162697d 85bba77abcc3
// docker network connect 58edfb01-0eea-498f-9f5e-afee3162697d a6ecd638718f
//  docker inspect f18a02fc8593 --format='{{json .NetworkSettings.Networks}}' | jq
// docker inspect a6ecd638718f --format='{{json .NetworkSettings.Networks}}' | jq