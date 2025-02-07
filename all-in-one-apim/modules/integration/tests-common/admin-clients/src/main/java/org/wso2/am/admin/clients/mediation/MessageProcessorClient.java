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

package org.wso2.am.admin.clients.mediation;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.message.processor.stub.MessageProcessorAdminServiceStub;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class MessageProcessorClient {

	private static final Log log = LogFactory.getLog(MessageProcessorClient.class);

	private MessageProcessorAdminServiceStub messageProcessorAdminServiceStub;
	private final String serviceName = "MessageProcessorAdminService";

	public MessageProcessorClient(String backEndUrl, String sessionCookie) throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		messageProcessorAdminServiceStub = new MessageProcessorAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, messageProcessorAdminServiceStub);
	}

	public MessageProcessorClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		messageProcessorAdminServiceStub = new MessageProcessorAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, messageProcessorAdminServiceStub);
	}

	public void addMessageProcessor(DataHandler dh) throws IOException, XMLStreamException {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement messageProcessorElem = builder.getDocumentElement();
		messageProcessorAdminServiceStub.addMessageProcessor(messageProcessorElem.toString());
	}

	public void addMessageProcessor(OMElement messageProcessor) throws RemoteException {
		messageProcessorAdminServiceStub.addMessageProcessor(messageProcessor.toString());
	}

	public void deleteMessageProcessor(String messageProcessorName) throws RemoteException {
		messageProcessorAdminServiceStub.deleteMessageProcessor(messageProcessorName);
	}

	public String[] getMessageProcessorNames() throws RemoteException {
		return messageProcessorAdminServiceStub.getMessageProcessorNames();
	}
}
