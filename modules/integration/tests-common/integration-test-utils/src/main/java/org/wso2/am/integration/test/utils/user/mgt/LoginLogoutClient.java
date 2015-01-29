/*
*Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.test.utils.user.mgt;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.AuthenticatorClient;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * A utility for logging into & logging out of Carbon servers
 */
public class LoginLogoutClient {
    private static final Log log = LogFactory.getLog(LoginLogoutClient.class);
    private String sessionCookie;
    private int port;
    private String hostName;
    private String backendURL;
    private AutomationContext automationContext;
    private AuthenticatorClient loginClient;

    public LoginLogoutClient(AutomationContext context) throws MalformedURLException,
                                                               XPathExpressionException, AxisFault {
        URL backend = new URL(context.getContextUrls().getBackEndUrl());
        backendURL = context.getContextUrls().getBackEndUrl();
        this.port = backend.getPort();
        this.hostName = backend.getHost();
        this.automationContext = context;
        this.loginClient = new AuthenticatorClient(backendURL);
    }

    /**
     * Log in to a Carbon server
     *
     * @return The session cookie on successful login
     */
    public String login()
            throws LoginAuthenticationExceptionException, IOException, XMLStreamException,
                   URISyntaxException, SAXException, XPathExpressionException {
        String userName;
        userName = automationContext.getContextTenant().getContextUser().getUserName();
        return loginClient.login(userName, automationContext.getContextTenant().getContextUser().getPassword()
                , automationContext.getInstance().getHosts().get("default"));
    }

    /**
     * Log out from carbon server
     */
    public void logout() throws LogoutAuthenticationExceptionException, RemoteException {
        loginClient.logOut();
    }
}
