/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.test.utils.thrift;

import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import java.io.File;

/**
 * DAS data publisher util class, used to configure the event receiver
 */
public class DataPublisherTestUtil {
    public static final String LOCAL_HOST = "localhost";

    public static void setTrustStoreParams() {
        String trustStore =
                FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM" + File.separator
                        + "configFiles" + File.separator + "stats" + File.separator + "client-truststore.jks";
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
    }

    public static void setKeyStoreParams() {
        String keyStore =
                FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM" + File.separator
                        + "configFiles" + File.separator + "stats" + File.separator + "wso2carbon.jks";
        System.setProperty("Security.KeyStore.Location", keyStore);
        System.setProperty("Security.KeyStore.Password", "wso2carbon");
    }

    public static String getDataAgentConfigPath() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM" + File.separator
                + "configFiles" + File.separator + "stats" + File.separator + "data-agent-config.xml";
    }

    public static String getDataBridgeConfigPath() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM" + File.separator
                + "configFiles" + File.separator + "stats" + File.separator + "data-bridge-config.xml";
    }
}
