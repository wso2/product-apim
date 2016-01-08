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

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Related to Patch Automation  https://wso2.org/jira/browse/APIMANAGER-4081
 * This test class tests the outcome of the paginated search result
 */
public class APIMANAGER4081PaginationCountTestCase extends APIMIntegrationBaseTest {

    private String publisherURLHttp;
    private String tenantDomain = "paginationtest.com";
    private String[] APINames;
    private String providerName;
    private int numberOfAPIs = 24;
    private String APIVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() {

        try {
            super.init();
            publisherURLHttp = publisherUrls.getWebAppURLHttp();

            // create a tenant
            TenantManagementServiceClient tenantManagementServiceClient = new TenantManagementServiceClient(
                    keyManagerContext.getContextUrls().getBackEndUrl(), createSession(keyManagerContext));

            tenantManagementServiceClient.addTenant(tenantDomain,
                    keyManagerContext.getContextTenant().getTenantAdmin().getPassword(),
                    keyManagerContext.getContextTenant().getTenantAdmin().getUserName(), "demo");

        } catch (XPathExpressionException e) {
            assertTrue(false, "Error occurred while retrieving context. Pagination count test case failed.");
        } catch (APIManagerIntegrationTestException e) {
            assertTrue(false, "Error occurred while adding tenant. Pagination count test case failed.");
        } catch (RemoteException e) {
            assertTrue(false, "Error occurred while creating session. Pagination count test case failed.");
        } catch (TenantMgtAdminServiceExceptionException e) {
            assertTrue(false, "Error while getting tenant management service. Pagination count test case failed.");
        }
    }


    @Test(groups = {"wso2.am"}, description = "Pagination test case")
    public void testPagination() throws Exception {

        //creates an array to store the names of the APIs, for cleanup purpose
        APINames = new String[numberOfAPIs];
        boolean isLoginSuccess = false;
        boolean isPaginationCorrect = false;
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);
        HttpResponse storeLoginResponse = null;
        String successResponse = "{\"error\" : false}";

        //create 24 APIs and publish them; these APIs should be paginated as 10,10,4
        try {
            for (int i = 0; i < numberOfAPIs; i++) {
                String APIName = "PaginationTestAPI" + Integer.toString(i);
                //put the name of the API in the array so that we can refer it when deleting
                APINames [i] = APIName;
                String APIContext = "paginationTest" + Integer.toString(i);
                String tags = "pagination";
                String url = "https://localhost:9443/test";
                String description = "This is test API create by API manager integration test";

                String APIVersion = "1.0.0";

                providerName = publisherContext.getContextTenant().getTenantAdmin().getUserName()
                        + "@" + tenantDomain;
                apiPublisher = new APIPublisherRestClient(publisherURLHttp);

                apiPublisher.login
                        (publisherContext.getContextTenant().getTenantAdmin().getUserName() + "@" + tenantDomain,
                         publisherContext.getContextTenant().getTenantAdmin().getPassword());

                APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
                apiRequest.setTags(tags);
                apiRequest.setDescription(description);
                apiRequest.setVersion(APIVersion);
                apiRequest.setSandbox(url);
                apiRequest.setResourceMethod("GET");
                apiRequest.setProvider(providerName);
                apiPublisher.addAPI(apiRequest);

                //update the lifecycle of the API to Published state, so that is it visible in store
                APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName,
                                                                                      providerName, APILifeCycleState.PUBLISHED);
                apiPublisher.changeAPILifeCycleStatus(updateRequest);
            }

            for (int i = 0; i < numberOfAPIs; i++) {
                String apiName = "PaginationTestAPI" + Integer.toString(i);
                String apiVersion = "1.0.0";
                waitForAPIDeploymentSync(publisherContext.getContextTenant().getTenantAdmin().getUserName()
                                         + "@" + tenantDomain, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);
            }

            //give a short time to apply the changes and build index, otherwise it will not be visible in the store
            //Thread.sleep(60000);
            //after adding the APIs, send the request to log-in to the api store
            storeLoginResponse = apiStore.login("admin", "admin");

        } catch (XPathExpressionException e) {
            assertTrue(false, "Error occurred when retrieving context to add APIs. Pagination count test case failed.");
        } catch (APIManagerIntegrationTestException e) {
            assertTrue(false, "Error occurred while log-in to add APIs. Pagination count test case failed.");
        } catch (MalformedURLException e) {
            assertTrue(false, "Invalid service URL to add APIs. Pagination count test case failed.");

            String loginResponseCookie = null;
            //get the cookie related to the login
            if (storeLoginResponse != null) {
                loginResponseCookie = storeLoginResponse.getHeaders().get("Set-Cookie");
                if (successResponse.equals(storeLoginResponse.getData())) {
                    isLoginSuccess = true;
                }
            }

            //the second request to get APIs for the pages, is sent only if the login is successful and and cookie is ok
            if (isLoginSuccess && loginResponseCookie != null) {
                int offset = 10;
                //fetch the first page, this page should have 10 results
                int countInFirstPage = getPaginationElementsCount(storeURLHttp, loginResponseCookie, 0, offset);
                //fetch the 2nd page, this page should have 10 results
                int countInSecondPage = getPaginationElementsCount(storeURLHttp, loginResponseCookie, 10, offset);
                //fetch the 3nd page, this page should have 4 results
                int countInThirdPage = getPaginationElementsCount(storeURLHttp, loginResponseCookie, 20, offset);

                //testing the output; added 24 APIs should be paginated as 10,10,4
                if (countInFirstPage == 10 && countInSecondPage == 10 && countInThirdPage == 4) {
                    isPaginationCorrect = true;
                }
            }
            assertTrue(isPaginationCorrect, "Incorrect Pagination during API Search. Pagination count test case failed.");
        }
    }

    /**
     * This method is used to get the number of APIs to be displayed in each page
     *
     * @param storeUrl url of the API store
     * @param loginResponseCookie login response
     * @param start starting index of the API list
     * @param offset offset from the starting index of the API list
     * @return number of APIs in each page
     */
    private int getPaginationElementsCount(String storeUrl, String loginResponseCookie, int start, int offset)
            throws JSONException {

        String paginationUrl = storeUrl + "store/site/blocks/api/listing/ajax/list.jag?" +
                "action=getAllPaginatedPublishedAPIs&" +
                "tenant=" + tenantDomain +
                "&start=" + Integer.toString(start) +
                "&end=" + Integer.toString(offset);
        int numberOfAPIsInCurrentPage = 0;
        Map<String, String> paginationRequestHeaders = new HashMap<String, String>();
        paginationRequestHeaders.put("Cookie", loginResponseCookie);
        HttpResponse paginationFetchResponse = null;

        try {
            paginationFetchResponse = HttpRequestUtil.doGet(paginationUrl, paginationRequestHeaders);
        } catch (IOException e) {
            //if an error occurs while getting the response, the test is failed
            assertTrue(false, "Retrieving response for Pagination count test case has failed.");
        }

        String dataText = "";
        if(paginationFetchResponse != null) {
            dataText = paginationFetchResponse.getData();
        }

        JSONObject paginationDataObject = new JSONObject(dataText);

        // the data in response is evaluated further only if the response indicates successful API retrieval
        // that is response for the "error" tag should be "false"
        if ("false".equals(paginationDataObject.get("error").toString())) {
            numberOfAPIsInCurrentPage = paginationDataObject.getJSONArray("apis").length();
        }
        return  numberOfAPIsInCurrentPage;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (apiPublisher != null) {
            //take the names of the newly added APIs from the saved array and delete them
            for (int j = 0; j < numberOfAPIs; j++) {
                apiPublisher.deleteAPI(APINames[j], APIVersion, providerName);
            }
        }
        super.cleanUp();
    }

}
