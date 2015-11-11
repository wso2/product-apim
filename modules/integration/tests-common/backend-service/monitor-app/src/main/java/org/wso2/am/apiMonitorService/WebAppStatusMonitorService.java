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
package org.wso2.am.apimonitorservice;

import org.apache.axis2.AxisFault;
import org.wso2.am.apimonitorservice.beans.WebAppDeployStatus;
import org.wso2.am.apimonitorservice.beans.WebAppStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.rmi.RemoteException;


@Path("/web_app_status/")
public class WebAppStatusMonitorService {

    WebAppStatus webAppStatus;
    WebAppStatusInfoUtil webAppStatusInfoUtil;
    WebAppDeployStatus webAppDeployStatus;

    public WebAppStatusMonitorService() throws AxisFault {
        webAppStatusInfoUtil = new WebAppStatusInfoUtil();
        webAppStatus = new WebAppStatus();
        webAppDeployStatus = new WebAppDeployStatus();
    }

    @Path("webapp-info/allApps")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WebAppStatus getWebAppStatus() throws
                                          RemoteException {
        webAppStatus.setStartedWebApps(webAppStatusInfoUtil.getWebAppStatus().getStartedWebApps());
        webAppStatus.setFaultyWebApps(webAppStatusInfoUtil.getWebAppStatus().getFaultyWebApps());
        webAppStatus.setStoppedWebApps(webAppStatusInfoUtil.getWebAppStatus().getSoppedWebApps());
        return webAppStatus;
    }

    @Path("webapp-info/{webAppName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WebAppDeployStatus getWebAppDeploymentStatus(@PathParam("webAppName") String webAppName)
            throws
            RemoteException {
        webAppDeployStatus.setIsWebAppExists(webAppStatusInfoUtil.getWebAppDeploymentStatus(webAppName).getIsWebAppExists());
        webAppDeployStatus.setWebAppData(webAppStatusInfoUtil.getWebAppDeploymentStatus(webAppName).getWebAppData());
        return webAppDeployStatus;
    }
}
