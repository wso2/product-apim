/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.*;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class DAOTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(DAOTestCase.class);
    private String apiId;
    private String applicationId;
    private String providerName;
    private ArrayList<String> grantTypes;



    @Factory(dataProvider = "userModeDataProvider")
    public DAOTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();
        grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

    }

    @Test(groups = { "wso2.am" }, description = "API Life cycle test case")
    public void testDAOTestCase() throws Exception {
        String applicaitionName = "DAOTestAPI-Application";
        String apiName = "DAOTestAPI";
        String apiContext = "DAOTestAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";

        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = addAPIResponse.getData();
        restAPIPublisher.deleteAPI(apiId);
        apiId = restAPIPublisher.addAPI(apiRequest).getData();
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());
        //Test API properties
        assertEquals(apidto.getName(), apiName, "API Name mismatch");
        assertTrue(apidto.getContext().contains(apiContext), "API context mismatch");
        assertEquals(apidto.getVersion(), APIVersion, "API version mismatch");
        assertEquals(apidto.getProvider(), providerName,
                "Provider Name mismatch");
        for (String tag : apidto.getTags()) {
            assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        assertEquals(apidto.getDescription(), description, "API description mismatch");

        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicaitionName,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "", "this-is-test");
        applicationId = applicationDTO.getApplicationId();
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName,
                storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName(applicaitionName);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken().getAccessToken());

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }

}
