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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class HostDockerInternalTest {

    @DataProvider
    public Object[][] valuesWithoutMapping() {
        return new Object[][]{
                {null},
                {""},
                {"   "},
                {"--add-host=other.host:10.0.0.5"},
                {"--network=host"},
                {"--add-host"}};
    }

    @Test(dataProvider = "valuesWithoutMapping")
    public void shouldDefaultToHostGatewayWithoutMapping(String extraHosts) {
        Assert.assertEquals(HostDockerInternal.target(extraHosts), "host-gateway");
    }

    @DataProvider
    public Object[][] valuesWithMapping() {
        return new Object[][]{
                {"--add-host=host.docker.internal:host-gateway", "host-gateway"},
                {"--add-host=host.docker.internal:172.17.0.2", "172.17.0.2"},
                {"  --add-host   host.docker.internal:172.17.0.2  ", "172.17.0.2"},
                {"--add-host=other.host:10.0.0.5 --add-host=host.docker.internal:10.1.0.4", "10.1.0.4"},
                {"--add-host=host.docker.internal:fd00::2", "fd00::2"},
                {"--add-host=host.docker.internal=172.17.0.2", "172.17.0.2"},
                {"--add-host host.docker.internal=fd00::2", "fd00::2"},
                {"--add-host=other.host=10.0.0.5 --add-host=host.docker.internal=10.1.0.4", "10.1.0.4"},
                {"--add-host=host.docker.internal=[fd00::2]", "fd00::2"},
                {"--add-host host.docker.internal:[fd00::2]", "fd00::2"}};
    }

    @Test(dataProvider = "valuesWithMapping")
    public void shouldUseMappedAddress(String extraHosts, String expectedTarget) {
        Assert.assertEquals(HostDockerInternal.target(extraHosts), expectedTarget);
    }

    @DataProvider
    public Object[][] valuesWithEmptyAddress() {
        return new Object[][]{
                {"--add-host=host.docker.internal:"},
                {"--add-host=host.docker.internal="},
                {"--add-host=host.docker.internal=[]"}};
    }

    @Test(dataProvider = "valuesWithEmptyAddress", expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectEmptyMappedAddress(String extraHosts) {
        HostDockerInternal.target(extraHosts);
    }
}
