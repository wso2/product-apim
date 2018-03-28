package org.wso2.carbon.apimgt.rest.integration.tests.util;

public class TokenInfo {
    private String token;
    private long expiryTime;
    private long expiryEpochTime;

    public TokenInfo(String token, long expiryTime) {
        this.token = token;
        this.expiryTime = expiryTime;
        this.expiryEpochTime = expiryTime * 1000 + System.currentTimeMillis();
    }

    public String getToken() {
        return token;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public long getExpiryEpochTime() {
        return expiryEpochTime;
    }
}
