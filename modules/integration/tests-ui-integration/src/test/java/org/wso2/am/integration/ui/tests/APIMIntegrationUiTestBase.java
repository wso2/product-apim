/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.ui.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.configurations.UrlGenerationUtil;

import javax.xml.xpath.XPathExpressionException;

public class APIMIntegrationUiTestBase extends APIMIntegrationBaseTest {

    private static final Log logger = LogFactory.getLog(APIMIntegrationUiTestBase.class);

    protected String getPublisherURL() throws Exception{
        return publisherUrls.getWebAppURLHttps() + "publisher";
    }
    
    protected String getStoreURL() throws Exception{
        return storeUrls.getWebAppURLHttps() + "store";
    }

    protected String getAdminDashboardURL() throws Exception{
        return publisherUrls.getWebAppURLHttps() + "admin-dashboard";
    }

    protected String getLoginURL() throws XPathExpressionException {
        return UrlGenerationUtil.getLoginURL(publisherContext.getInstance());
    }
}
