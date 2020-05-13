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
package org.wso2.am.admin.clients.rest.api;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.rest.api.stub.RestApiAdminAPIException;
import org.wso2.carbon.rest.api.stub.RestApiAdminStub;

import java.rmi.RemoteException;

public class RestApiAdminClient {
	private static final Log log = LogFactory.getLog(RestApiAdminClient.class);

	private RestApiAdminStub restApiAdminStub;
	private final String serviceName = "RestApiAdmin";

	public RestApiAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		restApiAdminStub = new RestApiAdminStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, restApiAdminStub);
	}

	public RestApiAdminClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		restApiAdminStub = new RestApiAdminStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, restApiAdminStub);
	}

	public boolean add(OMElement apiData) throws RestApiAdminAPIException, RemoteException {
		return restApiAdminStub.addApiFromString(apiData.toString());
	}

	public boolean deleteApi(String apiName) throws RestApiAdminAPIException, RemoteException {
		return restApiAdminStub.deleteApi(apiName);
	}

	public String[] getApiNames() throws RestApiAdminAPIException, RemoteException {
		return restApiAdminStub.getApiNames();
	}
}
