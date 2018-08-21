/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.restapi.testcases;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.tests.restapi.utils.RESTAPITestUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import static org.testng.Assert.assertEquals;

/**
 * This class holds the integration tests for Dynamic Certificate Management Rest api.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class CertificateAPITestCase extends APIMIntegrationBaseTest {

    private static final String SCOPE = "apim:api_create";
    private static final String CERTIFICATE_FILE_NAME = "testCert.crt";
    private static final String CERTIFICATE_MGT_RESOURCE_URL = "/api/am/publisher/v0.13/certificates";
    private static final String PARAM_ALIAS = "alias";
    private static final String PARAM_ENDPOINT = "endpoint";
    private static final String TEST_ALIAS = "wso2";
    private static final String TEST_ENDPOINT = "https://www.wso2.com";
    private static final String PARAM_CERTIFICATE = "certificate";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-type";
    private String certificateFile = TestConfigurationProvider.getResourceLocation() + File.separator + "certificate" +
            File.separator + CERTIFICATE_FILE_NAME;
    private String bearerToken = null;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();
        //Generate access tokens.
        String keyManagerURLHttp = getKeyManagerURLHttps();
        String gatewayURL = getGatewayURLNhttp();
        Map<String, String> dataMap = RESTAPITestUtil.registerOAuthApplication(keyManagerURLHttp);
        String accessToken = RESTAPITestUtil.generateOAuthAccessToken(SCOPE, dataMap, gatewayURL);
        bearerToken = "Bearer " + accessToken;
    }

    @Test(groups = {"wso2.am"}, description = "REST API Implementation test : Add Certificate Test case")
    public void testAddCertificate() throws IOException, JSONException {

        File file = new File(certificateFile);
        FileBody fileBody = new FileBody(file, MediaType.APPLICATION_OCTET_STREAM);
        String addCertificateUrl = getKeyManagerURLHttp() + CERTIFICATE_MGT_RESOURCE_URL;

        HttpPost httpPost = new HttpPost(addCertificateUrl);
        httpPost.setHeader(AUTHORIZATION_HEADER, bearerToken);
        httpPost.setHeader(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA);

        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart(PARAM_CERTIFICATE, fileBody);
        multipartEntity.addPart(PARAM_ALIAS, new StringBody(TEST_ALIAS));
        multipartEntity.addPart(PARAM_ENDPOINT, new StringBody(TEST_ENDPOINT));

        httpPost.setEntity(multipartEntity);
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpPost);
        assertEquals(response.getStatusLine().getStatusCode(), 201);
    }

    @Test(groups = {"wso2.am"}, dependsOnMethods = {"testAddCertificate"}, description = "REST API Implementation " +
            "test : Update Certificate test case")
    public void testUpdateCertificate() throws IOException, JSONException {

        File file = new File(certificateFile);
        FileBody fileBody = new FileBody(file, MediaType.APPLICATION_OCTET_STREAM);

        String addCertificateUrl = getKeyManagerURLHttp() + CERTIFICATE_MGT_RESOURCE_URL + "/" + TEST_ALIAS;
        HttpPut httpPut = new HttpPut(addCertificateUrl);
        httpPut.setHeader(CONTENT_TYPE_HEADER, MediaType.MULTIPART_FORM_DATA);
        httpPut.setHeader(AUTHORIZATION_HEADER, bearerToken);

        MultipartEntity multipartEntity = new MultipartEntity();
        multipartEntity.addPart(PARAM_CERTIFICATE, fileBody);
        multipartEntity.addPart(PARAM_ALIAS, new StringBody(TEST_ALIAS));
        httpPut.setEntity(multipartEntity);

        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpPut);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test(groups = {"wso2.am"}, dependsOnMethods = {"testUpdateCertificate"}, description = "REST API Implementation " +
            "test : Get Certificates test case")
    public void testGetCertificates() throws IOException, JSONException {

        String getCertificateDetailsUrl = getKeyManagerURLHttp() + CERTIFICATE_MGT_RESOURCE_URL;
        HttpGet httpGet = new HttpGet(getCertificateDetailsUrl);

        httpGet.setHeader(AUTHORIZATION_HEADER, bearerToken);
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test(groups = {"wso2.am"}, dependsOnMethods = {"testGetCertificates"}, description = "REST API Implementation " +
            "test : Get Certificate details test case.")
    public void testGetCertificateDetails() throws IOException, JSONException {

        String getCertificateDetailsUrl = getKeyManagerURLHttp() + CERTIFICATE_MGT_RESOURCE_URL + "/" + TEST_ALIAS;
        HttpGet httpGet = new HttpGet(getCertificateDetailsUrl);

        httpGet.setHeader(AUTHORIZATION_HEADER, bearerToken);
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test(groups = {"wso2.am"}, dependsOnMethods = {"testGetCertificateDetails"},
            description = "REST API Implementation test : Get Certificate content test case.")
    public void testGetCertificateContent() throws IOException, JSONException {

        String getCertificateDetailsUrl = getKeyManagerURLHttp() + CERTIFICATE_MGT_RESOURCE_URL + "/" + TEST_ALIAS
                + "/content";
        HttpGet httpGet = new HttpGet(getCertificateDetailsUrl);

        httpGet.setHeader(AUTHORIZATION_HEADER, bearerToken);
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test(groups = {"wso2.am"}, dependsOnMethods = {"testGetCertificateContent"},
            description = "REST API Implementation test : Delete certificate test case.")
    public void testDeleteCertificate() throws IOException, JSONException {

        String getCertificateDetailsUrl = getKeyManagerURLHttp() + CERTIFICATE_MGT_RESOURCE_URL + "/" + TEST_ALIAS;
        HttpDelete httpDelete = new HttpDelete(getCertificateDetailsUrl);

        httpDelete.setHeader(AUTHORIZATION_HEADER, bearerToken);
        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpDelete);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        super.cleanUp();
    }
}
