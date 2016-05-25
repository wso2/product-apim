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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.*;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SameVersionAPITestCase extends APIMIntegrationBaseTest{

    private static final Log log= LogFactory.getLog(SameVersionAPITestCase.class);
    private APIPublisherRestClient apiPublisher;
    private static final String API_NAME="SameVersionAPITest";
    private static final String API_CONTEXT="SameVersionAPI";
    private String version="1.0.0";
    private String newVersion="1.0.0";
    private String TAGS="testtag1, testtag2";
    private String providerName;
    private String visibility="public";
    private String description="Test Description";
    private static final String WEB_APP_FILE_NAME="jaxrs_basic";
    private String tier= APIMIntegrationConstants.API_TIER.GOLD;
    private String resTier= APIMIntegrationConstants.RESOURCE_TIER.ULTIMATE;
    private String endPointType="http";

    @Factory(dataProvider = "userModeDataProvider")
    public SameVersionAPITestCase(TestUserMode userMode){
        this.userMode=userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception{
        super.init();

        String sourcePath= TestConfigurationProvider.getResourceLocation()+ File.separator+
                "artifacts"+File.separator+"AM"+File.separator+"lifecycletest"+File.separator+
                "jaxrs_basic.war";

        String targetPath= FrameworkPathUtil.getCarbonHome()+File.separator+"repository"+
                File.separator+"deployment"+File.separator+"server"+File.separator+"webapps";

        FileManager.copyResourceToFileSystem(sourcePath, targetPath, "jaxrs_basic.war");

        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient =
                new WebAppAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        WebAppDeploymentUtil.isWebApplicationDeployed(gatewayContextWrk.getContextUrls().getBackEndUrl(),
                sessionId, WEB_APP_FILE_NAME);

        log.info("Web App Deployed");

        String publisherUrlHttp=publisherUrls.getWebAppURLHttp();
        apiPublisher=new APIPublisherRestClient(publisherUrlHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = "wso2am", description = "Copy Same Version")
    public void copySameVersion() throws Exception{
        String gatewayUrl;
        if(gatewayContextWrk.getContextTenant().getDomain().equals("carbon.super")){
            gatewayUrl=gatewayUrlsWrk.getWebAppURLNhttp();
        }
        else{
            gatewayUrl=gatewayUrlsWrk.getWebAppURLNhttp()+ "t/" + gatewayContextWrk.getContextTenant().getDomain() + "/";

        }

        String endpointUrl=gatewayUrl+"jaxrs_basic/services/customers/customerservice";
        providerName=publisherContext.getContextTenant().getContextUser().getUserName();

        List<APIResourceBean> resourceBeanList=new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET","Application & Application User", resTier, "customers/{id}/"));
        resourceBeanList.add(new APIResourceBean("POST","Application & Application User", resTier, "customers/name/"));

        APICreationRequestBean apiCreationRequestBean=new APICreationRequestBean(API_NAME,API_CONTEXT,
                version,providerName,new URL(endpointUrl));
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setTags(TAGS);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTier(tier);
        apiCreationRequestBean.setVisibility(visibility);
        apiCreationRequestBean.setEndpointType(endPointType);

        //add api
        HttpResponse apiAddRequest=apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiAddRequest.getResponseCode(), Response.Status.OK.getStatusCode(),"Invalid Response Code");
        assertTrue(apiAddRequest.getData().contains("{\"error\" : false}"), "Response Data Mismatched");

        //Copy api with same version
        HttpResponse copyAPIResponse=apiPublisher.copyAPI(providerName,API_NAME,version,newVersion,"");
        assertEquals(copyAPIResponse.getResponseCode(),Response.Status.OK.getStatusCode(),"Response Code Mismatched");
        assertTrue(copyAPIResponse.getData().contains("\"error\" : true"), "Response Data Mismatched. No error thrown.");
        assertTrue(copyAPIResponse.getData().contains("API already exists with version: " + version),
                   "Response Data Mismatched.");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception{
        apiPublisher.deleteAPI(API_NAME,version,providerName);
    }


    @DataProvider
    public static Object[][] userModeDataProvider(){
        return new Object[][]{
               new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
               new Object[]{TestUserMode.TENANT_ADMIN},
        };

    }

}
