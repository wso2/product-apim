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

import org.wso2.carbon.apimgt.samples.utils.Clients.WebAppAdminClient;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;

/**
 * This class consist of the functionalities related to war file deployments.a
 */
public class WebAppDeployUtils {

    /**
     * THis method is used to deploy war files.
     *
     * @param serviceEndpoint   service endpoint url of the instance that war files needs to be deployed.
     * @param username          admin username
     * @param password          admin password
     * @param warFileLocation   war file location
     * @param warFileName       war file name (Ex: of the war file is sample.war , warFileName is sample).
     * @return returns a boolean whether the web app is deployed or not.
     * @throws RemoteException  throws is an error occurred when calling to the service endpoint.
     * @throws MalformedURLException    throws is an error occurred when uploading the web app war file.
     */
    public static boolean deployWebApp(String serviceEndpoint, String username, String password, String warFileLocation,
            String warFileName) throws RemoteException, MalformedURLException {

        boolean result = false;
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(serviceEndpoint, username, password);

        List<String> deployedWebApps = webAppAdminClient.getWebAppList(warFileName);
        if (deployedWebApps.isEmpty()) {
            result = webAppAdminClient.uploadWarFile(warFileLocation);
        }
        return result;
    }
}
