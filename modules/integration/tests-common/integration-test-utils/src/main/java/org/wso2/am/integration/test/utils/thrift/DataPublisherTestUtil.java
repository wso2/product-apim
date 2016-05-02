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
