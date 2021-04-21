package org.wso2.am.integration.tests.application;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.testng.Assert.*;
import static org.testng.Assert.assertTrue;

public class ApplicationCertificateManagerTestCase extends APIManagerLifecycleBaseTest{
    private static final Log log = LogFactory.getLog(ApplicationTestCase.class);
    private static final String webApp = "jaxrs_basic";
    private final String tier = "Unlimited";
    private final String keyType = "PRODUCTION";
    private final String applicationName = "NewApplicationTest";
    private String apiName = "CertificateAPITest";
    private String apiContext = "subscriptionapicontext";
    private  String nameOne = "defaultOne_1";
    private  String nameTwo = "defaultTwo_1";
    private String applicationId;
    private String UUID;



    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationCertificateManagerTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }};
    }
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception{
        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "Test Application", tier,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();


    }

    @Test(groups = {"webapp"}, description = "Upload a Certificate to an Application")
    public void testUploadCertificateToApplication() throws Exception{

        String Certificate = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        HttpResponse response = restAPIStore.uploadCertificate(new File(Certificate), nameTwo, applicationId, keyType);
        UUID = response.getData();
        assertNotNull(response.getData(),"Failed to add the certificate");
    }

    @Test(groups = {"webapp"}, description = "Get Certificates By Application Id", dependsOnMethods = "testUploadCertificateToApplication")
    public void testGetCertificatesByApplicationId() throws Exception {

        HttpResponse response = restAPIStore.getCertificates(applicationId);
        assertEquals(response.getResponseCode(),HTTP_RESPONSE_CODE_OK,"ResponseCode mismatched when getting the certificates");
    }

    @Test(groups = {"webapp"}, description = "Get Certificate By UUID", dependsOnMethods ="testUploadCertificateToApplication")
    public void testGetCertificatesByUUID() throws Exception {

        HttpResponse response = restAPIStore.getCertificateByUUID(UUID,applicationId);
        assertEquals(response.getResponseCode(),HTTP_RESPONSE_CODE_OK,"ResponseCode mismatched when getting the certificate with UUID " + UUID);
    }

    @Test(groups = {"webapp"}, description = "Delete a Certificate By UUID", dependsOnMethods = {"testUploadCertificateToApplication", "testGetCertificatesByApplicationId", "testGetCertificatesByUUID"})
    public void testDeleteCertificatesByUUID() throws Exception {

        HttpResponse response = restAPIStore.deleteCertificateByUUID(UUID,applicationId);
        assertEquals(response.getResponseCode(),HTTP_RESPONSE_CODE_OK,"ResponseCode mismatched when updating the certificate with UUID " + UUID);
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

    }
}
