/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.api.lifecycle;

import com.google.gson.Gson;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * This test class is use to test the coping and updating api test cases
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class NewVersionUpdateTestCase extends APIMIntegrationBaseTest {

    private String apiName = "NewVersionUpdateTestCaseAPIName";
    private String APIContext = "NewVersionUpdateTestCaseContext";
    private String tags = "test";
    private String endpointUrl, endpointUrlNew;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String APIVersionNew = "2.0.0";
    private String apiId;
    private String apiId2;

    @Factory(dataProvider = "userModeDataProvider")
    public NewVersionUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";
        endpointUrlNew = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/multiply";

    }

    @Test(groups = {"wso2.am"}, description = "Create new version and publish")
    public void testAPINewVersionCreation() throws Exception {
        String providerName = user.getUserName();

        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //add test api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        //copy test api
        serviceResponse = restAPIPublisher.copyAPI(APIVersionNew, apiId, false);

        apiId2 = serviceResponse.getData();

        //test the copied api
        serviceResponse = restAPIPublisher.getAPI(apiId2);

        Gson g = new Gson();
        APIDTO apidto = g.fromJson(serviceResponse.getData(), APIDTO.class);
        String version = apidto.getVersion();
        assertEquals(version, APIVersionNew);

    }

    @Test(groups = {"wso2.am"}, description = "Update new version api with endpoint", dependsOnMethods = "testAPINewVersionCreation")
    public void testNewVersionAPIUpdate() throws Exception {
        //create update request for restrict HTTP
        APIRequest apiUpdateRequest = new APIRequest(apiName, APIContext,
                new URL(endpointUrlNew));
        String providerName = user.getUserName();
        apiUpdateRequest.setProvider(providerName);
        apiUpdateRequest.setVersion(APIVersionNew);
        HttpResponse updateResponse = restAPIPublisher.updateAPI(apiUpdateRequest, apiId);

        waitForAPIDeployment();
        HttpResponse apiResponse = restAPIPublisher.getAPI(updateResponse.getData());

        Gson g = new Gson();
        APIDTO apidto = g.fromJson(apiResponse.getData(), APIDTO.class);
        String endPointConfig = apidto.getEndpointConfig().toString();
        assertTrue(endPointConfig.contains(endpointUrlNew));
    }

    @Test(groups = { "wso2.am" },
            description = "Check the count of the APIs when display multiple versioned APIs option is disabled in "
                    + "devportal", dependsOnMethods = "testNewVersionAPIUpdate")
    public void testCheckMultipleVersionedAPIsCount()
            throws Exception {
        // Publish the versioned API
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());

        waitForAPIDeployment();

        APIListDTO restAPIStoreAllAPIs = restAPIStore.getAllAPIs(user.getUserDomain());
        assertEquals(restAPIStoreAllAPIs.getCount().toString(), String.valueOf(1), "Wrong API count returned");
        assertEquals(restAPIStoreAllAPIs.getList().get(0).getVersion(), APIVersionNew, "Wrong API list returned");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(apiId2);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }
}
