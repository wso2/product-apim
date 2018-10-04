/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests.util;

import java.util.ArrayList;
import java.util.List;

/**
 * API Model to Extract users.yaml file
 */
public class API {
    String user;
    String name;
    String context;
    String version;
    String description;
    String endpoint;
    List<String> lifecycleStatusChain;
    String visibility;
    List<String> visibleRoles;
    List<String> subscriptionPolicies = new ArrayList<>();
    List<Document> documents = new ArrayList();
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<String> getLifecycleStatusChain() {
        return lifecycleStatusChain;
    }

    public void setLifecycleStatusChain(List<String> lifecycleStatusChain) {
        this.lifecycleStatusChain = lifecycleStatusChain;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<String> getSubscriptionPolicies() {
        return subscriptionPolicies;
    }

    public void setSubscriptionPolicies(List<String> subscriptionPolicies) {
        this.subscriptionPolicies = subscriptionPolicies;
    }

    public List<String> getVisibleRoles() {
        return visibleRoles;
    }

    public void setVisibleRoles(List<String> visibleRoles) {
        this.visibleRoles = visibleRoles;
    }

    public List<Document> getDocuments() {

        return documents;
    }

    public void setDocuments(List<Document> documents) {

        this.documents = documents;
    }
}
