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

package org.wso2.am.integration.test.utils.esb;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ESBTestCaseUtils {

	protected Log log = LogFactory.getLog(getClass());
	private static int SERVICE_DEPLOYMENT_DELAY =
			TestConfigurationProvider.getServiceDeploymentDelay();
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
	public OMElement loadResource(String path) throws FileNotFoundException,
	                                                  XMLStreamException {
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
	 * Loads the specified ESB configuration file from the classpath and deploys it into the ESB.
	 *
	 * @param synapseFilePath A relative path to the configuration file
	 * @throws java.rmi.RemoteException If an error occurs while loading the specified configuration
	 */
	public void loadESBConfigurationFrom(String synapseFilePath, String backendURL,
	                                     String sessionCookie)
			throws Exception {
		OMElement configElement = loadResource(synapseFilePath);
		updateESBConfiguration(configElement, backendURL, sessionCookie);
	}

	/**
	 * Loads the configuration of the specified sample into the ESB.
	 *
	 * @param number Sample number
	 * @throws Exception If an error occurs while loading the sample configuration
	 */
	public OMElement loadESBSampleConfiguration(int number)
			throws Exception {
		String filePath = TestConfigurationProvider.getResourceLocation("ESB") +
		                  File.separator + "samples" + File.separator + "synapse_sample_" + number +
		                  ".xml";
		File configFile = new File(filePath);
		FileInputStream inputStream = null;
		XMLStreamReader parser = null;
		StAXOMBuilder builder = null;
		OMElement documentElement = null;
		try {
			inputStream = new FileInputStream(configFile.getAbsolutePath());
			parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
			builder = new StAXOMBuilder(parser);
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
		return documentElement;
	}

	/**
	 * load synapse configuration from OMElement
	 *
	 * @param synapseConfig synapse configuration
	 * @param backendURL    server backEnd url
	 * @param sessionCookie session Cookie
	 * @throws java.rmi.RemoteException
	 * @throws javax.xml.stream.XMLStreamException
	 * @throws javax.servlet.ServletException
	 */
	public void updateESBConfiguration(OMElement synapseConfig, String backendURL,
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

		Iterator<OMElement> localEntries = synapseConfig.getChildrenWithLocalName(LOCAL_ENTRY);
		while (localEntries.hasNext()) {
			OMElement localEntry = localEntries.next();
			String le = localEntry.getAttributeValue(new QName(KEY));
			if (ArrayUtils.contains(localEntryAdminServiceClient.getEntryNames(), le)) {
				Assert.assertTrue(localEntryAdminServiceClient.deleteLocalEntry(le),
				                  le + " Local Entry deletion failed");
				Assert.assertTrue(isLocalEntryUnDeployed(backendURL, sessionCookie, le),
				                  le + " Local Entry undeployment failed");
			}
			Assert.assertTrue(localEntryAdminServiceClient.addLocalEntry(localEntry),
			                  le + " Local Entry addition failed");
			log.info(le + " LocalEntry Uploaded");
		}

		Iterator<OMElement> endpoints = synapseConfig.getChildrenWithLocalName(ENDPOINT);
		while (endpoints.hasNext()) {
			OMElement endpoint = endpoints.next();
			String ep = endpoint.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(endPointAdminClient.getEndpointNames(), ep)) {
				Assert.assertTrue(endPointAdminClient.deleteEndpoint(ep),
				                  ep + " Endpoint deletion failed");
				Assert.assertTrue(isEndpointUnDeployed(backendURL, sessionCookie, ep),
				                  ep + " Endpoint undeployment failed");
			}
			Assert.assertTrue(endPointAdminClient.addEndPoint(endpoint),
			                  ep + " Endpoint addition failed");
			log.info(ep + " Endpoint Uploaded");
		}

		Iterator<OMElement> sequences = synapseConfig.getChildrenWithLocalName(SEQUENCE);
		while (sequences.hasNext()) {
			OMElement sequence = sequences.next();
			String sqn = sequence.getAttributeValue(new QName(NAME));
			boolean isSequenceExist = ArrayUtils.contains(sequenceAdminClient.getSequences(), sqn);
			if (("main".equalsIgnoreCase(sqn) || "fault".equalsIgnoreCase(sqn)) &&
			    isSequenceExist) {
				sequenceAdminClient.updateSequence(sequence);
			} else {
				if (isSequenceExist) {
					sequenceAdminClient.deleteSequence(sqn);
					Assert.assertTrue(isSequenceUnDeployed(backendURL, sessionCookie, sqn),
					                  sqn + " Sequence undeployment failed");
				}
				sequenceAdminClient.addSequence(sequence);
			}
			log.info(sqn + " Sequence Uploaded");
		}

		Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName(PROXY);
		while (proxies.hasNext()) {
			OMElement proxy = proxies.next();
			String proxyName = proxy.getAttributeValue(new QName(NAME));
			if (adminServiceService.isServiceExists(proxyName)) {
				proxyAdmin.deleteProxy(proxyName);
				Assert.assertTrue(isProxyUnDeployed(backendURL, sessionCookie, proxyName),
				                  proxyName + " Undeployment failed");
			}
			proxyAdmin.addProxyService(proxy);
			log.info(proxyName + " Proxy Uploaded");
		}

		Iterator<OMElement> messageStores = synapseConfig.getChildrenWithLocalName(MESSAGE_STORE);
		while (messageStores.hasNext()) {
			OMElement messageStore = messageStores.next();
			String mStore = messageStore.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(messageStoreAdminClient.getMessageStores(), mStore)) {
				messageStoreAdminClient.deleteMessageStore(mStore);
				Assert.assertTrue(isMessageStoreUnDeployed(backendURL, sessionCookie, mStore),
				                  mStore + " Message Store undeployment failed");
			}
			messageStoreAdminClient.addMessageStore(messageStore);
			log.info(mStore + " Message Store Uploaded");
		}

		Iterator<OMElement> messageProcessors =
				synapseConfig.getChildrenWithLocalName(MESSAGE_PROCESSOR);
		while (messageProcessors.hasNext()) {
			OMElement messageProcessor = messageProcessors.next();
			String mProcessor = messageProcessor.getAttributeValue(new QName(NAME));
			if (ArrayUtils
					.contains(messageProcessorClient.getMessageProcessorNames(), mProcessor)) {
				messageProcessorClient.deleteMessageProcessor(mProcessor);
				Assert.assertTrue(
						isMessageProcessorUnDeployed(backendURL, sessionCookie, mProcessor)
						, mProcessor + " Message Processor undeployment failed");
			}
			messageProcessorClient.addMessageProcessor(messageProcessor);
			log.info(mProcessor + " Message Processor Uploaded");
		}

		Iterator<OMElement> templates = synapseConfig.getChildrenWithLocalName(TEMPLATE);
		while (templates.hasNext()) {
			OMElement template = templates.next();
			String templateName = template.getAttributeValue(new QName(NAME));
			if (template.getFirstChildWithName(
					new QName(template.getNamespace().getNamespaceURI(), SEQUENCE)) != null) {
				if (ArrayUtils.contains(sequenceTemplateAdminServiceClient.getSequenceTemplates(),
				                        templateName)) {
					sequenceTemplateAdminServiceClient.deleteTemplate(templateName);
					Assert.assertTrue(
							isSequenceTemplateUnDeployed(backendURL, sessionCookie, templateName)
							, templateName + " Sequence Template undeployment failed");
				}
				sequenceTemplateAdminServiceClient.addSequenceTemplate(template);

			} else {

				if (ArrayUtils.contains(endpointTemplateAdminServiceClient.getEndpointTemplates(),
				                        templateName)) {
					endpointTemplateAdminServiceClient.deleteEndpointTemplate(templateName);
					Assert.assertTrue(
							isEndpointTemplateUnDeployed(backendURL, sessionCookie, templateName)
							, templateName + " Endpoint Template undeployment failed");
				}
				endpointTemplateAdminServiceClient.addEndpointTemplate(template);
			}
			log.info(templateName + " Template Uploaded");
		}

		Iterator<OMElement> apiElements = synapseConfig.getChildrenWithLocalName(API);
		while (apiElements.hasNext()) {
			OMElement api = apiElements.next();
			String apiName = api.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(apiAdminClient.getApiNames(), apiName)) {
				apiAdminClient.deleteApi(apiName);
				Assert.assertTrue(isApiUnDeployed(backendURL, sessionCookie, apiName)
						, apiName + " Api undeployment failed");
			}
			apiAdminClient.add(api);
			log.info(apiName + " API Uploaded");
		}

		Iterator<OMElement> priorityExecutorList =
				synapseConfig.getChildrenWithLocalName(PRIORITY_EXECUTOR);
		while (priorityExecutorList.hasNext()) {
			OMElement executor = priorityExecutorList.next();
			String executorName = executor.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(priorityMediationAdminClient.getExecutorList(), executorName)) {
				priorityMediationAdminClient.remove(executorName);
				Assert.assertTrue(
						isPriorityExecutorUnDeployed(backendURL, sessionCookie, executorName)
						, executorName + " Priority Executor undeployment failed");
			}
			priorityMediationAdminClient.addPriorityMediator(executorName, executor);
			log.info(executorName + " Priority Executor Uploaded");
		}

		Thread.sleep(1000);
		verifySynapseDeployment(synapseConfig, backendURL, sessionCookie);
		log.info("Synapse configuration  Deployed");

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param proxyConfig
	 * @throws javax.xml.stream.XMLStreamException
	 * @throws java.io.IOException
	 * @throws org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException
	 * @throws InterruptedException
	 */
	public void addProxyService(String backEndUrl, String sessionCookie, OMElement proxyConfig)
			throws Exception {
		ProxyServiceAdminClient proxyAdmin = new ProxyServiceAdminClient(backEndUrl, sessionCookie);
		proxyAdmin.addProxyService(proxyConfig);
		String proxyName = proxyConfig.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isProxyDeployed(backEndUrl, sessionCookie, proxyName),
		                  "Proxy Deployment failed or time out");

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param endpointConfig
	 * @throws Exception
	 */
	public void addEndpoint(String backEndUrl, String sessionCookie, OMElement endpointConfig)
			throws Exception {
		EndPointAdminClient endPointAdminClient =
				new EndPointAdminClient(backEndUrl, sessionCookie);
		endPointAdminClient.addEndPoint(endpointConfig);
		String ep = endpointConfig.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isEndpointDeployed(backEndUrl, sessionCookie, ep),
		                  ep + "Endpoint deployment not found or time out");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param localEntryConfig
	 * @return
	 * @throws Exception
	 */
	public void addLocalEntry(String backEndUrl, String sessionCookie,
	                          OMElement localEntryConfig) throws Exception {
		LocalEntriesAdminClient localEntryAdminServiceClient =
				new LocalEntriesAdminClient(backEndUrl,
				                            sessionCookie);
		boolean value = localEntryAdminServiceClient.addLocalEntry(localEntryConfig);
		Assert.assertTrue(value, "LocalEntry Addition failed");
		if (value) {
			String le = localEntryConfig.getAttributeValue(new QName(KEY));
			Assert.assertTrue(isLocalEntryDeployed(backEndUrl, sessionCookie, le),
			                  le + "LocalEntry deployment not found or time out");
		}

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param sequenceConfig
	 * @throws Exception
	 */
	public void addSequence(String backEndUrl, String sessionCookie, OMElement sequenceConfig)
			throws Exception {
		SequenceAdminServiceClient sequenceAdminClient = new SequenceAdminServiceClient(backEndUrl,
		                                                                                sessionCookie);
		sequenceAdminClient.addSequence(sequenceConfig);
		String sqn = sequenceConfig.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isSequenceDeployed(backEndUrl, sessionCookie, sqn),
		                  sqn + "Sequence deployment not found or time out");

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageStore
	 * @throws java.rmi.RemoteException
	 */
	public void addMessageStore(String backEndUrl, String sessionCookie, OMElement messageStore)
			throws Exception {
		MessageStoreAdminClient messageStoreAdminClient =
				new MessageStoreAdminClient(backEndUrl, sessionCookie);
		messageStoreAdminClient.addMessageStore(messageStore);
		String mStoreName = messageStore.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isMessageStoreDeployed(backEndUrl, sessionCookie, mStoreName),
		                  "Message Store Deployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageProcessor
	 * @throws java.rmi.RemoteException
	 */
	public void addMessageProcessor(String backEndUrl, String sessionCookie,
	                                OMElement messageProcessor)
			throws Exception {
		MessageProcessorClient messageProcessorClient =
				new MessageProcessorClient(backEndUrl, sessionCookie);
		messageProcessorClient.addMessageProcessor(messageProcessor);
		String mProcessorName = messageProcessor.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isMessageProcessorDeployed(backEndUrl, sessionCookie, mProcessorName),
		                  "Message Processor deployment failed");
	}

	public void addSequenceTemplate(String backEndUrl, String sessionCookie,
	                                OMElement sequenceTemplate) throws RemoteException {
		SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
				new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
		sequenceTemplateAdminServiceClient.addSequenceTemplate(sequenceTemplate);
		String seqTmpName = sequenceTemplate.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isSequenceTemplateDeployed(backEndUrl, sessionCookie, seqTmpName),
		                  "Sequence Template deployment failed");

	}

	public void addEndpointTemplate(String backEndUrl, String sessionCookie,
	                                OMElement endpointTemplate) throws RemoteException {
		EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
				new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
		endpointTemplateAdminServiceClient.addEndpointTemplate(endpointTemplate);
		String endpointTmpName = endpointTemplate.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isEndpointTemplateDeployed(backEndUrl, sessionCookie, endpointTmpName),
		                  "Endpoint Template deployment failed");

	}

	public void addAPI(String backEndUrl, String sessionCookie,
	                   OMElement api) throws RemoteException, RestApiAdminAPIException {
		RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
		apiAdminClient.add(api);
		String apiName = api.getAttributeValue(new QName(NAME));
		Assert.assertTrue(isApiDeployed(backEndUrl, sessionCookie, apiName),
		                  "Rest Api deployment failed");
	}

	public void addPriorityExecutor(String backEndUrl, String sessionCookie,
	                                OMElement priorityExecutor)
			throws RemoteException {
		PriorityMediationAdminClient priorityMediationAdminClient =
				new PriorityMediationAdminClient(backEndUrl, sessionCookie);
		String executorName = priorityExecutor.getAttributeValue(new QName(NAME));
		priorityMediationAdminClient.addPriorityMediator(executorName, priorityExecutor);
		Assert.assertTrue(isPriorityExecutorDeployed(backEndUrl, sessionCookie, executorName),
		                  "Priority Executor deployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param taskDescription
	 * @throws org.wso2.carbon.task.stub.TaskManagementException
	 * @throws java.rmi.RemoteException
	 */
	public void addScheduleTask(String backEndUrl, String sessionCookie, OMElement taskDescription)
			throws TaskManagementException, RemoteException {
		TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
		taskAdminClient.addTask(taskDescription);
		Assert.assertTrue(isScheduleTaskDeployed(backEndUrl, sessionCookie
				                  , taskDescription.getAttributeValue(new QName("name"))),
		                  "ScheduleTask deployment failed"
		);
	}

	/**
	 * Waiting for proxy to deploy
	 *
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param proxyName
	 * @return
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param endpointName
	 * @return
	 * @throws org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageProcessorName
	 * @return
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param sequenceName
	 * @return
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageStoreName
	 * @return
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * Wait for task to to deploy and return true once it deploy
	 *
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param taskName
	 * @return
	 * @throws java.rmi.RemoteException
	 * @throws org.wso2.carbon.task.stub.TaskManagementException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param proxyName
	 * @return
	 * @throws java.rmi.RemoteException
	 */
	public boolean isProxyServiceExist(String backEndUrl, String sessionCookie, String proxyName)
			throws RemoteException {
		ServiceAdminClient adminServiceService = new ServiceAdminClient(backEndUrl, sessionCookie);
		return adminServiceService.isServiceExists(proxyName);

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param localEntryName
	 * @return
	 * @throws org.wso2.carbon.localentry.stub.types.LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param sequenceName
	 * @return
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param endpointName
	 * @return
	 * @throws org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageProcessor
	 * @return
	 * @throws java.rmi.RemoteException
	 */
	public boolean isMessageStoreExist(String backEndUrl, String sessionCookie,
	                                   String messageProcessor) throws RemoteException {
		MessageStoreAdminClient messageStoreAdminClient =
				new MessageStoreAdminClient(backEndUrl, sessionCookie);
		return ArrayUtils.contains(messageStoreAdminClient.getMessageStores(), messageProcessor);

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageProcessor
	 * @return
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param proxyServiceName
	 * @throws org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException
	 * @throws java.rmi.RemoteException
	 */
	public void deleteProxyService(String backEndUrl, String sessionCookie, String proxyServiceName)
			throws ProxyServiceAdminProxyAdminException, RemoteException {
		ProxyServiceAdminClient proxyAdmin = new ProxyServiceAdminClient(backEndUrl, sessionCookie);
		proxyAdmin.deleteProxy(proxyServiceName);
		Assert.assertTrue(isProxyUnDeployed(backEndUrl, sessionCookie, proxyServiceName),
		                  "Proxy service undeployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param localEntryName
	 * @throws org.wso2.carbon.localentry.stub.types.LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
	public void deleteLocalEntry(String backEndUrl, String sessionCookie, String localEntryName)
			throws LocalEntryAdminException, RemoteException {
		LocalEntriesAdminClient localEntryAdminServiceClient =
				new LocalEntriesAdminClient(backEndUrl,
				                            sessionCookie);
		Assert.assertTrue(localEntryAdminServiceClient.deleteLocalEntry(localEntryName),
		                  "LocalEntry Deletion failed");
		Assert.assertTrue(isLocalEntryUnDeployed(backEndUrl, sessionCookie, localEntryName),
		                  "LocalEntry undeployment failed");

	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param endpointName
	 * @throws org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException
	 * @throws java.rmi.RemoteException
	 */
	public void deleteEndpoint(String backEndUrl, String sessionCookie, String endpointName)
			throws EndpointAdminEndpointAdminException, RemoteException {
		EndPointAdminClient endPointAdminClient = new EndPointAdminClient(backEndUrl,
		                                                                  sessionCookie);
		Assert.assertTrue(endPointAdminClient.deleteEndpoint(endpointName),
		                  "Endpoint deletion failed");
		Assert.assertTrue(isEndpointUnDeployed(backEndUrl, sessionCookie, endpointName),
		                  "Endpoint undeployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param sequenceName
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 * @throws java.rmi.RemoteException
	 */
	public void deleteSequence(String backEndUrl, String sessionCookie, String sequenceName)
			throws SequenceEditorException, RemoteException {
		SequenceAdminServiceClient sequenceAdminServiceClient =
				new SequenceAdminServiceClient(backEndUrl,
				                               sessionCookie);
		sequenceAdminServiceClient.deleteSequence(sequenceName);
		Assert.assertTrue(isSequenceUnDeployed(backEndUrl, sessionCookie, sequenceName),
		                  "Sequence undeployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageStore
	 * @throws java.rmi.RemoteException
	 */
	public void deleteMessageStore(String backEndUrl, String sessionCookie, String messageStore)
			throws RemoteException, SequenceEditorException {
		MessageStoreAdminClient messageStoreAdminClient =
				new MessageStoreAdminClient(backEndUrl, sessionCookie);
		messageStoreAdminClient.deleteMessageStore(messageStore);
		Assert.assertTrue(isMessageStoreUnDeployed(backEndUrl, sessionCookie, messageStore),
		                  "Message Store undeployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param messageProcessor
	 * @throws java.rmi.RemoteException
	 */
	public void deleteMessageProcessor(String backEndUrl, String sessionCookie,
	                                   String messageProcessor)
			throws RemoteException, SequenceEditorException {
		MessageProcessorClient messageProcessorClient =
				new MessageProcessorClient(backEndUrl, sessionCookie);
		messageProcessorClient.deleteMessageProcessor(messageProcessor);
		Assert.assertTrue(isMessageProcessorUnDeployed(backEndUrl, sessionCookie, messageProcessor),
		                  "Message Processor undeployment failed");
	}

	public void deleteEndpointTemplate(String backEndUrl, String sessionCookie,
	                                   String endpointTemplate)
			throws RemoteException, SequenceEditorException, EndpointAdminEndpointAdminException {

		EndpointTemplateAdminServiceClient endpointTemplateAdminServiceClient =
				new EndpointTemplateAdminServiceClient(backEndUrl, sessionCookie);
		endpointTemplateAdminServiceClient.deleteEndpointTemplate(endpointTemplate);
		Assert.assertTrue(isEndpointTemplateUnDeployed(backEndUrl, sessionCookie, endpointTemplate),
		                  "Endpoint Template undeployment failed");
	}

	public void deleteSequenceTemplate(String backEndUrl, String sessionCookie,
	                                   String sequenceTemplateName)
			throws RemoteException, SequenceEditorException, EndpointAdminEndpointAdminException {
		SequenceTemplateAdminServiceClient sequenceTemplateAdminServiceClient =
				new SequenceTemplateAdminServiceClient(backEndUrl, sessionCookie);
		sequenceTemplateAdminServiceClient.deleteTemplate(sequenceTemplateName);
		Assert.assertTrue(
				isSequenceTemplateUnDeployed(backEndUrl, sessionCookie, sequenceTemplateName),
				"Sequence Template undeployment failed");
	}

	public void deleteApi(String backEndUrl, String sessionCookie, String apiName)
			throws RemoteException, RestApiAdminAPIException {
		RestApiAdminClient apiAdminClient = new RestApiAdminClient(backEndUrl, sessionCookie);
		apiAdminClient.deleteApi(apiName);
		Assert.assertTrue(isApiUnDeployed(backEndUrl, sessionCookie, apiName),
		                  "API undeployment failed");
	}

	public void deletePriorityExecutor(String backEndUrl, String sessionCookie, String executorName)
			throws RemoteException {
		PriorityMediationAdminClient priorityMediationAdminClient =
				new PriorityMediationAdminClient(backEndUrl, sessionCookie);
		priorityMediationAdminClient.remove(executorName);
		Assert.assertTrue(isPriorityExecutorUnDeployed(backEndUrl, sessionCookie, executorName),
		                  "Priority Executor undeployment failed");
	}

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param taskName      name of the ScheduleTask
	 * @param group         group of the ScheduleTask
	 * @throws org.wso2.carbon.task.stub.TaskManagementException
	 * @throws java.rmi.RemoteException
	 */
	public void deleteScheduleTask(String backEndUrl, String sessionCookie, String taskName,
	                               String group)
			throws TaskManagementException, RemoteException {
		TaskAdminClient taskAdminClient = new TaskAdminClient(backEndUrl, sessionCookie);
		taskAdminClient.deleteTask(taskName, group);
		Assert.assertTrue(isScheduleTaskUnDeployed(backEndUrl, sessionCookie, taskName),
		                  "ScheduleTask deployment failed");

	}

	/**
	 * waiting for proxy to undeploy
	 *
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param proxyName
	 * @return
	 * @throws java.rmi.RemoteException
	 */

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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param localEntryName
	 * @return
	 * @throws org.wso2.carbon.localentry.stub.types.LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param localEntryName
	 * @return
	 * @throws org.wso2.carbon.localentry.stub.types.LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param sequenceName
	 * @return
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param endpointName
	 * @return
	 * @throws org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException
	 * @throws java.rmi.RemoteException
	 */
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

	/**
	 * wait for task to undeploy and return true once it is undeployed
	 *
	 * @param backEndUrl
	 * @param sessionCookie
	 * @param taskName
	 * @return
	 * @throws java.rmi.RemoteException
	 * @throws org.wso2.carbon.task.stub.TaskManagementException
	 */
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

	/**
	 * @param synapseConfig
	 * @param backendURL
	 * @param sessionCookie
	 * @throws org.wso2.carbon.localentry.stub.types.LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 * @throws org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException
	 * @throws org.wso2.carbon.sequences.stub.types.SequenceEditorException
	 */
	public void verifySynapseDeployment(OMElement synapseConfig, String backendURL,
	                                    String sessionCookie)
			throws LocalEntryAdminException, RemoteException, EndpointAdminEndpointAdminException,
			       SequenceEditorException, RestApiAdminAPIException {
		Iterator<OMElement> localEntries = synapseConfig.getChildrenWithLocalName(LOCAL_ENTRY);
		while (localEntries.hasNext()) {
			String le = localEntries.next().getAttributeValue(new QName(KEY));
			Assert.assertTrue(isLocalEntryDeployed(backendURL, sessionCookie, le),
			                  le + " LocalEntry deployment not found or time out");
		}

		Iterator<OMElement> endpoints = synapseConfig.getChildrenWithLocalName(ENDPOINT);
		while (endpoints.hasNext()) {
			String ep = endpoints.next().getAttributeValue(new QName(NAME));
			Assert.assertTrue(isEndpointDeployed(backendURL, sessionCookie, ep),
			                  ep + " Endpoint deployment not found or time out");
		}

		Iterator<OMElement> sequences = synapseConfig.getChildrenWithLocalName(SEQUENCE);
		while (sequences.hasNext()) {
			String sqn = sequences.next().getAttributeValue(new QName(NAME));
			Assert.assertTrue(isSequenceDeployed(backendURL, sessionCookie, sqn),
			                  sqn + " Sequence deployment not found or time out");
		}

		Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName(PROXY);
		while (proxies.hasNext()) {
			String proxy = proxies.next().getAttributeValue(new QName(NAME));
			Assert.assertTrue(isProxyDeployed(backendURL, sessionCookie, proxy),
			                  proxy + " Proxy Deployment not found or time out");
		}

		Iterator<OMElement> messageStores = synapseConfig.getChildrenWithLocalName(MESSAGE_STORE);
		while (messageStores.hasNext()) {
			String mStore = messageStores.next().getAttributeValue(new QName(NAME));
			Assert.assertTrue(isMessageStoreDeployed(backendURL, sessionCookie, mStore),
			                  mStore + " Message Store Deployment not found or time out");
		}

		Iterator<OMElement> messageProcessor =
				synapseConfig.getChildrenWithLocalName(MESSAGE_PROCESSOR);
		while (messageProcessor.hasNext()) {
			String mProcessor = messageProcessor.next().getAttributeValue(new QName(NAME));
			Assert.assertTrue(isMessageProcessorDeployed(backendURL, sessionCookie, mProcessor),
			                  mProcessor + " Message Processor Deployment not found or time out");
		}

		Iterator<OMElement> templates = synapseConfig.getChildrenWithLocalName(TEMPLATE);
		while (templates.hasNext()) {
			OMElement template = templates.next();
			String templateName = template.getAttributeValue(new QName(NAME));
			if (template.getFirstChildWithName(
					new QName(template.getNamespace().getNamespaceURI(), SEQUENCE)) != null) {
				Assert.assertTrue(
						isSequenceTemplateDeployed(backendURL, sessionCookie, templateName),
						templateName + " Sequence Template Deployment not found or time out");
			} else {

				Assert.assertTrue(
						isEndpointTemplateDeployed(backendURL, sessionCookie, templateName),
						templateName + " Endpoint Template Deployment not found or time out");
			}
			log.info("Template Uploaded");
		}

		Iterator<OMElement> apiList = synapseConfig.getChildrenWithLocalName(API);
		while (apiList.hasNext()) {
			OMElement api = apiList.next();
			String apiName = api.getAttributeValue(new QName(NAME));
			String version = api.getAttributeValue(new QName(VERSION));
			if(version != null && !version.equals("")){
				apiName = apiName +  ":v" + version;
			}
			Assert.assertTrue(isApiDeployed(backendURL, sessionCookie, apiName),
			                  apiName + " API Deployment not found or time out");
		}

		Iterator<OMElement> executorList =
				synapseConfig.getChildrenWithLocalName(PRIORITY_EXECUTOR);
		while (executorList.hasNext()) {
			String executorName = executorList.next().getAttributeValue(new QName(NAME));
			Assert.assertTrue(isPriorityExecutorDeployed(backendURL, sessionCookie, executorName),
			                  executorName + " Priority Executor Deployment not found or time out");
		}

	}

	public void deleteArtifact(OMElement synapseConfig, String backendURL,
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
		RestApiAdminClient apiAdminClient = new RestApiAdminClient(backendURL, sessionCookie);
		PriorityMediationAdminClient priorityMediationAdminClient =
				new PriorityMediationAdminClient(backendURL, sessionCookie);

		Iterator<OMElement> localEntries = synapseConfig.getChildrenWithLocalName(LOCAL_ENTRY);
		while (localEntries.hasNext()) {
			OMElement localEntry = localEntries.next();
			String le = localEntry.getAttributeValue(new QName(KEY));
			if (ArrayUtils.contains(localEntryAdminServiceClient.getEntryNames(), le)) {
				Assert.assertTrue(localEntryAdminServiceClient.deleteLocalEntry(le),
				                  le + " Local Entry deletion failed");
				Assert.assertTrue(isLocalEntryUnDeployed(backendURL, sessionCookie, le),
				                  le + " Local Entry undeployment failed");
			}
		}

		Iterator<OMElement> endpoints = synapseConfig.getChildrenWithLocalName(ENDPOINT);
		while (endpoints.hasNext()) {
			OMElement endpoint = endpoints.next();
			String ep = endpoint.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(endPointAdminClient.getEndpointNames(), ep)) {
				Assert.assertTrue(endPointAdminClient.deleteEndpoint(ep),
				                  ep + " Endpoint deletion failed");
				Assert.assertTrue(isEndpointUnDeployed(backendURL, sessionCookie, ep),
				                  ep + " Endpoint undeployment failed");
			}
		}

		Iterator<OMElement> sequences = synapseConfig.getChildrenWithLocalName(SEQUENCE);
		while (sequences.hasNext()) {
			OMElement sequence = sequences.next();
			String sqn = sequence.getAttributeValue(new QName(NAME));
			if (sqn.equalsIgnoreCase("fault") || sqn.equalsIgnoreCase("main")) {
				continue;
			}
			if (ArrayUtils.contains(sequenceAdminClient.getSequences(), sqn)) {
				sequenceAdminClient.deleteSequence(sqn);
				Assert.assertTrue(isSequenceUnDeployed(backendURL, sessionCookie, sqn),
				                  sqn + " Sequence undeployment failed");
			}

		}

		Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName(PROXY);
		while (proxies.hasNext()) {
			OMElement proxy = proxies.next();
			String proxyName = proxy.getAttributeValue(new QName(NAME));
			if (adminServiceService.isServiceExists(proxyName)) {
				proxyAdmin.deleteProxy(proxyName);
				Assert.assertTrue(isProxyUnDeployed(backendURL, sessionCookie, proxyName),
				                  proxyName + " Undeployment failed");
			}
		}

		Iterator<OMElement> messageStores = synapseConfig.getChildrenWithLocalName(MESSAGE_STORE);
		while (messageStores.hasNext()) {
			OMElement messageStore = messageStores.next();
			String mStore = messageStore.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(messageStoreAdminClient.getMessageStores(), mStore)) {
				messageStoreAdminClient.deleteMessageStore(mStore);
				Assert.assertTrue(isMessageStoreUnDeployed(backendURL, sessionCookie, mStore),
				                  mStore + " Message Store undeployment failed");
			}
		}

		Iterator<OMElement> messageProcessors =
				synapseConfig.getChildrenWithLocalName(MESSAGE_PROCESSOR);
		while (messageProcessors.hasNext()) {
			OMElement messageProcessor = messageProcessors.next();
			String mProcessor = messageProcessor.getAttributeValue(new QName(NAME));
			if (ArrayUtils
					.contains(messageProcessorClient.getMessageProcessorNames(), mProcessor)) {
				messageProcessorClient.deleteMessageProcessor(mProcessor);
				Assert.assertTrue(
						isMessageProcessorUnDeployed(backendURL, sessionCookie, mProcessor)
						, mProcessor + " Message Processor undeployment failed");
			}
		}

		Iterator<OMElement> templates = synapseConfig.getChildrenWithLocalName(TEMPLATE);
		while (templates.hasNext()) {
			OMElement template = templates.next();
			String templateName = template.getAttributeValue(new QName(NAME));
			if (template.getFirstChildWithName(
					new QName(template.getNamespace().getNamespaceURI(), SEQUENCE)) != null) {
				deleteSequenceTemplate(backendURL, sessionCookie, templateName);

			} else {
				deleteEndpointTemplate(backendURL, sessionCookie, templateName);
			}
			log.info("Template UnUploaded");
		}

		Iterator<OMElement> apiList = synapseConfig.getChildrenWithLocalName(API);
		while (apiList.hasNext()) {
			OMElement api = apiList.next();
			String apiName = api.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(apiAdminClient.getApiNames(), apiName)) {
				apiAdminClient.deleteApi(apiName);
				Assert.assertTrue(isApiUnDeployed(backendURL, sessionCookie, apiName)
						, apiName + " API undeployment failed");
			}
		}

		Iterator<OMElement> executorList =
				synapseConfig.getChildrenWithLocalName(PRIORITY_EXECUTOR);
		while (executorList.hasNext()) {
			OMElement executor = executorList.next();
			String executorName = executor.getAttributeValue(new QName(NAME));
			if (ArrayUtils.contains(priorityMediationAdminClient.getExecutorList(), executorName)) {
				priorityMediationAdminClient.remove(executorName);
				Assert.assertTrue(
						isPriorityExecutorUnDeployed(backendURL, sessionCookie, executorName)
						, executorName + " Priority Executor undeployment failed");
			}
		}

		log.info("Synapse configuration  unDeployed");

	}

	public void deploySynapseArtifactsFromFileSystem(String directoryPath, String backendURL,
	                                                 String sessionCookie) throws Exception {

		File[] fileList = new File(directoryPath).listFiles();

		HashMap<String, File[]> fileStructure = new HashMap<String, File[]>();

		for (int x = 0; x <= fileList.length - 1; x++) {
			File FileName = fileList[x];
			fileStructure.put(FileName.getName(), FileName.listFiles());
		}

		for (Map.Entry<String, File[]> stringEntry : fileStructure.entrySet()) {

			Map.Entry entry = (Map.Entry) stringEntry;
			File[] fileArr = (File[]) entry.getValue();

			if (entry.getKey().equals("api")) {

				OMElement apiOmElement;

				for (File aFileArr : fileArr) {
					//deploying the api to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					apiOmElement = loadResource(filePathArr[1]);

					String apiName = apiOmElement.getAttributeValue(new QName("name"));

					if (isApiExist(backendURL, sessionCookie, apiName)) {
						deleteApi(backendURL, sessionCookie, apiName);
					}
					addAPI(backendURL, sessionCookie, apiOmElement);
					Assert.assertTrue(isApiDeployed(backendURL, sessionCookie, apiName),
					                  "api " + apiName + "deployment failure");
				}
			} else if (entry.getKey().equals("endpoints")) {
				OMElement endpointOmElement;

				for (File aFileArr : fileArr) {
					//deploying the endpoints to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					endpointOmElement = loadResource(filePathArr[1]);

					String endpointName = endpointOmElement.getAttributeValue(new QName("name"));

					if (isEndpointExist(backendURL, sessionCookie, endpointName)) {
						deleteEndpoint(backendURL, sessionCookie, endpointName);
					}
					addEndpoint(backendURL, sessionCookie, endpointOmElement);
					Assert.assertTrue(isEndpointDeployed(backendURL, sessionCookie, endpointName),
					                  "endpoint " + endpointName + "deployment failure");
				}
			} else if (entry.getKey().equals("priority-executors")) {
				OMElement priorityExecutorsOmElement;

				for (File aFileArr : fileArr) {
					//deploying the priority-executors to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					priorityExecutorsOmElement = loadResource(filePathArr[1]);

					String proxyExecutorName =
							priorityExecutorsOmElement.getAttributeValue(new QName("name"));

					if (isPriorityExecutorExist(backendURL, sessionCookie, proxyExecutorName)) {
						deletePriorityExecutor(backendURL, sessionCookie, proxyExecutorName);
					}
					addPriorityExecutor(backendURL, sessionCookie, priorityExecutorsOmElement);
					Assert.assertTrue(isPriorityExecutorDeployed(backendURL, sessionCookie,
					                                             proxyExecutorName),
					                  "priority-executor " + proxyExecutorName +
					                  "deployment failure");
				}

			} else if (entry.getKey().equals("sequences")) {
				OMElement sequencesOmElement;

				for (File aFileArr : fileArr) {
					//deploying the sequences to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					sequencesOmElement = loadResource(filePathArr[1]);

					String sequenceName = sequencesOmElement.getAttributeValue(new QName("name"));

					if (isSequenceExist(backendURL, sessionCookie, sequenceName)) {
						deleteSequence(backendURL, sessionCookie, sequenceName);
					}
					addSequence(backendURL, sessionCookie, sequencesOmElement);
					Assert.assertTrue(isSequenceDeployed(backendURL, sessionCookie, sequenceName),
					                  "sequence " + sequenceName + "deployment failure");
				}
			} else if (entry.getKey().equals("proxy-services")) {
				OMElement proxyServicesOmElement;

				for (File aFileArr : fileArr) {
					//deploying the proxy-services to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					proxyServicesOmElement = loadResource(filePathArr[1]);

					String proxyServiceName =
							proxyServicesOmElement.getAttributeValue(new QName("name"));

					if (isProxyServiceExist(backendURL, sessionCookie, proxyServiceName)) {
						deleteProxyService(backendURL, sessionCookie, proxyServiceName);
					}
					addProxyService(backendURL, sessionCookie, proxyServicesOmElement);
					Assert.assertTrue(isProxyDeployed(backendURL, sessionCookie, proxyServiceName),
					                  "proxy-service " + proxyServiceName + "deployment failure");
				}
			} else if (entry.getKey().equals("local-entries")) {
				OMElement localEntriesOmElement;

				for (File aFileArr : fileArr) {
					//deploying the local-entries to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					localEntriesOmElement = loadResource(filePathArr[1]);

					String localEntryKey =
							localEntriesOmElement.getAttributeValue(new QName("key"));

					if (isLocalEntryExist(backendURL, sessionCookie, localEntryKey)) {
						deleteLocalEntry(backendURL, sessionCookie, localEntryKey);
					}
					addLocalEntry(backendURL, sessionCookie, localEntriesOmElement);
					Assert.assertTrue(
							isLocalEntryDeployed(backendURL, sessionCookie, localEntryKey),
							"local-entry " + localEntryKey + "deployment failure");
				}
			} else if (entry.getKey().equals("tasks")) {
				OMElement taskOmElement;

				for (File aFileArr : fileArr) {
					//deploying the tasks to esb
					String filePathArr[] = aFileArr.getAbsolutePath()
					                               .split(TestConfigurationProvider
							                                      .getResourceLocation());
					taskOmElement = loadResource(filePathArr[1]);

					if (taskOmElement.getNamespace().getPrefix().equals("")) {

						OMFactory fac = OMAbstractFactory.getOMFactory();
						OMNamespace omNs = fac.createOMNamespace(
								"http://www.wso2.org/products/wso2commons/tasks", "task");
						taskOmElement.setNamespace(omNs);

						Iterator it = taskOmElement.getChildElements();

						while (it.hasNext()) {
							((OMElement) it.next()).setNamespace(omNs);
						}
					}

					String taskName = taskOmElement.getAttributeValue(new QName("name"));
					String taskGroup = taskOmElement.getAttributeValue(new QName("group"));

					if (isScheduleTaskExist(backendURL, sessionCookie, taskName)) {
						deleteScheduleTask(backendURL, sessionCookie, taskName, taskGroup);
					}
					addScheduleTask(backendURL, sessionCookie, taskOmElement);
					Assert.assertTrue(isScheduleTaskDeployed(backendURL, sessionCookie, taskName),
					                  "task " + taskName + "deployment failure");

				}
			} else {
				log.info(entry.getKey() + " was not deployed");
			}
		}
	}
}
