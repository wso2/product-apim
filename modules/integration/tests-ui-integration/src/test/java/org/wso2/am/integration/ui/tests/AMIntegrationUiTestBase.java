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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.base.AMIntegrationConstants;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.ContextUrls;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class AMIntegrationUiTestBase {

    private static final Log log = LogFactory.getLog(AMIntegrationUiTestBase.class);
    protected AutomationContext apimContext;
    protected String sessionCookie;
    protected String backendURL;
    protected String webAppURL;
    protected LoginLogoutClient loginLogoutClient;
    protected User userInfo;

    protected OMElement synapseConfiguration = null;
    private List<String> proxyServicesList = null;
    private List<String> sequencesList = null;
    private List<String> endpointsList = null;
    private List<String> localEntryList = null;
    private List<String> messageProcessorsList = null;
    private List<String> messageStoresList = null;
    private List<String> sequenceTemplateList = null;
    private List<String> apiList = null;
    private List<String> priorityExecutorList = null;
    private List<String[]> scheduledTaskList = null;


    protected TestUserMode userMode;
    protected ContextUrls contextUrls;

    protected void init() throws Exception {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    protected void init(TestUserMode userMode) throws Exception {
        apimContext = new AutomationContext("APIM", userMode);
        contextUrls = apimContext.getContextUrls();
        sessionCookie = login(apimContext);
    }

    protected void init(String domainKey, String userKey) throws Exception {
        apimContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                            AMIntegrationConstants.AM_1ST_INSTANCE,
                                            domainKey, userKey);
        loginLogoutClient = new LoginLogoutClient(apimContext);
        sessionCookie = loginLogoutClient.login();
        backendURL = apimContext.getContextUrls().getBackEndUrl();
        webAppURL = apimContext.getContextUrls().getWebAppURL();
    }

    protected String login(AutomationContext apimContext)
            throws IOException, XPathExpressionException, URISyntaxException, SAXException,
                   XMLStreamException, LoginAuthenticationExceptionException {
        org.wso2.am.integration.test.utils.user.mgt.LoginLogoutClient loginLogoutClient = new org.wso2.am.integration.test.utils.user.mgt.LoginLogoutClient(apimContext);
        return loginLogoutClient.login();
    }


    protected String getLoginURL() throws XPathExpressionException {
     return apimContext.getContextUrls().getBackEndUrl();
    }
    
    protected String getPublisherURL() throws Exception{
        String carbonLoginURL = getLoginURL();
        if(carbonLoginURL.contains("/carbon")) {
            return carbonLoginURL.split("carbon")[0] + "publisher";
        } else {
        	throw new Exception("Error while composing Publisher Login URL");
        }
    }
    
    protected String getStoreURL() throws Exception{
        String carbonLoginURL = getLoginURL();
        if(carbonLoginURL.contains("/carbon")) {
            return carbonLoginURL.split("carbon")[0] + "store";
        } else {
        	throw new Exception("Error while composing Store Login URL");
        }
    }
}
