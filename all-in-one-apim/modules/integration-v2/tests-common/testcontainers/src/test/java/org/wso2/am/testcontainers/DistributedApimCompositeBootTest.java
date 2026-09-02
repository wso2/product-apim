package org.wso2.am.testcontainers;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.file.Path;

/** Focused end-to-end lifecycle probe for the CP/TM/Gateway/MySQL composite. */
public class DistributedApimCompositeBootTest {

    @Test
    public void shouldBootAndExposeTheDistributedRuntimeContract() throws Exception {
        Path cp = Path.of(System.getProperty("distributed.apim.cp.zip", ""));
        Path tm = Path.of(System.getProperty("distributed.apim.tm.zip", ""));
        Path gateway = Path.of(System.getProperty("distributed.apim.gateway.zip", ""));
        if (!java.nio.file.Files.isRegularFile(cp) || !java.nio.file.Files.isRegularFile(tm)
                || !java.nio.file.Files.isRegularFile(gateway)) {
            throw new SkipException("Distributed component ZIP properties are not configured");
        }
        Assert.assertTrue(java.nio.file.Files.isRegularFile(cp), "CP ZIP is required");
        Assert.assertTrue(java.nio.file.Files.isRegularFile(tm), "TM ZIP is required");
        Assert.assertTrue(java.nio.file.Files.isRegularFile(gateway), "Gateway ZIP is required");

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
