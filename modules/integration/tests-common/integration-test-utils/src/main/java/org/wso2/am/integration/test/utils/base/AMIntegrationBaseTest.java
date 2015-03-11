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

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.test.utils.esb.ESBTestCaseUtils;
import org.wso2.am.integration.test.utils.esb.EndpointGenerator;
import org.wso2.am.integration.test.utils.esb.ServiceDeploymentUtil;
import org.wso2.am.integration.test.utils.user.mgt.LoginLogoutClient;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.ContextUrls;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.xml.sax.SAXException;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.regex.Matcher;

/**
 * Base class for all API Manager integration tests
 * Users need to extend this class to write integration tests.
 */
public class AMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(AMIntegrationBaseTest.class);
    protected AutomationContext apimContext, storeContext, publisherContext, gatewayContext;
    protected String sessionCookie;
    protected String backendURL;
    protected String webAppURL;
    protected LoginLogoutClient loginLogoutClient;

    protected OMElement synapseConfiguration = null;

    protected ESBTestCaseUtils esbUtils;

    protected TestUserMode userMode;
    protected ContextUrls contextUrls, storeUrls, publisherUrls, gatewayUrls;

    protected void init() throws Exception {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    protected void init(TestUserMode userMode) throws Exception {
        apimContext = new AutomationContext("APIM", userMode);
        contextUrls = apimContext.getContextUrls();
        sessionCookie = login(apimContext);
        esbUtils = new ESBTestCaseUtils();

        storeContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_STORE_INSTANCE, userMode);

        storeUrls = storeContext.getContextUrls();

        publisherContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);

        publisherUrls = publisherContext.getContextUrls();

        gatewayContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_GATEWAY_INSTANCE, userMode);

        gatewayUrls = gatewayContext.getContextUrls();
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

    protected String getMainSequenceURL() {
        String mainSequenceUrl = contextUrls.getServiceUrl();
        if (mainSequenceUrl.endsWith("/services")) {
            mainSequenceUrl = mainSequenceUrl.replace("/services", "");
        }
        if (!mainSequenceUrl.endsWith("/")) {
            mainSequenceUrl = mainSequenceUrl + "/";
        }
        return mainSequenceUrl;

    }

    protected String getMainSequenceURLHttps() {
        String mainSequenceUrl = contextUrls.getSecureServiceUrl();
        if (mainSequenceUrl.endsWith("/services")) {
            mainSequenceUrl = mainSequenceUrl.replace("/services", "");
        }
        if (!mainSequenceUrl.endsWith("/")) {
            mainSequenceUrl = mainSequenceUrl + "/";
        }
        return mainSequenceUrl;

    }

    protected String getApiInvocationURLHttp(String apiContext) throws XPathExpressionException {
        if (isBuilderEnabled()) {
            return getMainSequenceURL() + apiContext;
        } else {
            return getGatewayServerURLHttp() + apiContext;
        }
    }

    protected String getApiInvocationURLHttps(String apiContext) throws XPathExpressionException {
        if (isBuilderEnabled()) {
            return getMainSequenceURLHttps() + apiContext;
        } else {
            return getGatewayServerURLHttps() + apiContext;
        }
    }

    protected String getProxyServiceURLHttp(String proxyServiceName) {
        return contextUrls.getServiceUrl() + "/" + proxyServiceName;
    }

    protected String getProxyServiceURLHttps(String proxyServiceName) {
        return contextUrls.getSecureServiceUrl() + "/" + proxyServiceName;
    }

    protected String getServerURLHttp() throws XPathExpressionException {
        return getServerBackendUrlHttp();
    }

    protected String getServerURLHttps() {
        String serverUrl = contextUrls.getBackEndUrl();
        if (serverUrl.endsWith("/services")) {
            serverUrl = serverUrl.replace("/services", "");
        }
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, (serverUrl.length() - 1));
        }
        return serverUrl;
    }

    private String getServerBackendUrlHttp() throws XPathExpressionException {
        String httpPort = apimContext.getInstance().getPorts().get("http");
        String hostName = apimContext.getInstance().getHosts().get("default");

        String url = "http://" + hostName;
        if (httpPort != null) {
            url = url + ":" + httpPort;
        }
        return url;
    }

    protected String login(AutomationContext apimContext)
            throws IOException, XPathExpressionException, URISyntaxException, SAXException,
                   XMLStreamException, LoginAuthenticationExceptionException {
        LoginLogoutClient loginLogoutClient = new LoginLogoutClient(apimContext);
        return loginLogoutClient.login();
    }

    protected boolean isRunningOnStratos() throws XPathExpressionException {
        return apimContext.getConfigurationValue("//executionEnvironment").equals("platform");
    }

    protected String getAMResourceLocation() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
               File.separator + "AM";
    }

    protected String getBackEndServiceUrl(String serviceName) throws XPathExpressionException {
        return EndpointGenerator.getBackEndServiceEndpointUrl(serviceName);
    }

    protected boolean isBuilderEnabled() throws XPathExpressionException {
        return apimContext.getConfigurationValue("//executionEnvironment").equals("standalone");
    }

    protected boolean isClusterEnabled() throws XPathExpressionException {
        return apimContext.getProductGroup().isClusterEnabled();
    }

    protected String getExecutionEnvironment() throws XPathExpressionException {
        return apimContext.getConfigurationValue("//executionEnvironment");
    }



    protected void loadAPIMConfigurationFromClasspath(String relativeFilePath) throws Exception {
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher
                .quoteReplacement(File.separator));

        OMElement synapseConfig = esbUtils.loadResource(relativeFilePath);
        updateAPIMConfiguration(synapseConfig);

    }

    protected void updateAPIMConfiguration(OMElement synapseConfig) throws Exception {

        if (synapseConfiguration == null) {
            synapseConfiguration = synapseConfig;
        } else {
            Iterator<OMElement> itr = synapseConfig.cloneOMElement().getChildElements();
            while (itr.hasNext()) {
                synapseConfiguration.addChild(itr.next());
            }
        }
        esbUtils.updateESBConfiguration(setEndpoints(synapseConfig), contextUrls.getBackEndUrl(),
                                        sessionCookie);

        if (apimContext.getProductGroup().isClusterEnabled()) {
            long deploymentDelay =
                    Long.parseLong(apimContext.getConfigurationValue("//deploymentDelay"));
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

    protected OMElement setEndpoints(OMElement synapseConfig)
            throws XMLStreamException, XPathExpressionException {
        if (isBuilderEnabled()) {
            return synapseConfig;
        }
        String config = replaceEndpoints(synapseConfig.toString());
        return AXIOMUtil.stringToOM(config);
    }

    protected DataHandler setEndpoints(DataHandler dataHandler)
            throws XMLStreamException, IOException, XPathExpressionException {
        if (isBuilderEnabled()) {
            return dataHandler;
        }
        String config = readInputStreamAsString(dataHandler.getInputStream());
        config = replaceEndpoints(config);
        ByteArrayDataSource dbs = new ByteArrayDataSource(config.getBytes());
        return new DataHandler(dbs);
    }

    private String replaceEndpoints(String config) throws XPathExpressionException {
        //this should be AS context
        String serviceUrl =
                new AutomationContext("AS", TestUserMode.SUPER_TENANT_ADMIN).getContextUrls()
                        .getServiceUrl();

        config = config.replace("http://localhost:9000/services/"
                , serviceUrl);
        config = config.replace("http://127.0.0.1:9000/services/"
                , serviceUrl);
        return config;
    }

    protected OMElement replaceEndpoints(String relativePathToConfigFile, String serviceName,
                                         String port)
            throws XMLStreamException, FileNotFoundException, XPathExpressionException {
        String config = esbUtils.loadResource(relativePathToConfigFile).toString();
        config = config.replace("http://localhost:" + port + "/services/" + serviceName,
                                getBackEndServiceUrl(serviceName));

        return AXIOMUtil.stringToOM(config);
    }

    private boolean isProxyWSDlExist(String serviceUrl, long synchronizingDelay)
            throws Exception {
        return new ServiceDeploymentUtil().isServiceWSDlExist(serviceUrl, synchronizingDelay);

    }

    private boolean isProxyWSDlNotExist(String serviceUrl, long synchronizingDelay)
            throws Exception {

        return new ServiceDeploymentUtil().isServiceWSDlNotExist(serviceUrl, synchronizingDelay);

    }

    protected String getSessionCookie() {
        return sessionCookie;
    }

    private String readInputStreamAsString(InputStream in)
            throws IOException {

        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1) {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    protected void cleanup() throws Exception {
        try {
            if (synapseConfiguration != null) {
                esbUtils.deleteArtifact(synapseConfiguration, contextUrls.getBackEndUrl(),
                                        sessionCookie);
            }
        } finally {
            synapseConfiguration = null;
        }
    }

    protected String getStoreServerURLHttp() {
        return storeUrls.getWebAppURL();
    }

    protected String getStoreServerURLHttps() {

        String httpsURL = null;

        httpsURL = storeUrls.getBackEndUrl();

        if (httpsURL.endsWith("/services")) {
            httpsURL = httpsURL.replace("/services", "");
        }

        return httpsURL;
    }

    protected String getPublisherServerURLHttp() {
        return publisherUrls.getWebAppURL();
    }

    protected String getPublisherServerURLHttps() {
        String httpsURL = publisherUrls.getBackEndUrl();
        if (httpsURL.endsWith("/services")) {
            httpsURL = httpsURL.replace("/services", "");
        }

        return httpsURL;
    }

    protected String getGatewayServerURLHttp() {
        String httpURL = gatewayUrls.getServiceUrl();
        if (httpURL.endsWith("/services")) {
            httpURL = httpURL.replace("/services", "");
        }

        return httpURL;
    }

    protected String getGatewayServerURLHttps() {
        String httpsURL = gatewayUrls.getServiceUrl();
        if (httpsURL.endsWith("/services")) {
            httpsURL = httpsURL.replace("/services", "");
        }

        return httpsURL;
    }

    protected String getTestApplicationUsagePublisherServerURLHttp() {
        return "http://localhost:9763/testapp/testUsageWithBAM.jag";
    }

    protected String getTestApplicationStoreServerURLHttp() {
        return "http://localhost:9763/testapp/testStore.jag";
    }

    protected String getTestApplicationPublisherServerURLHttp() {
        return "http://localhost:9763/testapp/testPublisher.jag";
    }

}

