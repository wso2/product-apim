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

package org.wso2.carbon.am.jmeter;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * This jmeter based test case added to automate test for issue
 * https://wso2.org/jira/browse/APIMANAGER-850
 */
public class JmeterDomainRestrictionTestCase {

    protected Log log = LogFactory.getLog(getClass());
    protected EnvironmentVariables amServer;
    protected UserInfo userInfo;
    protected OMElement synapseConfiguration = null;

    @BeforeClass(alwaysRun = true)
    public void testChangeTransportMechanism() throws Exception {
        init(0);
    }

    protected void init(int userId) throws Exception {
        userInfo = UserListCsvReader.getUserInfo(userId);
        EnvironmentBuilder builder = new EnvironmentBuilder().am(userId);
        amServer = builder.build().getAm();
    }

    @Test(groups = "wso2.am", description = "Login to api manager as user2")
    public void testListServices() throws Exception {
        JMeterTest script =
                new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator + "artifacts"
                                        + File.separator + "AM" + File.separator + "scripts"
                                        + File.separator + "DomainRestrictionTest.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
    }
}
