/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.am.integration.test.utils.bean;

public class DCRParamRequest {
    private String appName;
    private String callBackURL;
    private String tokenScope;
    private String appOwner;
    private String grantType;
    private String dcrEndpoint;
    private String username;
    private String password;
    private String tenantDomain;

    /**
     *
     * @param appName
     * @param callBackURL
     * @param tokenScope
     * @param appOwner
     * @param grantType
     * @param dcrEndpoint
     * @param username
     * @param password
     * @param tenantDomain
     */
    public DCRParamRequest(String appName, String callBackURL, String tokenScope, String appOwner, String grantType,
            String dcrEndpoint, String username, String password, String tenantDomain) {
        this.appName = appName;
        this.callBackURL = callBackURL;
        this.tokenScope = tokenScope;
        this.appOwner = appOwner;
        this.grantType = grantType;
        this.dcrEndpoint = dcrEndpoint;
        this.username = username;
        this.password = password;
        this.tenantDomain = tenantDomain;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getCallBackURL() {
        return callBackURL;
    }

    public void setCallBackURL(String callBackURL) {
        this.callBackURL = callBackURL;
    }

    public String getTokenScope() {
        return tokenScope;
    }

    public void setTokenScope(String tokenScope) {
        this.tokenScope = tokenScope;
    }

    public String getAppOwner() {
        return appOwner;
    }

    public void setAppOwner(String appOwner) {
        this.appOwner = appOwner;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getDcrEndpoint() {
        return dcrEndpoint;
    }

    public void setDcrEndpoint(String dcrEndpoint) {
        this.dcrEndpoint = dcrEndpoint;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }
}
