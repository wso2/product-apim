/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.test.impl;

import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.BandwidthLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.CustomAttributeDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ProductAPIDTO;

import java.util.Arrays;
import java.util.List;

public class DtoFactory {

    public static APIProductDTO createApiProductDTO(String provider, String name, String context, List<ProductAPIDTO> apis,
                                                    List<String> polices) {
        return new APIProductDTO().
                accessControl(APIProductDTO.AccessControlEnum.NONE).
                visibility(APIProductDTO.VisibilityEnum.PUBLIC).
                apis(apis).
                context(context).
                name(name).
                policies(polices).
                gatewayEnvironments(Arrays.asList("Production and Sandbox")).
                provider(provider);
    }

    /**
     * Creates an application throttling policy DTO using the given parameters.
     *
     * @param policyName   Name of the policy.
     * @param displayName  Display name  policy.
     * @param description  Description policy.
     * @param isDeployed   Deployed status of the policy.
     * @param defaultLimit Default Limit of the policy.
     * @return Created application throttling policy DTO.
     */
    public static ApplicationThrottlePolicyDTO createApplicationThrottlePolicyDTO(String policyName, String displayName,
                                                                                  String description,
                                                                                  boolean isDeployed,
                                                                                  ThrottleLimitDTO defaultLimit) {

        return new ApplicationThrottlePolicyDTO().
                policyName(policyName).
                displayName(displayName).
                description(description).
                isDeployed(isDeployed).
                defaultLimit(defaultLimit);
    }

    /**
     * Creates a throttle limit DTO using the given parameters.
     *
     * @param type                 Type of the throttle limit. (Eg:- Request Count Limit, Bandwidth Limit)
     * @param requestCountLimitDTO Request count limit DTO object.
     * @param bandwidthLimitDTO    Bandwidth limit DTO object.
     * @return Created throttle limit DTO.
     */
    public static ThrottleLimitDTO createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum type,
                                                          RequestCountLimitDTO requestCountLimitDTO,
                                                          BandwidthLimitDTO bandwidthLimitDTO) {

        return new ThrottleLimitDTO().
                type(type).
                requestCount(requestCountLimitDTO).
                bandwidth(bandwidthLimitDTO);
    }

    /**
     * Creates a request count limit DTO using the given parameters.
     *
     * @param timeUnit     Time limit.
     * @param unitTime     Unit of time.
     * @param requestCount Request count limit.
     * @return Created request count limit DTO.
     */
    public static RequestCountLimitDTO createRequestCountLimitDTO(String timeUnit, Integer unitTime,
                                                                  Long requestCount) {

        return new RequestCountLimitDTO().
                timeUnit(timeUnit).
                unitTime(unitTime).
                requestCount(requestCount);
    }

    /**
     * Creates a bandwidth limit DTO using the given parameters.
     *
     * @param timeUnit   Time limit.
     * @param unitTime   Unit of time.
     * @param dataAmount Data amount limit.
     * @param dataUnit   Unit of data.
     * @return Created bandwidth limit DTO.
     */
    public static BandwidthLimitDTO createBandwidthLimitDTO(String timeUnit, Integer unitTime, Long dataAmount,
                                                            String dataUnit) {

        return new BandwidthLimitDTO().
                timeUnit(timeUnit).
                unitTime(unitTime).
                dataAmount(dataAmount).
                dataUnit(dataUnit);
    }

    /**
     * Creates a subscription throttling policy DTO using the given parameters.
     *
     * @param policyName           Name of the policy.
     * @param displayName          Display name  policy.
     * @param description          Description policy.
     * @param isDeployed           Deployed status of the policy.
     * @param defaultLimit         Default Limit of the policy.
     * @param graphQLMaxComplexity Maximum Complexity of the GraphQL query.
     * @param graphQLMaxDepth      Maximum Depth of the GraphQL query.
     * @param rateLimitCount       Burst control request count.
     * @param rateLimitTimeUnit    Burst control time unit.
     * @param customAttributes     Custom attributes added to the Subscription Throttling Policy.
     * @param stopQuotaOnReach     Action to be taken when a user goes beyond the allocated quota.
     * @param billingPlan          Defines whether this is a Paid or a Free plan.
     * @return Created subscription throttling policy DTO.
     */
    public static SubscriptionThrottlePolicyDTO createSubscriptionThrottlePolicyDTO(String policyName,
                                                                                    String displayName,
                                                                                    String description,
                                                                                    boolean isDeployed,
                                                                                    ThrottleLimitDTO defaultLimit,
                                                                                    int graphQLMaxComplexity,
                                                                                    int graphQLMaxDepth,
                                                                                    int rateLimitCount,
                                                                                    String rateLimitTimeUnit,
                                                                                    List<CustomAttributeDTO> customAttributes,
                                                                                    boolean stopQuotaOnReach,
                                                                                    String billingPlan) {

        return new SubscriptionThrottlePolicyDTO().
                policyName(policyName).
                displayName(displayName).
                description(description).
                isDeployed(isDeployed).
                defaultLimit(defaultLimit).
                graphQLMaxComplexity(graphQLMaxComplexity).
                graphQLMaxDepth(graphQLMaxDepth).
                rateLimitCount(rateLimitCount).
                rateLimitTimeUnit(rateLimitTimeUnit).
                customAttributes(customAttributes).
                stopOnQuotaReach(stopQuotaOnReach).
                billingPlan(billingPlan);
    }
}
