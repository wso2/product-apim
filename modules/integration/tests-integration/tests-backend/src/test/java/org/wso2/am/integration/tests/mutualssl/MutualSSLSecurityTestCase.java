package org.wso2.am.integration.tests.mutualssl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class MutualSSLSecurityTestCase extends APIManagerLifecycleBaseTest {
    private final String mutualSSLOnlyAPIName = "mtls";
    private final String mutualSSLOnlyAPIContext = "mtls";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private final String API_END_POINT_METHOD = "/customers/123";
    private String apiId1;
    private String certId;
    public MutualSSLSecurityTestCase() {

    }

    @BeforeClass(alwaysRun = true)
    public void initialize()
            throws APIManagerIntegrationTestException, IOException, ApiException,
            org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException, AutomationUtilException,
            InterruptedException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest1 = new APIRequest(mutualSSLOnlyAPIName, mutualSSLOnlyAPIContext, new URL(apiEndPointUrl));
        apiRequest1.setVersion(API_VERSION_1_0_0);
        apiRequest1.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest1.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest1.setProvider(user.getUserName());
        apiRequest1.setDefault_version("true");
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest1.setOperationsDTOS(operationsDTOS);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("mutualssl");
        securitySchemes.add("mutualssl_mandatory");
        apiRequest1.setSecurityScheme(securitySchemes);
        apiRequest1.setDefault_version("true");
        apiRequest1.setHttps_checked("https");
        apiRequest1.setHttp_checked(null);
        apiRequest1.setDefault_version_checked("true");
        HttpResponse response1 = restAPIPublisher.addAPI(apiRequest1);
        apiId1 = response1.getData();
        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        HttpResponse certificateResponse = restAPIPublisher.uploadCertificate(new File(certOne), "example", apiId1,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        certId = certificateResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId1,false);
        Thread.sleep(120000);
    }
    @Test(description = "API invocation with mutual ssl Certificate")
    public void testAPIInvocationWithMutualSSLCertificate() throws IOException, XPathExpressionException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        HttpResponse response = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIContext, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                requestHeaders);
        HttpResponse defaultResponse = HTTPSClientUtils.doMutulSSLGet(
                getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                        + File.separator + "new-keystore.jks",
                getAPIInvocationURLHttps(mutualSSLOnlyAPIContext) + API_END_POINT_METHOD,
                requestHeaders);

        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
    }

    @Test(description = "API invocation with mutual ssl Certificate Header.")
    public void testAPIInvocationWithMutualSSLHeader() throws IOException, XPathExpressionException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("x-client-cert", retrieveUnEncodedCertificate());
        HttpResponse response = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLOnlyAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        HttpResponse defaultResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLOnlyAPIContext) + API_END_POINT_METHOD,
                        requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_OK,
                "Mutual SSL Authentication has not succeed");
    }

    @Test(description = "API invocation with mutual ssl Certificate Header.")
    public void testAPIInvocationWithMutualSSLHeaderNegative() throws IOException, XPathExpressionException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("x-client-cert", retrieveEncodedCertificate());
        HttpResponse response = HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLOnlyAPIContext,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        HttpResponse defaultResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttps(mutualSSLOnlyAPIContext) + API_END_POINT_METHOD,
                        requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Mutual SSL Authentication has not succeed");
        Assert.assertEquals(defaultResponse.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Mutual SSL Authentication has not succeed");
    }

    public String retrieveUnEncodedCertificate() throws IOException {

        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        String base64EncodedString = IOUtils.toString(new FileInputStream(certOne));
        base64EncodedString = base64EncodedString.replace("\n"," ");
        return base64EncodedString;
    }

    public String retrieveEncodedCertificate() throws IOException {
        String certOne = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        String base64EncodedString = IOUtils.toString(new FileInputStream(certOne));
        base64EncodedString = Base64.encodeBase64URLSafeString(base64EncodedString.getBytes());
        return base64EncodedString;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws IOException, AutomationUtilException, ApiException {
        restAPIPublisher.deleteAPI(apiId1);
    }
    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }


}
