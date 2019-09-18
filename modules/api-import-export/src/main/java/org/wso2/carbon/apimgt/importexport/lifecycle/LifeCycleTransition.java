/*
 *
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.wso2.carbon.apimgt.importexport.lifecycle;

import java.util.HashMap;

/**
 * This class represents lifecycle transition
 */
public class LifeCycleTransition {
    private HashMap<String, String> transitions;

    /**
     * Initialize class
     */
    public LifeCycleTransition() {
        this.transitions = new HashMap<String, String>();
    }

    /**
     * Returns action required to transit to state
     * @param state State to get action
     * @return lifecycle action associated or null if not found
     */
    public String getAction(String state){
        if (!transitions.containsKey(state)){
            return null;
        }
        return transitions.get(state);
    }

    /**
     * Adds a transition
     * @param targetStatus target status
     * @param action action associated with target
     */
    public void addTransition(String targetStatus, String action) {
        transitions.put(targetStatus, action);
    }
}
