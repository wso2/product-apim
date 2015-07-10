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
import apim.restful.importexport.APIImportExportConstants;
import apim.restful.importexport.APIImportException;
import apim.restful.importexport.APIService;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.FaultGatewaysException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.Icon;
import org.wso2.carbon.apimgt.api.model.Tier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;

import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.core.Resource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * This class provides the functions utilized to import an API from an API archive.
 */
public final class APIImportUtil {

    private static final Log log = LogFactory.getLog(APIService.class);
    static APIProvider provider;

    /**
     * This method initializes the Provider when there is a direct request to import an API
     *
     * @param currentUserName the current logged in user
     * @throws APIExportException if provider cannot be initialized
     */
    public static void initializeProvider(String currentUserName) throws APIExportException {
        provider = APIExportUtil.getProvider(currentUserName);
    }

    /**
     * This method uploads a given file to specified location
     *
     * @param uploadedInputStream input stream of the file
     * @param newFileName         name of the file to be created
     * @param storageLocation     destination of the new file
     * @throws APIImportException if the file transfer fails
     */
    public static void transferFile(InputStream uploadedInputStream, String newFileName, String storageLocation)
            throws APIImportException {
        FileOutputStream outFileStream = null;

        try {
            outFileStream = new FileOutputStream(new File(storageLocation, newFileName));
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outFileStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            log.error("Error in transferring files.", e);
            throw new APIImportException("Error in transferring archive files. " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(outFileStream);
        }
    }

    /**
     * This method decompresses API the archive
     *
     * @param sourceFile  The archive containing the API
     * @param destination location of the archive to be extracted
     * @return Name of the extracted directory
     * @throws APIImportException If the decompressing fails
     */
    public static String extractArchive(File sourceFile, String destination) throws APIImportException {

        BufferedInputStream inputStream = null;
        InputStream zipInputStream = null;
        FileOutputStream outputStream = null;
        ZipFile zip = null;
        String archiveName = null;

        try {
            zip = new ZipFile(sourceFile);
            Enumeration zipFileEntries = zip.entries();
            int index = 0;

            // Process each entry
            while (zipFileEntries.hasMoreElements()) {

                // grab a zip file entry
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();

                //This index variable is used to get the extracted folder name; that is root directory
                if (index == 0) {
                    archiveName = currentEntry.substring(0, currentEntry.indexOf(File.separatorChar));
                    --index;
                }

                File destinationFile = new File(destination, currentEntry);
                File destinationParent = destinationFile.getParentFile();

                // create the parent directory structure
                if (destinationParent.mkdirs()) {
                    log.info("Creation of folder is successful. Directory Name : " + destinationParent.getName());
                }

                if (!entry.isDirectory()) {
                    zipInputStream = zip.getInputStream(entry);
                    inputStream = new BufferedInputStream(zipInputStream);

                    // write the current file to the destination
                    outputStream = new FileOutputStream(destinationFile);
                    IOUtils.copy(inputStream, outputStream);
                }
            }
            return archiveName;
        } catch (IOException e) {
            log.error("Failed to extract archive file ", e);
            throw new APIImportException("Failed to extract archive file. " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(zipInputStream);
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }

    /**
     * This method imports an API
     *
     * @param pathToArchive            location of the extracted folder of the API
     * @param currentUser              the current logged in user
     * @param isDefaultProviderAllowed decision to keep or replace the provider
     * @throws APIImportException     if there is an error in importing an API
     */
    public static void importAPI(String pathToArchive, String currentUser, boolean isDefaultProviderAllowed)
            throws APIImportException {

        API importedApi;

        // If the original provider is preserved,
        if (isDefaultProviderAllowed) {

            FileInputStream inputStream = null;
            BufferedReader bufferedReader = null;

            try {
                inputStream = new FileInputStream(pathToArchive + APIImportExportConstants.JSON_FILE_LOCATION);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                importedApi = new Gson().fromJson(bufferedReader, API.class);
            } catch (FileNotFoundException e) {
                log.error("Error in locating api.json file. ", e);
                throw new APIImportException("Error in locating api.json file. " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(bufferedReader);
            }
        } else {

            String pathToJSONFile = pathToArchive + APIImportExportConstants.JSON_FILE_LOCATION;

            try {
                String jsonContent = FileUtils.readFileToString(new File(pathToJSONFile));
                JsonElement configElement = new JsonParser().parse(jsonContent);
                JsonObject configObject = configElement.getAsJsonObject();

                //locate the "providerName" within the "id" and set it as the current user
                JsonObject apiId = configObject.getAsJsonObject(APIImportExportConstants.ID_ELEMENT);
                apiId.addProperty(APIImportExportConstants.PROVIDER_ELEMENT, APIUtil.replaceEmailDomain(currentUser));
                importedApi = new Gson().fromJson(configElement, API.class);

            } catch (IOException e) {
                log.error("Error in setting API provider to logged in user. ", e);
                throw new APIImportException("Error in setting API provider to logged in user. " + e.getMessage());
            }
        }

        Set<Tier> allowedTiers;
        Set<Tier> unsupportedTiersList;

        try{
            allowedTiers = provider.getTiers();
        } catch (APIManagementException e) {
            log.error("Error in retrieving tiers of the provider. ", e);
            throw new APIImportException("Error in retrieving tiers of the provider. " + e.getMessage());
        }

        if (!(allowedTiers.isEmpty())){
            unsupportedTiersList = Sets.difference(importedApi.getAvailableTiers(), allowedTiers);

            //If at least one unsupported tier is found, it should be removed before adding API
            if (!(unsupportedTiersList.isEmpty())) {
                for (Tier unsupportedTier : unsupportedTiersList) {

                    //Process is continued with a warning and only supported tiers are added to the importer API
                    log.warn("Tier name : " + unsupportedTier.getName() + " is not supported.");
                }

                //Remove the unsupported tiers before adding the API
                importedApi.removeAvailableTiers(unsupportedTiersList);
            }
        }

        try{
            provider.addAPI(importedApi);
            addSwaggerDefinition(importedApi.getId(), pathToArchive);
        } catch (APIManagementException e){
            //Error is logged and APIImportException is thrown because adding API and swagger are mandatory steps
            log.error("Error in adding API to the provider. ", e);
            throw new APIImportException("Error in adding API to the provider. " + e.getMessage());
        }

        //Since Image, documents, sequences and WSDL are optional, exceptions are logged and ignored in implementation
        addAPIImage(pathToArchive, importedApi);
        addAPIDocuments(pathToArchive, importedApi);
        addAPISequences(pathToArchive, importedApi, currentUser);
        addAPIWsdl(pathToArchive, importedApi, currentUser);

    }

    /**
     * This method adds the icon to the API which is to be displayed at the API store.
     *
     * @param pathToArchive location of the extracted folder of the API
     * @param importedApi   the imported API object
     */
    private static void addAPIImage(String pathToArchive, API importedApi) {

        //Adding image icon to the API if there is any
        File imageFolder = new File(pathToArchive + APIImportExportConstants.IMAGE_FILE_LOCATION);
        File[] fileArray = imageFolder.listFiles();
        FileInputStream inputStream = null;

        try {
            if (imageFolder.isDirectory() && fileArray != null) {

                //This loop locates the icon of the API
                for (File imageFile : fileArray) {
                    if (imageFile != null && imageFile.getName().contains(APIImportExportConstants.IMAGE_FILE_NAME)) {

                        String mimeType = URLConnection.guessContentTypeFromName(imageFile.getName());
                        inputStream = new FileInputStream(imageFile.getAbsolutePath());
                        Icon apiImage = new Icon(inputStream, mimeType);
                        String thumbPath = APIUtil.getIconPath(importedApi.getId());
                        String thumbnailUrl = provider.addIcon(thumbPath, apiImage);

                        importedApi.setThumbnailUrl(APIUtil.prependTenantPrefix(thumbnailUrl,
                                importedApi.getId().getProviderName()));
                        APIUtil.setResourcePermissions(importedApi.getId().getProviderName(), null, null, thumbPath);
                        provider.updateAPI(importedApi);

                        //the loop is terminated after successfully locating the icon
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            //This is logged and process is continued because icon is optional for an API
            log.error("Icon for API is not found. ", e);
        } catch (APIManagementException e){
            //This is logged and process is continued because icon is optional for an API
            log.error("Failed to add icon to the API. ", e);
        } catch (FaultGatewaysException e) {
            //This is logged and process is continued because icon is optional for an API
            log.error("Failed to update API after adding icon. ", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * This method adds the documents to the imported API
     *
     * @param pathToArchive location of the extracted folder of the API
     * @param importedApi   the imported API object
     */
    private static void addAPIDocuments(String pathToArchive, API importedApi) {

        String docFileLocation = pathToArchive + APIImportExportConstants.DOCUMENT_FILE_LOCATION;
        FileInputStream inputStream = null;
        BufferedReader bufferedReader = null;
        APIIdentifier apiIdentifier = importedApi.getId();

        try {
            if (checkFileExistence(docFileLocation)) {

                inputStream = new FileInputStream(docFileLocation);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                Documentation[] documentations = new Gson().fromJson(bufferedReader, Documentation[].class);

                //For each type of document separate action is performed
                for (Documentation doc : documentations) {

                    if (APIImportExportConstants.INLINE_DOC_TYPE.equalsIgnoreCase(doc.getSourceType().toString())) {
                        provider.addDocumentation(apiIdentifier, doc);
                        provider.addDocumentationContent(importedApi, doc.getName(), doc.getSummary());

                    } else if (APIImportExportConstants.URL_DOC_TYPE.equalsIgnoreCase(doc.getSourceType().toString())) {
                        provider.addDocumentation(apiIdentifier, doc);

                    } else if (APIImportExportConstants.FILE_DOC_TYPE.
                            equalsIgnoreCase(doc.getSourceType().toString())) {
                        inputStream = new FileInputStream(pathToArchive + doc.getFilePath());
                        String docExtension = FilenameUtils.getExtension(pathToArchive + doc.getFilePath());
                        Icon apiDocument = new Icon(inputStream, docExtension);
                        String visibleRolesList = importedApi.getVisibleRoles();
                        String[] visibleRoles = new String[0];

                        if (visibleRolesList != null) {
                            visibleRoles = visibleRolesList.split(",");
                        }

                        String filePathDoc = APIUtil.getDocumentationFilePath(apiIdentifier, doc.getName());
                        APIUtil.setResourcePermissions(importedApi.getId().getProviderName(),
                                importedApi.getVisibility(), visibleRoles, filePathDoc);
                        doc.setFilePath(provider.addIcon(filePathDoc, apiDocument));
                        provider.addDocumentation(apiIdentifier, doc);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            //this error is logged and ignored because documents are optional in an API
            log.error("Failed to locate the document files of the API.", e);
        } catch (APIManagementException e){
            //this error is logged and ignored because documents are optional in an API
            log.error("Failed to add Documentations to API.", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(bufferedReader);
        }

    }

    /**
     * This method adds API sequences to the imported API. If the sequence is a newly defined one, it is added.
     *
     * @param pathToArchive location of the extracted folder of the API
     * @param importedApi   the imported API object
     * @param currentUser   current logged in username
     */
    private static void addAPISequences(String pathToArchive, API importedApi, String currentUser) {

        Registry registry = APIExportUtil.getRegistry(currentUser);
        String inSequenceFileName = importedApi.getInSequence() + APIImportExportConstants.XML_EXTENSION;
        String inSequenceFileLocation = pathToArchive + APIImportExportConstants.IN_SEQUENCE_LOCATION
                + inSequenceFileName;

        //Adding in-sequence, if any
        if (checkFileExistence(inSequenceFileLocation)) {
            addSequenceToRegistry(registry, APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN,
                    inSequenceFileName, inSequenceFileLocation);
        }

        String outSequenceFileName = importedApi.getOutSequence() + APIImportExportConstants.XML_EXTENSION;
        String outSequenceFileLocation = pathToArchive + APIImportExportConstants.OUT_SEQUENCE_LOCATION
                + outSequenceFileName;

        //Adding out-sequence, if any
        if (checkFileExistence(outSequenceFileLocation)) {
            addSequenceToRegistry(registry, APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT,
                    outSequenceFileName, outSequenceFileLocation);
        }

        String faultSequenceFileName = importedApi.getFaultSequence() + APIImportExportConstants.XML_EXTENSION;
        String faultSequenceFileLocation = pathToArchive + APIImportExportConstants.FAULT_SEQUENCE_LOCATION
                + faultSequenceFileName;

        //Adding fault-sequence, if any
        if (checkFileExistence(faultSequenceFileLocation)) {
            addSequenceToRegistry(registry, APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT,
                    faultSequenceFileName, faultSequenceFileLocation);
        }
    }

    /**
     * This method adds the sequence files to the registry.
     *
     * @param registry             the registry instance
     * @param customSequenceType   type of the sequence
     * @param sequenceFileName     name of the sequence
     * @param sequenceFileLocation location of the sequence file
     */
    private static void addSequenceToRegistry(Registry registry, String customSequenceType, String sequenceFileName,
                                              String sequenceFileLocation) {

        String regResourcePath = APIConstants.API_CUSTOM_SEQUENCE_LOCATION + File.separator + customSequenceType
                + File.separator + sequenceFileName;
        InputStream inSeqStream = null;
        try {
            if (registry.resourceExists(regResourcePath)) {
                if (log.isDebugEnabled()) {
                    log.debug("Defined sequences have already been added to the registry");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Adding defined sequences to the registry.");
                }
                File sequenceFile = new File(sequenceFileLocation);
                inSeqStream = new FileInputStream(sequenceFile);
                byte[] inSeqData = IOUtils.toByteArray(inSeqStream);
                Resource inSeqResource = (Resource) registry.newResource();
                inSeqResource.setContent(inSeqData);
                registry.put(regResourcePath, inSeqResource);
            }
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            //this is logged and ignored because sequences are optional
            log.error("Failed to add sequences into the registry: " + customSequenceType, e);
        } catch (IOException e) {
            //this is logged and ignored because sequences are optional
            log.error("I/O error while writing sequence data to the registry, Sequence type: " + customSequenceType, e);
        } finally {
            IOUtils.closeQuietly(inSeqStream);
        }
    }

    /**
     * This method adds the WSDL to the registry, if there is a WSDL associated with the API
     *
     * @param pathToArchive location of the extracted folder of the API
     * @param importedApi   the imported API object
     * @param currentUser   current logged in username
     */
    private static void addAPIWsdl(String pathToArchive, API importedApi, String currentUser) {

        String wsdlFileName = importedApi.getId().getApiName() + "-" + importedApi.getId().getVersion() +
                APIImportExportConstants.WSDL_EXTENSION;
        String wsdlPath = pathToArchive + APIImportExportConstants.WSDL_LOCATION + wsdlFileName;

        if (checkFileExistence(wsdlPath)) {
            try {
                URL wsdlFileUrl = new File(wsdlPath).toURI().toURL();
                importedApi.setWsdlUrl(wsdlFileUrl.toString());
                Registry registry = APIExportUtil.getRegistry(currentUser);
                APIUtil.createWSDL((org.wso2.carbon.registry.core.Registry) registry, importedApi);
            } catch (MalformedURLException e) {
                //this exception is logged and ignored since WSDL is optional for an API
                log.error("Error in getting WSDL URL. ", e);
            } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
                //this exception is logged and ignored since WSDL is optional for an API
                log.error("Error in putting the WSDL resource to registry. ", e);
            } catch (APIManagementException e) {
                //this exception is logged and ignored since WSDL is optional for an API
                log.error("Error in creating the WSDL resource in the registry. ", e);
            }
        }
    }

    /**
     * This method adds Swagger API definition to registry
     *
     * @param apiId       Identifier of the imported API
     * @param archivePath File path where API archive stored
     * @throws APIImportException if there is an error occurs when adding Swagger definition
     */
    private static void addSwaggerDefinition(APIIdentifier apiId, String archivePath)
            throws APIImportException {

        try {
            String swaggerContent = FileUtils.readFileToString(
                    new File(archivePath + APIImportExportConstants.SWAGGER_DEFINITION_LOCATION));
            provider.saveSwagger20Definition(apiId, swaggerContent);
        } catch (APIManagementException e) {
            log.error("Error in adding Swagger definition for the API. ", e);
            throw new APIImportException("Error in adding Swagger definition for the API. " + e.getMessage());
        } catch (IOException e) {
            log.error("Error in importing Swagger definition for the API. ", e);
            throw new APIImportException("Error in importing Swagger definition for the API. " + e.getMessage());
        }
    }

    /**
     * This method checks whether a given file exists in a given location
     *
     * @param fileLocation location of the file
     * @return true if the file exists, false otherwise
     */
    private static boolean checkFileExistence(String fileLocation) {
        File testFile = new File(fileLocation);
        return testFile.exists();
    }
}

