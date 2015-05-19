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

package apim.restful.importexport;


import java.io.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import apim.restful.importexport.utils.APIExportUtil;
import apim.restful.importexport.utils.ArchiveGeneratorUtil;
import apim.restful.importexport.utils.AuthenticatorUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

/**
 * This class provides JAX-RS services for exporting and importing APIs.
 * These services provides functionality for exporting and importing single API at a time.
 */
@Path("/")
public class APIService {

	//log name change
	private static final Log log = LogFactory.getLog(APIService.class);

	/**
	 * This service exports an API from API Manager for a given API ID
	 * Meta information, API icon, documentation, WSDL and sequences are exported
	 * This service generates a zipped archive which contains all the above mentioned resources
	 * for a given API
	 *
	 * @param id ID of the API that needs to be exported
	 * @return Zipped API as the response to the service call
	 */
	@GET
	@Path("/export-api/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.MULTIPART_FORM_DATA)
	//make them query params : provider, api name and version
	public Response exportAPI(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {

		String userName;
		log.info("Retrieving API for API-Id : " + id);
		APIIdentifier apiIdentifier;
		try {
			if (!AuthenticatorUtil.isAuthorizedUser(httpHeaders)) {
				//not an authorized user
				return Response.status(Response.Status.UNAUTHORIZED)
				               .entity("User Authorization " + "Failed")
				               .type(MediaType.APPLICATION_JSON).
						               build();
			}

			userName = AuthenticatorUtil.getAuthenticatedUserName();

			try {
				apiIdentifier = new APIIdentifier(id);
			} catch (APIManagementException e) {
				log.error("Invalid API Identifier " + id, e);

				return Response.status(Response.Status.BAD_REQUEST).entity("Invalid API " +
				                                                           "Identifier" +
				                                                           " " + id)
				               .type(MediaType.APPLICATION_JSON).
								build();
			}

			//Allows to export APIs created only by current tenant
			if (!apiIdentifier.getProviderName().equals(APIUtil.replaceEmailDomainBack(userName))) {
				//not authorized to export requested API
				log.error("Not authorized to " +
				          "export API " + id);
				return Response.status(Response.Status.FORBIDDEN).entity("Not authorized to " +
				                                                         "export API " + id)
				               .type(MediaType.APPLICATION_JSON).
								build();
			}

			String name = id.substring(id.indexOf("_") + 1, id.lastIndexOf("_"));
			String version = id.substring(id.lastIndexOf("_") + 1, id.length());

			//construct location for the exporting API
			String archivePath =
					APIImportExportConstants.BASE_ARCHIVE_PATH.concat("/" + name + "-" + version);

			Response ApiResourceRetrievalResponse =
					APIExportUtil.retrieveApiToExport(apiIdentifier, userName);

			//Retrieve resources : thumbnail, meta information, wsdl, sequences and documents
			// available for the exporting API
			if (!(Response.Status.OK.getStatusCode() == ApiResourceRetrievalResponse.getStatus())) {
				return ApiResourceRetrievalResponse;
			}

			ArchiveGeneratorUtil.archiveDirectory(archivePath);

			log.info("API" + name + "-" + version + " exported successfully");

			File file = new File(archivePath + ".zip");
			Response.ResponseBuilder response = Response.ok((Object) file);
			response.header("Content-Disposition",
			                "attachment; filename=\""+file.getName()+"\"");
			return response.build();

		} catch (APIExportException e) {
			log.error("APIExportException occurred while exporting ", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
			               .entity("Internal Server Error").type(MediaType.APPLICATION_JSON).
							build();
		}

	}



}
