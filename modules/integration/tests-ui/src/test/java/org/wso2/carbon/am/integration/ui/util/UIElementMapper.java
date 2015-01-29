/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * /
 */

package org.wso2.carbon.am.integration.ui.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Mapping the UI element identification which are stored in /mapper.properties
 */
public class UIElementMapper {
    private static Properties uiProperties = null;
    private static final Log log = LogFactory.getLog(UIElementMapper.class);

    /**
     * Provides the UI element access value configured in /mapper.properties
     *
     * @param key UI element key configured in /mapper.properties
     * @return String : Access value of the Ui element
     * @throws IOException
     */
    public static String getElement(String key) throws IOException {
        String uiELemetValue = null;
        if (uiProperties == null) {
            uiProperties = new Properties();
            InputStream inputStream = UIElementMapper.class.getResourceAsStream("/mapper.properties");
            try {
                if (inputStream.available() > 0) {
                    uiProperties.load(inputStream);
                }
            } catch (IOException ioE) {
                log.error(ioE);
                throw new ExceptionInInitializerError("Mapper stream not set. Failed to read file");
            } finally {
                try {
                    inputStream.close();
                } catch (Exception ioE) {
                    log.error(ioE);
                    throw new ExceptionInInitializerError("Mapper stream not closed correctly. Failed to close the stream");
                }
            }
        }

        uiELemetValue = uiProperties.getProperty(key);

        return uiELemetValue;
    }


}
