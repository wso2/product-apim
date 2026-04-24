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

package org.wso2.am.integration.tests.apimGovernance;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.governance.ApiResponse;
import org.wso2.am.integration.clients.governance.api.dto.ArtifactComplianceDetailsDTO;
import org.wso2.am.integration.clients.governance.api.dto.ArtifactInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class contains the test cases for MCP Server compliance.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class MCPComplianceTestCase extends APIMIntegrationBaseTest {

    private static final String OAS_FILE = "petstore-oas3.json";
    private static final String ADDITIONAL_PROPS_FILE = "petstore-oas3-mcp-additional-props.json";
    private static final String REST_BACKEND_CONTEXT = "response";
    private static final String URL_KEY = "url";
    private static final String PROD_ENDPOINTS = "production_endpoints";
    private static final String SANDBOX_ENDPOINTS = "sandbox_endpoints";
    private static final String ENDPOINT_TYPE = "endpoint_type";
    private static final long COMPLIANCE_TIMEOUT_MILLIS = 180000L;
    private static final long COMPLIANCE_POLL_INTERVAL_MILLIS = 5000L;

    private String mcpTestResourcePath;
    private String createdMCPServerId;

    @Factory(dataProvider = "userModeDataProvider")
    public MCPComplianceTestCase(TestUserMode userMode) {

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

        super.init(userMode);
        mcpTestResourcePath = TestConfigurationProvider.getResourceLocation() + "mcp" + File.separator;
    }

    /**
     * This test case is to test compliance details of an MCP Server after MCP Server creation with default policy.
     *
     * @throws Exception if an error occurs while retrieving the compliance details
     */
    @Test(groups = {"wso2.am"},
            description = "Test compliance details of an MCP Server after MCP Server creation with default policy")
    public void testComplianceDetailsOfMCPServerAfterCreateWithDefaultPolicy() throws Exception {

        File openApiDefFile = resolveExistingFile(mcpTestResourcePath, OAS_FILE);
        JSONObject additionalPropsObj = new JSONObject(readFileContent(mcpTestResourcePath, ADDITIONAL_PROPS_FILE));
        additionalPropsObj.put("endpointConfig", buildEndpointConfigJson(getGatewayURLNhttps() + REST_BACKEND_CONTEXT));

        MCPServerDTO mcpServer = restAPIPublisher.createMCPServerFromOpenAPI(openApiDefFile,
                additionalPropsObj.toString());
        assertNotNull(mcpServer, "MCP Server response is null");
        assertNotNull(mcpServer.getId(), "MCP Server ID is null");
        createdMCPServerId = mcpServer.getId();

        ArtifactComplianceDetailsDTO complianceDetails = waitForComplianceDetails(createdMCPServerId);
        assertNotNull(complianceDetails, "Cannot retrieve compliance details of the MCP Server");
        assertEquals(complianceDetails.getId(), createdMCPServerId,
                "Compliance details returned for an unexpected artifact");
        assertNotNull(complianceDetails.getInfo(), "Artifact information is missing in compliance details");
        assertEquals(complianceDetails.getInfo().getType(), ArtifactInfoDTO.TypeEnum.API,
                "Unexpected artifact type in compliance details");
        assertEquals(complianceDetails.getInfo().getExtendedType(), ArtifactInfoDTO.ExtendedTypeEnum.MCP,
                "Unexpected extended artifact type in compliance details");
        assertEquals(complianceDetails.getInfo().getName(), mcpServer.getName(),
                "Unexpected artifact name in compliance details");
        assertEquals(complianceDetails.getInfo().getVersion(), mcpServer.getVersion(),
                "Unexpected artifact version in compliance details");
        assertNotNull(complianceDetails.getStatus(), "Compliance status is missing for the MCP Server");
        assertTrue(complianceDetails.getStatus() != ArtifactComplianceDetailsDTO.StatusEnum.PENDING,
                "MCP Server compliance is still pending");
        assertTrue(complianceDetails.getGovernedPolicies() != null
                        && !complianceDetails.getGovernedPolicies().isEmpty(),
                "No governed policies found for the MCP Server");
    }

    private ArtifactComplianceDetailsDTO waitForComplianceDetails(String artifactId) throws Exception {

        long deadline = System.currentTimeMillis() + COMPLIANCE_TIMEOUT_MILLIS;
        ApiResponse<ArtifactComplianceDetailsDTO> latestResponse = null;
        ArtifactComplianceDetailsDTO latestDetails = null;

        while (System.currentTimeMillis() < deadline) {
            latestResponse = restAPIGovernance.getAPICompliance(artifactId);
            if (latestResponse.getStatusCode() == Response.Status.OK.getStatusCode()) {
                latestDetails = latestResponse.getData();
                if (latestDetails != null
                        && latestDetails.getStatus() != ArtifactComplianceDetailsDTO.StatusEnum.PENDING
                        && latestDetails.getGovernedPolicies() != null
                        && !latestDetails.getGovernedPolicies().isEmpty()) {
                    return latestDetails;
                }
            }
            Thread.sleep(COMPLIANCE_POLL_INTERVAL_MILLIS);
        }

        assertNotNull(latestResponse, "Compliance endpoint did not return a response");
        if (latestDetails == null) {
            assertEquals(latestResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                    "Timed out waiting for a successful compliance response for the MCP Server");
        }
        assertNotNull(latestDetails, "Compliance endpoint returned empty compliance details");
        return latestDetails;
    }

    private static JSONObject buildEndpointConfigJson(String url) throws JSONException {

        JSONObject endpointConfig = new JSONObject();
        JSONObject endpoints = new JSONObject().put(URL_KEY, url);
        endpointConfig.put(PROD_ENDPOINTS, endpoints);
        endpointConfig.put(SANDBOX_ENDPOINTS, endpoints);
        endpointConfig.put(ENDPOINT_TYPE, "http");
        return endpointConfig;
    }

    private static String readFileContent(String baseDir, String fileName) throws IOException {

        Path path = Paths.get(baseDir, fileName);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + path.toAbsolutePath());
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static File resolveExistingFile(String baseDir, String fileName) {

        File file = new File(baseDir, fileName);
        assertTrue(file.exists(), "File not found: " + file.getAbsolutePath());
        return file;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        try {
            if (createdMCPServerId != null) {
                restAPIPublisher.deleteMCPServer(createdMCPServerId);
            }
        } finally {
            super.cleanUp();
        }
    }
}
