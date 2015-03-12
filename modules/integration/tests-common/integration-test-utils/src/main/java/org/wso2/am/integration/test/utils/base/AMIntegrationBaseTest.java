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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.EndpointGenerator;
import org.wso2.am.integration.test.utils.generic.ServiceDeploymentUtil;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.ContextUrls;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;
import org.wso2.carbon.sequences.stub.types.SequenceEditorException;
import org.xml.sax.SAXException;

import javax.activation.DataHandler;
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
     * @throws Exception
     */

    protected void init() throws Exception {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * init the object with user mode , create context objects and get session cookies
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
        //gatewaySessionCookie = login(gatewayContext);
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
     * get main url non secure
     * @return
     */

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

    /**
     * get main url secure
     * @return
     */

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

    /**
     * api invocation URL non secure
     * @param apiContext
     * @return
     * @throws XPathExpressionException
     */

    protected String getApiInvocationURLHttp(String apiContext) throws XPathExpressionException {
        return getGatewayServerURLHttp() + apiContext;
    }

    /**
     * api invocation URL secure
     * @param apiContext
     * @return
     * @throws XPathExpressionException
     */

    protected String getApiInvocationURLHttps(String apiContext) throws XPathExpressionException {
        return getGatewayServerURLHttps() + apiContext;
    }

    /**
     * proxy service URL of deployed server non secure
     * @param proxyServiceName
     * @return
     */

    protected String getProxyServiceURLHttp(String proxyServiceName) {
        return contextUrls.getServiceUrl() + "/" + proxyServiceName;
    }

    /**
     * proxy service URL of deployed server secure
     * @param proxyServiceName
     * @return
     */

    protected String getProxyServiceURLHttps(String proxyServiceName) {
        return contextUrls.getSecureServiceUrl() + "/" + proxyServiceName;
    }


    /**
     * get server back end url secure
     * @return
     */

    protected String getBackEndURLHttps() {
        String serverUrl = contextUrls.getBackEndUrl();
        if (serverUrl.endsWith("/services")) {
            serverUrl = serverUrl.replace("/services", "");
        }
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, (serverUrl.length() - 1));
        }
        return serverUrl;
    }


    /**
     * get server back end url non secure
     * @return
     */

    protected String getServerBackendUrlHttp() throws XPathExpressionException {
        String serverUrl = contextUrls.getWebAppURL();
        return serverUrl;
    }

    /**
     * login and return session cookie
     * @param apimContext
     * @return
     * @throws IOException
     * @throws XPathExpressionException
     * @throws URISyntaxException
     * @throws SAXException
     * @throws XMLStreamException
     * @throws LoginAuthenticationExceptionException
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

    /**
     *
     * @param relativeFilePath
     * @throws Exception
     */

    protected void loadAPIMConfigurationFromClasspath(String relativeFilePath) throws Exception {
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher
                .quoteReplacement(File.separator));

        OMElement synapseConfig = apimTestCaseUtils.loadResource(relativeFilePath);
        updateAPIMConfiguration(synapseConfig);

    }

    /**
     * update API manager synapse configs
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
        apimTestCaseUtils.updateESBConfiguration(setEndpoints(synapseConfig), contextUrls.getBackEndUrl(),
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
                setEndpoints(proxyConfig));
    }

    protected void isProxyDeployed(String proxyServiceName) throws Exception {
        Assert.assertTrue(apimTestCaseUtils.isProxyDeployed(contextUrls.getBackEndUrl(), sessionCookie,
                                                   proxyServiceName),
                          "Proxy Deployment failed or time out");
    }

    protected void deleteProxyService(String proxyServiceName) throws Exception {
        if (apimTestCaseUtils.isProxyServiceExist(contextUrls.getBackEndUrl(), sessionCookie,
                                         proxyServiceName)) {
            apimTestCaseUtils.deleteProxyService(contextUrls.getBackEndUrl(), sessionCookie,
                                        proxyServiceName);

            if (!apimTestCaseUtils.isProxyUnDeployed(contextUrls.getBackEndUrl(), sessionCookie, proxyServiceName))  {
                log.error("Proxy Deletion failed or time out");
            }
        }

        if (proxyServicesList != null && proxyServicesList.contains(proxyServiceName)) {
            proxyServicesList.remove(proxyServiceName);
        }
    }

    protected void deleteSequence(String sequenceName)
            throws SequenceEditorException, RemoteException {
        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie, sequenceName)) {
            apimTestCaseUtils.deleteSequence(contextUrls.getBackEndUrl(), sessionCookie, sequenceName);
        }
        if (sequencesList != null && sequencesList.contains(sequenceName)) {
            sequencesList.remove(sequenceName);
        }
    }

    protected void addSequence(OMElement sequenceConfig) throws Exception {
        String sequenceName = sequenceConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie, sequenceName)) {
            apimTestCaseUtils.deleteSequence(contextUrls.getBackEndUrl(), sessionCookie, sequenceName);
        }
        apimTestCaseUtils.addSequence(contextUrls.getBackEndUrl(), sessionCookie,
                setEndpoints(sequenceConfig));
        if (sequencesList == null) {
            sequencesList = new ArrayList<String>();
        }
        sequencesList.add(sequenceName);
    }

    protected void addEndpoint(OMElement endpointConfig)
            throws Exception {
        String endpointName = endpointConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isSequenceExist(contextUrls.getBackEndUrl(), sessionCookie, endpointName)) {
            apimTestCaseUtils.deleteEndpoint(contextUrls.getBackEndUrl(), sessionCookie, endpointName);
        }
        apimTestCaseUtils.addEndpoint(contextUrls.getBackEndUrl(), sessionCookie,
                setEndpoints(endpointConfig));
        if (endpointsList == null) {
            endpointsList = new ArrayList<String>();
        }
        endpointsList.add(endpointName);

    }

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

    protected void addMessageProcessor(OMElement messageProcessorConfig) throws Exception {
        String messageProcessorName = messageProcessorConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isMessageProcessorExist(contextUrls.getBackEndUrl(), sessionCookie,
                                             messageProcessorName)) {
            apimTestCaseUtils.deleteMessageProcessor(contextUrls.getBackEndUrl(), sessionCookie,
                                            messageProcessorName);
        }
        apimTestCaseUtils.addMessageProcessor(contextUrls.getBackEndUrl(), sessionCookie,
                setEndpoints(messageProcessorConfig));
        if (messageProcessorsList == null) {
            messageProcessorsList = new ArrayList<String>();
        }
        messageProcessorsList.add(messageProcessorName);
    }

    protected void addMessageStore(OMElement messageStoreConfig) throws Exception {
        String messageStoreName = messageStoreConfig.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isMessageStoreExist(contextUrls.getBackEndUrl(), sessionCookie,
                                         messageStoreName)) {
            apimTestCaseUtils.deleteMessageStore(contextUrls.getBackEndUrl(), sessionCookie,
                                        messageStoreName);
        }
        apimTestCaseUtils.addMessageStore(contextUrls.getBackEndUrl(), sessionCookie,
                setEndpoints(messageStoreConfig));
        if (messageStoresList == null) {
            messageStoresList = new ArrayList<String>();
        }
        messageStoresList.add(messageStoreName);
    }

    protected void addSequenceTemplate(OMElement sequenceTemplate) throws Exception {
        String name = sequenceTemplate.getAttributeValue(new QName("name"));
        if (apimTestCaseUtils.isSequenceTemplateExist(contextUrls.getBackEndUrl(), sessionCookie, name)) {
            apimTestCaseUtils.deleteSequenceTemplate(contextUrls.getBackEndUrl(), sessionCookie, name);
        }
        apimTestCaseUtils.addSequenceTemplate(contextUrls.getBackEndUrl(), sessionCookie,
                setEndpoints(sequenceTemplate));

        if (sequenceTemplateList == null) {
            sequenceTemplateList = new ArrayList<String>();
        }
        sequenceTemplateList.add(name);
    }

    protected void addApi(OMElement api) throws Exception {
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
        scheduledTaskList.add(new String[] { taskName, taskGroup });
    }


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

    private void deleteSequences() {
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
                    } catch (Exception e) {
                        log.error("while undeploying Sequence. : " , e);
                    }
                }
            }
            sequencesList.clear();
        }
    }

    private void deleteProxyServices() {
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
                } catch (Exception e) {
                    log.error("Proxy deletion error : " , e);
                }
            }
            proxyServicesList.clear();
        }
    }

    private void deleteEndpoints() {
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
                } catch (Exception e) {
                    log.error("Endpoint deletion error : " , e);
                }
            }
            endpointsList.clear();
        }
    }

    private void deleteLocalEntries() {
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
                } catch (Exception e) {
                    log.error("Local entry deletion error : " , e);
                }
            }
            localEntryList.clear();
        }
    }

    private void deleteSequenceTemplates() {
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
                } catch (Exception e) {
                    log.error("Sequence entry deletion error : " , e);
                }
            }
            sequenceTemplateList.clear();
        }
    }

    private void deleteApi() {
        if (apiList != null) {
            Iterator<String> itr = apiList.iterator();
            while (itr.hasNext()) {
                String api = itr.next();
                try {
                    if (apimTestCaseUtils.isApiExist(contextUrls.getBackEndUrl(), sessionCookie, api)) {
                        apimTestCaseUtils.deleteApi(contextUrls.getBackEndUrl(), sessionCookie, api);
                    }
                } catch (Exception e) {
                    log.error("API deletion error : " , e);
                }
            }
            apiList.clear();
        }
    }

    private void deletePriorityExecutors() {
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
                } catch (Exception e) {
                    log.error("Priority executor deletion error : " , e);
                }
            }
            priorityExecutorList.clear();
        }
    }

    private void deleteScheduledTasks() {
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
                } catch (Exception e) {
                    log.error("Scheduled task deletion error : " , e);
                }
            }
            scheduledTaskList.clear();
        }
    }

    protected OMElement setEndpoints(OMElement synapseConfig)
            throws XMLStreamException, XPathExpressionException {

        String config = replaceEndpoints(synapseConfig.toString());
        return AXIOMUtil.stringToOM(config);
    }

    protected DataHandler setEndpoints(DataHandler dataHandler)
            throws XMLStreamException, IOException, XPathExpressionException {

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
        String config = apimTestCaseUtils.loadResource(relativePathToConfigFile).toString();
        config = config.replace("http://localhost:" + port + "/services/" + serviceName,
                                getBackEndServiceUrl(serviceName));

        return AXIOMUtil.stringToOM(config);
    }

    private boolean isProxyWSDlExist(String serviceUrl, long synchronizingDelay)
            throws Exception {
        return new ServiceDeploymentUtil().isServiceWSDlExist(serviceUrl, synchronizingDelay);

    }


    /**
     * read input stream as a string
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
            log.error("Stream error occured" + e);
            throw new IOException(e);
        }
        finally {
            buf.close();
            bis.close();
        }

        return resultStr;
    }

    /**
     * clean up deployed artifacts and other services
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

            deleteApi();

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

    /**
     * return publisher server url non secure
     * ex :  https://localhost:9743
     * @return
     */

    protected String getStoreServerURLHttp() {
        return storeUrls.getWebAppURL();
    }

    /**
     * return publisher server url secure
     * ex :  https://localhost:9443
     * @return
     */

    protected String getStoreServerURLHttps() {

        String httpsURL = null;

        httpsURL = storeUrls.getBackEndUrl();

        if (httpsURL.endsWith("/services")) {
            httpsURL = httpsURL.replace("/services", "");
        }

        return httpsURL;
    }

    /**
     * return publisher server url non secure
     * ex :  https://localhost:9743
     * @return
     */

    protected String getPublisherServerURLHttp() {
        return publisherUrls.getWebAppURL();
    }

    /**
     * return publisher server url secure
     * ex :  https://localhost:9443
     * @return
     */

    protected String getPublisherServerURLHttps() {
        String httpsURL = publisherUrls.getBackEndUrl();
        if (httpsURL.endsWith("/services")) {
            httpsURL = httpsURL.replace("/services", "");
        }

        return httpsURL;
    }

    /**
     * return gateway server url non secure
     * ex :  http://localhost:8280
     * @return
     */

    protected String getGatewayServerURLHttp() {
        String httpURL = gatewayUrls.getServiceUrl();
        if (httpURL.endsWith("/services")) {
            httpURL = httpURL.replace("/services", "");
        }

        return httpURL;
    }

    /**
     * return gateway server url secure
     * ex : https://localhost:8243
     * @return
     */

    protected String getGatewayServerURLHttps() {
        String httpsURL = gatewayUrls.getSecureServiceUrl();
        if (httpsURL.endsWith("/services")) {
            httpsURL = httpsURL.replace("/services", "");
        }

        return httpsURL;
    }

    /**
     * get APIM server sessionCookie
     * @return
     */

    protected String getSessionCookie() {
        return sessionCookie;
    }

    /**
     * return store session  cookie
     * @return
     */

    public String getStoreSessionCookie() {
        return storeSessionCookie;
    }

    /**
     * return publisher session  cookie
     * @return
     */

    public String getPublisherSessionCookie() {
        return publisherSessionCookie;
    }

    /**
     * return gateway session  cookie
     * @return
     */

    public String getGatewaySessionCookie() {
        return gatewaySessionCookie;
    }


}

