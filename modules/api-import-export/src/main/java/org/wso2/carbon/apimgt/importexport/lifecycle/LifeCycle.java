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
 * Represent lifecycle of an API
 */
public class LifeCycle {
    private HashMap<String, LifeCycleTransition> stateHashMap;

    /**
     * Initialize lifecycle
     */
    public LifeCycle() {
        this.stateHashMap = new HashMap<String, LifeCycleTransition>();
    }

    /**
     * Adds a state for a lifecycle
     * @param state State to be added
     * @param transition Transition associated with state
     */
    public void addLifeCycleState(String state, LifeCycleTransition transition){
        stateHashMap.put(state, transition);
    }

    /**
     * Returns the transition associated with state
     * @param state State to get transitions
     * @return Transition associated with state
     */
    public LifeCycleTransition getTransition(String state){
        if (!stateHashMap.containsKey(state)){
            return null;
        }
        return stateHashMap.get(state);
    }
}
