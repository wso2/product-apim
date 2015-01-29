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

package org.wso2.am.integration.admin.clients.mgt;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStub;
import org.wso2.carbon.transport.mgt.stub.Exception;
import org.wso2.carbon.transport.mgt.stub.TransportAdminStub;
import org.wso2.carbon.transport.mgt.stub.types.carbon.TransportData;

import java.rmi.RemoteException;

public class TransportManagementAdminServiceClient {
	private static final Log log = LogFactory.getLog(TransportManagementAdminServiceClient.class);

	private final String serviceName = "TransportAdmin";
	private TransportAdminStub transportAdminStub;

	public TransportManagementAdminServiceClient(String backEndUrl, String sessionCookie)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		transportAdminStub = new TransportAdminStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, transportAdminStub);
	}

	public TransportManagementAdminServiceClient(String backEndUrl, String userName,
	                                             String password) throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		transportAdminStub = new TransportAdminStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, transportAdminStub);
	}

	public TransportData[] getAllTransportData()
			throws RemoteException, org.wso2.carbon.transport.mgt.stub.Exception {
		return transportAdminStub.getAllTransportData();
	}

	/**
	 * @param serviceName
	 * @param transport   http or https
	 * @throws java.rmi.RemoteException
	 * @throws Exception
	 */
	public void addExposedTransports(String serviceName, String transport)
			throws RemoteException, Exception {

		transportAdminStub.addExposedTransports(serviceName, transport);
	}

}
