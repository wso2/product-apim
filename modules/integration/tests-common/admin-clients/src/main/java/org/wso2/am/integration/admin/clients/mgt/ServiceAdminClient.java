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
import org.wso2.carbon.service.mgt.stub.ServiceAdminException;
import org.wso2.carbon.service.mgt.stub.ServiceAdminStub;
import org.wso2.carbon.service.mgt.stub.types.carbon.FaultyService;
import org.wso2.carbon.service.mgt.stub.types.carbon.FaultyServicesWrapper;
import org.wso2.carbon.service.mgt.stub.types.carbon.ServiceMetaData;
import org.wso2.carbon.service.mgt.stub.types.carbon.ServiceMetaDataWrapper;

import java.rmi.RemoteException;

public class ServiceAdminClient {
	private static final Log log = LogFactory.getLog(ServiceAdminClient.class);
	private final String serviceName = "ServiceAdmin";
	private ServiceAdminStub serviceAdminStub;

	public ServiceAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {

		String endPoint = backEndUrl + serviceName;
		serviceAdminStub = new ServiceAdminStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, serviceAdminStub);
	}

	public ServiceAdminClient(String backEndUrl, String userName, String password)
			throws AxisFault {

		String endPoint = backEndUrl + serviceName;
		serviceAdminStub = new ServiceAdminStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, serviceAdminStub);
	}

	public void deleteService(String[] serviceGroup) throws RemoteException {

		serviceAdminStub.deleteServiceGroups(serviceGroup);

	}

	public boolean deleteFaultyService(String archiveName) throws RemoteException {
		try {
			return serviceAdminStub.deleteFaultyServiceGroup(archiveName);
		} catch (RemoteException e) {
			log.error("Faulty service deletion fails", e);
			throw new RemoteException("Faulty service deletion fails", e);
		}
	}

	public boolean deleteFaultyServiceByServiceName(String serviceName) throws RemoteException {
		try {
			return serviceAdminStub
					.deleteFaultyServiceGroup(getFaultyData(serviceName).getArtifact());
		} catch (RemoteException e) {
			log.error("Faulty service deletion fails", e);
			throw new RemoteException("Faulty service deletion fails", e);
		}
	}

	public void deleteAllNonAdminServiceGroups() throws RemoteException {

		serviceAdminStub.deleteAllNonAdminServiceGroups();

	}

	public ServiceMetaDataWrapper listServices(String serviceName)
			throws RemoteException {
		ServiceMetaDataWrapper serviceMetaDataWrapper;
		serviceMetaDataWrapper = serviceAdminStub.listServices("ALL", serviceName, 0);
		serviceAdminStub.getFaultyServiceArchives(0);
		return serviceMetaDataWrapper;
	}

	public ServiceMetaDataWrapper listServices(String serviceName, String filerType)
			throws RemoteException {
		ServiceMetaDataWrapper serviceMetaDataWrapper;
		serviceMetaDataWrapper = serviceAdminStub.listServices(filerType, serviceName, 0);
		serviceAdminStub.getFaultyServiceArchives(0);
		return serviceMetaDataWrapper;
	}

	public FaultyServicesWrapper getFaultyServiceArchives(int pageNumber) throws RemoteException {
		FaultyServicesWrapper faultyServicesWrapper;
		try {
			faultyServicesWrapper = serviceAdminStub.getFaultyServiceArchives(pageNumber);
		} catch (RemoteException e) {
			log.error("Unable to get faulty service Archives", e);
			throw new RemoteException("Unable to get faulty service Archives", e);
		}
		return faultyServicesWrapper;
	}

	public FaultyServicesWrapper listFaultyServices() throws RemoteException {
		FaultyServicesWrapper faultyServicesWrapper;

		faultyServicesWrapper = serviceAdminStub.getFaultyServiceArchives(0);

		return faultyServicesWrapper;
	}

	public boolean isServiceExists(String serviceName)
			throws RemoteException {
		boolean serviceState = false;
		ServiceMetaDataWrapper serviceMetaDataWrapper;
		ServiceMetaData[] serviceMetaDataList;
		serviceMetaDataWrapper = listServices(serviceName);
		serviceMetaDataList = serviceMetaDataWrapper.getServices();
		if (serviceMetaDataList == null || serviceMetaDataList.length == 0) {
			serviceState = false;
		} else {
			for (ServiceMetaData serviceData : serviceMetaDataList) {
				if (serviceData != null && serviceData.getName().equalsIgnoreCase(serviceName)) {
					return true;
				}
			}
		}
		return serviceState;
	}

	public void deleteMatchingServiceByGroup(String serviceFileName)
			throws RemoteException {
		String matchingServiceName = getMatchingServiceName(serviceFileName);
		if (matchingServiceName != null) {
			String serviceGroup[] = { getServiceGroup(matchingServiceName) };
			log.info("Service group name " + serviceGroup[0]);

			serviceAdminStub.deleteServiceGroups(serviceGroup);

		} else {
			log.error("Service group name cannot be null");
		}
	}

	public String deleteAllServicesByType(String type)
			throws RemoteException {
		ServiceMetaDataWrapper serviceMetaDataWrapper;

		serviceMetaDataWrapper = serviceAdminStub.listServices("ALL", null, 0);

		ServiceMetaData[] serviceMetaDataList;
		if (serviceMetaDataWrapper != null) {
			serviceMetaDataList = serviceMetaDataWrapper.getServices();

			String[] serviceGroup;
			if (serviceMetaDataList != null && serviceMetaDataList.length > 0) {

				for (ServiceMetaData serviceData : serviceMetaDataList) {
					if (serviceData.getServiceType().equalsIgnoreCase(type)) {
						serviceGroup = new String[] { serviceData.getServiceGroupName() };
						deleteService(serviceGroup);
					}
				}
			}
		}
		return null;

	}

	public String getMatchingServiceName(String serviceFileName)
			throws RemoteException {

		ServiceMetaDataWrapper serviceMetaDataWrapper;
		serviceMetaDataWrapper = serviceAdminStub.listServices("ALL", serviceFileName, 0);

		ServiceMetaData[] serviceMetaDataList;
		if (serviceMetaDataWrapper != null) {
			serviceMetaDataList = serviceMetaDataWrapper.getServices();
			if (serviceMetaDataList != null && serviceMetaDataList.length > 0) {

				for (ServiceMetaData serviceData : serviceMetaDataList) {
					if (serviceData != null && serviceData.getName().contains(serviceFileName)) {
						return serviceData.getName();
					}
				}
			}
		}
		return null;
	}

	public String getServiceGroup(String serviceName) throws RemoteException {
		ServiceMetaDataWrapper serviceMetaDataWrapper;
		ServiceMetaData[] serviceMetaDataList;
		serviceMetaDataWrapper = listServices(serviceName);
		serviceMetaDataList = serviceMetaDataWrapper.getServices();
		if (serviceMetaDataList != null && serviceMetaDataList.length > 0) {
			for (ServiceMetaData serviceData : serviceMetaDataList) {
				if (serviceData != null && serviceData.getName().equalsIgnoreCase(serviceName)) {
					return serviceData.getServiceGroupName();
				}
			}
		}
		return null;
	}

	public boolean isServiceFaulty(String serviceName) throws RemoteException {
		boolean serviceState = false;
		FaultyServicesWrapper faultyServicesWrapper;
		FaultyService[] faultyServiceList;
		faultyServicesWrapper = listFaultyServices();
		if (faultyServicesWrapper != null) {
			faultyServiceList = faultyServicesWrapper.getFaultyServices();
			if (faultyServiceList == null || faultyServiceList.length == 0) {
				serviceState = false;
			} else {
				for (FaultyService faultyServiceData : faultyServiceList) {
					if (faultyServiceData != null &&
					    faultyServiceData.getServiceName().equalsIgnoreCase(serviceName)) {
						return true;
					}
				}
			}
		}
		return serviceState;
	}

	public FaultyService getFaultyData(String serviceName) throws RemoteException {
		FaultyService faultyService = null;
		FaultyServicesWrapper faultyServicesWrapper;
		FaultyService[] faultyServiceList;
		faultyServicesWrapper = listFaultyServices();
		if (faultyServicesWrapper != null) {
			faultyServiceList = faultyServicesWrapper.getFaultyServices();
			if (faultyServiceList == null || faultyServiceList.length == 0) {
				throw new RuntimeException("Service not found in faulty service list");
			} else {
				for (FaultyService faultyServiceData : faultyServiceList) {
					if (faultyServiceData != null &&
					    faultyServiceData.getServiceName().equalsIgnoreCase(serviceName)) {
						faultyService = faultyServiceData;
					}
				}
			}
		}
		if (faultyService == null) {
			throw new RuntimeException("Service not found in faulty service list " + faultyService);
		}
		return faultyService;
	}

	public ServiceMetaData getServicesData(String serviceName)
			throws ServiceAdminException, RemoteException {

		return serviceAdminStub.getServiceData(serviceName);

	}

	public void startService(String serviceName) throws ServiceAdminException, RemoteException {

		serviceAdminStub.startService(serviceName);
		log.info("Service Started");

	}

	public void stopService(String serviceName) throws ServiceAdminException, RemoteException {

		serviceAdminStub.stopService(serviceName);
		log.info("Service Stopped");

	}

	/**
	 * @param serviceName
	 * @return
	 * @throws ServiceAdminException
	 * @throws java.rmi.RemoteException
	 */
	public String[] getExposedTransports(String serviceName)
			throws ServiceAdminException, RemoteException {

		return serviceAdminStub.getExposedTransports(serviceName);

	}

}
