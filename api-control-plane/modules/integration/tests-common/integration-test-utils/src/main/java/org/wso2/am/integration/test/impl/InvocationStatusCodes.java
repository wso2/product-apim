/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.am.integration.test.impl;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Define the expected status codes to be expected when invoking resources based on the scopes assigned to a given
 * resource
 */
public class InvocationStatusCodes {
    /**
     * The default expected status code to be expected when invoking resources that do not have any scopes
     */
    private int defaultStatusCode = HttpStatus.SC_OK;

    /**
     * Define the expected status code to be expected when invoking a resource that has a given scope configured.
     * This is stored as {scope} : {status_code} key/value pairs
     */
    private Map<String, Integer> scopeSpecificStatusCode = new HashMap<>();


    public int getDefaultStatusCode() {
        return defaultStatusCode;
    }

    public void setDefaultStatusCode(int defaultStatusCode) {
        this.defaultStatusCode = defaultStatusCode;
    }


    public int getStatusCodeForScope(String scope) {
        return scopeSpecificStatusCode.get(scope);
    }

    public void addScopeSpecificStatusCode(String scope, int statusCode) {
        scopeSpecificStatusCode.put(scope, statusCode);
    }
}
