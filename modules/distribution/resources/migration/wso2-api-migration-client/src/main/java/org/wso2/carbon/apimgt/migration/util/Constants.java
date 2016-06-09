/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.migration.util;

import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.governance.api.util.GovernanceConstants;
import org.wso2.carbon.registry.core.RegistryConstants;

public class Constants {

    public static final String VERSION_1_8 = "1.8.0";
    public static final String VERSION_1_9 = "1.9.0";
    public static final String VERSION_1_9_1 = "1.9.1";
    public static final String VERSION_1_10 = "1.10.0";
    public static final String VERSION_2_0_0 = "2.0.0";
    public static final String LINE_BREAK = "\\n";
    public static final String CONSTRAINT = "constraint";
    public static final String ALTER = "alter";
    public static final String DELIMITER = ";";
    public static final String API = "api";
    public static final String APIM_COMPONENT = "apim";

    public static final String GOVERNANCE_COMPONENT_REGISTRY_LOCATION = "/repository/components/org.wso2.carbon" +
            ".governance";
    public static final String RXT_PATH = "/repository/resources/rxts/api.rxt";
    public static final String DATA_SOURCE_NAME = "DataSourceName";
    public static final String RXT_REG_PATH = GovernanceConstants.RXT_CONFIGS_PATH;
    public static final String GOVERNANCE_ARTIFACT_CONFIGURATION_PATH =
            RegistryConstants.GOVERNANCE_COMPONENT_PATH + "/configuration/";

    // Migration client argument property names
    public static final String ARG_RUN_SPECIFIC_VERSION = "runSpecificVersion";
    public static final String ARG_MIGRATE_FROM_VERSION = "migrateFromVersion";
    public static final String ARG_OPTIONS = "options";
    public static final String ARG_COMPONENT = "component";
    public static final String ARG_MIGRATE_TENANTS = "tenants";
    public static final String ARG_MIGRATE_BLACKLIST_TENANTS = "blackListed";
    public static final String ARG_MIGRATE_ALL = "migrate";
    public static final String ARG_CLEANUP = "cleanup";
    public static final String ARG_MIGRATE_DB = "migrateDB";
    public static final String ARG_MIGRATE_REG = "migrateReg";
    public static final String ARG_MIGRATE_FILE_SYSTEM = "migrateFS";
    public static final String ARG_MIGRATE_STATS = "migrateStats";
    public static final String ARG_REMOVE_DECRYPTION_FAILED_CONSUMER_KEYS_FROM_DB = "removeDecryptionFailedKeysFromDB";

    // Synapse configuration related
    public static final String SYNAPSE_API_ROOT_ELEMENT = "api";
    public static final String SYNAPSE_API_ATTRIBUTE_CONTEXT = "context";
    public static final String SYNAPSE_API_ATTRIBUTE_VERSION = "version";
    public static final String SYNAPSE_API_ATTRIBUTE_VERSION_TYPE = "version-type";
    public static final String SYNAPSE_API_ATTRIBUTE_SOURCE = "source";
    public static final String SYNAPSE_API_ATTRIBUTE_NAME = "name";
    public static final String SYNAPSE_API_ATTRIBUTE_EXPRESSION = "expression";
    public static final String SYNAPSE_API_ATTRIBUTE_CLASS = "class";
    public static final String SYNAPSE_API_ATTRIBUTE_VALUE = "value";
    public static final String SYNAPSE_API_ATTRIBUTE_XMLNS = "xmlns";
    public static final String SYNAPSE_API_ELEMENT_PROPERTY = "property";
    public static final String SYNAPSE_API_ELEMENT_FILTER = "filter";
    public static final String SYNAPSE_API_ELEMENT_CLASS = "class";
    public static final String SYNAPSE_API_ELEMENT_INSEQUENCE = "inSequence";
    public static final String SYNAPSE_API_ELEMENT_OUTSEQUENCE = "outSequence";
    public static final String SYNAPSE_API_ELEMENT_HANDLERS = "handlers";
    public static final String SYNAPSE_API_ELEMENT_HANDLER = "handler";
    public static final String SYNAPSE_API_ELEMENT_THEN = "then";
    public static final String SYNAPSE_API_ELEMENT_SEND = "send";
    public static final String SYNAPSE_API_VALUE_BACKEND_REQUEST_TIME = "api.ut.backendRequestTime";
    public static final String SYNAPSE_API_VALUE_AM_KEY_TYPE = "$ctx:AM_KEY_TYPE";
    public static final String SYNAPSE_API_VALUE_EXPRESSION = "get-property('SYSTEM_TIME')";
    public static final String SYNAPSE_API_VALUE_RESPONSE_HANDLER = "org.wso2.carbon.apimgt.usage.publisher.APIMgtResponseHandler";
    public static final String SYNAPSE_API_VALUE_CORS_HANDLER = "org.wso2.carbon.apimgt.gateway.handlers.security.CORSRequestHandler";
    public static final String SYNAPSE_API_VALUE_THROTTLE_HANDLER = "org.wso2.carbon.apimgt.gateway.handlers.throttling.APIThrottleHandler";
    public static final String NEW_SYNAPSE_API_VALUE_THROTTLE_HANDLER = "org.wso2.carbon.apimgt.gateway.handlers.throttling.ThrottleHandler";
    public static final String SYNAPSE_API_VALUE_LATENCY_STATS_HANDLER = "org.wso2.carbon.apimgt.gateway.handlers.common.APIMgtLatencyStatsHandler";
    public static final String SYNAPSE_API_VALUE_INLINE = "inline";
    public static final String SYNAPSE_API_VALUE_INLINE_UPPERCASE = "INLINE";
    public static final String SYNAPSE_API_VALUE_ENPOINT = "ENDPOINT";
    public static final String SYNAPSE_API_VALUE_VERSION_TYPE_URL = "url";
    public static final String SYNAPSE_API_VALUE_VERSION_TYPE_CONTEXT = "context";
    public static final String SYNAPSE_API_XMLNS = "http://ws.apache.org/ns/synapse";
    public static final String SYNAPSE_API_VALUE_ID = "id";
    public static final String SYNAPSE_API_VALUE_A = "A";
    public static final String SYNAPSE_API_VALUE_RESOURCE_KEY = "policyKeyResource";
    public static final String SYNAPSE_API_VALUE_RESOURCE_KEY_VALUE = "gov:/apimgt/applicationdata/res-tiers.xml";
    public static final String SYNAPSE_API_VALUE_API_KEY = "policyKey";
    public static final String SYNAPSE_API_VALUE_API_KEY_VALUE = "gov:/apimgt/applicationdata/tiers.xml";
    public static final String SYNAPSE_API_VALUE_APP_KEY = "policyKeyApplication";
    public static final String SYNAPSE_API_VALUE_APP_KEY_VALUE = "gov:/apimgt/applicationdata/app-tiers.xml";


    //Swagger v2.0 constants
    public static final String SWAGGER_X_SCOPE = "x-scope";
    public static final String SWAGGER_X_AUTH_TYPE = "x-auth-type";
    public static final String SWAGGER_X_THROTTLING_TIER = "x-throttling-tier";
    public static final String SWAGGER_AUTH_TYPE = "auth_type";
    public static final String SWAGGER_THROTTLING_TIER = "throttling_tier";
    public static final String SWAGGER_X_MEDIATION_SCRIPT = "x-mediation-script";
    public static final String SWAGGER_X_WSO2_SECURITY = "x-wso2-security";
    public static final String SWAGGER_X_WSO2_SCOPES = "x-wso2-scopes";
    public static final String SWAGGER_SCOPE_KEY = "key";
    public static final String SWAGGER_NAME = "name";
    public static final String SWAGGER_DESCRIPTION = "description";
    public static final String SWAGGER_ROLES = "roles";
    public static final String SWAGGER_TITLE = "title";
    public static final String SWAGGER_EMAIL = "email";
    public static final String SWAGGER_URL = "url";
    public static final String SWAGGER_CONTACT = "contact";
    public static final String SWAGGER_LICENCE = "license";
    public static final String SWAGGER_LICENCE_URL = "licenseUrl";
    public static final String SWAGGER_VER = "version";
    public static final String SWAGGER_OBJECT_NAME_APIM = "apim";
    public static final String SWAGGER_PATHS = "paths";
    public static final String SWAGGER_RESPONSES = "responses";
    public static final String SWAGGER = "swagger";
    public static final String SWAGGER_V2 = "2.0";
    public static final String SWAGGER_INFO = "info";
    public static final String SWAGGER_REQUIRED_PARAM = "required";
    public static final String SWAGGER_HOST = "host";
    public static final String SWAGGER_BASE_PATH = "basePath";
    public static final String SWAGGER_SCHEMES = "schemes";
    public static final String SWAGGER_AUTHORIZATIONS = "authorizations";
    public static final String SWAGGER_SECURITY_DEFINITIONS = "securityDefinitions";
    public static final String SWAGGER_SCOPES = "scopes";
    public static final String SWAGGER_TERMS_OF_SERVICE = "termsOfService";
    public static final String SWAGGER_TERMS_OF_SERVICE_URL = "termsOfServiceUrl";
    public static final String SWAGGER_PARAM_TYPE_IN = "in";
    public static final String SWAGGER_OPERATION_ID = "operationId";
    public static final String SWAGGER_PARAMETERS = "parameters";
    public static final String SWAGGER_SUMMARY = "summary";
    public static final String SWAGGER_RESPONSE_200 = "200";
    public static final String SWAGGER_PARAM_TYPE = "type";
    public static final String SWAGGER_PARAM_TYPE_BODY = "body";
    public static final String SWAGGER_BODY_SCHEMA = "schema";
    public static final String SWAGGER_DEFINITIONS = "definitions";
    public static final String SWAGGER_REF = "$ref";
    public static final String SWAGGER_SAMPLE_DEFINITION = "sampleItem";
    public static final String SWAGGER_PRODUCES = "produces";



    // Work flow extensions
    public static final String WF_SUBSCRIPTION_DELETION_TAG = "SubscriptionDeletion";
    public static final String WF_EXECUTOR_ATTRIBUTE = "executor";
    public static final String WF_SUBSCRIPTION_DELETION_CLASS = "org.wso2.carbon.apimgt.impl.workflow.SubscriptionDeletionSimpleWorkflowExecutor";
    public static final String WF_SUBSCRIPTION_DELETION_TAG_COMMENT =
            "SubscriptionDeletion executor=\"org.wso2.carbon.apimgt.impl.workflow.SubscriptionDeletionSimpleWorkflowExecutor\">";
    public static final String WF_SUBSCRIPTION_SERVICE_ENDPOINT_COMMENT =
            "<Property name=\"serviceEndpoint\">http://localhost:9765/services/SubscriptionApprovalWorkFlowProcess/</Property>";
    public static final String WF_USERNAME_COMMENT = "<Property name=\"username\">admin</Property>";
    public static final String WF_PASSWORD_COMMENT = "<Property name=\"password\">admin</Property>";
    public static final String WF_CALLBACK_URL_COMMENT = "<Property name=\"callbackURL\">https://localhost:8243/services/WorkflowCallbackService</Property>";
    public static final String WF_SUBSCRIPTION_DELETION_CLOSING_TAG_COMMENT = "</SubscriptionDeletion";
    public static final String WF_APPLICATION_DELETION_TAG = "ApplicationDeletion";
    public static final String WF_APPLICATION_DELETION_CLASS = "org.wso2.carbon.apimgt.impl.workflow.ApplicationDeletionSimpleWorkflowExecutor";
    public static final String WF_APPLICATION_DELETION_TAG_COMMENT =
            "ApplicationDeletion executor=\"org.wso2.carbon.apimgt.impl.workflow.ApplicationDeletionSimpleWorkflowExecutor\">";
    public static final String WF_APPLICATION_SERVICE_ENDPOINT_COMMENT =
            "<Property name=\"serviceEndpoint\">http://localhost:9765/services/ApplicationApprovalWorkFlowProcess/</Property>";
    public static final String WF_APPLICATION_DELETION_CLOSING_TAG_COMMENT = "</ApplicationDeletion";
    public static final String WF_COMMENT_INDENT = "     ";

    // Tier xml
    public static final String TIER_THROTTLE_XMLNS = "http://www.wso2.org/products/wso2commons/throttle";
    public static final String TIER_THROTTLE_XMLNS_VAR = "throttle";
    public static final String TIER_WSP_XMLNS = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    public static final String TIER_WSP_XMLNS_VAR = "wsp";
    public static final String TIER_MEDIATOR_THROTTLE_ASSERTION_TAG = "MediatorThrottleAssertion";
    public static final String TIER_CONTROL_TAG = "Control";
    public static final String TIER_POLICY_TAG = "Policy";
    public static final String TIER_ATTRIBUTES_TAG = "Attributes";
    public static final String TIER_BILLING_PLAN_FREE = "FREE";
    public static final String TIER_STOP_ON_QUOTA_TRUE = "true";

    //database types
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_DB2 = "db2";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_MSSQL = "mssql";
    public static final String DB_TYPE_POSTGRE = "postgre";
    public static final String DB_TYPE_OPENEDGE = "openedge";

    // API Life Cycle
    public static final String LIFE_CYCLES_FOLDER = "lifecycles";
    public static final String API_LIFE_CYCLE_STATE_TAG = "state";
    public static final String API_LIFE_CYCLE_DATA_MODEL_TAG = "datamodel";
    public static final String API_LIFE_CYCLE_DATA_TAG = "data";
    public static final String API_LIFE_CYCLE_EXECUTORS_TAG = "transitionExecution";

    //default Swagger v2 response parameter
    public static final String DEFAULT_RESPONSE = "{ " +
            "\"200\": "
            + "{ " +
            "\"description\": \"No response was specified\"} "
            + "}";

    public static final String DEFAULT_SECURITY_SCHEME = "{" +
            "\"x-wso2-scopes\" : \"\", " +
            "\"type\" : \"\", " +
            "\"description\" : \"\", " +
            "\"name\" : \"\"," +
            " \"in\" : \"\", " +
            "\"flow\" : \"\", " +
            "\"authorizationUrl\" : \"\", " +
            "\"tokenUrl\" : \"\", " +
            "\"scopes\" : \"\"}";

    public static final String DEFAULT_INFO = "{" +
            "\"title\" : \"\", " +
            "\"version\" : \"\"" +
            "}";


    public static final String EXTERNAL_API_STORE = "ExternalAPIStore";
    public static final String ATTRIBUTE_CLASSNAME = "className";
    public static final String API_PUBLISHER_CLASSNAME = "org.wso2.carbon.apimgt.impl.publishers.WSO2APIPublisher";


    public static final String SWAGGER_CONSUMES = "consumes";
    public static final String DEFAULT_DATA_TYPE = "string";

	public static final String API_KEY_VALIDATOR_ENCRYPT_TOKENS =
            APIConstants.API_KEY_VALIDATOR + "EncryptPersistedTokens";

    public static final String SECURITY_DEFINITION__KEY = "securityDefinitions";
    public static final String SECURITY_DEFINITION_TYPE_KEY = "type";
    public static final String SECURITY_DEFINITION_AUTHORIZATION_URL_KEY = "authorizationUrl";
    public static final String SECURITY_DEFINITION_FLOW_KEY = "flow";
    public static final String SECURITY_DEFINITION_SCOPES_KEY = "scopes";
    public static final String SECURITY_DEFINITION_NAME_KEY = "sample_oauth";
    public static final String SECURITY_DEFINITION_TYPE_AUTH2 = "oauth2";
    public static final String SECURITY_DEFINITION_DEFAULT_GRANT_TYPES = "urn:ietf:params:oauth:grant-type:saml2-bearer,iwa:ntlm,implicit,refresh_token,client_credentials,authorization_code,password";
    public static final String REVOKE_URL_CONFIG_PATH = "OAuthConfigurations.RevokeAPIURL";

    public static final String SWAGGER_PATH_SECURITY_KEY = "security";
    public static final String SWAGGER_PATH_PARAMETERS_KEY = "parameters";

}
