///*
//*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//*
//*WSO2 Inc. licenses this file to you under the Apache License,
//*Version 2.0 (the "License"); you may not use this file except
//*in compliance with the License.
//*You may obtain a copy of the License at
//*
//*http://www.apache.org/licenses/LICENSE-2.0
//*
//*Unless required by applicable law or agreed to in writing,
//*software distributed under the License is distributed on an
//*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//*KIND, either express or implied.  See the License for the
//*specific language governing permissions and limitations
//*under the License.
//*/
//package org.wso2.am.integration.test.utils.common.utils.mgt;
//
//import org.wso2.am.integration.admin.clients.common.ServerAdminClient;
//import org.wso2.am.integration.test.utils.user.mgt.LoginLogoutClient;
//import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
//import org.wso2.carbon.automation.engine.context.AutomationContext;
//import org.wso2.carbon.automation.engine.context.TestUserMode;
//import org.wso2.carbon.automation.engine.frameworkutils.CodeCoverageUtils;
//import org.wso2.carbon.automation.extensions.servers.utils.ClientConnectionUtil;
//import org.wso2.carbon.automation.test.utils.common.FileManager;
//import org.wso2.carbon.utils.ServerConstants;
//import org.xml.sax.SAXException;
//
//import javax.xml.stream.XMLStreamException;
//import javax.xml.xpath.XPathExpressionException;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.nio.channels.FileChannel;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * This class can be used to configure server by  replacing axis2.xml or carbon.xml
// */
//public class ServerConfigurationManager {
//
//    private static final long TIME_OUT = 240000;
//    private File originalConfig;
//    private File backUpConfig;
//    private int port;
//    private String hostname;
//    private String backEndUrl;
//    private AutomationContext autoCtx;
//    private String sessionCookie;
//    private LoginLogoutClient loginLogoutClient;
//    private List<ConfigData> configDatas = new ArrayList<ConfigData>();
//
//    /**
//     * Create a ServerConfigurationManager
//     *
//     * @param productGroup product group
//     * @param userMode     user mode
//     * @throws java.io.IOException
//     * @throws javax.xml.xpath.XPathExpressionException
//     * @throws LoginAuthenticationExceptionException
//     * @throws java.net.URISyntaxException
//     * @throws org.xml.sax.SAXException
//     * @throws javax.xml.stream.XMLStreamException
//     */
//    public ServerConfigurationManager(String productGroup, TestUserMode userMode)
//            throws IOException, XPathExpressionException, LoginAuthenticationExceptionException,
//                   URISyntaxException,
//                   SAXException, XMLStreamException {
//        this.autoCtx = new AutomationContext(productGroup, userMode);
//        this.loginLogoutClient = new LoginLogoutClient(autoCtx);
//        this.backEndUrl = autoCtx.getContextUrls().getBackEndUrl();
//        this.port = new URL(backEndUrl).getPort();
//        this.hostname = new URL(backEndUrl).getHost();
//    }
//
//    /**
//     * Create a ServerConfigurationManager
//     *
//     * @param autoCtx automation context
//     * @throws java.io.IOException
//     * @throws javax.xml.xpath.XPathExpressionException
//     * @throws LoginAuthenticationExceptionException
//     * @throws java.net.URISyntaxException
//     * @throws org.xml.sax.SAXException
//     * @throws javax.xml.stream.XMLStreamException
//     */
//    public ServerConfigurationManager(AutomationContext autoCtx)
//            throws IOException, XPathExpressionException, LoginAuthenticationExceptionException,
//                   URISyntaxException,
//                   SAXException, XMLStreamException {
//        this.loginLogoutClient = new LoginLogoutClient(autoCtx);
//        this.autoCtx = autoCtx;
//        this.backEndUrl = autoCtx.getContextUrls().getBackEndUrl();
//        this.port = new URL(backEndUrl).getPort();
//        this.hostname = new URL(backEndUrl).getHost();
//    }
//
//
//    /**
//     * backup the current server configuration file
//     *
//     * @param fileName file name
//     */
//    private void backupConfiguration(String fileName) {
//        //restore backup configuration
//        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
//        String confDir = carbonHome + File.separator + "repository" + File.separator + "conf"
//                         + File.separator;
//        String AXIS2_XML = "axis2";
//        if (fileName.contains(AXIS2_XML)) {
//            confDir = confDir + "axis2" + File.separator;
//        }
//        originalConfig = new File(confDir + fileName);
//        backUpConfig = new File(confDir + fileName + ".backup");
//        originalConfig.renameTo(backUpConfig);
//
//        configDatas.add(new ConfigData(backUpConfig, originalConfig));
//    }
//
//    /**
//     * Backup a file residing in a cabron server.
//     *
//     * @param file file residing in server to backup.
//     */
//    private void backupConfiguration(File file) {
//        //restore backup configuration
//        originalConfig = file;
//        backUpConfig = new File(file.getAbsolutePath() + ".backup");
//        originalConfig.renameTo(backUpConfig);
//
//        configDatas.add(new ConfigData(backUpConfig, originalConfig));
//    }
//
//    /**
//     * @return will return the carbon home. the location of the server instance
//     */
//    public static String getCarbonHome() {
//        return System.getProperty(ServerConstants.CARBON_HOME);
//    }
//
//    /**
//     * Apply configuration from source file to a target file without restarting.
//     *
//     * @param sourceFile Source file to copy.
//     * @param targetFile Target file that is to be backed up and replaced.
//     * @param backup     boolean value, set this to true if you want to backup the original file.
//     * @throws Exception
//     */
//    public void applyConfigurationWithoutRestart(File sourceFile, File targetFile, boolean backup)
//            throws Exception {
//        // Using inputstreams to copy bytes instead of Readers that copy chars. Otherwise things like JKS files get corrupted during copy.
//        FileChannel source = null;
//        FileChannel destination = null;
//        if (backup) {
//            backupConfiguration(targetFile);
//            source = new FileInputStream(sourceFile).getChannel();
//            destination = new FileOutputStream(originalConfig).getChannel();
//        } else {
//            if (!targetFile.exists()) {
//                targetFile.createNewFile();
//            }
//            source = new FileInputStream(sourceFile).getChannel();
//            destination = new FileOutputStream(targetFile).getChannel();
//        }
//        destination.transferFrom(source, 0, source.size());
//        if (source != null) {
//            source.close();
//        }
//        if (destination != null) {
//            destination.close();
//        }
//    }
//
//    /**
//     * @param sourceFile       file  of the new configuration file
//     * @param targetFile       configuration file required to replace in the server. File must be created
//     *                         with the absolute path.
//     * @param backupConfigFile require to back the existing file
//     * @param restartServer    require to restart the server after replacing the config file
//     * @throws Exception
//     */
//    public void applyConfiguration(File sourceFile, File targetFile, boolean backupConfigFile,
//                                   boolean restartServer) throws Exception {
//        // Using inputstreams to copy bytes instead of Readers that copy chars. Otherwise things like JKS files get corrupted during copy.
//        FileChannel source = null;
//        FileChannel destination = null;
//        if (backupConfigFile) {
//            backupConfiguration(targetFile);
//            source = new FileInputStream(sourceFile).getChannel();
//            destination = new FileOutputStream(originalConfig).getChannel();
//        } else {
//            if (!targetFile.exists()) {
//                targetFile.createNewFile();
//            }
//            source = new FileInputStream(sourceFile).getChannel();
//            destination = new FileOutputStream(targetFile).getChannel();
//        }
//        destination.transferFrom(source, 0, source.size());
//        if (source != null) {
//            source.close();
//        }
//        if (destination != null) {
//            destination.close();
//        }
//        if (restartServer) {
//            restartGracefully();
//        }
//    }
//
//    /**
//     * restore to a last configuration and restart the server
//     *
//     * @throws Exception
//     */
//    public void restoreToLastConfiguration() throws Exception {
//        restoreToLastConfiguration(true);
//    }
//
//    /**
//     * restore all files to last configuration and restart the server
//     *
//     * @throws Exception
//     */
//    public void restoreToLastConfiguration(boolean isRestartRequired) throws Exception {
//        for (ConfigData data : configDatas) {
//            data.getBackupConfig().renameTo(data.getOriginalConfig());
//        }
//        if (isRestartRequired) {
//            restartGracefully();
//        }
//    }
//
//    /**
//     * apply configuration file and restart server to take effect the configuration
//     *
//     * @param newConfig configuration file
//     * @throws Exception
//     */
//    public void applyConfiguration(File newConfig) throws Exception {
//        //to backup existing configuration
//        backupConfiguration(newConfig.getName());
//        FileReader in = new FileReader(newConfig);
//        FileWriter out = new FileWriter(originalConfig);
//        int c;
//        while ((c = in.read()) != -1) {
//            out.write(c);
//        }
//        in.close();
//        out.close();
//        restartGracefully();
//    }
//
//    /**
//     * apply configuration file and restart server to take effect the configuration
//     *
//     * @param newConfig configuration file
//     * @throws Exception
//     */
//    public void applyConfigurationWithoutRestart(File newConfig) throws Exception {
//        //to backup existing configuration
//        backupConfiguration(newConfig.getName());
//        FileReader in = new FileReader(newConfig);
//        FileWriter out = new FileWriter(originalConfig);
//        int c;
//        while ((c = in.read()) != -1) {
//            out.write(c);
//        }
//        in.close();
//        out.close();
//    }
//
//    /**
//     * Methods to replace configuration files in products.
//     *
//     * @param sourceFile - configuration file to be copied for your local machine or carbon server it self.
//     * @param targetFile - configuration file in carbon server. e.g - path to axis2.xml in config directory
//     * @throws Exception - if file IO error
//     */
//    public void applyConfiguration(File sourceFile, File targetFile) throws Exception {
//        //to backup existing configuration
//        backupConfiguration(targetFile.getName());
//        FileReader in = new FileReader(sourceFile);
//        FileWriter out = new FileWriter(originalConfig);
//        int c;
//        while ((c = in.read()) != -1) {
//            out.write(c);
//        }
//        in.close();
//        out.close();
//        restartGracefully();
//    }
//
//    /**
//     * Restart Server Gracefully  from admin user
//     *
//     * @throws Exception
//     */
//    public void restartGracefully() throws Exception {
//        //todo use ServerUtils class restart
//        sessionCookie = loginLogoutClient.login();
//        ServerAdminClient serverAdmin = new ServerAdminClient(backEndUrl, sessionCookie);
//        serverAdmin.restartGracefully();
//        CodeCoverageUtils.renameCoverageDataFile(System.getProperty(ServerConstants.CARBON_HOME));
//        Thread.sleep(20000); //forceful wait until emma dump coverage data file.
//        ClientConnectionUtil.waitForPort(port, TIME_OUT, true, hostname);
//        Thread.sleep(5000); //forceful wait until server is ready to be served
//        ClientConnectionUtil.waitForLogin(autoCtx);
//    }
//
//    /**
//     * Restart server gracefully from current user session
//     *
//     * @param sessionCookie session cookie
//     * @throws Exception
//     */
//    public void restartGracefully(String sessionCookie) throws Exception {
//        //todo use ServerUtils class restart
//        ServerAdminClient serverAdmin = new ServerAdminClient(backEndUrl, sessionCookie);
//        CodeCoverageUtils.renameCoverageDataFile(System.getProperty(ServerConstants.CARBON_HOME));
//        serverAdmin.restartGracefully();
//        Thread.sleep(20000); //forceful wait until emma dump coverage data file.
//        ClientConnectionUtil.waitForPort(port, TIME_OUT, true, hostname);
//        Thread.sleep(5000); //forceful wait until server is ready to be served
//        ClientConnectionUtil.waitForLogin(autoCtx);
//    }
//
//    /**
//     * Restart Server forcefully  from admin user
//     *
//     * @throws Exception
//     */
//    public void restartForcefully() throws Exception {
//        //todo use ServerUtils class restart
//        sessionCookie = loginLogoutClient.login();
//        ServerAdminClient serverAdmin = new ServerAdminClient(backEndUrl, sessionCookie);
//        serverAdmin.restart();
//        CodeCoverageUtils.renameCoverageDataFile(System.getProperty(ServerConstants.CARBON_HOME));
//        Thread.sleep(20000); //forceful wait until emma dump coverage data file.
//        ClientConnectionUtil.waitForPort(port, TIME_OUT, true, hostname);
//        Thread.sleep(5000); //forceful wait until server is ready to be served
//        ClientConnectionUtil.waitForLogin(autoCtx);
//    }
//
//    /**
//     * Copy Jar file to server component/lib
//     *
//     * @param jar jar file
//     * @throws java.io.IOException
//     * @throws java.net.URISyntaxException
//     */
//    public void copyToComponentLib(File jar) throws IOException, URISyntaxException {
//        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
//        String lib = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
//                     + "lib";
//        FileManager.copyJarFile(jar, lib);
//    }
//
//    /**
//     * @param fileName file name
//     * @throws java.io.IOException
//     * @throws java.net.URISyntaxException
//     */
//    public void removeFromComponentLib(String fileName) throws IOException, URISyntaxException {
//        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
//        String filePath = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
//                          + "lib" + File.separator + fileName;
//        FileManager.deleteFile(filePath);
////      removing osgi bundle from dropins; OSGI bundle versioning starts with _1.0.0
//        fileName = fileName.replace("-", "_");
//        fileName = fileName.replace(".jar", "_1.0.0.jar");
//        removeFromComponentDropins(fileName);
//    }
//
//    /**
//     * /**
//     * Copy Jar file to server component/dropins
//     *
//     * @param jar jar file
//     * @throws java.io.IOException
//     * @throws java.net.URISyntaxException
//     */
//    public void copyToComponentDropins(File jar) throws IOException, URISyntaxException {
//        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
//        String lib = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
//                     + "dropins";
//        FileManager.copyJarFile(jar, lib);
//    }
//
//    /**
//     * @param fileName file name
//     * @throws java.io.IOException
//     * @throws java.net.URISyntaxException
//     */
//    public void removeFromComponentDropins(String fileName) throws IOException, URISyntaxException {
//        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
//        String filePath = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
//                          + "dropins" + File.separator + fileName;
//        FileManager.deleteFile(filePath);
//    }
//
//    /**
//     * Private class to hold config data
//     */
//    private class ConfigData {
//
//        private File backupConfig;
//        private File originalConfig;
//
//        public ConfigData(File backupConfig, File originalConfig) {
//            this.backupConfig = backupConfig;
//            this.originalConfig = originalConfig;
//        }
//
//        public File getBackupConfig() {
//            return backupConfig;
//        }
//
//        public File getOriginalConfig() {
//            return originalConfig;
//        }
//    }
//}
//
