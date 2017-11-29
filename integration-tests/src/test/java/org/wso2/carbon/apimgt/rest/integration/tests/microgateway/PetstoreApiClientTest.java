package org.wso2.carbon.apimgt.rest.integration.tests.microgateway;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetstoreApiClientTest {
    private final PetstoreApiClient petstoreApiClient = new PetstoreApiClient();
    Logger logger = LoggerFactory.getLogger(PetstoreApiClientTest.class);

    @Test
    public void getPetbyIdTest() {
        try {
            String response = petstoreApiClient.getPetById();
            Assert.assertEquals(response, "{\"code\":1,\"type\":\"error\",\"message\":\"Pet not found\"}", "Mocked api content");
        } catch (NoSuchAlgorithmException e) {
            logger.info("Failed to initialize SSL handling.", e);
        } catch (KeyManagementException e) {
            logger.info("Failed to initialize SSL handling.", e);
        }
    }

    @Test
    public void getInventoryTest() {
        try {
            String response = petstoreApiClient.getInventory();
//            Assert.assertEquals(response, "{\"sold\":81,\"NA\":1,\"string\":3951,\"nah\":2,\"1112\":1,\"pending\":36,\"foo\":1,\"available\":547,\"not available\":2,\"ex Duis ipsum\":1,\"status\":11}", "Petstore api content");
            Assert.assertNotNull(response);
        } catch (NoSuchAlgorithmException e) {
            logger.info("Failed to initialize SSL handling.", e);
        } catch (KeyManagementException e) {
            logger.info("Failed to initialize SSL handling.", e);
        }
    }
}
