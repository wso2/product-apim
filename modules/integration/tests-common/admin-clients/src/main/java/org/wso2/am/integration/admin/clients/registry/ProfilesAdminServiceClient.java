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
import org.wso2.carbon.registry.profiles.stub.ProfilesAdminServiceStub;
import org.wso2.carbon.registry.profiles.stub.beans.xsd.ProfilesBean;

import java.rmi.RemoteException;

public class ProfilesAdminServiceClient {

	private final String serviceName = "ProfilesAdminService";
	private ProfilesAdminServiceStub profilesAdminServiceStub;
	private String endPoint;
	private static final Log log = LogFactory.getLog(ProfilesAdminServiceClient.class);

	public ProfilesAdminServiceClient(String backEndUrl, String sessionCookie)
			throws RemoteException {
		this.endPoint = backEndUrl + serviceName;
		try {
			profilesAdminServiceStub = new ProfilesAdminServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("Error on initializing listMetadataServiceStub : " + axisFault.getMessage());
			throw new RemoteException("Error on initializing listMetadataServiceStub : ",
			                          axisFault);
		}
		AuthenticateStub.authenticateStub(sessionCookie, profilesAdminServiceStub);
	}

	public ProfilesAdminServiceClient(String backEndUrl, String userName, String password)
			throws RemoteException {
		this.endPoint = backEndUrl + serviceName;
		try {
			profilesAdminServiceStub = new ProfilesAdminServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("Error on initializing listMetadataServiceStub : " + axisFault.getMessage());
			throw new RemoteException("Error on initializing listMetadataServiceStub : ",
			                          axisFault);
		}
		AuthenticateStub.authenticateStub(userName, password, profilesAdminServiceStub);
	}

	public ProfilesBean getUserProfile(String path) throws Exception {
		try {
			return profilesAdminServiceStub.getUserProfile(path);
		} catch (Exception e) {
			String msg = "Unable to get user profiles ";
			log.error(msg + e.getMessage());
			throw new Exception(msg, e);
		}
	}

	public boolean putUserProfile(String path) throws Exception {
		try {
			return profilesAdminServiceStub.putUserProfile(path);
		} catch (Exception e) {
			String msg = "Unable to put user profiles ";
			log.error(msg + e.getMessage());
			throw new Exception(msg, e);
		}
	}
}
