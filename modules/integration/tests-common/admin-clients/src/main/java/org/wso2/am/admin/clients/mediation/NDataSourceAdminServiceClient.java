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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStubUtil;
import org.wso2.carbon.ndatasource.ui.stub.NDataSourceAdminDataSourceException;
import org.wso2.carbon.ndatasource.ui.stub.NDataSourceAdminStub;
import org.wso2.carbon.ndatasource.ui.stub.core.services.xsd.WSDataSourceInfo;
import org.wso2.carbon.ndatasource.ui.stub.core.services.xsd.WSDataSourceMetaInfo;

public class NDataSourceAdminServiceClient {
	private static final Log log = LogFactory
			.getLog(org.wso2.am.integration.admin.clients.common.NDataSourceAdminServiceClient.class);
	private final String serviceName = "NDataSourceAdmin";
	private NDataSourceAdminStub nDataSourceAdminStub;
	private String endPoint;

	public NDataSourceAdminServiceClient(String backEndUrl, String sessionCookie) throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		try {
			nDataSourceAdminStub = new NDataSourceAdminStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("nDataSourceAdminStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault("nDataSourceAdminStub Initialization fail ", axisFault);
		}
		AuthenticateStubUtil.authenticateStub(sessionCookie, nDataSourceAdminStub);
	}

	public NDataSourceAdminServiceClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		this.endPoint = backEndUrl + serviceName;
		try {
			nDataSourceAdminStub = new NDataSourceAdminStub(endPoint);
		} catch (AxisFault axisFault) {
			log.error("nDataSourceAdminStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault("nDataSourceAdminStub Initialization fail ", axisFault);
		}
		AuthenticateStubUtil.authenticateStub(userName, password, nDataSourceAdminStub);
	}

	public void addDataSource(WSDataSourceMetaInfo dataSourceMetaInfo) throws Exception {
		validateDataSourceMetaInformation(dataSourceMetaInfo);
		if (log.isDebugEnabled()) {
			log.debug("Going to add Datasource :" + dataSourceMetaInfo.getName());
		}
		try {
			nDataSourceAdminStub.addDataSource(dataSourceMetaInfo);
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
	}

	public boolean testDataSourceConnection(WSDataSourceMetaInfo dataSourceMetaInfo)
			throws Exception {
		validateDataSourceMetaInformation(dataSourceMetaInfo);
		if (log.isDebugEnabled()) {
			log.debug("Going test connection of Datasource :" + dataSourceMetaInfo.getName());
		}
		try {
			return nDataSourceAdminStub.testDataSourceConnection(dataSourceMetaInfo);
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return false;
	}

	public void deleteDataSource(String dsName) throws Exception {
		validateName(dsName);
		if (log.isDebugEnabled()) {
			log.debug("Going to delete a Data-source with name : " + dsName);
		}
		try {
			nDataSourceAdminStub.deleteDataSource(dsName);
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
	}

	public WSDataSourceInfo[] getAllDataSources() throws Exception {
		WSDataSourceInfo[] allDataSources = null;
		try {
			allDataSources = nDataSourceAdminStub.getAllDataSources();
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return allDataSources;
	}

	public WSDataSourceInfo getDataSource(String dsName) throws Exception {
		validateName(dsName);
		WSDataSourceInfo wsDataSourceInfo = null;
		try {
			wsDataSourceInfo = nDataSourceAdminStub.getDataSource(dsName);
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return wsDataSourceInfo;
	}

	public WSDataSourceInfo[] getAllDataSourcesForType(String dsType) throws Exception {
		validateType(dsType);
		WSDataSourceInfo[] allDataSources = null;
		try {
			allDataSources = nDataSourceAdminStub.getAllDataSourcesForType(dsType);
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return allDataSources;
	}

	public String[] getDataSourceTypes() throws Exception {
		String[] dataSourceTypes = null;
		try {
			dataSourceTypes = nDataSourceAdminStub.getDataSourceTypes();
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return dataSourceTypes;
	}

	public boolean reloadAllDataSources() throws Exception {
		try {
			return nDataSourceAdminStub.reloadAllDataSources();
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return false;
	}

	public boolean reloadDataSource(String dsName) throws Exception {
		validateName(dsName);
		try {
			return nDataSourceAdminStub.reloadDataSource(dsName);
		} catch (NDataSourceAdminDataSourceException e) {
			if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
				handleException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
			}
			handleException(e.getMessage(), e);
		}
		return false;
	}

	private static void validateDataSourceMetaInformation(WSDataSourceMetaInfo dataSourceMetaInfo) {
		if (dataSourceMetaInfo == null) {
			handleException("WSDataSourceMetaInfo can not be found.");
		}
	}

	private static void validateName(String name) {
		if (name == null || "".equals(name)) {
			handleException("Name is null or empty");
		}
	}

	private static void validateType(String type) {
		if (type == null || "".equals(type)) {
			handleException("Type is null or empty");
		}
	}

	private static void handleException(String msg) {
		log.error(msg);
		throw new IllegalArgumentException(msg);
	}

	private static void handleException(String msg, Exception e) throws Exception {
		throw new Exception(msg, e);
	}
}
