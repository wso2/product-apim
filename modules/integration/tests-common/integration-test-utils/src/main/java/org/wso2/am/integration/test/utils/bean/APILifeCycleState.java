/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


/**
 * API life cycle state used too update API's in test cases
 */
public enum APILifeCycleState {
    PUBLISHED("Published"), CREATED("Created"), DEPRECATED("Deprecated"), BLOCKED("Blocked"), RETIRED(
            "Retired"), PROTOTYPED("Prototyped"), PROMOTED("Promoted"),

    //TODO: REMOVE ALL BELOW ONCE JAGGERY TESTS ARE COMPLETELY REMOVED
    PUBLISHED_JAG("PUBLISHED"), CREATED_JAG("CREATED"), DEPRECATED_JAG("DEPRECATED"), BLOCKED_JAG("BLOCKED"), RETIRED_JAG(
            "RETIRED"), PROTOTYPED_JAG("PROTOTYPED");
    private String state;

    APILifeCycleState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}