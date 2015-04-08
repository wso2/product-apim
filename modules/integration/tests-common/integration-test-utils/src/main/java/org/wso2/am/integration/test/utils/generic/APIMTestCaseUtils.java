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

package org.wso2.am.integration.test.utils.generic;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.admin.clients.endpoint.EndPointAdminClient;
import org.wso2.am.admin.clients.localentry.LocalEntriesAdminClient;
import org.wso2.am.admin.clients.mediation.MessageProcessorClient;
import org.wso2.am.admin.clients.mediation.MessageStoreAdminClient;
import org.wso2.am.admin.clients.mediation.PriorityMediationAdminClient;
import org.wso2.am.admin.clients.proxy.admin.ProxyServiceAdminClient;
import org.wso2.am.admin.clients.rest.api.RestApiAdminClient;
import org.wso2.am.admin.clients.sequences.SequenceAdminServiceClient;
import org.wso2.am.admin.clients.service.mgt.ServiceAdminClient;
import org.wso2.am.admin.clients.tasks.TaskAdminClient;
import org.wso2.am.admin.clients.template.EndpointTemplateAdminServiceClient;
import org.wso2.am.admin.clients.template.SequenceTemplateAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIBean;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException;
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminException;
import org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException;
import org.wso2.carbon.rest.api.stub.RestApiAdminAPIException;
import org.wso2.carbon.sequences.stub.types.SequenceEditorException;
import org.wso2.carbon.task.stub.TaskManagementException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;

public class APIMTestCaseUtils {

    private static final Log log = LogFactory.getLog(APIMTestCaseUtils.class);
    private static int SERVICE_DEPLOYMENT_DELAY = TestConfigurationProvider.getServiceDeploymentDelay();
    private static final String PROXY = "proxy";
    private static final String LOCAL_ENTRY = "localEntry";
    private static final String ENDPOINT = "endpoint";
    private static final String SEQUENCE = "sequence";
    private static final String MESSAGE_STORE = "messageStore";
    private static final String MESSAGE_PROCESSOR = "messageProcessor";
    private static final String TEMPLATE = "template";
    private static final String API = "api";
    private static final String PRIORITY_EXECUTOR = "priorityExecutor";
    private static final String KEY = "key";
    private static final String NAME = "name";
    private static final String VERSION = "version";

    /**
     * Loads the specified resource from the classpath and returns its content as an OMElement.
     *
     * @param path A relative path to the resource file
     * @return An OMElement containing the resource content
     */
    public OMElement loadResource(String path) throws FileNotFoundException, XMLStreamException {
        OMElement documentElement = null;
        FileInputStream inputStream = null;
        XMLStreamReader parser = null;
        StAXOMBuilder builder = null;
        path = TestConfigurationProvider.getResourceLocation() + path;
        File file = new File(path);
        if (file.exists()) {
            try {
                inputStream = new FileInputStream(file);
                parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
                //create the builder
                builder = new StAXOMBuilder(parser);
                //get the root element (in this case the envelope)
                documentElement = builder.getDocumentElement().cloneOMElement();
            } finally {
                if (builder != null) {
                    builder.close();
                }
                if (parser != null) {
                    try {
                        parser.close();
                    } catch (XMLStreamException e) {
                        //ignore
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }

            }
        } else {
            throw new FileNotFoundException("File Not Exist at " + path);
        }
        return documentElement;
    }

    /**
     * @param synapseConfig - Synapse configuration to be updated
     * @param backendURL    - backend URL of the gateway server
     * @param sessionCookie - session cookie of user login
     * @throws Exception - Throws if update fails
     */
    public void updateSynapseConfiguration(OMElement synapseConfig, String backendURL,
                                           String sessionCookie)
            throws Exception {

        ProxyServiceAdminClient proxyAdmin = new ProxyServiceAdminClient(backendURL, sessionCookie);
        EndPointAdminClient endPointAdminClient =
                new EndPointAdminClient(backendURL, sessionCookie);
        SequenceAdminServiceClient sequenceAdminClient =
                new SequenceAdminServiceClient(backendURL, sessionCookie);
        LocalEntriesAdminClient localEntryAdminServiceClient =
                new LocalEntriesAdminClient(backendURL, sessionCookie);
        MessageProcessorClient messageProcessorClient =
                new MessageProcessorClient(backendURL, sessionCookie);
        MessageStoreAdminClient messageStoreAdminClient =
                new MessageStoreAdminClient(backendURL, sessionCookie);
        ServiceAdminClient adminServiceService = new ServiceAdminClient(backendURL, sessionCookie);
        EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
                new EndpointTemplateAdminServiceClient(backendURL, sessionCookie);
        SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
                new SequenceTemplateAdminServiceClient(backendURL, sessionCookie);
        RestApiAdminClient apiAdminClient = new RestApiAdminClient(backendURL, sessionCookie);
        PriorityMediationAdminClient priorityMediationAdminClient =
                new PriorityMediationAdminClient(backendURL, sessionCookie);

        checkLocalEntries(synapseConfig, backendURL, sessionCookie, localEntryAdminServiceClient);

        checkEndPoints(synapseConfig, backendURL, sessionCookie, endPointAdminClient);

        checkSequences(synapseConfig, backendURL, sessionCookie, sequenceAdminClient);

        checkProxies(synapseConfig, backendURL, sessionCookie, proxyAdmin, adminServiceService);

        checkMessageStores(synapseConfig, backendURL, sessionCookie, messageStoreAdminClient);

        checkMessageProcessors(synapseConfig, backendURL, sessionCookie, messageProcessorClient);

        checkTemplates(synapseConfig, backendURL, sessionCookie, endpointTemplateAdminServiceClient, sequenceTemplateAdminServiceClient);

        checkAPIs(synapseConfig, backendURL, sessionCookie, apiAdminClient);

        checkPriorityExecutors(synapseConfig, backendURL, sessionCookie, priorityMediationAdminClient);

        Thread.sleep(1000);
        verifySynapseDeployment(synapseConfig, backendURL, sessionCookie);
        log.info("Synapse configuration  Deployed");

    }

    private void checkPriorityExecutors(OMElement synapseConfig, String backendURL,
                                        String sessionCookie,
                                        PriorityMediationAdminClient priorityMediationAdminClient)
            throws RemoteException {
        Iterator priorityExecutorList =
                synapseConfig.getChildrenWithLocalName(PRIORITY_EXECUTOR);
        while (priorityExecutorList.hasNext()) {
            OMElement executor = (OMElement) priorityExecutorList.next();
            String executorName = executor.getAttributeValue(new QName(NAME));
            if (ArrayUtils.contains(priorityMediationAdminClient.getExecutorList(), executorName)) {
                priorityMediationAdminClient.remove(executorName);
                assertTrue(
                        isPriorityExecutorUnDeployed(backendURL, sessionCookie, executorName)
                        , executorName + " Priority Executor undeployment failed");
            }
            priorityMediationAdminClient.addPriorityMediator(executorName, executor);
            log.info(executorName + " Priority Executor Uploaded");
        }
    }

    private void checkAPIs(OMElement synapseConfig, String backendURL, String sessionCookie,
                           RestApiAdminClient apiAdminClient)
            throws RestApiAdminAPIException, RemoteException {
        Iterator apiElements = synapseConfig.getChildrenWithLocalName(API);
        while (apiElements.hasNext()) {
            OMElement api = (OMElement) apiElements.next();
            String apiName = api.getAttributeValue(new QName(NAME));
            if (ArrayUtils.contains(apiAdminClient.getApiNames(), apiName)) {
                apiAdminClient.deleteApi(apiName);
                assertTrue(isApiUnDeployed(backendURL, sessionCookie, apiName)
                        , apiName + " Api undeployment failed");
            }
            apiAdminClient.add(api);
            log.info(apiName + " API Uploaded");
        }
    }

    private void checkTemplates(OMElement synapseConfig, String backendURL, String sessionCookie,
                                EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient,
                                SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient)
            throws RemoteException, EndpointAdminEndpointAdminException {
        Iterator templates = synapseConfig.getChildrenWithLocalName(TEMPLATE);
        while (templates.hasNext()) {
            OMElement template = (OMElement) templates.next();
            String templateName = template.getAttributeValue(new QName(NAME));
            if (template.getFirstChildWithName(
                    new QName(template.getNamespace().getNamespaceURI(), SEQUENCE)) != null) {
                if (ArrayUtils.contains(sequenceTemplateAdminServiceClient.getSequenceTemplates(),
                                        templateName)) {
                    sequenceTemplateAdminServiceClient.deleteTemplate(templateName);
                    assertTrue(
                            isSequenceTemplateUnDeployed(backendURL, sessionCookie, templateName)
                            , templateName + " Sequence Template undeployment failed");
                }
                sequenceTemplateAdminServiceClient.addSequenceTemplate(template);

            } else {

                if (ArrayUtils.contains(endpointTemplateAdminServiceClient.getEndpointTemplates(),
                                        templateName)) {
                    endpointTemplateAdminServiceClient.deleteEndpointTemplate(templateName);
                    assertTrue(
                            isEndpointTemplateUnDeployed(backendURL, sessionCookie, templateName)
                            , templateName + " Endpoint Template undeployment failed");
                }
                endpointTemplateAdminServiceClient.addEndpointTemplate(template);
            }
            log.info(templateName + " Template Uploaded");
        }
    }

    private void checkMessageProcessors(OMElement synapseConfig, String backendURL,
                                        String sessionCookie,
                                        MessageProcessorClient messageProcessorClient)
            throws RemoteException, SequenceEditorException {
        Iterator messageProcessors =
                synapseConfig.getChildrenWithLocalName(MESSAGE_PROCESSOR);
        while (messageProcessors.hasNext()) {
            OMElement messageProcessor = (OMElement) messageProcessors.next();
            String mProcessor = messageProcessor.getAttributeValue(new QName(NAME));
            if (ArrayUtils
                    .contains(messageProcessorClient.getMessageProcessorNames(), mProcessor)) {
                messageProcessorClient.deleteMessageProcessor(mProcessor);
                assertTrue(
                        isMessageProcessorUnDeployed(backendURL, sessionCookie, mProcessor)
                        , mProcessor + " Message Processor undeployment failed");
            }
            messageProcessorClient.addMessageProcessor(messageProcessor);
            log.info(mProcessor + " Message Processor Uploaded");
        }
    }

    private void checkMessageStores(OMElement synapseConfig, String backendURL,
                                    String sessionCookie,
                                    MessageStoreAdminClient messageStoreAdminClient)
            throws RemoteException, SequenceEditorException,
                   org.wso2.carbon.message.store.stub.Exception {
        Iterator messageStores = synapseConfig.getChildrenWithLocalName(MESSAGE_STORE);
        while (messageStores.hasNext()) {
            OMElement messageStore = (OMElement) messageStores.next();
            String mStore = messageStore.getAttributeValue(new QName(NAME));
            if (ArrayUtils.contains(messageStoreAdminClient.getMessageStores(), mStore)) {
                messageStoreAdminClient.deleteMessageStore(mStore);
                assertTrue(isMessageStoreUnDeployed(backendURL, sessionCookie, mStore),
                           mStore + " Message Store undeployment failed");
            }
            messageStoreAdminClient.addMessageStore(messageStore);
            log.info(mStore + " Message Store Uploaded");
        }
    }

    private void checkProxies(OMElement synapseConfig, String backendURL, String sessionCookie,
                              ProxyServiceAdminClient proxyAdmin,
                              ServiceAdminClient adminServiceService) throws Exception {
        Iterator proxies = synapseConfig.getChildrenWithLocalName(PROXY);
        while (proxies.hasNext()) {
            OMElement proxy = (OMElement) proxies.next();
            String proxyName = proxy.getAttributeValue(new QName(NAME));
            if (adminServiceService.isServiceExists(proxyName)) {
                proxyAdmin.deleteProxy(proxyName);
                assertTrue(isProxyUnDeployed(backendURL, sessionCookie, proxyName),
                           proxyName + " Undeployment failed");
            }
            proxyAdmin.addProxyService(proxy);
            log.info(proxyName + " Proxy Uploaded");
        }
    }

    private void checkSequences(OMElement synapseConfig, String backendURL, String sessionCookie,
                                SequenceAdminServiceClient sequenceAdminClient)
            throws SequenceEditorException, RemoteException {
        Iterator sequences = synapseConfig.getChildrenWithLocalName(SEQUENCE);
        while (sequences.hasNext()) {
            OMElement sequence = (OMElement) sequences.next();
            String sqn = sequence.getAttributeValue(new QName(NAME));
            boolean isSequenceExist = ArrayUtils.contains(sequenceAdminClient.getSequences(), sqn);
            if (("main".equalsIgnoreCase(sqn) || "fault".equalsIgnoreCase(sqn)) &&
                isSequenceExist) {
                sequenceAdminClient.updateSequence(sequence);
            } else {
                if (isSequenceExist) {
                    sequenceAdminClient.deleteSequence(sqn);
                    assertTrue(isSequenceUnDeployed(backendURL, sessionCookie, sqn),
                               sqn + " Sequence undeployment failed");
                }
                sequenceAdminClient.addSequence(sequence);
            }
            log.info(sqn + " Sequence Uploaded");
        }
    }

    private void checkEndPoints(OMElement synapseConfig, String backendURL, String sessionCookie,
                                EndPointAdminClient endPointAdminClient)
            throws EndpointAdminEndpointAdminException, IOException, XMLStreamException {
        Iterator endpoints = synapseConfig.getChildrenWithLocalName(ENDPOINT);
        while (endpoints.hasNext()) {
            OMElement endpoint = (OMElement) endpoints.next();
            String ep = endpoint.getAttributeValue(new QName(NAME));
            if (ArrayUtils.contains(endPointAdminClient.getEndpointNames(), ep)) {
                assertTrue(endPointAdminClient.deleteEndpoint(ep),
                           ep + " Endpoint deletion failed");
                assertTrue(isEndpointUnDeployed(backendURL, sessionCookie, ep),
                           ep + " Endpoint undeployment failed");
            }
            assertTrue(endPointAdminClient.addEndPoint(endpoint),
                       ep + " Endpoint addition failed");
            log.info(ep + " Endpoint Uploaded");
        }
    }

    private void checkLocalEntries(OMElement synapseConfig, String backendURL, String sessionCookie,
                                   LocalEntriesAdminClient localEntryAdminServiceClient)
            throws LocalEntryAdminException, RemoteException {
        Iterator localEntries = synapseConfig.getChildrenWithLocalName(LOCAL_ENTRY);
        while (localEntries.hasNext()) {
            OMElement localEntry = (OMElement) localEntries.next();
            String le = localEntry.getAttributeValue(new QName(KEY));
            if (ArrayUtils.contains(localEntryAdminServiceClient.getEntryNames(), le)) {
                assertTrue(localEntryAdminServiceClient.deleteLocalEntry(le),
                           le + " Local Entry deletion failed");
                assertTrue(isLocalEntryUnDeployed(backendURL, sessionCookie, le),
                           le + " Local Entry undeployment failed");
            }
            assertTrue(localEntryAdminServiceClient.addLocalEntry(localEntry),
                       le + " Local Entry addition failed");
            log.info(le + " LocalEntry Uploaded");
        }
    }

    public void addProxyService(String backEndUrl, String sessionCookie, OMElement proxyConfig)
            throws Exception {
        ProxyServiceAdminClient proxyAdmin = new ProxyServiceAdminClient(backEndUrl, sessionCookie);
        proxyAdmin.addProxyService(proxyConfig);
        String proxyName = proxyConfig.getAttributeValue(new QName(NAME));
        assertTrue(isProxyDeployed(backEndUrl, sessionCookie, proxyName),
                   "Proxy Deployment failed or time out");

    }

    public void addEndpoint(String backEndUrl, String sessionCookie, OMElement endpointConfig)
            throws Exception {
        EndPointAdminClient endPointAdminClient =
                new EndPointAdminClient(backEndUrl, sessionCookie);
        endPointAdminClient.addEndPoint(endpointConfig);
        String ep = endpointConfig.getAttributeValue(new QName(NAME));
        assertTrue(isEndpointDeployed(backEndUrl, sessionCookie, ep),
                   ep + "Endpoint deployment not found or time out");
    }

    public void addLocalEntry(String backEndUrl, String sessionCookie,
                              OMElement localEntryConfig) throws Exception {
        LocalEntriesAdminClient localEntryAdminServiceClient =
                new LocalEntriesAdminClient(backEndUrl,
                                            sessionCookie);
        boolean value = localEntryAdminServiceClient.addLocalEntry(localEntryConfig);
        assertTrue(value, "LocalEntry Addition failed");
        if (value) {
            String le = localEntryConfig.getAttributeValue(new QName(KEY));
            assertTrue(isLocalEntryDeployed(backEndUrl, sessionCookie, le),
                       le + "LocalEntry deployment not found or time out");
        }

    }

    public void addSequence(String backEndUrl, String sessionCookie, OMElement sequenceConfig)
            throws Exception {
        SequenceAdminServiceClient sequenceAdminClient = new SequenceAdminServiceClient(backEndUrl,
                                                                                        sessionCookie);
        sequenceAdminClient.addSequence(sequenceConfig);
        String sqn = sequenceConfig.getAttributeValue(new QName(NAME));
        assertTrue(isSequenceDeployed(backEndUrl, sessionCookie, sqn),
                   sqn + "Sequence deployment not found or time out");

    }

    public void addMessageStore(String backEndUrl, String sessionCookie, OMElement messageStore)
            throws Exception {
        MessageStoreAdminClient messageStoreAdminClient =
                new MessageStoreAdminClient(backEndUrl, sessionCookie);
        messageStoreAdminClient.addMessageStore(messageStore);
        String mStoreName = messageStore.getAttributeValue(new QName(NAME));
        assertTrue(isMessageStoreDeployed(backEndUrl, sessionCookie, mStoreName),
                   "Message Store Deployment failed");
    }

    public void addMessageProcessor(String backEndUrl, String sessionCookie,
                                    OMElement messageProcessor)
            throws Exception {
        MessageProcessorClient messageProcessorClient =
                new MessageProcessorClient(backEndUrl, sessionCookie);
        messageProcessorClient.addMessageProcessor(messageProcessor);
        String mProcessorName = messageProcessor.getAttributeValue(new QName(NAME));
        assertTrue(isMessageProcessorDeployed(backEndUrl, sessionCookie, mProcessorName),
                   "Message Processor deployment failed");
    }

    public void addSequenceTemplate(String backEndUrl, String sessionCookie,
                                    OMElement sequenceTemplate) throws RemoteException {
        SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
                new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
        sequenceTemplateAdminServiceClient.addSequenceTemplate(sequenceTemplate);
        String seqTmpName = sequenceTemplate.getAttributeValue(new QName(NAME));
        assertTrue(isSequenceTemplateDeployed(backEndUrl, sessionCookie, seqTmpName),
                   "Sequence Template deployment failed");

    }

    public void addEndpointTemplate(String backEndUrl, String sessionCookie,
                                    OMElement endpointTemplate) throws RemoteException {
        EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
                new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
        endpointTemplateAdminServiceClient.addEndpointTemplate(endpointTemplate);
        String endpointTmpName = endpointTemplate.getAttributeValue(new QName(NAME));
        assertTrue(isEndpointTemplateDeployed(backEndUrl, sessionCookie, endpointTmpName),
                   "Endpoint Template deployment failed");

    }

    public void addAPI(String backEndUrl, String sessionCookie,
                       OMElement api) throws RemoteException, RestApiAdminAPIException {
        RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
        apiAdminClient.add(api);
        String apiName = api.getAttributeValue(new QName(NAME));
        assertTrue(isApiDeployed(backEndUrl, sessionCookie, apiName),
                   "Rest Api deployment failed");
    }

    public void addPriorityExecutor(String backEndUrl, String sessionCookie,
                                    OMElement priorityExecutor)
            throws RemoteException {
        PriorityMediationAdminClient priorityMediationAdminClient =
                new PriorityMediationAdminClient(backEndUrl, sessionCookie);
        String executorName = priorityExecutor.getAttributeValue(new QName(NAME));
        priorityMediationAdminClient.addPriorityMediator(executorName, priorityExecutor);
        assertTrue(isPriorityExecutorDeployed(backEndUrl, sessionCookie, executorName),
                   "Priority Executor deployment failed");
    }

    public void addScheduleTask(String backEndUrl, String sessionCookie, OMElement taskDescription)
            throws TaskManagementException, RemoteException {
        TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
        taskAdminClient.addTask(taskDescription);
        assertTrue(isScheduleTaskDeployed(backEndUrl, sessionCookie
                           , taskDescription.getAttributeValue(new QName("name"))),
                   "ScheduleTask deployment failed"
        );
    }

    public boolean isProxyDeployed(String backEndUrl, String sessionCookie, String proxyName)
            throws RemoteException {
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Proxy deployment " +
                 proxyName);

        boolean isServiceDeployed = false;
        ServiceAdminClient adminServiceService = new ServiceAdminClient(backEndUrl, sessionCookie);
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            if (adminServiceService.isServiceExists(proxyName)) {
                isServiceDeployed = true;
                log.info(proxyName + " Proxy Deployed in " + time + " millis");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
        }

        return isServiceDeployed;

    }

    public boolean isEndpointDeployed(String backEndUrl, String sessionCookie, String endpointName)
            throws EndpointAdminEndpointAdminException, RemoteException {
        EndPointAdminClient endPointAdminClient = new EndPointAdminClient(backEndUrl,
                                                                          sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Endpoint " + endpointName);
        boolean isEndpointExist = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] endpoints = endPointAdminClient.getEndpointNames();
            if (endpoints != null && endpoints.length > 0) {
                for (String endpoint : endpoints) {

                    if (endpointName.equals(endpoint)) {
                        isEndpointExist = true;
                        log.info(endpointName + " Endpoint Found in " + time + " millis");
                        break;
                    }
                }
            }
            if (isEndpointExist) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isEndpointExist;
    }

    public boolean isMessageProcessorDeployed(String backEndUrl, String sessionCookie,
                                              String messageProcessorName)
            throws SequenceEditorException, RemoteException {
        MessageProcessorClient messageProcessorClient =
                new MessageProcessorClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Message Processor " +
                 messageProcessorName);
        boolean isMessageStoreExist = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] messageProcessors = messageProcessorClient.getMessageProcessorNames();
            if (messageProcessors != null && messageProcessors.length > 0) {
                for (String mp : messageProcessors) {

                    if (mp.equals(messageProcessorName)) {
                        isMessageStoreExist = true;
                        log.info(messageProcessorName + " Message Processor Found in " + time +
                                 " millis");
                        break;
                    }
                }
            }
            if (isMessageStoreExist) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isMessageStoreExist;
    }

    public boolean isSequenceDeployed(String backEndUrl, String sessionCookie, String sequenceName)
            throws SequenceEditorException, RemoteException {
        SequenceAdminServiceClient sequenceAdminServiceClient =
                new SequenceAdminServiceClient(backEndUrl,
                                               sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Sequence " + sequenceName);
        boolean isSequenceExist = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] sequences = sequenceAdminServiceClient.getSequences();
            if (sequences != null && sequences.length > 0) {
                for (String sequence : sequences) {

                    if (sequence.equals(sequenceName)) {
                        isSequenceExist = true;
                        log.info(sequenceName + " Sequence Found in " + time + " millis");
                        break;
                    }
                }
            }
            if (isSequenceExist) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isSequenceExist;
    }

    public boolean isMessageStoreDeployed(String backEndUrl, String sessionCookie,
                                          String messageStoreName)
            throws SequenceEditorException, RemoteException {
        MessageStoreAdminClient messageStoreAdminClient =
                new MessageStoreAdminClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Message Store " +
                 messageStoreName);
        boolean isMessageStoreExist = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] messageStores = messageStoreAdminClient.getMessageStores();
            if (messageStores != null && messageStores.length > 0) {
                for (String ms : messageStores) {

                    if (ms.equals(messageStoreName)) {
                        isMessageStoreExist = true;
                        log.info(messageStoreName + " Message Store Found in " + time + " millis");
                        break;
                    }
                }
            }
            if (isMessageStoreExist) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isMessageStoreExist;
    }

    public boolean isSequenceTemplateDeployed(String backEndUrl, String sessionCookie,
                                              String sequenceTemplateName) throws RemoteException {
        SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
                new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Sequence Template " +
                 sequenceTemplateName);
        boolean isSequenceTmpFound = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] templates = sequenceTemplateAdminServiceClient.getSequenceTemplates();
            if (templates != null && templates.length > 0) {
                for (String tmpl : templates) {

                    if (tmpl.equals(sequenceTemplateName)) {
                        isSequenceTmpFound = true;
                        log.info(sequenceTemplateName + " Sequence Template Found in " + time +
                                 " millis");
                        break;
                    }
                }
            }
            if (isSequenceTmpFound) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isSequenceTmpFound;
    }

    public boolean isEndpointTemplateDeployed(String backEndUrl, String sessionCookie,
                                              String endpointTemplateName) throws RemoteException {
        EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
                new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Endpoint Template " +
                 endpointTemplateName);
        boolean isEndpointTmpFound = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] templates = endpointTemplateAdminServiceClient.getEndpointTemplates();
            if (templates != null && templates.length > 0) {
                for (String tmpl : templates) {

                    if (tmpl.equals(endpointTemplateName)) {
                        isEndpointTmpFound = true;
                        log.info(endpointTemplateName + " Endpoint Template Found in " + time +
                                 " millis");
                        break;
                    }
                }
            }
            if (isEndpointTmpFound) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isEndpointTmpFound;
    }

    public boolean isApiDeployed(String backEndUrl, String sessionCookie, String apiName)
            throws RemoteException, RestApiAdminAPIException {
        RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for API " + apiName);
        boolean isApiFound = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] apiList = apiAdminClient.getApiNames();
            if (apiList != null && apiList.length > 0) {
                for (String restApi : apiList) {

                    if (restApi.equals(apiName)) {
                        isApiFound = true;
                        log.info(apiName + " API Found in " + time + " millis");
                        break;
                    }
                }
            }
            if (isApiFound) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isApiFound;
    }

    public boolean isPriorityExecutorDeployed(String backEndUrl, String sessionCookie,
                                              String executorName)
            throws RemoteException {
        PriorityMediationAdminClient priorityMediationAdminClient =
                new PriorityMediationAdminClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Priority Executor " +
                 executorName);
        boolean isExecutorFound = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] executorList = priorityMediationAdminClient.getExecutorList();
            if (executorList != null && executorList.length > 0) {
                for (String executor : executorList) {

                    if (executor.equals(executorName)) {
                        isExecutorFound = true;
                        log.info(executorName + " Priority Executor Found in " + time + " millis");
                        break;
                    }
                }
            }
            if (isExecutorFound) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isExecutorFound;
    }

    public boolean isScheduleTaskDeployed(String backEndUrl, String sessionCookie, String taskName)
            throws RemoteException, TaskManagementException {
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Task deployment " + taskName);

        boolean isTaskDeployed = false;
        TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            if (taskAdminClient.getScheduleTaskList().contains(taskName)) {
                isTaskDeployed = true;
                log.info(taskName + " Task Deployed in " + time + " millis");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
        }

        return isTaskDeployed;
    }

    public boolean isProxyServiceExist(String backEndUrl, String sessionCookie, String proxyName)
            throws RemoteException {
        ServiceAdminClient adminServiceService = new ServiceAdminClient(backEndUrl, sessionCookie);
        return adminServiceService.isServiceExists(proxyName);

    }

    public boolean isLocalEntryExist(String backEndUrl, String sessionCookie, String localEntryName)
            throws LocalEntryAdminException, RemoteException {
        LocalEntriesAdminClient localEntryAdminServiceClient =
                new LocalEntriesAdminClient(backEndUrl,
                                            sessionCookie);
        String[] localEntries = localEntryAdminServiceClient.getEntryNames();
        if (localEntries == null || localEntries.length == 0) {
            return false;
        }
        return ArrayUtils.contains(localEntries, localEntryName);
    }

    public boolean isSequenceExist(String backEndUrl, String sessionCookie, String sequenceName)
            throws SequenceEditorException, RemoteException {
        SequenceAdminServiceClient sequenceAdminServiceClient =
                new SequenceAdminServiceClient(backEndUrl,
                                               sessionCookie);
        String[] sequences = sequenceAdminServiceClient.getSequences();
        if (sequences == null || sequences.length == 0) {
            return false;
        }
        return ArrayUtils.contains(sequences, sequenceName);
    }

    public boolean isEndpointExist(String backEndUrl, String sessionCookie, String endpointName)
            throws EndpointAdminEndpointAdminException, RemoteException {
        EndPointAdminClient endPointAdminClient = new EndPointAdminClient(backEndUrl,
                                                                          sessionCookie);
        String[] endpoints = endPointAdminClient.getEndpointNames();
        if (endpoints == null || endpoints.length == 0) {
            return false;
        }
        return ArrayUtils.contains(endpoints, endpointName);
    }

    public boolean isMessageStoreExist(String backEndUrl, String sessionCookie,
                                       String messageProcessor) throws RemoteException {
        MessageStoreAdminClient messageStoreAdminClient =
                new MessageStoreAdminClient(backEndUrl, sessionCookie);
        return ArrayUtils.contains(messageStoreAdminClient.getMessageStores(), messageProcessor);

    }

    public boolean isMessageProcessorExist(String backEndUrl, String sessionCookie,
                                           String messageProcessor) throws RemoteException {
        MessageProcessorClient messageProcessorClient =
                new MessageProcessorClient(backEndUrl, sessionCookie);
        return ArrayUtils
                .contains(messageProcessorClient.getMessageProcessorNames(), messageProcessor);

    }

    public boolean isSequenceTemplateExist(String backEndUrl, String sessionCookie,
                                           String sequenceTemplate) throws RemoteException {
        SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
                new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
        return ArrayUtils.contains(sequenceTemplateAdminServiceClient.getSequenceTemplates(),
                                   sequenceTemplate);

    }

    public boolean isEndpointTemplateExist(String backEndUrl, String sessionCookie,
                                           String endpointTemplate) throws RemoteException {
        EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
                new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
        return ArrayUtils.contains(endpointTemplateAdminServiceClient.getEndpointTemplates(),
                                   endpointTemplate);

    }

    public boolean isApiExist(String backEndUrl, String sessionCookie, String apiName)
            throws RemoteException, RestApiAdminAPIException {
        RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
        return ArrayUtils.contains(apiAdminClient.getApiNames(), apiName);

    }

    public boolean isPriorityExecutorExist(String backEndUrl, String sessionCookie,
                                           String priorityExecutorName) throws RemoteException {
        PriorityMediationAdminClient priorityMediationAdminClient =
                new PriorityMediationAdminClient(backEndUrl, sessionCookie);
        return ArrayUtils
                .contains(priorityMediationAdminClient.getExecutorList(), priorityExecutorName);

    }

    public boolean isScheduleTaskExist(String backEndUrl, String sessionCookie, String taskName)
            throws RemoteException, TaskManagementException {
        TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
        return taskAdminClient.getScheduleTaskList().contains(taskName);
    }

    public void deleteProxyService(String backEndUrl, String sessionCookie, String proxyServiceName)
            throws ProxyServiceAdminProxyAdminException, RemoteException {
        ProxyServiceAdminClient proxyAdmin = new ProxyServiceAdminClient(backEndUrl, sessionCookie);
        proxyAdmin.deleteProxy(proxyServiceName);
        assertTrue(isProxyUnDeployed(backEndUrl, sessionCookie, proxyServiceName),
                   "Proxy service undeployment failed");
    }

    public void deleteLocalEntry(String backEndUrl, String sessionCookie, String localEntryName)
            throws LocalEntryAdminException, RemoteException {
        LocalEntriesAdminClient localEntryAdminServiceClient =
                new LocalEntriesAdminClient(backEndUrl,
                                            sessionCookie);
        assertTrue(localEntryAdminServiceClient.deleteLocalEntry(localEntryName),
                   "LocalEntry Deletion failed");
        assertTrue(isLocalEntryUnDeployed(backEndUrl, sessionCookie, localEntryName),
                   "LocalEntry undeployment failed");

    }

    public void deleteEndpoint(String backEndUrl, String sessionCookie, String endpointName)
            throws EndpointAdminEndpointAdminException, RemoteException {
        EndPointAdminClient endPointAdminClient = new EndPointAdminClient(backEndUrl,
                                                                          sessionCookie);
        assertTrue(endPointAdminClient.deleteEndpoint(endpointName),
                   "Endpoint deletion failed");
        assertTrue(isEndpointUnDeployed(backEndUrl, sessionCookie, endpointName),
                   "Endpoint undeployment failed");
    }

    public void deleteSequence(String backEndUrl, String sessionCookie, String sequenceName)
            throws SequenceEditorException, RemoteException {
        SequenceAdminServiceClient sequenceAdminServiceClient =
                new SequenceAdminServiceClient(backEndUrl,
                                               sessionCookie);
        sequenceAdminServiceClient.deleteSequence(sequenceName);
        assertTrue(isSequenceUnDeployed(backEndUrl, sessionCookie, sequenceName),
                   "Sequence undeployment failed");
    }

    public void deleteMessageStore(String backEndUrl, String sessionCookie, String messageStore)
            throws RemoteException, SequenceEditorException {
        MessageStoreAdminClient messageStoreAdminClient =
                new MessageStoreAdminClient(backEndUrl, sessionCookie);
        messageStoreAdminClient.deleteMessageStore(messageStore);
        assertTrue(isMessageStoreUnDeployed(backEndUrl, sessionCookie, messageStore),
                   "Message Store undeployment failed");
    }

    public void deleteMessageProcessor(String backEndUrl, String sessionCookie,
                                       String messageProcessor)
            throws RemoteException, SequenceEditorException {
        MessageProcessorClient messageProcessorClient =
                new MessageProcessorClient(backEndUrl, sessionCookie);
        messageProcessorClient.deleteMessageProcessor(messageProcessor);
        assertTrue(isMessageProcessorUnDeployed(backEndUrl, sessionCookie, messageProcessor),
                   "Message Processor undeployment failed");
    }

    public void deleteEndpointTemplate(String backEndUrl, String sessionCookie,
                                       String endpointTemplate)
            throws RemoteException, SequenceEditorException, EndpointAdminEndpointAdminException {

        EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
                new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
        endpointTemplateAdminServiceClient.deleteEndpointTemplate(endpointTemplate);
        assertTrue(isEndpointTemplateUnDeployed(backEndUrl, sessionCookie, endpointTemplate),
                   "Endpoint Template undeployment failed");
    }

    public void deleteSequenceTemplate(String backEndUrl, String sessionCookie,
                                       String sequenceTemplateName)
            throws RemoteException, SequenceEditorException, EndpointAdminEndpointAdminException {
        SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
                new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
        sequenceTemplateAdminServiceClient.deleteTemplate(sequenceTemplateName);
        assertTrue(
                isSequenceTemplateUnDeployed(backEndUrl, sessionCookie, sequenceTemplateName),
                "Sequence Template undeployment failed");
    }

    public void deleteApi(String backEndUrl, String sessionCookie, String apiName)
            throws RemoteException, RestApiAdminAPIException {
        RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
        apiAdminClient.deleteApi(apiName);
        assertTrue(isApiUnDeployed(backEndUrl, sessionCookie, apiName),
                   "API undeployment failed");
    }

    public void deletePriorityExecutor(String backEndUrl, String sessionCookie, String executorName)
            throws RemoteException {
        PriorityMediationAdminClient priorityMediationAdminClient =
                new PriorityMediationAdminClient(backEndUrl, sessionCookie);
        priorityMediationAdminClient.remove(executorName);
        assertTrue(isPriorityExecutorUnDeployed(backEndUrl, sessionCookie, executorName),
                   "Priority Executor undeployment failed");
    }

    public void deleteScheduleTask(String backEndUrl, String sessionCookie, String taskName,
                                   String group)
            throws TaskManagementException, RemoteException {
        TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
        taskAdminClient.deleteTask(taskName, group);
        assertTrue(isScheduleTaskUnDeployed(backEndUrl, sessionCookie, taskName),
                   "ScheduleTask deployment failed");

    }

    public boolean isProxyUnDeployed(String backEndUrl, String sessionCookie, String proxyName)
            throws RemoteException {
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Proxy undeployment");
        ServiceAdminClient adminServiceService = new ServiceAdminClient(backEndUrl, sessionCookie);
        boolean isServiceDeleted = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            if (!adminServiceService.isServiceExists(proxyName)) {
                isServiceDeleted = true;
                log.info(proxyName + " Proxy undeployed in " + time + " millis");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }
        return isServiceDeleted;
    }

    public boolean isMessageStoreUnDeployed(String backEndUrl, String sessionCookie,
                                            String messageStoreName)
            throws SequenceEditorException, RemoteException {
        MessageStoreAdminClient messageStoreAdminClient =
                new MessageStoreAdminClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Undeployment Message Store " +
                 messageStoreName);
        boolean isMessageStoreDeleted = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] mStores = messageStoreAdminClient.getMessageStores();
            if (!ArrayUtils.contains(mStores, messageStoreName)) {
                isMessageStoreDeleted = true;
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isMessageStoreDeleted;
    }

    public boolean isMessageProcessorUnDeployed(String backEndUrl, String sessionCookie,
                                                String messageProcessorName)
            throws SequenceEditorException, RemoteException {
        MessageProcessorClient messageProcessorClient =
                new MessageProcessorClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY +
                 " millis for Undeployment Message Processor " + messageProcessorName);
        boolean isMessageProcessorDeleted = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] mProcessors = messageProcessorClient.getMessageProcessorNames();
            if (!ArrayUtils.contains(mProcessors, messageProcessorName)) {
                isMessageProcessorDeleted = true;
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isMessageProcessorDeleted;
    }

    public boolean isLocalEntryDeployed(String backEndUrl, String sessionCookie,
                                        String localEntryName)
            throws LocalEntryAdminException, RemoteException {
        LocalEntriesAdminClient localEntryAdminServiceClient =
                new LocalEntriesAdminClient(backEndUrl,
                                            sessionCookie);
        log.info(
                "waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for LocalEntry " + localEntryName);
        boolean isLocalEntryExist = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] localEntries = localEntryAdminServiceClient.getEntryNames();
            if (localEntries != null && localEntries.length > 0) {
                for (String localEntry : localEntries) {

                    if (localEntryName.equals(localEntry)) {
                        isLocalEntryExist = true;
                        log.info(localEntryName + " LocalEntry Found in " + time + " millis");
                        break;
                    }
                }
            }
            if (isLocalEntryExist) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isLocalEntryExist;
    }

    public boolean isLocalEntryUnDeployed(String backEndUrl, String sessionCookie,
                                          String localEntryName)
            throws LocalEntryAdminException, RemoteException {
        LocalEntriesAdminClient localEntryAdminServiceClient =
                new LocalEntriesAdminClient(backEndUrl,
                                            sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Undeployment LocalEntry " +
                 localEntryName);
        boolean isLocalEntryUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] localEntries = localEntryAdminServiceClient.getEntryNames();
            if (!ArrayUtils.contains(localEntries, localEntryName)) {
                isLocalEntryUnDeployed = true;
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isLocalEntryUnDeployed;
    }

    public boolean isSequenceUnDeployed(String backEndUrl, String sessionCookie,
                                        String sequenceName)
            throws SequenceEditorException, RemoteException {
        SequenceAdminServiceClient sequenceAdminServiceClient =
                new SequenceAdminServiceClient(backEndUrl,
                                               sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Undeployment Sequence " +
                 sequenceName);
        boolean isSequenceUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] sequences = sequenceAdminServiceClient.getSequences();
            if (!ArrayUtils.contains(sequences, sequenceName)) {
                isSequenceUnDeployed = true;
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isSequenceUnDeployed;
    }

    public boolean isEndpointUnDeployed(String backEndUrl, String sessionCookie,
                                        String endpointName)
            throws EndpointAdminEndpointAdminException, RemoteException {
        EndPointAdminClient endPointAdminClient = new EndPointAdminClient(backEndUrl,
                                                                          sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Undeployment Endpoint " +
                 endpointName);
        boolean isEndpointUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] endpoints = endPointAdminClient.getEndpointNames();
            if (!ArrayUtils.contains(endpoints, endpointName)) {
                isEndpointUnDeployed = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isEndpointUnDeployed;
    }

    public boolean isEndpointTemplateUnDeployed(String backEndUrl, String sessionCookie,
                                                String endpointTemplateName)
            throws EndpointAdminEndpointAdminException, RemoteException {
        EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
                new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY +
                 " millis for Undeployment Endpoint Template " + endpointTemplateName);
        boolean isEndpointTemplateUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] endpointTemplates = endpointTemplateAdminServiceClient.getEndpointTemplates();
            if (!ArrayUtils.contains(endpointTemplates, endpointTemplateName)) {
                isEndpointTemplateUnDeployed = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isEndpointTemplateUnDeployed;
    }

    public boolean isSequenceTemplateUnDeployed(String backEndUrl, String sessionCookie,
                                                String sequenceTemplateName)
            throws EndpointAdminEndpointAdminException, RemoteException {
        SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
                new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY +
                 " millis for Undeployment Sequence Template " + sequenceTemplateName);
        boolean isSequenceTemplateUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] sequenceTemplates = sequenceTemplateAdminServiceClient.getSequenceTemplates();
            if (!ArrayUtils.contains(sequenceTemplates, sequenceTemplateName)) {
                isSequenceTemplateUnDeployed = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isSequenceTemplateUnDeployed;
    }

    public boolean isApiUnDeployed(String backEndUrl, String sessionCookie,
                                   String apiName)
            throws RemoteException, RestApiAdminAPIException {
        RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Undeployment API " + apiName);
        boolean isApiUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] apiList = apiAdminClient.getApiNames();
            if (!ArrayUtils.contains(apiList, apiName)) {
                isApiUnDeployed = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isApiUnDeployed;
    }

    public boolean isPriorityExecutorUnDeployed(String backEndUrl, String sessionCookie,
                                                String executorName)
            throws RemoteException {
        PriorityMediationAdminClient priorityMediationAdminClient =
                new PriorityMediationAdminClient(backEndUrl, sessionCookie);
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY +
                 " millis for Undeployment Priority Executor " + executorName);
        boolean isExecutorUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            String[] executorList = priorityMediationAdminClient.getExecutorList();
            if (!ArrayUtils.contains(executorList, executorName)) {
                isExecutorUnDeployed = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        return isExecutorUnDeployed;
    }

    public boolean isScheduleTaskUnDeployed(String backEndUrl, String sessionCookie,
                                            String taskName)
            throws RemoteException, TaskManagementException {
        log.info("waiting " + SERVICE_DEPLOYMENT_DELAY + " millis for Task Undeployment " +
                 taskName);

        boolean isTaskUnDeployed = false;
        TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
               SERVICE_DEPLOYMENT_DELAY) {
            if (!taskAdminClient.getScheduleTaskList().contains(taskName)) {
                isTaskUnDeployed = true;
                log.info(taskName + " Task UnDeployed in " + time + " millis");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
        }

        return isTaskUnDeployed;
    }

    public void verifySynapseDeployment(OMElement synapseConfig, String backendURL,
                                        String sessionCookie)
            throws LocalEntryAdminException, RemoteException, EndpointAdminEndpointAdminException,
                   SequenceEditorException, RestApiAdminAPIException {
        Iterator localEntries = synapseConfig.getChildrenWithLocalName(LOCAL_ENTRY);
        while (localEntries.hasNext()) {
            OMElement leOM = (OMElement) localEntries.next();
            String le = leOM.getAttributeValue(new QName(KEY));
            assertTrue(isLocalEntryDeployed(backendURL, sessionCookie, le),
                       le + " LocalEntry deployment not found or time out");
        }

        Iterator endpoints = synapseConfig.getChildrenWithLocalName(ENDPOINT);
        while (endpoints.hasNext()) {
            OMElement epOM = (OMElement) endpoints.next();
            String ep = epOM.getAttributeValue(new QName(NAME));
            assertTrue(isEndpointDeployed(backendURL, sessionCookie, ep),
                       ep + " Endpoint deployment not found or time out");
        }

        Iterator sequences = synapseConfig.getChildrenWithLocalName(SEQUENCE);
        while (sequences.hasNext()) {
            OMElement sqnOM = (OMElement) sequences.next();
            String sqn = sqnOM.getAttributeValue(new QName(NAME));
            assertTrue(isSequenceDeployed(backendURL, sessionCookie, sqn),
                       sqn + " Sequence deployment not found or time out");
        }

        Iterator proxies = synapseConfig.getChildrenWithLocalName(PROXY);
        while (proxies.hasNext()) {
            OMElement proxyOM = (OMElement) proxies.next();
            String proxy = proxyOM.getAttributeValue(new QName(NAME));
            assertTrue(isProxyDeployed(backendURL, sessionCookie, proxy),
                       proxy + " Proxy Deployment not found or time out");
        }

        Iterator messageStores = synapseConfig.getChildrenWithLocalName(MESSAGE_STORE);
        while (messageStores.hasNext()) {
            OMElement mStoreOM = (OMElement) messageStores.next();
            String mStore = mStoreOM.getAttributeValue(new QName(NAME));
            assertTrue(isMessageStoreDeployed(backendURL, sessionCookie, mStore),
                       mStore + " Message Store Deployment not found or time out");
        }

        Iterator messageProcessor =
                synapseConfig.getChildrenWithLocalName(MESSAGE_PROCESSOR);
        while (messageProcessor.hasNext()) {
            OMElement mProcessorOM = (OMElement) messageProcessor.next();
            String mProcessor = mProcessorOM.getAttributeValue(new QName(NAME));
            assertTrue(isMessageProcessorDeployed(backendURL, sessionCookie, mProcessor),
                       mProcessor + " Message Processor Deployment not found or time out");
        }

        Iterator templates = synapseConfig.getChildrenWithLocalName(TEMPLATE);

        while (templates.hasNext()) {
            OMElement template = (OMElement) templates.next();
            String templateName = template.getAttributeValue(new QName(NAME));

            if (template.getFirstChildWithName(new QName(template.getNamespace().getNamespaceURI(),
                                                         SEQUENCE)) != null) {
                assertTrue(isSequenceTemplateDeployed(backendURL, sessionCookie, templateName),
                           templateName + " Sequence Template Deployment not found or time out");
            } else {

                assertTrue(isEndpointTemplateDeployed(backendURL, sessionCookie, templateName),
                           templateName + " Endpoint Template Deployment not found or time out");
            }
            log.info("Template Uploaded");
        }

        Iterator apiList = synapseConfig.getChildrenWithLocalName(API);
        while (apiList.hasNext()) {
            OMElement api = (OMElement) apiList.next();
            String apiName = api.getAttributeValue(new QName(NAME));
            String version = api.getAttributeValue(new QName(VERSION));
            if (version != null && !version.equals("")) {
                apiName = apiName + ":v" + version;
            }
            assertTrue(isApiDeployed(backendURL, sessionCookie, apiName),
                       apiName + " API Deployment not found or time out");
        }

        Iterator executorList =
                synapseConfig.getChildrenWithLocalName(PRIORITY_EXECUTOR);
        while (executorList.hasNext()) {
            OMElement executorNameOM = (OMElement) executorList.next();
            String executorName = executorNameOM.getAttributeValue(new QName(NAME));
            assertTrue(isPriorityExecutorDeployed(backendURL, sessionCookie, executorName),
                       executorName + " Priority Executor Deployment not found or time out");
        }
    }

    /**
     * create API bean object from response
     *
     * @param httpResponse - HTTP response
     * @return - APIBean instance
     */
    public static APIBean getAPIBeanFromHttpResponse(HttpResponse httpResponse)
            throws APIManagerIntegrationTestException {
        JSONObject jsonObject;
        String APIName;
        String APIProvider;
        String APIVersion;
        APIBean apiBean;
        try {
            jsonObject = new JSONObject(httpResponse.getData());

            APIName = ((JSONObject) jsonObject.get("api")).getString("name");
            APIVersion = ((JSONObject) jsonObject.get("api")).getString("version");
            APIProvider = ((JSONObject) jsonObject.get("api")).getString("provider");

            APIIdentifier identifier = new APIIdentifier(APIProvider, APIName, APIVersion);

            apiBean = new APIBean(identifier);
            apiBean.setContext(((JSONObject) jsonObject.get("api")).getString("context"));
            apiBean.setDescription(((JSONObject) jsonObject.get("api")).getString("description"));
            apiBean.setWsdlUrl(((JSONObject) jsonObject.get("api")).getString("wsdl"));
            apiBean.setTags(((JSONObject) jsonObject.get("api")).getString("tags"));
            apiBean.setAvailableTiers(((JSONObject) jsonObject.get("api")).getString("availableTiers"));

            apiBean.setThumbnailUrl(((JSONObject) jsonObject.get("api")).getString("thumb"));
            apiBean.setSandboxUrl(((JSONObject) jsonObject.get("api")).getString("sandbox"));
            apiBean.setBusinessOwner(((JSONObject) jsonObject.get("api")).getString("bizOwner"));
            apiBean.setBusinessOwnerEmail(((JSONObject) jsonObject.get("api")).getString("bizOwnerMail"));
            apiBean.setTechnicalOwner(((JSONObject) jsonObject.get("api")).getString("techOwner"));
            apiBean.setTechnicalOwnerEmail(((JSONObject) jsonObject.get("api")).getString("techOwnerMail"));

            apiBean.setWadlUrl(((JSONObject) jsonObject.get("api")).getString("wadl"));
            apiBean.setVisibility(((JSONObject) jsonObject.get("api")).getString("visibility"));
            apiBean.setVisibleRoles(((JSONObject) jsonObject.get("api")).getString("roles"));
            apiBean.setEndpointUTUsername(((JSONObject) jsonObject.get("api")).getString("epUsername"));
            apiBean.setEndpointUTPassword(((JSONObject) jsonObject.get("api")).getString("epPassword"));

            apiBean.setEndpointSecured((Boolean.getBoolean(((JSONObject) jsonObject.get("api")).getString("endpointTypeSecured"))));
            apiBean.setTransports(((JSONObject) jsonObject.get("api")).getString("transport_http"));
            apiBean.setTransports(((JSONObject) jsonObject.get("api")).getString("transport_https"));
            apiBean.setInSequence(((JSONObject) jsonObject.get("api")).getString("inSequence"));
            apiBean.setOutSequence(((JSONObject) jsonObject.get("api")).getString("outSequence"));
            apiBean.setAvailableTiers(((JSONObject) jsonObject.get("api")).getString("availableTiersDisplayNames"));


            //-----------Here are some of unused properties, if we need to use them add params to APIBean class
            //((JSONObject) jsonObject.get("api")).getString("name");
            //((JSONObject) jsonObject.get("api")).getString("endpoint");
            //((JSONObject) jsonObject.get("api")).getString("subscriptionAvailability");
            //((JSONObject) jsonObject.get("api")).getString("subscriptionTenants");
            //((JSONObject) jsonObject.get("api")).getString("endpointConfig");
            //((JSONObject) jsonObject.get("api")).getString("responseCache");
            //(((JSONObject) jsonObject.get("api")).getString("cacheTimeout");
            //((JSONObject) jsonObject.get("api")).getString("endpointConfig");
            //((JSONObject) jsonObject.get("api")).getString("version");
            //((JSONObject) jsonObject.get("api")).getString("apiStores");
            // ((JSONObject) jsonObject.get("api")).getString("provider");
            //)((JSONObject) jsonObject.get("api")).getString("tierDescs");
            //((JSONObject) jsonObject.get("api")).getString("subs");
            //((JSONObject) jsonObject.get("api")).getString("context");
            // apiBean.setLastUpdated(Date.parse((JSONObject); jsonObject.get("api")).getString("lastUpdated")));
            // apiBean.setUriTemplates((JSONObject) jsonObject.get("api")).getString("templates"));
        } catch (JSONException e) {
            throw new APIManagerIntegrationTestException("Generating APIBen instance fails ", e);
        }
        return apiBean;
    }

    public static void sendGetRequest(String url, String accessToken)
            throws XPathExpressionException, IOException {
        HttpResponse httpResponse;

        URL urlAPI = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setReadTimeout(10000);
        //setting headers

        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        conn.connect();
        // Get the response
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            httpResponse = new HttpResponse(sb.toString(), conn.getResponseCode());
            httpResponse.setResponseMessage(conn.getResponseMessage());
        } catch (IOException ignored) {

        } finally {
            if (rd != null) {
                rd.close();
            }
        }
    }

    public static String getDecodedJWT(String serverMessage) {
        // result comes as header values
        String[] headerArray = serverMessage.split("\n");
        //tokenize  from JWT assertion header
        String[] jwtEncodedArray = headerArray[1].trim().split(":");
        //take first part
        String[] jwtTokenArray = jwtEncodedArray[1].split(Pattern.quote("."));
        // decode  JWT part
        byte[] jwtByteArray = Base64.decodeBase64(jwtTokenArray[1].getBytes());
        return new String(jwtByteArray);
    }

    /**
     * Get the API information from the response  object as Lost of  APIIdentifier
     *
     * @param httpResponse Response that contains the API information
     * @return List of APIIdentifier
     * @throws JSONException
     */
    public static List<APIIdentifier> getAPIIdentifierListFromHttpResponse(
            HttpResponse httpResponse) throws JSONException {
        List<APIIdentifier> apiIdentifierList = new ArrayList<APIIdentifier>();
        String APIName;
        String APIProvider;
        String APIVersion;

        try {
            JSONObject jsonRootObject = new JSONObject(httpResponse.getData());

            if (jsonRootObject.has("apis")) {
                JSONArray jsonArray = jsonRootObject.getJSONArray("apis");
                for (int index = 0; index < jsonArray.length(); index++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(index);
                    APIName = jsonObject.getString("name");
                    APIVersion = jsonObject.getString("version");
                    APIProvider = jsonObject.getString("provider");
                    apiIdentifierList.add(new APIIdentifier(APIProvider, APIName, APIVersion));
                }
            } else if (jsonRootObject.has("api")) {
                APIName = jsonRootObject.getJSONObject("api").getString("name");
                APIVersion = jsonRootObject.getJSONObject("api").getString("version");
                APIProvider = jsonRootObject.getJSONObject("api").getString("provider");
                apiIdentifierList.add(new APIIdentifier(APIProvider, APIName, APIVersion));
            }

        } catch (JSONException e) {
            log.error("Error when extraction data from JSON" + e.getMessage());
            throw new RuntimeException("Error when extraction data from JSON", e);
        }
        return apiIdentifierList;
    }

    /**
     * Check  the given API is available in the APIIdentifier List. it will match for API Name,API Version and API Provider
     *
     * @param apiIdentifierToCheck - API identifier to verify
     * @param apiIdentifierList    - API identifier list
     * @return - Status of API availability
     */
    public static boolean isAPIAvailable(APIIdentifier apiIdentifierToCheck,
                                         List<APIIdentifier> apiIdentifierList) {
        boolean isFound = false;
        for (APIIdentifier apiIdentifier : apiIdentifierList) {
            if (apiIdentifier.getApiName().equals(apiIdentifierToCheck.getApiName()) &&
                apiIdentifier.getVersion().equals(apiIdentifierToCheck.getVersion()) &&
                apiIdentifier.getProviderName().equals(apiIdentifierToCheck.getProviderName())) {
                isFound = true;
                break;
            }
        }
        return isFound;
    }
}
