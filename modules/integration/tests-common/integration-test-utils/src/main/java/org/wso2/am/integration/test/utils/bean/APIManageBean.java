/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public class APIManageBean extends AbstractRequest {
    private String name;
    private String type = "";
    private String version = "";
    private String provider = "";
    private String default_version_checked = "";
    private String transport_https = "";
    private String transport_http = "";
    private String responseCache = "";
    private String apiTier = "";
    private String authorizationHeader = "";
    private String backend_tps = "";
    private String productionTps = "";
    private String sandboxTps = "";
    private String tier = "";
    private String api_level_policy = "";
    private String enableApiLevelPolicy = "";
    private String gateways = "";
    private String bizOwner = "";
    private String bizOwnerMail = "";
    private String techOwner = "";
    private String techOwnerMail = "";
    private String additionalProperties = "";
    private String environments = "";
    private String swagger = "";
    private String tiersCollection = "";

    public APIManageBean(String name, String version, String provider, String transport_https, String responseCache,
                         String api_level_policy, String environments, String swagger, String tiersCollection) {
        this.name = name;
        this.version = version;
        this.provider = provider;
        this.transport_https = transport_https;
        this.responseCache = responseCache;
        this.api_level_policy = api_level_policy;
        this.environments = environments;
        this.swagger = swagger;
        this.tiersCollection = tiersCollection;
    }

    @Override
    public void init() {
        addParameter("name", name);
        addParameter("version", version);
        addParameter("provider", provider);
        addParameter("swagger", swagger);
        addParameter("tiersCollection", "Unlimited, Gold");
        addParameter("bizOwner", "biz_ownx");
        addParameter("bizOwnerMail", "bbb@gmail.com");
        addParameter("techOwner", "techowsam");
        addParameter("techOwnerMail", "ttt@gmail.com");
        addParameter("tiersCollection", tiersCollection);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDefaultVersionChecked() {
        return default_version_checked;
    }

    public void setDefaultVersionChecked(String default_version_checked) {
        this.default_version_checked = default_version_checked;
    }

    public String getTransportHttps() {
        return transport_https;
    }

    public void setTransportHttps(String transport_https) {
        this.transport_https = transport_https;
    }

    public String getTransportHttp() {
        return transport_http;
    }

    public void setTransportHttp(String transport_http) {
        this.transport_http = transport_http;
    }

    public String getResponseCache() {
        return responseCache;
    }

    public void setResponseCache(String responseCache) {
        this.responseCache = responseCache;
    }

    public String getApiTier() {
        return apiTier;
    }

    public void setApiTier(String apiTier) {
        this.apiTier = apiTier;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public String getBackend_tps() {
        return backend_tps;
    }

    public void setBackend_tps(String backend_tps) {
        this.backend_tps = backend_tps;
    }

    public String getProductionTps() {
        return productionTps;
    }

    public void setProductionTps(String productionTps) {
        this.productionTps = productionTps;
    }

    public String getSandboxTps() {
        return sandboxTps;
    }

    public void setSandboxTps(String sandboxTps) {
        this.sandboxTps = sandboxTps;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getApi_level_policy() {
        return api_level_policy;
    }

    public void setApi_level_policy(String api_level_policy) {
        this.api_level_policy = api_level_policy;
    }

    public String getEnableApiLevelPolicy() {
        return enableApiLevelPolicy;
    }

    public void setEnableApiLevelPolicy(String enableApiLevelPolicy) {
        this.enableApiLevelPolicy = enableApiLevelPolicy;
    }

    public String getGateways() {
        return gateways;
    }

    public void setGateways(String gateways) {
        this.gateways = gateways;
    }

    public String getBizOwner() {
        return bizOwner;
    }

    public void setBizOwner(String bizOwner) {
        this.bizOwner = bizOwner;
    }

    public String getBizOwnerMail() {
        return bizOwnerMail;
    }

    public void setBizOwnerMail(String bizOwnerMail) {
        this.bizOwnerMail = bizOwnerMail;
    }

    public String getTechOwner() {
        return techOwner;
    }

    public void setTechOwner(String techOwner) {
        this.techOwner = techOwner;
    }

    public String getTechOwnerMail() {
        return techOwnerMail;
    }

    public void setTechOwnerMail(String techOwnerMail) {
        this.techOwnerMail = techOwnerMail;
    }

    public String getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(String additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public String getEnvironments() {
        return environments;
    }

    public void setEnvironments(String environments) {
        this.environments = environments;
    }

    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    public String getTiersCollection() {
        return tiersCollection;
    }

    public void setTiersCollection(String tiersCollection) {
        this.tiersCollection = tiersCollection;
    }

    @Override
    public void setAction() {
        setAction("manage");
    }

    @Override
    public void setAction(String action) {
        super.setAction(action);
    }
}
