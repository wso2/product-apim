/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.operationPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDataDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import wiremock.com.google.common.io.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class OperationPolicyTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(OperationPolicyTestCase.class);

    private final String API_NAME = "AddNewPolicyAndInvokeAPITest";
    private final String API_CONTEXT = "AddNewPolicyAndInvokeAPI";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";
    private final String POLICY_TYPE_COMMON = "common";
    private final String POLICY_WITH_SECRETS = "AddSecretHeaders";

    private final String TEST_POLICY_NAME = "customCommonLogPolicy";

    private final String TEST_JSON_POLICY_NAME = "customCommonLogJSONPolicy";

    private final String JSON_POLICY_TYPE = "JSON";

    private final String YAML_POLICY_TYPE = "YAML";
    private final String TEST_INVALID_POLICY_NAME = "customCommonLogPolicyInvalid";

    private final String TEST_POLICY_VERSION = "v1";

    private final int TEST_POLICY_LIMIT = 100;
    private final char[] PUBLISHER_USER_PASS = "pass@123".toCharArray();
    private String publisherUser = "importExportPublisher";

    private String applicationId;
    private String apiId;
    private String freshApiId;
    private String newVersionAPIId;
    private String newVersionIdOfAPIWithSecretPolicy;
    private String accessToken;
    private Map<String, String> policyMap;

    private File exportedOperationPolicyZip;
    private String exportUrl;
    private RestAPIPublisherImpl restAPIPublisherExport;

    @BeforeClass(alwaysRun = true) public void initialize() throws Exception {

        super.init();
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
        policyMap = restAPIPublisher.getAllCommonOperationPolicies();

        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        exportUrl = publisherURLHttps + APIMIntegrationConstants.REST_API_PUBLISHER_CONTEXT_FULL
                + APIMIntegrationConstants.REST_API_PUBLISHER_EXPORT_COMMON_POLICIES_API_RESOURCE;
        restAPIPublisherExport = new RestAPIPublisherImpl("admin", "admin", "carbon.super", publisherURLHttps);
    }

    @Test(groups = {"wso2.am"}, description = "Add common operation policy")
    public void testAddNewCommonOperationPolicy() throws Exception {

        HttpResponse addPolicyResponse = addPolicy(null, "customCommonLogPolicy.json", "customCommonLogPolicy.j2");

        assertNotNull(addPolicyResponse, "Error adding operation policy customCommonLogPolicy");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();
        Assert.assertNotNull(updatedCommonPolicyMap.get("customCommonLogPolicy"),
                "Unable to find the newly added common policy");
        policyMap.put("customCommonLogPolicy", newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Exporting Sample Common API Policy", dependsOnMethods = "testAddNewCommonOperationPolicy")
    public void testCommonOperationPolicyExport() throws Exception {

        exportedOperationPolicyZip = exportCommonOperationPolicyArtifact(TEST_POLICY_NAME, TEST_POLICY_VERSION, YAML_POLICY_TYPE, true);

        String extractedCommonAPIPolicyDir = exportedOperationPolicyZip.getParent();
        try {
            ZipFile zipFile = new ZipFile(exportedOperationPolicyZip);
            zipFile.extractAll(extractedCommonAPIPolicyDir);
        } catch (ZipException e) {
            throw new APIManagerIntegrationTestException("Error in extracting the exported API archive.", e);
        }

        String yamlPolicySpecPath = extractedCommonAPIPolicyDir + File.separator + TEST_POLICY_NAME + File.separator +
                TEST_POLICY_NAME + ".yaml";
        File policySpecFile = new File(yamlPolicySpecPath);

        assertTrue(policySpecFile.exists(), "API Policy Specification file does not exist");

        String synapseDefinitionPath = extractedCommonAPIPolicyDir + File.separator +  TEST_POLICY_NAME +
                File.separator +  TEST_POLICY_NAME + ".j2";
        File synapseDefFile = new File(synapseDefinitionPath);

        assertTrue(synapseDefFile.exists(), "Synapse Definition file does not exist");

        StringBuilder contentBuilderForPolicySpec = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(yamlPolicySpecPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilderForPolicySpec.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException("Error in reading from extracted api file " + yamlPolicySpecPath, e);
        }

        String policySpecContent = contentBuilderForPolicySpec.toString();

        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object operationPolicySpec = yamlReader.readValue(policySpecContent, Object.class);

        String expectedPolicySpecPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + TEST_POLICY_NAME + ".yaml";

        StringBuilder contentBuilderForExpectedPolicySpec = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(expectedPolicySpecPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilderForExpectedPolicySpec.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException("Error in reading from extracted api file " + expectedPolicySpecPath, e);
        }

        String expectedPolicySpecContent = contentBuilderForExpectedPolicySpec.toString();

        yamlReader = new ObjectMapper(new YAMLFactory());
        Object expectedOperationPolicySpec = yamlReader.readValue(expectedPolicySpecContent, Object.class);

        assertEquals(operationPolicySpec, expectedOperationPolicySpec, "Exported & Expected Policy Specifications are not matching");

        StringBuilder contentBuilderForSynapseDef = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(synapseDefinitionPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilderForSynapseDef.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException("Error in reading from extracted api file " + synapseDefinitionPath, e);
        }

        String synapseDefContent = contentBuilderForSynapseDef.toString();

        yamlReader = new ObjectMapper(new YAMLFactory());
        Object synapseDefinition = yamlReader.readValue(synapseDefContent, Object.class);

        String expectedSynapsePolicyDefPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + TEST_POLICY_NAME + ".j2";

        StringBuilder contentBuilderForExpectedSynapseDef = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(expectedSynapsePolicyDefPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilderForExpectedSynapseDef.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException("Error in reading from extracted api file " + expectedSynapsePolicyDefPath, e);
        }

        String expectedSynapseDefinitionContent = contentBuilderForExpectedSynapseDef.toString();

        yamlReader = new ObjectMapper(new YAMLFactory());
        Object expectedSynapseDefinition = yamlReader.readValue(expectedSynapseDefinitionContent, Object.class);

        assertEquals(synapseDefinition, expectedSynapseDefinition, "Exported & Expected Synapse Definitions are not matching");

    }

    @Test(groups = {"wso2.am"}, description = "Exporting Non Existing Common API Policy", dependsOnMethods = "testCommonOperationPolicyExport")
    public void testNonExistingCommonOperationPolicyExport() throws Exception {

        exportCommonOperationPolicyArtifact(TEST_INVALID_POLICY_NAME, TEST_POLICY_VERSION, YAML_POLICY_TYPE, false);

    }

    @Test(groups = {"wso2.am"}, description = "Delete common operation policy", dependsOnMethods = "testNonExistingCommonOperationPolicyExport")
    public void testDeleteCommonOperationPolicy() throws Exception {

        int responseCode = deleteOperationPolicy(policyMap.get("customCommonLogPolicy"), null);
        assertEquals(responseCode, 200);
        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();
        Assert.assertNull(updatedCommonPolicyMap.get("customCommonLogPolicy"));
        policyMap.remove("customCommonLogPolicy");
    }

    @Test(groups = {"wso2.am"}, description = "Import common operation policy", dependsOnMethods = {"testDeleteCommonOperationPolicy"} )
    public void testImportNewCommonOperationPolicy() throws Exception {

        ApiResponse importPolicyResponse = restAPIPublisher.importOperationPolicy(exportedOperationPolicyZip);

        assertNotNull(importPolicyResponse, "Error adding operation policy customCommonLogPolicy");

        assertEquals(importPolicyResponse.getStatusCode(), HttpStatus.SC_CREATED, "Response code mismatched");

        Map<String, String> commonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies(TEST_POLICY_LIMIT);
        Assert.assertNotNull(commonPolicyMap.get("customCommonLogPolicy"),
                "Unable to find the newly added common policy");
        policyMap.put("customCommonLogPolicy", commonPolicyMap.get("customCommonLogPolicy"));
    }

    @Test(groups = {"wso2.am"}, description = "Import existing common operation policy", dependsOnMethods = {"testImportNewCommonOperationPolicy"} )
    public void testImportExistingCommonOperationPolicy() {
        try{
            restAPIPublisher.importOperationPolicy(exportedOperationPolicyZip);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), HttpStatus.SC_CONFLICT, "Response code mismatched");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Import invalid common operation policy", dependsOnMethods = "testImportExistingCommonOperationPolicy")
    public void testImportInvalidCommonOperationPolicy() throws Exception {

        String yamlPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + TEST_INVALID_POLICY_NAME + ".yaml";
        String j2Path = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + TEST_POLICY_NAME + ".j2";

        File directory = Files.createTempDir();
        String zipDirectoryName = directory.getParentFile().getAbsolutePath() + File.separator + TEST_POLICY_NAME + "_" + TEST_POLICY_VERSION;
        directory.delete();
        directory = new File(zipDirectoryName);
        directory.mkdir();
        String destYaml = directory.getAbsolutePath() + File.separator + TEST_POLICY_NAME + ".yaml";
        String destJ2 = directory.getAbsolutePath() + File.separator + TEST_POLICY_NAME + ".j2";

        FileReader fileReader = new FileReader(yamlPath);
        FileWriter fileWriter = new FileWriter(destYaml);

        int res;
        while ((res = fileReader.read()) != -1) {
            fileWriter.write(res);
        }

        fileReader.close();
        fileWriter.close();

        fileReader = new FileReader(j2Path);
        fileWriter = new FileWriter(destJ2);

        while ((res = fileReader.read()) != -1) {
            fileWriter.write(res);
        }

        fileReader.close();
        fileWriter.close();

        File directoryToZip = new File(directory.getAbsolutePath());
        List<File> fileList = new ArrayList<>();

        File[] files = directoryToZip.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(file);
            }
        }

        writeArchiveFile(directoryToZip, fileList);

        File zipFileToBeExported = new File(directoryToZip.getAbsolutePath()+".zip");

        try{
            restAPIPublisher.importOperationPolicy(zipFileToBeExported);
        } catch (ApiException ex) {
            assertEquals(ex.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR, "Response code mismatched");
        }

    }

    @Test(groups = {"wso2.am"}, description = "Exporting Sample Common API Policy with JSON Policy Definition", dependsOnMethods = "testImportExistingCommonOperationPolicy")
    public void testCommonOperationPolicyExportWithJSONContent() throws Exception {

        exportedOperationPolicyZip = exportCommonOperationPolicyArtifact(TEST_POLICY_NAME, TEST_POLICY_VERSION, JSON_POLICY_TYPE, true);

        String extractedCommonAPIPolicyDir = exportedOperationPolicyZip.getParent();
        try {
            ZipFile zipFile = new ZipFile(exportedOperationPolicyZip);
            zipFile.extractAll(extractedCommonAPIPolicyDir);
        } catch (ZipException e) {
            throw new APIManagerIntegrationTestException("Error in extracting the exported API archive.", e);
        }

        String jsonPolicySpecPath = extractedCommonAPIPolicyDir + File.separator + TEST_POLICY_NAME + File.separator +
                TEST_POLICY_NAME + ".json";
        File policySpecFile = new File(jsonPolicySpecPath);

        assertTrue(policySpecFile.exists(), "API Policy Specification file does not exist");

        String synapseDefinitionPath = extractedCommonAPIPolicyDir + File.separator +  TEST_POLICY_NAME +
                File.separator +  TEST_POLICY_NAME + ".j2";
        File synapseDefFile = new File(synapseDefinitionPath);

        assertTrue(synapseDefFile.exists(), "Synapse Definition file does not exist");

        StringBuilder contentBuilderForPolicySpec = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(jsonPolicySpecPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilderForPolicySpec.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException("Error in reading from extracted API Policy file " + jsonPolicySpecPath, e);
        }

        String policySpecContent = contentBuilderForPolicySpec.toString();

        ObjectMapper jsonReader = new ObjectMapper();
        Object operationPolicySpec = jsonReader.readValue(policySpecContent, Object.class);

        String expectedPolicySpecPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + TEST_JSON_POLICY_NAME + ".json";

        StringBuilder contentBuilderForExpectedPolicySpec = new StringBuilder();
        try (Stream<String> stream = java.nio.file.Files.lines(Paths.get(expectedPolicySpecPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilderForExpectedPolicySpec.append(s).append("\n"));
        } catch (IOException e) {
            throw new APIManagerIntegrationTestException("Error in reading from expected Policy Specification file " + expectedPolicySpecPath, e);
        }

        String expectedPolicySpecContent = contentBuilderForExpectedPolicySpec.toString();

        jsonReader = new ObjectMapper();
        Object expectedOperationPolicySpec = jsonReader.readValue(expectedPolicySpecContent, Object.class);

        assertEquals(operationPolicySpec, expectedOperationPolicySpec, "Exported & Expected Policy Specifications are not matching");

    }

    @Test(groups = {"wso2.am"}, description = "Add API specific operation policy")
    public void testAddAPISpecificOperationPolicy() throws Exception {

        HttpResponse addPolicyResponse =
                addPolicy(apiId, "customAPISpecificLogPolicy.json", "customAPISpecificLogPolicy.j2");
        assertNotNull(addPolicyResponse, "Error adding operation policy customAPISpecificLogPolicy");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> apiSpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        assertTrue(apiSpecificPolicyMap.size() > 0);
        policyMap.put("customAPISpecificLogPolicy", newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Add another API specific operation policy with same name",
            dependsOnMethods = "testAddAPISpecificOperationPolicy")
    public void testAddAPISpecificOperationPolicyWithSamePolicyName() throws Exception {

        try {
            HttpResponse addPolicyResponse =
                    addPolicy(apiId, "customAPISpecificLogPolicy.json", "customAPISpecificLogPolicy.j2");
            assertNotEquals(addPolicyResponse.getResponseCode(), 201);
        } catch (ApiException e) {
            log.error(e);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Delete API specific operation policy",
            dependsOnMethods = "testAddAPISpecificOperationPolicyWithSamePolicyName")
    public void testDeleteAPISpecificOperationPolicy() throws Exception {

        int responseCode = deleteOperationPolicy(policyMap.get("customAPISpecificLogPolicy"), apiId);
        assertEquals(responseCode, 200);
        Map<String, String> updatedAPISpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        Assert.assertNull(updatedAPISpecificPolicyMap.get("customAPISpecificLogPolicy"));
        policyMap.remove("customAPISpecificLogPolicy");
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the log mediation")
    public void testAPIInvocationBeforeAddingNewOperationPolicy() throws Exception {

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(invokeAPIResponse.getHeaders("TestHeader").length, 0);
        assertEquals(invokeAPIResponse.getHeaders("Content-Type")[0].getValue(), "application/xml; charset=UTF-8");

    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testAPIInvocationBeforeAddingNewOperationPolicy")
    public void testAPIInvocationAfterAddingNewOperationPolicy() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "addHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader");
        attributeMap.put("headerValue", "TestValue");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v2");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);
        apiOperationPoliciesDTO.setResponse(opList);
        apiOperationPoliciesDTO.setFault(new ArrayList<>());

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        List<APIOperationsDTO> operations = apidto.getOperations();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/resource/");
        apiOperationsDTO.setAuthType("Application & Application User");
        apiOperationsDTO.setThrottlingPolicy("Unlimited");
        apiOperationsDTO.setOperationPolicies(apiOperationPoliciesDTO);
        operations.add(apiOperationsDTO);
        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        org.apache.http.HttpResponse invokeAPIResponseResource = invokeAPI(API_VERSION_1_0_0 + "/resource/");
        assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "TestValue");
        assertEquals(invokeAPIResponseResource.getHeaders("TestHeader")[0].getValue(), "TestValue");
    }

    @Test(groups = {"wso2.am"}, description = "Test fresh API with root path operation and operation policy")
    public void testFreshAPIWithRootPathOperationAndOperationPolicy() throws Exception {

        String freshApiName = "FreshAPIWithRootPath";
        String freshApiContext = "freshAPIWithRootPath";
        String freshApiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);

        APIRequest freshApiRequest = new APIRequest(freshApiName, freshApiContext, new URL(freshApiEndPointUrl));
        freshApiRequest.setVersion(API_VERSION_1_0_0);
        freshApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        freshApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        freshApiRequest.setTags(API_TAGS);

        freshApiId = createPublishAndSubscribeToAPIUsingRest(freshApiRequest, restAPIPublisher, restAPIStore,
                applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(freshApiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/");
        apiOperationsDTO.setAuthType("Application & Application User");
        apiOperationsDTO.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operations = new ArrayList<>();
        operations.add(apiOperationsDTO);
        apidto.setOperations(operations);

        String policyName = "addHeader";
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader");
        attributeMap.put("headerValue", "TestValue");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v2");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);
        apiOperationPoliciesDTO.setResponse(opList);
        apiOperationPoliciesDTO.setFault(new ArrayList<>());

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(freshApiId, restAPIPublisher);
        waitForAPIDeployment();

        subscribeToAPIUsingRest(freshApiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(freshApiContext, API_VERSION_1_0_0) + "/");
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse invokeAPIResponse = client.execute(request);

        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request to root path");
        org.apache.http.Header[] testHeaders = invokeAPIResponse.getHeaders("TestHeader");
        assertTrue(testHeaders.length > 0, "TestHeader not found in root path response");
        assertEquals(testHeaders[0].getValue(), "TestValue", "TestHeader value mismatch");
    }

    @Test(groups = {"wso2.am"}, description = "Validate the common operation policy clone at the update",
            dependsOnMethods = "testAPIInvocationAfterAddingNewOperationPolicy")
    public void testCommonOperationPolicyCloneToAPILevelWithUpdate() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);
        String clonedPolicyId = apidto.getOperations().get(0).getOperationPolicies().getRequest().get(0).getPolicyId();
        assertNotEquals(clonedPolicyId, policyMap.get("addHeader"));

        OperationPolicyDataDTO commonPolicy = restAPIPublisher.getCommonOperationPolicy(policyMap.get("addHeader"));
        OperationPolicyDataDTO clonedPolicy = restAPIPublisher.getAPISpecificOperationPolicy(clonedPolicyId, apiId);
        assertEquals(commonPolicy.getMd5(), clonedPolicy.getMd5());
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testCommonOperationPolicyCloneToAPILevelWithUpdate")
    public void testOperationPolicyAdditionWithMissingAttributes() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "removeHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, null));
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, null));

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        HttpResponse updateResponse = restAPIPublisher.updateAPIWithHttpInfo(apidto);
        assertEquals(updateResponse.getResponseCode(), 400);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testCommonOperationPolicyCloneToAPILevelWithUpdate")
    public void testAddOperationPolicyForNotSupportedFlow() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jsonFault";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, null));
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, null));

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        HttpResponse updateResponse = restAPIPublisher.updateAPIWithHttpInfo(apidto);
        assertEquals(updateResponse.getResponseCode(), 400);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testAPIInvocationAfterAddingNewOperationPolicy")
    public void testCreateNewVersionAfterAddingOperationPolicy() throws Exception {

        String newVersion = "2.0.0";
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(newVersion, apiId, null);
        assertEquals(newVersionResponse.getResponseCode(), HttpStatus.SC_OK, "Response Code Mismatch");
        newVersionAPIId = newVersionResponse.getData();

        HttpResponse getNewAPIResponse = restAPIPublisher.getAPI(newVersionAPIId);
        APIDTO apidto = new Gson().fromJson(getNewAPIResponse.getData(), APIDTO.class);

        Assert.assertNotNull(apidto.getOperations().get(0).getOperationPolicies(),
                "Unable to find a operation policies for the new version");
        Assert.assertNotNull(apidto.getOperations().get(0).getOperationPolicies().getRequest(),
                "Unable to find a operation policies for the new version request flow");
        String newVersionClonedPolicyId =
                apidto.getOperations().get(0).getOperationPolicies().getRequest().get(0).getPolicyId();

        assertNotEquals(newVersionClonedPolicyId, policyMap.get("addHeader"));
        String newVersionClonedPolicyType =
                apidto.getOperations().get(0).getOperationPolicies().getRequest().get(0).getPolicyType();

        assertNotEquals(newVersionClonedPolicyId, policyMap.get("api"));
        OperationPolicyDataDTO clonedPolicy =
                restAPIPublisher.getAPISpecificOperationPolicy(newVersionClonedPolicyId, newVersionAPIId);
        assertNotNull(clonedPolicy);

        createAPIRevisionAndDeployUsingRest(newVersionAPIId, restAPIPublisher);
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(newVersionAPIId,
                APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to PUBLISHED for the new version " + newVersionAPIId);
        waitForAPIDeployment();
        subscribeToAPIUsingRest(newVersionAPIId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED,
                restAPIStore);

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(newVersion);
        assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "TestValue");
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testCreateNewVersionAfterAddingOperationPolicy")
    public void testAPIInvocationAfterAddingNewMultipleOperationPolicies() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        List<OperationPolicyDTO> requestPolicyList = new ArrayList<>();
        List<OperationPolicyDTO> responsePolicyList = new ArrayList<>();
        String policyList[] = {"disableChunking", "jsonToXML", "xmlToJson"};
        String policyName = "addHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader");
        attributeMap.put("headerValue", "TestValue");

        for (int i = 0; i < 3; i++) {
            Assert.assertNotNull(policyMap.get(policyList[i]), "Unable to find a common policy with name " + policyList[i]);
            requestPolicyList.add(getPolicyList(policyList[i], POLICY_TYPE_COMMON, policyMap, null).get(0));
            responsePolicyList.add(getPolicyList(policyList[i], POLICY_TYPE_COMMON, policyMap, null).get(0));
        }
        List<OperationPolicyDTO> opList = getPolicyList(policyName, POLICY_TYPE_COMMON, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v2");

        requestPolicyList.add(opList.get(0));
        responsePolicyList.add(opList.get(0));
        apiOperationPoliciesDTO.setRequest(requestPolicyList);
        apiOperationPoliciesDTO.setResponse(responsePolicyList);
        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        restAPIPublisher.updateAPI(apidto);
        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "TestValue");
    }

    @Test(groups = {"wso2.am"}, description = "Add API specific operation policy using YAML Policy Definition",
            dependsOnMethods = "testAPIInvocationAfterAddingNewMultipleOperationPolicies")
    public void testAddAPISpecificOperationPolicyYAML() throws Exception {

        HttpResponse addPolicyResponse =
                addPolicy(apiId, "customAPISpecificLogPolicyForYAMLPolicyDefinitionTesting.yaml",
                        "customAPISpecificLogPolicy.j2");
        assertNotNull(addPolicyResponse, "Error adding operation policy customAPISpecificLogPolicyYAML");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> apiSpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        Assert.assertNotNull(apiSpecificPolicyMap.get("customAPISpecificLogPolicyYAML"),
                "Unable to find the newly added API specific policy");
        policyMap.put("customAPISpecificLogPolicyYAML", newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Delete API specific operation policy created using YAML Policy Definition",
            dependsOnMethods = "testAddAPISpecificOperationPolicyYAML")
    public void testDeleteAPISpecificOperationPolicyYAML() throws Exception {

        int responseCode = deleteOperationPolicy(policyMap.get("customAPISpecificLogPolicyYAML"), apiId);
        assertEquals(responseCode, 200);
        Map<String, String> updatedAPISpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        Assert.assertNull(updatedAPISpecificPolicyMap.get("customAPISpecificLogPolicyYAML"));
        policyMap.remove("customAPISpecificLogPolicyYAML");
    }

    @Test(groups = {"wso2.am"}, description = "Add common operation policy using YAML specification file",
            dependsOnMethods = "testDeleteAPISpecificOperationPolicyYAML")
    public void testAddNewCommonOperationPolicyYAML() throws Exception {

        HttpResponse addPolicyResponse = addPolicy(null,
                "customCommonLogPolicyForYAMLPolicyDefinitionTesting.yaml", "customCommonLogPolicy.j2");

        assertNotNull(addPolicyResponse, "Error adding operation policy customCommonLogPolicy");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();
        Assert.assertNotNull(updatedCommonPolicyMap.get("customCommonLogPolicyYAML"),
                "Unable to find the newly added common policy");
        policyMap.put("customCommonLogPolicyYAML", newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Delete common operation policy created using YAML specification file",
            dependsOnMethods = "testAddNewCommonOperationPolicyYAML")
    public void testDeleteCommonOperationPolicyYAML() throws Exception {

        int responseCode = deleteOperationPolicy(policyMap.get("customCommonLogPolicyYAML"), null);
        assertEquals(responseCode, 200);
        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();
        Assert.assertNull(updatedCommonPolicyMap.get("customCommonLogPolicyYAML"));
        policyMap.remove("customCommonLogPolicyYAML");
    }

    @Test(groups = {"wso2.am"}, description = "Add API specific operation policy with secret attributes",
            dependsOnMethods = "testDeleteCommonOperationPolicyYAML")
    public void testAddAPISpecificOperationPolicyWithSecrets() throws Exception {
        HttpResponse addPolicyResponse = addPolicy(apiId, "addSecretHeadersPolicy.json", "addSecretHeadersPolicy.j2");
        assertNotNull(addPolicyResponse, "Error adding operation policy addSecretHeadersPolicy");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> apiSpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        assertNotNull(apiSpecificPolicyMap.get(POLICY_WITH_SECRETS),
                "Unable to find the newly added API specific policy with secrets");
        policyMap.put(POLICY_WITH_SECRETS, newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Test API before attaching secret policy",
            dependsOnMethods = "testAddAPISpecificOperationPolicyWithSecrets")
    public void testAPIBeforeAttachingPolicyWithSecretAttributes() throws Exception {
        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(invokeAPIResponse.getHeaders("apiKey").length, 0);
        assertEquals(invokeAPIResponse.getHeaders("token").length, 0);
    }

    @Test(groups = {"wso2.am"}, description = "Test API after attaching policy with secret attributes",
            dependsOnMethods = "testAPIBeforeAttachingPolicyWithSecretAttributes")
    public void testAPIAfterAttachingPolicyWithSecretAttributes() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        assertNotNull(policyMap.get(POLICY_WITH_SECRETS), "Unable to find policy with name " + POLICY_WITH_SECRETS);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("apiKey", "test-api-key-123"); // Set the mandatory secret value
        attributeMap.put("token", ""); // Keeping the optional value empty

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(POLICY_WITH_SECRETS, POLICY_TYPE_COMMON, policyMap, attributeMap));
        apiOperationPoliciesDTO.setResponse(getPolicyList(POLICY_WITH_SECRETS, POLICY_TYPE_COMMON, policyMap, attributeMap));
        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getHeaders("apiKey")[0].getValue(), "test-api-key-123");
        assertEquals(invokeAPIResponse.getHeaders("token")[0].getValue(), "");
    }

    @Test(groups = {"wso2.am"}, description = "Test secret policy attribute retrieval",
            dependsOnMethods = "testAPIAfterAttachingPolicyWithSecretAttributes")
    public void testRetrievePolicyWithSecretAttributes() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        APIOperationPoliciesDTO policies = apidto.getOperations().get(0).getOperationPolicies();
        Map<String, Object> parameters = policies.getRequest().get(0).getParameters();

        // Verify that secret values are not returned
        assertEquals(parameters.get("apiKey"), "");
        assertNull(parameters.get("token"));
    }

    @Test(groups = {"wso2.am"}, description = "Test updating secret policy attributes with preserve option",
            dependsOnMethods = "testRetrievePolicyWithSecretAttributes")
    public void testUpdatePolicyWithSecretAttributes() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("apiKey", "");  // Empty value should preserve existing secret
        attributeMap.put("token", "");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(POLICY_WITH_SECRETS, POLICY_TYPE_COMMON, policyMap, attributeMap));
        apiOperationPoliciesDTO.setResponse(getPolicyList(POLICY_WITH_SECRETS, POLICY_TYPE_COMMON, policyMap, attributeMap));

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        // Values should remain unchanged
        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getHeaders("apiKey")[0].getValue(), "test-api-key-123");
        assertEquals(invokeAPIResponse.getHeaders("token")[0].getValue(), "");
    }

    @Test(groups = {"wso2.am"}, description = "Test version creation with secret policy attributes",
            dependsOnMethods = "testUpdatePolicyWithSecretAttributes")
    public void testVersionCreationWithPolicyWithSecretAttributes() throws Exception {
        String newVersion = "3.0.0";
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(newVersion, apiId, null);
        assertEquals(newVersionResponse.getResponseCode(), HttpStatus.SC_OK, "Response Code Mismatch");
        newVersionIdOfAPIWithSecretPolicy = newVersionResponse.getData();

        // Check if policies with secret attributes are preserved in the new version
        HttpResponse getNewAPIResponse = restAPIPublisher.getAPI(newVersionIdOfAPIWithSecretPolicy);
        APIDTO apidto = new Gson().fromJson(getNewAPIResponse.getData(), APIDTO.class);

        Assert.assertNotNull(apidto.getOperations().get(0).getOperationPolicies(),
                "Operation policies not found in the new version");
        Assert.assertNotNull(apidto.getOperations().get(0).getOperationPolicies().getRequest(),
                "Request operation policies not found in the new version");

        // Deploy and publish API
        createAPIRevisionAndDeployUsingRest(newVersionIdOfAPIWithSecretPolicy, restAPIPublisher);
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(
                newVersionIdOfAPIWithSecretPolicy, APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to publish new version " + newVersionIdOfAPIWithSecretPolicy);
        waitForAPIDeployment();
        subscribeToAPIUsingRest(newVersionIdOfAPIWithSecretPolicy, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);

        // Invoke the API to check if secret values are preserved
        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(newVersion);
        assertEquals(invokeAPIResponse.getHeaders("apiKey")[0].getValue(), "test-api-key-123");
        assertEquals(invokeAPIResponse.getHeaders("token")[0].getValue(), "");
    }

    @Test(groups = {"wso2.am"}, description = "Delete API specific operation policy with secret attributes",
            dependsOnMethods = "testVersionCreationWithPolicyWithSecretAttributes")
    public void testDeleteAPISpecificOperationPolicyWithSecrets() throws Exception {
        String secretPolicyId = policyMap.get(POLICY_WITH_SECRETS);
        assertNotNull(secretPolicyId, "Secret policy ID not found");

        // First, remove the policy from the API
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        // Clear the operation policies
        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(new ArrayList<>());
        apiOperationPoliciesDTO.setResponse(new ArrayList<>());

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        // Update the API without the policy
        restAPIPublisher.updateAPI(apidto);

        // Create revision and deploy to apply changes
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        // Delete the policy
        int responseCode = deleteOperationPolicy(secretPolicyId, apiId);
        assertEquals(responseCode, 200, "Failed to delete API specific policy with secret attributes");

        // Verify the policy is deleted
        Map<String, String> updatedAPISpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        assertNull(updatedAPISpecificPolicyMap.get(POLICY_WITH_SECRETS),
                "Secret header policy was not deleted successfully");
        policyMap.remove(POLICY_WITH_SECRETS);

        // Verify policy is no longer applied
        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(invokeAPIResponse.getHeaders("apiKey").length, 0,
                "apiKey header still exists after policy deletion");
        assertEquals(invokeAPIResponse.getHeaders("token").length, 0,
                "token header still exists after policy deletion");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(newVersionAPIId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(newVersionIdOfAPIWithSecretPolicy, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(freshApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(freshApiId);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newVersionAPIId);
        restAPIPublisher.deleteAPI(newVersionIdOfAPIWithSecretPolicy);
    }

    public HttpResponse addPolicy(String apiId, String policySpecName, String policyDefinitionName)
            throws ApiException {

        String policySpecPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + policySpecName;

        String synapsePolicyDefPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + policyDefinitionName;

        File specification = new File(policySpecPath);
        File synapseDefinition = new File(synapsePolicyDefPath);

        HttpResponse addPolicyResponse;
        if (apiId == null) {
            addPolicyResponse = restAPIPublisher.addCommonOperationPolicy(specification, synapseDefinition, null);
        } else {
            addPolicyResponse =
                    restAPIPublisher.addAPISpecificOperationPolicy(apiId, specification, synapseDefinition, null);
        }
        return addPolicyResponse;
    }

    /**
     * Exports the required throttling policy and writes to a temporary location
     *
     * @param policyName Operation Policy name
     * @param policyVersion Operation Policy type
     * @return Exported Operation policy file
     * @throws IOException  throws if connection issues occurred
     */
    private File exportCommonOperationPolicyArtifact(String policyName, String policyVersion, String format, boolean isValid) throws Exception {

        URL exportRequest =
                new URL(exportUrl + "?name=" + policyName + "&version=" + policyVersion + "&format=" + format);
        File zipTempDir = com.google.common.io.Files.createTempDir();
        //set the export file name with tenant prefix
        String fileName = policyName + "_" + policyVersion;
        File apiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");
        //save the exported API
        exportArtifact(exportRequest, apiZip, isValid);
        return apiZip;
    }

    private void exportArtifact(URL exportRequest, File fileName, boolean isValid) throws URISyntaxException, IOException {
        CloseableHttpResponse response = exportAPIRequest(exportRequest);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = new FileOutputStream(fileName);
            try {
                entity.writeTo(outStream);
            } finally {
                outStream.close();
            }
        }

        if (isValid) {
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code is not as expected");
        } else {
            assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NOT_FOUND, "Response code is not as expected");
        }

        Assert.assertTrue(fileName.exists(), "File save was not successful");
    }

    private CloseableHttpResponse exportAPIRequest(URL exportRequest)
            throws IOException, URISyntaxException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpGet get = new HttpGet(exportRequest.toURI());
        String accessToken = restAPIPublisherExport.getAccessToken();
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        CloseableHttpResponse response = client.execute(get);
        return response;
    }
    public int deleteOperationPolicy(String policyId, String apiId) throws ApiException {

        HttpResponse deletePolicyResponse;
        if (apiId == null) {
            deletePolicyResponse = restAPIPublisher.deleteCommonOperationPolicy(policyId);
        } else {
            deletePolicyResponse = restAPIPublisher.deleteAPISpecificPolicy(policyId, apiId);
        }
        return deletePolicyResponse.getResponseCode();
    }

    public List<OperationPolicyDTO> getPolicyList(String policyName, String policyType, Map<String, String> policyMap,
                                                  Map<String, Object> attributeMap) {

        List<OperationPolicyDTO> policyList = new ArrayList<>();
        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        policyDTO.setPolicyName(policyName);
        policyDTO.setPolicyType(policyType);
        policyDTO.setPolicyId(policyMap.get(policyName));
        policyDTO.setParameters(attributeMap);
        policyList.add(policyDTO);

        return policyList;
    }

    public org.apache.http.HttpResponse invokeAPI(String version) throws XPathExpressionException, IOException {

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, version));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        return response;
    }

    /**
     * Create an Archive File.
     *
     * @param directoryToZip Directory to be archived.
     * @param fileList Files to be included in the archive.
     * @throws IOException
     */
    private void writeArchiveFile(File directoryToZip, List<File> fileList) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(directoryToZip.getPath() + ".zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
        for (File file : fileList) {
            if (!file.isDirectory()) {
                addToArchive(directoryToZip, file, zipOutputStream);
            }
        }

        zipOutputStream.close();
    }

    private void addToArchive(File directoryToZip, File file, ZipOutputStream zipOutputStream)
            throws IOException {

        byte[] bytes = Files.toByteArray(file);
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);
        if (File.separatorChar != '/') {
            zipFilePath = zipFilePath.replace(File.separatorChar,'/');
        }
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipOutputStream.putNextEntry(zipEntry);

        zipOutputStream.write(bytes, 0, bytes.length);

        zipOutputStream.closeEntry();

    }
}
