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

package org.wso2.am.testcontainers;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.file.Path;

/** Focused end-to-end lifecycle probe for the CP/TM/Gateway/MySQL composite. */
public class DistributedApimCompositeBootTest {

    @Test
    public void shouldBootAndExposeTheDistributedRuntimeContract() throws Exception {
        if (!Boolean.getBoolean("distributed.apim.boot.probe")) {
            throw new SkipException("Distributed composite boot probe is opt-in");
        }
        Path cp = DistributedDynamicApimContainer.defaultPath("cp/deployment.toml");
        Path tm = DistributedDynamicApimContainer.defaultPath("tm/deployment.toml");
        Path gateway = DistributedDynamicApimContainer.defaultPath("gateway/deployment.toml");
        Assert.assertTrue(java.nio.file.Files.isRegularFile(cp), "CP image defaults are required");
        Assert.assertTrue(java.nio.file.Files.isRegularFile(tm), "TM image defaults are required");
        Assert.assertTrue(java.nio.file.Files.isRegularFile(gateway), "Gateway image defaults are required");

        DistributedDynamicApimContainer runtime = new DistributedDynamicApimContainer("composite", cp, tm, gateway);
        try {
            runtime.start();
            Assert.assertTrue(runtime.isStarted());
            Assert.assertTrue(runtime.getServletHttpsUrl().startsWith("https://"));
            Assert.assertTrue(runtime.getGatewayHttpsUrl().startsWith("https://"));
            Assert.assertFalse(runtime.getContainerId().isBlank());
            Assert.assertFalse(runtime.getTrafficManagerContainerId().isBlank());
            Assert.assertFalse(runtime.getGatewayContainerId().isBlank());
            Assert.assertTrue(runtime.getDatabaseHostJdbcUrl(DistributedMySqlContainer.APIM_DATABASE)
                    .contains("WSO2AM_DB"));
        } finally {
            runtime.stop();
        }
    }
}
