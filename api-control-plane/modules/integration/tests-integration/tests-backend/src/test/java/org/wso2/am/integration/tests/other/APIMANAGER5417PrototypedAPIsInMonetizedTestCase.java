/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIDesignBean;
import org.wso2.am.integration.test.utils.bean.APIImplementationBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Disables advanced throttling, enable monetization, create an API as a Prototyped API and browses the store and check
 * if it loads properly
 */
public class APIMANAGER5417PrototypedAPIsInMonetizedTestCase extends APIMIntegrationBaseTest {

    private final String apiName = "APIMFreePrototypedAPI";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private APIIdentifier apiIdentifierPublisher;
    private String apiEndPointUrl;
    private String tenantConfigBeforeTestCase;
    ServerConfigurationManager serverConfigurationManager;
    private static final Log log = LogFactory.getLog(APIMANAGER5417PrototypedAPIsInMonetizedTestCase.class);
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private final String TENANT_CONFIG_LOCATION = "/_system/config/apimgt/applicationdata/tenant-conf.json";
    private String apiProvider;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException,
            XPathExpressionException, IOException, TenantMgtAdminServiceExceptionException,
            AutomationUtilException {
        super.init();
        
        //Disabling advanced throttling
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "apiManagerXmlWithoutAdvancedThrottling"
                + File.separator + "api-manager.xml"));
        serverConfigurationManager.restartGracefully();

        String apiPrototypeEndpointPostfixUrl = "pizzashack-api-1.0.0/api/";
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiPrototypeEndpointPostfixUrl;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiPublisher.login(apiProvider, publisherContext.getContextTenant().getContextUser().getPassword());
        apiIdentifierPublisher = new APIIdentifier(apiProvider, apiName, apiVersion);
    }

    @Test(groups = {"wso2.am"}, description = "Create an API & deployed as a prototype and check " +
            "the visibility in prototype API In store")
    public void testVisibilityOfPrototypedAPIInStoreAfterMonetizationEnabled() throws Exception {

        //Enabling monetization by editing tenant-conf.json in super tenant registry
        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContextMgt));
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator 
                + "configFiles" + File.separator + "monetization" + File.separator + "tenant-conf.json"), "UTF-8");
        tenantConfigBeforeTestCase = resourceAdminServiceClient.getTextContent(TENANT_CONFIG_LOCATION);
        resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfContent);
        
        String apiContext = "apimfreepizzashack";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

        APIDesignBean apiDesignBean = new APIDesignBean(apiName, apiContext, apiVersion,
                apiDescription, apiTags);

        HttpResponse apiDesignResponse = apiPublisher.designAPI(apiDesignBean);
        assertEquals(apiDesignResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiDesignResponse.getData().contains("\"error\" : false"),
                apiName + "is not created as expected");

        APIImplementationBean apiImplementationBean = new APIImplementationBean(apiName,
                apiVersion, apiProvider, new URL(apiEndPointUrl));
        apiImplementationBean.setSwagger(apiDesignBean.getSwagger());

        HttpResponse apiImplementationResponse = apiPublisher.implement(apiImplementationBean);
        assertEquals(apiImplementationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiImplementationResponse.getData().contains("\"error\" : false"),
                apiName + "is not created as expected");

        //Deployed API as a Prototyped API & check the status
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, apiProvider,
                APILifeCycleState.PROTOTYPED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PROTOTYPED"),
                apiName + "  status not updated as Prototyped");

        //Check whether Prototype API is available in publisher
        List<APIIdentifier> implementedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisher, implementedAPIList),
                "Implemented" + apiName + " Api is visible in API Publisher.");

        Thread.sleep(15000);

        //Retrieves the super tenant store and check if it is successful.
        try {
            HttpResponse response = HTTPSClientUtils
                    .doGet(storeUrls.getWebAppURLHttps() + "store/?tenant=carbon.super", null);
            assertEquals(response.getResponseCode(), 200);
        } catch (IOException e) {
            log.error("Failed to get super tenant store", e);
            assertTrue(false, "Failed to get super tenant store due to " + e.getMessage());
        }
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfigBeforeTestCase);
        apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }

}