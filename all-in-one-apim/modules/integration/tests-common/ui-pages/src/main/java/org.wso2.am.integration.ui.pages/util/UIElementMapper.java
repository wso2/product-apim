/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.ui.pages.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Read mapper.properties file and load it's uiElements into Properties object.
 */
public class UIElementMapper {
    public static final Properties uiProperties = new Properties();
    private static final Log log = LogFactory.getLog(UIElementMapper.class);
    private static UIElementMapper instance;

    private UIElementMapper() {
    }

    public static synchronized UIElementMapper getInstance() throws IOException {
        if (instance == null) {
            setStream();
            instance = new UIElementMapper();
        }
        return instance;
    }

    public static Properties setStream() throws IOException {
        InputStream inputStream = UIElementMapper.class.getResourceAsStream("/mapper.properties");
        if (inputStream.available() > 0) {
            uiProperties.load(inputStream);
            inputStream.close();
            return uiProperties;
        }
        return null;
    }

    public String getElement(String key) {
        if (uiProperties != null) {
            return uiProperties.getProperty(key);
        }
        return null;
    }
}
