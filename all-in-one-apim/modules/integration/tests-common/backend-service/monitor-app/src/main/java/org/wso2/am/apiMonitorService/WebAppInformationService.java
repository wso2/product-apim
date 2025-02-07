/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.apiMonitorService;

import org.apache.axis2.AxisFault;
import org.wso2.am.apiMonitorService.beans.WebAppDeployStatus;
import org.wso2.am.apiMonitorService.beans.WebAppStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.rmi.RemoteException;


@Path("/webAppStatus/")
public class WebAppInformationService {

    WebAppStatus webAppStatus;
    WebAppStatusProvider webAppStatusInfoUtil;
    WebAppDeployStatus webAppDeployStatus;

    public WebAppInformationService() throws AxisFault {
        webAppStatusInfoUtil = new WebAppStatusProvider();
        webAppStatus = new WebAppStatus();
        webAppDeployStatus = new WebAppDeployStatus();
    }

    @Path("webappInfo/allApps")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WebAppStatus getWebAppStatus() throws
                                          RemoteException {
        webAppStatus.setStartedWebApps(webAppStatusInfoUtil.getWebAppStatus(null, 0).getStartedWebApps());
        webAppStatus.setFaultyWebApps(webAppStatusInfoUtil.getWebAppStatus(null, 0).getFaultyWebApps());
        webAppStatus.setStoppedWebApps(webAppStatusInfoUtil.getWebAppStatus(null, 0).getSoppedWebApps());
        return webAppStatus;
    }

    @Path("webappInfo/{tenatDomain}/{tenantId}/allApps")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WebAppStatus getWebAppStatusForTenant(@PathParam("tenatDomain") String tenantDomain, @PathParam("tenantId")
    int tenantId) throws
                  RemoteException {
        webAppStatus.setStartedWebApps(webAppStatusInfoUtil.getWebAppStatus(tenantDomain,tenantId).getStartedWebApps());
        webAppStatus.setFaultyWebApps(webAppStatusInfoUtil.getWebAppStatus(tenantDomain,tenantId).getFaultyWebApps());
        webAppStatus.setStoppedWebApps(webAppStatusInfoUtil.getWebAppStatus(tenantDomain,tenantId).getSoppedWebApps());
        return webAppStatus;
    }

    @Path("webappInfo/{webAppName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WebAppDeployStatus getWebAppDeploymentStatus(@PathParam("webAppName") String webAppName) throws
                                                                                                    RemoteException {
        webAppDeployStatus.setIsWebAppExists(webAppStatusInfoUtil
                                                     .getWebAppDeploymentStatus(webAppName, null, 0)
                                                     .getIsWebAppExists());
        webAppDeployStatus.setWebAppData(webAppStatusInfoUtil
                                                 .getWebAppDeploymentStatus(webAppName, null, 0)
                                                 .getWebAppData
                                                         ());
        return webAppDeployStatus;
    }

    @Path("webappInfo/{tenatDomain}/{tenantId}/{webAppName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WebAppDeployStatus getWebAppDeploymentStatusForTenat(@PathParam("webAppName") String webAppName,
                                                                @PathParam("tenatDomain") String tenantDomain,
                                                                @PathParam("tenantId")
                                                                int tenantId) throws
                                                                              RemoteException {
        webAppDeployStatus.setIsWebAppExists(webAppStatusInfoUtil
                                                     .getWebAppDeploymentStatus(webAppName, tenantDomain, tenantId)
                                                     .getIsWebAppExists());
        webAppDeployStatus.setWebAppData(webAppStatusInfoUtil
                                                 .getWebAppDeploymentStatus(webAppName, tenantDomain, tenantId)
                                                 .getWebAppData());
        return webAppDeployStatus;
    }
}
