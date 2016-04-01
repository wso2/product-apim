/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.test.utils.bean;

/**
 * Generate application key request
 *
 * action=generateApplicationKey&application=DefaultApplication&keytype=PRODUCTION&callbackUrl=&authorizedDomains=ALL&validityTime=360000
 */
public class APPKeyRequestGenerator extends AbstractRequest {

    private String application = "DefaultApplication";
    private String keyType = "PRODUCTION";
    private String callbackUrl = "some-url";
    private String authorizedDomains = "ALL";
    private int validityTime = 360000;
    private String appId = "1";
    private String tokenScope;

    public APPKeyRequestGenerator(String application) {
        this.application = application;
    }

    @Override
    public void setAction() {
        setAction("generateApplicationKey");
    }

    @Override
    public void init() {
        addParameter("application", application);
        addParameter("keytype", keyType);
        addParameter("callbackUrl", callbackUrl);
        addParameter("authorizedDomains", authorizedDomains);
        addParameter("validityTime", String.valueOf(validityTime));
        addParameter("selectedAppID", appId);
        if (tokenScope != null) {
            addParameter("tokenScope", tokenScope);
        }
    }

    public String getApplication() {
        return application;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getAuthorizedDomains() {
        return authorizedDomains;
    }

    public void setAuthorizedDomains(String authorizedDomains) {
        this.authorizedDomains = authorizedDomains;
    }

    public int getValidityTime() {
        return validityTime;
    }

    public void setValidityTime(int validityTime) {
        this.validityTime = validityTime;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTokenScope() {
        return tokenScope;
    }

    public void setTokenScope(String tokenScope) {
        this.tokenScope = tokenScope;
    }
}
