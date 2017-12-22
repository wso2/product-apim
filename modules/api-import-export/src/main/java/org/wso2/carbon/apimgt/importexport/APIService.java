/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.wso2.carbon.apimgt.importexport;


import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.importexport.utils.APIExportUtil;
import org.wso2.carbon.apimgt.importexport.utils.APIImportUtil;
import org.wso2.carbon.apimgt.importexport.utils.ArchiveGeneratorUtil;
import org.wso2.carbon.apimgt.importexport.utils.AuthenticatorUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This class provides JAX-RS services for exporting and importing APIs.
 * These services provides functionality for exporting and importing single API at a time.
 */
@Path("/")
public class APIService {

    private static final Log log = LogFactory.getLog(APIService.class);

    /**
     * This service exports an API from API Manager for a given API ID
     * Meta information, API icon, documentation, WSDL and sequences are exported
     * This service generates a zipped archive which contains all the above mentioned resources
     * for a given API
     *
     * @param name         Name of the API that needs to be exported
     * @param version      Version of the API that needs to be exported
     * @param providerName Provider name of the API that needs to be exported
     * @return Zipped API as the response to the service call
     */
    @GET
    @Path("/export-api")
    @Produces("application/zip")
    public Response exportAPI(@QueryParam("name") String name, @QueryParam("version") String version,
            @QueryParam("provider") String providerName, @Context HttpHeaders httpHeaders) {

        String userName;
        if (name == null || version == null || providerName == null) {
            log.error("Invalid API Information ");

            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid API Information")
                    .type(MediaType.APPLICATION_JSON).build();
        }
        log.info("Retrieving API for API-Id : " + name + "-" + version + "-" + providerName);
        APIIdentifier apiIdentifier;
        boolean isTenantFlowStarted = false;

        try {

            Response authorizationResponse = AuthenticatorUtil.authorizeUser(httpHeaders);
            if (Response.Status.OK.getStatusCode() != authorizationResponse.getStatus()) {
                return authorizationResponse;
            }

            userName = AuthenticatorUtil.getAuthenticatedUserName();
            //provider names with @ signs are only accepted
            String apiDomain = MultitenantUtils.getTenantDomain(providerName);
            String apiRequesterDomain = MultitenantUtils.getTenantDomain(userName);
            //Allows to export APIs created only in current tenant domain
            if (!apiDomain.equals(apiRequesterDomain)) {
                //not authorized to export requested API
                log.error("Not authorized to " +
                        "export API :" + name + "-" + version + "-" + providerName);
                return Response.status(Response.Status.FORBIDDEN).entity("Not authorized to export API :" +
                        name + "-" + version + "-" + providerName).type(MediaType.APPLICATION_JSON).build();
            }

            apiIdentifier = new APIIdentifier(APIUtil.replaceEmailDomain(providerName), name, version);

            //create temp location for storing API data to generate archive
            String currentDirectory = System.getProperty(APIImportExportConstants.TEMP_DIR);
            String createdFolders = File.separator + RandomStringUtils.
                    randomAlphanumeric(APIImportExportConstants.TEMP_FILENAME_LENGTH) + File.separator;
            File exportFolder = new File(currentDirectory + createdFolders);
            APIExportUtil.createDirectory(exportFolder.getPath());
            String archiveBasePath = exportFolder.toString();

            APIExportUtil.setArchiveBasePath(archiveBasePath);

            //Start tenant flow for the entire export process
            if (apiRequesterDomain != null &&
                !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(apiRequesterDomain)) {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(apiRequesterDomain, true);
                isTenantFlowStarted = true;
            }

            Response apiResourceRetrievalResponse = APIExportUtil.retrieveApiToExport(apiIdentifier, userName);

            //Retrieve resources : thumbnail, meta information, wsdl, sequences and documents
            // available for the exporting API
            if (!(Response.Status.OK.getStatusCode() == apiResourceRetrievalResponse.getStatus())) {
                return apiResourceRetrievalResponse;
            }

            ArchiveGeneratorUtil.archiveDirectory(archiveBasePath);

            log.info("API" + name + "-" + version + " exported successfully");

            File file = new File(archiveBasePath + ".zip");
            Response.ResponseBuilder response = Response.ok(file);
            response.header("Content-Disposition", "attachment; filename=\"" + file.getName() +
                    "\"");
            return response.build();

        } catch (APIExportException e) {
            log.error("APIExportException occurred while exporting ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

    }

    /**
     * This is the service which is used to import an API. All relevant API data will be included upon the creation of
     * the API. Depending on the choice of the user, provider of the imported API will be preserved or modified.
     *
     * @param uploadedInputStream uploadedInputStream input stream from the REST request
     * @param defaultProviderStatus     user choice to keep or replace the API provider
     * @param httpHeaders         HTTP headers for the authentication mechanism
     * @return response for the API process
     */
    @POST
    @Path("/import-api")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAPI(@Multipart("file") InputStream uploadedInputStream, @QueryParam("preserveProvider")
    String defaultProviderStatus, @Context HttpHeaders httpHeaders) {

        boolean isProviderPreserved = true;
        boolean isTenantFlowStarted = false;

        //Check if the URL parameter value is specified, otherwise the default value "true" is used
        if (APIImportExportConstants.STATUS_FALSE.equalsIgnoreCase(defaultProviderStatus)) {
            isProviderPreserved = false;
        }

        try {
            Response authorizationResponse = AuthenticatorUtil.authorizeUser(httpHeaders);

            if (Response.Status.OK.getStatusCode() != authorizationResponse.getStatus()) {
                return authorizationResponse;
            }

            String currentUser = AuthenticatorUtil.getAuthenticatedUserName();
            APIImportUtil.initializeProvider(currentUser);

            //Temporary directory is used to create the required folders
            String currentDirectory = System.getProperty(APIImportExportConstants.TEMP_DIR);
            String createdFolders = File.separator +
                    RandomStringUtils.randomAlphanumeric(APIImportExportConstants.TEMP_FILENAME_LENGTH) +
                    File.separator;
            File importFolder = new File(currentDirectory + createdFolders);
            boolean folderCreateStatus = importFolder.mkdirs();

            //API import process starts only if the required folder is created successfully
            if (folderCreateStatus) {
                String uploadFileName = APIImportExportConstants.UPLOAD_FILE_NAME;
                String absolutePath = currentDirectory + createdFolders;
                APIImportUtil.transferFile(uploadedInputStream, uploadFileName, absolutePath);

                String extractedFolderName;
                try {
                    extractedFolderName = APIImportUtil.extractArchive(
                            new File(absolutePath + uploadFileName), absolutePath);
                } catch (APIImportException e) {
                    return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
                }

                String tenantDomain = MultitenantUtils.getTenantDomain(currentUser);
                if (tenantDomain != null &&
                        !org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME
                                .equals(tenantDomain)) {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                    isTenantFlowStarted = true;
                }

                APIImportUtil.importAPI(absolutePath + extractedFolderName, currentUser, isProviderPreserved);

                importFolder.deleteOnExit();
                return Response.status(Status.CREATED).entity("API imported successfully.\n").build();
            } else {
                return Response.serverError().entity("Failed to create temporary directory.\n").build();
            }
        } catch (APIExportException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error in initializing API provider.\n").build();
        } catch (APIImportException e) {
            return Response.serverError().entity(e.getMessage()).build();
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }
}
