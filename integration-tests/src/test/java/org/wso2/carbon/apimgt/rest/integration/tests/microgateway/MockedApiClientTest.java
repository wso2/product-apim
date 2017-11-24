package org.wso2.carbon.apimgt.rest.integration.tests.microgateway;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MockedApiClientTest {
    private final MockedApiClient mockedApiClient = new MockedApiClient();
    Logger logger = LoggerFactory.getLogger(MockedApiClientTest.class);
    String validContext = "api";
    String invalidContext = "mockyApi";
    String validApiKey = "122456";
    String invalidApiKey = "111111";

    @Test
    public void validAPIkeyValidContextTest() {
        try {
            String response = mockedApiClient.get(validApiKey, validContext);
            Assert.assertEquals(response, "{ \"abc\": \"This is a test\" }", "Mocked api content");
        } catch (NoSuchAlgorithmException e) {
            logger.info("Failed to initialize SSL handling.", e);
        } catch (KeyManagementException e) {
            logger.info("Failed to initialize SSL handling.", e);
        }
    }

    @Test
    public void invalidAPIkeyValidContextTest() {
        try {
            String response = mockedApiClient.get(invalidApiKey, validContext);
            Assert.assertEquals(response, "{\"code\":900903,\"message\":\"subscription not found\"}",
                    "Error Message for invalid api_key");
        } catch (NoSuchAlgorithmException e) {
            logger.info("Failed to initialize SSL handling.", e);
        } catch (KeyManagementException e) {
            logger.info("Failed to initialize SSL handling.", e);
        }
    }

    @Test
    public void validAPIkeyInvalidContextTest() {
        try {
            String response = mockedApiClient.get(validApiKey, invalidContext);
            Assert.assertEquals(response, "no matching service found for path : /mockyApi",
                    "Error message for invalid context");
        } catch (NoSuchAlgorithmException e) {
            logger.info("Failed to initialize SSL handling.", e);
        } catch (KeyManagementException e) {
            logger.info("Failed to initialize SSL handling.", e);
        }
    }

    @Test
    public void invalidAPIkeyInvalidContextTest() {
        try {
            String response = mockedApiClient.get(invalidApiKey, invalidContext);
            Assert.assertEquals(response, "no matching service found for path : /mockyApi",
                    "Error message for invalid context");
        } catch (NoSuchAlgorithmException e) {
            logger.info("Failed to initialize SSL handling.", e);
        } catch (KeyManagementException e) {
            logger.info("Failed to initialize SSL handling.", e);
        }
    }

}
