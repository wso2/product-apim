/*
 *Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 LLC. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.am.integration.tests.crossSubscription;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import static org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO.SubscriptionAvailabilityEnum.ALL_TENANTS;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class CrossTenantSubscriptionUpdateTestCase extends APIManagerLifecycleBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private String apiId1;
    private final String apiName1 = "Test1";
    private final String apiVersion1 = "1.0.0";
    private final String apiContext1 = "/test1";
    private String apiId2;
    private final String apiName2 = "Test2";
    private final String apiVersion2 = "1.0.0";
    private final String apiContext2 = "/test2";

    private final String apiEndpoint1 = "http://localhost:9443";
    private final String apiEndpoint2 = "http://localhost:9444";
    private final String tenant3ApplicationName = "TestApp";
    private ApplicationDTO tenant3Application;
    private ApplicationKeyDTO tenant3AppTenant3Store;
    private String tenant1Domain;
    private String tenant2Domain;
    private final String errorMessageKeyGeneration = "Error occurred while generating keys";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "cross-tenant" + File.separator + "deployment.toml"));
        APIRequest apiRequest1 = new APIRequest(apiName1, apiContext1, new URL(apiEndpoint1));
        apiRequest1.setVersion(apiVersion1);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.GOLD + "," + APIMIntegrationConstants.API_TIER.BRONZE);
        apiRequest1.setSubscriptionAvailability(ALL_TENANTS.toString());
        HttpResponse response = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction());
        tenant1Domain = MultitenantUtils.getTenantDomain(user.getUserName());

        super.init(TestUserMode.TENANT_ADMIN);

        APIRequest apiRequest2 = new APIRequest(apiName2, apiContext2, new URL(apiEndpoint2));
        apiRequest2.setVersion(apiVersion2);
        apiRequest2.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        apiRequest2.setTiersCollection(APIMIntegrationConstants.API_TIER.GOLD + "," + APIMIntegrationConstants.API_TIER.BRONZE);
        apiRequest2.setSubscriptionAvailability(ALL_TENANTS.toString());
        apiRequest2.setProvider(user.getUserName());
        HttpResponse response2 = restAPIPublisher.addAPI(apiRequest2);
        apiId2 = response2.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());
        tenant2Domain = MultitenantUtils.getTenantDomain(user.getUserName());
        // tenant1 :carbon.super, tenant2 :wso2.com
        Assert.assertNotEquals(tenant1Domain, tenant2Domain);
    }

    @Test(description = "Create new application and generate access token using an already subscribed application")
    public void testCreateNewApplicationAndGenerateTokenSubscribedApplication() throws Exception {

        super.init(TestUserMode.TENANT_EMAIL_USER);

        tenant3Application = restAPIStore.addApplication(tenant3ApplicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                StringUtils.EMPTY, StringUtils.EMPTY);
        SubscriptionDTO subscriptionDTO1 = restAPIStore.subscribeToAPI(apiId1, tenant3Application.getApplicationId(),
                APIMIntegrationConstants.API_TIER.GOLD, tenant1Domain);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        //restAPIAdmin.getKeyManagers();  // Due to the issue: https://github.com/wso2/product-apim/issues/12634
        tenant3AppTenant3Store = restAPIStore.generateKeys(tenant3Application.getApplicationId(),
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME,
                StringUtils.EMPTY,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        Assert.assertNotNull(tenant3AppTenant3Store, errorMessageKeyGeneration);

        // subscribe to the other API
        SubscriptionDTO subscriptionDTO2 = restAPIStore.subscribeToAPI(apiId2, tenant3Application.getApplicationId(),
                APIMIntegrationConstants.API_TIER.GOLD, tenant2Domain);

        restAPIStore.updateSubscriptionToAPI(apiId2, tenant3Application.getApplicationId(), APIMIntegrationConstants.API_TIER.GOLD,
                APIMIntegrationConstants.API_TIER.BRONZE, SubscriptionDTO.StatusEnum.UNBLOCKED, subscriptionDTO2.getSubscriptionId(), tenant2Domain);

        verifyTenantDomainInWorkflowsTable(apiName2, tenant2Domain);

        restAPIStore.updateSubscriptionToAPI(apiId1, tenant3Application.getApplicationId(), APIMIntegrationConstants.API_TIER.GOLD,
                APIMIntegrationConstants.API_TIER.BRONZE, SubscriptionDTO.StatusEnum.UNBLOCKED, subscriptionDTO1.getSubscriptionId(), tenant1Domain);

        verifyTenantDomainInWorkflowsTable(apiName1, tenant1Domain);
    }

    private void verifyTenantDomainInWorkflowsTable (String apiName, String tenantDomain) throws JSONException, ApiException {
        org.wso2.am.integration.test.HttpResponse workflowsResponse = restAPIAdmin.getWorkflows(null);
        Assert.assertNotNull(workflowsResponse);
        JSONObject workflowRespObj = new JSONObject(workflowsResponse.getData());
        JSONArray arr = (JSONArray) workflowRespObj.get("list");

        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                Assert.assertEquals(listItem.get("tenantDomain"), tenantDomain);
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(tenant3Application.getApplicationId());
        serverConfigurationManager.restoreToLastConfiguration();
    }
}

