/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CertMetadataDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CertificateInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CertificateValidityDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CertificatesDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.data.xsd.LogEvent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.xpath.XPathExpressionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * This testcase contains the test for endpoint Certificates.
 */
public class APIEndpointCertificateTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIEndpointCertificateTestCase.class);
    private final String API_NAME = "APIEndpointCertificateTestCase";
    private final String API_CONTEXT = "APIEndpointCertificateTestCase";
    private final String API_VERSION_1_0_0 = "1.0.0";
    int securedEndpointPort;
    String securedEndpointHost;
    String applicationId;
    String apiId;
    WireMockServer wireMockServer;
    private String accessToken;
    private LogViewerClient logViewerClient;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointCertificateTestCase(TestUserMode userMode) {

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
    public void initialize() throws Exception {

        super.init(userMode);
        securedEndpointHost = InetAddress.getLocalHost().getHostName();
        int lowerPortLimit = 9950;
        int upperPortLimit = 9999;
        securedEndpointPort = getAvailablePort(lowerPortLimit, upperPortLimit);
        if (securedEndpointPort == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " +
                    lowerPortLimit + "-" + upperPortLimit + " was found");
        }
        log.info("Selected port " + securedEndpointPort + " to start backend server");
        startSecureEndpoint(securedEndpointPort);
        String providerName = user.getUserName();
        String endpointURL = "https://localhost:" + securedEndpointPort + "/abc";
        //create Oauth Base App
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationDTO =
                restAPIStore.createApplication("APIEndpointCertificateTestCase", "Test Application",
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationId = applicationDTO.getData();
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointURL));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(providerName);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("oauth2");
        securitySchemes.add("api_key");
        apiRequest.setSecurityScheme(securitySchemes);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        restAPIStore.subscribeToAPI(apiId, applicationId, TIER_UNLIMITED);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        //generate keys
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        waitForAPIDeploymentSync(user.getUserName(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        AutomationContext autoContext = new AutomationContext();
        logViewerClient = new LogViewerClient(autoContext.getContextUrls().getBackEndUrl(),
                autoContext.getSuperTenant().getTenantAdmin().getUserName(),
                autoContext.getSuperTenant().getTenantAdmin().getPassword());
        logViewerClient.clearLogs();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke API without inserting Endpoint Certificate")
    public void testInvokeAPIWithoutUploadingEndpointCertificate() throws XPathExpressionException, IOException {
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0),
                requestHeaders);
        Assert.assertEquals(apiResponse.getResponseCode(), 500);
    }

    @Test(groups = {"wso2.am"}, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testInvokeAPIWithoutUploadingEndpointCertificate"})
    public void testUploadEndpointCertificate() throws ApiException {

        String cert = getAMResourceLocation() + File.separator + "endpointCertificate" + File.separator +
                "endpoint.cer";
        String cert2 = getAMResourceLocation() + File.separator + "endpointCertificate" + File.separator +
                "endpoint2.cer";
        File file = new File(cert);
        ApiResponse<CertMetadataDTO> httpResponse = restAPIPublisher.uploadEndpointCertificate(file, "endpoint-1",
                "https://localhost" +
                        ":" + securedEndpointPort);
        Assert.assertEquals(httpResponse.getStatusCode(), 201);
        CertMetadataDTO data = httpResponse.getData();
        Assert.assertEquals(data.getAlias(), "endpoint-1");
        Assert.assertEquals(data.getEndpoint(), "https://localhost" + ":" + securedEndpointPort);
        httpResponse = restAPIPublisher.uploadEndpointCertificate(new File(cert2), "endpoint-2", "https://localhost" +
                ":" + securedEndpointPort);
        Assert.assertEquals(httpResponse.getStatusCode(), 201);
        data = httpResponse.getData();
        Assert.assertEquals(data.getAlias(), "endpoint-2");
        Assert.assertEquals(data.getEndpoint(), "https://localhost" + ":" + securedEndpointPort);
    }

    @Test(groups = {"wso2.am"}, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testUploadEndpointCertificate"})
    public void testUploadSameEndpointCertificateInSameAlias() {

        String cert = getAMResourceLocation() + File.separator + "endpointCertificate" + File.separator + "endpoint" +
                ".cer";
        File file = new File(cert);
        try {
            ApiResponse<CertMetadataDTO> httpResponse = restAPIPublisher.uploadEndpointCertificate(file, "endpoint-1",
                    "https://localhost" +
                            ":" + securedEndpointPort);
            Assert.fail("Certificate insert twice  with same detail");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 409);
        }
    }

    @Test(groups = {"wso2.am"}, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testUploadEndpointCertificate"})
    public void testUploadExpiredCert() {

        String cert = getAMResourceLocation() + File.separator + "endpointCertificate" + File.separator + "expired.cer";
        File file = new File(cert);
        try {
            ApiResponse<CertMetadataDTO> httpResponse = restAPIPublisher.uploadEndpointCertificate(file, "expired-1",
                    "https://localhost" + ":" + securedEndpointPort);
            Assert.fail("Expired Certificate insert ");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            Assert.assertTrue(e.getResponseBody().contains("Error while adding the certificate. Certificate Expired."), e.getResponseBody());
        }
    }

    @Test(groups = {"wso2.am"}, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testUploadEndpointCertificate"})
    public void testSearchEndpointCertificates() throws ApiException, ParseException {

        String endpoint = "https://localhost" + ":" + securedEndpointPort;
        CertificatesDTO endpointCertificates = restAPIPublisher.getEndpointCertificiates(endpoint, null);
        Assert.assertNotNull(endpointCertificates.getCertificates());
        Assert.assertEquals(endpointCertificates.getCertificates().size(), 2);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("IST"));
        for (CertMetadataDTO certificate : endpointCertificates.getCertificates()) {
            Assert.assertEquals(certificate.getEndpoint(), endpoint);
            if ("endpoint-1".equals(certificate.getAlias())) {
                CertificateInfoDTO certificateInfoDTO = restAPIPublisher.getendpointCertificateContent("endpoint-1");
                Assert.assertEquals(certificateInfoDTO.getStatus(), "Active");
                Assert.assertEquals(certificateInfoDTO.getSubject(), "CN=localhost, OU=localhost, C=LK");
                Assert.assertEquals(certificateInfoDTO.getVersion(), "3");
                Date to = simpleDateFormat.parse("Thu May 06 23:41:14 IST 2032");
                Date from = simpleDateFormat.parse("Fri May 06 23:41:14 IST 2022");
                SimpleDateFormat convertedSimpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                Assert.assertEquals(certificateInfoDTO.getValidity(),
                        new CertificateValidityDTO().to(convertedSimpleDateFormat.format(to)).from(convertedSimpleDateFormat.format(from)));
            }
        }
        endpointCertificates = restAPIPublisher.getEndpointCertificiates(null, "endpoint-2");
        Assert.assertNotNull(endpointCertificates.getCertificates());
        Assert.assertEquals(endpointCertificates.getCertificates().size(), 1);
        for (CertMetadataDTO certificate : endpointCertificates.getCertificates()) {
            Assert.assertEquals(certificate.getEndpoint(), endpoint);
            if ("endpoint-2".equals(certificate.getAlias())) {
                CertificateInfoDTO certificateInfoDTO = restAPIPublisher.getendpointCertificateContent("endpoint-2");
                Assert.assertEquals(certificateInfoDTO.getStatus(), "Active");
                Assert.assertEquals(certificateInfoDTO.getSubject(), "CN=wso2apim, OU=integration, O=WSO2," +
                        " ST=Colombo, C=LK");
                Assert.assertEquals(certificateInfoDTO.getVersion(), "3");
                Date to = simpleDateFormat.parse("Fri May 07 00:31:00 IST 2032");
                Date from = simpleDateFormat.parse("Sat May 07 00:31:00 IST 2022");
                SimpleDateFormat convertedSimpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

                Assert.assertEquals(certificateInfoDTO.getValidity(),
                        new CertificateValidityDTO().to(convertedSimpleDateFormat.format(to)).from(convertedSimpleDateFormat.format(from)));
            }
        }
        endpointCertificates = restAPIPublisher.getEndpointCertificiates(null, "endpoint-3");
        Assert.assertNotNull(endpointCertificates.getCertificates());
        Assert.assertEquals(endpointCertificates.getCertificates().size(), 0);
        endpointCertificates = restAPIPublisher.getEndpointCertificiates("https://abc.com", null);
        Assert.assertNotNull(endpointCertificates.getCertificates());
        Assert.assertEquals(endpointCertificates.getCertificates().size(), 0);
        endpointCertificates = restAPIPublisher.getEndpointCertificiates("https://localhost", null);
        Assert.assertNotNull(endpointCertificates.getCertificates());
        Assert.assertEquals(endpointCertificates.getCertificates().size(), 2);
        endpointCertificates = restAPIPublisher.getEndpointCertificiates(endpoint + "/api/v1", null);
        Assert.assertNotNull(endpointCertificates.getCertificates());
        Assert.assertEquals(endpointCertificates.getCertificates().size(), 2);
    }

    @Test(groups = {"wso2.am"}, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testSearchEndpointCertificates"})
    public void testInvokeAPI() throws ApiException, InterruptedException, XPathExpressionException, IOException {

        // Thread.sleep(60000); // Sleep to reload the transport
        // Wait for SSLProfile with the uploaded certificate to be reloaded in Gateway
        waitForSSLProfileReload();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0),
                requestHeaders);
        Assert.assertEquals(apiResponse.getResponseCode(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testInvokeAPI"})
    public void testInvokeAPIAfterRemovingCertificate() throws InterruptedException, XPathExpressionException,
            IOException, ApiException {

        ApiResponse<Void> response = restAPIPublisher.deleteEndpointCertificate("endpoint-1");
        Assert.assertEquals(response.getStatusCode(), 200);
        response = restAPIPublisher.deleteEndpointCertificate("endpoint-2");
        Assert.assertEquals(response.getStatusCode(), 200);
        // Thread.sleep(60500); // Sleep to reload the transport
        waitForSSLProfileReload();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        HttpResponse apiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0),
                requestHeaders);
        Assert.assertEquals(apiResponse.getResponseCode(), 500);
    }

    @Test(groups = { "wso2.am" }, description = "test Upload Endpoint Certificate", dependsOnMethods = {
            "testInvokeAPIAfterRemovingCertificate" })
    public void testDeleteNotAvailableCert() {

        try {
            restAPIPublisher.deleteEndpointCertificate("endpoint-negative");
            Assert.fail("Failing due to certificate available");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 404);
            ;
        }
    }

    private void startSecureEndpoint(int securedEndpointPort) {

        String jksPath = getAMResourceLocation() + File.separator + "endpointCertificate" + File.separator +
                "endpoint.jks";
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration();
        wireMockConfiguration.httpsPort(securedEndpointPort);
        wireMockConfiguration.keystorePath(jksPath);
        wireMockConfiguration.keystorePassword("wso2carbon");
        wireMockConfiguration.keyManagerPassword("wso2carbon");
        wireMockConfiguration.keystoreType("JKS");
        wireMockServer = new WireMockServer(wireMockConfiguration);
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/abc")).willReturn(aResponse().withHeader("Content" +
                "-Type", "text/plain").withBody("Hello world!")));
        wireMockServer.start();
    }

    private void waitForSSLProfileReload() throws RemoteException, InterruptedException {

        LogEvent[] logEvents;
        // Initial wait for the event to likely happen
        Thread.sleep(60000);

        int retryAttempt = 0;
        boolean isSSProfileReloaded = false;

        while (retryAttempt < 5 && !isSSProfileReloaded) {
            // FIX: Fetch logs INSIDE the loop to get the latest updates
            logEvents = logViewerClient.getAllRemoteSystemLogs();

            for (LogEvent logEvent : logEvents) {
                if (logEvent.getMessage().contains("PassThroughHttpSender reloading SSL Config")) {
                    isSSProfileReloaded = true;
                    log.info("SSLProfile has been reloaded successfully");
                    logViewerClient.clearLogs();
                    break;
                }
            }

            if (!isSSProfileReloaded) {
                retryAttempt++;
                log.info("SSLProfile has not been reloaded. Retry attempt - " + retryAttempt);
                Thread.sleep(12000); // Wait before fetching logs again
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws ApiException {

        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        wireMockServer.stop();
    }
}
