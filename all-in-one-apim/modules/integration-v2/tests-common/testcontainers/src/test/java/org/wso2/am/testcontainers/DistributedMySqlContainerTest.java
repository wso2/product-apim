package org.wso2.am.testcontainers;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testcontainers.containers.Network;

/** Focused lifecycle/readiness/schema test for the distributed database resource. */
public class DistributedMySqlContainerTest {

    @Test
    public void shouldInitializeDatabasesAndSeedSchema() throws Exception {
        if (!Boolean.getBoolean("distributed.apim.boot.probe")) {
            throw new SkipException("Distributed MySQL container probe is opt-in");
        }
        try (Network network = Network.newNetwork();
             DistributedMySqlContainer mysql = new DistributedMySqlContainer(network)
                     .withSchema(DistributedMySqlContainer.APIM_DATABASE,
                             "CREATE TABLE IF NOT EXISTS V2_PROBE (ID INT PRIMARY KEY);"
                                     + " INSERT IGNORE INTO V2_PROBE (ID) VALUES (1);")) {
            mysql.start();

            Assert.assertTrue(mysql.isInitialized());
            Assert.assertTrue(mysql.getJdbcUrl(DistributedMySqlContainer.APIM_DATABASE)
                    .startsWith("jdbc:mysql://mysql:3306/WSO2AM_DB"));
            Assert.assertEquals(mysql.execInContainer("mysql", "-h127.0.0.1", "-u"
                            + DistributedMySqlContainer.DATABASE_USER, "-p"
                            + DistributedMySqlContainer.DATABASE_PASSWORD,
                    DistributedMySqlContainer.APIM_DATABASE, "-N", "-B", "-e",
                    "SELECT COUNT(*) FROM V2_PROBE;").getStdout().trim(), "1");

            // Insert data outside the schema callback so the restart check can distinguish a fresh database
            // from a container that retained state from the previous instance.
            mysql.execInContainer("mysql", "-h127.0.0.1", "-u"
                            + DistributedMySqlContainer.DATABASE_USER, "-p"
                            + DistributedMySqlContainer.DATABASE_PASSWORD,
                    DistributedMySqlContainer.APIM_DATABASE, "-e",
                    "INSERT INTO V2_PROBE (ID) VALUES (2);");
            mysql.stop();
            mysql.start();
            Assert.assertEquals(mysql.execInContainer("mysql", "-h127.0.0.1", "-u"
                            + DistributedMySqlContainer.DATABASE_USER, "-p"
                            + DistributedMySqlContainer.DATABASE_PASSWORD,
                    DistributedMySqlContainer.APIM_DATABASE, "-N", "-B", "-e",
                    "SELECT COUNT(*) FROM V2_PROBE;").getStdout().trim(), "1");
            Assert.assertEquals(mysql.execInContainer("mysql", "-h127.0.0.1", "-u"
                            + DistributedMySqlContainer.DATABASE_USER, "-p"
                            + DistributedMySqlContainer.DATABASE_PASSWORD,
                    DistributedMySqlContainer.APIM_DATABASE, "-N", "-B", "-e",
                    "SELECT COUNT(*) FROM V2_PROBE WHERE ID = 2;").getStdout().trim(), "0");
        }
    }
}
