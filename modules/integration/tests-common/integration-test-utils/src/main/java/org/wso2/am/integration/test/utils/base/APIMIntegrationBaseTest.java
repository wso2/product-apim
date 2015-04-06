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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.ServiceDeploymentUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
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
     * This method will initialize test environment
     * based on user mode and configuration given at automation.xml
     *
     * @throws APIManagerIntegrationTestException
     */
    protected void init() throws APIManagerIntegrationTestException {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * init the object with user mode , create context objects and get session cookies
     *
     * @param userMode - user mode to run the tests
     * @throws APIManagerIntegrationTestException
     */
    protected void init(TestUserMode userMode) throws APIManagerIntegrationTestException {

        apimTestCaseUtils = new APIMTestCaseUtils();

        try {

            //create store server instance based on configuration given at automation.xml
            storeContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                            APIMIntegrationConstants.AM_STORE_INSTANCE, userMode);
            storeUrls = new APIMURLBean(storeContext.getContextUrls());

            //create publisher server instance based on configuration given at automation.xml
            publisherContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                            APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
            publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

            //create gateway server instance based on configuration given at automation.xml
            gatewayContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                            APIMIntegrationConstants.AM_GATEWAY_INSTANCE, userMode);
            gatewayUrls = new APIMURLBean(gatewayContext.getContextUrls());

        } catch (XPathExpressionException e) {
            log.error("Init failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }

    }

    /**
     * init the object with tenant domain, user key and instance of store,publisher and gateway
     * create context objects and construct URL bean
      * @param domainKey            - tenant domain key
     * @param userKey               - tenant user key
     * @param publisherInstance     - publisher instance name in automation.xml
     * @param storeInstance         - store instance name in automation.xml
     * @param gatewayInstance       - gateway instance name in automation.xml
     * @throws APIManagerIntegrationTestException
     */
    protected void init(String domainKey, String userKey,
                                     String publisherInstance, String storeInstance, String gatewayInstance)
            throws APIManagerIntegrationTestException {

        apimTestCaseUtils = new APIMTestCaseUtils();

        try {

            //create store server instance based configuration given at automation.xml
            storeContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                            storeInstance, domainKey, userKey);
            storeUrls = new APIMURLBean(storeContext.getContextUrls());

            //create publisher server instance
            publisherContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                            publisherInstance, domainKey, userKey);
            publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

            //create gateway server instance
            gatewayContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                            gatewayInstance, domainKey, userKey);
            gatewayUrls = new APIMURLBean(gatewayContext.getContextUrls());

        } catch (XPathExpressionException e) {
            log.error("Init failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }

    }

    /**
     * proxy service URL of deployed server non secure
     *
     * @param proxyServiceName - name of proxy service
     * @return                 - url of proxy service
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
     * @param relativeFilePath - file path to load config
     * @throws                   APIManagerIntegrationTestException
     */
    protected void loadSynapseConfigurationFromClasspath(String relativeFilePath, AutomationContext automationContext,
                                                         String sessionCookie) throws APIManagerIntegrationTestException {
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher
                .quoteReplacement(File.separator));

        OMElement synapseConfig;
        try {
            synapseConfig = apimTestCaseUtils.loadResource(relativeFilePath);
        } catch (FileNotFoundException e) {
            log.error("synapse config loading issue",e);
            throw new APIManagerIntegrationTestException("synapse config loading issue",e);
        } catch (XMLStreamException e) {
            log.error("synapse config loading issue", e);
            throw new APIManagerIntegrationTestException("synapse config loading issue",e);
        }

        try {
            updateSynapseConfiguration(synapseConfig, automationContext, sessionCookie);
        } catch (Exception e) {
            log.error("synapse config loading issue", e);
            throw new APIManagerIntegrationTestException("synapse config loading issue",e);
        }

    }

    /**
     *
     * @param automationContext - automation context instance of given server
     * @return                  - created session cookie variable
     * @throws                   APIManagerIntegrationTestException
     */
    protected String createSession(AutomationContext automationContext)
            throws APIManagerIntegrationTestException {
        LoginLogoutClient loginLogoutClient;
        try {
            loginLogoutClient = new LoginLogoutClient(automationContext);
            return loginLogoutClient.login();
        } catch (Exception e) {
            log.error("session creation error", e);
            throw new APIManagerIntegrationTestException("session creation error",e);
        }
    }

    /**
     * Get resources location for  test case
     *
     * @return - resource location file path
     */
    protected String getAMResourceLocation() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                File.separator + "AM";
    }

    /**
     * update synapse config to  server
     *
     * @param synapseConfig     - config to upload
     * @param automationContext - automation context of the server instance
     * @param sessionCookie     -  logged in session cookie
     * @throws APIManagerIntegrationTestException - APIManagerIntegrationTestException
     */
    protected void updateSynapseConfiguration(OMElement synapseConfig, AutomationContext automationContext, String sessionCookie)
            throws APIManagerIntegrationTestException {

        if (synapseConfiguration == null) {
            synapseConfiguration = synapseConfig;
        } else {
            Iterator<OMElement> itr = synapseConfig.cloneOMElement().getChildElements();  //ToDo
            while (itr.hasNext()) {
                synapseConfiguration.addChild(itr.next());
            }
        }

        try {

            apimTestCaseUtils.updateAPIMConfiguration(synapseConfig, automationContext.getContextUrls().getBackEndUrl(),
                    sessionCookie);

            if (automationContext.getProductGroup().isClusterEnabled()) {
                long deploymentDelay =
                        Long.parseLong(automationContext.getConfigurationValue("//deploymentDelay"));
                Thread.sleep(deploymentDelay);
                Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName("proxy"); //ToDo
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

        } catch (Exception e) {
            log.error("synapse config  upload error", e);
            throw new APIManagerIntegrationTestException("synapse config  upload error",e);
        }
    }

    /**
     * check whether the proxy service wsdl exist
     *
     * @param serviceUrl         - proxy service URL
     * @param synchronizingDelay - delay to update
     * @return                   - whether wsdl exist or not
     * @throws APIManagerIntegrationTestException - APIManagerIntegrationTestException
     */
    private boolean isProxyWSDlExist(String serviceUrl, long synchronizingDelay)
            throws APIManagerIntegrationTestException {
        try {
            return  ServiceDeploymentUtil.isServiceWSDlExist(serviceUrl, synchronizingDelay);
        } catch (Exception e) {
            log.error("wsdl lookup error", e);
            throw new APIManagerIntegrationTestException("wsdl lookup error",e);
        }

    }

    /**
     * clean up deployed artifacts and other services
     *
     * @throws APIManagerIntegrationTestException
     */
    protected void cleanup() throws APIManagerIntegrationTestException {

    }


}

