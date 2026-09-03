package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Focused CP/database integration probe. It is deliberately separate from the
 * framework suites: this proves the first distributed component can boot
 * against the block-owned database before TM and Gateway are introduced.
 */
public class DistributedCpMySqlBootTest {

    private static final String CP_IMAGE_PROPERTY = "distributed.apim.cp.image.name";

    @Test
    public void shouldBootControlPlaneAgainstProductMySqlSchemas() throws Exception {
        if (!Boolean.getBoolean("distributed.apim.boot.probe")) {
            throw new SkipException("Distributed CP/MySQL boot probe is opt-in");
        }
        Path sharedSchemaPath = DistributedDynamicApimContainer.defaultPath("cp/mysql.sql");
        Path apimSchemaPath = DistributedDynamicApimContainer.defaultPath("cp/apimgt-mysql.sql");
        Path deploymentTomlPath = DistributedDynamicApimContainer.defaultPath("cp/deployment.toml");
        Assert.assertTrue(Files.isRegularFile(sharedSchemaPath), "CP image shared schema is required");
        Assert.assertTrue(Files.isRegularFile(apimSchemaPath), "CP image API Manager schema is required");
        Assert.assertTrue(Files.isRegularFile(deploymentTomlPath), "CP image deployment.toml is required");

        String sharedSchema = Files.readString(sharedSchemaPath);
        String apimSchema = Files.readString(apimSchemaPath);
        String deploymentToml = mysqlDeployment(Files.readString(deploymentTomlPath));

        String cpImage = System.getProperty(CP_IMAGE_PROPERTY);
        Assert.assertFalse(cpImage == null || cpImage.isBlank(),
                "Set -D" + CP_IMAGE_PROPERTY + " from the CP product POM version");

        try (Network network = Network.newNetwork();
             DistributedMySqlContainer mysql = new DistributedMySqlContainer(network)
                     .withSchema(DistributedMySqlContainer.SHARED_DATABASE, sharedSchema)
                     .withSchema(DistributedMySqlContainer.APIM_DATABASE, apimSchema);
             GenericContainer<?> cp = new GenericContainer<>(cpImage)
                     .withNetwork(network)
                     .withNetworkAliases("apim-cp")
                     .withExposedPorts(9443)
                     .withCopyToContainer(Transferable.of(deploymentToml, 0666),
                             "/opt/wso2/repository/conf/deployment.toml")
                     .withCommand("-DportOffset=0")
                     .waitingFor(Wait.forLogMessage(".*WSO2 Carbon started in.*", 1)
                             .withStartupTimeout(Duration.ofMinutes(3)))
                     .withStartupAttempts(1)) {
            mysql.start();
            try {
                cp.start();
            } catch (RuntimeException e) {
                System.err.println("Distributed CP startup log:\n" + cp.getLogs());
                throw e;
            }

            Assert.assertTrue(cp.getLogs().contains("WSO2 Carbon started in"),
                    "CP did not report a completed startup");

            String tableCount = mysql.execInContainer("mysql", "-h127.0.0.1", "-u"
                            + DistributedMySqlContainer.DATABASE_USER, "-p"
                            + DistributedMySqlContainer.DATABASE_PASSWORD,
                    DistributedMySqlContainer.APIM_DATABASE, "-N", "-B", "-e",
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"
                            + DistributedMySqlContainer.APIM_DATABASE + "';")
                    .getStdout().trim();
            Assert.assertTrue(Integer.parseInt(tableCount) > 0,
                    "The APIM schema contains no tables");
        }
    }

    private static String mysqlDeployment(String defaults) {
        return defaults
                .replace("type = \"h2\"\nurl = \"jdbc:h2:./repository/database/WSO2AM_DB;DB_CLOSE_ON_EXIT=FALSE\"",
                        "type = \"mysql\"\nurl = \"jdbc:mysql://mysql:3306/WSO2AM_DB?autoReconnect=true&amp;allowPublicKeyRetrieval=true&amp;useSSL=false\"\ndriver = \"com.mysql.cj.jdbc.Driver\"")
                .replace("type = \"h2\"\nurl = \"jdbc:h2:./repository/database/WSO2SHARED_DB;DB_CLOSE_ON_EXIT=FALSE\"",
                        "type = \"mysql\"\nurl = \"jdbc:mysql://mysql:3306/WSO2AM_SHARED_DB?autoReconnect=true&amp;allowPublicKeyRetrieval=true&amp;useSSL=false\"\ndriver = \"com.mysql.cj.jdbc.Driver\"");
    }
}
