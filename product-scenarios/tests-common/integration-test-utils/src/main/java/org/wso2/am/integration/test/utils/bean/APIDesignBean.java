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


/**
 * Bean class to contain the information needed to create an API up to design level.
 */
public class APIDesignBean extends AbstractRequest {

    private String name = "";
    private String context = "";
    private String version = "";
    private String visibility = "public";
    private String roles = "";
    private String apiThumb = "";
    private String description = "";
    private String tags = "";
    private String swagger = "";

    public APIDesignBean(String name, String context, String version, String description,
                         String tags, String swagger) throws Exception{
        this.name = name;
        this.context = context;
        this.version = version;
        this.description = description;
        this.tags = tags;
        this.swagger = swagger;
    }


    public APIDesignBean(String name, String context, String version, String description, String tags)
    throws Exception{
        this.name = name;
        this.context = context;
        this.version = version;
        this.description = description;
        this.tags = tags;
        this.swagger = " {\"swagger\":\"2.0\",\"paths\":{\"/*\":{\"get\":{\"responses\":" +
                "{\"200\":{}}},\"post\":{\"responses\":{\"200\":{}}},\"put\":{\"responses\":" +
                "{\"200\":{}}},\"delete\":{\"responses\":{\"200\":{}}},\"head\":{\"responses\":" +
                "{\"200\":{}}}}},\"info\":{\"title\":\"" + name + "\",\"version\":" +
                "\"" + version + "\",\"description\":\"" + description + "\"}}";;
    }

    @Override
    public void setAction() {
        setAction("design");
    }

    @Override
    public void init() {

        addParameter("name", name);
        addParameter("context", context);
        addParameter("version", version);
        addParameter("visibility", visibility);
        addParameter("roles", roles);
        addParameter("urlThumb", apiThumb);
        addParameter("description", description);
        addParameter("tags", tags);
        addParameter("swagger", swagger);

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getApiThumb() {
        return apiThumb;
    }

    public void setApiThumb(String apiThumb) {
        this.apiThumb = apiThumb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }


}

