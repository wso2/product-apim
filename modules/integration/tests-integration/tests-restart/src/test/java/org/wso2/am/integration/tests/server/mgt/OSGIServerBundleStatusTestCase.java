/*
*Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.telnet.TelnetClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIMTestConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

/*
  This test class can be used to identify required osgi component service
  (eg: unsatisfied) in server startup
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class OSGIServerBundleStatusTestCase {

    private static final Log log = LogFactory.getLog(OSGIServerBundleStatusTestCase.class);
    private TelnetClient telnet = new TelnetClient();
    private ArrayList<String> arrList = new ArrayList<String>();
    private ArrayList<String> unsatisfiedList = new ArrayList<String>();
    private PrintStream out;

    @AfterClass(alwaysRun = true)
    public void disconnectFromOSGiConsole() throws Exception {
        disconnect();  // telnet disconnection
    }

    @Test(groups = "wso2.all", description = "Identifying and storing unsatisfied OSGI components")
    public void testOSGIUnsatisfiedComponents() throws Exception {
        telnet.connect(InetAddress.getLocalHost().getHostAddress(), Integer.parseInt(APIMTestConstants.OSGI_CONSOLE_TELNET_PORT));
        telnet.setSoTimeout(10000);
        ArrayList<String> arr = retrieveUnsatisfiedComponentsList("ls");
        for (int x = 0; x < arr.size(); x++) {
            unsatisfiedList.add(arrList.get(x).split("\t")[3]);
            log.info(unsatisfiedList.get(x));
        }
        assertEquals(unsatisfiedList.size(), 0, "Unsatisfied components detected" +
                                                " in server startup. " + getString(unsatisfiedList));
    }

    private ArrayList<String> retrieveUnsatisfiedComponentsList(String command) throws IOException {
        writeInputCommand(command);
        try {
            readResponse();
        } catch (SocketTimeoutException e) {
            log.error("Socket timeout Exception " + e);
        }
        return arrList;
    }

    private void writeInputCommand(String value) throws UnsupportedEncodingException {
        out = new PrintStream(telnet.getOutputStream(), true, "UTF-8");
        out.println(value);
        out.flush();
        log.info(value);
    }

    private void readResponse() throws IOException {
        InputStream in = telnet.getInputStream();
        BufferedReader inBuff = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String inputLine;
        while ((inputLine = inBuff.readLine()) != null) {
            if (inputLine.contains("Unsatisfied")) {  // filtering Unsatisfied components
                arrList.add(inputLine);
                log.info(inputLine);
            }
        }
        inBuff.close();
        out.close();
    }

    private void disconnect() {
        try {
            telnet.disconnect();
        } catch (IOException e) {
            log.error("Error occurred while telnet disconnection " + e);
        }
    }

    private String getString(ArrayList<String> list) {
        if (list != null && list.size() > 0) {
            return list.toString();
        }
        return "";
    }

}
