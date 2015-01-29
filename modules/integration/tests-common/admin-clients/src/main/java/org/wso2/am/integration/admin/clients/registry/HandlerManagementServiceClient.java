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

package org.wso2.am.integration.admin.clients.registry;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStub;
import org.wso2.carbon.registry.handler.stub.ExceptionException;
import org.wso2.carbon.registry.handler.stub.HandlerManagementServiceStub;

import java.rmi.RemoteException;

public class HandlerManagementServiceClient {
	private static final Log log = LogFactory.getLog(HandlerManagementServiceClient.class);
	private final String serviceName = "HandlerManagementService";
	private HandlerManagementServiceStub handlerManagementServiceStub;
	private String endPoint;

	public HandlerManagementServiceClient(String backEndUrl, String sessionCookie)
			throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		try {
			handlerManagementServiceStub = new HandlerManagementServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("handlerManagementServiceStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault("handlerManagementServiceStub Initialization fail ", axisFault);
		}
		AuthenticateStub.authenticateStub(sessionCookie, handlerManagementServiceStub);
	}

	public HandlerManagementServiceClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		try {
			handlerManagementServiceStub = new HandlerManagementServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("handlerManagementServiceStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault("handlerManagementServiceStub Initialization fail ", axisFault);
		}
		AuthenticateStub.authenticateStub(userName, password, handlerManagementServiceStub);
	}

	public String[] getHandlerList() throws RemoteException, ExceptionException {

		try {
			return handlerManagementServiceStub.getHandlerList();

		} catch (RemoteException e) {
			String msg = "Fails to get Handler List";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to get Handler List";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}

	}

	public boolean createHandler(String payload) throws RemoteException, ExceptionException {

		try {
			return handlerManagementServiceStub.createHandler(payload);
		} catch (RemoteException e) {
			String msg = "Fails to create Handler";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to create Handler";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}
	}

	public boolean updateHandler(String oldName, String payload)
			throws RemoteException, ExceptionException {

		try {
			return handlerManagementServiceStub.updateHandler(oldName, payload);
		} catch (RemoteException e) {
			String msg = "Fails to update Handler";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to update Handler";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}
	}

	public boolean deleteHandler(String name) throws RemoteException, ExceptionException {

		try {
			return handlerManagementServiceStub.deleteHandler(name);
		} catch (RemoteException e) {
			String msg = "Fails to delete Handler";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to delete Handler";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}
	}

	public String getHandlerConfiguration(String name) throws RemoteException, ExceptionException {

		try {
			return handlerManagementServiceStub.getHandlerConfiguration(name);
		} catch (RemoteException e) {
			String msg = "Fails to get Handler Configuration";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to get Handler Configuration";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}
	}

	public String getHandlerCollectionLocation() throws RemoteException, ExceptionException {

		try {
			return handlerManagementServiceStub.getHandlerCollectionLocation();
		} catch (RemoteException e) {
			String msg = "Fails to get Handler Collection Location";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to get Handler Collection Location";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}

	}

	public void setHandlerCollectionLocation(String location)
			throws RemoteException, ExceptionException {

		try {
			handlerManagementServiceStub.setHandlerCollectionLocation(location);
		} catch (RemoteException e) {
			String msg = "Fails to get Handler Collection Location";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (ExceptionException e) {
			String msg = "Fails to get Handler Collection Location";
			log.error(msg + " " + e.getMessage());
			throw new ExceptionException(msg, e);
		}

	}
}
