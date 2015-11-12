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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Add Documentation To An API using  Doc Type support Forum with different source types
 * Through Publisher Rest API
 * APIM2 -625
 */
public class APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase
        extends APIMIntegrationBaseTest{

    private final String apiName = "APIM625PublisherTest";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private String apiProvider;
    private String apiEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM625AddDocumentationToAnAPIWithDocTypeSupportForumThroughPublisherRestAPITestCase
            (TestUserMode userMode) {
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

        apiEndPointUrl = gatewayUrlsMgt.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type  support forum And" +
            " Source Url through the publisher rest API ")
    public void testAddDocumentToAnAPISupportInline() throws Exception {

        String apiContext = "apim625PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag625-1, tag625-2";
        String docName = "APIM625PublisherTestHowTo-Inline-summary";
        String docType = "support forum";
        String sourceType = "URL";
        String docUrl="https://docs.wso2.com/display/AM191/Published+APIs";


        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersion, apiProvider,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api625b");
        apiCreationRequestBean.setBizOwnerMail("api625b@ee.com");
        apiCreationRequestBean.setTechOwner("api625t");
        apiCreationRequestBean.setTechOwnerMail("api625t@ww.com");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertFalse(apiResponse.getBoolean("error"), apiName + "is not created as expected");

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName,apiProvider,apiVersion);
        JSONObject jsonObject = new JSONObject(apiResponsePublisher.getData());
        JSONObject apiObject = new JSONObject(jsonObject.getString("api"));
        assertFalse(jsonObject.getBoolean("error"), apiName + " is not visible in publisher");
        assertTrue(apiObject.getString("name").equals(apiName),
                apiName + " is not visible in publisher");
        assertTrue(apiObject.getString("status").equals("CREATED"),
                "Status of the " + apiName + "is not a valid status");

        //Add Documentation to an API "APIM625PublisherTest" -  support forum | Url
        HttpResponse docResponse = apiPublisher.addDocument
                (apiName,apiVersion,apiProvider,docName,docType,sourceType,docUrl,"Testing","","",null);
        JSONObject jsonObjectDoc1 = new JSONObject( docResponse.getData());
        assertFalse(jsonObjectDoc1.getBoolean("error"), "Error when adding document with source " +
                "Url to the API");
    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);

    }
}
