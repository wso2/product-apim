/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.am.integration.test;

public class Constants {

    /*
    Tenant Creation
     */
    public static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
    public static final String JAVAX_NET_SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String JAVAX_NET_SSL_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    public static final String CLIENT_TRUSTORE_JKS = "client-truststore.jks";
    public static final String WSO2_CARBON = "wso2carbon";
    public static final String JKS = "JKS";
    public static final String TENANT_MGT_ADMIN_SERVICE = "TenantMgtAdminService";
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String CHAR_AT = "@";
    public static final String USAGE_PLAN_DEMO = "demo";

    /*
    API Creation
     */
    public static final String API_DESCRIPTION = "This is the api description";
    public static final String PROVIDER_ADMIN = "admin";
    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_HTTPS = "https";
    public static final String GATEWAY_ENVIRONMENT = "Default";
    public static final String SOLACE_GATEWAY_ENVIRONMENT = "solace";
    public static final String WSO2_GATEWAY_ENVIRONMENT = "wso2";
    public static final String APPLICATION_JSON = "application/json";
    public static final String API_DEFINITION = "api-definition-";
    public static final String ENDPOINT_DEFINITION = "endpoint-config-";
    public static final String JSON_EXTENSION = ".json";
    public static final String TIERS_UNLIMITED = "Unlimited";
    public static final String PUBLISHED = "Publish";
    public static final String DEPRECATE = "Deprecate";
    public static final String RETIRE = "Retire";
    public static final String BLOCK = "Block";
    public static final String BLOCKED = "Blocked";
    public static final String REJECT = "Reject";
    public static final String DEPLOY_AS_PROTOTYPE = "Deploy as a Prototype";
    public static final String API_LIFECYCLE = "APILifeCycle";
    public static final String XML = ".xml";
    public static final String CARBON_HOME = "carbon.home";
    public static final String CARBON_HOME_VALUE = "../../";

    public static final String BANDWIDTH_TYPE = "bandwidthVolume";
    public static final String REQUEST_COUNT_TYPE = "requestCount";
    public static final String EVENT_COUNT_TYPE = "eventCount";

    public static class APIMGovernanceTestConstants {
        public static final String DEFAULT_RULESET_WSO2_API = "WSO2 API Management Guidelines";
        public static final String DEFAULT_RULESET_WSO2_REST = "WSO2 REST API Design Guidelines";
        public static final String DEFAULT_RULESET_OWASP = "OWASP Top 10";
        public static final String DEFAULT_POLICY_NAME = "WSO2 API Management Best Practices";

        public static final String REST_API_ARTIFACT_TYPE = "REST_API";
        public static final String API_DEFINITION_RULE_TYPE = "API_DEFINITION";
        public static final String SPECTRAL_RULE_CATEGORY  = "SPECTRAL";
        public static final String ADMIN_PROVIDER = "admin";

        public static final String SIMPLE_SPECTRAL_RULESET_FILE_NAME = "simple-spectral-ruleset.yaml";
        public static final String SIMPLE_SPECTRAL_RULESET_NAME = "Simple Spectral Ruleset";
        public static final String SIMPLE_SPECTRAL_RULESET_DESCRIPTION = "This is a sample ruleset description";
        public static final String RULESET_DOCUMENTATION_LINK = "https://wso2.com";
        public static final String TEST_RESOURCE_DIRECTORY = "apim-governance";

        public static final String TEST_POLICY_NAME = "TestPolicy";
        public static final String TEST_POLICY_DESCRIPTION = "Test Policy Description";
        public static final String GLOBAL_LABEL = "global";
    }
}
