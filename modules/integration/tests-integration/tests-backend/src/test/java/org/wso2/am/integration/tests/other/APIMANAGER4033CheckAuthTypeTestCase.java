/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.am.integration.tests.other;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;


public class APIMANAGER4033CheckAuthTypeTestCase extends APIManagerLifecycleBaseTest {
    private APIPublisherRestClient apiPublisher;
    private String apiEndPointUrl;
    APIIdentifier apiIdentifier;

    private static final String TEST_DATA_APINAME = "APIMANAGER4033";
    private static final String TEST_DATA_APICONTEXT = "apimanager4033";
    private static final String TEST_DATA_TAGS = "4033, auth_type";
    private static final String TEST_DATA_API_DESCRIPTION = "This is test API created to test Auth Type";
    private static final String TEST_DATA_API_PROVIDER_NAME = "admin";
    private static final String TEST_DATA_API_VERSION = "1.0.0";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String TEST_DATA_ENDPOINT_TYPE = "nonsecured";
    private static final String TEST_DATA_RESOURCE_METHOD = "GET";
    private static final String TEST_DATA_RESOURCE_METHOD_ENDPOINT_TYPE = "Any";
    private static final String TEST_DATA_EXPECTED_ENDPOINT_TYPE = "Application & Application User";
    private static final String TEST_DATA_RESOURCE_METHOD_ENDPOINT_THROTTLING = "Unlimited";
    private static final String TEST_DATA_API_VISIBILITY = "public";
    private static final String TEST_DATA_URI_TEMPLATE = "*";


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException, IOException,
            XPathExpressionException, URISyntaxException, SAXException, XMLStreamException,
            LoginAuthenticationExceptionException {
        super.init();
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
    }

    @Test(groups = {"wso2.am"}, description = "API Auth type test case")
    public void testAuthTypeOfCreatedAPI() throws Exception {

        apiIdentifier = new APIIdentifier(TEST_DATA_API_PROVIDER_NAME, TEST_DATA_APINAME, TEST_DATA_API_VERSION);
        //login to the publisher
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        //set api details to the API being published
        APIRequest apiRequest = new APIRequest(TEST_DATA_APINAME, TEST_DATA_APICONTEXT, new URL(apiEndPointUrl));
        apiRequest.setTags(TEST_DATA_TAGS);
        apiRequest.setDescription(TEST_DATA_API_DESCRIPTION);
        apiRequest.setVersion(TEST_DATA_API_VERSION);
        apiRequest.setEndpointType(TEST_DATA_ENDPOINT_TYPE);

        apiRequest.setResourceCount("0");
        apiRequest.setResourceMethod(TEST_DATA_RESOURCE_METHOD);
        apiRequest.setUriTemplate(TEST_DATA_URI_TEMPLATE);
        apiRequest.setResourceMethodAuthType(TEST_DATA_RESOURCE_METHOD_ENDPOINT_TYPE);
        apiRequest.setResourceMethodThrottlingTier(TEST_DATA_RESOURCE_METHOD_ENDPOINT_THROTTLING);
        apiRequest.setVisibility(TEST_DATA_API_VISIBILITY);
        apiRequest.setRoles(TEST_DATA_API_PROVIDER_NAME);

        //Add API
        apiPublisher.addAPI(apiRequest);

        //Update API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(TEST_DATA_APINAME,
                TEST_DATA_API_PROVIDER_NAME, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //getAPIManagePage
        HttpResponse managePage = apiPublisher.getAPIManagePage(TEST_DATA_APINAME, TEST_DATA_API_PROVIDER_NAME,
                TEST_DATA_API_VERSION);
        Assert.assertTrue(managePage.getData().contains(TEST_DATA_EXPECTED_ENDPOINT_TYPE),
                "Resource method endpoint type not set properly.");

    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        deleteAPI(apiIdentifier, apiPublisher);
    }
}
