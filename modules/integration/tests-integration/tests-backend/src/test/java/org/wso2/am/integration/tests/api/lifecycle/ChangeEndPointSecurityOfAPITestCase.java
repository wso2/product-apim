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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the endpoint security of APi and invoke. Endpoint application was developed to return thr security token in
 * the response body.
 */
public class ChangeEndPointSecurityOfAPITestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "ChangeEndPointSecurityOfAPITest";
    private final String API_CONTEXT = "ChangeEndPointSecurityOfAPI";
    private final String API_TAGS = "security, username, password";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeEndPointSecurityOfAPI";
    private HashMap<String, String> requestHeadersGet;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private String providerName;
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, RemoteException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/plain");
        requestHeadersGet.put("Content-Type", "text/plain");
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test the API with endpoint security enabled with simple password" +
            " that only has characters and numbers")
    public void testInvokeGETResourceWithSecuredEndPointPasswordOnlyNumbersAndLetters() throws Exception {
        String endpointUsername = "admin1";
        char[] endpointPassword = {'a', 'd', 'm', 'i', 'n', '1', '2', '3'};
        byte[] userNamePasswordByteArray = (endpointUsername + ":" + String.valueOf(endpointPassword)).getBytes();
        String encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);

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
        apiIdentifier.setTier(TIER_UNLIMITED);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/sec",
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "endpoint type secured. username:" + endpointUsername + " password:" + String.valueOf(endpointPassword));
        assertTrue(httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET" +
                " request for endpoint type secured. Expected value :" + encodedUserNamePassword + " not contains in " +
                "response data:" + httpResponseGet.getData() + "username:" + endpointUsername + " password:" +
                String.valueOf(endpointPassword));

    }


    @Test(groups = {"wso2.am"}, dataProvider = "SymbolCharacters", description = "Test the API with endpoint security" +
                                                                                 " enabled with complex password",
          dependsOnMethods = "testInvokeGETResourceWithSecuredEndPointPasswordOnlyNumbersAndLetters")
    public void testInvokeGETResourceWithSecuredEndPointComplexPassword()
            throws Exception {

        char[] symbolicCharacter = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '-', '+', '=', '{', '[',
                '}', ']', '|', '\\', ':', ';', '"', '\'', '<', ',', '>', '.', '?', '/'};
        for (int i = 0; i < symbolicCharacter.length; i++) {
            String endpointUsername = "user";
            char[] endpointPassword = {'a', 'b', 'c', 'd', symbolicCharacter[i], 'e', 'f', 'g', 'h', 'i', 'j', 'k'};
            byte[] userNamePasswordByteArray = (endpointUsername + ":" + String.valueOf(endpointPassword)).getBytes();
            String encodedUserNamePassword = DatatypeConverter.printBase64Binary(userNamePasswordByteArray);
            APICreationRequestBean apiCreationRequestBean =
                    new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
            apiCreationRequestBean.setTags(API_TAGS);
            apiCreationRequestBean.setDescription(API_DESCRIPTION);
            apiCreationRequestBean.setVisibility("public");
            apiCreationRequestBean.setEndpointType("secured");
            apiCreationRequestBean.setEpUsername(endpointUsername);
            apiCreationRequestBean.setEpPassword(URLEncoder.encode(String.valueOf(endpointPassword), "UTF-8"));
            //Update API with Edited information
            HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
            assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Update APi with new Resource " +
                    "information fail");
            assertEquals(updateAPIHTTPResponse.getData(), "{\"error\" : false}", "Update APi with new Resource information fail");
            //Send GET request

            waitForAPIDeployment();

            HttpResponse httpResponseGet =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/sec",
                            requestHeadersGet);
            assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                    "endpoint type secured. username:" + endpointUsername + " password:" + String.valueOf(endpointPassword));
            assertTrue(httpResponseGet.getData().contains(encodedUserNamePassword), "Response Data not match for GET" +
                    " request for endpoint type secured. Expected value : " + encodedUserNamePassword + " not contains in " +
                    "response data: " + httpResponseGet.getData() + " username:" + endpointUsername + " password:" +
                    String.valueOf(endpointPassword));
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
        super.cleanUp();
    }

}
