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

import org.apache.axiom.om.OMElement;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
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
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Add Documentation To An API using source type File Through Publisher Rest API
 * APIM2 -614 / APIM2 -622 /APIM2 -624 /APIM2 -626 /APIM2 -629
 */
public class APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
        extends APIMIntegrationBaseTest{

    private final String apiName = "APIM614PublisherTest";
    private final String apiVersion = "1.0.0";
    private final String fileName = "/home/bhagya/WS/apiDocumentation.txt";
    private APIPublisherRestClient apiPublisher;
    private String apiProvider;
    private String apiEndPointUrl;
    private String backendURL;
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    @Factory(dataProvider = "userModeDataProvider")
    public APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
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

               AuthenticatorClient login = new AuthenticatorClient
                       (gatewayContextMgt.getContextUrls().getBackEndUrl());
        String session = login.login("admin", "admin", "localhost");

         Thread.sleep(5000);

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiEndPointUrl = gatewayUrlsMgt.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        requestHeaders = new HashMap<String, String>();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API to update the documents with " +
            "source type file  through the publisher rest API ")
    public void testApiCreation() throws Exception {

        String apiContext = "apim614PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag614-1, tag622-2, tag624-3";
        String docName = "APIM611PublisherTestHowTo-Inline-summary";
        String docType = "samples";
        String sourceType = "Inline";

        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersion, apiProvider,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api620b");
        apiCreationRequestBean.setBizOwnerMail("api620b@ee.com");
        apiCreationRequestBean.setTechOwner("api620t");
        apiCreationRequestBean.setTechOwnerMail("api620t@ww.com");

        apiPublisher.addAPI(apiCreationRequestBean);

    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPIHowToFile() throws Exception {

        String docName = "APIM622PublisherTestHowTo-File-summary";
        String docType = "samples";
        String sourceType = "file";
        String summary = "Testing";
        String mimeType = "text/plain";
        String docUrl = "http://";
        String docLocation = "filename=\"apiDocumentation.txt\" Content-Type: text/plain apiDocumentation - APIM2-622:Add documentation to an API [ sample & SDK | File ] through the publisher REST api";
        String url = "https://localhost:9443/publisher/site/blocks/documentation/ajax/docs.jag";
        InputStream is = null;
        String results = null;

        String reqParameters ="action=addDocumentation&provider=" + apiProvider + "&apiName=" + apiName + "&version=" + apiVersion +
                "&docName=" + docName + "&docType=" + docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl +
                "&summary=" + summary  + "&mimeType=" + mimeType+"" ;


        HttpClient httpclient = getThreadSafeClient();
        HttpPost httppost = new HttpPost(url);


        File file = new File(fileName);
        FileBody fileBody = new FileBody(file,"text/plain");
//        requestHeaders.put("Content-Type", fileBody.getMimeType());
//        httppost.addHeader((Header) requestHeaders);


        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("docLocation", fileBody);
        multipartEntity.addPart("mode",new StringBody(""));
        multipartEntity.addPart("docName",new StringBody(docName));
        multipartEntity.addPart("docUrl",new StringBody("http://"));
        multipartEntity.addPart("sourceType",new StringBody(sourceType));
        multipartEntity.addPart("summary",new StringBody(summary));
        multipartEntity.addPart("docType",new StringBody(docType));
        multipartEntity.addPart("version",new StringBody(apiVersion));
        multipartEntity.addPart("apiName",new StringBody(apiName));
        multipartEntity.addPart("action",new StringBody("addDocumentation"));
        multipartEntity.addPart("provider",new StringBody(apiProvider));
        multipartEntity.addPart("mimeType",new StringBody(mimeType));
        multipartEntity.addPart("optionsRadios",new StringBody(docType));
        multipartEntity.addPart("optionsRadios1",new StringBody(sourceType));
        multipartEntity.addPart("optionsRadios1",new StringBody(sourceType));

        httppost.setEntity(multipartEntity);
//        httppost.setParams(reqParameters);
        httppost.addHeader("Content-Type", multipartEntity.getContentType().getValue());
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

}

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPISDKToFile() throws Exception {

        String docName = "APIM614PublisherTestHowTo-File-summary";
        String docType = "How To";
        String sourceType = "file";
        String docLocation="installations.txt";

        //Add Documentation to an API "APIM611PublisherTest" - How To | Url
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse docResponse = apiPublisher.addDocument
                (apiName,apiVersion,apiProvider,docName,docType,sourceType,"","Testing",docLocation,"","");
        JSONObject jsonObjectDoc1 = new JSONObject( docResponse.getData());
        assertFalse(jsonObjectDoc1.getBoolean("error"), "Error when adding document with source" +
                " file to the API");
    }


    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type  public forum And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPIPublicToFile() throws Exception {

        String docName = "APIM624PublisherTestHowTo-File-summary";
        String docType = "public forum";
        String sourceType = "file";
        String docLocation="installations.txt";

        //Add Documentation to an API "APIM611PublisherTest" -  public forum | File
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse docResponse = apiPublisher.addDocument
                (apiName,apiVersion,apiProvider,docName,docType,sourceType,"","Testing",docLocation,"","");
        JSONObject jsonObjectDoc1 = new JSONObject( docResponse.getData());
        assertFalse(jsonObjectDoc1.getBoolean("error"), "Error when adding document with source" +
                " file to the API");
    }


    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type  support forum And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPISupportToFile() throws Exception {

        String docName = "APIM626PublisherTestHowTo-File-summary";
        String docType = "support forum";
        String sourceType = "file";
        String docLocation="installations.txt";

        //Add Documentation to an API "APIM611PublisherTest" -  support forum | File
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse docResponse = apiPublisher.addDocument
                (apiName,apiVersion,apiProvider,docName,docType,sourceType,"","Testing",docLocation,"","");
        JSONObject jsonObjectDoc1 = new JSONObject( docResponse.getData());
        assertFalse(jsonObjectDoc1.getBoolean("error"), "Error when adding document with source" +
                " file to the API");
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPIOtherFile() throws Exception {

        String docName = "APIM629PublisherTestHowTo-File-summary";
        String docType = "Other";
        String sourceType = "file";
        String docLocation="installations.txt";
        String newType = "Type APIM628";

        //Add Documentation to an API "APIM611PublisherTest" - How To | Url
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse docResponse = apiPublisher.addDocument
                (apiName,apiVersion,apiProvider,docName,docType,sourceType,"","Testing",docLocation,"",newType);
        JSONObject jsonObjectDoc1 = new JSONObject( docResponse.getData());
        assertFalse(jsonObjectDoc1.getBoolean("error"), "Error when adding document with source" +
                " file to the API");
    }



    //    public HttpResponse sentMultiPartPostRequest() throws IOException {
//
//        String docName = "APIM622PublisherTestHowTo-File-summary";
//        String docType = "samples";
//        String sourceType = "file";
//        String summary = "Testing";
//        String mimeType = "text/plain";
//        String url = "http://localhost:9763/publisher/site/blocks/documentation/ajax/docs.jag";
//
//        HttpClient httpclient = new DefaultHttpClient();
//        HttpPost httppost = new HttpPost("http://localhost:9763/publisher/site/blocks/documentation/ajax/docs.jag");
//
//        FileBody bin = new FileBody(new File(fileName));
//
//        MultipartEntity multipartEntity = new MultipartEntity();
//        multipartEntity.addPart("docLocation", bin);
//        multipartEntity.addPart("docName",new StringBody(docName));
//        multipartEntity.addPart("docUrl",new StringBody("http://"));
//        multipartEntity.addPart("sourceType",new StringBody(sourceType));
//        multipartEntity.addPart("summary",new StringBody(summary));
//        multipartEntity.addPart("docType",new StringBody(docType));
//        multipartEntity.addPart("version",new StringBody(apiVersion));
//        multipartEntity.addPart("apiName",new StringBody(apiName));
//        multipartEntity.addPart("action",new StringBody("addDocumentation"));
//        multipartEntity.addPart("provider",new StringBody(apiProvider));
//        multipartEntity.addPart("mimeType",new StringBody(mimeType));
//        httppost.setEntity(multipartEntity);
//
//        return httpclient.execute(httppost);
//        HttpEntity resEntity = response.getEntity();
//    }

    public static DefaultHttpClient getThreadSafeClient()  {

        DefaultHttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,

                mgr.getSchemeRegistry()), params);
        return client;
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);

    }




}
