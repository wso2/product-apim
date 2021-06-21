/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.other;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static org.testng.Assert.assertEquals;

/**
 * Publish a API under Unlimited tier and test the invocation , then disable unlimited tier and change the api
 * tier to silver and do a new silver subscription and test invocation under Silver tier.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE })
public class UnlimitedTierDisabledTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(UnlimitedTierDisabledTestCase.class);
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIPublisherRestClient apiPublisherClientUser1;
    private boolean isInitialised = false;

    private ServerConfigurationManager serverConfigurationManager;
    private final String GRAPHQL_API_NAME = "CountriesGraphqlAPI";
    private final String END_POINT_URL = "https://localhost:9943/am-graphQL-sample/api/graphql/";

    private String schemaDefinition;
    private String graphqlAPIId;

    @Factory(dataProvider = "userModeDataProvider")
    public UnlimitedTierDisabledTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(publisherContext);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "unlimitedTier"
                        + File.separator + "deployment.toml"));
    }


    public void initialize() throws Exception {
        if (!isInitialised) {
            super.init();
            apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
            providerName = user.getUserName();
            String publisherURLHttp = getPublisherURLHttp();
            apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
            //Login to API Publisher with  admin
            apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
            isInitialised = true;
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create graphQL API and see if the rate limiting is set to a policy " +
            "other than Unlimited")
    public void testCreateGraphQLAPI() throws Exception {
        initialize();
        schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                "UTF-8");

        File file = getTempFileWithContent(schemaDefinition);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add("Production and Sandbox");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", "graphqlAPI");
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("gatewayEnvironments", environment);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        log.info("graphQLAPI"+ apidto);
        graphqlAPIId = apidto.getId();
        List<APIOperationsDTO> operationsList = apidto.getOperations();
        log.info("operationsList"+ operationsList);
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        //get the operation of the API and check whether throttling tier is not unlimited
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());
        Assert.assertFalse(operationsList.get(0).getThrottlingPolicy().equalsIgnoreCase(TIER_UNLIMITED),
                "Throttling policy " + operationsList.get(0).getThrottlingPolicy() + " is applied");

    }

    @Test(groups = {"wso2.am"}, description = "Create a REST API without x-throttling tier and check if " +
            "API gets published successfully")
    public void createAPIwithThrottlingTierNull() throws Exception {
        //create a REST API
        String swaggerPath = getAMResourceLocation() + File.separator + "configFiles" + File.separator + "unlimitedTier"
                + File.separator + "TestAPI.yaml";
        File definition = new File(swaggerPath);
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "test");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.SILVER);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "TestAPI");
        apiProperties.put("context", "/" + "test");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", "admin");
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);
        APIDTO restAPIDTO = restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
        String apiImportId = restAPIDTO.getId();
        restAPIPublisher.changeAPILifeCycleStatus(apiImportId, Constants.PUBLISHED);
        HttpResponse response = restAPIPublisher.getAPI(apiImportId);;
        assertEquals(Response.Status.OK.getStatusCode(), response.getResponseCode());
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

}