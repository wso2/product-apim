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
import org.wso2.carbon.registry.info.stub.RegistryExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceResourceServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.resource.stub.beans.xsd.*;
import org.wso2.carbon.registry.resource.stub.common.xsd.ResourceData;

import javax.activation.DataHandler;
import java.rmi.RemoteException;

public class ResourceAdminServiceClient {
	private static final Log log = LogFactory.getLog(ResourceAdminServiceClient.class);

	private final String serviceName = "ResourceAdminService";
	private ResourceAdminServiceStub resourceAdminServiceStub;

	private static final String MEDIA_TYPE_WSDL = "application/wsdl+xml";
	private static final String MEDIA_TYPE_WADL = "application/wadl+xml";
	private static final String MEDIA_TYPE_SCHEMA = "application/x-xsd+xml";
	private static final String MEDIA_TYPE_POLICY = "application/policy+xml";
	private static final String MEDIA_TYPE_GOVERNANCE_ARCHIVE =
			"application/vnd.wso2.governance-archive";

	public ResourceAdminServiceClient(String serviceUrl, String sessionCookie) throws AxisFault {
		String endPoint = serviceUrl + serviceName;
		resourceAdminServiceStub = new ResourceAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, resourceAdminServiceStub);
	}

	public ResourceAdminServiceClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		resourceAdminServiceStub = new ResourceAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, resourceAdminServiceStub);
	}

	public boolean addResource(String destinationPath, String mediaType,
	                           String description, DataHandler dh)
			throws ResourceAdminServiceExceptionException, RemoteException {

		if (log.isDebugEnabled()) {
			log.debug("Destination Path :" + destinationPath);
			log.debug("Media Type :" + mediaType);
		}
		return resourceAdminServiceStub
				.addResource(destinationPath, mediaType, description, dh, null, null);
	}

	public ResourceData[] getResource(String destinationPath)
			throws ResourceAdminServiceExceptionException, RemoteException {
		ResourceData[] rs;
		rs = resourceAdminServiceStub.getResourceData(new String[] { destinationPath });
		return rs;
	}

	public CollectionContentBean getCollectionContent(String destinationPath)
			throws RemoteException, ResourceAdminServiceExceptionException {
		CollectionContentBean collectionContentBean;

		try {
			collectionContentBean = resourceAdminServiceStub.getCollectionContent(destinationPath);
		} catch (RemoteException e) {
			log.error("Resource getting failed due to RemoteException : " + e);
			throw new RemoteException("Resource getting failed due to RemoteException :",
			                          e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Resource getting failed due to ResourceAdminServiceExceptionException : ",
			          e);
			throw new ResourceAdminServiceExceptionException(
					"Resource getting failed due to ResourceAdminServiceExceptionException:",
					e);
		}

		return collectionContentBean;
	}

	public boolean deleteResource(String destinationPath)
			throws ResourceAdminServiceExceptionException, RemoteException {

		return resourceAdminServiceStub.delete(destinationPath);
	}

	public void addWSDL(String description, DataHandler dh)
			throws ResourceAdminServiceExceptionException, RemoteException {

		String fileName;
		fileName = dh.getName().substring(dh.getName().lastIndexOf('/') + 1);
		log.debug(fileName);
		resourceAdminServiceStub
				.addResource("/" + fileName, MEDIA_TYPE_WSDL, description, dh, null, null);
	}

	public void addWADL(String description, DataHandler dh)
			throws ResourceAdminServiceExceptionException, RemoteException {

		String fileName;
		fileName = dh.getName().substring(dh.getName().lastIndexOf('/') + 1);
		log.debug(fileName);
		resourceAdminServiceStub
				.addResource("/" + fileName, MEDIA_TYPE_WADL, description, dh, null, null);
	}

	public void addWSDL(String resourceName, String description,
	                    String fetchURL)
			throws ResourceAdminServiceExceptionException, RemoteException {

		resourceAdminServiceStub.importResource("/", resourceName, MEDIA_TYPE_WSDL,
		                                        description, fetchURL, null, null);
	}

	public void addWADL(String resourceName, String description,
	                    String fetchURL)
			throws ResourceAdminServiceExceptionException, RemoteException {

		resourceAdminServiceStub.importResource("/", resourceName, MEDIA_TYPE_WADL,
		                                        description, fetchURL, null, null);
	}

	public void addSchema(String description, DataHandler dh)
			throws ResourceAdminServiceExceptionException, RemoteException {
		String fileName;
		fileName = dh.getName().substring(dh.getName().lastIndexOf('/') + 1);
		resourceAdminServiceStub.addResource("/" + fileName, MEDIA_TYPE_SCHEMA,
		                                     description, dh, null, null);
	}

	public void addSchema(String resourceName, String description,
	                      String fetchURL) throws ResourceAdminServiceExceptionException,
	                                              RemoteException {

		resourceAdminServiceStub.importResource("/", resourceName, MEDIA_TYPE_SCHEMA,
		                                        description, fetchURL, null, null);

	}

	public void addPolicy(String description, DataHandler dh)
			throws ResourceAdminServiceExceptionException, RemoteException {
		String fileName;
		fileName = dh.getName().substring(dh.getName().lastIndexOf('/') + 1);
		resourceAdminServiceStub.addResource("/" + fileName, MEDIA_TYPE_POLICY,
		                                     description, dh, null, null);
	}

	public void addPolicy(String resourceName, String description,
	                      String fetchURL)
			throws ResourceAdminServiceExceptionException, RemoteException {

		resourceAdminServiceStub.importResource("/", resourceName, MEDIA_TYPE_POLICY,
		                                        description, fetchURL, null, null);
	}

	public void uploadArtifact(String description, DataHandler dh)
			throws ResourceAdminServiceExceptionException, RemoteException {
		String fileName;
		fileName = dh.getName().substring(dh.getName().lastIndexOf('/') + 1);
		resourceAdminServiceStub.addResource("/" + fileName, MEDIA_TYPE_GOVERNANCE_ARCHIVE,
		                                     description, dh, null, null);
	}

	public String addCollection(String parentPath, String collectionName,
	                            String mediaType, String description)
			throws ResourceAdminServiceExceptionException, RemoteException {
		return resourceAdminServiceStub
				.addCollection(parentPath, collectionName, mediaType, description);
	}

	public void addSymbolicLink(String parentPath, String name,
	                            String targetPath)
			throws ResourceAdminServiceExceptionException, RemoteException {

		resourceAdminServiceStub.addSymbolicLink(parentPath, name, targetPath);
	}

	public void addTextResource(String parentPath, String fileName,
	                            String mediaType, String description, String content)
			throws RemoteException, ResourceAdminServiceExceptionException {

		resourceAdminServiceStub.addTextResource(parentPath, fileName, mediaType,
		                                         description, content);
	}

	public void addResourcePermission(String pathToAuthorize,
	                                  String roleToAuthorize,
	                                  String actionToAuthorize, String permissionType)
			throws RemoteException, ResourceAdminServiceResourceServiceExceptionException {

		resourceAdminServiceStub.addRolePermission(pathToAuthorize, roleToAuthorize,
		                                           actionToAuthorize, permissionType);

	}

	public String getProperty(String resourcePath, String key)
			throws RemoteException, ResourceAdminServiceExceptionException {

		return resourceAdminServiceStub.getProperty(resourcePath, key);

	}

	public MetadataBean getMetadata(String resourcePath)
			throws RemoteException, ResourceAdminServiceExceptionException {

		return resourceAdminServiceStub.getMetadata(resourcePath);
	}

	public ContentBean getResourceContent(String resourcePath)
			throws RemoteException, ResourceAdminServiceExceptionException {

		return resourceAdminServiceStub.getContentBean(resourcePath);

	}

	public ResourceData[] getResourceData(String resourcePath)
			throws RemoteException, ResourceAdminServiceExceptionException {
		String[] resourceArray = { resourcePath };

		return resourceAdminServiceStub.getResourceData(resourceArray);

	}

	public String getHumanReadableMediaTypes() throws Exception {
		try {
			return resourceAdminServiceStub.getHumanReadableMediaTypes();
		} catch (Exception e) {
			String msg = "get human readable media type error ";
			throw new Exception(msg, e);
		}
	}

	public String getMimeTypeFromHuman(String mediaType) throws Exception {

		try {
			return resourceAdminServiceStub.getMimeTypeFromHuman(mediaType);
		} catch (Exception e) {
			String msg = "get human readable media type error ";
			throw new Exception(msg, e);

		}
	}

	public void updateTextContent(String path, String content)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			resourceAdminServiceStub.updateTextContent(path, content);
		} catch (RemoteException e) {
			log.error("Cannot edit the content of the resource : " + e.getMessage());
			throw new RemoteException("Edit content error : ", e);

		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Cannot edit the content of the resource : " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("Get version error : ", e);

		}
	}

	public void copyResource(String parentPath, String oldResourcePath, String destinationPath,
	                         String targetName)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			resourceAdminServiceStub
					.copyResource(parentPath, oldResourcePath, destinationPath, targetName);
		} catch (RemoteException e) {
			log.error("Copy resource error ");
			throw new RemoteException("Copy resource error ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Copy resource error");
			throw new ResourceAdminServiceExceptionException("Copy resource error", e);
		}
	}

	public void moveResource(String parentPath, String oldResourcePath, String destinationPath,
	                         String targetName)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			resourceAdminServiceStub
					.moveResource(parentPath, oldResourcePath, destinationPath, targetName);
		} catch (RemoteException e) {
			log.error("Move resource error ");
			throw new RemoteException("Copy resource error ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Move resource error");
			throw new ResourceAdminServiceExceptionException("Copy resource error", e);
		}
	}

	public VersionPath[] getVersionPaths(String path)
			throws RemoteException, ResourceAdminServiceExceptionException {
		VersionPath[] versionPaths = null;
		try {
			VersionsBean vb = resourceAdminServiceStub.getVersionsBean(path);
			versionPaths = vb.getVersionPaths();
		} catch (RemoteException e) {
			log.error("No versions for created path : " + e.getMessage());
			throw new RemoteException("Get version error : ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Get version error : " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("Get version error : ", e);
		}
		return versionPaths;
	}

	public VersionsBean getVersionsBean(String path)
			throws RemoteException, ResourceAdminServiceExceptionException {

		try {
			return resourceAdminServiceStub.getVersionsBean(path);
		} catch (RemoteException e) {
			log.error("Get version bean fails: " + e.getMessage());
			throw new RemoteException("Get version bean fails:  ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Get version bean fails:  " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("Get version bean fails:  : ", e);
		}
	}

	public void createVersion(String resourcePath)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			resourceAdminServiceStub.createVersion(resourcePath);
		} catch (RemoteException e) {
			log.error("Create version error : " + e.getMessage());
			throw new RemoteException("Create version error : ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Create version error : " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("Create version error : ", e);
		}
	}

	public void deleteVersionHistory(String path, String snapshotID)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			resourceAdminServiceStub.deleteVersionHistory(path, snapshotID);

		} catch (RemoteException e) {
			log.error("No versions to delete : " + e.getMessage());
			throw new RemoteException("Delete version error : ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Delete version error : " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("Delete version error : ", e);

		}
	}

	public boolean restoreVersion(String path)
			throws RemoteException, ResourceAdminServiceExceptionException {
		boolean status = false;
		try {
			status = resourceAdminServiceStub.restoreVersion(path);

		} catch (RemoteException e) {
			log.error("No versions to restore : " + e.getMessage());
			throw new RemoteException("Restore version error : ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Restore version error : " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("Restore version error : ", e);
		}
		return status;
	}

	public String getTextContent(String path)
			throws RemoteException, ResourceAdminServiceExceptionException {
		String content = null;
		try {
			content = resourceAdminServiceStub.getTextContent(path);
		} catch (RemoteException e) {
			log.error("Unable get content : " + e.getMessage());
			throw new RemoteException("Restore version error : ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("GetTextContent Error : " + e.getMessage());
			throw new ResourceAdminServiceExceptionException("GetTextContent Error :  ", e);
		}
		return content;
	}

	public PermissionBean getPermission(String path) throws Exception {
		try {
			return resourceAdminServiceStub.getPermissions(path);
		} catch (Exception e) {
			log.error("Unable to get permission : " + e.getMessage());
			throw new Exception("Unable to get permission : ", e);
		}
	}

	public void renameResource(String parentPath, String oldResourcePath, String newResourceName)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			resourceAdminServiceStub.renameResource(parentPath, oldResourcePath, newResourceName);
		} catch (RemoteException e) {
			log.error("Rename resource error ");
			throw new RemoteException("Rename resource error ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Rename resource error");
			throw new ResourceAdminServiceExceptionException("Rename resource error", e);
		}
	}

	public boolean addExtension(String name, DataHandler content)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			return resourceAdminServiceStub.addExtension(name, content);
		} catch (RemoteException e) {
			log.error("Add extension error ");
			throw new RemoteException("Rename resource error ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Add Extension error");
			throw new ResourceAdminServiceExceptionException("Rename resource error", e);
		}
	}

	public boolean removeExtension(String name)
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			return resourceAdminServiceStub.removeExtension(name);
		} catch (RemoteException e) {
			log.error("Remove extension error ");
			throw new RemoteException("Remove resource error ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("Remove Extension error");
			throw new ResourceAdminServiceExceptionException("Remove resource error", e);
		}
	}

	public String[] listExtensions()
			throws RemoteException, ResourceAdminServiceExceptionException {
		try {
			return resourceAdminServiceStub.listExtensions();
		} catch (RemoteException e) {
			log.error("List extensions error ");
			throw new RemoteException("List extensions error ", e);
		} catch (ResourceAdminServiceExceptionException e) {
			log.error("List extensions error ");
			throw new ResourceAdminServiceExceptionException("List extensions error ", e);
		}
	}

	public void setDescription(String path, String description)
			throws RemoteException, RegistryExceptionException {
		try {
			resourceAdminServiceStub.setDescription(path, description);
		} catch (RemoteException e) {
			String msg = "Unable set description for the path - " + path;
			log.error(msg, e);
			throw new RemoteException("List extensions error ", e);

		} catch (ResourceAdminServiceExceptionException e) {
			String msg = "Unable set description for the path - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public ContentDownloadBean getContentDownloadBean(String path)
			throws RemoteException, RegistryExceptionException {
		try {
			return resourceAdminServiceStub.getContentDownloadBean(path);
		} catch (RemoteException e) {
			String msg = "Unable to retrieve content download bean - " + path;
			log.error(msg, e);
			throw new RemoteException(msg, e);

		} catch (ResourceAdminServiceExceptionException e) {
			String msg = "Unable to retrieve content download bean - " + path;
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public boolean importResource(String parentPath, String resourceName, String mediaType,
	                              String description, String fetchURL, String symLink)
			throws RemoteException, RegistryExceptionException {
		try {
			return resourceAdminServiceStub
					.importResource(parentPath, resourceName, mediaType, description, fetchURL,
					                symLink, null);
		} catch (RemoteException e) {
			String msg = "Unable to import resource";
			log.error(msg, e);
			throw new RemoteException(msg, e);

		} catch (ResourceAdminServiceExceptionException e) {
			String msg = "Unable to import resource";
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

	public ResourceTreeEntryBean getResourceTreeEntryBean(String resourcePath)
			throws RemoteException, RegistryExceptionException {
		try {
			return resourceAdminServiceStub.getResourceTreeEntry(resourcePath);
		} catch (RemoteException e) {
			String msg = "Unable get resource tree entry";
			log.error(msg, e);
			throw new RemoteException(msg, e);

		} catch (ResourceAdminServiceExceptionException e) {
			String msg = "Unable get resource tree entry";
			log.error(msg, e);
			throw new RegistryExceptionException(msg, e);
		}
	}

}
