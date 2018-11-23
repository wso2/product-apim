/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.test.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.bean.AbstractRequest;

import java.net.URL;

public class APIRequest extends AbstractRequest {
    private static final Log log = LogFactory.getLog(APIRequest.class);

    private String name;
    private String context;
    private String visibility;
    private String version;
    private String resource;
    private String swagger;
    private String tiersCollection;
    private JSONObject endpoint;
    private String bizOwner;
    private String bizOwnerMail;
    private String techOwner;
    private String techOwnerMail;
    private String endpointType;
    private String endpointAuthType;
    private String epUsername;
    private String epPassword;
    private String default_version_checked;
    private String responseCache;
    private String cacheTimeout;
    private String subscriptions;
    private String http_checked;
    private String https_checked;
    private String inSequence;
    private String outSequence;
    private String description;
    private String tag;

    private String import_definition;
    private String swagger_filename;
    private String swagger_url;
    private String type;

    public APIRequest(String name, String context, String visibility, String version, String resource) {
        this.name = name;
        this.context = context;
        this.visibility = visibility;
        this.version = version;
        this.resource = resource;
        constructSwagger();
    }

    public APIRequest(String name, String context, String visibility, String version, String resource, String tiersCollection, URL endpointUrl) {
        this.name = name;
        this.context = context;
        this.visibility = visibility;
        this.version = version;
        this.resource = resource;
        this.tiersCollection = tiersCollection;
        try {
            this.endpoint =
                    new JSONObject("{\"production_endpoints\":{\"url\":\""
                            + endpointUrl + "\",\"config\":null},\"endpoint_type\":\""
                            + "http" + "\"}");
        } catch (JSONException e) {
            log.error("JSON construct error", e);
        }
        constructSwagger();
    }

    public APIRequest(String name, String context, String visibility, String version, String resource,
                      String description, String tag, String tiersCollection, String backend, String bizOwner,
                      String bizOwnerMail, String techOwner, String techOwnerMail, String endpointType,
                      String endpointAuthType, String epUsername, String epPassword, String default_version_checked,
                      String responseCache, String cacheTimeout, String subscriptions, String http_checked,
                      String https_checked, String inSequence, String outSequence) {
        this.name = name;
        this.context = context;
        this.visibility = visibility;
        this.version = version;
        this.resource = resource;
        this.description = description;
        this.tag = tag;
        this.tiersCollection = tiersCollection;
        this.endpoint =
                new JSONObject("{\"production_endpoints\":{\"url\":\""
                        + backend + "\",\"config\":null},\"endpoint_type\":\""
                        + "http" + "\"}");
        this.bizOwner = bizOwner;
        this.bizOwnerMail = bizOwnerMail;
        this.techOwner = techOwner;
        this.techOwnerMail = techOwnerMail;
        this.endpointType = endpointType;
        this.endpointAuthType = endpointAuthType;
        this.epUsername = epUsername;
        this.epPassword = epPassword;
        this.default_version_checked = default_version_checked;
        this.responseCache = responseCache;
        this.cacheTimeout = cacheTimeout;
        this.subscriptions = subscriptions;
        this.http_checked = http_checked;
        this.https_checked = https_checked;
        this.inSequence = inSequence;
        this.outSequence = outSequence;
        constructSwagger();
    }

    public APIRequest(String definition, String filename, String url, String type) {
        this.import_definition = definition;
        this.swagger_filename = filename;
        this.swagger_url = url;
        this.type = type;
    }

    public APIRequest(String name, String context, String version) {
        this.name = name;
        this.context = context;
        this.version = version;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    @Override
    public void setAction() {
    }

    @Override
    public void init() {
        this.addParameter("name", this.name);
        this.addParameter("context", this.context);
        this.addParameter("visibility", this.visibility);
        this.addParameter("version", this.version);
        this.addParameter("swagger", this.swagger);
        if (tiersCollection != null) {
            this.addParameter("tiersCollection", this.tiersCollection);
        }
        if (endpoint != null) {
            this.addParameter("endpoint_config", endpoint.toString());
        }

        //Optional parameters
        if (this.description != null) {
            this.addParameter("description", this.description);
        }
        if (this.tag != null) {
            this.addParameter("tags", this.tag);
        }
        if (this.bizOwner != null) {
            this.addParameter("bizOwner", this.bizOwner);
        }
        if (this.bizOwnerMail != null) {
            this.addParameter("bizOwnerMail", this.bizOwnerMail);
        }
        if (this.techOwner != null) {
            this.addParameter("techOwner", techOwner);
        }
        if (this.techOwnerMail != null) {
            this.addParameter("techOwnerMail", techOwnerMail);
        }
        if (this.endpointType != null) {
            this.addParameter("endpointType", endpointType);
        }
        if (this.endpointAuthType != null) {
            this.addParameter("endpointAuthType", endpointAuthType);
        }
        if (this.epUsername != null) {
            this.addParameter("epUsername", epUsername);
        }
        if (this.epPassword != null) {
            this.addParameter("epPassword", epPassword);
        }
        if (this.default_version_checked != null) {
            this.addParameter("default_version_checked", default_version_checked);
        }
        if (this.responseCache != null) {
            this.addParameter("responseCache", responseCache);
        }
        if (this.cacheTimeout != null) {
            this.addParameter("cacheTimeout", cacheTimeout);
        }
        if (this.subscriptions != null) {
            this.addParameter("subscriptions", subscriptions);
        }
        if (this.http_checked != null && this.https_checked != null) {
            this.addParameter("http_checked", http_checked);
            this.addParameter("https_checked", https_checked);
        }
        if (this.inSequence != null && this.outSequence != null) {
            this.addParameter("inSequence", inSequence);
            this.addParameter("outSequence", outSequence);
        }

    }

    public void constructSwagger() {
        setSwagger("{\"swagger\":\"2.0\",\"paths\":{" + "\"" + resource + "\""
                + ":{\"post\":{\"parameters\":[{\"name\":\"Payload\",\"description\":\"Request Body\",\"required\":false,\"in\":\"body\",\"schema\":{\"type\":\"object\",\"properties\":{\"payload\":{\"type\":\"string\"}}}}],\"responses\":{\"200\":{\"description\":\"\"}}}}},\"info\":{\"title\": "
                + "\"" + name + "\"" + ",\"version\":" + "\"" + version + "\"" + "}}");
    }
}
