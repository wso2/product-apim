package org.wso2.am.integration.tests.scenariotest;

public enum RequestMethod {

    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    HEAD("HEAD");


    private final String methodName;
    RequestMethod(String name) {
        this.methodName = name;
    }

    @Override
    public String toString() {
        return this.methodName;
    }
}

