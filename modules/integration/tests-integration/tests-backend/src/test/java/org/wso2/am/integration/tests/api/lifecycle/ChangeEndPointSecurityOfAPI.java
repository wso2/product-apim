/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the endpoint security of APi and invoke. Endpoint application was developed to return thr security token in
 * the response body.
 */
public class ChangeEndPointSecurityOfAPI extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "security, username, password";
    private static final String API_END_POINT_URL = "http://localhost:9763/jaxrs_basic/services/customers/customerservice/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String ENDPOINT_USERNAME = "adminusername";
    private static final String ENDPOINT_PASSWORD = "adminpassword";
    private String APPLICATION_NAME = "ChangeEndPointSecurityOfAPI";
    private String accessToken;
    private HashMap<String, String> requestHeadersGet;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private String providerName;
    private String encodedUserNamePassword;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        String sourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                        File.separator + "configFiles" + File.separator + "lifecycletest" + File.separator + "jaxrs_basic.war";

        String targetPath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator + "deployment" +
                        File.separator + "server" + File.separator + "webapps";

        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(apimContext);

        FileManager.copyResourceToFileSystem(sourcePath, targetPath, "jaxrs_basic.war");
        serverConfigurationManager.restartGracefully();
        super.init();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        providerName = apimContext.getContextTenant().getContextUser().getUserName();
        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreServerURLHttp());
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/plain");
        requestHeadersGet.put("Content-Type", "text/plain");
        byte[] userNamePasswordByteArray = "adminsaneth+adminsanethpassword".getBytes();
        encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);


    }


    @Test(groups = {"wso2.am"}, description = "Test the API without endpoint security enabled")
    public void testInvokeGETResourceWithAuthTypeNonSecured() throws Exception {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        accessToken = getAccessToken(apiStoreClientUser1, APPLICATION_NAME);
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + "/sec",
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type None");
        assertTrue(!httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET request for" +
                " auth type Non3.  Value :\"" + encodedUserNamePassword + "\" contains in response data");

    }


    @Test(groups = {"wso2.am"}, description = "Test the API with endpoint security enabled",
            dependsOnMethods = "testInvokeGETResourceWithAuthTypeNonSecured")
    public void testInvokeGETResourceWithAuthTypeSecured() throws Exception {

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        // apiCreationRequestBean.setVersion(API_VERSION_1_0_0);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();

        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "None", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        apiCreationRequestBean.setEndpointType("secured");
        apiCreationRequestBean.setEpUsername(ENDPOINT_USERNAME);
        apiCreationRequestBean.setEpPassword(ENDPOINT_PASSWORD);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false", "Update APi with new Resource information fail");


        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + "/sec",
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type None");
        assertTrue(httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET request for" +
                " auth type Non3. Expected value :\"" + encodedUserNamePassword + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");

    }

}
