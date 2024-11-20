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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OpenAPIDefinitionValidationResponseDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class OASTestCase extends APIMIntegrationBaseTest {

    private String apiId;
    private String apiImportId;
    private String invalidApiImportId;
    private String resourcePath;
    private String oasVersion;
    private final static String OAS_V2 = "v2";
    private final static String OAS_V3 = "v3";
    private final static String SERVER_BLOCK = "https://test-unsupported.com";
    @Factory(dataProvider = "userModeDataProvider")
    public OASTestCase(TestUserMode userMode, String oasVersion) {
        this.userMode = userMode;
        this.oasVersion = oasVersion;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        if(OAS_V2.equalsIgnoreCase(oasVersion)) {
            resourcePath = "oas" + File.separator + "v2" + File.separator;
        } else {
            resourcePath = "oas" + File.separator + "v3" + File.separator;
        }
    }

    @Test(groups = { "wso2.am" }, description = "API creation")
    public void testNewAPI() throws Exception {
        String apiData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream( resourcePath + "apiData.json"), "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apidto = objectMapper.readValue(apiData, APIDTO.class);
        apidto.setProvider(user.getUserName());
        APIDTO responseApiDto = restAPIPublisher.addAPI(apidto, oasVersion);
        apiId = responseApiDto.getId();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);

        testUpdatedAPIDefinitionInPublisher(apidto, oasVersion);
        testUpdatedAPIDefinitionInStore(apidto, oasVersion);
    }

    @Test(groups = { "wso2.am" }, description = "API update", dependsOnMethods = "testNewAPI")
    public void testAPIUpdate() throws Exception {
        String updatedAPIData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "apiUpdateData.json"),
                "UTF-8");

        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apidto = objectMapper.readValue(updatedAPIData, APIDTO.class);
        apidto.setProvider(user.getUserName());
        APIDTO updatedApidto = restAPIPublisher.updateAPI(apidto, apiId);

        testUpdatedAPIDefinitionInPublisher(apidto, oasVersion);
        testUpdatedAPIDefinitionInStore(apidto, oasVersion);
    }

    @Test(groups = { "wso2.am" }, description = "API definition update", dependsOnMethods = "testAPIUpdate")
    public void testAPIDefinitionUpdate() throws Exception {
        String originalStoreDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas.json"),
                "UTF-8");
        String originalPublisherDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_publisher.json"),
                "UTF-8");
        String responseDefinition = restAPIPublisher.updateSwagger(apiId, originalPublisherDefinition);

        APIDTO apidto = restAPIPublisher.getAPIByID(apiId, user.getUserDomain());
        Assert.assertNotNull(apidto);

        String storeDefinition = restAPIStore.getSwaggerByID(apiId, user.getUserDomain());
        String publisherDefinition = restAPIPublisher.getSwaggerByID(apiId);

        validateDefinition(publisherDefinition);
        validateDefinition(responseDefinition);
        validateDefinition(storeDefinition);

        if (OAS_V2.equalsIgnoreCase(oasVersion)) {
            OAS2Utils.validateSwaggerDataInPublisher(apidto, responseDefinition);
            OAS2Utils.validateSwaggerDataInPublisher(apidto, publisherDefinition);
            OAS2Utils.validateSwaggerDataInStore(storeDefinition);

            OAS2Utils.validateUpdatedDefinition(originalPublisherDefinition, publisherDefinition);
            OAS2Utils.validateUpdatedDefinition(originalPublisherDefinition, responseDefinition);
            OAS2Utils.validateUpdatedDefinition(originalStoreDefinition, storeDefinition);
            OAS2Utils.validateUpdatedDefinition(originalPublisherDefinition, apidto);
        } else {
            OAS3Utils.validateSwaggerDataInPublisher(apidto, responseDefinition);
            OAS3Utils.validateSwaggerDataInPublisher(apidto, publisherDefinition);
            OAS3Utils.validateSwaggerDataInStore(storeDefinition);

            OAS3Utils.validateUpdatedDefinition(originalPublisherDefinition, publisherDefinition);
            OAS3Utils.validateUpdatedDefinition(originalPublisherDefinition, responseDefinition);
            OAS3Utils.validateUpdatedDefinition(originalStoreDefinition, storeDefinition);
            OAS3Utils.validateUpdatedDefinition(originalPublisherDefinition, apidto);
        }
    }

    @Test(groups = { "wso2.am" }, description = "API definition update with advance configs",
            dependsOnMethods = "testAPIDefinitionUpdate")
    public void testAddAdvanceConfigsToAPIDefinition() throws Exception {
        String apiDefinitionInPublisher = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_with_advance_configs.json"),
                "UTF-8");
        String addedAdvanceEndpointConfigsSandbox = "{\"circuitBreakers\":{\"maxRetries\":4.0,\"maxConnectionPools\"" +
                ":2048.0,\"maxRequests\":100.0,\"maxPendingRequests\":25.0,\"maxConnections\":2048.0}}" ;
        String addedAdvanceEndpointConfigsProduction = "{\"circuitBreakers\":{\"maxRetries\":3.0,\"maxConnectionPools" +
                "\":1024.0,\"maxRequests\":75.0,\"maxPendingRequests\":35.0,\"maxConnections\":1024.0}}";
        restAPIPublisher.updateSwagger(apiId, apiDefinitionInPublisher);

        APIDTO apidto = restAPIPublisher.getAPIByID(apiId, user.getUserDomain());
        Assert.assertNotNull(apidto);

        LinkedTreeMap endpointConfiglinkedTreeMap = (LinkedTreeMap) apidto.getEndpointConfig();
        Gson gson = new Gson();

        JsonObject advanceConfigsObject = gson.toJsonTree(((LinkedTreeMap)endpointConfiglinkedTreeMap
                .get("sandbox_endpoints")).get("advanceEndpointConfig")).getAsJsonObject();

        //added advance configs should be there in the updated endpointConfigs string
        Assert.assertEquals(advanceConfigsObject.toString(), addedAdvanceEndpointConfigsSandbox);

        advanceConfigsObject = gson.toJsonTree(((LinkedTreeMap)endpointConfiglinkedTreeMap
                .get("production_endpoints")).get("advanceEndpointConfig")).getAsJsonObject();

        //added advance configs should be there in the updated endpointConfigs string
        Assert.assertEquals(advanceConfigsObject.toString(), addedAdvanceEndpointConfigsProduction);

    }

    @Test(groups = { "wso2.am" }, description = "API definition import", dependsOnMethods =
            "testAddAdvanceConfigsToAPIDefinition")
    public void testAPIDefinitionImport() throws Exception {
        String originalDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_import.json"),
                "UTF-8");
        String originalPublisherDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_import_publisher.json"),
                "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "additionalProperties.json"),
                "UTF-8");
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
        additionalPropertiesObj.put("provider", user.getUserName());

        File file = geTempFileWithContent(originalDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        apiImportId = apidto.getId();

        restAPIPublisher.changeAPILifeCycleStatus(apiImportId, Constants.PUBLISHED);

        String storeDefinition = restAPIStore.getSwaggerByID(apiImportId, user.getUserDomain());
        String publisherDefinition = restAPIPublisher.getSwaggerByID(apiImportId);

        validateDefinition(publisherDefinition);
        validateDefinition(storeDefinition);

        if (OAS_V2.equalsIgnoreCase(oasVersion)) {
            OAS2Utils.validateSwaggerDataInPublisher(apidto, publisherDefinition);
            OAS2Utils.validateSwaggerDataInStore(storeDefinition);

            OAS2Utils.validateUpdatedDefinition(originalPublisherDefinition, publisherDefinition);
            OAS2Utils.validateUpdatedDefinition(originalDefinition, storeDefinition);
            OAS2Utils.validateUpdatedDefinition(originalDefinition, apidto);
        } else {
            OAS3Utils.validateSwaggerDataInPublisher(apidto, publisherDefinition);
            OAS3Utils.validateSwaggerDataInStore(storeDefinition);

            OAS3Utils.validateUpdatedDefinition(originalPublisherDefinition, publisherDefinition);
            OAS3Utils.validateUpdatedDefinition(originalDefinition, storeDefinition);
            OAS3Utils.validateUpdatedDefinition(originalDefinition, apidto);
        }
    }

    @Test(groups = { "wso2.am" }, description = "Validate API definition with unsupported server blocks import",
            dependsOnMethods = "testAPIDefinitionImport")
    public void testAPIDefinitionWithUnsupportedServerBlocksImport() throws Exception {
        if (oasVersion.equals(OAS_V3)) {
            testDeleteApi(apiImportId);
            String definition = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_with_unsupported_servers_block.json"),
                    "UTF-8");
            String additionalProperties = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream(resourcePath + "additionalProperties.json"),
                    "UTF-8");
            JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
            additionalPropertiesObj.put("provider", user.getUserName());

            File file = geTempFileWithContent(definition);
            APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
            apiImportId = apidto.getId();

            restAPIPublisher.changeAPILifeCycleStatus(apiImportId, Constants.PUBLISHED);

            String storeDefinition = restAPIStore.getSwaggerByID(apiImportId, user.getUserDomain());
            String publisherDefinition = restAPIPublisher.getSwaggerByID(apiImportId);
            Assert.assertFalse(storeDefinition.contains(SERVER_BLOCK));
            Assert.assertFalse(publisherDefinition.contains(SERVER_BLOCK));
        }
    }

    @Test(groups = { "wso2.am" }, description = "Validate API definitions with empty resource paths",
            dependsOnMethods = "testNewAPI")
    public void testValidateAPIDefinitionWithEmptyResourcePath() throws Exception {
        String invalidDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_invalid_import.json"),
                "UTF-8");

        File file = geTempFileWithContent(invalidDefinition);
        OpenAPIDefinitionValidationResponseDTO responseDTO = restAPIPublisher.validateOASDefinition(file);
        Assert.assertFalse(responseDTO.isIsValid());
    }

    @Test(groups = { "wso2.am" }, description = "Import API definition with empty resource paths",
            dependsOnMethods = "testValidateAPIDefinitionWithEmptyResourcePath")
    public void testAPIDefinitionImportWithEmptyResourcePath() throws Exception {
        String invalidDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_invalid_import.json"),
                "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "additionalProperties.json"),
                "UTF-8");
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
        additionalPropertiesObj.put("provider", user.getUserName());

        File file = geTempFileWithContent(invalidDefinition);
        try {
            APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
            invalidApiImportId = apidto.getId();
            Assert.fail("API definition import should fail with empty resource path");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
        }
    }

    @Test(groups = { "wso2.am" }, description = "Update API definition with empty resource paths",
            dependsOnMethods = { "testValidateAPIDefinitionWithEmptyResourcePath", "testAPIDefinitionUpdate" })
    public void testAPIDefinitionUpdateWithEmptyResourcePath() throws Exception {
        String invalidDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_invalid_update.json"),
                "UTF-8");
        String originalPublisherDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_publisher.json"),
                "UTF-8");

        try {
            restAPIPublisher.updateSwagger(apiId, invalidDefinition);
            Assert.fail("API definition import should fail with empty resource path");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
        }

        String publisherDefinition = restAPIPublisher.getSwaggerByID(apiId);
        if (OAS_V2.equalsIgnoreCase(oasVersion)) {
            OAS2Utils.validateUpdatedDefinition(originalPublisherDefinition, publisherDefinition);
        } else {
            OAS3Utils.validateUpdatedDefinition(originalPublisherDefinition, publisherDefinition);
        }
    }

    private void testUpdatedAPIDefinitionInPublisher(APIDTO apidto, String oasVersion) throws Exception {
        String oasDefinition = restAPIPublisher.getSwaggerByID(apiId);
        validateDefinition(oasDefinition);

        if (OAS_V2.equalsIgnoreCase(oasVersion)) {
            OAS2Utils.validateOperationCount(apidto, oasDefinition);
            OAS2Utils.validateResourcesOfOASDefinition(apidto, oasDefinition);
            OAS2Utils.validateSwaggerDataInPublisher(apidto, oasDefinition);
        } else {
            OAS3Utils.validateOperationCount(apidto, oasDefinition);
            OAS3Utils.validateResourcesOfOASDefinition(apidto, oasDefinition);
            OAS3Utils.validateSwaggerDataInPublisher(apidto, oasDefinition);
        }
    }

    private void testUpdatedAPIDefinitionInStore(APIDTO apidto, String oasVersion) throws Exception {

        String oasDefinition = restAPIStore.getSwaggerByID(apiId, user.getUserDomain());
        validateDefinition(oasDefinition);

        if (OAS_V2.equalsIgnoreCase(oasVersion)) {
            OAS2Utils.validateOperationCount(apidto, oasDefinition);
            OAS2Utils.validateResourcesOfOASDefinition(apidto, oasDefinition);
            OAS2Utils.validateSwaggerDataInStore(oasDefinition);
        } else {
            OAS3Utils.validateOperationCount(apidto, oasDefinition);
            OAS3Utils.validateResourcesOfOASDefinition(apidto, oasDefinition);
            OAS3Utils.validateSwaggerDataInStore(oasDefinition);
        }
    }

    private void validateDefinition(String oasDefinition) throws Exception {
        File file = geTempFileWithContent(oasDefinition);
        OpenAPIDefinitionValidationResponseDTO responseDTO = restAPIPublisher.validateOASDefinition(file);
        Assert.assertTrue(responseDTO.isIsValid());
    }

    private File geTempFileWithContent(String swagger) throws Exception {
        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(swagger);
        out.close();
        return temp;
    }

    private void testDeleteApi(String apiId) throws Exception {
        if (apiId == null) {
            return;
        }
        restAPIPublisher.deleteAPI(apiId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        testDeleteApi(apiId);
        testDeleteApi(apiImportId);
        if (invalidApiImportId != null) {
            testDeleteApi(invalidApiImportId);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN, OAS_V2 },
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN, OAS_V3 },
                new Object[] { TestUserMode.TENANT_ADMIN, OAS_V2 },
                new Object[] { TestUserMode.TENANT_ADMIN, OAS_V3 },
        };
    }

}
