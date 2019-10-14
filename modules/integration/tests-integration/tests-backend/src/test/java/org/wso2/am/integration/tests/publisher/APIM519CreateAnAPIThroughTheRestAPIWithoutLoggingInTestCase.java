/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * Create an API through the REST api without logging in
 */
public class APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase extends
        APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private String apiProviderName;
    private String apiProductionEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM519CreateAnAPIThroughTheRestAPIWithoutLoggingInTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API")
    public void testCreateAnAPIThroughThePublisherRest() throws Exception {

        String apiNameTest = "APIM519PublisherTest";
        String apiContextTest = "apim519PublisherTestAPI";
        String apiVersion = "1.0.0";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag519-1, tag519-2, tag519-3";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest, apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api519b");
        apiCreationRequestBean.setBizOwnerMail("api519b@ee.com");
        apiCreationRequestBean.setTechOwner("api519t");
        apiCreationRequestBean.setTechOwnerMail("api519t@ww.com");

        apiPublisher.logout();

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest +
                "cannot be created without logging in");
        assertTrue(apiResponse.getString("message").contains
                ("timeout"), apiNameTest + "cannot be created without logging in");
    }


}
