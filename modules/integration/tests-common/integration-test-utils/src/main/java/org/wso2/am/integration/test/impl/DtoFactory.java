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

import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ProductAPIDTO;

import java.util.List;

public class DtoFactory {

    /**
     * Creates Exported ThrottlingPolicyDTO using the given data
     *
     * @param type    Policy Type
     * @param subtype Throttling Policy Type
     * @param version APIM version
     * @param data    Throttling Policy data
     * @return
     */
    public static ExportThrottlePolicyDTO createExportThrottlePolicyDTO(String type, String subtype, String version,
            Object data) {
        ExportThrottlePolicyDTO exportPolicy = new ExportThrottlePolicyDTO();
        exportPolicy.data(data);
        exportPolicy.type(type);
        exportPolicy.subtype(subtype);
        exportPolicy.version(version);
        return exportPolicy;
    }

    public static APIProductDTO createApiProductDTO(String provider, String name, String context, String version,
            List<ProductAPIDTO> apis, List<String> polices) {
        return new APIProductDTO().
                accessControl(APIProductDTO.AccessControlEnum.NONE).
                visibility(APIProductDTO.VisibilityEnum.PUBLIC).
                apis(apis).
                context(context).
                name(name).
                version(version).
                policies(polices).
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

        ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO = new ApplicationThrottlePolicyDTO();
        applicationThrottlePolicyDTO.setPolicyName(policyName);
        applicationThrottlePolicyDTO.setDisplayName(displayName);
        applicationThrottlePolicyDTO.setDescription(description);
        applicationThrottlePolicyDTO.setIsDeployed(isDeployed);
        applicationThrottlePolicyDTO.setDefaultLimit(defaultLimit);
        BurstLimitDTO burstLimitDTO = new BurstLimitDTO();
        burstLimitDTO.setRateLimitCount(0);
        burstLimitDTO.setRateLimitTimeUnit(null);
        applicationThrottlePolicyDTO.setBurstLimit(burstLimitDTO);
        return applicationThrottlePolicyDTO;
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
     * Creates a throttle limit DTO using the given event count limit DTO.
     *
     * @param eventCountLimitDTO    Event count limit DTO object.
     * @return  Created throttle limit DTO.
     */
    public static ThrottleLimitDTO createEventCountThrottleLimitDTO(EventCountLimitDTO eventCountLimitDTO) {
        return new ThrottleLimitDTO().
                type(ThrottleLimitDTO.TypeEnum.EVENTCOUNTLIMIT).
                eventCount(eventCountLimitDTO);
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
     * Creates a subscription throttling permission DTO using the given parameters.
     *
     * @param permissionType   Permission type.
     * @param roles   Roles.
     * @return Created subscription throttle policy DTO.
     */
    public static SubscriptionThrottlePolicyPermissionDTO createSubscriptionThrottlePolicyPermissionDTO(
            SubscriptionThrottlePolicyPermissionDTO.PermissionTypeEnum permissionType, List<String> roles) {

        return new SubscriptionThrottlePolicyPermissionDTO().
                permissionType(permissionType).
                roles(roles);
    }

    /**
     * Creates a event count limit DTO using the given parameters.
     *
     * @param timeUnit      Time limit.
     * @param unitTime      Unit of time.
     * @param eventCount    Event count limit.
     * @return  Created event count limit DTO.
     */
    public static EventCountLimitDTO createEventCountLimitDTO(String timeUnit, Integer unitTime, Long eventCount) {
        return new EventCountLimitDTO().
                timeUnit(timeUnit).
                unitTime(unitTime).
                eventCount(eventCount);
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
            List<CustomAttributeDTO> customAttributes, boolean stopQuotaOnReach, String billingPlan,
            int subscriberCount, SubscriptionThrottlePolicyPermissionDTO permissions) {

        SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        subscriptionThrottlePolicyDTO.setPolicyName(policyName);
        subscriptionThrottlePolicyDTO.setDisplayName(displayName);
        subscriptionThrottlePolicyDTO.setDescription(description);
        subscriptionThrottlePolicyDTO.setIsDeployed(isDeployed);
        subscriptionThrottlePolicyDTO.setDefaultLimit(defaultLimit);
        subscriptionThrottlePolicyDTO.setGraphQLMaxComplexity(graphQLMaxComplexity);
        subscriptionThrottlePolicyDTO.setGraphQLMaxDepth(graphQLMaxDepth);
        subscriptionThrottlePolicyDTO.setRateLimitCount(rateLimitCount);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit(rateLimitTimeUnit);
        subscriptionThrottlePolicyDTO.setCustomAttributes(customAttributes);
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(stopQuotaOnReach);
        subscriptionThrottlePolicyDTO.setBillingPlan(billingPlan);
        subscriptionThrottlePolicyDTO.setSubscriberCount(subscriberCount);
        subscriptionThrottlePolicyDTO.setPermissions(permissions);

        return subscriptionThrottlePolicyDTO;
    }

    /**
     * Creates a subscription throttling policy DTO using the given parameters.
     *
     * @param policyName           Name of the policy.
     * @param displayName          Display name of the policy.
     * @param description          Description of the policy.
     * @param isDeployed           Deployed status of the policy.
     * @param defaultLimit         Default Limit of the policy.
     * @param rateLimitCount       Burst control request count.
     * @param rateLimitTimeUnit    Burst control time unit.
     * @param stopQuotaOnReach     Action to be taken when a user goes beyond the allocated quota.
     * @return Created subscription throttling policy DTO.
     */
    public static SubscriptionThrottlePolicyDTO createSubscriptionThrottlePolicyDTO(String policyName,
                                                                                    String displayName,
                                                                                    String description,
                                                                                    boolean isDeployed,
                                                                                    ThrottleLimitDTO defaultLimit,
                                                                                    int rateLimitCount,
                                                                                    String rateLimitTimeUnit,
                                                                                    boolean stopQuotaOnReach,
                                                                                    SubscriptionThrottlePolicyPermissionDTO permissions) {

        SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        subscriptionThrottlePolicyDTO.setPolicyName(policyName);
        subscriptionThrottlePolicyDTO.setDisplayName(displayName);
        subscriptionThrottlePolicyDTO.setDescription(description);
        subscriptionThrottlePolicyDTO.setIsDeployed(isDeployed);
        subscriptionThrottlePolicyDTO.setDefaultLimit(defaultLimit);
        subscriptionThrottlePolicyDTO.setRateLimitCount(rateLimitCount);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit(rateLimitTimeUnit);
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(stopQuotaOnReach);
        subscriptionThrottlePolicyDTO.setPermissions(permissions);
        subscriptionThrottlePolicyDTO.setSubscriberCount(0);
        subscriptionThrottlePolicyDTO.setBillingPlan("FREE");
        return subscriptionThrottlePolicyDTO;
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

        CustomRuleDTO ruleDTO = new CustomRuleDTO();
        ruleDTO.setPolicyName(policyName);
        ruleDTO.setDescription(description);
        ruleDTO.setIsDeployed(isDeployed);
        ruleDTO.setSiddhiQuery(siddhiQuery);
        ruleDTO.setKeyTemplate(keyTemplate);
        return ruleDTO;
    }

    /**
     * Creates a blacklist policy DTO using the given parameters.
     *
     * @param conditionType     Blocking condition type
     * @param conditionValue    Blocking condition value
     * @param conditionStatus   Activation status of the blocking condition
     * @return Created blocking conditions DTO.
     */
    public static BlockingConditionDTO createBlockingConditionDTO(BlockingConditionDTO.ConditionTypeEnum conditionType,
            String conditionValue, boolean conditionStatus) {

        return new BlockingConditionDTO().
                conditionType(conditionType).
                conditionValue(conditionValue).
                conditionStatus(conditionStatus);
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

        AdvancedThrottlePolicyDTO advancedPolicyDTO = new AdvancedThrottlePolicyDTO();
        advancedPolicyDTO.setPolicyName(policyName);
        advancedPolicyDTO.setDisplayName(displayName);
        advancedPolicyDTO.setDescription(description);
        advancedPolicyDTO.setIsDeployed(isDeployed);
        advancedPolicyDTO.setDefaultLimit(defaultLimit);
        advancedPolicyDTO.setConditionalGroups(conditionalGroups);
        return advancedPolicyDTO;
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

    /**
     * Creates an Environment DTO using the given parameters
     *
     * @param name        Name of the environment
     * @param displayName Display name of the environment
     * @param description Description of the environment
     * @param provider    Vendor provider of the environment
     * @param vhosts      Vhosts available in the environment
     * @return Environment DTO object
     */
    public static EnvironmentDTO createEnvironmentDTO(String name, String displayName, String description, String
            provider, boolean isReadOnly, List<VHostDTO> vhosts, String gatewayType) {
        return new EnvironmentDTO()
                .name(name)
                .displayName(displayName)
                .description(description)
                .provider(provider)
                .isReadOnly(isReadOnly)
                .vhosts(vhosts)
                .gatewayType(gatewayType);
    }

    /**
     * Creates a Vhost DTO using the given parameters
     *
     * @param host        Host name
     * @param httpContext HTTP context of access URL
     * @param httpPort    HTTP port
     * @param httpsPort   HTTPS port
     * @param wsPort      WS port
     * @param wssPort     WSS port
     * @return VHost DTO object
     */
    public static VHostDTO createVhostDTO(String host, String httpContext, Integer httpPort, Integer httpsPort,
                                          Integer wsPort, Integer wssPort) {
        return new VHostDTO().host(host)
                .httpContext(httpContext)
                .httpPort(httpPort)
                .httpsPort(httpsPort)
                .wsPort(wsPort)
                .wssPort(wssPort);
    }

    /**
     * Creates an api category DTO using the given parameters.
     *
     * @param name        Name of the label.
     * @param description Description of the label.
     * @return Created api category DTO.
     */
    public static APICategoryDTO createApiCategoryDTO(String name, String description) {

        return new APICategoryDTO().
                name(name).
                description(description);
    }
    /**
     * Creates an key manager DTO using the given parameters.
     *
     * @param name                       Name of key manager.
     * @param displayName                Display name of the key manager.
     * @param description                Description of the key manager.
     * @param type                       Type of the key manager.
     * @param issuer                     Issuer of the key manager.
     * @param consumerKeyClaim           Consumer key claim URI of the key manager.
     * @param scopesClaim                Scopes claim URI of the key manager.
     * @param availableGrantTypes        Available grant types of the key manager.
     * @param certificates               Certificates of the key manager.
     * @return Created key manager DTO.
     */
    public static KeyManagerDTO createKeyManagerDTO(String name, String description, String type, String displayName,
                                                    String issuer,
                                                    String consumerKeyClaim, String scopesClaim,
                                                    List<String> availableGrantTypes,
                                                    KeyManagerCertificatesDTO certificates) {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType(type);
        keyManagerDTO.setName(name);
        keyManagerDTO.setDescription(description);
        keyManagerDTO.setDisplayName(displayName);
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setConsumerKeyClaim(consumerKeyClaim);
        keyManagerDTO.setScopesClaim(scopesClaim);
        keyManagerDTO.setIssuer(issuer);
        keyManagerDTO.setCertificates(certificates);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO.setAvailableGrantTypes(availableGrantTypes);
        return keyManagerDTO;
    }


    /**
     * Creates an key manager DTO using the given parameters.
     *
     * @param name                       Name of key manager.
     * @param displayName                Display name of the key manager.
     * @param description                Description of the key manager.
     * @param type                       Type of the key manager.
     * @param issuer                     Issuer of the key manager.
     * @param clientRegistrationEndpoint Client registration endpoint of the key manager.
     * @param introspectionEndpoint      Introspection endpoint of the key manager.
     * @param tokenEndpoint              Token endpoint of the key manager.
     * @param revokeEndpoint             Revoke endpoint of the key manager.
     * @param userInfoEndpoint           User info endpoint of the key manager.
     * @param authorizeEndpoint          Authorize endpoint of the key manager.
     * @param scopeManagementEndpoint    Scope management endpoint of the key manager.
     * @param consumerKeyClaim           Consumer key claim URI of the key manager.
     * @param scopesClaim                Scopes claim URI of the key manager.
     * @param availableGrantTypes        Available grant types of the key manager.
     * @param additionalProperties       Additional properties of the key manager.
     * @param certificates               Certificates of the key manager.
     * @return Created key manager DTO.
     */
    public static KeyManagerDTO createKeyManagerDTO(String name, String description, String type, String displayName,
                                                    String introspectionEndpoint, String issuer,
                                                    String clientRegistrationEndpoint, String tokenEndpoint,
                                                    String revokeEndpoint, String userInfoEndpoint,
                                                    String authorizeEndpoint, String scopeManagementEndpoint,
                                                    String consumerKeyClaim, String scopesClaim,
                                                    List<String> availableGrantTypes, Object additionalProperties,
                                                    KeyManagerCertificatesDTO certificates) {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType(type);
        keyManagerDTO.setName(name);
        keyManagerDTO.setDescription(description);
        keyManagerDTO.setDisplayName(displayName);
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setIntrospectionEndpoint(introspectionEndpoint);
        keyManagerDTO.setRevokeEndpoint(revokeEndpoint);
        keyManagerDTO.setClientRegistrationEndpoint(clientRegistrationEndpoint);
        keyManagerDTO.setTokenEndpoint(tokenEndpoint);
        keyManagerDTO.setUserInfoEndpoint(userInfoEndpoint);
        keyManagerDTO.setAuthorizeEndpoint(authorizeEndpoint);
        keyManagerDTO.setScopeManagementEndpoint(scopeManagementEndpoint);
        keyManagerDTO.setConsumerKeyClaim(consumerKeyClaim);
        keyManagerDTO.setScopesClaim(scopesClaim);
        keyManagerDTO.setIssuer(issuer);
        keyManagerDTO.setCertificates(certificates);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO.setAvailableGrantTypes(availableGrantTypes);
        keyManagerDTO.setAdditionalProperties(additionalProperties);
        return keyManagerDTO;
    }

    /**
     * Creates an key manager certificate DTO using the given parameters.
     *
     * @param type        Type of the key manager certificate.
     * @param value       Value of the key manager certificate.
     * @return Created key manager certificate DTO.
     */
    public static KeyManagerCertificatesDTO createKeyManagerCertificatesDTO(KeyManagerCertificatesDTO.TypeEnum type,
                                                                            String value) {

        KeyManagerCertificatesDTO keyManagerCertificatesDTO =  new KeyManagerCertificatesDTO();
        keyManagerCertificatesDTO.setType(type);
        keyManagerCertificatesDTO.setValue(value);
        return keyManagerCertificatesDTO;
    }
}
