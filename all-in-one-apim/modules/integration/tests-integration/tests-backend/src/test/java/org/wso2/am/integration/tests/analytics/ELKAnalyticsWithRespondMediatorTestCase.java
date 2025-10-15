/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.analytics;

import com.google.gson.Gson;
import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDataDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.FileManager;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class ELKAnalyticsWithRespondMediatorTestCase extends APIManagerLifecycleBaseTest {
    private final String ELK_API_NAME = "ElkAnalyticsAPI";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String INVOKABLE_API_CONTEXT = "elkapi";
    private ServerConfigurationManager serverConfigurationManager;
    private String applicationId;
    private String apiId;
    private String policyId;
    private String accessToken;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws JSONException, APIManagerIntegrationTestException, ApiException,
            IOException, AutomationUtilException, XPathExpressionException,
            org.wso2.am.integration.clients.store.api.ApiException {
        super.init();
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "ElkAnalytics" +
                File.separator + "deployment.toml"));

        String ELK_APPLICATION_NAME = "ElkAnalyticsApplication";
        HttpResponse applicationResponse = restAPIStore.createApplication(ELK_APPLICATION_NAME,
                "Test Application for ELK", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        APIRequest apiRequest = new APIRequest(ELK_API_NAME, INVOKABLE_API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(groups = {"wso2.am"}, description = "Test ELK Analytics with Respond Mediator")
    public void testELKAnalyticsWithRespondMediator() throws Exception {
        // Add metrics logger to log4j2.properties
        String log4jPropertiesFile = getAMResourceLocation() + File.separator + "configFiles"
                + File.separator + "ElkAnalytics" + File.separator + "log4j2.properties";
        String log4jPropertiesTargetLocation = System.getProperty(ServerConstants.CARBON_HOME) + File.separator
                + "repository" + File.separator + "conf" + File.separator + "log4j2.properties";
        FileManager.copyFile(new File(log4jPropertiesFile), log4jPropertiesTargetLocation);


        // Add common operation policy with respond mediator
        addNewOperationPolicy();
        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies(70, 0, null);
        Assert.assertNotNull(updatedCommonPolicyMap.get("respondMediatorPolicy"),
                "Unable to find the newly added common policy");

        // Add policy to API
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);
        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        List<OperationPolicyDTO> policyList = new ArrayList<>();
        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        String POLICY_NAME = "respondMediatorPolicy";
        policyDTO.setPolicyName(POLICY_NAME);
        policyDTO.setPolicyId(updatedCommonPolicyMap.get(POLICY_NAME));
        policyList.add(policyDTO);
        apiOperationPoliciesDTO.setRequest(policyList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();


        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        String API_GET_ENDPOINT_METHOD = "/customers/123";
        HttpResponse response = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT, API_VERSION_1_0_0) +
                        API_GET_ENDPOINT_METHOD, requestHeaders);

        String metricsFile = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository"
                + File.separator + "logs" + File.separator + "apim_metrics.log";
        File file = new File(metricsFile);
        assertTrue(file.exists(), "Metrics file not found in " + metricsFile);
        assertTrue(validateFileContent(metricsFile), "Metrics file does not contain the expected content");

    }

    public void addNewOperationPolicy() throws ApiException {
        String policySpecPath = getAMResourceLocation() + File.separator + "configFiles"
                + File.separator + "ElkAnalytics" + File.separator + "respondMediatorPolicy.json";

        String synapsePolicyDefPath = getAMResourceLocation() + File.separator + "configFiles"
                + File.separator + "ElkAnalytics" + File.separator + "respondMediatorPolicy.j2";

        File specification = new File(policySpecPath);
        File synapseDefinition = new File(synapsePolicyDefPath);

        HttpResponse response = restAPIPublisher.addCommonOperationPolicy(specification, synapseDefinition,
                null);
        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(response.getData(), OperationPolicyDataDTO.class);
        policyId = policyDTO.getId();
    }

    public boolean validateFileContent(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(ELK_API_NAME) && line.contains("\"destination\":\"dummy_endpoint_address\"")) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteCommonOperationPolicy(policyId);
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
