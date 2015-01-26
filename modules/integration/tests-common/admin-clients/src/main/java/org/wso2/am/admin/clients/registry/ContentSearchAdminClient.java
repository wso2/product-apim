/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.am.admin.clients.registry;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.registry.indexing.stub.generated.ContentSearchAdminServiceStub;
import org.wso2.carbon.registry.indexing.stub.generated.xsd.SearchResultsBean;

import java.rmi.RemoteException;

public class ContentSearchAdminClient {

	private static final Log log = LogFactory.getLog(SearchAdminServiceClient.class);

	private ContentSearchAdminServiceStub contentSearchAdminServiceStub;

	public ContentSearchAdminClient(String backEndUrl, String username, String password)
			throws AxisFault {
		String serviceName = "ContentSearchAdminService";
		String endPoint = backEndUrl + serviceName;
		contentSearchAdminServiceStub = new ContentSearchAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(username, password, contentSearchAdminServiceStub);

	}

	public ContentSearchAdminClient(String sessionCookie, String backEndUrl)
			throws AxisFault {
		String serviceName = "ContentSearchAdminService";
		String endPoint = backEndUrl + serviceName;
		contentSearchAdminServiceStub = new ContentSearchAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, contentSearchAdminServiceStub);

	}

	public SearchResultsBean getContentSearchResults(String searchQuery) throws RemoteException {

		SearchResultsBean bean;
		try {
			bean = contentSearchAdminServiceStub.getContentSearchResults(searchQuery);
		} catch (RemoteException e) {
			String msg = "Unable o search the contents";
			log.error(msg + e);
			throw new RemoteException(msg, e);
		}

		return bean;
	}
}
