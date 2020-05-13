/*
 *  * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 */

package org.wso2.am.scenario.test.common.swagger;

import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;

import java.util.HashMap;
import java.util.Map;

public class TypeProperties {
    Map<String, Property> collection = new HashMap<>();

    public void addStringProperty(String name) {
        StringProperty property = new StringProperty();
        collection.put(name, property);
    }

    public void addIntegerProperty(String name) {
        IntegerProperty property = new IntegerProperty();
        collection.put(name, property);
    }

    public void addDoubleProperty(String name) {
        DoubleProperty property = new DoubleProperty();
        collection.put(name, property);
    }
}
