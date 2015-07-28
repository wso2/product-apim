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

package apim.restful.importexport.utils;

import apim.restful.importexport.APIExportException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIDefinition;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIStatus;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.definitions.APIDefinitionFromSwagger20;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the util class which consists of all the functions for exporting API
 */
public class APIExportUtil {

    private static final Log log = LogFactory.getLog(APIExportUtil.class);
    private static String archiveBasePath = null;

    private APIExportUtil() {
    }

    /**
     * Set base path where exporting archive should be generated
     *
     * @param path Temporary directory location
     */
    public static void setArchiveBasePath(String path) {
        archiveBasePath = path;
    }

    /**
     * Retrieve API provider
     *
     * @param userName User name
     * @return APIProvider Provider of the supplied user name
     * @throws APIExportException If an error occurs while retrieving the provider
     */
    public static APIProvider getProvider(String userName) throws APIExportException {
        APIProvider provider;
        try {
            provider = APIManagerFactory.getInstance().getAPIProvider(userName);

            if (log.isDebugEnabled()) {
                log.debug("Current provider retrieved successfully");
            }

            return provider;

        } catch (APIManagementException e) {
            log.error("Error while retrieving provider" + e.getMessage());
            throw new APIExportException("Error while retrieving current provider", e);
        }

    }

    /**
     * Retrieve registry for the current tenant
     *
     * @param userName user name of the tenant
     * @return Registry registry of the current tenant
     */
    public static Registry getRegistry(String userName) {
        boolean isTenantFlowStarted = false;
        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        try {
            if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
            }
            Registry registry = CarbonContext.getThreadLocalCarbonContext().
                    getRegistry(RegistryType.SYSTEM_GOVERNANCE);

            if (log.isDebugEnabled()) {
                log.debug("Registry of logged in user retrieved successfully");
            }
            return registry;

        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    /**
     * This method retrieves all meta information and registry resources required for an API to
     * recreate
     *
     * @param apiID    Identifier of the exporting API
     * @param userName User name of the requester
     * @return HttpResponse indicating whether resource retrieval got succeed or not
     * @throws APIExportException If an error occurs while retrieving API related resources
     */
    public static Response retrieveApiToExport(APIIdentifier apiID, String userName) throws APIExportException {

        API apiToReturn;
        String archivePath = archiveBasePath.concat(File.separator + apiID.getApiName() + "-" +
                apiID.getVersion());
        //initializing provider
        APIProvider provider = getProvider(userName);
        //registry for the current user
        Registry registry = APIExportUtil.getRegistry(userName);

        int tenantId = APIUtil.getTenantId(userName);

        //directory creation
        APIExportUtil.createDirectory(archivePath);

        try {
            apiToReturn = provider.getAPI(apiID);
        } catch (APIManagementException e) {
            log.error("Unable to retrieve API", e);
            return Response.status(Response.Status.NOT_FOUND).entity("Unable to retrieve API")
                    .type(MediaType.APPLICATION_JSON).
                            build();
        }

        //export thumbnail
        exportAPIThumbnail(apiID, registry);

        //export documents
        List<Documentation> docList;
        try {
            docList = provider.getAllDocumentation(apiID);
        } catch (APIManagementException e) {
            log.error("Unable to retrieve API Documentation", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Internal Server Error")
                    .type(MediaType.APPLICATION_JSON).build();
        }

        if (!docList.isEmpty()) {
            exportAPIDocumentation(docList, apiID, registry);
        }

        //export wsdl
        String wsdlUrl = apiToReturn.getWsdlUrl();
        if (wsdlUrl != null) {
            exportWSDL(apiID, registry);
        }

        //export sequences
        exportSequences(apiToReturn, apiID, tenantId);

        //set API status to created
        apiToReturn.setStatus(APIStatus.CREATED);

        //export meta information
        exportMetaInformation(apiToReturn, registry);

        return Response.ok().build();

    }

    /**
     * Retrieve thumbnail image for the exporting API and store it in the archive directory
     *
     * @param apiIdentifier ID of the requesting API
     * @param registry      Current tenant registry
     * @throws APIExportException If an error occurs while retrieving image from the registry or
     *                            storing in the archive directory
     */
    private static void exportAPIThumbnail(APIIdentifier apiIdentifier, Registry registry) throws APIExportException {
        String thumbnailUrl = APIConstants.API_IMAGE_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getVersion() + RegistryConstants.PATH_SEPARATOR +
                APIConstants.API_ICON_IMAGE;

        InputStream imageDataStream = null;
        OutputStream outputStream = null;
        String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() +
                "-" + apiIdentifier.getVersion());
        try {
            if (registry.resourceExists(thumbnailUrl)) {
                Resource icon = registry.get(thumbnailUrl);

                imageDataStream = icon.getContentStream();

                String mediaType = icon.getMediaType();
                String extension = getThumbnailFileType(mediaType);

                if (extension != null) {
                    createDirectory(archivePath + File.separator + "Image");

                    outputStream = new FileOutputStream(archivePath + File.separator + "Image" + File.separator +
                            "icon." + extension);

                    IOUtils.copy(imageDataStream, outputStream);

                    if (log.isDebugEnabled()) {
                        log.debug("Thumbnail image retrieved successfully");
                    }
                }
            }
        } catch (IOException e) {
            //Exception is ignored by logging due to the reason that Thumbnail is not essential for
            // an API to be recreated
            log.error("I/O error while writing API Thumbnail to file" + e.getMessage());
        } catch (RegistryException e) {
            log.error("Error while retrieving API Thumbnail " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(imageDataStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Retrieve content type of the thumbnail image for setting the exporting file extension
     *
     * @param mediaType Media type of the thumbnail registry resource
     * @return File extension for the exporting image
     */
    private static String getThumbnailFileType(String mediaType) {
        if (("image/png").equals(mediaType)) {
            return "png";
        } else if (("image/jpg").equals(mediaType)) {
            return "jpg";
        } else if ("image/jpeg".equals(mediaType)) {
            return "jpeg";
        } else if (("image/bmp").equals(mediaType)) {
            return "bmp";
        } else if (("image/gif").equals(mediaType)) {
            return "gif";
        } else {
            //api gets imported without thumbnail
            log.error("Unsupported media type for icon " + mediaType);
        }

        return null;
    }

    /**
     * Retrieve documentation for the exporting API and store it in the archive directory
     * FILE, INLINE and URL documentations are handled
     *
     * @param apiIdentifier ID of the requesting API
     * @param registry      Current tenant registry
     * @param docList       documentation list of the exporting API
     * @throws APIExportException If an error occurs while retrieving documents from the
     *                            registry or storing in the archive directory
     */
    public static void exportAPIDocumentation(List<Documentation> docList, APIIdentifier apiIdentifier,
            Registry registry) throws APIExportException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() + "-" +
                apiIdentifier.getVersion());
        createDirectory(archivePath + File.separator + "Docs");
        InputStream fileInputStream = null;
        OutputStream outputStream = null;
        try {
            for (Documentation doc : docList) {
                String sourceType = doc.getSourceType().name();
                if (Documentation.DocumentSourceType.FILE.toString().equalsIgnoreCase(sourceType)) {
                    String fileName = doc.getFilePath().substring(doc.getFilePath().
                            lastIndexOf(RegistryConstants.PATH_SEPARATOR) + 1);
                    String filePath = APIUtil.getDocumentationFilePath(apiIdentifier, fileName);

                    //check whether resource exists in the registry
                    Resource docFile = registry.get(filePath);
                    String localFilePath = File.separator + "Docs" + File.separator + fileName;
                    outputStream = new FileOutputStream(archivePath + localFilePath);
                    fileInputStream = docFile.getContentStream();

                    IOUtils.copy(fileInputStream, outputStream);

                    doc.setFilePath(localFilePath);

                    if (log.isDebugEnabled()) {
                        log.debug(fileName + " retrieved successfully");
                    }
                }
            }

            String json = gson.toJson(docList);
            writeFile(archivePath + File.separator + "Docs" + File.separator + "docs.json", json);

            if (log.isDebugEnabled()) {
                log.debug("API Documentation retrieved successfully");
            }

        } catch (IOException e) {
            log.error("I/O error while writing API documentation to file" + e.getMessage());
            throw new APIExportException("I/O error while writing API documentation to file", e);
        } catch (RegistryException e) {
            log.error("Error while retrieving documentation " + e.getMessage());
            throw new APIExportException("Error while retrieving documentation", e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Retrieve WSDL for the exporting API and store it in the archive directory
     *
     * @param apiIdentifier ID of the requesting API
     * @param registry      Current tenant registry
     * @throws APIExportException If an error occurs while retrieving WSDL from the registry or
     *                            storing in the archive directory
     */
    public static void exportWSDL(APIIdentifier apiIdentifier, Registry registry) throws APIExportException {

        InputStream wsdlStream = null;
        OutputStream outputStream = null;
        String archivePath = archiveBasePath.concat("/" + apiIdentifier.getApiName() + "-" +
                apiIdentifier.getVersion());

        try {
            String wsdlPath = APIConstants.API_WSDL_RESOURCE_LOCATION +
                    apiIdentifier.getProviderName() + "--" + apiIdentifier.getApiName() +
                    apiIdentifier.getVersion() + ".wsdl";
            if (registry.resourceExists(wsdlPath)) {
                createDirectory(archivePath + File.separator + "WSDL");

                Resource wsdl = registry.get(wsdlPath);

                wsdlStream = wsdl.getContentStream();

                outputStream = new FileOutputStream(archivePath + File.separator + "WSDL" + File.separator +
                        apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion() + ".wsdl");

                IOUtils.copy(wsdlStream, outputStream);

                if (log.isDebugEnabled()) {
                    log.debug("WSDL file retrieved successfully");
                }
            }
        } catch (IOException e) {
            log.error("I/O error while writing WSDL to file" + e.getMessage());
            throw new APIExportException("I/O error while writing WSDL to file", e);
        } catch (RegistryException e) {
            log.error("Error while retrieving WSDL " + e.getMessage());
            throw new APIExportException("Error while retrieving WSDL", e);
        } finally {
            IOUtils.closeQuietly(wsdlStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Retrieve available custom sequences for the exporting API
     *
     * @param api           exporting API
     * @param apiIdentifier ID of the requesting API
     * @throws APIExportException If an error occurs while retrieving sequences from registry
     */
    public static void exportSequences(API api, APIIdentifier apiIdentifier, int tenantId) throws APIExportException {

        Map<String, String> sequences = new HashMap<String, String>();

        if (api.getInSequence() != null) {
            sequences.put(APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN, api.getInSequence());
        }

        if (api.getOutSequence() != null) {
            sequences.put(APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT, api.getOutSequence());
        }

        if (api.getOutSequence() != null) {
            sequences.put(APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT, api.getFaultSequence());
        }

        if (!sequences.isEmpty()) {
            String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() + "-" +
                    apiIdentifier.getVersion());
            createDirectory(archivePath + File.separator + "Sequences");

            try {
                String sequenceName;
                String direction;
                OMElement sequenceConfig;
                for (Map.Entry<String, String> sequence : sequences.entrySet()) {
                    sequenceName = sequence.getValue();
                    direction = sequence.getKey();
                    if (sequenceName != null) {
                        if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN.equalsIgnoreCase(direction)) {
                            sequenceConfig = APIUtil.getCustomSequence(sequenceName, tenantId,
                                    APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN);
                            writeSequenceToFile(sequenceConfig, sequenceName, APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN,
                                    apiIdentifier);
                        } else if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT.equalsIgnoreCase(direction)) {
                            sequenceConfig = APIUtil.getCustomSequence(sequenceName, tenantId,
                                    APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT);
                            writeSequenceToFile(sequenceConfig, sequenceName, APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT,
                                    apiIdentifier);
                        } else {
                            sequenceConfig = APIUtil.getCustomSequence(sequenceName, tenantId,
                                    APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT);
                            writeSequenceToFile(sequenceConfig, sequenceName,
                                    APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT, apiIdentifier);
                        }
                    }
                }
            } catch (APIManagementException e) {
                log.error("Error while retrieving custom sequence" + e.getMessage());
                throw new APIExportException("Error while retrieving custom sequence", e);

            }
        }
    }

    /**
     * Store custom sequences in the archive directory
     *
     * @param sequenceConfig Sequence configuration
     * @param sequenceName   Sequence name
     * @param direction      Direction of the sequence "in", "out" or "fault"
     * @param apiIdentifier  ID of the requesting API
     * @throws APIExportException If an error occurs while serializing XML stream or storing in
     *                            archive directory
     */
    public static void writeSequenceToFile(OMElement sequenceConfig, String sequenceName, String direction,
            APIIdentifier apiIdentifier) throws APIExportException {
        OutputStream outputStream = null;
        String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() + "-" +
                apiIdentifier.getVersion()) + File.separator + "Sequences" + File.separator;

        String pathToExportedSequence = archivePath + direction + "-sequence" + File.separator;

        String exportedSequenceFile = pathToExportedSequence + sequenceName + ".xml";
        try {
            createDirectory(pathToExportedSequence);
            outputStream = new FileOutputStream(exportedSequenceFile);
            sequenceConfig.serialize(outputStream);

            if (log.isDebugEnabled()) {
                log.debug(sequenceName + " retrieved successfully");
            }

        } catch (FileNotFoundException e) {
            log.error("Unable to find file" + e.getMessage());
            throw new APIExportException("Unable to find file: " + exportedSequenceFile, e);
        } catch (XMLStreamException e) {
            log.error("Error while processing XML stream" + e.getMessage());
            throw new APIExportException("Error while processing XML stream", e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Create directory at the given path
     *
     * @param path Path of the directory
     * @throws APIExportException If directory creation failed
     */
    public static void createDirectory(String path) throws APIExportException {
        if (path != null) {
            File file = new File(path);
            if (!file.exists() && !file.mkdirs()) {
                log.error("Error while creating directory : " + path);
                throw new APIExportException("Directory creation failed " + path);
            }
        }
    }

    /**
     * Retrieve meta information of the API to export
     * URL template information are stored in swagger.json definition while rest of the required
     * data are in api.json
     *
     * @param apiToReturn API to be exported
     * @param registry    Current tenant registry
     * @throws APIExportException If an error occurs while exporting meta information
     */
    private static void exportMetaInformation(API apiToReturn, Registry registry) throws APIExportException {
        APIDefinition definitionFromSwagger20 = new APIDefinitionFromSwagger20();
        String archivePath = archiveBasePath.concat(File.separator + apiToReturn.getId().getApiName() + "-" +
                apiToReturn.getId().getVersion());

        createDirectory(archivePath + File.separator + "Meta-information");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String apiInJson = gson.toJson(apiToReturn);
        writeFile(archivePath + File.separator + "Meta-information" + File.separator + "api.json", apiInJson);

        try {
            String swaggerDefinition = definitionFromSwagger20.getAPIDefinition(apiToReturn.getId(), registry);
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(swaggerDefinition).getAsJsonObject();
            String formattedSwaggerJson = gson.toJson(json);
            writeFile(archivePath + File.separator + "Meta-information" + File.separator + "swagger.json",
                    formattedSwaggerJson);

            if (log.isDebugEnabled()) {
                log.debug("Meta information retrieved successfully");
            }

        } catch (APIManagementException e) {
            log.error("Error while retrieving Swagger definition" + e.getMessage());
            throw new APIExportException("Error while retrieving Swagger definition", e);
        }
    }

    /**
     * Write content to file
     *
     * @param path    Location of the file
     * @param content Content to be written
     * @throws APIExportException If an error occurs while writing to file
     */
    private static void writeFile(String path, String content) throws APIExportException {
        FileWriter writer = null;

        try {
            writer = new FileWriter(path);
            IOUtils.copy(new StringReader(content), writer);
        } catch (IOException e) {
            log.error("I/O error while writing to file" + e.getMessage());
            throw new APIExportException("I/O error while writing to file", e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

    }

}
