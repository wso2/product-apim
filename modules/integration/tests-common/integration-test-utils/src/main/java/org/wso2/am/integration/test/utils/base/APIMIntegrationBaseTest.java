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
package org.wso2.am.integration.test.utils.base;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.ServiceDeploymentUtil;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.regex.Matcher;

/**
 * Base class for all API Manager integration tests
 * Users need to extend this class to write integration tests.
 */
public class APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMIntegrationBaseTest.class);

    protected AutomationContext storeContext, publisherContext, gatewayContext;

    protected OMElement synapseConfiguration;

    protected APIMTestCaseUtils apimTestCaseUtils;

    protected TestUserMode userMode;
    protected APIMURLBean storeUrls, publisherUrls, gatewayUrls;

    /**
     * init basic class
     *
     * @throws Exception
     */
    protected void init() throws Exception {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * init the object with user mode , create context objects and get session cookies
     *
     * @param userMode
     * @throws Exception
     */

    protected void init(TestUserMode userMode) throws Exception {

        apimTestCaseUtils = new APIMTestCaseUtils();

        storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, userMode);
        storeUrls = new APIMURLBean(storeContext.getContextUrls());

        publisherContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
        publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

        gatewayContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_GATEWAY_INSTANCE, userMode);
        gatewayUrls = new APIMURLBean(gatewayContext.getContextUrls());

    }

    protected void init(String domainKey, String userKey, String instance) throws Exception {
        /*apimContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_1ST_INSTANCE,
                domainKey, userKey);
        loginLogoutClient = new LoginLogoutClient(apimContext);
        backendURL = apimContext.getContextUrls().getBackEndUrl();
        webAppURL = apimContext.getContextUrls().getWebAppURL();*/
    }

    /**
     * proxy service URL of deployed server non secure
     *
     * @param proxyServiceName
     * @return
     */
    protected String getProxyServiceURLHttp(String proxyServiceName) {
        try {
            return gatewayContext.getContextUrls().getServiceUrl() + "/" + proxyServiceName + "/";
        } catch (XPathExpressionException e) {
            log.error("URL retrieve error", e);
        }

        return null;
    }


    /**
     * @param relativeFilePath
     * @throws Exception
     */
    protected void loadSynapseConfigurationFromClasspath(String relativeFilePath, AutomationContext automationContext,
                                                         String sessionCookie) throws Exception {
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher
                .quoteReplacement(File.separator));

        OMElement synapseConfig = apimTestCaseUtils.loadResource(relativeFilePath);
        updateSynapseConfiguration(synapseConfig, automationContext, sessionCookie);

    }

    /**
     * login and return session cookie
     *
     * @param automationContext - created automation context
     * @return - session cookie
     * @throws IOException
     * @throws XPathExpressionException
     * @throws URISyntaxException
     * @throws SAXException
     * @throws XMLStreamException
     * @throws LoginAuthenticationExceptionException
     *
     */

    protected String createSession(AutomationContext automationContext)
            throws IOException, XPathExpressionException, URISyntaxException, SAXException,
            XMLStreamException, LoginAuthenticationExceptionException {
        LoginLogoutClient loginLogoutClient = new LoginLogoutClient(automationContext);
        return loginLogoutClient.login();
    }


    /**
     * Get resources location for  test case
     * @return
     */
    protected String getAMResourceLocation() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                File.separator + "AM";
    }


    /**
     * update API manager synapse configs
     *
     * @param synapseConfig
     * @throws Exception
     */
    protected void updateSynapseConfiguration(OMElement synapseConfig, AutomationContext automationContext, String sessionCookie)
            throws Exception {

        if (synapseConfiguration == null) {
            synapseConfiguration = synapseConfig;
        } else {
            Iterator<OMElement> itr = synapseConfig.cloneOMElement().getChildElements();
            while (itr.hasNext()) {
                synapseConfiguration.addChild(itr.next());
            }
        }
        apimTestCaseUtils.updateAPIMConfiguration(synapseConfig, automationContext.getContextUrls().getBackEndUrl(),
                sessionCookie);

        if (automationContext.getProductGroup().isClusterEnabled()) {
            long deploymentDelay =
                    Long.parseLong(automationContext.getConfigurationValue("//deploymentDelay"));
            Thread.sleep(deploymentDelay);
            Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName("proxy");
            while (proxies.hasNext()) {
                String proxy = proxies.next().getAttributeValue(new QName("name"));

                Assert.assertTrue(isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)
                        , "Deployment Synchronizing failed in workers");
                Assert.assertTrue(isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)
                        , "Deployment Synchronizing failed in workers");
                Assert.assertTrue(isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)
                        , "Deployment Synchronizing failed in workers");
            }
        }
    }

    /**
     * isProxyWSDlExist
     *
     * @param serviceUrl
     * @param synchronizingDelay
     * @return
     * @throws Exception
     */
    private boolean isProxyWSDlExist(String serviceUrl, long synchronizingDelay)
            throws Exception {
        return new ServiceDeploymentUtil().isServiceWSDlExist(serviceUrl, synchronizingDelay);

    }




    /**
     * clean up deployed artifacts and other services
     *
     * @throws Exception
     */

    protected void cleanup() throws Exception {

    }


}

