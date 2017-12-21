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

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIDefinition;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.FaultGatewaysException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.ResourceFile;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.api.model.Tier;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.definitions.APIDefinitionFromSwagger20;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.importexport.APIExportException;
import org.wso2.carbon.apimgt.importexport.APIImportException;
import org.wso2.carbon.apimgt.importexport.APIImportExportConstants;
import org.wso2.carbon.apimgt.importexport.APIService;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
            String errorMessage = "Error in transferring files.";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
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
                    archiveName = currentEntry.substring(0, currentEntry.indexOf(
                            APIImportExportConstants.ZIP_FILE_SEPARATOR));
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
            String errorMessage = "Failed to extract the archive (zip) file. ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
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
     * @throws APIImportException if there is an error in importing an API
     */
    public static void importAPI(String pathToArchive, String currentUser, boolean isDefaultProviderAllowed)
            throws APIImportException {

        API importedApi = null;
        String prevProvider;
        APIDefinition definitionFromSwagger20 = new APIDefinitionFromSwagger20();
        List<API> allMatchedApis;

        String pathToJSONFile = pathToArchive + APIImportExportConstants.JSON_FILE_LOCATION;

        try {
            String jsonContent = FileUtils.readFileToString(new File(pathToJSONFile));
            JsonElement configElement = new JsonParser().parse(jsonContent);
            JsonObject configObject = configElement.getAsJsonObject();

            //locate the "providerName" within the "id" and set it as the current user
            JsonObject apiId = configObject.getAsJsonObject(APIImportExportConstants.ID_ELEMENT);
            prevProvider = apiId.get(APIImportExportConstants.PROVIDER_ELEMENT).getAsString();
            String prevTenantDomain = MultitenantUtils
                    .getTenantDomain(APIUtil.replaceEmailDomainBack(prevProvider));
            String currentTenantDomain = MultitenantUtils
                    .getTenantDomain(APIUtil.replaceEmailDomainBack(currentUser));

            // If the original provider is preserved,
            if (isDefaultProviderAllowed) {

                if (!StringUtils.equals(prevTenantDomain, currentTenantDomain)) {
                    String errorMessage = "Tenant mismatch! Please enable preserveProvider property " +
                            "for cross tenant API Import ";
                    log.error(errorMessage);
                    throw new APIImportException(errorMessage);
                }
                importedApi = new Gson().fromJson(configElement, API.class);
            } else {
                apiId.addProperty(APIImportExportConstants.PROVIDER_ELEMENT, APIUtil.replaceEmailDomain(currentUser));
                importedApi = new Gson().fromJson(configElement, API.class);

                //Replace context to match with current provider
                importedApi = SetCurrentProvidertoAPIProperties(importedApi, currentTenantDomain, prevTenantDomain);
            }

            //Checking whether this is a duplicate API
            allMatchedApis = provider.searchAPIs(importedApi.getId().getApiName(), "Name", null);
            //if an API exist with the same name
            if (!allMatchedApis.isEmpty()) {
                for (API matchAPI : allMatchedApis) {
                    if (matchAPI.getId().getVersion().equalsIgnoreCase(importedApi.getId().getVersion())) {
                        String errorMessage = "Error occurred while adding the API. A duplicate API already exists " +
                                "for " + importedApi.getId().getApiName() + '-' + importedApi.getId().getVersion();
                        log.error(errorMessage);
                        throw new APIImportException(errorMessage);
                    }
                }
            }

        } catch (IOException e) {
            String errorMessage = "Error in reading API definition. ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
        } catch (APIManagementException e) {
            String errorMessage = "Error in checking API existence. ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
        }

        Set<Tier> allowedTiers;
        Set<Tier> unsupportedTiersList;

        try {
            allowedTiers = provider.getTiers();
        } catch (APIManagementException e) {
            String errorMessage = "Error in retrieving tiers of the provider. ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
        }

        if (!(allowedTiers.isEmpty())) {
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

        try {

            provider.addAPI(importedApi);

            //Swagger definition will only be available of API type HTTP. Web socket api does not have it.
            if (!APIConstants.APIType.WS.toString().equalsIgnoreCase(importedApi.getType())) {

                String swaggerContent = FileUtils.readFileToString(
                        new File(pathToArchive + APIImportExportConstants.SWAGGER_DEFINITION_LOCATION));

                addSwaggerDefinition(importedApi.getId(), swaggerContent);

                //Load required properties from swagger to the API
                Set<URITemplate> uriTemplates = definitionFromSwagger20.getURITemplates(importedApi, swaggerContent);
                importedApi.setUriTemplates(uriTemplates);
                Set<Scope> scopes = definitionFromSwagger20.getScopes(swaggerContent);
                importedApi.setScopes(scopes);

                for (URITemplate uriTemplate : uriTemplates) {
                    Scope scope = uriTemplate.getScope();
                    if (scope != null && !(APIUtil.isWhiteListedScope(scope.getKey()))) {
                        if (provider.isScopeKeyAssigned(importedApi.getId(), scope.getKey(),
                                PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true))) {
                            String errorMessage =
                                    "Error in adding API. Scope " + scope.getKey() + " is already assigned " +
                                            "by another API \n";
                            log.error(errorMessage);
                            throw new APIImportException(errorMessage);
                        }
                    }
                }
                // This is required to make url templates and scopes get effected.
                provider.updateAPI(importedApi);
            }

        } catch (APIManagementException e) {
            //Error is logged and APIImportException is thrown because adding API and swagger are mandatory steps
            String errorMessage = "Error in adding API to the provider. ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
        } catch (IOException e) {
            //Error is logged and APIImportException is thrown because adding API and swagger are mandatory steps
            String errorMessage = "Error in reading Swagger definition ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
        } catch (FaultGatewaysException e) {
            String errorMessage = "Error in updating API ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
        }

        //Since Image, documents, sequences and WSDL are optional, exceptions are logged and ignored in implementation
        addAPIImage(pathToArchive, importedApi);
        addAPIDocuments(pathToArchive, importedApi);
        addAPISequences(pathToArchive, importedApi);
        addAPISpecificSequences(pathToArchive, importedApi);
        addAPIWsdl(pathToArchive, importedApi);

    }

    /**
     * Replace original provider name from imported API properties with the logged in username
     * This method is used when "preserveProvider" property is set to false
     *
     * @param importedApi    Imported API
     * @param currentDomain  current domain name
     * @param previousDomain original domain name
     * @return API after changing provider details to match with imported environment
     */
    private static API SetCurrentProvidertoAPIProperties(API importedApi, String currentDomain, String previousDomain) {
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(currentDomain) &&
                !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(previousDomain)) {
            importedApi.setContext(importedApi.getContext().replace("/t/" + previousDomain, ""));
            importedApi.setContextTemplate(importedApi.getContextTemplate().replace("/t/" + previousDomain, ""));
        } else if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(currentDomain) &&
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(previousDomain)) {
            importedApi.setContext("/t/" + currentDomain + importedApi.getContext());
            importedApi.setContextTemplate("/t/" + currentDomain + importedApi
                    .getContextTemplate());
        } else if (!StringUtils.equalsIgnoreCase(currentDomain, previousDomain)) {
            importedApi.setContext(importedApi.getContext().replace(previousDomain, currentDomain));
            importedApi.setContextTemplate(importedApi.getContextTemplate().replace
                    (previousDomain, currentDomain));
        }

        return importedApi;

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
                        ResourceFile apiImage = new ResourceFile(inputStream, mimeType);
                        String thumbPath = APIUtil.getIconPath(importedApi.getId());
                        String thumbnailUrl = provider.addResourceFile(thumbPath, apiImage);

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
        } catch (APIManagementException e) {
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
                        inputStream = new FileInputStream(pathToArchive +
                                APIImportExportConstants.DIRECTORY_SEPARATOR +
                                APIImportExportConstants.DOCUMENT_DIRECTORY +
                                APIImportExportConstants.DIRECTORY_SEPARATOR +
                                APIImportExportConstants.INLINE_DOCUMENT_DIRECTORY +
                                APIImportExportConstants.DIRECTORY_SEPARATOR + doc.getName());
                        String inlineContent = IOUtils.toString(inputStream, APIImportExportConstants.CHARSET);
                        provider.addDocumentationContent(importedApi, doc.getName(), inlineContent);

                    } else if (APIImportExportConstants.URL_DOC_TYPE.equalsIgnoreCase(doc.getSourceType().toString())) {
                        provider.addDocumentation(apiIdentifier, doc);

                    } else if (APIImportExportConstants.FILE_DOC_TYPE.
                            equalsIgnoreCase(doc.getSourceType().toString())) {
                        inputStream = new FileInputStream(pathToArchive +
                                APIImportExportConstants.DIRECTORY_SEPARATOR +
                                APIImportExportConstants.DOCUMENT_DIRECTORY +
                                APIImportExportConstants.DIRECTORY_SEPARATOR +
                                APIImportExportConstants.FILE_DOCUMENT_DIRECTORY +
                                APIImportExportConstants.DIRECTORY_SEPARATOR +
                                doc.getFilePath());
                        String docExtension =
                                FilenameUtils.getExtension(pathToArchive + APIImportExportConstants.DIRECTORY_SEPARATOR
                                        + APIImportExportConstants.DOCUMENT_DIRECTORY
                                        + APIImportExportConstants.DIRECTORY_SEPARATOR
                                        + doc.getFilePath());

                        ResourceFile apiDocument = new ResourceFile(inputStream, docExtension);
                        String visibleRolesList = importedApi.getVisibleRoles();
                        String[] visibleRoles = new String[0];

                        if (visibleRolesList != null) {
                            visibleRoles = visibleRolesList.split(",");
                        }

                        String filePathDoc = APIUtil.getDocumentationFilePath(apiIdentifier, doc.getFilePath());
                        APIUtil.setResourcePermissions(importedApi.getId().getProviderName(),
                                importedApi.getVisibility(), visibleRoles, filePathDoc);
                        doc.setFilePath(provider.addResourceFile(filePathDoc, apiDocument));
                        provider.addDocumentation(apiIdentifier, doc);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            //this error is logged and ignored because documents are optional in an API
            log.error("Failed to locate the document files of the API.", e);
        } catch (APIManagementException e) {
            //this error is logged and ignored because documents are optional in an API
            log.error("Failed to add Documentations to API.", e);
        } catch (IOException e) {
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
     */
    private static void addAPISequences(String pathToArchive, API importedApi) {

        Registry registry = APIExportUtil.getRegistry();
        String inSequenceFileName = importedApi.getInSequence() + APIImportExportConstants.XML_EXTENSION;
        String inSequenceFileLocation = pathToArchive + APIImportExportConstants.IN_SEQUENCE_LOCATION
                + inSequenceFileName;
        String regResourcePath;

        //Adding in-sequence, if any
        if (checkFileExistence(inSequenceFileLocation)) {
            regResourcePath = APIConstants.API_CUSTOM_INSEQUENCE_LOCATION + inSequenceFileName;
            addSequenceToRegistry(registry, inSequenceFileLocation, regResourcePath);
        }

        String outSequenceFileName = importedApi.getOutSequence() + APIImportExportConstants.XML_EXTENSION;
        String outSequenceFileLocation = pathToArchive + APIImportExportConstants.OUT_SEQUENCE_LOCATION
                + outSequenceFileName;

        //Adding out-sequence, if any
        if (checkFileExistence(outSequenceFileLocation)) {
            regResourcePath = APIConstants.API_CUSTOM_OUTSEQUENCE_LOCATION + outSequenceFileName;
            addSequenceToRegistry(registry, outSequenceFileLocation, regResourcePath);
        }

        String faultSequenceFileName = importedApi.getFaultSequence() + APIImportExportConstants.XML_EXTENSION;
        String faultSequenceFileLocation = pathToArchive + APIImportExportConstants.FAULT_SEQUENCE_LOCATION
                + faultSequenceFileName;

        //Adding fault-sequence, if any
        if (checkFileExistence(faultSequenceFileLocation)) {
            regResourcePath = APIConstants.API_CUSTOM_FAULTSEQUENCE_LOCATION + faultSequenceFileName;
            addSequenceToRegistry(registry, faultSequenceFileLocation, regResourcePath);
        }
    }

    /**
     * This method adds API Specific sequences added through the store to the imported API.
     *
     * @param pathToArchive location of the extracted folder of the API
     * @param importedApi   the imported API object
     */
    private static void addAPISpecificSequences(String pathToArchive, API importedApi) {

        Registry registry = APIExportUtil.getRegistry();
        String regResourcePath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                importedApi.getId().getProviderName() + RegistryConstants.PATH_SEPARATOR +
                importedApi.getId().getApiName() + RegistryConstants.PATH_SEPARATOR +
                importedApi.getId().getVersion() + RegistryConstants.PATH_SEPARATOR;

        String inSequenceFileName = importedApi.getInSequence();
        String inSequenceFileLocation = pathToArchive + APIImportExportConstants.IN_SEQUENCE_LOCATION + "Custom" +
                File.separator + inSequenceFileName;

        //Adding in-sequence, if any
        if (checkFileExistence(inSequenceFileLocation)) {
            String inSequencePath = APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN +
                    RegistryConstants.PATH_SEPARATOR + inSequenceFileName;
            addSequenceToRegistry(registry, inSequenceFileLocation, regResourcePath + inSequencePath);
        }

        String outSequenceFileName = importedApi.getOutSequence();
        String outSequenceFileLocation = pathToArchive + APIImportExportConstants.OUT_SEQUENCE_LOCATION + "Custom" +
                File.separator + outSequenceFileName;

        //Adding out-sequence, if any
        if (checkFileExistence(outSequenceFileLocation)) {
            String outSequencePath = APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT +
                    RegistryConstants.PATH_SEPARATOR + outSequenceFileName;
            addSequenceToRegistry(registry, outSequenceFileLocation, regResourcePath + outSequencePath);
        }

        String faultSequenceFileName = importedApi.getFaultSequence();
        String faultSequenceFileLocation = pathToArchive + APIImportExportConstants.FAULT_SEQUENCE_LOCATION + "Custom" +
                File.separator + faultSequenceFileName;

        //Adding fault-sequence, if any
        if (checkFileExistence(faultSequenceFileLocation)) {
            String faultSequencePath = APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT +
                    RegistryConstants.PATH_SEPARATOR + faultSequenceFileName;
            addSequenceToRegistry(registry, faultSequenceFileLocation, regResourcePath + faultSequencePath);
        }
    }

    /**
     * This method adds the sequence files to the registry.
     *
     * @param registry             the registry instance
     * @param sequenceFileLocation location of the sequence file
     */
    private static void addSequenceToRegistry(Registry registry, String sequenceFileLocation, String regResourcePath) {

        InputStream seqStream = null;
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
                seqStream = new FileInputStream(sequenceFile);
                byte[] inSeqData = IOUtils.toByteArray(seqStream);
                Resource inSeqResource = (Resource) registry.newResource();
                inSeqResource.setContent(inSeqData);
                registry.put(regResourcePath, inSeqResource);
            }
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            //this is logged and ignored because sequences are optional
            log.error("Failed to add sequences into the registry : " + regResourcePath, e);
        } catch (IOException e) {
            //this is logged and ignored because sequences are optional
            log.error("I/O error while writing sequence data to the registry : " + regResourcePath, e);
        } finally {
            IOUtils.closeQuietly(seqStream);
        }
    }

    /**
     * This method adds the WSDL to the registry, if there is a WSDL associated with the API
     *
     * @param pathToArchive location of the extracted folder of the API
     * @param importedApi   the imported API object
     */
    private static void addAPIWsdl(String pathToArchive, API importedApi) {

        String wsdlFileName = importedApi.getId().getApiName() + "-" + importedApi.getId().getVersion() +
                APIImportExportConstants.WSDL_EXTENSION;
        String wsdlPath = pathToArchive + APIImportExportConstants.WSDL_LOCATION + wsdlFileName;

        if (checkFileExistence(wsdlPath)) {
            try {
                URL wsdlFileUrl = new File(wsdlPath).toURI().toURL();
                importedApi.setWsdlUrl(wsdlFileUrl.toString());
                Registry registry = APIExportUtil.getRegistry();
                APIUtil.createWSDL((org.wso2.carbon.registry.core.Registry) registry, importedApi);
                provider.updateAPI(importedApi);
            } catch (MalformedURLException e) {
                //this exception is logged and ignored since WSDL is optional for an API
                log.error("Error in getting WSDL URL. ", e);
            } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
                //this exception is logged and ignored since WSDL is optional for an API
                log.error("Error in putting the WSDL resource to registry. ", e);
            } catch (APIManagementException e) {
                //this exception is logged and ignored since WSDL is optional for an API
                log.error("Error in creating the WSDL resource in the registry. ", e);
            } catch (FaultGatewaysException e) {
                //This is logged and process is continued because WSDL is optional for an API
                log.error("Failed to update API after adding WSDL. ", e);
            }
        }
    }

    /**
     * This method adds Swagger API definition to registry
     *
     * @param apiId          Identifier of the imported API
     * @param swaggerContent Content of Swagger file
     * @throws APIImportException if there is an error occurs when adding Swagger definition
     */
    private static void addSwaggerDefinition(APIIdentifier apiId, String swaggerContent)
            throws APIImportException {

        try {
            provider.saveSwagger20Definition(apiId, swaggerContent);
        } catch (APIManagementException e) {
            String errorMessage = "Error in adding Swagger definition for the API. ";
            log.error(errorMessage, e);
            throw new APIImportException(errorMessage, e);
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

