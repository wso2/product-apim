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

package org.wso2.am.integration.tests.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;

/**
 * Deploy jaxrs_basic webApp and monitoring webApp required to run tests
 * jaxrs_basic - Provides rest backend to run tests
 * <p/>
 * APIStatusMonitor - Can be used to retrieve API deployment status in worker and manager nodes
 */
public class APIManagerConfigurationChangeTest extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIManagerConfigurationChangeTest.class);

    @Test(alwaysRun = true)
    public void configureEnvironment() throws Exception {

        gatewayContextMgt =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        gatewayUrlsMgt = new APIMURLBean(gatewayContextMgt.getContextUrls());
        String dcrURL = gatewayUrlsMgt.getWebAppURLHttps() + "client-registration/v0.17/register";

        //DCR call for publisher app
        DCRParamRequest publisherParamRequest =
                new DCRParamRequest(RestAPIPublisherImpl.appName, RestAPIPublisherImpl.callBackURL,
                        RestAPIPublisherImpl.tokenScope, RestAPIPublisherImpl.appOwner, RestAPIPublisherImpl.grantType,
                        dcrURL,
                        RestAPIPublisherImpl.username, RestAPIPublisherImpl.password,
                        APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(publisherParamRequest);
        //DCR call for dev portal app
        DCRParamRequest devPortalParamRequest =
                new DCRParamRequest(RestAPIStoreImpl.appName, RestAPIStoreImpl.callBackURL,
                        RestAPIStoreImpl.tokenScope, RestAPIStoreImpl.appOwner, RestAPIStoreImpl.grantType, dcrURL,
                        RestAPIStoreImpl.username, RestAPIStoreImpl.password,
                        APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(devPortalParamRequest);
        DCRParamRequest adminPortalParamRequest = new DCRParamRequest(RestAPIAdminImpl.appName,
                RestAPIAdminImpl.callBackURL,
                RestAPIAdminImpl.tokenScope, RestAPIAdminImpl.appOwner, RestAPIAdminImpl.grantType, dcrURL,
                RestAPIAdminImpl.username, RestAPIAdminImpl.password,
                APIMIntegrationConstants.SUPER_TENANT_DOMAIN);
        ClientAuthenticator.makeDCRRequest(adminPortalParamRequest);

        super.init();
    }
}
