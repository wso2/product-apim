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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
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
    private APIPublisherRestClient apiPublisher;
    private String apiProvider;
    private String apiEndPointUrl;
    private HttpClient httpClient;
    private HttpPost httpPostLogin;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM614AddDocumentationToAnAPIWithDocTypeSampleAndSDKThroughPublisherRestAPITestCase
            (TestUserMode userMode) {
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
        String loginUrl = publisherUrls.getWebAppURLHttp()+"publisher/site/blocks/user/login/ajax/login.jag";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        httpClient = HttpClients.createDefault();
        httpPostLogin = new HttpPost(loginUrl);

        List<NameValuePair> loginValue = new ArrayList<NameValuePair>();
        loginValue.add(new BasicNameValuePair("action", "login"));
        loginValue.add(new BasicNameValuePair("username", "admin"));
        loginValue.add(new BasicNameValuePair("password", "admin"));

        httpPostLogin.setEntity(new UrlEncodedFormEntity(loginValue));

        //Validate whether the newly created client can login to the API publisher
        HttpResponse loginResponse = httpClient.execute(httpPostLogin);
        HttpEntity loginEntity = loginResponse.getEntity();
        JSONObject jsonObjectLogin = new JSONObject(EntityUtils.toString(loginEntity));
        assertFalse(jsonObjectLogin.getBoolean("error"),
                "Error when login to the Publisher Rest API using new client");

    }

    @Test(groups = {"wso2.am"}, description = "Create an API to update the documents with " +
            "source type file  through the publisher rest API ")
    public void testApiCreation() throws Exception {

        String apiContext = "apim614PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag614-1, tag622-2, tag624-3";

        //Create an API and validate it
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

        JSONObject jsonObject = new JSONObject(apiPublisher.
                addAPI(apiCreationRequestBean).getData());
        assertFalse(jsonObject.getBoolean("error"), apiName + " is not created ");

    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type HowTo And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testApiCreation")
    public void testAddDocumentToAnAPIHowToFile() throws Exception {

        String fileNameAPIM614 = "APIM614.txt";
        String docName = "APIM614PublisherTestHowTo-File-summary";
        String docType = "How To";
        String sourceType = "file";
        String summary = "Testing";
        String mimeType = "text/plain";
        String docUrl = "http://";
        String filePathAPIM614 =TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + fileNameAPIM614;;
        String addDocUrl = publisherUrls.getWebAppURLHttp()+"publisher/site/blocks/documentation/ajax/docs.jag";


        //Send Http Post request to add a new file
        HttpPost httppost = new HttpPost(addDocUrl);
        File file = new File(filePathAPIM614);
        FileBody fileBody = new FileBody(file,"text/plain");

        //Create multipart entity to upload file as multipart file
        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("docLocation", fileBody);
        multipartEntity.addPart("mode",new StringBody(""));
        multipartEntity.addPart("docName",new StringBody(docName));
        multipartEntity.addPart("docUrl",new StringBody(docUrl));
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

        //Upload created file and validate
        HttpResponse response = httpClient.execute(httppost);
        HttpEntity entity = response.getEntity();
        JSONObject jsonObject1 = new JSONObject(EntityUtils.toString(entity));
        assertFalse(jsonObject1.getBoolean("error"), "Error when adding files to the API ");

}

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type Sample SDK And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPIHowToFile")
    public void testAddDocumentToAnAPISDKToFile() throws Exception {

        String fileNameAPIM622 = "APIM622.txt";
        String docName = "APIM622PublisherTestHowTo-File-summary";
        String docType = "samples";
        String sourceType = "file";
        String summary = "Testing";
        String mimeType = "text/plain";
        String docUrl = "http://";
        String filePathAPIM622 = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + fileNameAPIM622;
        String addDocUrl = publisherUrls.getWebAppURLHttp() +"publisher/site/blocks/documentation/ajax/docs.jag";

        //Send Http Post request to add a new file
        HttpPost httppost = new HttpPost(addDocUrl);
        File file = new File(filePathAPIM622);
        FileBody fileBody = new FileBody(file,"text/plain");

        //Create multipart entity to upload file as multipart file
        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("docLocation", fileBody);
        multipartEntity.addPart("mode",new StringBody(""));
        multipartEntity.addPart("docName",new StringBody(docName));
        multipartEntity.addPart("docUrl",new StringBody(docUrl));
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

        //Upload created file and validate
        HttpResponse response = httpClient.execute(httppost);
        HttpEntity entity = response.getEntity();
        JSONObject jsonObject1 = new JSONObject(EntityUtils.toString(entity));
        assertFalse(jsonObject1.getBoolean("error"), "Error when adding files to the API ");
    }


    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type  public forum And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPISDKToFile")
    public void testAddDocumentToAnAPIPublicToFile() throws Exception {

        String fileNameAPIM624 = "APIM624.txt";
        String docName = "APIM624PublisherTestHowTo-File-summary";
        String docType = "public forum";
        String sourceType = "file";
        String summary = "Testing";
        String mimeType = "text/plain";
        String docUrl = "http://";
        String filePathAPIM624 = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + fileNameAPIM624;
        String addDocUrl = publisherUrls.getWebAppURLHttp()+"publisher/site/blocks/documentation/ajax/docs.jag";

        //Send Http Post request to add a new file
        HttpPost httppost = new HttpPost(addDocUrl);
        File file = new File(filePathAPIM624);
        FileBody fileBody = new FileBody(file,"text/plain");

        //Create multipart entity to upload file as multipart file
        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("docLocation", fileBody);
        multipartEntity.addPart("mode",new StringBody(""));
        multipartEntity.addPart("docName",new StringBody(docName));
        multipartEntity.addPart("docUrl",new StringBody(docUrl));
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

        //Upload created file and validate
        HttpResponse response = httpClient.execute(httppost);
        HttpEntity entity = response.getEntity();
        JSONObject jsonObject1 = new JSONObject(EntityUtils.toString(entity));
        assertFalse(jsonObject1.getBoolean("error"), "Error when adding files to the API ");
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type  support forum And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPIPublicToFile")
    public void testAddDocumentToAnAPISupportToFile() throws Exception {

        String fileNameAPIM626 = "APIM626.txt";
        String docName = "APIM626PublisherTestHowTo-File-summary";
        String docType = "support forum";
        String sourceType = "file";
        String summary = "Testing";
        String mimeType = "text/plain";
        String docUrl = "http://";
        String filePathAPIM626 = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + fileNameAPIM626;
        String addDocUrl = publisherUrls.getWebAppURLHttp()+"publisher/site/blocks/documentation/ajax/docs.jag";

        //Send Http Post request to add a new file
        HttpPost httppost = new HttpPost(addDocUrl);
        File file = new File(filePathAPIM626);
        FileBody fileBody = new FileBody(file,"text/plain");

        //Create multipart entity to upload file as multipart file
        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("docLocation", fileBody);
        multipartEntity.addPart("mode",new StringBody(""));
        multipartEntity.addPart("docName",new StringBody(docName));
        multipartEntity.addPart("docUrl",new StringBody(docUrl));
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

        //Upload created file and validate
        HttpResponse response = httpClient.execute(httppost);
        HttpEntity entity = response.getEntity();
        JSONObject jsonObject1 = new JSONObject(EntityUtils.toString(entity));
        assertFalse(jsonObject1.getBoolean("error"), "Error when adding files to the API ");
    }

    @Test(groups = {"wso2.am"}, description = "Add Documentation To An API With Type Other And" +
            " Source File through the publisher rest API ",
            dependsOnMethods = "testAddDocumentToAnAPISupportToFile")
    public void testAddDocumentToAnAPIOtherFile() throws Exception {

        String fileNameAPIM629 = "APIM629.txt";
        String docName = "APIM629PublisherTestHowTo-File-summary";
        String docType = "Other";
        String sourceType = "file";
        String newType = "Type APIM629";
        String summary = "Testing";
        String mimeType = "text/plain";
        String docUrl = "http://";
        String filePathAPIM629 = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + fileNameAPIM629;
        String addDocUrl = publisherUrls.getWebAppURLHttp()+"publisher/site/blocks/documentation/ajax/docs.jag";

        //Send Http Post request to add a new file
        HttpPost httppost = new HttpPost(addDocUrl);
        File file = new File(filePathAPIM629);
        FileBody fileBody = new FileBody(file,"text/plain");

        //Create multipart entity to upload file as multipart file
        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart("docLocation", fileBody);
        multipartEntity.addPart("mode",new StringBody(""));
        multipartEntity.addPart("docName",new StringBody(docName));
        multipartEntity.addPart("docUrl",new StringBody(docUrl));
        multipartEntity.addPart("sourceType",new StringBody(sourceType));
        multipartEntity.addPart("summary",new StringBody(summary));
        multipartEntity.addPart("docType",new StringBody(docType));
        multipartEntity.addPart("version",new StringBody(apiVersion));
        multipartEntity.addPart("apiName",new StringBody(apiName));
        multipartEntity.addPart("action",new StringBody("addDocumentation"));
        multipartEntity.addPart("provider",new StringBody(apiProvider));
        multipartEntity.addPart("mimeType",new StringBody(mimeType));
        multipartEntity.addPart("newType",new StringBody(newType));
        multipartEntity.addPart("optionsRadios",new StringBody(docType));
        multipartEntity.addPart("optionsRadios1",new StringBody(sourceType));
        multipartEntity.addPart("optionsRadios1",new StringBody(sourceType));

        httppost.setEntity(multipartEntity);

        //Upload created file and validate
        HttpResponse response = httpClient.execute(httppost);
        HttpEntity entity = response.getEntity();
        JSONObject jsonObject1 = new JSONObject(EntityUtils.toString(entity));
        assertFalse(jsonObject1.getBoolean("error"), "Error when adding files to the API ");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);
        super.cleanUp();
    }




}
