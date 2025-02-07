/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.am.integration.tests.other;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;
//import sun.misc.BASE64Encoder;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIMANAGER5843WSDLHostnameTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(APIMANAGER5843WSDLHostnameTestCase.class);
    private String apiName = "APIMANAGER5843";
    private String apiContext = "apimanager5843";
    private String apiVersion = "1.0.0";
    private String backendEndWSDL;
    private String backendEndUrl;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER5843WSDLHostnameTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            super.init();
        }
        backendEndWSDL = getGatewayURLNhttp() + "services/echo?wsdl";
        backendEndUrl = getGatewayURLNhttp() + "services/echo";

    }

    @Test(groups = {"wso2.am"}, description = "API creation with wsdl")
    public void testAPICreationWithWSDL() throws Exception {


        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(backendEndUrl));

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setWsdl(backendEndWSDL);
        apiRequest.setProvider(user.getUserName());

        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();


        Map<String, String> map = new HashMap<String, String>();
        map.put("Authorization", "Basic " + new String(Base64.encodeBase64((user.getUserName() + ':' + user.getPassword()).getBytes())));

        String username = user.getUserNameWithoutDomain();
        if (TestUserMode.TENANT_ADMIN == userMode) {
            username = user.getUserNameWithoutDomain() + "-AT-" + user.getUserDomain();
        }
        String wsdl = HTTPSClientUtils.doGet(getGatewayMgtURLHttps() + "registry/resourceContent?"
                + "path=/_system/governance/apimgt/applicationdata/wsdls/" + username
                + "--APIMANAGER58431.0.0.wsdl", map).getData();

        /*when there are multiple gateway environments and none of them are of 'production' type, port address location
        shouldn't have null part like this.
             <wsdl:port name="echoHttpSoap11Endpoint" binding="ns:echoSoap11Binding">
                 <soap:address location="null/[t/b.com]/apimanager5843/1.0.0"/>
             </wsdl:port>
         */
        Assert.assertFalse(wsdl.contains("null/"), "WSDL has 'null' as service hostname.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] {TestUserMode.SUPER_TENANT_ADMIN},
                new Object[] {TestUserMode.TENANT_ADMIN},
        };
    }
}

