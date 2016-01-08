/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.test.utils.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.net.URL;

/**
 * Bean class to contain the information needed to Implement the already Designed API
 */

public class APIImplementationBean extends AbstractRequest {

    private static final Log log = LogFactory.getLog(APICreationRequestBean.class);
    private String implementMethod = "endpoint";
    private JSONObject endpoint;
    private String name = "";
    private String version = "";
    private String provider = "";
    private String swagger = "";

    public APIImplementationBean( String name, String version, String provider,
                                  URL prototypeEndpointUrl)
            throws Exception {
        this.name = name;
        this.version = version;
        this.provider = provider;
        try {
            this.endpoint = new JSONObject("{\"production_endpoints\":{\"url\":\""
                    + prototypeEndpointUrl + "\",\"config\":null},\"endpoint_type\":\""
                    + prototypeEndpointUrl.getProtocol() + "\"," +
                    "\"implementation_status\":\"prototyped\"}");
        } catch (JSONException e) {
            log.error("JSON construct error", e);
            throw new APIManagerIntegrationTestException(" Error When constructing the end point url JSON", e);
        }
    }

    @Override
    public void setAction() {
        setAction("implement");
    }


    @Override
    public void setAction(String action) {
        super.setAction(action);
    }


    @Override
    public void init() {

        addParameter("implementation_methods",implementMethod);
        addParameter("endpoint_config", endpoint.toString());
        addParameter("name", name);
        addParameter("version", version);
        addParameter("provider", provider);
        addParameter("swagger",swagger);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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


    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    public String getImplementMethod() {
        return implementMethod;
    }

    public void setImplementMethod(String implementMethod) {
        this.implementMethod = implementMethod;
    }




}
