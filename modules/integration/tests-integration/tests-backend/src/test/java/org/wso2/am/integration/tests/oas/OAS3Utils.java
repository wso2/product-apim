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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.Assert;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;

import java.util.List;
import java.util.Map;

public class OAS3Utils {

    public static void validateSwaggerDataInPublisher(APIDTO apidto, String oasDefinition) throws Exception {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        SwaggerParseResult parseAttemptForV3 = openAPIV3Parser.readContents(oasDefinition, null, null);
        OpenAPI openAPI = parseAttemptForV3.getOpenAPI();
        Map<String, Object> extensions = openAPI.getExtensions();
        OASBaseUtils.validateSwaggerExtensionDataInPublisher(extensions, apidto);
    }

    public static void validateOperationCount(APIDTO apidto, String oasDefinition) {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        SwaggerParseResult parseAttemptForV3 = openAPIV3Parser.readContents(oasDefinition, null, null);
        OpenAPI openAPI = parseAttemptForV3.getOpenAPI();
        int count = 0;
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            PathItem path = entry.getValue();
            count += path.readOperations().size();
        }
        Assert.assertEquals(apidto.getOperations().size(), count);
    }

    public static void validateResourcesOfOASDefinition(APIDTO apidto, String oasDefinition) {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        SwaggerParseResult parseAttemptForV3 = openAPIV3Parser.readContents(oasDefinition, null, null);
        OpenAPI openAPI = parseAttemptForV3.getOpenAPI();
        Assert.assertNotNull(openAPI);
        for (APIOperationsDTO operation : apidto.getOperations()) {
            Map<String, PathItem> paths = openAPI.getPaths();
            Assert.assertNotNull(paths);
            PathItem path = paths.get(operation.getTarget());
            Assert.assertNotNull(path);
            Operation oasOperation = path.readOperationsMap().get(PathItem.HttpMethod.valueOf(operation.getVerb()));
            Assert.assertNotNull(oasOperation);
            for (String scope : operation.getScopes()) {
                boolean found = false;
                List<SecurityRequirement> securities = oasOperation.getSecurity();
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
            Map<String, Object> ex = oasOperation.getExtensions();
            Assert.assertNotNull(ex);
            Assert.assertEquals(operation.getAuthType(), ex.get("x-auth-type"));
            Assert.assertEquals(operation.getThrottlingPolicy(), ex.get("x-throttling-tier"));
        }
    }

    public static void validateSwaggerDataInStore(String oasDefinition) {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        SwaggerParseResult parseAttemptForV3 = openAPIV3Parser.readContents(oasDefinition, null, null);
        OpenAPI openAPI = parseAttemptForV3.getOpenAPI();
        Map<String, Object> extensions = openAPI.getExtensions();
        if (extensions != null) {
            Assert.assertFalse(extensions.containsKey("x-wso2-auth-header"));
            Assert.assertFalse(extensions.containsKey("x-throttling-tier"));
            Assert.assertFalse(extensions.containsKey("x-wso2-cors"));
            Assert.assertFalse(extensions.containsKey("x-wso2-production-endpoints"));
            Assert.assertFalse(extensions.containsKey("x-wso2-sandbox-endpoints"));
            Assert.assertFalse(extensions.containsKey("x-wso2-transports"));
        }

        Map<String, PathItem> paths = openAPI.getPaths();
        Assert.assertNotNull(paths);
        for (String pathKey : paths.keySet()) {
            PathItem path = paths.get(pathKey);
            Map<PathItem.HttpMethod, Operation> operationMap = path.readOperationsMap();
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : operationMap.entrySet()) {
                Operation operation = entry.getValue();
                Map<String, Object> opsExMap = operation.getExtensions();
                if (operationMap != null) {
                    Assert.assertFalse(opsExMap.containsKey("x-mediation-script"));
                }
            }
        }
    }

    public static void validateUpdatedDefinition(String originalDefinition, String publisherDefinition) {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        SwaggerParseResult parseAttemptForV3 = openAPIV3Parser.readContents(originalDefinition, null, null);
        OpenAPI originalObj = parseAttemptForV3.getOpenAPI();
        parseAttemptForV3 = openAPIV3Parser.readContents(publisherDefinition, null, null);
        OpenAPI updatedObj = parseAttemptForV3.getOpenAPI();

        Assert.assertEquals(originalObj.getPaths().size(), updatedObj.getPaths().size());

        for (Map.Entry<String, PathItem> entry : originalObj.getPaths().entrySet()) {
            PathItem originalPath = entry.getValue();
            PathItem updatedPath = updatedObj.getPaths().get(entry.getKey());
            Map<PathItem.HttpMethod, Operation> originalOperationMap = originalPath.readOperationsMap();
            Map<PathItem.HttpMethod, Operation> updatedOperationMap = updatedPath.readOperationsMap();
            Assert.assertEquals(originalOperationMap.size(), updatedOperationMap.size());

            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : originalOperationMap.entrySet()) {
                Operation originalOperation = opEntry.getValue();
                Operation updatedOperation = updatedOperationMap.get(opEntry.getKey());
                Assert.assertEquals(originalOperation, updatedOperation);
            }
        }
    }

    public static void validateUpdatedDefinition(String original, APIDTO apidto) {
        OpenAPIV3Parser openAPIV3Parser = new OpenAPIV3Parser();
        SwaggerParseResult parseAttemptForV3 = openAPIV3Parser.readContents(original, null, null);
        OpenAPI originalObj = parseAttemptForV3.getOpenAPI();

        Map<String, Map<String, APIOperationsDTO>> updatedPaths = OASBaseUtils.getMapFromDTO(apidto);
        Assert.assertEquals(originalObj.getPaths().size(), updatedPaths.size());

        for (Map.Entry<String, PathItem> entry : originalObj.getPaths().entrySet()) {
            PathItem originalPath = entry.getValue();
            Map<String, APIOperationsDTO> updatedOps = updatedPaths.get(entry.getKey());
            Map<PathItem.HttpMethod, Operation> originalOperationMap = originalPath.readOperationsMap();
            Assert.assertEquals(originalOperationMap.size(), updatedOps.size());

            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : originalOperationMap.entrySet()) {
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
