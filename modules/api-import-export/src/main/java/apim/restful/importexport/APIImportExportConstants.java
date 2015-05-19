/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;

/**
 * This class contains all constants required for API Import and Export
 */
public final class APIImportExportConstants {

	//This is where archive file get generated for exporting API
	public static final String BASE_ARCHIVE_PATH = CarbonUtils.getCarbonHome() + File.separator + "tmp" +
	                                               File.separator + "work" ;

}
