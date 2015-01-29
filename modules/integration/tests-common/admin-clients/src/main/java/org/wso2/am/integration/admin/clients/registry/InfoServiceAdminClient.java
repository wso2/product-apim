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
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.info.stub.InfoAdminServiceStub;
import org.wso2.carbon.registry.info.stub.RegistryExceptionException;
import org.wso2.carbon.registry.info.stub.beans.xsd.*;

import java.rmi.RemoteException;

public class InfoServiceAdminClient {

	private static final Log log = LogFactory.getLog(InfoServiceAdminClient.class);

	private InfoAdminServiceStub infoAdminServiceStub;

	public InfoServiceAdminClient(String backEndUrl, String sessionCookie)
			throws RegistryException, AxisFault {
		String serviceName = "InfoAdminService";
		String endPoint = backEndUrl + serviceName;
		try {
			infoAdminServiceStub = new InfoAdminServiceStub(endPoint);
			AuthenticateStub.authenticateStub(sessionCookie, infoAdminServiceStub);
		} catch (AxisFault axisFault) {
			log.error("infoAdminServiceStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault(
					"infoAdminServiceStub Initialization fail " + axisFault.getMessage());
		}

	}

	public InfoServiceAdminClient(String backEndUrl, String userName, String password)
			throws RegistryException, AxisFault {
		String serviceName = "InfoAdminService";
		String endPoint = backEndUrl + serviceName;
		try {
			infoAdminServiceStub = new InfoAdminServiceStub(endPoint);
			AuthenticateStub.authenticateStub(userName, password, infoAdminServiceStub);
		} catch (AxisFault axisFault) {
			log.error("infoAdminServiceStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault(
					"infoAdminServiceStub Initialization fail " + axisFault.getMessage());
		}

	}

	public SubscriptionBean subscribe(String path, String endpoint, String eventName,
	                                  String sessionId) throws RemoteException {
		SubscriptionBean bean = null;
		try {
			bean = infoAdminServiceStub.subscribe(path, endpoint, eventName, sessionId);
		} catch (RemoteException remoteException) {
			log.error("infoAdminServiceStub subscription fail " + remoteException.getMessage());
			throw new RemoteException(
					"infoAdminServiceStub subscription fail " + remoteException.getMessage());

		} catch (RegistryExceptionException registryException) {
			log.error("infoAdminServiceStub subscription fail " + registryException.getMessage());
			throw new AxisFault(
					"infoAdminServiceStub subscription fail " + registryException.getMessage());

		}
		return bean;
	}

	public void addComment(String comment, String path, String sessionId)
			throws RegistryException, AxisFault {
		try {
			infoAdminServiceStub.addComment(comment, path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to add comment to resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to add comment to resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		}
	}

	public void addTag(String tag, String path, String sessionId)
			throws RegistryException, AxisFault {
		try {
			infoAdminServiceStub.addTag(tag, path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to add tag to resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to add tag to resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		}
	}

	public void rateResource(String rating, String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			infoAdminServiceStub.rateResource(rating, path, sessionId);

		} catch (RemoteException e) {
			String msg = "Unable to rate resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to rate resource path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public CommentBean getComments(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.getComments(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to get comments of path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to rate comments of path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public RatingBean getRatings(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.getRatings(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to get ratings of resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to get ratings of resource path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public TagBean getTags(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.getTags(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to get tags of resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to get tags of resource path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public EventTypeBean getEventTypes(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.getEventTypes(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to get events types of resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to get event types of resource path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public String getRemoteURL(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.getRemoteURL(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to get remote URL of resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to get remote URL of resource path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public SubscriptionBean getSubscriptions(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.getSubscriptions(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to get subscriptions of resource path - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to get subscriptions of resource path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public boolean isProfileExisting(String userName, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.isProfileExisting(userName, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to check profiles of - " + userName;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to check profiles of - " + userName;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public boolean isResource(String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.isResource(path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to check resource - " + path;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to check resource - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public boolean isRoleProfileExisting(String role, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.isRoleProfileExisting(role, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to check role profile of - " + role;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to check role profile of - " + role;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public boolean isRoleValid(String role, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.isRoleValid(role, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to check validity of role - " + role;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to check validity of role - " + role;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public boolean isUserValid(String userName, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			return infoAdminServiceStub.isUserValid(userName, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to check validity of user - " + userName;
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to check validity of user - " + userName;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public void removeComment(String commentPath, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			infoAdminServiceStub.removeComment(commentPath, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to remove comment";
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to remove comment  ";
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public void removeTag(String tag, String path, String sessionId)
			throws RegistryException, RegistryExceptionException {
		try {
			infoAdminServiceStub.removeTag(tag, path, sessionId);
		} catch (RemoteException e) {
			String msg = "Unable to remove tag";
			log.error(msg, e);
			throw new RegistryException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to remove tag  ";
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public void unsubscribe(String path, String subscriptionId, String sessionID)
			throws RemoteException, RegistryExceptionException {
		try {
			infoAdminServiceStub.unsubscribe(path, subscriptionId, sessionID);
		} catch (RemoteException e) {
			String msg = "Unable to unsubscribe";
			log.error(msg, e);
			throw new RemoteException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Unable to unsubscribe";
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}
}