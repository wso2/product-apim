/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.test.utils.bean;

/**
 * This bean class is used to represent production endpoint
 */
public class ProductionEndpointBean {
    private String url;
    private String config;
    //endpoint_type field is compose with "_" since it should be include in JSON message as a attribute
    private String endpoint_type;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getEndpoint_type() {
        return endpoint_type;
    }

    public void setEndpoint_type(String endpoint_type) {
        this.endpoint_type = endpoint_type;
    }
}
