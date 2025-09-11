/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.mcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;

/**
 * AI API Test Case
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class CreateMCPServerTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(CreateMCPServerTestCase.class);
    private String mcpTestResourcePath;

    private final String openApiDefFile = "petstore-oas3.json";
    private final String additionalPropFile = "petstore-oas3-additional-props.json";

    private String mcpserverId;

    @Factory(dataProvider = "userModeDataProvider")
    public CreateMCPServerTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN},
                {TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        mcpTestResourcePath = TestConfigurationProvider.getResourceLocation() + "mcp" + File.separator;
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Test creating MCP server using OpenAPI definition")
    public void createMCPServerUsingOpenAPIDefinition() throws Exception {

        String originalDefinition = readFile(mcpTestResourcePath + openApiDefFile);
        String additionalProperties = readFile(mcpTestResourcePath + additionalPropFile);
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        File file = getTempFileWithContent(originalDefinition);
        MCPServerDTO mcpServerDTO =
                restAPIPublisher.createMCPServerFromOpenAPI(file, additionalPropertiesObj.toString());

        mcpserverId = mcpServerDTO.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(mcpserverId);
        assertEquals(Response.Status.OK.getStatusCode(),
                createdApiResponse.getResponseCode(), "MCP server creation failed");
    }

//    @Test(groups = {"wso2.am"}, description = "Test creating MCP server using an API")
//    public void createMCPServerUsingAPI() throws Exception {
//
//        String originalDefinition = readFile(mcpTestResourcePath + openApiDefFile);
//        String additionalProperties = readFile(mcpTestResourcePath + additionalPropFile);
//        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
//
//        File file = getTempFileWithContent(originalDefinition);
//        MCPServerDTO mcpServerDTO =
//                restAPIPublisher.createMCPServerFromOpenAPI(file, additionalPropertiesObj.toString());
//
//        mcpserverId = mcpServerDTO.getId();
//
//        HttpResponse createdApiResponse = restAPIPublisher.getAPI(mcpserverId);
//        assertEquals(Response.Status.OK.getStatusCode(),
//                createdApiResponse.getResponseCode(), "MCP server creation failed");
//    }

    private File getTempFileWithContent(String content) throws IOException {

        File temp = File.createTempFile("swagger", ".json");
        try (BufferedWriter out = new BufferedWriter(new FileWriter(temp))) {
            out.write(content);
        }
        temp.deleteOnExit();
        return temp;
    }
}
