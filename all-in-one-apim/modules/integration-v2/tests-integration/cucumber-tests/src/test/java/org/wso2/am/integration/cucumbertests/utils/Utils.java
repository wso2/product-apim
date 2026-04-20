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

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String getDCREndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_DCR_EP;
    }

    public static String getAPIMTokenEndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_APIM_TOKEN_EP;
    }

    public static String getAPICreateEndpointURL(String baseUrl, String resourceType) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType;
    }

    public static String getResourceEndpointURL(String baseUrl, String resourceType, String resourceId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId;
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

    public static String getChangeLifecycleURL(String baseUrl, String resourceType, String apiId, String action, String lifecycleChecklist) {

        if (StringUtils.isBlank(apiId) || StringUtils.isBlank(action)) {
            throw new IllegalArgumentException("ID and Action must be provided.");
        }

        String idParam = "apis".equals(resourceType) ? "apiId" : "apiProductId";

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

        StringBuilder urlBuilder = new StringBuilder(baseUrl + Constants.DEFAULT_DEVPORTAL + "/subscriptions");

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

    public static String getApplicationAllKeys(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys";
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
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "subscriptions/unblock-subscription?subscriptionId=" + subscriptionID + "&blockState=BLOCKED";
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

    public static String getSwaggerURL(String baseUrl, String resourceType, String resourceId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType + "/" + resourceId + "/swagger";
    }

    public static String getAPIDefinitionURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/import-openapi";
    }


    public static String getInternalAPIKey(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/generate-key";
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
     * Extracts the API UUID
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

    /**
     * Extracts a value from a JSON payload using a JSONPath expression.
     *
     * @param jsonPayload the JSON string to parse
     * @param path        the JSONPath (e.g., "id" or "$.operations[0].verb")
     * @return the extracted value as an Object
     * @throws IOException if the payload is invalid or the path is not found
     */
    public static Object extractValueFromPayload(String jsonPayload, String path) throws IOException {

        if (StringUtils.isBlank(jsonPayload)) {
            throw new IOException("JSON payload is null or empty.");
        }

        // Validate JSON structure
        try {
            new JSONObject(jsonPayload.trim());
        } catch (Exception e) {
            throw new IOException("Provided string is not a valid JSON object.", e);
        }

        // Normalize the JSONPath
        String trimmedPath = path.trim();
        String jsonPath = trimmedPath.startsWith(Constants.JSON_PATH_ROOT)
                ? trimmedPath
                : Constants.JSON_PATH_ROOT_WITH_DOT + trimmedPath;

        // Extract using JsonPath
        try {
            return JsonPath.read(jsonPayload, jsonPath);
        } catch (Exception e) {
            throw new IOException("Path '" + jsonPath + "' not found or invalid in JSON payload.", e);
        }
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
}
