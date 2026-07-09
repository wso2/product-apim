/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONTokener;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.Assert;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    
    private static final Log log = LogFactory.getLog(Utils.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern UNIQUE_PLACEHOLDER = Pattern.compile("\\$\\{UNIQUE:([^}]+)\\}");

    public static String getDCREndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_DCR_EP;
    }

    public static String getAPIMTokenEndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_APIM_TOKEN_EP;
    }

    public static String getRevokeEndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_APIM_REVOKE_EP;
    }

    public static String getUserInfoEndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_APIM_USERINFO_EP;
    }

    public static String getAPICreateEndpointURL(String baseUrl, String resourceType) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType;
    }

    public static String getResourceEndpointURL(String baseUrl, String resourceType, String resourceId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId;
    }

    /** Publisher API endpoints sub-resource collection: {@code /apis/{apiId}/endpoints} (POST add, GET list). */
    public static String getApiEndpointsURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/endpoints";
    }

    /** Publisher GraphQL per-field complexity config: {@code /apis/{apiId}/graphql-policies/complexity} (GET/PUT). */
    public static String getGraphQLComplexityURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/graphql-policies/complexity";
    }

    /** Publisher API client-certificates (mutual SSL) collection: {@code /apis/{apiId}/client-certificates}. */
    public static String getClientCertificatesURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/client-certificates";
    }

    /** Publisher single API endpoint: {@code /apis/{apiId}/endpoints/{endpointId}} (GET, PUT, DELETE). */
    public static String getApiEndpointByIdURL(String baseUrl, String apiId, String endpointId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/endpoints/" + endpointId;
    }

    public static String getRevisionURL(String baseUrl, String resourceType, String resourceId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId + "/revisions";
    }

    public static String getRevisionDeploymentURL(String baseUrl, String resourceType, String resourceId, String revisionId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId + "/deploy-revision?revisionId=" + revisionId;
    }

    public static String getRevisionUnDeploymentURL(String baseUrl, String resourceType, String apiId, String revisionId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + apiId + "/undeploy-revision?revisionId=" + revisionId;
    }

    public static String getRevisionRestoreURL(String baseUrl, String resourceType, String apiId, String revisionId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + apiId + "/restore-revision?revisionId=" + revisionId;
    }

    public static String getRevisionByID(String baseUrl, String resourceType, String apiId, String revisionID) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + apiId + "/revisions/" + revisionID;
    }

    public static String getAPISearchEndpointURL(String baseUrl, String query, Integer limit, Integer offset) {

        StringBuilder urlBuilder = new StringBuilder(baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/");

        List<String> params = new ArrayList<>();
        if (query != null && !query.isEmpty()) {
            params.add("query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        }
        if (limit != null) {
            params.add("limit=" + limit);
        }
        if (offset != null) {
            params.add("offset=" + offset);
        }

        if (!params.isEmpty()) {
            urlBuilder.append("?").append(String.join("&", params));
        }
        return urlBuilder.toString();
    }

    /**
     * Extracts the API UUID from a Publisher search/list response when exactly one API is present.
     *
     * @param jsonPayload the JSON response payload
     * @return the API UUID if count == 1, otherwise null
     * @throws IOException if the payload cannot be parsed
     */
    public static String extractAPIUUID(String jsonPayload) throws IOException {

        JsonNode root = mapper.readTree(jsonPayload);

        JsonNode countNode = root.path("count");
        if (countNode.isInt() && countNode.asInt() == 1) {
            JsonNode list = root.path("list");
            if (list.isArray() && !list.isEmpty()) {
                JsonNode apiObj = list.get(0);
                return apiObj.path("id").asText(null);
            }
        }
        return null;
    }

    public static String getChangeLifecycleURL(String baseUrl, String resourceType, String apiId, String action,
                                               String lifecycleChecklist) {

        if (StringUtils.isBlank(apiId) || StringUtils.isBlank(action)) {
            throw new IllegalArgumentException("ID and Action must be provided.");
        }

        String idParam;
        if ("apis".equals(resourceType)) {
            idParam = "apiId";
        } else if ("mcp-servers".equals(resourceType)) {
            idParam = "mcpServerId";
        } else {
            idParam = "apiProductId";
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl)
                .append(Constants.DEFAULT_APIM_API_DEPLOYER)
                .append(resourceType)
                .append("/change-lifecycle?")
                .append(idParam).append("=")
                .append(URLEncoder.encode(apiId, StandardCharsets.UTF_8))
                .append("&action=")
                .append(URLEncoder.encode(action, StandardCharsets.UTF_8));

        // Append the lifecycleChecklist if provided
        if (lifecycleChecklist != null && !lifecycleChecklist.trim().isEmpty()) {
            String encodedChecklist = URLEncoder.encode(lifecycleChecklist, StandardCharsets.UTF_8);
            urlBuilder.append("&lifecycleChecklist=").append(encodedChecklist);
        }
        log.info("Change Lifecycle URL: " + urlBuilder);
        return urlBuilder.toString();
    }

    public static String getCreateSubscriptionURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "subscriptions";
    }

    public static String getSubscriptionURL(String baseUrl, String subscriptionId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "subscriptions/" + subscriptionId;
    }

    public static String getAllSubscriptionsURL(String baseUrl, String apiId, String applicationId, String groupId,
                                                Integer offset, Integer limit) {

        if (StringUtils.isBlank(apiId) && StringUtils.isBlank(applicationId)) {
            throw new IllegalArgumentException("Either apiId or applicationId must be provided.");
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl + Constants.DEFAULT_DEVPORTAL + "subscriptions");

        List<String> params = new ArrayList<>();
        if (StringUtils.isNotBlank(apiId)) {
            params.add("apiId=" + URLEncoder.encode(apiId, StandardCharsets.UTF_8));
        }
        if (StringUtils.isNotBlank(applicationId)) {
            params.add("applicationId=" + URLEncoder.encode(applicationId, StandardCharsets.UTF_8));
        }
        if (StringUtils.isNotBlank(groupId)) {
            params.add("groupId=" + URLEncoder.encode(groupId, StandardCharsets.UTF_8));
        }
        if (offset != null) {
            params.add("offset=" + offset);
        }
        if (limit != null) {
            params.add("limit=" + limit);
        }
        urlBuilder.append("?").append(String.join("&", params));
        return urlBuilder.toString();
    }

    /** Admin — list applications owned by a given user (owner search). */
    public static String getAdminApplicationsByOwnerURL(String baseUrl, String owner) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "applications?user="
                + URLEncoder.encode(owner, StandardCharsets.UTF_8);
    }

    /** Publisher — force-change a subscription's business plan (validates the plan; POST, query params). */
    public static String getChangeSubscriptionBusinessPlanURL(String baseUrl, String subscriptionId, String plan) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "subscriptions/change-business-plan?subscriptionId="
                + URLEncoder.encode(subscriptionId, StandardCharsets.UTF_8) + "&throttlingPolicy="
                + URLEncoder.encode(plan, StandardCharsets.UTF_8);
    }

    /** Admin — search applications by name (the admin /applications endpoint's {@code name} query param). */
    public static String getAdminApplicationsByNameURL(String baseUrl, String name) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "applications?name="
                + URLEncoder.encode(name, StandardCharsets.UTF_8);
    }

    /** Publisher — upload (POST, multipart) / download (GET) an API's thumbnail image. */
    public static String getThumbnailURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/thumbnail";
    }

    /** Publisher — import an API from an OpenAPI definition or archive (POST, multipart {@code file}). */
    public static String getImportOpenAPIURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/import-openapi";
    }

    /** Publisher — validate a system role by name. The path segment is URL-safe base64 WITHOUT padding:
     *  a literal '=' pad in the path breaks the auth-filter's resource match (→ 401). */
    public static String getValidateRoleURL(String baseUrl, String role) {
        String encoded = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(role.getBytes(StandardCharsets.UTF_8));
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "roles/" + encoded;
    }

    /** Publisher — generate the inline mock implementation script for an API (POST). */
    public static String getGenerateMockScriptsURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/generate-mock-scripts";
    }

    /** Publisher — retrieve the generated inline mock implementation script for an API (GET). */
    public static String getGeneratedMockScriptsURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/generated-mock-scripts";
    }

    public static String getAPILifecycleStateURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/lifecycle-state";
    }

    public static String getApplicationCreateURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications";
    }

    public static String getApplicationSearchURL(String baseUrl, String applicationName) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications?query=" + URLEncoder.encode(applicationName, StandardCharsets.UTF_8);
    }

    public static String getApiSearchURL(String baseUrl, String query) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    public static String getDevportalApiDetailURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId;
    }

    public static String getApiDocumentsURL(String baseUrl, String resourceId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + resourceId + "/documents";
    }

    public static String getApplicationEndpointURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId;
    }

    public static String getGenerateApplicationKeysURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/generate-keys";
    }

    /** DevPortal — regenerate (rotate) the consumer secret of an application's key mapping (by key-mapping id). */
    public static String getRegenerateConsumerSecretURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/"
                + keyMappingId + "/regenerate-secret";
    }

    public static String getApplicationAllKeys(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys";
    }

    /** DevPortal — map a pre-existing (BYO) OAuth client's consumer key/secret to an application. */
    public static String getMapKeysURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/map-keys";
    }

    /** DevPortal — clean up an application's key registration for a key mapping (after a failed/partial key-gen). */
    public static String getCleanupRegistrationURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/"
                + keyMappingId + "/clean-up";
    }

    public static String getGenerateApplicationSecretURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/" +
                keyMappingId + "/generate-secret";
    }

    public static String getAllApplicationSecretsURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/" +
                keyMappingId + "/secrets";
    }

    public static String getRevokeApplicationSecretURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/" +
                keyMappingId + "/revoke-secret";
    }

    public static String getGenerateApplicationTokenURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/" +
                keyMappingId + "/generate-token";
    }

    public static String getGenerateAPIKeyURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/api-keys/PRODUCTION/generate";
    }

    /** DevPortal — revoke an application API key ({@code applications/{id}/api-keys/PRODUCTION/revoke}). */
    public static String getRevokeAPIKeyURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/api-keys/PRODUCTION/revoke";
    }

    /** DevPortal — list an application's API keys ({@code applications/{id}/api-keys/PRODUCTION}); carries keyUUID. */
    public static String getListAPIKeysURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/api-keys/PRODUCTION";
    }

    public static String getUpdateKey(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/" + keyMappingId;
    }

    public static String getAPIInvocationURL(String baseGatewayUrl, String resourcePath, String tenantDomain) {
        return Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? baseGatewayUrl + resourcePath
                : baseGatewayUrl + "/t/" + tenantDomain + "/" + resourcePath;
    }

    public static String getAPIArtifactDeployedInGatewayURL(String baseUrl, String apiName, String version,
                                                            String tenantDomain) {
        return baseUrl + Constants.GATEWAY + "api-artifact?apiName=" + URLEncoder.encode(apiName,
                StandardCharsets.UTF_8) + "&version=" + URLEncoder.encode(version, StandardCharsets.UTF_8) +
                "&tenantDomain=" + URLEncoder.encode(tenantDomain, StandardCharsets.UTF_8);
    }

    public static String getGatewayHealthCheckURL(String baseUrl) {
        return baseUrl + Constants.GATEWAY + "server-startup-healthcheck";
    }

    public static String getRevisionDeployments(String baseUrl, String resourceType, String resourceId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId + "/revisions?query=deployed:true";
    }

    public static String getTenantMgtAdminServiceURL(String baseUrl) {
        return baseUrl + "services/TenantMgtAdminService";
    }

    public static String getMultipleCredentialsUserAdminServiceURL(String baseUrl) {
        return baseUrl + "services/UserAdmin";
    }

    public static String getRemoteUserStoreManagerServiceURL(String baseUrl) {
        return baseUrl + "services/RemoteUserStoreManagerService";
    }

    public static String getNewAPIVersionURL(String baseUrl, String resourceType, String newVersion, Boolean defaultVersion, String apiId) {

        String endpointPath;
        String idParameterName;

        if ("api-products".equals(resourceType)) {
            endpointPath = "/copy-api-products";
            idParameterName = "apiProductId";
        } else {
            // Default to APIs
            endpointPath = "/copy-api";
            idParameterName = "apiId";
        }

        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + endpointPath +
                "?newVersion=" + URLEncoder.encode(newVersion, StandardCharsets.UTF_8) +
                "&defaultVersion=" + defaultVersion +
                "&" + idParameterName + "=" + URLEncoder.encode(apiId, StandardCharsets.UTF_8);
    }

    public static String getAPIDocuments(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/documents";
    }

    public static String getAPIDocument(String baseUrl, String apiId, String documentId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/documents/" + documentId;
    }

    public static String getAPIDocumentContent(String baseUrl, String apiId, String documentId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/documents/" + documentId + "/content";
    }

    public static String getSubscriptionBlockingURL(String baseUrl, String subscriptionID) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "subscriptions/block-subscription?subscriptionId=" + subscriptionID + "&blockState=BLOCKED";
    }

    public static String getSubscriptionUnBlockingURL(String baseUrl, String subscriptionID) {
        // unblock-subscription takes only subscriptionId (per publisher-api.yaml); blockState is a
        // block-subscription param and is ignored here, so it is intentionally omitted.
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "subscriptions/unblock-subscription?subscriptionId=" + subscriptionID;
    }

    public static String getSubscriptions(String baseUrl, String resourceID) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "subscriptions?apiId=" + resourceID;
    }

    public static String getAPIScopes(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "scopes";
    }

    public static String getAPIScopesById(String baseUrl, String scopeId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "scopes/" + scopeId;
    }

    /** Publisher — retrieve (GET) or update (PUT multipart {@code schemaDefinition}) a GraphQL API's schema. */
    public static String getGraphQLSchemaOfApiURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/graphql-schema";
    }

    /** Publisher — validate a GraphQL schema (POST multipart {@code file}). */
    public static String getValidateGraphQLSchemaURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/validate-graphql-schema";
    }

    public static String getGraphQLSchema(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/import-graphql-schema";
    }

    public static String getAPIProvider(String baseUrl, String apiId, String providerName) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "apis/" + apiId + "/change-provider?provider=" + providerName;
    }

    public static String getProductSearchEndpointURL(String baseUrl, String productName) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "api-products?query=" + productName;
    }

    public static String getCommonPolicy(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "operation-policies";
    }

    public static String getAPISpecificPolicy(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/operation-policies";
    }

    public static String getAPISpecificPolicyById(String baseUrl, String apiId, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/operation-policies/" + policyId;
    }

    public static String getGlobalPolicy(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "gateway-policies";
    }

    public static String getGlobalPolicyDeploy(String baseUrl, String policyMappingId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "gateway-policies/" + policyMappingId + "/deploy";
    }

    /** Publisher REST API — create an MCP server by proxying a third-party MCP server (POST, JSON {@code url}). */
    public static String getMCPServerProxyURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "mcp-servers/generate-from-mcp-server";
    }

    /** Publisher REST API — create an MCP server from an OpenAPI definition (POST, multipart file). */
    public static String getMCPServerFromOpenAPIURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "mcp-servers/generate-from-openapi";
    }

    /** Publisher REST API — create an MCP server from an existing API (POST, JSON MCPServer body). */
    public static String getMCPServerFromAPIURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "mcp-servers/generate-from-api";
    }

    /** Publisher REST API — a single MCP server by id (get/delete). */
    public static String getMCPServerByIdURL(String baseUrl, String mcpServerId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "mcp-servers/" + mcpServerId;
    }

    /** Publisher REST API — an MCP server's backend endpoints collection {@code /mcp-servers/{id}/backends} (GET). */
    public static String getMCPServerBackendsURL(String baseUrl, String mcpServerId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "mcp-servers/" + mcpServerId + "/backends";
    }

    /** Publisher REST API — a single MCP server backend endpoint by id (GET, PUT). */
    public static String getMCPServerBackendByIdURL(String baseUrl, String mcpServerId, String backendId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "mcp-servers/" + mcpServerId + "/backends/" + backendId;
    }

    /** Admin REST API — AI service providers (list/create). */
    public static String getAIServiceProvidersURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "ai-service-providers";
    }

    /** Admin REST API — a single AI service provider by id (get/update/delete). */
    public static String getAIServiceProviderByIdURL(String baseUrl, String providerId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "ai-service-providers/" + providerId;
    }

    /** Publisher REST API — an AI service provider's model list ({@code /ai-service-providers/{id}/models}). */
    public static String getAIServiceProviderModelsURL(String baseUrl, String providerId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "ai-service-providers/" + providerId + "/models";
    }

    /** Admin REST API — application throttling policies (create/list). */
    public static String getApplicationThrottlingPoliciesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/application";
    }

    /** Admin REST API — a single application throttling policy by id (get/update/delete). */
    public static String getApplicationThrottlingPolicyByIdURL(String baseUrl, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/application/" + policyId;
    }

    /** Admin REST API — subscription throttling policies (create/list). */
    public static String getSubscriptionThrottlingPoliciesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/subscription";
    }

    /** Admin REST API — a single subscription throttling policy by id (get/update/delete). */
    public static String getSubscriptionThrottlingPolicyByIdURL(String baseUrl, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/subscription/" + policyId;
    }

    /** Admin REST API — advanced (API-level) throttling policies (create/list). */
    public static String getAdvancedThrottlingPoliciesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/advanced";
    }

    /** Admin REST API — a single advanced throttling policy by id (get/update/delete). */
    public static String getAdvancedThrottlingPolicyByIdURL(String baseUrl, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/advanced/" + policyId;
    }

    /** Admin REST API — throttling policies of a given type (create/list): {@code application|subscription|advanced|custom}. */
    public static String getThrottlingPoliciesByTypeURL(String baseUrl, String policyType) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/" + policyType;
    }

    /** Admin REST API — a single throttling policy of a given type by id (get/update/delete). */
    public static String getThrottlingPolicyByTypeURL(String baseUrl, String policyType, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/" + policyType + "/" + policyId;
    }

    /** Admin REST API — gateway environments (create/list). */
    public static String getEnvironmentsURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "environments";
    }

    /** Admin REST API — a single gateway environment by id (get/update/delete). */
    public static String getEnvironmentByIdURL(String baseUrl, String environmentId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "environments/" + environmentId;
    }

    /** Admin REST API — the gateway instances of an environment. */
    public static String getEnvironmentGatewaysURL(String baseUrl, String environmentId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "environments/" + environmentId + "/gateways";
    }

    /** Admin REST API — organizations collection (list/create). */
    public static String getOrganizationsURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "organizations";
    }

    /** Admin REST API — a single organization by id (get/update/delete). */
    public static String getOrganizationByIdURL(String baseUrl, String organizationId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "organizations/" + organizationId;
    }

    /** SOAP admin service — claim-metadata management (register local claims). */
    public static String getClaimMetadataMgtServiceURL(String baseUrl) {
        return baseUrl + "services/ClaimMetadataManagementService";
    }

    /** DevPortal REST API — key managers visible to the calling user's organization. */
    public static String getDevportalKeyManagersURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "key-managers";
    }

    /** Admin REST API — tenant configuration (get/update). */
    public static String getTenantConfigURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "tenant-config";
    }

    /** Admin REST API — tenant configuration JSON schema (get). */
    public static String getTenantConfigSchemaURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "tenant-config-schema";
    }

    /** Admin REST API — system-scope role-alias mappings (get/put). */
    public static String getRoleAliasesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "system-scopes/role-aliases";
    }

    /** Admin REST API — export a throttling policy by name + type ({@code sub}/{@code app}/{@code api}/{@code global}). */
    public static String getThrottlePolicyExportURL(String baseUrl, String name, String type) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/export?name="
                + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8) + "&type=" + type;
    }

    /** Admin REST API — import a throttling policy (multipart file); {@code overwrite} controls update vs conflict. */
    public static String getThrottlePolicyImportURL(String baseUrl, String overwrite) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/import?overwrite=" + overwrite;
    }

    /** Admin REST API — deny (blocking-condition) policies collection (list/create). */
    public static String getDenyPoliciesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/deny-policies";
    }

    /** Admin REST API — a single deny policy by condition id (get/update-status/delete). NOTE: singular path. */
    public static String getDenyPolicyByIdURL(String baseUrl, String conditionId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/deny-policy/" + conditionId;
    }

    /** Admin REST API — key managers collection (list/create). */
    public static String getKeyManagersURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "key-managers";
    }

    /** Admin REST API — a single key manager by id (get/update/delete). */
    public static String getKeyManagerByIdURL(String baseUrl, String keyManagerId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "key-managers/" + keyManagerId;
    }

    /** Governance REST API — rulesets collection (list/create). */
    public static String getGovernanceRulesetsURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_GOVERNANCE + "rulesets";
    }

    /** Governance REST API — a single ruleset by id (get/update/delete). */
    public static String getGovernanceRulesetByIdURL(String baseUrl, String rulesetId) {
        return baseUrl + Constants.DEFAULT_APIM_GOVERNANCE + "rulesets/" + rulesetId;
    }

    /** Governance REST API — the raw ruleset content by id. */
    public static String getGovernanceRulesetContentURL(String baseUrl, String rulesetId) {
        return baseUrl + Constants.DEFAULT_APIM_GOVERNANCE + "rulesets/" + rulesetId + "/content";
    }

    /** Governance REST API — policies collection (list/create). */
    public static String getGovernancePoliciesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_GOVERNANCE + "policies";
    }

    /** Governance REST API — a single policy by id (get/update/delete). */
    public static String getGovernancePolicyByIdURL(String baseUrl, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_GOVERNANCE + "policies/" + policyId;
    }

    /** Governance REST API — artifact compliance for an API by id. */
    public static String getGovernanceApiComplianceURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_GOVERNANCE + "artifact-compliance/api/" + apiId;
    }

    /** Admin REST API — custom (Siddhi) throttling rules (create/list). */
    public static String getCustomThrottlingPoliciesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/custom";
    }

    /** Admin REST API — a single custom throttling rule by id (get/update/delete). */
    public static String getCustomThrottlingPolicyByIdURL(String baseUrl, String policyId) {
        return baseUrl + Constants.DEFAULT_APIM_ADMIN + "throttling/policies/custom/" + policyId;
    }

    public static String getSwaggerURL(String baseUrl, String resourceType, String resourceId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId + "/swagger";
    }

    public static String getAPIDefinitionURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/import-openapi";
    }

    /** Publisher REST API — OpenAPI definition validation (multipart file / url). */
    public static String getValidateOpenAPIURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/validate-openapi";
    }

    /** Publisher REST API — linter custom rules. */
    public static String getLinterCustomRulesURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "linter-custom-rules";
    }

    /** Publisher REST API — available throttling policies for a policy level (subscription / api / application). */
    public static String getPublisherThrottlingPoliciesURL(String baseUrl, String policyLevel) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "throttling-policies/" + policyLevel;
    }


    public static String getInternalAPIKey(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/generate-key";
    }

    /** Publisher REST API — an API Product's swagger/OpenAPI definition. */
    public static String getAPIProductSwaggerURL(String baseUrl, String apiProductId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "api-products/" + apiProductId + "/swagger";
    }

    /** Publisher REST API — create a new version of an API Product (copy). */
    public static String getAPIProductNewVersionURL(String baseUrl, String newVersion, boolean defaultVersion,
                                                    String apiProductId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "api-products/copy-api-products?newVersion="
                + newVersion + "&defaultVersion=" + defaultVersion + "&apiProductId=" + apiProductId;
    }

    // --- API-bound API Key endpoints (devportal) ---

    public static String getAPIBoundApiKeyGenerateURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId + "/api-keys/generate";
    }

    public static String getAPIBoundApiKeyRegenerateURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId + "/api-keys/regenerate";
    }

    public static String getAPIBoundApiKeysListURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId + "/api-keys";
    }

    public static String getAPIBoundApiKeyRevokeURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId + "/api-keys/revoke";
    }

    public static String getAPIBoundApiKeyAssociateURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId + "/api-keys/associate";
    }

    public static String getAPIBoundApiKeyDissociateURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "apis/" + apiId + "/api-keys/dissociate";
    }

    public static String getAppApiKeyAssociateURL(String baseUrl, String appId, String keyType) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + appId + "/api-keys/" + keyType + "/associate";
    }

    public static String getAppApiKeyDissociateURL(String baseUrl, String appId, String keyType) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + appId + "/api-keys/" + keyType + "/dissociate";
    }

    public static String getAppApiKeyAssociationsURL(String baseUrl, String appId, String keyType) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + appId + "/api-keys/" + keyType + "/associations";
    }

    // --- Legacy (application-level) API Key endpoints ---

    public static String getLegacyApiKeysListURL(String baseUrl, String applicationId, String keyType) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/api-keys/" + keyType;
    }

    public static String getLegacyApiKeyRegenerateURL(String baseUrl, String applicationId, String keyType) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/api-keys/" + keyType + "/regenerate";
    }

    /**
     * Extracts a value from a JSON payload using a JSONPath expression.
     * Sample JSONPath expressions:
     *  - JSON Object: "id" or "$.id" or "$.operations[0].verb"
     *  - JSON Array: "[0].id" or "$[0].id"
     *
     * @param jsonPayload the JSON string to parse
     * @param path        the JSONPath
     * @return the extracted value as an Object
     * @throws IOException if the payload is invalid or the path is not found
     */
    public static Object extractValueFromPayload(String jsonPayload, String path) throws IOException {

        if (StringUtils.isBlank(jsonPayload)) {
            throw new IOException("JSON payload is null or empty.");
        }

        String trimmedPath = path.trim();
        String jsonPath = normalizeJsonPath(trimmedPath);

        try {
            return JsonPath.read(jsonPayload, jsonPath);
        } catch (Exception e) {
            throw new IOException("Path '" + jsonPath + "' not found or invalid in JSON payload.", e);
        }
    }

    /**
     * Finds an entry by {@code name} in a paginated list payload ({@code {"list":[{"id","name",...}]}}) and
     * returns its {@code id}, or {@code null} if no entry matches. Used to reference a named resource (e.g. a
     * built-in governance ruleset) whose id is not known ahead of time.
     */
    public static String extractIdByName(String jsonPayload, String name) throws IOException {

        if (StringUtils.isBlank(jsonPayload)) {
            throw new IOException("JSON payload is null or empty.");
        }
        try {
            java.util.List<Object> ids = JsonPath.read(jsonPayload,
                    "$.list[?(@.name == '" + name + "')].id");
            return ids.isEmpty() ? null : String.valueOf(ids.get(0));
        } catch (Exception e) {
            throw new IOException("Failed to locate id for name '" + name + "' in list payload.", e);
        }
    }

    /**
     * Normalizes shorthand paths into valid JSONPath expressions based on payload structure.
     * - If starts with "$", returns as-is (e.g., "$" -> "$").
     * - If starts with "[", prepends "$" (e.g., "[0].key" -> "$[0].key").
     * - Otherwise, prepends "$." (e.g., "list[0]" -> "$.list[0]").
     *
     * @param path        The shorthand path provided in the Gherkin step.
     * @return A valid, absolute JSONPath starting with "$".
     */
    private static String normalizeJsonPath(String path) {
        String trimmedPath = path.trim();

        // If path already starts with "$", return as-is
        if (trimmedPath.startsWith(Constants.JSON_PATH_ROOT)) {
            return trimmedPath;
        }

        // JSON Arrays: If it starts with "[", prepend "$"
        if (trimmedPath.startsWith(Constants.JSON_ARRAY_START_TOKEN)) {
            return Constants.JSON_PATH_ROOT + trimmedPath;
        }

        // JSON Objects: Otherwise, prepend "$."
        return Constants.JSON_PATH_ROOT_WITH_DOT + trimmedPath;
    }

    /**
     * Resolves a value from the {@link TestContext} using the given key.
     *
     * @param key the key
     * @return the resolved value
     * @throws IllegalArgumentException if the key is not found
     */
    public static Object resolveFromContext(String key) {

        String lookupKey = (key.startsWith("<") && key.endsWith(">")) ? key.substring(1, key.length() - 1) : key;

        Object value = TestContext.get(lookupKey);
        if (value == null) {
            throw new IllegalArgumentException("No value found in context for key: " + lookupKey);
        }
        return value;
    }

    /**
     * Resolves the value from context only if the input is a context key (e.g., "<sampleKey>").
     * If the input is not wrapped in brackets, it returns the input string as-is.
     *
     * @param input the string to check and potentially resolve
     * @return the resolved context value or the original input string
     */
    public static Object resolveIfContextKey(String input) {
        if (input != null && input.startsWith("<") && input.endsWith(">")) {
            return resolveFromContext(input);
        }
        return input;
    }

    /**
     * Normalizes a context key by removing surrounding angle brackets (< >), if present.
     *
     * @param key the raw key string from the step definition
     * @return the cleaned key without angle brackets
     */
    public static String normalizeContextKey(String key) {
        if (key != null && key.startsWith("<") && key.endsWith(">")) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    /**
     * Replaces name placeholders of the form {@code ${UNIQUE:<base>}} in a JSON payload with a
     * runner-unique value generated by {@link Names#unique(String)}. This lets payload files keep
     * stable base names while every runner instance gets distinct resource names/contexts.
     *
     * @param payload the raw payload string, potentially containing placeholders
     * @return the payload with placeholders substituted
     */
    public static String resolvePayloadPlaceholders(String payload) {

        if (payload == null || !payload.contains("${UNIQUE:")) {
            return payload;
        }
        Matcher matcher = UNIQUE_PLACEHOLDER.matcher(payload);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Names.unique(matcher.group(1)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Merges two TOML files. Values from the changesTomlPath will override those in the baseTomlPath.
     *
     * @param baseTomlPath    path to the base TOML file
     * @param changesTomlPath path to the TOML file with changes
     * @return merged TOML content as a String
     * @throws IOException if reading files fails
     */
    public static String mergeToml(String baseTomlPath, String changesTomlPath) throws IOException {
        TomlMapper tomlMapper = new TomlMapper();

        // Read TOMLs into JsonNode trees
        ObjectNode baseNode = (ObjectNode) tomlMapper.readTree(new File(baseTomlPath));
        ObjectNode changesNode = (ObjectNode) tomlMapper.readTree(new File(changesTomlPath));

        deepMerge(baseNode, changesNode);
        return tomlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseNode);
    }

    /**
     * Merges a sequence of TOML change-overlays onto a base TOML, applying each in order (later overlays
     * override earlier ones). Lets a block layer a small feature-specific overlay on top of the shared
     * {@code basic} overlay without copying the whole distribution config into a full-file replacement.
     *
     * @param baseTomlPath     path to the base TOML (the product distribution config)
     * @param changesTomlPaths ordered paths to change-overlays, each deep-merged onto the accumulated result
     * @return the merged TOML as a string
     */
    public static String mergeTomls(String baseTomlPath, List<String> changesTomlPaths) throws IOException {
        TomlMapper tomlMapper = new TomlMapper();
        ObjectNode baseNode = (ObjectNode) tomlMapper.readTree(new File(baseTomlPath));
        for (String changesTomlPath : changesTomlPaths) {
            ObjectNode changesNode = (ObjectNode) tomlMapper.readTree(new File(changesTomlPath));
            deepMerge(baseNode, changesNode);
        }
        return tomlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseNode);
    }

    /**
     * Produces the default-lane {@code deployment.toml} exactly the way the parallel block lane does: the shared
     * {@code basic} overlay merged onto the product distribution config. Anything that constructs a
     * {@code DynamicApimContainer} directly (e.g. framework-verification tests) MUST use this rather than read
     * {@code basic/deployment.toml} verbatim — {@code basic} is an overlay, not a complete server config, so
     * passing it as-is boots an under-configured server that exits during startup.
     *
     * @param moduleDir the integration-v2 cucumber-tests module directory
     * @return the merged (distribution + basic) deployment.toml content
     */
    public static String resolveDefaultToml(String moduleDir) throws IOException {
        String basePath = java.nio.file.Paths.get(moduleDir, Constants.DISTRIBUTION_TOML_PATH).normalize().toString();
        String overlayPath = java.nio.file.Paths.get(moduleDir, Constants.DEFAULT_TOML_PATH).normalize().toString();
        return mergeToml(basePath, overlayPath);
    }

    /**
     * Recursively merges two ObjectNode instances. Values from changesNode override those in baseNode or
     * add if not present.
     *
     * @param baseNode    the base ObjectNode to be modified
     * @param changesNode the ObjectNode with changes to apply
     */
    private static void deepMerge(ObjectNode baseNode, ObjectNode changesNode) {

        changesNode.fieldNames().forEachRemaining(field -> {
            JsonNode changeVal = changesNode.get(field);
            JsonNode baseVal = baseNode.get(field);

            // If both values are objects, merge recursively
            if (baseVal != null && baseVal.isObject() && changeVal.isObject()) {
                deepMerge((ObjectNode) baseVal, (ObjectNode) changeVal);
            } else {
                baseNode.set(field, changeVal);
            }
        });
    }

    /**
     * Evaluates the given XPath on the SOAP/XML string and returns the text of all
     * matched elements.
     *
     * @param soapXml        XML content
     * @param axiomXPathExpr XPath expression selecting elements
     * @return list of text values from matched elements
     * @throws JaxenException if the XPath is invalid or evaluation fails
     */
    public static List<String> getNodeTextsByXPath(String soapXml, String axiomXPathExpr) throws JaxenException {
        OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(new StringReader(soapXml));
        OMElement root = builder.getDocumentElement();
        try {
            AXIOMXPath xp = new AXIOMXPath(axiomXPathExpr);
            @SuppressWarnings("unchecked")
            List<Object> nodes = xp.selectNodes(root);
            List<String> out = new ArrayList<>(nodes.size());
            for (Object n : nodes) out.add(((OMElement) n).getText());
            return out;
        } finally {
            builder.close();
        }
    }

    /**
     * Retrieves a Tenant object from the TestContext using the specified key.
     * If the key is not found or the value is not a Tenant, the method fails the test.
     *
     * @param contextKey the key to look up in the TestContext
     * @return the Tenant object associated with the key
     */
    public static Tenant getTenantFromContext(String contextKey) {
        Object value = TestContext.get(contextKey);
        if (value == null) {
            Assert.fail("Tenant not found in context: " + contextKey);
        }
        if (!(value instanceof Tenant)) {
            Assert.fail("Context value for '" + contextKey + "' is not a Tenant: " + value.getClass().getName());
        }
        return (Tenant) value;
    }

    /**
     * Parses a string as a JSON value.
     *
     * @param value the JSON value as a string
     * @return the parsed value as a JSONObject, JSONArray, String,
     * Number, Boolean, or JSONObject.NULL
     * @throws IllegalArgumentException if the input is null, blank, or invalid JSON
     */
    public static Object parseConfigValue(String value) {

        value = value.trim();
        try {
            return new JSONTokener(value.trim()).nextValue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON value to append: " + value, e);
        }
    }

    /**
     * Reads a JSON file from the classpath and returns the value for the given key.
     *
     * @param filePath path to the JSON file in the classpath
     * @param key      key to look up in the JSON content
     * @return value associated with the key
     * @throws Exception if the file is missing, unreadable, or the key is not found
     */
    public static Object getValueFromFileByKey(String filePath, String key) throws Exception {
        InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new FileNotFoundException("File not found on classpath: " + filePath);
        }

        String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> valuesMap = objectMapper.readValue(content, new TypeReference<>() {});

        if (!valuesMap.containsKey(key)) {
            throw new IllegalStateException(
                    "No value found for key: " + key + ". Available keys: " + valuesMap.keySet()
            );
        }

        return valuesMap.get(key);
    }

    /**
     * Builds the default lookup key using database type, tenant domain, and username.
     *
     * @return composite key in the format dbType|tenantDomain|username
     */
    public static String buildUserScopedKey(Tenant tenant, User currentuser) {
        String dbType = System.getenv(Constants.API_MANAGER_DATABASE_TYPE);
        if (dbType == null) {
            throw new IllegalStateException("DB type is not set in environment variables");
        }

        String tenantDomain = tenant.getDomain();
        String username = currentuser.getUserNameWithoutDomain();

        if (tenantDomain == null || username == null) {
            throw new IllegalStateException("Tenant domain or username not found in TestContext. " +
                            "tenantDomain=" + tenantDomain + ", username=" + username);
        }
        // Create the composite key in the format dbType|tenantDomain|username
        return String.join(Constants.COMPOSITE_KEY_DELIMITER, dbType, tenantDomain, username);
    }

    /**
     * Replaces context placeholders (for example, {{subscriptionId}}) in the given input
     * string with the corresponding values stored in {@link TestContext}.
     *
     * @param input the input string
     * @return the input string with all placeholders resolved using values from {@link TestContext}
     */
    public static String resolveContextPlaceholders(String input) {
        if (input == null) {
            return null;
        }

        Matcher matcher = Constants.CONTEXT_PLACEHOLDER_PATTERN.matcher(input);
        StringBuilder resolved = new StringBuilder();

        while (matcher.find()) {
            String contextKey = matcher.group(1).trim();
            Object value = TestContext.get(contextKey);

            if (value == null) {
                throw new IllegalStateException("No value found in TestContext for key: " + contextKey);
            }

            matcher.appendReplacement(resolved, Matcher.quoteReplacement(String.valueOf(value)));
        }

        matcher.appendTail(resolved);
        return resolved.toString();
    }

    /**
     * Returns the first JSON object in the given array that matches all provided key-value pairs.
     *
     * @param list     the JSON array containing objects to search
     * @param criteria map of field names and expected values to match
     * @return the matching JSON object
     * @throws AssertionError if no matching object is found
     */
    public static JSONObject findMatchingJsonObjectInArray(JSONArray list, Map<String, String> criteria) {

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            boolean matches = true;

            for (Map.Entry<String, String> entry : criteria.entrySet()) {
                String actualValue = item.optString(entry.getKey(), "");
                String expectedValue = entry.getValue();

                if (!actualValue.equalsIgnoreCase(expectedValue)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return item;
            }
        }
        throw new AssertionError("No matching resource found for criteria: " + criteria);
    }

    /**
     * Asserts that the actual configuration value matches the expected value.
     * Supports booleans, numbers, JSON arrays, JSON objects, and strings.
     *
     * @param actualValue   the actual value to compare
     * @param expectedValue the expected value as a string
     * @throws AssertionError if values do not match
     */
    public static void assertConfigValueMatchesExpectedValue(Object actualValue, String expectedValue) {

        if (StringUtils.isBlank(expectedValue)) {
            Assert.assertNull(actualValue, "Expected null/empty but got: " + actualValue);
            return;
        }

        if (actualValue == null) {
            Assert.fail("Expected value '" + expectedValue + "' but got null");
        }

        try {
            switch (actualValue) {
                case Boolean b -> {
                    Assert.assertEquals(String.valueOf(actualValue), expectedValue, "Boolean values do not match");
                    return;
                }
                case Number number -> {
                    Assert.assertEquals(String.valueOf(actualValue), expectedValue, "Numeric values do not match");
                    return;
                }
                case JSONArray actualArray -> {
                    JSONArray expectedArray = new JSONArray(expectedValue);
                    JSONAssert.assertEquals(expectedArray, actualArray, JSONCompareMode.LENIENT);
                    return;
                }
                case JSONObject actualObject -> {
                    JSONObject expectedObject = new JSONObject(expectedValue);
                    JSONAssert.assertEquals(expectedObject, actualObject, JSONCompareMode.LENIENT);
                    return;
                }
                default -> {
                }
            }

            Assert.assertEquals(String.valueOf(actualValue), expectedValue, "String values do not match");
        } catch (Exception e) {
            log.error("Error comparing values. Actual: " + actualValue + ", Expected: " + expectedValue, e);
            throw new AssertionError("Comparison failed: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the given HTTP request repeatedly until the expected response status code
     * is received and the provided custom validation condition is satisfied, or until the
     * maximum number of retry attempts is reached.
     *
     *
     * @param requestAction       the HTTP request action to execute
     * @param expectedStatusCode  the expected HTTP response status code
     * @param customValidation    an additional validation condition to apply to the response
     * @return the first response that satisfies both the expected status code and custom
     *         validation, or the last received response if the retry limit is reached
     * @throws InterruptedException if the thread is interrupted while waiting between retries
     */
    public static HttpResponse executeWithRetry(RequestAction requestAction, int expectedStatusCode,
            Predicate<HttpResponse> customValidation) throws InterruptedException {

        HttpResponse response = null;

        for (int attempt = 1; attempt <= Constants.MAX_RETRIES; attempt++) {
            log.info("Attempt " + attempt + "/" + Constants.MAX_RETRIES + ": Executing request...");
            response = requestAction.execute();

            // Check if status code matches and custom validation passes
            if (response != null) {
                if ( response.getResponseCode() == expectedStatusCode && customValidation.test(response)) {
                    return response;
                } else {
                    log.warn("Attempt " + attempt + ": Criteria not met. Received: [" + response.getResponseCode()
                            + "]. Data: " + response.getData());
                }
            }

            if (attempt < Constants.MAX_RETRIES) {
                Thread.sleep(Constants.RETRY_INTERVAL_TIME);
            }
        }
        return response;
    }


    /**
     * Retrieves the pending HTTP request stored in the current test context.
     *
     *
     * @return the pending HTTP request action stored in the test context
     */
    public static RequestAction getPendingHttpRequest() {

        RequestAction requestAction = (RequestAction) TestContext.get(Constants.PENDING_HTTP_REQUEST);
        Assert.assertNotNull(requestAction, "No pending request found in TestContext");
        return requestAction;
    }
}
