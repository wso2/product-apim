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

package org.wso2.am.admin.clients.webapp;

import org.apache.axis2.AxisFault;
import org.wso2.carbon.integration.common.admin.client.utils.AuthenticateStubUtil;
import org.wso2.carbon.webapp.mgt.stub.WebappAdminStub;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.VersionedWebappMetadata;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappMetadata;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappsWrapper;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;

/**
 * Client that enable the functions of WebappAdmin service.
 */
public class WebAppAdminClient {
    private WebappAdminStub webappAdminStub;

    public WebAppAdminClient(String backendUrl, String sessionCookie) throws AxisFault {
        String serviceName = "WebappAdmin";
        String endPoint = backendUrl + serviceName;
        webappAdminStub = new WebappAdminStub(endPoint);
        AuthenticateStubUtil.authenticateStub(sessionCookie, webappAdminStub);
    }

    /**
     * Upload war files to the  "repository/deployment/server/webapps/" location.
     * Those war files will get automatically deployed.
     *
     * @param filePath - path to the location where the war files if locally located.
     * @return boolean - Return true if WebApp upload success else false.
     * @throws RemoteException       - Exception occurs when uploading an war file using WebappAdminStub.
     * @throws MalformedURLException -  Exception occurs when creating the URL Object.
     */

    public boolean uploadWarFile(String filePath) throws RemoteException, MalformedURLException {
        File file = new File(filePath);
        String fileName = file.getName();
        URL url = new URL("file://" + filePath);
        DataHandler dh = new DataHandler(url);
        WebappUploadData webApp;
        webApp = new WebappUploadData();
        webApp.setFileName(fileName);
        webApp.setDataHandler(dh);
        return webappAdminStub.uploadWebapp(new WebappUploadData[]{webApp});

    }

    /**
     * Get WebApp summary in Paged manner.
     *
     * @param searchString - Search string for get the WebAPP summary.
     * @param webAppType   - Type of the WebApp.
     * @param webAppState  - State of the WebApp.
     * @param pageNo       - Page no of the results.
     * @throws RemoteException - Exception occurs when retrieving the WebAppSummary from the WebappAdminStub
     */
    public WebappsWrapper getPagedWebAppsSummary(String searchString, String webAppType, String webAppState, int pageNo)
            throws RemoteException {
        return webappAdminStub.getPagedWebappsSummary(searchString, webAppType, webAppState, pageNo);
    }


    /**
     * Get WebApp list for the given search String.
     *
     * @param webAppNameSearchString - String that contains the data to filter the WebApp.
     * @return List - List of WebApps that matches to the given search String.
     * @throws RemoteException - Exception occurs when call the method getPagedWebAppsSummary().
     */
    public List<String> getWebAppList(String webAppNameSearchString) throws RemoteException {
        List<String> list = new ArrayList<String>();
        WebappsWrapper wrapper = getPagedWebAppsSummary(webAppNameSearchString, "ALL", "ALL", 0);
        VersionedWebappMetadata[] webAppGroups = wrapper.getWebapps();

        if (webAppGroups != null) {
            for (VersionedWebappMetadata webAppGroup : webAppGroups) {
                for (WebappMetadata metaData : webAppGroup.getVersionGroups()) {
                    list.add(metaData.getWebappFile());
                }
            }
        }
        return list;
    }

    /**
     * Get faulty WebApp summary in Paged manner.
     *
     * @param searchString - Search string for get the WebAPP summary.
     * @param webAppType   - Type of the WebApp.
     * @param pageNo       - State of the WebApp.
     * @throws RemoteException - Exception occurs when retrieving the WebAppSummary from the WebappAdminStub
     */
    public WebappsWrapper getPagedFaultyWebAppsSummary(String searchString, String webAppType, int pageNo)
            throws RemoteException {
        return webappAdminStub.getPagedFaultyWebappsSummary(searchString, webAppType, pageNo);
    }

    /**
     * Get faulty  WebApp list for the given search String.
     *
     * @param webAppNameSearchString - String that contains the data to filter the WebApp.
     * @return - List of WebApps that matches to the given search String.
     * @throws RemoteException - Exception occurs when call the method getPagedFaultyWebAppsSummary().
     */
    public List<String> getFaultyWebAppList(String webAppNameSearchString) throws RemoteException {
        List<String> list = new ArrayList<String>();
        WebappsWrapper wrapper = getPagedFaultyWebAppsSummary(webAppNameSearchString, "ALL", 0);
        VersionedWebappMetadata[] webAppGroups = wrapper.getWebapps();

        if (webAppGroups != null && webAppGroups[0].getVersionGroups() != null) {
            for (WebappMetadata metaData : webAppGroups[0].getVersionGroups()) {
                list.add(metaData.getWebappFile());
            }
        }
        return list;
    }

    public void deleteWebAppList(List<String> webAppList, String host) throws RemoteException {
        List<String> webAppKey = new ArrayList<String>();
        for(String webApp: webAppList){
            webAppKey.add(webappAdminStub.getStartedWebapp(webApp + ".war",host).getWebappKey());
        }
        webappAdminStub.deleteAllWebApps(webAppKey.toArray(new String[webAppKey.size()]));
    }

}
