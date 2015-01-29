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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractRequest {
    public String action;
    private Map parameterMap = new HashMap<String, String>();
    private static final String ACTION_PARAMETER_VALUE = "action";

    public String generateRequestParameters() {
        parameterMap.clear();
        setAction();
        init();
        String requestParams = ACTION_PARAMETER_VALUE + "=" + action;
        Iterator<String> irt = parameterMap.keySet().iterator();
        while (irt.hasNext()) {
            String key = irt.next();
            requestParams = requestParams + "&" + key + "=" + parameterMap.get(key);
        }
        return requestParams;
    }

    public String generateRequestParameters(String actionName) {
        parameterMap.clear();
        setAction();
        init();
        String requestParams = ACTION_PARAMETER_VALUE + "=" + actionName;
        Iterator<String> irt = parameterMap.keySet().iterator();
        while (irt.hasNext()) {
            String key = irt.next();
            requestParams = requestParams + "&" + key + "=" + parameterMap.get(key);
        }
        return requestParams;
    }

    public void addParameter(String key, String value) {
        parameterMap.put(key, value);
    }

    public abstract void setAction();

    public abstract void init();

    public void setAction(String actionName) {
        this.action = actionName;
    }
}
