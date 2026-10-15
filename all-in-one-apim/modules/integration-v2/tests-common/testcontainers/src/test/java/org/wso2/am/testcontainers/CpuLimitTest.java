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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;

public class CpuLimitTest {

    @DataProvider
    public Object[][] absentValues() {
        return new Object[][]{{null}, {""}, {"   "}};
    }

    @Test(dataProvider = "absentValues")
    public void shouldLeaveContainerUncappedWhenValueIsAbsent(String value) {
        Assert.assertTrue(CpuLimit.parse(value).isEmpty());
    }

    @DataProvider
    public Object[][] validValues() {
        return new Object[][]{
                {"1", 1_000_000_000L},
                {" 1.5 ", 1_500_000_000L},
                {"0.25", 250_000_000L},
                {"4", 4_000_000_000L}};
    }

    @Test(dataProvider = "validValues")
    public void shouldParseCpusIntoNanoCpus(String value, long expectedNanoCpus) {
        CpuLimit limit = CpuLimit.parse(value).orElseThrow();
        Assert.assertEquals(limit.nanoCpus(), expectedNanoCpus);
    }

    @DataProvider
    public Object[][] invalidValues() {
        return new Object[][]{{"abc"}, {"1 CPU"}, {"0"}, {"-1"}, {"NaN"}, {"Infinity"}, {"0.0000000001"}};
    }

    @Test(dataProvider = "invalidValues")
    public void shouldRejectInvalidValue(String value) {
        Assert.assertThrows(IllegalArgumentException.class, () -> CpuLimit.parse(value));
    }

    @Test
    public void shouldSetNanoCpusOnContainerCreateCommand() {
        CreateContainerCmd cmd = createCommandWith(new HostConfig());

        CpuLimit.of(1.3).applyTo(cmd);

        Assert.assertEquals(cmd.getHostConfig().getNanoCPUs(), Long.valueOf(1_300_000_000L));
    }

    @Test
    public void shouldLeaveOtherHostConfigUntouched() {
        CreateContainerCmd cmd = createCommandWith(new HostConfig().withMemory(512L * 1024 * 1024));

        CpuLimit.of(2).applyTo(cmd);

        Assert.assertEquals(cmd.getHostConfig().getMemory(), Long.valueOf(512L * 1024 * 1024));
        Assert.assertEquals(cmd.getHostConfig().getNanoCPUs(), Long.valueOf(2_000_000_000L));
    }

    /** A create command exposing only {@code getHostConfig()}, which is all a create-command modifier touches. */
    private static CreateContainerCmd createCommandWith(HostConfig hostConfig) {
        return (CreateContainerCmd) Proxy.newProxyInstance(CreateContainerCmd.class.getClassLoader(),
                new Class<?>[]{CreateContainerCmd.class}, (proxy, method, args) -> {
                    if ("getHostConfig".equals(method.getName())) {
                        return hostConfig;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
