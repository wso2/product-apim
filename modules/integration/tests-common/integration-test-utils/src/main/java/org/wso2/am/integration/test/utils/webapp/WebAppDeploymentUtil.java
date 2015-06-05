/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.am.integration.test.utils.webapp;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;

/**
 * Util that is enabled the more utility methods for handling the WebApps
 */
public class WebAppDeploymentUtil {
    private static Log log = LogFactory.getLog(WebAppDeploymentUtil.class);
    private static int WEB_APP_DEPLOYMENT_DELAY = 90 * 1000;

    /**
     * Check whether the given web app is deployed correctly. This method will wait maximum 90 seconds
     * until it get deployed. check for the web app d
     *
     * @param backEndUrl     - Backend URL of the server where the web app is host.
     * @param sessionCookie  - valid session cookie of the server where the web app is host.
     * @param webAppFileName - File name of the web app that want to check for the deployment.
     * @return boolean - if WebAPp is get deployed withing 90 seconds or else return false.
     * @throws APIManagerIntegrationTestException - Exception throws when creating WebAppAdminClient object and calling
     *                                            methods of WebAppAdminClient class.
     */
    public static boolean isWebApplicationDeployed(String backEndUrl, String sessionCookie, String webAppFileName)
            throws APIManagerIntegrationTestException {
        log.info("waiting " + WEB_APP_DEPLOYMENT_DELAY + " millis for Service deployment " + webAppFileName);

        WebAppAdminClient webAppAdminClient;
        try {
            webAppAdminClient = new WebAppAdminClient(backEndUrl, sessionCookie);
        } catch (AxisFault axisFault) {
            throw new APIManagerIntegrationTestException("AxisFault Exception occurs when creating the WebAppAdminClient" +
                    " object ", axisFault);
        }

        List<String> webAppList;
        List<String> faultyWebAppList;
        String webAppName = webAppFileName + ".war";
        boolean isWebAppDeployed = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) <
                WEB_APP_DEPLOYMENT_DELAY) {
            try {
                webAppList = webAppAdminClient.getWebAppList(webAppFileName);
                faultyWebAppList = webAppAdminClient.getFaultyWebAppList(webAppFileName);
            } catch (RemoteException e) {
                throw new APIManagerIntegrationTestException("RemoteException occurs while retrieving the web app list" +
                        " from WebAppAdminClient.", e);
            }
            //check in the faulty WebApp list
            for (String faultWebAppName : faultyWebAppList) {
                if (webAppName.equalsIgnoreCase(faultWebAppName)) {
                    isWebAppDeployed = false;
                    log.info(webAppFileName + "- Web Application is faulty");
                    return isWebAppDeployed;
                }
            }
            //check in the successfully deployed WebApp list
            for (String name : webAppList) {
                if (webAppName.equalsIgnoreCase(name)) {
                    isWebAppDeployed = true;
                    log.info(webAppFileName + " Web Application deployed in " + time + " millis");
                    return isWebAppDeployed;
                }
            }

            try {
                //Sleeping 500 milliseconds before the next check of the deployment.
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                log.warn("InterruptedException occurs while waiting sleep for 500 milliseconds");
            }
        }
        return isWebAppDeployed;
    }

}
