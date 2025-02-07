/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.test.utils.webapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Util that is enabled the more utility methods for handling the WebApps
 */
public class WebAppDeploymentUtil {
    private static Log log = LogFactory.getLog(WebAppDeploymentUtil.class);

    /**
     * Copy a war file to the webapp directory
     *
     * @param testWebAppResourcePath Sample webapp war file path
     * @param warFilePath            War file path
     * @throws IOException If an error occurs while reading/writing files
     */
    public static void copyWebApp(String testWebAppResourcePath, String warFilePath) throws IOException {
        try (InputStream webSource = WebAppDeploymentUtil.class.getResourceAsStream(testWebAppResourcePath)) {
            File warDestFile = new File(warFilePath + ".war");
            try (FileOutputStream warDest = new FileOutputStream(warDestFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = webSource.read(buffer)) > 0) {
                    warDest.write(buffer, 0, length);
                }
            }
        }
    }

}
