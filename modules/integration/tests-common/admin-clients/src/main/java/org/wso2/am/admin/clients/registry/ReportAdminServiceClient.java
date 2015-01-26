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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.registry.reporting.stub.ReportingAdminServiceStub;
import org.wso2.carbon.registry.reporting.stub.beans.xsd.ReportConfigurationBean;

import javax.activation.DataHandler;

public class ReportAdminServiceClient {

	private static final Log log =
			LogFactory.getLog(ReportAdminServiceClient.class);

	private ReportingAdminServiceStub reportingAdminServiceStub;
	private final String serviceName = "ReportingAdminService";

	public ReportAdminServiceClient(String backendURL, String sessionCookie) throws AxisFault {
		String endPoint = backendURL + serviceName;
		try {
			reportingAdminServiceStub = new ReportingAdminServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			String err = "Stub initialization fail";
			log.error("Stub initialization fail" + axisFault.getMessage());
			throw new AxisFault(err, axisFault);
		}
		AuthenticateStub.authenticateStub(sessionCookie, reportingAdminServiceStub);
	}

	public ReportAdminServiceClient(String backendURL, String userName, String password)
			throws AxisFault {
		String endPoint = backendURL + serviceName;
		try {
			reportingAdminServiceStub = new ReportingAdminServiceStub(endPoint);
		} catch (AxisFault axisFault) {
			String err = "Stub initialization fail";
			log.error("Stub initialization fail" + axisFault.getMessage());
			throw new AxisFault(err, axisFault);
		}
		AuthenticateStub.authenticateStub(userName, password, reportingAdminServiceStub);
	}

	public void saveReport(ReportConfigurationBean configuration) throws Exception {

		try {
			reportingAdminServiceStub.saveReport(configuration);
		} catch (Exception e) {
			String msg = "Unable to save report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public ReportConfigurationBean getSavedReport(String reportName) throws Exception {

		try {
			return reportingAdminServiceStub.getSavedReport(reportName);
		} catch (Exception e) {
			String msg = "Unable get saved report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public DataHandler getReportBytes(ReportConfigurationBean configuration) throws Exception {
		try {
			return reportingAdminServiceStub.getReportBytes(configuration);
		} catch (Exception e) {
			String msg = "Unable get Report in bytes";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public void deleteSavedReport(String name) throws Exception {
		try {
			reportingAdminServiceStub.deleteSavedReport(name);
		} catch (Exception e) {
			String msg = "Unable to delete saved report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public ReportConfigurationBean[] getSavedReports() throws Exception {
		try {
			return reportingAdminServiceStub.getSavedReports();
		} catch (Exception e) {
			String msg = "Unable to get saved report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public String[] getAttributeNames(String className) throws Exception {
		try {
			return reportingAdminServiceStub.getAttributeNames(className);
		} catch (Exception e) {
			String msg = "Unable to get attribute names";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public String[] getMandatoryAttributeNames(String className) throws Exception {
		try {
			return reportingAdminServiceStub.getMandatoryAttributeNames(className);
		} catch (Exception e) {
			String msg = "Unable to get mandatory attribute names";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public void copySavedReport(String saved, String copy) throws Exception {
		try {
			reportingAdminServiceStub.copySavedReport(saved, copy);
		} catch (Exception e) {
			String msg = "Unable to copy the report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public void scheduleReport(ReportConfigurationBean configuration) throws Exception {
		try {
			reportingAdminServiceStub.scheduleReport(configuration);
		} catch (Exception e) {
			String msg = "Unable to schedule the report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}

	public void stopScheduledReport(String reportName) throws Exception {
		try {
			reportingAdminServiceStub.stopScheduledReport(reportName);
		} catch (Exception e) {
			String msg = "Unable to stop the scheduled report";
			log.error(msg);
			throw new Exception(msg, e);
		}
	}
}