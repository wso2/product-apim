/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.oas;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.junit.Assert;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;

import java.util.List;
import java.util.Map;

public class OAS2Utils {

    public static void validateOperationCount(APIDTO apidto, String swagger) throws Exception {
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger swaggerObj = swaggerParser.parse(swagger);
        int count = 0;
        for (Map.Entry<String, Path> entry : swaggerObj.getPaths().entrySet()) {
            Path path = entry.getValue();
            count += path.getOperations().size();
        }
        Assert.assertEquals(apidto.getOperations().size(), count);
    }

    public static void validateSwaggerDataInPublisher(APIDTO apidto, String swagger) throws Exception {
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger swaggerObj = swaggerParser.parse(swagger);
        Map<String, Object> extensions = swaggerObj.getVendorExtensions();
        OASBaseUtils.validateSwaggerExtensionDataInPublisher(extensions, apidto);
    }

    public static void validateSwaggerDataInStore(String swagger) {
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger swaggerObj = swaggerParser.parse(swagger);
        Map<String, Object> extensions = swaggerObj.getVendorExtensions();
        if (extensions != null) {
            Assert.assertFalse(extensions.containsKey("x-wso2-auth-header"));
            Assert.assertFalse(extensions.containsKey("x-throttling-tier"));
            Assert.assertFalse(extensions.containsKey("x-wso2-cors"));
            Assert.assertFalse(extensions.containsKey("x-wso2-production-endpoints"));
            Assert.assertFalse(extensions.containsKey("x-wso2-sandbox-endpoints"));
            Assert.assertFalse(extensions.containsKey("x-wso2-transports"));
        }

        Map<String, Path> paths = swaggerObj.getPaths();
        Assert.assertNotNull(paths);
        for (String pathKey : paths.keySet()) {
            Path path = paths.get(pathKey);
            Map<HttpMethod, Operation> operationMap = path.getOperationMap();
            for (Map.Entry<HttpMethod, Operation> entry : operationMap.entrySet()) {
                Operation operation = entry.getValue();
                Map<String, Object> opsExMap = operation.getVendorExtensions();
                if (operationMap != null) {
                    Assert.assertFalse(opsExMap.containsKey("x-mediation-script"));
                }
            }
        }
    }

    public static void validateResourcesOfOASDefinition(APIDTO apidto, String swagger) {
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger swaggerObj = swaggerParser.parse(swagger);
        Assert.assertNotNull(swaggerObj);
        for (APIOperationsDTO operation : apidto.getOperations()) {
            Map<String, Path> paths = swaggerObj.getPaths();
            Assert.assertNotNull(paths);
            Path path = paths.get(operation.getTarget());
            Assert.assertNotNull(path);
            Operation oasOperation = path.getOperationMap().get(HttpMethod.valueOf(operation.getVerb()));
            Assert.assertNotNull(oasOperation);
            for (String scope : operation.getScopes()) {
                boolean found = false;
                List<Map<String, List<String>>> securities = oasOperation.getSecurity();
                Assert.assertNotNull(securities);
                for (Map<String, List<String>> aSec : securities) {
                    List<String> defaultScopes = aSec.get("default");
                    if (defaultScopes != null) {
                        Assert.assertTrue(aSec.get("default").contains(scope));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Assert.fail("Security not found");
                }
            }
            Map<String, Object> ex = oasOperation.getVendorExtensions();
            Assert.assertNotNull(ex);
            Assert.assertEquals(operation.getAuthType(), ex.get("x-auth-type"));
            Assert.assertEquals(operation.getThrottlingPolicy(), ex.get("x-throttling-tier"));
        }
    }

    public static void validateUpdatedDefinition(String original, String updated) {
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger originalObj = swaggerParser.parse(original);
        Swagger updatedObj = swaggerParser.parse(updated);
        Assert.assertEquals(originalObj.getPaths().size(), updatedObj.getPaths().size());

        for (Map.Entry<String, Path> entry : originalObj.getPaths().entrySet()) {
            Path originalPath = entry.getValue();
            Path updatedPath = updatedObj.getPath(entry.getKey());
            Map<HttpMethod, Operation> originalOperationMap = originalPath.getOperationMap();
            Map<HttpMethod, Operation> updatedOperationMap = updatedPath.getOperationMap();
            Assert.assertEquals(originalOperationMap.size(), updatedOperationMap.size());

            for (Map.Entry<HttpMethod, Operation> opEntry : originalOperationMap.entrySet()) {
                Operation originalOperation = opEntry.getValue();
                Operation updatedOperation = updatedOperationMap.get(opEntry.getKey());
                Assert.assertEquals(originalOperation, updatedOperation);
            }
        }
    }

    public static void validateUpdatedDefinition(String original, APIDTO apidto) {
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger originalObj = swaggerParser.parse(original);
        Map<String, Map<String, APIOperationsDTO>> updatedPaths = OASBaseUtils.getMapFromDTO(apidto);
        Assert.assertEquals(originalObj.getPaths().size(), updatedPaths.size());

        for (Map.Entry<String, Path> entry : originalObj.getPaths().entrySet()) {
            Path originalPath = entry.getValue();
            Map<String, APIOperationsDTO> updatedOps = updatedPaths.get(entry.getKey());
            Map<HttpMethod, Operation> originalOperationMap = originalPath.getOperationMap();
            Assert.assertEquals(originalOperationMap.size(), updatedOps.size());

            for (Map.Entry<HttpMethod, Operation> opEntry : originalOperationMap.entrySet()) {
                Operation originalOperation = opEntry.getValue();
                APIOperationsDTO updatedOperation = updatedOps.get(opEntry.getKey().name());
                Assert.assertNotNull(updatedOperation);
                if (updatedOperation.getScopes() != null && !updatedOperation.getScopes().isEmpty()) {
                    Assert.assertEquals(originalOperation.getSecurity().get(0).get("default").get(0),
                            updatedOperation.getScopes().get(0));
                } else {
                    Assert.assertTrue(originalOperation.getSecurity().get(0).get("default").isEmpty());
                }
            }
        }
    }

}
