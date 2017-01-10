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

import java.io.File;

/**
 * This class contains all the constants required for API Import and Export
 */
public final class APIImportExportConstants {

    //system dependent default path separator character, represented as a string
    public static final String DIRECTORY_SEPARATOR = File.separator;
    //System independent file separator for zip files
    public static final char ZIP_FILE_SEPARATOR = '/';
    //string representing the false state when preserving the provider
    public static final String STATUS_FALSE = "FALSE";
    //length of the name of the temporary directory
    public static final int TEMP_FILENAME_LENGTH = 5;
    //system property for default temporary directory
    public static final String TEMP_DIR = "java.io.tmpdir";
    //name of the uploaded zip file
    public static final String UPLOAD_FILE_NAME = "APIArchive.zip";
    //location of the api JSON file
    public static final String JSON_FILE_LOCATION = DIRECTORY_SEPARATOR + "Meta-information" + DIRECTORY_SEPARATOR +
            "api.json";
    //name of the id element tag of the api.json file
    public static final String ID_ELEMENT = "id";
    //name of the api provider element tag of the api.json file
    public static final String PROVIDER_ELEMENT = "providerName";
    //location of the api swagger definition file
    public static final String SWAGGER_DEFINITION_LOCATION = DIRECTORY_SEPARATOR + "Meta-information" +
            DIRECTORY_SEPARATOR + "swagger.json";
    //location of the image
    public static final String IMAGE_FILE_LOCATION = DIRECTORY_SEPARATOR + "Image" + DIRECTORY_SEPARATOR;
    //name of the image
    public static final String IMAGE_FILE_NAME = "icon";
    //location of the documents JSON file
    public static final String DOCUMENT_FILE_LOCATION = DIRECTORY_SEPARATOR + "Docs" + DIRECTORY_SEPARATOR +
            "docs.json";
    //name of the inline file type
    public static final String INLINE_DOC_TYPE = "INLINE";
    //name of the url file type
    public static final String URL_DOC_TYPE = "URL";
    //name of the physical file type
    public static final String FILE_DOC_TYPE = "FILE";
    //location of the in sequence
    public static final String IN_SEQUENCE_LOCATION = DIRECTORY_SEPARATOR + "Sequences" + DIRECTORY_SEPARATOR +
            "in-sequence" + DIRECTORY_SEPARATOR;
    //location of the out sequence
    public static final String OUT_SEQUENCE_LOCATION = DIRECTORY_SEPARATOR + "Sequences" + DIRECTORY_SEPARATOR +
            "out-sequence" + DIRECTORY_SEPARATOR;
    //location of the fault sequence
    public static final String FAULT_SEQUENCE_LOCATION = DIRECTORY_SEPARATOR + "Sequences" + DIRECTORY_SEPARATOR +
            "fault-sequence" + DIRECTORY_SEPARATOR;
    //extension of xml files
    public static final String XML_EXTENSION = ".xml";
    //location of the wsdl file
    public static final String WSDL_LOCATION = DIRECTORY_SEPARATOR + "WSDL" + DIRECTORY_SEPARATOR;
    //extension of wsdl files
    public static final String WSDL_EXTENSION = ".wsdl";

    public static final String DOCUMENT_DIRECTORY = "Docs";

    public static final String INLINE_DOCUMENT_DIRECTORY = "InlineContents";

    public static final String FILE_DOCUMENT_DIRECTORY = "FileContents";

    public static final String INLINE_DOC_CONTENT_REGISTRY_DIRECTORY = "contents";

    public static final String API_REGISTRY_BASE_LOCATION =
            "/registry/resource/_system/governance/apimgt/applicationdata/";

    public static final String CHARSET = "UTF-8";

    public static final String WSO_HEADER_PREFIX = "X-WSO2-";
    
    public static final String WSO_HEADER_TEMPLATE_MANAGER = "X-WSO2-TEMPLATECLASS";
}
