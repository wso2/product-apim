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
package org.wso2.am.admin.clients.localentry;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminException;
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminServiceStub;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class AdminServiceLocalEntryAdminService {
	private static final Log log = LogFactory.getLog(AdminServiceLocalEntryAdminService.class);

	private final String serviceName = "LocalEntryAdmin";
	private LocalEntryAdminServiceStub localEntryAdminServiceStub;
	private String endPoint;

	public AdminServiceLocalEntryAdminService(String backEndUrl) throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		localEntryAdminServiceStub = new LocalEntryAdminServiceStub(endPoint);
	}

	public void addLocalEntry(String sessionCookie, DataHandler dh)
			throws LocalEntryAdminException, IOException, XMLStreamException {
		AuthenticateStub.authenticateStub(sessionCookie, localEntryAdminServiceStub);
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		//create the builder
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement localEntryElem = builder.getDocumentElement();
		localEntryAdminServiceStub.addEntry(localEntryElem.toString());

	}

	public void deleteLocalEntry(String sessionCookie, String localEntryKey)
			throws LocalEntryAdminException, RemoteException {
		AuthenticateStub.authenticateStub(sessionCookie, localEntryAdminServiceStub);
		localEntryAdminServiceStub.deleteEntry(localEntryKey);

	}

	public OMElement getLocalEntry(String sessionCookie, String localEntryKey)
			throws LocalEntryAdminException, RemoteException {
		AuthenticateStub.authenticateStub(sessionCookie, localEntryAdminServiceStub);
		return (OMElement) localEntryAdminServiceStub.getEntry(localEntryKey);

	}
}
