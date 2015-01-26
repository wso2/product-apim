package org.wso2.jmeter.tests;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.automation.tools.jmeter.JMeterTest;
import org.wso2.automation.tools.jmeter.JMeterTestManager;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.automation.utils.esb.StockQuoteClient;
import org.wso2.carbon.h2.osgi.utils.CarbonUtils;
import org.wso2.carbon.utils.ServerConstants;

public class JmeterTestCases {

    private ServerConfigurationManager serverConfigurationManager;

    protected Log log = LogFactory.getLog(getClass());
    protected EnvironmentVariables amServer;
    protected UserInfo userInfo;
    protected OMElement synapseConfiguration = null;

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @BeforeClass(alwaysRun = true)
    public void testChangeTransportMechanism() throws Exception, AxisFault {
        init(2);
        serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

        File axis2xmlFile = new File(carbonHome + File.separator + "repository" + File.separator + "conf"
                                     + File.separator + "axis2" + File.separator + "axis2.xml");

        File sourceAxis2xmlFile = new File(carbonHome + File.separator + "repository" + File.separator
                                           + "conf" + File.separator + "axis2" + File.separator + "axis2.xml_NHTTP");

        if (!axis2xmlFile.exists() || !sourceAxis2xmlFile.exists()) {
            throw new IOException("File not found in given location");
        }

        serverConfigurationManager.applyConfiguration(sourceAxis2xmlFile, axis2xmlFile);
    }

    protected void init(int userId) throws Exception {
        userInfo = UserListCsvReader.getUserInfo(userId);
        EnvironmentBuilder builder = new EnvironmentBuilder().am(userId);
        amServer = builder.build().getAm();
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = "wso2.am", description = "Covers tenant creation, role creation, API creation, publish api," +
                                            "get default app id, subscribe users to default app, invoke api")
    public void testListServices() throws Exception {
        JMeterTest script =
                new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator + "artifacts"
                                        + File.separator + "AM" + File.separator + "scripts"
                                        + File.separator + "API_Manager_functionality_and_loadTest.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @AfterClass(alwaysRun = true)
    public void testCleanup() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
        serverConfigurationManager = null;
    }
}
