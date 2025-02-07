/*
 *Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.client.utils.AuthenticateStubUtil;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceStub;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.AttributeMappingDTO;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.ClaimPropertyDTO;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.dto.LocalClaimDTO;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TenantClaimsTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(TenantClaimsTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private final String STORE_USERNAME = "kimhill@wso2.com";
    private final String STORE_PASSWORD = "kimhill1234";
    private static final String hostname = "localhost";
    private static final String port = "9943";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";
    private static final String USER_EMAIL_ADDRESS = "kim@wso2.com";
    private String storeURLHttp;
    private String newSignUPXML;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private final String SIGNUP_XML_REG_CONFIG_LOCATION = "/_system/governance/apimgt/applicationdata/sign-up-config.xml";
    private final String claims = " Kim|Hill|ABC Network|USA|kim@wso2.com|0016269934122|0016269934134|kimhill|" +
            "www.abcNsounds.org|department|12-12-2000|subscriber";

    @Factory(dataProvider = "userModeDataProvider")
    public TenantClaimsTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    public void initialize() throws APIManagerIntegrationTestException {
        try {
            resourceAdminServiceClient =
                    new ResourceAdminServiceClient(publisherContext.getContextUrls().getBackEndUrl(),
                            createSession(publisherContext));
            String artifactsLocation =
                    TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                            File.separator + "AM" + File.separator + "lifecycletest" + File.separator + "sign-up-config.xml";
            newSignUPXML = readFile(artifactsLocation);
        } catch (AxisFault axisFault) {
            log.error("Error while getting accessing claims");
        } catch (XPathExpressionException e) {
            log.error("Error while getting accessing claims");
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    //When running this test case individually run HostObjectTestCase as well.
    @Test(groups = {"wso2.am"}, description = "Testing Notification Feature")
    public void tenantClaimsTestCase() throws APIManagerIntegrationTestException {
        try {
            replaceSignUpXml();

            apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                    storeContext.getContextTenant().getContextUser().getPassword());

            addClaims();
            checkDisplayOrder();
            //sign up new user
            HttpResponse storeSignUpResponse = apiStore.signUpforTenant(STORE_USERNAME, STORE_PASSWORD, claims);
            log.info("Sign Up User: " + STORE_USERNAME);
            JSONObject signUpJsonObject = null;
            signUpJsonObject = new JSONObject(storeSignUpResponse.getData());
            assertFalse(signUpJsonObject.getBoolean("error"), "Error in user sign up Response");
            //login with new user
            HttpResponse loginResponse = apiStore.login(STORE_USERNAME, STORE_PASSWORD);
            JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
            assertFalse(loginJsonObject.getBoolean("error"), "Error in Login Request: User Name : " + STORE_USERNAME);

        } catch (XPathExpressionException e) {
            log.error("Error while getting store context", e);
        } catch (JSONException e) {
            log.error("Error while signing up", e);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    /**
     * This method replaces the signup.xml
     *
     * @throws APIManagerIntegrationTestException - exception throws when reading the file.
     */
    public void replaceSignUpXml() throws APIManagerIntegrationTestException {
        initialize();
        try {
            resourceAdminServiceClient.updateTextContent(SIGNUP_XML_REG_CONFIG_LOCATION, newSignUPXML);
        } catch (RemoteException e) {
            log.error("Error while updating text context", e);
        } catch (ResourceAdminServiceExceptionException e) {
            log.error("Error while getting service client", e);
        }
    }

    /**
     * This method is used to add claims to the tenant
     */
    public void addClaims() {

        String url = null;
        try {
            url = keyManagerContext.getContextUrls().getBackEndUrl();

            ClaimMetadataManagementServiceStub stub;
            stub = new ClaimMetadataManagementServiceStub(null, url + "ClaimMetadataManagementService");
            AuthenticateStubUtil.authenticateStub("admin@wso2.com", "admin", stub);

            AttributeMappingDTO attributeMappingDTO = new AttributeMappingDTO();
            attributeMappingDTO.setAttributeName("dateOfBirth");
            attributeMappingDTO.setUserStoreDomain("primary");

            LocalClaimDTO localClaimDTO = new LocalClaimDTO();
            ClaimPropertyDTO claimPropertyDTO1 = new ClaimPropertyDTO();
            ClaimPropertyDTO claimPropertyDTO2 = new ClaimPropertyDTO();
            claimPropertyDTO1.setPropertyName("DisplayName");
            claimPropertyDTO1.setPropertyValue("Birth Date");
            claimPropertyDTO2.setPropertyName("SupportedByDefault");
            claimPropertyDTO2.setPropertyValue("true");

            ClaimPropertyDTO[] claimPropertyDTO = {claimPropertyDTO1, claimPropertyDTO2};
            AttributeMappingDTO[] attributeMappingDTO1 = {attributeMappingDTO};
            localClaimDTO.setAttributeMappings(attributeMappingDTO1);
            localClaimDTO.setClaimProperties(claimPropertyDTO);
            localClaimDTO.setLocalClaimURI("http://wso2.org/claims/dob");
            stub.updateLocalClaim(localClaimDTO);

        } catch (XPathExpressionException e) {
            log.error("Error while getting Key Manager context", e);
        } catch (AxisFault axisFault) {
            log.error("Error while getting accessing claims", axisFault);
        } catch (ClaimMetadataManagementServiceClaimMetadataException e) {
            log.error("Error while updating local claim", e);
        } catch (RemoteException e) {
            log.error("Error while connecting to stub", e);
        }
    }

    /**
     * This method checks the display order of userfields
     *
     * @throws APIManagerIntegrationTestException - exception throws when reading the file.
     */

    public boolean checkDisplayOrder() throws APIManagerIntegrationTestException {

        apiStore.login(user.getUserName(), user.getPassword());

        String fileStore;
        int deploymentDelayInMilliseconds = 90 * 1000;
        String finalOutputStore = null;
        String[] responseArrayFromStore = new String[27];
        List<String> userFieldNames = new ArrayList<String>();
        boolean isStoreResponse = false;
        long startTime = System.currentTimeMillis();

        try {
            storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                    APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.TENANT_ADMIN);
            if (storeContext.getContextTenant().getDomain().equals("carbon.super")) {
                fileStore = "testStore.jag";
            } else {
                fileStore = "testStoreTenantClaims.jag";
            }

            String sourcePath = computeSourcePath(fileStore);
            String destinationPath = computeDestPath(fileStore);
            copySampleFile(sourcePath, destinationPath);

            while (((System.currentTimeMillis() - startTime) < deploymentDelayInMilliseconds) && !isStoreResponse) {
                URL jaggeryURL = new URL(storeUrls.getWebAppURLHttp() + "testapp/" + fileStore);
                URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        jaggeryServerConnection.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    finalOutputStore = inputLine;
                }

                assertNotNull(finalOutputStore);

                Gson gson = new Gson();
                if (null != finalOutputStore) {

                    responseArrayFromStore = finalOutputStore.split("==");
                    ArrayList userFields = gson.fromJson(responseArrayFromStore[8], ArrayList.class);

                    for (Object string : userFields) {
                        userFieldNames.add(string.toString().split(",")[0].split("=")[1]);
                    }
                    assertTrue(userFieldNames.get(0).contentEquals("First Name"));
                    assertTrue(userFieldNames.get(1).contentEquals("Last Name"));
                    assertTrue(userFieldNames.get(2).contentEquals("Organization"));
                    assertTrue(userFieldNames.get(3).contentEquals("Country"));
                    assertTrue(userFieldNames.get(4).contentEquals("Email"));
                    assertTrue(userFieldNames.get(5).contentEquals("Telephone"));
                    assertTrue(userFieldNames.get(6).contentEquals("Mobile"));
                    assertTrue(userFieldNames.get(7).contentEquals("IM"));
                    assertTrue(userFieldNames.get(8).contentEquals("URL"));
                    assertTrue(userFieldNames.get(9).contentEquals("Department"));
                    assertTrue(userFieldNames.get(10).contentEquals("Birth Date"));
                    assertTrue(userFieldNames.get(11).contentEquals("Role"));
                }
                in.close();
                assertNotNull(finalOutputStore, "Result cannot be null");
            }
            log.info(finalOutputStore);
        } catch (XPathExpressionException e) {
            log.error("Error while getting Tenant domain", e);
        } catch (IOException e) {
            log.error("Error while invoking test application to test publisher host object", e);
        } finally {
            assertNotNull(finalOutputStore, "Result cannot be null");
        }
        return true;
    }

    /**
     * Read the file content and return the content as String.
     *
     * @param fileLocation - Location of the file.
     * @return String - content of the file.
     * @throws APIManagerIntegrationTestException - exception throws when reading the file.
     */
    protected String readFile(String fileLocation) throws APIManagerIntegrationTestException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException ioE) {
            throw new APIManagerIntegrationTestException("IOException when reading the file from:" + fileLocation, ioE);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.warn("Error when closing the buffer reader which used to reed the file:" + fileLocation +
                            ". Error:" + e.getMessage());
                }
            }
        }
    }

    private void copySampleFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the other into Jaggery server", e);
        }
    }

    private String computeDestPath(String fileName) {
        String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
        String deploymentPath = serverRoot + "/repository/deployment/server/jaggeryapps/testapp";
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : "
                    + deploymentPath);
        }
        return deploymentPath + File.separator + fileName;
    }

    private String computeSourcePath(String fileName) {

        return getAMResourceLocation()
                + File.separator + "jaggery/" + fileName;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}