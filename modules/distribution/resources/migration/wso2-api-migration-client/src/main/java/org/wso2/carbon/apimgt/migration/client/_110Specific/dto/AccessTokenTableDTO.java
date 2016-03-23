/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * /
 */

package org.wso2.carbon.apimgt.migration.client._110Specific.dto;

import java.sql.Timestamp;

public class AccessTokenTableDTO {
    private String accessToken;
    private String refreshToken;
    private ConsumerKeyDTO consumerKey;
    private String authzUser;
    private String userType;
    private Timestamp timeCreated;
    private long validityPeriod;
    private String tokenScope;
    private String tokenState;
    private String tokenStateID;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public ConsumerKeyDTO getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(ConsumerKeyDTO consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getAuthzUser() {
        return authzUser;
    }

    public void setAuthzUser(String authzUser) {
        this.authzUser = authzUser;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Timestamp getTimeCreated() {
        return new Timestamp(timeCreated.getTime());
    }

    public void setTimeCreated(Timestamp timeCreated) {
        this.timeCreated = new Timestamp(timeCreated.getTime());
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public String getTokenScope() {
        return tokenScope;
    }

    public void setTokenScope(String tokenScope) {
        this.tokenScope = tokenScope;
    }

    public String getTokenState() {
        return tokenState;
    }

    public void setTokenState(String tokenState) {
        this.tokenState = tokenState;
    }

    public String getTokenStateID() {
        return tokenStateID;
    }

    public void setTokenStateID(String tokenStateID) {
        this.tokenStateID = tokenStateID;
    }
}
