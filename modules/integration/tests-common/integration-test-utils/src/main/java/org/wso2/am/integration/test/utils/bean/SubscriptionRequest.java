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
 * action=addAPISubscription&name=apiName&version=1.0.0&provider=provider&tier=Gold&applicationName=DefaultApplication
 */

/**
 * Basic request for subscribe to API
 */

public class SubscriptionRequest extends AbstractRequest {

    private String name;
    private String provider;
    private String version = "1.0.0";
    private String applicationName = "DefaultApplication";
    private String tier = "Gold";

    /**
     * Constructor using default values
     * @param apiName
     * @param provider
     */

    public SubscriptionRequest(String apiName, String provider) {
        this.name = apiName;
        this.provider = provider;
    }

    /**
     * constructor with more configurable params
     * @param apiName
     * @param apiVersion
     * @param provider
     * @param appName
     * @param tier
     */

    public SubscriptionRequest(String apiName, String apiVersion, String provider, String appName, String tier){
        this.name = apiName;
        this.version = apiVersion;
        this.provider = provider;
        this.applicationName = appName;
        this.tier = tier;
    }

    @Override
    public void setAction() {
        setAction("addAPISubscription");
    }

    @Override
    public void init() {
        addParameter("name", name);
        addParameter("provider", provider);
        addParameter("version", version);
        addParameter("applicationName", applicationName);
        addParameter("tier", tier);
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }
}
