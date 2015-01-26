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
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminException;
import org.wso2.carbon.message.store.stub.MessageInfo;
import org.wso2.carbon.message.store.stub.MessageStoreAdminServiceStub;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class MassageStoreAdminClient {

	String backendUrl = null;
	String SessionCookie = null;
	MessageStoreAdminServiceStub messageStoreAdminServiceStub;
	private static final Log log = LogFactory.getLog(MassageStoreAdminClient.class);

	public MassageStoreAdminClient(String backendUrl, String sessionCookie) {
		this.backendUrl = backendUrl;
		this.SessionCookie = sessionCookie;
	}

	private MessageStoreAdminServiceStub setMessageStoreStubStub() throws AxisFault {
		final String messageStoreServiceUrl = backendUrl + "MessageStoreAdminService";
		MessageStoreAdminServiceStub messageStoreAdminService = null;
		messageStoreAdminService = new MessageStoreAdminServiceStub(messageStoreServiceUrl);
		AuthenticateStub.authenticateStub(SessionCookie, messageStoreAdminService);
		return messageStoreAdminService;
	}

	public void addMessageStore(DataHandler dh)
			throws IOException, LocalEntryAdminException, XMLStreamException,
			       Exception {
		messageStoreAdminServiceStub = this.setMessageStoreStubStub();
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement localEntryElem = builder.getDocumentElement();
		messageStoreAdminServiceStub.addMessageStore(localEntryElem.toString());
	}

	public String[] getMessageStores() throws RemoteException {
		messageStoreAdminServiceStub = this.setMessageStoreStubStub();
		return messageStoreAdminServiceStub.getMessageStoreNames();
	}

	public int getMessageCount(String storeName) throws RemoteException {
		messageStoreAdminServiceStub = this.setMessageStoreStubStub();
		return messageStoreAdminServiceStub.getSize(storeName);
	}

	public MessageInfo[] getAllMessages(String storeName) throws RemoteException {
		messageStoreAdminServiceStub = this.setMessageStoreStubStub();
		return messageStoreAdminServiceStub.getAllMessages(storeName);
	}
}
