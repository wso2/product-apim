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
