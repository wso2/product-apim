/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.apimgt.samples.utils;

import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiClient;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyCollectionApi;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.AdvancedThrottlePolicy;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.BandwidthLimit;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.ConditionalGroup;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.HeaderCondition;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.IPCondition;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.RequestCountLimit;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.ThrottleCondition;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.ThrottleLimit;

public class ThrottlingUtils {

    /**
     * This method is used to add advanceThrottlingPolicies
     *
     * @param displayName  policy display name
     * @param policyName   policy name
     * @param description  policy description
     * @param timeUnit     time unit
     * @param unitTime     unit time
     * @param requestCount request count per unit time
     * @param typeEnum     throttling type
     * @param dataAmount   bandwidth amount
     * @param dataUnit     bandwidth  unit
     * @return created throttle policy Id.
     * @throws ApiException Throws is an error occurs when creating a advance throttle policy.
     */
    public static String addAdvanceThrottlePolicy(String displayName, String policyName, String description,
            String timeUnit, Integer unitTime, long requestCount, ThrottleLimit.TypeEnum typeEnum, long dataAmount,
            String dataUnit) throws ApiException {

        AdvancedThrottlePolicy advancedThrottlePolicy = createThrottlePolicyObject(displayName, policyName, description,
                timeUnit, unitTime, requestCount, typeEnum, dataAmount, dataUnit);

        AdvancedPolicyCollectionApi advancedPolicyCollectionApi = new AdvancedPolicyCollectionApi();
        AdvancedThrottlePolicy result = advancedPolicyCollectionApi
                .throttlingPoliciesAdvancedPost(advancedThrottlePolicy, Constants.APPLICATION_JSON);
        return result.getPolicyId();
    }

    /**
     * This method is used to add advanceThrottlingPolicies for tenants.
     *
     * @param displayName   policy display name
     * @param policyName    policy name
     * @param description   policy description
     * @param timeUnit      time unit
     * @param unitTime      unit time
     * @param requestCount  request count per unit time
     * @param typeEnum      throttling type
     * @param dataAmount    bandwidth amount
     * @param dataUnit      bandwidth  unit
     * @param tenantDomain  tenant domain
     * @param adminUsername tenant admin username
     * @param adminPassword tenant admin password
     * @return created throttle policy Id.
     * @throws ApiException Throws is an error occurs when creating a advance throttle policy.
     */
    public static String addAdvanceThrottlePolicyForTenants(String displayName, String policyName, String description,
            String timeUnit, Integer unitTime, long requestCount, ThrottleLimit.TypeEnum typeEnum, long dataAmount,
            String dataUnit, String tenantDomain, String adminUsername, String adminPassword) throws ApiException {

        AdvancedThrottlePolicy advancedThrottlePolicy = createThrottlePolicyObject(displayName, policyName, description,
                timeUnit, unitTime, requestCount, typeEnum, dataAmount, dataUnit);

        ApiClient apiClient = new ApiClient(tenantDomain, adminUsername, adminPassword);
        AdvancedPolicyCollectionApi advancedPolicyCollectionApi = new AdvancedPolicyCollectionApi();
        advancedPolicyCollectionApi.setApiClient(apiClient);
        AdvancedThrottlePolicy result = advancedPolicyCollectionApi
                .throttlingPoliciesAdvancedPost(advancedThrottlePolicy, Constants.APPLICATION_JSON);
        return result.getPolicyId();
    }

    /**
     * This method is used to add advanceThrottlingPolicies with Conditional Groups.
     *
     * @param displayName           policy display name
     * @param policyName            policy name
     * @param description           policy description
     * @param timeUnit              time unit
     * @param unitTime              unit time
     * @param requestCount          request count per unit time
     * @param typeEnum              throttling type
     * @param dataAmount            bandwidth amount
     * @param dataUnit              bandwidth  unit
     * @param startingIP            starting ip address of allowed ip range
     * @param endingIP              ending ip address of allowed ip range
     * @param headerName            header name
     * @param headerValue           heder value
     * @param invertHeaderCondition blocking the header name for the above value or not.
     * @return created throttle policy Id.
     * @throws ApiException Throws is an error occurs when creating a advance throttle policy.
     */
    public static String addAdvanceThrottlePolicyWithConditionalGroups(String displayName, String policyName,
            String description, String timeUnit, Integer unitTime, long requestCount, ThrottleLimit.TypeEnum typeEnum,
            long dataAmount, String dataUnit, String startingIP, String endingIP, String headerName, String headerValue,
            boolean invertHeaderCondition, String conditionalGroupDescription) throws ApiException {

        AdvancedThrottlePolicy advancedThrottlePolicy = createThrottlePolicyObjectWithConditionalGroups(displayName,
                policyName, description, timeUnit, unitTime, requestCount, typeEnum, dataAmount, dataUnit, startingIP,
                endingIP, headerName, headerValue, invertHeaderCondition, conditionalGroupDescription);

        AdvancedPolicyCollectionApi advancedPolicyCollectionApi = new AdvancedPolicyCollectionApi();
        AdvancedThrottlePolicy result = advancedPolicyCollectionApi
                .throttlingPoliciesAdvancedPost(advancedThrottlePolicy, Constants.APPLICATION_JSON);
        return result.getPolicyId();
    }

    private static AdvancedThrottlePolicy createThrottlePolicyObject(String displayName, String policyName,
            String description, String timeUnit, Integer unitTime, long requestCount, ThrottleLimit.TypeEnum typeEnum,
            long dataAmount, String dataUnit) {

        AdvancedThrottlePolicy advancedThrottlePolicy = new AdvancedThrottlePolicy();
        advancedThrottlePolicy.displayName(displayName);
        advancedThrottlePolicy.setPolicyName(policyName);
        advancedThrottlePolicy.setDescription(description);

        if (ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT.equals(typeEnum)) {
            RequestCountLimit requestCountLimit = new RequestCountLimit();
            requestCountLimit.setType(ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT);
            requestCountLimit.setTimeUnit(timeUnit);
            requestCountLimit.setUnitTime(unitTime);
            requestCountLimit.requestCount(requestCount);

            advancedThrottlePolicy.setDefaultLimit(requestCountLimit);

        } else if (ThrottleLimit.TypeEnum.BANDWIDTHLIMIT.equals(typeEnum)) {
            BandwidthLimit bandwidthLimit = new BandwidthLimit();
            bandwidthLimit.setDataAmount(dataAmount);
            bandwidthLimit.setDataUnit(dataUnit);
            bandwidthLimit.setTimeUnit(timeUnit);
            bandwidthLimit.setUnitTime(unitTime);
            bandwidthLimit.setType(ThrottleLimit.TypeEnum.BANDWIDTHLIMIT);

            advancedThrottlePolicy.setDefaultLimit(bandwidthLimit);

        } else {
            return null;
        }
        return advancedThrottlePolicy;
    }

    private static AdvancedThrottlePolicy createThrottlePolicyObjectWithConditionalGroups(String displayName,
            String policyName, String description, String timeUnit, Integer unitTime, long requestCount,
            ThrottleLimit.TypeEnum typeEnum, long dataAmount, String dataUnit, String startingIP, String endingIP,
            String headerName, String headerValue, boolean invertHeaderCondition, String conditionalGroupDescription) {

        AdvancedThrottlePolicy advancedThrottlePolicy = createThrottlePolicyObject(displayName, policyName, description,
                timeUnit, unitTime, requestCount, typeEnum, dataAmount, dataUnit);

        IPCondition ipCondition = new IPCondition();
        ipCondition.setIpConditionType(IPCondition.IpConditionTypeEnum.IPRANGE);
        ipCondition.setStartingIP(startingIP);
        ipCondition.setEndingIP(endingIP);
        ipCondition.setType(ThrottleCondition.TypeEnum.IPCONDITION);

        HeaderCondition headerCondition = new HeaderCondition();
        headerCondition.setType(ThrottleCondition.TypeEnum.HEADERCONDITION);
        headerCondition.setHeaderName(headerName);
        headerCondition.setHeaderValue(headerValue);
        headerCondition.invertCondition(invertHeaderCondition);

        ConditionalGroup conditionalGroup = new ConditionalGroup();
        conditionalGroup.setDescription(conditionalGroupDescription);
        conditionalGroup.addConditionsItem(ipCondition);
        conditionalGroup.addConditionsItem(headerCondition);

        if (ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT.equals(typeEnum)) {
            RequestCountLimit requestCountLimit = new RequestCountLimit();
            requestCountLimit.setType(ThrottleLimit.TypeEnum.REQUESTCOUNTLIMIT);
            requestCountLimit.setTimeUnit(timeUnit);
            requestCountLimit.setUnitTime(unitTime);
            requestCountLimit.requestCount(requestCount);

            conditionalGroup.setLimit(requestCountLimit);

        } else if (ThrottleLimit.TypeEnum.BANDWIDTHLIMIT.equals(typeEnum)) {
            BandwidthLimit bandwidthLimit = new BandwidthLimit();
            bandwidthLimit.setDataAmount(dataAmount);
            bandwidthLimit.setDataUnit(dataUnit);
            bandwidthLimit.setTimeUnit(timeUnit);
            bandwidthLimit.setUnitTime(unitTime);
            bandwidthLimit.setType(ThrottleLimit.TypeEnum.BANDWIDTHLIMIT);

            conditionalGroup.setLimit(bandwidthLimit);
        }

        advancedThrottlePolicy.addConditionalGroupsItem(conditionalGroup);

        return advancedThrottlePolicy;
    }

}
