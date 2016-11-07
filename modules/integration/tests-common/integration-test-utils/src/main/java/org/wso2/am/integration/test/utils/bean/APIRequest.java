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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.net.URI;
import java.net.URL;

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
    private JSONObject endpoint;
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
    private String type = "http";
    private String resourceCount = "0";
    private String resourceMethod = "GET,POST,PUT,PATCH,DELETE,HEAD";
    private String resourceMethodAuthType = "Application & Application User,Application & Application User";
    private String resourceMethodThrottlingTier = "Unlimited,Unlimited";
    private String uriTemplate = "/*";
    private String roles = "";
    private String wsdl = "";
    private String default_version = "";
    private String default_version_checked = "";
    private String sandbox = "";
    private String provider = "admin";
    private JSONObject corsConfiguration;
    private String environment = "Production and Sandbox";
    private String ws = "false";
    private String apiTier = "";

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
            this.endpoint =
                    new JSONObject("{\"production_endpoints\":{\"url\":\""
                            + endpointUrl + "\",\"config\":null},\"endpoint_type\":\""
                            + endpointUrl.getProtocol() + "\"}");
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException("JSON construct error", e);
        }

    }

    public APIRequest(String apiName, String context, URI productionEndpointUri, URI sandboxEndpointUri )
            throws APIManagerIntegrationTestException {
        this.name = apiName;
        this.context = context;
        try {
            this.endpoint =
                    new JSONObject("{\"production_endpoints\":{\"url\":\""
                            + productionEndpointUri + "\",\"config\":null}, \"sandbox_endpoints\":{\"url\":\""
                            + sandboxEndpointUri + "\",\"config\":null},\"endpoint_type\":\""
                            + productionEndpointUri.getScheme() + "\"}");
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException e) {
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
            this.endpoint =
                    new JSONObject("{\"production_endpoints\":{\"url\":\""
                            + productionEndpointUrl + "\",\"config\":null}, \"sandbox_endpoints\":{\"url\":\""
                            + sandboxEndpointUrl + "\",\"config\":null},\"endpoint_type\":\""
                            + productionEndpointUrl.getProtocol() + "\"}");
            this.corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : false, " +
                    "\"accessControlAllowOrigins\" : [\"*\"], " +
                    "\"accessControlAllowCredentials\" : true, " +
                    "\"accessControlAllowHeaders\" : " +
                    "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                    "\"Content-Type\"], \"accessControlAllowMethods\" : [\"POST\", " +
                    "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        } catch (JSONException e) {
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
        addParameter("ws", getWs());
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

    public JSONObject getEndpointConfig() {
        return endpoint;
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

    public String getWs() {
        return ws;
    }

    public void setWs(String ws) {
        this.ws = ws;
    }

    public String getApiTier() {
        return apiTier;
    }

    public void setApiTier(String apiTier) {
        this.apiTier = apiTier;
    }
}
