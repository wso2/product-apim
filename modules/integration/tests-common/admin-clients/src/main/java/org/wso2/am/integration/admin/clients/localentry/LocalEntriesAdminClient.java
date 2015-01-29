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

package org.wso2.am.integration.admin.clients.localentry;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStub;
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminException;
import org.wso2.carbon.localentry.stub.types.LocalEntryAdminServiceStub;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class LocalEntriesAdminClient {

	private static final Log log = LogFactory.getLog(LocalEntriesAdminClient.class);

	private LocalEntryAdminServiceStub localEntryAdminServiceStub;
	private final String serviceName = "LocalEntryAdmin";

	/**
	 * creation of  LocalEntriesAdminClient using sessionCokkie
	 *
	 * @param backendUrl
	 * @param sessionCookie
	 * @throws AxisFault
	 */
	public LocalEntriesAdminClient(String backendUrl, String sessionCookie) throws AxisFault {
		String endPoint = backendUrl + serviceName;
		localEntryAdminServiceStub = new LocalEntryAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, localEntryAdminServiceStub);
	}

	/**
	 * Creation of LocalEntriesAdminClient using userName and password
	 *
	 * @param backendUrl
	 * @param userName
	 * @param password
	 * @throws AxisFault
	 */

	public LocalEntriesAdminClient(String backendUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backendUrl + serviceName;
		localEntryAdminServiceStub = new LocalEntryAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, localEntryAdminServiceStub);
	}

	/**
	 * Add Local entry by DataHandler
	 *
	 * @param dh
	 * @return
	 * @throws java.io.IOException
	 * @throws LocalEntryAdminException
	 * @throws javax.xml.stream.XMLStreamException
	 */
	public boolean addLocalEntery(DataHandler dh)
			throws IOException, LocalEntryAdminException, XMLStreamException {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement localEntryElem = builder.getDocumentElement();
		return (localEntryAdminServiceStub.addEntry(localEntryElem.toString()));
	}

	/**
	 * Add local entry by OMElement
	 *
	 * @param localEntry
	 * @return
	 * @throws LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
	public boolean addLocalEntry(OMElement localEntry)
			throws LocalEntryAdminException, RemoteException {
		return (localEntryAdminServiceStub.addEntry(localEntry.toString()));
	}

	/**
	 * Delete local entry
	 *
	 * @param localEntryKey
	 * @return
	 * @throws LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
	public boolean deleteLocalEntry(String localEntryKey)
			throws LocalEntryAdminException, RemoteException {
		return (localEntryAdminServiceStub.deleteEntry(localEntryKey));
	}

	/**
	 * @return
	 * @throws LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
	public String getEntryNamesString()
			throws LocalEntryAdminException, RemoteException {
		return localEntryAdminServiceStub.getEntryNamesString();
	}

	/**
	 * @return
	 * @throws LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
	public int getEntryDataCount()
			throws LocalEntryAdminException, RemoteException {
		return localEntryAdminServiceStub.getEntryDataCount();
	}

	/**
	 * @return
	 * @throws LocalEntryAdminException
	 * @throws java.rmi.RemoteException
	 */
	public String[] getEntryNames() throws LocalEntryAdminException, RemoteException {
		return localEntryAdminServiceStub.getEntryNames();
	}
}
