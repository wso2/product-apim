/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.test.utils.bean;

/**
 * Bean class to contain the HTTP API resource information.
 */
public class APIResourceBean {

    private String resourceMethod = "GET";
    private String resourceMethodAuthType = "Application & Application User";
    private String resourceMethodThrottlingTier = "Unlimited";
    private String uriTemplate = "/*";

    /**
     * constructor of  the APIResourceBean Class
     *
     * @param resourceMethod               - Resource method Type. Ex. GET/POST/PUT..
     * @param resourceMethodAuthType       - Authentication type of the resource
     * @param resourceMethodThrottlingTier - Throttling tier of the resource method
     * @param uriTemplate                  - URL template of the resource
     */
    public APIResourceBean(String resourceMethod, String resourceMethodAuthType, String resourceMethodThrottlingTier, String uriTemplate) {
        this.resourceMethod = resourceMethod;
        this.resourceMethodAuthType = resourceMethodAuthType;
        this.resourceMethodThrottlingTier = resourceMethodThrottlingTier;
        this.uriTemplate = uriTemplate;
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
}
