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

package org.wso2.carbon.apimgt.importexport.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.apache.axiom.om.OMElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.carbon.apimgt.api.APIDefinition;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.dto.CertificateMetadataDTO;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.definitions.APIDefinitionFromOpenAPISpec;
import org.wso2.carbon.apimgt.impl.dto.UserRegistrationConfigDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.SelfSignUpUtil;
import org.wso2.carbon.apimgt.importexport.APIExportException;
import org.wso2.carbon.apimgt.importexport.APIImportExportConstants;
import org.wso2.carbon.apimgt.importexport.CertificateDetail;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.governance.lcm.util.CommonUtil;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

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

            //If user is authorized, add default life cycle for the user and load its
            // tenant registry.
            int tenantId = APIUtil.getTenantId(userName);

            CarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            RegistryService registryService =
                    (RegistryService) carbonContext.getOSGiService(RegistryService.class, null);

            CommonUtil.addDefaultLifecyclesIfNotAvailable(registryService.getConfigSystemRegistry(tenantId),
                                                          CommonUtil.getRootSystemRegistry(tenantId));

            return provider;

        } catch (APIManagementException e) {
            String errorMessage = "Error while retrieving provider";
            log.error( errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (XMLStreamException e) {
            String errorMessage = "Error while loading logged in user's tenant registry";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (FileNotFoundException e) {
            String errorMessage = "Error while loading logged in user's tenant registry";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
            String errorMessage = "Error while loading logged in user's tenant registry";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        }
    }

    /**
     * Retrieve registry for the current tenant
     *
     * @return Registry registry of the current tenant
     */
    public static Registry getRegistry() {

        Registry registry = CarbonContext.getThreadLocalCarbonContext().
                getRegistry(RegistryType.SYSTEM_GOVERNANCE);

        if (log.isDebugEnabled()) {
            log.debug("Registry of logged in user retrieved successfully");
        }
        return registry;
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
    public static Response retrieveApiToExport(APIIdentifier apiID, String userName, boolean isStatusPreserved) throws APIExportException {

        API apiToReturn;
        String archivePath = archiveBasePath.concat(File.separator + apiID.getApiName() + "-" +
                apiID.getVersion());
        //initializing provider
        APIProvider provider = getProvider(userName);
        //registry for the current user
        Registry registry = APIExportUtil.getRegistry();

        int tenantId = APIUtil.getTenantId(userName);

        //directory creation
        APIExportUtil.createDirectory(archivePath);

        try {
            apiToReturn = provider.getAPI(apiID);
        } catch (APIManagementException e) {
            String errorMessage = "Unable to retrieve API";
            log.error(errorMessage, e);
            return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).type(MediaType.APPLICATION_JSON).
                    build();
        }

        //export thumbnail
        exportAPIThumbnail(apiID, registry);

        //export documents
        List<Documentation> docList;
        try {
            docList = provider.getAllDocumentation(apiID, userName);
        } catch (APIManagementException e) {
            String errorMessage = "Unable to retrieve API Documentation";
            log.error(errorMessage, e);
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
        exportSequences(apiToReturn, apiID, registry);

        //set API status to created if status is not preserved
        if (!isStatusPreserved) {
            apiToReturn.setStatus(APIConstants.CREATED);
        }

        // export certificates
        exportEndpointCertificates(apiToReturn, tenantId, provider);

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
                '-' + apiIdentifier.getVersion());
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
            log.error("I/O error while writing API Thumbnail to file", e);
        } catch (RegistryException e) {
            log.error("Error while retrieving API Thumbnail ", e);
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
                    String localDirectoryPath = File.separator + APIImportExportConstants.DOCUMENT_DIRECTORY +
                                                File.separator + APIImportExportConstants.FILE_DOCUMENT_DIRECTORY;
                    createDirectory(archivePath + File.separator + localDirectoryPath);
                    String localFilePath = localDirectoryPath+ File.separator + fileName;
                    outputStream = new FileOutputStream(archivePath + localFilePath);
                    fileInputStream = docFile.getContentStream();

                    IOUtils.copy(fileInputStream, outputStream);

                    doc.setFilePath(fileName);

                    if (log.isDebugEnabled()) {
                        log.debug(fileName + " retrieved successfully");
                    }
                } else if (Documentation.DocumentSourceType.INLINE.toString().equalsIgnoreCase
                        (sourceType)) {
                    createDirectory(archivePath + File.separator + APIImportExportConstants.DOCUMENT_DIRECTORY
                                    + File.separator + APIImportExportConstants.INLINE_DOCUMENT_DIRECTORY);
                    String contentPath = APIUtil.getAPIDocPath(apiIdentifier) + RegistryConstants.PATH_SEPARATOR
                                         + APIImportExportConstants.INLINE_DOC_CONTENT_REGISTRY_DIRECTORY
                                         + RegistryConstants.PATH_SEPARATOR + doc.getName();
                    Resource docFile = registry.get(contentPath);
                    String localFilePath = File.separator + APIImportExportConstants.DOCUMENT_DIRECTORY
                                           + File.separator + APIImportExportConstants.INLINE_DOCUMENT_DIRECTORY
                                           + File.separator + doc.getName();
                    outputStream = new FileOutputStream(archivePath + localFilePath);
                    fileInputStream = docFile.getContentStream();

                    IOUtils.copy(fileInputStream, outputStream);
                }
            }

            String json = gson.toJson(docList);
            writeFile(archivePath + APIImportExportConstants.DOCUMENT_FILE_LOCATION, json);

            if (log.isDebugEnabled()) {
                log.debug("API Documentation retrieved successfully");
            }

        } catch (IOException e) {
            String errorMessage = "I/O error while writing API documentation to file";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (RegistryException e) {
            String errorMessage = "Error while retrieving documentation ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
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
            String errorMessage = "I/O error while writing WSDL to file";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (RegistryException e) {
            String errorMessage = "Error while retrieving WSDL ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(wsdlStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Retrieve available custom sequences and API specific sequences for API export
     *
     * @param api           exporting API
     * @param apiIdentifier ID of the requesting API
     * @param registry      current tenant registry
     * @throws APIExportException If an error occurs while exporting sequences
     */
    public static void exportSequences(API api, APIIdentifier apiIdentifier, Registry registry)
        throws APIExportException {

        Map<String, String> sequences = new HashMap<String, String>();

        if (api.getInSequence() != null) {
            sequences.put(APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN, api.getInSequence());
        }

        if (api.getOutSequence() != null) {
            sequences.put(APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT, api.getOutSequence());
        }

        if (api.getFaultSequence() != null) {
            sequences.put(APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT, api.getFaultSequence());
        }

        if (!sequences.isEmpty()) {
            String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() + "-" +
                    apiIdentifier.getVersion());
            createDirectory(archivePath + File.separator + "Sequences");

            String sequenceName;
            String direction;
            for (Map.Entry<String, String> sequence : sequences.entrySet()) {
                sequenceName = sequence.getValue();
                direction = sequence.getKey();
                AbstractMap.SimpleEntry<String, OMElement> sequenceDetails;
                if (sequenceName != null) {
                    if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN.equalsIgnoreCase(direction)) {
                        sequenceDetails = getCustomSequence(sequenceName, APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN,
                                                            registry);
                        if (sequenceDetails == null) {
                            sequenceDetails = getAPISpecificSequence(api.getId(),sequenceName, APIConstants
                                    .API_CUSTOM_SEQUENCE_TYPE_IN, registry);
                            writeAPISpecificSequenceToFile(sequenceDetails, APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN,
                                                           apiIdentifier);
                        } else {
                            writeSequenceToFile(sequenceDetails, APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN,
                                                apiIdentifier);
                        }

                    } else if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT.equalsIgnoreCase(direction)) {
                        sequenceDetails = getCustomSequence(sequenceName, APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT,
                                                            registry);
                        if (sequenceDetails == null) {
                            sequenceDetails = getAPISpecificSequence(api.getId(),sequenceName, APIConstants
                                    .API_CUSTOM_SEQUENCE_TYPE_OUT, registry);
                            writeAPISpecificSequenceToFile(sequenceDetails, APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT,
                                                           apiIdentifier);
                        } else {
                            writeSequenceToFile(sequenceDetails, APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT,
                                                apiIdentifier);
                        }
                    } else {
                        sequenceDetails = getCustomSequence(sequenceName, APIConstants
                                .API_CUSTOM_SEQUENCE_TYPE_FAULT, registry);

                        if (sequenceDetails == null) {
                            sequenceDetails = getAPISpecificSequence(api.getId(), sequenceName, APIConstants
                                    .API_CUSTOM_SEQUENCE_TYPE_FAULT, registry);
                            writeAPISpecificSequenceToFile(sequenceDetails, APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT,
                                                           apiIdentifier);
                        } else {
                            writeSequenceToFile(sequenceDetails, APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT,
                                                apiIdentifier);
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieve custom sequence details from the registry
     *
     * @param sequenceName Name of the sequence
     * @param type         Sequence type
     * @param registry     Current tenant registry
     * @return Registry resource name of the sequence and its content
     * @throws APIExportException If an error occurs while retrieving registry elements
     */
    private static AbstractMap.SimpleEntry<String, OMElement> getCustomSequence(String sequenceName,
        String type, Registry registry) throws APIExportException {
        AbstractMap.SimpleEntry<String, OMElement> sequenceDetails;

        org.wso2.carbon.registry.api.Collection seqCollection = null;

        try {
            if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN.equals(type)) {
                seqCollection = (org.wso2.carbon.registry.api.Collection)
                                registry.get(APIConstants.API_CUSTOM_INSEQUENCE_LOCATION);
            } else if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT.equals(type)) {
                seqCollection = (org.wso2.carbon.registry.api.Collection)
                                registry.get(APIConstants.API_CUSTOM_OUTSEQUENCE_LOCATION);
            } else
                if (APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT.equals(type)) {
                    seqCollection = (org.wso2.carbon.registry.api.Collection)
                                    registry.get(APIConstants.API_CUSTOM_FAULTSEQUENCE_LOCATION);
            }

            if (seqCollection != null) {
                String[] childPaths = seqCollection.getChildren();

                for (String childPath : childPaths) {
                    Resource sequence = registry.get(childPath);
                    OMElement seqElment = APIUtil.buildOMElement(sequence.getContentStream());
                    if (sequenceName.equals(seqElment.getAttributeValue(new QName("name")))) {
                        String sequenceFileName = sequence.getPath().substring(sequence.getPath().
                                lastIndexOf(RegistryConstants.PATH_SEPARATOR));
                        sequenceDetails = new AbstractMap.SimpleEntry<String, OMElement>(sequenceFileName, seqElment);
                        return sequenceDetails;
                    }
                }
            }

        } catch (RegistryException e) {
            String errorMessage = "Error while retrieving sequence from the registry ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (Exception e ) { //APIUtil.buildOMElement() throws a generic exception
            String errorMessage = "Error while reading sequence content ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        }

        return null;

    }

    /**
     * Retrieve API Specific sequence details from the registry
     *
     * @param sequenceName Name of the sequence
     * @param type         Sequence type
     * @param registry     Current tenant registry
     * @return Registry resource name of the sequence and its content
     * @throws APIExportException If an error occurs while retrieving registry elements
     */
    private static AbstractMap.SimpleEntry<String, OMElement> getAPISpecificSequence(APIIdentifier api,String sequenceName, String type, Registry registry) throws APIExportException {
        AbstractMap.SimpleEntry<String, OMElement> sequenceDetails;

        org.wso2.carbon.registry.api.Collection seqCollection;

        String regPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR + api
                .getProviderName() + RegistryConstants.PATH_SEPARATOR + api.getApiName() +
                         RegistryConstants.PATH_SEPARATOR + api.getVersion() + RegistryConstants.PATH_SEPARATOR + type;

        try {
            seqCollection = (org.wso2.carbon.registry.api.Collection) registry.get(regPath);

            if (seqCollection != null) {
                String[] childPaths = seqCollection.getChildren();

                for (String childPath : childPaths) {
                    Resource sequence = registry.get(childPath);
                    OMElement seqElment = APIUtil.buildOMElement(sequence.getContentStream());
                    if (sequenceName.equals(seqElment.getAttributeValue(new QName("name")))) {
                        String sequenceFileName = sequence.getPath().
                                substring(sequence.getPath().lastIndexOf(RegistryConstants.PATH_SEPARATOR));
                        sequenceDetails = new AbstractMap.SimpleEntry<String, OMElement>(sequenceFileName, seqElment);
                        return sequenceDetails;
                    }
                }
            }

        } catch (RegistryException e) {
            String errorMessage = "Error while retrieving sequence from the registry ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (Exception e ) { //APIUtil.buildOMElement() throws a generic exception
            String errorMessage = "Error while reading sequence content ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        }

        return null;

    }


    /**
     * Store custom sequences in the archive directory
     *
     * @param sequenceDetails   Details of the sequence
     * @param direction      Direction of the sequence "in", "out" or "fault"
     * @param apiIdentifier  ID of the requesting API
     * @throws APIExportException If an error occurs while serializing XML stream or storing in
     *                            archive directory
     */
    private static void writeSequenceToFile(AbstractMap.SimpleEntry<String, OMElement> sequenceDetails,
                                            String direction,
                                            APIIdentifier apiIdentifier)
            throws APIExportException {
        OutputStream outputStream = null;
        String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() + "-" +
                apiIdentifier.getVersion()) + File.separator + "Sequences" + File.separator;

        String pathToExportedSequence = archivePath + direction + "-sequence" + File.separator;
        String sequenceFileName = sequenceDetails.getKey();
        OMElement sequenceConfig = sequenceDetails.getValue();
        String exportedSequenceFile = pathToExportedSequence + sequenceFileName;
        try {
            createDirectory(pathToExportedSequence);
            outputStream = new FileOutputStream(exportedSequenceFile);
            sequenceConfig.serialize(outputStream);

            if (log.isDebugEnabled()) {
                log.debug(sequenceFileName + " retrieved successfully");
            }

        } catch (FileNotFoundException e) {
            String errorMessage = "Unable to find file: " + exportedSequenceFile;
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (XMLStreamException e) {
            String errorMessage = "Error while processing XML stream ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Store API Specific sequences in the archive directory
     *
     * @param sequenceDetails   Details of the sequence
     * @param direction      Direction of the sequence "in", "out" or "fault"
     * @param apiIdentifier  ID of the requesting API
     * @throws APIExportException If an error occurs while serializing XML stream or storing in
     *                            archive directory
     */
    private static void writeAPISpecificSequenceToFile(AbstractMap.SimpleEntry<String, OMElement> sequenceDetails,
                                                       String direction,
                                                       APIIdentifier apiIdentifier)
            throws APIExportException {
        OutputStream outputStream = null;
        String archivePath = archiveBasePath.concat(File.separator + apiIdentifier.getApiName() + "-" + apiIdentifier
                .getVersion()) + File.separator + "Sequences" + File.separator;

        String pathToExportedSequence = archivePath + direction + "-sequence" + File.separator + "Custom";
        String sequenceFileName = sequenceDetails.getKey();
        OMElement sequenceConfig = sequenceDetails.getValue();
        String exportedSequenceFile = pathToExportedSequence + sequenceFileName;
        try {
            createDirectory(pathToExportedSequence);
            outputStream = new FileOutputStream(exportedSequenceFile);
            sequenceConfig.serialize(outputStream);

            if (log.isDebugEnabled()) {
                log.debug(sequenceFileName + " retrieved successfully");
            }

        } catch (FileNotFoundException e) {
            String errorMessage = "Unable to find file: " + exportedSequenceFile;
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } catch (XMLStreamException e) {
            String errorMessage = "Error while processing XML stream ";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
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
                String errorMessage = "Error while creating directory : " + path;
                log.error(errorMessage);
                throw new APIExportException(errorMessage);
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
        APIDefinition definitionFromOpenAPISpec = new APIDefinitionFromOpenAPISpec();
        String archivePath = archiveBasePath.concat(File.separator + apiToReturn.getId().getApiName() + "-" +
                apiToReturn.getId().getVersion());

        createDirectory(archivePath + File.separator + "Meta-information");
        //Remove unnecessary data from exported Api
        cleanApiDataToExport(apiToReturn);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String apiInJson = gson.toJson(apiToReturn);
        writeFile(archivePath + File.separator + "Meta-information" + File.separator + "api.json", apiInJson);

        try {
            //If a web socket API is exported, it does not contain a swagger file.
            //Therefore swagger export is only required for REST or SOAP based APIs
            if (!APIConstants.APIType.WS.toString().equalsIgnoreCase(apiToReturn.getType())) {
                String swaggerDefinition = definitionFromOpenAPISpec.getAPIDefinition(apiToReturn.getId(), registry);
                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(swaggerDefinition).getAsJsonObject();
                String formattedSwaggerJson = gson.toJson(json);
                writeFile(archivePath + File.separator + "Meta-information" + File.separator + "swagger.json",
                          formattedSwaggerJson);

                if (log.isDebugEnabled()) {
                    log.debug("Meta information retrieved successfully");
                }
            }
        } catch (APIManagementException e) {
            String errorMessage = "Error while retrieving Swagger definition";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        }
    }

    /**
     * Clean api by removing unnecessary details
     *
     * @param api api to be exported
     */
    private static void cleanApiDataToExport(API api) {
        // Thumbnail will be set according to the importing environment. Therefore current URL is removed
        api.setThumbnailUrl(null);
        // WSDL file path will be set according to the importing environment. Therefore current path is removed
        api.setWsdlUrl(null);
        // Swagger.json contains complete details about scopes and URI templates. Therefore scope and URI template
        // details are removed from api.json
        api.setScopes(new TreeSet<Scope>());
        //api.setUriTemplates(new TreeSet<URITemplate>());
        // Secure endpoint password is removed, as it causes security issues. When importing need to add it manually, if Secure Endpoint is enabled.
        if (api.getEndpointUTPassword() != null) {
            api.setEndpointUTPassword("");
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
            String errorMessage = "I/O error while writing to file";
            log.error(errorMessage, e);
            throw new APIExportException(errorMessage, e);
        } finally {
            IOUtils.closeQuietly(writer);
        }

    }

    /**
     * Checks whether the request is violating cross tenant permission policies. Cross tenant resource access is allowed
     * only for the super tenant admin user, only if the server is started with 'migrationMode=true' system property set.
     *
     * @param apiDomain Tenant domain of the API's provider
     * @param username  Logged in user name
     * @return Whether cross tenant access policies violated
     * @throws APIExportException
     */
    public static Boolean isCrossTenantAccessPermissionsViolated(String apiDomain, String username) throws APIExportException {
        String resourceRquesterDomain = MultitenantUtils.getTenantDomain(username);
        if (resourceRquesterDomain.equals(apiDomain)) {
            return false;
        }
        String superAdminRole;
        try {
            superAdminRole = ServiceReferenceHolder.getInstance().getRealmService().
                    getTenantUserRealm(MultitenantConstants.SUPER_TENANT_ID).getRealmConfiguration().getAdminRoleName();
        } catch (UserStoreException e) {
            String errorMsg = "Error in getting super admin role name";
            throw new APIExportException(errorMsg, e);
        }

        //check whether logged in user is a super tenant user
        String superTenantDomain;
        try {
            superTenantDomain = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager().getSuperTenantDomain();
        } catch (UserStoreException e) {
            String errorMsg = "Error in getting the super tenant domain";
            throw new APIExportException(errorMsg, e);
        }
        boolean isSuperTenantUser = resourceRquesterDomain.equals(superTenantDomain);

        if(!isSuperTenantUser) {
            return true;
        }
        //check whether the user has super tenant admin role
        boolean isSuperAdminRoleNameExist;
        try {
            isSuperAdminRoleNameExist = isUserInRole(username, superAdminRole);
        } catch (UserStoreException e) {
            String errorMsg = "Error in checking whether the user" + username + " has admin role";
            throw new APIExportException(errorMsg, e);
        } catch (APIManagementException e) {
            String errorMsg = "Error in checking whether the user" + username + " has admin role";
            throw new APIExportException(errorMsg, e);
        }
        return !isSuperAdminRoleNameExist;
    }

    /**
     * Check whether the user has the given role
     *
     * @throws UserStoreException
     * @throws APIManagementException
     */
    public static boolean isUserInRole(String user, String role) throws UserStoreException, APIManagementException {
        String tenantDomain = MultitenantUtils.getTenantDomain(APIUtil.replaceEmailDomainBack(user));
        UserRegistrationConfigDTO signupConfig = SelfSignUpUtil.getSignupConfiguration(tenantDomain);
        user = SelfSignUpUtil.getDomainSpecificUserName(user, signupConfig);
        String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(user);
        RealmService realmService = ServiceReferenceHolder.getInstance().getRealmService();
        int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                .getTenantId(tenantDomain);
        UserRealm realm = (UserRealm) realmService.getTenantUserRealm(tenantId);
        UserStoreManager manager = realm.getUserStoreManager();
        AbstractUserStoreManager abstractManager = (AbstractUserStoreManager) manager;
        return abstractManager.isUserInRole(tenantAwareUserName, role);
    }

    /**
     *export endpoint certificates
     *
     * @param api API to be exported
     * @param tenantid tenant id of the user
     * @param apiprovider api Provider
     * @throws APIExportException
     */
    private static void exportEndpointCertificates(API api, int tenantid, APIProvider apiprovider)
            throws APIExportException {
        String archivePath = archiveBasePath.concat(File.separator + api.getId().getApiName() + "-" +
                api.getId().getVersion());
        createDirectory(archivePath + File.separator + APIImportExportConstants.META_INFO_DIRECTORY);
        JSONObject endpointConfig = null;
        JSONTokener tokener = new JSONTokener(api.getEndpointConfig());
        List<CertificateMetadataDTO> certificateMetadataDTOS;
        List<String> productionHostNames;
        List<String> sandboxEndpoints;
        Set<String> uniqueHostNames = new HashSet<String>();
        List<CertificateDetail> endpointCertificatesDetails = new ArrayList<CertificateDetail>();
        try {
            endpointConfig = new JSONObject(tokener);
            productionHostNames = getHostNames(endpointConfig, "production_endpoints", api.getId().getApiName());
            sandboxEndpoints = getHostNames(endpointConfig, "sandbox_endpoints", api.getId().getApiName());
            uniqueHostNames.addAll(productionHostNames); // Remove duplicate and append result
            uniqueHostNames.addAll(sandboxEndpoints);
            for (String hostname : uniqueHostNames) {
                List<CertificateDetail> list = getCertificateContentAndMetaData(tenantid, hostname, apiprovider);
                endpointCertificatesDetails.addAll(list);
            }
            if (endpointCertificatesDetails.size() > 0) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String element = gson.toJson(endpointCertificatesDetails,
                        new TypeToken<ArrayList<CertificateDetail>>() {
                        }.getType());
                writeFile(archivePath + File.separator + APIImportExportConstants.META_INFO_DIRECTORY +
                                File.separator + APIImportExportConstants.ENDPOINTS_CERTIFICATE_FILE,
                        element);
            }
        } catch (JSONException e) {
            String errorMsg = "Error in converting Endpoint config to JSON object in API [" + api.getId().getApiName() + "]" ;
            throw new APIExportException(errorMsg, e);
        }
    }

    /**
     * get the hostname from a given endpoint url
     *
     * @param url url of the endpoint
     * @return host address of the endpoint
     */
    private static String getHostAddress(URL url){
        String host = url.getHost();
        String protocol = url.getProtocol();
        int port = url.getPort();
        String address = null;
        if (port != -1){
            address = protocol + "://" + host + ":" + port;
        } else {
            address = protocol + "://" + host;
        }
        return address;
    }

    /**
     * Get hostname list from endpoint config
     *
     * @param endpointConfig JSON converted endpoint config
     * @param type end point type - production/sandbox
     * @return list of hostnames
     * @throws APIExportException
     */
    private static List<String> getHostNames(JSONObject endpointConfig, String type, String apiName) throws APIExportException {
        List<String> hostNames = new ArrayList<String>();
        List<String> urls = new ArrayList<String>();
        if (endpointConfig != null) {
            try {
                Object item = null;
                item = endpointConfig.get(type);
                if (item instanceof JSONArray) {
                    JSONArray endpointsJSON = new JSONArray(endpointConfig.getJSONArray(type).toString());
                    for (int i = 0; i < endpointsJSON.length(); i++) {
                        try {
                            String urlvalue = endpointsJSON.getJSONObject(i).get("url").toString();
                            urls.add(urlvalue);

                        } catch (JSONException ex) {
                            log.error("Endpoint URL extraction from endpoints JSON objectfailed in API[" +apiName + "]", ex);
                            continue;
                        }
                    }
                } else if (item instanceof JSONObject) {
                    JSONObject endpointJSON = new JSONObject(endpointConfig.getJSONObject(type).toString());
                    try {
                        String urlvalue = endpointJSON.get("url").toString();
                        urls.add(urlvalue);
                    } catch (JSONException ex) {
                        log.error("Endpoint URL extraction from endpoint JSON object failed in API[" +apiName + "]", ex);
                    }
                }
                for (String url : urls) {
                    try {
                        URL urlObj = new URL(url);
                        String address = getHostAddress(urlObj);
                        hostNames.add(address);
                    } catch (MalformedURLException e) {
                        log.error("URL object creation from extracted endpoint URL [" + url + "] failed in API [" + apiName + "]");
                        continue;
                    }
                }
            } catch (JSONException ex) {
                log.info("Endpoint type [" + type + "] not found in API [" + apiName + "]");
            }
        }
        return hostNames;
    }

    /**
     * get Certificate MetaData and Certificate detail and build JSON list
     *
     * @param tenantid tenant id of the user
     * @param hostname hostname of the endpoint
     * @param apiProvider api Provider
     * @return list of certificate detail JSON objects
     * @throws APIExportException
     */
    private static List<CertificateDetail> getCertificateContentAndMetaData(int tenantid, String hostname,
                                                                            APIProvider apiProvider)
            throws APIExportException {
        List<CertificateDetail> certificateDetails = new ArrayList<CertificateDetail>();
        List<CertificateMetadataDTO> certificateMetadataDTOS;
        try {
            certificateMetadataDTOS = apiProvider.searchCertificates(tenantid, null, hostname);

        } catch (APIManagementException e) {
            String errorMsg = "Error retrieving certificate meta data. tenantId [" + tenantid + "] hostname ["
                    + hostname + "]";
            throw new APIExportException(errorMsg, e);
        }
        ByteArrayInputStream certificate = null;
        try {
            for (CertificateMetadataDTO metadataDTO : certificateMetadataDTOS) {
                try {
                    certificate = apiProvider.getCertificateContent(metadataDTO.getAlias());
                    certificate.close();
                    byte[] certificateContent = IOUtils.toByteArray(certificate);
                    String encodedCertificate = new String(Base64.encodeBase64(certificateContent));
                    CertificateDetail certificateDetail = new CertificateDetail();
                    certificateDetail.setHostName(hostname);
                    certificateDetail.setAlias(metadataDTO.getAlias());
                    certificateDetail.setCertificate(encodedCertificate);
                    certificateDetails.add(certificateDetail);
                } catch (APIManagementException e) {
                    log.error("Error retrieving certificate content. tenantId [" + tenantid + "] hostname [" +
                            hostname + "] Alias [" + metadataDTO.getAlias() + "]");
                    continue;
                } catch (IOException e) {
                    log.error("Error while converting certificate content to Byte Array");
                    continue;
                }
            }
        } finally {
            if(certificate != null)
                IOUtils.closeQuietly(certificate);
        }

        return certificateDetails;
    }
}
