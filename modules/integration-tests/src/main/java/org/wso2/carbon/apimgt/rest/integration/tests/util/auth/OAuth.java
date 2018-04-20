/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.apimgt.rest.integration.tests.util.auth;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TokenInfo;

/**
 * Interceptor class for handle OAuth Bearer Token with given username,password,scopes.
 */

public class OAuth implements RequestInterceptor {
    private String username;
    private String password;
    private String scopes;
    static final int MILLIS_PER_SECOND = 1000;

    private volatile String accessToken;
    private Long expirationTimeMillis;

    public OAuth(String username, String password, String scopes) {
        this.username = username;
        this.password = password;
        this.scopes = scopes;
    }

    @Override
    public void apply(RequestTemplate template) {
        // If the request already have an authorization (eg. Basic auth), do nothing
        if (template.headers().containsKey("Authorization")) {
            return;
        }
        // If first time, get the token
        if (expirationTimeMillis == null || System.currentTimeMillis() >= expirationTimeMillis) {
            updateAccessToken();
        }
        if (getAccessToken() != null) {
            template.header("Authorization", "Bearer " + getAccessToken());
        }
    }

    public synchronized void updateAccessToken() {
        TokenInfo tokenInfo;
        try {
            tokenInfo = TestUtil.generateToken(username, password, scopes);

        } catch (Exception e) {
            throw new RetryableException(e.getMessage(), e, null);
        }
        if (tokenInfo != null && tokenInfo.getToken() != null) {
            setAccessToken(tokenInfo.getToken(), tokenInfo.getExpiryTime());
        }
    }


    public synchronized String getAccessToken() {
        return accessToken;
    }

    public synchronized void setAccessToken(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.expirationTimeMillis = System.currentTimeMillis() + expiresIn * MILLIS_PER_SECOND;
    }
}
