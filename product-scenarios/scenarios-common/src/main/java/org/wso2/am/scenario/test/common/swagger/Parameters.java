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

import io.swagger.models.ModelImpl;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;

import java.util.ArrayList;
import java.util.List;

public class Parameters {
    List<Parameter> collection = new ArrayList<>();

    public void addBodyParameter(String name, String description, boolean isRequired, TypeProperties properties) {
        BodyParameter body = new BodyParameter();
        body.setName(name);
        body.setDescription(description);
        body.setRequired(isRequired);
        body.setIn("body");

        ModelImpl model = new ModelImpl();
        model.setType("object");
        model.setProperties(properties.collection);
        body.setSchema(model);

        collection.add(body);
    }

    public void addQueryParameter(String name, String description, boolean isRequired) {
        QueryParameter query = new QueryParameter();
        query.setName(name);
        query.setDescription(description);
        query.setRequired(isRequired);
        query.setIn("query");

        collection.add(query);
    }

    public void addPathParameter(String name, String description, boolean isRequired) {
        PathParameter path = new PathParameter();
        path.setName(name);
        path.setDescription(description);
        path.setRequired(isRequired);
        path.setIn("path");

        collection.add(path);
    }
}
