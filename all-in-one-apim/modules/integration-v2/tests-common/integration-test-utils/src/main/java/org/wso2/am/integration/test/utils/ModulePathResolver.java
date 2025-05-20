package org.wso2.am.integration.test.utils;

import java.io.File;

public class ModulePathResolver {
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

