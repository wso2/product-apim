/*
*Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * This is the test case for https://wso2.org/jira/browse/APIMANAGER-4765
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APIM4765ResourceOrderInSwagger extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIM4765ResourceOrderInSwagger.class);
    private String apiId;
    private final String SWAGGER_FOLDER = "swagger";

    @Factory(dataProvider = "userModeDataProvider")
    public APIM4765ResourceOrderInSwagger(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }, };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = { "wso2.am" }, description = "Test resource order in the swagger")
    public void swaggerResourceOrderTest() throws Exception {

        String APIName = "SwaggerReorderTest";
        String APIContext = "swagger_reorder_test";
        String tags = "youtube, token, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        String resourcePath = "artifacts" + File.separator + "AM" + File.separator + "swagger" + File.separator + "ordered_resources_api_oas.json";
        String swagger = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath), "UTF-8");
        String orderedResourcesFilePath = "artifacts" + File.separator + "AM" + File.separator + "swagger" + File.separator +
                "ordered_resources_paths.json";
        String orderedResources = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(orderedResourcesFilePath), "UTF-8");

        restAPIPublisher.updateSwagger(apiId, swagger);
        //get swagger doc.
        String storeDefinition = restAPIStore.getSwaggerByID(apiId, user.getUserDomain());

        //orderedResources should  be equal to the given resource order.
        boolean isResourceOrderEqual = storeDefinition.contains(orderedResources);
        assertTrue(isResourceOrderEqual, "Resource order is not equal to the given order.");
    }

    @AfterClass(alwaysRun = true) public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        super.cleanUp();
    }
}
