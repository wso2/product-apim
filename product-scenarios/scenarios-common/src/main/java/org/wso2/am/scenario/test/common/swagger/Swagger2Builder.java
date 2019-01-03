/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.scenario.test.common.swagger;

import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

import java.util.List;
import java.util.Map;

public class Swagger2Builder {
    private Swagger swagger = new Swagger();

    public Swagger2Builder(String apiName, String apiVersion) {
        Info info = new Info();
        info.setTitle(apiName);
        info.setVersion(apiVersion);
        swagger.setInfo(info);
    }

    public void createResourcePaths(ResourcePaths resourcePaths) {
        for (Map.Entry<String, List<ResourcePaths.Element>> resourcePath : resourcePaths.collection.entrySet()) {
            String pathName = resourcePath.getKey();
            List<ResourcePaths.Element> resourceElements = resourcePath.getValue();

            Path path = new Path();

            for (ResourcePaths.Element resourceElement : resourceElements) {
                Operation operation = new Operation();

                if (resourceElement.parameters != null) {
                    operation.setParameters(resourceElement.parameters.collection);
                }

                operation.setResponses(resourceElement.responses.collection);

                switch (resourceElement.httpMethod) {
                    case GET:
                        path.setGet(operation);
                        break;
                    case POST:
                        path.setPost(operation);
                        break;
                    case PUT:
                        path.setPut(operation);
                        break;
                    case DELETE:
                        path.setDelete(operation);
                        break;
                    case PATCH:
                        path.setPatch(operation);
                        break;
                    case HEAD:
                        path.setHead(operation);
                        break;
                    case OPTIONS:
                        path.setOptions(operation);
                        break;
                    default:
                        break;
                }
            }

            swagger.path(pathName, path);
        }
    }

    public String getSwaggerJSON() {
        return Json.pretty(swagger);
    }
}
