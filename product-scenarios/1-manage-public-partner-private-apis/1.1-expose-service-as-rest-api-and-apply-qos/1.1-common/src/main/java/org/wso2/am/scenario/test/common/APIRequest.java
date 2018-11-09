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

import org.wso2.am.integration.test.utils.bean.AbstractRequest;

public class APIRequest extends AbstractRequest {

    private String name;
    private String context;
    private String visibility;
    private String version;
    private String resource;
    private String swagger;

    public APIRequest(String name, String context, String visibility, String version, String resource) {
        this.name = name;
        this.context = context;
        this.visibility = visibility;
        this.version = version;
        this.resource = resource;
        constructSwagger();
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    @Override
    public void setAction() {}

    @Override
    public void init() {
        this.addParameter("name", this.name);
        this.addParameter("context", this.context);
        this.addParameter("visibility", this.visibility);
        this.addParameter("version", this.version);
        this.addParameter("swagger", this.swagger);
    }

    public void constructSwagger() {
        setSwagger("{\"swagger\":\"2.0\",\"paths\":{" + "\"" + resource + "\""
                + ":{\"post\":{\"parameters\":[{\"name\":\"Payload\",\"description\":\"Request Body\",\"required\":false,\"in\":\"body\",\"schema\":{\"type\":\"object\",\"properties\":{\"payload\":{\"type\":\"string\"}}}}],\"responses\":{\"200\":{\"description\":\"\"}}}}},\"info\":{\"title\": "
                + "\"" + name + "\"" + ",\"version\":" + "\"" + version + "\"" + "}}");
    }
}
