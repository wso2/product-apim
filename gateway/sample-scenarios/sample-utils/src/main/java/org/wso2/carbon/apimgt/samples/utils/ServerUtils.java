/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.apimgt.samples.utils;

import org.apache.axis2.AxisFault;
import org.wso2.carbon.apimgt.samples.utils.Clients.ServerAdminServiceClient;
import org.wso2.carbon.server.admin.stub.ServerAdminException;

import java.rmi.RemoteException;

public class ServerUtils {

    private static ServerAdminServiceClient serverAdminServiceClient;

    public static boolean restartServer(String serviceEndpoint, String username, String password)
            throws RemoteException, ServerAdminException {
        System.out.print("Restarting the server...");
        serverAdminServiceClient = new ServerAdminServiceClient(serviceEndpoint, username, password);
        return serverAdminServiceClient.restartServer();
    }

    public static boolean waitForServerStartup(String serviceEndpoint, String username, String password)
            throws RemoteException, ServerAdminException, InterruptedException {

        // This sleep is introduced to wait until the server is shutdown.
        Thread.sleep(10000);
        boolean resultOne = true;
        boolean resultTwo = false;
        while (resultOne) {
            try {
                serverAdminServiceClient = new ServerAdminServiceClient(serviceEndpoint, username, password);
                resultTwo = serverAdminServiceClient.isAlive();
            } catch (AxisFault e) {
                //Ignoring the exception to execute the loop since this code blocks purpose is to check the server is up
                //and running.
            }
            if (resultTwo) {
                resultOne = false;
            }
            Thread.sleep(1000);
            System.out.print(".");
        }
        System.out.println(" Server started");
        return resultTwo;
    }
}
