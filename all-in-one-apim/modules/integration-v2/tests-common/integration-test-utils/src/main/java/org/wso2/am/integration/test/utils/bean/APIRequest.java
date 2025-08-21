/*
 *Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.test.utils.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationPolicyDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Class to Provides basic API request.
 * <p/>
 * action=addAPI&name=YoutubeFeeds&visibility=public&version=1.0.0&description=Youtube Live Feeds&endpointType=nonsecured
 * &http_checked=http&https_checked=https&endpoint=http://gdata.youtube.com/feeds/api/standardfeeds&wsdl=&
 * tags=youtube,gdata,multimedia&tier=Silver&thumbUrl=http://www.10bigideas.com.au/www/573/files/pf-thumbnail-youtube_logo.jpg
 * &context=/youtube&tiersCollection=Gold&resourceCount=0&resourceMethod-0=GET
 * &resourceMethodAuthType-0=Application&resourceMethodThrottlingTier-0=Unlimited&uriTemplate-0=/*
 */
public class APIRequest extends AbstractRequest {

    private static final Log log = LogFactory.getLog(APIRequest.class);

    private String name;
    private String context;
    private org.json.simple.JSONObject endpoint;
    private String visibility = "public";
    private String version = "1.0.0";
    private String description = "description";
    private String endpointType = "nonsecured";
    private String http_checked = "http";
    private String https_checked = "https";
    private String tags = "tags";
    private String tier = "Silver";
    private String thumbUrl = "";
    private String tiersCollection = "Gold";
    private String type = "HTTP";
    private String resourceCount = "0";
    private String resourceMethod = "GET,POST,PUT,PATCH,DELETE,HEAD";
    private String resourceMethodAuthType = "Application & Application User,Application & Application User";
    private String resourceMethodThrottlingTier = "Unlimited,Unlimited";
    private String uriTemplate = "/*";
    private String roles = "";
    private String wsdl = "";
    private String default_version = "";

    @JsonProperty("isDefaultVersion")
    private String default_version_checked = "";

    private String sandbox = "";
    private String provider = "admin";
    private JSONObject corsConfiguration;
    private String environment = Constants.GATEWAY_ENVIRONMENT;
    private String apiTier = "";
    private String accessControl;
    private String accessControlRoles;
    private String businessOwner;
    private String businessOwnerEmail;
    private String technicalOwner;
    private String technicalOwnerEmail;
    private List<String> securityScheme;
    private List<String> apiCategories;
    private List<String> keyManagers;
    private String subscriptionAvailability;
    private String gatewayType;

    @JsonProperty("operations")
    private List<APIOperationsDTO> operationsDTOS;

    public String getGatewayType() {
        return gatewayType;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    public List<String> getVisibleTenants() {

        return visibleTenants;
    }

    public void setVisibleTenants(List<String> visibleTenants) {

        this.visibleTenants = visibleTenants;
    }

    private List<String> visibleTenants;

    public List<String> getSecurityScheme() {
        return securityScheme;
    }

    public void setSecurityScheme(List<String> securityScheme) {
        this.securityScheme = securityScheme;
    }

    public List<MediationPolicyDTO> getMediationPolicies() {
        return mediationPolicies;
    }

    public void setMediationPolicies(List<MediationPolicyDTO> mediationPolicies) {
        this.mediationPolicies = mediationPolicies;
    }

    private List<MediationPolicyDTO> mediationPolicies;

    public List<APIOperationsDTO> getOperationsDTOS() {
        return operationsDTOS;
    }

    public void setOperationsDTOS(List<APIOperationsDTO> operationsDTOS) {
        this.operationsDTOS = operationsDTOS;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getSandbox() {
        return sandbox;
    }

    public void setSandbox(String sandbox) {
        this.sandbox = sandbox;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getWsdl() {
        return wsdl;
    }

    public void setWsdl(String wsdl) {
        this.wsdl = wsdl;
    }

    public List<String> getKeyManagers() {

        return keyManagers;
    }

    public void setKeyManagers(List<String> keyManagers) {

        this.keyManagers = keyManagers;
    }

    /**
     * Default constructor for APIRequest.
     * This constructor is required by Jackson for deserialization from JSON.
     */
    public APIRequest() {
    }

    /**
     * This method will create API request.
     *
     * @param apiName     - Name of the API
     * @param context     - API context
     * @param endpointUrl - API endpoint URL
     * @throws APIManagerIntegrationTestException - Throws if API request cannot be generated.
     */
    public APIRequest(String apiName, String context, URL endpointUrl) throws APIManagerIntegrationTestException {
        this.name = apiName;
        this.context = context;
        try {
            String endPointString = "{\n" +
                    "  \"production_endpoints\": {\n" +
                    "    \"template_not_supported\": false,\n" +
                    "    \"config\": null,\n" +
                    "    \"url\": \"" + endpointUrl + "\"\n" +
                    "  },\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"" + endpointUrl + "\",\n" +
                    "    \"config\": null,\n" +
                    "    \"template_not_supported\": false\n" +
                    "  },\n" +
                    "  \"endpoint_type\": \"http\"\n" +
                    "}";

            JSONParser parser = new JSONParser();
            this.endpoint = (org.json.simple.JSONObject) parser.parse(endPointString);
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException | ParseException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException("JSON construct error", e);
        }

    }

    /**
     * This method will create API request.
     *
     * @param apiName       - Name of the API
     * @param context       - API context
     * @param endpointUrl   - API endpoint URL
     * @param isCORSEnabled - CORS configurations is enabled
     * @throws APIManagerIntegrationTestException - Throws if API request cannot be generated.
     */
    public APIRequest(String apiName, String context, URL endpointUrl, boolean isCORSEnabled) throws
            APIManagerIntegrationTestException {

        this.name = apiName;
        this.context = context;
        try {
            String endPointString = "{\n" +
                    "  \"production_endpoints\": {\n" +
                    "    \"template_not_supported\": false,\n" +
                    "    \"config\": null,\n" +
                    "    \"url\": \"" + endpointUrl + "\"\n" +
                    "  },\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"" + endpointUrl + "\",\n" +
                    "    \"config\": null,\n" +
                    "    \"template_not_supported\": false\n" +
                    "  },\n" +
                    "  \"endpoint_type\": \"http\"\n" +
                    "}";

            JSONParser parser = new JSONParser();
            this.endpoint = (org.json.simple.JSONObject) parser.parse(endPointString);
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : " + isCORSEnabled + ", " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException | ParseException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException("JSON construct error", e);
        }
    }

    public APIRequest(String apiName, String context, String version, List<String> productionEndpoints,
                      List<String> sandboxEndpoints) throws APIManagerIntegrationTestException {
        this.name = apiName;
        this.context = context;
        this.version = version;
        String productionEPs = "";
        String sandboxEPs = "";
        if (productionEndpoints != null) {
            for (String productionEndpoint : productionEndpoints) {
                String uri = "{\n" +
                        "\"url\":\"" + productionEndpoint + "\",\n" +
                        "\"config\":null,\n" +
                        "\"template_not_supported\": false\n" +
                        "},";
                productionEPs = productionEPs + uri;
            }
            productionEPs = "\"production_endpoints\": [\n" + productionEPs + "],";
        }
        if (sandboxEndpoints != null) {
            for (String sandboxEndpoint : sandboxEndpoints) {
                String uri = "{\n" +
                        "\"url\":\"" + sandboxEndpoint + "\",\n" +
                        "\"config\":null,\n" +
                        "\"template_not_supported\": false\n" +
                        " },";
                sandboxEPs = sandboxEPs + uri;
            }
            sandboxEPs = "\"sandbox_endpoints\": [\n" + sandboxEPs + "],";
        }
        try {
            JSONParser parser = new JSONParser();
            this.endpoint = (org.json.simple.JSONObject) parser.parse(
                    "{ \n"
                            + productionEPs +
                            "\"algoCombo\":\"org.apache.synapse.endpoints.algorithms.RoundRobin\",\n" +
                            "\"failOver\":\"False\",\n" +
                            "\"algoClassName\":\"org.apache.synapse.endpoints.algorithms.RoundRobin\",\n" +
                            "\"sessionManagement\":\"\",\n"
                            + sandboxEPs +
                            "\"implementation_status\":\"managed\",\n" +
                            "\"endpoint_type\":\"load_balance\"\n" +
                            "}"
            );
        } catch (JSONException | ParseException e) {
            log.error("Error when constructing JSON", e);
            throw new APIManagerIntegrationTestException("Error when constructing JSON", e);
        }
    }

    public APIRequest(String apiName, String context) {
        this.name = apiName;
        this.context = context;
        this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                "\"accessControlAllowOrigins\" : [\"*\"], " +
                "\"accessControlAllowCredentials\" : true, " +
                "\"accessControlAllowHeaders\" : " +
                "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
    }

    public APIRequest(String apiName, String context, URI productionEndpointUri, URI sandboxEndpointUri)
            throws APIManagerIntegrationTestException {
        this.name = apiName;
        this.context = context;
        try {
            String endPointString = "{\n" +
                    "  \"production_endpoints\": {\n" +
                    "    \"template_not_supported\": false,\n" +
                    "    \"config\": null,\n" +
                    "    \"url\":\"" + productionEndpointUri + "\"\n" +
                    "  },\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"" + sandboxEndpointUri + "\",\n" +
                    "    \"config\": null,\n" +
                    "    \"template_not_supported\": false\n" +
                    "  },\n" +
                    "  \"endpoint_type\": \"" + productionEndpointUri.getScheme() + "\"\n" +
                    "}";

            JSONParser parser = new JSONParser();
            this.endpoint = (org.json.simple.JSONObject) parser.parse(endPointString);


            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException | ParseException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException("JSON construct error", e);
        }

    }


    /**
     * This method will create API request.
     *
     * @param apiName               - Name of the API
     * @param context               - API context
     * @param productionEndpointUrl - API endpoint URL
     * @throws APIManagerIntegrationTestException - Throws if API request cannot be generated.
     */
    public APIRequest(String apiName, String context, URL productionEndpointUrl, URL sandboxEndpointUrl) throws APIManagerIntegrationTestException {
        this.name = apiName;
        this.context = context;
        try {
            String endPointString = "{\n" +
                    "  \"production_endpoints\": {\n" +
                    "    \"template_not_supported\": false,\n" +
                    "    \"config\": null,\n" +
                    "    \"url\":\"" + productionEndpointUrl + "\"\n" +
                    "  },\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"" + sandboxEndpointUrl + "\",\n" +
                    "    \"config\": null,\n" +
                    "    \"template_not_supported\": false\n" +
                    "  },\n" +
                    "  \"endpoint_type\": \"" + productionEndpointUrl.getProtocol() + "\"\n" +
                    "}";

            JSONParser parser = new JSONParser();
            this.endpoint = (org.json.simple.JSONObject) parser.parse(endPointString);
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException | ParseException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException("JSON construct error", e);
        }

    }

    /**
     * This method will create API request.
     *
     * @param apiName                  - Name of the API
     * @param context                  - API context
     * @param prodEndpointAvailability - True = Only Product, False = Only Sandbox
     * @param endpointUrl              - API endpoint URL
     * @throws APIManagerIntegrationTestException - Throws if API request cannot be generated.
     */
    public APIRequest(String apiName, String context, boolean prodEndpointAvailability, URL endpointUrl) throws APIManagerIntegrationTestException {
        this.name = apiName;
        this.context = context;
        try {
            String endPointString = "{\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"" + endpointUrl + "\",\n" +
                    "    \"config\": null,\n" +
                    "    \"template_not_supported\": false\n" +
                    "  },\n" +
                    "  \"endpoint_type\": \"http\"\n" +
                    "}";
            if (prodEndpointAvailability) {
                endPointString = "{\n" +
                        "  \"production_endpoints\": {\n" +
                        "    \"template_not_supported\": false,\n" +
                        "    \"config\": null,\n" +
                        "    \"url\": \"" + endpointUrl + "\"\n" +
                        "  },\n" +
                        "  \"endpoint_type\": \"http\"\n" +
                        "}";
            }

            JSONParser parser = new JSONParser();
            this.endpoint = (org.json.simple.JSONObject) parser.parse(endPointString);
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException | ParseException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException("JSON construct error", e);
        }
    }

    @Override
    public void setAction() {
        setAction("addAPI");
    }

    public void setAction(String action) {
        super.setAction(action);
    }

    /**
     * initialize method
     */
    @Override
    public void init() {

        addParameter("name", name);
        addParameter("context", context);
        addParameter("endpoint_config", endpoint.toString());
        addParameter("provider", getProvider());
        addParameter("visibility", getVisibility());
        addParameter("accessControl", getAccessControl());
        addParameter("accessControlRoles", getAccessControlRoles());
        addParameter("version", getVersion());
        addParameter("description", getDescription());
        addParameter("endpointType", getEndpointType());
        addParameter("http_checked", getHttp_checked());
        addParameter("https_checked", getHttps_checked());
        addParameter("tags", getTags());
        addParameter("tier", getTier());
        addParameter("thumbUrl", getThumbUrl());
        addParameter("tiersCollection", getTiersCollection());
        addParameter("type", getType());
        addParameter("resourceCount", getResourceCount());
        addParameter("resourceMethod-0", getResourceMethod());
        addParameter("resourceMethodAuthType-0", getResourceMethodAuthType());
        addParameter("resourceMethodThrottlingTier-0", getResourceMethodThrottlingTier());
        addParameter("uriTemplate-0", getUriTemplate());
        addParameter("default_version", getDefault_version());
        addParameter("default_version_checked", getDefault_version_checked());
        addParameter("environments", getEnvironment());
        addParameter("corsConfiguration", getCorsConfiguration().toString());
        addParameter("apiTier", getApiTier());

        if (roles.length() > 1) {
            addParameter("roles", getRoles());
        }
        if (wsdl.length() > 1) {
            addParameter("wsdl", getWsdl());
        }
        if (sandbox.length() > 1) {
            addParameter("sandbox", getSandbox());
        }

    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public org.json.simple.JSONObject getEndpointConfig() {
        return endpoint;
    }

    public void setEndpointConfig(org.json.simple.JSONObject endpoint) {
        this.endpoint = endpoint;
    }

    public String getContext() {
        return context;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public void setEndpoint(org.json.simple.JSONObject endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttp_checked() {
        return http_checked;
    }

    public void setHttp_checked(String http_checked) {
        this.http_checked = http_checked;
    }

    public String getHttps_checked() {
        return https_checked;
    }

    public void setHttps_checked(String https_checked) {
        this.https_checked = https_checked;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getTiersCollection() {
        return tiersCollection;
    }

    public void setTiersCollection(String tiersCollection) {
        this.tiersCollection = tiersCollection;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(String resourceCount) {
        this.resourceCount = resourceCount;
    }

    public String getResourceMethod() {
        return resourceMethod;
    }

    public void setResourceMethod(String resourceMethod) {
        this.resourceMethod = resourceMethod;
    }

    public String getResourceMethodAuthType() {
        return resourceMethodAuthType;
    }

    public void setResourceMethodAuthType(String resourceMethodAuthType) {
        this.resourceMethodAuthType = resourceMethodAuthType;
    }

    public String getResourceMethodThrottlingTier() {
        return resourceMethodThrottlingTier;
    }

    public void setResourceMethodThrottlingTier(String resourceMethodThrottlingTier) {
        this.resourceMethodThrottlingTier = resourceMethodThrottlingTier;
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public String getDefault_version() {
        return default_version;
    }

    public void setDefault_version(String default_version) {
        this.default_version = default_version;
    }

    public String getDefault_version_checked() {
        return default_version_checked;
    }

    public void setDefault_version_checked(String default_version_checked) {
        this.default_version_checked = default_version_checked;
    }

    public JSONObject getCorsConfiguration() {
        return corsConfiguration;
    }

    public void setCorsConfiguration(JSONObject corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    public String getApiTier() {
        return apiTier;
    }

    public void setApiTier(String apiTier) {
        this.apiTier = apiTier;
    }

    public String getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(String accessControl) {
        this.accessControl = accessControl;
    }

    public String getAccessControlRoles() {
        return accessControlRoles;
    }

    public void setAccessControlRoles(String accessControlRoles) {
        this.accessControlRoles = accessControlRoles;
    }

    public String getBusinessOwner() {
        return businessOwner;
    }

    public void setBusinessOwner(String businessOwner) {
        this.businessOwner = businessOwner;
    }

    public String getBusinessOwnerEmail() {
        return businessOwnerEmail;
    }

    public void setBusinessOwnerEmail(String businessOwnerEmail) {
        this.businessOwnerEmail = businessOwnerEmail;
    }

    public String getTechnicalOwner() {
        return technicalOwner;
    }

    public void setTechnicalOwner(String technicalOwner) {
        this.technicalOwner = technicalOwner;
    }

    public String getTechnicalOwnerEmail() {
        return technicalOwnerEmail;
    }

    public void setTechnicalOwnerEmail(String technicalOwnerEmail) {
        this.technicalOwnerEmail = technicalOwnerEmail;
    }

    public void setApiCategories(List<String> apiCategories) {
        this.apiCategories = apiCategories;
    }

    public List<String> getApiCategories() {
        return apiCategories;
    }

    public String getSubscriptionAvailability() {

        return subscriptionAvailability;
    }

    public void setSubscriptionAvailability(String subscriptionAvailability) {

        this.subscriptionAvailability = subscriptionAvailability;
    }
}
