
package org.wso2.carbon.apimgt.rest.integration.tests.exceptions;

public class AMIntegrationTestException extends Exception {
    public AMIntegrationTestException() {
    }

    public AMIntegrationTestException(String message) {
        super(message);
    }

    public AMIntegrationTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public AMIntegrationTestException(Throwable cause) {
        super(cause);
    }

    public AMIntegrationTestException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
