/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.wso2.am.integration.cucumbertests.utils;

import java.io.File;

public class ModulePathResolver {

    /**
     * Determines the root directory of the Maven module where the given class resides.
     *
     * @param clazz the class whose module directory should be resolved
     * @return the absolute path to the module directory
     * @throws RuntimeException if the module directory cannot be determined
     */
    public static String getModuleDir(Class<?> clazz) {
        try {
            File location = new File(
                    clazz.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );

            File targetDir = location.getParentFile(); // /module-x/target
            File moduleDir = targetDir.getParentFile(); // /module-x
            return moduleDir.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Unable to determine module path for class: " + clazz.getName(), e);
        }
    }
}
