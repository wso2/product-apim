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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIDesignBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertTrue;

/**
 * Create an API through the Publisher Rest API without providing mandatory fields
 * JIRA Issue Id - APIMANAGER-4039
 */

public class APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase extends
        APIMIntegrationBaseTest {
    private final String apiNameTest1 = "APIM514PublisherTest1";
    private final String apiVersion = "1.0.0";
    private final String apiDescription =
            "This is Test API Created by API Manager Integration Test";
    private String apiTag = "tag514-1, tag514-2, tag514-3";
    private APIPublisherRestClient apiPublisher;
    private String apiProviderName;
    private String apiProductionEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM514CreateAnAPIWithoutProvidingMandatoryFieldsTestCase(TestUserMode userMode) {
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

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiProductionEndPointUrl = getGatewayURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API" +
            " without proving API Name")
    public void testCreateAnAPIThroughThePublisherRestWithoutName() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI1";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean("", apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), "can be create API without name");
        assertTrue(apiResponse.getString("message").contains
                ("Unable to find the API"), "can be create API without name");

    }


    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving Context")
    public void testCreateAnAPIThroughThePublisherRestWithoutContext() throws Exception {

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest1, "", apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest1 + "can be create without Context");
        assertTrue(apiResponse.getString("message").contains
                        (" Context not defined for API"),
                apiNameTest1 + "can be create without Context");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving version")
    public void testCreateAnAPIThroughThePublisherRestWithoutVersion() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI2";
        String apiNameTest2 = "APIM514PublisherTest2";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest2, apiContextTest, "", apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest2 + "can be create without Version");
        assertTrue(apiResponse.getString("message").contains
                ("Version not specified for API"), apiNameTest2 + "can be create without Version");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving tier availability")
    public void testCreateAnAPIThroughThePublisherRestWithoutTierAvailability() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI3";
        String apiNameTest3 = "APIM514PublisherTest3";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest3, apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("");
        apiCreationRequestBean.setTier("");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest3 +
                "can be create without Tier availability");
        assertTrue(apiResponse.getString("message").contains
                ("No tier defined for the API"), apiNameTest3 + "can be create without Tier availability");
    }

//    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
//            "without proving Production endpoint")
    public void testCreateAnAPIThroughThePublisherRestWithoutEndpoint() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI4";
        String apiNameTest4 = "APIM514PublisherTest4";
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiNameTest4, apiContextTest,
                apiVersion, apiProviderName,new URL(null));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest4 + "can be create without Endpoint");
        assertTrue(apiResponse.getString("message").contains
                ("null"), apiNameTest4 + "can be create without Endpoint");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving Resources")
    public void testCreateAnAPIThroughThePublisherRestWithoutResources() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI5";
        String apiNameTest5 = "APIM514PublisherTest5";

        APIDesignBean designBean =
                new APIDesignBean(apiNameTest5, apiContextTest, apiVersion, apiDescription, apiTag);
        designBean.setSwagger("");

        HttpResponse apiCreationResponse = apiPublisher.designAPI(designBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest5 +
                "can be create without Resources");
        assertTrue(apiResponse.getString("message").contains
                ("Invalid resource configuration "), apiNameTest5 +
                "can be create without Resources");
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API " +
            "without proving Action")
    public void testCreateAnAPIThroughThePublisherRestWithoutAction() throws Exception {

        String apiContextTest = "apim514PublisherTestAPI6";
        String apiNameTest6 = "APIM514PublisherTest6";

        APICreationRequestBeanWithoutAction apiCreationRequestBean =
                new APICreationRequestBeanWithoutAction
                        (apiNameTest6, apiContextTest, apiVersion, apiProviderName,
                                new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setAction("");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertTrue(apiResponse.getBoolean("error"), apiNameTest6 + "can be create without Action");
        assertTrue(apiResponse.getString("message").contains
                (" is not supported"), apiNameTest6 + "can be create without Action");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI("",apiVersion,apiProviderName);
        apiPublisher.deleteAPI("APIM514PublisherTest5",apiVersion,apiProviderName);

    }

    private class APICreationRequestBeanWithoutAction extends APICreationRequestBean {

        public APICreationRequestBeanWithoutAction(String apiName, String context, String version,
                                                   String provider, URL endpointUrl)
                throws APIManagerIntegrationTestException {
            super(apiName, context, version, provider, endpointUrl);
        }

        @Override
        public void setAction() {
            //setting action empty
            setAction("");
        }
    }


}
