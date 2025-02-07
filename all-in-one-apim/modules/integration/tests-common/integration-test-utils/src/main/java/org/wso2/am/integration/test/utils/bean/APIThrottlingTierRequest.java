/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * Basic request bean for adding/editing throttling tier
 * action=addTier&tierName=Platinum&requestCount=30&unitTime=60&description=Platinum&stopOnQuotaReach=true&tierPlan=FREE
 * &attributes={}
 *
 */
public class APIThrottlingTierRequest extends AbstractRequest {
    
    private String tierName;
    private String requestCount;
    private String unitTime;
    private String description;
    private String stopOnQuotaReach;
    private String action;
    private String tierPlan;
    private String attributes;


    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public void setTierPlan(String tierPlan) {
        this.tierPlan = tierPlan;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public String getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(String requestCount) {
        this.requestCount = requestCount;
    }

    public String getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(String unitTime) {
        this.unitTime = unitTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String isStopOnQuotaReach() {
        return stopOnQuotaReach;
    }

    public void setStopOnQuotaReach(String stopOnQuotaReach) {
        this.stopOnQuotaReach = stopOnQuotaReach;
    }
    
    public APIThrottlingTierRequest(String tierName, String requestCount, String description, String unitTime, 
                                    String stopOnQuotaReach, String tierPlan) {
        this.tierName = tierName;
        this.requestCount = requestCount;
        this.description = description;
        this.unitTime = unitTime;
        this.stopOnQuotaReach = stopOnQuotaReach;
        this.tierPlan = tierPlan;
    }

    @Override
    public void setAction() {
        if (action != null) {
            super.setAction(action);
        } else {
            super.setAction("addTier");
        }
        
    }

    @Override
    public void init() {
       addParameter("tierName", tierName);  
       addParameter("requestCount", requestCount);
       addParameter("description", description);
       addParameter("unitTime", unitTime);
       addParameter("stopOnQuotaReach", stopOnQuotaReach);
       addParameter("tierPlan", tierPlan);
       addParameter("attributes", attributes);
    }

}
