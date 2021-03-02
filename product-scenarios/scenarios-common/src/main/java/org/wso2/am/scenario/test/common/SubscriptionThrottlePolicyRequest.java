/*
 *Copyright (c), WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.test.common;

import org.wso2.am.integration.test.utils.bean.AbstractRequest;

public class SubscriptionThrottlePolicyRequest extends AbstractRequest {

    private String policyName;
    private String description;
    private String defaultQuotaPolicy = "requestCount";
    private String defaultRequestCount;
    private String defaultUnitTime;
    private String defaultTimeUnit;
    private String rateLimitCount = "-1";
    private String rateLimitTimeUnit = "NA";
    private String attributes = "[{\"name\":\"key\", \"value\":\"values\"}]";
    private String stopOnQuotaReach = "true";
    private String tierPlan = "FREE";
    private String permissionType = "allow";
    private String roles = ScenarioTestConstants.EVERYONE_ROLE;

    public SubscriptionThrottlePolicyRequest(String policyName, String description, String requestCount,
            String unitTime, String timeUnit) {
        this.policyName = policyName;
        this.description = description;
        this.defaultRequestCount = requestCount;
        this.defaultUnitTime = unitTime;
        this.defaultTimeUnit = timeUnit;
    }

    @Override
    public void setAction() {
        if (action != null) {
            super.setAction(action);
        } else {
            super.setAction("add");
        }
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultQuotaPolicy() {
        return defaultQuotaPolicy;
    }

    public void setDefaultQuotaPolicy(String defaultQuotaPolicy) {
        this.defaultQuotaPolicy = defaultQuotaPolicy;
    }

    public String getDefaultRequestCount() {
        return defaultRequestCount;
    }

    public void setDefaultRequestCount(String defaultRequestCount) {
        this.defaultRequestCount = defaultRequestCount;
    }

    public String getDefaultUnitTime() {
        return defaultUnitTime;
    }

    public void setDefaultUnitTime(String defaultUnitTime) {
        this.defaultUnitTime = defaultUnitTime;
    }

    public String getDefaultTimeUnit() {
        return defaultTimeUnit;
    }

    public void setDefaultTimeUnit(String defaultTimeUnit) {
        this.defaultTimeUnit = defaultTimeUnit;
    }

    public String getRateLimitCount() {
        return rateLimitCount;
    }

    public void setRateLimitCount(String rateLimitCount) {
        this.rateLimitCount = rateLimitCount;
    }

    public String getRateLimitTimeUnit() {
        return rateLimitTimeUnit;
    }

    public void setRateLimitTimeUnit(String rateLimitTimeUnit) {
        this.rateLimitTimeUnit = rateLimitTimeUnit;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(String stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }

    public String getTierPlan() {
        return tierPlan;
    }

    public void setTierPlan(String tierPlan) {
        this.tierPlan = tierPlan;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    @Override
    public void init() {
        addParameter("policyName", policyName);
        addParameter("description", description);
        addParameter("defaultQuotaPolicy", defaultQuotaPolicy);
        addParameter("defaultRequestCount", defaultRequestCount);
        addParameter("defaultUnitTime", defaultUnitTime);
        addParameter("defaultTimeUnit", defaultTimeUnit);
        addParameter("rateLimitCount", rateLimitCount);
        addParameter("rateLimitTimeUnit", rateLimitTimeUnit);
        addParameter("attributes", attributes);
        addParameter("stopOnQuotaReach", stopOnQuotaReach);
        addParameter("tierPlan", tierPlan);
        addParameter("permissionType", permissionType);
        addParameter("roles", roles);
    }
}
