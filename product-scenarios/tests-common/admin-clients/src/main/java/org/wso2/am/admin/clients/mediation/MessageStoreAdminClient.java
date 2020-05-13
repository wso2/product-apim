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
import org.wso2.carbon.message.store.stub.Exception;
import org.wso2.carbon.message.store.stub.MessageInfo;
import org.wso2.carbon.message.store.stub.MessageStoreAdminServiceStub;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class MessageStoreAdminClient {

	private static final Log log = LogFactory.getLog(MessageStoreAdminClient.class);
	private MessageStoreAdminServiceStub messageStoreAdminServiceStub;
	private final String serviceName = "MessageStoreAdminService";

	public MessageStoreAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		messageStoreAdminServiceStub = new MessageStoreAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, messageStoreAdminServiceStub);
	}

	public MessageStoreAdminClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		messageStoreAdminServiceStub = new MessageStoreAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, messageStoreAdminServiceStub);
	}

	public void addMessageStore(DataHandler dh)
			throws IOException, XMLStreamException, org.wso2.carbon.message.store.stub.Exception {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement messageStore = builder.getDocumentElement();
		messageStoreAdminServiceStub.addMessageStore(messageStore.toString());
	}

	public void addMessageStore(OMElement messageStore) throws RemoteException, Exception {
		messageStoreAdminServiceStub.addMessageStore(messageStore.toString());
	}

	public void updateMessageStore(OMElement messageStore) throws RemoteException {
		messageStoreAdminServiceStub.modifyMessageStore(messageStore.toString());
	}

	public void deleteMessageStore(String messageStoreName) throws RemoteException {
		messageStoreAdminServiceStub.deleteMessageStore(messageStoreName);
	}

	public void deleteMessage(String storeName, String messageId) throws RemoteException {
		messageStoreAdminServiceStub.deleteMessage(storeName, messageId);
	}

	public String[] getMessageStores() throws RemoteException {
		return messageStoreAdminServiceStub.getMessageStoreNames();
	}

	public int getMessageCount(String storeName) throws RemoteException {
		return messageStoreAdminServiceStub.getSize(storeName);
	}

	public MessageInfo[] getAllMessages(String storeName) throws RemoteException {
		return messageStoreAdminServiceStub.getAllMessages(storeName);
	}

	public MessageInfo[] getPaginatedMessages(String storeName, int pageNo) throws RemoteException {
		return messageStoreAdminServiceStub.getPaginatedMessages(storeName, pageNo);
	}

	public String getEnvelope(String storeName, String messageId) throws RemoteException {
		return messageStoreAdminServiceStub.getEnvelope(storeName, messageId);
	}
}
