/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@SetEnvironment (executionEnvironments = { ExecutionEnvironment.STANDALONE})
public class APIMANAGER3226APINameWithDifferentCaseTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER3226APINameWithDifferentCaseTestCase.class);

    private RestAPIPublisherImpl restAPIPublisher;
    private String apiID;
    String apiName = "echo";
    String providerName;
    String apiVersion = "1.0.0";

    @Factory (dataProvider = "userModeDataProvider")
    public APIMANAGER3226APINameWithDifferentCaseTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass (alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();
        String tenantDomain = storeContext.getContextTenant().getDomain();
        restAPIPublisher = new RestAPIPublisherImpl(user.getUserNameWithoutDomain(), user.getPassword(), tenantDomain,
                publisherURLHttps);
    }

    @Test (groups = {"wso2.am"}, description = "Test validation of adding api with same name and different case"
            + "(uppercase)")
    public void testValidateAddAPIsWithDifferentCase() throws Exception {
        String apiContext = "test";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(providerName);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(addResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Error while adding API");
        apiID = addResponse.getData();

        String apiNameWithUppercaseLetters = "ECho";
        apiRequest = new APIRequest(apiNameWithUppercaseLetters, apiContext, new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(providerName);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        HttpResponse addDuplicateAPIResponse = restAPIPublisher.addAPI(apiRequest);
        assertNull(addDuplicateAPIResponse, "Validation fails for adding API with same name with different case");
    }

    @AfterClass (alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiID);
        super.cleanUp();
    }
}
