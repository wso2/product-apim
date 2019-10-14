/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;



public class APIM714GetAllDocumentationTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(APIM714GetAllDocumentationTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private static final String apiName = "DocumentTestAPI";
    private static final String apiVersion = "1.0.0";
    private final String applicationName = "NewApplication";
    private static final String apiContext = "documenttestapi";
    private final String tags = "document";
    private String tier= APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String resTier= APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED;

    private final String description = "testApiWithDocument";
    private String apiProvider;
    private static final String webApp = "jaxrs_basic";
    private String endpointUrl;
    private final String endPointType = "http";
    private final String visibility = "public";
    private int documentsCount;
    private String sourceTypeInLine = "";
    private String sourceTypeUrl = "";
    private String sourceTypeFile="";
    private String docType = "";
    private String docName = "";
    private String docNameWithUrl = "";
    private String docNameWithFile="";
    private String docUrl = "";
    private String sourceDocUrl = "";



    @Factory(dataProvider = "userModeDataProvider")
    public APIM714GetAllDocumentationTestCase(TestUserMode userMode) {
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
        log.info("Test Starting user mode:" + userMode);

        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        //publisher login
        HttpResponse publisherLogin = apiPublisher.login
                (publisherContext.getContextTenant().getContextUser().getUserName(),
                        publisherContext.getContextTenant().getContextUser().getPassword());
        assertEquals(publisherLogin.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Publisher Login Response Code is Mismatched: ");

        //store login
        HttpResponse loginResponse = apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        assertEquals(loginResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code is Mismatched in Login Response");
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Response data error in Login Request");


        String uri = "customers/{id}/";
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", resTier, uri));
        String endpoint = "/services/customers/customerservice";

        endpointUrl = gatewayUrlsWrk.getWebAppURLHttp() + webApp + endpoint;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                apiProvider, new URL(endpointUrl));
        apiCreationRequestBean.setEndpointType(endPointType);
        apiCreationRequestBean.setTier(tier);
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setVisibility(visibility);

        HttpResponse apiCreateResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");

        //assert JSON object
        JSONObject createApiJsonObject = new JSONObject(apiCreateResponse.getData());
        assertEquals(createApiJsonObject.getBoolean("error"), false, "Error in API Creation");

        HttpResponse verifyApiResponse = apiPublisher.getApi(apiName, apiProvider, apiVersion);
        JSONObject verifyApiJsonObject = new JSONObject(verifyApiResponse.getData());
        assertFalse(verifyApiJsonObject.getBoolean("error"), "Error in Verify API Response");

        //publish API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, apiProvider,
                APILifeCycleState.PUBLISHED);

        HttpResponse statusUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertEquals(statusUpdateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code is Mismatched");

        JSONObject statusUpdateJsonObject = new JSONObject(statusUpdateResponse.getData());
        assertFalse(statusUpdateJsonObject.getBoolean("error"), "API is not published");

        docName = "TestDocumentInLine";
        docType = "how to";
        sourceTypeInLine = "inline";
        String summary="TestFile";
        String docLocation = "";

        //add document with document type="how-to" and source type=inline;
        HttpResponse addDocumentResponse = apiPublisher.addDocument(apiName, apiVersion, apiProvider, docName, docType,
                sourceTypeInLine, docUrl, summary, docLocation);
        assertEquals(addDocumentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in Add Document Response Code");
        JSONObject addDocumentJsonObject = new JSONObject(addDocumentResponse.getData());
        assertFalse(addDocumentJsonObject.getBoolean("error"), "Response Error in Add Document: " + docName);

        //add document with document type="how-to" and source type=url
        sourceDocUrl = "https://docs.wso2.com/display/AM191/Tutorials";
        sourceTypeUrl = "url";
        docNameWithUrl = "TestDocumentWithUrl";

        HttpResponse addDocumentWithUrlResponse = apiPublisher.addDocument(apiName, apiVersion, apiProvider,
                docNameWithUrl, docType, sourceTypeUrl, sourceDocUrl, summary, docLocation);
        assertEquals(addDocumentWithUrlResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatched in Add Document");
        JSONObject addDocumentWithUrlJsonObject = new JSONObject(addDocumentWithUrlResponse.getData());
        assertFalse(addDocumentWithUrlJsonObject.getBoolean("error"), "Error in Add Document with Response");

        //add a document with doc_type="how to" and source type=file
//        docNameWithFile="TestDocumentWithTextFile";
//        sourceTypeFile="file";
//        docUrl="";
//        docType = "how to";
//        String mimeType="text/plain";
//        docLocation="";







        //no of documents
        documentsCount = 2;
    }


    @Test(groups = "webapp", description = "Get All Documents")
    public void testAllDocuments() throws Exception {

        apiProvider = storeContext.getContextTenant().getContextUser().getUserName();
        HttpResponse getAllDocumentsResponse = apiStore.getAllDocumentationOfAPI(apiName, apiVersion, apiProvider);
        assertEquals(getAllDocumentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in Get All Documents Response: " + apiName);
        JSONObject getAllDocumentsJsonObject = new JSONObject(getAllDocumentsResponse.getData());
        assertFalse(getAllDocumentsJsonObject.getBoolean("error"), "Error in Get All Documentation Response: " + apiName);
        JSONArray getAllDocumentJsonArray = getAllDocumentsJsonObject.getJSONArray("documentations");
        assertEquals(getAllDocumentJsonArray.length(), documentsCount, "Invalid Documents Count");
        //verify documents data
        boolean isDocumentWithUrlAppAvailable = false;
        boolean isDocumentAppAvailable = false;
        for (int arrayIndex = 0; arrayIndex < getAllDocumentJsonArray.length(); arrayIndex++) {
            if (getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("name").equals(docNameWithUrl)) {
                isDocumentWithUrlAppAvailable = true;
                assertTrue(getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("sourceType").equalsIgnoreCase
                                (sourceTypeUrl),
                        "Error in Source Type");
                assertTrue(getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("sourceUrl").contains
                                (sourceDocUrl),
                        "Error in Document Source Url: ");
                assertTrue(getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("type").equalsIgnoreCase(docType),
                        "Error in Doc Type: " + docType);

            } else if (getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("name").equals(docName)) {
                isDocumentAppAvailable = true;
                assertTrue(getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("sourceType").equalsIgnoreCase
                                (sourceTypeInLine),
                        "Error in Source Type");
                assertTrue(getAllDocumentJsonArray.getJSONObject(arrayIndex).getString("type").equalsIgnoreCase(docType),
                        "Error in Doc Type: " + docType);
            }
        }
        assertTrue(isDocumentWithUrlAppAvailable, "Error: Document With Url is not available");
        assertTrue(isDocumentAppAvailable, "Error: Document App is not available");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse apiDeleteResponse = apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);
        assertEquals(apiDeleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in API delete Response Code");
        JSONObject apiDeleteJsonObject = new JSONObject(apiDeleteResponse.getData());
        assertFalse(apiDeleteJsonObject.getBoolean("error"), "Response data Error in Api Deletion: " + apiName);
        super.cleanUp();
    }
}
