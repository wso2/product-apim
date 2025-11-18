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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.json.JSONObject;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.io.StringReader;

public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String getDCREndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_DCR_EP;
    }

    public static String getAPIMTokenEndpointURL(String baseUrl) {

        return baseUrl + Constants.DEFAULT_APIM_TOKEN_EP;
    }


    public static String getAPICreateEndpointURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis";
    }

    public static String getAPIEndpointURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId;
    }

    public static String getAPIRevisionURL(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/revisions";
    }

    public static String getAPIRevisionDeploymentURL(String baseUrl, String apiId, String revisionId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/deploy-revision?revisionId=" + revisionId;
    }

    public static String getAPIRevisionUnDeploymentURL(String baseUrl, String apiId, String revisionId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/undeploy-revision?revisionId=" + revisionId;
    }

    public static String getAPIRevisionRestoreURL(String baseUrl, String apiId, String revisionId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/restore-revision?revisionId=" + revisionId;
    }

    public static String getAPIRevisionByID(String baseUrl, String apiId, String revisionID) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId + "/revisions/" + revisionID;
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

    public static String getChangeLifecycleURL(String baseUrl, String apiId, String action, String lifecycleChecklist) {

        if (StringUtils.isBlank(apiId) || StringUtils.isBlank(action)) {
            throw new IllegalArgumentException("API ID and Action must be provided.");
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl)
                .append(Constants.DEFAULT_APIM_API_DEPLOYER)
                .append("apis/change-lifecycle?apiId=")
                .append(URLEncoder.encode(apiId, StandardCharsets.UTF_8))
                .append("&action=")
                .append(URLEncoder.encode(action, StandardCharsets.UTF_8));

        // Append the lifecycleChecklist if provided
        if (lifecycleChecklist != null && !lifecycleChecklist.trim().isEmpty()) {
            String encodedChecklist = URLEncoder.encode(lifecycleChecklist, StandardCharsets.UTF_8);
            urlBuilder.append("&lifecycleChecklist=").append(encodedChecklist);
        }
        log.info("Change API Lifecycle URL: " + urlBuilder);
        return urlBuilder.toString();
    }

    public static String getCreateSubscriptionURL(String baseUrl) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "subscriptions";
    }

    public static String getSubscriptionURL(String baseUrl, String subscriptionId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "subscriptions/" + subscriptionId;
    }

    public static String getAllSubscriptionsURL(String baseUrl,String apiId, String applicationId, String groupId,
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

    public static String getApplicationEndpointURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId;
    }

    public static String getGenerateApplicationKeysURL(String baseUrl, String applicationId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/generate-keys";
    }

    public static String getGenerateApplicationTokenURL(String baseUrl, String applicationId, String keyMappingId) {
        return baseUrl + Constants.DEFAULT_DEVPORTAL + "applications/" + applicationId + "/oauth-keys/" +
                keyMappingId + "/generate-token";
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

    public static String getTenantMgtAdminServiceURL(String baseUrl) {
        return baseUrl + "services/TenantMgtAdminService";
    }

    public static String getMultipleCredentialsUserAdminServiceURL(String baseUrl) {
        return baseUrl + "services/UserAdmin";
    }

    public static String getRemoteUserStoreManagerServiceURL(String baseUrl) {
        return baseUrl + "services/RemoteUserStoreManagerService";
    }

    public static String getNewAPIVersionURL(String baseUrl, String newVersion, Boolean defaultVersion, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/copy-api" +
                "?newVersion=" + URLEncoder.encode(newVersion, StandardCharsets.UTF_8) +
                "&defaultVersion=" + defaultVersion +
                "&apiId=" + URLEncoder.encode(apiId, StandardCharsets.UTF_8);
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

    public static String getSubscriptions(String baseUrl, String apiId) {
        return baseUrl + Constants.DEFAULT_APIM_API_DEPLOYER + "subscriptions?apiId=" + apiId;
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
     * Extracts the value for the given key from a JSON payload.
     *
     * @param jsonPayload the response payload as a JSON string
     * @param key         the key to extract
     * @return the value for the key
     * @throws IOException if the payload cannot be parsed
     */
    public static Object extractValueFromPayload(String jsonPayload, String key) throws IOException {

        if (StringUtils.isBlank(jsonPayload)) {
            throw new IOException("JSON payload is null or empty.");
        }

        JSONObject jsonObject = new JSONObject(jsonPayload);
        if (!jsonObject.has(key)) {
            throw new IOException("Key '" + key + "' not found in JSON payload.");
        }
        return jsonObject.getString(key);
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
     *
     * @param soapXml         XML content
     * @param axiomXPathExpr  XPath expression selecting elements
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


}
