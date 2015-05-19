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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the endpoint security of APi and invoke. Endpoint application was developed to return thr security token in
 * the response body.
 */
public class ChangeEndPointSecurityOfAPITestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "security, username, password";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "ChangeEndPointSecurityOfAPI";
    private HashMap<String, String> requestHeadersGet;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private String providerName;
    private String apiEndPointUrl;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        String sourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "lifecycletest" + File.separator + "jaxrs_basic.war";
        String targetPath =
                CARBON_HOME + File.separator + "repository" + File.separator + "deployment" + File.separator +
                        "server" + File.separator + "webapps";
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        FileManager.copyResourceToFileSystem(sourcePath, targetPath, "jaxrs_basic.war");
        serverConfigurationManager.restartGracefully();
        super.init();
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/plain");
        requestHeadersGet.put("Content-Type", "text/plain");
    }


    @Test(groups = {"wso2.am"}, description = "Test the API with endpoint security enabled with simple password" +
            " that only has characters and numbers")
    public void testInvokeGETResourceWithSecuredEndPointPasswordOnlyNumbersAndLetters() throws
            APIManagerIntegrationTestException, IOException {
        String endpointUsername = "admin1";
        char[] endpointPassword = {'a', 'd', 'm', 'i', 'n', '1', '2', '3'};
        byte[] userNamePasswordByteArray = (endpointUsername + ":" + endpointPassword).getBytes();
        String encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_UNLIMITED, "", "");
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setEndpointType("secured");
        apiCreationRequestBean.setEpUsername(endpointUsername);
        apiCreationRequestBean.setEpPassword(String.valueOf(endpointPassword));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + "/sec",
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "endpoint type secured. username:" + endpointUsername + " password:" + String.valueOf(endpointPassword));
        assertTrue(httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET" +
                " request for endpoint type secured. Expected value :" + encodedUserNamePassword + " not contains in " +
                "response data:" + httpResponseGet.getData() + "username:" + endpointUsername + " password:" +
                String.valueOf(endpointPassword));

    }


    @Test(groups = {"wso2.am"}, dataProvider = "SymbolCharacters", description = "Test the API with endpoint security enabled with" +
            " complex password", dependsOnMethods = "testInvokeGETResourceWithSecuredEndPointPasswordOnlyNumbersAndLetters")
    public void testInvokeGETResourceWithSecuredEndPointComplexPassword(String st) throws Exception {
        String endpointUsername = "user";
        char[] endpointPassword = {'a', 'b', 'c', 'd', st.charAt(0), 'e', 'f', 'g', 'h', 'i', 'j', 'k'};
        byte[] userNamePasswordByteArray = (endpointUsername + ":" + endpointPassword).getBytes();
        String encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility("public");
        apiCreationRequestBean.setEndpointType("secured");
        apiCreationRequestBean.setEpUsername(endpointUsername);
        apiCreationRequestBean.setEpPassword(String.valueOf(endpointPassword));
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Update APi with new Resource " +
                "information fail");
        assertEquals(updateAPIHTTPResponse.getData(), "{\"error\" : false}", "Update APi with new Resource information fail");
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + "/sec",
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "endpoint type secured. username:" + endpointUsername + " password:" + String.valueOf(endpointPassword));
        assertTrue(httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET" +
                " request for endpoint type secured. Expected value : " + encodedUserNamePassword + " not contains in " +
                "response data: " + httpResponseGet.getData() + " username:" + endpointUsername + " password:" +
                String.valueOf(endpointPassword));
    }


    @DataProvider(name = "SymbolCharacters")
    public static Object[][] getSymbolCharacters() {
        return new Object[][]{
                {"!"}, {"@"}, {"#"}, {"$"}, {"%"}, {"^"}, {"&"}, {"*"}, {"("}, {")"}, {"_"}, {"-"}, {"+"}, {"="},
                {"{"}, {"["}, {"}"}, {"]"}, {"|"}, {"\\"}, {":"}, {";"}, {"\""}, {"'"}, {"<"}, {","}, {">"}, {"."},
                {"?"}, {"/"}};
    }

}
