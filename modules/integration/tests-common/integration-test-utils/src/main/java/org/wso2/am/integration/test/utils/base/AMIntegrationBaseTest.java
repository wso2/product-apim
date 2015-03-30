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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.ServiceDeploymentUtil;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.ContextUrls;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminException;
import org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException;
import org.wso2.carbon.rest.api.stub.RestApiAdminAPIException;
import org.wso2.carbon.sequences.stub.types.SequenceEditorException;
import org.wso2.carbon.task.stub.TaskManagementException;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Base class for all API Manager integration tests
 * Users need to extend this class to write integration tests.
 */
public class AMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(AMIntegrationBaseTest.class);

    protected AutomationContext apimContext, storeContext, publisherContext, gatewayContext;
    protected String sessionCookie;
    protected String storeSessionCookie;
    protected String publisherSessionCookie;
    protected String gatewaySessionCookie;
    protected String backendURL;
    protected String webAppURL;
    protected LoginLogoutClient loginLogoutClient;

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

    protected APIMTestCaseUtils apimTestCaseUtils;

    protected TestUserMode userMode;
    protected ContextUrls contextUrls, storeUrls, publisherUrls, gatewayUrls;

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

        apimContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME, userMode);
        contextUrls = apimContext.getContextUrls();
        sessionCookie = login(apimContext);
        apimTestCaseUtils = new APIMTestCaseUtils();

        storeContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_STORE_INSTANCE, userMode);
        storeUrls = storeContext.getContextUrls();
        storeSessionCookie = login(storeContext);

        publisherContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
        publisherUrls = publisherContext.getContextUrls();
        publisherSessionCookie = login(publisherContext);

        gatewayContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_GATEWAY_INSTANCE, userMode);
        gatewayUrls = gatewayContext.getContextUrls();
        gatewaySessionCookie = login(gatewayContext);

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

    /**
     * - eg: http://localhost:8280/
     *
     * @return main sequence url non secure
     */

    protected String getMainSequenceURL() {
        String mainSequenceUrl = contextUrls.getServiceUrl();
        if (mainSequenceUrl.endsWith("/services")) {
            mainSequenceUrl = mainSequenceUrl.replace("/services", "/");
        }
        if (!mainSequenceUrl.endsWith("/")) {
            mainSequenceUrl = mainSequenceUrl + "/";
        }
        return mainSequenceUrl;

    }

    /**
     * - eg: https://localhost:8243/
     *
     * @return main sequence url secure
     */

    protected String getMainSequenceURLHttps() {
        String mainSequenceUrl = contextUrls.getSecureServiceUrl();
        if (mainSequenceUrl.endsWith("/services")) {
            mainSequenceUrl = mainSequenceUrl.replace("/services", "/");
        }
        if (!mainSequenceUrl.endsWith("/")) {
            mainSequenceUrl = mainSequenceUrl + "/";
        }
        return mainSequenceUrl;

    }

    /**
     * eg :  http://localhost:9763/services/
     *
     * @return gateway server url non secure
     */

    protected String getGatewayServerURLHttp() {
        return gatewayUrls.getWebAppURL() + "/services/";
    }

    /**
     * eg : https://localhost:9443/services/
     *
     * @return gateway server url secure
     */

    protected String getGatewayServerURLHttps() {

        return gatewayUrls.getBackEndUrl();
    }

    /**
     * ex :  https://localhost:9763
     *
     * @return publisher server url non secure
     */

    protected String getPublisherServerURLHttp() {
        return publisherUrls.getWebAppURL() + "/publisher/";
    }

    /**
     * ex :  https://localhost:9443
     *
     * @return publisher server url secure
     */

    protected String getPublisherServerURLHttps() {
        String httpsURL = publisherUrls.getBackEndUrl();
        if (httpsURL.endsWith("services/")) {
            httpsURL = httpsURL.replace("services/", "publisher/");
        }

        return httpsURL;
    }

    /**
     * ex :  https://localhost:9763
     *
     * @return store server url non secure
     */

    protected String getStoreServerURLHttp() {
        return storeUrls.getWebAppURL() + "/store/";
    }

    /**
     * ex :  https://localhost:9443
     *
     * @return store server url secure
     */

    protected String getStoreServerURLHttps() {

        String httpsURL = null;

        httpsURL = storeUrls.getBackEndUrl();

        if (httpsURL.endsWith("services/")) {
            httpsURL = httpsURL.replace("services/", "store/");
        }

        return httpsURL;
    }

    /**
     * - eg: http://localhost:8280/apiContext/
     *
     * @param apiContext
     * @return api invocation URL non secure
     * @throws XPathExpressionException
     */

    protected String getApiInvocationURLHttp(String apiContext) throws XPathExpressionException {

        if (getMainSequenceURL().endsWith("/")) {
            return getMainSequenceURL() + apiContext.replaceFirst("/", "") + "/";
        } else {
            return getMainSequenceURL() + apiContext + "/";
        }

    }

    /**
     * - eg: https://localhost:8243/apiContext/
     *
     * @param apiContext
     * @return api invocation URL secure
     * @throws XPathExpressionException
     */

    protected String getApiInvocationURLHttps(String apiContext) throws XPathExpressionException {
        if (getMainSequenceURL().endsWith("/")) {
            return getMainSequenceURLHttps() + apiContext.replaceFirst("/", "") + "/";
        } else {
            return getMainSequenceURLHttps() + apiContext + "/";
        }
    }

    /**
     * proxy service URL of deployed server non secure
     *
     * @param proxyServiceName
     * @return
     */

    protected String getProxyServiceURLHttp(String proxyServiceName) {
        return contextUrls.getServiceUrl() + "/" + proxyServiceName + "/";
    }

    /**
     * proxy service URL of deployed server secure
     *
     * @param proxyServiceName
     * @return
     */

    protected String getProxyServiceURLHttps(String proxyServiceName) {
        return contextUrls.getSecureServiceUrl() + "/" + proxyServiceName + "/";
    }

    protected String getBackEndServiceUrl(String serviceName) throws XPathExpressionException {
        return contextUrls.getBackEndUrl();
    }

    /**
     * get server back end url secure
     *
     * @return
     */

    protected String getBackEndURLHttps() {
        /*  if (serverUrl.endsWith("/services")) {
            serverUrl = serverUrl.replace("/services", "");
        }
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, (serverUrl.length() - 1));
        }*/
        return contextUrls.getBackEndUrl();
    }


    /**
     * get server back end url non secure
     *
     * @return
     */

    protected String getServerBackendUrlHttp() throws XPathExpressionException {
        return contextUrls.getWebAppURL() + "/services/";
    }

    /**
     * @param relativeFilePath
     * @throws Exception
     */


    /**
     * get APIM server sessionCookie
     *
     * @return
     */


    protected void loadAPIMConfigurationFromClasspath(String relativeFilePath) throws Exception {
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher
                .quoteReplacement(File.separator));

        OMElement synapseConfig = apimTestCaseUtils.loadResource(relativeFilePath);
        updateAPIMConfiguration(synapseConfig);

    }

    /**
     * login and return session cookie
     *
     * @param apimContext
     * @return
     * @throws IOException
     * @throws XPathExpressionException
     * @throws URISyntaxException
     * @throws SAXException
     * @throws XMLStreamException
     * @throws LoginAuthenticationExceptionException
     *
     */

    protected String login(AutomationContext apimContext)
            throws IOException, XPathExpressionException, URISyntaxException, SAXException,
            XMLStreamException, LoginAuthenticationExceptionException {
        LoginLogoutClient loginLogoutClient = new LoginLogoutClient(apimContext);
        return loginLogoutClient.login();
    }


    protected String getAMResourceLocation() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                File.separator + "AM";
    }

    /**
     * this is used for for call axis2 server externally
     *
     * @param serviceName
     * @return
     * @throws XPathExpressionException
     */


    /**
     * update API manager synapse configs
     *
     * @param synapseConfig
     * @throws Exception
     */

    protected void updateAPIMConfiguration(OMElement synapseConfig) throws Exception {

        if (synapseConfiguration == null) {
            synapseConfiguration = synapseConfig;
        } else {
            Iterator<OMElement> itr = synapseConfig.cloneOMElement().getChildElements();
            while (itr.hasNext()) {
                synapseConfiguration.addChild(itr.next());
            }
        }
        apimTestCaseUtils.updateAPIMConfiguration(synapseConfig, contextUrls.getBackEndUrl(),
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

    /**
     * add proxy service config
     *
     * @param proxyConfig
     * @throws Exception
     */
    protected void addProxyService(OMElement proxyConfig) throws Exception {
        String proxyName = proxyConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isProxyServiceExist(contextUrls.getBackEndUrl(), sessionCookie, proxyName)) {
            apimTestCaseUtils.deleteProxyService(contextUrls.getBackEndUrl(), sessionCookie, proxyName);
        }
        if (proxyServicesList == null) {
            proxyServicesList = new ArrayList<String>();
        }
        proxyServicesList.add(proxyName);
        apimTestCaseUtils.addProxyService(contextUrls.getBackEndUrl(), sessionCookie,
                proxyConfig);
    }

    /**
     * check proxy deployed
     *
     * @param proxyServiceName
     * @throws Exception
     */

    protected void isProxyDeployed(String proxyServiceName) throws Exception {
        Assert.assertTrue(apimTestCaseUtils.isProxyDeployed(contextUrls.getBackEndUrl(), sessionCookie,
                proxyServiceName),
                "Proxy Deployment failed or time out");
    }

    /**
     * delete proxy services
     *
     * @param proxyServiceName
     * @throws Exception
     */

    protected void deleteProxyService(String proxyServiceName) throws Exception {
        if (apimTestCaseUtils.isProxyServiceExist(contextUrls.getBackEndUrl(), sessionCookie,
                proxyServiceName)) {
            apimTestCaseUtils.deleteProxyService(contextUrls.getBackEndUrl(), sessionCookie,
                    proxyServiceName);

            if (!apimTestCaseUtils.isProxyUnDeployed(contextUrls.getBackEndUrl(), sessionCookie, proxyServiceName)) {
                log.error("Proxy Deletion failed or time out");
            }
        }

        if (proxyServicesList != null && proxyServicesList.contains(proxyServiceName)) {
            proxyServicesList.remove(proxyServiceName);
        }
    }

    /**
     * deleteSequence
     *
     * @param sequenceName
     * @throws SequenceEditorException
     * @throws RemoteException
     */

    protected void deleteSequence(String sequenceName)
            throws SequenceEditorException, RemoteException {
        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie, sequenceName)) {
            apimTestCaseUtils.deleteSequence(contextUrls.getBackEndUrl(), sessionCookie, sequenceName);
        }
        if (sequencesList != null && sequencesList.contains(sequenceName)) {
            sequencesList.remove(sequenceName);
        }
    }

    /**
     * addSequence
     *
     * @param sequenceConfig
     * @throws Exception
     */

    protected void addSequence(OMElement sequenceConfig) throws Exception {
        String sequenceName = sequenceConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie, sequenceName)) {
            apimTestCaseUtils.deleteSequence(contextUrls.getBackEndUrl(), sessionCookie, sequenceName);
        }
        apimTestCaseUtils.addSequence(contextUrls.getBackEndUrl(), sessionCookie,
                sequenceConfig);
        if (sequencesList == null) {
            sequencesList = new ArrayList<String>();
        }
        sequencesList.add(sequenceName);
    }

    /**
     * addEndpoint
     *
     * @param endpointConfig
     * @throws Exception
     */

    protected void addEndpoint(OMElement endpointConfig)
            throws Exception {
        String endpointName = endpointConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie, endpointName)) {
            apimTestCaseUtils.deleteEndpoint(contextUrls.getBackEndUrl(), sessionCookie, endpointName);
        }
        apimTestCaseUtils.addEndpoint(contextUrls.getBackEndUrl(), sessionCookie,
                endpointConfig);
        if (endpointsList == null) {
            endpointsList = new ArrayList<String>();
        }
        endpointsList.add(endpointName);

    }

    /**
     * addLocalEntry
     *
     * @param localEntryConfig
     * @throws Exception
     */

    protected void addLocalEntry(OMElement localEntryConfig) throws Exception {
        String localEntryName = localEntryConfig.getAttributeValue(new QName("key"));
        if (apimTestCaseUtils
                .isLocalEntryExist(contextUrls.getBackEndUrl(), sessionCookie, localEntryName)) {
            apimTestCaseUtils.deleteLocalEntry(contextUrls.getBackEndUrl(), sessionCookie, localEntryName);
        }
        apimTestCaseUtils.addLocalEntry(contextUrls.getBackEndUrl(), sessionCookie, localEntryConfig);

        if (localEntryList == null) {
            localEntryList = new ArrayList<String>();
        }
        localEntryList.add(localEntryName);
    }

    /**
     * addMessageProcessor
     *
     * @param messageProcessorConfig
     * @throws Exception
     */

    protected void addMessageProcessor(OMElement messageProcessorConfig) throws Exception {
        String messageProcessorName = messageProcessorConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isMessageProcessorExist(contextUrls.getBackEndUrl(), sessionCookie,
                messageProcessorName)) {
            apimTestCaseUtils.deleteMessageProcessor(contextUrls.getBackEndUrl(), sessionCookie,
                    messageProcessorName);
        }
        apimTestCaseUtils.addMessageProcessor(contextUrls.getBackEndUrl(), sessionCookie,
                messageProcessorConfig);
        if (messageProcessorsList == null) {
            messageProcessorsList = new ArrayList<String>();
        }
        messageProcessorsList.add(messageProcessorName);
    }

    /**
     * addMessageStore
     *
     * @param messageStoreConfig
     * @throws Exception
     */

    protected void addMessageStore(OMElement messageStoreConfig) throws Exception {
        String messageStoreName = messageStoreConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isMessageStoreExist(contextUrls.getBackEndUrl(), sessionCookie,
                messageStoreName)) {
            apimTestCaseUtils.deleteMessageStore(contextUrls.getBackEndUrl(), sessionCookie,
                    messageStoreName);
        }
        apimTestCaseUtils.addMessageStore(contextUrls.getBackEndUrl(), sessionCookie,
                messageStoreConfig);
        if (messageStoresList == null) {
            messageStoresList = new ArrayList<String>();
        }
        messageStoresList.add(messageStoreName);
    }

    /**
     * addSequenceTemplate
     *
     * @param sequenceTemplate
     * @throws Exception
     */

    protected void addSequenceTemplate(OMElement sequenceTemplate) throws Exception {
        String name = sequenceTemplate.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isSequenceTemplateExist(contextUrls.getBackEndUrl(), sessionCookie, name)) {
            apimTestCaseUtils.deleteSequenceTemplate(contextUrls.getBackEndUrl(), sessionCookie, name);
        }
        apimTestCaseUtils.addSequenceTemplate(contextUrls.getBackEndUrl(), sessionCookie,
                sequenceTemplate);

        if (sequenceTemplateList == null) {
            sequenceTemplateList = new ArrayList<String>();
        }
        sequenceTemplateList.add(name);
    }

    /**
     * addAPI
     *
     * @param api
     * @throws Exception
     */

    protected void addAPI(OMElement api) throws Exception {
        String apiName = api.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isApiExist(contextUrls.getBackEndUrl(), sessionCookie, apiName)) {
            apimTestCaseUtils.deleteApi(contextUrls.getBackEndUrl(), sessionCookie, apiName);
        }
        apimTestCaseUtils.addAPI(contextUrls.getBackEndUrl(), sessionCookie, api);

        if (apiList == null) {
            apiList = new ArrayList<String>();
        }
        apiList.add(apiName);
    }

    /**
     * addPriorityExecutor
     *
     * @param priorityExecutor
     * @throws Exception
     */

    protected void addPriorityExecutor(OMElement priorityExecutor) throws Exception {
        String executorName = priorityExecutor.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isPriorityExecutorExist(contextUrls.getBackEndUrl(), sessionCookie,
                executorName)) {
            apimTestCaseUtils.deletePriorityExecutor(contextUrls.getBackEndUrl(), sessionCookie,
                    executorName);
        }
        apimTestCaseUtils.addPriorityExecutor(contextUrls.getBackEndUrl(), sessionCookie, priorityExecutor);

        if (priorityExecutorList == null) {
            priorityExecutorList = new ArrayList<String>();
        }
        priorityExecutorList.add(executorName);
    }

    /**
     * addScheduledTask
     *
     * @param task
     * @throws Exception
     */

    protected void addScheduledTask(OMElement task) throws Exception {
        String taskName = task.getAttributeValue(new QName("name"));
        String taskGroup = task.getAttributeValue(new QName("group"));
        if (apimTestCaseUtils.isScheduleTaskExist(contextUrls.getBackEndUrl(), sessionCookie, taskName)) {
            apimTestCaseUtils.deleteScheduleTask(contextUrls.getBackEndUrl(), sessionCookie, taskName,
                    taskGroup);
        }
        apimTestCaseUtils.addScheduleTask(contextUrls.getBackEndUrl(), sessionCookie, task);

        if (scheduledTaskList == null) {
            scheduledTaskList = new ArrayList<String[]>();
        }
        scheduledTaskList.add(new String[]{taskName, taskGroup});
    }

    /**
     * deleteMessageProcessors
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteMessageProcessors() throws APIManagerIntegrationTestException {
        if (messageProcessorsList != null) {
            Iterator<String> itr = messageProcessorsList.iterator();
            while (itr.hasNext()) {
                String messageProcessor = itr.next();
                try {
                    if (apimTestCaseUtils.isMessageProcessorExist(contextUrls.getBackEndUrl(), sessionCookie,
                            messageProcessor)) {
                        apimTestCaseUtils.deleteMessageProcessor(contextUrls.getBackEndUrl(), sessionCookie,
                                messageProcessor);
                    }
                } catch (RemoteException e) {
                    log.error("deleteMessageProcessors error ", e);
                    throw new APIManagerIntegrationTestException("deleteMessageProcessors error ", e);
                } catch (SequenceEditorException e) {
                    log.error("deleteMessageProcessors error ", e);
                    throw new APIManagerIntegrationTestException("deleteMessageProcessors error ", e);
                }
            }
            messageProcessorsList.clear();
        }
    }

    /**
     * deleteMessageStores
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteMessageStores() throws APIManagerIntegrationTestException {
        if (messageStoresList != null) {
            Iterator<String> itr = messageStoresList.iterator();
            while (itr.hasNext()) {
                String messageStore = itr.next();
                try {
                    if (apimTestCaseUtils.isMessageStoreExist(contextUrls.getBackEndUrl(), sessionCookie,
                            messageStore)) {
                        apimTestCaseUtils.deleteMessageStore(contextUrls.getBackEndUrl(), sessionCookie,
                                messageStore);
                    }
                } catch (RemoteException e) {
                    log.error("while undeploying Message store. : ", e);
                    throw new APIManagerIntegrationTestException("deleteMessageStores error ", e);
                } catch (SequenceEditorException e) {
                    log.error("while undeploying Message store. : ", e);
                    throw new APIManagerIntegrationTestException("deleteMessageStores error ", e);
                }
            }
            messageStoresList.clear();
        }
    }

    /**
     * deleteSequences
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteSequences() throws APIManagerIntegrationTestException {
        if (sequencesList != null) {
            Iterator<String> itr = sequencesList.iterator();
            while (itr.hasNext()) {
                String sequence = itr.next();
                if (!sequence.equalsIgnoreCase("fault")) {
                    try {
                        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie,
                                sequence)) {
                            apimTestCaseUtils.deleteSequence(contextUrls.getBackEndUrl(), sessionCookie,
                                    sequence);
                        }
                    } catch (RemoteException e) {
                        log.error("while deleteSequences : ", e);
                        throw new APIManagerIntegrationTestException("deleteSequences error ", e);
                    } catch (SequenceEditorException e) {
                        log.error("deleteSequences : ", e);
                        throw new APIManagerIntegrationTestException("deleteSequences error ", e);
                    }
                }
            }
            sequencesList.clear();
        }
    }

    /**
     * deleteProxyServices
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteProxyServices() throws APIManagerIntegrationTestException {
        if (proxyServicesList != null) {
            Iterator<String> itr = proxyServicesList.iterator();
            while (itr.hasNext()) {
                String proxyName = itr.next();
                try {
                    if (apimTestCaseUtils.isProxyServiceExist(contextUrls.getBackEndUrl(), sessionCookie,
                            proxyName)) {
                        apimTestCaseUtils.deleteProxyService(contextUrls.getBackEndUrl(), sessionCookie,
                                proxyName);
                    }
                } catch (RemoteException e) {
                    log.error("while deleteProxyServices : ", e);
                    throw new APIManagerIntegrationTestException("deleteProxyServices error ", e);
                } catch (ProxyServiceAdminProxyAdminException e) {
                    log.error("while deleteProxyServices : ", e);
                    throw new APIManagerIntegrationTestException("deleteProxyServices error ", e);
                }
            }
            proxyServicesList.clear();
        }
    }

    /**
     * deleteEndpoints
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteEndpoints() throws APIManagerIntegrationTestException {
        if (endpointsList != null) {
            Iterator<String> itr = endpointsList.iterator();
            while (itr.hasNext()) {
                String endpoint = itr.next();
                try {
                    if (apimTestCaseUtils.isEndpointExist(contextUrls.getBackEndUrl(), sessionCookie,
                            endpoint)) {
                        apimTestCaseUtils.deleteEndpoint(contextUrls.getBackEndUrl(), sessionCookie,
                                endpoint);
                    }
                } catch (RemoteException e) {
                    log.error("while deleteEndpoints : ", e);
                    throw new APIManagerIntegrationTestException("deleteEndpoints error ", e);
                } catch (EndpointAdminEndpointAdminException e) {
                    log.error("while deleteEndpoints : ", e);
                    throw new APIManagerIntegrationTestException("deleteEndpoints error ", e);
                }
            }
            endpointsList.clear();
        }
    }

    /**
     * deleteLocalEntries
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteLocalEntries() throws APIManagerIntegrationTestException {
        if (localEntryList != null) {
            Iterator<String> itr = localEntryList.iterator();
            while (itr.hasNext()) {
                String localEntry = itr.next();
                try {
                    if (apimTestCaseUtils.isLocalEntryExist(contextUrls.getBackEndUrl(), sessionCookie,
                            localEntry)) {
                        apimTestCaseUtils.deleteLocalEntry(contextUrls.getBackEndUrl(), sessionCookie,
                                localEntry);
                    }
                } catch (RemoteException e) {
                    log.error("while deleteLocalEntries : ", e);
                    throw new APIManagerIntegrationTestException("deleteLocalEntries error ", e);
                } catch (LocalEntryAdminException e) {
                    log.error("while deleteLocalEntries : ", e);
                    throw new APIManagerIntegrationTestException("deleteLocalEntries error ", e);
                }
            }
            localEntryList.clear();
        }
    }

    /**
     * deleteSequenceTemplates
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteSequenceTemplates() throws APIManagerIntegrationTestException {
        if (sequenceTemplateList != null) {
            Iterator<String> itr = sequenceTemplateList.iterator();
            while (itr.hasNext()) {
                String localEntry = itr.next();
                try {
                    if (apimTestCaseUtils.isSequenceTemplateExist(contextUrls.getBackEndUrl(), sessionCookie,
                            localEntry)) {
                        apimTestCaseUtils.deleteSequenceTemplate(contextUrls.getBackEndUrl(), sessionCookie,
                                localEntry);
                    }
                } catch (RemoteException e) {
                    log.error("while deleteSequenceTemplates : ", e);
                    throw new APIManagerIntegrationTestException("deleteSequenceTemplates error ", e);
                } catch (EndpointAdminEndpointAdminException e) {
                    log.error("while deleteSequenceTemplates : ", e);
                    throw new APIManagerIntegrationTestException("deleteSequenceTemplates error ", e);
                } catch (SequenceEditorException e) {
                    log.error("while deleteSequenceTemplates : ", e);
                    throw new APIManagerIntegrationTestException("deleteSequenceTemplates error ", e);
                }
            }
            sequenceTemplateList.clear();
        }
    }

    /**
     * deleteAPI
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteAPI() throws APIManagerIntegrationTestException {
        if (apiList != null) {
            Iterator<String> itr = apiList.iterator();
            while (itr.hasNext()) {
                String api = itr.next();
                try {
                    if (apimTestCaseUtils.isApiExist(contextUrls.getBackEndUrl(), sessionCookie, api)) {
                        apimTestCaseUtils.deleteApi(contextUrls.getBackEndUrl(), sessionCookie, api);
                    }
                } catch (RemoteException e) {
                    log.error("while deleteAPI : ", e);
                    throw new APIManagerIntegrationTestException("deleteAPI error ", e);
                } catch (RestApiAdminAPIException e) {
                    log.error("while deleteAPI : ", e);
                    throw new APIManagerIntegrationTestException("deleteAPI error ", e);
                }
            }
            apiList.clear();
        }
    }

    /**
     * deletePriorityExecutors
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deletePriorityExecutors() throws APIManagerIntegrationTestException {
        if (priorityExecutorList != null) {
            Iterator<String> itr = priorityExecutorList.iterator();
            while (itr.hasNext()) {
                String executor = itr.next();
                try {
                    if (apimTestCaseUtils.isPriorityExecutorExist(contextUrls.getBackEndUrl(), sessionCookie,
                            executor)) {
                        apimTestCaseUtils.deleteProxyService(contextUrls.getBackEndUrl(), sessionCookie,
                                executor);
                    }
                } catch (RemoteException e) {
                    log.error("while deletePriorityExecutors : ", e);
                    throw new APIManagerIntegrationTestException("deletePriorityExecutors error ", e);
                } catch (ProxyServiceAdminProxyAdminException e) {
                    log.error("while deletePriorityExecutors : ", e);
                    throw new APIManagerIntegrationTestException("deletePriorityExecutors error ", e);
                }
            }
            priorityExecutorList.clear();
        }
    }

    /**
     * deleteScheduledTasks
     *
     * @throws APIManagerIntegrationTestException
     *
     */

    private void deleteScheduledTasks() throws APIManagerIntegrationTestException {
        if (scheduledTaskList != null) {
            Iterator<String[]> itr = scheduledTaskList.iterator();
            while (itr.hasNext()) {
                String[] executor = itr.next();
                try {
                    if (apimTestCaseUtils.isScheduleTaskExist(contextUrls.getBackEndUrl(), sessionCookie,
                            executor[0])) {
                        apimTestCaseUtils.deleteScheduleTask(contextUrls.getBackEndUrl(), sessionCookie,
                                executor[0], executor[1]);
                    }
                } catch (RemoteException e) {
                    log.error("while deleteScheduledTasks : ", e);
                    throw new APIManagerIntegrationTestException("deleteScheduledTasks error ", e);
                } catch (TaskManagementException e) {
                    log.error("while deleteScheduledTasks : ", e);
                    throw new APIManagerIntegrationTestException("deleteScheduledTasks error ", e);
                }
            }
            scheduledTaskList.clear();
        }
    }

    /**
     * setEndpoints
     * @param synapseConfig
     * @return
     * @throws XMLStreamException
     * @throws XPathExpressionException
     */
/*

    protected OMElement setEndpoints(OMElement synapseConfig)
            throws XMLStreamException, XPathExpressionException {

        String config = replaceEndpoints(synapseConfig.toString());
        return AXIOMUtil.stringToOM(config);
    }
*/

    /**
     * setEndpoints
     * @param dataHandler
     * @return
     * @throws XMLStreamException
     * @throws IOException
     * @throws XPathExpressionException
     */
/*
    protected DataHandler setEndpoints(DataHandler dataHandler)
            throws XMLStreamException, IOException, XPathExpressionException {

        String config = readInputStreamAsString(dataHandler.getInputStream());
        config = replaceEndpoints(config);
        ByteArrayDataSource dbs = new ByteArrayDataSource(config.getBytes());
        return new DataHandler(dbs);
    }*/

    /**
     * replaceEndpoints
     * @param config
     * @return
     * @throws XPathExpressionException
     */

    /*  private String replaceEndpoints(String config) throws XPathExpressionException {
        //this should be AS context
        String serviceUrl =
                new AutomationContext("AS", TestUserMode.SUPER_TENANT_ADMIN).getContextUrls()
                        .getServiceUrl();

        config = config.replace("http://localhost:9000/services/"
                , serviceUrl);
        config = config.replace("http://127.0.0.1:9000/services/"
                , serviceUrl);
        return config;
    }*/

    /**
     * replaceEndpoints
     *
     * @param relativePathToConfigFile
     * @param serviceName
     * @param port
     * @return
     * @throws XMLStreamException
     * @throws FileNotFoundException
     * @throws XPathExpressionException
     */

    protected OMElement replaceEndpoints(String relativePathToConfigFile, String serviceName,
                                         String port)
            throws XMLStreamException, FileNotFoundException, XPathExpressionException {
        String config = apimTestCaseUtils.loadResource(relativePathToConfigFile).toString();
        config = config.replace("http://localhost:" + port + "/services/" + serviceName,
                getBackEndServiceUrl(serviceName));

        return AXIOMUtil.stringToOM(config);
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
     * read input stream as a string
     *
     * @param in
     * @return
     * @throws IOException
     */

    private String readInputStreamAsString(InputStream in) throws IOException {

        String resultStr = null;

        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        try {

            int result = bis.read();
            while (result != -1) {
                byte b = (byte) result;
                buf.write(b);

                result = bis.read();

            }

            resultStr = buf.toString();

        } catch (IOException e) {
            log.error("Stream error occurred" + e);
            throw new IOException(e);
        } finally {
            buf.close();
            bis.close();
        }

        return resultStr;
    }

    /**
     * clean up deployed artifacts and other services
     *
     * @throws Exception
     */

    protected void cleanup() throws Exception {
        try {
            if (synapseConfiguration != null) {
                apimTestCaseUtils.deleteArtifact(synapseConfiguration, contextUrls.getBackEndUrl(),
                        sessionCookie);
                if (apimContext.getProductGroup().isClusterEnabled()) {

                    long deploymentDelay =
                            Long.parseLong(apimContext.getConfigurationValue("//deploymentDelay"));
                    Thread.sleep(deploymentDelay);
                    Iterator<OMElement> proxies =
                            synapseConfiguration.getChildrenWithLocalName("proxy");
                    while (proxies.hasNext()) {
                        String proxy = proxies.next().getAttributeValue(new QName("name"));

                        if (isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)) {
                            log.error("UnDeployment Synchronizing failed in workers");
                        }
                    }
                }
            }

            deleteProxyServices();

            deleteSequences();

            deleteEndpoints();

            deleteMessageProcessors();

            deleteMessageStores();

            deleteSequenceTemplates();

            deleteLocalEntries();

            deleteAPI();

            deletePriorityExecutors();

            deleteScheduledTasks();

        } finally {
            synapseConfiguration = null;
            proxyServicesList = null;
            messageProcessorsList = null;
            proxyServicesList = null;
            sequencesList = null;
            endpointsList = null;
            localEntryList = null;
            apiList = null;
            priorityExecutorList = null;
            apimTestCaseUtils = null;
            scheduledTaskList = null;

        }
    }


    protected String getSessionCookie() {
        return sessionCookie;
    }

    /**
     * return store session  cookie
     *
     * @return
     */

    public String getStoreSessionCookie() {
        return storeSessionCookie;
    }

    /**
     * return publisher session  cookie
     *
     * @return
     */

    public String getPublisherSessionCookie() {
        return publisherSessionCookie;
    }

    /**
     * return gateway session  cookie
     *
     * @return
     */

    public String getGatewaySessionCookie() {
        return gatewaySessionCookie;
    }


}

