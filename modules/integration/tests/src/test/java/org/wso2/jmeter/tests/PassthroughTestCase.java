package org.wso2.jmeter.tests;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.automation.tools.jmeter.JMeterTest;
import org.wso2.automation.tools.jmeter.JMeterTestManager;
import org.wso2.carbon.am.tests.util.WireMonitorServer;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.IOException;

public class PassthroughTestCase {

    private ServerConfigurationManager serverConfigurationManager;

    private Log log = LogFactory.getLog(getClass());
    private EnvironmentVariables amServer;
    private UserInfo userInfo;
    private OMElement synapseConfiguration = null;

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @BeforeClass(alwaysRun = true)
    public void testChangeTransportMechanism() throws Exception, AxisFault {
        int userId = 2;
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
                                        + File.separator + "API_Manager_functionality_and_loadTest_new_tenant.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = "wso2.am", description = "creates and API, subscribe to it and send GET and DELETE requests without " +
                                            "Content-Type header and checks for if the Content-Type header is forcefully added by APIM, " +
                                            "which should not happen")
    public void JIRA_APIMANAGER_1397_testContentTypeHeaderInsertionCheck() throws Exception {
        JMeterTest publishScript = new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator
                                                           + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                                                           + File.separator + "content_type_check_publish_and_subscribe_script.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(publishScript);

        WireMonitorServer wireMonitorServer = new WireMonitorServer(6789);

        wireMonitorServer.start();

        Thread.sleep(1000);

        JMeterTest scriptGET = new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator
                                                       + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                                                       + File.separator + "content_type_check_for_GET_script.jmx"));

        manager.runTest(scriptGET);

        while (true) {
            String reply = wireMonitorServer.getCapturedMessage();
            if(reply.length()>1)
                if(reply.contains("ThisParamIsRequiredForTest_GET")){
                    /**
                     * Assert for the Content-Type header
                     */
                    Assert.assertTrue(!reply.contains("Content-Type"), "Content-Type header has been added to GET request forcefully!!");
                }
            break;
        }

        wireMonitorServer = new WireMonitorServer(6789);

        wireMonitorServer.start();

        Thread.sleep(1000);

        JMeterTest scriptDELETE = new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator
                                                          + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                                                          + File.separator + "content_type_check_for_DELETE_script.jmx"));

        manager.runTest(scriptDELETE);

        while (true) {
            String reply = wireMonitorServer.getCapturedMessage();
            if(reply.length()>1)
                if(reply.contains("ThisParamIsRequiredForTest_DELETE")){
                    /**
                     * Assert for the Content-Type header
                     */
                    Assert.assertTrue(!reply.contains("Content-Type"), "Content-Type header has been added to DELETE request forcefully!!");
                }
            break;
        }

    }    
}
