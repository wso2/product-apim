package org.wso2.am.integration.test.utils;

/**
 * Custom exception to used in API Manager Test scenarios.
 */
public class APIManagerIntegrationTestException extends  Exception {


    /**
     * Constructor for the custom Exception  class APIManagerIntegrationTestException.
     * @param message Custom message.
     * @param throwable Original Exception.
     */
    public APIManagerIntegrationTestException(String message, Throwable throwable) {
        super(message, throwable);
    }


}
