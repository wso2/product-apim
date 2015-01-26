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
package org.wso2.am.admin.clients.registry;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.registry.activities.stub.ActivityAdminServiceStub;
import org.wso2.carbon.registry.activities.stub.RegistryExceptionException;
import org.wso2.carbon.registry.activities.stub.beans.xsd.ActivityBean;

import java.rmi.RemoteException;

public class ActivityAdminServiceClient {
	private static final Log log = LogFactory.getLog(ActivityAdminServiceClient.class);

	private final String serviceName = "ActivityAdminService";
	private ActivityAdminServiceStub activityAdminServiceStub;
	private String endPoint;

	public final static String FILTER_ALL = "All";
	public final static String FILTER_ASSOCIATE_ASPECT = "Associate Aspect";
	public final static String FILTER_RESOURCE_ADDED = "Resource Add";
	public final static String FILTER_RESOURCE_UPDATE = "Resource Update";

	public ActivityAdminServiceClient(String backEndUrl, String sessionCookie) throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		try {
			activityAdminServiceStub = new ActivityAdminServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("activityAdminServiceStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault("activityAdminServiceStub Initialization fail ", axisFault);
		}
		AuthenticateStub.authenticateStub(sessionCookie, activityAdminServiceStub);
	}

	public ActivityAdminServiceClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		try {
			activityAdminServiceStub = new ActivityAdminServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("activityAdminServiceStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault("activityAdminServiceStub Initialization fail ", axisFault);
		}
		AuthenticateStub.authenticateStub(userName, password, activityAdminServiceStub);
	}

	public ActivityBean getActivities(String sessionCookie, String userName, String resourcePath
			, String fromDate, String toDate, String filter, int page)
			throws RemoteException, RegistryExceptionException {
		try {
			return activityAdminServiceStub.getActivities(userName, resourcePath, fromDate, toDate
					, filter, page + "", sessionCookie);
		} catch (RemoteException e) {
			String msg = "Fails to get activities";
			log.error(msg + " " + e.getMessage());
			throw new RemoteException(msg, e);
		} catch (RegistryExceptionException e) {
			String msg = "Fails to get activities";
			log.error(msg + " " + e.getMessage());
			throw new RegistryExceptionException(msg, e);
		}
	}

	public ConfigurationContext getConfigurationContext() {
		return activityAdminServiceStub._getServiceClient().getServiceContext()
		                               .getConfigurationContext();
	}
}
