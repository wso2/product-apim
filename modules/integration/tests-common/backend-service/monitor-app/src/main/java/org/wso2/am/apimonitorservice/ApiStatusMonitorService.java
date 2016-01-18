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

package org.wso2.am.apimonitorservice;


import org.apache.axis2.AxisFault;
import org.wso2.am.apimonitorservice.beans.APIDeployStatus;
import org.wso2.am.apimonitorservice.beans.APIStats;
import org.wso2.am.apimonitorservice.beans.TenantStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.rmi.RemoteException;

@Path("/api_status/")
/**
 * REST web service that provide the tenant status (TenantStatus and the web-app status(WebAppStatus).
 * TenantStatus is include whether the Tenant context is loaded or not.
 * WebAppStatus id include whether the Tenant context is loaded or not, web-app is started or not and the web-app is in
 * ghost status or not.
 */
public class ApiStatusMonitorService {

    org.wso2.am.apimonitorservice.APIStatusUtil apiStatusUtil;
    APIStats stats;
    APIDeployStatus apiStatus;

    public ApiStatusMonitorService() throws AxisFault {
        apiStatusUtil = new org.wso2.am.apimonitorservice.APIStatusUtil();
        stats = new APIStats();
        apiStatus = new APIDeployStatus();
    }

    @Path("tenant-status/{tenantDomain}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public TenantStatus isTenantLoaded(@PathParam("tenantDomain") String tenantDomain) {
        return APIStatusUtil.getTenantStatus(tenantDomain);


    }

    @Path("api-data/{apiName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of tenant.
     */
    public APIDeployStatus isAPILoaded(@PathParam("apiName") String apiName) {
        apiStatus.setIsApiExists(apiStatusUtil.isApiExists(apiName));
        apiStatus.setApiData(apiStatusUtil.getAPIDataByName(apiName));
        return apiStatus;
    }

    @Path("api-info/{tenantDomain}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of api.
     */
    public APIStats getApiCount(@PathParam("tenantDomain") String tenantDomain) throws
                                                                                RemoteException {
        stats.setDeployedApiCount(apiStatusUtil.getDeployedApiStats());
        stats.setListOfApiNames(apiStatusUtil.getDeployedApiNames());
        return stats;
    }




}
