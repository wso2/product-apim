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

import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.BandwidthLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.CustomAttributeDTO;
import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.JWTClaimsConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.LabelDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
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
     * @param displayName  Display name of the policy.
     * @param description  Description of the policy.
     * @param isDeployed   Deployed status of the policy.
     * @param defaultLimit Default Limit of the policy.
     * @return Created application throttling policy DTO.
     */
    public static ApplicationThrottlePolicyDTO createApplicationThrottlePolicyDTO(String policyName, String displayName,
            String description, boolean isDeployed, ThrottleLimitDTO defaultLimit) {

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
            RequestCountLimitDTO requestCountLimitDTO, BandwidthLimitDTO bandwidthLimitDTO) {

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
     * @param displayName          Display name of the policy.
     * @param description          Description of the policy.
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
            String displayName, String description, boolean isDeployed, ThrottleLimitDTO defaultLimit,
            int graphQLMaxComplexity, int graphQLMaxDepth, int rateLimitCount, String rateLimitTimeUnit,
            List<CustomAttributeDTO> customAttributes, boolean stopQuotaOnReach, String billingPlan) {

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

    /**
     * Creates a custom throttling policy DTO using the given parameters.
     *
     * @param policyName  Name of the policy.
     * @param description Description of the policy.
     * @param isDeployed  Deployed status of the policy.
     * @param siddhiQuery Siddhi query which represents the custom throttling policy.
     * @param keyTemplate The specific combination of attributes that are checked in the policy.
     * @return Created custom throttling policy DTO.
     */
    public static CustomRuleDTO createCustomThrottlePolicyDTO(String policyName, String description, boolean isDeployed,
            String siddhiQuery, String keyTemplate) {

        return new CustomRuleDTO().
                policyName(policyName).
                description(description).
                isDeployed(isDeployed).
                siddhiQuery(siddhiQuery).
                keyTemplate(keyTemplate);
    }

    /**
     * Creates a header condition DTO using the given parameters.
     *
     * @param headerName  Name of the header.
     * @param headerValue Value of the header.
     * @return Created header condition DTO.
     */
    public static HeaderConditionDTO createHeaderConditionDTO(String headerName, String headerValue) {

        return new HeaderConditionDTO().
                headerName(headerName).
                headerValue(headerValue);
    }

    /**
     * Creates a IP condition DTO using the given parameters.
     *
     * @param ipConditionType Type of the IP condition.
     * @param specificIP      Specific IP when "IPSPECIFIC" is used as the ipConditionType.
     * @param startingIP      Staring IP when "IPRANGE" is used as the ipConditionType
     * @param endingIP        Ending IP when "IPRANGE" is used as the ipConditionType.
     * @return Created IP condition DTO.
     */
    public static IPConditionDTO createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum ipConditionType,
            String specificIP, String startingIP, String endingIP) {

        return new IPConditionDTO().
                ipConditionType(ipConditionType).
                specificIP(specificIP).
                startingIP(startingIP).
                endingIP(endingIP);
    }

    /**
     * Creates a JWT claims condition DTO using the given parameters.
     *
     * @param claimUrl  JWT claim URL.
     * @param attribute Attribute to be matched.
     * @return Created JWT claims condition DTO.
     */
    public static JWTClaimsConditionDTO createJWTClaimsConditionDTO(String claimUrl, String attribute) {

        return new JWTClaimsConditionDTO().
                claimUrl(claimUrl).
                attribute(attribute);
    }

    /**
     * Creates a query parameter condition DTO using the given parameters.
     *
     * @param parameterName  Name of the query parameter.
     * @param parameterValue Value of the query parameter to be matched.
     * @return Created query parameter condition DTO.
     */
    public static QueryParameterConditionDTO createQueryParameterConditionDTO(String parameterName,
            String parameterValue) {

        return new QueryParameterConditionDTO().
                parameterName(parameterName).
                parameterValue(parameterValue);
    }

    /**
     * Creates a throttle condition DTO using the given parameters.
     *
     * @param type                    Type of the throttling condition.
     * @param invertCondition         Specifies whether inversion of the condition to be matched against the request.
     * @param headerCondition         HTTP Header based throttling condition.
     * @param ipCondition             IP based throttling condition.
     * @param jwtClaimsCondition      JWT claim attribute based throttling condition.
     * @param queryParameterCondition Query parameter based throttling condition.
     * @return Created throttle condition DTO.
     */
    public static ThrottleConditionDTO createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum type,
            boolean invertCondition, HeaderConditionDTO headerCondition, IPConditionDTO ipCondition,
            JWTClaimsConditionDTO jwtClaimsCondition, QueryParameterConditionDTO queryParameterCondition) {

        return new ThrottleConditionDTO().
                type(type).
                invertCondition(invertCondition).
                headerCondition(headerCondition).
                ipCondition(ipCondition).
                jwtClaimsCondition(jwtClaimsCondition).
                queryParameterCondition(queryParameterCondition);
    }

    /**
     * Creates a conditional group DTO using the given parameters.
     *
     * @param description Description of the conditional group.
     * @param conditions  Individual throttling conditions.
     * @param limit       Throttle limit of the conditional group.
     * @return Created conditional group DTO.
     */
    public static ConditionalGroupDTO createConditionalGroupDTO(String description,
            List<ThrottleConditionDTO> conditions, ThrottleLimitDTO limit) {

        return new ConditionalGroupDTO().
                description(description).
                conditions(conditions).
                limit(limit);
    }

    /**
     * Creates an advanced throttling policy DTO using the given parameters.
     *
     * @param policyName        Name of the policy.
     * @param displayName       Display name of the policy.
     * @param description       Description of the policy.
     * @param isDeployed        Deployed status of the policy.
     * @param defaultLimit      Default Limit of the policy.
     * @param conditionalGroups List of conditional groups attached to the policy.
     * @return Created advanced throttling policy DTO.
     */
    public static AdvancedThrottlePolicyDTO createAdvancedThrottlePolicyDTO(String policyName, String displayName,
            String description, boolean isDeployed, ThrottleLimitDTO defaultLimit,
            List<ConditionalGroupDTO> conditionalGroups) {

        return new AdvancedThrottlePolicyDTO().
                policyName(policyName).
                displayName(displayName).
                description(description).
                isDeployed(isDeployed).
                defaultLimit(defaultLimit).
                conditionalGroups(conditionalGroups);
    }

    /**
     * Creates a label DTO using the given parameters.
     *
     * @param name        Name of the label.
     * @param description Description of the label.
     * @param accessUrls  Access URLs.
     * @return Created label DTO.
     */
    public static LabelDTO createLabelDTO(String name, String description, List<String> accessUrls) {

        return new LabelDTO().
                name(name).
                description(description).
                accessUrls(accessUrls);
    }
}
