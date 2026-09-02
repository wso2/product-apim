package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipFile;

/**
 * Focused CP/database integration probe. It is deliberately separate from the
 * framework suites: this proves the first distributed component can boot
 * against the block-owned database before TM and Gateway are introduced.
 */
public class DistributedCpMySqlBootTest {

    private static final String CP_IMAGE = "distributed-apim-cp:4.7.0-SNAPSHOT";
    private static final String CP_ZIP_PROPERTY = "distributed.apim.cp.zip";

    @Test
    public void shouldBootControlPlaneAgainstProductMySqlSchemas() throws Exception {
        Path cpZip = Path.of(System.getProperty(CP_ZIP_PROPERTY, ""));
        if (!Files.isRegularFile(cpZip)) {
            throw new SkipException("Distributed CP ZIP property is not configured");
        }
        Assert.assertTrue(Files.isRegularFile(cpZip),
                "Set -D" + CP_ZIP_PROPERTY + " to the built CP ZIP");

        String sharedSchema = readZipEntry(cpZip, "dbscripts/mysql.sql");
        String apimSchema = readZipEntry(cpZip, "dbscripts/apimgt/mysql.sql");
        String deploymentToml = mysqlDeployment(readZipEntry(cpZip,
                "repository/conf/deployment.toml"));

        try (Network network = Network.newNetwork();
             DistributedMySqlContainer mysql = new DistributedMySqlContainer(network)
                     .withSchema(DistributedMySqlContainer.SHARED_DATABASE, sharedSchema)
                     .withSchema(DistributedMySqlContainer.APIM_DATABASE, apimSchema);
             GenericContainer<?> cp = new GenericContainer<>(CP_IMAGE)
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

    private static String readZipEntry(Path zip, String suffix) throws IOException {
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            return archive.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(suffix))
                    .findFirst()
                    .map(entry -> {
                        try {
                            return new String(archive.getInputStream(entry).readAllBytes(),
                                    StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new IllegalStateException("Unable to read " + suffix, e);
                        }
                    })
                    .orElseThrow(() -> new IllegalStateException("ZIP entry not found: " + suffix));
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
