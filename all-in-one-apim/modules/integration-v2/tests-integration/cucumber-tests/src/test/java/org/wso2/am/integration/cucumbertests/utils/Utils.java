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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.wso2.am.integration.test.Constants;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
     * @param context the context to resolve from
     * @param key     the key
     * @return the resolved value
     * @throws IllegalArgumentException if the key is not found
     */
    public static Object resolveFromContext(TestContext context, String key) {

        String lookupKey = (key.startsWith("<") && key.endsWith(">")) ? key.substring(1, key.length() - 1) : key;

        Object value = context.get(lookupKey);
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
}
